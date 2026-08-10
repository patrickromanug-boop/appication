package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AccentOrange
import com.example.ui.theme.PrimaryBlue

@Composable
fun LSLogoLockup(
    modifier: Modifier = Modifier,
    logoSize: Float = 26f,
    showPill: Boolean = true
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // "L" (in Blue)
        Text(
            text = "L",
            color = PrimaryBlue,
            fontSize = logoSize.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.SansSerif
        )
        // "S" (in Orange)
        Text(
            text = "S",
            color = AccentOrange,
            fontSize = logoSize.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.SansSerif,
            modifier = Modifier.padding(end = 6.dp)
        )

        if (showPill) {
            // "Services" small pill
            Box(
                modifier = Modifier
                    .background(
                        color = PrimaryBlue,
                        shape = RoundedCornerShape(100)
                    )
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "Services",
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontSize = (logoSize * 0.45).sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.SansSerif
                )
            }
        }
    }
}
