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
import com.example.myplayer.userInfo.isConnected
import com.example.myplayer.webSocketManager
import org.json.JSONObject
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import androidx.compose.material3.Text
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.myplayer.model.DatabaseProvider
import com.example.myplayer.model.chat.ChatMessage
import com.example.myplayer.userInfo.u_id
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import coil.compose.rememberImagePainter
import coil3.compose.AsyncImage
import coil3.compose.ImagePainter


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
                //Toast.makeText(context, "刷新好友列表成功！", Toast.LENGTH_SHORT).show()
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
                            userInfo.friendList[userInfo.friendList.indexOfFirst { it.u_id == friend.u_id }].isChecked = 0
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



    val chatMessages by getChatMessageById(context, u_id, friend!!.u_id)
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

    Scaffold(
    ) { innerPadding ->
        Column(
            // 这个 Column 填充 Card 的内容区域，并应用 Scaffold 的内边距
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding) // Scaffold 的 padding 会避开 bottomBar
        ) {
            //顶部
            Box(modifier = Modifier.fillMaxWidth().height(56.dp)) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    // 占位元素确保右侧不会挤压文字
                    Spacer(modifier = Modifier.weight(1f))
                }
                Text(
                    text = friend?.u_name ?: "聊天",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            // 中层聊天面板
            Column(
                modifier = Modifier
                    .weight(1f)  // 让中间部分填充剩余空间
                    .fillMaxWidth()
            ) {
            // 聊天内容列表，填满剩余空间
            LazyColumn(
                modifier = Modifier.weight(1f),  // 占满剩余空间
                state = listState
            ) {
                items(chatMessages) { msg ->
                    val isMyMessage = msg.sender_id == u_id
                    var avatarUrl: String? = null
                    if (isMyMessage) {
                        avatarUrl = userInfo.u_avatar
                    } else {
                        avatarUrl = friend!!.u_avatar
                    }
                    if (avatarUrl == null) {
                        Log.d("ChatScreen", "无头像")
                    }
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
                                PendingChatBubble(
                                    message = content,
                                    isMyMessage = isMyMessage,
                                    time = time,
                                    avatarUrl = avatarUrl ?: ""
                                ) // 提供默认值或处理 null
                            }
                        }
                    }
                }
            }

        }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        //.imePadding() // 放在这里通常能确保整个 Row 随着键盘上移
                        .height(56.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                        TextField(
                            value = message, // 现在 messageInput 是 TextFieldValue 类型，匹配
                            onValueChange = { newValue -> // newValue 是 TextFieldValue 类型，匹配
                                message = newValue // 更新状态
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp)   // 高度显式设定
                                .background(
                                    // 使用背景色和圆角，这是 TextField 的外部容器样式
                                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                    shape = RoundedCornerShape(26.dp) // 更大的圆角
                                )
                                .border(
                                    // 添加边框，通常与背景使用相同的圆角，以达到胶囊形状效果
                                    width = 1.dp,
                                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                    shape = RoundedCornerShape(26.dp) // 边框圆角与背景一致
                                )
                            // 注意：这里的 padding 是应用于整个 TextField Composable 的外部，
                            // 而不是文本内容内部的 padding。
                            // .padding(horizontal = 12.dp), // 这个 padding 会把整个 TextField 往里推
                            ,placeholder = {
                                Text(
                                    text = "输入信息...",
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            singleLine = true,
                            textStyle = LocalTextStyle.current.copy(
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                            ),
                            colors = TextFieldDefaults.colors(
                                // 文本颜色 (根据需要修改或使用默认)
                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                disabledTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                                errorTextColor = MaterialTheme.colorScheme.error,

                                // 容器颜色 - 根据新的参考，这些是参数，设置为透明
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                disabledContainerColor = Color.Transparent,
                                errorContainerColor = Color.Transparent,

                                // 光标颜色
                                cursorColor = MaterialTheme.colorScheme.primary,
                                errorCursorColor = MaterialTheme.colorScheme.error,

                                // 文本选择颜色 (使用本地主题的) - 参数名是 selectionColors
                                selectionColors = LocalTextSelectionColors.current,

                                // 指示线颜色 - 设置为透明以移除指示线
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                disabledIndicatorColor = Color.Transparent,
                                errorIndicatorColor = Color.Transparent,

                                // Icon 颜色 (如果使用了 leading/trailing icons) - 根据需要修改或使用默认
                                focusedLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unfocusedLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                disabledLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f),
                                errorLeadingIconColor = MaterialTheme.colorScheme.error,
                                focusedTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unfocusedTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f),
                                errorTrailingIconColor = MaterialTheme.colorScheme.error,

                                // Label 颜色 (如果使用了 label) - 根据需要修改或使用默认
                                // focusedLabelColor = ..., unfocusedLabelColor = ..., etc.
                                // (此处省略，如果您不使用 label，可以不设置或依赖默认)

                                // Placeholder 颜色 - 根据您的需求修改
                                focusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant, // 占位符颜色
                                unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                disabledPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f),
                                errorPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,

                                // Supporting Text 颜色 (如果使用了 supportingText) - 根据需要修改或使用默认
                                // focusedSupportingTextColor = ..., unfocusedSupportingTextColor = ..., etc.
                                focusedSupportingTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unfocusedSupportingTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                disabledSupportingTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f),
                                errorSupportingTextColor = MaterialTheme.colorScheme.error,


                                // Prefix/Suffix 颜色 (如果使用了 prefix/suffix) - 根据需要修改或使用默认
                                // focusedPrefixColor = ..., unfocusedPrefixColor = ..., etc.
                                focusedPrefixColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unfocusedPrefixColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                disabledPrefixColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f),
                                errorPrefixColor = MaterialTheme.colorScheme.error,
                                focusedSuffixColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unfocusedSuffixColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                disabledSuffixColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f),
                                errorSuffixColor = MaterialTheme.colorScheme.error,

                                // *** 注意：根据您最新的参考，内容内边距参数不再这里！***
                                // focusedContentPadding = ..., unfocusedContentPadding = ..., etc. // 移除这些行
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
                            color = if (isMyMessage) Color(0xFFFFE6EC) else Color(0xFFF2E8E9),
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
                        color = if (isMyMessage) Color.Black else Color.Black,
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
                            color = if (isMyMessage) Color(0xFFFFE6EC) else Color(0xFFF2E8E9),
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
                        color = if (isMyMessage) Color.Black else Color.Black,
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
    // 头像
    val imagePainter = rememberImagePainter(
        data = avatarUrl,
        builder = {
            crossfade(true)
        }
    )
    Image(
        painter = imagePainter,
        contentDescription = "用户头像",
        modifier = Modifier
            .size(24.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentScale = ContentScale.Crop
    )
}


