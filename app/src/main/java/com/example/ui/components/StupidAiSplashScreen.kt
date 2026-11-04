package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R

@Composable
fun StupidAiSplashScreen(
    modifier: Modifier = Modifier
) {
    val scale = remember { Animatable(0.85f) }
    val alpha = remember { Animatable(0f) }

    // Shimmer sweep animation across the logo silhouette
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer_transition")
    val shimmerTranslateAnim by infiniteTransition.animateFloat(
        initialValue = -300f,
        targetValue = 900f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_anim"
    )

    // Pulse glow animation around the logo container
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_anim"
    )

    LaunchedEffect(Unit) {
        scale.animateTo(
            targetValue = 1.0f,
            animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing)
        )
    }

    LaunchedEffect(Unit) {
        alpha.animateTo(
            targetValue = 1.0f,
            animationSpec = tween(durationMillis = 500)
        )
    }

    // Shimmer Brush gradient
    val shimmerColors = listOf(
        Color.Transparent,
        Color.White.copy(alpha = 0.25f),
        Color(0xFF818CF8).copy(alpha = 0.6f),
        Color.White.copy(alpha = 0.8f),
        Color(0xFFC084FC).copy(alpha = 0.6f),
        Color.White.copy(alpha = 0.25f),
        Color.Transparent
    )

    val shimmerBrush = Brush.linearGradient(
        colors = shimmerColors,
        start = Offset(shimmerTranslateAnim, shimmerTranslateAnim),
        end = Offset(shimmerTranslateAnim + 300f, shimmerTranslateAnim + 300f)
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0F172A),
                        Color(0xFF0B132B),
                        Color(0xFF020617)
                    )
                )
            )
            .testTag("splash_screen_container"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(32.dp)
        ) {
            // Main Logo Box with Shimmer & Glowing Outline
            Box(
                modifier = Modifier
                    .scale(scale.value)
                    .alpha(alpha.value)
                    .size(200.dp)
                    .clip(RoundedCornerShape(40.dp))
                    .border(
                        width = 2.dp,
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color(0xFF818CF8).copy(alpha = glowAlpha),
                                Color(0xFFC084FC).copy(alpha = glowAlpha),
                                Color(0xFF38BDF8).copy(alpha = glowAlpha)
                            )
                        ),
                        shape = RoundedCornerShape(40.dp)
                    )
                    .background(Color(0xFF0A0F1D)),
                contentAlignment = Alignment.Center
            ) {
                // Background zoomed app logo image
                Image(
                    painter = painterResource(id = R.drawable.img_app_logo_zoomed),
                    contentDescription = "STUPID AI Logo",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                // Shimmer Overlay across logo silhouette
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(shimmerBrush)
                )
            }

            Spacer(modifier = Modifier.height(36.dp))

            // App Name
            Text(
                text = "STUPID AI",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 3.sp
                ),
                color = Color.White,
                modifier = Modifier.alpha(alpha.value)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Next-Gen Artificial Intelligence",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF94A3B8),
                modifier = Modifier.alpha(alpha.value)
            )

            Spacer(modifier = Modifier.height(44.dp))

            // Glowing Animated Loading Bar
            Box(
                modifier = Modifier
                    .alpha(alpha.value)
                    .width(160.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF1E293B))
            ) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape),
                    color = Color(0xFF818CF8),
                    trackColor = Color(0xFF1E293B)
                )
            }
        }
    }
}
