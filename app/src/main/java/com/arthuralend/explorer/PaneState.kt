package com.arthuralend.explorer

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.arthuralend.explorer.data.FileItem
import com.arthuralend.explorer.data.FileLister
import com.arthuralend.explorer.data.SortMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/** Состояние одной половины экрана: текущая папка, история, выделение, вид. */
class PaneState(
    private val scope: CoroutineScope,
    private val roots: () -> List<File>,
    start: File?,
) {
    /** null — «Этот компьютер». */
    var dir by mutableStateOf(start)
        private set
    var items by mutableStateOf(emptyList<FileItem>())
        private set
    var loading by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set

    var selection by mutableStateOf(emptySet<String>())
    var multiSelect by mutableStateOf(false)
    var menuFor by mutableStateOf<String?>(null)
    var pcSelected by mutableStateOf<String?>(null)

    var sort by mutableStateOf(SortMode.NAME)
        private set
    var ascending by mutableStateOf(true)
        private set
    var grid by mutableStateOf(false)
    var showHidden by mutableStateOf(false)

    var canBack by mutableStateOf(false)
        private set
    var canForward by mutableStateOf(false)
        private set

    val listState = LazyListState()
    val gridState = LazyGridState()

    private val backStack = ArrayDeque<File?>()
    private val forwardStack = ArrayDeque<File?>()
    private var job: Job? = null

    fun open(target: File?, record: Boolean = true) {
        if (record) {
            if (target?.absolutePath == dir?.absolutePath) {
                refresh()
                return
            }
            backStack.addLast(dir)
            if (backStack.size > 100) backStack.removeFirst()
            forwardStack.clear()
        }
        dir = target
        selection = emptySet()
        multiSelect = false
        menuFor = null
        pcSelected = null
        items = emptyList()
        error = null
        updateNav()
        refresh(resetScroll = true)
    }

    fun refresh(resetScroll: Boolean = false) {
        job?.cancel()
        val d = dir
        if (d == null) {
            items = emptyList()
            error = null
            loading = false
            return
        }
        job = scope.launch {
            loading = true
            val result = withContext(Dispatchers.IO) { FileLister.list(d, showHidden, sort, ascending) }
            loading = false
            if (result == null) {
                items = emptyList()
                error = if (!d.exists()) "Папка не найдена" else "Нет доступа к этой папке"
            } else {
                error = null
                items = result
                val paths = result.mapTo(HashSet()) { it.path }
                selection = selection.filterTo(LinkedHashSet()) { it in paths }
            }
            if (resetScroll) {
                listState.requestScrollToItem(0)
                gridState.requestScrollToItem(0)
            }
        }
    }

    fun goBack() {
        if (backStack.isEmpty()) return
        forwardStack.addLast(dir)
        open(backStack.removeLast(), record = false)
    }

    fun goForward() {
        if (forwardStack.isEmpty()) return
        backStack.addLast(dir)
        open(forwardStack.removeLast(), record = false)
    }

    fun goUp() {
        val d = dir ?: return
        val isRoot = roots().any { it.absolutePath == d.absolutePath }
        val parent = d.parentFile
        open(if (isRoot || parent == null || !parent.canRead()) null else parent)
    }

    fun setSort(mode: SortMode) {
        if (sort == mode) {
            ascending = !ascending
        } else {
            sort = mode
            ascending = mode == SortMode.NAME || mode == SortMode.TYPE
        }
        refresh()
    }

    fun toggleSelect(path: String) {
        selection = if (path in selection) selection - path else selection + path
    }

    fun selectAll() {
        selection = items.mapTo(LinkedHashSet()) { it.path }
        if (items.size > 1) multiSelect = true
    }

    fun clearSelection() {
        selection = emptySet()
        multiSelect = false
    }

    fun selectedFiles(): List<File> = selection.map(::File)

    private fun updateNav() {
        canBack = backStack.isNotEmpty()
        canForward = forwardStack.isNotEmpty()
    }
}
