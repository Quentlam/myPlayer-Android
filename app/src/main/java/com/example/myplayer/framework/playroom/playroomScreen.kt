package com.example.myplayer.framework.playroom

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import com.example.myplayer.model.playroom.Playroom
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.example.myplayer.model.playroom.Member
import com.example.myplayer.network.BaseRequest
import com.example.myplayer.network.DatabaseProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject


@Composable
fun playroomScreen() {
    val scope = rememberCoroutineScope()
    var showManageDialog by remember { mutableStateOf(false) }
    var currentRoom by remember { mutableStateOf<Playroom?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    // 使用State而不是StateList以避免集合操作问题
    var playrooms by remember { mutableStateOf<Flow<List<Playroom>>>(flowOf(emptyList())) }
    var response by remember { mutableStateOf<Response?>(null) }
    val wsClient = remember { WebSocketClient(scope) }
    var message by remember { mutableStateOf("") }

    val context = LocalContext.current

    // 使用try-catch包裹数据库操作
    LaunchedEffect(Unit) {
                val dao = DatabaseProvider.getDatabase(context).playroomDao()
                // 插入数据
                val playroom = Playroom(
                    r_id = "124",
                    r_name = "游戏室2",
                    r_avatar = "avatar_url",
                    r_introduction = "这是一个游戏室",
                    current_url = "current_url"
                )
                dao.insertPlayroom(playroom)
                // 查询数据
                playrooms = dao.getAllPlayrooms()
    }

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { /* 创建新播放室逻辑 */ },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("新建播放室") }
            )
        },
        topBar = {
            CustomTopAppBar(
                searchQuery = searchQuery,
                onSearchQueryChange = { searchQuery = it },
                onAddClick = { /* 创建新播放室逻辑 */ }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
//            if (playrooms.isEmpty()) {
//                Text("{$message}")
//            }
// 然后在 LazyColumn 中使用 collectAsState
            val playroomsList by playrooms.collectAsState(initial = emptyList())
            // 播放室列表
            LazyColumn {
                itemsIndexed(playroomsList) { index, room ->
                    val room = playroomsList[index]
                    PlayroomItem(
                        room = room,
                        onJoin = { /* 加入播放室逻辑 */ },
                        onManage = {
                            currentRoom = room
                            showManageDialog = true
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun PlayroomItem(room: Playroom, onJoin: () -> Unit, onManage: () -> Unit) {
    // 使用State而不是StateList
    var memberCount by remember { mutableStateOf(0) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onManage) {
                Icon(Icons.Default.Settings, contentDescription = "管理")
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(room.r_name, style = MaterialTheme.typography.titleMedium)
                Text("在线人数: $memberCount", style = MaterialTheme.typography.bodySmall)
            }
            Button(onClick = onJoin) {
                Text("加入")
            }
        }
    }
}


// WebSocket客户端封装
class WebSocketClient(private val coroutineScope: CoroutineScope) {
    private var webSocket: WebSocket? = null
    var currentPlaybackPosition by mutableStateOf(0L)
    var isPlaying by mutableStateOf(false)

    fun connect(url: String) {
        require(url.isNotBlank()) { "URL不能为空" }
        coroutineScope.launch {
            try {
                if (webSocket != null) {
                    disconnect()
                }
                val serverUrl = "ws://api.myplayer.com/room/$url/ws"
                val client = OkHttpClient()
                val request = Request.Builder().url(serverUrl).build()
                webSocket = client.newWebSocket(request, object : WebSocketListener() {
                    override fun onMessage(webSocket: WebSocket, text: String) {
                        when (JSONObject(text).optString("type")) {
                            "SYNC" -> {
                                currentPlaybackPosition = JSONObject(text).getLong("position")
                                isPlaying = JSONObject(text).getBoolean("isPlaying")
                            }
                        }
                    }
                })
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }




    fun sendCommand(position: Long, play: Boolean) {
        webSocket?.send(
            JSONObject()
                .put("type", "CONTROL")
                .put("position", position)
                .put("isPlaying", play)
                .toString()
        )
    }

    fun disconnect() {
        try {
            webSocket?.close(1000, "正常关闭")
            webSocket = null
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            currentPlaybackPosition = 0L
            isPlaying = false
        }
    }

}

@Composable
fun CustomTopAppBar(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onAddClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .background(Color(0xFFF19E9E))  // 使用固定的紫色作为背景
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .height(40.dp)
                .background(Color.White, RoundedCornerShape(4.dp))
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp)
            ) {
                Icon(
                    Icons.Default.Search,
                    contentDescription = null,
                    tint = Color.Gray
                )
                Spacer(modifier = Modifier.width(8.dp))
                BasicTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    textStyle = TextStyle(color = Color.Black),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            if (searchQuery.isEmpty()) {
                Text(
                    "搜索播放室",
                    color = Color.Gray,
                    modifier = Modifier.padding(start = 40.dp, top = 10.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        IconButton(onClick = onAddClick) {
            Icon(
                Icons.Default.Add,
                contentDescription = "添加播放室",
                tint = Color.White
            )
        }
    }
}