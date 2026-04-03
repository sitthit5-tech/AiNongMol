package org.nehuatl.sample

import android.content.ContentResolver
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

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

    private var ctx: Any? = null
    private var generationJob: Job? = null

    fun setModel(uriString: String, name: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _chatState.value = ChatUiState.LoadingModel
            try {
                ctx = null
                val file = File(filesDir, "model.gguf")
                contentResolver.openInputStream(Uri.parse(uriString))?.use { input ->
                    FileOutputStream(file).use { output -> input.copyTo(output) }
                }

                val clazz = listOf("org.nehuatl.llamacpp.LlamaContext", "org.nehuatl.llamacpp.LLamaContext")
                    .firstNotNullOfOrNull { try { Class.forName(it) } catch (e: Exception) { null } }
                    ?: throw Exception("Library not found")

                ctx = clazz.getConstructor(String::class.java).newInstance(file.absolutePath)
                _modelName.value = name
                _chatState.value = ChatUiState.Idle
            } catch (e: Exception) {
                _modelName.value = "❌ พลาด: ${e.message}"
                _chatState.value = ChatUiState.Error(e.message ?: "Load Fail")
            }
        }
    }

    fun sendPrompt(text: String) {
        val currentCtx = ctx ?: return
        if (_chatState.value is ChatUiState.Generating) return

        val updatedMessages = _messages.value + ChatMessage("user", text)
        _messages.value = updatedMessages

        generationJob?.cancel()
        generationJob = viewModelScope.launch(Dispatchers.IO) {
            _chatState.value = ChatUiState.Generating("")

            try {
                val methods = currentCtx.javaClass.methods
                val setPrompt = methods.firstOrNull { it.name.lowercase().contains("prompt") }
                val completion = methods.firstOrNull { it.name == "completion" }

                // Context Memory: จำย้อนหลัง 8 รอบ
                val prompt = updatedMessages.takeLast(8).joinToString("\n") {
                    "${if (it.role == "user") "User" else "Assistant"}: ${it.content}"
                } + "\nAssistant:"

                setPrompt?.invoke(currentCtx, prompt)

                val params = mapOf(
                    "n_predict" to 256,
                    "temperature" to 0.7,
                    "top_p" to 0.9,
                    "top_k" to 40,
                    "repeat_penalty" to 1.1
                )

                val result = completion?.invoke(currentCtx, 256, params)?.toString() ?: ""

                // 🔥 Streaming Simulation (จำลองการพิมพ์ทีละนิดให้ดู Pro)
                var partial = ""
                result.split(" ").forEach { word ->
                    partial += "$word "
                    _chatState.value = ChatUiState.Generating(partial)
                    delay(50) // ความเร็วในการพิมพ์
                }

                _messages.value = _messages.value + ChatMessage("assistant", partial.trim())
            } catch (e: Exception) {
                _messages.value = _messages.value + ChatMessage("assistant", "Error: ${e.cause?.message ?: e.message}")
            }
            _chatState.value = ChatUiState.Idle
        }
    }

    fun stopGeneration() {
        generationJob?.cancel()
        _chatState.value = ChatUiState.Idle
    }
}
