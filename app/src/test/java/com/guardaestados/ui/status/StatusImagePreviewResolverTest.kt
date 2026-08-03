package com.guardaestados.ui.status

import com.guardaestados.domain.status.StatusGalleryState
import org.junit.Assert.assertEquals
import org.junit.Test

class StatusImagePreviewResolverTest {
    private val resolver = StatusImagePreviewResolver()

    @Test
    fun `returns unavailable when route uri is missing`() {
        assertEquals(
            StatusImagePreviewState.Unavailable,
            resolver.resolve(StatusGalleryState.Empty, null)
        )
    }

    @Test
    fun `maps gallery permission state to preview permission state`() {
        assertEquals(
            StatusImagePreviewState.PermissionLost,
            resolver.resolve(StatusGalleryState.PermissionLost, "content://image")
        )
    }

    @Test
    fun `maps gallery loading state to preview loading state`() {
        assertEquals(
            StatusImagePreviewState.Loading,
            resolver.resolve(StatusGalleryState.Loading, "content://image")
        )
    }

    @Test
    fun `maps missing folder state to preview missing folder state`() {
        assertEquals(
            StatusImagePreviewState.NoFolderSelected,
            resolver.resolve(StatusGalleryState.NoFolderSelected, "content://image")
        )
    }
}
