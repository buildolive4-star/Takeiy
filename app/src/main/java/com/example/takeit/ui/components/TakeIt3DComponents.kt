package com.example.takeit.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Computes a darker shaded 3D side/extrusion color from a face color so every
 * card and button has a natural physical depth bevel.
 */
fun Color.darken3D(factor: Float = 0.28f): Color = lerp(this, Color.Black, factor.coerceIn(0f, 0.85f))

/**
 * Computes a brighter specular highlight rim color.
 */
fun Color.lighten3D(factor: Float = 0.30f): Color = lerp(this, Color.White, factor.coerceIn(0f, 0.9f))

/**
 * Ambient 3D Spatial Backdrop with drifting depth spheres & soft rim lighting.
 */
@Composable
fun TakeIt3DBackdrop(
    accentPrimary: Color,
    accentSecondary: Color,
    darkMode: Boolean = false,
    baseColor: Color = if (darkMode) Color(0xFF0A0F1E) else Color(0xFFF6F7FB),
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val inf = rememberInfiniteTransition(label = "backdrop3D")
    val drift by inf.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(6500, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "orbDrift"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(baseColor)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // Top-right 3D glowing sphere
            val orb1Center = Offset(w * 0.88f + drift * 22f, h * 0.14f - drift * 14f)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        accentPrimary.copy(alpha = if (darkMode) 0.24f else 0.14f),
                        accentPrimary.copy(alpha = if (darkMode) 0.06f else 0.03f),
                        Color.Transparent
                    ),
                    center = orb1Center,
                    radius = w * 0.55f
                ),
                radius = w * 0.55f,
                center = orb1Center
            )

            // Mid-left secondary sphere
            val orb2Center = Offset(w * 0.10f - drift * 18f, h * 0.48f + drift * 24f)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        accentSecondary.copy(alpha = if (darkMode) 0.20f else 0.12f),
                        accentSecondary.copy(alpha = if (darkMode) 0.04f else 0.02f),
                        Color.Transparent
                    ),
                    center = orb2Center,
                    radius = w * 0.50f
                ),
                radius = w * 0.50f,
                center = orb2Center
            )

            // Bottom subtle glow
            val orb3Center = Offset(w * 0.72f, h * 0.88f - drift * 16f)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        accentPrimary.copy(alpha = if (darkMode) 0.15f else 0.09f),
                        Color.Transparent
                    ),
                    center = orb3Center,
                    radius = w * 0.48f
                ),
                radius = w * 0.48f,
                center = orb3Center
            )
        }
        content()
    }
}

/**
 * Tactile 3D Extruded Card:
 * - Renders an offset physical 3D base edge (`depthColor`) underneath the main face.
 * - Top specular sheen rim + multi-layer drop shadow.
 * - Spring press-down physics when `onClick != null` so tapping feels physical.
 */
@Composable
fun TakeIt3DCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    containerColor: Color = Color.White,
    depthColor: Color = Color(0xFFD5DAE3),
    borderColor: Color? = Color.White.copy(alpha = 0.7f),
    cornerRadius: Dp = 22.dp,
    depth: Dp = 5.dp,
    shadowElevation: Dp = 8.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()

    val pressOffset by animateDpAsState(
        targetValue = if (pressed && onClick != null) (depth - 1.dp).coerceAtLeast(0.dp) else 0.dp,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioNoBouncy),
        label = "card3DPress"
    )
    val pressScale by animateFloatAsState(
        targetValue = if (pressed && onClick != null) 0.985f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "card3DScale"
    )

    val shape = RoundedCornerShape(cornerRadius)
    val resolvedBorder = borderColor ?: containerColor.lighten3D(0.25f)

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
            }
            .padding(bottom = depth)
    ) {
        // 3D Bottom Extrusion Base Slab
        Box(
            modifier = Modifier
                .matchParentSize()
                .offset(y = depth)
                .shadow(shadowElevation, shape, clip = false)
                .clip(shape)
                .background(depthColor)
        )

        // Elevated Top Face with Specular Top Rim
        Surface(
            shape = shape,
            color = containerColor,
            border = BorderStroke(1.dp, resolvedBorder),
            modifier = Modifier
                .fillMaxWidth()
                .offset(y = pressOffset)
                .then(
                    if (onClick != null) {
                        Modifier.clickable(
                            interactionSource = interactionSource,
                            indication = ripple(),
                            onClick = onClick
                        )
                    } else Modifier
                )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.White.copy(alpha = if (containerColor.red < 0.25f) 0.08f else 0.55f),
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.03f)
                            )
                        )
                    )
            ) {
                Column(modifier = Modifier.fillMaxWidth(), content = content)
            }
        }
    }
}

