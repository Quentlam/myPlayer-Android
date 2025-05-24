package com.example.myplayer.framework.me

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.example.myplayer.R
import com.example.myplayer.model.BaseSentJsonData
import com.example.myplayer.model.BaseInformation
import com.example.myplayer.network.BaseRequest
import com.example.myplayer.network.interceptor.TokenRefreshInterceptor
import com.example.myplayer.userInfo
import com.example.myplayer.webSocketManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONObject
import java.io.File
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException

@SuppressLint("ContextCastToActivity")
@Composable
fun SettingScreen(onLogout: () -> Unit) {
    var username by remember { mutableStateOf(userInfo.u_name) }
    var account by remember { mutableStateOf(userInfo.u_id) }
    var signature by remember { mutableStateOf(userInfo.u_introduction) }

    var showChangePasswordDialog by remember { mutableStateOf(false) }
    var showChangeUsernameDialog by remember { mutableStateOf(false) }
    var showChangeAccountDialog by remember { mutableStateOf(false) }
    var showChangeSignatureDialog by remember { mutableStateOf(false) }

    var avatarUri by remember { mutableStateOf<Uri?>(if (userInfo.u_avatar.isNotEmpty()) Uri.parse(userInfo.u_avatar) else null) }
    var showToast by remember { mutableStateOf(false) }
    var toastMessage by remember { mutableStateOf("") }
    val context = LocalContext.current.applicationContext


    // 显示Toast的函数
    fun showToastMessage(message: String) {
        toastMessage = message
        showToast = true
    }

    // Toast组件
    if (showToast) {
        Toast.makeText(context, toastMessage, Toast.LENGTH_SHORT).show()
        showToast = false
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        AvatarWithEditDialog(
            avatarUri = avatarUri,
            onAvatarUriChange = { newUri -> avatarUri = newUri },
            account = account
        )

        Spacer(modifier = Modifier.height(24.dp))

        // 用户名
        SettingItem(
            title = "用户名",
            value = username,
            onClick = { showChangeUsernameDialog = true }
        )

        // 账号
        SettingItem(
            title = "账号",
            value = account,
            onClick = {
                showToastMessage("账号不可修改")
            }
        )



        // 个性签名
        SettingItem(
            title = "个性签名",
            value = signature,
            onClick = { showChangeSignatureDialog = true }
        )

        // 修改密码
        SettingItem(
            title = "修改密码",
            value = "点击修改",
            onClick = { showChangePasswordDialog = true }
        )
        
        // 新增退出登录按钮
        Spacer(modifier = Modifier.weight(1f))
        Button(
            onClick = { 
                webSocketManager?.disconnect(onLogout)
            },
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error
            )
        ) {
            Text("退出登录")
        }
    }

// 2. Compose 层调用：按照 ChangeTextDialog 的例子去掉 title 参数
    if (showChangePasswordDialog) {
        ChangePasswordDialog(
            onDismiss = { showChangePasswordDialog = false },
            onConfirm = { oldPwd, newPwd ->
                // 使用 IO 线程调用接口
                CoroutineScope(Dispatchers.IO).launch {
                    val result = updatePassword(this, oldPwd, newPwd)
                    withContext(Dispatchers.Main) {
                        if (result != null && result.first == 0) {
                            showToastMessage("密码修改成功")
                            showChangePasswordDialog = false
                        } else {
                            showToastMessage("修改失败：${result?.second ?: "网络错误"}")
                        }
                    }
                }
            }
        )
    }

    // 修改用户名对话框
    if (showChangeUsernameDialog) {
        ChangeTextDialog(
            title = "修改用户名",
            initialValue = username,
            onDismiss = { showChangeUsernameDialog = false },
            onConfirm = { newValue ->
                // 调用 updateUserName 函数更新用户名
                CoroutineScope(Dispatchers.IO).launch {
                    updateUserName(this, newValue)
                }
                // 更新本地用户名
                username = newValue
                userInfo.u_name=newValue
                showChangeUsernameDialog = false

                // 显示成功提示
                showToastMessage("用户名修改成功")
            }
        )
    }


    // 修改个性签名对话框
    if (showChangeSignatureDialog) {
        ChangeTextDialog(
            title = "修改个性签名",
            initialValue = signature,
            onDismiss = { showChangeSignatureDialog = false },
            onConfirm = { newValue ->
                CoroutineScope(Dispatchers.IO).launch {
                    updatesignature(this, newValue)
                }
                signature = newValue
                userInfo.u_introduction=newValue
                showChangeSignatureDialog = false
                showToastMessage("个性签名修改成功")
            }
        )
    }
}

