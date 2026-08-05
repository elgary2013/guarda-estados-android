package com.guardaestados.domain.status

class StatusImageClassifier {
    fun isAccepted(candidate: StatusImageCandidate): Boolean {
        if (candidate.isDirectory) return false
        if (candidate.name.isNullOrBlank()) return false
        if (candidate.isTemporaryFile()) return false
        if (candidate.sizeBytes != null && candidate.sizeBytes <= 0L) return false
        return normalizeMimeType(candidate.mimeType) != null
    }

    fun normalizeMimeType(mimeType: String?): String? {
        return when (mimeType?.trim()?.lowercase()) {
            "image/jpeg", "image/jpg" -> "image/jpeg"
            "image/png" -> "image/png"
            "image/webp" -> "image/webp"
            "video/mp4" -> "video/mp4"
            "video/3gpp" -> "video/3gpp"
            "video/webm" -> "video/webm"
            else -> null
        }
    }

    private fun StatusImageCandidate.isTemporaryFile(): Boolean {
        val normalizedName = name.orEmpty().trim().lowercase()
        return normalizedName.startsWith(".") ||
            normalizedName.endsWith(".tmp") ||
            normalizedName.endsWith(".temp") ||
            normalizedName.endsWith(".part") ||
            normalizedName.endsWith("~")
    }
}
