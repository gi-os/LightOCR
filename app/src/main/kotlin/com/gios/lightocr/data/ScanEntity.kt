package com.gios.lightocr.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * One past scan. [thumbnailFileName] names a downscaled JPEG under
 * `filesDir/scans/` -- images live on disk, never as a DB blob, the same pattern every
 * other Light* app uses for photos it needs to keep.
 */
@Entity(tableName = "scans")
data class ScanEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "thumbnail_file") val thumbnailFileName: String,
    @ColumnInfo(name = "text") val text: String,
    @ColumnInfo(name = "created_at") val createdAt: Long,
)
