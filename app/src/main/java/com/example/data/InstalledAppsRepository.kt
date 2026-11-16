package com.example.data

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class InstalledAppsRepository(private val context: Context) {

    data class PresetDefinition(
        val packageName: String,
        val appName: String,
        val category: AppCategory,
        val description: String
    )

    private data class InstalledAppMetadata(
        val label: String,
        val icon: Drawable?,
        val isSystemApp: Boolean,
        val versionName: String?
    )

    companion object {
        val SOCIAL_AND_VIDEO_PRESETS = listOf(
            PresetDefinition(
                packageName = "com.zhiliaoapp.musically",
                appName = "TikTok",
                category = AppCategory.SOCIAL,
                description = "Flux vidéo court très consommateur en 4G/5G"
            ),
            PresetDefinition(
                packageName = "com.ss.android.ugc.trill",
                appName = "TikTok (Asie / Afrique)",
                category = AppCategory.SOCIAL,
                description = "Édition régionale TikTok (pré-chargement vidéo)"
            ),
            PresetDefinition(
                packageName = "com.instagram.android",
                appName = "Instagram",
                category = AppCategory.SOCIAL,
                description = "Reels, Stories et synchronisation média continue"
            ),
            PresetDefinition(
                packageName = "com.facebook.katana",
                appName = "Facebook",
                category = AppCategory.SOCIAL,
                description = "Fil d'actualité vidéo et services en arrière-plan"
            ),
            PresetDefinition(
                packageName = "com.google.android.youtube",
                appName = "YouTube",
                category = AppCategory.VIDEO,
                description = "Streaming HD et lecture automatique"
            ),
            PresetDefinition(
                packageName = "com.snapchat.android",
                appName = "Snapchat",
                category = AppCategory.SOCIAL,
                description = "Spotlight et pré-chargement de Snaps"
            ),
            PresetDefinition(
                packageName = "com.twitter.android",
                appName = "X (Twitter)",
                category = AppCategory.SOCIAL,
                description = "Médias intégrés et rafraîchissement automatique"
            ),
            PresetDefinition(
                packageName = "tv.twitch.android.app",
                appName = "Twitch",
                category = AppCategory.VIDEO,
                description = "Directs vidéo haute bande passante"
            )
        )

        val SOCIAL_PRESET_PACKAGES: Set<String> = SOCIAL_AND_VIDEO_PRESETS.map { it.packageName }.toSet()
    }

    suspend fun loadMergedAppRules(blockedPackages: Set<String>): List<AppFirewallRule> =
        withContext(Dispatchers.IO) {
            val pm = context.packageManager
            val launcherIntent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }

            val installedMap = linkedMapOf<String, InstalledAppMetadata>()
            val resolveList = pm.queryIntentActivities(launcherIntent, PackageManager.MATCH_ALL)
            for (resolveInfo in resolveList) {
                val appInfo = resolveInfo.activityInfo?.applicationInfo
                val pkg = resolveInfo.activityInfo?.packageName ?: continue
                if (pkg == context.packageName) continue
                if (!installedMap.containsKey(pkg)) {
                    val label = resolveInfo.loadLabel(pm)?.toString()?.takeIf { it.isNotBlank() } ?: pkg
                    val icon = try {
                        resolveInfo.loadIcon(pm)
                    } catch (_: Exception) {
                        null
                    }
                    val isSystem = appInfo != null && (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                    val version = try {
                        pm.getPackageInfo(pkg, 0)?.versionName
                    } catch (_: Exception) {
                        null
                    }
                    installedMap[pkg] = InstalledAppMetadata(
                        label = label,
                        icon = icon,
                        isSystemApp = isSystem,
                        versionName = version
                    )
                }
            }

            val rules = mutableListOf<AppFirewallRule>()
            val handledPackages = mutableSetOf<String>()

            for (preset in SOCIAL_AND_VIDEO_PRESETS) {
                val installedInfo = installedMap[preset.packageName]
                handledPackages.add(preset.packageName)
                rules.add(
                    AppFirewallRule(
                        packageName = preset.packageName,
                        appName = installedInfo?.label ?: preset.appName,
                        category = preset.category,
                        isInstalled = installedInfo != null,
                        isBlocked = blockedPackages.contains(preset.packageName),
                        isPreset = true,
                        description = preset.description,
                        isSystemApp = installedInfo?.isSystemApp ?: false,
                        versionName = installedInfo?.versionName,
                        iconDrawable = installedInfo?.icon
                    )
                )
            }

            val remainingInstalled = installedMap.entries
                .filter { !handledPackages.contains(it.key) }
                .sortedWith(
                    compareByDescending<Map.Entry<String, InstalledAppMetadata>> {
                        blockedPackages.contains(it.key)
                    }.thenBy { it.value.label.lowercase() }
                )

            for ((pkg, meta) in remainingInstalled) {
                val inferredCategory = when {
                    pkg.contains("video", ignoreCase = true) ||
                        pkg.contains("netflix", ignoreCase = true) ||
                        pkg.contains("music", ignoreCase = true) ||
                        pkg.contains("spotify", ignoreCase = true) -> AppCategory.VIDEO
                    pkg.contains("whatsapp", ignoreCase = true) ||
                        pkg.contains("telegram", ignoreCase = true) ||
                        pkg.contains("reddit", ignoreCase = true) ||
                        pkg.contains("discord", ignoreCase = true) -> AppCategory.SOCIAL
                    else -> AppCategory.OTHER
                }
                rules.add(
                    AppFirewallRule(
                        packageName = pkg,
                        appName = meta.label,
                        category = inferredCategory,
                        isInstalled = true,
                        isBlocked = blockedPackages.contains(pkg),
                        isPreset = false,
                        description = if (meta.isSystemApp) "Application système installée" else "Application utilisateur installée",
                        isSystemApp = meta.isSystemApp,
                        versionName = meta.versionName,
                        iconDrawable = meta.icon
                    )
                )
            }

            rules
        }
}
