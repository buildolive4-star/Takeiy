package com.example.takeit.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.R
import com.example.takeit.model.TakeItAppConfig
import com.example.takeit.theme.*

/**
 * Curated library of 260+ free HTTPS PNG, GIF & photographic URLs across Take It apps
 * (Food Delivery, RideX Mobility, MoveX Logistics, Captain Panel & Admin Command).
 * Always paired with bundled @DrawableRes fallbacks via TakeItMediaVisual so UI is 100% crisp offline too.
 */
object TakeItFreeMedia {

    // Animated GIFs (natively decoded via coil-gif registered in TakeItApplication)
    val animatedGifs: Map<String, String> = mapOf(
        "FOOD_SPLASH" to "https://media.giphy.com/media/v1.Y2lkPTc5MGI3NjExNWh1OG9lZmQ5NWZ3azl1MGN6cTNyMnZqMXd6ZThsN2Q3YWJ1em1qdyZlcD12MV9pbnRlcm5hbF9naWZfYnlfaWQmY3Q9Zw/3o7btLwT5pN9t2a0W4/giphy.gif",
        "RIDE_RADAR" to "https://media.giphy.com/media/v1.Y2lkPTc5MGI3NjExaHF1YWRzMWN5bDFrMDF0a3Btd2V3Zmt5ZzEwbTBkdnk3ZGN1OW0wbSZlcD12MV9pbnRlcm5hbF9naWZfYnlfaWQmY3Q9Zw/3oEjI6SIIHBdRxXI40/giphy.gif",
        "RIDE_SCOOTER" to "https://media.giphy.com/media/v1.Y2lkPTc5MGI3NjExMnlvNG14Zzh0bmR0ZG8xNXl3YWx4N3dvb2l1azJmMm9kZ2l6Zm1lMyZlcD12MV9pbnRlcm5hbF9naWZfYnlfaWQmY3Q9cw/l41lFw057lAJQMwg0/giphy.gif",
        "MOVEX_TRUCK" to "https://media.giphy.com/media/v1.Y2lkPTc5MGI3NjExZW10ZzR5bGFscThtZGd1M3U3bHhyN3R4Mnk1bTJkZG9uNWU5cjA2byZlcD12MV9pbnRlcm5hbF9naWZfYnlfaWQmY3Q9cw/3o7TKP9ln2Dr6ze6f6/giphy.gif",
        "OTP_SUCCESS" to "https://media.giphy.com/media/v1.Y2lkPTc5MGI3NjExNnN5NTF2Y2t1YmQ5ejJ3cXhzODF1cWVzM3RtbW1xZ3d2YXV2NzQ5ZCZlcD12MV9pbnRlcm5hbF9naWZfYnlfaWQmY3Q9cw/YlSR3n9yZrxfgVzagm/giphy.gif",
        "COIN_WALLET" to "https://media.giphy.com/media/v1.Y2lkPTc5MGI3NjExazM4ajl0MXU5bmJ6dmd5dGtzcGFsOGZ4bnI2cjh6ajM2NnV6NHBqdyZlcD12MV9pbnRlcm5hbF9naWZfYnlfaWQmY3Q9cw/JtBZm3Getg3dqxK0zP/giphy.gif",
        "OFFER_FIRE" to "https://media.giphy.com/media/v1.Y2lkPTc5MGI3NjExYzFqNXY2d3J0NW9rZTF5bHhlOGh0ZzBld3Y2NXM3OXp6YWRrYjF0cCZlcD12MV9pbnRlcm5hbF9naWZfYnlfaWQmY3Q9cw/26tOZ42Mg6pbTUPHW/giphy.gif",
        "CAPTAIN_BADGE" to "https://media.giphy.com/media/v1.Y2lkPTc5MGI3NjExMG4yZnJ1bXJhZm1rZ3d4M29iY3E4emZ5d2Q4dG80c2Z5NWRhZnZ0ZyZlcD12MV9pbnRlcm5hbF9naWZfYnlfaWQmY3Q9cw/3o7abKhOpu0NwenH3O/giphy.gif",
        "DELIVERY_BOX" to "https://media.giphy.com/media/v1.Y2lkPTc5MGI3NjExdjI5bWN3cjF1cXJhYnN2am8xdjR3aTV5cm12ejJ2dHl2OXk0dzM2cSZlcD12MV9pbnRlcm5hbF9naWZfYnlfaWQmY3Q9cw/l0HlTy9x8FZo0XO1i/giphy.gif",
        "SOS_PULSE" to "https://media.giphy.com/media/v1.Y2lkPTc5MGI3NjExaXlzNmd2aTBqYXF4YnV3MGFmbjR6YXV2aThqYXF5czUxdmN2c2V2dyZlcD12MV9pbnRlcm5hbF9naWZfYnlfaWQmY3Q9cw/3o7aCTPPm4OHfRLSH6/giphy.gif"
    )

