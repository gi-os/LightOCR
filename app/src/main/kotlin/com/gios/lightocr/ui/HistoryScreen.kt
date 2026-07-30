package com.gios.lightocr.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.gios.lightocr.core.OcrTextUtils
import com.gios.lightocr.core.ScanNaming
import com.gios.lightocr.data.ScanEntity
import com.gios.lightocr.ui.theme.LightIcons
import com.gios.lightocr.ui.theme.LightText
import com.gios.lightocr.ui.theme.LightTextVariant
import com.gios.lightocr.ui.theme.gridUnitsAsDp
import com.gios.lightocr.ui.theme.lightClickable

@Composable
fun HistoryScreen(vm: ScanViewModel, onOpen: (ScanEntity) -> Unit) {
    val history by vm.history.collectAsState()

    Column(Modifier.fillMaxSize()) {
        LightTopBar(title = "History")
        if (history.isEmpty()) {
            EmptyState(
                text = "No scans yet",
                detail = "Take a photo or pick one from your files on the Scan tab.",
            )
        } else {
            LazyColumn(Modifier.fillMaxSize()) {
                items(history, key = { it.id }) { scan ->
                    HistoryRow(
                        scan = scan,
                        thumbnailFile = vm.thumbnailFile(scan),
                        onOpen = { onOpen(scan) },
                        onDelete = { vm.delete(scan) },
                    )
                }
            }
        }
    }
}

@Composable
private fun HistoryRow(
    scan: ScanEntity,
    thumbnailFile: java.io.File,
    onOpen: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .lightClickable { onOpen() }
            .padding(horizontal = 2f.gridUnitsAsDp(), vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Image(
            painter = rememberAsyncImagePainter(model = thumbnailFile),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(4f.gridUnitsAsDp())
                .clip(RoundedCornerShape(2.dp))
                .background(androidx.compose.ui.graphics.Color(0xFF1A1A1A)),
        )
        Column(Modifier.weight(1f)) {
            LightText(
                text = OcrTextUtils.snippet(scan.text),
                variant = LightTextVariant.Copy,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            LightText(
                text = ScanNaming.formatTimestamp(scan.createdAt),
                variant = LightTextVariant.Detail,
                lighten = true,
            )
        }
        ChromeIcon(icon = LightIcons.Trash, onClick = onDelete)
    }
}
