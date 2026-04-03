package org.nehuatl.sample

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// ✅ Import หัวใจสำคัญสำหรับ Property Delegate (by)
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState

@Composable
fun ChatScreen(viewModel: MainViewModel, onPickModel: () -> Unit) {
    // 🛡️ เชื่อมต่อ StateFlow จาก ViewModel
    val messages by viewModel.messages.collectAsState()
    val chatState by viewModel.chatState.collectAsState()
    val modelName by viewModel.modelName.collectAsState()
    val listState = rememberLazyListState()

    // Auto-scroll เมื่อมีข้อความใหม่ (เลื่อนไป Index 0 เพราะเราใช้ reverseLayout)
    LaunchedEffect(messages.size, chatState) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(0)
        }
    }

    Column(Modifier.fillMaxSize().background(Color(0xFF121212))) {
        // --- Top Bar ---
        Row(
            Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("🤖 NongMol AI", color = Color.White, style = MaterialTheme.typography.titleMedium)
            IconButton(onClick = onPickModel) { Text("📂", color = Color.White) }
        }

        // --- Chat Area ---
        if (modelName.isEmpty()) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text("กรุณาเลือกโมเดลก่อนใช้งานค่ะพี่ ✨", color = Color.Gray)
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f).padding(horizontal = 8.dp), 
                reverseLayout = true // ✅ กลับด้าน Layout เพื่อให้พิมพ์แล้วดันขึ้นจากล่าง
            ) {
                // ✅ ส่ง List ปกติเข้าไป (ไม่ต้อง .reversed() แล้ว) เพราะ reverseLayout จัดการให้แล้ว
                items(messages.asReversed()) { msg ->
                    ChatBubble(msg)
                }
            }
        }

        // --- Status Indicator ---
        if (chatState is ChatUiState.Generating) {
            Text("น้องมลกำลังคิด...", color = Color.Gray, modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp), style = MaterialTheme.typography.bodySmall)
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth().height(1.dp), color = Color(0xFF4CAF50))
        }

        ChatInputBar(enabled = modelName.isNotEmpty()) { viewModel.sendPrompt(it) }
    }
}

@Composable
fun ChatBubble(msg: ChatMessage) {
    val isUser = msg.role == "user"
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start) {
        Surface(
            color = if (isUser) Color(0xFF4CAF50) else Color(0xFF2A2A2A),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.padding(horizontal = 8.dp)
        ) {
            Text(msg.content, color = Color.White, modifier = Modifier.padding(12.dp))
        }
    }
}

@Composable
fun ChatInputBar(enabled: Boolean, onSend: (String) -> Unit) {
    var text by remember { mutableStateOf("") }
    Surface(color = Color(0xFF1E1E1E), tonalElevation = 2.dp) {
        Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            TextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier.weight(1f),
                enabled = enabled,
                placeholder = { Text("พิมพ์ข้อความหาพี่มล...", color = Color.Gray) },
                colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent)
            )
            IconButton(onClick = { if (text.isNotBlank()) { onSend(text); text = "" } }, enabled = enabled) {
                Icon(Icons.Default.Send, contentDescription = "Send", tint = if (enabled) Color(0xFF4CAF50) else Color.Gray)
            }
        }
    }
}
