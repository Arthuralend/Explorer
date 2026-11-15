package com.arthuralend.explorer.data

import androidx.compose.ui.graphics.Color
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class FileItem(
    val file: File,
    val name: String,
    val isDir: Boolean,
    val size: Long,
    val modified: Long,
    val hidden: Boolean,
) {
    val path: String get() = file.absolutePath
    val ext: String get() = if (isDir) "" else name.substringAfterLast('.', "").lowercase(Locale.ROOT)

    companion object {
        fun of(f: File): FileItem {
            val dir = f.isDirectory
            return FileItem(
                file = f,
                name = f.name,
                isDir = dir,
                size = if (dir) 0L else f.length(),
                modified = f.lastModified(),
                hidden = f.name.startsWith(".") || f.isHidden,
            )
        }
    }
}

enum class SortMode(val label: String) {
    NAME("Имя"), DATE("Дата изменения"), TYPE("Тип"), SIZE("Размер")
}

/** Сравнение «как в Проводнике»: file2 < file10. */
object NaturalOrder : Comparator<String> {
    override fun compare(a: String, b: String): Int {
        var i = 0
        var j = 0
        while (i < a.length && j < b.length) {
            val ca = a[i]
            val cb = b[j]
            if (ca.isDigit() && cb.isDigit()) {
                var ei = i
                while (ei < a.length && a[ei].isDigit()) ei++
                var ej = j
                while (ej < b.length && b[ej].isDigit()) ej++
                val na = a.substring(i, ei).trimStart('0')
                val nb = b.substring(j, ej).trimStart('0')
                if (na.length != nb.length) return na.length - nb.length
                val c = na.compareTo(nb)
                if (c != 0) return c
                i = ei
                j = ej
            } else {
                val c = ca.lowercaseChar().compareTo(cb.lowercaseChar())
                if (c != 0) return c
                i++
                j++
            }
        }
        return (a.length - i) - (b.length - j)
    }
}

object FileLister {
    /** null — папку прочитать нельзя. */
    fun list(dir: File, showHidden: Boolean, sort: SortMode, ascending: Boolean): List<FileItem>? {
        val files = dir.listFiles() ?: return null
        val items = files.map(FileItem::of).filter { showHidden || !it.hidden }
        val byName = Comparator<FileItem> { x, y -> NaturalOrder.compare(x.name, y.name) }
        val cmp: Comparator<FileItem> = when (sort) {
            SortMode.NAME -> byName
            SortMode.DATE -> compareBy<FileItem> { it.modified }.then(byName)
            SortMode.SIZE -> compareBy<FileItem> { it.size }.then(byName)
            SortMode.TYPE -> compareBy<FileItem> { it.ext }.then(byName)
        }
        val ordered = if (ascending) cmp else cmp.reversed()
        // Папки всегда сверху, как в Windows.
        return items.filter { it.isDir }.sortedWith(ordered) + items.filter { !it.isDir }.sortedWith(ordered)
    }
}

object Fmt {
    private val ru = Locale("ru")
    private val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", ru)

    fun date(t: Long): String = if (t <= 0) "" else synchronized(dateFormat) { dateFormat.format(Date(t)) }

    fun size(b: Long): String {
        if (b < 1024) return "$b Б"
        val units = arrayOf("КБ", "МБ", "ГБ", "ТБ")
        var v = b / 1024.0
        var i = 0
        while (v >= 1024 && i < units.size - 1) {
            v /= 1024
            i++
        }
        return String.format(ru, if (v < 10) "%.1f %s" else "%.0f %s", v, units[i])
    }

    fun bytes(b: Long): String = String.format(ru, "%,d %s", b, plural(b, "байт", "байта", "байт"))

    fun plural(n: Long, one: String, few: String, many: String): String {
        val m10 = n % 10
        val m100 = n % 100
        return when {
            m10 == 1L && m100 != 11L -> one
            m10 in 2..4 && m100 !in 12..14 -> few
            else -> many
        }
    }

    fun items(n: Int): String = "$n ${plural(n.toLong(), "элемент", "элемента", "элементов")}"
    fun files(n: Int): String = "$n ${plural(n.toLong(), "файл", "файла", "файлов")}"
    fun folders(n: Int): String = "$n ${plural(n.toLong(), "папка", "папки", "папок")}"
}

enum class Category { IMAGE, VIDEO, AUDIO, ARCHIVE, APK, PDF, DOC, SHEET, SLIDES, TEXT, CODE, OTHER }

data class FileType(val category: Category, val color: Color, val description: String)

object FileTypes {
    private val image = setOf("jpg", "jpeg", "png", "gif", "webp", "bmp", "heic", "heif", "svg", "ico", "tif", "tiff")
    private val video = setOf("mp4", "mkv", "avi", "mov", "webm", "3gp", "wmv", "flv", "m4v", "ts")
    private val audio = setOf("mp3", "wav", "flac", "ogg", "m4a", "aac", "opus", "wma", "amr", "mid")
    private val archive = setOf("zip", "rar", "7z", "tar", "gz", "bz2", "xz", "tgz", "zst")
    private val doc = setOf("doc", "docx", "odt", "rtf")
    private val sheet = setOf("xls", "xlsx", "ods", "csv")
    private val slides = setOf("ppt", "pptx", "odp")
    private val text = setOf("txt", "md", "log", "ini", "cfg", "conf", "nfo")
    private val code = setOf(
        "json", "xml", "html", "htm", "css", "js", "ts", "kt", "kts", "java", "py", "c", "cpp", "h",
        "sh", "bat", "yml", "yaml", "gradle", "sql", "php", "go", "rs", "swift",
    )

    fun of(ext: String): FileType {
        val e = ext.uppercase(Locale.ROOT)
        return when (ext) {
            in image -> FileType(Category.IMAGE, Color(0xFF788C5D), "Изображение $e")
            in video -> FileType(Category.VIDEO, Color(0xFF8E6BB8), "Видео $e")
            in audio -> FileType(Category.AUDIO, Color(0xFFC0628C), "Аудио $e")
            in archive -> FileType(Category.ARCHIVE, Color(0xFFB8892B), "Архив $e")
            "apk" -> FileType(Category.APK, Color(0xFF5E8C3A), "Пакет Android")
            "pdf" -> FileType(Category.PDF, Color(0xFFC0453A), "Документ PDF")
            in doc -> FileType(Category.DOC, Color(0xFF3B6FB6), "Документ $e")
            in sheet -> FileType(Category.SHEET, Color(0xFF2F7D4F), "Таблица $e")
            in slides -> FileType(Category.SLIDES, Color(0xFFC6532B), "Презентация $e")
            in text -> FileType(Category.TEXT, Color(0xFF6B6A64), "Текстовый документ")
            in code -> FileType(Category.CODE, Color(0xFF6A9BCC), "Файл $e")
            "" -> FileType(Category.OTHER, Color(0xFF9A978D), "Файл")
            else -> FileType(Category.OTHER, Color(0xFF9A978D), "Файл $e")
        }
    }

    fun describe(item: FileItem): String = if (item.isDir) "Папка с файлами" else of(item.ext).description
}
