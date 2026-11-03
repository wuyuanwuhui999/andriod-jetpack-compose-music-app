package com.player.music.ui.page

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import com.player.music.ui.theme.Color
import com.player.music.ui.theme.Dimens

/**
 * 首页（占位页面）
 * 当前仅展示 Hello World，后续音乐业务页面按需替换
 */
@Composable
fun HomePage(navController: NavHostController) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.PageBackground),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Hello World",
            color = Color.Black,
            fontSize = Dimens.bigFontSize
        )
    }
}
