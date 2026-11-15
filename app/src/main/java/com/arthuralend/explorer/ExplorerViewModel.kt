package com.arthuralend.explorer

import android.app.Application
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.webkit.MimeTypeMap
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.arthuralend.explorer.data.FileItem
import com.arthuralend.explorer.data.FileOps
import com.arthuralend.explorer.data.Fmt
import com.arthuralend.explorer.data.FolderKind
import com.arthuralend.explorer.data.Storage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale

sealed interface DialogState {
    data class NewFolder(val pane: PaneState) : DialogState
    data class Rename(val file: File) : DialogState
    data class Delete(val files: List<File>) : DialogState
    data class Properties(val files: List<File>) : DialogState
}

data class Clip(val files: List<File>, val cut: Boolean) {
    val paths: Set<String> = files.mapTo(HashSet()) { it.absolutePath }
}

class ExplorerViewModel(app: Application) : AndroidViewModel(app) {
    private val prefs = app.getSharedPreferences("explorer", Context.MODE_PRIVATE)

    var volumes by mutableStateOf(Storage.volumes(app))
        private set
    var specialFolders by mutableStateOf(Storage.specialFolders())
        private set
    private var specialKinds: Map<String, FolderKind> = specialFolders.associate { it.file.absolutePath to it.kind }

    val left = PaneState(viewModelScope, { volumes.map { it.root } }, null)
    val right = PaneState(viewModelScope, { volumes.map { it.root } }, Storage.primaryRoot())

    var activeLeft by mutableStateOf(true)
        private set
    val active: PaneState get() = if (activeLeft) left else right
    fun other(p: PaneState): PaneState = if (p === left) right else left
    fun activate(p: PaneState) {
        if (activeLeft != (p === left)) activeLeft = p === left
    }

    var oneTapOpen by mutableStateOf(prefs.getBoolean("oneTap", false))
        private set
    var showHidden by mutableStateOf(prefs.getBoolean("hidden", false))
        private set

    var clipboard by mutableStateOf<Clip?>(null)
        private set
    var dialog by mutableStateOf<DialogState?>(null)
    var busy by mutableStateOf<String?>(null)
        private set
    var busyDetail by mutableStateOf("")
        private set

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val messages: SharedFlow<String> = _messages
    fun toast(text: String) {
        _messages.tryEmit(text)
    }

    init {
        left.showHidden = showHidden
        right.showHidden = showHidden
        left.grid = prefs.getBoolean("gridL", false)
        right.grid = prefs.getBoolean("gridR", false)
        right.refresh()
    }

    fun onResume() {
        volumes = Storage.volumes(getApplication())
        specialFolders = Storage.specialFolders()
        specialKinds = specialFolders.associate { it.file.absolutePath to it.kind }
        refreshPanes()
    }

    fun refreshPanes() {
        left.refresh()
        right.refresh()
    }

    fun glyphFor(item: FileItem): FolderKind? = if (item.isDir) specialKinds[item.path] else null
    fun isCut(path: String): Boolean = clipboard?.let { it.cut && path in it.paths } == true

    // ---- Настройки ----

    fun toggleOneTap() {
        oneTapOpen = !oneTapOpen
        prefs.edit().putBoolean("oneTap", oneTapOpen).apply()
    }

    fun toggleHidden() {
        showHidden = !showHidden
        prefs.edit().putBoolean("hidden", showHidden).apply()
        left.showHidden = showHidden
        right.showHidden = showHidden
        refreshPanes()
    }

    fun setGrid(pane: PaneState, grid: Boolean) {
        pane.grid = grid
        prefs.edit().putBoolean(if (pane === left) "gridL" else "gridR", grid).apply()
    }

    // ---- Панели ----

    fun swapPanes() {
        val l = left.dir
        val r = right.dir
        left.open(r)
        right.open(l)
    }

    fun mirror() {
        other(active).open(active.dir)
    }

    fun openItem(pane: PaneState, item: FileItem) {
        if (item.isDir) pane.open(item.file) else openFile(item.file)
    }

    private fun uriFor(file: File): Uri {
        val ctx = getApplication<Application>()
        return FileProvider.getUriForFile(ctx, ctx.packageName + ".fileprovider", file)
    }

