package com.player.music.ui.page

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
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
import com.player.music.viewmodel.MusicSearchViewModel

/**
 * 音乐搜索页
 * 对应 Flutter 端 lib/pages/MusicSearchPage.dart：
 * 顶部搜索框（清空按钮 + 搜索按钮）、历史搜索标签、搜索结果列表（点击播放、点赞、更多），
 * 上拉加载更多，点歌曲进播放页
 *
 * @param initialKeyword 首页搜索栏带入的关键词，作为输入框提示
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MusicSearchPage(
    navController: NavHostController,
    initialKeyword: String = "",
    viewModel: MusicSearchViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val input by viewModel.input.collectAsState()
    val results by viewModel.results.collectAsState()
    val total by viewModel.total.collectAsState()
    val isSearching by viewModel.isSearching.collectAsState()
    val isLoadingMore by viewModel.isLoadingMore.collectAsState()
    val reachedEnd by viewModel.reachedEnd.collectAsState()
    val hasSearched by viewModel.hasSearched.collectAsState()
    val history by viewModel.history.collectAsState()
    val currentMusic by viewModel.player.currentMusic.collectAsState()
    val toastMessage by viewModel.toastMessage.collectAsState()

    val listState = rememberLazyListState()

    LaunchedEffect(toastMessage) {
        toastMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearToast()
        }
    }

    // 滚动到底部自动加载下一页（等价于 Flutter 端 EasyRefresh 的 onLoad）
    val isAtBottom by remember {
        derivedStateOf {
            val info = listState.layoutInfo
            val lastVisible = info.visibleItemsInfo.lastOrNull()?.index ?: -1
            info.totalItemsCount > 0 && lastVisible >= info.totalItemsCount - 2
        }
    }
    LaunchedEffect(isAtBottom, results.size) {
        if (isAtBottom) viewModel.loadMore()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.PageBackground)
            .statusBarsPadding()
    ) {
        SearchBar(
            input = input,
            hint = initialKeyword.ifBlank { "搜索歌曲、歌手、专辑" },
            onInputChange = { viewModel.onInputChange(it) },
            onClear = { viewModel.clearInput() },
            onBack = { navController.popBackStack() },
            onSearch = { viewModel.search() }
        )

        when {
            // 未搜索：历史搜索
            !hasSearched -> HistorySection(
                history = history,
                onHistoryClick = { viewModel.search(it) }
            )

            isSearching && results.isEmpty() -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color.Primary)
                }
            }

            results.isEmpty() -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "没有找到相关歌曲",
                        color = Color.Secondary,
                        fontSize = Dimens.normalFontSize
                    )
                }
            }

            else -> {
                Text(
                    text = "共找到 ${total} 首歌曲",
                    color = Color.Secondary,
                    fontSize = Dimens.normalFontSize,
                    modifier = Modifier.padding(
                        start = Dimens.middleGap,
                        end = Dimens.middleGap,
                        bottom = Dimens.smallGap
                    )
                )
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = Dimens.middleGap)
                ) {
                    itemsIndexed(results, key = { _, item -> item.id }) { index, music ->
                        SearchMusicRow(
                            music = music,
                            isCurrent = currentMusic?.id == music.id,
                            isFirst = index == 0,
                            isLast = index == results.lastIndex,
                            onClick = {
                                viewModel.play(index)
                                navController.navigate(Screens.MusicPlayer.route)
                            },
                            onLikeClick = { viewModel.toggleLike(music) }
                        )
                    }

                    item(key = "load_more") {
                        LoadMoreFooter(
                            isLoadingMore = isLoadingMore,
                            reachedEnd = reachedEnd,
                            onLoadMore = { viewModel.loadMore() }
                        )
                    }

                    item(key = "bottom_space") {
                        Spacer(modifier = Modifier.navigationBarsPadding())
                    }
                }
            }
        }
    }
}

/**
 * 顶部搜索栏：返回 + 灰色圆角输入框（清空按钮）+ 搜索按钮
 * 样式与首页搜索栏一致（白底圆角卡片、左右 middleGap 外边距与内边距）
 */
