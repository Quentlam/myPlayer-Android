package com.example.myplayer.framework.chat

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.sp
import coil.compose.rememberImagePainter
import com.example.myplayer.userInfo.isConnected
import com.example.myplayer.webSocketManager
import org.json.JSONObject
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import androidx.compose.material3.Text
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.myplayer.model.DatabaseProvider
import com.example.myplayer.model.chat.ChatMessage
import com.example.myplayer.userInfo.u_id
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow


val TAG = "ChatScreen"

// 界面状态管理
enum class ChatScreenState {
    FRIEND_LIST, CHAT_DETAIL
}

// 使用可观察的StateList
val chatMessagesMap = mutableStateMapOf<String, SnapshotStateList<ChatMessage>>()

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
@SuppressLint("CoroutineCreationDuringComposition", "UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun ChatScreen() {
    var currentScreen by remember { mutableStateOf(ChatScreenState.FRIEND_LIST) }
    var selectedFriend by remember { mutableStateOf<UserInfo?>(null) } // 修改类型
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    coroutineScope.launch {
        withContext(Dispatchers.IO){
            getFriendList(coroutineScope)
        }
    }

    when (currentScreen) {
        ChatScreenState.FRIEND_LIST -> {
            // isRefreshing 状态放在外层，记得state要记住
            var isRefreshing by remember { mutableStateOf(false) }

            val pullRefreshState = rememberPullRefreshState(
                refreshing = isRefreshing,
                onRefresh = {//刷新逻辑
                    coroutineScope.launch {
                        isRefreshing = true
                        try {
                            getFriendList(coroutineScope)
                            Toast.makeText(context, "刷新好友列表成功！", Toast.LENGTH_SHORT).show()
                        } catch (e: Exception) {
                            isRefreshing = false
                        } finally {
                            isRefreshing = false
                        }
                    }
                }
            )

            Scaffold(
                topBar = {
                    CenterAlignedTopAppBar(
                        title = { Text("myplayer") }
                    )
                }
            ) { paddingValues ->  // Scaffold 内容槽的默认参数，用来处理系统/状态栏等间距
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .pullRefresh(pullRefreshState)  // 下拉刷新的modifier放这里
                ) {
                    FriendListView(
                        friends = userInfo.friendList,
                        onFriendClick = { friend ->
                            selectedFriend = friend
                            currentScreen = ChatScreenState.CHAT_DETAIL
                        },
                        modifier = Modifier.fillMaxSize()
                    )

                    PullRefreshIndicator(
                        refreshing = isRefreshing,
                        state = pullRefreshState,
                        modifier = Modifier.align(Alignment.TopCenter)
                    )
                }
            }

        }
        ChatScreenState.CHAT_DETAIL -> {
            ChatDetailScreen(
                friend = selectedFriend,
                onBack = { currentScreen = ChatScreenState.FRIEND_LIST },
                onMessageSent = { it ->
                    try {
                        val wsComStr = JSONObject().apply {
                            put("system", false)
                            put("group", false)
                            put("message", true)
                            put("sender", userInfo.u_id) // 发送者ID
                            put("sender_name", userInfo.u_name) // 发送者姓名
                            put("target", selectedFriend?.u_id) // 接收者ID
                            put("content", it)
                            put("time", ZonedDateTime.now(ZoneId.of("Asia/Shanghai"))
                                .format(DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss"))) // 使用上海时区
                        }.toString()

                        val isSent : Boolean? = webSocketManager?.sendMessage(wsComStr)


                        if(isSent == true && isConnected == true) {
                            CoroutineScope(Dispatchers.Main).launch {
                                saveChatMessage(
                                    context,
                                    ChatMessage(
                                        chat_id = 0,
                                        sender_id = userInfo.u_id,
                                        accpet_id = selectedFriend!!.u_id,
                                        content = it,
                                        isMyMessage = true,
                                        isSent = true,
                                        time = ZonedDateTime.now(ZoneId.of("Asia/Shanghai"))
                                            .format(DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss")),
                                    )
                                )
                            }
                            true
                        }
                        else{
                            // 新增消息到本地聊天记录
                            selectedFriend?.u_id?.let { friendId ->
                                val messages = chatMessagesMap.getOrPut(friendId) { mutableStateListOf() }
                                messages.add(
                                    ChatMessage(
                                        chat_id = 0,
                                        sender_id = userInfo.u_id,
                                        accpet_id = selectedFriend!!.u_id,
                                        content = it,
                                        isMyMessage = true,
                                        isSent = true,
                                        time = ZonedDateTime.now(ZoneId.of("Asia/Shanghai"))
                                            .format(DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss")),
                                    )
                                )
                            }
                            Toast.makeText(context, "信息发送失败！", Toast.LENGTH_SHORT).show()
                            false
                        }
                    }
                    catch (e :Exception)
                    {
                        Toast.makeText(context, "信息发送异常！${e.message}", Toast.LENGTH_SHORT).show()
                        false
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("UnrememberedMutableState")
@Composable
private fun ChatDetailScreen(
    friend: UserInfo?,
    onBack: () -> Unit,
    onMessageSent: (String) -> Boolean
) {
    var message by remember { mutableStateOf("") }
    val context = LocalContext.current
// 将 Flow 转为 State<List<ChatMessage>>

    val chatMessages by getChatMessageById(context,userInfo.u_id,friend!!.u_id)
        .collectAsStateWithLifecycle(initialValue = emptyList())//直接获取Flow并且转换为Status


    val listState = rememberLazyListState()

    LaunchedEffect(chatMessages.size) {
        if (chatMessages.isNotEmpty()) {
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
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            LazyColumn(
                modifier = Modifier.weight(1f),
                state = listState
            ) {
                items(chatMessages) { msg ->
                    val isMyMessage = msg.sender_id == userInfo.u_id
                    if (msg.isSent == true) {
                        msg.content?.let { ChatBubble(message = it, isMyMessage = isMyMessage) }
                    } else {
                        msg.content?.let { PendingChatBubble(message = it, isMyMessage = isMyMessage) }
                    }
                }
            }
        }
    }
}



@Composable
fun ChatBubble(
    message: String,
    isMyMessage: Boolean
) {
    // 气泡颜色
    val bubbleColor = if (isMyMessage)
        MaterialTheme.colorScheme.primaryContainer
    else
        MaterialTheme.colorScheme.secondaryContainer

    // 文字颜色
    val textColor = if (isMyMessage)
        MaterialTheme.colorScheme.onPrimaryContainer
    else
        MaterialTheme.colorScheme.onSecondaryContainer

    // 气泡形状，四个角分开设置，避免圆度过大
    val shape = if (isMyMessage) {
        RoundedCornerShape(
            topStart = 12.dp,
            topEnd = 12.dp,
            bottomStart = 12.dp,
            bottomEnd = 0.dp  // 尖角在右下
        )
    } else {
        RoundedCornerShape(
            topStart = 12.dp,
            topEnd = 12.dp,
            bottomStart = 0.dp,  // 尖角在左下
            bottomEnd = 12.dp
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalAlignment = if (isMyMessage) Alignment.End else Alignment.Start
    ) {
        Box(
            modifier = Modifier
                .background(color = bubbleColor, shape = shape)
                .padding(horizontal = 14.dp, vertical = 10.dp)
                .widthIn(max = 280.dp)  // 最大宽度限制，防止过宽
        ) {
            Text(
                text = message,
                color = textColor,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
fun PendingChatBubble(
    message: String,
    isMyMessage: Boolean  // 这里也传入以决定气泡左右位置
) {
    val bubbleColor = if (isMyMessage)
        MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f) // 警示色淡色气泡
    else
        MaterialTheme.colorScheme.secondaryContainer

    val textColor = if (isMyMessage)
        MaterialTheme.colorScheme.onErrorContainer
    else
        MaterialTheme.colorScheme.onSecondaryContainer

    val shape = if (isMyMessage) {
        RoundedCornerShape(
            topStart = 12.dp,
            topEnd = 12.dp,
            bottomStart = 12.dp,
            bottomEnd = 0.dp
        )
    } else {
        RoundedCornerShape(
            topStart = 12.dp,
            topEnd = 12.dp,
            bottomStart = 0.dp,
            bottomEnd = 12.dp
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalAlignment = if (isMyMessage) Alignment.End else Alignment.Start
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(0.8f), // 限制Row最大宽度，防止气泡太宽
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = if (isMyMessage) Arrangement.End else Arrangement.Start,
            // 感叹号总是放在消息体左边，顺序不变
        ) {
            // 感叹号
//            Text(
//                text = "⚠️",
//                color = MaterialTheme.colorScheme.error,
//                fontSize = 16.sp,
//                modifier = Modifier.padding(bottom = 2.dp)
//            )
            Icon(
                imageVector = Icons.Default.Error,
                contentDescription = "错误",
                tint = MaterialTheme.colorScheme.error, // 一般红色
                modifier = Modifier.size(16.dp)
            )


            Spacer(modifier = Modifier.width(6.dp))

            Box(
                modifier = Modifier
                    .background(color = bubbleColor, shape = shape)
                    .padding(horizontal = 14.dp, vertical = 10.dp)
                    .widthIn(max = 280.dp),
            ) {
                Text(
                    text = message,
                    color = textColor,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}




@Composable
private fun FriendListView(
    friends: List<UserInfo>, // 修改参数类型
    onFriendClick: (UserInfo) -> Unit, // 修改参数类型
    modifier: Modifier
) {
    LazyColumn(modifier = modifier) {
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

fun getChatMessageById(context : Context, u_id : String,friendId : String) : Flow<List<ChatMessage>>
{
    try {
        val dao = DatabaseProvider.getChatMessageDatabase(context).chatMessageDao()
        Log.d("ChatMessage","获取当前房间的弹幕信息成功！")
        return dao.getUserMessageById(friendId,u_id)
    }
    catch (e : Exception)
    {
        Log.e("ChatMessage","弹幕消息存储失败！${e.message}")
        return emptyFlow()
    }
}