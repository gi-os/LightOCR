package com.gios.lightocr.ui

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.gios.lightocr.data.ScanEntity
import com.gios.lightocr.data.ScanRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface ScanUiState {
    /** The camera preview is up, waiting for a shutter press or a picked photo. */
    data object Ready : ScanUiState
    data object Processing : ScanUiState
    data class Result(val scan: ScanEntity) : ScanUiState
    data class Failed(val message: String) : ScanUiState
}

class ScanViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = ScanRepository(application)

    private val _uiState = MutableStateFlow<ScanUiState>(ScanUiState.Ready)
    val uiState: StateFlow<ScanUiState> = _uiState.asStateFlow()

    val history: StateFlow<List<ScanEntity>> = repository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun onCaptured(bitmap: Bitmap) {
        _uiState.value = ScanUiState.Processing
        viewModelScope.launch {
            runCatching { repository.processCapturedBitmap(bitmap) }
                .onSuccess { _uiState.value = ScanUiState.Result(it) }
                .onFailure { _uiState.value = ScanUiState.Failed(it.message ?: "Couldn't read this photo.") }
        }
    }

    fun onPicked(uri: Uri) {
        _uiState.value = ScanUiState.Processing
        viewModelScope.launch {
            runCatching { repository.processPickedDocument(uri) }
                .onSuccess { _uiState.value = ScanUiState.Result(it) }
                .onFailure { _uiState.value = ScanUiState.Failed(it.message ?: "Couldn't read this photo.") }
        }
    }

    fun reset() {
        _uiState.value = ScanUiState.Ready
    }

    fun thumbnailFile(scan: ScanEntity) = repository.thumbnailFile(scan)

    fun delete(scan: ScanEntity) {
        viewModelScope.launch { repository.delete(scan) }
    }

    override fun onCleared() {
        super.onCleared()
        repository.close()
    }
}
