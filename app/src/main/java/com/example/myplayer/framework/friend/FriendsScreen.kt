package com.example.myplayer.framework.friend

import android.annotation.SuppressLint
import android.util.Log
import android.widget.Toast
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.runtime.derivedStateOf
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import coil.compose.rememberImagePainter
import com.example.myplayer.model.BaseSentJsonData
import com.example.myplayer.network.BaseRequest
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.io.IOException

val TAG = "FriendsScreen"

data class Inviting(
    val inviter : String,         //申请人id
    val inviter_name : String,    //申请人姓名
    val inviter_avatar : String,  //申请人头像
    val room : String,            //是否为房间申请
    val time : String             //申请时间
)

// 修改 UserData 类
object UserData {
    var userList: List<UserInfo> by mutableStateOf(emptyList())    //搜索用户列表
    var invitingList:List<Inviting> by mutableStateOf(emptyList()) //当前用户接收申请列表
}

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
                onBackClick = { navController.popBackStack() }
            )
        }
    }
}

@SuppressLint("CoroutineCreationDuringComposition")
@Composable
fun FriendsListScreen(navController: NavController) {

    var isLoading by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    // 搜索框相关变量
    var searchQuery by remember { mutableStateOf("") }
    var lastSearchJob by remember { mutableStateOf<Job?>(null) }
    var expanded by remember { mutableStateOf(false) } // 控制搜索栏展开状态

    var showInvitingDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    // 在 FriendsListScreen 中更新搜索逻辑
    fun handleSearch(query: String) {
        lastSearchJob?.cancel()
        lastSearchJob = coroutineScope.launch {
            isLoading = true
            // 执行搜索
            withContext(Dispatchers.IO) {
                searchFriend(coroutineScope, query)
            }
            // 搜索完成后操作
            withContext(Dispatchers.Main) { // 切回主线程更新UI
                // 如果当前无输入内容，清空结果
                if (searchQuery.isEmpty()) {
                    UserData.userList = emptyList()
                }
            }
            isLoading = false
        }
    }

    // 添加好友
    fun handleAddFriend(newfriend: UserInfo) {
        coroutineScope.launch {
            // 调用网络请求
            try {
                withContext(Dispatchers.IO) {
                    addFriend(coroutineScope,newfriend)
                }
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "已发送好友申请", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e(TAG, "添加好友失败：${e.message}")
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "发送好友申请失败，请重试", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

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

    // 关键修复：使用可观察的State初始化
    val friendList by remember {
        mutableStateOf(userInfo.friendList).apply {
            // 初始加载逻辑
            if (value.isEmpty()) {
                coroutineScope.launch {
                    isLoading = true
                    withContext(Dispatchers.IO) {
                        getFriendList(coroutineScope)
                    }
                    isLoading = false
                }
            }
        }
    }

    Scaffold (
        topBar = {
            FriendSearchBar(
                searchQuery = searchQuery,
                onSearchQueryChange = { searchQuery = it },
                onSearch = ::handleSearch,
                onAddFriend = ::handleAddFriend,
                expanded = expanded, // 新增参数
                onExpandedChange = { expanded = it }, // 新增参数
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showInvitingDialog = true }) {
                Icon(Icons.Default.Notifications, contentDescription = "Notice")
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
                isLoading -> CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
                friendList.isEmpty() -> Text(
                    "暂无好友",
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
                else -> FriendListView(
                    friends = friendList,
                    onFriendClick = { navController.navigate("friendDetail/${it.u_id}") }
                )
            }

            // 添加Dialog调用
            InvitingDialog(
                showDialog = showInvitingDialog,
                onDismiss = { showInvitingDialog = false }
            )
        }
    }
}

//好友搜索 是否留历史记录？当前为保留上次查询记录的版本
@OptIn(ExperimentalMaterial3Api::class)  //标记为实验性的
@Composable
private fun FriendSearchBar(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onSearch: (String) -> Unit,
    onAddFriend: (UserInfo) -> Unit, // 新增添加好友回调
    modifier: Modifier,
    expanded: Boolean,          // 新增
    onExpandedChange: (Boolean) -> Unit // 新增
){
    // 使用响应式状态监听
    val searchResults by remember {
        derivedStateOf { UserData.userList }
    }
    Box(
        modifier
            .fillMaxWidth()
            .semantics { isTraversalGroup = true }
    ){
        SearchBar(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth(),
            inputField = {
                SearchBarDefaults.InputField(
                    query = searchQuery,
                    onQueryChange = { newText ->
                        onSearchQueryChange(newText)
                    },
                    onSearch = {
                        onSearch(it)
                    },
                    expanded = expanded,
                    onExpandedChange = onExpandedChange,
                    placeholder = { Text("好友搜索") },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Search, contentDescription = "Search Icon")
                    },
                    trailingIcon = {
                        if(searchQuery.isNotEmpty() || expanded){
                            Icon(
                                modifier = Modifier.clickable {
                                    if (searchQuery.isNotEmpty()){
                                        onSearchQueryChange("") // 清空搜索内容
                                    } else {
                                        onExpandedChange(false)
                                    }
                                },
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close Icon")
                        }
                    }
                )
            },

            expanded = expanded,
            onExpandedChange = onExpandedChange
        ) {

            // 搜索结果列表（仅在展开时显示）
            if (expanded) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp)  // 避免被搜索框遮挡
                        .heightIn(max = 400.dp)
                ) {
                    items(searchResults) { user ->
                        UserSearchItem(
                            user = user,
                            onAddClick = { onAddFriend(user) },
                            onItemClick = {
                                onSearchQueryChange(user.u_name) // 填充搜索框
                                onSearch(user.u_name)            // 触发搜索
                                onExpandedChange(false)          // 关闭下拉
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    // 添加空状态提示
                    if (searchResults.isEmpty()) {
                        item {
                            Text(
                                "未找到相关用户",
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun UserSearchItem(
    user: UserInfo,
    onAddClick: () -> Unit,
    onItemClick: () -> Unit, // 新增条目点击回调
    modifier: Modifier = Modifier
) {   //好友搜索条目显示
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .clickable (onClick = onItemClick), // 绑定外部点击事件
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = rememberImagePainter(
                data = user.u_avatar,
                builder = {
                    crossfade(true)
                },
            ),
            contentDescription = "用户头像",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(user.u_name, fontWeight = FontWeight.Bold)
            Text("@${user.u_id}", color = Color.Gray)
        }

        IconButton(onClick = onAddClick) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "添加好友",
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

// inviting对话框
@Composable
private fun InvitingDialog(
    showDialog: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    // 使用响应式状态监听
    val invitations by remember {
        derivedStateOf { UserData.invitingList }
    }

    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()

    val context = LocalContext.current

    // 加载好友申请数据
    LaunchedEffect(showDialog) {
        if (showDialog) {
            try {
                withContext(Dispatchers.IO) {
                    getInvitingList(coroutineScope)
                }
                isLoading = false
            } catch (e: Exception) {
                errorMessage = "加载申请失败：${e.message}"
                isLoading = false
            }
        }
    }

    if (showDialog) {
        AlertDialog(
            modifier = modifier,
            onDismissRequest = onDismiss,
            title = {
                Text(text = "好友申请", style = MaterialTheme.typography.headlineSmall)
            },
            text = {
                when {
                    isLoading -> {
                        Box(modifier = Modifier.fillMaxWidth()) {
                            CircularProgressIndicator(
                                modifier = Modifier.align(Alignment.Center)
                            )
                        }
                    }
                    errorMessage != null -> {
                        Text(errorMessage!!, color = MaterialTheme.colorScheme.error)
                    }
                    invitations.isEmpty() -> {
                        Text("暂无新的好友申请")
                    }
                    else -> {
                        LazyColumn(
                            modifier = Modifier.heightIn(max = 400.dp)
                        ) {
                            items(invitations) { invitation ->
                                InvitationItem(
                                    invitation = invitation,
                                    onAccept = {
                                        // 先删除本地数据
                                        UserData.invitingList = UserData.invitingList.filterNot { it == invitation }
                                        // 再调用网络请求
                                        coroutineScope.launch {
                                            try {
                                                withContext(Dispatchers.IO) {
                                                    acceptInviting(coroutineScope, invitation)
                                                }
                                                // 网络请求成功后显示 Toast
                                                withContext(Dispatchers.Main) {
                                                    Toast.makeText(
                                                        context,
                                                        "已接受 ${invitation.inviter_name} 的好友申请",
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                }
                                            } catch (e: Exception) {
                                                // 失败时恢复本地数据
                                                UserData.invitingList = UserData.invitingList + invitation
                                                withContext(Dispatchers.Main) {
                                                    Toast.makeText(
                                                        context,
                                                        "接受好友申请失败，请重试",
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                }
                                            }
                                        }
                                    },
                                    onDecline = {
                                        // 先删除本地数据
                                        UserData.invitingList = UserData.invitingList.filterNot { it == invitation }
                                        // 再调用网络请求
                                        coroutineScope.launch {
                                            try {
                                                withContext(Dispatchers.IO) {
                                                    refuseInviting(coroutineScope, invitation)
                                                }
                                                // 网络请求成功后显示 Toast
                                                withContext(Dispatchers.Main) {
                                                    Toast.makeText(
                                                        context,
                                                        "已拒绝 ${invitation.inviter_name} 的好友申请",
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                }
                                            } catch (e: Exception) {
                                                // 失败时恢复本地数据
                                                UserData.invitingList = UserData.invitingList + invitation
                                                // 显示错误 Toast
                                                withContext(Dispatchers.Main) {
                                                    Toast.makeText(
                                                        context,
                                                        "拒绝好友申请失败，请重试",
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                }
                                            }
                                        }
                                    }
                                )
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
}

// 单个申请项组件
@Composable
private fun InvitationItem(
    invitation: Inviting,
    onAccept: () -> Unit,
    onDecline: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 用户头像
            Image(
                painter = rememberImagePainter(
                    data = invitation.inviter_avatar,
                    builder = {
                        crossfade(true)
                    }
                ),
                contentDescription = "用户头像",
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = invitation.inviter_name,
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = "UID：${invitation.inviter}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }

            // 操作按钮
            IconButton(onClick = onAccept) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "接受",
                    tint = Color.Green
                )
            }
            IconButton(onClick = onDecline) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "拒绝",
                    tint = Color.Red
                )
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
    val context = LocalContext.current

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
                            withContext(Dispatchers.Main) {
                                Toast.makeText(
                                    context,
                                    "删除好友失败，请重试",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
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

suspend fun searchFriend(coroutineScope: CoroutineScope,userName:String){ //好友搜索
    try {
        val response = GetRequest(
            interfaceName = "/friend/searchuser",
            queryParams = mapOf("u_name" to userName)
        ).execute(coroutineScope)

        // 强制读取并缓存响应体内容（避免多次调用 string()）
        val rawResponseBody = response.body?.string() ?: ""
        Log.d(TAG, "原始响应内容:\n$rawResponseBody") // 打印原始响应

        // 重新解析响应体（需重新构建输入流）
        val type = object : TypeToken<BaseResponseJsonData<List<UserInfo>>>() {}.type
        val data = Gson().fromJson<BaseResponseJsonData<List<UserInfo>>>(rawResponseBody, type)

        // 直接更新全局数据
        UserData.userList = data.data ?: emptyList()

        if (data.data != null) {
            UserData.userList = data.data.also {  //userList
                Log.d(TAG, "用户列表更新：${it.size}条记录")
            }

            Log.d(TAG, "用户列表详情：\n${UserData.userList?.joinToString("\n") {
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
//inviting
suspend fun addFriend(coroutineScope: CoroutineScope,target:UserInfo){ //添加好友，发送申请
    try {
        val payload = listOf(
            BaseSentJsonData("sender", userInfo.u_id),//发送者
            BaseSentJsonData("target", target.u_id) //接收方
        )
        val response = BaseRequest(payload, "/inviting/sendinviting")
            .sendPostRequest(coroutineScope)
        // 添加成功日志（根据实际响应结构调整）
        Log.d(TAG, "好友申请发送成功 -> 发送人：${userInfo.u_id} 接收人：${target.u_id}")
    } catch (e: Exception) {
        Log.e(TAG, "好友申请发送失败：${e.message}")
        throw e
    }
}

suspend fun getInvitingList(coroutineScope: CoroutineScope){ //获取当前用户的申请表
    try {
        val response = GetRequest(
            interfaceName = "/inviting/getinvitings",
            queryParams = mapOf()
        ).execute(coroutineScope)
        val type = object : TypeToken<BaseResponseJsonData<List<Inviting>>>() {}.type
        val responseBody = response.body?.string()
        Log.d(TAG, "原始响应内容:\n$responseBody") // 打印原始响应
        val data = Gson().fromJson<BaseResponseJsonData<List<Inviting>>>(responseBody, type)

        data.data?.let { list ->
            // 过滤掉房间申请（保留 room 为空的好友申请）
            val filteredList = list.filter {
                it.room.isNullOrEmpty() // 根据实际字段类型调整判断逻辑
            }

            UserData.invitingList = filteredList

            Log.d(TAG, "好友申请列表更新：${filteredList.size}条记录")
            Log.d(TAG, "申请详情：\n${
                filteredList.joinToString("\n") {
                    "好友申请 | 发送者：${it.inviter_name} (ID:${it.inviter}) " +
                            "头像：${it.inviter_avatar} 是否为房间邀请:${it.room} 时间：${it.time}"
                }
            }")
        } ?: run {
            UserData.invitingList = emptyList()
            Log.e(TAG, "空列表：${data.msg}")
        }
    } catch (e: Exception) {
        Log.e(TAG, "获取申请列表异常：${e.stackTraceToString()}")
        UserData.invitingList = emptyList()
        throw e
    }
}

suspend fun acceptInviting(coroutineScope: CoroutineScope,inviting: Inviting){ //同意好友申请
    try {
        val payload = listOf(
            BaseSentJsonData("inviter", inviting.inviter),//申请人
            BaseSentJsonData("target", userInfo.u_id),
            BaseSentJsonData("room", inviting.room)
        )
        val response = BaseRequest(payload, "/inviting/passinviting")
            .sendPostRequest(coroutineScope)
        // 验证响应状态码
        if (!response.isSuccessful) {
            throw IOException("服务器返回错误：${response.code} ${response.message}")
        }

        // 解析响应内容
        val responseBody = response.body?.string()
        Log.d(TAG, "服务器响应：$responseBody")

        // 验证业务逻辑成功（根据实际接口结构调整）
        if (responseBody?.contains("\"code\":200") != true) {
            throw IOException("操作失败：$responseBody")
        }

        Log.d(TAG, "✅ 好友关系已建立 | 对方ID：${inviting.inviter}")
    } catch (e: Exception) {
        Log.e(TAG, "通过好友申请失败：${e.message}")
        throw e
    }
}

suspend fun refuseInviting(coroutineScope: CoroutineScope,inviting: Inviting){ //拒绝好友申请
    try {
        val payload = listOf(
            BaseSentJsonData("inviter", inviting.inviter),//申请人
            BaseSentJsonData("target", userInfo.u_id),
            BaseSentJsonData("room", inviting.room)
        )
        val response = BaseRequest(payload, "/inviting/refuseinviting")
            .sendPostRequest(coroutineScope)
        // 添加成功日志（根据实际响应结构调整）
        Log.d(TAG, "已拒绝好友申请")
    } catch (e: Exception) {
        Log.e(TAG, "拒绝好友申请失败：${e.message}")
        throw e
    }
}

suspend fun deleteFriend(coroutineScope: CoroutineScope,userId:String){ //删除好友
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

suspend fun getFriendList(coroutineScope: CoroutineScope){ //好友列表
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