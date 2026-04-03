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
                ctx?.close()
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
            
            // สร้าง Prompt แบบ Chat History (จดจำบริบท 5 ข้อความล่าสุด)
            val prompt = _messages.value.takeLast(6).joinToString("\n") { 
                "${if (it.role == \"user\") \"User\" else \"Assistant\"}: ${it.content}" 
            } + "\nAssistant:"

            val response = try {
                // ✅ Fix เป็น completion(prompt) ตามมาตรฐาน llama.cpp wrapper ส่วนใหญ่
                currentCtx.completion(prompt)
            } catch (e: Exception) {
                "Error: ${e.message}"
            }

            _messages.value = _messages.value + ChatMessage("assistant", response.trim())
            _chatState.value = ChatUiState.Idle
        }
    }

    override fun onCleared() {
        super.onCleared()
        ctx?.close()
        ctx = null
    }
}
