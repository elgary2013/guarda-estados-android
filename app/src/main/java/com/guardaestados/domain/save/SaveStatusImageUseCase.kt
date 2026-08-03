package com.guardaestados.domain.save

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

sealed interface SaveStatusImageResult {
    data class Success(val displayName: String) : SaveStatusImageResult
    data object Error : SaveStatusImageResult
}
