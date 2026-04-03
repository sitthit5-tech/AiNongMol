package org.nehuatl.sample

import android.content.ContentResolver
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import org.nehuatl.llamacpp.LlamaContext

class MainViewModel(
    private val contentResolver: ContentResolver,
    private val filesDir: File
) : ViewModel() {

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages

    private val _chatState = MutableStateFlow<ChatUiState>(ChatUiState.Idle)
    val chatState: StateFlow<ChatUiState> = _chatState

    private val _modelName = MutableStateFlow("พร้อมใช้งาน")
    val modelName: StateFlow<String> = _modelName

    private var ctx: LlamaContext? = null

    fun setModel(uriString: String, name: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _chatState.value = ChatUiState.LoadingModel
            try {
                val file = File(filesDir, "model.gguf")
                contentResolver.openInputStream(Uri.parse(uriString))?.use { input ->
                    FileOutputStream(file).use { output -> input.copyTo(output) }
                }

                // ✅ Production Constructor Fallback
                ctx = try {
                    LlamaContext(file.absolutePath, 2048)
                } catch (e: Throwable) {
                    LlamaContext(file.absolutePath)
                }

                _modelName.value = name
                _chatState.value = ChatUiState.Idle
            } catch (e: Exception) {
                _modelName.value = "❌ โหลดไม่สำเร็จ"
                _chatState.value = ChatUiState.Error(e.message ?: "Load Fail")
            }
        }
    }

    fun sendPrompt(text: String) {
        val currentCtx = ctx ?: return
        if (_chatState.value is ChatUiState.Generating) return

        _messages.value = _messages.value + ChatMessage("user", text)

        viewModelScope.launch(Dispatchers.IO) {
            _chatState.value = ChatUiState.Generating("")
            try {
                // 🧠 จัด Format Prompt ให้โมเดลเข้าใจง่ายขึ้น
                val formattedPrompt = "User: $text\nAssistant:"
                
                // 🔥 ใช้ Signature ที่เป๊ะที่สุด: completion(Int, Map<String, Any>)
                val response = currentCtx.completion(
                    128, 
                    mapOf<String, Any>(
                        "prompt" to formattedPrompt,
                        "temperature" to 0.7,
                        "n_predict" to 128
                    )
                )

                _messages.value = _messages.value + ChatMessage(
                    "assistant",
                    response.toString().trim()
                )

            } catch (e: Exception) {
                _messages.value = _messages.value + ChatMessage(
                    "assistant",
                    "❌ Error: ${e.message}"
                )
            }
            _chatState.value = ChatUiState.Idle
        }
    }

    fun stopGeneration() {
        _chatState.value = ChatUiState.Idle
    }
}
