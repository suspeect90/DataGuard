package com.example.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class OfflineCacheRepository(
    private val context: Context,
    private val prefsRepo: DataGuardPreferencesRepository
) {
    private val cacheDir: File
        get() = File(context.filesDir, "offline_cache").apply {
            if (!exists()) mkdirs()
        }

    suspend fun listCachedPacks(metadataEntries: Set<String>): List<OfflineCachePack> =
        withContext(Dispatchers.IO) {
            val metaMap = metadataEntries.mapNotNull { entry ->
                val parts = entry.split("|")
                if (parts.size >= 3) parts[0] to (parts[1] to parts[2]) else null
            }.toMap()

            val files = cacheDir.listFiles()?.filter { it.isFile && it.name.endsWith(".dat") } ?: emptyList()
            files.map { file ->
                val meta = metaMap[file.name]
                val sizeMb = (file.length() / (1024L * 1024L)).toInt().coerceAtLeast(1)
                OfflineCachePack(
                    id = file.name,
                    fileName = file.name,
                    title = meta?.first ?: "Pack Hors-Ligne",
                    category = meta?.second ?: "Local",
                    sizeMb = sizeMb,
                    createdAtMs = file.lastModified()
                )
            }.sortedByDescending { it.createdAtMs }
        }

    suspend fun getUsedStorageMb(): Int = withContext(Dispatchers.IO) {
        val totalBytes = cacheDir.listFiles()?.sumOf { it.length() } ?: 0L
        (totalBytes / (1024L * 1024L)).toInt()
    }

    suspend fun createOfflinePack(
        preset: OfflinePackPreset,
        quotaLimitMb: Int,
        onProgress: (Float) -> Unit
    ): Result<OfflineCachePack> = withContext(Dispatchers.IO) {
        val usedMb = getUsedStorageMb()
        if (usedMb + preset.sizeMb > quotaLimitMb) {
            return@withContext Result.failure(
                IllegalStateException(
                    "Quota atteint ($usedMb Mo / $quotaLimitMb Mo). Libérez de l'espace avant de pré-charger ${preset.sizeMb} Mo."
                )
            )
        }

        val fileName = "${preset.id}_${System.currentTimeMillis()}.dat"
        val targetFile = File(cacheDir, fileName)
        try {
            val oneMbChunk = ByteArray(1024 * 1024) { index ->
                ((index * 31) and 0xFF).toByte()
            }
            FileOutputStream(targetFile).use { fos ->
                for (step in 1..preset.sizeMb) {
                    fos.write(oneMbChunk)
                    onProgress(step.toFloat() / preset.sizeMb.toFloat())
                }
                fos.flush()
            }
            prefsRepo.recordPackMetadata(
                fileName = fileName,
                title = preset.title,
                category = preset.category
            )
            Result.success(
                OfflineCachePack(
                    id = fileName,
                    fileName = fileName,
                    title = preset.title,
                    category = preset.category,
                    sizeMb = preset.sizeMb,
                    createdAtMs = targetFile.lastModified()
                )
            )
        } catch (e: Exception) {
            if (targetFile.exists()) targetFile.delete()
            Result.failure(e)
        }
    }

    suspend fun deletePack(fileName: String): Boolean = withContext(Dispatchers.IO) {
        val target = File(cacheDir, fileName)
        val deleted = !target.exists() || target.delete()
        if (deleted) {
            prefsRepo.removePackMetadata(fileName)
        }
        deleted
    }

    suspend fun clearAllPacks(): Int = withContext(Dispatchers.IO) {
        val files = cacheDir.listFiles() ?: emptyArray()
        var deletedCount = 0
        for (file in files) {
            if (file.delete()) deletedCount++
        }
        prefsRepo.clearAllPackMetadata()
        deletedCount
    }
}
