package com.gios.lightocr

import com.gios.lightocr.core.OcrTextUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OcrTextUtilsTest {

    @Test
    fun `collapses runs of blank lines to one`() {
        val raw = "Hello\n\n\n\nWorld"
        assertEquals("Hello\n\nWorld", OcrTextUtils.cleanRecognizedText(raw))
    }

    @Test
    fun `trims leading and trailing blank lines`() {
        val raw = "\n\n  Hello\nWorld  \n\n\n"
        assertEquals("Hello\nWorld", OcrTextUtils.cleanRecognizedText(raw))
    }

    @Test
    fun `normalizes windows line endings`() {
        val raw = "Line one\r\nLine two\r\n\r\n\r\nLine three"
        assertEquals("Line one\nLine two\n\nLine three", OcrTextUtils.cleanRecognizedText(raw))
    }

    @Test
    fun `snippet takes first non-blank line`() {
        val text = "\n  \nFirst real line\nSecond line"
        assertEquals("First real line", OcrTextUtils.snippet(text))
    }

    @Test
    fun `snippet truncates long lines with ellipsis`() {
        val longLine = "x".repeat(200)
        val snippet = OcrTextUtils.snippet(longLine, maxLen = 10)
        assertEquals(10, snippet.length)
        assertTrue(snippet.endsWith("…"))
    }

    @Test
    fun `snippet reports empty result`() {
        assertEquals("(no text found)", OcrTextUtils.snippet("   \n  \n"))
    }

    @Test
    fun `isEmptyResult detects blank text`() {
        assertTrue(OcrTextUtils.isEmptyResult("   \n\t"))
        assertTrue(!OcrTextUtils.isEmptyResult("hi"))
    }
}
