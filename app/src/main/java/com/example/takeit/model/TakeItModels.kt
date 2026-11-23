package com.example.takeit.model

import android.content.Context
import androidx.annotation.DrawableRes
import androidx.annotation.Keep
import com.example.BuildConfig
import com.example.R
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties

const val MAIN_ADMIN_EMAIL = "buildolive4@gmail.com"

enum class TakeItAppVariant {
    CUSTOMER,
    ADMIN;

    companion object {
        fun fromContext(context: Context): TakeItAppVariant {
            if (context.packageName.endsWith(".admin", ignoreCase = true)) {
                return ADMIN
            }
            val resMode = runCatching { context.getString(R.string.takeit_app_variant) }
                .getOrDefault(BuildConfig.APP_BUILD_MODE)
                .trim()
                .uppercase()
            return when (resMode) {
                "ADMIN", "ADMIN_PANEL", "CONTROL" -> ADMIN
                else -> fromBuildConfig()
            }
        }

        fun fromBuildConfig(): TakeItAppVariant {
            return when (BuildConfig.APP_BUILD_MODE.trim().uppercase()) {
                "ADMIN", "ADMIN_PANEL", "CONTROL" -> ADMIN
                else -> CUSTOMER
            }
        }
    }
}

enum class CustomerTab(val label: String) {
    HOME("Home"),
    ORDERS("Orders"),
    OFFERS("Offers"),
    ACCOUNT("Account")
}

sealed class CustomerScreen {
    data object MainTabs : CustomerScreen()
    data class RestaurantsList(val initialCategory: String = "All") : CustomerScreen()
    data class RestaurantDetail(val restaurantId: String) : CustomerScreen()
    data object Cart : CustomerScreen()
    data class TrackOrder(val orderId: String) : CustomerScreen()
    data class OrderDelivered(val orderId: String) : CustomerScreen()
    data object Addresses : CustomerScreen()
    data object Wallet : CustomerScreen()
    data object Support : CustomerScreen()
    data object About : CustomerScreen()
}

enum class AdminTab(val label: String) {
    OVERVIEW("Dashboard"),
    ORDERS("Orders"),
    CATALOG("Menu"),
    OFFERS("Coupons"),
    PEOPLE("Users"),
    CO_ADMINS("Co-Admins"),
    PAYMENTS("Payments"),
    SETTINGS("Controls")
}

data class FoodCategoryItem(
    val name: String,
    val emoji: String,
    @DrawableRes val fallbackDrawable: Int
)

val TakeItFoodCategories = listOf(
    FoodCategoryItem("Pizza", "🍕", R.drawable.img_takeit_pizza_offer_1795427326877),
    FoodCategoryItem("Burger", "🍔", R.drawable.img_takeit_hero_burger_1795427328311),
    FoodCategoryItem("Biryani", "🍛", R.drawable.img_food_feast_1795421416008),
    FoodCategoryItem("Chinese", "🍜", R.drawable.img_food_feast_1795421416008),
    FoodCategoryItem("South Indian", "🥞", R.drawable.img_takeit_main_hero_1795429839427),
    FoodCategoryItem("North Indian", "🍲", R.drawable.img_food_feast_1795421416008),
    FoodCategoryItem("Desserts", "🍰", R.drawable.img_takeit_hero_burger_1795427328311),
    FoodCategoryItem("Snacks", "🍟", R.drawable.img_takeit_pizza_offer_1795427326877),
    FoodCategoryItem("Healthy", "🥗", R.drawable.img_grocery_basket_1795421415559),
    FoodCategoryItem("All", "🍽️", R.drawable.logo_takeit_food)
)

@DrawableRes
fun resolveFoodDrawable(categoryOrName: String): Int {
    val q = categoryOrName.lowercase()
    return when {
        q.contains("pizza") || q.contains("margherita") || q.contains("garlic") -> R.drawable.img_takeit_pizza_offer_1795427326877
        q.contains("burger") || q.contains("fries") || q.contains("snack") || q.contains("dessert") || q.contains("cake") -> R.drawable.img_takeit_hero_burger_1795427328311
        q.contains("healthy") || q.contains("salad") || q.contains("bowl") -> R.drawable.img_grocery_basket_1795421415559
        else -> R.drawable.img_food_feast_1795421416008
    }
}

