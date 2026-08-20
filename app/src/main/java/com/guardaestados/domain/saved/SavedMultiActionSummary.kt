package com.guardaestados.domain.saved

data class SavedMultiActionSummary(
    val successCount: Int,
    val failedCount: Int
) {
    init {
        require(successCount >= 0) { "successCount must be non-negative" }
        require(failedCount >= 0) { "failedCount must be non-negative" }
    }
}

class SavedMultiActionSummaryCalculator {
    fun summarize(
        successCount: Int,
        totalCount: Int
    ): SavedMultiActionSummary {
        require(successCount >= 0) { "successCount must be non-negative" }
        require(totalCount >= 0) { "totalCount must be non-negative" }
        require(successCount <= totalCount) { "successCount cannot exceed totalCount" }
        return SavedMultiActionSummary(
            successCount = successCount,
            failedCount = totalCount - successCount
        )
    }
}
