package com.guardaestados.ui.saved

import android.content.Context
import android.content.IntentSender
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.guardaestados.data.saved.MediaStoreSavedImagesRepository
import com.guardaestados.domain.saved.DeleteSavedImageResult
import com.guardaestados.domain.saved.DeleteSavedImageUseCase
import com.guardaestados.domain.saved.LoadSavedImagesUseCase
import com.guardaestados.domain.saved.SavedImage
import com.guardaestados.domain.saved.SavedImagesState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SavedImagesViewModel(
    private val loadSavedImages: LoadSavedImagesUseCase,
    private val deleteSavedImage: DeleteSavedImageUseCase
) : ViewModel() {
    private val _uiState = MutableStateFlow<SavedImagesState>(SavedImagesState.Loading)
    val uiState: StateFlow<SavedImagesState> = _uiState.asStateFlow()

    private val _deleteState = MutableStateFlow<SavedImageDeleteState>(SavedImageDeleteState.Idle)
    val deleteState: StateFlow<SavedImageDeleteState> = _deleteState.asStateFlow()

    private var pendingSystemConfirmationImage: SavedImage? = null
    private var nextSystemConfirmationRequestId = 0L

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = SavedImagesState.Loading
            _uiState.value = loadSavedImages.execute()
        }
    }

    fun delete(image: SavedImage) {
        viewModelScope.launch(Dispatchers.IO) {
            _deleteState.value = SavedImageDeleteState.Deleting
            handleDeleteResult(image, deleteSavedImage.execute(image))
        }
    }

    fun onSystemDeleteConfirmationLaunched() {
        _deleteState.value = SavedImageDeleteState.Deleting
    }

    fun onSystemDeleteConfirmationResult(confirmed: Boolean) {
        val pendingImage = pendingSystemConfirmationImage
        pendingSystemConfirmationImage = null
        if (!confirmed || pendingImage == null) {
            _deleteState.value = SavedImageDeleteState.Error
            return
        }
        delete(pendingImage)
    }

    fun clearDeleteMessage() {
        _deleteState.value = SavedImageDeleteState.Idle
    }

    private suspend fun handleDeleteResult(
        image: SavedImage,
        result: DeleteSavedImageResult
    ) {
        when (result) {
            DeleteSavedImageResult.Deleted -> {
                refreshSavedImagesAfterDelete()
                _deleteState.value = SavedImageDeleteState.Success
            }

            DeleteSavedImageResult.AlreadyMissing -> {
                refreshSavedImagesAfterDelete()
                _deleteState.value = SavedImageDeleteState.AlreadyMissing
            }

            DeleteSavedImageResult.InvalidTarget -> _deleteState.value = SavedImageDeleteState.InvalidTarget
            DeleteSavedImageResult.Error -> _deleteState.value = SavedImageDeleteState.Error
            is DeleteSavedImageResult.NeedsSystemConfirmation -> {
                pendingSystemConfirmationImage = image
                _deleteState.value = SavedImageDeleteState.NeedsSystemConfirmation(
                    requestId = nextSystemConfirmationRequestId++,
                    intentSender = result.intentSender
                )
            }
        }
    }

    private fun refreshSavedImagesAfterDelete() {
        _uiState.value = loadSavedImages.execute()
    }
}

sealed interface SavedImageDeleteState {
    data object Idle : SavedImageDeleteState
    data object Deleting : SavedImageDeleteState
    data object Success : SavedImageDeleteState
    data object AlreadyMissing : SavedImageDeleteState
    data object InvalidTarget : SavedImageDeleteState
    data object Error : SavedImageDeleteState
    data class NeedsSystemConfirmation(
        val requestId: Long,
        val intentSender: IntentSender
    ) : SavedImageDeleteState
}

class SavedImagesViewModelFactory(
    context: Context
) : ViewModelProvider.Factory {
    private val appContext = context.applicationContext

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SavedImagesViewModel::class.java)) {
            val repository = MediaStoreSavedImagesRepository(appContext)
            val loadUseCase = LoadSavedImagesUseCase(repository)
            val deleteUseCase = DeleteSavedImageUseCase(repository)
            return SavedImagesViewModel(loadUseCase, deleteUseCase) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
