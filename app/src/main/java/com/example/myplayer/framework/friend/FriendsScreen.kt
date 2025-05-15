package com.example.myplayer.framework.friend

import android.annotation.SuppressLint
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBar
import androidx.compose.material3.Text
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
import androidx.compose.ui.unit.dp
import com.example.myplayer.model.BaseResponseJsonData
import com.example.myplayer.network.networkAPI.GetRequest
import com.example.myplayer.model.UserInfo
import com.example.myplayer.userInfo
import com.example.myplayer.webSocketManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter
import coil.compose.rememberImagePainter
import coil.request.ImageRequest
import com.example.myplayer.R
import com.example.myplayer.model.BaseSentJsonData
import com.example.myplayer.network.BaseRequest
import kotlinx.coroutines.launch

val TAG = "FriendsScreen"

@Composable
fun FriendsScreen() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "friendsList"
    ) {
        composable("friendsList") {
            FriendsListScreen(navController = navController)
        }
        composable(
            "friendDetail/{userId}",
            arguments = listOf(navArgument("userId") { type = NavType.StringType })
        ) { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId") ?: ""
            // 这里应该从你的数据源获取对应的UserInfo
            val friend = userInfo.friendList.find { it.u_id == userId } ?: return@composable

            FriendDetailScreen(
                navController = navController,
                friend = friend,
                //userId = userId,
                onBackClick = { navController.popBackStack() }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("CoroutineCreationDuringComposition")
@Composable
fun FriendsListScreen(navController: NavController) {
    var searchQuery by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    // 关键修改：直接观察 userInfo.friendList 的变化
    val friendList by remember { mutableStateOf(userInfo.friendList) }

    // 搜索栏状态
    var active by remember { mutableStateOf(false) }

    // 监听返回时的刷新标志
    val savedStateHandle = navController.currentBackStackEntry?.savedStateHandle
    LaunchedEffect(savedStateHandle) {
        savedStateHandle?.get<Boolean>("shouldRefresh")?.let { shouldRefresh ->
            if (shouldRefresh) {
                // 强制重新加载数据
                coroutineScope.launch {
                    isLoading = true
                    withContext(Dispatchers.IO) {
                        getFriendList(coroutineScope) // 从服务器重新拉取最新数据
                    }
                    isLoading = false
                }
                savedStateHandle.remove<Boolean>("shouldRefresh")
            }
        }
    }

    // 初始加载数据（仅第一次进入时执行）
    LaunchedEffect(Unit) {
        if (userInfo.friendList.isEmpty()) { // 避免重复加载
            isLoading = true
            withContext(Dispatchers.IO) {
                getFriendList(coroutineScope)
            }
            isLoading = false
        }
    }

    Scaffold (
        topBar = {
            Column {
                SearchBar(
                    query = searchQuery,
                    onQueryChange = { searchQuery = it },
                    onSearch = { active = false },
                    active = active,
                    onActiveChange = { active = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    placeholder = {
                        Text("搜索好友")
                    },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Search, contentDescription = "Search Icon")
                    },
                    trailingIcon = {
                        if(active){
                            Icon(
                                modifier = Modifier.clickable {
                                    if (searchQuery.isNotEmpty()){
                                        searchQuery = ""
                                    } else {
                                        active = false
                                    }
                                },
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close Icon")
                        }
                    }
                ) {
                    // 搜索建议内容（可为空）
                }
            }
        }
    ){ innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {

            Spacer(modifier = Modifier.height(16.dp))

            // 好友列表显示
            when {
                isLoading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                }
                friendList == null -> {
                    Text("加载失败", modifier = Modifier.align(Alignment.CenterHorizontally))
                }
                friendList!!.isEmpty() -> {
                    Text("暂无好友", modifier = Modifier.align(Alignment.CenterHorizontally))
                }
                else -> {
                    FriendListView(
                        friends = friendList!!.filter {
                            it.u_name.contains(searchQuery, ignoreCase = true)
                        },
                        onFriendClick = { friend ->
                            // 导航到好友详情页
                            navController.navigate("friendDetail/${friend.u_id}")
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FriendDetailScreen(
    navController: NavController,
    friend : UserInfo,
    //userId: String,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {

    val coroutineScope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("好友详情") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                }
            )
        },
        modifier = modifier
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
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
                    .size(120.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 用户名
            Text(
                text = "${friend?.u_name}",
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 用户ID
            Text(
                text = "ID: ${friend?.u_id}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 个性签名标题
            Text(
                text = "个性签名",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.align(Alignment.Start)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 个性签名内容
            Text(
                text = "${friend?.u_introduction}",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.fillMaxWidth()
            )

            // 这里可以添加更多信息或操作按钮
            Spacer(modifier = Modifier.weight(1f))

            // 删除好友按钮
            Button(
                onClick = { /* 处理删除好友逻辑 */
                    coroutineScope.launch {
                        // 先删除本地数据
                        userInfo.friendList = userInfo.friendList.filterNot { it.u_id == friend.u_id }

                        // 再调用网络请求
                        try {
                            withContext(Dispatchers.IO) {
                                deleteFriend(coroutineScope, friend.u_id)
                            }
                        } catch (e: Exception) {
                            // 网络失败时恢复本地数据
                            userInfo.friendList = userInfo.friendList + friend
                        }

                        // 设置刷新标志并返回
                        navController.previousBackStackEntry?.savedStateHandle?.set("shouldRefresh", true)
                        navController.popBackStack()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("删除好友")
            }
        }
    }
}

@Composable
private fun FriendListView(
    friends: List<UserInfo>, // 修改参数类型
    onFriendClick: (UserInfo) -> Unit // 修改参数类型
) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(friends) { friend ->
            FriendListItem(friend = friend, onClick = { onFriendClick(friend) })
        }
    }
}

@Composable
private fun FriendListItem(friend: UserInfo, onClick: () -> Unit) { //好友列表显示
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = rememberImagePainter(
                data = friend.u_avatar,
                builder = {
                    crossfade(true)
                }
            ),
            contentDescription = "用户头像",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
        )
        Spacer(Modifier.width(16.dp))
        Text(text = friend.u_name, style = MaterialTheme.typography.bodyLarge)
    }
}

suspend fun searchFriend(coroutineScope: CoroutineScope){ //好友搜索逻辑
    try {
        val response = GetRequest(
            interfaceName = "/friend/search",
            queryParams = mapOf()
        ).execute(coroutineScope)
        val type = object : TypeToken<BaseResponseJsonData<List<UserInfo>>>() {}.type
        val data = Gson().fromJson<BaseResponseJsonData<List<UserInfo>>>(response.body?.string(), type)

        if (data.data != null) {
            userInfo.friendList = data.data.also {  //这里借用了friendList，实际意义是userList
                Log.d(TAG, "用户列表更新：${it.size}条记录")
            }

            Log.d(TAG, "用户列表详情：\n${userInfo.friendList?.joinToString("\n") {
                "用户ID：${it.u_id} 姓名：${it.u_name} 头像：${it.u_avatar}"
            } ?: "空列表"}")
        } else {
            Log.e(TAG, "获取用户列表失败：${data.msg}")
        }
    } catch (e: Exception) {
        Log.e(TAG, "获取用户列表异常：${e.message}")
        throw e
    }
}

suspend fun deleteFriend(coroutineScope: CoroutineScope,userId:String){ //删除好友逻辑
    try {
        val payload = listOf(
            BaseSentJsonData("u_id", userId)
        )
        val response = BaseRequest(payload, "/friend/deletefriend")
            .sendPostRequest(coroutineScope)
    } catch (e: Exception) {
        Log.e(TAG, "删除好友失败：${e.message}")
        throw e
    }
}

suspend fun getFriendList(coroutineScope: CoroutineScope){//好友列表逻辑
    try {
        val response = GetRequest(
            interfaceName = "/friend/getfriends",
            queryParams = mapOf()
        ).execute(coroutineScope)
        val type = object : TypeToken<BaseResponseJsonData<List<UserInfo>>>() {}.type
        val data = Gson().fromJson<BaseResponseJsonData<List<UserInfo>>>(response.body?.string(), type)

        if (data.data != null) {
            userInfo.friendList = data.data.also {
                Log.d(TAG, "好友列表更新：${it.size}条记录")
            }
            // 若需要多属性设置才使用apply：
            /*
            userInfo.apply {
                friendList = data.data
                version++
            }
            */
            Log.d(TAG, "好友列表详情：\n${userInfo.friendList?.joinToString("\n") {
                "好友ID：${it.u_id} 姓名：${it.u_name} 头像：${it.u_avatar}"
            } ?: "空列表"}")
        } else {
            Log.e(TAG, "获取好友列表失败：${data.msg}")
        }
    } catch (e: Exception) {
        Log.e(TAG, "获取好友列表异常：${e.message}")
        throw e
    }
}