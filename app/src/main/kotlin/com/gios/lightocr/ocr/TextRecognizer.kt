package com.gios.lightocr.ocr

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.gios.lightocr.core.OcrTextUtils
import kotlinx.coroutines.tasks.await

/**
 * Wraps ML Kit's **bundled** text recognizer -- `com.google.mlkit:text-recognition`, not
 * `com.google.android.gms:play-services-mlkit-text-recognition`. The bundled variant ships
 * its model inside the APK and never talks to Google Play Services at runtime, which is
 * the only kind of on-device ML that works on the LPIII. Latin script only: the phone has
 * no reason to carry the extra CJK/Devanagari/Korean/Japanese model files.
 */
class OcrTextRecognizer {
    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    /** Runs recognition on an already-upright bitmap and returns cleaned-up text. */
    suspend fun recognize(bitmap: Bitmap): String {
        val image = InputImage.fromBitmap(bitmap, 0)
        val result = recognizer.process(image).await()
        return OcrTextUtils.cleanRecognizedText(result.text)
    }

    fun close() {
        recognizer.close()
    }
}
