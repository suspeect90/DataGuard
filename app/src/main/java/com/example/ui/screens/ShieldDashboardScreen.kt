package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DataSaverOn
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Dns
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.AppFirewallRule
import com.example.data.FirewallPacketEvent
import com.example.ui.DataGuardUiState
import com.example.ui.components.AppIconThumbnail
import com.example.ui.components.FirewallRadarHero
import com.example.ui.components.MetricCard
import com.example.ui.components.TrafficPulseSparklineCard
import com.example.ui.components.formatBytesFrench
import com.example.ui.theme.LocalDataGuardColors
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ShieldDashboardScreen(
    uiState: DataGuardUiState,
    onToggleFirewall: () -> Unit,
    onToggleMobileDataAllowed: (String, Boolean) -> Unit,
    onNavigateToApps: () -> Unit,
    onNavigateToCache: () -> Unit,
    onClearLogs: () -> Unit,
    onDismissGuide: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dgColors = LocalDataGuardColors.current
    val quickRules = remember(uiState.allAppRules) {
        uiState.allAppRules.filter { it.isPreset }.take(4)
    }
    val recentEvents = remember(uiState.vpnState.recentEvents) {
        uiState.vpnState.recentEvents.take(6)
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        if (!uiState.guideBannerDismissed) {
            item {
                PurposeGuideCard(
                    onDismiss = onDismissGuide,
                    modifier = Modifier.widthIn(max = 680.dp)
                )
            }
        }

        item {
            FirewallRadarHero(
                isActive = uiState.vpnState.isActive,
                isStandbyNoInstalledTargets = uiState.vpnState.isStandbyNoInstalledTargets,
                matchedInstalledCount = uiState.vpnState.matchedInstalledCount,
                totalBlockedRulesCount = uiState.blockedPackagesCount,
                networkSnapshot = uiState.networkSnapshot,
                sessionStartMs = uiState.vpnState.sessionStartMs,
                onToggleFirewall = onToggleFirewall,
                modifier = Modifier.widthIn(max = 680.dp)
            )
        }

        item {
            Row(
                modifier = Modifier
                    .widthIn(max = 680.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MetricCard(
                    title = "Paquets bloqués",
                    value = uiState.vpnState.sessionPacketsBlocked.toString(),
                    subtitle = if (uiState.vpnState.isActive) "Session en cours" else "En attente",
                    accent = dgColors.blockedCrimson,
                    icon = Icons.Rounded.Block,
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    title = "Données sauvées",
                    value = formatBytesFrench(uiState.totalSavedBytesAllTime + uiState.vpnState.sessionBytesBlocked),
                    subtitle = "${uiState.blockedPackagesCount} règle(s) actives",
                    accent = dgColors.shieldActive,
                    icon = Icons.Rounded.DataSaverOn,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            TrafficPulseSparklineCard(
                isVpnActive = uiState.vpnState.isActive,
                recentEvents = uiState.vpnState.recentEvents,
                modifier = Modifier.widthIn(max = 680.dp)
            )
        }

        item {
            Surface(
                onClick = onNavigateToCache,
                modifier = Modifier
                    .widthIn(max = 680.dp)
                    .fillMaxWidth()
                    .testTag("dashboard_cache_summary_card"),
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, dgColors.cardBorder)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = dgColors.wifiAccent.copy(alpha = 0.14f)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Storage,
                                contentDescription = null,
                                tint = dgColors.wifiAccent,
                                modifier = Modifier
                                    .padding(10.dp)
                                    .size(22.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Stockage Hors-Ligne (1 Go)",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${uiState.usedCacheMb} Mo / ${uiState.quotaLimitMb} Mo utilisés • ${uiState.cachedPacks.size} pack(s)",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Text(
                        text = "Gérer",
                        style = MaterialTheme.typography.labelLarge,
                        color = dgColors.wifiAccent
                    )
                }
            }
        }

        item {
            Row(
                modifier = Modifier
                    .widthIn(max = 680.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Cibles prioritaires",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
                TextButton(
                    onClick = onNavigateToApps,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("see_all_apps_button")
                ) {
                    Text(
                        text = "Toutes les apps (${uiState.allAppRules.size})",
                        style = MaterialTheme.typography.labelLarge,
                        color = dgColors.wifiAccent
                    )
                }
            }
        }

        items(quickRules, key = { it.packageName }) { rule ->
            QuickAppRuleRow(
                rule = rule,
                onToggleMobileData = { allowed -> onToggleMobileDataAllowed(rule.packageName, allowed) },
                modifier = Modifier.widthIn(max = 680.dp)
            )
        }

        item {
            Row(
                modifier = Modifier
                    .widthIn(max = 680.dp)
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Dns,
                        contentDescription = null,
                        tint = dgColors.shieldActive,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Journal d'interception VPN",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
                AnimatedVisibility(visible = recentEvents.isNotEmpty()) {
                    IconButton(
                        onClick = onClearLogs,
                        modifier = Modifier.testTag("clear_logs_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.DeleteSweep,
                            contentDescription = "Effacer le journal",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        if (recentEvents.isEmpty()) {
            item {
                Surface(
                    modifier = Modifier
                        .widthIn(max = 680.dp)
                        .fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, dgColors.cardBorder)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Security,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(28.dp)
                        )
                        Text(
                            text = if (uiState.vpnState.isActive) {
                                "Tunnel à l'écoute : aucun paquet sortant bloqué pour l'instant."
                            } else {
                                "Activez le pare-feu pour inspecter et bloquer les paquets IPv4 des apps ciblées."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(recentEvents, key = { it.id }) { event ->
                PacketEventRow(
                    event = event,
                    modifier = Modifier.widthIn(max = 680.dp)
                )
            }
        }

        item { Spacer(modifier = Modifier.height(12.dp)) }
    }
}

@Composable
private fun QuickAppRuleRow(
    rule: AppFirewallRule,
    onToggleMobileData: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val dgColors = LocalDataGuardColors.current
    val isAllowed = rule.isMobileDataAllowed
    val accent = if (isAllowed) dgColors.shieldActive else dgColors.blockedCrimson

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, dgColors.cardBorder)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AppIconThumbnail(
                appName = rule.appName,
                iconDrawable = rule.iconDrawable,
                accentColor = accent
            )

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = rule.appName,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Surface(
                        shape = MaterialTheme.shapes.extraSmall,
                        color = if (rule.isInstalled) {
                            dgColors.shieldActive.copy(alpha = 0.14f)
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        }
                    ) {
                        Text(
                            text = if (rule.isInstalled) "Installée" else "Règle prête",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (rule.isInstalled) dgColors.shieldActive else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Text(
                    text = if (isAllowed) "Données mobiles : Autorisées" else "Données mobiles : Bloquées",
                    style = MaterialTheme.typography.labelSmall,
                    color = accent,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            val haptics = LocalHapticFeedback.current
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Switch(
                    checked = isAllowed,
                    onCheckedChange = { allowed ->
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onToggleMobileData(allowed)
                    },
                    modifier = Modifier.testTag("quick_switch_${rule.packageName}"),
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                        checkedTrackColor = dgColors.shieldActive,
                        uncheckedThumbColor = MaterialTheme.colorScheme.onSurface,
                        uncheckedTrackColor = dgColors.blockedCrimson.copy(alpha = 0.7f)
                    )
                )
                Text(
                    text = if (isAllowed) "4G ON" else "4G OFF",
                    style = MaterialTheme.typography.labelSmall,
                    color = accent
                )
            }
        }
    }
}

@Composable
fun PacketEventRow(
    event: FirewallPacketEvent,
    modifier: Modifier = Modifier
) {
    val dgColors = LocalDataGuardColors.current
    val timeFormat = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }
    val timeLabel = remember(event.timestampMs) { timeFormat.format(Date(event.timestampMs)) }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                Surface(
                    shape = MaterialTheme.shapes.extraSmall,
                    color = dgColors.blockedCrimson.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = event.protocol,
                        style = MaterialTheme.typography.labelSmall,
                        color = dgColors.blockedCrimson,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                    )
                }
                Column {
                    Text(
                        text = if (event.destinationPort > 0) {
                            "${event.destinationIp}:${event.destinationPort}"
                        } else {
                            event.destinationIp
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "$timeLabel • Bloqué par VpnService",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Text(
                text = "-${event.bytesDropped} o",
                style = MaterialTheme.typography.labelMedium,
                color = dgColors.blockedCrimson
            )
        }
    }
}

@Composable
private fun PurposeGuideCard(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dgColors = LocalDataGuardColors.current
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("purpose_guide_card"),
        shape = MaterialTheme.shapes.medium,
        color = dgColors.wifiAccent.copy(alpha = 0.10f),
        border = BorderStroke(1.dp, dgColors.wifiAccent.copy(alpha = 0.35f))
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Info,
                        contentDescription = null,
                        tint = dgColors.wifiAccent,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "À quoi sert DataGuard ?",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("dismiss_guide_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Masquer ce guide",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Text(
                text = "Économisez votre forfait 4G/5G sans couper vos applications utiles (WhatsApp, navigateur, appels).",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = "• Bouclier : active un pare-feu local qui bloque TikTok, Instagram ou Facebook en arrière-plan.\n" +
                    "• Applis : coupez ou autorisez la 4G/5G pour chaque application installée.\n" +
                    "• Analyse & Cache : surveillez la conso en direct et préparez des packs en Wi-Fi gratuit.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
