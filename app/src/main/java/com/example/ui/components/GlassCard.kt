package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.*

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 20.dp,
    borderWidth: Dp = 1.dp,
    elevation: Dp = 6.dp,
    content: @Composable BoxScope.() -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val shape = RoundedCornerShape(cornerRadius)

    val surfaceColor = if (isDark) GlassDarkSurface else GlassLightSurface
    val borderBrush = if (isDark) {
        Brush.linearGradient(
            colors = listOf(
                TrueBlueLight.copy(alpha = 0.5f),
                Color.White.copy(alpha = 0.1f)
            )
        )
    } else {
        Brush.linearGradient(
            colors = listOf(
                TrueBlue.copy(alpha = 0.3f),
                Color.White.copy(alpha = 0.8f)
            )
        )
    }

    Surface(
        modifier = modifier
            .shadow(elevation, shape, ambientColor = TrueBlue.copy(alpha = 0.15f), spotColor = TrueBlue.copy(alpha = 0.25f))
            .clip(shape)
            .border(BorderStroke(borderWidth, borderBrush), shape),
        color = surfaceColor
    ) {
        Box(
            modifier = Modifier
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            if (isDark) TrueBlue.copy(alpha = 0.08f) else TrueBlueContainer.copy(alpha = 0.15f),
                            Color.Transparent
                        )
                    )
                )
        ) {
            content()
        }
    }
}
