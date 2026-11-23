package com.example.data

import android.content.Context
import androidx.room.Room
import com.example.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

class SuperAppRepository(private val db: ZiporaDatabase) {

    val appState: Flow<AppStateEntity> = db.appStateDao().observeState().map { it ?: AppStateEntity() }
    val orders: Flow<List<OrderEntity>> = db.orderDao().observeOrders()
    val transactions: Flow<List<WalletTxEntity>> = db.walletTxDao().observeTransactions()
    val menuItems: Flow<List<MenuItemEntity>> = db.menuItemDao().observeAll()
    val groceryProducts: Flow<List<GroceryProductEntity>> = db.groceryDao().observeAll()
    val notifications: Flow<List<NotificationEntity>> = db.notificationDao().observeAll()
    val managedUsers: Flow<List<ManagedUserEntity>> = db.managedUserDao().observeAll()

    suspend fun ensureSeeded() {
        if (db.appStateDao().getStateOnce() == null) {
            db.appStateDao().upsert(AppStateEntity())
        }
        val now = System.currentTimeMillis()
        if (db.menuItemDao().count() == 0) {
            db.menuItemDao().insertAll(DemoCatalog.seedMenuItems())
        }
        if (db.groceryDao().count() == 0) {
            db.groceryDao().insertAll(DemoCatalog.seedGroceryProducts())
        }
        if (db.orderDao().count() == 0) {
            db.orderDao().insertAll(DemoCatalog.seedOrders(now))
        }
        if (db.walletTxDao().count() == 0) {
            db.walletTxDao().insertAll(DemoCatalog.seedWalletTransactions(now))
        }
        if (db.notificationDao().count() == 0) {
            db.notificationDao().insertAll(DemoCatalog.seedNotifications(now))
        }
        if (db.managedUserDao().count() == 0) {
            db.managedUserDao().insertAll(DemoCatalog.seedManagedUsers())
        }
    }

    suspend fun updateAppState(transform: (AppStateEntity) -> AppStateEntity) {
        val current = db.appStateDao().getStateOnce() ?: AppStateEntity()
        db.appStateDao().upsert(transform(current))
    }

    suspend fun createOrder(order: OrderEntity, deductFromWallet: Boolean) {
        db.orderDao().insertOrder(order)
        if (deductFromWallet) {
            updateAppState { s ->
                s.copy(
                    walletBalance = (s.walletBalance - order.amount).coerceAtLeast(0.0),
                    rewardPoints = s.rewardPoints + (order.amount / 10).toInt().coerceAtLeast(10)
                )
            }
            db.walletTxDao().insertTx(
                WalletTxEntity(
                    id = "TX-${UUID.randomUUID().toString().take(6).uppercase()}",
                    title = order.title,
                    txType = "DEBIT",
                    amount = order.amount,
                    method = order.paymentMethod,
                    timestamp = System.currentTimeMillis()
                )
            )
        } else {
            updateAppState { s ->
                s.copy(rewardPoints = s.rewardPoints + (order.amount / 12).toInt().coerceAtLeast(10))
            }
        }
        pushNotification(
            category = when (order.type) {
                "RIDE" -> "Booking Confirmation"
                "FOOD" -> "Food Order Accepted"
                "GROCERY" -> "Instant Grocery"
                "PARCEL" -> "Parcel Confirmed"
                "TRANSPORT" -> "Transport Confirmed"
                else -> "Order Confirmed"
            },
            title = "${order.title} Confirmed",
            message = "Order ${order.id} • ₹${order.amount.toInt()} via ${order.paymentMethod}. OTP: ${order.otp}"
        )
    }

    suspend fun updateOrderStatus(orderId: String, status: String) {
        db.orderDao().updateStatus(orderId, status)
        pushNotification(
            category = "Order Update",
            title = "Order $orderId is now ${status.replace('_', ' ')}",
            message = "Live status synced across your Zipora ecosystem."
        )
    }

    suspend fun rateOrder(orderId: String, rating: Int) {
        db.orderDao().updateRating(orderId, rating)
    }

