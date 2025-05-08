package com.example.myplayer.framework.playroom

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import com.example.myplayer.model.playroom.Playroom
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.myplayer.network.BaseInformation.currentRoom
import com.example.myplayer.network.DatabaseProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import okhttp3.Response


@Composable
fun chosePlayroomScreen() {
    val localNavController = rememberNavController()
    NavHost(
        navController = localNavController,
        startDestination = "playroomList"
    ) {
        composable("playroomList") {
            Column {
                PlayroomListItem(localNavController)
            }
        }
        composable("room/{roomId}") {
            backStackEntry ->
            roomScreen(
                roomId = backStackEntry.arguments?.getString("roomId") ?: "0000000",
                videoUrl = currentRoom.current_url
            )
        }
    }
}

@Composable
private fun PlayroomListItem(
    localNavController : NavHostController
)
{
    val scope = rememberCoroutineScope()
    var showManageDialog by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    // 使用State而不是StateList以避免集合操作问题
    var playrooms by remember { mutableStateOf<Flow<List<Playroom>>>(flowOf(emptyList())) }
    var response by remember { mutableStateOf<Response?>(null) }
    val wsClient = remember { WebSocketClient(scope) }
    var message by remember { mutableStateOf("") }
    val navController = rememberNavController()

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
                    current_url = "none"
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
    ) {
        padding ->
        Column(modifier = Modifier.padding(padding)) {
// 然后在 LazyColumn 中使用 collectAsState
            val playroomsList by playrooms.collectAsState(initial = emptyList())
            // 播放室列表，这里是把从数据库里拿到的播放室都摆到中间键里，也就是加载播放室列表
            LazyColumn {
                itemsIndexed(playroomsList) { index, room ->
                    var room = playroomsList[index]
                    room.current_url = "http://10.61.164.47:9990/test.mp4"//测试用
                    PlayroomItem(
                        room = room,
                        onJoin = {
                            currentRoom = room
                            showManageDialog = true
                            localNavController.navigate("room/${room.r_id}?videoUrl=${room.current_url}")
                        },
                        onManage = {

                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun PlayroomItem(room: Playroom, onJoin: () -> Unit, onManage: () -> Unit)
{
    // 使用State而不是StateList
    var memberCount by remember { mutableStateOf(0) }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        // 在PlayroomItem的onJoin回调中添加
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