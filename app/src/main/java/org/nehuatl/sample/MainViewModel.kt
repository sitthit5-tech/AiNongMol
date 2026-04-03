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

                val clazz = listOf("org.nehuatl.llamacpp.LlamaContext", "org.nehuatl.llamacpp.LLamaContext")
                    .firstNotNullOfOrNull { try { Class.forName(it) } catch (e: Exception) { null } }
                    ?: throw Exception("ไม่พบ Library")

                ctx = clazz.getConstructor(String::class.java).newInstance(file.absolutePath)
                
                _modelName.value = name
                _chatState.value = ChatUiState.Idle
            } catch (e: Exception) {
                _modelName.value = "❌ " + (e.message ?: "Load Fail")
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
                "${if (it.role == "user") "User" else "Assistant"}: ${it.content}" 
            } + "\nAssistant:"

            try {
                val methods = currentCtx.javaClass.methods
                
                // 🔍 1. หา Method สำหรับส่ง Prompt (เช่น setPrompt, feed, tokenize)
                val setPromptMethod = methods.firstOrNull { 
                    val n = it.name.lowercase()
                    (n.contains("prompt") || n.contains("feed") || n.contains("text")) && it.parameterTypes.size == 1
                }
                
                // 🔍 2. หา Method completion(Int, Map)
                val completionMethod = methods.firstOrNull { 
                    it.name == "completion" && it.parameterTypes.size == 2
                }

                // 🔥 ปฏิบัติการ:
                setPromptMethod?.invoke(currentCtx, prompt)

                val params = mapOf("temperature" to 0.7, "top_k" to 40)
                val result = completionMethod?.invoke(currentCtx, 128, params)

                val textResult = result?.toString()?.trim() ?: "ไม่มีคำตอบ"
                _messages.value = _messages.value + ChatMessage("assistant", textResult)

            } catch (e: Exception) {
                _messages.value = _messages.value + ChatMessage("assistant", "Error: " + (e.cause?.message ?: e.message))
            }
            _chatState.value = ChatUiState.Idle
        }
    }
}
