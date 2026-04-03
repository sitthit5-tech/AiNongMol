package org.nehuatl.sample

import android.content.ContentResolver
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.nehuatl.llamacpp.$REAL_CLASS
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

    private var ctx: $REAL_CLASS? = null

    fun setModel(uriString: String, name: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _chatState.value = ChatUiState.LoadingModel
            try {
                ctx?.close()
                ctx = null

                val file = File(filesDir, "model.gguf")
                if (file.exists()) file.delete()

                contentResolver.openInputStream(Uri.parse(uriString))?.use { input ->
                    FileOutputStream(file).use { output ->
                        input.copyTo(output)
                    }
                }

                // ✅ เรียก Constructor ตรงๆ ไม่ต้องใช้ Reflection
                ctx = $REAL_CLASS(file.absolutePath)
                
                _modelName.value = name
                _chatState.value = ChatUiState.Idle
                Log.d("AiNongMol", "Model Loaded: $name")
            } catch (e: Exception) {
                _modelName.value = "โหลดพลาด: " + e.message
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
                (if (it.role == "user") "User" else "Assistant") + ": " + it.content 
            } + "\nAssistant:"

            try {
                // ✅ เรียก Method ตรงๆ (ผมใส่ Parameter ยอดนิยมไว้ให้ ถ้าติดแดงพี่บอกผมนะ)
                // ส่วนใหญ่จะใช้ completion(prompt) หรือ completion(prompt, tokens)
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
        ctx = null
    }
}
