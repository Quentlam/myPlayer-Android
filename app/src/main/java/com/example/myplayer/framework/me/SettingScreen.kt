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
import androidx.compose.runtime.produceState
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
import com.example.myplayer.model.playroom.Playroom
import com.example.myplayer.framework.Dao.PlayroomDao
import com.example.myplayer.jsonToModel.JsonToBaseResponse
import com.example.myplayer.model.BaseResponseJsonData
import com.example.myplayer.model.BaseSentJsonData
import com.example.myplayer.network.BaseInformation
import com.example.myplayer.network.BaseRequest
import com.example.myplayer.network.DatabaseProvider
import com.example.myplayer.network.LoginRequest
import com.example.myplayer.userInfo
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File

@SuppressLint("ContextCastToActivity")
@Composable
fun SettingScreen() {
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
    val context = LocalContext.current as Activity
    val scope = rememberCoroutineScope()



    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            avatarUri = it
            val filePath = getRealPathFromUri(context, it)
            filePath?.let { path ->
                CoroutineScope(Dispatchers.IO).launch {
                    updateAvatarUri(this, path)
                }
                userInfo.u_avatar = path
            }
        }
    }


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
        // 头像部分
        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary)
                .align(Alignment.CenterHorizontally)
                .clickable {
                    imagePickerLauncher.launch("image/*")
                }
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
            onClick = { showChangeAccountDialog = true }
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
    }

    // 修改密码对话框
    if (showChangePasswordDialog) {
        ChangePasswordDialog(
            onDismiss = { showChangePasswordDialog = false },
            onConfirm = { oldPassword, newPassword ->
                // 这里假设原密码是"123456"，实际应用中应该从数据库或服务器验证
                if (oldPassword == "123456") {
                    // 密码正确，修改成功
                    showToastMessage("密码修改成功")
                    showChangePasswordDialog = false
                } else {
                    // 密码错误
                    showToastMessage("原密码错误，修改失败")
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

    // 修改账号对话框
    if (showChangeAccountDialog) {
        ChangeTextDialog(
            title = "修改账号",
            initialValue = account,
            onDismiss = { showChangeAccountDialog = false },
            onConfirm = { newValue ->
                account = newValue
                showChangeAccountDialog = false
                showToastMessage("账号修改成功")
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
        Divider(
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

suspend fun updateAvatarUri(coroutineScope: CoroutineScope, filePath: String) {
    try {
        val file = File(filePath)
        val requestBody = file.asRequestBody("image/*".toMediaTypeOrNull())
        val multipartBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("pic", file.name, requestBody)
            .build()

        val request = Request.Builder()
            .url(BaseInformation.HOST + "/uploadavatar")
            .post(multipartBody)
            .build()

        val client = BaseRequest.getOkHttpClient(coroutineScope)
        client.newCall(request).execute()
    } catch (e: Exception) {
        // 忽略异常
    }
    userInfo.u_avatar = filePath
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