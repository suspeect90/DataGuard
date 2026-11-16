package com.example.ui.screens

import android.content.Intent
import android.provider.Settings
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Analytics
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.AppDataConsumption
import com.example.data.UsageChartMode
import com.example.data.UsagePeriod
import com.example.ui.DataGuardUiState
import com.example.ui.components.AppIconThumbnail
import com.example.ui.components.DataUsageChartCard
import com.example.ui.components.formatBytesFrench
import com.example.ui.theme.LocalDataGuardColors

@Composable
fun DataAnalyticsScreen(
    uiState: DataGuardUiState,
    onSelectPeriod: (UsagePeriod) -> Unit,
    onSelectChartMode: (UsageChartMode) -> Unit,
    modifier: Modifier = Modifier
) {
    val dgColors = LocalDataGuardColors.current
    val context = LocalContext.current
    val snapshot = uiState.dataUsageSnapshot
    val maxAppBytes = remember(snapshot.perAppConsumption) {
        snapshot.perAppConsumption.maxOfOrNull { it.totalBytes }?.coerceAtLeast(1L) ?: 1L
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        if (!snapshot.hasUsageAccess) {
            item {
                Surface(
                    modifier = Modifier
                        .widthIn(max = 680.dp)
                        .fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    color = dgColors.wifiAccent.copy(alpha = 0.10f),
                    border = BorderStroke(1.dp, dgColors.wifiAccent.copy(alpha = 0.35f))
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Analytics,
                                contentDescription = null,
                                tint = dgColors.wifiAccent,
                                modifier = Modifier.size(22.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Statistiques temps réel actives (TrafficStats)",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Accordez l'accès aux statistiques d'utilisation pour débloquer l'historique exact 24h et 7j par réseau.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Button(
                            onClick = {
                                val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                context.startActivity(intent)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("grant_usage_access_button"),
                            shape = MaterialTheme.shapes.small,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = dgColors.wifiAccent,
                                contentColor = MaterialTheme.colorScheme.onSecondary
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Settings,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Autoriser l'historique 24h / 7j",
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }
                }
            }
        }

        item {
            DataUsageChartCard(
                snapshot = snapshot,
                selectedPeriod = uiState.selectedUsagePeriod,
                chartMode = uiState.usageChartMode,
                onSelectPeriod = onSelectPeriod,
                onSelectChartMode = onSelectChartMode,
                modifier = Modifier.widthIn(max = 680.dp)
            )
        }

        item {
            Text(
                text = "Détail par application (${snapshot.perAppConsumption.size})",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier
                    .widthIn(max = 680.dp)
                    .fillMaxWidth()
            )
        }

        items(snapshot.perAppConsumption, key = { it.packageName }) { item ->
            AppConsumptionRow(
                item = item,
                maxAppBytes = maxAppBytes,
                modifier = Modifier.widthIn(max = 680.dp)
            )
        }

        item { Spacer(modifier = Modifier.height(14.dp)) }
    }
}

@Composable
private fun AppConsumptionRow(
    item: AppDataConsumption,
    maxAppBytes: Long,
    modifier: Modifier = Modifier
) {
    val dgColors = LocalDataGuardColors.current
    val ratio = (item.totalBytes.toFloat() / maxAppBytes.toFloat()).coerceIn(0f, 1f)
    val accent = if (item.isBlocked) dgColors.blockedCrimson else dgColors.wifiAccent

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, dgColors.cardBorder)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AppIconThumbnail(
                    appName = item.appName,
                    iconDrawable = item.iconDrawable,
                    accentColor = accent
                )

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = item.appName,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (item.liveBytesPerSec > 0L) {
                            Surface(
                                shape = MaterialTheme.shapes.extraSmall,
                                color = dgColors.shieldActive.copy(alpha = 0.14f)
                            ) {
                                Text(
                                    text = "${formatBytesFrench(item.liveBytesPerSec)}/s",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = dgColors.shieldActive,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                    Text(
                        text = "4G: ${formatBytesFrench(item.mobileBytes)} • Wi-Fi: ${formatBytesFrench(item.wifiBytes)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Text(
                    text = formatBytesFrench(item.totalBytes),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            LinearProgressIndicator(
                progress = { ratio },
                modifier = Modifier.fillMaxWidth(),
                color = accent,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        }
    }
}