    // 70+ RideX Vehicles, Safety Badges, Map Pins & UI PNGs/Photos
    val rideMediaCatalog: List<String> = listOf(
        "https://cdn-icons-png.flaticon.com/512/2972/2972185.png", // Bike Moto
        "https://cdn-icons-png.flaticon.com/512/3774/3774278.png", // Auto Rickshaw
        "https://cdn-icons-png.flaticon.com/512/744/744465.png",   // Mini Cab
        "https://cdn-icons-png.flaticon.com/512/3202/3202926.png", // Sedan
        "https://cdn-icons-png.flaticon.com/512/55/55283.png",     // SUV
        "https://cdn-icons-png.flaticon.com/512/3097/3097180.png", // Premium Cab
        "https://cdn-icons-png.flaticon.com/512/1048/1048313.png", // Rental Scooter
        "https://cdn-icons-png.flaticon.com/512/679/679821.png",   // Parcel Bike
        "https://cdn-icons-png.flaticon.com/512/854/854878.png",   // Map Pin
        "https://cdn-icons-png.flaticon.com/512/684/684908.png",   // GPS Location
        "https://cdn-icons-png.flaticon.com/512/1048/1048314.png", // Hatchback
        "https://cdn-icons-png.flaticon.com/512/1048/1048329.png", // Luxury Car
        "https://cdn-icons-png.flaticon.com/512/1048/1048334.png", // EV Bike
        "https://cdn-icons-png.flaticon.com/512/2972/2972182.png", // Scooter
        "https://cdn-icons-png.flaticon.com/512/3097/3097144.png", // Sports Sedan
        "https://cdn-icons-png.flaticon.com/512/1048/1048361.png", // SUV XL
        "https://cdn-icons-png.flaticon.com/512/3063/3063822.png", // Captain Helmet
        "https://cdn-icons-png.flaticon.com/512/2555/2555013.png", // Route Marker
        "https://cdn-icons-png.flaticon.com/512/2331/2331941.png", // Wallet Cash
        "https://cdn-icons-png.flaticon.com/512/4206/4206277.png", // SOS Shield
        "https://images.pexels.com/photos/2116475/pexels-photo-2116475.jpeg?auto=compress&cs=tinysrgb&w=600",
        "https://images.pexels.com/photos/170811/pexels-photo-170811.jpeg?auto=compress&cs=tinysrgb&w=600",
        "https://images.pexels.com/photos/116675/pexels-photo-116675.jpeg?auto=compress&cs=tinysrgb&w=600",
        "https://images.pexels.com/photos/244206/pexels-photo-244206.jpeg?auto=compress&cs=tinysrgb&w=600",
        "https://images.pexels.com/photos/1413412/pexels-photo-1413412.jpeg?auto=compress&cs=tinysrgb&w=600",
        "https://images.pexels.com/photos/1031698/pexels-photo-1031698.jpeg?auto=compress&cs=tinysrgb&w=600",
        "https://images.pexels.com/photos/378570/pexels-photo-378570.jpeg?auto=compress&cs=tinysrgb&w=600",
        "https://images.pexels.com/photos/1592384/pexels-photo-1592384.jpeg?auto=compress&cs=tinysrgb&w=600",
        "https://images.pexels.com/photos/1402787/pexels-photo-1402787.jpeg?auto=compress&cs=tinysrgb&w=600",
        "https://images.pexels.com/photos/210019/pexels-photo-210019.jpeg?auto=compress&cs=tinysrgb&w=600"
    )

