package com.player.music.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.player.music.R
import com.player.music.model.Music
import com.player.music.ui.theme.Color
import com.player.music.ui.theme.Dimens

/**
 * 通用歌曲列表卡片：白色圆角卡片 + 行间分割线
 * 行内容：封面 + 「歌手 - 歌名」+ 播放/正在播放 + 点赞 + 更多
 * 播放中/被点赞的歌曲高亮
 *
 * @param songs 歌曲列表
 * @param isLoading 首次加载中（列表为空时展示 loading）
 * @param currentMusicId 当前播放的歌曲 id（用于高亮与播放中图标）
 * @param emptyText 列表为空的提示文案
 * @param onSongClick 点击整行（参数为下标）
 * @param onLikeClick 点击点赞图标
 */
@Composable
fun SongListCard(
    songs: List<Music>,
    isLoading: Boolean,
    currentMusicId: Long?,
    onSongClick: (Int) -> Unit,
    onLikeClick: (Music) -> Unit,
    modifier: Modifier = Modifier,
    emptyText: String = "暂无歌曲"
) {
    Column(
        modifier = modifier
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
                        text = emptyText,
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
                            .clickable { onSongClick(index) }
                            .padding(vertical = Dimens.middleGap),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        MusicCover(coverUrl = music.cover, size = Dimens.middleAvatar)
                        Spacer(modifier = Modifier.width(Dimens.middleGap))
                        Text(
                            text = listOfNotNull(
                                music.authorName?.takeIf { it.isNotBlank() },
                                music.songName?.takeIf { it.isNotBlank() }
                            ).joinToString(" - "),
                            color = if (currentMusicId == music.id) Color.Primary else Color.Black,
                            fontSize = Dimens.normalFontSize,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(Dimens.middleGap))
                        Image(
                            painter = painterResource(
                                if (currentMusicId == music.id) R.drawable.icon_music_playing_grey
                                else R.drawable.icon_music_play
                            ),
                            contentDescription = "播放",
                            modifier = Modifier.size(Dimens.listIconSize)
                        )
                        Spacer(modifier = Modifier.width(Dimens.middleGap))
                        Image(
                            painter = painterResource(
                                if (music.isLike == 1) R.drawable.icon_like_active else R.drawable.icon_like
                            ),
                            contentDescription = if (music.isLike == 1) "取消点赞" else "点赞",
                            modifier = Modifier
                                .size(Dimens.listIconSize)
                                .clickable { onLikeClick(music) }
                        )
                        Spacer(modifier = Modifier.width(Dimens.middleGap))
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
