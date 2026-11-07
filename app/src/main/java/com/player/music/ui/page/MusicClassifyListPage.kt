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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.player.music.R
import com.player.music.navigation.Screens
import com.player.music.ui.components.SongListCard
import com.player.music.ui.theme.Color
import com.player.music.ui.theme.Dimens
import com.player.music.viewmodel.MusicClassifyListViewModel

/**
 * 分类歌曲列表页（首页各分类模块点「更多」进入，推荐歌手模块除外）
 * 对应 Flutter 端 lib/pages/MusicClassifyListPage.dart：
 * 顶部标题为分类名，下面该分类的歌曲列表（封面/歌手-歌名/播放/点赞/更多），
 * 上拉加载更多，点击歌曲播放整页列表并进入播放页
 */
@Composable
fun MusicClassifyListPage(
    navController: NavHostController,
    viewModel: MusicClassifyListViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val songs by viewModel.songs.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isLoadingMore by viewModel.isLoadingMore.collectAsState()
    val reachedEnd by viewModel.reachedEnd.collectAsState()
    val currentMusic by viewModel.player.currentMusic.collectAsState()
    val toastMessage by viewModel.toastMessage.collectAsState()

    val listState = rememberLazyListState()

    LaunchedEffect(toastMessage) {
        toastMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearToast()
        }
    }

    // 滚动到底部自动加载下一页
    val isAtBottom by remember {
        derivedStateOf {
            val info = listState.layoutInfo
            val lastVisible = info.visibleItemsInfo.lastOrNull()?.index ?: -1
            info.totalItemsCount > 0 && lastVisible >= info.totalItemsCount - 2
        }
    }
    LaunchedEffect(isAtBottom, songs.size) {
        if (isAtBottom) viewModel.loadMore()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.PageBackground)
    ) {
        // 顶部导航：标题为分类名
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .statusBarsPadding()
                .padding(horizontal = Dimens.middleGap, vertical = Dimens.middleGap),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(R.drawable.icon_back),
                contentDescription = "返回",
                modifier = Modifier
                    .size(Dimens.middleIconSize)
                    .clickable { navController.popBackStack() }
            )
            Text(
                text = viewModel.classifyName,
                color = Color.Black,
                fontSize = Dimens.bigFontSize,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.size(Dimens.middleIconSize))
        }

        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(Dimens.middleGap),
            verticalArrangement = Arrangement.spacedBy(Dimens.middleGap)
        ) {
            item(key = "songs") {
                SongListCard(
                    songs = songs,
                    isLoading = isLoading,
                    currentMusicId = currentMusic?.id,
                    emptyText = "该分类暂无歌曲",
                    onSongClick = { index ->
                        viewModel.play(index)
                        navController.navigate(Screens.MusicPlayer.route)
                    },
                    onLikeClick = { viewModel.toggleLike(it) }
                )
            }

            item(key = "load_more") {
                if (songs.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = !reachedEnd) { viewModel.loadMore() }
                            .padding(vertical = Dimens.middleGap),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = when {
                                reachedEnd -> "已经到底了"
                                isLoadingMore -> "加载中..."
                                else -> "上拉加载更多"
                            },
                            color = Color.Secondary,
                            fontSize = Dimens.normalFontSize
                        )
                    }
                }
            }

            item(key = "bottom_space") {
                Spacer(modifier = Modifier.navigationBarsPadding())
            }
        }
    }
}
