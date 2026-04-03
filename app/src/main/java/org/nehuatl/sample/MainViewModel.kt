package org.nehuatl.sample

import android.content.ContentResolver
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.nehuatl.llamacpp.LlamaContext
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

    private val _modelName = MutableStateFlow("ไม่ได้เลือกโมเดล")
    val modelName: StateFlow<String> = _modelName

    private var ctx: LlamaContext? = null

    fun setModel(uriString: String, name: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _chatState.value = ChatUiState.LoadingModel
            try {
                // เคลียร์ของเก่า
                ctx?.let { it.javaClass.getMethod("close").invoke(it) }
                ctx = null
                
                val file = File(filesDir, "model.gguf")
                if (file.exists()) file.delete()
                
                contentResolver.openInputStream(Uri.parse(uriString))?.use { input ->
                    FileOutputStream(file).use { output -> input.copyTo(output) }
                }
                
                ctx = LlamaContext(file.absolutePath)
                _modelName.value = name
                _chatState.value = ChatUiState.Idle
            } catch (e: Exception) {
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
            
            // สร้าง Prompt แบบเรียบง่ายที่สุดเพื่อลด Error
            val prompt = _messages.value.takeLast(4).joinToString("\n") { 
                val role = if (it.role == "user") "User" else "Assistant"
                "$role: ${it.content}"
            } + "\nAssistant:"

            try {
                // สุ่มเรียก completion แบบปลอดภัย (ลองส่ง String ตัวเดียวก่อน)
                // ถ้า Signature เป็น (String, Int) เราจะส่ง 128 เป็นค่า default
                val response = try {
                    currentCtx.completion(prompt)
                } catch (e: Exception) {
                    // Fallback กรณีต้องการ Int (n_predict)
                    val method = currentCtx.javaClass.methods.find { it.name == "completion" }
                    if (method?.parameterTypes?.size == 2) {
                        method.invoke(currentCtx, prompt, 128) as String
                    } else {
                        "Error calling completion"
                    }
                }
                
                _messages.value = _messages.value + ChatMessage("assistant", response.toString().trim())
            } catch (e: Exception) {
                _messages.value = _messages.value + ChatMessage("assistant", "Error: ${e.message}")
            }
            _chatState.value = ChatUiState.Idle
        }
    }

    override fun onCleared() {
        super.onCleared()
        try {
            ctx?.let { it.javaClass.getMethod("close").invoke(it) }
        } catch (e: Exception) {}
        ctx = null
    }
}
