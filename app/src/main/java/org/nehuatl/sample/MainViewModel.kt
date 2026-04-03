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

    private val _modelName = MutableStateFlow("กำลังเตรียมกุญแจ...")
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

                val clazz = listOf("org.nehuatl.llamacpp.LlamaContext", "org.nehuatl.llamacpp.LLamaContext")
                    .firstNotNullOfOrNull { try { Class.forName(it) } catch (e: Exception) { null } }
                    ?: throw Exception("Library not found")

                // 🔑 พยายามสร้าง Object ด้วย 3 ท่ามาตรฐาน
                ctx = try {
                    // ท่าที่ 1: Static Create (พบมากที่สุด)
                    val createMethod = clazz.methods.firstOrNull { it.name == "create" && it.parameterTypes.size == 1 }
                    createMethod?.invoke(null, file.absolutePath)
                } catch (e: Exception) {
                    try {
                        // ท่าที่ 2: Static Load
                        val loadMethod = clazz.methods.firstOrNull { it.name == "load" && it.parameterTypes.size == 1 }
                        loadMethod?.invoke(null, file.absolutePath)
                    } catch (e: Exception) {
                        // ท่าที่ 3: Constructor รับ String (ถ้ามี)
                        clazz.getConstructor(String::class.java).newInstance(file.absolutePath)
                    }
                }

                if (ctx == null) throw Exception("ไม่พบช่องทางสร้าง Context")

                _modelName.value = name
                _chatState.value = ChatUiState.Idle
            } catch (e: Exception) {
                val err = e.cause?.message ?: e.message
                _modelName.value = "❌ กุญแจผิดดอก: $err"
                _chatState.value = ChatUiState.Error(err ?: "Unknown")
            }
        }
    }

    fun sendPrompt(text: String) {
        val currentCtx = ctx ?: return
        _messages.value = _messages.value + ChatMessage("user", text)
        
        viewModelScope.launch(Dispatchers.IO) {
            _chatState.value = ChatUiState.Generating("")
            try {
                // หา Method สำหรับคุย (มักชื่อ completion หรือ generate)
                val method = currentCtx.javaClass.methods.firstOrNull { 
                    it.name == "completion" || it.name == "generate" 
                }
                
                val result = method?.invoke(currentCtx, text, mapOf("n_predict" to 64))
                _messages.value = _messages.value + ChatMessage("assistant", result?.toString()?.trim() ?: "...")
            } catch (e: Exception) {
                _messages.value = _messages.value + ChatMessage("assistant", "Error: ${e.message}")
            }
            _chatState.value = ChatUiState.Idle
        }
    }

    fun stopGeneration() { _chatState.value = ChatUiState.Idle }
}
