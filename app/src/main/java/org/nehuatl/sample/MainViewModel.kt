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

    private val _modelName = MutableStateFlow("โหมดสืบสวน")
    val modelName: StateFlow<String> = _modelName

    fun setModel(uriString: String, name: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _chatState.value = ChatUiState.LoadingModel
            try {
                // ค้นหา Class ที่มีอยู่จริง
                val clazz = listOf("org.nehuatl.llamacpp.LlamaContext", "org.nehuatl.llamacpp.LLamaContext")
                    .firstNotNullOfOrNull { try { Class.forName(it) } catch (e: Exception) { null } }
                    ?: throw Exception("หา Library ไม่เจอ")

                // 🔍 ส่อง Constructor
                val ctors = clazz.constructors.joinToString("\n") { c -> 
                    "Ctor: (${c.parameterTypes.joinToString { it.simpleName }})"
                }

                // 🔍 ส่อง Method
                val methods = clazz.methods
                    .filter { it.declaringClass == clazz }
                    .joinToString("\n") { m -> 
                        "Method: ${m.name}(${m.parameterTypes.joinToString { it.simpleName }})"
                    }

                // พ่นข้อมูลออกมาที่หน้าจอ Error เพื่อให้พี่อ่าน
                throw Exception("--- พบโครงสร้างดังนี้ ---\n$ctors\n$methods")

            } catch (e: Exception) {
                _modelName.value = "❌ ข้อมูล API"
                _chatState.value = ChatUiState.Error(e.message ?: "Unknown Error")
            }
        }
    }

    fun sendPrompt(text: String) {}
    fun stopGeneration() {}
}
