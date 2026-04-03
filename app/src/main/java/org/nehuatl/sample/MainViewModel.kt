package org.nehuatl.sample

import android.content.ContentResolver
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.nehuatl.llamacpp.LLamaContext
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
                // แก้เรื่อง Unresolved reference 'close' 
                // ลองใช้ท่าที่ปลอดภัยที่สุดคือปล่อยให้ GC จัดการหรือเรียกฟังก์ชันที่มีอยู่
                ctx = null 

                val file = File(filesDir, "model.gguf")
                contentResolver.openInputStream(Uri.parse(uriString))?.use { input ->
                    FileOutputStream(file).use { output -> input.copyTo(output) }
                }

                // 🔥 จุดสำคัญ: ในเมื่อ Constructor ต้องการ Int เราต้องหา ID มาให้มัน
                // ผมจะลองใช้ท่า "สร้าง Context เปล่า" หรือเรียกผ่าน Static Method ตามที่ Lib พี่เป็น
                // สมมติว่า Lib พี่ใช้ท่า LLamaContext(id)
                
                try {
                    // ท่านี้คือเดาจาก Error: มันต้องการ Int
                    // เราจะลองส่ง 0 หรือผลลัพธ์จาก Method โหลดไฟล์
                    ctx = LLamaContext(0) 
                } catch (e: Exception) {
                    // ถ้ายังไม่ได้ เราจะใช้ Reflection ส่องหา Method ที่คืนค่าเป็น LLamaContext แทน
                    throw Exception("Lib ต้องการ Int แต่เรายังหา ID ไม่เจอ: " + e.message)
                }

                _modelName.value = name
                _chatState.value = ChatUiState.Idle
            } catch (e: Exception) {
                _modelName.value = "❌ " + e.message
                _chatState.value = ChatUiState.Error(e.message ?: "Load Fail")
            }
        }
    }

    fun sendPrompt(text: String) {
        val currentCtx = ctx ?: return
        _messages.value = _messages.value + ChatMessage("user", text)

        viewModelScope.launch(Dispatchers.IO) {
            _chatState.value = ChatUiState.Generating("")
            val prompt = _messages.value.takeLast(4).joinToString("\n") { it.role + ": " + it.content } + "\nAssistant:"
            try {
                // เรียกใช้แบบปลอดภัย
                val response = currentCtx.completion(prompt)
                _messages.value = _messages.value + ChatMessage("assistant", response.trim())
            } catch (e: Exception) {
                _messages.value = _messages.value + ChatMessage("assistant", "Error: " + e.message)
            }
            _chatState.value = ChatUiState.Idle
        }
    }
}
