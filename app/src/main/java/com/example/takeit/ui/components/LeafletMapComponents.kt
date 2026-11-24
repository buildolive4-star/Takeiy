package com.example.takeit.ui.components

import android.annotation.SuppressLint
import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

private class TouchableMapWebView(context: Context) : WebView(context) {
    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        when (event?.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                parent?.requestDisallowInterceptTouchEvent(true)
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                parent?.requestDisallowInterceptTouchEvent(false)
            }
        }
        return super.onTouchEvent(event)
    }
}

private fun escapeJsString(raw: String): String =
    raw.replace("\\", "\\\\")
        .replace("'", "\\'")
        .replace("\"", "\\\"")
        .replace("\n", " ")

/**
 * Interactive OpenStreetMap + Leaflet 1.9.4 live route tracking map (100% free, zero API keys).
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun LeafletTrackingMap(
    pickupLat: Double,
    pickupLng: Double,
    pickupTitle: String,
    dropLat: Double,
    dropLng: Double,
    dropTitle: String,
    riderLabel: String,
    progressFraction: Float,
    modifier: Modifier = Modifier,
    accentHex: String = "#E53935"
) {
    var pageLoaded by remember { mutableStateOf(false) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }

    val safePickupLat = if (pickupLat == 0.0) 28.4744 else pickupLat
    val safePickupLng = if (pickupLng == 0.0) 77.5040 else pickupLng
    val safeDropLat = if (dropLat == 0.0) 28.4842 else dropLat
    val safeDropLng = if (dropLng == 0.0) 77.5150 else dropLng
    val clampedProgress = progressFraction.coerceIn(0.05f, 1f)

    val htmlContent = remember {
        """
        <!DOCTYPE html>
        <html>
        <head>
          <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no" />
          <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css" />
          <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
          <style>
            html, body, #map { margin: 0; padding: 0; width: 100%; height: 100%; background: #e8f1ff; }
            .pin-badge {
              display: flex; align-items: center; justify-content: center;
              width: 30px; height: 30px; border-radius: 50%;
              border: 3px solid #ffffff; color: #ffffff;
              font-family: sans-serif; font-weight: 800; font-size: 13px;
              box-shadow: 0 4px 10px rgba(0,0,0,0.35);
            }
            .rider-badge {
              display: flex; align-items: center; justify-content: center;
              width: 34px; height: 34px; border-radius: 50%;
              background: #FF6D00; border: 3px solid #ffffff;
              box-shadow: 0 0 0 6px rgba(255,109,0,0.28), 0 4px 12px rgba(0,0,0,0.4);
              font-size: 16px;
            }
          </style>
        </head>
        <body>
          <div id="map"></div>
          <script>
            var map = L.map('map', { zoomControl: true, attributionControl: false }).setView([$safePickupLat, $safePickupLng], 14);
            L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png', { maxZoom: 19 }).addTo(map);
            var pickupMarker = null;
            var dropMarker = null;
            var riderMarker = null;
            var fullLine = null;
            var doneLine = null;
            var hasFitted = false;

            window.updateRoute = function(pLat, pLng, pTitle, dLat, dLng, dTitle, rTitle, prog, accent) {
              if (typeof L === 'undefined') return;
              var midLat = (pLat + dLat) / 2.0 + 0.0022;
              var midLng = (pLng + dLng) / 2.0 - 0.0018;
              var rLat, rLng;
              if (prog < 0.5) {
                var t = prog * 2.0;
                rLat = pLat + (midLat - pLat) * t;
                rLng = pLng + (midLng - pLng) * t;
              } else {
                var t2 = (prog - 0.5) * 2.0;
                rLat = midLat + (dLat - midLat) * t2;
                rLng = midLng + (dLng - midLng) * t2;
              }

              var pickupIcon = L.divIcon({ className: '', html: '<div class="pin-badge" style="background:' + accent + ';">P</div>', iconSize: [30, 30], iconAnchor: [15, 15] });
              var dropIcon = L.divIcon({ className: '', html: '<div class="pin-badge" style="background:#16A34A;">D</div>', iconSize: [30, 30], iconAnchor: [15, 15] });
              var riderIcon = L.divIcon({ className: '', html: '<div class="rider-badge">🛵</div>', iconSize: [34, 34], iconAnchor: [17, 17] });

              if (!pickupMarker) {
                pickupMarker = L.marker([pLat, pLng], { icon: pickupIcon }).addTo(map);
              } else {
                pickupMarker.setLatLng([pLat, pLng]);
              }
              pickupMarker.bindPopup(pTitle);

              if (!dropMarker) {
                dropMarker = L.marker([dLat, dLng], { icon: dropIcon }).addTo(map);
              } else {
                dropMarker.setLatLng([dLat, dLng]);
              }
              dropMarker.bindPopup(dTitle);

              if (!riderMarker) {
                riderMarker = L.marker([rLat, rLng], { icon: riderIcon }).addTo(map);
              } else {
                riderMarker.setLatLng([rLat, rLng]);
              }
              riderMarker.bindPopup(rTitle);

              if (fullLine) map.removeLayer(fullLine);
              fullLine = L.polyline([[pLat, pLng], [midLat, midLng], [dLat, dLng]], {
                color: '#64748B', weight: 4, opacity: 0.55, dashArray: '8, 8'
              }).addTo(map);

              var donePoints = prog < 0.5
                ? [[pLat, pLng], [rLat, rLng]]
                : [[pLat, pLng], [midLat, midLng], [rLat, rLng]];
              if (doneLine) map.removeLayer(doneLine);
              doneLine = L.polyline(donePoints, { color: accent, weight: 5, opacity: 0.95 }).addTo(map);

              if (!hasFitted) {
                hasFitted = true;
                var bounds = L.latLngBounds([[pLat, pLng], [midLat, midLng], [dLat, dLng]]);
                map.fitBounds(bounds.pad(0.28));
              }
            };
          </script>
        </body>
        </html>
        """.trimIndent()
    }

    LaunchedEffect(pageLoaded, safePickupLat, safePickupLng, safeDropLat, safeDropLng, clampedProgress, pickupTitle, dropTitle, riderLabel) {
        if (pageLoaded) {
            val js = "window.updateRoute && window.updateRoute($safePickupLat, $safePickupLng, '${escapeJsString(pickupTitle)}', $safeDropLat, $safeDropLng, '${escapeJsString(dropTitle)}', '${escapeJsString(riderLabel)}', $clampedProgress, '$accentHex');"
            webViewRef?.evaluateJavascript(js, null)
        }
    }

    Box(modifier = modifier.background(Color(0xFFE8F1FF))) {
        AndroidView(
            factory = { ctx ->
                TouchableMapWebView(ctx).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.loadWithOverviewMode = true
                    settings.useWideViewPort = true
                    webChromeClient = WebChromeClient()
                    webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView?, url: String?) {
                            pageLoaded = true
                            val js = "window.updateRoute && window.updateRoute($safePickupLat, $safePickupLng, '${escapeJsString(pickupTitle)}', $safeDropLat, $safeDropLng, '${escapeJsString(dropTitle)}', '${escapeJsString(riderLabel)}', $clampedProgress, '$accentHex');"
                            view?.evaluateJavascript(js, null)
                        }
                    }
                    loadDataWithBaseURL("https://unpkg.com/", htmlContent, "text/html", "UTF-8", null)
                    webViewRef = this
                }
            },
            modifier = Modifier.fillMaxSize()
        )
        if (!pageLoaded) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        }
    }
}

private class AddressPickerJsBridge(
    private val mainHandler: Handler,
    private val onMoved: (Double, Double) -> Unit
) {
    @JavascriptInterface
    fun onPinMoved(lat: Double, lng: Double) {
        mainHandler.post { onMoved(lat, lng) }
    }
}

/**
 * Interactive OpenStreetMap + Leaflet tap-to-pin & draggable marker address selector.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun LeafletAddressPickerMap(
    selectedLat: Double,
    selectedLng: Double,
    onLocationPicked: (Double, Double) -> Unit,
    modifier: Modifier = Modifier
) {
    val safeLat = if (selectedLat == 0.0) 28.4744 else selectedLat
    val safeLng = if (selectedLng == 0.0) 77.5040 else selectedLng
    var pageLoaded by remember { mutableStateOf(false) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    val currentCallback by rememberUpdatedState(onLocationPicked)
    val mainHandler = remember { Handler(Looper.getMainLooper()) }

    val htmlContent = remember {
        """
        <!DOCTYPE html>
        <html>
        <head>
          <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no" />
          <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css" />
          <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
          <style>
            html, body, #map { margin: 0; padding: 0; width: 100%; height: 100%; background: #eef2ff; }
            .drop-pin {
              width: 28px; height: 28px; border-radius: 50%;
              background: #E53935; border: 3px solid #ffffff;
              box-shadow: 0 4px 10px rgba(0,0,0,0.4);
            }
          </style>
        </head>
        <body>
          <div id="map"></div>
          <script>
            var map = L.map('map', { zoomControl: true, attributionControl: false }).setView([$safeLat, $safeLng], 15);
            L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png', { maxZoom: 19 }).addTo(map);
            var icon = L.divIcon({ className: '', html: '<div class="drop-pin"></div>', iconSize: [28, 28], iconAnchor: [14, 14] });
            var marker = L.marker([$safeLat, $safeLng], { draggable: true, icon: icon }).addTo(map);

            marker.on('dragend', function(e) {
              var pos = marker.getLatLng();
              if (window.AndroidMap) window.AndroidMap.onPinMoved(pos.lat, pos.lng);
            });
            map.on('click', function(e) {
              marker.setLatLng(e.latlng);
              if (window.AndroidMap) window.AndroidMap.onPinMoved(e.latlng.lat, e.latlng.lng);
            });

            window.movePin = function(lat, lng) {
              if (marker && map) {
                marker.setLatLng([lat, lng]);
                map.setView([lat, lng], Math.max(map.getZoom(), 15));
              }
            };
          </script>
        </body>
        </html>
        """.trimIndent()
    }

    LaunchedEffect(pageLoaded, safeLat, safeLng) {
        if (pageLoaded) {
            webViewRef?.evaluateJavascript("window.movePin && window.movePin($safeLat, $safeLng);", null)
        }
    }

    Box(modifier = modifier.background(Color(0xFFEEF2FF))) {
        AndroidView(
            factory = { ctx ->
                TouchableMapWebView(ctx).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    webChromeClient = WebChromeClient()
                    addJavascriptInterface(
                        AddressPickerJsBridge(mainHandler) { lat, lng -> currentCallback(lat, lng) },
                        "AndroidMap"
                    )
                    webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView?, url: String?) {
                            pageLoaded = true
                            view?.evaluateJavascript("window.movePin && window.movePin($safeLat, $safeLng);", null)
                        }
                    }
                    loadDataWithBaseURL("https://unpkg.com/", htmlContent, "text/html", "UTF-8", null)
                    webViewRef = this
                }
            },
            modifier = Modifier.fillMaxSize()
        )
        if (!pageLoaded) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        }
    }
}

/**
 * Resolves device GPS coordinates using FusedLocationProviderClient and reverse-geocodes via Android Geocoder.
 */
