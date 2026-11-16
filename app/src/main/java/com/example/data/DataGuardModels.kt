package com.example.data

import android.graphics.drawable.Drawable

enum class NetworkTransportStatus(val label: String) {
    WIFI_FREE("Wi-Fi Gratuit"),
    WIFI_METERED("Wi-Fi Limité"),
    CELLULAR("Données Mobiles (4G/5G)"),
    OFFLINE("Hors-Ligne")
}

data class NetworkSnapshot(
    val status: NetworkTransportStatus = NetworkTransportStatus.OFFLINE,
    val downstreamKbps: Int = 0
) {
    val isWifi: Boolean
        get() = status == NetworkTransportStatus.WIFI_FREE || status == NetworkTransportStatus.WIFI_METERED

    val isUnmeteredWifi: Boolean
        get() = status == NetworkTransportStatus.WIFI_FREE
}

enum class AppCategory(val label: String) {
    SOCIAL("Réseaux sociaux"),
    VIDEO("Vidéo & Streaming"),
    OTHER("Autres applications")
}

data class AppFirewallRule(
    val packageName: String,
    val appName: String,
    val category: AppCategory,
    val isInstalled: Boolean,
    val isBlocked: Boolean,
    val isPreset: Boolean,
    val description: String,
    val isSystemApp: Boolean = false,
    val versionName: String? = null,
    val iconDrawable: Drawable? = null
) {
    val isMobileDataAllowed: Boolean
        get() = !isBlocked
}

enum class AppListFilter(val label: String) {
    INSTALLED("Installées"),
    BLOCKED("Données coupées"),
    ALL("Toutes")
}

data class FirewallPacketEvent(
    val id: Long,
    val timestampMs: Long,
    val protocol: String,
    val destinationIp: String,
    val destinationPort: Int,
    val bytesDropped: Int
)

data class OfflineCachePack(
    val id: String,
    val fileName: String,
    val title: String,
    val category: String,
    val sizeMb: Int,
    val createdAtMs: Long
)

enum class OfflinePackPreset(
    val id: String,
    val title: String,
    val subtitle: String,
    val category: String,
    val sizeMb: Int
) {
    TRENDING_VIDEOS(
        id = "pack_videos",
        title = "Pack Vidéos Tendances",
        subtitle = "Flux pré-chargé haute compression pour visionnage sans 4G",
        category = "Vidéo",
        sizeMb = 50
    ),
    TUTORIALS_BUNDLE(
        id = "pack_tutorials",
        title = "Pack Éducatif & Tutoriels",
        subtitle = "Contenus courts sauvegardés localement en Wi-Fi",
        category = "Éducation",
        sizeMb = 25
    ),
    AUDIO_PODCASTS(
        id = "pack_audio",
        title = "Pack Audio & Podcasts",
        subtitle = "Épisodes compressés à faible empreinte mémoire",
        category = "Audio",
        sizeMb = 15
    )
}
