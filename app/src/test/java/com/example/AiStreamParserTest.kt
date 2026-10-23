package com.example

import com.example.data.remote.AiService
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AiStreamParserTest {

    @Test
    fun `openai sse line extracts delta content`() {
        val line = """data: {"choices":[{"delta":{"content":"Hello there"}}]}"""
        assertEquals("Hello there", AiService.parseOpenAiSseLine(line))
    }

    @Test
    fun `openai done marker is ignored`() {
        assertNull(AiService.parseOpenAiSseLine("data: [DONE]"))
    }

    @Test
    fun `openai role-only delta is ignored`() {
        val line = """data: {"choices":[{"delta":{"role":"assistant"}}]}"""
        assertNull(AiService.parseOpenAiSseLine(line))
    }

    @Test
    fun `non data lines are ignored`() {
        assertNull(AiService.parseOpenAiSseLine("event: message"))
        assertNull(AiService.parseOpenAiSseLine(""))
        assertNull(AiService.parseOpenAiSseLine("data: "))
    }

    @Test
    fun `openai malformed payload is ignored`() {
        assertNull(AiService.parseOpenAiSseLine("data: {not-json}"))
        assertNull(AiService.parseOpenAiSseLine("""data: {"choices":[]}"""))
    }

    @Test
    fun `gemini sse line concatenates text parts`() {
        val line = """data: {"candidates":[{"content":{"parts":[{"text":"Hi "},{"text":"there"}]}}]}"""
        assertEquals("Hi there", AiService.parseGeminiSseLine(line))
    }

    @Test
    fun `gemini empty candidates are ignored`() {
        assertNull(AiService.parseGeminiSseLine("""data: {"candidates":[{"content":{"parts":[]}}]}"""))
        assertNull(AiService.parseGeminiSseLine("data: [DONE]"))
        assertNull(AiService.parseGeminiSseLine("data: {oops}"))
    }
}
