package com.guardaestados.ui.saved

import com.guardaestados.domain.saved.SavedImage
import com.guardaestados.domain.saved.SavedImagesState

class SavedImagePreviewResolver {
    fun resolve(
        savedImagesState: SavedImagesState,
        imageUri: String?
    ): SavedImagePreviewState {
        if (imageUri.isNullOrBlank()) {
            return SavedImagePreviewState.Unavailable
        }

        return when (savedImagesState) {
            SavedImagesState.Loading -> SavedImagePreviewState.Loading
            SavedImagesState.Empty,
            SavedImagesState.RecoverableError -> SavedImagePreviewState.Unavailable

            is SavedImagesState.Content -> savedImagesState.images
                .firstOrNull { image -> image.uri.toString() == imageUri }
                ?.let(SavedImagePreviewState::Content)
                ?: SavedImagePreviewState.Unavailable
        }
    }
}

sealed interface SavedImagePreviewState {
    data object Loading : SavedImagePreviewState
    data object Unavailable : SavedImagePreviewState
    data class Content(val image: SavedImage) : SavedImagePreviewState
}
