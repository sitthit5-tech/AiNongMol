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
import org.nehuatl.llamacpp.LlamaContext
import java.io.File
import java.io.FileOutputStream

class MainViewModel(
    private val contentResolver: ContentResolver,
    private val filesDir: File
) : ViewModel() {
    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _chatState = MutableStateFlow<ChatUiState>(ChatUiState.Idle)
    val chatState: StateFlow<ChatUiState> = _chatState.asStateFlow()

    private val _modelName = MutableStateFlow("")
    val modelName: StateFlow<String> = _modelName.asStateFlow()

    private var llamaContext: LlamaContext? = null
    private var currentModelFile: File? = null

    fun setModel(uriString: String, name: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _chatState.value = ChatUiState.LoadingModel
            try {
                llamaContext = null
                currentModelFile?.delete()
                
                val uri = Uri.parse(uriString)
                // ✅ ใช้ filesDir ที่ส่งมาจาก MainActivity ชัวร์ที่สุด
                val internalFile = File(filesDir, "active_model.gguf")
                
                contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(internalFile).use { output ->
                        input.copyTo(output)
                    }
                }
                
                val ctx = LlamaContext(internalFile.absolutePath)
                llamaContext = ctx
                currentModelFile = internalFile
                _modelName.value = name
                _chatState.value = ChatUiState.Idle
            } catch (e: Exception) {
                _chatState.value = ChatUiState.Idle
                _modelName.value = "Error: ${e.message}"
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
            val prompt = "User: $userText\nAssistant:"
            var response = ""
            try {
                response = ctx.sendPrompt(prompt, temperature = 0.7f, maxTokens = 256)
            } catch (e: Exception) {
                response = "เกิดปัญหา: ${e.message}"
            }
            
            val updatedMsgs = _messages.value.toMutableList()
            updatedMsgs.add(ChatMessage("assistant", response.trim()))
            _messages.value = updatedMsgs
            _chatState.value = ChatUiState.Idle
        }
    }
}