@SuppressLint("MissingPermission")
fun autofetchCurrentAddress(
    context: Context,
    scope: CoroutineScope,
    onSuccess: (lat: Double, lng: Double, city: String, areaStreet: String, fullAddr: String, mapLink: String) -> Unit,
    onError: (String) -> Unit
) {
    val client = LocationServices.getFusedLocationProviderClient(context)
    val cts = CancellationTokenSource()
    client.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cts.token)
        .addOnSuccessListener { loc ->
            if (loc != null) {
                reverseGeocodeLocation(context, scope, loc.latitude, loc.longitude, onSuccess, onError)
            } else {
                client.lastLocation
                    .addOnSuccessListener { last ->
                        if (last != null) {
                            reverseGeocodeLocation(context, scope, last.latitude, last.longitude, onSuccess, onError)
                        } else {
                            onError("Turn on device GPS/Location and tap on map or retry.")
                        }
                    }
                    .addOnFailureListener { e ->
                        onError(e.localizedMessage ?: "Unable to read GPS location")
                    }
            }
        }
        .addOnFailureListener {
            client.lastLocation
                .addOnSuccessListener { last ->
                    if (last != null) {
                        reverseGeocodeLocation(context, scope, last.latitude, last.longitude, onSuccess, onError)
                    } else {
                        onError("GPS unavailable. You can tap anywhere on the Leaflet map to drop your pin.")
                    }
                }
                .addOnFailureListener { e ->
                    onError(e.localizedMessage ?: "Location lookup failed")
                }
        }
}

