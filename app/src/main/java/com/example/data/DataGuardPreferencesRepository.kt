package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.vpn.FirewallStateHolder
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataGuardDataStore: DataStore<Preferences> by preferencesDataStore(name = "dataguard_prefs")

data class DataGuardSettings(
    val blockedPackages: Set<String> = FirewallStateHolder.DEFAULT_BLOCKED_PACKAGES,
    val cellularOnlyFirewall: Boolean = false,
    val wifiOnlyDownloads: Boolean = true,
    val darkThemeEnabled: Boolean = true,
    val quotaLimitMb: Int = 1024,
    val totalSavedBytesAllTime: Long = 0L,
    val packMetadataEntries: Set<String> = emptySet(),
    val guideBannerDismissed: Boolean = false
)

class DataGuardPreferencesRepository(private val context: Context) {

    private object Keys {
        val BLOCKED_PACKAGES = stringSetPreferencesKey("blocked_packages")
        val CELLULAR_ONLY_FIREWALL = booleanPreferencesKey("cellular_only_firewall")
        val WIFI_ONLY_DOWNLOADS = booleanPreferencesKey("wifi_only_downloads")
        val DARK_THEME_ENABLED = booleanPreferencesKey("dark_theme_enabled")
        val QUOTA_LIMIT_MB = intPreferencesKey("quota_limit_mb")
        val TOTAL_SAVED_BYTES = longPreferencesKey("total_saved_bytes")
        val PACK_METADATA = stringSetPreferencesKey("pack_metadata")
        val GUIDE_BANNER_DISMISSED = booleanPreferencesKey("guide_banner_dismissed")
    }

    val settingsFlow: Flow<DataGuardSettings> = context.dataGuardDataStore.data.map { prefs ->
        DataGuardSettings(
            blockedPackages = prefs[Keys.BLOCKED_PACKAGES] ?: FirewallStateHolder.DEFAULT_BLOCKED_PACKAGES,
            cellularOnlyFirewall = prefs[Keys.CELLULAR_ONLY_FIREWALL] ?: false,
            wifiOnlyDownloads = prefs[Keys.WIFI_ONLY_DOWNLOADS] ?: true,
            darkThemeEnabled = prefs[Keys.DARK_THEME_ENABLED] ?: true,
            quotaLimitMb = prefs[Keys.QUOTA_LIMIT_MB] ?: 1024,
            totalSavedBytesAllTime = prefs[Keys.TOTAL_SAVED_BYTES] ?: 0L,
            packMetadataEntries = prefs[Keys.PACK_METADATA] ?: emptySet(),
            guideBannerDismissed = prefs[Keys.GUIDE_BANNER_DISMISSED] ?: false
        )
    }

    suspend fun setPackageBlocked(packageName: String, blocked: Boolean) {
        context.dataGuardDataStore.edit { prefs ->
            val current = (prefs[Keys.BLOCKED_PACKAGES] ?: FirewallStateHolder.DEFAULT_BLOCKED_PACKAGES).toMutableSet()
            if (blocked) current.add(packageName) else current.remove(packageName)
            prefs[Keys.BLOCKED_PACKAGES] = current
        }
    }

    suspend fun setBlockedPackages(packages: Set<String>) {
        context.dataGuardDataStore.edit { prefs ->
            prefs[Keys.BLOCKED_PACKAGES] = packages
        }
    }

    suspend fun setCellularOnlyFirewall(enabled: Boolean) {
        context.dataGuardDataStore.edit { prefs ->
            prefs[Keys.CELLULAR_ONLY_FIREWALL] = enabled
        }
    }

    suspend fun setWifiOnlyDownloads(enabled: Boolean) {
        context.dataGuardDataStore.edit { prefs ->
            prefs[Keys.WIFI_ONLY_DOWNLOADS] = enabled
        }
    }

    suspend fun setDarkThemeEnabled(enabled: Boolean) {
        context.dataGuardDataStore.edit { prefs ->
            prefs[Keys.DARK_THEME_ENABLED] = enabled
        }
    }

    suspend fun setGuideBannerDismissed(dismissed: Boolean) {
        context.dataGuardDataStore.edit { prefs ->
            prefs[Keys.GUIDE_BANNER_DISMISSED] = dismissed
        }
    }

    suspend fun addSavedBytes(bytes: Long) {
        if (bytes <= 0L) return
        context.dataGuardDataStore.edit { prefs ->
            val prev = prefs[Keys.TOTAL_SAVED_BYTES] ?: 0L
            prefs[Keys.TOTAL_SAVED_BYTES] = prev + bytes
        }
    }

    suspend fun recordPackMetadata(fileName: String, title: String, category: String) {
        val sanitizedTitle = title.replace("|", " ")
        val sanitizedCat = category.replace("|", " ")
        val entry = "$fileName|$sanitizedTitle|$sanitizedCat"
        context.dataGuardDataStore.edit { prefs ->
            val current = (prefs[Keys.PACK_METADATA] ?: emptySet()).filterNot { it.startsWith("$fileName|") }.toMutableSet()
            current.add(entry)
            prefs[Keys.PACK_METADATA] = current
        }
    }

    suspend fun removePackMetadata(fileName: String) {
        context.dataGuardDataStore.edit { prefs ->
            val current = (prefs[Keys.PACK_METADATA] ?: emptySet()).filterNot { it.startsWith("$fileName|") }.toSet()
            prefs[Keys.PACK_METADATA] = current
        }
    }

    suspend fun clearAllPackMetadata() {
        context.dataGuardDataStore.edit { prefs ->
            prefs[Keys.PACK_METADATA] = emptySet()
        }
    }
}
