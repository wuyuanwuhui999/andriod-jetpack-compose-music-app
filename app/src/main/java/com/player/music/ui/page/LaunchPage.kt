package com.player.music.ui.page

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.player.music.R
import com.player.music.navigation.Screens
import com.player.music.ui.theme.Color
import com.player.music.ui.theme.Dimens
import com.player.music.viewmodel.MainViewModel

/**
 * 启动页：展示 logo，并校验本地 token 决定进入首页还是登录页
 */
@Composable
fun LaunchPage(
    navController: NavHostController,
    mainViewModel: MainViewModel = hiltViewModel()
) {
    LaunchedEffect(Unit) {
        // 延迟，确保界面先显示
        kotlinx.coroutines.delay(1500)

        // 1. 本地没有 token，直接去登录页
        if (!mainViewModel.hasTokenLocally()) {
            navController.navigate(Screens.Login.route) {
                popUpTo(Screens.Launch.route) { inclusive = true }
            }
            return@LaunchedEffect
        }

        // 2. 有 token 则调用接口校验有效性
        try {
            val isValid = mainViewModel.validateToken()
            val target = if (isValid) Screens.Home.route else Screens.Login.route
            navController.navigate(target) {
                popUpTo(Screens.Launch.route) { inclusive = true }
            }
        } catch (e: Exception) {
            // 网络异常等，回到登录页
            navController.navigate(Screens.Login.route) {
                popUpTo(Screens.Launch.route) { inclusive = true }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Image(
                painter = painterResource(R.drawable.icon_logo),
                contentDescription = "logo",
                modifier = Modifier.size(Dimens.bigIconSize),
                contentScale = ContentScale.Fit
            )
            Spacer(modifier = Modifier.height(Dimens.bigGap))
            Text(
                text = "欢迎使用",
                fontSize = Dimens.bigFontSize
            )
        }
    }
}
