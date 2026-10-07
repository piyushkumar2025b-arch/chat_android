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
    val pubDate: String = "",
    val category: String = "General"
)

enum class SearchCategory(val id: String, val label: String, val icon: String) {
    ALL("all", "All Sources", "🌐"),
    KNOWLEDGE("knowledge", "Encyclopedia", "📖"),
    NEWS("news", "Live News", "📰"),
    SCIENCE("science", "Science & Research", "🔬"),
    CODE("code", "Code & Dev", "💻")
}

object NewsFeedService {
    private val client = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(12, TimeUnit.SECONDS)
        .build()

    enum class NewsCategory(val label: String, val rssUrl: String) {
        ALL("All News", "https://feeds.bbci.co.uk/news/rss.xml"),
        TECH("Tech & AI", "https://feeds.bbci.co.uk/news/technology/rss.xml"),
        WORLD("World", "https://feeds.bbci.co.uk/news/world/rss.xml"),
        SCIENCE("Science", "https://feeds.bbci.co.uk/news/science_and_environment/rss.xml"),
        BUSINESS("Business", "https://feeds.bbci.co.uk/news/business/rss.xml"),
        ENTERTAINMENT("Entertainment", "https://feeds.bbci.co.uk/news/entertainment_and_arts/rss.xml"),
        HEALTH("Health", "https://feeds.bbci.co.uk/news/health/rss.xml")
    }

    suspend fun fetchNews(category: NewsCategory): Result<List<NewsArticle>> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(category.rssUrl)
                .header("User-Agent", "Mozilla/5.0 (Android; Mobile; rv:125.0) OmniChat/1.0")
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
                                val cleanDesc = cleanHtml(description)
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
}

object WebSearchService {
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    suspend fun search(
        query: String,
        category: SearchCategory = SearchCategory.ALL
    ): Result<List<WebSearchResult>> = withContext(Dispatchers.IO) {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return@withContext Result.success(emptyList())

        val results = mutableListOf<WebSearchResult>()

        try {
            coroutineScope {
                val deferreds = when (category) {
                    SearchCategory.ALL -> listOf(
                        async { runCatching { fetchWikipediaSearch(trimmed) }.getOrDefault(emptyList()) },
                        async { runCatching { fetchDuckDuckGo(trimmed) }.getOrDefault(emptyList()) },
                        async { runCatching { fetchBingNewsSearch(trimmed) }.getOrDefault(emptyList()) },
                        async { runCatching { fetchArXivSearch(trimmed) }.getOrDefault(emptyList()) },
                        async { runCatching { fetchGitHubSearch(trimmed) }.getOrDefault(emptyList()) },
                        async { runCatching { fetchHnSearch(trimmed) }.getOrDefault(emptyList()) },
                        async { runCatching { fetchCrossRefSearch(trimmed) }.getOrDefault(emptyList()) }
                    )
                    SearchCategory.KNOWLEDGE -> listOf(
                        async { runCatching { fetchWikipediaSearch(trimmed) }.getOrDefault(emptyList()) },
                        async { runCatching { fetchDuckDuckGo(trimmed) }.getOrDefault(emptyList()) },
                        async { runCatching { fetchWiktionarySearch(trimmed) }.getOrDefault(emptyList()) }
                    )
                    SearchCategory.NEWS -> listOf(
                        async { runCatching { fetchBingNewsSearch(trimmed) }.getOrDefault(emptyList()) },
                        async { runCatching { fetchHnSearch(trimmed) }.getOrDefault(emptyList()) },
                        async { runCatching { fetchWikipediaSearch(trimmed) }.getOrDefault(emptyList()) }
                    )
                    SearchCategory.SCIENCE -> listOf(
                        async { runCatching { fetchArXivSearch(trimmed) }.getOrDefault(emptyList()) },
                        async { runCatching { fetchCrossRefSearch(trimmed) }.getOrDefault(emptyList()) },
                        async { runCatching { fetchWikipediaSearch(trimmed) }.getOrDefault(emptyList()) }
                    )
                    SearchCategory.CODE -> listOf(
                        async { runCatching { fetchGitHubSearch(trimmed) }.getOrDefault(emptyList()) },
                        async { runCatching { fetchHnSearch(trimmed) }.getOrDefault(emptyList()) },
                        async { runCatching { fetchWikipediaSearch(trimmed) }.getOrDefault(emptyList()) }
                    )
                }

                for (deferred in deferreds) {
                    results.addAll(deferred.await())
                }
            }

            // Deduplicate by normalized title & URL, preserving order
            val finalResults = results
                .distinctBy { it.title.lowercase().trim() }
                .take(18)

            Result.success(finalResults)
        } catch (e: Exception) {
            e.printStackTrace()
            if (results.isNotEmpty()) {
                Result.success(results.distinctBy { it.title.lowercase().trim() }.take(15))
            } else {
                Result.failure(e)
            }
        }
    }

