package com.guardaestados.ui.status

import com.guardaestados.domain.status.StatusGalleryState
import com.guardaestados.domain.status.StatusImage

class StatusImagePreviewResolver {
    fun resolve(
        galleryState: StatusGalleryState,
        routeImageUri: String?
    ): StatusImagePreviewState {
        if (routeImageUri.isNullOrBlank()) {
            return StatusImagePreviewState.Unavailable
        }

        return when (galleryState) {
            StatusGalleryState.Loading -> StatusImagePreviewState.Loading
            StatusGalleryState.NoFolderSelected -> StatusImagePreviewState.NoFolderSelected
            StatusGalleryState.PermissionLost -> StatusImagePreviewState.PermissionLost
            StatusGalleryState.Empty,
            StatusGalleryState.RecoverableError -> StatusImagePreviewState.Unavailable
            is StatusGalleryState.Content -> galleryState.images
                .firstOrNull { image -> image.uri.toString() == routeImageUri }
                ?.let(StatusImagePreviewState::Content)
                ?: StatusImagePreviewState.Unavailable
        }
    }
}

sealed interface StatusImagePreviewState {
    data object Loading : StatusImagePreviewState
    data object NoFolderSelected : StatusImagePreviewState
    data object PermissionLost : StatusImagePreviewState
    data object Unavailable : StatusImagePreviewState
    data class Content(val image: StatusImage) : StatusImagePreviewState
}