    suspend fun addWalletMoney(amount: Double, method: String, txType: String = "CREDIT", title: String = "Added to Zipora Wallet") {
        updateAppState { s -> s.copy(walletBalance = s.walletBalance + amount) }
        db.walletTxDao().insertTx(
            WalletTxEntity(
                id = "TX-${UUID.randomUUID().toString().take(6).uppercase()}",
                title = title,
                txType = txType,
                amount = amount,
                method = method,
                timestamp = System.currentTimeMillis()
            )
        )
        pushNotification(
            category = "Payment Successful",
            title = "₹${amount.toInt()} Credited to Wallet",
            message = "Simulated $method transaction completed successfully."
        )
    }

    suspend fun upsertMenuItem(item: MenuItemEntity) = db.menuItemDao().upsert(item)
    suspend fun setMenuItemAvailable(id: String, available: Boolean) = db.menuItemDao().setAvailable(id, available)
    suspend fun updateMenuItemPrice(id: String, price: Int) = db.menuItemDao().updatePrice(id, price)

    suspend fun upsertGroceryProduct(product: GroceryProductEntity) = db.groceryDao().upsert(product)
    suspend fun updateGroceryStockAndPrice(id: String, stock: Int, price: Int) = db.groceryDao().updateStockAndPrice(id, stock, price)

    suspend fun updateManagedUser(user: ManagedUserEntity) = db.managedUserDao().update(user)

    suspend fun pushNotification(category: String, title: String, message: String) {
        db.notificationDao().insert(
            NotificationEntity(
                id = "N-${UUID.randomUUID().toString().take(6).uppercase()}",
                category = category,
                title = title,
                message = message,
                timestamp = System.currentTimeMillis(),
                isRead = false
            )
        )
    }

    suspend fun markAllNotificationsRead() = db.notificationDao().markAllRead()

    companion object {
        @Volatile
        private var INSTANCE: ZiporaDatabase? = null

        fun getDatabase(context: Context): ZiporaDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    ZiporaDatabase::class.java,
                    "zipora_superapp.db"
                ).fallbackToDestructiveMigration(true).build().also { INSTANCE = it }
            }
        }
    }
}

object DemoCatalog {

    val savedAddresses = listOf(
        SavedAddress("Home", "402, Skyview Residency, 100ft Rd, Indiranagar, Bengaluru", "Near Metro Station"),
        SavedAddress("Work", "Tower B, Bagmane Tech Park, CV Raman Nagar, Bengaluru", "Gate 3 Lobby"),
        SavedAddress("Gym", "Cult.Fit Studio, 12th Main, HAL 2nd Stage, Bengaluru", "Opp. Cafe Coffee Day"),
        SavedAddress("Airport", "Kempegowda International Airport, Terminal 2, Bengaluru", "Departure Drop Lane 4")
    )

    val rideVehicles = listOf(
        VehicleOption("bike", "Bike Taxi", "Fastest traffic beater", 2, "1 Rider", 25, 8, "FASTEST"),
        VehicleOption("auto", "Auto Rickshaw", "Zero surge everyday auto", 4, "3 Riders", 40, 13, "POPULAR"),
        VehicleOption("mini", "Mini Cab", "Compact AC hatchback", 5, "4 Riders", 65, 16, "VALUE"),
        VehicleOption("sedan", "Prime Sedan", "Spacious boot & top captains", 6, "4 Riders", 95, 20, "COMFORT"),
        VehicleOption("suv", "Executive SUV", "6-seater group luxury", 8, "6 Riders", 140, 26, "GROUP")
    )

