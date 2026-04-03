package org.nehuatl.sample

import android.content.ContentResolver
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.nehuatl.llamacpp.LlamaContext // ✅ Import ตรงๆ
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

    private val _modelName = MutableStateFlow("รอโหลดโมเดล...")
    val modelName: StateFlow<String> = _modelName

    private var ctx: LlamaContext? = null // ✅ ใช้ Type จริง

    fun setModel(uriString: String, name: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _chatState.value = ChatUiState.LoadingModel
            try {
                ctx?.close()
                ctx = null

                val file = File(filesDir, "model.gguf")
                contentResolver.openInputStream(Uri.parse(uriString))?.use { input ->
                    FileOutputStream(file).use { output -> input.copyTo(output) }
                }

                // ✅ เรียกตรงๆ ถ้า Signature ผิด CI จะแดงทันที!
                // ถ้า LlamaContext ต้องการ params เพิ่ม CI จะบอกเราที่นี่
                ctx = LlamaContext(file.absolutePath)
                
                _modelName.value = name
                _chatState.value = ChatUiState.Idle
            } catch (e: Exception) {
                _modelName.value = "❌ พลาด: " + e.message
                _chatState.value = ChatUiState.Error(e.message ?: "Load Fail")
            }
        }
    }

    fun sendPrompt(text: String) {
        val currentCtx = ctx ?: return
        _messages.value = _messages.value + ChatMessage("user", text)

        viewModelScope.launch(Dispatchers.IO) {
            _chatState.value = ChatUiState.Generating("")
            val prompt = _messages.value.takeLast(4).joinToString("\n") { it.role + ": " + it.content } + "\nAssistant:"

            try {
                // ✅ เรียก completion ตรงๆ
                val response = currentCtx.completion(prompt)
                _messages.value = _messages.value + ChatMessage("assistant", response.trim())
            } catch (e: Exception) {
                _messages.value = _messages.value + ChatMessage("assistant", "Error: " + e.message)
            }
            _chatState.value = ChatUiState.Idle
        }
    }

    override fun onCleared() {
        super.onCleared()
        ctx?.close()
    }
}
