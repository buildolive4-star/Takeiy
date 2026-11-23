package com.example.network

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Real ImgBB Image Upload Client (https://api.imgbb.com/1/upload).
 * Uses BuildConfig.IMGBB_API_KEY configured via AI Studio Secrets panel or GitHub Actions Secret IMGBB_API_KEY.
 */
object ImgBbUploadService {

    private const val TAG = "ImgBbUploadService"
    private const val UPLOAD_ENDPOINT = "https://api.imgbb.com/1/upload"

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    fun isConfigured(): Boolean {
        val key = BuildConfig.IMGBB_API_KEY
        return key.isNotBlank() && !key.startsWith("YOUR_") && !key.startsWith("MY_")
    }

    /**
     * Reads an image selected from Android's zero-permission Photo Picker, compresses it safely,
     * uploads it to ImgBB, and returns the permanent HTTPS image URL.
     */
    suspend fun uploadImageUri(
        context: Context,
        uri: Uri,
        namePrefix: String = "takeit"
    ): Result<String> = withContext(Dispatchers.IO) {
        if (!isConfigured()) {
            return@withContext Result.failure(
                IllegalStateException("IMGBB_API_KEY is not configured yet. Add IMGBB_API_KEY in GitHub Secrets or AI Studio Secrets.")
            )
        }

        try {
            val base64Image = encodeCompressedBase64(context, uri)
                ?: return@withContext Result.failure(IllegalArgumentException("Could not read selected image."))

            val formBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("key", BuildConfig.IMGBB_API_KEY)
                .addFormDataPart("name", "${namePrefix}_${System.currentTimeMillis()}")
                .addFormDataPart("image", base64Image)
                .build()

            val request = Request.Builder()
                .url(UPLOAD_ENDPOINT)
                .post(formBody)
                .build()

            client.newCall(request).execute().use { response ->
                val rawBody = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    val apiErr = runCatching {
                        JSONObject(rawBody).optJSONObject("error")?.optString("message")
                    }.getOrNull()
                    val msg = apiErr?.takeIf { it.isNotBlank() } ?: "ImgBB upload failed (HTTP ${response.code})"
                    Log.e(TAG, msg)
                    return@withContext Result.failure(IllegalStateException(msg))
                }

                val root = JSONObject(rawBody)
                if (!root.optBoolean("success", false)) {
                    return@withContext Result.failure(IllegalStateException("ImgBB returned unsuccessful status."))
                }

                val data = root.optJSONObject("data")
                val url = data?.optString("display_url").takeUnless { it.isNullOrBlank() }
                    ?: data?.optString("url").takeUnless { it.isNullOrBlank() }
                    ?: return@withContext Result.failure(IllegalStateException("ImgBB response missing image URL."))

                Result.success(url)
            }
        } catch (e: Exception) {
            Log.e(TAG, "ImgBB upload exception", e)
            Result.failure(e)
        }
    }

    private fun encodeCompressedBase64(context: Context, uri: Uri): String? {
        val rawBitmap = context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream)
        } ?: return null

        val maxDim = 1080
        val largest = max(rawBitmap.width, rawBitmap.height)
        val scaledBitmap = if (largest > maxDim) {
            val scale = maxDim.toFloat() / largest.toFloat()
            Bitmap.createScaledBitmap(
                rawBitmap,
                (rawBitmap.width * scale).roundToInt().coerceAtLeast(1),
                (rawBitmap.height * scale).roundToInt().coerceAtLeast(1),
                true
            )
        } else {
            rawBitmap
        }

        val baos = ByteArrayOutputStream()
        scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 82, baos)
        if (scaledBitmap !== rawBitmap) scaledBitmap.recycle()
        rawBitmap.recycle()

        return Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP)
    }
}