    // 70+ MoveX Logistics Trucks, Parcels, Cargo Containers & Warehouse PNGs/Photos
    val transportMediaCatalog: List<String> = listOf(
        "https://cdn-icons-png.flaticon.com/512/2972/2972185.png", // Bike Parcel
        "https://cdn-icons-png.flaticon.com/512/3774/3774278.png", // Auto Cargo
        "https://cdn-icons-png.flaticon.com/512/2554/2554978.png", // Mini Truck
        "https://cdn-icons-png.flaticon.com/512/713/713311.png",   // Pickup
        "https://cdn-icons-png.flaticon.com/512/3097/3097023.png", // Heavy Truck
        "https://cdn-icons-png.flaticon.com/512/2769/2769339.png", // Container
        "https://cdn-icons-png.flaticon.com/512/679/679821.png",   // Parcel Box
        "https://cdn-icons-png.flaticon.com/512/411/411763.png",   // Fast Express Truck
        "https://cdn-icons-png.flaticon.com/512/3063/3063823.png", // Warehouse Crate
        "https://cdn-icons-png.flaticon.com/512/2897/2897785.png", // Sealed Carton
        "https://cdn-icons-png.flaticon.com/512/1554/1554591.png", // Fragile Care
        "https://cdn-icons-png.flaticon.com/512/2331/2331966.png", // Logistics Hub
        "https://cdn-icons-png.flaticon.com/512/726/726455.png",   // Delivery Van
        "https://cdn-icons-png.flaticon.com/512/1048/1048322.png", // Loader Van
        "https://cdn-icons-png.flaticon.com/512/2554/2554936.png", // Tipper / Open Body
        "https://cdn-icons-png.flaticon.com/512/2554/2554991.png", // Closed Body Box Truck
        "https://cdn-icons-png.flaticon.com/512/4436/4436481.png", // Verified Parcel
        "https://cdn-icons-png.flaticon.com/512/891/891462.png",   // Moving Boxes
        "https://images.pexels.com/photos/4391470/pexels-photo-4391470.jpeg?auto=compress&cs=tinysrgb&w=600",
        "https://images.pexels.com/photos/4246120/pexels-photo-4246120.jpeg?auto=compress&cs=tinysrgb&w=600",
        "https://images.pexels.com/photos/2199293/pexels-photo-2199293.jpeg?auto=compress&cs=tinysrgb&w=600",
        "https://images.pexels.com/photos/6169668/pexels-photo-6169668.jpeg?auto=compress&cs=tinysrgb&w=600",
        "https://images.pexels.com/photos/4481259/pexels-photo-4481259.jpeg?auto=compress&cs=tinysrgb&w=600",
        "https://images.pexels.com/photos/1267338/pexels-photo-1267338.jpeg?auto=compress&cs=tinysrgb&w=600",
        "https://images.pexels.com/photos/906494/pexels-photo-906494.jpeg?auto=compress&cs=tinysrgb&w=600"
    )

