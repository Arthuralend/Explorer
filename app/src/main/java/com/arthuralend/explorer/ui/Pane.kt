package com.arthuralend.explorer.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DriveFileMove
import androidx.compose.material.icons.filled.DriveFileRenameOutline
import androidx.compose.material.icons.filled.FileCopy
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.VerticalSplit
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.arthuralend.explorer.ExplorerViewModel
import com.arthuralend.explorer.DialogState
import com.arthuralend.explorer.PaneState
import com.arthuralend.explorer.data.FileItem
import com.arthuralend.explorer.data.FileTypes
import com.arthuralend.explorer.data.Fmt
import com.arthuralend.explorer.data.SortMode
import com.arthuralend.explorer.data.Storage
import com.arthuralend.explorer.data.Volume
import java.io.File

private val RowShape = RoundedCornerShape(6.dp)

@Composable
fun Pane(state: PaneState, vm: ExplorerViewModel, isActive: Boolean, modifier: Modifier = Modifier) {
    val cs = MaterialTheme.colorScheme
    Surface(
        modifier = modifier.pointerInput(state) {
            awaitPointerEventScope {
                while (true) {
                    val e = awaitPointerEvent(PointerEventPass.Initial)
                    if (e.type == PointerEventType.Press) vm.activate(state)
                }
            }
        },
        shape = RoundedCornerShape(14.dp),
        color = cs.surfaceContainerLowest,
        border = BorderStroke(
            if (isActive) 1.5.dp else 1.dp,
            if (isActive) cs.primary.copy(alpha = 0.85f) else cs.outlineVariant,
        ),
    ) {
        Column(Modifier.fillMaxSize()) {
            NavRow(state, vm, isActive)
            HorizontalDivider(color = cs.outlineVariant)
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            ) {
                val error = state.error
                when {
                    state.dir == null -> ThisPcView(state, vm)
                    error != null -> CenterMessage(error)
                    state.items.isEmpty() && !state.loading -> CenterMessage("Эта папка пуста.")
                    state.grid -> FileGrid(state, vm)
                    else -> FileList(state, vm)
                }
            }
            HorizontalDivider(color = cs.outlineVariant)
            StatusLine(state, vm)
        }
    }
}

@Composable
private fun CenterMessage(text: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(24.dp),
        )
    }
}

// ---------------- Строка навигации и адресная строка ----------------