    private fun mimeOf(file: File): String {
        val ext = file.extension.lowercase(Locale.ROOT)
        if (ext == "apk") return "application/vnd.android.package-archive"
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext) ?: "*/*"
    }

    fun openFile(file: File) {
        val ctx = getApplication<Application>()
        try {
            val intent = Intent(Intent.ACTION_VIEW)
                .setDataAndType(uriFor(file), mimeOf(file))
                .addFlags(
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or
                        Intent.FLAG_GRANT_WRITE_URI_PERMISSION or
                        Intent.FLAG_ACTIVITY_NEW_TASK,
                )
            try {
                ctx.startActivity(intent)
            } catch (e: ActivityNotFoundException) {
                ctx.startActivity(
                    Intent.createChooser(intent.setDataAndType(intent.data, "*/*"), "Открыть с помощью")
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                )
            }
        } catch (e: Exception) {
            toast("Нет приложения, чтобы открыть «${file.name}»")
        }
    }

    fun share(files: List<File>) {
        val list = files.filter { it.isFile }
        if (list.isEmpty()) {
            toast("Поделиться можно только файлами")
            return
        }
        val ctx = getApplication<Application>()
        try {
            val uris = ArrayList(list.map(::uriFor))
            val intent = if (uris.size == 1) {
                Intent(Intent.ACTION_SEND).setType(mimeOf(list[0])).putExtra(Intent.EXTRA_STREAM, uris[0])
            } else {
                Intent(Intent.ACTION_SEND_MULTIPLE).setType("*/*").putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
            }
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            ctx.startActivity(Intent.createChooser(intent, "Поделиться").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (e: Exception) {
            toast("Не удалось поделиться: ${e.message}")
        }
    }

    // ---- Буфер обмена и операции ----

    fun cut(files: List<File>) {
        if (files.isEmpty()) return
        clipboard = Clip(files, cut = true)
        toast("Вырезано: ${Fmt.items(files.size)}")
    }

    fun copy(files: List<File>) {
        if (files.isEmpty()) return
        clipboard = Clip(files, cut = false)
        toast("Скопировано в буфер: ${Fmt.items(files.size)}")
    }

    fun paste(pane: PaneState) {
        val dst = pane.dir ?: return
        val clip = clipboard ?: return
        transfer(clip.files, dst, clip.cut)
        if (clip.cut) clipboard = null
    }

    fun transferToOther(pane: PaneState, files: List<File>, move: Boolean) {
        if (files.isEmpty()) return
        val dst = other(pane).dir
        if (dst == null) {
            toast("Откройте папку в другой панели")
            return
        }
        transfer(files, dst, move)
    }

    private fun transfer(files: List<File>, dst: File, move: Boolean) {
        runOp(if (move) "Перемещение…" else "Копирование…") {
            files.forEach {
                if (move) FileOps.move(it, dst, ::progress) else FileOps.copy(it, dst, ::progress)
            }
            "${if (move) "Перемещено" else "Скопировано"}: ${Fmt.items(files.size)}"
        }
    }

    fun delete(files: List<File>) {
        runOp("Удаление…") {
            files.forEach {
                progress(it.name)
                FileOps.delete(it)
            }
            "Удалено: ${Fmt.items(files.size)}"
        }
    }

    private fun validateName(name: String): String? = when {
        name.isBlank() -> "Введите имя"
        name.contains('/') || name.contains('\u0000') -> "Имя не может содержать символ /"
        name == "." || name == ".." -> "Недопустимое имя"
        else -> null
    }

    /** @return текст ошибки или null при успехе. */
    fun rename(file: File, newName: String): String? {
        validateName(newName)?.let { return it }
        if (newName == file.name) {
            dialog = null
            return null
        }
        val target = File(file.parentFile, newName)
        if (target.exists() && !target.name.equals(file.name, ignoreCase = true)) {
            return "Элемент с таким именем уже существует"
        }
        if (!file.renameTo(target)) return "Не удалось переименовать"
        dialog = null
        refreshPanes()
        listOf(left, right).forEach { p ->
            if (p.dir?.absolutePath == file.parentFile?.absolutePath) p.selection = setOf(target.absolutePath)
        }
        return null
    }

    fun createFolder(pane: PaneState, name: String): String? {
        val dir = pane.dir ?: return "Откройте папку"
        validateName(name)?.let { return it }
        val target = File(dir, name)
        if (target.exists()) return "Элемент с таким именем уже существует"
        if (!target.mkdir()) return "Не удалось создать папку"
        dialog = null
        refreshPanes()
        pane.selection = setOf(target.absolutePath)
        return null
    }

    private fun progress(name: String) {
        busyDetail = name
    }

    private fun runOp(title: String, block: suspend () -> String) {
        viewModelScope.launch {
            busy = title
            busyDetail = ""
            try {
                val msg = withContext(Dispatchers.IO) { block() }
                toast(msg)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                toast(e.message ?: "Ошибка")
            } finally {
                busy = null
                refreshPanes()
            }
        }
    }
}
