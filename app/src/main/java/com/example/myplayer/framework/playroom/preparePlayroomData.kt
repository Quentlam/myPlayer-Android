package com.example.myplayer.framework.playroom

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.myplayer.jsonToModel.JsonToBaseResponse
import com.example.myplayer.model.BaseResponseJsonData
import com.example.myplayer.model.BaseSentJsonData
import com.example.myplayer.model.playroom.JoinMessage
import com.example.myplayer.model.playroom.RequestDetails
import com.example.myplayer.model.playroom.ReadyMessage
import com.example.myplayer.model.playroom.StartMessage
import com.example.myplayer.model.playroom.StopMessage
import com.example.myplayer.model.playroom.SynchronousRequestMessage
import com.example.myplayer.model.playroom.RoomWebSocketMessage
import com.example.myplayer.model.playroom.Member
import com.example.myplayer.model.playroom.Message
import com.example.myplayer.model.playroom.Playroom
import com.example.myplayer.model.playroom.PlayroomContent
import com.example.myplayer.model.playroom.SynchronousResponseMessage
import com.example.myplayer.model.playroom.UrlMessage
import com.example.myplayer.model.BaseInformation.currentMemberList
import com.example.myplayer.model.BaseInformation.currentRequestList
import com.example.myplayer.model.BaseInformation.currentRoom
import com.example.myplayer.model.BaseInformation.roomList
import com.example.myplayer.model.BaseInformation.testUrl2
import com.example.myplayer.network.BaseRequest
import com.example.myplayer.model.DatabaseProvider
import com.example.myplayer.network.networkAPI.GetRequest
import com.example.myplayer.userInfo
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener


@Composable
fun getInvitations(roomId: String)
{
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(roomId) {
        isLoading = true
        error = null
        try {
            val inviteRequest = GetRequest(
                interfaceName = "/inviting/getinvitings/${roomId}",
                queryParams = mapOf()
            )
            // 在协程中执行网络请求
            withContext(Dispatchers.IO) {
                val inviteResponse = inviteRequest.execute(scope)
                val gson = Gson()
                val inviteType = object : TypeToken<BaseResponseJsonData<List<RequestDetails>>>() {}.type
                val inviteData = gson.fromJson<BaseResponseJsonData<List<RequestDetails>>>(
                    inviteResponse.body?.string(),
                    inviteType
                )

                // 更新状态
                withContext(Dispatchers.Main) {
                    currentRequestList = inviteData.data ?: emptyList()
                    isLoading = false
                }
                Log.d("roomScreen", "申请列表获取成功")
                Log.d("roomScreen", currentRequestList.toString())
            }
        } catch (e: Exception) {
            Log.e("roomScreen", "申请列表获取失败", e)
            error = "申请列表获取失败: ${e.message}"
            isLoading = false
        }
    }
}


@Composable
fun getMembers(roomId: String)
{
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(roomId) {
        try {
            isLoading = true
            error = null

            val memberRequest = GetRequest(
                interfaceName = "/room/getmember",
                queryParams = mapOf("r_id" to roomId)
            )

            // 在协程中执行网络请求
            withContext(Dispatchers.IO) {
                val memberResponse = memberRequest.execute(scope)
                val gson = Gson()
                val memberType = object : TypeToken<BaseResponseJsonData<List<Member>>>() {}.type
                val memberData = gson.fromJson<BaseResponseJsonData<List<Member>>>(
                    memberResponse.body?.string(),
                    memberType
                )

                // 更新状态
                withContext(Dispatchers.Main) {
                    currentMemberList = memberData.data ?: emptyList()
                    isLoading = false
                }
                Log.d("roomScreen", "加载成员列表成功，已加载成员：${currentMemberList.size}")
            }
        } catch (e: Exception) {
            Log.e("roomScreen", "加载成员列表失败", e)
            error = "加载成员列表失败: ${e.message}"
            isLoading = false
        }
    }
}



