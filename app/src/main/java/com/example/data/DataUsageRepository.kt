package com.example.data

import android.app.AppOpsManager
import android.app.usage.NetworkStats
import android.app.usage.NetworkStatsManager
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.net.ConnectivityManager
import android.net.TrafficStats
import android.os.Build
import android.os.Process
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class UsagePeriod(val label: String, val durationMs: Long) {
    REALTIME("Temps réel", 60L * 60L * 1000L),
    DAY_24H("24 heures", 24L * 60L * 60L * 1000L),
    WEEK_7D("7 jours", 7L * 24L * 60L * 60L * 1000L)
}

enum class UsageChartMode(val label: String) {
    AREA_TIMELINE("Courbe live"),
    BAR_BY_APP("Par app")
}

data class AppDataConsumption(
    val packageName: String,
    val appName: String,
    val uid: Int,
    val mobileBytes: Long,
    val wifiBytes: Long,
    val liveBytesPerSec: Long,
    val isBlocked: Boolean,
    val iconDrawable: Drawable?
) {
    val totalBytes: Long
        get() = mobileBytes + wifiBytes
}

data class UsageTimelinePoint(
    val timestampMs: Long,
    val label: String,
    val mobileBytes: Long,
    val wifiBytes: Long,
    val blockedBytes: Long
)

data class DataUsageSnapshot(
    val period: UsagePeriod = UsagePeriod.REALTIME,
    val hasUsageAccess: Boolean = false,
    val perAppConsumption: List<AppDataConsumption> = emptyList(),
    val timelinePoints: List<UsageTimelinePoint> = emptyList(),
    val totalMobileBytes: Long = 0L,
    val totalWifiBytes: Long = 0L,
    val totalLiveBytesPerSec: Long = 0L,
    val lastUpdatedMs: Long = System.currentTimeMillis()
)

class DataUsageRepository(private val context: Context) {

    private val pm: PackageManager = context.packageManager
    private val networkStatsManager: NetworkStatsManager? =
        context.getSystemService(Context.NETWORK_STATS_SERVICE) as? NetworkStatsManager

    private val previousUidTrafficBytes = mutableMapOf<Int, Long>()
    private val sessionUidMobileBytes = mutableMapOf<Int, Long>()
    private val sessionUidWifiBytes = mutableMapOf<Int, Long>()
    private val liveTimeline = ArrayDeque<UsageTimelinePoint>()
    private var lastSampleTimeMs: Long = 0L
    private var lastBlockedBytesSnapshot: Long = 0L