@Composable
fun SettingItem(
    title: String,
    value: String,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp)
    ) {
        Text(
            text = title,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium
        )
        HorizontalDivider(
            modifier = Modifier.padding(top = 12.dp),
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
        )
    }
}

@Composable
fun ChangePasswordDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var oldPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("修改密码") },
        text = {
            Column {
                OutlinedTextField(
                    value = oldPassword,
                    onValueChange = { oldPassword = it },
                    label = { Text("原密码") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = newPassword,
                    onValueChange = { newPassword = it },
                    label = { Text("新密码") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(oldPassword, newPassword)
                }
            ) {
                Text("确认")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

@Composable
fun ChangeTextDialog(
    title: String,
    initialValue: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var value by remember { mutableStateOf(initialValue) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = { value = it },
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(value)
                }
            ) {
                Text("确认")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

suspend fun updateUserName(coroutineScope: CoroutineScope, newValuename: String) {
    try {
        val payload = listOf(
            BaseSentJsonData("u_name", newValuename)
        )
        val response = BaseRequest(payload, "/user/updatename")
            .sendPostRequest(coroutineScope)
    } catch (e: Exception) {

    }
    userInfo.u_name = newValuename
}

suspend fun updatesignature(coroutineScope: CoroutineScope, newValuesignature: String) {
    try {
        val payload = listOf(
            BaseSentJsonData("u_introduction", newValuesignature)
        )
        val response = BaseRequest(payload, "/user/updateintroduction")
            .sendPostRequest(coroutineScope)
    } catch (e: Exception) {

    }
    userInfo.u_introduction = newValuesignature
}

suspend fun updateAvatarUri(coroutineScope: CoroutineScope,context: Context,filePath: String, account: String) {
    try {
        val file = File(filePath)
        val requestBody = file.asRequestBody("image/*".toMediaTypeOrNull())
        val multipartBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("file", file.name, requestBody)
            .addFormDataPart("id", account) // 新增 id 参数
            .build()

        val request = Request.Builder()
            .url(BaseInformation.HOST + "/avatar/upload")
            .post(multipartBody)
            .build()

        // 添加一行代码打印请求的 URL
        Log.d("UpdateAvatarRequest", "Request URL: ${request.url}")

        val client = OkHttpClient.Builder()
            .addInterceptor(TokenRefreshInterceptor(coroutineScope)) // 只加 token 拦截器
            .build()

        val response = client.newCall(request).execute()

        if (response.isSuccessful) {
            val responseBody = response.body?.string()
            Log.d("UpdateAvatarResponse", "Response: $responseBody")

            val jsonResponse = JSONObject(responseBody ?: "{}")
            val code = jsonResponse.getInt("code")
            val msg = jsonResponse.getString("msg")
            Log.d("UpdateAvatarResponse", "Code: $code, Message: $msg")
            CoroutineScope(Dispatchers.Main).launch {
                Toast.makeText(context, "修改头像成功！", Toast.LENGTH_SHORT).show()
            }
        }
        else {
            Log.e("UpdateAvatarError", "Request failed with code: ${response.code}, body: ${response.body?.string()}")
            Log.e("UpdateAvatarError", "Request failed with code: ${response.code}")
            CoroutineScope(Dispatchers.Main).launch{
                Toast.makeText(context, "修改头像失败！${response.code}", Toast.LENGTH_SHORT).show()
            }
        }
    } catch (e: Exception) {
        Log.e("UpdateAvatarError", "Exception: ${e.localizedMessage}")
        CoroutineScope(Dispatchers.Main).launch {
            Toast.makeText(context, "修改头像失败！${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}


fun getRealPathFromUri(context: Context, uri: Uri): String? {
    val cursor = context.contentResolver.query(uri, null, null, null, null)
    return cursor?.use {
        val index = it.getColumnIndex(MediaStore.Images.Media.DATA)
        it.moveToFirst()
        it.getString(index)
    } ?: run {
        if (uri.scheme == "file") uri.path else null
    }
}

// 1. 修改密码的网络请求函数
// 1. 修改密码的网络请求函数
suspend fun updatePassword(
    coroutineScope: CoroutineScope,
    oldPassword: String,
    newPassword: String
): Pair<Int, String>? {
    return try {
        val encryptedOld = sha256(oldPassword)
        val encryptedNew = sha256(newPassword)

        val formBody = FormBody.Builder()
            .add("oldPassword", encryptedOld)
            .add("password", encryptedNew)
            .build()

        val request = Request.Builder()
            .url(BaseInformation.HOST + "/user/updatepassword")
            .post(formBody)
            .build()

        Log.d("UpdatePasswordRequest", "Request URL: ${request.url}")

        val client = OkHttpClient.Builder()
            .addInterceptor(TokenRefreshInterceptor(coroutineScope))
            .build()

        val response = client.newCall(request).execute()

        if (response.isSuccessful) {
            val body = response.body?.string().orEmpty()
            Log.d("UpdatePasswordResponse", "Response: $body")
            val json = JSONObject(body)
            val code = json.getInt("code")
            val msg = json.getString("msg")
            code to msg
        } else {
            Log.e("UpdatePasswordError", "HTTP ${response.code}: ${response.body?.string()}")
            null
        }
    } catch (e: Exception) {
        Log.e("UpdatePasswordException", "Exception: ${e.localizedMessage}")
        null
    }
}

fun sha256(input: String): String {
    return try {
        val messageDigest = MessageDigest.getInstance("SHA-256")
        val digest = messageDigest.digest(input.toByteArray())
        val hexString = StringBuilder()
        for (b in digest) {
            val hex = Integer.toHexString(0xff and b.toInt())
            if (hex.length == 1) {
                hexString.append('0')
            }
            hexString.append(hex)
        }
        hexString.toString()
    } catch (e: NoSuchAlgorithmException) {
        throw RuntimeException("SHA-256 algorithm not found", e)
    }
}


@Composable
fun AvatarWithEditDialog(
    avatarUri: Uri?,
    onAvatarUriChange: (Uri?) -> Unit,
    account: String
) {
    val context = LocalContext.current.applicationContext
    val scope = rememberCoroutineScope()

    var showAvatarDialog by remember { mutableStateOf(false) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            onAvatarUriChange(it)
            val filePath = getRealPathFromUri(context, it)
            filePath?.let { path ->
                scope.launch(Dispatchers.IO) {
                    updateAvatarUri(this, context,path, account)
                }
            }
        }
        showAvatarDialog = false
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary)
                .clickable { showAvatarDialog = true }
                .align(Alignment.CenterHorizontally),
            contentAlignment = Alignment.Center
        ) {
            if (avatarUri != null) {
                Image(
                    painter = rememberAsyncImagePainter(avatarUri),
                    contentDescription = "头像",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Image(
                    painter = painterResource(id = R.drawable.ic_launcher_foreground),
                    contentDescription = "头像",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
        }
    }


    if (showAvatarDialog) {
        AlertDialog(
            onDismissRequest = { showAvatarDialog = false },
            title = null,
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(200.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                    ) {
                        if (avatarUri != null) {
                            Image(
                                painter = rememberAsyncImagePainter(avatarUri),
                                contentDescription = "头像大图",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Image(
                                painter = painterResource(id = R.drawable.ic_launcher_foreground),
                                contentDescription = "头像大图",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(onClick = {
                        imagePickerLauncher.launch("image/*")
                    }) {
                        Text(text = "修改头像")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAvatarDialog = false }) {
                    Text("关闭")
                }
            }
        )
    }
}