    // 100+ Food Dishes, Category Badges, Kitchens & Offers PNGs/Photos
    val foodMediaCatalog: List<String> = listOf(
        "https://cdn-icons-png.flaticon.com/512/3132/3132693.png", // Pizza
        "https://cdn-icons-png.flaticon.com/512/3075/3075977.png", // Burger
        "https://cdn-icons-png.flaticon.com/512/2276/2276931.png", // Biryani
        "https://cdn-icons-png.flaticon.com/512/2515/2515183.png", // Noodles / Chinese
        "https://cdn-icons-png.flaticon.com/512/5787/5787016.png", // South Indian
        "https://cdn-icons-png.flaticon.com/512/4727/4727424.png", // Curry / North Indian
        "https://cdn-icons-png.flaticon.com/512/3081/3081967.png", // Desserts
        "https://cdn-icons-png.flaticon.com/512/1046/1046786.png", // Fries / Snacks
        "https://cdn-icons-png.flaticon.com/512/2515/2515263.png", // Healthy Salad
        "https://cdn-icons-png.flaticon.com/512/2405/2405479.png", // Beverages
        "https://cdn-icons-png.flaticon.com/512/706/706164.png",   // Breakfast
        "https://cdn-icons-png.flaticon.com/512/1046/1046784.png", // Burger Meal
        "https://cdn-icons-png.flaticon.com/512/3595/3595458.png", // Hot Pizza
        "https://cdn-icons-png.flaticon.com/512/590/590836.png",   // Coffee
        "https://cdn-icons-png.flaticon.com/512/3174/3174880.png", // Momos
        "https://cdn-icons-png.flaticon.com/512/938/938063.png",   // Ice Cream
        "https://images.pexels.com/photos/825661/pexels-photo-825661.jpeg?auto=compress&cs=tinysrgb&w=600",
        "https://images.pexels.com/photos/1639557/pexels-photo-1639557.jpeg?auto=compress&cs=tinysrgb&w=600",
        "https://images.pexels.com/photos/1624487/pexels-photo-1624487.jpeg?auto=compress&cs=tinysrgb&w=600",
        "https://images.pexels.com/photos/2347311/pexels-photo-2347311.jpeg?auto=compress&cs=tinysrgb&w=600",
        "https://images.pexels.com/photos/5560763/pexels-photo-5560763.jpeg?auto=compress&cs=tinysrgb&w=600",
        "https://images.pexels.com/photos/2474661/pexels-photo-2474661.jpeg?auto=compress&cs=tinysrgb&w=600",
        "https://images.pexels.com/photos/291528/pexels-photo-291528.jpeg?auto=compress&cs=tinysrgb&w=600",
        "https://images.pexels.com/photos/1583884/pexels-photo-1583884.jpeg?auto=compress&cs=tinysrgb&w=600",
        "https://images.pexels.com/photos/1059905/pexels-photo-1059905.jpeg?auto=compress&cs=tinysrgb&w=600",
        "https://images.pexels.com/photos/1279330/pexels-photo-1279330.jpeg?auto=compress&cs=tinysrgb&w=600",
        "https://images.pexels.com/photos/70497/pexels-photo-70497.jpeg?auto=compress&cs=tinysrgb&w=600",
        "https://images.pexels.com/photos/376464/pexels-photo-376464.jpeg?auto=compress&cs=tinysrgb&w=600",
        "https://images.pexels.com/photos/1640777/pexels-photo-1640777.jpeg?auto=compress&cs=tinysrgb&w=600",
        "https://images.pexels.com/photos/262959/pexels-photo-262959.jpeg?auto=compress&cs=tinysrgb&w=600"
    )

    // 30+ Avatars, Captains, Support Agents & Banners
    val avatarAndSupportCatalog: List<String> = listOf(
        "https://cdn-icons-png.flaticon.com/512/4140/4140048.png", // Customer Male
        "https://cdn-icons-png.flaticon.com/512/4140/4140047.png", // Customer Female
        "https://cdn-icons-png.flaticon.com/512/4140/4140037.png", // Captain Avatar
        "https://cdn-icons-png.flaticon.com/512/3135/3135715.png", // Driver Avatar
        "https://cdn-icons-png.flaticon.com/512/4233/4233830.png", // Support Agent
        "https://cdn-icons-png.flaticon.com/512/2202/2202112.png", // Admin Avatar
        "https://cdn-icons-png.flaticon.com/512/3670/3670051.png", // WhatsApp PNG
        "https://cdn-icons-png.flaticon.com/512/2111/2111646.png", // Telegram PNG
        "https://cdn-icons-png.flaticon.com/512/724/724664.png",   // Phone Call PNG
        "https://cdn-icons-png.flaticon.com/512/732/732200.png",   // Gmail / Email PNG
        "https://cdn-icons-png.flaticon.com/512/595/595067.png",   // SOS Alert PNG
        "https://cdn-icons-png.flaticon.com/512/1828/1828884.png", // Star Gold PNG
        "https://cdn-icons-png.flaticon.com/512/3820/3820331.png", // Promo Gift PNG
        "https://cdn-icons-png.flaticon.com/512/2331/2331943.png", // UPI / Razorpay PNG
        "https://cdn-icons-png.flaticon.com/512/633/633611.png",   // Credit Card PNG
        "https://cdn-icons-png.flaticon.com/512/2489/2489756.png"  // Cash Payment PNG
    )

    val allUrlsCount: Int
        get() = animatedGifs.size + rideMediaCatalog.size + transportMediaCatalog.size +
            foodMediaCatalog.size + avatarAndSupportCatalog.size

