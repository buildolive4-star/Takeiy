package com.example.data

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "app_state")
data class AppStateEntity(
    @PrimaryKey val id: Int = 1,
    val userName: String = "Aarav Sharma",
    val userPhone: String = "+91 98765 43210",
    val userEmail: String = "aarav.sharma@zipora.app",
    val activeAddressLabel: String = "Home",
    val activeAddressLine: String = "402, Skyview Residency, Indiranagar, Bengaluru",
    val walletBalance: Double = 2450.0,
    val rewardPoints: Int = 1280,
    val activePanelName: String = "CUSTOMER",
    val isAdminAuthorized: Boolean = false,
    val driverOnline: Boolean = true,
    val deliveryPartnerOnline: Boolean = true,
    val language: String = "English (India)",
    val notificationsEnabled: Boolean = true,
    val emergencyContactName: String = "Priya Sharma",
    val emergencyContactPhone: String = "+91 98450 11223"
)

@Entity(tableName = "orders")
data class OrderEntity(
    @PrimaryKey val id: String,
    val type: String, // RIDE, FOOD, GROCERY, PARCEL, TRANSPORT, SHOPPING
    val title: String,
    val subtitle: String,
    val status: String, // PENDING, ACCEPTED, PREPARING, ON_THE_WAY, COMPLETED, CANCELLED, REFUNDED
    val amount: Double,
    val paymentMethod: String,
    val pickupLocation: String,
    val dropLocation: String,
    val otp: String,
    val partnerName: String,
    val timestamp: Long,
    val rating: Int = 0
)

@Entity(tableName = "wallet_transactions")
data class WalletTxEntity(
    @PrimaryKey val id: String,
    val title: String,
    val txType: String, // CREDIT, DEBIT, CASHBACK, REFUND, PROMO
    val amount: Double,
    val method: String,
    val timestamp: Long
)

@Entity(tableName = "menu_items")
data class MenuItemEntity(
    @PrimaryKey val id: String,
    val restaurantId: String,
    val category: String,
    val name: String,
    val description: String,
    val price: Int,
    val isVeg: Boolean,
    val isAvailable: Boolean = true,
    val isBestseller: Boolean = false,
    val rating: Double = 4.6
)

@Entity(tableName = "grocery_products")
data class GroceryProductEntity(
    @PrimaryKey val id: String,
    val name: String,
    val category: String,
    val unit: String,
    val mrp: Int,
    val sellingPrice: Int,
    val stockCount: Int,
    val deliveryMins: Int = 9,
    val isFeatured: Boolean = false
)

@Entity(tableName = "notifications")
data class NotificationEntity(
    @PrimaryKey val id: String,
    val category: String,
    val title: String,
    val message: String,
    val timestamp: Long,
    val isRead: Boolean = false
)

@Entity(tableName = "managed_users")
data class ManagedUserEntity(
    @PrimaryKey val id: String,
    val name: String,
    val role: String, // Customer, Driver, Restaurant, Shop
    val city: String,
    val rating: Double,
    val isVerified: Boolean,
    val isSuspended: Boolean,
    val totalOrders: Int
)

@Dao
interface AppStateDao {
    @Query("SELECT * FROM app_state WHERE id = 1")
    fun observeState(): Flow<AppStateEntity?>

    @Query("SELECT * FROM app_state WHERE id = 1")
    suspend fun getStateOnce(): AppStateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(state: AppStateEntity)
}

@Dao
interface OrderDao {
    @Query("SELECT * FROM orders ORDER BY timestamp DESC")
    fun observeOrders(): Flow<List<OrderEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrder(order: OrderEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(orders: List<OrderEntity>)

    @Query("UPDATE orders SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: String, status: String)

    @Query("UPDATE orders SET rating = :rating WHERE id = :id")
    suspend fun updateRating(id: String, rating: Int)

    @Query("SELECT COUNT(*) FROM orders")
    suspend fun count(): Int
}

@Dao
interface WalletTxDao {
    @Query("SELECT * FROM wallet_transactions ORDER BY timestamp DESC")
    fun observeTransactions(): Flow<List<WalletTxEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTx(tx: WalletTxEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(txs: List<WalletTxEntity>)

    @Query("SELECT COUNT(*) FROM wallet_transactions")
    suspend fun count(): Int
}

@Dao
interface MenuItemDao {
    @Query("SELECT * FROM menu_items ORDER BY isBestseller DESC, name ASC")
    fun observeAll(): Flow<List<MenuItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: MenuItemEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<MenuItemEntity>)

    @Query("UPDATE menu_items SET isAvailable = :available WHERE id = :id")
    suspend fun setAvailable(id: String, available: Boolean)

    @Query("UPDATE menu_items SET price = :price WHERE id = :id")
    suspend fun updatePrice(id: String, price: Int)

    @Query("SELECT COUNT(*) FROM menu_items")
    suspend fun count(): Int
}

@Dao
interface GroceryDao {
    @Query("SELECT * FROM grocery_products ORDER BY isFeatured DESC, name ASC")
    fun observeAll(): Flow<List<GroceryProductEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(product: GroceryProductEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(products: List<GroceryProductEntity>)

    @Query("UPDATE grocery_products SET stockCount = :stock, sellingPrice = :price WHERE id = :id")
    suspend fun updateStockAndPrice(id: String, stock: Int, price: Int)

    @Query("SELECT COUNT(*) FROM grocery_products")
    suspend fun count(): Int
}

@Dao
interface NotificationDao {
    @Query("SELECT * FROM notifications ORDER BY timestamp DESC")
    fun observeAll(): Flow<List<NotificationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(notification: NotificationEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(notifications: List<NotificationEntity>)

    @Query("UPDATE notifications SET isRead = 1")
    suspend fun markAllRead()

    @Query("SELECT COUNT(*) FROM notifications")
    suspend fun count(): Int
}

@Dao
interface ManagedUserDao {
    @Query("SELECT * FROM managed_users ORDER BY role ASC, name ASC")
    fun observeAll(): Flow<List<ManagedUserEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(users: List<ManagedUserEntity>)

    @Update
    suspend fun update(user: ManagedUserEntity)

    @Query("SELECT COUNT(*) FROM managed_users")
    suspend fun count(): Int
}

@Database(
    entities = [
        AppStateEntity::class,
        OrderEntity::class,
        WalletTxEntity::class,
        MenuItemEntity::class,
        GroceryProductEntity::class,
        NotificationEntity::class,
        ManagedUserEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class ZiporaDatabase : RoomDatabase() {
    abstract fun appStateDao(): AppStateDao
    abstract fun orderDao(): OrderDao
    abstract fun walletTxDao(): WalletTxDao
    abstract fun menuItemDao(): MenuItemDao
    abstract fun groceryDao(): GroceryDao
    abstract fun notificationDao(): NotificationDao
    abstract fun managedUserDao(): ManagedUserDao
}
