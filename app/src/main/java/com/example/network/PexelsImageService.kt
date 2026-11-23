package com.example.network

import android.util.Log
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/**
 * Fetches live, royalty-free HD product/food/grocery/fashion photos from Pexels API
 * using BuildConfig.PEXELS_API_KEY (configured via AI Studio Secrets or GitHub Secret PEXELS_API_KEY).
 * Gracefully falls back to bundled 3D drawables whenever the key is missing or device is offline.
 */
object PexelsImageService {

    private val urlCache = ConcurrentHashMap<String, String>()
    private val failedQueries = ConcurrentHashMap.newKeySet<String>()

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(6, TimeUnit.SECONDS)
            .readTimeout(6, TimeUnit.SECONDS)
            .build()
    }

    fun isConfigured(): Boolean {
        val key = BuildConfig.PEXELS_API_KEY
        return key.isNotBlank() && !key.startsWith("YOUR_") && !key.startsWith("MY_")
    }

    suspend fun fetchPhotoUrl(query: String): String? {
        val normalized = query.trim().lowercase()
        if (normalized.isEmpty()) return null
        urlCache[normalized]?.let { return it }
        if (!isConfigured() || normalized in failedQueries) return null

        return withContext(Dispatchers.IO) {
            try {
                val encoded = URLEncoder.encode(normalized, "UTF-8")
                val request = Request.Builder()
                    .url("https://api.pexels.com/v1/search?query=$encoded&per_page=1&orientation=landscape")
                    .header("Authorization", BuildConfig.PEXELS_API_KEY)
                    .get()
                    .build()

                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        Log.w("PexelsImageService", "Pexels HTTP ${response.code} for '$normalized'")
                        failedQueries.add(normalized)
                        return@withContext null
                    }
                    val body = response.body?.string() ?: return@withContext null
                    val photos = JSONObject(body).optJSONArray("photos")
                    val first = photos?.optJSONObject(0)
                    val src = first?.optJSONObject("src")
                    val url = src?.optString("medium").takeUnless { it.isNullOrBlank() }
                        ?: src?.optString("large")
                    if (!url.isNullOrBlank()) {
                        urlCache[normalized] = url
                        url
                    } else {
                        failedQueries.add(normalized)
                        null
                    }
                }
            } catch (e: Exception) {
                Log.w("PexelsImageService", "Pexels lookup failed for '$normalized': ${e.message}")
                null
            }
        }
    }
}

/**
 * Drop-in photo composable:
 * 1) Renders direct ImgBB uploaded HTTPS URL when provided.
 * 2) Otherwise fetches live HD photo from Pexels when PEXELS_API_KEY is configured.
 * 3) Gracefully falls back to bundled 3D drawable while loading or offline.
 */
@Composable
fun TakeItSmartPhoto(
    query: String,
    @DrawableRes fallbackDrawableRes: Int,
    contentDescription: String,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    directImageUrl: String? = null
) {
    var remoteUrl by remember(query, directImageUrl) {
        mutableStateOf(directImageUrl?.takeIf { it.isNotBlank() })
    }

    LaunchedEffect(query, directImageUrl) {
        if (!directImageUrl.isNullOrBlank()) {
            remoteUrl = directImageUrl
        } else if (PexelsImageService.isConfigured()) {
            remoteUrl = PexelsImageService.fetchPhotoUrl(query)
        }
    }

    if (!remoteUrl.isNullOrBlank()) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(remoteUrl)
                .crossfade(true)
                .build(),
            placeholder = painterResource(fallbackDrawableRes),
            error = painterResource(fallbackDrawableRes),
            contentDescription = contentDescription,
            contentScale = contentScale,
            modifier = modifier
        )
    } else {
        Image(
            painter = painterResource(fallbackDrawableRes),
            contentDescription = contentDescription,
            contentScale = contentScale,
            modifier = modifier
        )
    }
}
