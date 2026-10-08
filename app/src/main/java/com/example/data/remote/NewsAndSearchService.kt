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
    CODE("code", "Code & Dev", "💻"),
    BOOKS("books", "Books & Archive", "📚")
}

object NewsFeedService {
    private val client = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(12, TimeUnit.SECONDS)
        .build()

    enum class NewsCategory(val label: String, val feeds: List<Pair<String, String>>) {
        ALL(
            "All News",
            listOf(
                "BBC News" to "https://feeds.bbci.co.uk/news/rss.xml",
                "New York Times" to "https://rss.nytimes.com/services/xml/rss/nyt/HomePage.xml",
                "The Guardian" to "https://www.theguardian.com/world/rss",
                "Al Jazeera" to "https://www.aljazeera.com/xml/rss/all.xml",
                "CNBC News" to "https://search.cnbc.com/rs/search/combinedcms/view.xml?partnerId=wrss01&id=100003114"
            )
        ),
        TECH(
            "Tech & AI",
            listOf(
                "BBC Tech" to "https://feeds.bbci.co.uk/news/technology/rss.xml",
                "Ars Technica" to "https://feeds.arstechnica.com/arstechnica/index",
                "NYT Tech" to "https://rss.nytimes.com/services/xml/rss/nyt/Technology.xml",
                "The Guardian Tech" to "https://www.theguardian.com/technology/rss",
                "TechCrunch" to "https://feeds.feedburner.com/TechCrunch/",
                "CNBC Tech" to "https://search.cnbc.com/rs/search/combinedcms/view.xml?partnerId=wrss01&id=19854910"
            )
        ),
        WORLD(
            "World",
            listOf(
                "BBC World" to "https://feeds.bbci.co.uk/news/world/rss.xml",
                "NYT World" to "https://rss.nytimes.com/services/xml/rss/nyt/World.xml",
                "The Guardian World" to "https://www.theguardian.com/world/rss",
                "Al Jazeera" to "https://www.aljazeera.com/xml/rss/all.xml"
            )
        ),
        SCIENCE(
            "Science",
            listOf(
                "BBC Science" to "https://feeds.bbci.co.uk/news/science_and_environment/rss.xml",
                "NYT Science" to "https://rss.nytimes.com/services/xml/rss/nyt/Science.xml",
                "The Guardian Science" to "https://www.theguardian.com/science/rss",
                "NASA Releases" to "https://www.nasa.gov/news-release/feed/"
            )
        ),
        BUSINESS(
            "Business",
            listOf(
                "BBC Business" to "https://feeds.bbci.co.uk/news/business/rss.xml",
                "NYT Business" to "https://rss.nytimes.com/services/xml/rss/nyt/Business.xml",
                "The Guardian Business" to "https://www.theguardian.com/business/rss",
                "CNBC Markets" to "https://search.cnbc.com/rs/search/combinedcms/view.xml?partnerId=wrss01&id=100003114"
            )
        ),
        ENTERTAINMENT(
            "Entertainment",
            listOf(
                "BBC Culture" to "https://feeds.bbci.co.uk/news/entertainment_and_arts/rss.xml",
                "The Guardian Culture" to "https://www.theguardian.com/culture/rss",
                "NYT Arts" to "https://rss.nytimes.com/services/xml/rss/nyt/Arts.xml"
            )
        ),
        HEALTH(
            "Health",
            listOf(
                "BBC Health" to "https://feeds.bbci.co.uk/news/health/rss.xml",
                "NYT Health" to "https://rss.nytimes.com/services/xml/rss/nyt/Health.xml",
                "The Guardian Health" to "https://www.theguardian.com/lifeandstyle/health-and-wellbeing/rss"
            )
        )
    }

