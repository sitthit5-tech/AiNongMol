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
    private var currentModelFile: File? = null // 📂 ไว้ลบไฟล์เก่ากัน Storage บวม
    
    private val systemPrompt = "คุณคือ 'น้องมล' AI ภาษาไทยที่ฉลาด ตอบสั้น สุภาพ แทนตัวว่าน้องมล และเรียกผู้ใช้ว่าพี่"

    fun setModel(uriString: String, name: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _chatState.value = ChatUiState.LoadingModel
            try {
                // 🧹 ลบไฟล์โมเดลเก่าก่อนโหลดใหม่
                currentModelFile?.delete()
                
                val uri = Uri.parse(uriString)
                val tempFile = File.createTempFile("model_", ".gguf")
                contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(tempFile).use { output -> input.copyTo(output) }
                }
                
                llamaContext = LLamaContext(tempFile.absolutePath)
                currentModelFile = tempFile // เก็บ reference ไว้ลบครั้งหน้า
                _modelName.value = name
                _chatState.value = ChatUiState.Idle
            } catch (e: Exception) { _chatState.value = ChatUiState.Idle }
        }
    }

    private fun buildPrompt(): String {
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
        if (_chatState.value is ChatUiState.Generating) return
        
        // 🛑 1. กันโมเดลว่าง: แจ้งเตือน User ทันทีถ้ายังไม่เลือกโมเดล
        if (llamaContext == null) {
            val msgs = _messages.value.toMutableList()
            msgs.add(ChatMessage("assistant", "กรุณาเลือกโมเดลก่อนนะคะพี่ น้องมลยังไม่พร้อมคุยค่ะ ✨"))
            _messages.value = msgs
            return
        }

        val userMsgs = _messages.value.toMutableList()
        userMsgs.add(ChatMessage("user", userText))
        _messages.value = userMsgs

        viewModelScope.launch(Dispatchers.IO) {
            _chatState.value = ChatUiState.Generating("")
            val prompt = buildPrompt()
            var response = ""
            var tokenCount = 0 // 🛑 2. Hard Stop Token

            try {
                val ctx = llamaContext!!
                ctx.sendPrompt(prompt, temperature = 0.5f, topP = 0.9f, repeatPenalty = 1.2f, maxTokens = 300)
                
                for (token in ctx.getTokens()) {
                    tokenCount++
                    // 🛡️ กันโมเดลรั่ว หรือตอบยาวเกินความจำเป็น
                    if (tokenCount > 300) break
                    if (token.contains("<|im_end|>") || token.contains("<|endoftext|>")) break
                    
                    response += token
                    _chatState.value = ChatUiState.Generating(response)
                }
            } catch (e: Exception) { response = "น้องมลมีปัญหาค่ะพี่" }
            
            // ✨ 3. Bonus: ขัดเกลาช่องว่างหัวประโยคและบรรทัดว่าง
            val safeResponse = response.replace(Regex("<\\|.*?\\|>"), "")
                .replace(Regex("^\\s+"), "") // ตัดช่องว่างหัวประโยค
                .replace(Regex("\n{3,}"), "\n\n")
                .trim()
                .ifBlank { "น้องมลยังตอบไม่ได้ค่ะพี่" }

            val assistantMsgs = _messages.value.toMutableList()
            assistantMsgs.add(ChatMessage("assistant", safeResponse))
            _messages.value = assistantMsgs
            _chatState.value = ChatUiState.Idle
        }
    }
}