fun reverseGeocodeCoordinates(
    context: Context,
    scope: CoroutineScope,
    lat: Double,
    lng: Double,
    onSuccess: (lat: Double, lng: Double, city: String, areaStreet: String, fullAddr: String, mapLink: String) -> Unit
) {
    reverseGeocodeLocation(context, scope, lat, lng, onSuccess) {
        val link = "https://www.openstreetmap.org/?mlat=${"%.5f".format(Locale.US, lat)}&mlon=${"%.5f".format(Locale.US, lng)}#map=17/${"%.5f".format(Locale.US, lat)}/${"%.5f".format(Locale.US, lng)}"
        onSuccess(lat, lng, "", "", "Pinned (${"%.4f".format(Locale.US, lat)}, ${"%.4f".format(Locale.US, lng)})", link)
    }
}

private fun reverseGeocodeLocation(
    context: Context,
    scope: CoroutineScope,
    lat: Double,
    lng: Double,
    onSuccess: (lat: Double, lng: Double, city: String, areaStreet: String, fullAddr: String, mapLink: String) -> Unit,
    onError: (String) -> Unit
) {
    val mapLink = "https://www.openstreetmap.org/?mlat=${"%.5f".format(Locale.US, lat)}&mlon=${"%.5f".format(Locale.US, lng)}#map=17/${"%.5f".format(Locale.US, lat)}/${"%.5f".format(Locale.US, lng)}"
    fun emitAddress(addr: Address?) {
        if (addr == null) {
            onSuccess(
                lat, lng,
                "Greater Noida",
                "Pinned Location",
                "Location (${"%.4f".format(Locale.US, lat)}, ${"%.4f".format(Locale.US, lng)})",
                mapLink
            )
            return
        }
        val city = addr.locality ?: addr.subAdminArea ?: addr.adminArea ?: "Greater Noida"
        val streetParts = listOfNotNull(addr.subLocality, addr.thoroughfare, addr.featureName)
            .distinct()
            .filter { it.isNotBlank() }
        val areaStreet = streetParts.take(2).joinToString(", ").ifBlank { city }
        val fullLine = (0..addr.maxAddressLineIndex)
            .mapNotNull { runCatching { addr.getAddressLine(it) }.getOrNull() }
            .joinToString(", ")
            .ifBlank { "$areaStreet, $city" }
        onSuccess(lat, lng, city, areaStreet, fullLine, mapLink)
    }

    if (!Geocoder.isPresent()) {
        emitAddress(null)
        return
    }
    val geocoder = Geocoder(context, Locale.getDefault())
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        try {
            geocoder.getFromLocation(lat, lng, 1) { list ->
                Handler(Looper.getMainLooper()).post {
                    emitAddress(list.firstOrNull())
                }
            }
        } catch (e: Exception) {
            onError(e.localizedMessage ?: "Geocoder error")
        }
    } else {
        scope.launch(Dispatchers.IO) {
            @Suppress("DEPRECATION")
            val list = runCatching { geocoder.getFromLocation(lat, lng, 1) }.getOrNull()
            withContext(Dispatchers.Main) {
                emitAddress(list?.firstOrNull())
            }
        }
    }
}
