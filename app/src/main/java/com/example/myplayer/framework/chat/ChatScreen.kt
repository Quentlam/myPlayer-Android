package com.example.myplayer.framework.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import okhttp3.*
import java.util.concurrent.TimeUnit

// 在Friend数据类定义之后添加：
private val okHttpClient by lazy {
    OkHttpClient.Builder()
        .readTimeout(3, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()
}

class WebSocketManager(private val url: String) {
    private var webSocket: WebSocket? = null

    fun connect(listener: WebSocketListener) {
        val request = Request.Builder()
            .url(url)
            .build()
        webSocket = okHttpClient.newWebSocket(request, listener)
    }

    fun sendMessage(message: String) {
        webSocket?.send(message)
    }

    fun disconnect() {
        webSocket?.close(1000, "Normal closure")
    }
}

// 好友数据类
data class Friend(
    val id: String,
    val name: String,
    val avatarUrl: String = ""
)

// 界面状态管理
enum class ChatScreenState {
    FRIEND_LIST, CHAT_DETAIL
}

@Composable
fun ChatScreen() {
    var currentScreen by remember { mutableStateOf(ChatScreenState.FRIEND_LIST) }
    var selectedFriend by remember { mutableStateOf<Friend?>(null) }

    when (currentScreen) {
        ChatScreenState.FRIEND_LIST -> {
            FriendListView(
                friends = remember { generateSampleFriends() },
                onFriendClick = { friend ->
                    selectedFriend = friend
                    currentScreen = ChatScreenState.CHAT_DETAIL
                }
            )
        }
        ChatScreenState.CHAT_DETAIL -> {
            ChatDetailScreen(
                friend = selectedFriend,
                onBack = { currentScreen = ChatScreenState.FRIEND_LIST }
            )
        }
    }
}

// 新增数据类，用于存储聊天消息和时间
data class ChatMessage(
    val content: String,
    val isMyMessage: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChatDetailScreen(
    friend: Friend?,
    onBack: () -> Unit
) {
    var message by remember { mutableStateOf("") }
    var chatMessages by remember {
        mutableStateOf(
            mutableListOf(
                ChatMessage("你好", isMyMessage = true),
                ChatMessage("你好", isMyMessage = false),
                ChatMessage("最近怎么样？", isMyMessage = true),
                ChatMessage("我挺好的，谢谢关心。", isMyMessage = false),
                ChatMessage("你呢？", isMyMessage = false),
                ChatMessage("我过的很开心，希望我们一直开心。", isMyMessage = true)
            )
        )
    }

    val listState = rememberLazyListState()

    // 将 LaunchedEffect 移到 @Composable 函数的顶层
    LaunchedEffect(chatMessages) {
        listState.animateScrollToItem(chatMessages.size - 1)
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(friend?.name ?: "聊天") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        },
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextField(
                    value = message,
                    onValueChange = { message = it },
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp),
                    placeholder = { Text("输入消息") }
                )
                Button(
                    onClick = {
                        if (message.isNotBlank()) {
                            chatMessages = chatMessages.toMutableList().apply {
                                add(ChatMessage(message, isMyMessage = true))
                            }
                            message = ""
                            // 这里可以添加发送消息到 WebSocket 的逻辑
                        }
                    }
                ) {
                    Text("发送")
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            LazyColumn(
                modifier = Modifier.weight(1f),
                reverseLayout = false,
                state = listState
            ) {
                val sortedMessages = chatMessages.sortedBy { it.timestamp }
                items(sortedMessages) { msg ->
                    ChatBubble(
                        message = msg.content,
                        isMyMessage = msg.isMyMessage,
                        timestamp = msg.timestamp
                    )
                }
            }
        }
    }
}

@Composable
private fun ChatBubble(
    message: String,
    isMyMessage: Boolean,
    timestamp: Long
) {
    // 转换时间戳为可读格式，这里简单示例，实际可根据需求调整
    val timeText = java.text.SimpleDateFormat("HH:mm").format(java.util.Date(timestamp))
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        horizontalAlignment = if (isMyMessage) Alignment.End else Alignment.Start
    ) {
        Box(
            modifier = Modifier
                .background(
                    color = if (isMyMessage) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer,
                    shape = if (isMyMessage) {
                        CircleShape.copy(bottomEnd = CornerSize(0.dp))
                    } else {
                        CircleShape.copy(bottomStart = CornerSize(0.dp))
                    }
                )
                .padding(12.dp)
        ) {
            Text(
                text = message,
                color = if (isMyMessage) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
        Text(
            text = timeText,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.outline,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

@Composable
private fun FriendListView(
    friends: List<Friend>,
    onFriendClick: (Friend) -> Unit
) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(friends) { friend ->
            FriendListItem(friend = friend, onClick = { onFriendClick(friend) })
        }
    }
}

@Composable
private fun FriendListItem(friend: Friend, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 头像占位（实际项目应使用Coil/Glide加载图片）
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(MaterialTheme.colorScheme.primary, CircleShape)
        )
        Spacer(Modifier.width(16.dp))
        Text(text = friend.name, style = MaterialTheme.typography.bodyLarge)
    }
}

// 生成示例好友数据
private fun generateSampleFriends(): List<Friend> {
    return listOf(
        Friend("1", "张三"),
        Friend("2", "李四"),
        Friend("3", "王五"),
        Friend("4", "赵六"),
        Friend("5", "陈七"),
        Friend("6", "林八"),
        Friend("7", "周九"),
        Friend("8", "吴十"),
        Friend("9", "黄十一"),
        Friend("10", "郑十二"),
        // 新增以下好友
        Friend("11", "孙十三"),
        Friend("12", "朱十四"),
        Friend("13", "马十五"),
        Friend("14", "胡十六"),
        Friend("15", "郭十七"),
        Friend("16", "何十八"),
        Friend("17", "高十九"),
        Friend("18", "罗二十"),
        Friend("19", "梁二十一"),
        Friend("20", "宋二十二")
    )
}