@Composable
private fun NavRow(state: PaneState, vm: ExplorerViewModel, isActive: Boolean) {
    val cs = MaterialTheme.colorScheme
    Row(
        Modifier
            .fillMaxWidth()
            .background(if (isActive) cs.primaryContainer.copy(alpha = 0.45f) else cs.surfaceContainerLow)
            .height(46.dp)
            .padding(horizontal = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        NavButton(Icons.AutoMirrored.Filled.ArrowBack, "Назад", state.canBack) { state.goBack() }
        NavButton(Icons.AutoMirrored.Filled.ArrowForward, "Вперёд", state.canForward) { state.goForward() }
        NavButton(Icons.Filled.ArrowUpward, "Вверх", state.dir != null) { state.goUp() }
        AddressBar(state, vm, Modifier.weight(1f))
        NavButton(Icons.Filled.Refresh, "Обновить", true) {
            if (state.dir == null) vm.onResume() else state.refresh()
        }
    }
}

@Composable
private fun NavButton(icon: ImageVector, label: String, enabled: Boolean, onClick: () -> Unit) {
    IconButton(onClick = onClick, enabled = enabled, modifier = Modifier.size(36.dp)) {
        Icon(icon, contentDescription = label, modifier = Modifier.size(20.dp))
    }
}

private data class Crumb(val label: String, val file: File?)

private fun crumbs(dir: File?, volumes: List<Volume>): List<Crumb> {
    val list = mutableListOf(Crumb("Этот компьютер", null))
    if (dir == null) return list
    val vol = Storage.volumeFor(dir, volumes)
    var cur: File
    val rest: String
    if (vol != null) {
        list += Crumb("${vol.label} (${vol.letter}:)", vol.root)
        cur = vol.root
        rest = dir.absolutePath.removePrefix(vol.root.absolutePath)
    } else {
        cur = File("/")
        list += Crumb("/", cur)
        rest = dir.absolutePath
    }
    rest.split('/').filter { it.isNotEmpty() }.forEach {
        cur = File(cur, it)
        list += Crumb(it, cur)
    }
    return list
}

@Composable
private fun AddressBar(state: PaneState, vm: ExplorerViewModel, modifier: Modifier) {
    val cs = MaterialTheme.colorScheme
    val segments = remember(state.dir, vm.volumes) { crumbs(state.dir, vm.volumes) }
    val rowState = rememberLazyListState()
    LaunchedEffect(segments) { rowState.scrollToItem((segments.size - 1).coerceAtLeast(0)) }
    Surface(
        modifier = modifier
            .padding(horizontal = 4.dp)
            .height(34.dp),
        shape = RoundedCornerShape(8.dp),
        color = cs.surfaceContainerLowest,
        border = BorderStroke(1.dp, cs.outlineVariant),
    ) {
        LazyRow(
            state = rowState,
            verticalAlignment = Alignment.CenterVertically,
            contentPadding = PaddingValues(horizontal = 4.dp),
        ) {
            itemsIndexed(segments) { i, seg ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (i > 0) {
                        Icon(
                            Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = cs.onSurfaceVariant,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                    val last = i == segments.lastIndex
                    Row(
                        Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { state.open(seg.file) }
                            .padding(horizontal = 5.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (i == 0) {
                            Image(ExplorerIcons.ThisPc, contentDescription = seg.label, modifier = Modifier.size(18.dp))
                        }
                        if (i > 0 || segments.size == 1) {
                            if (i == 0) Spacer(Modifier.width(5.dp))
                            Text(
                                seg.label,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = if (last) FontWeight.SemiBold else FontWeight.Normal,
                                maxLines = 1,
                            )
                        }
                    }
                }
            }
        }
    }
}

// ---------------- Статусная строка ----------------

@Composable
private fun StatusLine(state: PaneState, vm: ExplorerViewModel) {
    val cs = MaterialTheme.colorScheme
    Row(
        Modifier
            .fillMaxWidth()
            .height(28.dp)
            .padding(start = 10.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val text = if (state.dir == null) {
            Fmt.items(vm.specialFolders.size + vm.volumes.size)
        } else {
            buildString {
                append(Fmt.items(state.items.size))
                if (state.selection.isNotEmpty()) {
                    append("   |   Выбрано: ${state.selection.size}")
                    val size = state.items.filter { it.path in state.selection && !it.isDir }.sumOf { it.size }
                    if (size > 0) append(" (${Fmt.size(size)})")
                }
            }
        }
        Text(
            text,
            style = MaterialTheme.typography.labelSmall,
            color = cs.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        if (state.loading) {
            CircularProgressIndicator(Modifier.size(12.dp), strokeWidth = 1.5.dp)
            Spacer(Modifier.width(6.dp))
        }
        if (state.dir != null) {
            ViewToggle(Icons.AutoMirrored.Filled.ViewList, "Таблица", !state.grid) { vm.setGrid(state, false) }
            ViewToggle(Icons.Filled.GridView, "Крупные значки", state.grid) { vm.setGrid(state, true) }
        }
    }
}

@Composable
private fun ViewToggle(icon: ImageVector, label: String, selected: Boolean, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    Box(
        Modifier
            .size(26.dp)
            .clip(RoundedCornerShape(5.dp))
            .background(if (selected) cs.primary.copy(alpha = 0.16f) else Color.Transparent)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            icon,
            contentDescription = label,
            tint = if (selected) cs.primary else cs.onSurfaceVariant,
            modifier = Modifier.size(16.dp),
        )
    }
}

// ---------------- Клики «как в Windows» ----------------

@OptIn(ExperimentalFoundationApi::class)
private fun Modifier.itemClicks(item: FileItem, state: PaneState, vm: ExplorerViewModel): Modifier {
    val oneTap = vm.oneTapOpen
    val open = { vm.openItem(state, item) }
    return combinedClickable(
        onClick = {
            when {
                state.multiSelect -> state.toggleSelect(item.path)
                oneTap -> open()
                else -> state.selection = setOf(item.path)
            }
        },
        onDoubleClick = if (oneTap || state.multiSelect) null else open,
        onLongClick = {
            if (item.path !in state.selection) {
                state.selection = if (state.multiSelect) state.selection + item.path else setOf(item.path)
            }
            state.menuFor = item.path
        },
    )
}

@Composable
private fun Modifier.selectionLook(selected: Boolean, shape: RoundedCornerShape = RowShape): Modifier {
    val cs = MaterialTheme.colorScheme
    return this
        .clip(shape)
        .background(if (selected) cs.primary.copy(alpha = 0.14f) else Color.Transparent)
        .then(if (selected) Modifier.border(1.dp, cs.primary.copy(alpha = 0.5f), shape) else Modifier)
}

// ---------------- Таблица ----------------

@Composable
private fun FileList(state: PaneState, vm: ExplorerViewModel) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val wide = maxWidth >= 400.dp
        val showType = maxWidth >= 600.dp
        Column(Modifier.fillMaxSize()) {
            if (wide) ColumnHeader(state, showType)
            LazyColumn(
                state = state.listState,
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp),
            ) {
                items(state.items, key = { it.path }) { item ->
                    FileRow(item, state, vm, wide, showType)
                }
            }
        }
    }
}

private val DateCol = 112.dp
private val TypeCol = 130.dp
private val SizeCol = 72.dp

@Composable
private fun ColumnHeader(state: PaneState, showType: Boolean) {
    val cs = MaterialTheme.colorScheme
    Row(
        Modifier
            .fillMaxWidth()
            .background(cs.surfaceContainerLow)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val lead = 36.dp + if (state.multiSelect) 32.dp else 0.dp
        HeaderCell(state, SortMode.NAME, "Имя", Modifier.weight(1f).padding(start = lead))
        HeaderCell(state, SortMode.DATE, "Дата изменения", Modifier.width(DateCol).padding(start = 8.dp))
        if (showType) HeaderCell(state, SortMode.TYPE, "Тип", Modifier.width(TypeCol).padding(start = 8.dp))
        HeaderCell(state, SortMode.SIZE, "Размер", Modifier.width(SizeCol), end = true)
    }
    HorizontalDivider(color = cs.outlineVariant)
}

@Composable
private fun HeaderCell(state: PaneState, mode: SortMode, label: String, modifier: Modifier, end: Boolean = false) {
    val cs = MaterialTheme.colorScheme
    Row(
        modifier
            .clickable { state.setSort(mode) }
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = if (end) Arrangement.End else Arrangement.Start,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = if (state.sort == mode) cs.primary else cs.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false),
        )
        if (state.sort == mode) {
            Icon(
                if (state.ascending) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                contentDescription = null,
                tint = cs.primary,
                modifier = Modifier.size(14.dp),
            )
        }
    }
}

