package com.arthuralend.explorer.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.arthuralend.explorer.data.Category
import com.arthuralend.explorer.data.FileItem
import com.arthuralend.explorer.data.FileType
import com.arthuralend.explorer.data.FileTypes
import com.arthuralend.explorer.data.FolderKind
import java.util.Locale

private fun PathBuilder.roundRect(l: Float, t: Float, r: Float, b: Float, rad: Float) {
    moveTo(l + rad, t)
    lineTo(r - rad, t)
    quadTo(r, t, r, t + rad)
    lineTo(r, b - rad)
    quadTo(r, b, r - rad, b)
    lineTo(l + rad, b)
    quadTo(l, b, l, b - rad)
    lineTo(l, t + rad)
    quadTo(l, t, l + rad, t)
    close()
}

private fun PathBuilder.circle(cx: Float, cy: Float, r: Float) {
    moveTo(cx - r, cy)
    arcTo(r, r, 0f, true, true, cx + r, cy)
    arcTo(r, r, 0f, true, true, cx - r, cy)
    close()
}

private fun icon(name: String, block: ImageVector.Builder.() -> Unit): ImageVector =
    ImageVector.Builder(
        name = name,
        defaultWidth = 48.dp,
        defaultHeight = 48.dp,
        viewportWidth = 48f,
        viewportHeight = 48f,
    ).apply(block).build()

/** Векторные иконки в духе Windows 11. */
object ExplorerIcons {
    val Folder: ImageVector by lazy {
        icon("Folder") {
            // Задняя стенка с «ушком»
            path(fill = SolidColor(Color(0xFFE9A42F))) {
                moveTo(4f, 11f)
                quadTo(4f, 8f, 7f, 8f)
                lineTo(17f, 8f)
                quadTo(18.5f, 8f, 19.5f, 9f)
                lineTo(22f, 12f)
                lineTo(41f, 12f)
                quadTo(44f, 12f, 44f, 15f)
                lineTo(44f, 37f)
                quadTo(44f, 40f, 41f, 40f)
                lineTo(7f, 40f)
                quadTo(4f, 40f, 4f, 37f)
                close()
            }
            // Лист бумаги внутри
            path(fill = SolidColor(Color(0xFFFFFDF7))) { roundRect(8f, 14.5f, 40f, 26f, 1f) }
            // Передняя стенка
            path(
                fill = Brush.verticalGradient(
                    listOf(Color(0xFFFFDA6A), Color(0xFFFFC23D)),
                    startY = 18f,
                    endY = 40f,
                ),
            ) {
                moveTo(4f, 21f)
                quadTo(4f, 18f, 7f, 18f)
                lineTo(41f, 18f)
                quadTo(44f, 18f, 44f, 21f)
                lineTo(44f, 37f)
                quadTo(44f, 40f, 41f, 40f)
                lineTo(7f, 40f)
                quadTo(4f, 40f, 4f, 37f)
                close()
            }
            path(fill = SolidColor(Color(0x80FFF3C4))) { roundRect(5f, 18.6f, 43f, 19.6f, 0.5f) }
        }
    }

    val Page: ImageVector by lazy {
        icon("Page") {
            path(
                fill = SolidColor(Color(0xFFFFFFFF)),
                stroke = SolidColor(Color(0xFFA8A59B)),
                strokeLineWidth = 1.5f,
            ) {
                moveTo(13f, 4f)
                lineTo(29f, 4f)
                lineTo(38f, 13f)
                lineTo(38f, 41f)
                quadTo(38f, 44f, 35f, 44f)
                lineTo(13f, 44f)
                quadTo(10f, 44f, 10f, 41f)
                lineTo(10f, 7f)
                quadTo(10f, 4f, 13f, 4f)
                close()
            }
            path(
                fill = SolidColor(Color(0xFFE6E3D8)),
                stroke = SolidColor(Color(0xFFA8A59B)),
                strokeLineWidth = 1.5f,
            ) {
                moveTo(29f, 4f)
                lineTo(29f, 10f)
                quadTo(29f, 13f, 32f, 13f)
                lineTo(38f, 13f)
                close()
            }
        }
    }

    val Drive: ImageVector by lazy {
        icon("Drive") {
            path(
                fill = Brush.verticalGradient(
                    listOf(Color(0xFFEDEBE3), Color(0xFFC9C6BB)),
                    startY = 16f,
                    endY = 30f,
                ),
            ) { roundRect(4f, 16f, 44f, 34f, 3f) }
            path(fill = SolidColor(Color(0xFF9E9B90))) {
                moveTo(4f, 28f)
                lineTo(44f, 28f)
                lineTo(44f, 31f)
                quadTo(44f, 34f, 41f, 34f)
                lineTo(7f, 34f)
                quadTo(4f, 34f, 4f, 31f)
                close()
            }
            path(fill = SolidColor(Color(0xFF4FB06A))) { circle(38.5f, 31f, 1.4f) }
            // «Окно» на системном диске
            path(fill = SolidColor(Color(0xFFD97757))) { roundRect(8f, 19.5f, 11.5f, 23f, 0.4f) }
            path(fill = SolidColor(Color(0xFFE8A48A))) { roundRect(12f, 19.5f, 15.5f, 23f, 0.4f) }
            path(fill = SolidColor(Color(0xFFE8A48A))) { roundRect(8f, 23.5f, 11.5f, 27f, 0.4f) }
            path(fill = SolidColor(Color(0xFFD97757))) { roundRect(12f, 23.5f, 15.5f, 27f, 0.4f) }
        }
    }