    val transportVehicles = listOf(
        TransportVehicleOption("twowheel", "Two Wheeler", 20, "40 x 40 cm", 49, 10, 5, "Urgent docs & keys"),
        TransportVehicleOption("threewheel", "Three Wheeler", 500, "5.5 x 4.5 ft", 180, 22, 9, "Appliances & cartons"),
        TransportVehicleOption("tata_ace", "Tata Ace", 750, "7 x 4.8 ft", 280, 28, 12, "1BHK shift & retail stock"),
        TransportVehicleOption("pickup", "Pickup 8ft", 1200, "8 x 5 ft", 390, 34, 15, "Furniture & hardware"),
        TransportVehicleOption("mini_truck", "Mini Truck", 2000, "10 x 6 ft", 650, 45, 20, "Office move & wholesale"),
        TransportVehicleOption("large_truck", "Large Truck", 3500, "14 x 6.5 ft", 1150, 62, 28, "Industrial & bulk cargo")
    )

    val restaurants = listOf(
        RestaurantInfo("r1", "Artisan Crust Pizzeria", "Wood-Fired Pizza, Italian, Pasta", 4.7, 2410, 22, 1.8, 550, "50% OFF up to ₹120", false, "Pizza", 0xFFFF9F43),
        RestaurantInfo("r2", "Nizam Royal Biryani", "Hyderabadi Biryani, Kebabs, Mughlai", 4.8, 5120, 26, 2.4, 600, "FLAT ₹100 OFF", false, "Biryani", 0xFFEF4444),
        RestaurantInfo("r3", "Smash & Melt Burgers", "Gourmet Burgers, Loaded Fries, Shakes", 4.6, 1890, 19, 1.2, 450, "FREE FRIES ON ₹299+", false, "Burger", 0xFFFFD43B),
        RestaurantInfo("r4", "Madras Filter Kitchen", "South Indian, Ghee Roast, Filter Coffee", 4.9, 6300, 16, 0.9, 300, "20% OFF + FREE DELIVERY", true, "South Indian", 0xFF22C55E),
        RestaurantInfo("r5", "Tandoor Haveli Express", "North Indian, Dal Makhani, Naan", 4.5, 3100, 28, 3.1, 650, "₹125 OFF above ₹499", false, "North Indian", 0xFF8B5CF6),
        RestaurantInfo("r6", "Wok Dragon Street", "Chinese, Dimsum, Schezwan Noodles", 4.4, 1450, 24, 2.2, 480, "BUY 1 GET 1 BOWLS", false, "Chinese", 0xFF38BDF8),
        RestaurantInfo("r7", "Velvet Bean Patisserie", "Desserts, Tiramisu, Cold Brew", 4.8, 2280, 18, 1.5, 400, "30% OFF DESSERTS", true, "Desserts", 0xFFEC4899)
    )

    val localShopProducts = listOf(
        LocalShopProduct("ls1", "AeroPods Pro ANC Earbuds", "Croma Express Indiranagar", "Electronics", 3999, 2499, 4.7, 25, "38% OFF"),
        LocalShopProduct("ls2", "65W GaN Fast USB-C Charger", "TechHive Local Store", "Electronics", 2499, 1499, 4.8, 20, "EXPRESS"),
        LocalShopProduct("ls3", "Linen Breeze Overshirt", "Urban Loom Boutique", "Fashion", 2199, 1349, 4.6, 35, "TRENDING"),
        LocalShopProduct("ls4", "CloudRun Mesh Sneakers", "Stride Studio 12th Main", "Fashion", 4299, 2999, 4.8, 40, "NEW DROP"),
        LocalShopProduct("ls5", "Nordic Ceramic Diffuser Set", "Habitat Home Decor", "Home Essentials", 1599, 999, 4.7, 30, "BESTSELLER"),
        LocalShopProduct("ls6", "Smart LED Desk Lamp", "Lumina Living Hub", "Home Essentials", 2299, 1599, 4.5, 28, "25 MIN")
    )

    val coupons = listOf(
        CouponItem("ZIP50", "Flat ₹50 Off", "Valid on Food, Grocery & Parcels above ₹199", 50, 199, "ALL"),
        CouponItem("RIDEFIRST", "₹30 Off Your Ride", "Instant savings on Bike Taxi, Auto & Cab", 30, 60, "RIDE"),
        CouponItem("BLINK100", "₹100 Super Saver", "Save big on Instant Grocery baskets over ₹499", 100, 499, "GROCERY"),
        CouponItem("MOVE150", "₹150 Off Goods Transport", "Applies to Tata Ace, Pickup & Mini Truck bookings", 150, 350, "TRANSPORT")
    )

