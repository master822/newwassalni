package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.*

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 16.dp,
    borderWidth: Dp = 1.dp,
    elevation: Dp = 2.dp,
    content: @Composable BoxScope.() -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val shape = RoundedCornerShape(cornerRadius)
    val surfaceColor = if (isDark) DarkSurface else LightSurface
    val borderColor = if (isDark) DarkBorderColor else LightBorderColor

    Surface(
        modifier = modifier
            .shadow(elevation, shape, ambientColor = Color.Black.copy(alpha = 0.04f), spotColor = Color.Black.copy(alpha = 0.08f))
            .clip(shape)
            .border(BorderStroke(borderWidth, borderColor), shape),
        color = surfaceColor
    ) {
        Box {
            content()
        }
    }
}

