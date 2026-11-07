package com.player.music.ui.page

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.player.music.R
import com.player.music.ui.theme.Color
import com.player.music.ui.theme.Dimens
import com.player.music.utils.CommonUtils
import com.player.music.utils.LyricLine
import com.player.music.utils.LyricParser
import com.player.music.viewmodel.MusicPlayerViewModel

/**
 * 歌词页
 * 对应 Flutter 端 lib/pages/MusicLyricPage.dart：模糊封面背景 + 全屏歌词
 * （Flutter 版歌词组件是注释掉的空占位，这里按播放页的歌词样式补全：
 *  当前行高亮、自动滚动居中、点击某行跳转到该句）
 *
 * 从播放页点击歌词区域进入（Screens.MusicLyric）。
 */
@Composable
fun MusicLyricPage(
    navController: NavHostController,
    viewModel: MusicPlayerViewModel = hiltViewModel()
) {
    val music by viewModel.currentMusic.collectAsState()
    val positionMs by viewModel.positionMs.collectAsState()

    val lyrics = remember(music?.id, music?.lyrics) { LyricParser.parse(music?.lyrics) }
    val currentIndex = remember(lyrics, positionMs) {
        LyricParser.currentIndex(lyrics, positionMs)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // 背景：模糊封面（与播放页一致）
        val coverUrl = CommonUtils.formatImageUrl(music?.cover)
        if (coverUrl != null) {
            AsyncImage(
                model = coverUrl,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .blur(50.dp),
                contentScale = ContentScale.Crop
            )
        } else {
            Image(
                painter = painterResource(R.drawable.default_cover),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .blur(50.dp),
                contentScale = ContentScale.Crop
            )
        }
        // 压暗，保证文字可读
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.55f))
        )

        Column(modifier = Modifier.fillMaxSize()) {
            // 顶部：返回 + 歌名/歌手
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = Dimens.middleGap, vertical = Dimens.middleGap),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(R.drawable.icon_back),
                    contentDescription = "返回",
                    colorFilter = ColorFilter.tint(Color.White),
                    modifier = Modifier
                        .size(Dimens.middleIconSize)
                        .clickable { navController.popBackStack() }
                )
                Spacer(modifier = Modifier.width(Dimens.middleGap))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = music?.songName ?: "",
                        color = Color.White,
                        fontSize = Dimens.normalFontSize,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = music?.authorName ?: "",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = Dimens.normalFontSize,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // 歌词
            LyricList(
                lyrics = lyrics,
                currentIndex = currentIndex,
                onLineClick = { line -> viewModel.seekTo(line.timeMs) },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            )

            Spacer(modifier = Modifier.navigationBarsPadding())
        }
    }
}

/**
 * 歌词列表：当前行高亮放大，自动滚动到可视区域中间，点击某行跳到该句
 */
@Composable
private fun LyricList(
    lyrics: List<LyricLine>,
    currentIndex: Int,
    onLineClick: (LyricLine) -> Unit,
    modifier: Modifier = Modifier
) {
    if (lyrics.isEmpty()) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Text(
                text = "暂无歌词",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = Dimens.normalFontSize
            )
        }
        return
    }

    BoxWithConstraints(modifier = modifier) {
        // 按行高估算一屏能显示多少行，滚动时把当前行放在中间
        val visibleLines = (maxHeight / Dimens.lyricLineHeight).toInt().coerceAtLeast(1)
        val listState = rememberLazyListState()

        LaunchedEffect(currentIndex) {
            if (currentIndex >= 0 && currentIndex < lyrics.size) {
                val target = (currentIndex - visibleLines / 2).coerceAtLeast(0)
                listState.animateScrollToItem(target)
            }
        }

        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Dimens.smallGap)
        ) {
            itemsIndexed(lyrics) { index, line ->
                val isCurrent = index == currentIndex
                Text(
                    text = line.text,
                    color = if (isCurrent) Color.Primary else Color.White.copy(alpha = 0.55f),
                    fontSize = if (isCurrent) Dimens.bigFontSize else Dimens.normalFontSize,
                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                    textAlign = TextAlign.Center,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Dimens.bigGap, vertical = Dimens.smallGap)
                        .clickable { onLineClick(line) }
                )
            }
        }
    }
}
