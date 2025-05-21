package com.example.myplayer

import SHA256Util
import android.annotation.SuppressLint
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.myplayer.jsonToModel.JsonToBaseResponse
import com.example.myplayer.model.BaseResponseJsonData
import com.example.myplayer.model.BaseSentJsonData
import com.example.myplayer.model.UserInfo
import com.example.myplayer.model.BaseInformation
import com.example.myplayer.network.LoginRequest
import com.example.myplayer.network.networkAPI.GetRequest
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.window.Dialog
import com.example.myplayer.framework.chat.chatMessagesMap
import com.example.myplayer.model.WebSocketResponse
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.window.DialogProperties
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.example.myplayer.framework.chat.GlobalMessageNotifier
import com.example.myplayer.framework.chat.saveChatMessage
import com.example.myplayer.framework.playroom.saveAccount
import com.example.myplayer.model.DatabaseProvider
import com.example.myplayer.model.LoginAccount
import com.example.myplayer.model.chat.ChatMessage
import com.example.myplayer.userInfo.isConnected
import kotlinx.coroutines.delay
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException


private val TAG = "LoginScreen"

@SuppressLint("CoroutineCreationDuringComposition")
@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    navController: NavHostController,
    onLogout: () -> Unit
) {
    var account by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isRegister by remember { mutableStateOf(false) }
    var showErrorDialog by remember { mutableStateOf(false) }
    var passwordVisible by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    var isWSConnected by remember { mutableStateOf(false) }//是否登录


    val coroutineScope = rememberCoroutineScope()
    val friendCoroutineScope = rememberCoroutineScope()
    val userInfoCoroutineScope = rememberCoroutineScope()
    var errorMessage by remember { mutableStateOf("") }
    val context = LocalContext.current.applicationContext

    LaunchedEffect(Unit) {
        CoroutineScope(Dispatchers.IO).launch{
            try{
                val dao = DatabaseProvider.getPlayRoomDatabase(context).accountDao()
                val oldAccount = dao.getLastInsertedAccount()
                if(oldAccount != null) {
                    account = oldAccount.account.toString()
                    password = oldAccount.password.toString()
                    Log.d("LoginScreen", oldAccount.toString())
                }
            }
            catch (e : Exception)
            {

            }
        }
    }

    if (showErrorDialog) {
        AlertDialog(
            onDismissRequest = { showErrorDialog = false },
            title = { Text("提示") },
            text = { Text(errorMessage) },
            confirmButton = {
                TextButton(onClick = { showErrorDialog = false }) {
                    Text("确定")
                }
            }
        )
    }

    if (!isRegister) {
        // 背景渐变色，您可以换成自己喜欢的颜色或图片
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFFFFFFF))
                .padding(16.dp)
        ) {
            // 可滚动支持，防止软键盘遮挡问题
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.Center)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val customTextSelectionColors = TextSelectionColors(
                    handleColor = MaterialTheme.colorScheme.primary,
                    backgroundColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                )

                Text(
                    text = "欢迎登录",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(bottom = 24.dp)
                )

                OutlinedTextField(
                    value = account,
                    onValueChange = { account = it },
                    label = { Text("账号") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "账号图标"
                        )
                    },
                    enabled = !loading,
                    colors = TextFieldDefaults.colors(
                        MaterialTheme.colorScheme.primary,
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),

                        Color.White,
                        Color.White,
                        Color.White,

                        Color.White,
                        MaterialTheme.colorScheme.primary,
                        Color.White.copy(alpha = 0.5f),

                        MaterialTheme.colorScheme.primary,
                        Color.White.copy(alpha = 0.7f),
                        customTextSelectionColors,

                        MaterialTheme.colorScheme.primary,
                        Color.White.copy(alpha = 0.7f),
                        Color.White.copy(alpha = 0.38f),
                        )
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("密码") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "密码图标"
                        )
                    },
                    trailingIcon = {
                        val image = if (passwordVisible)
                            Icons.Default.Visibility
                        else
                            Icons.Default.VisibilityOff

                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(imageVector = image, contentDescription = "切换密码可见")
                        }
                    },
                    enabled = !loading,
                    colors = TextFieldDefaults.colors(
                        MaterialTheme.colorScheme.primary,
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),

                        Color.White,
                        Color.White,
                        Color.White,

                        Color.White,
                        MaterialTheme.colorScheme.primary,
                        Color.White.copy(alpha = 0.5f),

                        MaterialTheme.colorScheme.primary,
                        Color.White.copy(alpha = 0.7f),
                        customTextSelectionColors,

                        MaterialTheme.colorScheme.primary,
                        Color.White.copy(alpha = 0.7f),
                        Color.White.copy(alpha = 0.38f),
                    )
                )

                Spacer(modifier = Modifier.height(32.dp))

                Button(
                    onClick = {
                        loading = true
                        coroutineScope.launch {
                            withContext(Dispatchers.IO) {
                                try {
                                    val response = LoginRequest(
                                        listOf(
                                            BaseSentJsonData("u_account", account),
                                            BaseSentJsonData(
                                                "u_password",
                                                SHA256Util.sha256Encrypt(password)
                                            )
                                        ), "/login"
                                    ).sendRequest(coroutineScope)
                                    val data = JsonToBaseResponse<String>(response).getResponseData()


                                    if(data.code == 200) {
                                        withContext(Dispatchers.Main) {
                                            loading = false
                                            BaseInformation.account = account
                                            BaseInformation.password = SHA256Util.sha256Encrypt(password)
                                            BaseInformation.token = data.data.toString()
                                            Log.i("loginScreen", data.toString())
                                            Log.i("loginScreen-token", BaseInformation.token)

                                            saveAccount(
                                                context,
                                                LoginAccount(
                                                    null,
                                                    account,
                                                    password
                                                )
                                            )
                                            getUserInfo(friendCoroutineScope,context)
                                            getFriendList(userInfoCoroutineScope,context)
                                            connectToWS(onLogout, context, {
                                                CoroutineScope(Dispatchers.Main).launch{
                                                    Toast.makeText(context, "已登录！", Toast.LENGTH_SHORT).show()
                                                }

                                                isWSConnected = true
                                                isConnected = true
                                                onLoginSuccess()
                                            })
                                        }
                                    }

                                } catch (e: Exception) {
                                    withContext(Dispatchers.Main) {
                                        loading = false
                                        errorMessage = when (e) {
                                            is IOException,
                                            is SocketTimeoutException,
                                            is UnknownHostException -> "网络异常，请检查网络连接"
                                            else -> "登录失败，请检查账号或密码"
                                        }
                                        showErrorDialog = true
                                    }
                                    Log.d("loginScreen", e.toString())
                                }
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    enabled = !loading,
                    shape = MaterialTheme.shapes.medium
                ) {
                    Text(
                        text = if (loading) "登录中..." else "登录",
                        style = MaterialTheme.typography.titleMedium
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                TextButton(
                    onClick = { isRegister = true },
                    enabled = !loading,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = "没有账号？立即注册",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            if (loading) {
                Dialog(
                    onDismissRequest = { /* 禁止关闭 */ },
                    properties = DialogProperties(
                        dismissOnBackPress = false,
                        dismissOnClickOutside = false,
                        usePlatformDefaultWidth = false  // 允许自定义宽度，避免默认的宽度限制
                    )
                ) {
                    // 这里用Box全屏填充
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.5f)),  // 半透明遮罩
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surface,
                            tonalElevation = 16.dp,
                            shadowElevation = 8.dp,
                            modifier = Modifier.size(150.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(24.dp),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                // 加载Lottie动画资源
                                val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.loading_animation))
                                val progress by animateLottieCompositionAsState(
                                    composition,
                                    iterations = LottieConstants.IterateForever
                                )

                                LottieAnimation(
                                    composition = composition,
                                    progress = progress,
                                    modifier = Modifier.size(80.dp)
                                )

                                Spacer(modifier = Modifier.height(20.dp))

                                Text(
                                    text = "登录中...",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

        }
    } else {
        navController.navigate("register")
    }
}


suspend fun getUserInfo(coroutineScope: CoroutineScope,context: Context) {
    withContext(Dispatchers.IO) {
        try {
            val request = GetRequest(
                interfaceName = "/user/getuserinfo",
                queryParams = mapOf()
            )
            val response = request.execute(coroutineScope)
            val gson = Gson()
            val type = object : TypeToken<BaseResponseJsonData<UserInfo>>() {}.type
            val data = gson.fromJson<BaseResponseJsonData<UserInfo>>(response.body?.string(), type)

            if (data != null && data.data != null) {
                Log.d("LoginScreen", "获取用户信息成功：${data.data}")
                Log.d("LoginScreen", "u_name: ${data.data.u_name}")

                userInfo.u_name = data.data.u_name
                userInfo.u_introduction = data.data.u_introduction
                userInfo.u_avatar = data.data.u_avatar
                userInfo.u_id = data.data.u_id

            } else {
                Log.e("LoginScreen", "获取用户信息失败：${data.msg}")
                CoroutineScope(Dispatchers.Main).launch{
                    Log.e("LoginScreen", "获取用户信息失败：${data.msg}")
                }
            }
        } catch (e: Exception) {
            Log.e("LoginScreen", "获取用户信息异常：${e.message}")
            Toast.makeText(context, "获取用户信息异常！${e.message}", Toast.LENGTH_SHORT).show()
            throw e
        }
    }
}

suspend fun getFriendList(coroutineScope: CoroutineScope,context: Context){
    withContext(Dispatchers.IO) {
        try {
            val response = GetRequest(
                interfaceName = "/friend/getfriends",
                queryParams = mapOf()
            ).execute(coroutineScope)
            val type = object : TypeToken<BaseResponseJsonData<List<UserInfo>>>() {}.type
            val data =
                Gson().fromJson<BaseResponseJsonData<List<UserInfo>>>(response.body?.string(), type)

            if (data != null && data.data != null) {
                userInfo.friendList = data.data.also {
                    Log.d(com.example.myplayer.framework.chat.TAG, "好友列表更新：${it.size}条记录")
                }
                Log.d(
                    com.example.myplayer.framework.chat.TAG, "好友列表详情：\n${
                        userInfo.friendList?.joinToString("\n") {
                            "好友ID：${it.u_id} 姓名：${it.u_name} 头像：${it.u_avatar}"
                        } ?: "空列表"
                    }")
            } else {
                CoroutineScope(Dispatchers.Main).launch{
                    Log.e(com.example.myplayer.framework.chat.TAG, "获取好友列表失败：${data.msg}")
                }
                Log.e(com.example.myplayer.framework.chat.TAG, "获取好友列表失败：${data.msg}")
            }
        } catch (e: Exception) {
            Log.e(com.example.myplayer.framework.chat.TAG, "获取好友列表异常：${e.message}")
            CoroutineScope(Dispatchers.Main).launch{
                Toast.makeText(context, "刷新好友列表失败！${e.message}", Toast.LENGTH_SHORT).show()
            }
            throw e
        }
    }
}
var webSocketManager: WebSocketManager? = null;
suspend fun connectToWS(
    onLogout: () -> Unit,
    context: Context,
    onWSConnected: () -> Unit  // 新增回调，通知连接成功
){
    webSocketManager = WebSocketManager("wss://www.myplayer.merlin.xin/online?u_id=${userInfo.u_id}&u_name=${userInfo.u_name}")
    val listener = object : WebSocketListener() {
        override fun onMessage(webSocket: WebSocket, text: String) {
            val type = object : TypeToken<WebSocketResponse>() {}.type
            val data = Gson().fromJson<WebSocketResponse>(text, type)
            if(data.engaged){ //如果被占线
                webSocketManager?.disconnect(onLogout)
                Log.i(TAG, "WebSocket断开连接")
            }
            else{
                if(data.system){

                }
                else if(data.message){
                    if (!data.content.isNullOrEmpty()) {
                        CoroutineScope(Dispatchers.IO).launch {
                            saveChatMessage(
                                context,
                                ChatMessage(
                                    chat_id = 0,
                                    accpet_id = userInfo.u_id,
                                    sender_id = data.sender,
                                    content = data.content ?: "",
                                    isMyMessage = data.sender == userInfo.u_id,
                                    isSent = true,
                                    time = data.time,
                                )
                            )
                        }
                        if(userInfo.currentFriend != data.sender)
                        {
                            CoroutineScope(Dispatchers.Default).launch{
                                //Toast.makeText(context, "收到来自${data.sender_name}的消息: ${data.content}", Toast.LENGTH_SHORT).show()
                                val notifyMsg = "收到来自${data.sender_name}的消息: ${data.content}"
                                GlobalMessageNotifier.notify(notifyMsg)
                            }
                        }
                        Log.i(TAG, "收到来自${data.sender_name}的消息: ${data.content}")
                    }
                }
            }
            Log.d(TAG, data.toString())
        }
        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            super.onClosed(webSocket, code, reason)
            Log.i(TAG, "连接正常关闭 code:$code reason:$reason")
//            CoroutineScope(Dispatchers.Main).launch{
//                Toast.makeText(context, "已离线！", Toast.LENGTH_SHORT).show()
//            }
            isConnected = false
            restartWebSocketWithDelay()
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            super.onFailure(webSocket, t, response)
//            CoroutineScope(Dispatchers.Main).launch{
//                Toast.makeText(context, "离线异常！", Toast.LENGTH_SHORT).show()
//            }
            Log.e(TAG, "连接异常断开", t)
            isConnected = false
            restartWebSocketWithDelay()
        }


        override fun onOpen(webSocket: WebSocket, response: Response) {
            super.onOpen(webSocket, response)
            onWSConnected()
            isConnected = true
            Log.i(TAG, "登录的webSocket连接成功！")
        }

        private fun restartWebSocketWithDelay() {
            try {
                // 3秒后重连，避免频繁重连导致资源浪费或被封禁
                Handler(Looper.getMainLooper()).postDelayed({
                    Log.d("LoginScreen", "开始重连登录WebSocket")
//                    CoroutineScope(Dispatchers.Main).launch{
//                        Toast.makeText(context, "正在重新登录", Toast.LENGTH_SHORT)
//                            .show()
//                    }
                    CoroutineScope(Dispatchers.IO).launch {
                        connectToWS(onLogout,
                            context, {
//                                CoroutineScope(Dispatchers.Main).launch {
//                                Toast.makeText(context, "已重新登录！", Toast.LENGTH_SHORT).show()
//                              }
                                isConnected = true
                            }
                        )
                    }
                }, 3000)
            } catch (e: Exception) {
                Log.e("LoginScreen", "重连登录WebSocket失败:${e.message}")
                CoroutineScope(Dispatchers.Main).launch{
                    Toast.makeText(context, "重新登录异常！", Toast.LENGTH_SHORT)
                        .show()
                }
                isConnected = false
            }
        }
    }
    webSocketManager?.connect(listener)
}


