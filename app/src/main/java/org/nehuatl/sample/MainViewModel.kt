package org.nehuatl.sample

import android.content.ContentResolver
import android.net.Uri
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

    private val _modelName = MutableStateFlow("ยังไม่ได้โหลด")
    val modelName: StateFlow<String> = _modelName

    private var ctx: Any? = null

    fun setModel(uriString: String, name: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _chatState.value = ChatUiState.LoadingModel
            try {
                closeContext()

                val file = File(filesDir, "model.gguf")
                if (file.exists()) file.delete()

                contentResolver.openInputStream(Uri.parse(uriString))?.use { input ->
                    FileOutputStream(file).use { output ->
                        input.copyTo(output)
                    }
                }

                val classNames = listOf(
                    "org.nehuatl.llamacpp.LLamaContext",
                    "org.nehuatl.llamacpp.LlamaContext"
                )

                val clazz = classNames.firstNotNullOfOrNull {
                    try { Class.forName(it) } catch (e: Exception) { null }
                } ?: throw Exception("Context class not found")

                val constructor = clazz.getConstructor(String::class.java)
                ctx = constructor.newInstance(file.absolutePath)

                _modelName.value = name
                _chatState.value = ChatUiState.Idle

            } catch (e: Exception) {
                _chatState.value = ChatUiState.Error(e.message ?: "Load Fail")
            }
        }
    }

    fun sendPrompt(text: String) {
        val currentCtx = ctx ?: return
        if (_chatState.value is ChatUiState.Generating) return

        _messages.value = _messages.value + ChatMessage("user", text)

        viewModelScope.launch(Dispatchers.IO) {
            _chatState.value = ChatUiState.Generating("")

            val history = _messages.value.takeLast(4)
            val prompt = history.joinToString("\n") { msg ->
                val role = if (msg.role == "user") "User" else "Assistant"
                role + ": " + msg.content
            } + "\nAssistant:"

            try {
                val method = currentCtx.javaClass.methods.firstOrNull {
                    it.name == "completion" || it.name == "sendPrompt"
                }

                val result: Any? = if (method != null) {
                    val paramCount = method.parameterTypes.size
                    when (paramCount) {
                        1 -> method.invoke(currentCtx, prompt)
                        2 -> {
                            val type = method.parameterTypes[1]
                            if (type == Int::class.javaPrimitiveType || type == Int::class.java) {
                                method.invoke(currentCtx, prompt, 128)
                            } else {
                                method.invoke(currentCtx, prompt, emptyMap<String, Any>())
                            }
                        }
                        else -> method.invoke(currentCtx, prompt)
                    }
                } else {
                    "No method found"
                }

                val textResult = result?.toString()?.trim() ?: "ไม่มีคำตอบ"

                _messages.value = _messages.value + ChatMessage("assistant", textResult)

            } catch (e: Exception) {
                _messages.value = _messages.value + ChatMessage("assistant", "Error: " + (e.message ?: "unknown"))
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
        } catch (_: Exception) {
        }
        ctx = null
    }

    override fun onCleared() {
        super.onCleared()
        closeContext()
    }
}
