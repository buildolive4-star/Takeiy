package com.example.model

import androidx.compose.ui.graphics.Color
import com.example.ui.theme.*

enum class AppPanel(
    val title: String,
    val subtitle: String,
    val accentColor: Color,
    val isDarkTheme: Boolean = false,
    val isAdminOnly: Boolean = false
) {
    CUSTOMER(
        title = "Customer Panel",
        subtitle = "Rides, food, groceries & parcels",
        accentColor = SkyBlue,
        isDarkTheme = false
    ),
    DRIVER_CAPTAIN(
        title = "Driver / Captain Panel",
        subtitle = "Bike taxi, auto & cab earnings",
        accentColor = PremiumYellow,
        isDarkTheme = true
    ),
    FOOD_DELIVERY_PARTNER(
        title = "Food Delivery Partner",
        subtitle = "Restaurant pickups & instant payouts",
        accentColor = OrangeAccent,
        isDarkTheme = true
    ),
    RESTAURANT_OWNER(
        title = "Restaurant Owner Panel",
        subtitle = "Menu, live kitchen & settlements",
        accentColor = CoralDanger,
        isDarkTheme = false
    ),
    GROCERY_OWNER(
        title = "Grocery Store Owner",
        subtitle = "Inventory, packing & instant dispatch",
        accentColor = SuccessGreen,
        isDarkTheme = false
    ),
    PARCEL_PARTNER(
        title = "Parcel Delivery Partner",
        subtitle = "Express document & package runs",
        accentColor = TealAccent,
        isDarkTheme = true
    ),
    TRANSPORT_PARTNER(
        title = "Transport Partner Panel",
        subtitle = "Mini trucks, Tata Ace & cargo loads",
        accentColor = VioletAccent,
        isDarkTheme = true
    ),
    BUSINESS_OWNER(
        title = "Business Owner Panel",
        subtitle = "B2B fleet, retail & enterprise hub",
        accentColor = PinkAccent,
        isDarkTheme = false
    ),
    ADMIN(
        title = "Admin Panel",
        subtitle = "Authorized platform control center",
        accentColor = SkyBlue,
        isDarkTheme = true,
        isAdminOnly = true
    )
}

sealed interface AppRoute {
    data object PanelHome : AppRoute
    data object RideBooking : AppRoute
    data object FoodDiscovery : AppRoute
    data class RestaurantDetail(val restaurantId: String) : AppRoute
    data object GroceryStore : AppRoute
    data object ParcelDelivery : AppRoute
    data object GoodsTransport : AppRoute
    data object LocalShopping : AppRoute
    data object CartCheckout : AppRoute
    data object Wallet : AppRoute
    data object Notifications : AppRoute
    data object Profile : AppRoute
    data class OrderHistory(val filterType: String = "ALL") : AppRoute
    data object CouponsOffers : AppRoute
    data object ReferEarn : AppRoute
    data object Support : AppRoute
    data class InfoPage(val title: String, val body: String) : AppRoute
}

data class VehicleOption(
    val id: String,
    val name: String,
    val tagline: String,
    val etaMin: Int,
    val capacity: String,
    val baseFare: Int,
    val perKmRate: Int,
    val badge: String = ""
)

data class TransportVehicleOption(
    val id: String,
    val name: String,
    val maxWeightKg: Int,
    val dimensions: String,
    val baseFare: Int,
    val perKmRate: Int,
    val etaMin: Int,
    val idealFor: String
)

data class RestaurantInfo(
    val id: String,
    val name: String,
    val cuisines: String,
    val rating: Double,
    val reviewCount: Int,
    val deliveryMins: Int,
    val distanceKm: Double,
    val priceForTwo: Int,
    val offerText: String,
    val isPureVeg: Boolean,
    val categoryTag: String,
    val accentHex: Long,
    val bannerStyle: Int = 0
)

data class CartEntry(
    val itemId: String,
    val name: String,
    val sourceName: String,
    val categoryType: String, // FOOD, GROCERY, SHOPPING
    val unitPrice: Int,
    val mrp: Int,
    val quantity: Int,
    val customization: String = ""
)

data class LocalShopProduct(
    val id: String,
    val name: String,
    val shopName: String,
    val category: String, // Electronics, Fashion, Home Essentials
    val mrp: Int,
    val price: Int,
    val rating: Double,
    val deliveryMins: Int,
    val badge: String
)

data class IncomingJobRequest(
    val id: String,
    val serviceType: String, // Bike Taxi, Auto, Cab, Food Delivery, Parcel, Mini Truck
    val customerName: String,
    val pickupTitle: String,
    val dropTitle: String,
    val pickupDistKm: Double,
    val tripDistKm: Double,
    val fare: Int,
    val partnerEarnings: Int,
    val paymentType: String,
    val otp: String,
    val restaurantOrShop: String = ""
)

data class CouponItem(
    val code: String,
    val title: String,
    val description: String,
    val discountAmount: Int,
    val minOrder: Int,
    val category: String
)

data class SavedAddress(
    val label: String,
    val fullAddress: String,
    val landmark: String
)

data class AdminPlatformConfig(
    val platformCommissionPct: Int = 12,
    val baseDeliveryFee: Int = 29,
    val surgeMultiplier: Double = 1.0,
    val activeCities: List<String> = listOf("Bengaluru", "Mumbai", "Hyderabad", "Delhi NCR", "Pune"),
    val enabledServices: Set<String> = setOf("Ride Booking", "Food Delivery", "Instant Grocery", "Parcel Delivery", "Goods Transport", "Local Shopping")
)
