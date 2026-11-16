package com.example.vpn

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.data.DataGuardPreferencesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.FileInputStream

class DataGuardVpnService : VpnService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Volatile
    private var vpnInterface: ParcelFileDescriptor? = null

    @Volatile
    private var isRunning = false

    private var workerThread: Thread? = null

    companion object {
        const val ACTION_START = "com.example.dataguard.START_VPN"
        const val ACTION_STOP = "com.example.dataguard.STOP_VPN"
        const val ACTION_UPDATE_RULES = "com.example.dataguard.UPDATE_VPN_RULES"

        private const val NOTIFICATION_CHANNEL_ID = "dataguard_vpn_channel"
        private const val NOTIFICATION_ID = 4041
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopFirewallVpn()
                return START_NOT_STICKY
            }
            ACTION_UPDATE_RULES -> {
                restartTunnelWithLatestRules()
            }
            ACTION_START, null -> {
                if (intent == null) {
                    serviceScope.launch {
                        val saved = DataGuardPreferencesRepository(applicationContext)
                            .settingsFlow.first().blockedPackages
                        FirewallStateHolder.setTargetPackages(saved)
                        startFirewallVpn()
                    }
                } else {
                    startFirewallVpn()
                }
            }
        }
        return START_STICKY
    }

    private fun restartTunnelWithLatestRules() {
        stopTunnelOnly()
        startFirewallVpn()
    }

    @Synchronized
    private fun startFirewallVpn() {
        if (isRunning) return

        ensureNotificationChannel()
        val notification = buildStatusNotification(
            blockedCount = FirewallStateHolder.getTargetPackages().size
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                )
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        } catch (e: Exception) {
            FirewallStateHolder.onVpnStopped("Impossible de démarrer le service avant-plan : ${e.message}")
            stopSelf()
            return
        }

        try {
            val targetPackages = FirewallStateHolder.getTargetPackages()
            val builder = Builder()
                .setSession(getString(R.string.vpn_service_name))
                .addAddress("10.0.0.2", 32)
                .addDnsServer("1.1.1.1")

            var matchedInstalled = 0
            for (pkg in targetPackages) {
                if (pkg == packageName) continue
                try {
                    builder.addAllowedApplication(pkg)
                    matchedInstalled++
                } catch (_: PackageManager.NameNotFoundException) {
                    // L'application cible n'est pas installée sur ce téléphone
                }
            }

            // Sécurité critique Android :
            // Si aucune app ciblée n'est installée, addAllowedApplication() n'a jamais été appelé.
            // Sans garde-fou, addRoute("0.0.0.0", 0) couperait TOUT le trafic de l'appareil !
            // On n'intercepte donc 0.0.0.0/0 que si au moins un paquet installé est ciblé.
            if (matchedInstalled > 0) {
                builder.addRoute("0.0.0.0", 0)
            } else {
                builder.addRoute("10.0.0.0", 24)
            }

            val descriptor = builder.establish()
            if (descriptor == null) {
                FirewallStateHolder.onVpnStopped("Autorisation VPN requise par Android.")
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return
            }

            vpnInterface = descriptor
            isRunning = true
            FirewallStateHolder.onVpnStarted(matchedInstalledCount = matchedInstalled)

            var lastUiLogEmitMs = 0L
            workerThread = Thread({
                val input = FileInputStream(descriptor.fileDescriptor)
                val packet = ByteArray(32767)
                try {
                    while (isRunning && !Thread.currentThread().isInterrupted) {
                        val length = input.read(packet)
                        if (length < 0) break
                        if (length > 0) {
                            val parsed = PacketParser.parseIpv4(packet, length)
                            if (parsed != null) {
                                val now = System.currentTimeMillis()
                                if (now - lastUiLogEmitMs >= 40L) {
                                    lastUiLogEmitMs = now
                                    FirewallStateHolder.onPacketBlocked(parsed)
                                }
                            }
                        }
                    }
                } catch (_: Exception) {
                    // Fermeture normale du descripteur VPN
                } finally {
                    try {
                        input.close()
                    } catch (_: Exception) {
                    }
                }
            }, "DataGuard-VPN-Loop").apply {
                isDaemon = true
                start()
            }
        } catch (e: Exception) {
            FirewallStateHolder.onVpnStopped("Erreur pare-feu VPN : ${e.localizedMessage ?: "inconnue"}")
            stopTunnelOnly()
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    @Synchronized
    private fun stopTunnelOnly() {
        isRunning = false
        workerThread?.interrupt()
        workerThread = null
        try {
            vpnInterface?.close()
        } catch (_: Exception) {
        }
        vpnInterface = null
    }

    @Synchronized
    private fun stopFirewallVpn() {
        stopTunnelOnly()
        FirewallStateHolder.onVpnStopped()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onRevoke() {
        stopFirewallVpn()
        super.onRevoke()
    }

    override fun onDestroy() {
        stopTunnelOnly()
        FirewallStateHolder.onVpnStopped()
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun ensureNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.notification_channel_desc)
                setShowBadge(false)
            }
            nm.createNotificationChannel(channel)
        }
    }

    private fun buildStatusNotification(blockedCount: Int): Notification {
        val openAppIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = PendingIntent.getService(
            this,
            1,
            Intent(this, DataGuardVpnService::class.java).apply {
                action = ACTION_STOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle(getString(R.string.notification_title_active))
            .setContentText("$blockedCount règle(s) d'application actives • Tunnel local 0 octet")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .setContentIntent(openAppIntent)
            .addAction(
                0,
                getString(R.string.notification_stop_action),
                stopIntent
            )
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
}
