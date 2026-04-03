package org.nehuatl.sample

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class MainActivity : ComponentActivity() {
    private lateinit var viewModel: MainViewModel

    private val modelPicker = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { viewModel.setModel(it.toString(), it.lastPathSegment ?: "model.gguf") }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // ✅ ปรับ Factory ให้รองรับ Type Checking แบบที่ Gradle ชอบ
        val factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
                    @Suppress("UNCHECKED_CAST")
                    return MainViewModel(contentResolver, filesDir) as T
                }
                throw IllegalArgumentException("Unknown ViewModel class")
            }
        }
        
        viewModel = ViewModelProvider(this, factory)[MainViewModel::class.java]

        setContent {
            ChatScreen(
                viewModel = viewModel,
                onPickModel = { modelPicker.launch("*/*") }
            )
        }
    }
}
