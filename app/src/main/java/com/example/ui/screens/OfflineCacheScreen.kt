package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.FolderZip
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.WifiLock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.OfflineCachePack
import com.example.data.OfflinePackPreset
import com.example.ui.DataGuardUiState
import com.example.ui.components.StorageQuotaRing
import com.example.ui.theme.LocalDataGuardColors
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun OfflineCacheScreen(
    uiState: DataGuardUiState,
    onToggleWifiOnly: (Boolean) -> Unit,
    onDownloadPack: (OfflinePackPreset) -> Unit,
    onDeletePack: (OfflineCachePack) -> Unit,
    onClearAllCache: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dgColors = LocalDataGuardColors.current
    val isDownloadLockedByCellular = uiState.wifiOnlyDownloads && !uiState.networkSnapshot.isWifi

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Surface(
                modifier = Modifier
                    .widthIn(max = 680.dp)
                    .fillMaxWidth(),
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(
                    1.2.dp,
                    Brush.linearGradient(
                        colors = listOf(
                            dgColors.wifiAccent.copy(alpha = 0.55f),
                            dgColors.cardBorder
                        )
                    )
                )
            ) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    Image(
                        painter = painterResource(id = R.drawable.img_vault_banner),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .matchParentSize()
                            .alpha(0.32f)
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.surface.copy(alpha = 0.72f),
                                        MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)
                                    )
                                )
                            )
                    )
                    Row(
                        modifier = Modifier.padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(18.dp)
                    ) {
                        StorageQuotaRing(
                            usedMb = uiState.usedCacheMb,
                            quotaMb = uiState.quotaLimitMb
                        )

                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "Coffre Hors-Ligne",
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${uiState.usedCacheMb} Mo utilisés sur ${uiState.quotaLimitMb} Mo réservés",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            val remainingMb = (uiState.quotaLimitMb - uiState.usedCacheMb).coerceAtLeast(0)
                            Text(
                                text = "$remainingMb Mo disponibles localement",
                                style = MaterialTheme.typography.labelMedium,
                                color = dgColors.wifiAccent
                            )
                        }
                    }
                }
            }
        }

        item {
            Surface(
                modifier = Modifier
                    .widthIn(max = 680.dp)
                    .fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                color = if (isDownloadLockedByCellular) {
                    dgColors.cellularWarning.copy(alpha = 0.12f)
                } else {
                    MaterialTheme.colorScheme.surface
                },
                border = BorderStroke(
                    1.dp,
                    if (isDownloadLockedByCellular) dgColors.cellularWarning.copy(alpha = 0.45f) else dgColors.cardBorder
                )
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = if (isDownloadLockedByCellular) Icons.Rounded.Lock else Icons.Rounded.WifiLock,
                        contentDescription = null,
                        tint = if (isDownloadLockedByCellular) dgColors.cellularWarning else dgColors.wifiAccent,
                        modifier = Modifier.size(24.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Protection Wi-Fi Uniquement",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (isDownloadLockedByCellular) {
                                "Réseau actuel : ${uiState.networkSnapshot.status.label}. Pré-chargement bloqué."
                            } else {
                                "Bloque automatiquement les téléchargements hors-ligne sur 4G/5G."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = uiState.wifiOnlyDownloads,
                        onCheckedChange = onToggleWifiOnly,
                        modifier = Modifier.testTag("wifi_only_switch")
                    )
                }
            }
        }

        item {
            AnimatedVisibility(visible = uiState.downloadingPreset != null) {
                val activePreset = uiState.downloadingPreset
                if (activePreset != null) {
                    Surface(
                        modifier = Modifier
                            .widthIn(max = 680.dp)
                            .fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium,
                        color = dgColors.wifiAccent.copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, dgColors.wifiAccent.copy(alpha = 0.4f))
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Sauvegarde locale : ${activePreset.title}",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${(uiState.downloadProgress * 100).toInt()}%",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = dgColors.wifiAccent
                                )
                            }
                            LinearProgressIndicator(
                                progress = { uiState.downloadProgress },
                                modifier = Modifier.fillMaxWidth(),
                                color = dgColors.wifiAccent
                            )
                        }
                    }
                }
            }
        }

        item {
            Text(
                text = "Pré-charger un pack local",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier
                    .widthIn(max = 680.dp)
                    .fillMaxWidth()
            )
        }

        items(OfflinePackPreset.entries, key = { it.id }) { preset ->
            PresetDownloadCard(
                preset = preset,
                isBusy = uiState.downloadingPreset != null,
                isLockedByWifiGuard = isDownloadLockedByCellular,
                onDownload = { onDownloadPack(preset) },
                modifier = Modifier.widthIn(max = 680.dp)
            )
        }

        item {
            Row(
                modifier = Modifier
                    .widthIn(max = 680.dp)
                    .fillMaxWidth()
                    .padding(top = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Fichiers en cache (${uiState.cachedPacks.size})",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
                if (uiState.cachedPacks.isNotEmpty()) {
                    TextButton(
                        onClick = onClearAllCache,
                        modifier = Modifier.testTag("clear_all_cache_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.DeleteSweep,
                            contentDescription = null,
                            tint = dgColors.blockedCrimson,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Tout vider",
                            color = dgColors.blockedCrimson
                        )
                    }
                }
            }
        }

        if (uiState.cachedPacks.isEmpty()) {
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
                        modifier = Modifier.padding(22.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.FolderZip,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(32.dp)
                        )
                        Text(
                            text = "Aucun pack sauvegardé en local",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Connectez-vous au Wi-Fi pour pré-charger du contenu dans votre quota de 1 Go.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(uiState.cachedPacks, key = { it.id }) { pack ->
                CachedPackRow(
                    pack = pack,
                    onDelete = { onDeletePack(pack) },
                    modifier = Modifier.widthIn(max = 680.dp)
                )
            }
        }

        item { Spacer(modifier = Modifier.height(12.dp)) }
    }
}

