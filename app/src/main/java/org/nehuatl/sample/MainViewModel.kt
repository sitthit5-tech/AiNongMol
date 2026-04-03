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

                // 🔍 ส่องหา Class LlamaContext
                val clazz = listOf("org.nehuatl.llamacpp.LlamaContext", "org.nehuatl.llamacpp.LLamaContext")
                    .firstNotNullOfOrNull { try { Class.forName(it) } catch (e: Exception) { null } }
                    ?: throw Exception("หา Library ไม่เจอ")

                // 🔥 ท่าแก้เผด็จศึก: หา Constructor ที่รับ String ถ้าไม่มีให้ใช้ Constructor เปล่า
                val constructor = clazz.constructors.firstOrNull { it.parameterTypes.size == 1 && it.parameterTypes[0] == String::class.java }
                
                ctx = if (constructor != null) {
                    constructor.newInstance(file.absolutePath)
                } else {
                    // ถ้าไม่มี Constructor รับ String ให้ลองสร้างแบบเปล่า แล้วหา method load
                    val instance = clazz.getConstructor().newInstance()
                    val loadMethod = clazz.methods.firstOrNull { it.name.contains("load", ignoreCase = true) }
                    loadMethod?.invoke(instance, file.absolutePath)
                    instance
                }
                
                _modelName.value = name
                _chatState.value = ChatUiState.Idle
            } catch (e: Exception) {
                // ดึงสาเหตุที่แท้จริงออกมาโชว์
                val cause = e.cause?.toString() ?: e.toString()
                _modelName.value = "❌ พลาด: $cause"
                _chatState.value = ChatUiState.Error(cause)
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
                val setPrompt = methods.firstOrNull { it.name.lowercase().contains("prompt") }
                val completion = methods.firstOrNull { it.name == "completion" }

                setPrompt?.invoke(currentCtx, text)
                val result = completion?.invoke(currentCtx, 128, mapOf("temp" to 0.7))

                _messages.value = _messages.value + ChatMessage("assistant", result?.toString()?.trim() ?: "AI เงียบไป...")
            } catch (e: Exception) {
                _messages.value = _messages.value + ChatMessage("assistant", "Error: ${e.cause ?: e.message}")
            }
            _chatState.value = ChatUiState.Idle
        }
    }
}
