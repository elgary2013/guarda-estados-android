package com.guardaestados.domain.status

import com.guardaestados.data.folder.FolderSelectionState
import com.guardaestados.data.status.StatusImageRepository

class LoadStatusImagesUseCase(
    private val repository: StatusImageRepository
) {
    fun execute(folderSelectionState: FolderSelectionState): StatusGalleryState {
        return when (folderSelectionState) {
            FolderSelectionState.Loading -> StatusGalleryState.Loading
            FolderSelectionState.NotSelected -> StatusGalleryState.NoFolderSelected
            is FolderSelectionState.PermissionLost -> StatusGalleryState.PermissionLost
            is FolderSelectionState.Selected -> repository.loadImages(folderSelectionState.uriString)
                .fold(
                    onSuccess = { images ->
                        if (images.isEmpty()) {
                            StatusGalleryState.Empty
                        } else {
                            StatusGalleryState.Content(images)
                        }
                    },
                    onFailure = { StatusGalleryState.RecoverableError }
                )
        }
    }
}

sealed interface StatusGalleryState {
    data object Loading : StatusGalleryState
    data object NoFolderSelected : StatusGalleryState
    data object PermissionLost : StatusGalleryState
    data object Empty : StatusGalleryState
    data object RecoverableError : StatusGalleryState
    data class Content(val images: List<StatusImage>) : StatusGalleryState
}
