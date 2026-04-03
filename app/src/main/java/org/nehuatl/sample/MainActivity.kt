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
        
        // ✅ ส่งทั้ง contentResolver และ filesDir เข้าไป
        viewModel = ViewModelProvider(this, object : ViewModelProvider.Factory {
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                return MainViewModel(contentResolver, filesDir) as T
            }
        })[MainViewModel::class.java]

        setContent {
            ChatScreen(
                viewModel = viewModel,
                onPickModel = { modelPicker.launch("*/*") }
            )
        }
    }
}
