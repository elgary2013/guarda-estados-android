package com.guardaestados.domain.status

data class StatusImageCandidate(
    val name: String?,
    val mimeType: String?,
    val isDirectory: Boolean,
    val sizeBytes: Long?
)
