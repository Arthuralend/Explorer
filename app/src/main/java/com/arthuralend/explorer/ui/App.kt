package com.arthuralend.explorer.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DriveFileMove
import androidx.compose.material.icons.filled.DriveFileRenameOutline
import androidx.compose.material.icons.filled.FileCopy
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LibraryAddCheck
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.arthuralend.explorer.DialogState
import com.arthuralend.explorer.ExplorerViewModel
import com.arthuralend.explorer.data.SortMode
import kotlinx.coroutines.flow.collectLatest

@Composable
fun ExplorerApp(vm: ExplorerViewModel, hasPermission: Boolean, onRequestPermission: () -> Unit) {
    if (!hasPermission) {
        PermissionScreen(onRequestPermission)
        return
    }
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(vm) { vm.messages.collectLatest { snackbar.showSnackbar(it) } }

    val active = vm.active
    BackHandler(enabled = active.multiSelect || active.canBack) {
        if (active.multiSelect) active.clearSelection() else active.goBack()
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { ExplorerTopBar(vm) },
        bottomBar = { CommandBar(vm) },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        BoxWithConstraints(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(horizontal = 8.dp, vertical = 4.dp),
        ) {
            if (maxWidth > maxHeight) {
                Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Pane(vm.left, vm, vm.activeLeft, Modifier.weight(1f).fillMaxHeight())
                    Pane(vm.right, vm, !vm.activeLeft, Modifier.weight(1f).fillMaxHeight())
                }
            } else {
                Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Pane(vm.left, vm, vm.activeLeft, Modifier.weight(1f).fillMaxWidth())
                    Pane(vm.right, vm, !vm.activeLeft, Modifier.weight(1f).fillMaxWidth())
                }
            }
        }
    }

    ExplorerDialogs(vm)
    BusyDialog(vm)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExplorerTopBar(vm: ExplorerViewModel) {
    val landscape = LocalConfiguration.current.screenWidthDp > LocalConfiguration.current.screenHeightDp
    TopAppBar(
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                FolderIcon(30.dp)
                Spacer(Modifier.width(10.dp))
                Text(
                    "Проводник",
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Medium,
                    style = MaterialTheme.typography.titleLarge,
                )
            }
        },
        actions = {
            IconButton(onClick = { vm.mirror() }) {
                Icon(Icons.Filled.Sync, contentDescription = "Открыть эту папку в другой панели")
            }
            IconButton(onClick = { vm.swapPanes() }) {
                Icon(
                    if (landscape) Icons.Filled.SwapHoriz else Icons.Filled.SwapVert,
                    contentDescription = "Поменять панели местами",
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
    )
}

// ---------------- Командная панель (как в Windows 11) ----------------

@Composable
private fun CommandBar(vm: ExplorerViewModel) {
    val cs = MaterialTheme.colorScheme
    val p = vm.active
    val inFolder = p.dir != null
    val sel = p.selectedFiles()
    val hasSel = inFolder && sel.isNotEmpty()
    var more by remember { mutableStateOf(false) }

    Surface(color = cs.surfaceContainer) {
        Column(Modifier.navigationBarsPadding()) {
            HorizontalDivider(color = cs.outlineVariant)
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CmdButton(Icons.Filled.CreateNewFolder, "Новая папка", inFolder) {
                    vm.dialog = DialogState.NewFolder(p)
                }
                CmdButton(Icons.Filled.ContentCut, "Вырезать", hasSel) { vm.cut(sel) }
                CmdButton(Icons.Filled.ContentCopy, "Копировать", hasSel) { vm.copy(sel) }
                CmdButton(Icons.Filled.ContentPaste, "Вставить", inFolder && vm.clipboard != null) { vm.paste(p) }
                CmdButton(Icons.Filled.DriveFileRenameOutline, "Переименовать", inFolder && sel.size == 1) {
                    vm.dialog = DialogState.Rename(sel.first())
                }
                CmdButton(Icons.Filled.Delete, "Удалить", hasSel) { vm.dialog = DialogState.Delete(sel) }
                CmdButton(Icons.Filled.LibraryAddCheck, "Выделение", inFolder, highlighted = p.multiSelect) {
                    if (p.multiSelect) p.clearSelection() else p.multiSelect = true
                }
                Box {
                    CmdButton(Icons.Filled.MoreVert, "Ещё", true) { more = true }
                    DropdownMenu(expanded = more, onDismissRequest = { more = false }) {
                        fun act(block: () -> Unit) {
                            more = false
                            block()
                        }
                        MenuItem("Копировать в другую панель", Icons.Filled.FileCopy) {
                            act { vm.transferToOther(p, sel, move = false) }
                        }
                        MenuItem("Переместить в другую панель", Icons.Filled.DriveFileMove) {
                            act { vm.transferToOther(p, sel, move = true) }
                        }
                        MenuItem("Выделить всё", Icons.Filled.SelectAll) { act { p.selectAll() } }
                        MenuItem("Поделиться", Icons.Filled.Share) { act { vm.share(sel) } }
                        MenuItem("Свойства", Icons.Filled.Info) {
                            act {
                                val files = if (sel.isNotEmpty()) sel else listOfNotNull(p.dir)
                                if (files.isNotEmpty()) vm.dialog = DialogState.Properties(files)
                            }
                        }
                        HorizontalDivider()
                        SortMode.entries.forEach { mode ->
                            MenuItem(
                                "Сортировка: ${mode.label}",
                                if (mode == SortMode.NAME) Icons.AutoMirrored.Filled.Sort else null,
                                trailing = if (p.sort == mode) {
                                    { Text(if (p.ascending) "↑" else "↓", color = cs.primary) }
                                } else null,
                            ) { act { p.changeSort(mode) } }
                        }
                        HorizontalDivider()
                        MenuItem(
                            if (p.grid) "Вид: таблица" else "Вид: крупные значки",
                            if (p.grid) Icons.AutoMirrored.Filled.ViewList else Icons.Filled.GridView,
                        ) { act { vm.setGrid(p, !p.grid) } }
                        MenuItem("Скрытые элементы", Icons.Filled.Visibility, trailing = checkMark(vm.showHidden)) {
                            act { vm.toggleHidden() }
                        }
                        MenuItem("Открывать одним нажатием", Icons.Filled.TouchApp, trailing = checkMark(vm.oneTapOpen)) {
                            act { vm.toggleOneTap() }
                        }
                    }
                }
            }
        }
    }
}

