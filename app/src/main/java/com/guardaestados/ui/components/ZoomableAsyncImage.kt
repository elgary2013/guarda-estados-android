package com.guardaestados.ui.components

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import kotlin.math.abs
import kotlin.math.max

private const val NormalScale = 1f
private const val DoubleTapScale = 2.5f
private const val MaxScale = 4f
private const val ZoomedScaleThreshold = 1.01f

@Composable
fun ZoomableAsyncImage(
    model: ImageRequest,
    contentDescription: String,
    resetKey: Any,
    onZoomChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit,
    onError: () -> Unit = {}
) {
    var containerSize by remember { mutableStateOf(IntSize.Zero) }
    var scale by remember(resetKey) { mutableStateOf(NormalScale) }
    var offset by remember(resetKey) { mutableStateOf(Offset.Zero) }

    LaunchedEffect(resetKey) {
        onZoomChanged(false)
    }

    LaunchedEffect(scale) {
        onZoomChanged(scale > ZoomedScaleThreshold)
    }

    AsyncImage(
        model = model,
        contentDescription = contentDescription,
        contentScale = contentScale,
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged { size ->
                containerSize = size
                offset = offset.coerceWithin(containerSize, scale)
            }
            .pointerInput(resetKey, containerSize) {
                detectTapGestures(
                    onDoubleTap = { tapOffset ->
                        if (scale > ZoomedScaleThreshold) {
                            scale = NormalScale
                            offset = Offset.Zero
                        } else {
                            scale = DoubleTapScale
                            offset = (centerOffset(containerSize) - tapOffset)
                                .times(DoubleTapScale - NormalScale)
                                .coerceWithin(containerSize, DoubleTapScale)
                        }
                    }
                )
            }
            .pointerInput(resetKey, containerSize) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    var pastTouchSlop = false
                    var accumulatedZoom = 1f
                    var accumulatedPan = Offset.Zero

                    do {
                        val event = awaitPointerEvent()
                        val pressedPointerCount = event.changes.count { it.pressed }
                        if (pressedPointerCount == 0) {
                            break
                        }

                        val zoomChange = if (pressedPointerCount > 1) event.calculateZoom() else NormalScale
                        val panChange = event.calculatePan()
                        val shouldHandleGesture = scale > ZoomedScaleThreshold ||
                            pressedPointerCount > 1 ||
                            zoomChange != NormalScale

                        if (shouldHandleGesture) {
                            if (!pastTouchSlop) {
                                accumulatedZoom *= zoomChange
                                accumulatedPan += panChange

                                val zoomMotion = abs(NormalScale - accumulatedZoom) *
                                    minOf(containerSize.width, containerSize.height)
                                val panMotion = accumulatedPan.getDistance()
                                pastTouchSlop = zoomMotion > viewConfiguration.touchSlop ||
                                    panMotion > viewConfiguration.touchSlop
                            }

                            if (pastTouchSlop) {
                                val newScale = (scale * zoomChange).coerceIn(NormalScale, MaxScale)
                                scale = newScale
                                offset = if (newScale <= ZoomedScaleThreshold) {
                                    Offset.Zero
                                } else {
                                    (offset + panChange).coerceWithin(containerSize, newScale)
                                }
                                event.changes.forEach { change ->
                                    if (change.positionChanged()) {
                                        change.consume()
                                    }
                                }
                            }
                        }
                    } while (event.changes.any { it.pressed })
                }
                    }
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                translationX = offset.x
                translationY = offset.y
            },
        onError = { onError() }
    )
}

private fun centerOffset(size: IntSize): Offset {
    return Offset(size.width / 2f, size.height / 2f)
}

private fun Offset.coerceWithin(size: IntSize, scale: Float): Offset {
    if (size.width <= 0 || size.height <= 0 || scale <= ZoomedScaleThreshold) {
        return Offset.Zero
    }
    val maxX = max(0f, size.width * (scale - NormalScale) / 2f)
    val maxY = max(0f, size.height * (scale - NormalScale) / 2f)
    return Offset(
        x = x.coerceIn(-maxX, maxX),
        y = y.coerceIn(-maxY, maxY)
    )
}
