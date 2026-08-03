package com.guardaestados.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.guardaestados.R
import com.guardaestados.domain.status.StatusGalleryState
import com.guardaestados.domain.status.StatusImage
import java.text.DateFormat
import java.util.Date

@Composable
fun StatesScreen(
    statusGalleryState: StatusGalleryState,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = stringResource(R.string.states_title),
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = stringResource(R.string.states_subtitle),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Button(onClick = onRefresh) {
                        Text(text = stringResource(R.string.states_action_refresh))
                    }
                }
            }

            when (statusGalleryState) {
                StatusGalleryState.Loading -> item {
                    GalleryMessageCard(
                        title = stringResource(R.string.states_loading_title),
                        body = stringResource(R.string.states_loading_body)
                    )
                }

                StatusGalleryState.NoFolderSelected -> item {
                    GalleryMessageCard(
                        title = stringResource(R.string.states_no_folder_title),
                        body = stringResource(R.string.states_no_folder_body)
                    )
                }

                StatusGalleryState.PermissionLost -> item {
                    GalleryMessageCard(
                        title = stringResource(R.string.states_permission_lost_title),
                        body = stringResource(R.string.states_permission_lost_body)
                    )
                }

                StatusGalleryState.Empty -> item {
                    GalleryMessageCard(
                        title = stringResource(R.string.states_empty_title),
                        body = stringResource(R.string.states_empty_body)
                    )
                }

                StatusGalleryState.RecoverableError -> item {
                    GalleryMessageCard(
                        title = stringResource(R.string.states_error_title),
                        body = stringResource(R.string.states_error_body)
                    )
                }

                is StatusGalleryState.Content -> {
                    item {
                        GalleryMessageCard(
                            title = stringResource(R.string.states_found_title),
                            body = stringResource(
                                R.string.states_found_body,
                                statusGalleryState.images.size
                            )
                        )
                    }
                    items(
                        items = statusGalleryState.images,
                        key = { image -> image.uri.toString() }
                    ) { image ->
                        StatusImageCard(image = image)
                    }
                }
            }
        }
    }
}

@Composable
private fun GalleryMessageCard(
    title: String,
    body: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun StatusImageCard(image: StatusImage) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = image.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = stringResource(R.string.status_image_mime_type, image.mimeType),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = stringResource(
                    R.string.status_image_size,
                    image.sizeBytes?.toString() ?: stringResource(R.string.status_image_value_unavailable)
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = stringResource(
                    R.string.status_image_date,
                    image.lastModifiedMillis?.formatDate()
                        ?: stringResource(R.string.status_image_value_unavailable)
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = stringResource(R.string.status_image_uri, image.uri.toString()),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun Long.formatDate(): String {
    return DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(this))
}
