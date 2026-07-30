package com.gios.lightocr

import com.gios.lightocr.core.ScanNaming
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.ZoneOffset

class ScanNamingTest {

    @Test
    fun `thumbnail file name is derived from id`() {
        assertEquals("scan_abc123.jpg", ScanNaming.thumbnailFileName("abc123"))
    }

    @Test
    fun `formats timestamp deterministically in a fixed zone`() {
        // 2026-07-30T14:03:00Z
        val epochMillis = 1785420180000L
        assertEquals(
            "30 Jul 2026, 14:03",
            ScanNaming.formatTimestamp(epochMillis, ZoneOffset.UTC),
        )
    }
}