/**
 * Physical 3D Extruded Push Button with spring depression into its 3D base slab.
 */
@Composable
fun TakeIt3DButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color,
    depthColor: Color = containerColor.darken3D(0.38f),
    contentColor: Color = Color.White,
    enabled: Boolean = true,
    cornerRadius: Dp = 18.dp,
    depth: Dp = 5.dp,
    height: Dp = 54.dp,
    content: @Composable RowScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()

    val offset by animateDpAsState(
        targetValue = if (pressed && enabled) depth else 0.dp,
        animationSpec = spring(stiffness = Spring.StiffnessHigh, dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "btn3DOffset"
    )

    val faceColor = if (enabled) containerColor else containerColor.copy(alpha = 0.45f)
    val baseColor = if (enabled) depthColor else depthColor.copy(alpha = 0.35f)
    val shape = RoundedCornerShape(cornerRadius)

    Box(
        modifier = modifier
            .height(height + depth)
            .padding(bottom = depth)
    ) {
        // 3D Base Slab
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(height)
                .offset(y = depth)
                .shadow(if (enabled) 8.dp else 2.dp, shape, clip = false)
                .clip(shape)
                .background(baseColor)
        )

        // Top Glossy Button Cap
        Surface(
            shape = shape,
            color = faceColor,
            contentColor = contentColor,
            border = BorderStroke(1.dp, faceColor.lighten3D(0.32f)),
            modifier = Modifier
                .fillMaxWidth()
                .height(height)
                .offset(y = offset)
                .clickable(
                    enabled = enabled,
                    interactionSource = interactionSource,
                    indication = ripple(color = contentColor),
                    onClick = onClick
                )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.White.copy(alpha = 0.24f),
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.12f)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    content = content
                )
            }
        }
    }
}

/**
 * Perspective 3D Floating Hero Viewport with subtle camera tilt pendulum & diagonal glossy sheen.
 */
@Composable
fun TakeIt3DHeroBanner(
    modifier: Modifier = Modifier,
    borderColor: Color,
    depthColor: Color,
    cornerRadius: Dp = 26.dp,
    depth: Dp = 6.dp,
    tiltAmplitude: Float = 2.6f,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val inf = rememberInfiniteTransition(label = "hero3DTilt")
    val tiltY by inf.animateFloat(
        initialValue = -tiltAmplitude,
        targetValue = tiltAmplitude,
        animationSpec = infiniteRepeatable(tween(3800, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "tiltY"
    )
    val tiltX by inf.animateFloat(
        initialValue = 1.6f,
        targetValue = -1.6f,
        animationSpec = infiniteRepeatable(tween(4600, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "tiltX"
    )
    val shimmerShift by inf.animateFloat(
        initialValue = -0.5f,
        targetValue = 1.6f,
        animationSpec = infiniteRepeatable(tween(3600, easing = LinearEasing), RepeatMode.Restart),
        label = "shimmer3D"
    )

    val shape = RoundedCornerShape(cornerRadius)

    Box(
        modifier = modifier
            .graphicsLayer {
                rotationY = tiltY
                rotationX = tiltX
                cameraDistance = 16f * density
            }
            .padding(bottom = depth)
    ) {
        // 3D Extruded Pedestal
        Box(
            modifier = Modifier
                .matchParentSize()
                .offset(y = depth)
                .shadow(16.dp, shape, clip = false)
                .clip(shape)
                .background(depthColor)
        )

        // Hero Content Container + Diagonal Specular Glass Sweep
        Surface(
            shape = shape,
            border = BorderStroke(2.dp, borderColor),
            modifier = Modifier
                .fillMaxSize()
                .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                content()
                // Specular diagonal glass sweep overlay
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val startX = size.width * shimmerShift
                    drawLine(
                        brush = Brush.linearGradient(
                            listOf(
                                Color.Transparent,
                                Color.White.copy(alpha = 0.16f),
                                Color.Transparent
                            ),
                            start = Offset(startX - 80f, 0f),
                            end = Offset(startX + 80f, size.height)
                        ),
                        start = Offset(startX, -20f),
                        end = Offset(startX - 160f, size.height + 20f),
                        strokeWidth = 90f
                    )
                }
                // Top specular inner bevel line
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .align(Alignment.TopCenter)
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color.Transparent, Color.White.copy(alpha = 0.65f), Color.Transparent)
                            )
                        )
                )
            }
        }
    }
}

