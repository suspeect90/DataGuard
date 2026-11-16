package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.CellTower
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.AppFirewallRule
import com.example.data.AppListFilter
import com.example.data.NetworkTransportStatus
import com.example.ui.DataGuardUiState
import com.example.ui.components.AppIconThumbnail
import com.example.ui.theme.LocalDataGuardColors

@Composable
fun AppFirewallScreen(
    uiState: DataGuardUiState,
    onSearchQueryChange: (String) -> Unit,
    onFilterSelect: (AppListFilter) -> Unit,
    onToggleHideSystemApps: (Boolean) -> Unit,
    onToggleCellularOnly: (Boolean) -> Unit,
    onToggleMobileDataAllowed: (String, Boolean) -> Unit,
    onSetAllInstalledAllowed: (Boolean) -> Unit,
    onBlockAllSocial: () -> Unit,
    onRefreshApps: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dgColors = LocalDataGuardColors.current

    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 680.dp)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            MobileDataControlHeader(
                installedTotal = uiState.installedTotalCount,
                installedAllowed = uiState.installedAllowedCount,
                installedBlocked = uiState.installedBlockedCount,
                cellularOnly = uiState.cellularOnlyFirewall,
                networkStatus = uiState.networkSnapshot.status,
                isVpnActive = uiState.vpnState.isActive,
                onToggleCellularOnly = onToggleCellularOnly,
                onRefreshApps = onRefreshApps
            )

            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = onSearchQueryChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("app_search_input"),
                placeholder = {
                    Text(
                        text = "Rechercher une application installée...",
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Rounded.Search,
                        contentDescription = null
                    )
                },
                trailingIcon = {
                    if (uiState.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchQueryChange("") }) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = stringResource(R.string.cd_clear_search)
                            )
                        }
                    }
                },
                singleLine = true,
                shape = MaterialTheme.shapes.medium
            )

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(AppListFilter.entries) { filter ->
                    val countLabel = when (filter) {
                        AppListFilter.INSTALLED -> "${uiState.installedTotalCount}"
                        AppListFilter.BLOCKED -> "${uiState.blockedPackagesCount}"
                        AppListFilter.ALL -> "${uiState.allAppRules.size}"
                    }
                    FilterChip(
                        selected = uiState.selectedFilter == filter,
                        onClick = { onFilterSelect(filter) },
                        label = { Text("${filter.label} ($countLabel)") },
                        modifier = Modifier.testTag("filter_chip_${filter.name.lowercase()}"),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = dgColors.wifiAccent.copy(alpha = 0.20f),
                            selectedLabelColor = dgColors.wifiAccent
                        )
                    )
                }

                item {
                    FilterChip(
                        selected = uiState.hideSystemApps,
                        onClick = { onToggleHideSystemApps(!uiState.hideSystemApps) },
                        label = { Text("Masquer système") },
                        modifier = Modifier.testTag("filter_chip_hide_system"),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = dgColors.shieldActive.copy(alpha = 0.18f),
                            selectedLabelColor = dgColors.shieldActive
                        )
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { onSetAllInstalledAllowed(true) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("allow_all_installed_button"),
                    shape = MaterialTheme.shapes.small,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CheckCircle,
                        contentDescription = null,
                        tint = dgColors.shieldActive,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.size(4.dp))
                    Text(
                        text = "Tout autoriser",
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                OutlinedButton(
                    onClick = { onSetAllInstalledAllowed(false) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("block_all_installed_button"),
                    shape = MaterialTheme.shapes.small,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Block,
                        contentDescription = null,
                        tint = dgColors.blockedCrimson,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.size(4.dp))
                    Text(
                        text = "Tout couper",
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                OutlinedButton(
                    onClick = onBlockAllSocial,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("block_all_social_button"),
                    shape = MaterialTheme.shapes.small,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Shield,
                        contentDescription = null,
                        tint = dgColors.wifiAccent,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.size(4.dp))
                    Text(
                        text = "Réseaux",
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        if (uiState.isLoadingApps) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator(color = dgColors.shieldActive)
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Détection des applications installées...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else if (uiState.filteredAppRules.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Apps,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(44.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Aucune application trouvée.",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Modifiez la recherche ou le filtre pour afficher d'autres applications.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(uiState.filteredAppRules, key = { it.packageName }) { rule ->
                    MobileDataAppCard(
                        rule = rule,
                        cellularOnlyMode = uiState.cellularOnlyFirewall,
                        onToggleMobileData = { allowed ->
                            onToggleMobileDataAllowed(rule.packageName, allowed)
                        },
                        modifier = Modifier.widthIn(max = 680.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun MobileDataControlHeader(
    installedTotal: Int,
    installedAllowed: Int,
    installedBlocked: Int,
    cellularOnly: Boolean,
    networkStatus: NetworkTransportStatus,
    isVpnActive: Boolean,
    onToggleCellularOnly: (Boolean) -> Unit,
    onRefreshApps: () -> Unit
) {
    val dgColors = LocalDataGuardColors.current

    Surface(
        modifier = Modifier.fillMaxWidth(),
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
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CellTower,
                        contentDescription = null,
                        tint = dgColors.wifiAccent,
                        modifier = Modifier.size(20.dp)
                    )
                    Column {
                        Text(
                            text = "Accès Données Mobiles par App",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "$installedTotal installées • $installedAllowed autorisées • $installedBlocked bloquées",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                IconButton(
                    onClick = onRefreshApps,
                    modifier = Modifier.testTag("refresh_apps_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Refresh,
                        contentDescription = "Actualiser les applications installées",
                        tint = dgColors.wifiAccent
                    )
                }
            }

            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = !cellularOnly,
                    onClick = { onToggleCellularOnly(false) },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                    modifier = Modifier.testTag("scope_all_networks_button")
                ) {
                    Text(
                        text = "Tout réseau",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                SegmentedButton(
                    selected = cellularOnly,
                    onClick = { onToggleCellularOnly(true) },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                    modifier = Modifier.testTag("scope_cellular_only_button")
                ) {
                    Text(
                        text = "4G/5G uniquement",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            val scopeHelpText = when {
                !isVpnActive -> "Activez la protection VPN dans l'onglet Bouclier pour appliquer ces blocages."
                cellularOnly && networkStatus != NetworkTransportStatus.CELLULAR ->
                    "Mode 4G/5G actif : les applis coupées restent accessibles en ${networkStatus.label}."
                cellularOnly ->
                    "Connexion 4G/5G détectée : les données mobiles des applis désactivées sont bloquées."
                else ->
                    "Mode permanent : tout accès réseau des applis désactivées est bloqué par le VPN."
            }
            Text(
                text = scopeHelpText,
                style = MaterialTheme.typography.labelSmall,
                color = if (cellularOnly && networkStatus == NetworkTransportStatus.CELLULAR) {
                    dgColors.cellularWarning
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }
    }
}

@Composable
private fun MobileDataAppCard(
    rule: AppFirewallRule,
    cellularOnlyMode: Boolean,
    onToggleMobileData: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val dgColors = LocalDataGuardColors.current
    val haptics = LocalHapticFeedback.current
    val isAllowed = rule.isMobileDataAllowed
    val statusColor = if (isAllowed) dgColors.shieldActive else dgColors.blockedCrimson

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .clickable {
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onToggleMobileData(!isAllowed)
            },
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            width = 1.dp,
            color = if (isAllowed) dgColors.cardBorder else dgColors.blockedCrimson.copy(alpha = 0.42f)
        )
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AppIconThumbnail(
                appName = rule.appName,
                iconDrawable = rule.iconDrawable,
                accentColor = statusColor
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = rule.appName,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (rule.isSystemApp && rule.isInstalled) {
                        Surface(
                            shape = MaterialTheme.shapes.extraSmall,
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                text = "Système",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    } else if (!rule.isInstalled) {
                        Surface(
                            shape = MaterialTheme.shapes.extraSmall,
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                text = "Non installée",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Text(
                    text = buildString {
                        append(rule.packageName)
                        if (!rule.versionName.isNullOrBlank()) {
                            append(" • v${rule.versionName}")
                        }
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = when {
                        isAllowed -> "Données mobiles : Autorisées"
                        cellularOnlyMode -> "Données mobiles : Coupées (4G/5G)"
                        else -> "Données mobiles : Bloquées"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = statusColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Switch(
                    checked = isAllowed,
                    onCheckedChange = { allowed ->
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onToggleMobileData(allowed)
                    },
                    modifier = Modifier.testTag("app_switch_${rule.packageName}"),
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
                    color = statusColor
                )
            }
        }
    }
}