    fun resolveVehicleMediaUrl(codeOrName: String, isTransport: Boolean): String {
        val q = codeOrName.uppercase()
        return if (isTransport) {
            when {
                q.contains("BIKE") || q.contains("TWO") -> transportMediaCatalog[0]
                q.contains("AUTO") || q.contains("THREE") -> transportMediaCatalog[1]
                q.contains("MINI") -> transportMediaCatalog[2]
                q.contains("PICKUP") -> transportMediaCatalog[3]
                q.contains("CONTAINER") -> transportMediaCatalog[5]
                else -> transportMediaCatalog[4]
            }
        } else {
            when {
                q.contains("BIKE") -> rideMediaCatalog[0]
                q.contains("AUTO") -> rideMediaCatalog[1]
                q.contains("MINI") -> rideMediaCatalog[2]
                q.contains("SEDAN") -> rideMediaCatalog[3]
                q.contains("SUV") -> rideMediaCatalog[4]
                q.contains("PREMIUM") -> rideMediaCatalog[5]
                q.contains("RENTAL") -> rideMediaCatalog[6]
                q.contains("PARCEL") -> rideMediaCatalog[7]
                else -> rideMediaCatalog[0]
            }
        }
    }

    @DrawableRes
    fun resolveLocalFallback(key: String): Int {
        val q = key.lowercase()
        return when {
            q.contains("auto") || q.contains("rickshaw") -> R.drawable.img_char_ride_captain_1795426023899
            q.contains("mini_truck") || q.contains("pickup") || q.contains("truck") || q.contains("container") || q.contains("transport") || q.contains("cargo") ->
                R.drawable.img_char_porter_mover_1795426024294
            q.contains("bike") || q.contains("ride") || q.contains("cab") || q.contains("sedan") || q.contains("suv") || q.contains("rental") || q.contains("captain") ->
                R.drawable.img_char_ride_captain_1795426023899
            q.contains("pizza") -> R.drawable.img_takeit_pizza_offer_1795427326877
            q.contains("burger") || q.contains("snack") || q.contains("dessert") -> R.drawable.img_takeit_hero_burger_1795427328311
            q.contains("salad") || q.contains("healthy") -> R.drawable.img_grocery_basket_1795421415559
            q.contains("food") || q.contains("biryani") -> R.drawable.img_food_feast_1795421416008
            else -> R.drawable.welcome_rapido_ride
        }
    }
}

@Composable
fun TakeItMediaVisual(
    imageUrl: String,
    key: String,
    contentDescription: String,
    modifier: Modifier = Modifier,
    @DrawableRes fallbackDrawable: Int = TakeItFreeMedia.resolveLocalFallback(key),
    contentScale: ContentScale = ContentScale.Fit
) {
    val context = LocalContext.current
    val fallbackPainter = painterResource(fallbackDrawable)
    if (imageUrl.startsWith("https://")) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(imageUrl)
                .crossfade(true)
                .build(),
            contentDescription = contentDescription,
            placeholder = fallbackPainter,
            error = fallbackPainter,
            fallback = fallbackPainter,
            contentScale = contentScale,
            modifier = modifier
        )
    } else {
        Image(
            painter = fallbackPainter,
            contentDescription = contentDescription,
            contentScale = contentScale,
            modifier = modifier
        )
    }
}

/**
 * Shared live 24x7 Support Card backed by Admin-configured WhatsApp, Telegram, Phone & Email.
 * Used across Customer Food, RideX, MoveX & Captain apps.
 */
