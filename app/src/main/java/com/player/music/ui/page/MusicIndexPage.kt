package com.player.music.ui.page

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.player.music.R
import com.player.music.navigation.Screens
import com.player.music.ui.components.Avatar
import com.player.music.ui.components.AvatarSize
import com.player.music.ui.components.MusicCover
import com.player.music.ui.theme.Color
import com.player.music.ui.theme.Dimens
import com.player.music.viewmodel.MainViewModel
import com.player.music.viewmodel.MusicIndexViewModel

/**
 * 音乐主框架页面（底部四个 Tab + 中间悬浮迷你播放器）
 * 对应 Flutter 端 lib/pages/MusicIndexPage.dart：
 * 首页 / 推荐 / 音乐圈 / 我的
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MusicIndexPage(
    navController: NavHostController,
    mainViewModel: MainViewModel = hiltViewModel(),
    viewModel: MusicIndexViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val currentUser by mainViewModel.currentUser.collectAsState()
    val recommendMusic by viewModel.recommendMusic.collectAsState()
    val toastMessage by viewModel.toastMessage.collectAsState()

    var currentIndex by rememberSaveable { mutableIntStateOf(0) }
    // 列表滚动状态提升到这里，切换 Tab 后返回仍保留位置
    val homeListState = rememberLazyListState()
    val recommendListState = rememberLazyListState()
    val circleListState = rememberLazyListState()
    val mineListState = rememberLazyListState()

    // 统一 Toast 提示
    LaunchedEffect(toastMessage) {
        toastMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearToast()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color.PageBackground,
        bottomBar = {
            MusicBottomBar(
                currentIndex = currentIndex,
                onTabSelected = { currentIndex = it }
            )
        },
        floatingActionButton = {
            MiniPlayerButton(
                coverUrl = recommendMusic?.cover,
                onClick = {
                    Toast.makeText(context, "播放器功能开发中，敬请期待", Toast.LENGTH_SHORT).show()
                }
            )
        },
        floatingActionButtonPosition = FabPosition.Center
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (currentIndex) {
                0 -> MusicHomePage(
                    navController = navController,
                    viewModel = viewModel,
                    currentUserAvatar = currentUser?.avatar,
                    listState = homeListState
                )
                1 -> MusicRecommendPage(listState = recommendListState)
                2 -> MusicCirclePage(
                    listState = circleListState,
                    currentUserId = currentUser?.id,
                    currentUsername = currentUser?.username,
                    currentUserAvatar = currentUser?.avatar
                )
                else -> MusicUserPage(
                    navController = navController,
                    currentUser = currentUser,
                    listState = mineListState
                )
            }
        }
    }
}

/**
 * 底部导航栏：首页 / 推荐 / [迷你播放器占位] / 音乐圈 / 我的
 */
@Composable
private fun MusicBottomBar(
    currentIndex: Int,
    onTabSelected: (Int) -> Unit
) {
    val tabs = remember {
        listOf(
            TabItem("首页", R.drawable.icon_home, R.drawable.icon_home_active),
            TabItem("推荐", R.drawable.icon_recomment, R.drawable.icon_recomment_active),
            TabItem("音乐圈", R.drawable.icon_music_circle, R.drawable.icon_music_circle_active),
            TabItem("我的", R.drawable.icon_tab_user, R.drawable.icon_tab_user_active)
        )
    }

    BottomAppBar(
        modifier = Modifier.heightIn(min = Dimens.bottomBarHeight),
        containerColor = Color.White,
        contentPadding = PaddingValues(0.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BottomBarItem(
                tab = tabs[0],
                selected = currentIndex == 0,
                modifier = Modifier.weight(1f),
                onClick = { onTabSelected(0) }
            )
            BottomBarItem(
                tab = tabs[1],
                selected = currentIndex == 1,
                modifier = Modifier.weight(1f),
                onClick = { onTabSelected(1) }
            )
            // 中间给悬浮播放器留出位置
            Spacer(modifier = Modifier.width(Dimens.miniPlayerSize))
            BottomBarItem(
                tab = tabs[2],
                selected = currentIndex == 2,
                modifier = Modifier.weight(1f),
                onClick = { onTabSelected(2) }
            )
            BottomBarItem(
                tab = tabs[3],
                selected = currentIndex == 3,
                modifier = Modifier.weight(1f),
                onClick = { onTabSelected(3) }
            )
        }
    }
}

/**
 * 底部导航单个条目
 */
@Composable
private fun BottomBarItem(
    tab: TabItem,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .clickable { onClick() }
            .padding(vertical = Dimens.smallGap),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(if (selected) tab.activeIconRes else tab.iconRes),
            contentDescription = tab.title,
            modifier = Modifier.size(Dimens.middleIconSize)
        )
        Spacer(modifier = Modifier.height(Dimens.smallGap))
        Text(
            text = tab.title,
            color = if (selected) Color.Primary else Color.Secondary,
            fontSize = Dimens.normalFontSize
        )
    }
}

/**
 * 中间悬浮的迷你播放器（当前仅展示推荐歌曲封面，播放器开发后接入）
 */
@Composable
private fun MiniPlayerButton(coverUrl: String?, onClick: () -> Unit) {
    FloatingActionButton(
        onClick = onClick,
        modifier = Modifier.size(Dimens.miniPlayerSize),
        shape = CircleShape,
        containerColor = Color.White,
        contentColor = Color.Primary,
        elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp)
    ) {
        MusicCover(
            coverUrl = coverUrl,
            size = Dimens.miniPlayerSize - 12.dp
        )
    }
}

/**
 * 底部 Tab 数据
 */
private data class TabItem(
    val title: String,
    val iconRes: Int,
    val activeIconRes: Int
)
