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
    private val systemPrompt = "คุณคือ 'น้องมล' AI ภาษาไทยที่ฉลาด ตอบสั้น สุภาพ แทนตัวว่าน้องมล และเรียกผู้ใช้ว่าพี่"

    fun setModel(uriString: String, name: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _chatState.value = ChatUiState.LoadingModel
            try {
                val uri = Uri.parse(uriString)
                val tempFile = File.createTempFile("model_", ".gguf")
                contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(tempFile).use { output -> input.copyTo(output) }
                }
                llamaContext = LLamaContext(tempFile.absolutePath)
                _modelName.value = name
                _chatState.value = ChatUiState.Idle
            } catch (e: Exception) { _chatState.value = ChatUiState.Idle }
        }
    }

    private fun buildPrompt(): String {
        // 🎯 ขยับเป็น 8 ข้อความเพื่อความฉลาดต่อเนื่อง
        val history = _messages.value.takeLast(8)
        val sb = StringBuilder()
        sb.append("<|im_start|>system\n$systemPrompt<|im_end|>\n")
        for (msg in history) {
            sb.append("<|im_start|>${msg.role}\n${msg.content}<|im_end|>\n")
        }
        sb.append("<|im_start|>assistant\n")
        return sb.toString()
    }

    fun sendPrompt(userText: String) {
        // 🚫 1. กัน Spam: ถ้ากำลังคิดอยู่ ไม่ให้ส่งซ้ำ
        if (_chatState.value is ChatUiState.Generating) return
        val ctx = llamaContext ?: return

        // 🛡️ 2. ปลอดภัยจาก Race Condition: เพิ่ม User Message และ Update State ทันที
        val userMsgs = _messages.value.toMutableList()
        userMsgs.add(ChatMessage("user", userText))
        _messages.value = userMsgs

        viewModelScope.launch(Dispatchers.IO) {
            _chatState.value = ChatUiState.Generating("")
            val prompt = buildPrompt()
            var response = ""
            try {
                ctx.sendPrompt(prompt, temperature = 0.5f, topP = 0.9f, repeatPenalty = 1.2f, maxTokens = 256)
                for (token in ctx.getTokens()) {
                    if (token.contains("<|im_end|>") || token.contains("<|endoftext|>")) break
                    response += token
                    _chatState.value = ChatUiState.Generating(response)
                }
            } catch (e: Exception) { response = "น้องมลมีปัญหาค่ะพี่" }
            
            // ✨ ขัดเกลาข้อความ: ตัด Tag, Trim และจัดการบรรทัดว่าง
            val finalResponse = response.replace(Regex("<\\|.*?\\|>"), "")
                .replace(Regex("\n{3,}"), "\n\n")
                .trim()
            
            val safeResponse = if (finalResponse.isBlank()) "น้องมลยังตอบไม่ได้ค่ะพี่" else finalResponse

            // 🛡️ 3. ปลอดภัยจาก Race Condition: ดึง State ล่าสุดมาเพิ่ม Assistant Message
            val assistantMsgs = _messages.value.toMutableList()
            assistantMsgs.add(ChatMessage("assistant", safeResponse))
            _messages.value = assistantMsgs
            
            _chatState.value = ChatUiState.Idle
        }
    }
}
