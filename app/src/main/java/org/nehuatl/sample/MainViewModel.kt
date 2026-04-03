package org.nehuatl.sample

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File

class MainViewModel(
    private val contentResolver: android.content.ContentResolver,
    private val filesDir: File
) : ViewModel() {

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages

    private val _chatState = MutableStateFlow<ChatUiState>(ChatUiState.Idle)
    val chatState: StateFlow<ChatUiState> = _chatState

    private val _modelName = MutableStateFlow("โหมดตรวจสอบ API")
    val modelName: StateFlow<String> = _modelName

    fun setModel(uriString: String, name: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _chatState.value = ChatUiState.LoadingModel
            try {
                // 🔍 1. หาคลาสตัวจริง
                val clazz = listOf("org.nehuatl.llamacpp.LlamaContext", "org.nehuatl.llamacpp.LLamaContext")
                    .firstNotNullOfOrNull { try { Class.forName(it) } catch (e: Exception) { null } }
                    ?: throw Exception("Library not found")

                // 🔍 2. ส่อง Constructor ทั้งหมด
                val ctors = clazz.constructors.joinToString("\n") { c -> 
                    "Ctor: (${c.parameterTypes.joinToString { it.simpleName }})"
                }

                // 🔍 3. ส่อง Static Methods (พวกฟังก์ชันที่ใช้ Load)
                val methods = clazz.methods
                    .filter { java.lang.reflect.Modifier.isStatic(it.modifiers) }
                    .joinToString("\n") { m -> 
                        "Static: ${m.name}(${m.parameterTypes.joinToString { it.simpleName }})"
                    }

                // พ่นออกมาบนหน้าจอ Error ของแอป
                throw Exception("--- ข้อมูล API ---\n$ctors\n$methods")

            } catch (e: Exception) {
                _modelName.value = "❌ ตรวจสอบสำเร็จ"
                _chatState.value = ChatUiState.Error(e.message ?: "Unknown")
            }
        }
    }

    fun sendPrompt(text: String) {}
    fun stopGeneration() {}
}
