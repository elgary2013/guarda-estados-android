package com.guardaestados.domain.saved

import org.junit.Assert.assertEquals
import org.junit.Test

class SavedImagesSorterTest {
    private val sorter = SavedImagesSorter()

    @Test
    fun sortsNewestImagesFirst() {
        val old = TestSavedImage("old.jpg", 1000L)
        val newest = TestSavedImage("newest.jpg", 3000L)
        val middle = TestSavedImage("middle.jpg", 2000L)

        assertEquals(
            listOf(newest, middle, old),
            sorter.sortBySavedMetadata(
                items = listOf(old, newest, middle),
                dateAddedMillis = TestSavedImage::dateAddedMillis,
                name = TestSavedImage::name
            )
        )
    }

    @Test
    fun sortsMissingDatesLastByName() {
        val second = TestSavedImage("b.jpg", null)
        val first = TestSavedImage("a.jpg", null)
        val dated = TestSavedImage("dated.jpg", 1L)

        assertEquals(
            listOf(dated, first, second),
            sorter.sortBySavedMetadata(
                items = listOf(second, first, dated),
                dateAddedMillis = TestSavedImage::dateAddedMillis,
                name = TestSavedImage::name
            )
        )
    }

    private data class TestSavedImage(
        val name: String,
        val dateAddedMillis: Long?
    )
}