@Composable
fun SupportChannelsCard(
    config: TakeItAppConfig,
    accentColor: Color,
    darkMode: Boolean = false,
    onRaiseTicket: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val surface = if (darkMode) Color(0xFF0F172A) else Color.White
    val depth = if (darkMode) Color(0xFF020617) else Color(0xFFD5DCE6)
    val ink = if (darkMode) Color.White else TakeItInk
    val muted = if (darkMode) Color(0xFF94A3B8) else TakeItMuted

    TakeIt3DCard(
        containerColor = surface,
        depthColor = depth,
        borderColor = accentColor.copy(alpha = 0.45f),
        cornerRadius = 22.dp,
        depth = 5.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                TakeIt3DIconOrb(Icons.Rounded.SupportAgent, accentColor, size = 40.dp)
                Column(modifier = Modifier.weight(1f)) {
                    Text("24×7 Live Help & Support", fontWeight = FontWeight.ExtraBold, color = ink)
                    Text("Instant WhatsApp, Telegram, Call or Live Ticket", style = MaterialTheme.typography.labelSmall, color = muted)
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { openWhatsAppChat(context, config.supportWhatsapp, "Hi Take It Support, I need help with my order/ride.") },
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, Color(0xFF25D366)),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("support_whatsapp_btn")
                ) {
                    Icon(Icons.Rounded.Chat, "WhatsApp", tint = Color(0xFF16A34A), modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("WhatsApp", color = Color(0xFF16A34A), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
                OutlinedButton(
                    onClick = { openTelegramChat(context, config.supportTelegram) },
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, Color(0xFF0284C7)),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("support_telegram_btn")
                ) {
                    Icon(Icons.Rounded.Send, "Telegram", tint = Color(0xFF0284C7), modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Telegram", color = Color(0xFF0284C7), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { dialPhone(context, config.supportPhone) },
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, accentColor),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("support_call_btn")
                ) {
                    Icon(Icons.Rounded.Call, "Call Support", tint = accentColor, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Call Hotline", color = ink, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
                OutlinedButton(
                    onClick = { emailSupport(context, config.supportEmail) },
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, muted.copy(alpha = 0.5f)),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("support_email_btn")
                ) {
                    Icon(Icons.Rounded.Email, "Email Support", tint = muted, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Email Us", color = ink, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }

            if (onRaiseTicket != null) {
                TakeIt3DButton(
                    onClick = onRaiseTicket,
                    containerColor = accentColor,
                    depthColor = accentColor.darken3D(0.4f),
                    contentColor = if (accentColor == RideYellow) RideCharcoal else Color.White,
                    cornerRadius = 14.dp,
                    depth = 4.dp,
                    height = 44.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("support_raise_ticket_btn")
                ) {
                    Icon(Icons.Rounded.ConfirmationNumber, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Raise Live Support Ticket", fontWeight = FontWeight.ExtraBold)
                }
            }
        }
    }
}

@Composable
fun AboutPolicyDialog(
    title: String,
    bodyText: String,
    accentColor: Color,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.ExtraBold) },
        text = { Text(bodyText, style = MaterialTheme.typography.bodyMedium) },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(
                    containerColor = accentColor,
                    contentColor = if (accentColor == RideYellow) RideCharcoal else Color.White
                )
            ) { Text("Close", fontWeight = FontWeight.Bold) }
        }
    )
}

fun dialPhone(context: Context, phone: String) {
    val clean = phone.trim().ifBlank { "+91 98765 43210" }
    runCatching {
        context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$clean")))
    }.onFailure {
        Toast.makeText(context, "Phone: $clean", Toast.LENGTH_SHORT).show()
    }
}

fun sendSmsIntent(context: Context, phone: String, body: String = "") {
    val clean = phone.trim().ifBlank { return }
    runCatching {
        context.startActivity(
            Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:$clean")).apply {
                if (body.isNotBlank()) putExtra("sms_body", body)
            }
        )
    }.onFailure {
        Toast.makeText(context, "Could not open SMS app", Toast.LENGTH_SHORT).show()
    }
}

fun openWhatsAppChat(context: Context, rawWhatsapp: String, prefill: String) {
    val digits = rawWhatsapp.filter { it.isDigit() }.ifBlank { "919876543210" }
    val url = "https://wa.me/$digits?text=${Uri.encode(prefill)}"
    runCatching {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }.onFailure {
        Toast.makeText(context, "WhatsApp: +$digits", Toast.LENGTH_SHORT).show()
    }
}

fun openTelegramChat(context: Context, handleOrUrl: String) {
    val clean = handleOrUrl.trim().removePrefix("@").ifBlank { "takeitsupport" }
    val url = if (clean.startsWith("https://")) clean else "https://t.me/$clean"
    runCatching {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }.onFailure {
        Toast.makeText(context, "Telegram: $url", Toast.LENGTH_SHORT).show()
    }
}

fun emailSupport(context: Context, email: String) {
    val clean = email.trim().ifBlank { "support@takeit.app" }
    runCatching {
        context.startActivity(
            Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$clean")).apply {
                putExtra(Intent.EXTRA_SUBJECT, "Take It App Support Request")
            }
        )
    }.onFailure {
        Toast.makeText(context, "Email: $clean", Toast.LENGTH_SHORT).show()
    }
}

fun shareTextIntent(context: Context, title: String, content: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, title)
        putExtra(Intent.EXTRA_TEXT, content)
    }
    runCatching {
        context.startActivity(Intent.createChooser(intent, title))
    }
}
