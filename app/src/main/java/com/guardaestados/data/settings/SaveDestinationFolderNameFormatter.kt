package com.guardaestados.data.settings

import java.net.URLDecoder
import java.nio.charset.StandardCharsets

class SaveDestinationFolderNameFormatter {
    fun format(uriString: String): String {
        val decodedValue = runCatching {
            URLDecoder.decode(uriString, StandardCharsets.UTF_8.name())
        }.getOrDefault(uriString)
        return decodedValue
            .substringBefore('?')
            .substringAfterLast(':')
            .substringAfterLast('/')
            .ifBlank { uriString }
    }
}