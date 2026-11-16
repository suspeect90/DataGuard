package com.example.ui

import android.app.Application
import android.content.Intent
import android.net.VpnService
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppFirewallRule
import com.example.data.AppListFilter
import com.example.data.DataGuardPreferencesRepository
import com.example.data.DataGuardSettings
import com.example.data.DataUsageRepository
import com.example.data.DataUsageSnapshot
import com.example.data.InstalledAppsRepository
import com.example.data.NetworkMonitor
import com.example.data.NetworkSnapshot
import com.example.data.NetworkTransportStatus
import com.example.data.OfflineCachePack
import com.example.data.OfflineCacheRepository
import com.example.data.OfflinePackPreset
import com.example.data.UsageChartMode
import com.example.data.UsagePeriod
import com.example.vpn.DataGuardVpnService
import com.example.vpn.FirewallStateHolder
import com.example.vpn.VpnRuntimeState
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DataGuardUiState(
    val vpnState: VpnRuntimeState = VpnRuntimeState(),
    val networkSnapshot: NetworkSnapshot = NetworkSnapshot(),
    val allAppRules: List<AppFirewallRule> = emptyList(),
    val filteredAppRules: List<AppFirewallRule> = emptyList(),
    val installedTotalCount: Int = 0,
    val installedAllowedCount: Int = 0,
    val installedBlockedCount: Int = 0,
    val blockedPackagesCount: Int = 0,
    val searchQuery: String = "",
    val selectedFilter: AppListFilter = AppListFilter.INSTALLED,
    val hideSystemApps: Boolean = false,
    val cellularOnlyFirewall: Boolean = false,
    val cachedPacks: List<OfflineCachePack> = emptyList(),
    val usedCacheMb: Int = 0,
    val quotaLimitMb: Int = 1024,
    val wifiOnlyDownloads: Boolean = true,
    val darkThemeEnabled: Boolean = true,
    val totalSavedBytesAllTime: Long = 0L,
    val downloadingPreset: OfflinePackPreset? = null,
    val downloadProgress: Float = 0f,
    val isLoadingApps: Boolean = true,
    val selectedUsagePeriod: UsagePeriod = UsagePeriod.REALTIME,
    val usageChartMode: UsageChartMode = UsageChartMode.AREA_TIMELINE,
    val dataUsageSnapshot: DataUsageSnapshot = DataUsageSnapshot(),
    val guideBannerDismissed: Boolean = false
)

sealed interface DataGuardEffect {
    data class RequestVpnConsent(val intent: Intent) : DataGuardEffect
    data class ShowMessage(val message: String, val isError: Boolean = false) : DataGuardEffect
}

class DataGuardViewModel(application: Application) : AndroidViewModel(application) {

    private val prefsRepo = DataGuardPreferencesRepository(application)
    private val networkMonitor = NetworkMonitor(application)
    private val appsRepo = InstalledAppsRepository(application)
    private val cacheRepo = OfflineCacheRepository(application, prefsRepo)
    private val usageRepo = DataUsageRepository(application)

    private val _effects = Channel<DataGuardEffect>(Channel.BUFFERED)
    val effects: Flow<DataGuardEffect> = _effects.receiveAsFlow()

    private val _searchQuery = MutableStateFlow("")
    private val _selectedFilter = MutableStateFlow(AppListFilter.INSTALLED)
    private val _hideSystemApps = MutableStateFlow(false)
    private val _appRules = MutableStateFlow<List<AppFirewallRule>>(emptyList())
    private val _isLoadingApps = MutableStateFlow(true)
    private val _cachedPacks = MutableStateFlow<List<OfflineCachePack>>(emptyList())
    private val _usedCacheMb = MutableStateFlow(0)
    private val _downloadingPreset = MutableStateFlow<OfflinePackPreset?>(null)
    private val _downloadProgress = MutableStateFlow(0f)
    private val _selectedUsagePeriod = MutableStateFlow(UsagePeriod.REALTIME)
    private val _usageChartMode = MutableStateFlow(UsageChartMode.AREA_TIMELINE)
    private val _dataUsageSnapshot = MutableStateFlow(DataUsageSnapshot())

    private var lastAccountedSessionBytes = 0L
    private var lastPushedVpnTargets: Set<String>? = null

