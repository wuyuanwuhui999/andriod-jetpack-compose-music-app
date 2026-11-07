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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.player.music.R
import com.player.music.model.Music
import com.player.music.navigation.Screens
import com.player.music.ui.components.MusicCover
import com.player.music.ui.theme.Color
import com.player.music.ui.theme.Dimens
import com.player.music.viewmodel.RecordMusicViewModel

/**
 * 播放记录页（我的页「播放记录」更多进入）
 * 对应 Flutter 端 lib/pages/RecordMusicPage.dart：
 * 标题「最近听过的歌曲」，圆形封面 + 歌名 + 「听过N次」+ 播放/更多，
 * 上拉加载更多，点击播放整页记录并进入播放页
 */
@Composable
fun RecordMusicPage(
    navController: NavHostController,
    viewModel: RecordMusicViewModel = hiltViewModel()
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
                text = "最近听过的歌曲",
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
            item(key = "records") {
                RecordListCard(
                    songs = songs,
                    isLoading = isLoading,
                    currentMusicId = currentMusic?.id,
                    onPlay = { index ->
                        viewModel.play(index)
                        navController.navigate(Screens.MusicPlayer.route)
                    }
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

/**
 * 播放记录列表：圆形封面 + 歌名 + 「听过N次」+ 播放/更多
 */
@Composable
private fun RecordListCard(
    songs: List<Music>,
    isLoading: Boolean,
    currentMusicId: Long?,
    onPlay: (Int) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Dimens.moduleBorderRadius))
            .background(Color.White)
            .padding(horizontal = Dimens.middleGap)
    ) {
        when {
            isLoading && songs.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = Dimens.bigGap),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(Dimens.middleIconSize),
                        color = Color.Primary,
                        strokeWidth = 2.dp
                    )
                }
            }

            songs.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = Dimens.bigGap),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "还没有播放记录",
                        color = Color.Secondary,
                        fontSize = Dimens.normalFontSize
                    )
                }
            }

            else -> {
                songs.forEachIndexed { index, music ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onPlay(index) }
                            .padding(vertical = Dimens.middleGap),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 与 Flutter 端一致：播放记录用圆形封面
                        MusicCover(
                            coverUrl = music.cover,
                            size = Dimens.bigAvatar,
                            modifier = Modifier.clip(RoundedCornerShape(percent = 50))
                        )
                        Spacer(modifier = Modifier.width(Dimens.middleGap))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = music.songName ?: "",
                                color = if (currentMusicId == music.id) Color.Primary else Color.Black,
                                fontSize = Dimens.normalFontSize,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.size(Dimens.smallGap))
                            Text(
                                text = "听过${music.times}次",
                                color = Color.Secondary,
                                fontSize = Dimens.normalFontSize
                            )
                        }
                        Image(
                            painter = painterResource(
                                if (currentMusicId == music.id) R.drawable.icon_music_playing_grey
                                else R.drawable.icon_music_play
                            ),
                            contentDescription = "播放",
                            modifier = Modifier.size(Dimens.listIconSize)
                        )
                        Spacer(modifier = Modifier.width(Dimens.middleGap * 2))
                        Image(
                            painter = painterResource(R.drawable.icon_music_menu),
                            contentDescription = "更多操作",
                            modifier = Modifier.size(Dimens.listIconSize)
                        )
                    }
                    if (index != songs.lastIndex) {
                        Divider(color = Color.PageBackground, thickness = Dimens.borderSize)
                    }
                }
            }
        }
    }
}
