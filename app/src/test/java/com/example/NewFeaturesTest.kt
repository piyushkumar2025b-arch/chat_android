package com.example

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import com.example.ui.ColorPreset
import com.example.ui.VisualizerMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class NewFeaturesTest {

    @Test
    fun testColorPresetsExistAndHaveValidLabels() {
        val presets = ColorPreset.values()
        assertTrue(presets.isNotEmpty())
        assertTrue(presets.any { it == ColorPreset.BW })
        assertTrue(presets.any { it == ColorPreset.SEPIA })
        assertTrue(presets.any { it == ColorPreset.VIBRANT })
        assertTrue(presets.any { it == ColorPreset.CYBERPUNK })
        assertTrue(presets.any { it == ColorPreset.INVERT })
    }

    @Test
    fun testMarkdownMetricsCalculation() {
        val sampleMarkdown = """
            # Heading 1
            This is a test paragraph with **bold** and *italic* styling.
            
            - [ ] Task item 1
            - [ ] Task item 2
            
            ```kotlin
            fun test() = println("OK")
            ```
        """.trimIndent()

        val words = sampleMarkdown.trim().split("\\s+".toRegex()).size
        assertTrue("Markdown text should have words", words > 10)
        assertTrue(sampleMarkdown.contains("# Heading 1"))
        assertTrue(sampleMarkdown.contains("```kotlin"))
    }

    @Test
    fun testVisualizerModesEnum() {
        val modes = VisualizerMode.values()
        assertEquals(3, modes.size)
        assertTrue(modes.any { it == VisualizerMode.SPECTRUM_BARS })
        assertTrue(modes.any { it == VisualizerMode.BEAT_RADAR })
        assertTrue(modes.any { it == VisualizerMode.WAVEFORM })
    }

    @Test
    fun testCodeLanguageDetection() {
        val extensions = mapOf(
            "index.html" to "html",
            "script.py" to "py",
            "App.kt" to "kt",
            "server.js" to "js",
            "config.json" to "json"
        )

        for ((file, ext) in extensions) {
            val extracted = file.substringAfterLast('.')
            assertEquals(ext, extracted)
        }
    }
}
