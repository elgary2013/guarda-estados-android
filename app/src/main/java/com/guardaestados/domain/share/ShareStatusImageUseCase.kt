package com.guardaestados.domain.share

import com.guardaestados.domain.status.StatusImage

class ShareStatusImageUseCase(
    private val repository: StatusImageSharerRepository
) {
    suspend fun execute(image: StatusImage): ShareStatusImageResult {
        return repository.share(image)
    }
}

interface StatusImageSharerRepository {
    suspend fun share(image: StatusImage): ShareStatusImageResult
}

sealed interface ShareStatusImageResult {
    data object ChooserOpened : ShareStatusImageResult
    data object Error : ShareStatusImageResult
    data object NoCompatibleApp : ShareStatusImageResult
}
