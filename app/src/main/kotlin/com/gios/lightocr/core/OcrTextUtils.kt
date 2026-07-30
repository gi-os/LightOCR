package com.gios.lightocr.core

/**
 * Pure-Kotlin text cleanup for whatever ML Kit hands back, deliberately free of any
 * android.* import. That is not a style preference: the sandbox this app is developed in
 * cannot run a full Android build (no working aapt2/emulator), so anything that can be
 * verified without one is written to allow it -- these functions compile and test with a
 * standalone `kotlinc` + JUnit, no Gradle/Android SDK required.
 */
object OcrTextUtils {

    /**
     * Trims leading/trailing whitespace off every line, collapses runs of blank lines to at most
     * one, and trims leading/trailing blank lines from the whole block.
     *
     * ML Kit's [text recognition result](Text.getText) already joins lines with "\n", but
     * on a photo with a lot of empty margin it is common to get several blank lines in a
     * row where the detector found a text block with nothing legible in it.
     */
    fun cleanRecognizedText(raw: String): String {
        val lines = raw.replace("\r\n", "\n").split("\n").map { it.trim() }
        val collapsed = mutableListOf<String>()
        var lastBlank = false
        for (line in lines) {
            val isBlank = line.isBlank()
            if (isBlank && lastBlank) continue
            collapsed.add(line)
            lastBlank = isBlank
        }
        while (collapsed.isNotEmpty() && collapsed.first().isBlank()) collapsed.removeAt(0)
        while (collapsed.isNotEmpty() && collapsed.last().isBlank()) collapsed.removeAt(collapsed.size - 1)
        return collapsed.joinToString("\n")
    }

    /**
     * A one-line preview for a history row: the first non-blank line, truncated, or a
     * placeholder if the scan found no text at all.
     */
    fun snippet(text: String, maxLen: Int = 80): String {
        val firstLine = text.lineSequence().firstOrNull { it.isNotBlank() }?.trim()
        if (firstLine.isNullOrEmpty()) return "(no text found)"
        return if (firstLine.length <= maxLen) firstLine else firstLine.take(maxLen - 1).trimEnd() + "…"
    }

    /** Whether a scan is worth keeping a "no text found" placeholder view for. */
    fun isEmptyResult(text: String): Boolean = text.isBlank()
}
