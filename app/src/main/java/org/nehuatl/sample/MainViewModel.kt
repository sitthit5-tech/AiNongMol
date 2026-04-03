package org.nehuatl.sample

import android.content.ContentResolver
import android.net.Uri
import android.util.Log
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
                ctx = null 
                val file = File(filesDir, "model.gguf")
                contentResolver.openInputStream(Uri.parse(uriString))?.use { input ->
                    FileOutputStream(file).use { output -> input.copyTo(output) }
                }

                // ค้นหา Class และสร้าง Instance (ใช้ท่า Safe Reflection)
                val clazz = listOf("org.nehuatl.llamacpp.LlamaContext", "org.nehuatl.llamacpp.LLamaContext")
                    .firstNotNullOfOrNull { try { Class.forName(it) } catch (e: Exception) { null } }
                    ?: throw Exception("ไม่พบ Library AI")

                ctx = clazz.getConstructor(String::class.java).newInstance(file.absolutePath)
                
                _modelName.value = name
                _chatState.value = ChatUiState.Idle
            } catch (e: Exception) {
                _modelName.value = "❌ โหลดพลาด: " + e.message
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

            val prompt = _messages.value.takeLast(4).joinToString("\n") {
                val role = if (it.role == "user") "User" else "Assistant"
                "$role: ${it.content}"
            } + "\nAssistant:"

            try {
                // 🎯 จุดตาย: เรียก Method 'completion' ด้วย (String, Map)
                val method = currentCtx.javaClass.methods.firstOrNull { it.name == "completion" }
                
                val params = mapOf(
                    "n_predict" to 128,
                    "temperature" to 0.7,
                    "top_k" to 40,
                    "top_p" to 0.9,
                    "repeat_penalty" to 1.1
                )

                // เรียกใช้จริง: invoke(object, arg1, arg2)
                val result = method?.invoke(currentCtx, prompt, params)

                val textResult = result?.toString()?.trim() ?: "ไม่มีคำตอบจาก AI"
                _messages.value = _messages.value + ChatMessage("assistant", textResult)

            } catch (e: Exception) {
                val errorMsg = e.cause?.message ?: e.message ?: "Unknown Error"
                _messages.value = _messages.value + ChatMessage("assistant", "Error: $errorMsg")
            }

            _chatState.value = ChatUiState.Idle
        }
    }
}
