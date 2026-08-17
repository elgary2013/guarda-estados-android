package com.guardaestados.ui.screens

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.guardaestados.R
import com.guardaestados.ui.theme.BrandGlassCard
import com.guardaestados.ui.theme.LocalGuardaEstadosColors

@Composable
fun HomeScreen(
    homeBackgroundUri: String?,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    modifier: Modifier = Modifier
) {
    val appColors = LocalGuardaEstadosColors.current
    val backgroundImageUri = remember(homeBackgroundUri) { homeBackgroundUri?.let(Uri::parse) }
    val hasBackgroundImage = backgroundImageUri != null

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (hasBackgroundImage) {
                AsyncImage(
                    model = backgroundImageUri,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    appColors.background,
                                    appColors.surfaceStrong,
                                    appColors.background
                                )
                            )
                        )
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = if (hasBackgroundImage) {
                                listOf(
                                    Color(0xE8031519),
                                    Color(0xA0062622),
                                    Color(0xF2031519)
                                )
                            } else {
                                listOf(
                                    appColors.background.copy(alpha = 0.9f),
                                    appColors.background.copy(alpha = 0.7f),
                                    appColors.background.copy(alpha = 0.95f)
                                )
                            }
                        )
                    )
            )

            HomeCoverContent(
                glassOnPhoto = hasBackgroundImage,
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .padding(horizontal = 24.dp)
                    .padding(
                        top = 28.dp,
                        bottom = contentPadding.calculateBottomPadding() + 28.dp
                    )
            )
        }
    }
}

@Composable
private fun HomeCoverContent(
    glassOnPhoto: Boolean,
    modifier: Modifier = Modifier
) {
    val titleColor = if (glassOnPhoto) Color.White else MaterialTheme.colorScheme.onBackground
    val bodyColor = if (glassOnPhoto) Color.White.copy(alpha = 0.82f) else MaterialTheme.colorScheme.onSurfaceVariant
    val brandActiveColor = LocalGuardaEstadosColors.current.active

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = buildAnnotatedString {
                    append("Estado")
                    withStyle(SpanStyle(color = brandActiveColor)) {
                        append("Go")
                    }
                },
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = titleColor
            )
            Text(
                text = stringResource(R.string.home_subtitle),
                style = MaterialTheme.typography.titleMedium,
                color = bodyColor
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        BrandGlassCard(
            contentPadding = PaddingValues(18.dp),
            shape = RoundedCornerShape(8.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.Top
            ) {
                Surface(
                    modifier = Modifier.size(48.dp),
                    shape = RoundedCornerShape(8.dp),
                    color = LocalGuardaEstadosColors.current.surfaceSoft,
                    contentColor = LocalGuardaEstadosColors.current.active
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            painter = painterResource(R.drawable.ic_image),
                            contentDescription = null,
                            modifier = Modifier.size(26.dp),
                            tint = LocalGuardaEstadosColors.current.active
                        )
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = stringResource(R.string.home_device_states_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = titleColor
                    )
                    Text(
                        text = stringResource(R.string.home_device_states_body),
                        style = MaterialTheme.typography.bodyMedium,
                        color = bodyColor
                    )
                }
            }
        }
    }
}
