package com.guardaestados.ui.video

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.guardaestados.data.video.AndroidVideoPartSharerRepository
import com.guardaestados.data.video.Media3VideoSplitterRepository
import com.guardaestados.domain.video.GeneratedVideoPart
import com.guardaestados.domain.video.SelectedVideo
import com.guardaestados.domain.video.VideoMetadataResult
import com.guardaestados.domain.video.VideoPartSharerRepository
import com.guardaestados.domain.video.VideoShareResult
import com.guardaestados.domain.video.VideoSplitPlanner
import com.guardaestados.domain.video.VideoSplitProgress
import com.guardaestados.domain.video.VideoSplitResult
import com.guardaestados.domain.video.VideoSplitterRepository
import com.guardaestados.domain.video.VideoTrimPlanner
import com.guardaestados.domain.video.VideoTrimRange
import com.guardaestados.domain.video.VideoTrimRangeValidation
import com.guardaestados.domain.video.VideoTrimResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class VideoSplitterViewModel(
    private val splitterRepository: VideoSplitterRepository,
    private val sharerRepository: VideoPartSharerRepository,
    private val planner: VideoSplitPlanner = VideoSplitPlanner(),
    private val trimPlanner: VideoTrimPlanner = VideoTrimPlanner()
) : ViewModel() {
    private val _uiState = MutableStateFlow(VideoSplitterUiState())
    val uiState: StateFlow<VideoSplitterUiState> = _uiState.asStateFlow()

    private var loadVideoJob: Job? = null
    private var processingJob: Job? = null

    fun onVideoSelected(uri: Uri?) {
        if (uri == null) return
        loadVideoJob?.cancel()
        processingJob?.cancel()
        _uiState.value = VideoSplitterUiState(status = VideoSplitterStatus.LoadingVideo)
        loadVideoJob = viewModelScope.launch(Dispatchers.IO) {
            when (val result = splitterRepository.loadVideo(uri)) {
                is VideoMetadataResult.Success -> {
                    val selectedSeconds = DEFAULT_PART_SECONDS
                    val trimRange = trimPlanner.defaultRange(result.video.durationMs)
                    val trimValidation = trimPlanner.validate(trimRange, result.video.durationMs)
                    _uiState.value = VideoSplitterUiState(
                        selectedVideo = result.video,
                        previewUri = result.video.uri,
                        previewTrimRange = null,
                        previewShouldAutoPlay = false,
                        activeMode = VideoSplitterMode.Trim,
                        selectedPartSeconds = selectedSeconds,
                        estimatedParts = planner.estimatedPartCount(result.video.durationMs, selectedSeconds),
                        trimRange = trimRange,
                        trimRangeValidation = trimValidation,
                        status = VideoSplitterStatus.Ready
                    )
                }
                VideoMetadataResult.EmptyOrUnknownDuration -> showMessage(VideoSplitterMessage.UnknownDuration)
                VideoMetadataResult.FileUnavailable -> showMessage(VideoSplitterMessage.FileUnavailable)
                VideoMetadataResult.Error -> showMessage(VideoSplitterMessage.LoadError)
            }
        }
    }

    fun resetTemporaryStateIfIdle() {
        if (processingJob?.isActive == true) return
        loadVideoJob?.cancel()
        loadVideoJob = null
        processingJob = null
        _uiState.value = VideoSplitterUiState()
    }

    fun selectPartDuration(seconds: Int) {
        if (seconds !in PART_SECONDS_OPTIONS) return
        _uiState.update { state ->
            state.copy(
                selectedPartSeconds = seconds,
                estimatedParts = planner.estimatedPartCount(state.selectedVideo?.durationMs, seconds)
            )
        }
    }

    fun selectMode(mode: VideoSplitterMode) {
        if (_uiState.value.status == VideoSplitterStatus.Processing) return
        _uiState.update { state ->
            if (state.activeMode == mode) {
                state.copy(message = null)
            } else {
                state.copy(
                    activeMode = mode,
                    previewTrimRange = null,
                    previewRequestKey = state.previewRequestKey + 1,
                    previewShouldAutoPlay = false,
                    message = null
                )
            }
        }
    }

    fun updateTrimRange(startSeconds: Int, endSeconds: Int) {
        if (_uiState.value.status == VideoSplitterStatus.Processing) return
        _uiState.update { state ->
            val range = trimPlanner.coerceRange(
                startSeconds = startSeconds,
                endSeconds = endSeconds,
                durationMs = state.selectedVideo?.durationMs,
                previousRange = state.trimRange
            )
            state.withTrimRange(range)
        }
    }

    fun adjustTrimStart(deltaSeconds: Int) {
        if (_uiState.value.status == VideoSplitterStatus.Processing) return
        _uiState.update { state ->
            val range = trimPlanner.adjustStart(
                range = state.trimRange,
                deltaSeconds = deltaSeconds,
                durationMs = state.selectedVideo?.durationMs
            )
            state.withTrimRange(range)
        }
    }

    fun adjustTrimEnd(deltaSeconds: Int) {
        if (_uiState.value.status == VideoSplitterStatus.Processing) return
        _uiState.update { state ->
            val range = trimPlanner.adjustEnd(
                range = state.trimRange,
                deltaSeconds = deltaSeconds,
                durationMs = state.selectedVideo?.durationMs
            )
            state.withTrimRange(range)
        }
    }

    fun createParts() {
        val video = _uiState.value.selectedVideo ?: return
        if (processingJob?.isActive == true) return
        processingJob = viewModelScope.launch(Dispatchers.IO) {
            _uiState.update {
                it.copy(
                    status = VideoSplitterStatus.Processing,
                    progress = VideoSplitProgress(currentPart = 0, totalParts = it.estimatedParts),
                    message = null,
                    generatedParts = emptyList()
                )
            }
            val result = splitterRepository.splitVideo(
                video = video,
                partDurationSeconds = _uiState.value.selectedPartSeconds,
                onProgress = { progress -> _uiState.update { it.copy(progress = progress) } }
            )
            when (result) {
                is VideoSplitResult.Success -> _uiState.update {
                    it.copy(
                        generatedParts = result.parts.sortedBy { part -> part.index },
                        previewUri = result.parts.firstOrNull()?.uri ?: it.previewUri,
                        status = VideoSplitterStatus.Completed,
                        progress = null,
                        message = VideoSplitterMessage.SplitSuccess
                    )
                }
                VideoSplitResult.Cancelled -> showMessage(VideoSplitterMessage.Cancelled, VideoSplitterStatus.Ready)
                VideoSplitResult.EmptyOrUnknownDuration -> showMessage(VideoSplitterMessage.UnknownDuration, VideoSplitterStatus.Ready)
                VideoSplitResult.FileUnavailable -> showMessage(VideoSplitterMessage.FileUnavailable, VideoSplitterStatus.Ready)
                VideoSplitResult.InsufficientStorage -> showMessage(VideoSplitterMessage.InsufficientStorage, VideoSplitterStatus.Ready)
                VideoSplitResult.UnsupportedAndroidVersion -> showMessage(VideoSplitterMessage.UnsupportedAndroidVersion, VideoSplitterStatus.Ready)
                VideoSplitResult.ExportError -> showMessage(VideoSplitterMessage.ExportError, VideoSplitterStatus.Ready)
            }
        }
    }

    fun createTrim() {
        val state = _uiState.value
        val video = state.selectedVideo ?: return
        if (processingJob?.isActive == true || state.trimRangeValidation != VideoTrimRangeValidation.Valid) return
        processingJob = viewModelScope.launch(Dispatchers.IO) {
            _uiState.update {
                it.copy(
                    status = VideoSplitterStatus.Processing,
                    progress = null,
                    message = null,
                    generatedTrim = null
                )
            }
            when (val result = splitterRepository.trimVideo(video, state.trimRange)) {
                is VideoTrimResult.Success -> _uiState.update {
                    it.copy(
                        generatedTrim = result.trim,
                        previewUri = result.trim.uri,
                        previewTrimRange = null,
                        previewShouldAutoPlay = false,
                        status = VideoSplitterStatus.Completed,
                        progress = null,
                        message = VideoSplitterMessage.TrimSuccess
                    )
                }
                VideoTrimResult.Cancelled -> showMessage(VideoSplitterMessage.Cancelled, VideoSplitterStatus.Ready)
                VideoTrimResult.InvalidRange -> showMessage(VideoSplitterMessage.InvalidTrimRange, VideoSplitterStatus.Ready)
                VideoTrimResult.EmptyOrUnknownDuration -> showMessage(VideoSplitterMessage.UnknownDuration, VideoSplitterStatus.Ready)
                VideoTrimResult.FileUnavailable -> showMessage(VideoSplitterMessage.FileUnavailable, VideoSplitterStatus.Ready)
                VideoTrimResult.InsufficientStorage -> showMessage(VideoSplitterMessage.InsufficientStorage, VideoSplitterStatus.Ready)
                VideoTrimResult.DestinationPermissionLost -> showMessage(VideoSplitterMessage.DestinationPermissionLost, VideoSplitterStatus.Ready)
                VideoTrimResult.DestinationUnavailable -> showMessage(VideoSplitterMessage.DestinationUnavailable, VideoSplitterStatus.Ready)
                VideoTrimResult.UnsupportedAndroidVersion -> showMessage(VideoSplitterMessage.UnsupportedAndroidVersion, VideoSplitterStatus.Ready)
                VideoTrimResult.ExportError -> showMessage(VideoSplitterMessage.ExportError, VideoSplitterStatus.Ready)
            }
        }
    }

    fun cancelProcessing() {
        processingJob?.cancel()
        processingJob = null
        _uiState.update {
            it.copy(
                status = if (it.selectedVideo == null) VideoSplitterStatus.Idle else VideoSplitterStatus.Ready,
                progress = null,
                message = VideoSplitterMessage.Cancelled
            )
        }
    }

    fun previewOriginal() {
        _uiState.update {
            it.copy(
                previewUri = it.selectedVideo?.uri ?: it.previewUri,
                previewTrimRange = null,
                previewShouldAutoPlay = false
            )
        }
    }

    fun previewTrimRange() {
        _uiState.update { state ->
            if (state.trimRangeValidation != VideoTrimRangeValidation.Valid) {
                return@update state
            }
            val selectedVideo = state.selectedVideo ?: return@update state
            state.copy(
                previewUri = selectedVideo.uri,
                previewTrimRange = state.trimRange,
                previewRequestKey = state.previewRequestKey + 1,
                previewShouldAutoPlay = true
            )
        }
    }

    fun previewPart(part: GeneratedVideoPart) {
        _uiState.update {
            it.copy(
                previewUri = part.uri,
                previewTrimRange = null,
                previewRequestKey = it.previewRequestKey + 1,
                previewShouldAutoPlay = false
            )
        }
    }

    fun sharePart(part: GeneratedVideoPart) {
        share { sharerRepository.sharePart(part) }
    }

    fun shareAllParts() {
        val parts = _uiState.value.generatedParts
        share { sharerRepository.shareAll(parts) }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }

    private fun share(block: suspend () -> VideoShareResult) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(shareStatus = VideoShareStatus.Opening, message = null) }
            val message = when (block()) {
                VideoShareResult.ChooserOpened -> VideoSplitterMessage.ShareChooserOpened
                VideoShareResult.NoCompatibleApp -> VideoSplitterMessage.ShareNoCompatibleApp
                VideoShareResult.FileUnavailable -> VideoSplitterMessage.FileUnavailable
                VideoShareResult.Error -> VideoSplitterMessage.ShareError
            }
            _uiState.update { it.copy(shareStatus = VideoShareStatus.Idle, message = message) }
        }
    }

    private suspend fun showMessage(
        message: VideoSplitterMessage,
        status: VideoSplitterStatus = VideoSplitterStatus.Idle
    ) {
        withContext(Dispatchers.Main.immediate) {
            _uiState.update {
                it.copy(status = status, progress = null, message = message)
            }
        }
    }

    private fun VideoSplitterUiState.withTrimRange(range: VideoTrimRange): VideoSplitterUiState {
        val shouldPauseTrimPreview = previewTrimRange != null || previewShouldAutoPlay
        return copy(
            trimRange = range,
            trimRangeValidation = trimPlanner.validate(range, selectedVideo?.durationMs),
            previewTrimRange = null,
            previewRequestKey = if (shouldPauseTrimPreview) previewRequestKey + 1 else previewRequestKey,
            previewShouldAutoPlay = false,
            message = null
        )
    }

    private companion object {
        const val DEFAULT_PART_SECONDS = 30
        val PART_SECONDS_OPTIONS = setOf(15, 30, 60)
    }
}

