package com.player.music.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import coil.compose.AsyncImage
import com.player.music.R
import com.player.music.ui.theme.Color
import com.player.music.ui.theme.Dimens
import com.player.music.utils.CommonUtils

/**
 * 音乐封面 / 歌手头像组件（圆形，与 Flutter 端 MusicAvaterComponent 一致）
 * @param coverUrl 服务端返回的封面地址（相对路径或完整 http 地址）
 * @param size 直径
 * @param name 无封面时展示的占位文字（如歌手名首字），为空时展示默认图
 * @param fallbackRes 加载失败 / 无图时的默认图，歌手上传用 default_avater
 */
@Composable
fun MusicCover(
    coverUrl: String?,
    size: Dp,
    modifier: Modifier = Modifier,
    name: String = "",
    @DrawableRes fallbackRes: Int = R.drawable.default_cover
) {
    val fullUrl = CommonUtils.formatImageUrl(coverUrl)

    if (fullUrl != null) {
        AsyncImage(
            model = fullUrl,
            contentDescription = "封面",
            modifier = modifier
                .size(size)
                .clip(CircleShape),
            contentScale = ContentScale.Crop,
            placeholder = painterResource(fallbackRes),
            error = painterResource(fallbackRes)
        )
    } else if (name.isNotBlank()) {
        // 没有图片但有名称：灰底 + 名称首字
        Box(
            modifier = modifier
                .size(size)
                .clip(CircleShape)
                .background(Color.PageBackground),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = name,
                color = Color.Secondary,
                fontSize = Dimens.normalFontSize
            )
        }
    } else {
        Image(
            painter = painterResource(fallbackRes),
            contentDescription = "默认封面",
            modifier = modifier
                .size(size)
                .clip(CircleShape),
            contentScale = ContentScale.Crop
        )
    }
}
