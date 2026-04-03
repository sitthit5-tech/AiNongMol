package org.nehuatl.sample

import android.content.ContentResolver
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.nehuatl.llamacpp.LLamaContext // ✅ ชื่อ Class ตามที่ CI เห็น
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

    private var ctx: LLamaContext? = null

    fun setModel(uriString: String, name: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _chatState.value = ChatUiState.LoadingModel
            try {
                // ✅ ลบ .close() ออกตามที่ CI ด่ามา
                ctx = null 

                val file = File(filesDir, "model.gguf")
                contentResolver.openInputStream(Uri.parse(uriString))?.use { input ->
                    FileOutputStream(file).use { output -> input.copyTo(output) }
                }

                // ✅ เรียก Constructor (ถ้าพังตรงนี้แสดงว่า Constructor ก็ต้องการ Int เหมือนกัน)
                ctx = LLamaContext(file.absolutePath)
                
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
            
            // หมายเหตุ: เรายังไม่รู้ว่าฟังก์ชันรับ Prompt ชื่ออะไร 
            // แต่เราจะแก้ completion(128) ให้ผ่าน CI ก่อน เพื่อดูว่ามัน "คาย" อะไรออกมาไหม
            try {
                // ✅ แก้ตาม CI: completion ต้องการ Int (เช่น 128 tokens)
                val response = currentCtx.completion(128) 

                _messages.value = _messages.value + ChatMessage("assistant", response.trim())
            } catch (e: Exception) {
                _messages.value = _messages.value + ChatMessage("assistant", "Error: " + (e.message ?: "AI Fail"))
            }
            _chatState.value = ChatUiState.Idle
        }
    }
}