fun getAllPlayrooms(coroutineScope: CoroutineScope,context: Context): Flow<List<Playroom>> = flow {
    try {
        val playroomRequest = GetRequest(
            interfaceName = "/room/getrooms",
            queryParams = mapOf()
        )
        val response = playroomRequest.execute(coroutineScope)
        val gson = Gson()
        val type = object : TypeToken<BaseResponseJsonData<List<Playroom>>>() {}.type
        val data = gson.fromJson<BaseResponseJsonData<List<Playroom>>>(response.body?.string(), type)

        if (data.data != null) {
            Log.d("Playrooms", "获取播放室列表成功：${data.data}")
            emit(data.data)
        } else {
            Log.e("Playrooms", "获取播放室列表失败：${data.msg}")
            emit(emptyList())
        }
    } catch (e: Exception) {
        Log.e("Playrooms", "获取播放室列表异常：${e.message}")
        CoroutineScope(Dispatchers.Main).launch{
            Toast.makeText(context, "已离线！", Toast.LENGTH_SHORT).show()
        }
        throw e
    }
}.flowOn(Dispatchers.IO)

suspend fun loadAllPlayroom(coroutineScope: CoroutineScope,context: Context) : Boolean
{
    try {
        withContext(Dispatchers.IO) {
            getAllPlayrooms(coroutineScope, context)
                .collect { rooms ->
                    withContext(Dispatchers.Main) {
                        roomList = rooms ?: emptyList()
                    }
                }
        }
        Log.d("preparePlayroomData","加载房间内容成功！:${roomList}")
        Toast.makeText(context, "加载房间内容成功！", Toast.LENGTH_SHORT).show()
        return false
    } catch (e: Exception) {
        withContext(Dispatchers.Main) {
            Log.e("preparePlayroomData","加载房间内容失败！:${e.message}")
            Toast.makeText(context, "加载房间内容失败！:${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    return true
}


@Composable
fun playroomListScreen(onJoinRoom: (Playroom) -> Unit) {
    val scope = rememberCoroutineScope()
    var searchQuery by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var searchRoomList by remember { mutableStateOf<List<Playroom>>(emptyList()) }

    val context = LocalContext.current

    // 这是过滤后的列表状态，默认为全部
    var filteredRoomList by remember { mutableStateOf(roomList.toList()) }

    LaunchedEffect(Unit) {
        isLoading = loadAllPlayroom(scope,context)
        // 加载后初始化过滤列表为全部
        filteredRoomList = roomList.toList()
    }

    var showAddPlayroomDialog by remember { mutableStateOf(false) }
    var showSearchRoomDialog by remember { mutableStateOf(false) }
    var showSearchRoomListDialog by remember { mutableStateOf(false) }
    var showInviteCodeDialog by remember { mutableStateOf(false) }


    if (showSearchRoomDialog) {
        SearchRoomDialog(
            onDismiss = { showSearchRoomDialog = false },
            onSearch = { roomName ->
                scope.launch {
                    try {
                        val rooms = searchPlayroomsByName(scope, roomName, context).first() ?: emptyList()
                        searchRoomList = rooms
                        showSearchRoomDialog = false  // 关闭输入框
                        showSearchRoomListDialog = true // 弹出显示结果
                    } catch (e: Exception) {
                        Log.e("preparePlayroomData", "搜索失败：${e.message}")
                        // 可考虑显示错误信息
                    }
                }
            }
        )
    }
    if (showSearchRoomListDialog) {
        SearchResultDialog(
            searchRoomList = searchRoomList,
            onDismiss = { showSearchRoomListDialog = false },
            onJoinRoom = { room ->
                val request = BaseRequest(
                    listOf(
                        BaseSentJsonData("inviter", userInfo.u_id),
                        BaseSentJsonData("target", userInfo.u_id),
                        BaseSentJsonData("room", room.r_id)
                    ),
                    "/inviting/sendinviting"
                )
                var response : Response? = null
                scope.launch {
                    try{
                        withContext(Dispatchers.IO)
                        {
                            response = request.sendPostRequest(scope)
                            val data = JsonToBaseResponse<String>(response!!).getResponseData()
                            Log.d("preparePlayroomData","申请加入房间成功！：${data.msg}")
                        }
                        showSearchRoomListDialog = false
                        Toast.makeText(context, "申请加入房间成功！", Toast.LENGTH_SHORT).show()
                    }
                    catch (e : Exception)
                    {
                        Toast.makeText(context, "申请加入房间失败！", Toast.LENGTH_SHORT).show()
                        Log.e("preparePlayroomData","申请加入房间失败！：${e.message},${response?.body.toString()}")
                    }
                }
            }
        )
    }


    // 邀请码弹窗
    if (showInviteCodeDialog) {
        InviteCodeDialog(
            onDismiss = { showInviteCodeDialog = false },
            onConfirm = { inviteCode ->
                // TODO: 这里处理邀请码逻辑，inviteCode 是输入内容
                println("输入的邀请码：$inviteCode")
                showInviteCodeDialog = false
            }
        )
    }

    if(showAddPlayroomDialog) {
        AlertDialog(
            onDismissRequest = { showAddPlayroomDialog = false },
            title = { Text("添加新的播放室") },
            text = {
                AddNewPlayroom(
                    onCreatePlayroom = { newPlayroom ->
                        scope.launch {
                            isLoading = true
                            isLoading = loadAllPlayroom(scope,context)
                            filteredRoomList = roomList.toList() // 刷新过滤列表
                        }
                        showAddPlayroomDialog = false
                    },
                    onCancel = {
                        showAddPlayroomDialog = false
                    }
                )
            },
            confirmButton = {}
        )
    }

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddPlayroomDialog = true },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("新建播放室") }
            )
        },
        topBar = {
            customTopAppBar(
                searchQuery = searchQuery,
                onSearchQueryChange = { searchQuery = it },
                onSearchClick = {
                    // 搜索按钮点击时执行筛选
                    filteredRoomList = if (searchQuery.isBlank()) {
                        roomList.toList() // 空关键字显示全部
                    } else {
                        roomList.filter { room ->
                            // 您可以根据需求调整筛选条件，比如按房间名或者简介筛选
                            (room.r_name?.contains(searchQuery, ignoreCase = true) == true) ||
                                    (room.r_introduction?.contains(searchQuery, ignoreCase = true) == true)
                        }
                    }
                },
                onSearchFriendPlayroomClick = {
                    showSearchRoomDialog = true
                },
                onInviteCodeClick = {
                    showInviteCodeDialog = true
                }

            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (error != null) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("加载失败: $error", color = Color.Red)
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = {
                            error = null
                            isLoading = true
                            scope.launch {
                                isLoading = loadAllPlayroom(scope,context)
                                filteredRoomList = roomList.toList()
                            }
                        }) {
                            Text("重试")
                        }
                    }
                }
            } else if (filteredRoomList.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("暂无符合条件的播放室")
                }
            } else {
                LazyColumn {
                    items(filteredRoomList) { room ->
                        room.current_url = testUrl2 // 用于测试
                        setPlayroomItem(
                            room = room,
                            onJoin = { onJoinRoom(room) },
                            onManage = {}
                        )
                    }
                }
            }
        }
    }
}