    fun hasUsageStatsPermission(): Boolean {
        return try {
            val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
            val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                appOps.unsafeCheckOpNoThrow(
                    AppOpsManager.OPSTR_GET_USAGE_STATS,
                    Process.myUid(),
                    context.packageName
                )
            } else {
                @Suppress("DEPRECATION")
                appOps.checkOpNoThrow(
                    AppOpsManager.OPSTR_GET_USAGE_STATS,
                    Process.myUid(),
                    context.packageName
                )
            }
            mode == AppOpsManager.MODE_ALLOWED
        } catch (_: Exception) {
            false
        }
    }

    suspend fun sampleAndQueryUsage(
        period: UsagePeriod,
        rules: List<AppFirewallRule>,
        networkStatus: NetworkTransportStatus,
        sessionBlockedBytes: Long
    ): DataUsageSnapshot = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val hasAccess = hasUsageStatsPermission()
        val installedRules = rules.filter { it.isInstalled }

        val elapsedSec = if (lastSampleTimeMs > 0L) {
            ((now - lastSampleTimeMs) / 1000.0).coerceAtLeast(0.5)
        } else {
            1.0
        }

        val uidToRule = linkedMapOf<Int, AppFirewallRule>()
        val liveRateByUid = mutableMapOf<Int, Long>()
        var intervalMobileDelta = 0L
        var intervalWifiDelta = 0L

        for (rule in installedRules) {
            val uid = try {
                pm.getApplicationInfo(rule.packageName, 0).uid
            } catch (_: Exception) {
                continue
            }
            if (!uidToRule.containsKey(uid)) {
                uidToRule[uid] = rule
            }

            val rx = TrafficStats.getUidRxBytes(uid).let { if (it > 0L) it else 0L }
            val tx = TrafficStats.getUidTxBytes(uid).let { if (it > 0L) it else 0L }
            val currentTotal = rx + tx
            val prevTotal = previousUidTrafficBytes[uid]
            previousUidTrafficBytes[uid] = currentTotal

            if (prevTotal != null && currentTotal >= prevTotal) {
                val delta = currentTotal - prevTotal
                liveRateByUid[uid] = (delta / elapsedSec).toLong()
                if (networkStatus == NetworkTransportStatus.CELLULAR) {
                    sessionUidMobileBytes[uid] = (sessionUidMobileBytes[uid] ?: 0L) + delta
                    intervalMobileDelta += delta
                } else if (networkStatus == NetworkTransportStatus.WIFI_FREE ||
                    networkStatus == NetworkTransportStatus.WIFI_METERED
                ) {
                    sessionUidWifiBytes[uid] = (sessionUidWifiBytes[uid] ?: 0L) + delta
                    intervalWifiDelta += delta
                }
            } else {
                liveRateByUid[uid] = 0L
            }
        }

        val blockedDelta = (sessionBlockedBytes - lastBlockedBytesSnapshot).coerceAtLeast(0L)
        lastBlockedBytesSnapshot = sessionBlockedBytes
        lastSampleTimeMs = now

        val timeFmt = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        if (liveTimeline.size >= 16) {
            liveTimeline.removeFirst()
        }
        liveTimeline.addLast(
            UsageTimelinePoint(
                timestampMs = now,
                label = timeFmt.format(Date(now)),
                mobileBytes = intervalMobileDelta,
                wifiBytes = intervalWifiDelta,
                blockedBytes = blockedDelta
            )
        )

        val mobileByUid = mutableMapOf<Int, Long>()
        val wifiByUid = mutableMapOf<Int, Long>()

        if (hasAccess && networkStatsManager != null) {
            val startTime = now - period.durationMs
            @Suppress("DEPRECATION")
            querySummaryIntoMap(ConnectivityManager.TYPE_MOBILE, startTime, now, mobileByUid)
            @Suppress("DEPRECATION")
            querySummaryIntoMap(ConnectivityManager.TYPE_WIFI, startTime, now, wifiByUid)
        } else {
            for ((uid, _) in uidToRule) {
                val totalSinceBoot = previousUidTrafficBytes[uid] ?: 0L
                val trackedMobile = sessionUidMobileBytes[uid] ?: 0L
                val trackedWifi = sessionUidWifiBytes[uid] ?: 0L
                val remainingBootBytes = (totalSinceBoot - trackedMobile - trackedWifi).coerceAtLeast(0L)
                if (networkStatus == NetworkTransportStatus.CELLULAR) {
                    mobileByUid[uid] = trackedMobile + remainingBootBytes
                    wifiByUid[uid] = trackedWifi
                } else {
                    mobileByUid[uid] = trackedMobile
                    wifiByUid[uid] = trackedWifi + remainingBootBytes
                }
            }
        }

        val perAppList = uidToRule.entries.map { (uid, rule) ->
            AppDataConsumption(
                packageName = rule.packageName,
                appName = rule.appName,
                uid = uid,
                mobileBytes = mobileByUid[uid] ?: 0L,
                wifiBytes = wifiByUid[uid] ?: 0L,
                liveBytesPerSec = liveRateByUid[uid] ?: 0L,
                isBlocked = rule.isBlocked,
                iconDrawable = rule.iconDrawable
            )
        }.sortedWith(
            compareByDescending<AppDataConsumption> { it.liveBytesPerSec }
                .thenByDescending { it.totalBytes }
                .thenBy { it.appName.lowercase() }
        )

        val timelinePoints = if (period == UsagePeriod.REALTIME || !hasAccess || networkStatsManager == null) {
            liveTimeline.toList()
        } else {
            queryHistoricalTimeline(period, now)
        }

        val totalMobile = perAppList.sumOf { it.mobileBytes }
        val totalWifi = perAppList.sumOf { it.wifiBytes }
        val totalLiveRate = perAppList.sumOf { it.liveBytesPerSec }

        DataUsageSnapshot(
            period = period,
            hasUsageAccess = hasAccess,
            perAppConsumption = perAppList,
            timelinePoints = timelinePoints,
            totalMobileBytes = totalMobile,
            totalWifiBytes = totalWifi,
            totalLiveBytesPerSec = totalLiveRate,
            lastUpdatedMs = now
        )
    }

    private fun querySummaryIntoMap(
        networkType: Int,
        startTime: Long,
        endTime: Long,
        target: MutableMap<Int, Long>
    ) {
        val manager = networkStatsManager ?: return
        try {
            val stats = manager.querySummary(networkType, null, startTime, endTime)
            val bucket = NetworkStats.Bucket()
            while (stats.hasNextBucket()) {
                stats.getNextBucket(bucket)
                val uid = bucket.uid
                if (uid > 0) {
                    val bytes = (bucket.rxBytes + bucket.txBytes).coerceAtLeast(0L)
                    target[uid] = (target[uid] ?: 0L) + bytes
                }
            }
            stats.close()
        } catch (_: Exception) {
        }
    }

    private fun queryHistoricalTimeline(period: UsagePeriod, endTime: Long): List<UsageTimelinePoint> {
        val bucketCount = when (period) {
            UsagePeriod.REALTIME -> 8
            UsagePeriod.DAY_24H -> 8
            UsagePeriod.WEEK_7D -> 7
        }
        val sliceMs = period.durationMs / bucketCount
        val startTime = endTime - period.durationMs
        val formatter = when (period) {
            UsagePeriod.WEEK_7D -> SimpleDateFormat("EEE", Locale.getDefault())
            else -> SimpleDateFormat("HH:mm", Locale.getDefault())
        }

        val points = mutableListOf<UsageTimelinePoint>()
        for (i in 0 until bucketCount) {
            val sliceStart = startTime + i * sliceMs
            val sliceEnd = if (i == bucketCount - 1) endTime else sliceStart + sliceMs
            @Suppress("DEPRECATION")
            val mobile = sumNetworkBucket(ConnectivityManager.TYPE_MOBILE, sliceStart, sliceEnd)
            @Suppress("DEPRECATION")
            val wifi = sumNetworkBucket(ConnectivityManager.TYPE_WIFI, sliceStart, sliceEnd)
            points.add(
                UsageTimelinePoint(
                    timestampMs = sliceEnd,
                    label = formatter.format(Date(sliceEnd)),
                    mobileBytes = mobile,
                    wifiBytes = wifi,
                    blockedBytes = 0L
                )
            )
        }
        return points
    }

    private fun sumNetworkBucket(networkType: Int, startTime: Long, endTime: Long): Long {
        val manager = networkStatsManager ?: return 0L
        return try {
            val bucket = manager.querySummaryForDevice(networkType, null, startTime, endTime)
            (bucket.rxBytes + bucket.txBytes).coerceAtLeast(0L)
        } catch (_: Exception) {
            0L
        }
    }
}