    suspend fun fetchNews(category: NewsCategory): Result<List<NewsArticle>> = withContext(Dispatchers.IO) {
        val allArticles = mutableListOf<NewsArticle>()

        try {
            coroutineScope {
                val deferreds = category.feeds.map { (sourceName, feedUrl) ->
                    async {
                        runCatching {
                            val request = Request.Builder()
                                .url(feedUrl)
                                .header("User-Agent", "Mozilla/5.0 (Android; Mobile; rv:125.0) OmniChat/1.0")
                                .build()

                            val body = client.newCall(request).execute().use { response ->
                                if (response.isSuccessful) response.body?.string().orEmpty() else ""
                            }
                            if (body.isNotBlank()) parseRssFeed(body, category.label, sourceName) else emptyList()
                        }.getOrDefault(emptyList())
                    }
                }

                for (deferred in deferreds) {
                    allArticles.addAll(deferred.await())
                }
            }

            if (allArticles.isNotEmpty()) {
                val deduplicated = allArticles
                    .distinctBy { it.title.lowercase().trim() }
                    .sortedByDescending { it.id }
                Result.success(deduplicated)
            } else {
                Result.failure(Exception("Could not fetch news from sources. Please check network connection."))
            }
        } catch (e: Exception) {
            e.printStackTrace()
            if (allArticles.isNotEmpty()) {
                Result.success(allArticles.distinctBy { it.title.lowercase().trim() })
            } else {
                Result.failure(e)
            }
        }
    }

