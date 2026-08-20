package com.guardaestados.domain.save

import android.net.Uri
import com.guardaestados.domain.status.StatusImage

class SaveStatusImageUseCase(
    private val repository: StatusImageSaverRepository
) {
    suspend fun execute(image: StatusImage): SaveStatusImageResult {
        return repository.save(image)
    }
}

interface StatusImageSaverRepository {
    suspend fun save(image: StatusImage): SaveStatusImageResult
}

class ImportSavedMediaUseCase(
    private val repository: ImportedMediaSaverRepository
) {
    suspend fun execute(uri: Uri): ImportSavedMediaResult {
        return repository.importMedia(uri)
    }
}

interface ImportedMediaSaverRepository {
    suspend fun importMedia(uri: Uri): ImportSavedMediaResult
}

sealed interface SaveStatusImageResult {
    data class Success(val displayName: String) : SaveStatusImageResult
    data object Duplicate : SaveStatusImageResult
    data object DestinationPermissionLost : SaveStatusImageResult
    data object DestinationUnavailable : SaveStatusImageResult
    data object Error : SaveStatusImageResult
}

sealed interface ImportSavedMediaResult {
    data class Success(val displayName: String) : ImportSavedMediaResult
    data object Unsupported : ImportSavedMediaResult
    data object Missing : ImportSavedMediaResult
    data object DestinationPermissionLost : ImportSavedMediaResult
    data object DestinationUnavailable : ImportSavedMediaResult
    data object Error : ImportSavedMediaResult
}