data class VideoSplitterUiState(
    val selectedVideo: SelectedVideo? = null,
    val previewUri: Uri? = null,
    val previewTrimRange: VideoTrimRange? = null,
    val previewRequestKey: Int = 0,
    val previewShouldAutoPlay: Boolean = false,
    val activeMode: VideoSplitterMode = VideoSplitterMode.Split,
    val selectedPartSeconds: Int = 30,
    val estimatedParts: Int = 0,
    val trimRange: VideoTrimRange = VideoTrimRange(startSeconds = 0, endSeconds = 1),
    val trimRangeValidation: VideoTrimRangeValidation = VideoTrimRangeValidation.UnknownDuration,
    val generatedParts: List<GeneratedVideoPart> = emptyList(),
    val generatedTrim: GeneratedVideoPart? = null,
    val status: VideoSplitterStatus = VideoSplitterStatus.Idle,
    val progress: VideoSplitProgress? = null,
    val shareStatus: VideoShareStatus = VideoShareStatus.Idle,
    val message: VideoSplitterMessage? = null
)

enum class VideoSplitterMode {
    Split,
    Trim
}

enum class VideoSplitterStatus {
    Idle,
    LoadingVideo,
    Ready,
    Processing,
    Completed
}

enum class VideoShareStatus {
    Idle,
    Opening
}

enum class VideoSplitterMessage {
    UnknownDuration,
    FileUnavailable,
    LoadError,
    InsufficientStorage,
    UnsupportedAndroidVersion,
    ExportError,
    InvalidTrimRange,
    DestinationPermissionLost,
    DestinationUnavailable,
    Cancelled,
    SplitSuccess,
    TrimSuccess,
    ShareChooserOpened,
    ShareNoCompatibleApp,
    ShareError
}

class VideoSplitterViewModelFactory(
    context: Context
) : ViewModelProvider.Factory {
    private val appContext = context.applicationContext

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(VideoSplitterViewModel::class.java)) {
            return VideoSplitterViewModel(
                splitterRepository = Media3VideoSplitterRepository(appContext),
                sharerRepository = AndroidVideoPartSharerRepository(appContext)
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