    val incomingDriverRequests = listOf(
        IncomingJobRequest("REQ-901", "Bike Taxi", "Rohan Verma", "Indiranagar Metro Gate 2", "Koramangala 4th Block", 0.6, 5.4, 68, 59, "UPI", "4821"),
        IncomingJobRequest("REQ-902", "Auto Rickshaw", "Ananya Iyer", "100ft Rd, HAL 2nd Stage", "MG Road Trinity Circle", 1.1, 4.2, 96, 84, "Zipora Wallet", "7390"),
        IncomingJobRequest("REQ-903", "Mini Cab", "Vikram Malhotra", "Domlur Flyover", "Manyata Tech Park Gate 5", 1.5, 11.8, 254, 218, "Cash", "1945")
    )

    val incomingFoodDeliveries = listOf(
        IncomingJobRequest("FD-411", "Food Delivery", "Meera Nair", "Artisan Crust Pizzeria", "HSR Sector 2, Apt 5B", 0.8, 3.6, 540, 72, "Paid Online", "6204", "Artisan Crust Pizzeria"),
        IncomingJobRequest("FD-412", "Food Delivery", "Kabir Sen", "Nizam Royal Biryani", "Bellandur Green Glen", 1.2, 4.9, 680, 88, "UPI", "3188", "Nizam Royal Biryani")
    )

    val incomingParcelAndTransport = listOf(
        IncomingJobRequest("PR-701", "Parcel Delivery", "Sneha Rao", "WeWork Embassy GolfLinks", "Whitefield ITPL Main Rd", 0.9, 9.2, 149, 125, "Zipora Wallet", "8842"),
        IncomingJobRequest("TR-802", "Tata Ace Cargo", "Suresh Hardware Depot", "KR Market Wholesale Hub", "Electronic City Phase 1", 2.3, 16.5, 742, 640, "Business Invoice", "5519")
    )

    fun seedMenuItems(): List<MenuItemEntity> = listOf(
        MenuItemEntity("m1", "r1", "Wood-Fired Pizza", "Truffle Burrata Margherita", "San Marzano tomatoes, fresh creamy burrata, basil oil", 429, true, true, true, 4.8),
        MenuItemEntity("m2", "r1", "Wood-Fired Pizza", "Smoked Pepperoni Hot Honey", "Crispy pepperoni cups, mozzarella, chilli blossom honey", 499, false, true, true, 4.9),
        MenuItemEntity("m3", "r1", "Sides & Beverages", "Garlic Herb Knots (6 pcs)", "Oven-baked sourdough knots with roasted garlic dip", 189, true, true, false, 4.6),
        MenuItemEntity("m4", "r2", "Royal Biryani", "Hyderabadi Chicken Dum Biryani", "Long-grain saffron basmati, succulent spiced chicken, mirchi salan", 349, false, true, true, 4.9),
        MenuItemEntity("m5", "r2", "Royal Biryani", "Subz Zafrani Paneer Biryani", "Chargrilled malai paneer layered with caramelized onions", 319, true, true, false, 4.7),
        MenuItemEntity("m6", "r2", "Kebabs", "Galouti Melt Kebab Platter", "4 melt-in-mouth saffron kebabs served with ulte tawe ka paratha", 299, false, true, true, 4.8),
        MenuItemEntity("m7", "r3", "Signature Burgers", "Double Truffle Smash Burger", "Crispy lacy patties, aged cheddar, caramelized onions, brioche", 289, false, true, true, 4.8),
        MenuItemEntity("m8", "r3", "Signature Burgers", "Crispy Halloumi Chipotle Burger", "Golden fried paneer & halloumi, chipotle aioli, pickled jalapenos", 249, true, true, false, 4.6),
        MenuItemEntity("m9", "r4", "South Indian Classics", "Benne Masala Dosa", "Crisp golden butter dosa with potato palya & 3 chutneys", 145, true, true, true, 4.9),
        MenuItemEntity("m10", "r4", "Beverages", "Kumbakonam Brass Filter Coffee", "Slow-brewed chicory decoction with frothy farm milk", 75, true, true, true, 4.9),
        MenuItemEntity("m11", "r5", "North Indian Mains", "24-Hr Slow Cooked Dal Makhani", "Creamy black lentils simmered with white butter & kasuri methi", 295, true, true, true, 4.7),
        MenuItemEntity("m12", "r5", "Breads", "Chur Chur Garlic Naan", "Flaky tandoor baked naan brushed with roasted garlic ghee", 95, true, true, false, 4.6),
        MenuItemEntity("m13", "r6", "Wok Specials", "Burnt Garlic Chilli Hakka Noodles", "Wok-tossed noodles with crisp bok choy & chilli crisp", 239, true, true, true, 4.5),
        MenuItemEntity("m14", "r7", "Desserts", "Belgian Dark Chocolate Tiramisu", "Espresso-soaked savoiardi with mascarpone mousse", 265, true, true, true, 4.9)
    )

