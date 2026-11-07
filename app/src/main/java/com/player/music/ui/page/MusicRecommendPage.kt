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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
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
import com.player.music.viewmodel.MusicRecommendViewModel

/**
 * 推荐页（推荐 Tab）
 * 对应 Flutter 端 lib/pages/MusicRecommentPage.dart：
 * 按榜单顺序展示歌曲，前三名展示金银铜奖牌图标，支持上拉加载更多与点赞/取消点赞
 */
@Composable
fun MusicRecommendPage(
    navController: NavHostController,
    listState: LazyListState,
    viewModel: MusicRecommendViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val musicList by viewModel.musicList.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isLoadingMore by viewModel.isLoadingMore.collectAsState()
    val reachedEnd by viewModel.reachedEnd.collectAsState()
    val toastMessage by viewModel.toastMessage.collectAsState()

    LaunchedEffect(toastMessage) {
        toastMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearToast()
        }
    }

    when {
        isLoading && musicList.isEmpty() -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.PageBackground),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color.Primary)
            }
        }

        musicList.isEmpty() -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.PageBackground),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "暂无推荐歌曲",
                    color = Color.Secondary,
                    fontSize = Dimens.normalFontSize
                )
            }
        }

        else -> {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.PageBackground),
                contentPadding = PaddingValues(
                    start = Dimens.middleGap,
                    end = Dimens.middleGap,
                    top = Dimens.middleGap,
                    bottom = Dimens.miniPlayerSize + Dimens.middleGap
                ),
                verticalArrangement = Arrangement.spacedBy(Dimens.middleGap)
            ) {
                itemsIndexed(musicList, key = { _, music -> music.id }) { index, music ->
                    RecommendMusicItem(
                        index = index,
                        music = music,
                        onPlayClick = {
                            viewModel.play(index)
                            navController.navigate(Screens.MusicPlayer.route)
                        },
                        onLikeClick = { viewModel.toggleLike(music) }
                    )
                }

                item(key = "load_more") {
                    RecommendLoadMoreFooter(
                        isLoadingMore = isLoadingMore,
                        reachedEnd = reachedEnd,
                        onLoadMore = { viewModel.loadMore() }
                    )
                }
            }
        }
    }
}

/**
 * 推荐列表单项：排名 + 封面 + 歌名/歌手 + 播放/点赞/更多
 */
@Composable
private fun RecommendMusicItem(
    index: Int,
    music: Music,
    onPlayClick: () -> Unit,
    onLikeClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Dimens.moduleBorderRadius),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimens.middleGap),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 排名：前三名展示奖牌图标，其余展示序号
            Box(
                modifier = Modifier.size(Dimens.middleIconSize),
                contentAlignment = Alignment.Center
            ) {
                when (index) {
                    0 -> Image(
                        painter = painterResource(R.drawable.icon_no1),
                        contentDescription = "第一名",
                        modifier = Modifier.size(Dimens.middleIconSize)
                    )
                    1 -> Image(
                        painter = painterResource(R.drawable.icon_no2),
                        contentDescription = "第二名",
                        modifier = Modifier.size(Dimens.middleIconSize)
                    )
                    2 -> Image(
                        painter = painterResource(R.drawable.icon_no3),
                        contentDescription = "第三名",
                        modifier = Modifier.size(Dimens.middleIconSize)
                    )
                    else -> Text(
                        text = (index + 1).toString(),
                        color = Color.Secondary,
                        fontSize = Dimens.normalFontSize
                    )
                }
            }

            Spacer(modifier = Modifier.width(Dimens.middleGap))

            MusicCover(
                coverUrl = music.cover,
                size = Dimens.middleAvatar
            )

            Spacer(modifier = Modifier.width(Dimens.middleGap))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = music.songName ?: "",
                    color = Color.Black,
                    fontSize = Dimens.normalFontSize,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(Dimens.smallGap))
                Text(
                    text = music.authorName ?: "",
                    color = Color.Secondary,
                    fontSize = Dimens.normalFontSize,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Image(
                painter = painterResource(R.drawable.icon_music_play),
                contentDescription = "播放",
                modifier = Modifier
                    .size(Dimens.listIconSize)
                    .clickable { onPlayClick() }
            )

            Spacer(modifier = Modifier.width(Dimens.middleGap))

            Image(
                painter = painterResource(
                    if (music.isLike == 1) R.drawable.icon_like_active else R.drawable.icon_like
                ),
                contentDescription = "点赞",
                modifier = Modifier
                    .size(Dimens.listIconSize)
                    .clickable { onLikeClick() }
            )

            Spacer(modifier = Modifier.width(Dimens.middleGap))

            Image(
                painter = painterResource(R.drawable.icon_music_menu),
                contentDescription = "更多操作",
                modifier = Modifier.size(Dimens.listIconSize)
            )
        }
    }
}

/**
 * 列表底部：进入可见区域自动加载下一页（相当于 Flutter 的上拉加载），到底后展示"已经到底了"
 */
@Composable
private fun RecommendLoadMoreFooter(
    isLoadingMore: Boolean,
    reachedEnd: Boolean,
    onLoadMore: () -> Unit
) {
    LaunchedEffect(reachedEnd) {
        if (!reachedEnd) onLoadMore()
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !reachedEnd) { onLoadMore() }
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
