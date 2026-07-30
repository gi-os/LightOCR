package com.gios.lightocr.camera

import android.content.Context
import android.graphics.BitmapFactory
import android.util.Size
import android.view.OrientationEventListener
import android.view.Surface
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.concurrent.Executors
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * A minimal CameraX wrapper: preview plus still capture, nothing else. LightOCR isn't a
 * camera app -- it just needs one photo to run text recognition on.
 *
 * Two choices carried over from `gi-os/LightCamera`, which hit both the hard way:
 *
 * - **A moderate capture resolution, not the sensor's full size.** The LPIII's sensor is
 *   50MP; asking CameraX for its default (largest) size costs a second or more of ISP
 *   readout and JPEG encode for a photo that is about to be downsampled again to feed an
 *   OCR model anyway. 2000x1500 (that sensor's 4:3 shape) is comfortably above ML Kit's own
 *   "16px per character" accuracy guidance for anything short of a full page of tiny print,
 *   and the shutter answers immediately.
 * - **`ImageCapture.targetRotation` has to track the accelerometer.** The activity is not
 *   orientation-locked in the manifest, but Compose content is easiest to reason about
 *   without the window itself rotating, so the window stays portrait and only the photo's
 *   rotation follows [OrientationEventListener] -- otherwise a photo of a landscape
 *   document, held sideways, comes back on its side.
 */
class CameraCapture(private val context: Context) {

    private var provider: ProcessCameraProvider? = null
    private var imageCapture: ImageCapture? = null
    private val captureExecutor = Executors.newSingleThreadExecutor()

    @Volatile private var lastRotation = Surface.ROTATION_0

    private val orientationListener = object : OrientationEventListener(context) {
        override fun onOrientationChanged(degrees: Int) {
            if (degrees == ORIENTATION_UNKNOWN) return
            val rotation = when (degrees) {
                in 45..134 -> Surface.ROTATION_270
                in 135..224 -> Surface.ROTATION_180
                in 225..314 -> Surface.ROTATION_90
                else -> Surface.ROTATION_0
            }
            if (rotation == lastRotation) return
            lastRotation = rotation
            imageCapture?.targetRotation = rotation
        }
    }

    fun bind(owner: LifecycleOwner, previewView: PreviewView, onReady: (Boolean) -> Unit) {
        orientationListener.enable()
        val future = ProcessCameraProvider.getInstance(context)
        future.addListener({
            val provider = runCatching { future.get() }.getOrNull()
            if (provider == null) {
                onReady(false)
                return@addListener
            }
            this.provider = provider

            val previewSelector = ResolutionSelector.Builder()
                .setAspectRatioStrategy(AspectRatioStrategy.RATIO_4_3_FALLBACK_AUTO_STRATEGY)
                .build()
            val preview = Preview.Builder()
                .setResolutionSelector(previewSelector)
                .build()
                .also { it.setSurfaceProvider(previewView.surfaceProvider) }

            // Moderate resolution: enough for OCR, not the sensor's full 50MP.
            val captureSelector = ResolutionSelector.Builder()
                .setAspectRatioStrategy(AspectRatioStrategy.RATIO_4_3_FALLBACK_AUTO_STRATEGY)
                .setResolutionStrategy(
                    ResolutionStrategy(
                        Size(2000, 1500),
                        ResolutionStrategy.FALLBACK_RULE_CLOSEST_LOWER_THEN_HIGHER,
                    ),
                )
                .build()
            val capture = ImageCapture.Builder()
                .setResolutionSelector(captureSelector)
                .setJpegQuality(92)
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .setTargetRotation(lastRotation)
                .build()
            this.imageCapture = capture

            val selector = CameraSelector.DEFAULT_BACK_CAMERA
            runCatching {
                provider.unbindAll()
                provider.bindToLifecycle(owner, selector, preview, capture)
                onReady(true)
            }.onFailure { onReady(false) }
        }, ContextCompat.getMainExecutor(context))
    }

    /** Takes a photo and decodes it straight to a [android.graphics.Bitmap], already upright. */
    suspend fun capture(): android.graphics.Bitmap = suspendCancellableCoroutine { cont ->
        val capture = imageCapture
        if (capture == null) {
            cont.resumeWithException(IllegalStateException("camera not bound"))
            return@suspendCancellableCoroutine
        }
        capture.takePicture(
            captureExecutor,
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    val result = runCatching {
                        val buffer = image.planes[0].buffer
                        val bytes = ByteArray(buffer.remaining())
                        buffer.get(bytes)
                        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                        rotateIfNeeded(bitmap, image.imageInfo.rotationDegrees)
                    }
                    image.close()
                    result.fold(
                        onSuccess = { if (cont.isActive) cont.resume(it) },
                        onFailure = { if (cont.isActive) cont.resumeWithException(it) },
                    )
                }

                override fun onError(exception: ImageCaptureException) {
                    if (cont.isActive) cont.resumeWithException(exception)
                }
            },
        )
    }

    private fun rotateIfNeeded(bitmap: android.graphics.Bitmap, degrees: Int): android.graphics.Bitmap {
        if (degrees == 0) return bitmap
        val matrix = android.graphics.Matrix().apply { postRotate(degrees.toFloat()) }
        val rotated = android.graphics.Bitmap.createBitmap(
            bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true,
        )
        if (rotated !== bitmap) bitmap.recycle()
        return rotated
    }

    fun shutdown() {
        runCatching { orientationListener.disable() }
        runCatching { provider?.unbindAll() }
        captureExecutor.shutdown()
    }
}
