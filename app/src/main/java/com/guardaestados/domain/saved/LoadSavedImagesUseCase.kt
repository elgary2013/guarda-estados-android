package com.guardaestados.domain.saved

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

interface SavedImagesRepository {
    fun loadImages(): Result<List<SavedImage>>
}

sealed interface SavedImagesState {
    data object Loading : SavedImagesState
    data object Empty : SavedImagesState
    data object RecoverableError : SavedImagesState
    data class Content(val images: List<SavedImage>) : SavedImagesState
}
