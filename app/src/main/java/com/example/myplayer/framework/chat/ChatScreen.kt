package com.example.myplayer.framework.chat

import android.annotation.SuppressLint
import android.util.Log
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
import com.example.myplayer.WebSocketManager
import com.example.myplayer.model.BaseResponseJsonData
import com.example.myplayer.model.UserInfo
import com.example.myplayer.model.WebSocketResponse
import com.example.myplayer.network.networkAPI.GetRequest
import com.example.myplayer.userInfo
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.*
import java.util.concurrent.TimeUnit

val TAG = "ChatScreen"

// 界面状态管理
enum class ChatScreenState {
    FRIEND_LIST, CHAT_DETAIL
}

@SuppressLint("CoroutineCreationDuringComposition")
@Composable
fun ChatScreen() {
    var currentScreen by remember { mutableStateOf(ChatScreenState.FRIEND_LIST) }
    var selectedFriend by remember { mutableStateOf<UserInfo?>(null) } // 修改类型
    val coroutineScope = rememberCoroutineScope()
    coroutineScope.launch {
        withContext(Dispatchers.IO){
            getFriendList(coroutineScope)
        }
    }

    when (currentScreen) {
        ChatScreenState.FRIEND_LIST -> {
            FriendListView(
                friends = userInfo.friendList, // 使用真实数据
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
    friend: UserInfo?,
    onBack: () -> Unit
) {
    var message by remember { mutableStateOf("") }
    var chatMessages by remember {
        mutableStateOf(
            mutableListOf<ChatMessage>()
        )
    }

    val webSocketManager = WebSocketManager("wss://www.myplayer.merlin.xin/online?u_id=${userInfo.u_id}&u_name=${userInfo.u_name}")
    val listener = object : WebSocketListener() {
        override fun onMessage(webSocket: WebSocket, text: String) {

            val type = object : TypeToken<WebSocketResponse>() {}.type
            val data = Gson().fromJson<WebSocketResponse>(text, type)

            if(data.sender == friend?.u_id){ //如果发送者是选择的好友
                chatMessages = chatMessages.toMutableList().apply {
                    add(ChatMessage(data.content, false))
                }
            }
            else if(data.sender == userInfo?.u_id){
                chatMessages = chatMessages.toMutableList().apply {
                    add(ChatMessage(data.content, true))
                }
            }
            Log.d(TAG, data.toString())
        }
    }
    webSocketManager.connect(listener)

    val listState = rememberLazyListState()

    // 将 LaunchedEffect 移到 @Composable 函数的顶层
    LaunchedEffect(chatMessages) {
        if(chatMessages.size - 1 > 0){
            listState.animateScrollToItem(chatMessages.size - 1)
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(friend?.u_name ?: "聊天") },
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
                            webSocketManager.sendMessage(message)
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
    friends: List<UserInfo>, // 修改参数类型
    onFriendClick: (UserInfo) -> Unit // 修改参数类型
) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(friends) { friend ->
            FriendListItem(friend = friend, onClick = { onFriendClick(friend) })
        }
    }
}

@Composable
private fun FriendListItem(friend: UserInfo, onClick: () -> Unit) { // 修改参数类型
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(MaterialTheme.colorScheme.primary, CircleShape)
        )
        Spacer(Modifier.width(16.dp))
        Text(text = friend.u_name, style = MaterialTheme.typography.bodyLarge) // 使用u_name
    }
}

// 删除原有的generateSampleFriends方法
// private fun generateSampleFriends(): List<Friend> {
//     return listOf(...)
// }

suspend fun getFriendList(coroutineScope: CoroutineScope){
    try {
        val response = GetRequest(
            interfaceName = "/friend/getfriends",
            queryParams = mapOf()
        ).execute(coroutineScope)
        val type = object : TypeToken<BaseResponseJsonData<List<UserInfo>>>() {}.type
        val data = Gson().fromJson<BaseResponseJsonData<List<UserInfo>>>(response.body?.string(), type)

        if (data.data != null) {
            userInfo.friendList = data.data.also { 
                Log.d(TAG, "好友列表更新：${it.size}条记录")
            }
            // 若需要多属性设置才使用apply：
            /*
            userInfo.apply {
                friendList = data.data
                version++
            }
            */
            Log.d(TAG, "好友列表详情：\n${userInfo.friendList?.joinToString("\n") { 
                "好友ID：${it.u_id} 姓名：${it.u_name} 头像：${it.u_avatar}"
            } ?: "空列表"}")
        } else {
            Log.e(TAG, "获取好友列表失败：${data.msg}")
        }
    } catch (e: Exception) {
        Log.e(TAG, "获取好友列表异常：${e.message}")
        throw e
    }
}