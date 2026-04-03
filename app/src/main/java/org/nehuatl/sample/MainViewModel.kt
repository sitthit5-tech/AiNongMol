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

    private val _modelName = MutableStateFlow("สแกนหา Factory Method")
    val modelName: StateFlow<String> = _modelName

    fun setModel(uriString: String, name: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _chatState.value = ChatUiState.LoadingModel
            try {
                val clazz = listOf("org.nehuatl.llamacpp.LlamaContext", "org.nehuatl.llamacpp.LLamaContext")
                    .firstNotNullOfOrNull { try { Class.forName(it) } catch (e: Exception) { null } }
                    ?: throw Exception("Library not found")

                // 🔍 สแกนหา Static Method ที่คืนค่าเป็นคลาสตัวมันเอง
                val factoryMethods = clazz.methods
                    .filter { Modifier.isStatic(it.modifiers) && (it.returnType == clazz || it.returnType.name.contains("LlamaContext")) }
                    .joinToString("\n") { m -> 
                        "✨ ${m.name}(${m.parameterTypes.joinToString { it.simpleName }})"
                    }

                // 🔍 สแกนหา Method อื่น ๆ ที่น่าสงสัย
                val otherMethods = clazz.methods
                    .filter { Modifier.isStatic(it.modifiers) && it.name.lowercase().let { n -> n.contains("create") || n.contains("load") || n.contains("init") } }
                    .joinToString("\n") { m -> 
                        "🔍 ${m.name} -> ${m.returnType.simpleName}"
                    }

                throw Exception("--- พบช่องทางเข้า ---\n$factoryMethods\n$otherMethods")

            } catch (e: Exception) {
                _modelName.value = "❌ พบช่องทางแล้ว"
                _chatState.value = ChatUiState.Error(e.message ?: "Unknown Error")
            }
        }
    }

    fun sendPrompt(text: String) {}
    fun stopGeneration() {}
}
