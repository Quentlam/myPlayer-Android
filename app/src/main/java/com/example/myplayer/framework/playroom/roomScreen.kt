package com.example.myplayer.framework.playroom

import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.myplayer.jsonToModel.JsonToBaseResponse
import com.example.myplayer.model.BaseResponseJsonData
import com.example.myplayer.model.BaseSentJsonData
import com.example.myplayer.model.playroom.PlayroomContent
import com.example.myplayer.model.playroom.RequestDetails
import com.example.myplayer.network.BaseInformation.currentMemberList
import com.example.myplayer.network.BaseInformation.currentRequestList
import com.example.myplayer.network.BaseInformation.currentRoom
import com.example.myplayer.network.BaseRequest
import com.example.myplayer.userInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Response
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import coil.compose.rememberAsyncImagePainter
import com.example.myplayer.framework.playroom.player.exoPlayerView
import com.example.myplayer.model.playroom.Member
import com.example.myplayer.network.BaseInformation
import kotlinx.coroutines.delay

@Composable
fun manageRoomContent(
    onBack: () -> Unit
) {
    var showInvitationDialog by remember { mutableStateOf(false) }
    var showRoomSetting by remember { mutableStateOf(false) }
    var showSuccessToast by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

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


    // 处理邀请对话框
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
                                // 处理通过逻辑
                                onApprove = {
                                    coroutineScope.launch {
                                        withContext(Dispatchers.IO) {
                                            Log.d("roomScreen",
                                                "inviter:${request.inviter} " +
                                                        "target:${request.inviter} " +
                                                        "room:${request.room} "
                                            )
                                            var data = BaseResponseJsonData<String>()
                                            var response: Response
                                            try {
                                                response =
                                                    BaseRequest(
                                                        listOf(
                                                            BaseSentJsonData("inviter", request.inviter),//申请者的id
                                                            BaseSentJsonData("target", request.inviter),//申请者的id
                                                            BaseSentJsonData("room", request.room),//申请的房间的id
                                                        ),
                                                        "/passinviting"
                                                    ).sendPostRequest(coroutineScope)
                                                data = JsonToBaseResponse<String>(response).getResponseData()
                                                Log.d("roomScreen", "同意加入房间成功！${data.msg}")
                                            } catch (e: Exception) {
                                                Log.e("roomScreen", "同意加入房间失败！${e}")
                                            }
                                        }
                                    }
                                },
                                // 处理拒绝逻辑
                                onReject = {
                                    coroutineScope.launch {
                                        withContext(Dispatchers.IO) {
                                            Log.d("roomScreen",
                                                "inviter:${request.inviter} " +
                                                        "target:${request.inviter} " +
                                                        "room:${request.room} "
                                            )
                                            var data = BaseResponseJsonData<String>()
                                            var response: Response
                                            try {
                                                response =
                                                    BaseRequest(
                                                        listOf(
                                                            BaseSentJsonData("inviter", request.inviter),//申请者的id
                                                            BaseSentJsonData("target", request.inviter),//申请者的id
                                                            BaseSentJsonData("room", request.room),//申请的房间的id
                                                        ),
                                                        "/refuseinviting"
                                                    ).sendPostRequest(coroutineScope)
                                                data = JsonToBaseResponse<String>(response).getResponseData()
                                                Log.d("roomScreen", "拒绝加入房间成功！${data.msg}")
                                            } catch (e: Exception) {
                                                Log.e("roomScreen", "拒绝加入房间失败！${e}")
                                            }
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
                                BaseInformation.currentRoom = BaseInformation.currentRoom.copy(r_avatar = uploadedUrl)                            } catch (e: Exception) {
                                // 上传失败处理
                                Log.e("manageRoomContent", "头像上传失败: $e")
                            }
                        }
                        // 更新视频流地址
                        BaseInformation.currentRoom = BaseInformation.currentRoom.copy(current_url = videoUrl)
                        Log.d("roomScreen","修改视频URL成功！${currentRoom.current_url}")
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
fun roomScreen(
    navController: NavController,
) {
    Log.d("chosePlayroomScreen","现在的房间ID：${currentRoom.r_id},视频连接：${currentRoom.current_url},头像URL${currentRoom.r_avatar},房间介绍${currentRoom.r_introduction},房间名字${currentRoom.r_name}")
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var currentTab by remember { mutableStateOf(0) }
    var messageInput by remember { mutableStateOf("") }
    val messageList by getPlayroomMessage(context, currentRoom.r_id)
        .collectAsStateWithLifecycle(initialValue = emptyList())//直接获取Flow并且转换为Status

    // 添加 LazyListState 来控制滚动
    val listState = rememberLazyListState()

    // 当消息列表更新时，自动滚动到底部
    LaunchedEffect(messageList.size) {
        if (messageList.isNotEmpty()) {
            listState.animateScrollToItem(messageList.size - 1)
        }
    }
    getMembers(currentRoom.r_id)
    getInvitations(currentRoom.r_id)

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
                        text = { Text("在线成员") }
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
                        ){
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

            // 底部输入框
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextField(
                    value = messageInput,
                    onValueChange = { messageInput = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("输入弹幕...") }
                )
                Button(
                    onClick = {
                        if (messageInput.isNotBlank()) {
                            //messageList.add(messageInput)
                            scope.launch {//保存弹幕信息
                                messageInput = savePlayroomMessage(
                                    context,
                                    PlayroomContent(
                                        0,
                                        currentRoom.r_id,
                                        userInfo.u_id,
                                        userInfo.u_name,
                                        messageInput,
                                        LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                                    )
                                )
                            }
                        }
                    },
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Text("发送")
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