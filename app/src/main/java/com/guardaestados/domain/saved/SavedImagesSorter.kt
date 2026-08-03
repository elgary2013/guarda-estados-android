package com.guardaestados.domain.saved

class SavedImagesSorter {
    fun sort(images: List<SavedImage>): List<SavedImage> {
        return sortBySavedMetadata(
            items = images,
            dateAddedMillis = SavedImage::dateAddedMillis,
            name = SavedImage::name
        )
    }

    fun <T> sortBySavedMetadata(
        items: List<T>,
        dateAddedMillis: (T) -> Long?,
        name: (T) -> String
    ): List<T> {
        return items.sortedWith(
            compareByDescending<T> { dateAddedMillis(it) ?: 0L }
                .thenBy { name(it) }
        )
    }
}