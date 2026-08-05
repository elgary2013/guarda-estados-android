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
import com.guardaestados.domain.saved.OpenSavedImageResult
import com.guardaestados.domain.saved.OpenSavedImageUseCase
import com.guardaestados.domain.saved.SavedImage
import com.guardaestados.domain.saved.SavedImagesState
import com.guardaestados.domain.saved.ShareSavedImageResult
import com.guardaestados.domain.saved.ShareSavedImageUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SavedImagesViewModel(
    private val loadSavedImages: LoadSavedImagesUseCase,
    private val deleteSavedImage: DeleteSavedImageUseCase,
    private val shareSavedImage: ShareSavedImageUseCase,
    private val openSavedImage: OpenSavedImageUseCase
) : ViewModel() {
    private val _uiState = MutableStateFlow<SavedImagesState>(SavedImagesState.Loading)
    val uiState: StateFlow<SavedImagesState> = _uiState.asStateFlow()

    private val _deleteState = MutableStateFlow<SavedImageDeleteState>(SavedImageDeleteState.Idle)
    val deleteState: StateFlow<SavedImageDeleteState> = _deleteState.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _shareState = MutableStateFlow<SavedImageShareState>(SavedImageShareState.Idle)
    val shareState: StateFlow<SavedImageShareState> = _shareState.asStateFlow()

    private val _openState = MutableStateFlow<SavedImageOpenState>(SavedImageOpenState.Idle)
    val openState: StateFlow<SavedImageOpenState> = _openState.asStateFlow()

    private val _selectedPreviewImage = MutableStateFlow<SavedImage?>(null)
    val selectedPreviewImage: StateFlow<SavedImage?> = _selectedPreviewImage.asStateFlow()

    private var pendingSystemConfirmationImage: SavedImage? = null
    private var nextSystemConfirmationRequestId = 0L

    init {
        refresh()
    }

    fun refresh() {
        if (_isRefreshing.value) return

        viewModelScope.launch(Dispatchers.IO) {
            _isRefreshing.value = true
            if (_uiState.value !is SavedImagesState.Content) {
                _uiState.value = SavedImagesState.Loading
            }
            try {
                _uiState.value = loadSavedImages.execute()
            } finally {
                withContext(NonCancellable) {
                    _isRefreshing.value = false
                }
            }
        }
    }

    fun selectForPreview(image: SavedImage) {
        _selectedPreviewImage.value = image
    }

    fun delete(image: SavedImage) {
        viewModelScope.launch(Dispatchers.IO) {
            _deleteState.value = SavedImageDeleteState.Deleting
            handleDeleteResult(image, deleteSavedImage.execute(image))
        }
    }

    fun share(image: SavedImage) {
        if (_shareState.value == SavedImageShareState.Sharing) return

        viewModelScope.launch(Dispatchers.IO) {
            _shareState.value = SavedImageShareState.Sharing
            _shareState.value = when (shareSavedImage.execute(image)) {
                ShareSavedImageResult.ChooserOpened -> SavedImageShareState.ChooserOpened
                ShareSavedImageResult.AlreadyMissing -> SavedImageShareState.AlreadyMissing
                ShareSavedImageResult.InvalidTarget -> SavedImageShareState.InvalidTarget
                ShareSavedImageResult.NoCompatibleApp -> SavedImageShareState.NoCompatibleApp
                ShareSavedImageResult.Error -> SavedImageShareState.Error
            }
            if (_shareState.value == SavedImageShareState.AlreadyMissing) {
                refreshSavedImagesAfterDelete()
            }
        }
    }

    fun open(image: SavedImage) {
        if (_openState.value == SavedImageOpenState.Opening) return

        viewModelScope.launch(Dispatchers.IO) {
            _openState.value = SavedImageOpenState.Opening
            _openState.value = when (openSavedImage.execute(image)) {
                OpenSavedImageResult.ViewerOpened -> SavedImageOpenState.ViewerOpened
                OpenSavedImageResult.AlreadyMissing -> SavedImageOpenState.AlreadyMissing
                OpenSavedImageResult.InvalidTarget -> SavedImageOpenState.InvalidTarget
                OpenSavedImageResult.NoCompatibleApp -> SavedImageOpenState.NoCompatibleApp
                OpenSavedImageResult.Error -> SavedImageOpenState.Error
            }
            if (_openState.value == SavedImageOpenState.AlreadyMissing) {
                refreshSavedImagesAfterDelete()
            }
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

    fun clearShareMessage() {
        _shareState.value = SavedImageShareState.Idle
    }

    fun clearOpenMessage() {
        _openState.value = SavedImageOpenState.Idle
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

    private suspend fun refreshSavedImagesAfterDelete() {
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

sealed interface SavedImageShareState {
    data object Idle : SavedImageShareState
    data object Sharing : SavedImageShareState
    data object ChooserOpened : SavedImageShareState
    data object AlreadyMissing : SavedImageShareState
    data object InvalidTarget : SavedImageShareState
    data object NoCompatibleApp : SavedImageShareState
    data object Error : SavedImageShareState
}

sealed interface SavedImageOpenState {
    data object Idle : SavedImageOpenState
    data object Opening : SavedImageOpenState
    data object ViewerOpened : SavedImageOpenState
    data object AlreadyMissing : SavedImageOpenState
    data object InvalidTarget : SavedImageOpenState
    data object NoCompatibleApp : SavedImageOpenState
    data object Error : SavedImageOpenState
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
            val shareUseCase = ShareSavedImageUseCase(repository)
            val openUseCase = OpenSavedImageUseCase(repository)
            return SavedImagesViewModel(loadUseCase, deleteUseCase, shareUseCase, openUseCase) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
