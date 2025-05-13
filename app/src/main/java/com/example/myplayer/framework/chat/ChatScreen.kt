package com.example.myplayer.framework.chat

import android.annotation.SuppressLint
import android.util.Log
import androidx.compose.foundation.Image
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.example.myplayer.model.BaseResponseJsonData
import com.example.myplayer.model.UserInfo
import com.example.myplayer.network.networkAPI.GetRequest
import com.example.myplayer.userInfo
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.layout.ContentScale
import coil.compose.rememberImagePainter
import com.example.myplayer.webSocketManager
import org.json.JSONObject

val TAG = "ChatScreen"

// 界面状态管理
enum class ChatScreenState {
    FRIEND_LIST, CHAT_DETAIL
}

// 使用可观察的StateList
val chatMessagesMap = mutableStateMapOf<String, SnapshotStateList<ChatMessage>>()

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
                onBack = { currentScreen = ChatScreenState.FRIEND_LIST },
                onMessageSent = { it ->
                   val wsComStr = JSONObject().apply {
                       put("system", false)
                       put("group", false)
                       put("message", true)
                       put("sender", userInfo.u_id) // 发送者ID
                       put("sender_name", userInfo.u_name) // 发送者姓名
                       put("target", selectedFriend?.u_id) // 接收者ID
                       put("content", it)
                       put("time", "2025/5/13 00:00:00") // 时间戳
                   }.toString()
                   webSocketManager?.sendMessage(wsComStr)

                    // 新增消息到本地聊天记录
                    selectedFriend?.u_id?.let { friendId ->
                        val messages = chatMessagesMap.getOrPut(friendId) { mutableStateListOf() }
                        messages.add(ChatMessage(
                            content = it,
                            isMyMessage = true
                        ))
                    }
                }
            )
        }
    }
}

// 新增数据类，用于存储聊天消息和时间
data class ChatMessage(
    val content: String,
    val isMyMessage: Boolean
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChatDetailScreen(
    friend: UserInfo?,
    onBack: () -> Unit,
    onMessageSent: (String) -> Unit  // 新增回调
) {
    var message by remember { mutableStateOf("") }
    var chatMessages = chatMessagesMap[friend?.u_id]?: mutableListOf()
    Log.d(TAG, chatMessages.toString())
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
                            onMessageSent(message)  // 改用回调处理
                            message = ""
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
                items(chatMessages) { msg ->
                    ChatBubble(
                        message = msg.content,
                        isMyMessage = msg.isMyMessage
                    )
                }
            }
        }
    }
}

@Composable
private fun ChatBubble(
    message: String,
    isMyMessage: Boolean
) {
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
private fun FriendListItem(friend: UserInfo, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = rememberImagePainter(
                data = friend.u_avatar,
                builder = {
                    crossfade(true)
                }
            ),
            contentDescription = "用户头像",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
        )
        Spacer(Modifier.width(16.dp))
        Text(text = friend.u_name, style = MaterialTheme.typography.bodyLarge)
    }
}

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