@Composable
private fun setPlayroomItem(room: Playroom, onJoin: () -> Unit, onManage: () -> Unit)
{
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        // 在PlayroomItem的onJoin回调中添加
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onManage) {
                Icon(Icons.Default.PlayArrow, contentDescription = "管理")
            }
            Column(modifier = Modifier.weight(1f)) {
                Text("房间名称：${room.r_name}", style = MaterialTheme.typography.titleMedium)
                Text("介绍：${room.r_introduction}", style = MaterialTheme.typography.bodyMedium)
            }
            Button(onClick = onJoin) {
                Text("加入")
            }
        }
    }
}

fun getPlayroomMessage(context : Context,roomId : String) : Flow<List<PlayroomContent>>
{
    try {
        val dao = DatabaseProvider.getPlayRoomDatabase(context).playroomContentDao()
        Log.d("preparePlayroomData","获取当前房间的弹幕信息成功！")
        return dao.getCurrentPlayroomContent(roomId)
    }
    catch (e : Exception)
    {
        Log.e("preparePlayroomData","弹幕消息存储失败！${e.message}")
        return emptyFlow()
    }
}

@Composable
fun AddNewPlayroom(
    onCreatePlayroom: (Playroom) -> Unit,
    onCancel: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    var showErrorDialog by remember { mutableStateOf(false) }
    if (showErrorDialog) {
        AlertDialog(
            onDismissRequest = { showErrorDialog = false },
            title = { Text("提示") },
            text = { Text("创建失败") },
            confirmButton = {
                TextButton(onClick = { showErrorDialog = false }) {
                    Text("确定")
                }
            }
        )
    }


    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        var roomName by remember { mutableStateOf("") }
        var roomAvatar by remember { mutableStateOf("") }
        var roomIntroduction by remember { mutableStateOf("") }
        var currentUrl by remember { mutableStateOf("") }

        // 播放室名称
        OutlinedTextField(
            value = roomName,
            onValueChange = { roomName = it },
            label = { Text("播放室名称") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        // 播放室头像URL
        OutlinedTextField(
            value = roomAvatar,
            onValueChange = { roomAvatar = it },
            label = { Text("播放室头像URL") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        // 播放室简介
        OutlinedTextField(
            value = roomIntroduction,
            onValueChange = { roomIntroduction = it },
            label = { Text("播放室简介") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3,
            maxLines = 5
        )

        // 当前播放URL
        OutlinedTextField(
            value = currentUrl,
            onValueChange = { currentUrl = it },
            label = { Text("当前播放URL") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
        ) {
            TextButton(onClick = onCancel) {
                Text("取消")
            }

            Button(
                onClick = {
                    val roomId = java.util.UUID.randomUUID().toString()
                    val newPlayroom = Playroom(
                        r_id = roomId,
                        r_name = roomName,
                        r_avatar = roomAvatar,
                        r_introduction = roomIntroduction,
                        current_url = currentUrl
                    )
                    coroutineScope.launch {
                        withContext(Dispatchers.IO) {
                            try {
                                val response = BaseRequest(
                                    listOf(
                                        BaseSentJsonData("r_name", roomName)
                                    ),
                                    "/room/create"
                                ).sendPostRequest(coroutineScope)
                                val data = JsonToBaseResponse<String>(response).getResponseData()
                                Log.i("preparePlayroomData", "创建房间成功！：${data.msg}")
                                Toast.makeText(context, "创建房间成功！", Toast.LENGTH_SHORT).show()
                                // 创建成功后，重新加载播放室列表
                                loadAllPlayroom(coroutineScope,context)
                                // 通知父组件创建成功
                                withContext(Dispatchers.Main) {
                                    onCreatePlayroom(newPlayroom)
                                }
                            } catch(e: Exception) {
                                withContext(Dispatchers.Main) {
                                    showErrorDialog = true
                                }
                                Toast.makeText(context, "创建房间失败！${e.message}", Toast.LENGTH_SHORT).show()
                                Log.d("preparePlayroomData", e.toString())
                            }
                        }
                    }
                },
                enabled = roomName.isNotBlank()
            ) {
                Text("创建播放室")
            }
        }
    }
}


var playRoomWebSocketManager: PlayroomWebSocketManager? = null
suspend fun connectToPlayroomWS(
    context: Context,
    coroutineScope: CoroutineScope,
    messageHandler: PlayroomMessageHandler
) {
    try {
        withContext(Dispatchers.IO)
        {
            playRoomWebSocketManager =
                PlayroomWebSocketManager("wss://www.myplayer.merlin.xin/video?u_id=${userInfo.u_id}&u_name=${userInfo.u_name}&r_id=${currentRoom.r_id}")
            val json = Json {
                ignoreUnknownKeys = true
                classDiscriminator = "type" // 与服务端字段对应，不能改
                isLenient = true
                serializersModule = SerializersModule {
                    polymorphic(RoomWebSocketMessage::class) {
                        subclass(JoinMessage::class, JoinMessage.serializer())
                        subclass(UrlMessage::class, UrlMessage.serializer())
                        subclass(ReadyMessage::class, ReadyMessage.serializer())
                        subclass(StartMessage::class, StartMessage.serializer())
                        subclass(StopMessage::class, StopMessage.serializer())
                        subclass(
                            SynchronousRequestMessage::class,
                            SynchronousRequestMessage.serializer()
                        )
                        subclass(
                            SynchronousResponseMessage::class,
                            SynchronousResponseMessage.serializer()
                        )
                    }
                }
            }
            val mainHandler = Handler(Looper.getMainLooper())
            val listener = object : WebSocketListener() {
                override fun onMessage(webSocket: WebSocket, text: String) {

                    val msg = try {
                        json.decodeFromString(RoomWebSocketMessage.serializer(), text)
                    } catch (e: Exception) {
                        Log.e("PlayroomWebSocketManager", "反序列化消息失败：$text", e)
                        return
                    }

                    when (msg) {
                        is JoinMessage -> messageHandler.onUserJoined(context, msg)
                        is UrlMessage -> messageHandler.onUrlReceived(msg)
                        is ReadyMessage -> messageHandler.onUserReady(msg)
                        is StartMessage -> messageHandler.onStart(msg)
                        is StopMessage -> messageHandler.onStop(msg)
                        is SynchronousRequestMessage -> messageHandler.onSynchronousRequest(msg)
                        is SynchronousResponseMessage -> messageHandler.onSynchronousResponse(msg)
                        is Message -> messageHandler.onChatMessage(context, coroutineScope, msg)
                    }
                }

                override fun onOpen(webSocket: WebSocket, response: Response) {
                    super.onOpen(webSocket, response)
                    Log.d("PlayroomWebSocketManager", "房间：${currentRoom.r_id}WebSocket连接成功！")
                    mainHandler.post {
                        Toast.makeText(context, "连接房间成功！", Toast.LENGTH_SHORT).show()
                    }
                    // 此处可以通知UI或更新状态：连接已建立
                }

                override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                    Log.e("PlayroomWebSocketManager", "WebSocket连接失败，准备重连", t)
                    mainHandler.post {
                        Toast.makeText(context, "与房间断开连接！准备重连", Toast.LENGTH_SHORT)
                            .show()
                    }
                    restartWebSocketWithDelay()
                }

                override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                    Log.d("PlayroomWebSocketManager", "WebSocket已关闭，准备重连")
                    mainHandler.post {
                        Toast.makeText(context, "房间关闭连接！准备重连", Toast.LENGTH_SHORT).show()
                    }
                    restartWebSocketWithDelay()
                }

                private fun restartWebSocketWithDelay() {
                    try {
                        // 3秒后重连，避免频繁重连导致资源浪费或被封禁
                        Handler(Looper.getMainLooper()).postDelayed({
                            Log.d("PlayroomWebSocketManager", "开始重连WebSocket")
                            mainHandler.post {
                                Toast.makeText(context, "开始重连WebSocket", Toast.LENGTH_SHORT)
                                    .show()
                            }
                            // 重新调用连接函数
                            coroutineScope.launch {
                                connectToPlayroomWS(
                                    context,
                                    coroutineScope,
                                    messageHandler
                                )
                            }
                        }, 3000)
                    } catch (e: Exception) {
                        Log.e("PlayroomWebSocketManager", "WebSocket重连失败:${e.message}")
                        mainHandler.post {
                            Toast.makeText(
                                context,
                                "WebSocket重连失败:${e.message}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            }
            playRoomWebSocketManager?.connect(listener)
            Log.d(
                "PlayroomWebSocketManager",
                "尝试连接：wss://www.myplayer.merlin.xin/video?u_id=${userInfo.u_id}&u_name=${userInfo.u_name}&r_id=${currentRoom.r_id}"
            )
            mainHandler.post {
                Toast.makeText(context, "重连房间中！", Toast.LENGTH_SHORT).show()
            }
        }
    } catch (e: Exception) {
        Log.e("PlayroomWebSocketManager", "连接webSocket失败", e)
        Toast.makeText(context, "连接房间失败！:${e.message}", Toast.LENGTH_SHORT).show()
    }
}



@Composable
fun SearchRoomDialog(
    onDismiss: () -> Unit,
    onSearch: (String) -> Unit
) {
    var roomName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "搜索朋友播放室") },
        text = {
            TextField(
                value = roomName,
                onValueChange = { roomName = it },
                label = { Text("请输入播放室名称") },
                singleLine = true
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSearch(roomName.trim())
                }
            ) {
                Text("搜索")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss
            ) {
                Text("取消")
            }
        }
    )
}


@Composable
fun InviteCodeDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var inviteCode by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "请输入邀请码") },
        text = {
            TextField(
                value = inviteCode,
                onValueChange = { inviteCode = it },
                label = { Text("邀请码") },
                singleLine = true
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(inviteCode.trim())
                }
            ) {
                Text("确认")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss
            ) {
                Text("取消")
            }
        }
    )
}