    fun seedGroceryProducts(): List<GroceryProductEntity> = listOf(
        GroceryProductEntity("g1", "Fresh Organic Strawberries", "Fruits", "200 g pack", 149, 109, 42, 8, true),
        GroceryProductEntity("g2", "Hass Ripe Avocados (2 pcs)", "Fruits", "320 g", 280, 215, 25, 8, true),
        GroceryProductEntity("g3", "Baby Spinach Hydroponic", "Vegetables", "150 g", 85, 62, 38, 9, false),
        GroceryProductEntity("g4", "Cherry Tomatoes Vine Fresh", "Vegetables", "250 g", 95, 69, 54, 9, false),
        GroceryProductEntity("g5", "A2 Farm Fresh Gir Cow Milk", "Dairy", "1 Litre", 98, 86, 65, 8, true),
        GroceryProductEntity("g6", "Artisan Greek Yogurt Honey", "Dairy", "400 g tub", 180, 149, 29, 9, false),
        GroceryProductEntity("g7", "Kettle Sea Salt Potato Chips", "Snacks", "125 g", 99, 79, 80, 8, true),
        GroceryProductEntity("g8", "Cold Pressed Valencia Orange", "Drinks", "500 ml", 160, 125, 34, 8, true),
        GroceryProductEntity("g9", "Country Sourdough Boule", "Bakery", "400 g loaf", 190, 155, 18, 10, true),
        GroceryProductEntity("g10", "Botanical Tea Tree Face Wash", "Personal Care", "150 ml", 349, 269, 22, 10, false),
        GroceryProductEntity("g11", "Eco Citrus Floor Cleaner", "Household Cleaning", "1 Litre", 220, 174, 41, 10, false),
        GroceryProductEntity("g12", "Ultra Soft Baby Wipes (72s)", "Baby Care", "Pack of 72", 199, 145, 36, 9, false),
        GroceryProductEntity("g13", "Cold Pressed Extra Virgin Olive Oil", "Daily Essentials", "500 ml", 699, 549, 19, 10, true),
        GroceryProductEntity("g14", "Himalayan Pink Rock Salt", "Daily Essentials", "1 kg", 140, 105, 50, 9, false)
    )