    private fun parseRssFeed(xml: String, categoryName: String, defaultSourceName: String): List<NewsArticle> {
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
                                "source", "news:source", "dc:creator" -> {
                                    val text = parser.nextText()
                                    if (source.isEmpty() && text.isNotBlank()) source = text
                                }
                                "description" -> description = parser.nextText()
                            }
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        if (tagName.equals("item", ignoreCase = true) && inItem) {
                            if (title.isNotBlank()) {
                                val cleanDesc = cleanHtml(description)
                                val finalSource = when {
                                    source.isNotBlank() && !source.contains("Inside Climate", ignoreCase = true) -> source
                                    title.contains(" - ") -> title.substringAfterLast(" - ").trim()
                                    else -> defaultSourceName
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
                        async { runCatching { fetchStackOverflowSearch(trimmed) }.getOrDefault(emptyList()) },
                        async { runCatching { fetchDevToSearch(trimmed) }.getOrDefault(emptyList()) },
                        async { runCatching { fetchArXivSearch(trimmed) }.getOrDefault(emptyList()) },
                        async { runCatching { fetchPubMedSearch(trimmed) }.getOrDefault(emptyList()) },
                        async { runCatching { fetchOpenLibrarySearch(trimmed) }.getOrDefault(emptyList()) },
                        async { runCatching { fetchInternetArchiveSearch(trimmed) }.getOrDefault(emptyList()) },
                        async { runCatching { fetchGitHubSearch(trimmed) }.getOrDefault(emptyList()) },
                        async { runCatching { fetchHnSearch(trimmed) }.getOrDefault(emptyList()) },
                        async { runCatching { fetchOpenAlexSearch(trimmed) }.getOrDefault(emptyList()) },
                        async { runCatching { fetchCrossRefSearch(trimmed) }.getOrDefault(emptyList()) },
                        async { runCatching { fetchWikiquoteSearch(trimmed) }.getOrDefault(emptyList()) },
                        async { runCatching { fetchWikinewsSearch(trimmed) }.getOrDefault(emptyList()) }
                    )
                    SearchCategory.KNOWLEDGE -> listOf(
                        async { runCatching { fetchWikipediaSearch(trimmed) }.getOrDefault(emptyList()) },
                        async { runCatching { fetchDuckDuckGo(trimmed) }.getOrDefault(emptyList()) },
                        async { runCatching { fetchOpenLibrarySearch(trimmed) }.getOrDefault(emptyList()) },
                        async { runCatching { fetchInternetArchiveSearch(trimmed) }.getOrDefault(emptyList()) },
                        async { runCatching { fetchWikiquoteSearch(trimmed) }.getOrDefault(emptyList()) },
                        async { runCatching { fetchWiktionarySearch(trimmed) }.getOrDefault(emptyList()) }
                    )
                    SearchCategory.NEWS -> listOf(
                        async { runCatching { fetchBingNewsSearch(trimmed) }.getOrDefault(emptyList()) },
                        async { runCatching { fetchWikinewsSearch(trimmed) }.getOrDefault(emptyList()) },
                        async { runCatching { fetchHnSearch(trimmed) }.getOrDefault(emptyList()) },
                        async { runCatching { fetchWikipediaSearch(trimmed) }.getOrDefault(emptyList()) }
                    )
                    SearchCategory.SCIENCE -> listOf(
                        async { runCatching { fetchPubMedSearch(trimmed) }.getOrDefault(emptyList()) },
                        async { runCatching { fetchArXivSearch(trimmed) }.getOrDefault(emptyList()) },
                        async { runCatching { fetchOpenAlexSearch(trimmed) }.getOrDefault(emptyList()) },
                        async { runCatching { fetchCrossRefSearch(trimmed) }.getOrDefault(emptyList()) },
                        async { runCatching { fetchWikipediaSearch(trimmed) }.getOrDefault(emptyList()) }
                    )
                    SearchCategory.CODE -> listOf(
                        async { runCatching { fetchStackOverflowSearch(trimmed) }.getOrDefault(emptyList()) },
                        async { runCatching { fetchGitHubSearch(trimmed) }.getOrDefault(emptyList()) },
                        async { runCatching { fetchDevToSearch(trimmed) }.getOrDefault(emptyList()) },
                        async { runCatching { fetchHnSearch(trimmed) }.getOrDefault(emptyList()) },
                        async { runCatching { fetchWikipediaSearch(trimmed) }.getOrDefault(emptyList()) }
                    )
                    SearchCategory.BOOKS -> listOf(
                        async { runCatching { fetchOpenLibrarySearch(trimmed) }.getOrDefault(emptyList()) },
                        async { runCatching { fetchInternetArchiveSearch(trimmed) }.getOrDefault(emptyList()) },
                        async { runCatching { fetchWikiquoteSearch(trimmed) }.getOrDefault(emptyList()) },
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
                .take(24)

            Result.success(finalResults)
        } catch (e: Exception) {
            e.printStackTrace()
            if (results.isNotEmpty()) {
                Result.success(results.distinctBy { it.title.lowercase().trim() }.take(18))
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
                                    snippet = if (cleanSnippet.isNotBlank()) "$cleanSnippet..." else "Wikipedia entry for $title.",
                                    url = fullUrl,
                                    source = "Wikipedia",
                                    pubDate = formatPubDate(item.optString("timestamp")),
                                    category = "Encyclopedia"
                                )
                            )
                        }
                    }
                }
            }

            // Fallback to opensearch if list=search produced zero results
            if (list.isEmpty()) {
                val openSearchUrl = "https://en.wikipedia.org/w/api.php?action=opensearch&search=$encoded&limit=4&namespace=0&format=json"
                val osReq = Request.Builder()
                    .url(openSearchUrl)
                    .header("User-Agent", "Mozilla/5.0 (Android; Mobile; rv:125.0) OmniChat/1.0")
                    .build()
                val osBody = client.newCall(osReq).execute().use { response ->
                    if (response.isSuccessful) response.body?.string().orEmpty() else ""
                }
                if (osBody.isNotBlank()) {
                    val rootArray = JSONArray(osBody)
                    if (rootArray.length() >= 4) {
                        val titles = rootArray.optJSONArray(1) ?: JSONArray()
                        val descs = rootArray.optJSONArray(2) ?: JSONArray()
                        val urls = rootArray.optJSONArray(3) ?: JSONArray()

                        for (i in 0 until titles.length()) {
                            val t = titles.optString(i)
                            val d = descs.optString(i)
                            val u = urls.optString(i)
                            if (t.isNotBlank() && u.isNotBlank()) {
                                list.add(
                                    WebSearchResult(
                                        title = t,
                                        snippet = if (d.isNotBlank()) d else "Wikipedia article on $t.",
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
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    /**
     * DuckDuckGo Instant Answer API + Related Topics.
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

            val body = client.newCall(request).execute().use { response ->
                if (response.isSuccessful) response.body?.string().orEmpty() else ""
            }

            if (body.isNotBlank()) {
                val root = JSONObject(body)
                val abstractText = root.optString("AbstractText")
                val abstractUrl = root.optString("AbstractURL")
                val heading = root.optString("Heading")
                val source = root.optString("AbstractSource").ifEmpty { "DuckDuckGo" }

                if (abstractText.isNotBlank()) {
                    list.add(
                        WebSearchResult(
                            title = heading.ifEmpty { query },
                            snippet = abstractText,
                            url = abstractUrl.ifEmpty { "https://duckduckgo.com/?q=$encoded" },
                            source = source,
                            pubDate = "Instant Answer",
                            category = "Knowledge"
                        )
                    )
                }

                // Extract from RelatedTopics
                val relatedTopics = root.optJSONArray("RelatedTopics")
                if (relatedTopics != null) {
                    for (i in 0 until relatedTopics.length()) {
                        if (list.size >= 4) break
                        val topic = relatedTopics.optJSONObject(i) ?: continue
                        val text = topic.optString("Text")
                        val firstUrl = topic.optString("FirstURL")

                        if (text.isNotBlank() && firstUrl.isNotBlank()) {
                            val topicTitle = text.substringBefore(" - ").take(70)
                            list.add(
                                WebSearchResult(
                                    title = topicTitle,
                                    snippet = text,
                                    url = firstUrl,
                                    source = "DuckDuckGo",
                                    pubDate = "Related Topic",
                                    category = "Web"
                                )
                            )
                        } else {
                            // Subtopics array check
                            val topicsArray = topic.optJSONArray("Topics")
                            if (topicsArray != null) {
                                for (j in 0 until topicsArray.length()) {
                                    if (list.size >= 4) break
                                    val subTopic = topicsArray.optJSONObject(j) ?: continue
                                    val subText = subTopic.optString("Text")
                                    val subUrl = subTopic.optString("FirstURL")
                                    if (subText.isNotBlank() && subUrl.isNotBlank()) {
                                        list.add(
                                            WebSearchResult(
                                                title = subText.substringBefore(" - ").take(70),
                                                snippet = subText,
                                                url = subUrl,
                                                source = "DuckDuckGo",
                                                pubDate = "Related Topic",
                                                category = "Web"
                                            )
                                        )
                                    }
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
     * Bing News RSS Live Web Feed.
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
                                    val realUrl = if (link.contains("url=")) {
                                        try {
                                            URLDecoder.decode(link.substringAfter("url=").substringBefore("&"), "UTF-8")
                                        } catch (_: Exception) { link }
                                    } else link

                                    val finalSource = source.ifEmpty { "Bing News" }

                                    list.add(
                                        WebSearchResult(
                                            title = cleanHtml(title),
                                            snippet = cleanHtml(description).ifEmpty { "Live news report on $title." },
                                            url = realUrl.trim(),
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
     * StackOverflow & Stack Exchange Developer Solutions & QA.
     */
    private fun fetchStackOverflowSearch(query: String): List<WebSearchResult> {
        val list = mutableListOf<WebSearchResult>()
        try {
            val encoded = URLEncoder.encode(query, "UTF-8")
            val url = "https://api.stackexchange.com/2.3/search/advanced?order=desc&sort=relevance&q=$encoded&site=stackoverflow&pagesize=3"
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "OmniChat/1.0 (Android; Mobile)")
                .header("Accept-Encoding", "identity")
                .build()

            val body = client.newCall(request).execute().use { response ->
                if (response.isSuccessful) response.body?.string().orEmpty() else ""
            }

            if (body.isNotBlank()) {
                val root = JSONObject(body)
                val items = root.optJSONArray("items") ?: JSONArray()
                for (i in 0 until items.length()) {
                    if (list.size >= 3) break
                    val item = items.optJSONObject(i) ?: continue
                    val title = cleanHtml(item.optString("title"))
                    val link = item.optString("link")
                    val isAnswered = item.optBoolean("is_answered", false)
                    val score = item.optInt("score", 0)
                    val answerCount = item.optInt("answer_count", 0)
                    val tagsArr = item.optJSONArray("tags")
                    val tags = mutableListOf<String>()
                    if (tagsArr != null) {
                        for (t in 0 until tagsArr.length()) {
                            tags.add(tagsArr.optString(t))
                        }
                    }

                    if (title.isNotBlank() && link.isNotBlank()) {
                        val tagString = if (tags.isNotEmpty()) "Tags: [${tags.take(4).joinToString(", ")}]. " else ""
                        val status = if (isAnswered) "Answered ($answerCount answers, score $score)" else "Score $score ($answerCount answers)"
                        list.add(
                            WebSearchResult(
                                title = title,
                                snippet = "$tagString$status - Verified developer solution from Stack Overflow community.",
                                url = link,
                                source = "Stack Overflow",
                                pubDate = "Q&A Solution",
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
     * OpenAlex Global Scientific Catalog (250M+ open scholarly papers).
     */
    private fun fetchOpenAlexSearch(query: String): List<WebSearchResult> {
        val list = mutableListOf<WebSearchResult>()
        try {
            val encoded = URLEncoder.encode(query, "UTF-8")
            val url = "https://api.openalex.org/works?search=$encoded&per-page=3"
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "OmniChat/1.0 (mailto:dev@omnichat.app)")
                .build()

            val body = client.newCall(request).execute().use { response ->
                if (response.isSuccessful) response.body?.string().orEmpty() else ""
            }

            if (body.isNotBlank()) {
                val root = JSONObject(body)
                val results = root.optJSONArray("results") ?: JSONArray()
                for (i in 0 until results.length()) {
                    if (list.size >= 3) break
                    val item = results.optJSONObject(i) ?: continue
                    val title = item.optString("title").trim()
                    val doi = item.optString("doi")
                    val pubYear = item.optInt("publication_year", 0)
                    val citedCount = item.optInt("cited_by_count", 0)
                    val primaryLocation = item.optJSONObject("primary_location")
                    val sourceObj = primaryLocation?.optJSONObject("source")
                    val sourceDisplayName = sourceObj?.optString("display_name").orEmpty()

                    if (title.isNotBlank()) {
                        val yearText = if (pubYear > 0) " ($pubYear)" else ""
                        val citeText = if (citedCount > 0) " • Cited $citedCount times" else ""
                        val venueText = if (sourceDisplayName.isNotBlank()) "Published in $sourceDisplayName." else "Peer-reviewed scientific research."
                        val linkUrl = if (doi.isNotBlank()) doi else item.optString("id")

                        list.add(
                            WebSearchResult(
                                title = title,
                                snippet = "$venueText$yearText$citeText Open access academic paper via OpenAlex.",
                                url = linkUrl.ifEmpty { "https://openalex.org/works?search=$encoded" },
                                source = "OpenAlex",
                                pubDate = if (pubYear > 0) pubYear.toString() else "Paper",
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
                    val repo = items.optJSONObject(i) ?: continue
                    val fullName = repo.optString("full_name")
                    val desc = repo.optString("description")
                    val htmlUrl = repo.optString("html_url")
                    val stars = repo.optInt("stargazers_count", 0)
                    val language = repo.optString("language")

                    if (fullName.isNotBlank() && htmlUrl.isNotBlank()) {
                        val langText = if (language.isNotBlank() && language != "null") " [$language]" else ""
                        val starsText = if (stars > 0) " ⭐ $stars" else ""
                        val cleanDesc = if (desc.isNotBlank() && desc != "null") desc else "Open-source repository on GitHub."

                        list.add(
                            WebSearchResult(
                                title = "$fullName$langText",
                                snippet = "$cleanDesc$starsText",
                                url = htmlUrl,
                                source = "GitHub",
                                pubDate = if (stars > 0) "⭐ $stars" else "Repository",
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
     * Hacker News Real-time Tech & Discussion (Algolia).
     */
    private fun fetchHnSearch(query: String): List<WebSearchResult> {
        val list = mutableListOf<WebSearchResult>()
        try {
            val encoded = URLEncoder.encode(query, "UTF-8")
            val url = "https://hn.algolia.com/api/v1/search?query=$encoded&hitsPerPage=4"
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
                    val rawUrl = hit.optString("url")
                    val objectId = hit.optString("objectID")
                    val points = hit.optInt("points", 0)
                    val comments = hit.optInt("num_comments", 0)
                    val author = hit.optString("author")
                    val storyText = hit.optString("story_text")

                    val finalUrl = if (rawUrl.isNotBlank()) rawUrl else "https://news.ycombinator.com/item?id=$objectId"

                    if (title.isNotBlank()) {
                        val snippet = if (storyText.isNotBlank()) {
                            cleanHtml(storyText).take(220)
                        } else {
                            "Hacker News discussion: $points points, $comments comments by $author."
                        }

                        list.add(
                            WebSearchResult(
                                title = title,
                                snippet = snippet,
                                url = finalUrl,
                                source = "Hacker News",
                                pubDate = if (points > 0) "$points pts ($comments comments)" else "Tech",
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
     * Wikiquote Notable Quotes & Speeches.
     */
    private fun fetchWikiquoteSearch(query: String): List<WebSearchResult> {
        val list = mutableListOf<WebSearchResult>()
        try {
            val encoded = URLEncoder.encode(query, "UTF-8")
            val url = "https://en.wikiquote.org/w/api.php?action=query&list=search&srsearch=$encoded&format=json&srlimit=3"
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
                    val pageUrl = "https://en.wikiquote.org/wiki/" + URLEncoder.encode(title.replace(" ", "_"), "UTF-8")
                    if (title.isNotBlank() && snippet.isNotBlank()) {
                        list.add(
                            WebSearchResult(
                                title = "$title (Quotes)",
                                snippet = "$snippet...",
                                url = pageUrl,
                                source = "Wikiquote",
                                pubDate = "Quotes",
                                category = "Knowledge"
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
     * Wikinews Free News Reports.
     */
    private fun fetchWikinewsSearch(query: String): List<WebSearchResult> {
        val list = mutableListOf<WebSearchResult>()
        try {
            val encoded = URLEncoder.encode(query, "UTF-8")
            val url = "https://en.wikinews.org/w/api.php?action=query&list=search&srsearch=$encoded&format=json&srlimit=3"
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
                    val pageUrl = "https://en.wikinews.org/wiki/" + URLEncoder.encode(title.replace(" ", "_"), "UTF-8")
                    if (title.isNotBlank()) {
                        list.add(
                            WebSearchResult(
                                title = title,
                                snippet = if (snippet.isNotBlank()) "$snippet..." else "Wikinews coverage of $title.",
                                url = pageUrl,
                                source = "Wikinews",
                                pubDate = formatPubDate(item.optString("timestamp")),
                                category = "News"
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

    /**
     * PubMed / NCBI - Over 36 million biomedical citations and research articles.
     */
    private fun fetchPubMedSearch(query: String): List<WebSearchResult> {
        val list = mutableListOf<WebSearchResult>()
        try {
            val encoded = URLEncoder.encode(query, "UTF-8")
            val searchUrl = "https://eutils.ncbi.nlm.nih.gov/entrez/eutils/esearch.fcgi?db=pubmed&term=$encoded&retmode=json&retmax=4"
            val request = Request.Builder()
                .url(searchUrl)
                .header("User-Agent", "Mozilla/5.0 (Android; Mobile; rv:125.0) OmniChat/1.0")
                .build()

            val body = client.newCall(request).execute().use { response ->
                if (response.isSuccessful) response.body?.string().orEmpty() else ""
            }

            if (body.isNotBlank()) {
                val root = JSONObject(body)
                val idList = root.optJSONObject("esearchresult")?.optJSONArray("idlist")
                if (idList != null && idList.length() > 0) {
                    val ids = mutableListOf<String>()
                    for (i in 0 until idList.length()) {
                        ids.add(idList.optString(i))
                    }
                    val idsParam = ids.joinToString(",")
                    val sumUrl = "https://eutils.ncbi.nlm.nih.gov/entrez/eutils/esummary.fcgi?db=pubmed&id=$idsParam&retmode=json"
                    val sumReq = Request.Builder()
                        .url(sumUrl)
                        .header("User-Agent", "Mozilla/5.0 (Android; Mobile; rv:125.0) OmniChat/1.0")
                        .build()
                    val sumBody = client.newCall(sumReq).execute().use { r ->
                        if (r.isSuccessful) r.body?.string().orEmpty() else ""
                    }
                    if (sumBody.isNotBlank()) {
                        val sumRoot = JSONObject(sumBody)
                        val resObj = sumRoot.optJSONObject("result")
                        if (resObj != null) {
                            for (id in ids) {
                                val item = resObj.optJSONObject(id) ?: continue
                                val rawTitle = item.optString("title")
                                val cleanTitle = cleanHtml(rawTitle).trimEnd('.')
                                val journal = item.optString("source")
                                val pubDate = item.optString("pubdate")
                                val articleUrl = "https://pubmed.ncbi.nlm.nih.gov/$id/"

                                if (cleanTitle.isNotBlank()) {
                                    list.add(
                                        WebSearchResult(
                                            title = cleanTitle,
                                            snippet = if (journal.isNotBlank()) "Biomedical research published in $journal. PubMed ID: $id." else "National Library of Medicine PubMed research. ID: $id.",
                                            url = articleUrl,
                                            source = "PubMed",
                                            pubDate = formatPubDate(pubDate),
                                            category = "Science & Medicine"
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
     * DEV.to - Premier global software developer and engineering community.
     */
    private fun fetchDevToSearch(query: String): List<WebSearchResult> {
        val list = mutableListOf<WebSearchResult>()
        try {
            val encoded = URLEncoder.encode(query, "UTF-8")
            val url = "https://dev.to/api/articles?search=$encoded&per_page=4"
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Android; Mobile; rv:125.0) OmniChat/1.0")
                .build()

            val body = client.newCall(request).execute().use { response ->
                if (response.isSuccessful) response.body?.string().orEmpty() else ""
            }

            if (body.isNotBlank()) {
                val array = JSONArray(body)
                for (i in 0 until array.length()) {
                    val item = array.optJSONObject(i) ?: continue
                    val title = item.optString("title").trim()
                    val desc = item.optString("description").trim()
                    val urlVal = item.optString("url")
                    val dateVal = item.optString("readable_publish_date")

                    if (title.isNotBlank() && urlVal.isNotBlank()) {
                        list.add(
                            WebSearchResult(
                                title = title,
                                snippet = if (desc.isNotBlank()) cleanHtml(desc) else "Developer article on DEV Community.",
                                url = urlVal,
                                source = "DEV.to",
                                pubDate = if (dateVal.isNotBlank()) dateVal else "Dev Article",
                                category = "Code & Dev"
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
     * OpenLibrary - Free catalog of millions of books, literature and authors.
     */
    private fun fetchOpenLibrarySearch(query: String): List<WebSearchResult> {
        val list = mutableListOf<WebSearchResult>()
        try {
            val encoded = URLEncoder.encode(query, "UTF-8")
            val url = "https://openlibrary.org/search.json?q=$encoded&limit=4"
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Android; Mobile; rv:125.0) OmniChat/1.0")
                .build()

            val body = client.newCall(request).execute().use { response ->
                if (response.isSuccessful) response.body?.string().orEmpty() else ""
            }

            if (body.isNotBlank()) {
                val root = JSONObject(body)
                val docs = root.optJSONArray("docs") ?: JSONArray()
                for (i in 0 until docs.length()) {
                    val item = docs.optJSONObject(i) ?: continue
                    val title = item.optString("title").trim()
                    val key = item.optString("key")
                    val authorsArr = item.optJSONArray("author_name")
                    val author = if (authorsArr != null && authorsArr.length() > 0) authorsArr.optString(0) else ""
                    val year = item.optInt("first_publish_year", 0)
                    val bookUrl = "https://openlibrary.org$key"

                    if (title.isNotBlank() && key.isNotBlank()) {
                        val snippet = buildString {
                            if (author.isNotBlank()) append("Book by $author. ")
                            if (year > 0) append("First published in $year. ")
                            append("Available in Open Library universal catalog.")
                        }
                        list.add(
                            WebSearchResult(
                                title = "$title (Book)",
                                snippet = snippet,
                                url = bookUrl,
                                source = "OpenLibrary",
                                pubDate = if (year > 0) year.toString() else "Book",
                                category = "Books & Literature"
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
     * Internet Archive - Non-profit library of millions of books, media, and digital documents.
     */
    private fun fetchInternetArchiveSearch(query: String): List<WebSearchResult> {
        val list = mutableListOf<WebSearchResult>()
        try {
            val encoded = URLEncoder.encode(query, "UTF-8")
            val url = "https://archive.org/advancedsearch.php?q=$encoded&fl[]=identifier,title,description,mediatype&rows=4&output=json"
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Android; Mobile; rv:125.0) OmniChat/1.0")
                .build()

            val body = client.newCall(request).execute().use { response ->
                if (response.isSuccessful) response.body?.string().orEmpty() else ""
            }

            if (body.isNotBlank()) {
                val root = JSONObject(body)
                val docs = root.optJSONObject("response")?.optJSONArray("docs") ?: JSONArray()
                for (i in 0 until docs.length()) {
                    val item = docs.optJSONObject(i) ?: continue
                    val identifier = item.optString("identifier")
                    val title = item.optString("title").trim()
                    val rawDesc = item.optString("description")
                    val cleanDesc = cleanHtml(rawDesc)
                    val mediaType = item.optString("mediatype", "archive")
                    val itemUrl = "https://archive.org/details/$identifier"

                    if (identifier.isNotBlank() && title.isNotBlank()) {
                        list.add(
                            WebSearchResult(
                                title = title,
                                snippet = if (cleanDesc.isNotBlank()) "$cleanDesc..." else "Historical digital resource preserved on archive.org ($mediaType).",
                                url = itemUrl,
                                source = "Internet Archive",
                                pubDate = mediaType.replaceFirstChar { it.uppercase() },
                                category = "Archive & History"
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
