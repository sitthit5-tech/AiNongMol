package org.nehuatl.sample

data class ChatMessage(
    val role: String,
    val content: String
)

sealed class ChatUiState {
    object Idle : ChatUiState()
    object LoadingModel : ChatUiState()
    data class Generating(val partialText: String) : ChatUiState()
}