fun searchPlayroomsByName(coroutineScope: CoroutineScope,name : String,context: Context): Flow<List<Playroom>> = flow {
    try {
        val playroomRequest = GetRequest(
            interfaceName = "/room/search",
            queryParams = mapOf(
                "r_name" to name
            )
        )
        val response = playroomRequest.execute(coroutineScope)
        val gson = Gson()
        val type = object : TypeToken<BaseResponseJsonData<List<Playroom>>>() {}.type
        val data =
            gson.fromJson<BaseResponseJsonData<List<Playroom>>>(response.body?.string(), type)

        if (data.data != null) {
            Log.d("preparePlayroomData", "搜索播放室成功：${data.data}")
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "搜索播放室成功！", Toast.LENGTH_SHORT).show()
            }
            emit(data.data)
        } else if (data.code != 200) {
            Log.e("preparePlayroomData", "搜索播放室失败：${data.msg}")
            Log.e("preparePlayroomData", "搜索播放室失败：${response}")
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "搜索失败", Toast.LENGTH_SHORT).show()
            }
            emit(emptyList())
        }

    } catch (e: Exception) {
        Log.e("preparePlayroomData", "搜索播放室异常：${e.message}")
        Toast.makeText(context, "搜索播放室异常！：${e.message}", Toast.LENGTH_SHORT).show()

        throw e
    }
}.flowOn(Dispatchers.IO)


@Composable
fun SearchResultDialog(
    searchRoomList: List<Playroom>,
    onDismiss: () -> Unit,
    onJoinRoom: (Playroom) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "搜索结果") },
        text = {
            if (searchRoomList.isEmpty()) {
                Text(text = "没有找到符合条件的播放室", style = MaterialTheme.typography.bodyMedium)
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 300.dp) // 限制最大高度，避免对话框过高
                ) {
                    items(searchRoomList) { room ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(12.dp)
                                    .fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = room.r_name ?: "无名房间",
                                        style = MaterialTheme.typography.titleLarge,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = room.r_introduction ?: "暂无介绍",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                Button(
                                    onClick = { onJoinRoom(room) },
                                    modifier = Modifier.height(36.dp)
                                ) {
                                    Text(text = "加入")
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("关闭")
            }
        }
    )
}