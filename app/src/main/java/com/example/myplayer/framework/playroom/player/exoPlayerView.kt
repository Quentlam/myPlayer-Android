//package com.example.myplayer.framework.playroom.player
//
//import android.app.Activity
//import android.content.Context
//import android.content.pm.ActivityInfo
//import android.content.res.Configuration
//import android.widget.Toast
//import androidx.annotation.OptIn
//import androidx.compose.animation.AnimatedVisibility
//import androidx.compose.animation.fadeIn
//import androidx.compose.animation.fadeOut
//import androidx.compose.foundation.background
//import androidx.compose.foundation.border
//import androidx.compose.foundation.clickable
//import androidx.compose.foundation.interaction.MutableInteractionSource
//import androidx.compose.foundation.interaction.collectIsPressedAsState
//import androidx.compose.foundation.layout.*
//import androidx.compose.material.icons.Icons
//import androidx.compose.material.icons.automirrored.filled.ArrowBack
//import androidx.compose.material.icons.filled.*
//import androidx.compose.material3.*
//import androidx.compose.runtime.*
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.graphics.Color
//import androidx.compose.ui.platform.LocalConfiguration
//import androidx.compose.ui.platform.LocalContext
//import androidx.compose.ui.text.font.FontWeight
//import androidx.compose.ui.text.style.TextOverflow
//import androidx.compose.ui.unit.dp
//import androidx.compose.ui.viewinterop.AndroidView
//import androidx.lifecycle.LifecycleOwner
//import androidx.lifecycle.compose.LocalLifecycleOwner
//import androidx.media3.common.MediaItem
//import androidx.media3.common.util.Log
//import androidx.media3.common.util.UnstableApi
//import androidx.media3.datasource.DefaultDataSource
//import androidx.media3.exoplayer.ExoPlayer
//import androidx.media3.exoplayer.dash.DashMediaSource
//import androidx.media3.exoplayer.hls.HlsMediaSource
//import androidx.media3.exoplayer.source.ProgressiveMediaSource
//import androidx.media3.ui.PlayerView
//import kotlinx.coroutines.delay
//import kotlinx.coroutines.launch
//
//private fun isSupportedFormat(url: String): Boolean {
//    val supportedFormats = listOf(
//        ".mp4", ".m4a", ".m4v",
//        ".mp3", ".webm", ".mkv",
//        ".flv", ".wav", ".ogg",
//        ".ts", ".m3u8", ".mpd"
//    )
//    return supportedFormats.any { url.lowercase().endsWith(it) }
//}
//
//@OptIn(UnstableApi::class)
//@Composable
//fun PlayerWithFloatingControls(
//    context: Context,
//    videoUrl: String,
//    roomId: String,
//    onBack: () -> Unit,
//    startPositionMs: Long = 0L,
//    lifecycleOwner: LifecycleOwner = LocalLifecycleOwner.current,
//    reloadTrigger: Int = 0
//) {
//    var controlsVisible by remember { mutableStateOf(false) }
//    val coroutineScope = rememberCoroutineScope()
//    var isPlaying by remember { mutableStateOf(false) }
//
//    val configuration = LocalConfiguration.current
//    val orientation = configuration.orientation
//
//    val exoPlayer = remember(context) {
//        ExoPlayer.Builder(context).build().apply {
//            playWhenReady = false
//        }
//    }
//
//    DisposableEffect(lifecycleOwner) {
//        onDispose { exoPlayer.release() }
//    }
//
//    fun startAutoHideTimer() {
//        coroutineScope.launch {
//            delay(4000)
//            controlsVisible = false
//        }
//    }
//
//    LaunchedEffect(videoUrl, reloadTrigger) {
//        try {
//            exoPlayer.stop()
//            exoPlayer.clearMediaItems()
//
//            if (!isSupportedFormat(videoUrl)) {
//                Toast.makeText(context, "不支持的视频格式", Toast.LENGTH_SHORT).show()
//                return@LaunchedEffect
//            }
//
//            val dataSourceFactory = DefaultDataSource.Factory(context)
//            val mediaSource = when {
//                videoUrl.endsWith(".m3u8", true) -> HlsMediaSource.Factory(dataSourceFactory)
//                    .createMediaSource(MediaItem.fromUri(videoUrl))
//                videoUrl.endsWith(".mpd", true) -> DashMediaSource.Factory(dataSourceFactory)
//                    .createMediaSource(MediaItem.fromUri(videoUrl))
//                else -> ProgressiveMediaSource.Factory(dataSourceFactory)
//                    .createMediaSource(MediaItem.fromUri(videoUrl))
//            }
//
//            exoPlayer.setMediaSource(mediaSource)
//            exoPlayer.prepare()
//
//            if (startPositionMs > 0) exoPlayer.seekTo(startPositionMs)
//
//            exoPlayer.playWhenReady = true
//            isPlaying = true
//
//        } catch (e: Exception) {
//            Toast.makeText(context, "视频加载失败：${e.message}", Toast.LENGTH_SHORT).show()
//        }
//    }
//
//    Box(
//        modifier = Modifier
//            .fillMaxSize()
//            .background(Color.Black)
//            .clickable(
//                indication = null,
//                interactionSource = remember { MutableInteractionSource() }
//            ) {
//                controlsVisible = !controlsVisible
//                Log.d("exoPlayer","controlsVisible = $controlsVisible") // 打印状态
//                if (controlsVisible) startAutoHideTimer()
//            }
//    ) {
//        AndroidView(
//            factory = {
//                PlayerView(it).apply {
//                    player = exoPlayer
//                    useController = false
//                }
//            },
//            modifier = Modifier.fillMaxSize()
//        )
//
//        AnimatedVisibility(
//            visible = controlsVisible,
//            enter = fadeIn(),
//            exit = fadeOut(),
//            modifier = Modifier
//                .fillMaxSize()
//                .background(Color(0x55000000))
//        ) {
//            val activity = (context as? Activity)
//            Box(
//                modifier = Modifier.fillMaxSize()
//            )
//            {
//                    // 顶部区域
//                    if (orientation == Configuration.ORIENTATION_PORTRAIT) {
//                        Box(
//                            modifier = Modifier
//                                .fillMaxWidth()
//                                .height(56.dp)
//                                .align(Alignment.TopStart)
//                                .background(Color.Blue.copy(alpha = 0.3f))
//                        ) {
//                            Row(
//                                modifier = Modifier
//                                    .fillMaxWidth()
//                                    .height(56.dp)
//                                    .padding(horizontal = 12.dp, vertical = 8.dp),
//                                verticalAlignment = Alignment.CenterVertically
//                            ) {
//                                IconButton(onClick = onBack, modifier = Modifier.size(36.dp)) {
//                                    Icon(
//                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
//                                        contentDescription = "返回",
//                                        tint = Color.White
//                                    )
//                                }
//                                Spacer(modifier = Modifier.width(8.dp))
//                                Text(
//                                    text = "房间ID：$roomId",
//                                    color = Color.White,
//                                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
//                                    maxLines = 1,
//                                    overflow = TextOverflow.Ellipsis
//                                )
//                            }
//                        }
//                    } else {
//                        // 横屏顶部可以不显示返回栏，或你也可以加其它控件
//                        Spacer(modifier = Modifier.height(56.dp))
//                    }
//
//                    // 中间
//                    Box(
//                        modifier = Modifier
//                            .fillMaxWidth()
//                            .height(80.dp)
//                            .align(Alignment.Center)
//                            .background(Color.Magenta.copy(alpha = 0.3f))
//                    ) {
//                        Row(
//                            modifier = Modifier
//                                .fillMaxWidth()
//                                .height(80.dp),  // 这里固定高度，不用weight撑开
//                            horizontalArrangement = Arrangement.spacedBy(
//                                40.dp,
//                                Alignment.CenterHorizontally
//                            ),
//                            verticalAlignment = Alignment.CenterVertically,
//                        ) {
//                            // 快退按钮
//                            IconButton(
//                                onClick = {
//                                    val newPos =
//                                        (exoPlayer.currentPosition - 15_000).coerceAtLeast(0L)
//                                    exoPlayer.seekTo(newPos)
//                                },
//                                modifier = Modifier.size(60.dp)
//                            ) {
//                                Text("<< 15s", color = Color.White)
//                            }
//
//                            // 播放/暂停按钮
//                            IconButton(
//                                onClick = {
//                                    if (exoPlayer.isPlaying) {
//                                        exoPlayer.pause()
//                                        isPlaying = false
//                                    } else {
//                                        exoPlayer.play()
//                                        isPlaying = true
//                                    }
//                                },
//                                modifier = Modifier.size(70.dp)
//                            ) {
//                                Icon(
//                                    imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
//                                    contentDescription = if (isPlaying) "暂停" else "播放",
//                                    tint = Color.White,
//                                    modifier = Modifier.size(50.dp)
//                                )
//                            }
//
//                            // 快进按钮
//                            IconButton(
//                                onClick = {
//                                    val duration =
//                                        exoPlayer.duration.takeIf { it > 0 } ?: Long.MAX_VALUE
//                                    val newPos =
//                                        (exoPlayer.currentPosition + 15_000).coerceAtMost(duration)
//                                    exoPlayer.seekTo(newPos)
//                                },
//                                modifier = Modifier.size(60.dp)
//                            ) {
//                                Text("15s >>", color = Color.White)
//                            }
//                        }
//                    }
//
//                    // 底部区域保持不变
//                    Row(
//                        modifier = Modifier
//                            .fillMaxWidth()
//                            .height(48.dp)
//                            .background(Color.Red.copy(alpha = 0.5f))
//                            .border(2.dp, Color.Yellow)
//                            .align(Alignment.BottomStart)
//                            .padding(horizontal = 12.dp),
//                        verticalAlignment = Alignment.CenterVertically
//                    ) {
//                        PlayerProgressBar(
//                            exoPlayer = exoPlayer,
//                            modifier = Modifier.weight(1f)
//                        )
//                        //Spacer(modifier = Modifier.width(8.dp))
//
//                        IconButton(onClick = {
//                            if (orientation == Configuration.ORIENTATION_PORTRAIT) {
//                                (context as? Activity)?.requestedOrientation =
//                                    ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
//                            } else {
//                                (context as? Activity)?.requestedOrientation =
//                                    ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
//                            }
//                        }) {
//                            Icon(
//                                imageVector = if (orientation == Configuration.ORIENTATION_PORTRAIT) Icons.Filled.Fullscreen else Icons.Filled.FullscreenExit,
//                                contentDescription = "全屏切换",
//                                tint = Color.White,
//                                modifier = Modifier.size(28.dp)
//                            )
//                        }
//                    }
//
//            }
//        }
//    }
//}
//
//@kotlin.OptIn(ExperimentalMaterial3Api::class)
//@Composable
//fun PlayerProgressBar(
//    exoPlayer: ExoPlayer,
//    modifier: Modifier = Modifier
//) {
//    var sliderPosition by remember { mutableStateOf(0f) }
//    var isUserDragging by remember { mutableStateOf(false) }
//
//    val interactionSource = remember { MutableInteractionSource() }
//    val isPressed by interactionSource.collectIsPressedAsState()
//
//    LaunchedEffect(exoPlayer.isPlaying) {
//        while (true) {
//            if (!isUserDragging) {
//                val duration = exoPlayer.duration.takeIf { it > 0 } ?: 0L
//                val position = exoPlayer.currentPosition.coerceAtMost(duration)
//                sliderPosition = if (duration > 0) (position.toFloat() / duration) else 0f
//            }
//            delay(500)
//        }
//    }
//
//    val duration = exoPlayer.duration.takeIf { it > 0 } ?: 0L
//    val currentPosition = (sliderPosition * duration).toLong()
//
//    Column(modifier = modifier.padding(horizontal = 16.dp)) {
//        Slider(
//            value = sliderPosition,
//            onValueChange = {
//                sliderPosition = it
//                isUserDragging = true
//            },
//            onValueChangeFinished = {
//                isUserDragging = false
//                exoPlayer.seekTo((sliderPosition * duration).toLong())
//            },
//            modifier = Modifier
//                .fillMaxWidth()
//                .height(24.dp),
//            colors = SliderDefaults.colors(
//                thumbColor = Color.Transparent,
//                activeTrackColor = Color.White,
//                inactiveTrackColor = Color.Gray
//            ),
//            thumb = { sliderState ->
//                Icon(
//                    imageVector = Icons.Default.AirportShuttle,
//                    contentDescription = "滑块",
//                    tint = if (isPressed) Color.Yellow else Color.White,
//                    modifier = Modifier.size(20.dp)
//                )
//            }
//        )
//        Row(
//            modifier = Modifier.fillMaxWidth(),
//            horizontalArrangement = Arrangement.SpaceBetween
//        ) {
//            Text(text = formatTime(currentPosition), color = Color.White, fontSize = MaterialTheme.typography.bodySmall.fontSize)
//            Text(text = formatTime(duration), color = Color.White, fontSize = MaterialTheme.typography.bodySmall.fontSize)
//        }
//    }
//}
//
//fun formatTime(millis: Long): String {
//    val totalSeconds = millis / 1000
//    val minutes = totalSeconds / 60
//    val seconds = totalSeconds % 60
//    return String.format("%02d:%02d", minutes, seconds)
//}
