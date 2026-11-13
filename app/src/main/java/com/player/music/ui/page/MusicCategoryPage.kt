package com.player.music.ui.page

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.ColorFilter
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
import com.player.music.model.MusicCategory
import com.player.music.navigation.Screens
import com.player.music.ui.components.SongListCard
import com.player.music.ui.theme.Color
import com.player.music.ui.theme.Dimens
import com.player.music.viewmodel.MusicCategoryViewModel

/**
 * 歌曲分类页（首页「分类歌曲」快捷入口）
 * 对应 Flutter 端 lib/pages/MusicCategoryPage.dart：
 * 三列分类网格（默认展示前 9 个，可展开/收起，选中橙色高亮）+ 该分类的歌曲列表
 * （封面/歌手-歌名/播放/点赞/更多），上拉加载更多，点击歌曲播放并进入播放页
 */
@Composable
fun MusicCategoryPage(
    navController: NavHostController,
    viewModel: MusicCategoryViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val categories by viewModel.categories.collectAsState()
    val expanded by viewModel.expanded.collectAsState()
    val activeIndex by viewModel.activeIndex.collectAsState()
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

    val visibleCategories = remember(categories, expanded) {
        if (expanded) categories else categories.take(MusicCategoryViewModel.COLLAPSED_COUNT)
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
                text = "分类歌曲",
                color = Color.Black,
                fontSize = Dimens.bigFontSize,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.size(Dimens.middleIconSize))
        }

        when {
            isLoading && categories.isEmpty() -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color.Primary)
                }
            }

            categories.isEmpty() -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "暂无歌曲分类",
                        color = Color.Secondary,
                        fontSize = Dimens.normalFontSize
                    )
                }
            }

            else -> {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(Dimens.middleGap),
                    verticalArrangement = Arrangement.spacedBy(Dimens.middleGap)
                ) {
                    // 分类网格
                    item(key = "category") {
                        CategoryGrid(
                            categories = visibleCategories,
                            activeIndex = activeIndex,
                            expanded = expanded,
                            canExpand = categories.size > MusicCategoryViewModel.COLLAPSED_COUNT,
                            onSelect = { viewModel.selectCategory(it) },
                            onToggleExpand = { viewModel.toggleExpand() }
                        )
                    }

                    // 歌曲列表
                    item(key = "songs") {
                        SongListCard(
                            songs = songs,
                            isLoading = isLoading,
                            currentMusicId = currentMusic?.id,
                            onSongClick = { index ->
                                viewModel.play(index)
                                navController.navigate(Screens.MusicPlayer.route)
                            },
                            onLikeClick = { viewModel.toggleLike(it) }
                        )
                    }

                    // 上拉加载
                    item(key = "load_more") {
                        LoadMoreFooter(
                            isLoadingMore = isLoadingMore,
                            reachedEnd = reachedEnd,
                            hasData = songs.isNotEmpty(),
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
 * 分类网格：一行三个，选中项橙色边框 + 橙色文字，底部可展开/收起
 */
@Composable
private fun CategoryGrid(
    categories: List<MusicCategory>,
    activeIndex: Int,
    expanded: Boolean,
    canExpand: Boolean,
    onSelect: (Int) -> Unit,
    onToggleExpand: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Dimens.moduleBorderRadius))
            .background(Color.White)
            .padding(Dimens.middleGap)
    ) {
        categories.chunked(3).forEachIndexed { rowIndex, row ->
            if (rowIndex > 0) {
                Spacer(modifier = Modifier.height(Dimens.smallGap))
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Dimens.smallGap)
            ) {
                row.forEachIndexed { columnIndex, category ->
                    val index = rowIndex * 3 + columnIndex
                    val selected = index == activeIndex
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(Dimens.inputHeight)
                            .clip(RoundedCornerShape(Dimens.moduleBorderRadius))
                            .border(
                                width = Dimens.borderSize,
                                color = if (selected) Color.Primary else Color.DisableColor,
                                shape = RoundedCornerShape(Dimens.moduleBorderRadius)
                            )
                            .clickable { onSelect(index) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = category.displayName,
                            color = if (selected) Color.Primary else Color.Black,
                            fontSize = Dimens.normalFontSize,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                // 不足三个时补空位
                repeat(3 - row.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }

        if (canExpand) {
            Spacer(modifier = Modifier.height(Dimens.middleGap))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleExpand() },
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (expanded) "收起" else "展开更多",
                    color = Color.Secondary,
                    fontSize = Dimens.normalFontSize
                )
                Spacer(modifier = Modifier.width(Dimens.smallGap))
                Image(
                    painter = painterResource(R.drawable.icon_arrow),
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(Color.Secondary),
                    modifier = Modifier
                        .size(Dimens.smallIconSize)
                        .rotate(if (expanded) 90f else 0f)
                )
            }
        }
    }
}

/**
 * 列表底部状态
 */
@Composable
private fun LoadMoreFooter(
    isLoadingMore: Boolean,
    reachedEnd: Boolean,
    hasData: Boolean,
    onLoadMore: () -> Unit
) {
    if (!hasData) return
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
