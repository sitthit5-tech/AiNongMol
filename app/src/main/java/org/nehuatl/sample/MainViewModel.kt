package org.nehuatl.sample

import android.content.ContentResolver
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainViewModel(private val contentResolver: ContentResolver) : ViewModel() {
    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _chatState = MutableStateFlow<ChatUiState>(ChatUiState.Idle)
    val chatState: StateFlow<ChatUiState> = _chatState.asStateFlow()

    private val _modelName = MutableStateFlow("")
    val modelName: StateFlow<String> = _modelName.asStateFlow()

    fun setModel(uri: String, name: String) {
        _modelName.value = name
    }

    fun sendPrompt(userText: String) {
        if (_chatState.value is ChatUiState.Generating) return

        // 1. เพิ่มข้อความ User เข้าไปใน List
        val currentMsgs = _messages.value.toMutableList()
        currentMsgs.add(ChatMessage("user", userText))
        _messages.value = currentMsgs

        viewModelScope.launch {
            // 2. เปลี่ยนสถานะเป็น Generating (ส่งค่าว่างไปก่อน)
            _chatState.value = ChatUiState.Generating("")
            
            // 3. Mock การหน่วงเวลาของ AI
            delay(1000)
            val mockResponse = "น้องมลได้รับข้อความ '$userText' แล้วค่ะ! (ตอนนี้ Build ผ่านแล้ว เดี๋ยวเราค่อยเชื่อมสมอง AI กันนะคะพี่)"
            
            // 4. อัปเดตข้อความ Assistant
            val updatedMsgs = _messages.value.toMutableList()
            updatedMsgs.add(ChatMessage("assistant", mockResponse))
            _messages.value = updatedMsgs
            
            // 5. กลับสู่สถานะ Idle
            _chatState.value = ChatUiState.Idle
        }
    }
}
