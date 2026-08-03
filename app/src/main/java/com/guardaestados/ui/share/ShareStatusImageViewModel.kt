package com.guardaestados.ui.share

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.guardaestados.data.share.AndroidStatusImageSharerRepository
import com.guardaestados.domain.share.ShareStatusImageResult
import com.guardaestados.domain.share.ShareStatusImageUseCase
import com.guardaestados.domain.status.StatusImage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ShareStatusImageViewModel(
    private val shareStatusImage: ShareStatusImageUseCase
) : ViewModel() {
    private val _uiState = MutableStateFlow<ShareStatusImageUiState>(ShareStatusImageUiState.Idle)
    val uiState: StateFlow<ShareStatusImageUiState> = _uiState.asStateFlow()

    fun share(image: StatusImage) {
        if (_uiState.value == ShareStatusImageUiState.Sharing) return

        viewModelScope.launch {
            _uiState.value = ShareStatusImageUiState.Sharing
            _uiState.value = when (shareStatusImage.execute(image)) {
                ShareStatusImageResult.ChooserOpened -> ShareStatusImageUiState.ChooserOpened
                ShareStatusImageResult.Error -> ShareStatusImageUiState.Error
                ShareStatusImageResult.NoCompatibleApp -> ShareStatusImageUiState.NoCompatibleApp
            }
        }
    }
}

sealed interface ShareStatusImageUiState {
    data object Idle : ShareStatusImageUiState
    data object Sharing : ShareStatusImageUiState
    data object ChooserOpened : ShareStatusImageUiState
    data object Error : ShareStatusImageUiState
    data object NoCompatibleApp : ShareStatusImageUiState
}

class ShareStatusImageViewModelFactory(
    context: Context
) : ViewModelProvider.Factory {
    private val appContext = context.applicationContext

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ShareStatusImageViewModel::class.java)) {
            val repository = AndroidStatusImageSharerRepository(appContext)
            val useCase = ShareStatusImageUseCase(repository)
            return ShareStatusImageViewModel(useCase) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
