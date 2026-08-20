package com.guardaestados.ui.save

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.guardaestados.data.save.MediaStoreStatusImageSaverRepository
import com.guardaestados.domain.save.MultiSaveStatusSummary
import com.guardaestados.domain.save.MultiSaveStatusSummaryCalculator
import com.guardaestados.domain.save.SaveStatusImageResult
import com.guardaestados.domain.save.SaveStatusImageUseCase
import com.guardaestados.domain.status.StatusImage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SaveStatusImageViewModel(
    private val saveStatusImage: SaveStatusImageUseCase,
    private val multiSaveSummaryCalculator: MultiSaveStatusSummaryCalculator = MultiSaveStatusSummaryCalculator()
) : ViewModel() {
    private val _uiState = MutableStateFlow<SaveStatusImageUiState>(SaveStatusImageUiState.Idle)
    val uiState: StateFlow<SaveStatusImageUiState> = _uiState.asStateFlow()

    private val _multiSaveState = MutableStateFlow<MultiSaveStatusUiState>(MultiSaveStatusUiState.Idle)
    val multiSaveState: StateFlow<MultiSaveStatusUiState> = _multiSaveState.asStateFlow()

    fun save(image: StatusImage) {
        if (_uiState.value == SaveStatusImageUiState.Saving) return

        viewModelScope.launch {
            _uiState.value = SaveStatusImageUiState.Saving
            _uiState.value = when (val result = saveStatusImage.execute(image)) {
                SaveStatusImageResult.Duplicate -> SaveStatusImageUiState.Duplicate
                SaveStatusImageResult.DestinationPermissionLost -> SaveStatusImageUiState.DestinationPermissionLost
                SaveStatusImageResult.DestinationUnavailable -> SaveStatusImageUiState.DestinationUnavailable
                SaveStatusImageResult.Error -> SaveStatusImageUiState.Error
                is SaveStatusImageResult.Success -> SaveStatusImageUiState.Success(result.displayName)
            }
        }
    }

    fun saveAll(images: List<StatusImage>) {
        if (images.isEmpty() || _multiSaveState.value is MultiSaveStatusUiState.Saving) return

        viewModelScope.launch {
            val results = mutableListOf<SaveStatusImageResult>()
            _multiSaveState.value = MultiSaveStatusUiState.Saving(
                processedCount = 0,
                totalCount = images.size
            )
            images.forEach { image ->
                val result = saveStatusImage.execute(image)
                results += result
                _multiSaveState.value = MultiSaveStatusUiState.Saving(
                    processedCount = results.size,
                    totalCount = images.size
                )
            }
            _multiSaveState.value = MultiSaveStatusUiState.Finished(
                multiSaveSummaryCalculator.summarize(results)
            )
        }
    }

    fun clearMultiSaveResult() {
        if (_multiSaveState.value is MultiSaveStatusUiState.Finished) {
            _multiSaveState.value = MultiSaveStatusUiState.Idle
        }
    }
}

sealed interface SaveStatusImageUiState {
    data object Idle : SaveStatusImageUiState
    data object Saving : SaveStatusImageUiState
    data object Duplicate : SaveStatusImageUiState
    data object DestinationPermissionLost : SaveStatusImageUiState
    data object DestinationUnavailable : SaveStatusImageUiState
    data class Success(val displayName: String) : SaveStatusImageUiState
    data object Error : SaveStatusImageUiState
}

sealed interface MultiSaveStatusUiState {
    data object Idle : MultiSaveStatusUiState
    data class Saving(
        val processedCount: Int,
        val totalCount: Int
    ) : MultiSaveStatusUiState

    data class Finished(
        val summary: MultiSaveStatusSummary
    ) : MultiSaveStatusUiState
}

class SaveStatusImageViewModelFactory(
    context: Context
) : ViewModelProvider.Factory {
    private val appContext = context.applicationContext

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SaveStatusImageViewModel::class.java)) {
            val repository = MediaStoreStatusImageSaverRepository(appContext)
            val useCase = SaveStatusImageUseCase(repository)
            return SaveStatusImageViewModel(useCase) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