@Keep
@IgnoreExtraProperties
data class TakeItAppConfig(
    @DocumentId val id: String = "live",
    val appName: String = "Take It",
    val cityName: String = "Greater Noida",
    val heroBannerTitle: String = "Flat 50% OFF",
    val heroBannerSubtitle: String = "on your Favourite Food",
    val heroBannerCode: String = "FOOD50",
    val lateNightTitle: String = "Late Night Cravings?",
    val lateNightSubtitle: String = "We're Here! Hot meals till 3 AM",
    val deliveryFee: Int = 20,
    val platformFee: Int = 5,
    val gstPercent: Int = 5,
    val freeDeliveryAbove: Int = 299,
    val maintenanceMode: Boolean = false,
    val acceptingOrders: Boolean = true,
    val supportPhone: String = "+91 98765 43210",
    val supportEmail: String = MAIN_ADMIN_EMAIL,
    val announcementText: String = "",
    val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class CoAdminAccount(
    @DocumentId val id: String = "",
    val email: String = "",
    val name: String = "",
    val roleTitle: String = "Operations Manager",
    val passcode: String = "",
    val activeFlag: Boolean = true,
    val canManageOrders: Boolean = true,
    val canManageRestaurants: Boolean = true,
    val canManageCoupons: Boolean = false,
    val canManageUsers: Boolean = false,
    val canManageSettings: Boolean = false,
    val createdBy: String = MAIN_ADMIN_EMAIL,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class Restaurant(
    @DocumentId val id: String = "",
    val name: String = "",
    val cuisine: String = "",
    val category: String = "Pizza",
    val rating: Double = 4.5,
    val ratingCount: String = "1K+",
    val deliveryTime: String = "30 min",
    val priceForTwo: Int = 150,
    val offerTag: String = "50% OFF",
    val offerCode: String = "FOOD50",
    val address: String = "Sector 62, Greater Noida",
    val imageUrl: String = "",
    val pureVegFlag: Boolean = false,
    val freeDeliveryFlag: Boolean = false,
    val verifiedFlag: Boolean = true,
    val openFlag: Boolean = true,
    val featuredFlag: Boolean = true,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class MenuItem(
    @DocumentId val id: String = "",
    val restaurantId: String = "",
    val restaurantName: String = "",
    val name: String = "",
    val description: String = "",
    val category: String = "Recommended",
    val price: Int = 149,
    val originalPrice: Int = 199,
    val vegFlag: Boolean = true,
    val availableFlag: Boolean = true,
    val bestsellerFlag: Boolean = false,
    val rating: Double = 4.5,
    val imageUrl: String = "",
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class PromoCoupon(
    @DocumentId val id: String = "",
    val code: String = "",
    val title: String = "",
    val description: String = "",
    val discountType: String = "PERCENT", // PERCENT or FLAT
    val discountValue: Int = 50,
    val maxDiscount: Int = 100,
    val minOrderAmount: Int = 149,
    val activeFlag: Boolean = true,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class FoodOrder(
    @DocumentId val id: String = "",
    val orderNumber: String = "",
    val userId: String = "",
    val customerName: String = "",
    val customerEmail: String = "",
    val customerPhone: String = "",
    val restaurantId: String = "",
    val restaurantName: String = "",
    val itemsSummary: String = "",
    val itemTotal: Int = 0,
    val deliveryFee: Int = 20,
    val platformFee: Int = 5,
    val gstAmount: Int = 0,
    val discountAmount: Int = 0,
    val couponCode: String = "",
    val totalAmount: Int = 0,
    val paymentMethod: String = "RAZORPAY",
    val paymentStatus: String = "PAID",
    val paymentRef: String = "",
    val deliveryAddress: String = "",
    val status: String = "PLACED", // PLACED, CONFIRMED, PREPARING, OUT_FOR_DELIVERY, DELIVERED, CANCELLED, REFUNDED
    val riderName: String = "Assigning Rider...",
    val riderPhone: String = "",
    val riderVehicle: String = "",
    val etaMinutes: Int = 25,
    val otp: String = "4829",
    val rating: Int = 0,
    val reviewText: String = "",
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class TakeItUserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "Take It Foodie",
    val email: String = "",
    val phone: String = "",
    val avatarUrl: String = "",
    val activeAddressLabel: String = "Home",
    val activeAddressLine: String = "Greater Noida, Sector 62",
    val walletBalance: Double = 0.0,
    val goldMemberFlag: Boolean = true,
    val suspendedFlag: Boolean = false,
    val favoriteRestaurantIds: List<String> = emptyList(),
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class SavedAddress(
    @DocumentId val id: String = "",
    val userId: String = "",
    val label: String = "Home",
    val fullAddress: String = "",
    val landmark: String = "",
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class WalletTx(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val txType: String = "CREDIT",
    val amount: Double = 0.0,
    val method: String = "RAZORPAY",
    val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class RestaurantReview(
    @DocumentId val id: String = "",
    val userId: String = "",
    val userName: String = "",
    val restaurantId: String = "",
    val orderId: String = "",
    val rating: Int = 5,
    val comment: String = "",
    val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class SupportTicket(
    @DocumentId val id: String = "",
    val userId: String = "",
    val userName: String = "",
    val userEmail: String = "",
    val subject: String = "",
    val message: String = "",
    val status: String = "OPEN", // OPEN, IN_PROGRESS, RESOLVED
    val adminReply: String = "",
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)

data class CartItem(
    val menuItem: MenuItem,
    val quantity: Int
)

enum class AdminRole {
    NONE,
    MAIN_ADMIN,
    CO_ADMIN
}
