package com.example.data.model

import java.util.UUID

object ArtifactExtractor {

    private val CLAUDE_ARTIFACT_REGEX = Regex(
        """<(?:antArtifact|artifact)\s+([^>]*?)>([\s\S]*?)</(?:antArtifact|artifact)>""",
        RegexOption.IGNORE_CASE
    )

    private val ATTR_REGEX = Regex("""(\w+)\s*=\s*["']([^"']*)["']""")

    private val CODE_BLOCK_REGEX = Regex(
        """```([a-zA-Z0-9_\-+]+)?(?::([^\n\r]+))?\r?\n([\s\S]*?)```"""
    )

    fun extractFromMessage(message: ChatMessageEntity): List<ArtifactItem> {
        val artifacts = mutableListOf<ArtifactItem>()
        val content = message.content
        if (content.isBlank() || message.role == "user" || message.isError) {
            return emptyList()
        }

        // 1. Check for Claude / Anthropic style XML artifacts
        val claudeMatches = CLAUDE_ARTIFACT_REGEX.findAll(content).toList()
        for ((idx, match) in claudeMatches.withIndex()) {
            val attrString = match.groupValues[1]
            val body = match.groupValues[2].trim()
            val attrs = ATTR_REGEX.findAll(attrString).associate {
                it.groupValues[1].lowercase() to it.groupValues[2]
            }

            val title = attrs["title"] ?: attrs["identifier"] ?: "Artifact ${idx + 1}"
            val typeStr = attrs["type"]?.lowercase() ?: ""
            val lang = attrs["language"]?.lowercase() ?: detectLanguageFromType(typeStr, body)

            val artifactType = when {
                typeStr.contains("html") || lang == "html" || body.contains("<!DOCTYPE html", ignoreCase = true) -> ArtifactType.HTML_WEB
                typeStr.contains("svg") || lang == "svg" || body.trimStart().startsWith("<svg", ignoreCase = true) -> ArtifactType.SVG
                typeStr.contains("markdown") || lang == "markdown" || lang == "md" -> ArtifactType.MARKDOWN
                typeStr.contains("json") || lang == "json" -> ArtifactType.JSON_DATA
                lang in listOf("bash", "sh", "shell", "zsh") -> ArtifactType.SHELL
                else -> ArtifactType.CODE
            }

            artifacts.add(
                ArtifactItem(
                    id = "${message.id}_claude_$idx",
                    messageId = message.id,
                    sessionId = message.sessionId,
                    title = title,
                    type = artifactType,
                    language = lang,
                    content = body,
                    timestamp = message.timestamp,
                    lineCount = body.lines().size,
                    charCount = body.length
                )
            )
        }

        // 2. Check for Markdown fenced code blocks
        val codeMatches = CODE_BLOCK_REGEX.findAll(content).toList()
        for ((idx, match) in codeMatches.withIndex()) {
            val rawLang = match.groupValues[1].trim().lowercase()
            val explicitFilename = match.groupValues[2].trim()
            val codeBody = match.groupValues[3].trimEnd()

            if (codeBody.isBlank() || codeBody.lines().size < 2) {
                continue
            }

            val detectedTitle = when {
                explicitFilename.isNotBlank() -> explicitFilename
                else -> detectTitleFromCode(codeBody, rawLang, idx + 1)
            }

            val type = detectArtifactType(rawLang, codeBody)

            artifacts.add(
                ArtifactItem(
                    id = "${message.id}_code_$idx",
                    messageId = message.id,
                    sessionId = message.sessionId,
                    title = detectedTitle,
                    type = type,
                    language = if (rawLang.isBlank()) "text" else rawLang,
                    content = codeBody,
                    timestamp = message.timestamp,
                    lineCount = codeBody.lines().size,
                    charCount = codeBody.length
                )
            )
        }

        return artifacts
    }

    private fun detectArtifactType(lang: String, body: String): ArtifactType {
        val trimmed = body.trimStart()
        return when {
            lang in listOf("html", "htm") || trimmed.startsWith("<!DOCTYPE html", ignoreCase = true) || trimmed.startsWith("<html", ignoreCase = true) -> ArtifactType.HTML_WEB
            lang == "svg" || trimmed.startsWith("<svg", ignoreCase = true) -> ArtifactType.SVG
            lang in listOf("bash", "sh", "zsh", "shell", "powershell") -> ArtifactType.SHELL
            lang in listOf("json", "yaml", "yml", "xml", "csv", "sql") -> ArtifactType.JSON_DATA
            lang in listOf("md", "markdown") -> ArtifactType.MARKDOWN
            else -> ArtifactType.CODE
        }
    }

    private fun detectLanguageFromType(type: String, body: String): String {
        return when {
            type.contains("html") -> "html"
            type.contains("svg") -> "svg"
            type.contains("markdown") -> "markdown"
            type.contains("json") -> "json"
            type.contains("python") -> "python"
            type.contains("javascript") -> "javascript"
            type.contains("typescript") -> "typescript"
            body.trimStart().startsWith("<!DOCTYPE html", ignoreCase = true) -> "html"
            body.trimStart().startsWith("<svg", ignoreCase = true) -> "svg"
            else -> "code"
        }
    }

    private fun detectTitleFromCode(code: String, lang: String, index: Int): String {
        val lines = code.lines()
        val firstLine = lines.firstOrNull()?.trim() ?: ""

        // Check for file annotations like // filename: App.kt or # file: script.py
        val filenameRegex = Regex("""(?:(?://|#|/\*|<!--)\s*(?:filename|file|title)?:\s*([a-zA-Z0-9_.\-]+))""", RegexOption.IGNORE_CASE)
        val filenameMatch = filenameRegex.find(firstLine)
        if (filenameMatch != null) {
            val name = filenameMatch.groupValues[1].trim()
            if (name.isNotEmpty()) return name
        }

        // Check for class declaration
        val classMatch = Regex("""(?:class|interface|object)\s+([a-zA-Z0-9_]+)""").find(code)
        if (classMatch != null) {
            val className = classMatch.groupValues[1]
            val ext = when (lang) {
                "kotlin", "kt" -> ".kt"
                "java" -> ".java"
                "python", "py" -> ".py"
                "typescript", "ts" -> ".ts"
                "javascript", "js" -> ".js"
                else -> ""
            }
            return "$className$ext"
        }

        // Check for HTML document title
        if (lang in listOf("html", "htm") || code.contains("<html", ignoreCase = true)) {
            val titleMatch = Regex("""<title>(.*?)</title>""", RegexOption.IGNORE_CASE).find(code)
            if (titleMatch != null) {
                val t = titleMatch.groupValues[1].trim()
                if (t.isNotBlank()) return "$t (index.html)"
            }
            return "index.html"
        }

        if (lang == "svg" || code.trimStart().startsWith("<svg", ignoreCase = true)) {
            return "vector_graphic.svg"
        }

        if (lang in listOf("bash", "sh", "shell")) {
            return "script.sh"
        }

        if (lang == "json") {
            return "config.json"
        }

        if (lang == "sql") {
            return "schema.sql"
        }

        val langDisplay = if (lang.isNotBlank()) lang.replaceFirstChar { it.uppercase() } else "Code"
        return "$langDisplay Artifact #$index"
    }
}
