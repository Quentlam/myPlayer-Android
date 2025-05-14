package com.example.myplayer

import SHA256Util
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.myplayer.jsonToModel.JsonToBaseResponse
import com.example.myplayer.model.BaseResponseJsonData
import com.example.myplayer.model.BaseSentJsonData
import com.example.myplayer.model.UserInfo
import com.example.myplayer.model.playroom.Playroom
import com.example.myplayer.network.BaseInformation
import com.example.myplayer.network.LoginRequest
import com.example.myplayer.network.networkAPI.GetRequest
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import com.example.myplayer.framework.chat.ChatMessage
import com.example.myplayer.framework.chat.chatMessagesMap
import com.example.myplayer.model.WebSocketResponse
import okhttp3.WebSocket
import okhttp3.WebSocketListener

private val TAG = "LoginScreen"

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    navController: NavHostController
    ) {
    var account by remember { mutableStateOf("1959804282@qq.com") }
    var password by remember { mutableStateOf("123456") }
    var isRegister by remember { mutableStateOf(false) }
    var showErrorDialog by remember { mutableStateOf(false) }
    var passwordVisible by remember { mutableStateOf(false) }

    if (showErrorDialog) {
        AlertDialog(
            onDismissRequest = { showErrorDialog = false },
            title = { Text("提示") },
            text = { Text("登录失败") },
            confirmButton = {
                TextButton(onClick = { showErrorDialog = false }) {
                    Text("确定")
                }
            }
        )
    }

    val coroutineScope = rememberCoroutineScope()

    if(!isRegister) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            TextField(
                value = account,
                onValueChange = { account = it },
                label = { Text("账号") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            TextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("密码") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    coroutineScope.launch {
                        withContext(Dispatchers.IO) {
                            try {
                                val response =
                                    LoginRequest(
                                        listOf(
                                            BaseSentJsonData("u_account", account),
                                            BaseSentJsonData(
                                                "u_password",
                                                SHA256Util.sha256Encrypt(password)
                                            )
                                        ),
                                        "/login"
                                    ).sendRequest(coroutineScope)
                                val data = JsonToBaseResponse<String>(response).getResponseData()
                                // 在主线程中更新 UI 状态
                                // 修改后登录逻辑：
                                    onLoginSuccess()
                                    BaseInformation.account = account
                                    BaseInformation.password = SHA256Util.sha256Encrypt(password)
                                    BaseInformation.token = data.data.toString()
                                    Log.i("loginScreen", data.toString())
                                    Log.i("loginScreen-token", BaseInformation.token)

                                    getUserInfo(coroutineScope)
                                    getFriendList(coroutineScope)
                                    connectToWS()
                            }
                            catch (e: Exception)
                            {
                                showErrorDialog = true
                                Log.d("loginScreen", e.toString())
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("登录")
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = { isRegister = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Transparent),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent,
                    contentColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text(
                    text = "注册",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
    else
    {
        navController.navigate("register")
    }

}

suspend fun getUserInfo(coroutineScope: CoroutineScope){
    try {
        val request = GetRequest(
            interfaceName = "/user/getuserinfo",
            queryParams = mapOf()
        )
        val response = request.execute(coroutineScope)
        val gson = Gson()
        val type = object : TypeToken<BaseResponseJsonData<UserInfo>>() {}.type
        val data = gson.fromJson<BaseResponseJsonData<UserInfo>>(response.body?.string(), type)

        if (data.data != null) {
            Log.d("LoginScreen", "获取用户信息成功：${data.data}")
            Log.d("LoginScreen", "u_name: ${data.data.u_name}")

            userInfo.u_name = data.data.u_name
            userInfo.u_introduction = data.data.u_introduction
            userInfo.u_avatar = data.data.u_avatar
            userInfo.u_id = data.data.u_id

        } else {
            Log.e("LoginScreen", "获取用户信息失败：${data.msg}")
        }
    } catch (e: Exception) {
        Log.e("LoginScreen", "获取用户信息异常：${e.message}")
        throw e
    }
}
suspend fun getFriendList(coroutineScope: CoroutineScope){
    try {
        val response = GetRequest(
            interfaceName = "/friend/getfriends",
            queryParams = mapOf()
        ).execute(coroutineScope)
        val type = object : TypeToken<BaseResponseJsonData<List<UserInfo>>>() {}.type
        val data = Gson().fromJson<BaseResponseJsonData<List<UserInfo>>>(response.body?.string(), type)

        if (data.data != null) {
            userInfo.friendList = data.data.also {
                Log.d(com.example.myplayer.framework.chat.TAG, "好友列表更新：${it.size}条记录")
            }
            Log.d(com.example.myplayer.framework.chat.TAG, "好友列表详情：\n${userInfo.friendList?.joinToString("\n") {
                "好友ID：${it.u_id} 姓名：${it.u_name} 头像：${it.u_avatar}"
            } ?: "空列表"}")
        } else {
            Log.e(com.example.myplayer.framework.chat.TAG, "获取好友列表失败：${data.msg}")
        }
    } catch (e: Exception) {
        Log.e(com.example.myplayer.framework.chat.TAG, "获取好友列表异常：${e.message}")
        throw e
    }
}
var webSocketManager: WebSocketManager? = null;
    suspend fun connectToWS(){
    webSocketManager = WebSocketManager("wss://www.myplayer.merlin.xin/online?u_id=${userInfo.u_id}&u_name=${userInfo.u_name}")
    val listener = object : WebSocketListener() {
        override fun onMessage(webSocket: WebSocket, text: String) {
            val type = object : TypeToken<WebSocketResponse>() {}.type
            val data = Gson().fromJson<WebSocketResponse>(text, type)
            // 确保每个发送者有对应的消息列表
            val messages = chatMessagesMap.getOrPut(data.sender) { mutableStateListOf() }
            // 添加新消息（需在UI线程更新）
            messages.add(ChatMessage(
                content = data.content, 
                isMyMessage = data.sender == userInfo.u_id // 根据发送者判断是否是自己的消息
            ))
            Log.i(TAG, "收到来自${data.sender_name}的消息: ${data.content}")
            Log.d(TAG, data.toString())
        }
    }
    webSocketManager?.connect(listener)
}
