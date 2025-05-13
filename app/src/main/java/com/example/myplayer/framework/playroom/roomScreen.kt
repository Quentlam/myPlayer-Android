package com.example.myplayer.framework.playroom

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.example.myplayer.model.playroom.Member


@Composable
fun manageRoomScreen(
    onBack: () -> Unit,
    onAvatarUpdate: (String) -> Unit
) {
    var newAvatarUrl by remember { mutableStateOf(currentRoom.r_avatar) }
    var showInvitationDialog by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    if(showInvitationDialog)
    {
        AlertDialog(
            onDismissRequest = { showInvitationDialog = false },
            title = { Text("加入申请列表") },
            text = {
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
                                                        //BaseSentJsonData("status", 1),//状态

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
                                                        //BaseSentJsonData("status", 1),//状态

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
            },
            confirmButton = {
                TextButton(onClick = { showInvitationDialog = false }) {
                    Text("关闭")
                }
            }
        )
    }

    Column(modifier = Modifier.padding(16.dp)) {
        Text("房间ID: ${currentRoom.r_id}")
        Spacer(modifier = Modifier.height(8.dp))
        Text("当前视频流: ${currentRoom.current_url}")
        Spacer(modifier = Modifier.height(16.dp))
        Text("当前房间成员数: ${currentMemberList.size}")
        Spacer(modifier = Modifier.height(16.dp))
        AsyncImage(
            model = currentRoom.r_avatar,
            contentDescription = "房间头像",
            modifier = Modifier
                .size(100.dp)
                .background(Color.LightGray)
        )

        Button(
            onClick = { /* TODO: 实现头像修改逻辑 */ },
            modifier = Modifier.padding(top = 8.dp)
        ) {
            Text("修改头像")
        }
        Button(
            onClick = {
                showInvitationDialog = true
            },
            modifier = Modifier.padding(top = 8.dp)
        ) {
            Text("查看房间加入申请")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = onBack) {
            Text("返回")
        }
    }
}


@Composable
fun roomScreen(
    navController: NavController,
) {
    Log.d("chosePlayroomScreen","现在的房间ID：${currentRoom.r_id},视频连接：${currentRoom.current_url}")
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
                exoPlayerView(
                    context = LocalContext.current,
                    videoUrl = currentRoom.current_url,
                    lifecycleOwner = LocalLifecycleOwner.current
                )
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
                        onClick = {
                            navController.navigate("manageRoom/${currentRoom.r_id}")
                                  },
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