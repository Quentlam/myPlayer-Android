package com.example.myplayer.framework.playroom

import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PlayArrow
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.myplayer.model.BaseResponseJsonData
import com.example.myplayer.model.playroom.RequestDetails
import com.example.myplayer.model.playroom.Member
import com.example.myplayer.model.playroom.Playroom
import com.example.myplayer.network.BaseInformation.currentMemberList
import com.example.myplayer.network.BaseInformation.currentRequestList
import com.example.myplayer.network.BaseInformation.currentRoom
import com.example.myplayer.network.BaseInformation.roomList
import com.example.myplayer.network.BaseInformation.testUrl
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
fun getInvitations(roomId: String)
{
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(roomId) {
        try {
            isLoading = true
            error = null

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



suspend fun getAllPlayrooms(coroutineScope: CoroutineScope): Flow<List<Playroom>> = flow {
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
        throw e
    }
}.flowOn(Dispatchers.IO)

suspend fun loadAllPlayroom(coroutineScope: CoroutineScope) : Boolean
{
    try {
        withContext(Dispatchers.IO) {
            getAllPlayrooms(coroutineScope)
                .collect { rooms ->
                    withContext(Dispatchers.Main) {
                        roomList = rooms ?: emptyList()
                    }
                }
        }
        Log.d("preparePlayroomData","加载房间内容成功！:${roomList}")
        return false
    } catch (e: Exception) {
        withContext(Dispatchers.Main) {
            Log.e("preparePlayroomData","加载房间内容失败！:${e.message}")
        }
    }
    return true
}


@Composable
fun setPlayroomList(
    localNavController : NavHostController
)
{
    val scope = rememberCoroutineScope()
    var showManageDialog by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    // 修改状态声明
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    // 使用 collectAsState 来收集 Flow
    LaunchedEffect(Unit) {
                isLoading = loadAllPlayroom(scope)
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
            customTopAppBar(
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
                               isLoading = loadAllPlayroom(scope)
                            }
                        }) {
                            Text("重试")
                        }
                    }
                }
            } else if (roomList.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("暂无播放室")
                }
            } else {
                LazyColumn {
                    items(roomList) { room ->
                        room.current_url = testUrl//用于测试
                        setPlayroomItem(
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

@Composable
fun loadMessage()
{

}