@Composable
private fun FriendListView(
    friends: List<UserInfo>,
    onFriendClick: (UserInfo) -> Unit,
    modifier: Modifier = Modifier,
) {
    // 消息通知状态
    var showMessage by remember { mutableStateOf(false) }
    var messageContent by remember { mutableStateOf("") }

    // 监听全局消息
    LaunchedEffect(Unit) {
        try {
            Log.i("FriendsScreen", "开始监听信息")
            GlobalMessageNotifier.messages.collect { message ->
                Log.i("FriendsScreen", "收到消息: ${message}")
                messageContent = message
                showMessage = true
            }
        }
        catch (e: Exception) {
            println("接收消息失败: ${e.message}")
        }

    }
    Column(modifier = modifier) {
        if (!userInfo.isConnected) {
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
        // 显示消息通知
        if (showMessage) {
            TopMessageBanner(
                message = messageContent,
                onDismiss = { showMessage = false }
            )
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
        // 头像容器，使用Box来叠加标记
        Box {
            // 头像
            val imagePainter = rememberImagePainter(
                data = friend.u_avatar,
                builder = {
                    crossfade(true)
                }
            )
            Image(
                painter = imagePainter,
                contentDescription = "用户头像",
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )

            // 如果isChecked不为0，显示标记
            if (friend.isChecked != 0) {
                Box(
                    modifier = Modifier
                        .size(16.dp) // 这个Box定义了红色圆点的大小
                        .offset(x = (-2).dp, y = (-2).dp) // 这个Box相对于头像的位置
                        .clip(CircleShape)
                        .background(Color.Red)
                        .align(Alignment.TopStart), // 这个Box在父Box (头像容器) 里的位置
                    contentAlignment = Alignment.Center // <<< 这个属性让里面的Text居中！
                ) {
                    Text(
                        text = if(friend.isChecked < 99)friend.isChecked.toString() else "99+",
                        modifier = Modifier.offset(y = (-2.4).dp), // 例如，向下微调1dp
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        // 移除 Modifier.wrapContentSize，让父Box的contentAlignment起作用
                        // Modifier = Modifier.wrapContentSize(align = Alignment.Center), // 移除或注释掉这行
                        textAlign = TextAlign.Center // 保持这个，它让文字在Text自身的范围内水平居中
                    )
                }
            }
        }

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