/**
 * Spherical / Squircle 3D Glossy Icon Orb with specular top highlight & extrusion base.
 */
@Composable
fun TakeIt3DIconOrb(
    icon: ImageVector,
    containerColor: Color,
    depthColor: Color = containerColor.darken3D(0.38f),
    iconTint: Color = Color.White,
    size: Dp = 44.dp,
    shape: Shape = CircleShape,
    onClick: (() -> Unit)? = null,
    contentDescription: String? = null,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val offset by animateDpAsState(
        targetValue = if (pressed && onClick != null) 3.dp else 0.dp,
        animationSpec = spring(stiffness = Spring.StiffnessHigh),
        label = "orbPress"
    )

    Box(
        modifier = modifier
            .size(width = size, height = size + 3.dp)
            .padding(bottom = 3.dp)
    ) {
        Box(
            modifier = Modifier
                .size(size)
                .offset(y = 3.dp)
                .shadow(6.dp, shape, clip = false)
                .clip(shape)
                .background(depthColor)
        )
        Box(
            modifier = Modifier
                .size(size)
                .offset(y = offset)
                .clip(shape)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            containerColor.lighten3D(0.22f),
                            containerColor,
                            containerColor.darken3D(0.10f)
                        )
                    )
                )
                .border(1.dp, Color.White.copy(alpha = 0.45f), shape)
                .then(
                    if (onClick != null) {
                        Modifier.clickable(
                            interactionSource = interactionSource,
                            indication = ripple(bounded = true),
                            onClick = onClick
                        )
                    } else Modifier
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = contentDescription, tint = iconTint, modifier = Modifier.size(size * 0.48f))
        }
    }
}

data class Nav3DTabItem(
    val id: String,
    val label: String,
    val icon: ImageVector,
    val selected: Boolean,
    val testTag: String,
    val onClick: () -> Unit
)

/**
 * Floating 3D Capsule Bottom Navigation Dock with glowing active pill & inset clearance.
 */
@Composable
fun TakeIt3DFloatingNavBar(
    items: List<Nav3DTabItem>,
    containerColor: Color = Color.White,
    activePodColor: Color,
    activeContentColor: Color,
    inactiveColor: Color = Color(0xFF6B7280),
    borderColor: Color = Color(0xFFE5E7EB),
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        // 3D Bottom Slab Shadow
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(66.dp)
                .offset(y = 4.dp)
                .shadow(16.dp, RoundedCornerShape(24.dp), clip = false)
                .clip(RoundedCornerShape(24.dp))
                .background(containerColor.darken3D(0.18f))
        )

        Surface(
            shape = RoundedCornerShape(24.dp),
            color = containerColor,
            border = BorderStroke(1.dp, borderColor),
            modifier = Modifier
                .fillMaxWidth()
                .height(66.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 6.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                items.forEach { item ->
                    val podBg by animateColorAsState(
                        targetValue = if (item.selected) activePodColor else Color.Transparent,
                        animationSpec = tween(220),
                        label = "navPodBg"
                    )
                    val tint by animateColorAsState(
                        targetValue = if (item.selected) activeContentColor else inactiveColor,
                        animationSpec = tween(220),
                        label = "navPodTint"
                    )
                    val scale by animateFloatAsState(
                        targetValue = if (item.selected) 1.04f else 0.96f,
                        animationSpec = spring(stiffness = Spring.StiffnessMedium),
                        label = "navPodScale"
                    )

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .graphicsLayer {
                                scaleX = scale
                                scaleY = scale
                            }
                            .clip(RoundedCornerShape(18.dp))
                            .background(
                                if (item.selected) {
                                    Brush.verticalGradient(listOf(podBg.lighten3D(0.15f), podBg))
                                } else {
                                    Brush.verticalGradient(listOf(Color.Transparent, Color.Transparent))
                                }
                            )
                            .then(
                                if (item.selected) {
                                    Modifier.border(1.dp, Color.White.copy(alpha = 0.35f), RoundedCornerShape(18.dp))
                                } else Modifier
                            )
                            .clickable(onClick = item.onClick)
                            .testTag(item.testTag),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = item.label,
                                tint = tint,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = item.label,
                                color = tint,
                                fontSize = 11.sp,
                                fontWeight = if (item.selected) FontWeight.ExtraBold else FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}
