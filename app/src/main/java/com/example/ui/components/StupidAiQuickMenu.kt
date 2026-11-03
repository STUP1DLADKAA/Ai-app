package com.example.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StupidAiQuickMenu(
    sheetState: SheetState,
    isDarkModeOverride: Boolean?,
    onDismissRequest: () -> Unit,
    onReloadClick: () -> Unit,
    onShareClick: () -> Unit,
    onClearCacheClick: () -> Unit,
    onToggleDarkModeClick: () -> Unit,
    onAboutClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = modifier.testTag("quick_menu_bottom_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp, top = 8.dp)
        ) {
            Text(
                text = "Quick Actions",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
            )

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 8.dp),
                color = MaterialTheme.colorScheme.outlineVariant
            )

            QuickMenuItem(
                icon = Icons.Default.Refresh,
                title = "Reload Application",
                subtitle = "Refresh page content and connection",
                onClick = {
                    onReloadClick()
                    onDismissRequest()
                },
                testTag = "menu_reload"
            )

            QuickMenuItem(
                icon = Icons.Default.Share,
                title = "Share Web App",
                subtitle = "Share STUPID AI link with others",
                onClick = {
                    onShareClick()
                    onDismissRequest()
                },
                testTag = "menu_share"
            )

            QuickMenuItem(
                icon = Icons.Default.CleaningServices,
                title = "Optimize Cache",
                subtitle = "Clear temporary assets (preserves user login & sessions)",
                onClick = {
                    onClearCacheClick()
                    onDismissRequest()
                },
                testTag = "menu_clear_cache"
            )

            val modeLabel = when (isDarkModeOverride) {
                null -> "System Default"
                true -> "Dark Theme"
                false -> "Light Theme"
            }

            QuickMenuItem(
                icon = Icons.Default.DarkMode,
                title = "Appearance",
                subtitle = "Mode: $modeLabel",
                onClick = onToggleDarkModeClick,
                testTag = "menu_theme_toggle"
            )

            QuickMenuItem(
                icon = Icons.Default.Info,
                title = "About STUPID AI",
                subtitle = "App details, RAM optimization & security",
                onClick = {
                    onAboutClick()
                    onDismissRequest()
                },
                testTag = "menu_about"
            )
        }
    }
}

@Composable
private fun QuickMenuItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    testTag: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 12.dp)
            .testTag(testTag)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(end = 16.dp)
        )
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