    val SdCard: ImageVector by lazy {
        icon("SdCard") {
            path(fill = SolidColor(Color(0xFF3D3D3A))) {
                moveTo(19f, 6f)
                lineTo(34f, 6f)
                quadTo(37f, 6f, 37f, 9f)
                lineTo(37f, 39f)
                quadTo(37f, 42f, 34f, 42f)
                lineTo(14f, 42f)
                quadTo(11f, 42f, 11f, 39f)
                lineTo(11f, 14f)
                close()
            }
            for (x in listOf(19f, 23f, 27f, 31f)) {
                path(fill = SolidColor(Color(0xFFE0B04A))) { roundRect(x, 8.5f, x + 2.2f, 15f, 0.4f) }
            }
            path(fill = SolidColor(Color(0xFFD97757))) { roundRect(14f, 26f, 34f, 38f, 1.5f) }
        }
    }

    val ThisPc: ImageVector by lazy {
        icon("ThisPc") {
            path(fill = SolidColor(Color(0xFF3D3D3A))) { roundRect(4f, 8f, 44f, 34f, 3f) }
            path(
                fill = Brush.linearGradient(
                    listOf(Color(0xFFF0B8A2), Color(0xFFD97757)),
                    start = androidx.compose.ui.geometry.Offset(6f, 10f),
                    end = androidx.compose.ui.geometry.Offset(42f, 32f),
                ),
            ) { roundRect(6.5f, 10.5f, 41.5f, 31.5f, 1f) }
            path(fill = SolidColor(Color(0xFF8A877D))) {
                moveTo(20f, 34f)
                lineTo(28f, 34f)
                lineTo(29f, 39f)
                lineTo(19f, 39f)
                close()
            }
            path(fill = SolidColor(Color(0xFFA8A59B))) { roundRect(14f, 38.5f, 34f, 41.5f, 1.5f) }
        }
    }
}

fun FolderKind.glyph(): ImageVector = when (this) {
    FolderKind.DOWNLOADS -> Icons.Filled.Download
    FolderKind.DOCUMENTS -> Icons.Filled.Description
    FolderKind.PICTURES -> Icons.Filled.Image
    FolderKind.CAMERA -> Icons.Filled.PhotoCamera
    FolderKind.MUSIC -> Icons.Filled.MusicNote
    FolderKind.VIDEOS -> Icons.Filled.Movie
}

@Composable
fun FolderIcon(size: Dp, kind: FolderKind? = null, modifier: Modifier = Modifier) {
    Box(modifier.size(size)) {
        Image(ExplorerIcons.Folder, contentDescription = null, modifier = Modifier.fillMaxSize())
        if (kind != null) {
            Icon(
                kind.glyph(),
                contentDescription = null,
                tint = Color(0xFF9A5A12),
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(y = size * 0.13f)
                    .size(size * 0.36f),
            )
        }
    }
}

@Composable
fun PageIcon(size: Dp, type: FileType, ext: String, modifier: Modifier = Modifier) {
    Box(modifier.size(size)) {
        Image(ExplorerIcons.Page, contentDescription = null, modifier = Modifier.fillMaxSize())
        val label = ext.uppercase(Locale.ROOT).take(4)
        if (label.isNotEmpty()) {
            val fs = (size.value * 0.19f).coerceAtLeast(5f).sp
            Box(
                Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = size * 0.06f, bottom = size * 0.2f)
                    .background(type.color, RoundedCornerShape(size * 0.06f))
                    .padding(horizontal = size * 0.05f, vertical = size * 0.015f),
            ) {
                Text(
                    label,
                    color = Color.White,
                    fontSize = fs,
                    lineHeight = fs,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false)),
                )
            }
        }
    }
}

@Composable
fun ItemIcon(item: FileItem, size: Dp, kind: FolderKind? = null, modifier: Modifier = Modifier) {
    if (item.isDir) {
        FolderIcon(size, kind, modifier)
        return
    }
    val type = FileTypes.of(item.ext)
    if (type.category == Category.IMAGE && item.ext != "svg") {
        val placeholder = rememberVectorPainter(ExplorerIcons.Page)
        AsyncImage(
            model = item.file,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            placeholder = placeholder,
            error = placeholder,
            modifier = modifier
                .size(size)
                .clip(RoundedCornerShape(size * 0.14f)),
        )
    } else {
        PageIcon(size, type, item.ext, modifier)
    }
}