    /**
     * Wikipedia query list search + opensearch fallback.
     */
    private fun fetchWikipediaSearch(query: String): List<WebSearchResult> {
        val list = mutableListOf<WebSearchResult>()
        try {
            val encoded = URLEncoder.encode(query, "UTF-8")
            val url = "https://en.wikipedia.org/w/api.php?action=query&list=search&srsearch=$encoded&format=json&srlimit=6"
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Android; Mobile; rv:125.0) OmniChat/1.0")
                .build()

            val body = client.newCall(request).execute().use { response ->
                if (response.isSuccessful) response.body?.string().orEmpty() else ""
            }

            if (body.isNotBlank()) {
                val root = JSONObject(body)
                val queryObj = root.optJSONObject("query")
                val searchArr = queryObj?.optJSONArray("search")
                if (searchArr != null) {
                    for (i in 0 until searchArr.length()) {
                        if (list.size >= 5) break
                        val item = searchArr.optJSONObject(i) ?: continue
                        val title = item.optString("title").trim()
                        val rawSnippet = item.optString("snippet")
                        val cleanSnippet = cleanHtml(rawSnippet)
                        val fullUrl = "https://en.wikipedia.org/wiki/" + URLEncoder.encode(title.replace(" ", "_"), "UTF-8")

                        if (title.isNotBlank()) {
                            list.add(
                                WebSearchResult(
                                    title = title,
                                    snippet = if (cleanSnippet.isNotBlank()) cleanSnippet else "Encyclopedia article on $title.",
                                    url = fullUrl,
                                    source = "Wikipedia",
                                    pubDate = "Encyclopedia",
                                    category = "Encyclopedia"
                                )
                            )
                        }
                    }
                }
            }

            // Fallback to opensearch if list search returned no results
            if (list.isEmpty()) {
                val openUrl = "https://en.wikipedia.org/w/api.php?action=opensearch&search=$encoded&limit=4&namespace=0&format=json"
                val openReq = Request.Builder()
                    .url(openUrl)
                    .header("User-Agent", "Mozilla/5.0 (Android; Mobile; rv:125.0) OmniChat/1.0")
                    .build()
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
                                    pubDate = "Encyclopedia",
                                    category = "Encyclopedia"
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

    /**
     * DuckDuckGo Instant Answers and Related Topics.
     */
    private fun fetchDuckDuckGo(query: String): List<WebSearchResult> {
        val list = mutableListOf<WebSearchResult>()
        try {
            val encoded = URLEncoder.encode(query, "UTF-8")
            val url = "https://api.duckduckgo.com/?q=$encoded&format=json&no_html=1&skip_disambig=0"
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Android; Mobile; rv:125.0) OmniChat/1.0")
                .build()

            val body = client.newCall(request).execute().use { res ->
                if (res.isSuccessful) res.body?.string().orEmpty() else ""
            }

            if (body.isNotBlank()) {
                val root = JSONObject(body)
                val abstractText = root.optString("AbstractText").trim()
                val abstractSource = root.optString("AbstractSource")
                val abstractUrl = root.optString("AbstractURL")
                val heading = root.optString("Heading")

                if (abstractText.isNotBlank()) {
                    list.add(
                        WebSearchResult(
                            title = if (heading.isNotBlank()) heading else if (abstractSource.isNotBlank()) "Overview ($abstractSource)" else "Direct Answer",
                            snippet = abstractText,
                            url = abstractUrl.ifEmpty { "https://duckduckgo.com/?q=$encoded" },
                            source = abstractSource.ifEmpty { "DuckDuckGo" },
                            pubDate = "Instant",
                            category = "Web Answers"
                        )
                    )
                }

                // Parse RelatedTopics
                val relatedTopics = root.optJSONArray("RelatedTopics")
                if (relatedTopics != null) {
                    for (i in 0 until relatedTopics.length()) {
                        if (list.size >= 4) break
                        val topic = relatedTopics.optJSONObject(i) ?: continue

                        // Check if direct topic item
                        val text = topic.optString("Text").trim()
                        val firstUrl = topic.optString("FirstURL").trim()

                        if (text.isNotBlank() && firstUrl.isNotBlank()) {
                            val parts = text.split(" - ", limit = 2)
                            val title = if (parts.size > 1) parts[0].trim() else text.take(60)
                            val desc = if (parts.size > 1) parts[1].trim() else text
                            list.add(
                                WebSearchResult(
                                    title = title,
                                    snippet = desc,
                                    url = firstUrl,
                                    source = "DuckDuckGo",
                                    pubDate = "Topic",
                                    category = "Web Answers"
                                )
                            )
                        } else if (topic.has("Topics")) {
                            // Subtopics cluster
                            val subArr = topic.optJSONArray("Topics") ?: continue
                            for (j in 0 until subArr.length()) {
                                if (list.size >= 4) break
                                val sub = subArr.optJSONObject(j) ?: continue
                                val subText = sub.optString("Text").trim()
                                val subUrl = sub.optString("FirstURL").trim()
                                if (subText.isNotBlank()) {
                                    val parts = subText.split(" - ", limit = 2)
                                    val title = if (parts.size > 1) parts[0].trim() else subText.take(60)
                                    val desc = if (parts.size > 1) parts[1].trim() else subText
                                    list.add(
                                        WebSearchResult(
                                            title = title,
                                            snippet = desc,
                                            url = subUrl,
                                            source = "DuckDuckGo",
                                            pubDate = "Topic",
                                            category = "Web Answers"
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    /**
     * Bing News RSS search.
     */
    private fun fetchBingNewsSearch(query: String): List<WebSearchResult> {
        val list = mutableListOf<WebSearchResult>()
        try {
            val encoded = URLEncoder.encode(query, "UTF-8")
            val url = "https://www.bing.com/news/search?q=$encoded&format=rss"
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Android; Mobile; rv:125.0) OmniChat/1.0")
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

                while (eventType != XmlPullParser.END_DOCUMENT && list.size < 5) {
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
                                    val cleanDesc = cleanHtml(description)
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
                                            pubDate = formatPubDate(pubDate),
                                            category = "News"
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

    /**
     * arXiv Open Scientific Papers API.
     */
    private fun fetchArXivSearch(query: String): List<WebSearchResult> {
        val list = mutableListOf<WebSearchResult>()
        try {
            val encoded = URLEncoder.encode(query, "UTF-8")
            val url = "https://export.arxiv.org/api/query?search_query=all:$encoded&start=0&max_results=4"
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Android; Mobile; rv:125.0) OmniChat/1.0")
                .build()

            val body = client.newCall(request).execute().use { response ->
                if (response.isSuccessful) response.body?.string().orEmpty() else ""
            }

            if (body.isNotBlank() && body.contains("<entry>")) {
                val factory = XmlPullParserFactory.newInstance()
                factory.isNamespaceAware = false
                val parser = factory.newPullParser()
                parser.setInput(StringReader(body))

                var eventType = parser.eventType
                var inEntry = false
                var title = ""
                var summary = ""
                var link = ""
                var published = ""
                var author = ""

                while (eventType != XmlPullParser.END_DOCUMENT && list.size < 4) {
                    val tagName = parser.name ?: ""
                    when (eventType) {
                        XmlPullParser.START_TAG -> {
                            if (tagName.equals("entry", ignoreCase = true)) {
                                inEntry = true
                                title = ""
                                summary = ""
                                link = ""
                                published = ""
                                author = ""
                            } else if (inEntry) {
                                when (tagName.lowercase()) {
                                    "title" -> title = parser.nextText().replace("\n", " ").trim()
                                    "summary" -> summary = parser.nextText().replace("\n", " ").trim()
                                    "id" -> if (link.isEmpty()) link = parser.nextText().trim()
                                    "published" -> published = parser.nextText().trim()
                                    "name" -> if (author.isEmpty()) author = parser.nextText().trim()
                                }
                            }
                        }
                        XmlPullParser.END_TAG -> {
                            if (tagName.equals("entry", ignoreCase = true) && inEntry) {
                                if (title.isNotBlank() && !title.startsWith("arXiv Query:", ignoreCase = true)) {
                                    val snippetText = if (summary.isNotBlank()) {
                                        if (author.isNotBlank()) "By $author: ${summary.take(280)}..." else summary.take(300)
                                    } else "Scientific research publication on $title."

                                    list.add(
                                        WebSearchResult(
                                            title = title,
                                            snippet = snippetText,
                                            url = link.ifEmpty { "https://arxiv.org/search/?query=$encoded&searchtype=all" },
                                            source = "arXiv",
                                            pubDate = formatPubDate(published),
                                            category = "Science"
                                        )
                                    )
                                }
                                inEntry = false
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

    /**
     * GitHub Open Source Repositories & Tools.
     */
    private fun fetchGitHubSearch(query: String): List<WebSearchResult> {
        val list = mutableListOf<WebSearchResult>()
        try {
            val encoded = URLEncoder.encode(query, "UTF-8")
            val url = "https://api.github.com/search/repositories?q=$encoded&per_page=4&sort=stars"
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "OmniChat-Android")
                .header("Accept", "application/vnd.github.v3+json")
                .build()

            val body = client.newCall(request).execute().use { response ->
                if (response.isSuccessful) response.body?.string().orEmpty() else ""
            }

            if (body.isNotBlank()) {
                val root = JSONObject(body)
                val items = root.optJSONArray("items") ?: JSONArray()
                for (i in 0 until items.length()) {
                    if (list.size >= 4) break
                    val item = items.optJSONObject(i) ?: continue
                    val fullName = item.optString("full_name")
                    val desc = item.optString("description")
                    val stars = item.optInt("stargazers_count", 0)
                    val lang = item.optString("language")
                    val htmlUrl = item.optString("html_url")
                    val updatedAt = item.optString("updated_at")

                    if (fullName.isNotBlank()) {
                        val starsStr = if (stars >= 1000) String.format("%.1fk", stars / 1000.0) else "$stars"
                        val meta = buildString {
                            append("★ $starsStr")
                            if (lang.isNotBlank() && lang != "null") append(" • $lang")
                        }
                        val snippet = if (desc.isNotBlank() && desc != "null") "$desc ($meta)" else "GitHub open-source repository ($meta)"

                        list.add(
                            WebSearchResult(
                                title = fullName,
                                snippet = snippet,
                                url = htmlUrl,
                                source = "GitHub",
                                pubDate = formatPubDate(updatedAt),
                                category = "Code"
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

    /**
     * Hacker News Algolia Community Search.
     */
    private fun fetchHnSearch(query: String): List<WebSearchResult> {
        val list = mutableListOf<WebSearchResult>()
        try {
            val encoded = URLEncoder.encode(query, "UTF-8")
            val url = "https://hn.algolia.com/api/v1/search?query=$encoded&hitsPerPage=5"
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Android; Mobile; rv:125.0) OmniChat/1.0")
                .build()

            val body = client.newCall(request).execute().use { response ->
                if (response.isSuccessful) response.body?.string().orEmpty() else ""
            }

            if (body.isNotBlank()) {
                val root = JSONObject(body)
                val hits = root.optJSONArray("hits") ?: JSONArray()
                for (i in 0 until hits.length()) {
                    if (list.size >= 4) break
                    val hit = hits.optJSONObject(i) ?: continue
                    val title = hit.optString("title").ifEmpty { hit.optString("story_title") }
                    val rawUrl = hit.optString("url").ifEmpty { "https://news.ycombinator.com/item?id=" + hit.optString("objectID") }
                    val points = hit.optInt("points", 0)
                    val comments = hit.optInt("num_comments", 0)
                    val author = hit.optString("author")
                    val storyText = hit.optString("story_text")

                    if (title.isNotBlank()) {
                        val snippet = if (storyText.isNotBlank()) {
                            cleanHtml(storyText)
                        } else {
                            "Discussion ($points points, $comments comments by $author)."
                        }

                        list.add(
                            WebSearchResult(
                                title = title,
                                snippet = snippet,
                                url = rawUrl,
                                source = "Hacker News",
                                pubDate = formatPubDate(hit.optString("created_at")),
                                category = "Tech"
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

    /**
     * CrossRef Scholarly Publications.
     */
    private fun fetchCrossRefSearch(query: String): List<WebSearchResult> {
        val list = mutableListOf<WebSearchResult>()
        try {
            val encoded = URLEncoder.encode(query, "UTF-8")
            val url = "https://api.crossref.org/works?query=$encoded&rows=3"
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "OmniChat/1.0 (mailto:assistant@omnichat.app)")
                .build()

            val body = client.newCall(request).execute().use { response ->
                if (response.isSuccessful) response.body?.string().orEmpty() else ""
            }

            if (body.isNotBlank()) {
                val root = JSONObject(body)
                val msg = root.optJSONObject("message")
                val items = msg?.optJSONArray("items") ?: JSONArray()
                for (i in 0 until items.length()) {
                    if (list.size >= 3) break
                    val item = items.optJSONObject(i) ?: continue
                    val titleArr = item.optJSONArray("title")
                    val title = titleArr?.optString(0)?.trim().orEmpty()
                    val publisher = item.optString("publisher")
                    val rawUrl = item.optString("URL")
                    val doi = item.optString("DOI")

                    if (title.isNotBlank()) {
                        val snippet = if (publisher.isNotBlank()) {
                            "Scholarly publication by $publisher. DOI: $doi"
                        } else "Peer-reviewed paper. DOI: $doi"

                        list.add(
                            WebSearchResult(
                                title = title,
                                snippet = snippet,
                                url = rawUrl.ifEmpty { "https://doi.org/$doi" },
                                source = "CrossRef",
                                pubDate = "Journal",
                                category = "Science"
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

    /**
     * Wiktionary terminology and definitions.
     */
    private fun fetchWiktionarySearch(query: String): List<WebSearchResult> {
        val list = mutableListOf<WebSearchResult>()
        try {
            val encoded = URLEncoder.encode(query, "UTF-8")
            val url = "https://en.wiktionary.org/w/api.php?action=query&list=search&srsearch=$encoded&format=json&srlimit=3"
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Android; Mobile; rv:125.0) OmniChat/1.0")
                .build()

            val body = client.newCall(request).execute().use { response ->
                if (response.isSuccessful) response.body?.string().orEmpty() else ""
            }

            if (body.isNotBlank()) {
                val root = JSONObject(body)
                val queryObj = root.optJSONObject("query")
                val searchArr = queryObj?.optJSONArray("search") ?: JSONArray()
                for (i in 0 until searchArr.length()) {
                    val item = searchArr.optJSONObject(i) ?: continue
                    val title = item.optString("title")
                    val snippet = cleanHtml(item.optString("snippet"))
                    val pageUrl = "https://en.wiktionary.org/wiki/" + URLEncoder.encode(title.replace(" ", "_"), "UTF-8")
                    if (title.isNotBlank() && snippet.isNotBlank()) {
                        list.add(
                            WebSearchResult(
                                title = title,
                                snippet = snippet,
                                url = pageUrl,
                                source = "Wiktionary",
                                pubDate = "Definition",
                                category = "Encyclopedia"
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
}

private fun cleanHtml(html: String): String {
    return html
        .replace(Regex("<[^>]*>"), "")
        .replace("&quot;", "\"")
        .replace("&amp;", "&")
        .replace("&#039;", "'")
        .replace("&apos;", "'")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&nbsp;", " ")
        .trim()
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
