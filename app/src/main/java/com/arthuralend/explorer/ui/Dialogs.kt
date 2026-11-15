package com.arthuralend.explorer.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.arthuralend.explorer.DialogState
import com.arthuralend.explorer.ExplorerViewModel
import com.arthuralend.explorer.data.FileItem
import com.arthuralend.explorer.data.FileOps
import com.arthuralend.explorer.data.FileTypes
import com.arthuralend.explorer.data.Fmt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun ExplorerDialogs(vm: ExplorerViewModel) {
    val close = { vm.dialog = null }
    when (val d = vm.dialog) {
        null -> Unit
        is DialogState.NewFolder -> NameDialog(
            title = "Новая папка",
            initial = FileOps.uniqueName(d.pane.dir, "Новая папка"),
            selectBaseOnly = false,
            confirm = "Создать",
            onDismiss = close,
        ) { vm.createFolder(d.pane, it) }

        is DialogState.Rename -> NameDialog(
            title = "Переименовать",
            initial = d.file.name,
            selectBaseOnly = !d.file.isDirectory,
            confirm = "Переименовать",
            onDismiss = close,
        ) { vm.rename(d.file, it) }

        is DialogState.Delete -> DeleteDialog(d.files, onDismiss = close) {
            vm.dialog = null
            vm.delete(d.files)
        }

        is DialogState.Properties -> PropertiesDialog(d.files, onDismiss = close)
    }
}

@Composable
private fun NameDialog(
    title: String,
    initial: String,
    selectBaseOnly: Boolean,
    confirm: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> String?,
) {
    val end = if (selectBaseOnly) initial.lastIndexOf('.').takeIf { it > 0 } ?: initial.length else initial.length
    var value by remember { mutableStateOf(TextFieldValue(initial, TextRange(0, end))) }
    var error by remember { mutableStateOf<String?>(null) }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        delay(150)
        runCatching { focus.requestFocus() }
    }
    val submit = { error = onConfirm(value.text.trim()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = {
                    value = it
                    error = null
                },
                singleLine = true,
                isError = error != null,
                supportingText = error?.let { e -> { Text(e) } },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { submit() }),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focus),
            )
        },
        confirmButton = { TextButton(onClick = submit) { Text(confirm) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } },
    )
}

@Composable
private fun DeleteDialog(files: List<File>, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Filled.Delete, contentDescription = null, tint = cs.error) },
        title = { Text("Удалить безвозвратно?") },
        text = {
            Text(
                if (files.size == 1) {
                    "Элемент «${files[0].name}» будет удалён без возможности восстановления."
                } else {
                    "Выбрано ${Fmt.items(files.size)}. Они будут удалены без возможности восстановления."
                },
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = cs.error, contentColor = cs.onError),
            ) { Text("Удалить") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } },
    )
}

@Composable
private fun PropertiesDialog(files: List<File>, onDismiss: () -> Unit) {
    var stats by remember { mutableStateOf<FileOps.Stats?>(null) }
    LaunchedEffect(files) {
        stats = withContext(Dispatchers.IO) {
            files.fold(FileOps.Stats(0, 0, 0)) { acc, f -> acc + FileOps.stats(f) }
        }
    }
    val single = files.size == 1
    val first = remember(files) { FileItem.of(files[0]) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (single) ItemIcon(first, 40.dp) else FolderIcon(40.dp)
                Spacer(Modifier.width(12.dp))
                Text(
                    if (single) first.name else Fmt.items(files.size),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        },
        text = {
            Column {
                HorizontalDivider(Modifier.padding(bottom = 8.dp))
                if (single) PropRow("Тип", FileTypes.describe(first))
                PropRow("Расположение", files[0].parent ?: "/")
                val s = stats
                PropRow("Размер", s?.let { "${Fmt.size(it.size)} (${Fmt.bytes(it.size)})" } ?: "Вычисление…")
                if (files.any { it.isDirectory } && s != null) {
                    PropRow("Содержит", "${Fmt.files(s.files)}, ${Fmt.folders(s.dirs)}")
                }
                if (single) {
                    PropRow("Изменён", Fmt.date(first.modified))
                    val attrs = buildList {
                        if (!files[0].canWrite()) add("Только чтение")
                        if (first.hidden) add("Скрытый")
                    }
                    PropRow("Атрибуты", if (attrs.isEmpty()) "—" else attrs.joinToString(", "))
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("ОК") } },
    )
}

@Composable
private fun PropRow(label: String, value: String) {
    Row(Modifier.padding(vertical = 4.dp)) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(110.dp),
        )
        Text(value, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
    }
}
