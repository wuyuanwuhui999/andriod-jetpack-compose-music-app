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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.player.music.R
import com.player.music.navigation.Screens
import com.player.music.ui.components.MusicCover
import com.player.music.ui.components.SongListCard
import com.player.music.ui.theme.Color
import com.player.music.ui.theme.Dimens
import com.player.music.viewmodel.MusicFavoriteListViewModel

/**
 * 我的收藏夹歌曲列表页（我的页点某个收藏夹进入）
 * 对应 Flutter 端 lib/pages/MusicFavoriteListPage.dart：
 * 顶部收藏夹信息卡（封面 + 名称 + 歌曲数）、歌曲列表（封面/歌手-歌名/播放/点赞/更多）、
 * 上拉加载更多、点击歌曲播放整页列表并进入播放页
 */
@Composable
fun MusicFavoriteListPage(
    navController: NavHostController,
    viewModel: MusicFavoriteListViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val songs by viewModel.songs.collectAsState()
    val directoryName by viewModel.directoryName.collectAsState()
    val directoryTotal by viewModel.directoryTotal.collectAsState()
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
        // 顶部导航
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
                text = "我的收藏夹",
                color = Color.Black,
                fontSize = Dimens.bigFontSize,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
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
            // 收藏夹信息卡
            item(key = "directory") {
                DirectoryHeaderCard(
                    name = directoryName,
                    cover = viewModel.cover,
                    total = directoryTotal,
                    onEditClick = {
                        Toast.makeText(context, "重命名收藏夹功能开发中，敬请期待", Toast.LENGTH_SHORT).show()
                    }
                )
            }

            // 歌曲列表
            item(key = "songs") {
                SongListCard(
                    songs = songs,
                    isLoading = isLoading,
                    currentMusicId = currentMusic?.id,
                    emptyText = "收藏夹还没有歌曲",
                    onSongClick = { index ->
                        viewModel.play(index)
                        navController.navigate(Screens.MusicPlayer.route)
                    },
                    onLikeClick = { viewModel.toggleLike(it) }
                )
            }

            // 上拉加载
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

/**
 * 收藏夹信息卡：封面 + 名称 + 歌曲数 + 编辑图标（与 Flutter 端 buildCoverWidget 一致）
 */
@Composable
private fun DirectoryHeaderCard(
    name: String,
    cover: String,
    total: Int,
    onEditClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Dimens.moduleBorderRadius))
            .background(Color.White)
            .padding(Dimens.middleGap),
        verticalAlignment = Alignment.CenterVertically
    ) {
        MusicCover(
            coverUrl = cover.takeIf { it.isNotBlank() },
            size = Dimens.bigAvatar,
            name = name.take(1)
        )
        Spacer(modifier = Modifier.width(Dimens.middleGap))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = name,
                color = Color.Black,
                fontSize = Dimens.bigFontSize,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.size(Dimens.smallGap))
            Text(
                text = "${total}首",
                color = Color.Secondary,
                fontSize = Dimens.normalFontSize
            )
        }
        Image(
            painter = painterResource(R.drawable.icon_edit),
            contentDescription = "编辑收藏夹",
            colorFilter = ColorFilter.tint(Color.Secondary),
            modifier = Modifier
                .size(Dimens.middleIconSize)
                .clickable { onEditClick() }
        )
    }
}
