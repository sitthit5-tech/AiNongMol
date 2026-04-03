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

                // 🔍 หา Class (ลองทั้งสองแบบที่อาจเป็นไปได้)
                val clazz = listOf("org.nehuatl.llamacpp.LlamaContext", "org.nehuatl.llamacpp.LLamaContext")
                    .firstNotNullOfOrNull { try { Class.forName(it) } catch (e: Exception) { null } }
                    ?: throw Exception("Library not found")

                // ⚡ ลองสร้าง Instance (ท่าที่ 1: Constructor รับ String)
                ctx = try {
                    clazz.getConstructor(String::class.java).newInstance(file.absolutePath)
                } catch (e: Exception) {
                    // ท่าที่ 2: Constructor ว่าง แล้วเรียก load
                    val instance = clazz.getConstructor().newInstance()
                    clazz.methods.firstOrNull { it.name.contains("load", ignoreCase = true) }
                        ?.invoke(instance, file.absolutePath)
                    instance
                }

                _modelName.value = name
                _chatState.value = ChatUiState.Idle
            } catch (e: Exception) {
                val msg = e.cause?.message ?: e.message
                _modelName.value = "❌ โหลดไม่เข้า: $msg"
                _chatState.value = ChatUiState.Error(msg ?: "Load Fail")
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
                val methods = currentCtx.javaClass.methods
                val completion = methods.firstOrNull { it.name == "completion" }
                
                // เตรียม Parameter (Map คือท่ามาตรฐาน)
                val params = mapOf(
                    "prompt" to text,
                    "n_predict" to 128,
                    "temperature" to 0.7
                )

                // เรียกใช้ (รองรับทั้งแบบส่ง Prompt แยก หรือส่งรวมใน Map)
                val result = if (completion?.parameterTypes?.size == 2) {
                    completion.invoke(currentCtx, text, params)
                } else {
                    completion?.invoke(currentCtx, params)
                }

                val response = result?.toString()?.trim() ?: "ไม่มีคำตอบ"
                _messages.value = _messages.value + ChatMessage("assistant", response)
            } catch (e: Exception) {
                _messages.value = _messages.value + ChatMessage("assistant", "Error: ${e.cause?.message ?: e.message}")
            }
            _chatState.value = ChatUiState.Idle
        }
    }

    fun stopGeneration() {
        _chatState.value = ChatUiState.Idle
    }
}
