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

    private var ctx: Any? = null

    fun setModel(uriString: String, name: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _chatState.value = ChatUiState.LoadingModel
            try {
                val file = File(filesDir, "model.gguf")
                contentResolver.openInputStream(Uri.parse(uriString))?.use { input ->
                    FileOutputStream(file).use { output -> input.copyTo(output) }
                }

                // 🔑 ใช้กุญแจที่เราสแกนเจอ (LlamaContext)
                val clazz = Class.forName("org.nehuatl.llamacpp.LlamaContext")
                
                // ลองสร้างด้วยท่าสร้าง (Factory) หรือ Constructor
                ctx = try {
                    val create = clazz.methods.firstOrNull { it.name == "create" || it.name == "load" }
                    create?.invoke(null, file.absolutePath) ?: clazz.getConstructor(String::class.java).newInstance(file.absolutePath)
                } catch (e: Exception) {
                    clazz.getConstructor().newInstance().also { instance ->
                        clazz.methods.firstOrNull { it.name == "load" }?.invoke(instance, file.absolutePath)
                    }
                }

                _modelName.value = name
                _chatState.value = ChatUiState.Idle
            } catch (e: Exception) {
                _modelName.value = "❌ โหลดไม่เข้า: ${e.cause?.message ?: e.message}"
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
                // 🗣️ หา Method สำหรับตอบคำถาม
                val methods = currentCtx.javaClass.methods
                val completion = methods.firstOrNull { it.name == "completion" || it.name == "generate" }
                
                // ส่งคำถามเข้าไป (รองรับทั้งแบบ String และ Map)
                val response = if (completion?.parameterTypes?.size == 1) {
                    completion.invoke(currentCtx, text)
                } else {
                    completion?.invoke(currentCtx, text, mapOf("n_predict" to 128))
                }

                val finalMessage = response?.toString()?.trim() ?: "AI ไม่ตอบกลับ"
                _messages.value = _messages.value + ChatMessage("assistant", finalMessage)
            } catch (e: Exception) {
                _messages.value = _messages.value + ChatMessage("assistant", "❌ เกิดข้อผิดพลาด: ${e.cause?.message ?: e.message}")
            }
            _chatState.value = ChatUiState.Idle
        }
    }

    fun stopGeneration() { _chatState.value = ChatUiState.Idle }
}
