package com.example.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.StringReader
import java.net.URLDecoder
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
        ALL("All News", "https://feeds.bbci.co.uk/news/rss.xml"),
        TECH("Tech & AI", "https://feeds.bbci.co.uk/news/technology/rss.xml"),
        WORLD("World", "https://feeds.bbci.co.uk/news/world/rss.xml"),
        SCIENCE("Science", "https://feeds.bbci.co.uk/news/science_and_environment/rss.xml"),
        BUSINESS("Business", "https://feeds.bbci.co.uk/news/business/rss.xml")
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
                                "source", "news:source" -> source = parser.nextText()
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
                                    if (parts.size > 1) parts.last().trim() else "BBC News"
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

        val results = mutableListOf<WebSearchResult>()

        try {
            coroutineScope {
                val bingDeferred = async { fetchBingNewsSearch(trimmed) }
                val wikiDeferred = async { fetchWikipediaSearch(trimmed) }
                val hnDeferred = async { fetchHnSearch(trimmed) }
                val ddgDeferred = async { fetchDuckDuckGoInstant(trimmed) }

                val ddg = ddgDeferred.await()
                val wiki = wikiDeferred.await()
                val bing = bingDeferred.await()
                val hn = hnDeferred.await()

                // Direct answer & encyclopedia definitions first
                results.addAll(ddg)
                results.addAll(wiki)
                // Real-time news & verified publications
                results.addAll(bing)
                // Community, web & developer discussions
                results.addAll(hn)
            }

            // Deduplicate by normalized title & URL, preserving order
            val finalResults = results
                .distinctBy { it.title.lowercase().trim() }
                .take(12)

            Result.success(finalResults)
        } catch (e: Exception) {
            e.printStackTrace()
            if (results.isNotEmpty()) {
                Result.success(results.distinctBy { it.title.lowercase().trim() }.take(10))
            } else {
                Result.failure(e)
            }
        }
    }

    private fun fetchBingNewsSearch(query: String): List<WebSearchResult> {
        val list = mutableListOf<WebSearchResult>()
        try {
            val encoded = URLEncoder.encode(query, "UTF-8")
            val url = "https://www.bing.com/news/search?q=$encoded&format=rss"
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Android; Mobile; rv:109.0)")
                .build()

            val body = client.newCall(request).execute().use { response ->
                if (response.isSuccessful) response.body?.string().orEmpty() else ""
            }

            if (body.isNotBlank() && body.contains("<item>")) {
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
                                    "source", "news:source" -> source = parser.nextText()
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

                                    val directUrl = try {
                                        if (link.contains("url=")) {
                                            val extracted = link.substringAfter("url=").substringBefore("&")
                                            URLDecoder.decode(extracted, "UTF-8")
                                        } else link.trim()
                                    } catch (_: Exception) { link.trim() }

                                    val finalSource = if (source.isNotBlank()) {
                                        source.trim()
                                    } else {
                                        try {
                                            java.net.URI(directUrl).host?.removePrefix("www.") ?: "News"
                                        } catch (_: Exception) { "News" }
                                    }

                                    list.add(
                                        WebSearchResult(
                                            title = title.trim(),
                                            snippet = if (cleanDesc.isNotBlank()) cleanDesc else "News report from $finalSource on ${title.trim()}.",
                                            url = directUrl,
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
            val url = "https://en.wikipedia.org/w/api.php?action=query&generator=search&gsrsearch=$encoded&gsrlimit=6&prop=extracts|info&inprop=url&exintro=1&explaintext=1&exchars=350&format=json"
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "OmniChat/1.0 (Mobile Assistant; contact@omnichat.app)")
                .build()

            val body = client.newCall(request).execute().use { response ->
                if (response.isSuccessful) response.body?.string().orEmpty() else ""
            }

            if (body.isNotBlank()) {
                val root = JSONObject(body)
                val queryObj = root.optJSONObject("query")
                val pagesObj = queryObj?.optJSONObject("pages")
                if (pagesObj != null) {
                    val keys = pagesObj.keys()
                    while (keys.hasNext() && list.size < 5) {
                        val pageId = keys.next()
                        val page = pagesObj.optJSONObject(pageId) ?: continue
                        val title = page.optString("title")
                        val extract = page.optString("extract").trim()
                        val fullUrl = page.optString("fullurl").ifEmpty {
                            "https://en.wikipedia.org/wiki/" + URLEncoder.encode(title.replace(" ", "_"), "UTF-8")
                        }

                        if (title.isNotBlank()) {
                            list.add(
                                WebSearchResult(
                                    title = title,
                                    snippet = if (extract.isNotBlank()) extract else "Encyclopedia article on $title.",
                                    url = fullUrl,
                                    source = "Wikipedia",
                                    pubDate = "Encyclopedia"
                                )
                            )
                        }
                    }
                }
            }

            // Fallback to opensearch if generator had 0 results
            if (list.isEmpty()) {
                val openUrl = "https://en.wikipedia.org/w/api.php?action=opensearch&search=$encoded&limit=4&namespace=0&format=json"
                val openReq = Request.Builder().url(openUrl).header("User-Agent", "OmniChat/1.0").build()
                val openBody = client.newCall(openReq).execute().use { r -> if (r.isSuccessful) r.body?.string().orEmpty() else "" }
                if (openBody.isNotBlank()) {
                    val arr = JSONArray(openBody)
                    val titles = arr.optJSONArray(1) ?: JSONArray()
                    val snippets = arr.optJSONArray(2) ?: JSONArray()
                    val urls = arr.optJSONArray(3) ?: JSONArray()
                    for (i in 0 until titles.length()) {
                        val t = titles.optString(i)
                        val s = snippets.optString(i)
                        val u = urls.optString(i)
                        if (t.isNotBlank()) {
                            list.add(
                                WebSearchResult(
                                    title = t,
                                    snippet = if (s.isNotBlank()) s else "Encyclopedia information on $t.",
                                    url = u,
                                    source = "Wikipedia",
                                    pubDate = "Encyclopedia"
                                )
                            )
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    private fun fetchHnSearch(query: String): List<WebSearchResult> {
        val list = mutableListOf<WebSearchResult>()
        try {
            val encoded = URLEncoder.encode(query, "UTF-8")
            val url = "https://hn.algolia.com/api/v1/search?query=$encoded&hitsPerPage=6"
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "OmniChat/1.0 (Mobile Assistant)")
                .build()

            val body = client.newCall(request).execute().use { response ->
                if (response.isSuccessful) response.body?.string().orEmpty() else ""
            }

            if (body.isNotBlank()) {
                val root = JSONObject(body)
                val hits = root.optJSONArray("hits") ?: JSONArray()
                for (i in 0 until hits.length()) {
                    if (list.size >= 5) break
                    val hit = hits.optJSONObject(i) ?: continue
                    val title = hit.optString("title").ifEmpty { hit.optString("story_title") }
                    val rawUrl = hit.optString("url").ifEmpty { "https://news.ycombinator.com/item?id=" + hit.optString("objectID") }
                    val points = hit.optInt("points", 0)
                    val comments = hit.optInt("num_comments", 0)
                    val author = hit.optString("author")
                    val storyText = hit.optString("story_text")

                    if (title.isNotBlank()) {
                        val sourceDomain = try {
                            java.net.URI(rawUrl).host?.removePrefix("www.") ?: "Web"
                        } catch (_: Exception) { "Web Discussion" }

                        val snippet = if (storyText.isNotBlank()) {
                            try {
                                android.text.Html.fromHtml(storyText, android.text.Html.FROM_HTML_MODE_LEGACY).toString().trim()
                            } catch (_: Throwable) { storyText }
                        } else {
                            "Article & Discussion ($points points, $comments comments by $author)."
                        }

                        list.add(
                            WebSearchResult(
                                title = title,
                                snippet = snippet,
                                url = rawUrl,
                                source = sourceDomain,
                                pubDate = formatPubDate(hit.optString("created_at"))
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
                .header("User-Agent", "OmniChat/1.0 (Mobile Assistant)")
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
                            pubDate = "Instant"
                        )
                    )
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
            } else if (raw.contains("T")) {
                raw.substringBefore("T")
            } else raw
        } catch (_: Exception) {
            raw
        }
    }
}
