package com.example.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.StringReader
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

data class NewsArticle(
    val id: String,
    val title: String,
    val link: String,
    val source: String,
    val pubDate: String,
    val description: String,
    val category: String
)

data class WebSearchResult(
    val title: String,
    val snippet: String,
    val url: String,
    val source: String = "Web",
    val pubDate: String = ""
)

object NewsFeedService {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    enum class NewsCategory(val label: String, val rssUrl: String) {
        ALL("All News", "https://news.google.com/rss?hl=en-US&gl=US&ceid=US:en"),
        TECH("Tech & AI", "https://news.google.com/rss/headlines/section/topic/TECHNOLOGY?hl=en-US&gl=US&ceid=US:en"),
        WORLD("World", "https://news.google.com/rss/headlines/section/topic/WORLD?hl=en-US&gl=US&ceid=US:en"),
        SCIENCE("Science", "https://news.google.com/rss/headlines/section/topic/SCIENCE?hl=en-US&gl=US&ceid=US:en"),
        BUSINESS("Business", "https://news.google.com/rss/headlines/section/topic/BUSINESS?hl=en-US&gl=US&ceid=US:en")
    }

    suspend fun fetchNews(category: NewsCategory): Result<List<NewsArticle>> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(category.rssUrl)
                .header("User-Agent", "Mozilla/5.0 (Android; Mobile; rv:109.0)")
                .build()

