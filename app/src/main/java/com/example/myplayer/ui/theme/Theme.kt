package com.example.myplayer.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFFC94240),
    secondary = Color(0xFFA73E52),
    tertiary = Color(0xFFEBA9B7),
    background = Color(0xFFFFF1F1),
    surface = Color(0xFFFFF1F1),
    error = Color(0xB00020), // 推荐 Material 默认错误红色，您也可以自定义
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color(0xFF5F2A33),
    onBackground = Color(0xFF2E1518),
    onSurface = Color(0xFF2E1518),
    onError = Color.White, // 错误色上文字颜色，一般白色
)

@Composable
fun MyPlayerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}