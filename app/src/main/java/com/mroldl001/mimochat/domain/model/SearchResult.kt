
package com.mroldl001.mimochat.domain.model

data class SearchResult(
    val message: Message,
    val chat: Chat,
    val highlightedContent: String
)
