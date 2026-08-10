package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.AppViewModel
import com.example.ui.OfflineBannerState
import com.example.ui.theme.PrimaryBlue

@Composable
fun OfflineBanner(
    viewModel: AppViewModel,
    modifier: Modifier = Modifier
) {
    val bannerState by viewModel.offlineBannerState.collectAsState()
    val lastCacheTime by viewModel.lastCacheFormattedTime.collectAsState()

    AnimatedVisibility(
        visible = bannerState != OfflineBannerState.HIDDEN,
        enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
        modifier = modifier
    ) {
        val backgroundColor = if (bannerState == OfflineBannerState.BACK_ONLINE) {
            PrimaryBlue
        } else {
            Color(0xFF2D3748) // Neutral dark grey
        }

        val text = if (bannerState == OfflineBannerState.BACK_ONLINE) {
            "Back online — updating feed..."
        } else {
            "You're offline — showing recent jobs (Updated $lastCacheTime)"
        }

        val icon = if (bannerState == OfflineBannerState.BACK_ONLINE) {
            Icons.Default.Wifi
        } else {
            Icons.Default.WifiOff
        }

        Surface(
            color = backgroundColor,
            contentColor = Color.White,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("offline_banner_surface")
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = if (bannerState == OfflineBannerState.BACK_ONLINE) "Online" else "Offline",
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = text,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
            }
        }
    }
}
