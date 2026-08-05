package com.guardaestados.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

val PrimaryBrandGradient = Brush.linearGradient(
    colors = listOf(SocialGreen, SocialViolet)
)

val HighlightBrandGradient = Brush.linearGradient(
    colors = listOf(SocialViolet, SocialFuchsia)
)

@Composable
fun BrandGradientButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    highlight: Boolean = false
) {
    val brush = if (highlight) HighlightBrandGradient else PrimaryBrandGradient
    Box(
        modifier = modifier
            .defaultMinSize(minHeight = 48.dp)
            .background(
                brush = brush,
                shape = RoundedCornerShape(8.dp),
                alpha = if (enabled) 1f else 0.42f
            )
            .clickable(
                enabled = enabled,
                role = Role.Button,
                onClick = onClick
            )
            .padding(horizontal = 18.dp, vertical = 12.dp)
            .alpha(if (enabled) 1f else 0.62f),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = SocialTextPrimary
        )
    }
}

fun brandGradientBorder(highlight: Boolean = false): BorderStroke {
    return BorderStroke(
        width = 1.dp,
        brush = if (highlight) HighlightBrandGradient else PrimaryBrandGradient
    )
}
