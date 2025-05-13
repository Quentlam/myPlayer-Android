package com.example.myplayer.framework.playroom

import android.util.Log
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import com.example.myplayer.model.playroom.Playroom
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.myplayer.model.BaseResponseJsonData
import com.example.myplayer.network.BaseInformation.currentRoom
import com.example.myplayer.network.networkAPI.GetRequest
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


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

    // 修改状态声明
    var isLoading by remember { mutableStateOf(true) }
    var playrooms by remember { mutableStateOf<List<Playroom>>(emptyList()) }
    var error by remember { mutableStateOf<String?>(null) }

    // 使用 collectAsState 来收集 Flow
    LaunchedEffect(Unit) {
        try {
            withContext(Dispatchers.IO) {
                getAllPlayrooms(scope)
                    .collect { rooms ->
                        withContext(Dispatchers.Main) {
                            playrooms = rooms ?: emptyList()
                            isLoading = false
                        }
                    }
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                error = e.message
                isLoading = false
                Log.e("Playrooms", "Error loading playrooms: ${e.message}")
            }
        }
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
                                try {
                                    withContext(Dispatchers.IO) {
                                        getAllPlayrooms(scope)
                                            .collect { rooms ->
                                                withContext(Dispatchers.Main) {
                                                    playrooms = rooms ?: emptyList()
                                                    isLoading = false
                                                }
                                            }
                                    }
                                } catch (e: Exception) {
                                    withContext(Dispatchers.Main) {
                                        error = e.message
                                        isLoading = false
                                    }
                                }
                            }
                        }) {
                            Text("重试")
                        }
                    }
                }
            } else if (playrooms.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("暂无播放室")
                }
            } else {
                LazyColumn {
                    items(playrooms) { room ->
                        room.current_url = "http://10.61.164.47:9990/test.mp4"

                        PlayroomItem(
                            room = room,
                            onJoin = {
                                currentRoom = room
                                showManageDialog = true
                                localNavController.navigate("room/${room.r_id}?videoUrl=${room.current_url}")
                            },
                            onManage = {}
                        )
                    }
                }
            }
        }
    }
}

suspend fun getAllPlayrooms(coroutineScope: CoroutineScope): Flow<List<Playroom>> = flow {
    try {
        val request = GetRequest(
            interfaceName = "/room/getrooms",
            queryParams = mapOf()
        )
        val response = request.execute(coroutineScope)
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
        throw e
    }
}.flowOn(Dispatchers.IO)



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