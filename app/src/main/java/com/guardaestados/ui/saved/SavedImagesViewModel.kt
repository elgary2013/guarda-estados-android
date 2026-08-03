package com.guardaestados.ui.saved

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.guardaestados.data.saved.MediaStoreSavedImagesRepository
import com.guardaestados.domain.saved.LoadSavedImagesUseCase
import com.guardaestados.domain.saved.SavedImagesState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SavedImagesViewModel(
    private val loadSavedImages: LoadSavedImagesUseCase
) : ViewModel() {
    private val _uiState = MutableStateFlow<SavedImagesState>(SavedImagesState.Loading)
    val uiState: StateFlow<SavedImagesState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = SavedImagesState.Loading
            _uiState.value = loadSavedImages.execute()
        }
    }
}

class SavedImagesViewModelFactory(
    context: Context
) : ViewModelProvider.Factory {
    private val appContext = context.applicationContext

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SavedImagesViewModel::class.java)) {
            val repository = MediaStoreSavedImagesRepository(appContext)
            val useCase = LoadSavedImagesUseCase(repository)
            return SavedImagesViewModel(useCase) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
