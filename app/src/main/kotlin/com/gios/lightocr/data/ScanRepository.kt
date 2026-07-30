package com.gios.lightocr.data

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import com.gios.lightocr.ocr.OcrTextRecognizer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.util.UUID

class ScanRepository(private val context: Context) {
    private val dao = ScanDatabase.get(context).scanDao()
    private val thumbnails = ThumbnailStore(context)
    private val recognizer = OcrTextRecognizer()

    fun observeAll(): Flow<List<ScanEntity>> = dao.observeAll()
    fun observeOne(id: String): Flow<ScanEntity?> = dao.observeOne(id)

    suspend fun delete(scan: ScanEntity) = withContext(Dispatchers.IO) {
        thumbnails.delete(scan.thumbnailFileName)
        dao.delete(scan)
    }

    fun thumbnailFile(scan: ScanEntity) = thumbnails.file(scan.thumbnailFileName)

    /** Runs OCR on a bitmap fresh off the camera, saves a thumbnail, and records the scan. */
    suspend fun processCapturedBitmap(bitmap: Bitmap): ScanEntity = withContext(Dispatchers.Default) {
        val text = recognizer.recognize(bitmap)
        withContext(Dispatchers.IO) { store(bitmap, text) }
    }

    /**
     * Runs OCR on a photo picked through the Storage Access Framework
     * (`ACTION_OPEN_DOCUMENT`). The picker hands back a scoped `content://` Uri with no
     * runtime permission needed -- deliberately not the Android Photo Picker or
     * `READ_MEDIA_IMAGES`, since the Photo Picker's implementation on some OS builds
     * depends on a Google Play system module this phone doesn't have.
     */
    suspend fun processPickedDocument(uri: Uri): ScanEntity = withContext(Dispatchers.IO) {
        val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: throw IllegalStateException("could not open picked document")
        val bitmap = decodeUpright(bytes)
        val text = withContext(Dispatchers.Default) { recognizer.recognize(bitmap) }
        store(bitmap, text)
    }

    private suspend fun store(bitmap: Bitmap, text: String): ScanEntity {
        val id = UUID.randomUUID().toString()
        val fileName = thumbnails.save(id, bitmap)
        val scan = ScanEntity(
            id = id,
            thumbnailFileName = fileName,
            text = text,
            createdAt = System.currentTimeMillis(),
        )
        dao.insert(scan)
        return scan
    }

    /** Decodes a picked image and applies its EXIF orientation, since a gallery photo may not be upright. */
    private fun decodeUpright(bytes: ByteArray): Bitmap {
        val bitmap = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        val orientation = runCatching {
            ExifInterface(java.io.ByteArrayInputStream(bytes))
                .getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        }.getOrDefault(ExifInterface.ORIENTATION_NORMAL)

        val degrees = when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90
            ExifInterface.ORIENTATION_ROTATE_180 -> 180
            ExifInterface.ORIENTATION_ROTATE_270 -> 270
            else -> 0
        }
        if (degrees == 0) return bitmap
        val matrix = android.graphics.Matrix().apply { postRotate(degrees.toFloat()) }
        val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        if (rotated !== bitmap) bitmap.recycle()
        return rotated
    }

    fun close() = recognizer.close()
}
