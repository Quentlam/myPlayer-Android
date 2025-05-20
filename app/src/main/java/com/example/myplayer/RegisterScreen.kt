package com.example.myplayer

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.myplayer.model.BaseSentJsonData
import com.example.myplayer.network.LoginRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Response

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun RegisterScreen(
    modifier: Modifier = Modifier
        .fillMaxSize()
        .wrapContentSize(Alignment.Center)
        ,onNavigateToLogin: () -> Unit
) {
    var account by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var verificationCode by remember { mutableStateOf("") }
    var responseText by remember { mutableStateOf("") }
    
    val coroutineScope = rememberCoroutineScope()
    Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    navigationIcon = {
                        IconButton(onClick = { onNavigateToLogin() }) {
                            Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "返回")
                        }
                    },
                    title = {
                        Text(text = "myplayer账号注册")
                    }
                )
            }
    ){
        Column(
            modifier = modifier,
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            TextField(
                value = account,
                onValueChange = { account = it },
                label = { Text("账号") },
                placeholder = { Text("请输入邮箱") }
            )

            Spacer(modifier = Modifier.height(8.dp))

            TextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("密码") }
            )

            Spacer(modifier = Modifier.height(8.dp))

            TextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it },
                label = { Text("确认密码") }
            )

            Spacer(modifier = Modifier.height(8.dp))

            TextField(
                value = verificationCode,
                onValueChange = { verificationCode = it },
                label = { Text("验证码") }
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(onClick = {
                coroutineScope.launch {
                    withContext(Dispatchers.IO) {
                        val response = LoginRequest(
                            listOf(
                                BaseSentJsonData("u_account", account),
                                BaseSentJsonData("u_password", password),
                                BaseSentJsonData("u_confirm_password", confirmPassword),
                                BaseSentJsonData("u_verification_code", verificationCode)
                            ),
                            "/register"
                        ).sendRequest(coroutineScope)

                        responseText = response!!.body?.string() ?: ""
                    }
                }
            }) {
                Text("注册")
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(responseText)
        }
    }

}