private val CheckIcon: @Composable () -> Unit = {
    Icon(Icons.Filled.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
}

private fun checkMark(on: Boolean): (@Composable () -> Unit)? = if (on) CheckIcon else null

@Composable
private fun CmdButton(
    icon: ImageVector,
    label: String,
    enabled: Boolean,
    highlighted: Boolean = false,
    onClick: () -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .size(42.dp)
            .clip(CircleShape)
            .background(if (highlighted) cs.primary.copy(alpha = 0.18f) else Color.Transparent),
    ) {
        Icon(
            icon,
            contentDescription = label,
            tint = if (highlighted) cs.primary else cs.onSurface,
            modifier = Modifier
                .size(22.dp)
                .alpha(if (enabled) 1f else 0.35f),
        )
    }
}

// ---------------- Прочее ----------------

@Composable
private fun BusyDialog(vm: ExplorerViewModel) {
    val title = vm.busy ?: return
    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
    ) {
        Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh) {
            Column(
                Modifier
                    .padding(24.dp)
                    .widthIn(min = 260.dp),
            ) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(14.dp))
                LinearProgressIndicator(Modifier.fillMaxWidth())
                Spacer(Modifier.height(10.dp))
                Text(
                    vm.busyDetail,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun PermissionScreen(onRequest: () -> Unit) {
    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(32.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            FolderIcon(112.dp)
            Spacer(Modifier.height(20.dp))
            Text(
                "Нужен доступ к файлам",
                style = MaterialTheme.typography.headlineSmall,
                fontFamily = FontFamily.Serif,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(10.dp))
            Text(
                "Чтобы показывать и изменять папки на телефоне и SD-карте, разрешите приложению доступ ко всем файлам.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(24.dp))
            Button(onClick = onRequest) { Text("Предоставить доступ") }
        }
    }
}
