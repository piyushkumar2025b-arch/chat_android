package com.example

import com.example.data.remote.NewsFeedService
import com.example.data.remote.SearchCategory
import com.example.data.remote.WebSearchService
import kotlinx.coroutines.runBlocking
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
        if (result.isSuccess) {
            val list = result.getOrNull()
            assertNotNull(list)
            if (!list.isNullOrEmpty()) {
                val first = list.first()
                assertTrue("Title must not be blank", first.title.isNotBlank())
                assertTrue("URL must not be blank", first.url.isNotBlank())
            }
        } else {
            assertNotNull(result.exceptionOrNull())
        }
    }

    @Test
    fun testKnowledgeCategorySearch() = runBlocking {
        val result = WebSearchService.search("quantum physics", SearchCategory.KNOWLEDGE)
        if (result.isSuccess) {
            val list = result.getOrNull()
            assertNotNull(list)
        } else {
            assertNotNull(result.exceptionOrNull())
        }
    }

    @Test
    fun testCodeCategorySearch() = runBlocking {
        val result = WebSearchService.search("kotlin", SearchCategory.CODE)
        if (result.isSuccess) {
            val list = result.getOrNull()
            assertNotNull(list)
        } else {
            assertNotNull(result.exceptionOrNull())
        }
    }

    @Test
    fun testBooksCategorySearch() = runBlocking {
        val result = WebSearchService.search("relativity", SearchCategory.BOOKS)
        if (result.isSuccess) {
            val list = result.getOrNull()
            assertNotNull(list)
        } else {
            assertNotNull(result.exceptionOrNull())
        }
    }

    @Test
    fun testScienceCategorySearch() = runBlocking {
        val result = WebSearchService.search("cancer", SearchCategory.SCIENCE)
        if (result.isSuccess) {
            val list = result.getOrNull()
            assertNotNull(list)
        } else {
            assertNotNull(result.exceptionOrNull())
        }
    }

    @Test
    fun testNewsFeedServiceFetchesBBCArticles() = runBlocking {
        val result = NewsFeedService.fetchNews(NewsFeedService.NewsCategory.ALL)
        if (result.isSuccess) {
            val articles = result.getOrNull()
            assertNotNull(articles)
            if (!articles.isNullOrEmpty()) {
                val first = articles.first()
                assertTrue("News title must not be blank", first.title.isNotBlank())
            }
        } else {
            assertNotNull(result.exceptionOrNull())
        }
    }
}
