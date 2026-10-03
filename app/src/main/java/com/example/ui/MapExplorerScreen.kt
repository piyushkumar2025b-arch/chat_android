package com.example.ui

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import java.net.URLEncoder
import java.util.Locale
import java.util.concurrent.TimeUnit

data class MapLocation(
    val name: String,
    val lat: Double,
    val lon: Double,
    val displayName: String,
    val type: String
)

enum class MapTileLayer(val label: String, val tileUrl: String, val maxZoom: Int) {
    OSM_STANDARD("OpenStreetMap", "https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png", 19),
    CARTO_DARK("Carto Dark", "https://cartodb-basemaps-a.global.ssl.fastly.net/dark_all/{z}/{x}/{y}.png", 19),
    CARTO_LIGHT("Carto Positron", "https://cartodb-basemaps-a.global.ssl.fastly.net/light_all/{z}/{x}/{y}.png", 19),
    OPENTOPO("OpenTopo Relief", "https://tile.opentopomap.org/{z}/{x}/{y}.png", 17)
}

@Composable
fun MapExplorerScreen(
    onSendLocationToChat: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var searchQuery by remember { mutableStateOf("") }
    var isSearching by remember { mutableStateOf(false) }
    var selectedLayer by remember { mutableStateOf(MapTileLayer.OSM_STANDARD) }
    var currentLocation by remember {
        mutableStateOf(
            MapLocation(
                name = "Tokyo, Japan",
                lat = 35.6762,
                lon = 139.6503,
                displayName = "Tokyo, Japan",
                type = "City"
            )
        )
    }

    val searchResults = remember { mutableStateListOf<MapLocation>() }
    var currentZoom by remember { mutableIntStateOf(14) }

    val quickDestinations = listOf(
        Pair("Tokyo", Pair(35.6762, 139.6503)),
        Pair("Paris", Pair(48.8566, 2.3522)),
        Pair("New York", Pair(40.7128, -74.0060)),
        Pair("Cairo", Pair(30.0444, 31.2357)),
        Pair("San Francisco", Pair(37.7749, -122.4194)),
        Pair("Sydney", Pair(-33.8688, 151.2093)),
        Pair("Rome", Pair(41.9028, 12.4964))
    )

    fun searchLocations() {
        val q = searchQuery.trim()
        if (q.isEmpty()) return

        isSearching = true
        coroutineScope.launch {
            try {
                val encoded = URLEncoder.encode(q, "UTF-8")
                val url = "https://nominatim.openstreetmap.org/search?format=json&q=$encoded&addressdetails=1&limit=6"
                val client = OkHttpClient.Builder()
                    .connectTimeout(12, TimeUnit.SECONDS)
                    .readTimeout(12, TimeUnit.SECONDS)
                    .build()
                val req = Request.Builder()
                    .url(url)
                    .header("User-Agent", "OmniChat-Android/1.0 (Android AI Studio)")
                    .build()

                val items = withContext(Dispatchers.IO) {
                    client.newCall(req).execute().use { res ->
                        if (!res.isSuccessful) return@withContext emptyList<MapLocation>()
                        val body = res.body?.string().orEmpty()
                        if (body.isBlank()) return@withContext emptyList<MapLocation>()
                        val array = JSONArray(body)
                        val list = mutableListOf<MapLocation>()
                        for (i in 0 until array.length()) {
                            val obj = array.getJSONObject(i)
                            val name = obj.optString("name").ifEmpty { obj.optString("display_name").substringBefore(",") }
                            val lat = obj.optDouble("lat", 0.0)
                            val lon = obj.optDouble("lon", 0.0)
                            val disp = obj.optString("display_name")
                            val type = obj.optString("type", "Place")
                            list.add(MapLocation(name = name, lat = lat, lon = lon, displayName = disp, type = type))
                        }
                        list
                    }
                }
                searchResults.clear()
                searchResults.addAll(items)
                if (items.isEmpty()) {
                    Toast.makeText(context, "No locations found for '$q'", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Search failed: ${e.localizedMessage ?: "Network error"}", Toast.LENGTH_SHORT).show()
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
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Explore,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        "Free Maps & Travel Explorer",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Powered by OpenStreetMap & Nominatim • 100% Free & Open",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Search Bar Card
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
                            .testTag("map_search_input"),
                        placeholder = { Text("Search any city, landmark, or place...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = ""; searchResults.clear() }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear")
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = { searchLocations() },
                        enabled = !isSearching && searchQuery.isNotBlank(),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("map_search_button")
                    ) {
                        if (isSearching) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        } else {
                            Text("Find")
                        }
                    }
                }
            }
        }

        // Quick City Pills
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(quickDestinations) { (cityName, coords) ->
                    val isCitySelected = currentLocation.name.equals(cityName, ignoreCase = true) ||
                            (Math.abs(currentLocation.lat - coords.first) < 0.05 && Math.abs(currentLocation.lon - coords.second) < 0.05)
                    FilterChip(
                        selected = isCitySelected,
                        onClick = {
                            currentLocation = MapLocation(
                                name = cityName,
                                lat = coords.first,
                                lon = coords.second,
                                displayName = "$cityName, Coordinates: ${coords.first}, ${coords.second}",
                                type = "City"
                            )
                            currentZoom = 13
                        },
                        label = { Text(cityName) },
                        leadingIcon = {
                            Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(14.dp))
                        }
                    )
                }
            }
        }

        // Search Results List (if any)
        if (searchResults.isNotEmpty()) {
            item {
                Text("Search Results (${searchResults.size})", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            }
            items(searchResults) { loc ->
                Card(
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            currentLocation = loc
                            searchResults.clear()
                            currentZoom = 14
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(loc.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            Text(loc.displayName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                        }
                    }
                }
            }
        }

        // Map Tile Layer Selector
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Map Style Layer", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(4.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(MapTileLayer.values()) { layer ->
                    FilterChip(
                        selected = selectedLayer == layer,
                        onClick = {
                            selectedLayer = layer
                            if (currentZoom > layer.maxZoom) {
                                currentZoom = layer.maxZoom
                            }
                        },
                        label = { Text(layer.label) },
                        leadingIcon = if (selectedLayer == layer) {
                            { Icon(Icons.Default.Layers, contentDescription = null, modifier = Modifier.size(14.dp)) }
                        } else null
                    )
                }
            }
        }

        // Interactive Map View Container
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(280.dp)
                            .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                            .background(MaterialTheme.colorScheme.surface)
                    ) {
                        // 3x3 Tile Grid with sub-tile coordinate precision
                        val n = 1 shl currentZoom
                        val exactTileX = (currentLocation.lon + 180.0) / 360.0 * n
                        val latRad = Math.toRadians(currentLocation.lat.coerceIn(-85.0511, 85.0511))
                        val exactTileY = ((1.0 - Math.log(Math.tan(latRad) + 1.0 / Math.cos(latRad)) / Math.PI) / 2.0 * n)

                        val centerTileX = exactTileX.toInt().coerceIn(0, n - 1)
                        val centerTileY = exactTileY.toInt().coerceIn(0, n - 1)
                        val fracX = (exactTileX - centerTileX - 0.5).toFloat()
                        val fracY = (exactTileY - centerTileY - 0.5).toFloat()

                        Column(modifier = Modifier.fillMaxSize()) {
                            for (dy in -1..1) {
                                Row(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth()
                                 ) {
                                    for (dx in -1..1) {
                                        val tileX = (centerTileX + dx).mod(n)
                                        val tileY = (centerTileY + dy).coerceIn(0, n - 1)
                                        val tileUrl = selectedLayer.tileUrl
                                            .replace("{s}", "a")
                                            .replace("{z}", currentZoom.toString())
                                            .replace("{x}", tileX.toString())
                                            .replace("{y}", tileY.toString())

                                        AsyncImage(
                                            model = tileUrl,
                                            contentDescription = "Map Tile",
                                            modifier = Modifier
                                                .weight(1f)
                                                .fillMaxHeight(),
                                            contentScale = ContentScale.Crop
                                        )
                                    }
                                }
                            }
                        }

                        // Center Pin Marker with sub-tile pixel offset
                        BoxWithConstraints(
                            modifier = Modifier.fillMaxSize()
                        ) {
                            val tileWidth = maxWidth / 3f
                            val tileHeight = maxHeight / 3f
                            val pinXOffset = tileWidth * fracX
                            val pinYOffset = tileHeight * fracY - 14.dp

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .offset(x = pinXOffset, y = pinYOffset)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                                    shadowElevation = 4.dp
                                ) {
                                    Text(
                                        text = currentLocation.name,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Default.LocationOn,
                                    contentDescription = "Marker",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                        }

                        // Zoom Controls (+ / -) in Top-Right
                        Column(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                                shadowElevation = 3.dp
                            ) {
                                IconButton(
                                    onClick = { if (currentZoom < selectedLayer.maxZoom) currentZoom++ },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Text("+", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                                }
                            }
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                                shadowElevation = 3.dp
                            ) {
                                IconButton(
                                    onClick = { if (currentZoom > 2) currentZoom-- },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Text("−", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                                }
                            }
                        }

                        // Zoom & Layer Badge in Top-Left
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(8.dp)
                        ) {
                            Text(
                                text = "Zoom Level $currentZoom • ${selectedLayer.label}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    // Location Information Bar & AI Actions
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    currentLocation.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    "${String.format(Locale.US, "%.4f", currentLocation.lat)}, ${String.format(Locale.US, "%.4f", currentLocation.lon)} • ${currentLocation.type}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            IconButton(
                                onClick = {
                                    val geoUri = Uri.parse("geo:${currentLocation.lat},${currentLocation.lon}?q=${currentLocation.lat},${currentLocation.lon}(${Uri.encode(currentLocation.name)})")
                                    val mapIntent = Intent(Intent.ACTION_VIEW, geoUri)
                                    try {
                                        context.startActivity(mapIntent)
                                    } catch (_: Exception) {
                                        try {
                                            val osmWeb = "https://www.openstreetmap.org/#map=14/${currentLocation.lat}/${currentLocation.lon}"
                                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(osmWeb)))
                                        } catch (_: Exception) {}
                                    }
                                }
                            ) {
                                Icon(Icons.Default.OpenInBrowser, contentDescription = "Open External Map")
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // AI Actions
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    val prompt = "Create a 3-day travel itinerary with top attractions, hidden gems, and local food recommendations for: ${currentLocation.displayName} (Coordinates: ${currentLocation.lat}, ${currentLocation.lon})"
                                    onSendLocationToChat(prompt)
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Plan Itinerary", style = MaterialTheme.typography.labelSmall)
                            }

                            Button(
                                onClick = {
                                    val prompt = "Tell me the rich history, cultural significance, and fascinating facts about: ${currentLocation.displayName}"
                                    onSendLocationToChat(prompt)
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Ask AI History", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }
    }
}