            val body = client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Result.failure(Exception("HTTP ${response.code}: ${response.message}"))
                }
                response.body?.string().orEmpty()
            }

            val articles = parseRssFeed(body, category.label)
            Result.success(articles)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun parseRssFeed(xml: String, categoryName: String): List<NewsArticle> {
        val list = mutableListOf<NewsArticle>()
        try {
            val factory = XmlPullParserFactory.newInstance()
            factory.isNamespaceAware = false
            val parser = factory.newPullParser()
            parser.setInput(StringReader(xml))

            var eventType = parser.eventType
            var inItem = false
            var title = ""
            var link = ""
            var pubDate = ""
            var source = ""
            var description = ""

            while (eventType != XmlPullParser.END_DOCUMENT) {
                val tagName = parser.name ?: ""
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        if (tagName.equals("item", ignoreCase = true)) {
                            inItem = true
                            title = ""
                            link = ""
                            pubDate = ""
                            source = ""
                            description = ""
                        } else if (inItem) {
                            when (tagName.lowercase()) {
                                "title" -> title = parser.nextText()
                                "link" -> link = parser.nextText()
                                "pubdate" -> pubDate = parser.nextText()
                                "source" -> source = parser.nextText()
                                "description" -> description = parser.nextText()
                            }
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        if (tagName.equals("item", ignoreCase = true) && inItem) {
                            if (title.isNotBlank()) {
                                val cleanDesc = try {
                                    android.text.Html.fromHtml(description, android.text.Html.FROM_HTML_MODE_LEGACY).toString().trim()
                                } catch (_: Throwable) {
                                    description
                                        .replace(Regex("<[^>]*>"), "")
                                        .replace("&quot;", "\"")
                                        .replace("&amp;", "&")
                                        .replace("&apos;", "'")
                                        .replace("&lt;", "<")
                                        .replace("&gt;", ">")
                                        .replace("&nbsp;", " ")
                                        .replace("&#39;", "'")
                                        .trim()
                                }

                                // If source is empty, try to extract from title (e.g. "Title - Source")
                                val finalSource = if (source.isNotBlank()) {
                                    source
                                } else {
                                    val parts = title.split(" - ")
                                    if (parts.size > 1) parts.last().trim() else "Google News"
                                }
                                val finalTitle = if (title.contains(" - ")) {
                                    title.substringBeforeLast(" - ").trim()
                                } else title.trim()

                                list.add(
                                    NewsArticle(
                                        id = "${list.size}_${System.currentTimeMillis()}",
                                        title = finalTitle,
                                        link = link.trim(),
                                        source = finalSource,
                                        pubDate = formatPubDate(pubDate),
                                        description = cleanDesc,
                                        category = categoryName
                                    )
                                )
                            }
                            inItem = false
                        }
                    }
                }
                eventType = parser.next()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    private fun formatPubDate(raw: String): String {
        return try {
            // Raw typically: "Thu, 01 Oct 2026 11:30:00 GMT" -> shorten to date & time
            val parts = raw.split(" ")
            if (parts.size >= 5) {
                "${parts[1]} ${parts[2]} ${parts[4]}"
            } else raw
        } catch (_: Exception) {
            raw
        }
    }
}

object WebSearchService {
    private val client = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(12, TimeUnit.SECONDS)
        .build()

    suspend fun search(query: String): Result<List<WebSearchResult>> = withContext(Dispatchers.IO) {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return@withContext Result.success(emptyList())

        try {
            val encoded = URLEncoder.encode(trimmed, "UTF-8")
            // DuckDuckGo Instant Answer API
            val ddgUrl = "https://api.duckduckgo.com/?q=$encoded&format=json&no_html=1&skip_disambig=1"
            val request = Request.Builder()
                .url(ddgUrl)
                .header("User-Agent", "Mozilla/5.0 (Android; Mobile; rv:109.0)")
                .build()

            val jsonStr = client.newCall(request).execute().use { response ->
                if (response.isSuccessful) response.body?.string().orEmpty() else ""
            }
            val results = mutableListOf<WebSearchResult>()

            if (jsonStr.isNotBlank()) {
                val root = JSONObject(jsonStr)

                // 1. Check Abstract
                val abstractText = root.optString("AbstractText")
                val abstractSource = root.optString("AbstractSource")
                val abstractUrl = root.optString("AbstractURL")
                if (abstractText.isNotBlank()) {
                    results.add(
                        WebSearchResult(
                            title = if (abstractSource.isNotBlank()) "Overview ($abstractSource)" else "Direct Answer",
                            snippet = abstractText,
                            url = abstractUrl.ifEmpty { "https://duckduckgo.com/?q=$encoded" }
                        )
                    )
                }

                // 2. Check Related Topics
                val related = root.optJSONArray("RelatedTopics") ?: JSONArray()
                for (i in 0 until related.length()) {
                    val item = related.optJSONObject(i) ?: continue
                    val text = item.optString("Text")
                    val firstUrl = item.optString("FirstURL")
                    if (text.isNotBlank()) {
                        val parts = text.split(" - ", limit = 2)
                        val title = parts.firstOrNull() ?: "Result"
                        val snippet = if (parts.size > 1) parts[1] else text
                        results.add(
                            WebSearchResult(
                                title = title,
                                snippet = snippet,
                                url = firstUrl.ifEmpty { "https://duckduckgo.com/?q=$encoded" }
                            )
                        )
                    }
                }
            }

            // If DDG Instant Answer has few results, supplement with HTML search
            if (results.size < 3) {
                val htmlResults = fetchDuckDuckGoHtml(trimmed)
                results.addAll(htmlResults)
            }

            Result.success(results.distinctBy { it.title }.take(8))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun fetchDuckDuckGoHtml(query: String): List<WebSearchResult> {
        val list = mutableListOf<WebSearchResult>()
        try {
            val encoded = URLEncoder.encode(query, "UTF-8")
            val htmlUrl = "https://html.duckduckgo.com/html/?q=$encoded"
            val req = Request.Builder()
                .url(htmlUrl)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                .build()

            val html = client.newCall(req).execute().use { res ->
                if (res.isSuccessful) res.body?.string().orEmpty() else ""
            }
            if (html.isNotBlank()) {
                val resultRegex = Regex("""<a class="result__a"[^>]*href="([^"]+)"[^>]*>([\s\S]*?)</a>[\s\S]*?<a class="result__snippet"[^>]*>([\s\S]*?)</a>""")
                resultRegex.findAll(html).take(6).forEach { match ->
                    val rawHref = match.groupValues[1]
                    val rawTitle = match.groupValues[2]
                    val rawSnippet = match.groupValues[3]

                    val decodedUrl = if (rawHref.contains("uddg=")) {
                        try {
                            java.net.URLDecoder.decode(rawHref.substringAfter("uddg=").substringBefore("&"), "UTF-8")
                        } catch (_: Exception) { rawHref }
                    } else if (rawHref.startsWith("//")) {
                        "https:$rawHref"
                    } else rawHref

                    val title = try {
                        android.text.Html.fromHtml(rawTitle, android.text.Html.FROM_HTML_MODE_LEGACY).toString().trim()
                    } catch (_: Throwable) {
                        rawTitle.replace(Regex("<[^>]*>"), "").trim()
                    }

                    val snippet = try {
                        android.text.Html.fromHtml(rawSnippet, android.text.Html.FROM_HTML_MODE_LEGACY).toString().trim()
                    } catch (_: Throwable) {
                        rawSnippet.replace(Regex("<[^>]*>"), "").trim()
                    }

                    if (title.isNotBlank()) {
                        list.add(
                            WebSearchResult(
                                title = title,
                                snippet = snippet,
                                url = decodedUrl
                            )
                        )
                    }
                }
            }
        } catch (_: Exception) {}
        return list
    }
}
