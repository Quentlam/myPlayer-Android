package com.example.myplayer.framework.playroom

import android.content.Context
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.myplayer.jsonToModel.JsonToBaseResponse
import com.example.myplayer.model.BaseSentJsonData
import com.example.myplayer.model.playroom.PlayroomContent
import com.example.myplayer.model.playroom.RequestDetails
import com.example.myplayer.model.BaseInformation.currentMemberList
import com.example.myplayer.model.BaseInformation.currentRequestList
import com.example.myplayer.model.BaseInformation.currentRoom
import com.example.myplayer.network.BaseRequest
import com.example.myplayer.userInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Response
import java.time.format.DateTimeFormatter
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.example.myplayer.framework.playroom.player.exoPlayerView
import com.example.myplayer.model.playroom.JoinMessage
import com.example.myplayer.model.playroom.Member
import com.example.myplayer.model.playroom.Message
import com.example.myplayer.model.playroom.Playroom
import com.example.myplayer.model.playroom.ReadyMessage
import com.example.myplayer.model.playroom.StartMessage
import com.example.myplayer.model.playroom.StopMessage
import com.example.myplayer.model.playroom.SynchronousRequestMessage
import com.example.myplayer.model.playroom.SynchronousResponseMessage
import com.example.myplayer.model.playroom.UrlMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import org.json.JSONObject
import java.time.ZoneId
import java.time.ZonedDateTime


