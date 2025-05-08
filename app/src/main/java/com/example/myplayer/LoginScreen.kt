package com.example.myplayer

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.myplayer.jsonToModel.JsonToBaseResponse
import com.example.myplayer.model.BaseSentJsonData
import com.example.myplayer.network.BaseInformation
import com.example.myplayer.network.LoginRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    navController: NavHostController
    ) {
    var account by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isRegister by remember { mutableStateOf(false) }
    var showErrorDialog by remember { mutableStateOf(false) }


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

    Button(onClick = {
        isRegister = true//当前状态为注册状态
    })
    {
        Text("注册")
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

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    coroutineScope.launch {
                        withContext(Dispatchers.IO) {
                            val response =
                                LoginRequest(
                                    listOf(
                                        BaseSentJsonData("u_account", "1959804282@qq.com"),
                                        BaseSentJsonData("u_password", "8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92")
                                    ),
                                    "/login"
                                ).sendRequest(coroutineScope)
                            val data = JsonToBaseResponse<String>(response).getResponseData()
                            // 在主线程中更新 UI 状态
                            // 修改后登录逻辑：
                            if (data.data != null) {
                                onLoginSuccess()
                                account = "1959804282@qq.com"
                                password = "8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92"
                                BaseInformation.account = account
                                BaseInformation.password = password
                                BaseInformation.token = data.data
                                Log.e("loginScreen",data.toString())
                                Log.e("loginScreen-token",BaseInformation.token)
                            } else {
                                showErrorDialog = true
                                data.msg?.let { Log.e("loginScreen", it) }
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("登录")
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
    else
    {
        navController.navigate("register")
    }

}
