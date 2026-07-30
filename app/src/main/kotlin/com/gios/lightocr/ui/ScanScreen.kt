package com.gios.lightocr.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.viewinterop.AndroidView
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import com.gios.lightocr.camera.CameraCapture
import com.gios.lightocr.core.OcrTextUtils
import com.gios.lightocr.ui.theme.LightIcons
import com.gios.lightocr.ui.theme.LightText
import com.gios.lightocr.ui.theme.LightTextVariant
import com.gios.lightocr.ui.theme.LightThemeTokens
import com.gios.lightocr.ui.theme.gridUnitsAsDp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * The whole capture flow: camera preview, a shutter and a gallery button, then whatever
 * text was found -- with one tap to copy it and one to go back and scan something else.
 * Every successful scan is recorded to history automatically; there is no separate "save"
 * step, since a scan you didn't want to keep costs nothing to delete from History later.
 */
@Composable
fun ScanScreen(vm: ScanViewModel) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val clipboard = LocalClipboardManager.current

    var cameraGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED,
        )
    }
    val askCamera = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted -> cameraGranted = granted }
    LaunchedEffect(Unit) { if (!cameraGranted) askCamera.launch(Manifest.permission.CAMERA) }

    // The Storage Access Framework's document picker -- deliberately not
    // ActivityResultContracts.PickVisualMedia() (the Android Photo Picker) and not
    // READ_MEDIA_IMAGES: the Photo Picker's implementation on some OS builds relies on a
    // Google Play system module, and this phone has no Google Play Services at all.
    val pickDocument = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri -> if (uri != null) vm.onPicked(uri) }

    val uiState by vm.uiState.collectAsState()

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        when (val state = uiState) {
            is ScanUiState.Result -> ResultView(
                text = state.scan.text,
                onCopy = { clipboard.setText(AnnotatedString(state.scan.text)) },
                onNewScan = vm::reset,
            )

            ScanUiState.Processing -> ProcessingView()

            is ScanUiState.Failed -> {
                EmptyState(text = "Couldn't read that photo", detail = state.message)
                LaunchedEffect(state) {
                    // Give the message a moment on screen, then return to the camera.
                    delay(2500)
                    vm.reset()
                }
            }

            ScanUiState.Ready -> {
                if (!cameraGranted) {
                    EmptyState(
                        text = "Scanner needs the camera",
                        detail = "Grant it to take a photo, or pick one from your files instead.",
                    )
                } else {
                    val camera = remember { CameraCapture(context) }
                    var ready by remember { mutableStateOf(false) }
                    DisposableEffect(Unit) { onDispose { camera.shutdown() } }
                    AndroidView(
                        factory = { ctx ->
                            PreviewView(ctx).apply {
                                camera.bind(lifecycleOwner, this) { ok -> ready = ok }
                            }
                        },
                        modifier = Modifier.fillMaxSize(),
                    )
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .background(LightThemeTokens.colors.background.copy(alpha = 0.55f))
                            .padding(vertical = 2f.gridUnitsAsDp()),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                    ) {
                        ChromeIcon(icon = LightIcons.Gallery, onClick = { pickDocument.launch(arrayOf("image/*")) })
                        ChromeIcon(
                            icon = LightIcons.Camera,
                            size = 3f.gridUnitsAsDp(),
                            enabled = ready,
                            onClick = {
                                scope.launch {
                                    runCatching { camera.capture() }.onSuccess { vm.onCaptured(it) }
                                }
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProcessingView() {
    Column(
        Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        LightText("Reading text…", LightTextVariant.Subheading)
    }
}

@Composable
private fun ResultView(text: String, onCopy: () -> Unit, onNewScan: () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        LightTopBar(title = "Scan", onBack = onNewScan)
        Column(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 2f.gridUnitsAsDp(), vertical = 1f.gridUnitsAsDp()),
        ) {
            if (OcrTextUtils.isEmptyResult(text)) {
                LightText("No text found in this photo.", LightTextVariant.Paragraph, lighten = true)
            } else {
                LightText(text, LightTextVariant.Copy)
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 1f.gridUnitsAsDp()),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            ChromeIcon(icon = LightIcons.Copy, onClick = onCopy, enabled = !OcrTextUtils.isEmptyResult(text))
        }
    }
}