val messageHandler = object : PlayroomMessageHandler {
    override fun onUserJoined(context : Context,msg: JoinMessage) {//如果加入了，就需要给房间的视频连接
        Log.i("roomScreenWS", "用户加入房间: ${msg.u_name}")
        Toast.makeText(context, "${msg.u_name}加入房间！", Toast.LENGTH_SHORT).show()
        //如果是房主，那么就要发送信息给新来的人
        if(currentMemberList.find { it.role == 2 && it.m_name == userInfo.u_name } != null)
        {
            try {
                val wsComStr = JSONObject().apply {
                    put("type", "url") // 弹幕消息类型
                    put("r_id", currentRoom.r_id) // 房间ID
                    put("from", userInfo.u_id) // 发送者u_id
                    put("to", msg.u_id) // 接收者的u_id
                    put("url", currentRoom.current_url) // 当前房间的视频连接
                }.toString()
                playRoomWebSocketManager?.sendMessage(wsComStr)
                Log.d("roomScreenWS","房主将视频信息发送给刚加入房间的用户发送成功！：${wsComStr}")
            }
            catch (e : Exception)
            {
                Log.e("roomScreenWS","房主将视频信息发送给刚加入房间的用户失败！！：${e.message}")
            }
        }

        // 只有房主处理，您可以在这里实现相关逻辑
    }
    override fun onUrlReceived(msg: UrlMessage) {
        Log.i("roomScreenWS", "收到视频链接: ${msg.url}")
        // 加载视频，准备就绪后调用服务器发送 ready 消息
    }
    override fun onUserReady(msg: ReadyMessage) {
        Log.i("roomScreenWS", "用户准备就绪: ${msg.fromUserId}")
        try {
            val wsComStr = JSONObject().apply {
                put("type", "ready") // 弹幕消息类型
                put("r_id", currentRoom.r_id) // 房间ID
                put("from", userInfo.u_id) // 发送者u_id
            }.toString()
            playRoomWebSocketManager?.sendMessage(wsComStr)
            Log.d("roomScreenWS","用户准备就绪发送成功！：${wsComStr}")
        }
        catch (e : Exception)
        {
            Log.e("roomScreenWS","用户准备就绪发送失败！！：${e.message}")
        }
    }
    override fun onStart(msg: StartMessage) {
        Log.i("roomScreenWS", "开始播放")
        // 启动播放器
    }
    override fun onStop(msg: StopMessage) {
        Log.i("roomScreenWS", "暂停播放")
        // 暂停播放器
    }
    override fun onSynchronousRequest(msg: SynchronousRequestMessage) {//等下修改
        try {
            val wsComStr = JSONObject().apply {
                put("type", "synchronous request") // 弹幕消息类型
                put("r_id", currentRoom.r_id) // 房间ID
                put("from", userInfo.u_id) // 发送者u_id
            }.toString()
            playRoomWebSocketManager?.sendMessage(wsComStr)
            Log.d("roomScreenWS","用户向房主获取当前时间戳发送成功！：${wsComStr}")
        }
        catch (e : Exception)
        {
            Log.e("roomScreenWS","用户向房主获取当前时间戳发送失败！！：${e.message}")
        }
        Log.i("roomScreenWS", "同步请求")
    }
    // 房主暂停播放，获取时间戳，发送同步响应
    override fun onSynchronousResponse(msg: SynchronousResponseMessage) {
        val positionMs = (msg.currentTime * 1000).toLong()
        onUpdateStartPositionMs(positionMs)
        try {
            val wsComStr = JSONObject().apply {
                put("type", "synchronous response") // 弹幕消息类型
                put("r_id", currentRoom.r_id) // 房间ID
                put("from", userInfo.u_id) // 发送者u_id
                put("url", currentRoom.current_url) // 房间url
                put("currentTime", userInfo.u_id) // 发送者u_id
            }.toString()
            playRoomWebSocketManager?.sendMessage(wsComStr)
            Log.d("roomScreenWS","用户向房主获取当前时间戳发送成功！：${wsComStr}")
        }
        catch (e : Exception)
        {
            Log.e("roomScreenWS","用户向房主获取当前时间戳发送失败！！：${e.message}")
        }
        Log.i("roomScreenWS", "同步响应，时间戳: ${msg.currentTime}")
    }


    override fun onChatMessage(context : Context, coroutineScope : CoroutineScope, content: Message) {
        Log.d("roomScreenWS","收到服务器弹幕信息！:${content.content}")
        coroutineScope.launch {
            savePlayroomMessage(
                context,
                PlayroomContent(
                    0,
                    currentRoom.r_id,
                    content.fromUserId,
                    content.u_name,
                    content.content,
                    ZonedDateTime.now(ZoneId.of("Asia/Shanghai"))
                        .format(DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss"))
                )
            )
        }
    }


    override fun onUpdateStartPositionMs(ms: Long) {
        Log.i("roomScreenWS", "更新播放位置: $ms ms")
        // 通过您已有的回调更新UI播放位置
    }

}

@Composable
fun manageRoomContent(
    onBack: () -> Unit
) {
    var showInvitationDialog by remember { mutableStateOf(false) }
    var showRoomSetting by remember { mutableStateOf(false) }
    var showSuccessToast by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    getInvitations(currentRoom.r_id)


    // 房间设置弹窗临时状态
    var videoUrl by remember { mutableStateOf(currentRoom.current_url ?: "") }
    var avatarUrl by remember { mutableStateOf(currentRoom.r_avatar ?: "") } // 头像URL字符串
    var avatarUri by remember { mutableStateOf<Uri?>(null) } // 本地选择的Uri


    // 用于触发打开系统图片选取器
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            avatarUri = uri
        }
    }


    if (showInvitationDialog) {
        AlertDialog(
            onDismissRequest = { showInvitationDialog = false },
            title = {
                Text(
                    "加入申请列表",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                if (currentRequestList.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "暂无申请",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn {
                        items(currentRequestList) { request ->
                            RequestItem(
                                request = request,
                                onApprove = {
                                    // 本地移除该申请
                                    currentRequestList = currentRequestList.filter { it != request }
                                    var response: Response? = null
                                    // 异步请求服务器
                                    coroutineScope.launch {
                                        Log.d(
                                            "roomScreen",
                                            "向服务器发送同意进入房间！${request.inviter},${request.inviter},${request.inviter}"
                                        )

                                        try {
                                            withContext(Dispatchers.IO) {
                                                response = BaseRequest(
                                                    listOf(
                                                        BaseSentJsonData(
                                                            "inviter",
                                                            request.inviter
                                                        ),
                                                        BaseSentJsonData(
                                                            "target",
                                                            request.inviter
                                                        ),
                                                        BaseSentJsonData("room", request.room)
                                                    ),
                                                    "/inviting/passinviting"
                                                ).sendPostRequest(coroutineScope)
                                                val data =
                                                    JsonToBaseResponse<String>(response!!).getResponseData()

                                                Log.d(
                                                    "roomScreen",
                                                    "同意加入房间成功！${data.msg}"
                                                )
                                                withContext(Dispatchers.Main) {
                                                    Toast.makeText(
                                                        context,
                                                        "同意加入房间成功！${data.msg}",
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                }
                                            }
                                        } catch (e: Exception) {
                                            Toast.makeText(
                                                context,
                                                "同意加入房间失败！：${response?.message.toString()}",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                            Log.e("roomScreen", "同意加入房间失败！${e.message}")
                                            // 失败时，可以考虑恢复该申请
                                            currentRequestList = currentRequestList + request
                                        }

                                    }
                                },
                                onReject = {
                                    var response : Response? = null
                                    // 本地移除该申请
                                    currentRequestList = currentRequestList.filter { it != request }
                                    coroutineScope.launch {
                                        try {
                                            withContext(Dispatchers.IO) {
                                                response = BaseRequest(
                                                    listOf(
                                                        BaseSentJsonData(
                                                            "inviter",
                                                            request.inviter
                                                        ),
                                                        BaseSentJsonData("target", request.inviter),
                                                        BaseSentJsonData("room", request.room)
                                                    ),
                                                    "/refuseinviting"
                                                ).sendPostRequest(coroutineScope)
                                                val data =
                                                    JsonToBaseResponse<String>(response!!).getResponseData()
                                                Log.d("roomScreen", "拒绝加入房间成功！${data.msg}")
                                                withContext(Dispatchers.Main) {
                                                    Toast.makeText(
                                                        context,
                                                        "同意加入房间成功！${data.msg}",
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                }
                                            }
                                        } catch (e: Exception) {
                                            Log.e("roomScreen", "拒绝加入房间失败！${e}")
                                                Toast.makeText(
                                                    context,
                                                    "拒绝加入房间失败！${response?.body.toString()}",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            currentRequestList = currentRequestList + request
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showInvitationDialog = false }) {
                    Text("关闭")
                }
            },
            shape = RoundedCornerShape(16.dp)
        )
    }
    // 显示成功Toast
    if (showSuccessToast) {
        Toast.makeText(LocalContext.current, "修改成功", Toast.LENGTH_SHORT).show()
        showSuccessToast = false
    }

    if (showRoomSetting) {
        AlertDialog(
            onDismissRequest = { showRoomSetting = false },
            title = { Text("房间设置") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = videoUrl,
                        onValueChange = { videoUrl = it },
                        label = { Text("视频流地址") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = "当前头像：")
                    if (avatarUri != null) {
                        // 优先显示本地选中的图片
                        Image(
                            painter = rememberAsyncImagePainter(model = avatarUri),
                            contentDescription = "选择的头像预览",
                            modifier = Modifier
                                .size(80.dp)
                                .clip(CircleShape)
                        )
                    } else if (avatarUrl.isNotBlank()) {
                        // 显示远程头像URL
                        Image(
                            painter = rememberAsyncImagePainter(model = avatarUrl),
                            contentDescription = "当前头像",
                            modifier = Modifier
                                .size(80.dp)
                                .clip(CircleShape)
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .background(Color.Gray, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("无头像", color = Color.White)
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = {
                        launcher.launch("image/*")  // 选择图片
                    }) {
                        Text("修改房间头像")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    coroutineScope.launch {
                        // 如果用户选择了本地图片，需要上传
                        if (avatarUri != null) {
                            try {
                                // 这里写上传逻辑，返回远程头像URL的字符串
                                val uploadedUrl = uploadImageAndGetUrl(avatarUri!!)
                                // 更新头像URL和currentRoom数据
                                avatarUrl = uploadedUrl
                                currentRoom = currentRoom.copy(r_avatar = uploadedUrl)
                            } catch (e: Exception) {
                                // 上传失败处理
                                Log.e("manageRoomContent", "头像上传失败: $e")
                            }
                        }
                        // 更新视频流地址
                        currentRoom = currentRoom.copy(current_url = videoUrl)
                        Log.d("roomScreen", "修改视频URL成功！${currentRoom.current_url}")
                        showSuccessToast = true
                        showRoomSetting = false
                        // 清空本地Uri状态
                        avatarUri = null
                    }
                }) {
                    Text("确认")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showRoomSetting = false
                    avatarUri = null  // 取消选择，清空临时Uri
                }) {
                    Text("取消")
                }
            }
        )
    }


    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Row(  // 改为水平布局，节省垂直空间
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),  // 减小内部padding
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        "房间: ${currentRoom.r_name ?: ""}",  // 合并标题和名称
                        style = MaterialTheme.typography.titleMedium,  // 使用稍小的字体
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))  // 减小间距
                    Text(
                        "ID: ${currentRoom.r_id ?: ""}",
                        style = MaterialTheme.typography.bodySmall,  // 使用更小的字体
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))  // 减小间距
                    Text(
                        currentRoom.r_introduction ?: "",
                        style = MaterialTheme.typography.bodySmall,  // 使用更小的字体
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,  // 限制行数
                        overflow = TextOverflow.Ellipsis  // 超出部分显示省略号
                    )
                }

                // 头像放在右侧，尺寸更小
                AsyncImage(
                    model = currentRoom.r_avatar ?: "",
                    contentDescription = "房间头像",
                    modifier = Modifier
                        .padding(start = 12.dp)
                        .size(48.dp)  // 减小头像尺寸
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                )
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                ManagementItem(
                    icon = Icons.Default.Notifications,
                    title = "申请列表",
                    subtitle = "处理加入申请",
                    onClick = { showInvitationDialog = true }
                )
            }

            item {
                ManagementItem(
                    icon = Icons.Default.Settings,
                    title = "房间设置",
                    subtitle = "修改房间信息",
                    onClick = {
                        videoUrl = currentRoom.current_url ?: ""
                        avatarUrl = currentRoom.r_avatar ?: ""
                        avatarUri = null
                        showRoomSetting = true
                    }
                )
            }

            item {
                ManagementItem(
                    icon = Icons.Default.Delete,
                    title = "解散房间",
                    subtitle = "永久删除此房间",
                    onClick = { /* 处理解散房间点击 */ },
                    isDangerous = true
                )
            }
        }
    }
}

@Composable
private fun ManagementItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    isDangerous: Boolean = false
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDangerous)
                MaterialTheme.colorScheme.errorContainer
            else
                MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isDangerous)
                    MaterialTheme.colorScheme.error
                else
                    MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (isDangerous)
                        MaterialTheme.colorScheme.error
                    else
                        MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isDangerous)
                        MaterialTheme.colorScheme.error
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun roomScreen(room : Playroom,onBack: () -> Unit) {
    Log.d(
        "chosePlayroomScreen",
        "现在的房间ID：${currentRoom.r_id},视频连接：${currentRoom.current_url},头像URL${currentRoom.r_avatar},房间介绍${currentRoom.r_introduction},房间名字${currentRoom.r_name}"
    )
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var currentTab by remember { mutableStateOf(0) }
    var messageInput by remember { mutableStateOf("") }
    val messageList by getPlayroomMessage(context, currentRoom.r_id)
        .collectAsStateWithLifecycle(initialValue = emptyList())//直接获取Flow并且转换为Status
    var startPositionMs by remember { mutableStateOf(0L) }


    // 添加 LazyListState 来控制滚动
    val listState = rememberLazyListState()

    DisposableEffect(Unit) {
        // 进入roomScreen，启动连接
        scope.launch {
            connectToPlayroomWS(
                roomId = currentRoom.r_id,
                context = context,
                coroutineScope = scope,
                messageHandler = messageHandler
            )
        }
        onDispose {
            // 离开roomScreen，断开WebSocket
            playRoomWebSocketManager?.disconnect()
            playRoomWebSocketManager = null
            Log.d("roomScreen", "roomScreen离开，WebSocket已正常断开")
            Toast.makeText(context, "已离开房间！:${currentRoom.r_id}", Toast.LENGTH_SHORT).show()
        }
    }

    // 当消息列表更新时，自动滚动到底部
    LaunchedEffect(messageList.size) {
        if (messageList.isNotEmpty()) {
            listState.animateScrollToItem(messageList.size - 1)
        }
    }
    getMembers(currentRoom.r_id)

    Scaffold(
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // 视频播放器区域 - 设置固定高度或者合适的比例
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)  // 使用16:9的视频比例
            ) {
                currentRoom.current_url?.let {
                    exoPlayerView(
                        context = LocalContext.current,
                        videoUrl = it,
                        startPositionMs = startPositionMs,
                        lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
                    )
                }
            }

            // 中层切换面板
            Column(
                modifier = Modifier
                    .weight(1f)  // 让中间部分填充剩余空间
                    .fillMaxWidth()
            ) {
                TabRow(selectedTabIndex = currentTab) {
                    Tab(
                        selected = currentTab == 0,
                        onClick = { currentTab = 0 },
                        text = { Text("弹幕") }
                    )
                    Tab(
                        selected = currentTab == 1,
                        onClick = { currentTab = 1 },
                        text = { Text("房间成员") }
                    )
                    Tab(
                        selected = currentTab == 2,
                        onClick = { currentTab = 2 },
                        text = { Text("管理") }
                    )
                }

                // 列表内容区域
                Box(modifier = Modifier.weight(1f)) {
                    when (currentTab) {
                        0 -> LazyColumn(
                            state = listState
                        ) {
                            items(messageList) { message ->
                                messageElment(message)
                            }
                        }

                        1 -> LazyColumn {
                            items(currentMemberList) { member ->
                                memberElement(member)
                            }
                        }

                        2 -> {
                            if (currentRoom != null) {
                                manageRoomContent(onBack = { currentTab = 0 })
                            } else {
                                // 显示错误状态
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("房间信息加载失败")
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
                    .height(48.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp) // 增加组件间距
            ) {
                val customTextSelectionColors = TextSelectionColors(
                    handleColor = MaterialTheme.colorScheme.primary,
                    backgroundColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                )
                CompositionLocalProvider(LocalTextSelectionColors provides customTextSelectionColors) {
                    TextField(
                        value = messageInput,
                        onValueChange = { messageInput = it },
                        modifier = Modifier
                            .weight(1f)
                            .background( // 添加背景色
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(24.dp)
                            )
                            .border( // 添加轻微边框
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                shape = RoundedCornerShape(24.dp)
                            )
                            .padding(horizontal = 16.dp),
                        placeholder = {
                            Text(
                                "输入弹幕...",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        singleLine = true,
                        textStyle = LocalTextStyle.current.copy(
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            disabledIndicatorColor = Color.Transparent,
                            errorIndicatorColor = Color.Transparent
                        ),
                        shape = RoundedCornerShape(24.dp)
                    )
                }

                Button(
                    onClick = {
                        if (messageInput.isNotBlank()) {
                            scope.launch {
                                savePlayroomMessage(
                                    context,
                                    PlayroomContent(
                                        0,
                                        currentRoom.r_id,
                                        userInfo.u_id,
                                        userInfo.u_name,
                                        messageInput,
                                        ZonedDateTime.now(ZoneId.of("Asia/Shanghai"))
                                            .format(DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss"))
                                    )
                                )
                                try {
                                    val wsComStr = JSONObject().apply {
                                        put("type", "message")
                                        put("r_id", currentRoom.r_id)
                                        put("from", userInfo.u_id)
                                        put("u_name", userInfo.u_name)
                                        put("content", messageInput)
                                    }.toString()
                                    playRoomWebSocketManager?.sendMessage(wsComStr)
                                    Log.d(
                                        "PlayroomWebSocketManager",
                                        "本地弹幕信息发送成功！：${wsComStr}"
                                    )
                                    messageInput = ""
                                } catch (e: Exception) {
                                    Log.e(
                                        "PlayroomWebSocketManager",
                                        "本地弹幕信息发送失败！：${e.message}"
                                    )
                                }
                            }
                        }
                    },
                    modifier = Modifier
                        .height(40.dp)
                        .widthIn(min = 72.dp)
                        .scale(0.9f),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f),
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    interactionSource = remember { MutableInteractionSource() }, // 保留这个
                ) {
                    Text(
                        "发送",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium, // 稍微加粗
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }

        }
    }
}
@Composable
fun RequestItem(
    request: RequestDetails,
    onApprove: () -> Unit,
    onReject: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row()
        {

            // 左侧显示申请者信息
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = request.inviter_name,
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = "ID: ${request.inviter}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // 右侧按钮
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onApprove,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text("通过")
                }

                Button(
                    onClick = onReject,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("拒绝")
                }
            }
        }
    }
}

@Composable
fun messageElment(message : PlayroomContent)
{
    Box(
        modifier = Modifier
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .background(
                    color = if (message.u_id == userInfo.u_id)
                        MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(8.dp)
                )
                .padding(8.dp)
                // 修正：使用 alignBy 而不是 align
                .align(if (message.u_id == userInfo.u_id) Alignment.CenterEnd else Alignment.CenterStart)
                .widthIn(max = 280.dp)
        ) {
            Text(
                text = if (message.u_id == userInfo.u_id) "你" else message.u_name,
                style = MaterialTheme.typography.labelLarge,
                color = if (message.u_id == userInfo.u_id)
                    MaterialTheme.colorScheme.onPrimaryContainer
                else MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = message.content,
                style = MaterialTheme.typography.bodyMedium,
                color = if (message.u_id == userInfo.u_id)
                    MaterialTheme.colorScheme.onPrimaryContainer
                else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = 4.dp)
            )
            Text(
                text = message.time,
                style = MaterialTheme.typography.bodySmall,
                color = if (message.u_id == userInfo.u_id)
                    MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                modifier = Modifier
                    .padding(top = 4.dp)
                    .align(Alignment.End)  // 这里的 align 是在 Column 中使用，所以保持不变
            )
        }
    }
}

@Composable
fun memberElement(member : Member) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 用户头像
            AsyncImage(
                model = member.m_avatar,
                contentDescription = "用户头像",
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = CircleShape
                    )
            )

            Column {
                // 用户名
                Text(
                    text = member.m_name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                // 用户ID
                Text(
                    text = "ID: ${member.m_id}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

suspend fun uploadImageAndGetUrl(uri: Uri): String {//待定，因为服务器后端还没有写
    delay(1000) // 模拟网络上传延迟
    return "https://123.png"
}

