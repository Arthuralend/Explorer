package com.arthuralend.explorer.data

import java.io.File
import java.io.IOException

object FileOps {
    data class Stats(val size: Long, val files: Int, val dirs: Int) {
        operator fun plus(o: Stats) = Stats(size + o.size, files + o.files, dirs + o.dirs)
    }

    /** «Имя - копия.ext», «Имя - копия (2).ext» — как в Windows. */
    fun uniqueTarget(dir: File, name: String): File {
        var t = File(dir, name)
        if (!t.exists()) return t
        val dot = name.lastIndexOf('.')
        val base = if (dot > 0) name.substring(0, dot) else name
        val ext = if (dot > 0) name.substring(dot) else ""
        t = File(dir, "$base - копия$ext")
        var n = 2
        while (t.exists()) {
            t = File(dir, "$base - копия ($n)$ext")
            n++
        }
        return t
    }

    fun uniqueName(dir: File?, name: String): String {
        if (dir == null || !File(dir, name).exists()) return name
        var n = 2
        while (File(dir, "$name ($n)").exists()) n++
        return "$name ($n)"
    }

    private fun isInside(child: File, parent: File): Boolean {
        val c = child.canonicalPath
        val p = parent.canonicalPath
        return c == p || c.startsWith("$p/")
    }

    fun copy(src: File, dstDir: File, progress: (String) -> Unit): File {
        if (src.isDirectory && isInside(dstDir, src)) {
            throw IOException("Нельзя скопировать папку «${src.name}» в саму себя")
        }
        val target = uniqueTarget(dstDir, src.name)
        copyRec(src, target, progress)
        return target
    }

    private fun copyRec(src: File, dst: File, progress: (String) -> Unit) {
        if (src.isDirectory) {
            if (!dst.mkdirs() && !dst.isDirectory) throw IOException("Не удалось создать папку «${dst.name}»")
            src.listFiles()?.forEach { copyRec(it, File(dst, it.name), progress) }
        } else {
            progress(src.name)
            src.inputStream().use { input ->
                dst.outputStream().use { output -> input.copyTo(output, 256 * 1024) }
            }
        }
        dst.setLastModified(src.lastModified())
    }

    fun move(src: File, dstDir: File, progress: (String) -> Unit): File {
        if (src.parentFile?.canonicalPath == dstDir.canonicalPath) return src
        if (src.isDirectory && isInside(dstDir, src)) {
            throw IOException("Нельзя переместить папку «${src.name}» в саму себя")
        }
        val target = uniqueTarget(dstDir, src.name)
        progress(src.name)
        if (src.renameTo(target)) return target
        copyRec(src, target, progress)
        if (!src.deleteRecursively()) throw IOException("Не удалось удалить исходный «${src.name}»")
        return target
    }

    fun delete(f: File) {
        if (!f.deleteRecursively()) throw IOException("Не удалось удалить «${f.name}»")
    }

    fun stats(f: File): Stats {
        if (!f.isDirectory) return Stats(f.length(), 1, 0)
        var size = 0L
        var files = 0
        var dirs = 0
        f.walkTopDown().forEach {
            if (it == f) return@forEach
            if (it.isDirectory) dirs++ else {
                files++
                size += it.length()
            }
        }
        return Stats(size, files, dirs)
    }
}
