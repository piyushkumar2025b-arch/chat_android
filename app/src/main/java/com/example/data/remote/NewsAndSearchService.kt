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
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    suspend fun search(query: String): Result<List<WebSearchResult>> = withContext(Dispatchers.IO) {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return@withContext Result.success(emptyList())

        val results = mutableListOf<WebSearchResult>()

        try {
            // 1. Fetch live news & current events from Google News Search RSS
            val newsResults = fetchGoogleNewsSearch(trimmed)
            results.addAll(newsResults)

            // 2. Fetch authoritative encyclopedia & knowledge from Wikipedia Live API
            val wikiResults = fetchWikipediaSearch(trimmed)
            results.addAll(wikiResults)

            // 3. Supplement with DuckDuckGo Instant Answers if available
            val ddgResults = fetchDuckDuckGoInstant(trimmed)
            results.addAll(ddgResults)

            // Deduplicate by title & link, preserving order
            val finalResults = results
                .distinctBy { it.title.lowercase().trim() }
                .take(12)

            Result.success(finalResults)
        } catch (e: Exception) {
            e.printStackTrace()
            if (results.isNotEmpty()) {
                Result.success(results.distinctBy { it.title }.take(10))
            } else {
                Result.failure(e)
            }
        }
    }

    private fun fetchGoogleNewsSearch(query: String): List<WebSearchResult> {
        val list = mutableListOf<WebSearchResult>()
        try {
            val encoded = URLEncoder.encode(query, "UTF-8")
            val url = "https://news.google.com/rss/search?q=$encoded&hl=en-US&gl=US&ceid=US:en"
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Android; Mobile; rv:109.0)")
                .build()

            val body = client.newCall(request).execute().use { response ->
                if (response.isSuccessful) response.body?.string().orEmpty() else ""
            }

            if (body.isNotBlank()) {
                val factory = XmlPullParserFactory.newInstance()
                factory.isNamespaceAware = false
                val parser = factory.newPullParser()
                parser.setInput(StringReader(body))

                var eventType = parser.eventType
                var inItem = false
                var title = ""
                var link = ""
                var pubDate = ""
                var source = ""
                var description = ""

                while (eventType != XmlPullParser.END_DOCUMENT && list.size < 6) {
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
                                        description.replace(Regex("<[^>]*>"), "").trim()
                                    }

                                    val finalSource = if (source.isNotBlank()) {
                                        source
                                    } else {
                                        val parts = title.split(" - ")
                                        if (parts.size > 1) parts.last().trim() else "Google News"
                                    }
                                    val finalTitle = if (title.contains(" - ")) {
                                        title.substringBeforeLast(" - ").trim()
                                    } else title.trim()

                                    val snippet = if (cleanDesc.isNotBlank() && cleanDesc != finalTitle) {
                                        cleanDesc
                                    } else {
                                        "Live reporting from $finalSource on $finalTitle."
                                    }

                                    list.add(
                                        WebSearchResult(
                                            title = finalTitle,
                                            snippet = snippet,
                                            url = link.trim(),
                                            source = finalSource,
                                            pubDate = formatPubDate(pubDate)
                                        )
                                    )
                                }
                                inItem = false
                            }
                        }
                    }
                    eventType = parser.next()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    private fun fetchWikipediaSearch(query: String): List<WebSearchResult> {
        val list = mutableListOf<WebSearchResult>()
        try {
            val encoded = URLEncoder.encode(query, "UTF-8")
            val url = "https://en.wikipedia.org/w/api.php?action=query&list=search&srsearch=$encoded&format=json&utf8=1"
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "OmniChat/1.0 (Mobile Assistant)")
                .build()

            val body = client.newCall(request).execute().use { response ->
                if (response.isSuccessful) response.body?.string().orEmpty() else ""
            }

            if (body.isNotBlank()) {
                val root = JSONObject(body)
                val queryObj = root.optJSONObject("query")
                val searchArr = queryObj?.optJSONArray("search") ?: JSONArray()
                for (i in 0 until searchArr.length()) {
                    if (list.size >= 4) break
                    val item = searchArr.optJSONObject(i) ?: continue
                    val title = item.optString("title")
                    val pageId = item.optLong("pageid")
                    val rawSnippet = item.optString("snippet")
                    val cleanSnippet = try {
                        android.text.Html.fromHtml(rawSnippet, android.text.Html.FROM_HTML_MODE_LEGACY).toString().trim()
                    } catch (_: Throwable) {
                        rawSnippet.replace(Regex("<[^>]*>"), "").trim()
                    }

                    if (title.isNotBlank() && cleanSnippet.isNotBlank()) {
                        list.add(
                            WebSearchResult(
                                title = title,
                                snippet = cleanSnippet,
                                url = "https://en.wikipedia.org/?curid=$pageId",
                                source = "Wikipedia",
                                pubDate = "Encyclopedia"
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    private fun fetchDuckDuckGoInstant(query: String): List<WebSearchResult> {
        val list = mutableListOf<WebSearchResult>()
        try {
            val encoded = URLEncoder.encode(query, "UTF-8")
            val url = "https://api.duckduckgo.com/?q=$encoded&format=json&no_html=1&skip_disambig=1"
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Android; Mobile; rv:109.0)")
                .build()

            val body = client.newCall(request).execute().use { res ->
                if (res.isSuccessful) res.body?.string().orEmpty() else ""
            }

            if (body.isNotBlank()) {
                val root = JSONObject(body)
                val abstractText = root.optString("AbstractText")
                val abstractSource = root.optString("AbstractSource")
                val abstractUrl = root.optString("AbstractURL")
                if (abstractText.isNotBlank()) {
                    list.add(
                        WebSearchResult(
                            title = if (abstractSource.isNotBlank()) "Overview ($abstractSource)" else "Direct Answer",
                            snippet = abstractText,
                            url = abstractUrl.ifEmpty { "https://duckduckgo.com/?q=$encoded" },
                            source = abstractSource.ifEmpty { "DuckDuckGo" },
                            pubDate = "Live"
                        )
                    )
                }

                val related = root.optJSONArray("RelatedTopics") ?: JSONArray()
                for (i in 0 until related.length()) {
                    if (list.size >= 4) break
                    val item = related.optJSONObject(i) ?: continue
                    val text = item.optString("Text")
                    val firstUrl = item.optString("FirstURL")
                    if (text.isNotBlank()) {
                        val parts = text.split(" - ", limit = 2)
                        val title = parts.firstOrNull() ?: "Topic"
                        val snippet = if (parts.size > 1) parts[1] else text
                        list.add(
                            WebSearchResult(
                                title = title,
                                snippet = snippet,
                                url = firstUrl.ifEmpty { "https://duckduckgo.com/?q=$encoded" },
                                source = "DuckDuckGo",
                                pubDate = "Live"
                            )
                        )
                    }
                }
            }
        } catch (_: Exception) {}
        return list
    }

    private fun formatPubDate(raw: String): String {
        return try {
            val parts = raw.split(" ")
            if (parts.size >= 5) {
                "${parts[1]} ${parts[2]} ${parts[4]}"
            } else raw
        } catch (_: Exception) {
            raw
        }
    }
}
