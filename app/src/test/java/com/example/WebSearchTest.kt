package com.example

import com.example.data.remote.NewsFeedService
import com.example.data.remote.WebSearchService
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class WebSearchTest {

    @Test
    fun testEmptyQueryReturnsEmptyList() = runBlocking {
        val result = WebSearchService.search("")
        assertTrue(result.isSuccess)
        assertTrue(result.getOrNull()?.isEmpty() == true)
    }

    @Test
    fun testWebSearchReturnsLiveResults() = runBlocking {
        val result = WebSearchService.search("artificial intelligence")
        assertTrue(result.isSuccess)
        val list = result.getOrNull()
        assertNotNull(list)
        assertTrue("Web search should return results", list!!.isNotEmpty())
        val first = list.first()
        assertTrue("Title must not be blank", first.title.isNotBlank())
        assertTrue("Snippet must not be blank", first.snippet.isNotBlank())
        assertTrue("URL must not be blank", first.url.isNotBlank())
    }

    @Test
    fun testNewsFeedServiceFetchesBBCArticles() = runBlocking {
        val result = NewsFeedService.fetchNews(NewsFeedService.NewsCategory.ALL)
        assertTrue(result.isSuccess)
        val articles = result.getOrNull()
        assertNotNull(articles)
        assertTrue("News feed should return articles", articles!!.isNotEmpty())
        val first = articles.first()
        assertTrue("News title must not be blank", first.title.isNotBlank())
        assertTrue("News link must not be blank", first.link.isNotBlank())
    }
}
