package com.example.myplayer

import androidx.compose.foundation.layout.*
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

@Composable
fun RegisterScreen(
    modifier: Modifier = Modifier
        .fillMaxSize()
        .wrapContentSize(Alignment.Center)
        ,onNavigateToRegister: () -> Unit
) {
    var account by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var verificationCode by remember { mutableStateOf("") }
    var responseText by remember { mutableStateOf("") }
    
    val coroutineScope = rememberCoroutineScope()
    
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        TextField(
            value = account,
            onValueChange = { account = it },
            label = { Text("账号") }
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
                    
                    responseText = response.body?.string() ?: ""
                }
            }
        }) {
            Text("注册")
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(responseText)
    }
}