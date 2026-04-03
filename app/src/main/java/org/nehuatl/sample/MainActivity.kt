package org.nehuatl.sample

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class MainActivity : ComponentActivity() {
    private lateinit var viewModel: MainViewModel

    private val modelPicker = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { viewModel.setModel(it.toString(), "model.gguf") }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        viewModel = ViewModelProvider(this, object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return MainViewModel(contentResolver) as T
            }
        })[MainViewModel::class.java]

        setContent {
            // ✅ เรียกใช้ให้ตรงกับ Parameter ใน ChatScreen.kt
            ChatScreen(
                viewModel = viewModel,
                onPickModel = { modelPicker.launch("*/*") }
            )
        }
    }
}
