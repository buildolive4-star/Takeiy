package com.example.util

import android.Manifest
import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.net.Uri
import android.os.Build
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.Locale

data class GpsLocationResult(
    val label: String,
    val fullAddress: String,
    val latitude: Double,
    val longitude: Double
)

object DeviceActions {

    fun hasLocationPermission(context: Context): Boolean {
        val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    @SuppressLint("MissingPermission")
    suspend fun fetchCurrentGpsLocation(context: Context): Result<GpsLocationResult> {
        if (!hasLocationPermission(context)) {
            return Result.failure(SecurityException("Location permission not granted"))
        }
        return try {
            val client = LocationServices.getFusedLocationProviderClient(context)
            val cts = CancellationTokenSource()
            val location = client.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cts.token).await()
                ?: client.lastLocation.await()
                ?: return Result.failure(IllegalStateException("GPS signal unavailable. Please enable location services."))

            val lat = location.latitude
            val lng = location.longitude
            val resolved = reverseGeocode(context, lat, lng)
            Result.success(
                GpsLocationResult(
                    label = resolved.first,
                    fullAddress = resolved.second,
                    latitude = lat,
                    longitude = lng
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    @Suppress("DEPRECATION")
    private suspend fun reverseGeocode(context: Context, lat: Double, lng: Double): Pair<String, String> {
        return withContext(Dispatchers.IO) {
            try {
                val geocoder = Geocoder(context, Locale.getDefault())
                val addresses = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    var resultList: List<android.location.Address>? = null
                    val latch = java.util.concurrent.CountDownLatch(1)
                    geocoder.getFromLocation(lat, lng, 1) { list ->
                        resultList = list
                        latch.countDown()
                    }
                    latch.await(3, java.util.concurrent.TimeUnit.SECONDS)
                    resultList
                } else {
                    geocoder.getFromLocation(lat, lng, 1)
                }
                val first = addresses?.firstOrNull()
                if (first != null) {
                    val shortLabel = listOfNotNull(first.subLocality, first.locality)
                        .distinct()
                        .joinToString(", ")
                        .ifBlank { first.featureName ?: "Current GPS" }
                    val full = first.getAddressLine(0) ?: "$shortLabel (${"%.4f".format(lat)}, ${"%.4f".format(lng)})"
                    shortLabel to full
                } else {
                    "Current GPS" to "GPS (${"%.4f".format(lat)}, ${"%.4f".format(lng)})"
                }
            } catch (_: Exception) {
                "Current GPS" to "GPS (${"%.4f".format(lat)}, ${"%.4f".format(lng)})"
            }
        }
    }

    fun dialPhoneNumber(context: Context, phone: String) {
        runCatching {
            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${phone.trim()}")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
    }

    fun openSosDialer(context: Context) {
        dialPhoneNumber(context, "112")
    }

    fun shareText(context: Context, subject: String, text: String) {
        runCatching {
            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, subject)
                putExtra(Intent.EXTRA_TEXT, text)
            }
            val chooser = Intent.createChooser(sendIntent, subject).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        }
    }

    fun copyToClipboard(context: Context, label: String, text: String) {
        runCatching {
            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            cm?.setPrimaryClip(ClipData.newPlainText(label, text))
        }
    }
}
