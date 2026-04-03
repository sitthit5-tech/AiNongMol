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

    private val _modelName = MutableStateFlow("สถานะ: รอโหลดโมเดล")
    val modelName: StateFlow<String> = _modelName

    private var ctx: Any? = null

    fun setModel(uriString: String, name: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _chatState.value = ChatUiState.LoadingModel
            _modelName.value = "⏳ กำลังเตรียมไฟล์..."
            
            try {
                closeContext()

                val file = File(filesDir, "model.gguf")
                if (file.exists()) file.delete()

                // 🔍 1. เช็ค InputStream
                val inputStream = contentResolver.openInputStream(Uri.parse(uriString)) 
                    ?: throw Exception("เปิดไฟล์ไม่ได้ (Stream เป็น Null)")

                _modelName.value = "📂 กำลัง Copy โมเดล..."
                inputStream.use { input ->
                    FileOutputStream(file).use { output ->
                        input.copyTo(output)
                    }
                }

                // 🔍 2. เช็คขนาดไฟล์หลัง Copy
                val fileSizeMB = file.length() / (1024 * 1024)
                if (fileSizeMB == 0L) throw Exception("Copy ล้มเหลว (ไฟล์มีขนาด 0 Byte)")
                
                _modelName.value = "🧠 กำลัง Init โมเดล (" + fileSizeMB + "MB)..."

                // 🔍 3. ลอง Init Class (ใช้ Reflection แบบชัวร์ๆ)
                val classNames = listOf(
                    "org.nehuatl.llamacpp.LLamaContext",
                    "org.nehuatl.llamacpp.LlamaContext"
                )

                val clazz = classNames.firstNotNullOfOrNull {
                    try { Class.forName(it) } catch (e: Exception) { null }
                } ?: throw Exception("ไม่พบ Lib llama.cpp ในเครื่อง")

                val constructor = clazz.getConstructor(String::class.java)
                ctx = constructor.newInstance(file.absolutePath)

                _modelName.value = "✅ พร้อมใช้งาน: " + name + " (" + fileSizeMB + "MB)"
                _chatState.value = ChatUiState.Idle

            } catch (e: Exception) {
                val errorMsg = e.message ?: "Unknown Error"
                Log.e("AiNongMol", "Error: " + errorMsg)
                _modelName.value = "❌ พลาด: " + errorMsg
                _chatState.value = ChatUiState.Error(errorMsg)
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
                val role = if (it.role == "user") "User" else "Assistant"
                role + ": " + it.content
            } + "\nAssistant:"

            try {
                val method = currentCtx.javaClass.methods.firstOrNull { 
                    it.name == "completion" || it.name == "sendPrompt" 
                }

                val result = if (method != null) {
                    when (method.parameterTypes.size) {
                        1 -> method.invoke(currentCtx, prompt)
                        2 -> method.invoke(currentCtx, prompt, 128)
                        else -> method.invoke(currentCtx, prompt)
                    }
                } else "ไม่พบ Method สำหรับประมวลผล"

                val textResult = result?.toString()?.trim() ?: "ไม่มีคำตอบจากโมเดล"
                _messages.value = _messages.value + ChatMessage("assistant", textResult)

            } catch (e: Exception) {
                _messages.value = _messages.value + ChatMessage("assistant", "Error: " + (e.message ?: "AI พัง"))
            }
            _chatState.value = ChatUiState.Idle
        }
    }

    private fun closeContext() {
        try {
            ctx?.let {
                val method = it.javaClass.methods.firstOrNull { m -> 
                    m.name == "close" || m.name == "release" 
                }
                method?.invoke(it)
            }
        } catch (e: Exception) {}
        ctx = null
    }

    override fun onCleared() {
        super.onCleared()
        closeContext()
    }
}
