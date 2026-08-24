package com.guardaestados.ui.theme

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.guardaestados.data.settings.IncludedHomeBackground

@Composable
fun EstadoGoIncludedHomeBackground(
    background: IncludedHomeBackground,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(background.baseBrush())
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            when (background) {
                IncludedHomeBackground.AuraGreen -> {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color(0xB01BCB7A), Color.Transparent),
                            center = Offset(size.width * 0.26f, size.height * 0.24f),
                            radius = size.minDimension * 0.72f
                        ),
                        radius = size.minDimension * 0.72f,
                        center = Offset(size.width * 0.26f, size.height * 0.24f)
                    )
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color(0x7034E3A1), Color.Transparent),
                            center = Offset(size.width * 0.82f, size.height * 0.72f),
                            radius = size.minDimension * 0.56f
                        ),
                        radius = size.minDimension * 0.56f,
                        center = Offset(size.width * 0.82f, size.height * 0.72f)
                    )
                }

                IncludedHomeBackground.EmeraldWaves -> {
                    repeat(5) { index ->
                        val y = size.height * (0.22f + index * 0.15f)
                        val path = Path().apply {
                            moveTo(-size.width * 0.08f, y)
                            cubicTo(
                                size.width * 0.24f,
                                y - size.height * 0.12f,
                                size.width * 0.58f,
                                y + size.height * 0.12f,
                                size.width * 1.08f,
                                y - size.height * 0.04f
                            )
                        }
                        drawPath(
                            path = path,
                            color = Color(0xFF2CE8A0).copy(alpha = 0.18f - index * 0.018f),
                            style = Stroke(width = (18 - index * 2).dp.toPx(), cap = StrokeCap.Round)
                        )
                    }
                }

                IncludedHomeBackground.LuminousNight -> {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color(0x8848F2B9), Color.Transparent),
                            center = Offset(size.width * 0.72f, size.height * 0.18f),
                            radius = size.minDimension * 0.32f
                        ),
                        radius = size.minDimension * 0.32f,
                        center = Offset(size.width * 0.72f, size.height * 0.18f)
                    )
                    drawArc(
                        color = Color(0xFF6BFFD0).copy(alpha = 0.24f),
                        startAngle = 205f,
                        sweepAngle = 86f,
                        useCenter = false,
                        topLeft = Offset(size.width * 0.18f, size.height * 0.34f),
                        size = Size(size.width * 0.78f, size.height * 0.42f),
                        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                    )
                    drawCircle(
                        color = Color.White.copy(alpha = 0.18f),
                        radius = 2.dp.toPx(),
                        center = Offset(size.width * 0.34f, size.height * 0.28f)
                    )
                    drawCircle(
                        color = Color.White.copy(alpha = 0.14f),
                        radius = 1.5.dp.toPx(),
                        center = Offset(size.width * 0.62f, size.height * 0.42f)
                    )
                }
            }
        }
    }
}

private fun IncludedHomeBackground.baseBrush(): Brush {
    return when (this) {
        IncludedHomeBackground.AuraGreen -> Brush.verticalGradient(
            colors = listOf(Color(0xFF031316), Color(0xFF06251D), Color(0xFF020B0D))
        )
        IncludedHomeBackground.EmeraldWaves -> Brush.linearGradient(
            colors = listOf(Color(0xFF041113), Color(0xFF08261F), Color(0xFF031012))
        )
        IncludedHomeBackground.LuminousNight -> Brush.verticalGradient(
            colors = listOf(Color(0xFF020817), Color(0xFF07191E), Color(0xFF010407))
        )
    }
}
