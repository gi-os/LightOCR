package com.gios.lightocr.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import com.gios.lightocr.core.ScanNaming
import java.io.File
import kotlin.math.max

/**
 * Downscaled JPEG thumbnails for the history list, kept in `filesDir/scans/` rather than
 * in the database -- the same pattern every other Light* app uses for images it needs to
 * keep around. Room only ever stores the filename.
 */
class ThumbnailStore(context: Context) {
    private val dir: File = File(context.filesDir, "scans").apply { mkdirs() }

    /** Longest edge a stored thumbnail is allowed to have -- plenty for a history row. */
    private val maxEdge = 640

    fun save(id: String, bitmap: Bitmap): String {
        val fileName = ScanNaming.thumbnailFileName(id)
        val scaled = scaleDown(bitmap, maxEdge)
        File(dir, fileName).outputStream().use { out ->
            scaled.compress(Bitmap.CompressFormat.JPEG, 85, out)
        }
        if (scaled !== bitmap) scaled.recycle()
        return fileName
    }

    fun file(fileName: String): File = File(dir, fileName)

    fun delete(fileName: String) {
        runCatching { File(dir, fileName).delete() }
    }

    private fun scaleDown(bitmap: Bitmap, edge: Int): Bitmap {
        val longest = max(bitmap.width, bitmap.height)
        if (longest <= edge) return bitmap
        val scale = edge.toFloat() / longest
        val matrix = Matrix().apply { postScale(scale, scale) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }
}
