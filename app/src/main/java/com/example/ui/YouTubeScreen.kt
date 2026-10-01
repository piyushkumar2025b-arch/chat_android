package com.example.ui

import android.content.Intent
import android.net.Uri
import android.view.View
import android.view.ViewGroup
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import com.example.data.local.PreferencesManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

data class YouTubeVideoItem(
    val videoId: String,
    val title: String,
    val channelTitle: String,
    val description: String,
    val thumbnailUrl: String
)

val FEATURED_VIDEOS = listOf(
    YouTubeVideoItem(
        videoId = "zjkBMFhNj_g",
        title = "Intro to Large Language Models",
        channelTitle = "Andrej Karpathy",
        description = "The busy person's intro to LLMs: what they are, how they work, where they are going, and security challenges.",
        thumbnailUrl = "https://i.ytimg.com/vi/zjkBMFhNj_g/hqdefault.jpg"
    ),
    YouTubeVideoItem(
        videoId = "aircAruvnKk",
        title = "Neural Networks from Scratch",
        channelTitle = "3Blue1Brown",
        description = "What is a neural network? A visual introduction to deep learning and backpropagation.",
        thumbnailUrl = "https://i.ytimg.com/vi/aircAruvnKk/hqdefault.jpg"
    ),
    YouTubeVideoItem(
        videoId = "bZQun8Y4L2A",
        title = "Generative AI in 5 Minutes",
        channelTitle = "Google Cloud Tech",
        description = "Overview of foundational generative AI models, transformer architectures, and fine-tuning techniques.",
        thumbnailUrl = "https://i.ytimg.com/vi/bZQun8Y4L2A/hqdefault.jpg"
    )
)