    private val settingsFlow: StateFlow<DataGuardSettings> = prefsRepo.settingsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = DataGuardSettings()
        )

    private val networkFlow: StateFlow<NetworkSnapshot> = networkMonitor.observeNetwork()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = NetworkSnapshot()
        )

    private val usageUiStateFlow = combine(
        _selectedUsagePeriod,
        _usageChartMode,
        _dataUsageSnapshot
    ) { period, mode, snapshot ->
        Triple(period, mode, snapshot)
    }

    val uiState: StateFlow<DataGuardUiState> = combine(
        FirewallStateHolder.state,
        networkFlow,
        settingsFlow,
        _appRules,
        _searchQuery,
        _selectedFilter,
        _hideSystemApps,
        _cachedPacks,
        _usedCacheMb,
        _downloadingPreset,
        _downloadProgress,
        _isLoadingApps,
        usageUiStateFlow
    ) { values ->
        val vpnState = values[0] as VpnRuntimeState
        val network = values[1] as NetworkSnapshot
        val settings = values[2] as DataGuardSettings
        @Suppress("UNCHECKED_CAST")
        val allRules = values[3] as List<AppFirewallRule>
        val query = values[4] as String
        val filter = values[5] as AppListFilter
        val hideSystem = values[6] as Boolean
        @Suppress("UNCHECKED_CAST")
        val packs = values[7] as List<OfflineCachePack>
        val usedMb = values[8] as Int
        val downloading = values[9] as OfflinePackPreset?
        val progress = values[10] as Float
        val loadingApps = values[11] as Boolean
        @Suppress("UNCHECKED_CAST")
        val usageTuple = values[12] as Triple<UsagePeriod, UsageChartMode, DataUsageSnapshot>

        val filtered = allRules.filter { rule ->
            val matchesFilter = when (filter) {
                AppListFilter.INSTALLED -> rule.isInstalled
                AppListFilter.BLOCKED -> rule.isBlocked
                AppListFilter.ALL -> true
            }
            val matchesSystem = !hideSystem || !rule.isSystemApp || !rule.isInstalled
            val matchesQuery = query.isBlank() ||
                rule.appName.contains(query, ignoreCase = true) ||
                rule.packageName.contains(query, ignoreCase = true)
            matchesFilter && matchesSystem && matchesQuery
        }

        val installedRules = allRules.filter { it.isInstalled }
        val installedBlocked = installedRules.count { it.isBlocked }
        val installedAllowed = installedRules.size - installedBlocked
        val blockedCount = allRules.count { it.isBlocked }

        DataGuardUiState(
            vpnState = vpnState,
            networkSnapshot = network,
            allAppRules = allRules,
            filteredAppRules = filtered,
            installedTotalCount = installedRules.size,
            installedAllowedCount = installedAllowed,
            installedBlockedCount = installedBlocked,
            blockedPackagesCount = blockedCount,
            searchQuery = query,
            selectedFilter = filter,
            hideSystemApps = hideSystem,
            cellularOnlyFirewall = settings.cellularOnlyFirewall,
            cachedPacks = packs,
            usedCacheMb = usedMb,
            quotaLimitMb = settings.quotaLimitMb,
            wifiOnlyDownloads = settings.wifiOnlyDownloads,
            darkThemeEnabled = settings.darkThemeEnabled,
            totalSavedBytesAllTime = settings.totalSavedBytesAllTime,
            downloadingPreset = downloading,
            downloadProgress = progress,
            isLoadingApps = loadingApps,
            selectedUsagePeriod = usageTuple.first,
            usageChartMode = usageTuple.second,
            dataUsageSnapshot = usageTuple.third,
            guideBannerDismissed = settings.guideBannerDismissed
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = DataGuardUiState()
    )

    init {
        viewModelScope.launch {
            settingsFlow
                .map { it.blockedPackages }
                .distinctUntilChanged()
                .collect { blocked ->
                    _appRules.value = appsRepo.loadMergedAppRules(blocked)
                    _isLoadingApps.value = false
                }
        }

        viewModelScope.launch {
            combine(
                settingsFlow.map { it.blockedPackages to it.cellularOnlyFirewall }.distinctUntilChanged(),
                networkFlow.map { it.status }.distinctUntilChanged()
            ) { (blocked, cellularOnly), networkStatus ->
                computeEffectiveTargets(blocked, cellularOnly, networkStatus)
            }.distinctUntilChanged().collect { effectiveTargets ->
                FirewallStateHolder.setTargetPackages(effectiveTargets)
                val previous = lastPushedVpnTargets
                lastPushedVpnTargets = effectiveTargets
                if (previous != null && FirewallStateHolder.state.value.isActive) {
                    val ctx = getApplication<Application>()
                    val intent = Intent(ctx, DataGuardVpnService::class.java).apply {
                        action = DataGuardVpnService.ACTION_UPDATE_RULES
                    }
                    ContextCompat.startForegroundService(ctx, intent)
                }
            }
        }

        viewModelScope.launch {
            settingsFlow
                .map { it.packMetadataEntries }
                .distinctUntilChanged()
                .collect { metadata ->
                    refreshOfflinePacks(metadata)
                }
        }

        viewModelScope.launch {
            FirewallStateHolder.state.collect { vpn ->
                if (!vpn.isActive) {
                    lastAccountedSessionBytes = 0L
                    vpn.lastError?.let { errorMsg ->
                        _effects.send(DataGuardEffect.ShowMessage(errorMsg, isError = true))
                    }
                } else {
                    val delta = vpn.sessionBytesBlocked - lastAccountedSessionBytes
                    if (delta >= 1024L) {
                        lastAccountedSessionBytes = vpn.sessionBytesBlocked
                        prefsRepo.addSavedBytes(delta)
                    }
                }
            }
        }

        viewModelScope.launch {
            while (true) {
                sampleUsageOnce()
                delay(2_500L)
            }
        }
    }

    private suspend fun sampleUsageOnce() {
        val rules = _appRules.value
        if (rules.isEmpty()) return
        _dataUsageSnapshot.value = usageRepo.sampleAndQueryUsage(
            period = _selectedUsagePeriod.value,
            rules = rules,
            networkStatus = networkFlow.value.status,
            sessionBlockedBytes = FirewallStateHolder.state.value.sessionBytesBlocked
        )
    }

    fun refreshUsageNow() {
        viewModelScope.launch {
            sampleUsageOnce()
        }
    }

    fun selectUsagePeriod(period: UsagePeriod) {
        _selectedUsagePeriod.value = period
        refreshUsageNow()
    }

    fun selectUsageChartMode(mode: UsageChartMode) {
        _usageChartMode.value = mode
    }

    fun dismissGuideBanner() {
        viewModelScope.launch {
            prefsRepo.setGuideBannerDismissed(true)
        }
    }

    private fun computeEffectiveTargets(
        blockedPackages: Set<String>,
        cellularOnly: Boolean,
        networkStatus: NetworkTransportStatus
    ): Set<String> {
        return if (!cellularOnly) {
            blockedPackages
        } else if (networkStatus == NetworkTransportStatus.CELLULAR) {
            blockedPackages
        } else {
            emptySet()
        }
    }

    private suspend fun refreshOfflinePacks(metadataEntries: Set<String> = settingsFlow.value.packMetadataEntries) {
        _cachedPacks.value = cacheRepo.listCachedPacks(metadataEntries)
        _usedCacheMb.value = cacheRepo.getUsedStorageMb()
    }

    fun refreshInstalledApps() {
        viewModelScope.launch {
            _isLoadingApps.value = true
            _appRules.value = appsRepo.loadMergedAppRules(settingsFlow.value.blockedPackages)
            _isLoadingApps.value = false
            _effects.send(DataGuardEffect.ShowMessage("Liste des applications installées actualisée."))
        }
    }

    fun onFirewallToggleRequested() {
        val isActive = FirewallStateHolder.state.value.isActive
        if (isActive) {
            stopVpnService()
        } else {
            val prepareIntent = VpnService.prepare(getApplication())
            if (prepareIntent != null) {
                viewModelScope.launch {
                    _effects.send(DataGuardEffect.RequestVpnConsent(prepareIntent))
                }
            } else {
                startVpnService()
            }
        }
    }

    fun onVpnConsentResult(granted: Boolean) {
        if (granted) {
            startVpnService()
        } else {
            viewModelScope.launch {
                _effects.send(
                    DataGuardEffect.ShowMessage(
                        "Autorisation VPN refusée par le système.",
                        isError = true
                    )
                )
            }
        }
    }

    private fun startVpnService() {
        val ctx = getApplication<Application>()
        val settings = settingsFlow.value
        val effective = computeEffectiveTargets(
            settings.blockedPackages,
            settings.cellularOnlyFirewall,
            networkFlow.value.status
        )
        FirewallStateHolder.setTargetPackages(effective)
        lastPushedVpnTargets = effective
        val intent = Intent(ctx, DataGuardVpnService::class.java).apply {
            action = DataGuardVpnService.ACTION_START
        }
        ContextCompat.startForegroundService(ctx, intent)
    }

    private fun stopVpnService() {
        val ctx = getApplication<Application>()
        val intent = Intent(ctx, DataGuardVpnService::class.java).apply {
            action = DataGuardVpnService.ACTION_STOP
        }
        ctx.startService(intent)
    }

    fun toggleMobileDataAllowed(packageName: String, allowed: Boolean) {
        toggleAppBlocked(packageName = packageName, blocked = !allowed)
    }

    fun toggleAppBlocked(packageName: String, blocked: Boolean) {
        viewModelScope.launch {
            prefsRepo.setPackageBlocked(packageName, blocked)
        }
    }

    fun setAllInstalledMobileDataAllowed(allowed: Boolean) {
        viewModelScope.launch {
            val installedPkgs = _appRules.value.filter { it.isInstalled }.map { it.packageName }.toSet()
            val current = settingsFlow.value.blockedPackages
            val updated = if (allowed) {
                current - installedPkgs
            } else {
                current + installedPkgs
            }
            prefsRepo.setBlockedPackages(updated)
            val msg = if (allowed) {
                "Données mobiles autorisées pour toutes les applis installées."
            } else {
                "Données mobiles bloquées pour toutes les applis installées (${installedPkgs.size})."
            }
            _effects.send(DataGuardEffect.ShowMessage(msg))
        }
    }

    fun setCellularOnlyFirewall(enabled: Boolean) {
        viewModelScope.launch {
            prefsRepo.setCellularOnlyFirewall(enabled)
        }
    }

    fun setHideSystemApps(hide: Boolean) {
        _hideSystemApps.value = hide
    }

    fun blockAllSocialPresets() {
        viewModelScope.launch {
            val updated = settingsFlow.value.blockedPackages + InstalledAppsRepository.SOCIAL_PRESET_PACKAGES
            prefsRepo.setBlockedPackages(updated)
            _effects.send(DataGuardEffect.ShowMessage("Réseaux sociaux coupés sur les données mobiles."))
        }
    }

    fun resetDefaultRules() {
        viewModelScope.launch {
            prefsRepo.setBlockedPackages(FirewallStateHolder.DEFAULT_BLOCKED_PACKAGES)
            _effects.send(DataGuardEffect.ShowMessage("Règles par défaut restaurées (TikTok, Instagram, Facebook)."))
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setFilter(filter: AppListFilter) {
        _selectedFilter.value = filter
    }

    fun toggleWifiOnlyDownloads(enabled: Boolean) {
        viewModelScope.launch {
            prefsRepo.setWifiOnlyDownloads(enabled)
        }
    }

    fun toggleDarkTheme(enabled: Boolean) {
        viewModelScope.launch {
            prefsRepo.setDarkThemeEnabled(enabled)
        }
    }

    fun downloadOfflinePack(preset: OfflinePackPreset) {
        if (_downloadingPreset.value != null) return

        val snapshot = networkFlow.value
        val wifiOnly = settingsFlow.value.wifiOnlyDownloads
        if (wifiOnly && !snapshot.isWifi) {
            val reason = when (snapshot.status) {
                NetworkTransportStatus.CELLULAR ->
                    "Téléchargement bloqué en 4G/5G : connectez-vous au Wi-Fi pour économiser vos données."
                else ->
                    "Aucune connexion Wi-Fi détectée. Connectez-vous au Wi-Fi pour pré-charger ce pack."
            }
            viewModelScope.launch {
                _effects.send(DataGuardEffect.ShowMessage(reason, isError = true))
            }
            return
        }

        if (snapshot.status == NetworkTransportStatus.OFFLINE) {
            viewModelScope.launch {
                _effects.send(
                    DataGuardEffect.ShowMessage(
                        "Appareil hors-ligne : impossible de pré-charger le pack.",
                        isError = true
                    )
                )
            }
            return
        }

        viewModelScope.launch {
            _downloadingPreset.value = preset
            _downloadProgress.value = 0f

            val result = cacheRepo.createOfflinePack(
                preset = preset,
                quotaLimitMb = settingsFlow.value.quotaLimitMb,
                onProgress = { progress ->
                    _downloadProgress.update { progress }
                }
            )

            _downloadingPreset.value = null
            _downloadProgress.value = 0f
            refreshOfflinePacks()

            result.onSuccess {
                _effects.send(
                    DataGuardEffect.ShowMessage("${preset.title} (${preset.sizeMb} Mo) sauvegardé en local !")
                )
            }.onFailure { err ->
                _effects.send(
                    DataGuardEffect.ShowMessage(err.message ?: "Échec de la sauvegarde locale.", isError = true)
                )
            }
        }
    }

    fun deletePack(pack: OfflineCachePack) {
        viewModelScope.launch {
            cacheRepo.deletePack(pack.fileName)
            refreshOfflinePacks()
            _effects.send(DataGuardEffect.ShowMessage("${pack.title} supprimé (${pack.sizeMb} Mo libérés)."))
        }
    }

    fun clearAllCache() {
        viewModelScope.launch {
            val count = cacheRepo.clearAllPacks()
            refreshOfflinePacks()
            val msg = if (count > 0) {
                "Cache vidé : $count pack(s) supprimé(s)."
            } else {
                "Le cache hors-ligne est déjà vide."
            }
            _effects.send(DataGuardEffect.ShowMessage(msg))
        }
    }

    fun clearPacketLogs() {
        FirewallStateHolder.clearLogs()
    }
}
