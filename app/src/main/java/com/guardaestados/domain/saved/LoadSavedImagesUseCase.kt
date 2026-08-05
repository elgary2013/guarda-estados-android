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

class ShareSavedImageUseCase(
    private val repository: SavedImagesRepository
) {
    suspend fun execute(image: SavedImage): ShareSavedImageResult {
        return repository.shareImage(image)
    }
}

class OpenSavedImageUseCase(
    private val repository: SavedImagesRepository
) {
    suspend fun execute(image: SavedImage): OpenSavedImageResult {
        return repository.openImage(image)
    }
}

interface SavedImagesRepository {
    fun loadImages(): Result<List<SavedImage>>
    suspend fun deleteImage(uri: Uri): DeleteSavedImageResult
    suspend fun shareImage(image: SavedImage): ShareSavedImageResult
    suspend fun openImage(image: SavedImage): OpenSavedImageResult
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

sealed interface ShareSavedImageResult {
    data object ChooserOpened : ShareSavedImageResult
    data object AlreadyMissing : ShareSavedImageResult
    data object InvalidTarget : ShareSavedImageResult
    data object NoCompatibleApp : ShareSavedImageResult
    data object Error : ShareSavedImageResult
}

sealed interface OpenSavedImageResult {
    data object ViewerOpened : OpenSavedImageResult
    data object AlreadyMissing : OpenSavedImageResult
    data object InvalidTarget : OpenSavedImageResult
    data object NoCompatibleApp : OpenSavedImageResult
    data object Error : OpenSavedImageResult
}
