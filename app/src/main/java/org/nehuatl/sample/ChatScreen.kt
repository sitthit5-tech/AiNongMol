package org.nehuatl.sample

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(viewModel: MainViewModel, onPickModel: () -> Unit) {
    val messages by viewModel.messages.collectAsState()
    val chatState by viewModel.chatState.collectAsState()
    val modelName by viewModel.modelName.collectAsState()
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(0)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text("น้องมล AI", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        if (modelName.isNotEmpty()) {
                            Text(modelName, fontSize = 10.sp, color = Color.Gray)
                        }
                    }
                },
                actions = {
                    Button(
                        onClick = onPickModel,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF333333)),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Text("📂 เลือกโมเดล", fontSize = 12.sp)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF1A1A1A), titleContentColor = Color.White)
            )
        },
        containerColor = Color(0xFF121212)
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
                reverseLayout = true
            ) {
                items(messages.asReversed()) { msg ->
                    ChatBubble(msg)
                }
            }

            if (chatState is ChatUiState.LoadingModel) {
                LinearProgressIndicator(Modifier.fillMaxWidth(), color = Color.Cyan)
            }
            
            if (chatState is ChatUiState.Generating) {
                Text("น้องมลกำลังคิด...", color = Color.Gray, modifier = Modifier.padding(16.dp), fontSize = 12.sp)
            }

            ChatInputBar(enabled = modelName.isNotEmpty()) { viewModel.sendPrompt(it) }
        }
    }
}

@Composable
fun ChatBubble(msg: ChatMessage) {
    val isUser = msg.role == "user"
    Row(
        Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            color = if (isUser) Color(0xFF007AFF) else Color(0xFF2C2C2E),
            shape = RoundedCornerShape(
                topStart = 16.dp, topEnd = 16.dp,
                bottomStart = if (isUser) 16.dp else 2.dp,
                bottomEnd = if (isUser) 2.dp else 16.dp
            ),
            modifier = Modifier.widthIn(max = 300.dp)
        ) {
            Text(msg.content, color = Color.White, modifier = Modifier.padding(12.dp), fontSize = 15.sp)
        }
    }
}

@Composable
fun ChatInputBar(enabled: Boolean, onSend: (String) -> Unit) {
    var text by remember { mutableStateOf("") }
    Surface(color = Color(0xFF1C1C1E), modifier = Modifier.fillMaxWidth()) {
        Row(
            Modifier.padding(12.dp).navigationBarsPadding().imePadding(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier.weight(1f),
                enabled = enabled,
                placeholder = { Text("ถามอะไรหน่อยค่ะพี่...", color = Color.Gray) },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                )
            )
            IconButton(
                onClick = { if (text.isNotBlank()) { onSend(text); text = "" } },
                enabled = enabled,
                modifier = Modifier.background(if (enabled) Color(0xFF007AFF) else Color.DarkGray, CircleShape)
            ) {
                Icon(Icons.Default.Send, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
            }
        }
    }
}
