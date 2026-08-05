package com.guardaestados.domain.save

class UniqueFileNameGenerator {
    fun generate(preferredName: String, exists: (String) -> Boolean): String {
        val normalizedName = preferredName.trim().ifBlank { DEFAULT_NAME }
        if (!exists(normalizedName)) return normalizedName

        val baseName = normalizedName.substringBeforeLast('.', normalizedName).ifBlank { DEFAULT_NAME }
        val extension = normalizedName.substringAfterLast('.', missingDelimiterValue = "")
        var index = 1
        while (true) {
            val candidate = if (extension.isBlank()) {
                "${baseName}_$index"
            } else {
                "${baseName}_$index.$extension"
            }
            if (!exists(candidate)) return candidate
            index++
        }
    }

    private companion object {
        const val DEFAULT_NAME = "archivo"
    }
}