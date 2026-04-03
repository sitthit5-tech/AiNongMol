package org.nehuatl.sample

import android.content.ContentResolver
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.nehuatl.llamacpp.LLamaContext
import java.io.File
import java.io.FileOutputStream

class MainViewModel(private val contentResolver: ContentResolver) : ViewModel() {
    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _chatState = MutableStateFlow<ChatUiState>(ChatUiState.Idle)
    val chatState: StateFlow<ChatUiState> = _chatState.asStateFlow()

    private val _modelName = MutableStateFlow("")
    val modelName: StateFlow<String> = _modelName.asStateFlow()

    private var llamaContext: LLamaContext? = null
    private var currentModelFile: File? = null

    fun setModel(uriString: String, name: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _chatState.value = ChatUiState.LoadingModel
            try {
                currentModelFile?.delete()
                val uri = Uri.parse(uriString)
                
                // 📂 ใช้คาถาเดิม: โหลดเข้า Temp Dir ของระบบที่เคยผ่านชัวร์ๆ
                val internalFile = File(System.getProperty("java.io.tmpdir"), "active_model.gguf")
                contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(internalFile).use { output -> input.copyTo(output) }
                }
                
                val ctx = LLamaContext(internalFile.absolutePath)
                llamaContext = ctx
                currentModelFile = internalFile
                _modelName.value = name
                _chatState.value = ChatUiState.Idle
            } catch (e: Exception) {
                _chatState.value = ChatUiState.Idle
                _modelName.value = "โหลดพลาด: ${e.message}"
            }
        }
    }

    fun sendPrompt(userText: String) {
        val ctx = llamaContext ?: return
        if (_chatState.value is ChatUiState.Generating) return

        val userMsgs = _messages.value.toMutableList()
        userMsgs.add(ChatMessage("user", userText))
        _messages.value = userMsgs

        viewModelScope.launch(Dispatchers.IO) {
            _chatState.value = ChatUiState.Generating("")
            
            // ใช้ Prompt มาตรฐานที่ AI ส่วนใหญ่เข้าใจ
            val prompt = "User: $userText\nAssistant:"
            var response = ""
            try {
                // ⚡ เชื่อมต่อ Engine จริง (Inference)
                response = ctx.sendPrompt(prompt, temperature = 0.7f, maxTokens = 256)
            } catch (e: Exception) {
                response = "น้องมลเอ๋อค่ะพี่: ${e.message}"
            }
            
            val updatedMsgs = _messages.value.toMutableList()
            updatedMsgs.add(ChatMessage("assistant", response.trim()))
            _messages.value = updatedMsgs
            _chatState.value = ChatUiState.Idle
        }
    }
}
