package com.example.myplayer

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
    var responseText by remember { mutableStateOf("") }
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
                    onLoginSuccess()
//                    coroutineScope.launch {
//                        withContext(Dispatchers.IO) {
//                            val response =
//                                LoginRequest(
//                                    listOf(
//                                        BaseSentJsonData("u_account", account),
//                                        BaseSentJsonData("u_password", password)
//                                    ),
//                                    "/login"
//                                ).sendRequest(coroutineScope)
//
//                            val data = JsonToBaseResponse<String>(response).getResponseData()
//                            // 在主线程中更新 UI 状态
//                            // 修改后登录逻辑：
//                            if (data.code == 200) {
//                                onLoginSuccess()
//                            } else {
//                                onLoginSuccess()
//                                //showErrorDialog = true
//                                //responseText = "登录失败：${data.code}"
//                            }
//                        }
//                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("登录")
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(responseText)
        }
    }
    else
    {
        navController.navigate("register")
    }

}
