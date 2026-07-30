package com.gios.lightocr.core

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Naming and formatting helpers kept free of android.* imports for the same reason as
 * [OcrTextUtils]: they are the part of the history feature that can be verified inside a
 * sandbox with no Android build tooling.
 */
object ScanNaming {

    /** The on-disk thumbnail filename for a scan, stored under filesDir/scans/. */
    fun thumbnailFileName(id: String): String = "scan_$id.jpg"

    /** A short label for a history row, e.g. "30 Jul 2026, 14:03". */
    fun formatTimestamp(epochMillis: Long, zone: ZoneId = ZoneId.systemDefault()): String {
        val formatter = DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm")
        return formatter.format(Instant.ofEpochMilli(epochMillis).atZone(zone))
    }
}
