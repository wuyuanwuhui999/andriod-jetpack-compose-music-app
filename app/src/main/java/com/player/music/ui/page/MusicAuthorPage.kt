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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.player.music.R
import com.player.music.navigation.Screens
import com.player.music.ui.components.MusicCover
import com.player.music.ui.theme.Color
import com.player.music.ui.theme.Dimens
import com.player.music.viewmodel.MusicAuthorViewModel

/**
 * 歌手详情页
 * 播放页 / 首页推荐歌手点击后进入：歌手头像 + 名字 + 关注 + 歌曲列表
 */
@Composable
fun MusicAuthorPage(
    navController: NavHostController,
    authorId: Long,
    authorName: String,
    avatar: String,
    viewModel: MusicAuthorViewModel = hiltViewModel()
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val songs by viewModel.songs.collectAsState()
    val followed by viewModel.followed.collectAsState()
    val loading by viewModel.loading.collectAsState()
    val currentMusic by viewModel.player.currentMusic.collectAsState()
    val toastMessage by viewModel.toastMessage.collectAsState()

    LaunchedEffect(authorId) {
        viewModel.load(authorId)
    }

    LaunchedEffect(toastMessage) {
        toastMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearToast()
        }
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
                text = "歌手",
                color = Color.Black,
                fontSize = Dimens.bigFontSize,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.size(Dimens.middleIconSize))
        }

        // 歌手信息
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimens.middleGap)
                .clip(RoundedCornerShape(Dimens.moduleBorderRadius))
                .background(Color.White)
                .padding(Dimens.middleGap),
            verticalAlignment = Alignment.CenterVertically
        ) {
            MusicCover(
                coverUrl = avatar,
                size = Dimens.bigAvatar,
                name = authorName.take(1)
            )
            Spacer(modifier = Modifier.width(Dimens.middleGap))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = authorName.ifBlank { "未知歌手" },
                    color = Color.Black,
                    fontSize = Dimens.bigFontSize,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(Dimens.smallGap))
                Text(
                    text = "${songs.size}首歌曲",
                    color = Color.Secondary,
                    fontSize = Dimens.normalFontSize
                )
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(Dimens.bigBorderRadius))
                    .background(if (followed) Color.PageBackground else Color.Primary)
                    .clickable { viewModel.toggleFollow(authorId) }
                    .padding(horizontal = Dimens.middleGap, vertical = Dimens.smallGap),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (followed) "已关注" else "关注",
                    color = if (followed) Color.Secondary else Color.White,
                    fontSize = Dimens.normalFontSize
                )
            }
        }

        when {
            loading && songs.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color.Primary)
                }
            }

            songs.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "该歌手暂无歌曲",
                        color = Color.Secondary,
                        fontSize = Dimens.normalFontSize
                    )
                }
            }

            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = Dimens.middleGap),
                    verticalArrangement = Arrangement.spacedBy(Dimens.middleGap)
                ) {
                    itemsIndexed(songs, key = { _, item -> item.id }) { index, music ->
                        AuthorSongRow(
                            index = index,
                            songName = music.songName ?: "",
                            albumName = music.albumName ?: "",
                            cover = music.cover,
                            isCurrent = currentMusic?.id == music.id,
                            onClick = {
                                viewModel.playSong(index)
                                navController.navigate(Screens.MusicPlayer.route)
                            }
                        )
                    }
                    item { Spacer(modifier = Modifier.navigationBarsPadding()) }
                }
            }
        }
    }
}

/**
 * 歌手页的歌曲行：序号/封面 + 歌名 + 专辑 + 播放图标
 */
@Composable
private fun AuthorSongRow(
    index: Int,
    songName: String,
    albumName: String,
    cover: String?,
    isCurrent: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Dimens.moduleBorderRadius))
            .background(Color.White)
            .clickable { onClick() }
            .padding(Dimens.middleGap),
        verticalAlignment = Alignment.CenterVertically
    ) {
        MusicCover(coverUrl = cover, size = Dimens.middleAvatar)
        Spacer(modifier = Modifier.width(Dimens.middleGap))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = songName,
                color = if (isCurrent) Color.Primary else Color.Black,
                fontSize = Dimens.normalFontSize,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = albumName.ifBlank { "第 ${index + 1} 首" },
                color = Color.Secondary,
                fontSize = Dimens.normalFontSize,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Image(
            painter = painterResource(
                if (isCurrent) R.drawable.icon_music_playing else R.drawable.icon_music_play
            ),
            contentDescription = "播放",
            colorFilter = ColorFilter.tint(Color.Secondary),
            modifier = Modifier.size(Dimens.listIconSize)
        )
    }
}
