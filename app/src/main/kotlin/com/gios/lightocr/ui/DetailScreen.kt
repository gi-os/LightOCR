package com.gios.lightocr.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import coil.compose.rememberAsyncImagePainter
import com.gios.lightocr.core.OcrTextUtils
import com.gios.lightocr.core.ScanNaming
import com.gios.lightocr.data.ScanEntity
import com.gios.lightocr.ui.theme.LightIcons
import com.gios.lightocr.ui.theme.LightText
import com.gios.lightocr.ui.theme.LightTextVariant
import com.gios.lightocr.ui.theme.gridUnitsAsDp

/** A past scan: its thumbnail, the full recognized text, and a copy button. */
@Composable
fun DetailScreen(vm: ScanViewModel, scanId: String, onBack: () -> Unit) {
    val history by vm.history.collectAsState()
    val scan = history.firstOrNull { it.id == scanId }
    val clipboard = LocalClipboardManager.current

    Column(Modifier.fillMaxSize()) {
        LightTopBar(title = "Scan", onBack = onBack)
        if (scan == null) {
            EmptyState(text = "This scan is gone", detail = "It may have just been deleted.")
        } else {
            DetailBody(
                scan = scan,
                thumbnailFile = vm.thumbnailFile(scan),
                onCopy = { clipboard.setText(AnnotatedString(scan.text)) },
            )
        }
    }
}

@Composable
private fun DetailBody(scan: ScanEntity, thumbnailFile: java.io.File, onCopy: () -> Unit) {
    Column(
        Modifier
            .weight(1f, fill = true)
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 2f.gridUnitsAsDp(), vertical = 1f.gridUnitsAsDp()),
    ) {
        Image(
            painter = rememberAsyncImagePainter(model = thumbnailFile),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(4f / 3f)
                .background(androidx.compose.ui.graphics.Color(0xFF1A1A1A)),
        )
        LightText(
            text = ScanNaming.formatTimestamp(scan.createdAt),
            variant = LightTextVariant.Detail,
            lighten = true,
            modifier = Modifier.padding(top = 1f.gridUnitsAsDp(), bottom = 1f.gridUnitsAsDp()),
        )
        if (OcrTextUtils.isEmptyResult(scan.text)) {
            LightText("No text found in this photo.", LightTextVariant.Paragraph, lighten = true)
        } else {
            LightText(scan.text, LightTextVariant.Copy)
        }
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 1f.gridUnitsAsDp()),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        ChromeIcon(icon = LightIcons.Copy, onClick = onCopy, enabled = !OcrTextUtils.isEmptyResult(scan.text))
    }
}
