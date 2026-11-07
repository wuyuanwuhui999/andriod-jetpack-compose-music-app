package com.player.music.ui.page

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.player.music.R
import com.player.music.model.Music
import com.player.music.ui.components.MusicCover
import com.player.music.ui.theme.Color
import com.player.music.ui.theme.Dimens
import com.player.music.viewmodel.MusicPickViewModel

/** 选择音乐页回传给分享页的 key */
const val PICKED_MUSIC_KEY = "picked_music"

/**
 * 选择音乐页（分享到音乐圈时挑歌用）
 * 对应 uni-app 端 pages/MusicCirCleCheckPage.vue：
 * 输入歌曲/歌手名搜索 → 列表（封面 + 歌手 - 歌名）→ 点一条即选中并返回分享页
 */
@Composable
fun MusicPickPage(
    navController: NavHostController,
    viewModel: MusicPickViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val input by viewModel.input.collectAsState()
    val results by viewModel.results.collectAsState()
    val isSearching by viewModel.isSearching.collectAsState()
    val hasSearched by viewModel.hasSearched.collectAsState()
    val toastMessage by viewModel.toastMessage.collectAsState()

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
        // 顶部标题栏
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
                text = "选择音乐",
                color = Color.Black,
                fontSize = Dimens.bigFontSize,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.size(Dimens.middleIconSize))
        }

        // 搜索框
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimens.middleGap)
                .height(Dimens.inputHeight)
                .clip(RoundedCornerShape(Dimens.bigBorderRadius))
                .background(Color.White)
                .padding(horizontal = Dimens.middleGap),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                if (input.isEmpty()) {
                    Text(
                        text = "请输入歌曲或者歌手名称",
                        color = Color.Secondary,
                        fontSize = Dimens.normalFontSize,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                BasicTextField(
                    value = input,
                    onValueChange = { viewModel.onInputChange(it) },
                    singleLine = true,
                    textStyle = TextStyle(
                        color = Color.Black,
                        fontSize = Dimens.normalFontSize
                    ),
                    cursorBrush = SolidColor(Color.Primary),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Spacer(modifier = Modifier.width(Dimens.middleGap))
            Image(
                painter = painterResource(R.drawable.icon_search),
                contentDescription = "搜索",
                modifier = Modifier
                    .size(Dimens.middleIconSize)
                    .clickable { viewModel.search() }
            )
        }

        when {
            isSearching -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color.Primary)
                }
            }

            results.isNotEmpty() -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = Dimens.middleGap,
                        end = Dimens.middleGap,
                        bottom = Dimens.middleGap
                    )
                ) {
                    itemsIndexed(results, key = { _, item -> item.id }) { index, music ->
                        MusicPickRow(
                            music = music,
                            isFirst = index == 0,
                            isLast = index == results.lastIndex,
                            onClick = {
                                navController.previousBackStackEntry
                                    ?.savedStateHandle
                                    ?.set(PICKED_MUSIC_KEY, viewModel.toJson(music))
                                navController.popBackStack()
                            }
                        )
                    }
                    item { Spacer(modifier = Modifier.navigationBarsPadding()) }
                }
            }

            hasSearched -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "暂无数据",
                        color = Color.Secondary,
                        fontSize = Dimens.normalFontSize
                    )
                }
            }
        }
    }
}

/**
 * 选择音乐列表行：封面 + 「歌手 - 歌名」，首尾行带圆角
 */
@Composable
private fun MusicPickRow(
    music: Music,
    isFirst: Boolean,
    isLast: Boolean,
    onClick: () -> Unit
) {
    val shape = when {
        isFirst && isLast -> RoundedCornerShape(Dimens.moduleBorderRadius)
        isFirst -> RoundedCornerShape(
            topStart = Dimens.moduleBorderRadius,
            topEnd = Dimens.moduleBorderRadius
        )
        isLast -> RoundedCornerShape(
            bottomStart = Dimens.moduleBorderRadius,
            bottomEnd = Dimens.moduleBorderRadius
        )
        else -> RoundedCornerShape(0.dp)
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(Color.White)
                .clickable { onClick() }
                .padding(Dimens.middleGap),
            verticalAlignment = Alignment.CenterVertically
        ) {
            MusicCover(coverUrl = music.cover, size = Dimens.middleAvatar)
            Spacer(modifier = Modifier.width(Dimens.middleGap))
            Text(
                text = listOfNotNull(
                    music.authorName?.takeIf { it.isNotBlank() },
                    music.songName?.takeIf { it.isNotBlank() }
                ).joinToString(" - "),
                color = Color.Black,
                fontSize = Dimens.normalFontSize,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(Dimens.middleGap))
            Image(
                painter = painterResource(R.drawable.icon_arrow),
                contentDescription = null,
                modifier = Modifier.size(Dimens.smallIconSize)
            )
        }

        if (!isLast) {
            Divider(
                color = Color.DisableColor,
                thickness = Dimens.borderSize,
                modifier = Modifier.padding(horizontal = Dimens.middleGap)
            )
        }
    }
}
