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
                ctx = null
                val file = File(filesDir, "model.gguf")
                contentResolver.openInputStream(Uri.parse(uriString))?.use { input ->
                    FileOutputStream(file).use { output -> input.copyTo(output) }
                }

                // 1. หา Class
                val clazz = listOf("org.nehuatl.llamacpp.LlamaContext", "org.nehuatl.llamacpp.LLamaContext")
                    .firstNotNullOfOrNull { try { Class.forName(it) } catch (e: Exception) { null } }
                    ?: throw Exception("Library not found")

                // 2. ⚡ ท่าไม้ตาย: หา Static Method สำหรับโหลด (มักจะรับ String)
                val loadMethod = clazz.methods.firstOrNull { 
                    java.lang.reflect.Modifier.isStatic(it.modifiers) && 
                    it.parameterTypes.size == 1 && 
                    it.parameterTypes[0] == String::class.java
                }

                if (loadMethod != null) {
                    // ถ้าเจอ Method สำหรับโหลด ให้เรียกตัวนั้น
                    ctx = loadMethod.invoke(null, file.absolutePath)
                } else {
                    // ถ้าไม่เจอจริงๆ ให้ลอง Constructor แบบ Default (ไม่มี Parameter) 
                    // แล้วค่อยไปเซ็ต Path ทีหลัง
                    ctx = clazz.getConstructor().newInstance()
                    clazz.methods.firstOrNull { it.name.contains("load") || it.name.contains("Model") }
                        ?.invoke(ctx, file.absolutePath)
                }

                _modelName.value = name
                _chatState.value = ChatUiState.Idle
            } catch (e: Exception) {
                val realError = e.cause?.message ?: e.message
                _modelName.value = "❌ พลาด: $realError"
                _chatState.value = ChatUiState.Error(realError ?: "Unknown")
            }
        }
    }

    fun sendPrompt(text: String) {
        val currentCtx = ctx ?: return
        viewModelScope.launch(Dispatchers.IO) {
            _chatState.value = ChatUiState.Generating("")
            try {
                // หา Method completion
                val method = currentCtx.javaClass.methods.firstOrNull { it.name == "completion" }
                val prompt = text + "\nAssistant:"
                val params = mapOf("n_predict" to 128)
                
                // ลองเรียกแบบ (String, Map) หรือ (Map)
                val result = if (method?.parameterTypes?.size == 2) {
                    method.invoke(currentCtx, prompt, params)
                } else {
                    method?.invoke(currentCtx, params)
                }

                _messages.value = _messages.value + ChatMessage("user", text)
                _messages.value = _messages.value + ChatMessage("assistant", result?.toString()?.trim() ?: "...")
            } catch (e: Exception) {
                _messages.value = _messages.value + ChatMessage("assistant", "Error: ${e.message}")
            }
            _chatState.value = ChatUiState.Idle
        }
    }
}
