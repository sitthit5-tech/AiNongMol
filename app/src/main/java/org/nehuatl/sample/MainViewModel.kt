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

    private val _modelName = MutableStateFlow("รอโหลดโมเดล...")
    val modelName: StateFlow<String> = _modelName

    private var ctx: LlamaContext? = null

    fun setModel(uriString: String, name: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _chatState.value = ChatUiState.LoadingModel
            try {
                ctx = null 
                val file = File(filesDir, "model.gguf")
                contentResolver.openInputStream(Uri.parse(uriString))?.use { input ->
                    FileOutputStream(file).use { output -> input.copyTo(output) }
                }

                // เรียก Constructor (ส่ง Path)
                ctx = LlamaContext(file.absolutePath)
                
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
            
            try {
                // ✅ แก้ตาม CI: ส่ง Map เข้าไป (จะใส่ค่าว่างหรือ Config ก็ได้)
                val params = mapOf<String, Any>(
                    "n_predict" to 128,
                    "prompt" to text // ลองใส่ prompt ลงใน map เผื่อ lib รับทางนี้
                )
                
                val result = currentCtx.completion(params) 

                // ✅ แก้เรื่อง trim: แปลงเป็น String ก่อน แล้วค่อยจัดการ
                val response = result?.toString() ?: "ไม่มีคำตอบ"

                _messages.value = _messages.value + ChatMessage("assistant", response.trim())
            } catch (e: Exception) {
                _messages.value = _messages.value + ChatMessage("assistant", "Error: " + (e.message ?: "AI Fail"))
            }
            _chatState.value = ChatUiState.Idle
        }
    }
}
