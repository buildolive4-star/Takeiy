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

const val STAFF_ROLE_CO_ADMIN = "CO_ADMIN"
const val STAFF_ROLE_RESTAURANT_OWNER = "RESTAURANT_OWNER"
const val STAFF_ROLE_DELIVERY_PARTNER = "DELIVERY_PARTNER"

enum class TakeItAppVariant {
    CUSTOMER,
    RIDE,
    TRANSPORT,
    DELIVERY,
    ADMIN;

    companion object {
        fun fromContext(context: Context): TakeItAppVariant {
            val pkg = context.packageName.lowercase()
            when {
                pkg.endsWith(".admin") -> return ADMIN
                pkg.endsWith(".ride") -> return RIDE
                pkg.endsWith(".transport") -> return TRANSPORT
                pkg.endsWith(".delivery") -> return DELIVERY
            }
            val resMode = runCatching { context.getString(R.string.takeit_app_variant) }
                .getOrDefault(BuildConfig.APP_BUILD_MODE)
                .trim()
                .uppercase()
            return when (resMode) {
                "ADMIN", "ADMIN_PANEL", "CONTROL" -> ADMIN
                "RIDE", "RAPIDO" -> RIDE
                "TRANSPORT", "PARCEL" -> TRANSPORT
                "DELIVERY", "DELIVERY_PARTNER", "RIDER", "COURIER" -> DELIVERY
                else -> fromBuildConfig()
            }
        }

        fun fromBuildConfig(): TakeItAppVariant {
            return when (BuildConfig.APP_BUILD_MODE.trim().uppercase()) {
                "ADMIN", "ADMIN_PANEL", "CONTROL" -> ADMIN
                "RIDE", "RAPIDO" -> RIDE
                "TRANSPORT", "PARCEL" -> TRANSPORT
                "DELIVERY", "DELIVERY_PARTNER", "RIDER", "COURIER" -> DELIVERY
                else -> CUSTOMER
            }
        }
    }
}

enum class CustomerTab(val label: String) {
    HOME("Home"),
    HISTORY("History"),
    EXPLORE("Explore"),
    OFFERS("Offers"),
    ACCOUNT("Account")
}

enum class RideTab(val label: String) {
    BOOK("Book Ride"),
    TRIPS("My Rides"),
    WALLET("Wallet"),
    ACCOUNT("Account")
}

enum class TransportTab(val label: String) {
    SEND("Send Parcel"),
    SHIPMENTS("Shipments"),
    WALLET("Wallet"),
    ACCOUNT("Account")
}

enum class DeliveryTab(val label: String) {
    TASKS("Tasks"),
    ACTIVE("Route Map"),
    EARNINGS("History"),
    ACCOUNT("Account")
}

sealed class CustomerScreen {
    data object MainTabs : CustomerScreen()
    data class RestaurantsList(val initialCategory: String = "All") : CustomerScreen()
    data class RestaurantDetail(val restaurantId: String) : CustomerScreen()
    data object Cart : CustomerScreen()
    data class TrackOrder(val orderId: String) : CustomerScreen()
    data class OrderDelivered(val orderId: String) : CustomerScreen()
    data object MyOrders : CustomerScreen()
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
    CO_ADMINS("Staff & Roles"),
    PAYMENTS("Payments"),
    SETTINGS("Controls")
}

data class FoodCategoryItem(
    val name: String,
    val emoji: String,
    @DrawableRes val fallbackDrawable: Int,
    val imageUrl: String = ""
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

val TakeItDishSections = listOf(
    "Recommended",
    "Starters",
    "Main Course",
    "Combos",
    "Sides",
    "Biryani & Rice",
    "Desserts",
    "Beverages"
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
data class FoodCategory(
    @DocumentId val id: String = "",
    val name: String = "",
    val emoji: String = "🍽️",
    val imageUrl: String = "",
    val sortOrder: Int = 0,
    val activeFlag: Boolean = true,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class HeroBannerSlide(
    @DocumentId val id: String = "",
    val title: String = "",
    val subtitle: String = "",
    val imageUrl: String = "",
    val restaurantId: String = "",
    val promoCode: String = "",
    val sortOrder: Int = 0,
    val activeFlag: Boolean = true,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)

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
    val roleType: String = STAFF_ROLE_CO_ADMIN, // CO_ADMIN, RESTAURANT_OWNER, DELIVERY_PARTNER
    val assignedRestaurantId: String = "",
    val assignedRestaurantName: String = "",
    val phone: String = "",
    val vehicleNumber: String = "",
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
    val offerImageUrl: String = "",
    val latitude: Double = 28.4744,
    val longitude: Double = 77.5040,
    val ownerEmail: String = "",
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
    val deliveryLat: Double = 28.4812,
    val deliveryLng: Double = 77.5125,
    val pickupLat: Double = 28.4744,
    val pickupLng: Double = 77.5040,
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
    val city: String = "",
    val areaStreet: String = "",
    val mapLink: String = "",
    val receiverName: String = "",
    val receiverPhone: String = "",
    val forSomeoneElse: Boolean = false,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
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

@Keep
@IgnoreExtraProperties
data class RideBooking(
    @DocumentId val id: String = "",
    val rideNumber: String = "",
    val userId: String = "",
    val customerName: String = "",
    val customerEmail: String = "",
    val customerPhone: String = "",
    val vehicleType: String = "BIKE", // BIKE, AUTO, MINI_CAB, PRIME_SEDAN
    val pickupAddress: String = "",
    val dropAddress: String = "",
    val distanceKm: Int = 5,
    val totalFare: Int = 49,
    val paymentMethod: String = "RAZORPAY",
    val paymentStatus: String = "PAID",
    val paymentRef: String = "",
    val status: String = "REQUESTED", // REQUESTED, CAPTAIN_ASSIGNED, ARRIVING, ON_TRIP, COMPLETED, CANCELLED, REFUNDED
    val captainName: String = "Finding Captain...",
    val captainPhone: String = "",
    val vehicleNumber: String = "",
    val etaMinutes: Int = 4,
    val otp: String = "4821",
    val rating: Int = 0,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class ParcelOrder(
    @DocumentId val id: String = "",
    val parcelNumber: String = "",
    val userId: String = "",
    val senderName: String = "",
    val senderEmail: String = "",
    val senderPhone: String = "",
    val receiverName: String = "",
    val receiverPhone: String = "",
    val pickupAddress: String = "",
    val dropAddress: String = "",
    val vehicleType: String = "TWO_WHEELER", // TWO_WHEELER, THREE_WHEELER, MINI_TRUCK, PICKUP_8FT
    val packageType: String = "Box / Documents",
    val weightKg: Int = 3,
    val fragileFlag: Boolean = false,
    val totalFare: Int = 79,
    val paymentMethod: String = "RAZORPAY",
    val paymentStatus: String = "PAID",
    val paymentRef: String = "",
    val status: String = "BOOKED", // BOOKED, PARTNER_ASSIGNED, PICKED_UP, IN_TRANSIT, DELIVERED, CANCELLED, REFUNDED
    val partnerName: String = "Assigning Partner...",
    val partnerPhone: String = "",
    val vehicleNumber: String = "",
    val etaMinutes: Int = 25,
    val otp: String = "9274",
    val rating: Int = 0,
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
    CO_ADMIN,
    RESTAURANT_OWNER,
    DELIVERY_PARTNER
}