@Composable
private fun FileRow(item: FileItem, state: PaneState, vm: ExplorerViewModel, wide: Boolean, showType: Boolean) {
    val cs = MaterialTheme.colorScheme
    val selected = item.path in state.selection
    val faded = item.hidden || vm.isCut(item.path)
    Box {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(vertical = 1.dp)
                .selectionLook(selected)
                .itemClicks(item, state, vm)
                .padding(horizontal = 8.dp, vertical = if (wide) 6.dp else 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (state.multiSelect) {
                Checkbox(checked = selected, onCheckedChange = null, modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(8.dp))
            }
            ItemIcon(
                item,
                if (wide) 26.dp else 34.dp,
                vm.glyphFor(item),
                Modifier.alpha(if (faded) 0.5f else 1f),
            )
            Spacer(Modifier.width(10.dp))
            if (wide) {
                Text(
                    item.name,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                SmallCell(Fmt.date(item.modified), Modifier.width(DateCol).padding(start = 8.dp))
                if (showType) SmallCell(FileTypes.describe(item), Modifier.width(TypeCol).padding(start = 8.dp))
                SmallCell(
                    if (item.isDir) "" else Fmt.size(item.size),
                    Modifier.width(SizeCol),
                    TextAlign.End,
                )
            } else {
                Column(Modifier.weight(1f)) {
                    Text(
                        item.name,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        if (item.isDir) "${Fmt.date(item.modified)}  ·  Папка"
                        else "${Fmt.date(item.modified)}  ·  ${Fmt.size(item.size)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = cs.onSurfaceVariant,
                        maxLines = 1,
                    )
                }
            }
        }
        ItemMenu(item, state, vm)
    }
}

@Composable
private fun SmallCell(text: String, modifier: Modifier, align: TextAlign = TextAlign.Start) {
    Text(
        text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        textAlign = align,
        modifier = modifier,
    )
}

// ---------------- Крупные значки ----------------

@Composable
private fun FileGrid(state: PaneState, vm: ExplorerViewModel) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(88.dp),
        state = state.gridState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(6.dp),
    ) {
        items(state.items, key = { it.path }) { item ->
            val selected = item.path in state.selection
            Box {
                Column(
                    Modifier
                        .padding(2.dp)
                        .fillMaxWidth()
                        .selectionLook(selected, RoundedCornerShape(8.dp))
                        .itemClicks(item, state, vm)
                        .padding(6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box {
                        ItemIcon(
                            item,
                            56.dp,
                            vm.glyphFor(item),
                            Modifier.alpha(if (item.hidden || vm.isCut(item.path)) 0.5f else 1f),
                        )
                        if (state.multiSelect) {
                            Checkbox(
                                checked = selected,
                                onCheckedChange = null,
                                modifier = Modifier
                                    .size(20.dp)
                                    .align(Alignment.TopStart),
                            )
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        item.name,
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                ItemMenu(item, state, vm)
            }
        }
    }
}

// ---------------- Контекстное меню (аналог правой кнопки) ----------------

@Composable
private fun ItemMenu(item: FileItem, state: PaneState, vm: ExplorerViewModel) {
    val expanded = state.menuFor == item.path
    if (!expanded) return
    val targets = if (item.path in state.selection) state.selectedFiles() else listOf(item.file)
    val single = targets.size == 1
    fun act(block: () -> Unit) {
        state.menuFor = null
        block()
    }
    DropdownMenu(expanded = true, onDismissRequest = { state.menuFor = null }) {
        if (single) {
            MenuItem("Открыть", Icons.AutoMirrored.Filled.OpenInNew) { act { vm.openItem(state, item) } }
            if (item.isDir) {
                MenuItem("Открыть в другой панели", Icons.Filled.VerticalSplit) {
                    act { vm.other(state).open(item.file) }
                }
            }
            HorizontalDivider()
        }
        MenuItem("Вырезать", Icons.Filled.ContentCut) { act { vm.cut(targets) } }
        MenuItem("Копировать", Icons.Filled.ContentCopy) { act { vm.copy(targets) } }
        MenuItem("Копировать в другую панель", Icons.Filled.FileCopy) {
            act { vm.transferToOther(state, targets, move = false) }
        }
        MenuItem("Переместить в другую панель", Icons.Filled.DriveFileMove) {
            act { vm.transferToOther(state, targets, move = true) }
        }
        HorizontalDivider()
        if (single) {
            MenuItem("Переименовать", Icons.Filled.DriveFileRenameOutline) {
                act { vm.dialog = DialogState.Rename(item.file) }
            }
        }
        MenuItem("Удалить", Icons.Filled.Delete) { act { vm.dialog = DialogState.Delete(targets) } }
        if (targets.all { it.isFile }) {
            MenuItem("Поделиться", Icons.Filled.Share) { act { vm.share(targets) } }
        }
        HorizontalDivider()
        MenuItem("Свойства", Icons.Filled.Info) { act { vm.dialog = DialogState.Properties(targets) } }
    }
}

@Composable
fun MenuItem(text: String, icon: ImageVector?, trailing: (@Composable () -> Unit)? = null, onClick: () -> Unit) {
    DropdownMenuItem(
        text = { Text(text) },
        onClick = onClick,
        leadingIcon = icon?.let { { Icon(it, contentDescription = null, modifier = Modifier.size(20.dp)) } },
        trailingIcon = trailing,
    )
}

// ---------------- «Этот компьютер» ----------------

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ThisPcView(state: PaneState, vm: ExplorerViewModel) {
    val folders = vm.specialFolders
    val volumes = vm.volumes
    LazyVerticalGrid(
        columns = GridCells.Adaptive(92.dp),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(8.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        if (folders.isNotEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) { SectionHeader("Папки (${folders.size})") }
            items(folders, key = { "f:" + it.file.path }) { f ->
                val path = f.file.absolutePath
                Column(
                    Modifier
                        .selectionLook(state.pcSelected == path, RoundedCornerShape(8.dp))
                        .pcClicks(state, vm, path) { state.open(f.file) }
                        .padding(6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    FolderIcon(52.dp, f.kind)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        f.label,
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
        item(span = { GridItemSpan(maxLineSpan) }) { SectionHeader("Устройства и диски (${volumes.size})") }
        items(volumes, key = { "v:" + it.root.path }, span = { GridItemSpan(maxLineSpan) }) { v ->
            val path = v.root.absolutePath
            DriveTile(
                v,
                Modifier
                    .padding(vertical = 2.dp)
                    .selectionLook(state.pcSelected == path, RoundedCornerShape(10.dp))
                    .pcClicks(state, vm, path) { state.open(v.root) },
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
private fun Modifier.pcClicks(state: PaneState, vm: ExplorerViewModel, path: String, open: () -> Unit): Modifier {
    val oneTap = vm.oneTapOpen
    return combinedClickable(
        onClick = { if (oneTap) open() else state.pcSelected = path },
        onDoubleClick = if (oneTap) null else open,
    )
}

@Composable
private fun SectionHeader(title: String) {
    val cs = MaterialTheme.colorScheme
    Row(
        Modifier
            .fillMaxWidth()
            .padding(start = 4.dp, end = 4.dp, top = 8.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            title,
            style = MaterialTheme.typography.titleSmall,
            color = cs.onSurface,
        )
        Spacer(Modifier.width(10.dp))
        HorizontalDivider(Modifier.weight(1f), color = cs.outlineVariant)
    }
}

@Composable
private fun DriveTile(v: Volume, modifier: Modifier) {
    val cs = MaterialTheme.colorScheme
    val total = remember(v) { v.total }
    val free = remember(v) { v.free }
    val frac = if (total > 0) ((total - free).toFloat() / total).coerceIn(0f, 1f) else 0f
    Row(
        modifier
            .fillMaxWidth()
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            if (v.removable) ExplorerIcons.SdCard else ExplorerIcons.Drive,
            contentDescription = null,
            modifier = Modifier.size(46.dp),
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                "${v.label} (${v.letter}:)",
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(6.dp))
            UsageBar(frac, if (frac > 0.9f) cs.error else cs.primary, 8.dp)
            Spacer(Modifier.height(4.dp))
            Text(
                "Свободно ${Fmt.size(free)} из ${Fmt.size(total)}",
                style = MaterialTheme.typography.bodySmall,
                color = cs.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun UsageBar(fraction: Float, color: Color, height: Dp) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(height / 2))
            .background(MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Box(
            Modifier
                .fillMaxHeight()
                .fillMaxWidth(fraction)
                .clip(RoundedCornerShape(height / 2))
                .background(color),
        )
    }
}
