package org.nehuatl.sample

// 💬 โครงสร้างข้อความแชท
data class ChatMessage(
    val role: String,
    val content: String
)

// 🧠 สถานะของ UI (รองรับสตรีมข้อความด้วย Generating)
sealed class ChatUiState {
    object Idle : ChatUiState()
    object LoadingModel : ChatUiState()
    data class Generating(val partialText: String) : ChatUiState()
}
