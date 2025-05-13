package com.example.myplayer.framework.playroom

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import com.example.myplayer.model.playroom.Playroom
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.myplayer.network.BaseInformation.currentMemberList
import com.example.myplayer.network.BaseInformation.currentRoom
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


@Composable
fun chosePlayroomScreen() {
    val localNavController = rememberNavController()
    NavHost(
        navController = localNavController,
        startDestination = "playroomList"
    ) {
        composable("playroomList") {
            Column {
                setPlayroomList(localNavController)
            }
        }
        composable("room/{roomId}") {
            backStackEntry ->
            roomScreen(
                localNavController
            )
        }
        composable("manageRoom/{roomId}") {
                backStackEntry ->
            manageRoomScreen(
                onBack = { localNavController.popBackStack() } ,
                onAvatarUpdate =
                {
                   //更新头像之后的逻辑
                }
            )
        }
    }
}

@Composable
fun customTopAppBar(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onAddClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .background(Color(0xFFF19E9E))//粉色背景
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .height(40.dp)
                .background(Color.White, RoundedCornerShape(4.dp))
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp)
            ) {
                Icon(
                    Icons.Default.Search,
                    contentDescription = null,
                    tint = Color.Gray
                )
                Spacer(modifier = Modifier.width(8.dp))
                BasicTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    textStyle = TextStyle(color = Color.Black),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            if (searchQuery.isEmpty()) {
                Text(
                    "搜索播放室",
                    color = Color.Gray,
                    modifier = Modifier.padding(start = 40.dp, top = 10.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(16.dp))
        Button(
            onClick = {

            }
        ) {
            Text("搜索")
        }

    }
}