@Composable
fun YouTubeScreen(
    preferencesManager: PreferencesManager,
    onOpenSettings: () -> Unit = {},
    onSummarizeVideoInChat: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var searchQuery by remember { mutableStateOf("") }
    var isSearching by remember { mutableStateOf(false) }
    var activeVideo by remember { mutableStateOf<YouTubeVideoItem?>(FEATURED_VIDEOS.first()) }
    var isPlayingInline by remember { mutableStateOf(false) }
    val searchResults = remember { mutableStateListOf<YouTubeVideoItem>() }

    val hasKey = preferencesManager.youtubeApiKey.isNotBlank()

    fun searchYouTube() {
        val q = searchQuery.trim()
        if (q.isEmpty()) return

        val key = preferencesManager.youtubeApiKey
        if (key.isBlank()) {
            Toast.makeText(context, "Please enter your YouTube API key in Settings to search live YouTube", Toast.LENGTH_LONG).show()
            onOpenSettings()
            return
        }

        isSearching = true
        coroutineScope.launch {
            try {
                val encoded = URLEncoder.encode(q, "UTF-8")
                val url = "https://www.googleapis.com/youtube/v3/search?part=snippet&maxResults=10&q=$encoded&type=video&key=$key"
                val client = OkHttpClient.Builder().connectTimeout(12, TimeUnit.SECONDS).build()
                val req = Request.Builder().url(url).build()

                val res = withContext(Dispatchers.IO) { client.newCall(req).execute() }
                if (res.isSuccessful) {
                    val body = res.body?.string().orEmpty()
                    val json = JSONObject(body)
                    val items = json.optJSONArray("items")
                    searchResults.clear()
                    if (items != null) {
                        for (i in 0 until items.length()) {
                            val item = items.getJSONObject(i)
                            val idObj = item.optJSONObject("id")
                            val videoId = idObj?.optString("videoId") ?: continue
                            val snippet = item.optJSONObject("snippet") ?: continue
                            val title = snippet.optString("title")
                                .replace("&amp;", "&")
                                .replace("&quot;", "\"")
                                .replace("&#39;", "'")
                            val channel = snippet.optString("channelTitle")
                            val desc = snippet.optString("description")
                            val thumb = snippet.optJSONObject("thumbnails")
                                ?.optJSONObject("medium")?.optString("url")
                                ?: "https://i.ytimg.com/vi/$videoId/hqdefault.jpg"

                            searchResults.add(YouTubeVideoItem(videoId, title, channel, desc, thumb))
                        }
                    }
                    if (searchResults.isNotEmpty()) {
                        activeVideo = searchResults.first()
                    }
                } else {
                    val code = res.code
                    Toast.makeText(context, "YouTube API error ($code). Check your API key in Settings.", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Search failed: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            } finally {
                isSearching = false
            }
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.errorContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.VideoLibrary,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        "YouTube Search & Player",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        if (hasKey) "YouTube Data API v3 Active • Search & In-App Player"
                        else "Add YouTube API Key in Settings to search any video",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Missing API Key Warning & Setup Card
        if (!hasKey) {
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Key, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("YouTube API Key Setup", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Text("Enter your free Google Cloud YouTube Data API v3 key in Settings to search all videos.", style = MaterialTheme.typography.bodySmall)
                        }
                        Button(
                            onClick = onOpenSettings,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Add Key")
                        }
                    }
                }
            }
        }

        // Active Player Card
        if (activeVideo != null) {
            val vid = activeVideo!!
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        // Embedded HTML5 YouTube Player or Thumbnail Preview
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(16f / 9f)
                                .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                                .background(MaterialTheme.colorScheme.surface)
                        ) {
                            if (isPlayingInline) {
                                AndroidView(
                                    factory = { ctx ->
                                        WebView(ctx).apply {
                                            // Software layer avoids MESA rendernode lookup in container environments
                                            setLayerType(View.LAYER_TYPE_SOFTWARE, null)
                                            settings.javaScriptEnabled = true
                                            settings.domStorageEnabled = true
                                            settings.mediaPlaybackRequiresUserGesture = false
                                            webViewClient = object : WebViewClient() {
                                                override fun onRenderProcessGone(view: WebView?, detail: RenderProcessGoneDetail?): Boolean {
                                                    try {
                                                        view?.let {
                                                            (it.parent as? ViewGroup)?.removeView(it)
                                                            it.destroy()
                                                        }
                                                    } catch (_: Exception) {}
                                                    isPlayingInline = false
                                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://youtube.com/watch?v=${vid.videoId}"))
                                                    try { context.startActivity(intent) } catch (_: Exception) {}
                                                    return true
                                                }
                                            }
                                            val embedHtml = """
                                                <!DOCTYPE html>
                                                <html>
                                                <head>
                                                    <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no" />
                                                    <style>body,html,iframe { margin:0; padding:0; width:100%; height:100%; background:#000; }</style>
                                                </head>
                                                <body>
                                                    <iframe src="https://www.youtube.com/embed/${vid.videoId}?autoplay=1&playsinline=1" frameborder="0" allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture" allowfullscreen></iframe>
                                                </body>
                                                </html>
                                            """.trimIndent()
                                            loadDataWithBaseURL("https://www.youtube.com", embedHtml, "text/html", "UTF-8", null)
                                        }
                                    },
                                    update = { wv ->
                                        val embedHtml = """
                                            <!DOCTYPE html>
                                            <html>
                                            <head>
                                                <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no" />
                                                <style>body,html,iframe { margin:0; padding:0; width:100%; height:100%; background:#000; }</style>
                                            </head>
                                            <body>
                                                <iframe src="https://www.youtube.com/embed/${vid.videoId}?autoplay=1&playsinline=1" frameborder="0" allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture" allowfullscreen></iframe>
                                            </body>
                                            </html>
                                        """.trimIndent()
                                        wv.loadDataWithBaseURL("https://www.youtube.com", embedHtml, "text/html", "UTF-8", null)
                                    },
                                    onRelease = { wv ->
                                        try {
                                            wv.stopLoading()
                                            wv.loadUrl("about:blank")
                                            wv.destroy()
                                        } catch (_: Exception) {}
                                    },
                                    modifier = Modifier.fillMaxSize()
                                )

                                IconButton(
                                    onClick = { isPlayingInline = false },
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(8.dp)
                                        .size(36.dp)
                                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.85f), CircleShape)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Stop playback",
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            } else {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clickable { isPlayingInline = true },
                                    contentAlignment = Alignment.Center
                                ) {
                                    AsyncImage(
                                        model = vid.thumbnailUrl,
                                        contentDescription = vid.title,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )

                                    Surface(
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.error,
                                        shadowElevation = 6.dp,
                                        modifier = Modifier.size(56.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Default.PlayArrow,
                                                contentDescription = "Play video",
                                                tint = MaterialTheme.colorScheme.onError,
                                                modifier = Modifier.size(36.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Video Title and Information
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(vid.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(vid.channelTitle, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                            if (vid.description.isNotBlank()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(vid.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Button(
                                    onClick = {
                                        val prompt = "Please summarize the core ideas and key takeaways of this YouTube video:\n\nTitle: ${vid.title}\nChannel: ${vid.channelTitle}\nDescription: ${vid.description}\nLink: https://youtube.com/watch?v=${vid.videoId}"
                                        onSummarizeVideoInChat(prompt)
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary
                                    ),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Summarize with AI", style = MaterialTheme.typography.labelSmall)
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    IconButton(
                                        onClick = {
                                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                                type = "text/plain"
                                                putExtra(Intent.EXTRA_TEXT, "Watch: https://youtube.com/watch?v=${vid.videoId}\n\n${vid.title}")
                                            }
                                            context.startActivity(Intent.createChooser(shareIntent, "Share Video"))
                                        }
                                    ) {
                                        Icon(Icons.Default.Share, contentDescription = "Share Video")
                                    }

                                    IconButton(
                                        onClick = {
                                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://youtube.com/watch?v=${vid.videoId}"))
                                            context.startActivity(intent)
                                        }
                                    ) {
                                        Icon(Icons.Default.OpenInBrowser, contentDescription = "Open in YouTube App")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Search Input Card
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("youtube_search_input"),
                        placeholder = { Text("Search videos on YouTube...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear")
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = { searchYouTube() },
                        enabled = !isSearching && searchQuery.isNotBlank(),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("youtube_search_button")
                    ) {
                        if (isSearching) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        } else {
                            Text("Search")
                        }
                    }
                }
            }
        }

        // Video Results or Featured Videos
        item {
            Text(
                if (searchResults.isNotEmpty()) "Search Results (${searchResults.size})" else "Featured AI & Tech Videos",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        val displayList = if (searchResults.isNotEmpty()) searchResults else FEATURED_VIDEOS
        items(displayList) { video ->
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (activeVideo?.videoId == video.videoId) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { activeVideo = video }
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(width = 100.dp, height = 65.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        AsyncImage(
                            model = video.thumbnailUrl,
                            contentDescription = video.title,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.8f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = "Play", tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(18.dp))
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(video.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(video.channelTitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }
    }
}