    fun seedOrders(now: Long): List<OrderEntity> = listOf(
        OrderEntity("ZP-8841", "FOOD", "Artisan Crust Pizzeria", "1x Truffle Burrata Margherita, 1x Garlic Knots", "PREPARING", 618.0, "Zipora Wallet", "Artisan Crust, Indiranagar", "402, Skyview Residency", "4921", "Kiran Gowda", now - 900_000L, 0),
        OrderEntity("ZP-8839", "RIDE", "Bike Taxi • Indiranagar to MG Road", "4.8 km • 14 mins • Captain Arjun K.", "COMPLETED", 64.0, "UPI", "100ft Rd Indiranagar", "MG Road Metro", "3104", "Arjun Kumar", now - 7_200_000L, 5),
        OrderEntity("ZP-8832", "GROCERY", "Zipora Blink Basket (5 items)", "Strawberries, A2 Milk, Sourdough & 2 more", "COMPLETED", 495.0, "Zipora Wallet", "Zipora Dark Store Indiranagar", "402, Skyview Residency", "8820", "Deepak R.", now - 43_200_000L, 5),
        OrderEntity("ZP-8825", "PARCEL", "Express Document Parcel", "Legal Folder • 0.5 kg • Verified OTP", "COMPLETED", 98.0, "UPI", "Indiranagar Home", "Koramangala WeWork", "2051", "Manoj Singh", now - 86_400_000L, 4),
        OrderEntity("ZP-8810", "TRANSPORT", "Tata Ace • Furniture Move", "14.2 km • 1 Loading Helper", "PENDING", 690.0, "Debit Card", "HSR Layout Sector 1", "Whitefield Prestige Shantiniketan", "7732", "Ramesh Yadav", now - 1_800_000L, 0)
    )

    fun seedWalletTransactions(now: Long): List<WalletTxEntity> = listOf(
        WalletTxEntity("TX-9901", "Zipora Super Cashback", "CASHBACK", 120.0, "Promo Credit", now - 3_600_000L),
        WalletTxEntity("TX-9902", "Artisan Crust Pizzeria", "DEBIT", 618.0, "Zipora Wallet", now - 900_000L),
        WalletTxEntity("TX-9903", "Wallet Top-up via UPI", "CREDIT", 2000.0, "UPI • GPay", now - 21_600_000L),
        WalletTxEntity("TX-9904", "Cancelled Order Refund", "REFUND", 185.0, "Instant Refund", now - 64_800_000L),
        WalletTxEntity("TX-9905", "Zipora Blink Basket", "DEBIT", 495.0, "Zipora Wallet", now - 43_200_000L)
    )

    fun seedNotifications(now: Long): List<NotificationEntity> = listOf(
        NotificationEntity("N-101", "Food Prepared", "Chef is firing your Truffle Burrata Pizza", "Artisan Crust Pizzeria is packing Order ZP-8841. Pickup in 4 mins.", now - 300_000L, false),
        NotificationEntity("N-102", "Promotional Offers", "Flat ₹100 OFF on Instant Groceries!", "Use code BLINK100 on your next 9-minute basket above ₹499.", now - 1_800_000L, false),
        NotificationEntity("N-103", "Payment Successful", "₹120 Cashback Credited", "Your super app loyalty reward is now active in Zipora Wallet.", now - 3_600_000L, true),
        NotificationEntity("N-104", "Ride Completed", "Thanks for riding with Captain Arjun!", "Your 4.8 km Bike Taxi trip saved 18 minutes in peak traffic.", now - 7_200_000L, true)
    )

    fun seedManagedUsers(): List<ManagedUserEntity> = listOf(
        ManagedUserEntity("U-101", "Aarav Sharma", "Customer", "Bengaluru", 4.9, true, false, 64),
        ManagedUserEntity("U-102", "Diya Menon", "Customer", "Mumbai", 4.8, true, false, 41),
        ManagedUserEntity("U-103", "Arjun Kumar", "Driver", "Bengaluru", 4.92, true, false, 1240),
        ManagedUserEntity("U-104", "Kiran Gowda", "Driver", "Bengaluru", 4.78, true, false, 860),
        ManagedUserEntity("U-105", "Ravi Teja", "Driver", "Hyderabad", 4.4, false, false, 112),
        ManagedUserEntity("U-106", "Artisan Crust Pizzeria", "Restaurant", "Bengaluru", 4.7, true, false, 2410),
        ManagedUserEntity("U-107", "Nizam Royal Biryani", "Restaurant", "Hyderabad", 4.8, true, false, 5120),
        ManagedUserEntity("U-108", "FreshMart Indiranagar", "Shop", "Bengaluru", 4.85, true, false, 3190),
        ManagedUserEntity("U-109", "Metro Daily Mart", "Shop", "Pune", 4.3, false, false, 410)
    )
}
