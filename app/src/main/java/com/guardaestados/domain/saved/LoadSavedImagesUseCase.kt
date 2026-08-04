package com.guardaestados.domain.saved

import android.content.IntentSender
import android.net.Uri

class LoadSavedImagesUseCase(
    private val repository: SavedImagesRepository,
    private val sorter: SavedImagesSorter = SavedImagesSorter()
) {
    fun execute(): SavedImagesState {
        return repository.loadImages()
            .fold(
                onSuccess = { images ->
                    val sortedImages = sorter.sort(images)
                    if (sortedImages.isEmpty()) SavedImagesState.Empty else SavedImagesState.Content(sortedImages)
                },
                onFailure = { SavedImagesState.RecoverableError }
            )
    }
}

class DeleteSavedImageUseCase(
    private val repository: SavedImagesRepository
) {
    suspend fun execute(image: SavedImage): DeleteSavedImageResult {
        return repository.deleteImage(image.uri)
    }
}

interface SavedImagesRepository {
    fun loadImages(): Result<List<SavedImage>>
    suspend fun deleteImage(uri: Uri): DeleteSavedImageResult
}

sealed interface SavedImagesState {
    data object Loading : SavedImagesState
    data object Empty : SavedImagesState
    data object RecoverableError : SavedImagesState
    data class Content(val images: List<SavedImage>) : SavedImagesState
}

sealed interface DeleteSavedImageResult {
    data object Deleted : DeleteSavedImageResult
    data object AlreadyMissing : DeleteSavedImageResult
    data object InvalidTarget : DeleteSavedImageResult
    data object Error : DeleteSavedImageResult
    data class NeedsSystemConfirmation(
        val uri: Uri,
        val intentSender: IntentSender
    ) : DeleteSavedImageResult
}