@Composable
private fun SearchBar(
    input: String,
    hint: String,
    onInputChange: (String) -> Unit,
    onClear: () -> Unit,
    onBack: () -> Unit,
    onSearch: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Dimens.middleGap, vertical = Dimens.middleGap)
            .clip(RoundedCornerShape(Dimens.moduleBorderRadius))
            .background(Color.White)
            .padding(Dimens.middleGap),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(R.drawable.icon_back),
            contentDescription = "返回",
            modifier = Modifier
                .size(Dimens.middleIconSize)
                .clickable { onBack() }
        )

        Spacer(modifier = Modifier.width(Dimens.middleGap))

        // 输入框
        Row(
            modifier = Modifier
                .weight(1f)
                .height(Dimens.inputHeight)
                .clip(RoundedCornerShape(Dimens.inputHeight / 2))
                .background(Color.PageBackground)
                .padding(horizontal = Dimens.middleGap),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                if (input.isEmpty()) {
                    Text(
                        text = hint,
                        color = Color.Secondary,
                        fontSize = Dimens.normalFontSize,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                BasicTextField(
                    value = input,
                    onValueChange = onInputChange,
                    singleLine = true,
                    textStyle = TextStyle(
                        color = Color.Black,
                        fontSize = Dimens.normalFontSize
                    ),
                    cursorBrush = SolidColor(Color.Primary),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (input.isNotEmpty()) {
                Spacer(modifier = Modifier.width(Dimens.smallGap))
                Image(
                    painter = painterResource(R.drawable.icon_clear),
                    contentDescription = "清空",
                    modifier = Modifier
                        .size(Dimens.smallIconSize)
                        .clickable { onClear() }
                )
            }
        }

        Spacer(modifier = Modifier.width(Dimens.middleGap))

        // 搜索按钮
        Box(
            modifier = Modifier
                .height(Dimens.inputHeight)
                .clip(RoundedCornerShape(Dimens.bigBorderRadius))
                .background(Color.Primary)
                .clickable { onSearch() }
                .padding(horizontal = Dimens.middleGap * 2),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "搜索",
                color = Color.White,
                fontSize = Dimens.normalFontSize
            )
        }
    }
}

/**
 * 历史搜索：白色卡片 + 「历史搜索」标题 + 关键词标签（无记录时展示提示）
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HistorySection(
    history: List<String>,
    onHistoryClick: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Dimens.middleGap)
            .clip(RoundedCornerShape(Dimens.moduleBorderRadius))
            .background(Color.White)
            .padding(Dimens.middleGap)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .width(Dimens.titleBarWidth)
                    .height(Dimens.titleBarHeight)
                    .background(Color.Primary)
            )
            Spacer(modifier = Modifier.width(Dimens.smallGap))
            Text(
                text = "历史搜索",
                color = Color.Black,
                fontSize = Dimens.normalFontSize,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(Dimens.middleGap))

        if (history.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(Dimens.emptyTipHeight),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "暂无搜索记录",
                    color = Color.Secondary,
                    fontSize = Dimens.normalFontSize
                )
            }
        } else {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(Dimens.smallGap),
                verticalArrangement = Arrangement.spacedBy(Dimens.smallGap)
            ) {
                history.forEach { keyword ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(Dimens.bigBorderRadius))
                            .background(Color.Primary)
                            .clickable { onHistoryClick(keyword) }
                            .padding(
                                horizontal = Dimens.middleGap,
                                vertical = Dimens.smallGap
                            )
                    ) {
                        Text(
                            text = keyword,
                            color = Color.White,
                            fontSize = Dimens.normalFontSize,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

/**
 * 搜索结果单行：封面 + 「歌手 - 歌名」+ 播放/正在播放 + 点赞 + 更多
 * 首尾行带圆角，中间行直角，整体拼成一张白色卡片
 */
@Composable
private fun SearchMusicRow(
    music: Music,
    isCurrent: Boolean,
    isFirst: Boolean,
    isLast: Boolean,
    onClick: () -> Unit,
    onLikeClick: () -> Unit
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
                color = if (isCurrent) Color.Primary else Color.Black,
                fontSize = Dimens.normalFontSize,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )

            Spacer(modifier = Modifier.width(Dimens.middleGap))

            Image(
                painter = painterResource(
                    if (isCurrent) R.drawable.icon_music_playing_grey else R.drawable.icon_music_play
                ),
                contentDescription = "播放",
                modifier = Modifier.size(Dimens.listIconSize)
            )

            Spacer(modifier = Modifier.width(Dimens.middleGap))

            Image(
                painter = painterResource(
                    if (music.isLike == 1) R.drawable.icon_like_active else R.drawable.icon_like
                ),
                contentDescription = if (music.isLike == 1) "取消喜欢" else "喜欢",
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

        if (!isLast) {
            Divider(
                color = Color.DisableColor,
                thickness = Dimens.borderSize,
                modifier = Modifier.padding(horizontal = Dimens.middleGap)
            )
        }
    }
}

/**
 * 列表底部状态：加载中 / 上拉加载更多 / 已经到底了（点击可手动重试）
 */
@Composable
private fun LoadMoreFooter(
    isLoadingMore: Boolean,
    reachedEnd: Boolean,
    onLoadMore: () -> Unit
) {
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
