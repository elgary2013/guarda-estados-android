package com.guardaestados.ui.status

class StatusImagePresentationFormatter {
    fun title(name: String, fallbackDate: String?): String {
        return name.trim().ifBlank { fallbackDate.orEmpty().trim() }
    }

    fun sizeValue(sizeBytes: Long?): String? {
        return sizeBytes?.takeIf { it > 0L }?.toString()
    }
}
