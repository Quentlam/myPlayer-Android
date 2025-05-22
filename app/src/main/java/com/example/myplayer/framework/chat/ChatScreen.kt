package com.example.myplayer.framework.chat

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.ui.text.style.TextAlign
import coil3.compose.AsyncImage


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
                            val success = sendMessageToFriend(friend, message,context)
                            if (success) {
                                CoroutineScope(Dispatchers.IO).launch {
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
                                CoroutineScope(Dispatchers.Main).launch{
                                    Toast.makeText(context, "信息发送失败，请检查网络设置！", Toast.LENGTH_SHORT).show()
                                }
                                // Optionally, save as pending if send failed
                                CoroutineScope(Dispatchers.IO).launch{
                                    saveChatMessage(
                                        context = context,
                                        ChatMessage(
                                            sender_id = u_id,
                                            accpet_id = friend.u_id,
                                            content = message,
                                            isMyMessage = true,
                                            isSent = false, // Mark as not sent
                                            time = ZonedDateTime.now(ZoneId.of("Asia/Shanghai"))
                                                .format(DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss")),
                                        )
                                    )
                                }
                            }
                            success // Return success status
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
    onMessageSent: (String) -> Boolean // 修改为返回Boolean
) {
    BackHandler(enabled = true) {
        onBack()
    }
    LaunchedEffect(Unit) {
        onEnterChatDetialScreen()
    }
    var message by remember { mutableStateOf("") }
    val context = LocalContext.current.applicationContext
    // 将 Flow 转为 State<List<ChatMessage>>
    // 注意：根据你的 sendMessageToFriend 返回值变化，这里可能需要调整一下消息发送后的处理逻辑，
    // 比如失败的消息显示 PendingChatBubble，成功的消息显示 ChatBubble。
    // 你的代码中已经根据 msg.isSent 判断了，这是正确的。
    val chatMessages by getChatMessageById(context, u_id,friend!!.u_id)
        .collectAsStateWithLifecycle(initialValue = emptyList())//直接获取Flow并且转换为Status


    val listState = rememberLazyListState()

    // 滚动到最新消息，只有当有新消息添加时才滚动
    LaunchedEffect(chatMessages.size) {
        if (chatMessages.isNotEmpty()) {
            // 使用 snapshots.SnapshotStateList 监听变化更细致
            // 但对于简单的列表大小变化滚动，这个也够用
            listState.animateScrollToItem(chatMessages.size - 1)
        }
    }

    // *** 将整个 Scaffold 内容包裹在 Card 中以实现圆角外观 ***
    Card(
        modifier = Modifier
            .fillMaxSize() // Card 填充可用空间
            .padding(horizontal = 8.dp, vertical = 16.dp), // 卡片整体的边距
        shape = RoundedCornerShape(16.dp), // 卡片的圆角
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp) // 可选的阴影
    ) {
        Scaffold(
            // 顶部栏 Header 在 Column 中定义，不需要在 Scaffold 的 topBar 插槽
            // 底部消息输入栏
            bottomBar = {
                Box( // 使用 Box 来设置底部栏的背景色和 imePadding
                    modifier = Modifier
                        .fillMaxWidth()
                        // .imePadding() // 尝试将 imePadding 放在 Row 上，如果不行再放这里
                        .background(MaterialTheme.colorScheme.surface) // 设置底部栏背景色
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .imePadding() // 放在这里通常能确保整个 Row 随着键盘上移
                            .height(56.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TextField(
                            value = message,
                            onValueChange = { message = it },
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp)
                                .background(
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .border(
                                    width = 1.dp,
                                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .padding(horizontal = 12.dp), // TextField 内部文本的 padding
                            placeholder = {
                                Text(
                                    text = "输入消息",
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            singleLine = true,
                            textStyle = LocalTextStyle.current.copy(
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                lineHeight = 30.sp
                            ),
                            shape = RoundedCornerShape(12.dp), // 虽然外层 Box/Background 已经设了，这里再设一次确保
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                disabledIndicatorColor = Color.Transparent,
                                errorIndicatorColor = Color.Transparent
                            )
                        )

                        // *** 将 Button 替换为 IconButton ***
                        IconButton(
                            onClick = {
                                if (message.isNotBlank()) {
                                    // onMessageSent 会处理发送逻辑和保存到数据库
                                    onMessageSent(message)
                                    message = "" // 清空输入框，无论发送成功与否
                                }
                            },
                            // IconButton 通常不需要显式设置 height/widthIn，它根据内容和 padding 自适应
                            modifier = Modifier.align(Alignment.CenterVertically),
                            // IconButton 的颜色通过 tint 设置 Icon 或通过 colors 参数设置
                            colors = IconButtonDefaults.iconButtonColors(
                                contentColor = MaterialTheme.colorScheme.primary // 图标颜色
                            )
                            // IconButton 不需要 shape, interactionSource
                        ) {
                            // *** 使用发送图标 ***
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send, // 使用发送图标
                                contentDescription = "发送消息"
                            )
                        }
                    }
                }
            }
        ) {
                innerPadding ->
            Column(
                // 这个 Column 填充 Card 的内容区域，并应用 Scaffold 的内边距
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding) // Scaffold 的 padding 会避开 bottomBar
            ) {
                // 顶部 Header Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
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
                    modifier = Modifier.weight(1f),  // 占满剩余空间
                    state = listState
                ) {
                    items(chatMessages) { msg ->
                        val isMyMessage = msg.sender_id == u_id
                        var avatarUrl : String ? = null
                        if(isMyMessage) {
                            avatarUrl = userInfo.u_avatar
                        }
                        else avatarUrl = friend!!.u_avatar
                        // 根据 isSent 决定使用 ChatBubble 还是 PendingChatBubble
                        if (msg.isSent == true) {
                            msg.content?.let { content ->
                                msg.time?.let { time ->
                                    ChatBubble(
                                        message = content, isMyMessage = isMyMessage, time = time,
                                        avatarUrl = avatarUrl ?: "" // 提供默认值或处理 null
                                    )
                                }
                            }
                        } else {
                            msg.content?.let { content ->
                                msg.time?.let { time ->
                                    PendingChatBubble(message = content, isMyMessage = isMyMessage, time = time, avatarUrl = avatarUrl ?: "") // 提供默认值或处理 null
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}


// 简单的聊天气泡 Composable (无姓名)
@Composable
fun ChatBubble(
    message: String,
    isMyMessage: Boolean,
    time: String,
    avatarUrl: String? // 使 avatarUrl 可选
) {
    // 外层 Row 控制左右对齐
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = if (isMyMessage) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Bottom // 确保头像和时间行与气泡底部对齐
    ) {
        // 主体 Column 包含气泡、头像和时间
        Column(
            horizontalAlignment = if (isMyMessage) Alignment.End else Alignment.Start
        ) {
            Row(
                modifier = Modifier
                    .align(if (isMyMessage) Alignment.End else Alignment.Start)
                    .padding(top = 2.dp), // 头像/时间与气泡之间的间距
                verticalAlignment = Alignment.Bottom
            )
            {
                if (!isMyMessage) {
                    AvatarImage(avatarUrl)
                    Spacer(modifier = Modifier.width(4.dp))
                }
                // 气泡 Box
                Box(
                    modifier = Modifier
                        .background(
                            color = if (isMyMessage) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(
                                topStart = 16.dp,
                                topEnd = 16.dp,
                                bottomStart = if (isMyMessage) 16.dp else 4.dp,
                                bottomEnd = if (isMyMessage) 4.dp else 16.dp
                            )
                        )
                        .padding(horizontal = 12.dp, vertical = 8.dp) // 调整内部padding
                        .widthIn(max = 280.dp)
                ) {
                    Text(
                        text = message,
                        color = if (isMyMessage) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                if (isMyMessage) {
                    Spacer(modifier = Modifier.width(4.dp))
                    AvatarImage(avatarUrl)
                }
            }


            // 包含头像和时间的 Row
            Row(
                modifier = Modifier
                    .align(if (isMyMessage) Alignment.End else Alignment.Start)
                    .padding(top = 2.dp), // 头像/时间与气泡之间的间距
                verticalAlignment = Alignment.Bottom
            ) {


                Text(
                    text = time,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.bodySmall,
                )

            }
        }
    }
}


// 带有错误提示的聊天气泡 Composable
@Composable
fun PendingChatBubble(
    message: String,
    isMyMessage: Boolean,
    time: String,
    avatarUrl: String? // 使 avatarUrl 可选
) {
    // 外层 Row 控制左右对齐
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = if (isMyMessage) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Bottom // 确保头像和时间行与气泡底部对齐
    ) {
        // 主体 Column 包含气泡+图标块、头像和时间
        Column(
            horizontalAlignment = if (isMyMessage) Alignment.End else Alignment.Start
        ) {
            // 包含图标和气泡的 Row (图标只在发送失败时显示，通常是我的消息)
            Row(
                verticalAlignment = Alignment.CenterVertically // 图标和气泡垂直居中对齐
            ) {
                // 如果是我的消息且 pending，显示错误图标 (根据您的逻辑判断是否 pending)
                // 这里简化处理，假设 PendingChatBubble 就是为了显示错误状态
                if (isMyMessage) {
                    Icon(
                        imageVector = Icons.Default.Error,
                        contentDescription = "发送失败",
                        tint = MaterialTheme.colorScheme.error, // 错误颜色
                        modifier = Modifier.size(20.dp) // 图标大小
                    )
                    Spacer(modifier = Modifier.width(6.dp)) // 图标和气泡之间的间距
                }

                // 气泡 Box
                Box(
                    modifier = Modifier
                        .background(
                            // 可以使用稍微不同的颜色表示发送失败，或者保持一致
                            color = if (isMyMessage) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(
                                topStart = 16.dp,
                                topEnd = 16.dp,
                                bottomStart = if (isMyMessage) 16.dp else 4.dp,
                                bottomEnd = if (isMyMessage) 4.dp else 16.dp
                            )
                        )
                        .padding(horizontal = 12.dp, vertical = 8.dp) // 调整内部padding
                        .widthIn(max = 280.dp)
                ) {
                    Text(
                        text = message,
                        color = if (isMyMessage) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }

            }

            // 包含头像和时间的 Row，位于气泡下方
            Row(
                modifier = Modifier
                    .align(if (isMyMessage) Alignment.End else Alignment.Start)
                    .padding(top = 2.dp), // 头像/时间与气泡之间的间距
                verticalAlignment = Alignment.Bottom
            ) {
                if (!isMyMessage) {
                    AvatarImage(avatarUrl)
                    Spacer(modifier = Modifier.width(4.dp))
                }

                Text(
                    text = time,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.bodySmall,
                )

                if (isMyMessage) {
                    Spacer(modifier = Modifier.width(4.dp))
                    AvatarImage(avatarUrl)
                }
            }
        }
    }
}


// 辅助函数：头像 Composable，方便复用
@Composable
fun AvatarImage(avatarUrl: String?, modifier: Modifier = Modifier) {
    AsyncImage(
        model = avatarUrl,
        contentDescription = "用户头像",
        modifier = modifier
            .size(24.dp) // 调整头像大小，根据图片样式看起来比 32dp 略小
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant) // 占位符背景
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



fun sendMessageToFriend(friend: UserInfo, message: String,context: Context): Boolean {
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

        webSocketManager?.sendMessage(wsComStr)
        if(isConnected)
        {
            Log.d("ChatScreen","信息发送成功${message}！")
            return true
        }
        else
        {
            Toast.makeText(context, "信息发送失败，请检查网络设置！", Toast.LENGTH_SHORT).show()
            return false
        }

    } catch (e: Exception) {
        CoroutineScope(Dispatchers.Main).launch{
            Toast.makeText(context, "信息发送异常，请检查网络设置！", Toast.LENGTH_SHORT).show()
            Log.e("ChatScreen","信息发送失败！${e.message}")
        }
        return false
    }
}