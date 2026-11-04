package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Custom error page view displayed when the WebView fails to load
 * due to network connectivity or server issues, featuring a prominent 'Retry' button.
 */
@Composable
fun StupidAiErrorScreen(
    errorMessage: String,
    onRetryClick: () -> Unit,
    isDarkTheme: Boolean = true,
    modifier: Modifier = Modifier
) {
    val bgColor = if (isDarkTheme) Color(0xFF0F172A) else Color(0xFFF8FAFC)
    val cardBg = if (isDarkTheme) Color(0xFF1E293B) else Color(0xFFFFFFFF)
    val textColor = if (isDarkTheme) Color(0xFFF8FAFC) else Color(0xFF0F172A)
    val subTextColor = if (isDarkTheme) Color(0xFF94A3B8) else Color(0xFF64748B)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(bgColor)
            .padding(24.dp)
            .testTag("network_error_screen"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .background(cardBg)
                .padding(horizontal = 24.dp, vertical = 36.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFEF4444).copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.WifiOff,
                    contentDescription = "Connection Failed",
                    tint = Color(0xFFEF4444),
                    modifier = Modifier.size(40.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Connection Failed",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp
                ),
                color = textColor,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "STUPID AI could not load due to a network connection issue. Your saved conversations and session remain intact.",
                style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 20.sp),
                color = subTextColor,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 12.dp)
            )

            if (errorMessage.isNotBlank() && !errorMessage.contains("Connection Failed", ignoreCase = true)) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = errorMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = subTextColor.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            Button(
                onClick = onRetryClick,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF6366F1), // Indigo accent
                    contentColor = Color.White
                ),
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .height(50.dp)
                    .testTag("retry_button")
            ) {
                Text(
                    text = "Retry",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                )
            }
        }
    }
}
