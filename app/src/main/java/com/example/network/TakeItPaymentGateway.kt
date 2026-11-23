package com.example.network

import android.content.Context
import android.content.Intent
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class PaymentDeepLinkResult(
    val orderRef: String,
    val paymentId: String,
    val orderId: String,
    val amount: Int,
    val status: String
)

data class TakeItAdminTransaction(
    val paymentId: String,
    val orderId: String,
    val orderRef: String,
    val amount: Int,
    val status: String,
    val method: String,
    val serviceType: String,
    val itemTitle: String,
    val customerName: String,
    val customerPhone: String,
    val verifiedAt: Long
)

data class TakeItAdminTransactionsResponse(
    val count: Int,
    val totalRevenue: Int,
    val transactions: List<TakeItAdminTransaction>
)

object TakeItPaymentBus {
    private val _results = MutableSharedFlow<PaymentDeepLinkResult>(extraBufferCapacity = 8)
    val results: SharedFlow<PaymentDeepLinkResult> = _results.asSharedFlow()

    fun emitResult(result: PaymentDeepLinkResult) {
        _results.tryEmit(result)
    }
}

object TakeItPaymentGateway {
    const val BASE_URL = "https://jaiambey.shop"

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(25, TimeUnit.SECONDS)
            .build()
    }

    /**
     * Redirects user out of Take It app into Google Chrome for hosted Razorpay Checkout.
     * Upon successful verification on server, Chrome redirects back via takeit://payment-result.
     */
    fun launchChromePayment(
        context: Context,
        orderRef: String,
        amount: Int,
        serviceType: String,
        itemTitle: String,
        customerName: String,
        customerPhone: String,
        userId: String
    ): Boolean {
        val checkoutUri = Uri.parse("$BASE_URL/takeit-pay").buildUpon()
            .appendQueryParameter("orderRef", orderRef)
            .appendQueryParameter("amount", amount.coerceAtLeast(1).toString())
            .appendQueryParameter("serviceType", serviceType)
            .appendQueryParameter("itemTitle", itemTitle.take(80))
            .appendQueryParameter("customerName", customerName.take(60))
            .appendQueryParameter("customerPhone", customerPhone.take(20))
            .appendQueryParameter("userId", userId.take(64))
            .build()

        // Prefer Google Chrome explicitly; fall back gracefully to default browser if Chrome unavailable
        val chromeIntent = Intent(Intent.ACTION_VIEW, checkoutUri).apply {
            setPackage("com.android.chrome")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return try {
            context.startActivity(chromeIntent)
            true
        } catch (_: Exception) {
            try {
                val fallbackIntent = Intent(Intent.ACTION_VIEW, checkoutUri).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(fallbackIntent)
                true
            } catch (_: Exception) {
                false
            }
        }
    }

    /**
     * Checks verified payment status directly against the Take It backend endpoint.
     */
    suspend fun checkPaymentStatus(orderRef: String): PaymentDeepLinkResult? = withContext(Dispatchers.IO) {
        if (orderRef.isBlank()) return@withContext null
        try {
            val url = "$BASE_URL/takeit/payment-status/${Uri.encode(orderRef)}"
            val req = Request.Builder().url(url).get().build()
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return@withContext null
                val body = resp.body?.string().orEmpty()
                if (body.isBlank()) return@withContext null
                val json = JSONObject(body)
                if (!json.optBoolean("verified", false)) return@withContext null
                PaymentDeepLinkResult(
                    orderRef = json.optString("orderRef", orderRef),
                    paymentId = json.optString("paymentId", ""),
                    orderId = json.optString("orderId", ""),
                    amount = json.optInt("amount", 0),
                    status = "success"
                )
            }
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Fetches ONLY Take It Super App transactions (strictly isolated on server via notes.app === "TakeIt").
     */
    suspend fun fetchAdminTransactions(): Result<TakeItAdminTransactionsResponse> = withContext(Dispatchers.IO) {
        try {
            val req = Request.Builder()
                .url("$BASE_URL/takeit/admin/transactions?count=50")
                .get()
                .build()
            client.newCall(req).execute().use { resp ->
                val body = resp.body?.string().orEmpty()
                if (!resp.isSuccessful) {
                    return@withContext Result.failure(IllegalStateException("Server returned HTTP ${resp.code}"))
                }
                val root = JSONObject(body)
                val arr = root.optJSONArray("transactions")
                val list = buildList {
                    if (arr != null) {
                        for (i in 0 until arr.length()) {
                            val obj = arr.optJSONObject(i) ?: continue
                            add(
                                TakeItAdminTransaction(
                                    paymentId = obj.optString("paymentId"),
                                    orderId = obj.optString("orderId"),
                                    orderRef = obj.optString("orderRef"),
                                    amount = obj.optInt("amount", 0),
                                    status = obj.optString("status", "CAPTURED"),
                                    method = obj.optString("method", "RAZORPAY"),
                                    serviceType = obj.optString("serviceType", "ORDER"),
                                    itemTitle = obj.optString("itemTitle", "Take It Order"),
                                    customerName = obj.optString("customerName", "Take It Customer"),
                                    customerPhone = obj.optString("customerPhone", ""),
                                    verifiedAt = obj.optLong("verifiedAt", System.currentTimeMillis())
                                )
                            )
                        }
                    }
                }
                Result.success(
                    TakeItAdminTransactionsResponse(
                        count = root.optInt("count", list.size),
                        totalRevenue = root.optInt("totalRevenue", list.sumOf { it.amount }),
                        transactions = list
                    )
                )
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
