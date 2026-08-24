package com.guardaestados.ui.video

import android.net.Uri
import android.os.Looper
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.PlayerMessage
import androidx.media3.ui.PlayerView
import com.guardaestados.R
import kotlinx.coroutines.delay

@OptIn(UnstableApi::class)
@Composable
fun VideoPlayerPreview(
    uri: Uri,
    modifier: Modifier = Modifier,
    previewStartMs: Long? = null,
    previewStopMs: Long? = null,
    playbackRequestKey: Int = 0,
    autoPlayOnRequest: Boolean = false,
    showPlaybackStatus: Boolean = false,
    errorMessage: String? = null,
    immersiveControls: Boolean = false,
    onSeekInteractionChanged: (Boolean) -> Unit = {}
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val safePreviewStartMs = previewStartMs?.coerceAtLeast(0L)
    val safePreviewStopMs = previewStopMs?.takeIf { endMs ->
        val startMs = safePreviewStartMs ?: 0L
        endMs > startMs
    }
    var isLoading by remember(uri) { mutableStateOf(true) }
    var hasError by remember(uri) { mutableStateOf(false) }
    var isPlaying by remember(uri) { mutableStateOf(false) }
    var positionMs by remember(uri) { mutableStateOf(0L) }
    var durationMs by remember(uri) { mutableStateOf(0L) }
    var pendingSeekMs by remember(uri) { mutableStateOf<Long?>(null) }
    val player = remember(uri) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(uri))
            playWhenReady = false
            prepare()
        }
    }

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                isLoading = playbackState == Player.STATE_BUFFERING || playbackState == Player.STATE_IDLE
                if (playbackState == Player.STATE_READY || playbackState == Player.STATE_ENDED) {
                    isLoading = false
                }
                if (playbackState == Player.STATE_ENDED) {
                    player.pause()
                    isPlaying = false
                }
                durationMs = player.duration.takeIf { it != C.TIME_UNSET }?.coerceAtLeast(0L) ?: 0L
                positionMs = player.currentPosition.coerceAtLeast(0L)
            }

            override fun onIsPlayingChanged(isPlayingValue: Boolean) {
                isPlaying = isPlayingValue
            }

            override fun onPlayerError(error: PlaybackException) {
                hasError = true
                isLoading = false
                isPlaying = false
            }
        }
        player.addListener(listener)
        onDispose {
            player.removeListener(listener)
            player.release()
        }
    }

    DisposableEffect(lifecycleOwner, player) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE || event == Lifecycle.Event.ON_STOP) {
                player.pause()
                isPlaying = false
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(player, playbackRequestKey, autoPlayOnRequest) {
        if (playbackRequestKey <= 0) return@LaunchedEffect
        player.seekTo(safePreviewStartMs ?: 0L)
        if (autoPlayOnRequest) {
            player.play()
        } else {
            player.pause()
        }
        isPlaying = player.isPlaying
    }

    DisposableEffect(player, playbackRequestKey, safePreviewStopMs) {
        val stopMessage = if (playbackRequestKey > 0 && safePreviewStopMs != null) {
            player.createMessage(
            PlayerMessage.Target { _, _ ->
                player.pause()
            }
        )
                .setLooper(Looper.getMainLooper())
                .setPosition(safePreviewStopMs)
                .setDeleteAfterDelivery(true)
                .send()
        } else {
            null
        }
        onDispose {
            stopMessage?.let { message -> runCatching { message.cancel() } }
        }
    }

    LaunchedEffect(player) {
        while (true) {
            if (player.duration != C.TIME_UNSET) {
                durationMs = player.duration.coerceAtLeast(0L)
            }
            positionMs = player.currentPosition.coerceAtLeast(0L)
            delay(250)
        }
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { viewContext ->
                PlayerView(viewContext).apply {
                    this.player = player
                    useController = !immersiveControls
                    setOnTouchListener { _, _ -> false }
                    setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
                    setShutterBackgroundColor(android.graphics.Color.BLACK)
                }
            },
            update = { playerView ->
                playerView.player = player
                playerView.useController = !immersiveControls
                playerView.setOnTouchListener { _, _ -> false }
            }
        )

        if (showPlaybackStatus && isLoading && !hasError) {
            CircularProgressIndicator(color = Color.White.copy(alpha = 0.86f))
        }

        if (showPlaybackStatus && hasError) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.72f))
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = errorMessage.orEmpty(),
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
            }
        }

        if (immersiveControls && !hasError) {
            ImmersiveVideoControls(
                isPlaying = isPlaying,
                isLoading = isLoading,
                positionMs = pendingSeekMs ?: positionMs,
                durationMs = durationMs,
                onTogglePlayback = {
                    if (player.playbackState == Player.STATE_ENDED) {
                        player.seekTo(0L)
                    }
                    if (player.isPlaying) {
                        player.pause()
                    } else {
                        player.play()
                    }
                    isPlaying = player.isPlaying
                },
                onSeekChange = { value ->
                    onSeekInteractionChanged(true)
                    pendingSeekMs = value.toLong()
                },
                onSeekFinished = {
                    pendingSeekMs?.let { seekMs -> player.seekTo(seekMs) }
                    pendingSeekMs = null
                    onSeekInteractionChanged(false)
                },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
private fun ImmersiveVideoControls(
    isPlaying: Boolean,
    isLoading: Boolean,
    positionMs: Long,
    durationMs: Long,
    onTogglePlayback: () -> Unit,
    onSeekChange: (Float) -> Unit,
    onSeekFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(enabled = !isLoading && isPlaying, onClick = onTogglePlayback)
        )

        if (!isLoading && !isPlaying) {
            Surface(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(84.dp)
                    .clickable(onClick = onTogglePlayback),
                shape = RoundedCornerShape(999.dp),
                color = Color.Black.copy(alpha = 0.46f),
                contentColor = Color.White
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        painter = painterResource(R.drawable.ic_play_arrow),
                        contentDescription = stringResource(R.string.preview_video_play),
                        modifier = Modifier.size(46.dp),
                        tint = Color.White
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.52f))
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val progressDescription = stringResource(R.string.preview_video_progress)
            Slider(
                value = positionMs.coerceIn(0L, durationMs.coerceAtLeast(0L)).toFloat(),
                onValueChange = onSeekChange,
                valueRange = 0f..durationMs.coerceAtLeast(1L).toFloat(),
                onValueChangeFinished = onSeekFinished,
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = progressDescription },
                enabled = durationMs > 0L
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(
                        R.string.preview_video_time,
                        positionMs.formatPlaybackTime(),
                        durationMs.formatPlaybackTime()
                    ),
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(alpha = 0.88f),
                    maxLines = 1
                )
                IconButton(onClick = onTogglePlayback, enabled = !isLoading) {
                    Icon(
                        painter = painterResource(
                            if (isPlaying) {
                                R.drawable.ic_pause
                            } else {
                                R.drawable.ic_play_arrow
                            }
                        ),
                        contentDescription = stringResource(
                            if (isPlaying) {
                                R.string.preview_video_pause
                            } else {
                                R.string.preview_video_play
                            }
                        ),
                        modifier = Modifier.size(30.dp),
                        tint = Color.White
                    )
                }
            }
        }
    }
}

private fun Long.formatPlaybackTime(): String {
    val totalSeconds = (this / 1000L).coerceAtLeast(0L)
    val minutes = totalSeconds / 60L
    val seconds = totalSeconds % 60L
    return "%d:%02d".format(minutes, seconds)
}