@Composable
private fun PresetDownloadCard(
    preset: OfflinePackPreset,
    isBusy: Boolean,
    isLockedByWifiGuard: Boolean,
    onDownload: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dgColors = LocalDataGuardColors.current
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, dgColors.cardBorder)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = preset.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = preset.subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Surface(
                    shape = MaterialTheme.shapes.extraSmall,
                    color = dgColors.wifiAccent.copy(alpha = 0.14f)
                ) {
                    Text(
                        text = "${preset.sizeMb} Mo",
                        style = MaterialTheme.typography.labelMedium,
                        color = dgColors.wifiAccent,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Button(
                onClick = onDownload,
                enabled = !isBusy,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("download_${preset.id}_button"),
                shape = MaterialTheme.shapes.small,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isLockedByWifiGuard) {
                        MaterialTheme.colorScheme.surfaceVariant
                    } else {
                        dgColors.wifiAccent
                    },
                    contentColor = if (isLockedByWifiGuard) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        Color(0xFF031926)
                    }
                )
            ) {
                Icon(
                    imageVector = if (isLockedByWifiGuard) Icons.Rounded.Lock else Icons.Rounded.Download,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isLockedByWifiGuard) {
                        "WI-FI REQUIS POUR PRÉ-CHARGER"
                    } else {
                        "PRÉ-CHARGER (${preset.sizeMb} MO)"
                    },
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun CachedPackRow(
    pack: OfflineCachePack,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dgColors = LocalDataGuardColors.current
    val dateFormat = remember { SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()) }
    val dateStr = remember(pack.createdAtMs) { dateFormat.format(Date(pack.createdAtMs)) }

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
            Surface(
                shape = MaterialTheme.shapes.small,
                color = dgColors.shieldActive.copy(alpha = 0.14f)
            ) {
                Icon(
                    imageVector = Icons.Rounded.FolderZip,
                    contentDescription = null,
                    tint = dgColors.shieldActive,
                    modifier = Modifier
                        .padding(10.dp)
                        .size(20.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = pack.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${pack.category} • ${pack.sizeMb} Mo • $dateStr",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(
                onClick = onDelete,
                modifier = Modifier.testTag("delete_pack_${pack.fileName}")
            ) {
                Icon(
                    imageVector = Icons.Rounded.Delete,
                    contentDescription = stringResource(R.string.cd_delete_pack),
                    tint = dgColors.blockedCrimson
                )
            }
        }
    }
}
