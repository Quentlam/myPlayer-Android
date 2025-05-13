package com.example.myplayer.framework.playroom

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.myplayer.jsonToModel.JsonToBaseResponse
import com.example.myplayer.model.BaseResponseJsonData
import com.example.myplayer.model.BaseSentJsonData
import com.example.myplayer.model.playroom.RequestDetails
import com.example.myplayer.network.BaseInformation.currentMemberList
import com.example.myplayer.network.BaseInformation.currentRequestList
import com.example.myplayer.network.BaseInformation.currentRoom
import com.example.myplayer.network.BaseRequest
import com.example.myplayer.network.LoginRequest
import com.example.myplayer.userInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Response


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
    getMembers(currentRoom.r_id)
    getInvitations(currentRoom.r_id)

    var currentTab by remember { mutableStateOf(0) }
    var messageInput by remember { mutableStateOf("") }
    val danmuList = remember { mutableStateListOf<String>() }
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
                        0 -> LazyColumn {
                            items(danmuList.size) { index ->
                                Text(danmuList[index], modifier = Modifier.padding(8.dp))
                            }
                        }

                        1 -> LazyColumn {
                            items(currentMemberList.size) { member ->
                                Text(currentMemberList[member].m_name, modifier = Modifier.padding(8.dp))
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
                            danmuList.add(messageInput)
                            messageInput = ""
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

