package com.arthuralend.explorer.data

import android.content.Context
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.os.storage.StorageManager
import java.io.File

data class Volume(val root: File, val label: String, val letter: Char, val removable: Boolean) {
    val total: Long get() = runCatching { StatFs(root.path).totalBytes }.getOrDefault(0L)
    val free: Long get() = runCatching { StatFs(root.path).availableBytes }.getOrDefault(0L)
}

enum class FolderKind { DOWNLOADS, DOCUMENTS, PICTURES, CAMERA, MUSIC, VIDEOS }

data class SpecialFolder(val label: String, val file: File, val kind: FolderKind)

object Storage {
    fun primaryRoot(): File = Environment.getExternalStorageDirectory()

    fun volumes(ctx: Context): List<Volume> {
        val roots = LinkedHashMap<String, Pair<String, Boolean>>()
        if (Build.VERSION.SDK_INT >= 30) {
            val sm = ctx.getSystemService(StorageManager::class.java)
            for (v in sm.storageVolumes) {
                val dir = v.directory ?: continue
                val state = v.state
                if (state != Environment.MEDIA_MOUNTED && state != Environment.MEDIA_MOUNTED_READ_ONLY) continue
                val label = if (v.isPrimary) "Внутренняя память" else (v.getDescription(ctx) ?: "Накопитель")
                roots[dir.absolutePath] = label to v.isRemovable
            }
        }
        if (roots.isEmpty()) {
            ctx.getExternalFilesDirs(null).filterNotNull().forEachIndexed { i, f ->
                val p = f.absolutePath
                val idx = p.indexOf("/Android/data")
                if (idx > 0) {
                    roots.putIfAbsent(p.substring(0, idx), (if (i == 0) "Внутренняя память" else "SD-карта") to (i != 0))
                }
            }
        }
        if (roots.isEmpty()) roots[primaryRoot().absolutePath] = "Внутренняя память" to false
        var letter = 'C'
        return roots.map { (path, info) -> Volume(File(path), info.first, letter++, info.second) }
    }

    fun specialFolders(): List<SpecialFolder> {
        val root = primaryRoot()
        return listOf(
            Triple("Загрузки", Environment.DIRECTORY_DOWNLOADS, FolderKind.DOWNLOADS),
            Triple("Документы", Environment.DIRECTORY_DOCUMENTS, FolderKind.DOCUMENTS),
            Triple("Изображения", Environment.DIRECTORY_PICTURES, FolderKind.PICTURES),
            Triple("Камера", Environment.DIRECTORY_DCIM, FolderKind.CAMERA),
            Triple("Музыка", Environment.DIRECTORY_MUSIC, FolderKind.MUSIC),
            Triple("Видео", Environment.DIRECTORY_MOVIES, FolderKind.VIDEOS),
        ).map { (label, name, kind) -> SpecialFolder(label, File(root, name), kind) }
            .filter { it.file.isDirectory }
    }

    fun volumeFor(file: File, volumes: List<Volume>): Volume? {
        val p = file.absolutePath
        return volumes
            .filter { p == it.root.absolutePath || p.startsWith(it.root.absolutePath + "/") }
            .maxByOrNull { it.root.absolutePath.length }
    }
}
