package com.example.myplayer.framework.chat

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.rememberAsyncImagePainter
import com.example.myplayer.model.DatabaseProvider
import com.example.myplayer.model.chat.ChatMessage
import com.example.myplayer.userInfo.u_id
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.text.style.TextAlign


val TAG = "ChatScreen"

// 界面状态管理
enum class ChatScreenState {
    FRIEND_LIST, CHAT_DETAIL
}

// 使用可观察的StateList
val chatMessagesMap = mutableStateMapOf<String, SnapshotStateList<ChatMessage>>()

// ChatScreen 中新增刷新状态
@OptIn(ExperimentalMaterialApi::class, ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun ChatScreen(
    onEnterChatDetialScreen: () -> Unit,
    onExitChatDetialScreen: () -> Unit
) {
    var currentScreen by remember { mutableStateOf(ChatScreenState.FRIEND_LIST) }
    var selectedFriend by remember { mutableStateOf<UserInfo?>(null) }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current.applicationContext

    // 新增刷新状态
    var isRefreshing by remember { mutableStateOf(false) }
    // 下拉刷新状态
    val pullRefreshState = rememberPullRefreshState(isRefreshing, {
        // 触发刷新时调用
        coroutineScope.launch {
            isRefreshing = true
            val result = getFriendList(coroutineScope)
            result.onSuccess { friendList ->
                userInfo.friendList = friendList
                Toast.makeText(context, "刷新好友列表成功！", Toast.LENGTH_SHORT).show()
            }.onFailure { throwable ->
                Toast.makeText(context, "刷新好友列表失败！：${throwable.message}", Toast.LENGTH_SHORT).show()
            }
            isRefreshing = false
        }
    })

    // 初始加载，放弃之前用的 coroutineScope.launch(Dispatchers.Main) 方式，改为 LaunchedEffect 不带参数，避免重复加载
    LaunchedEffect(Unit) {
        isRefreshing = true
        val result = getFriendList(coroutineScope)
        result.onSuccess { friendList ->
            userInfo.friendList = friendList
        }.onFailure { throwable ->
            Toast.makeText(context, "获取好友列表失败：${throwable.message}", Toast.LENGTH_SHORT).show()
        }
        isRefreshing = false
    }

    AnimatedContent(
        targetState = currentScreen,
        transitionSpec = {
            if (targetState == ChatScreenState.CHAT_DETAIL) {
                (slideInHorizontally(
                    initialOffsetX = { fullWidth: Int -> fullWidth },  // 显式声明参数类型Int
                    animationSpec = tween(durationMillis = 300)
                ) + fadeIn(animationSpec = tween(300))).togetherWith(
                    slideOutHorizontally(
                        targetOffsetX = { fullWidth: Int -> -fullWidth / 3 },
                        animationSpec = tween(300)
                    ) + fadeOut(animationSpec = tween(300))
                )
            } else {
                (slideInHorizontally(
                    initialOffsetX = { fullWidth: Int -> -fullWidth },
                    animationSpec = tween(durationMillis = 300)
                ) + fadeIn(animationSpec = tween(300))).togetherWith(
                    slideOutHorizontally(
                        targetOffsetX = { fullWidth: Int -> fullWidth / 3 },
                        animationSpec = tween(300)
                    ) + fadeOut(animationSpec = tween(300))
                )
            }
        },
        modifier = Modifier
            .fillMaxSize()
            // 添加 pullRefresh 修饰符
            .pullRefresh(pullRefreshState)
    ) { screen ->
        when (screen) {
            ChatScreenState.FRIEND_LIST -> {
                Box(modifier = Modifier.fillMaxSize()) {
                    FriendListView(
                        friends = userInfo.friendList,
                        onFriendClick = { friend ->
                            selectedFriend = friend
                            currentScreen = ChatScreenState.CHAT_DETAIL
                            onEnterChatDetialScreen()
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
            ChatScreenState.CHAT_DETAIL -> {
                selectedFriend?.let { friend ->
                    ChatDetailScreen(
                        friend = friend,
                        onBack = {
                            currentScreen = ChatScreenState.FRIEND_LIST
                            onExitChatDetialScreen()
                        },
                        onEnterChatDetialScreen = onEnterChatDetialScreen,
                        onMessageSent = { message ->
                            val success = sendMessageToFriend(friend, message)
                            if (success) {
                                coroutineScope.launch {
                                    saveChatMessage(
                                        context = context,
                                        ChatMessage(
                                            sender_id = u_id,
                                            accpet_id = friend.u_id,
                                            content = message,
                                            isMyMessage = true,
                                            isSent = true,
                                            time = ZonedDateTime.now(ZoneId.of("Asia/Shanghai"))
                                                .format(DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss")),
                                        )
                                    )
                                }
                            } else {
                                Toast.makeText(context, "信息发送失败！", Toast.LENGTH_SHORT).show()
                            }
                            success
                        }
                    )
                }
            }
        }
    }
}




@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("UnrememberedMutableState", "UnusedMaterial3ScaffoldPaddingParameter")
@Composable
private fun ChatDetailScreen(
    friend: UserInfo?,
    onEnterChatDetialScreen: () -> Unit,
    onBack: () -> Unit,
    onMessageSent: (String) -> Boolean
) {
    LaunchedEffect(Unit) {
        onEnterChatDetialScreen()
    }
    var message by remember { mutableStateOf("") }
    val context = LocalContext.current.applicationContext
// 将 Flow 转为 State<List<ChatMessage>>

    val chatMessages by getChatMessageById(context, u_id,friend!!.u_id)
        .collectAsStateWithLifecycle(initialValue = emptyList())//直接获取Flow并且转换为Status


    val listState = rememberLazyListState()

    LaunchedEffect(chatMessages.size) {
        if (chatMessages.isNotEmpty()) {
            listState.animateScrollToItem(chatMessages.size - 1) // 滚动到顶部显示最新
        }
    }

    Scaffold(
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.ime)  // 让内容在键盘弹起时上移
        ) {
            // 顶部栏，仿CenterAlignedTopAppBar实现
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = friend?.u_name ?: "聊天",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f)
                )
                // 如果有需要，可以加右侧其他操作按钮
            }

            // 聊天内容列表，填满剩余空间
            LazyColumn(
                modifier = Modifier.weight(1f),
                state = listState,
                contentPadding = PaddingValues(vertical = 8.dp, horizontal = 12.dp),
                reverseLayout = false // 根据需要调整是否倒序显示
            ) {
                items(chatMessages) { msg ->
                    val isMyMessage = msg.sender_id == u_id
                    var avatarUrl : String ? = null
                    if(isMyMessage) {
                        avatarUrl = userInfo.u_avatar
                    }
                    else avatarUrl = friend!!.u_avatar
                    if (msg.isSent == true) {
                        msg.content?.let { content ->
                            msg.time?.let { time ->
                                ChatBubble(
                                    message = content, isMyMessage = isMyMessage, time = time,
                                    avatarUrl = avatarUrl
                                )
                            }
                        }
                    } else {
                        msg.content?.let { content ->
                            msg.time?.let { time ->
                                PendingChatBubble(message = content, isMyMessage = isMyMessage, time = time, avatarUrl = avatarUrl)
                            }
                        }
                    }
                }
            }

            // 底部消息输入栏
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
                        .height(56.dp)   // 高度显式设定，略小于Row高度，避免挤压
                        .background(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(horizontal = 12.dp),
                    placeholder = {
                        Text(
                            text = "输入消息",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    singleLine = true,
                    textStyle = LocalTextStyle.current.copy(
                        fontSize = 14.sp,    // 放大字体
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = 30.sp   // 设置行高
                    ),
                    shape = RoundedCornerShape(12.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        disabledIndicatorColor = Color.Transparent,
                        errorIndicatorColor = Color.Transparent
                    )
                )
                Button(
                    onClick = {
                        if (message.isNotBlank()) {
                            onMessageSent(message)
                            message = ""
                        }
                    },
                    modifier = Modifier
                        .height(40.dp)
                        .widthIn(min = 72.dp)
                        .align(Alignment.CenterVertically),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f),
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    interactionSource = remember { MutableInteractionSource() },
                ) {
                    Text("发送")
                }
            }
        }

    }
}


@Composable
fun ChatBubble(
    message: String,
    isMyMessage: Boolean,
    time: String,
    avatarUrl: String  // 新增头像URL参数
) {
    val bubbleColor = if (isMyMessage)
        MaterialTheme.colorScheme.primaryContainer
    else
        MaterialTheme.colorScheme.secondaryContainer

    val textColor = if (isMyMessage)
        MaterialTheme.colorScheme.onPrimaryContainer
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

    // 整体容器，左右排列消息内容和头像
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = if (isMyMessage) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Bottom
    ) {

        // 消息气泡和时间放同一个Column，方便垂直排列
        Column(
            horizontalAlignment = if (isMyMessage) Alignment.End else Alignment.Start
        ) {
            Row() {
                if (!isMyMessage) {
                    // 左侧头像
                    AvatarImage(avatarUrl)
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Box(
                    modifier = Modifier
                        .background(color = bubbleColor, shape = shape)
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                        .widthIn(max = 280.dp)
                ) {
                    Text(
                        text = message,
                        color = textColor,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                if (isMyMessage) {
                    Spacer(modifier = Modifier.width(8.dp))
                    // 右侧头像
                    AvatarImage(avatarUrl)
                }
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = time,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }
    }
}

@Composable
fun PendingChatBubble(
    message: String,
    isMyMessage: Boolean,
    time: String,
    avatarUrl: String
) {
    val bubbleColor = if (isMyMessage)
        MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)
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

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = if (isMyMessage) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Bottom
    ) {


        Column(
            horizontalAlignment = if (isMyMessage) Alignment.End else Alignment.Start
        ) {
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = if (isMyMessage) Arrangement.End else Arrangement.Start,
                modifier = Modifier.widthIn(max = 280.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Error,
                    contentDescription = "错误",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Row() {
                    if (!isMyMessage) {
                        AvatarImage(avatarUrl)
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Box(
                        modifier = Modifier
                            .background(color = bubbleColor, shape = shape)
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                    ) {
                        Text(
                            text = message,
                            color = textColor,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                    if (isMyMessage) {
                        Spacer(modifier = Modifier.width(8.dp))
                        AvatarImage(avatarUrl)
                    }

                }
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = time,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }
    }
}

// 头像组件，圆形裁剪，固定大小
@Composable//使用coil的库来实现头像的异步加载
fun AvatarImage(avatarUrl: String) {
    Image(
        painter = rememberAsyncImagePainter(
            model = avatarUrl,
            contentScale = ContentScale.Crop
        ),
        contentDescription = "头像",
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
    )
}


@Composable
private fun FriendListView(
    friends: List<UserInfo>,
    onFriendClick: (UserInfo) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        if (isConnected == false) {
            // 黑色卡片，圆角，带阴影
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Black),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // 红色感叹号图标，这里用Material3内置的Error图标，如果你有自定义的可替换
                    Icon(
                        imageVector = Icons.Default.Error, // 需要引入 androidx.compose.material.icons.filled.Error
                        contentDescription = "Error",
                        tint = Color.Red,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "请检查网络设置",
                        color = Color.White,
                        fontSize = 16.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        Text(
            text = "myplayer",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            textAlign = TextAlign.Center
        )

        LazyColumn {
            items(friends) { friend ->
                FriendListItem(friend = friend, onClick = { onFriendClick(friend) })
            }
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


suspend fun getFriendList(coroutineScope: CoroutineScope): Result<List<UserInfo>> = withContext(Dispatchers.IO) {
    try {
        val response = GetRequest(
            interfaceName = "/friend/getfriends",
            queryParams = mapOf()
        ).execute(coroutineScope)  // 这里是同步阻塞调用

        if (!response.isSuccessful) {
            return@withContext Result.failure(Exception("服务器返回失败，状态码：${response.code}"))
        }

        val bodyString = response.body?.string()
        if (bodyString.isNullOrEmpty()) {
            return@withContext Result.failure(Exception("响应体为空"))
        }

        val type = object : TypeToken<BaseResponseJsonData<List<UserInfo>>>() {}.type
        val data = Gson().fromJson<BaseResponseJsonData<List<UserInfo>>>(bodyString, type)

        if (data.data != null) {
            Log.d(TAG, "好友列表更新：${data.data.size}条记录")
            Result.success(data.data)
        } else {
            Result.failure(Exception("获取好友列表失败：${data.msg}"))
        }
    } catch (e: Exception) {
        Log.e(TAG, "获取好友列表异常：${e.message}")
        Result.failure(e)
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



fun sendMessageToFriend(friend: UserInfo, message: String): Boolean {
    return try {
        val wsComStr = JSONObject().apply {
            put("system", false)
            put("group", false)
            put("message", true)
            put("sender", u_id)
            put("sender_name", userInfo.u_name)
            put("target", friend.u_id)
            put("content", message)
            put(
                "time", ZonedDateTime.now(ZoneId.of("Asia/Shanghai"))
                    .format(DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss"))
            )
        }.toString()

        val isSent: Boolean? = webSocketManager?.sendMessage(wsComStr)
        isSent == true && isConnected == true
    } catch (e: Exception) {
        false
    }
}