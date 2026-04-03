package org.nehuatl.sample

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import java.lang.reflect.Modifier

class MainViewModel(
    private val contentResolver: android.content.ContentResolver,
    private val filesDir: File
) : ViewModel() {

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages

    private val _chatState = MutableStateFlow<ChatUiState>(ChatUiState.Idle)
    val chatState: StateFlow<ChatUiState> = _chatState

    private val _modelName = MutableStateFlow("สแกนหาทางเข้า...")
    val modelName: StateFlow<String> = _modelName

    fun setModel(uriString: String, name: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _chatState.value = ChatUiState.LoadingModel
            try {
                // 1. หาคลาสตัวจริง
                val clazz = listOf("org.nehuatl.llamacpp.LlamaContext", "org.nehuatl.llamacpp.LLamaContext")
                    .firstNotNullOfOrNull { try { Class.forName(it) } catch (e: Exception) { null } }
                    ?: throw Exception("Library not found")

                // 2. สแกนหา Static Method ทั้งหมด (ประตูข้าง)
                val methods = clazz.methods
                    .filter { Modifier.isStatic(it.modifiers) }
                    .joinToString("\n") { m -> 
                        "${m.name}(${m.parameterTypes.joinToString { it.simpleName }}) -> ${m.returnType.simpleName}"
                    }

                // 3. พ่นรายชื่อออกมาดู
                throw Exception("--- รายชื่อประตูข้าง ---\n$methods")

            } catch (e: Exception) {
                _modelName.value = "🔍 สแกนสำเร็จ"
                _chatState.value = ChatUiState.Error(e.message ?: "Unknown")
            }
        }
    }

    fun sendPrompt(text: String) {}
    fun stopGeneration() {}
}
