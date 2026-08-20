package com.guardaestados.domain.save

data class MultiSaveStatusSummary(
    val savedCount: Int,
    val failedCount: Int
) {
    init {
        require(savedCount >= 0) { "savedCount must be non-negative" }
        require(failedCount >= 0) { "failedCount must be non-negative" }
    }
}

class MultiSaveStatusSummaryCalculator {
    fun summarize(results: List<SaveStatusImageResult>): MultiSaveStatusSummary {
        val savedCount = results.count { result -> result is SaveStatusImageResult.Success }
        return MultiSaveStatusSummary(
            savedCount = savedCount,
            failedCount = results.size - savedCount
        )
    }
}
