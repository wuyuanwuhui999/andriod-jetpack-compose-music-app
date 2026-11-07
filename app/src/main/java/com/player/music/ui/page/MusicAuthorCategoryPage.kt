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
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.player.music.R
import com.player.music.model.MusicAuthor
import com.player.music.navigation.Screens
import com.player.music.ui.components.MusicCover
import com.player.music.ui.theme.Color
import com.player.music.ui.theme.Dimens
import com.player.music.viewmodel.MusicAuthorCategoryViewModel

/**
 * 歌手分类页（首页「歌手」快捷入口）
 * 对应 Flutter 端 lib/pages/MusicAuthorCategoryPage.dart：
 * 顶部歌手分类网格（四列，选中高亮）+ 该分类下的歌手列表（头像、名字、歌曲数、播放、关注、更多），
 * 上拉加载更多，点击歌手进入歌手详情页
 */
@Composable
fun MusicAuthorCategoryPage(
    navController: NavHostController,
    viewModel: MusicAuthorCategoryViewModel = hiltViewModel()
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val categories by viewModel.categories.collectAsState()
    val activeIndex by viewModel.activeIndex.collectAsState()
    val authors by viewModel.authors.collectAsState()
    val isInitialLoading by viewModel.isInitialLoading.collectAsState()
    val isLoadingMore by viewModel.isLoadingMore.collectAsState()
    val reachedEnd by viewModel.reachedEnd.collectAsState()
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
                text = "歌手分类",
                color = Color.Black,
                fontSize = Dimens.bigFontSize,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.size(Dimens.middleIconSize))
        }

        when {
            isInitialLoading && categories.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color.Primary)
                }
            }

            categories.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "暂无歌手分类",
                        color = Color.Secondary,
                        fontSize = Dimens.normalFontSize
                    )
                }
            }

            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(Dimens.middleGap),
                    verticalArrangement = Arrangement.spacedBy(Dimens.middleGap)
                ) {
                    // 歌手分类
                    item(key = "category") {
                        CategoryGrid(
                            categories = categories.map { it.categoryName },
                            activeIndex = activeIndex,
                            onSelect = { viewModel.selectCategory(it) }
                        )
                    }

                    // 歌手列表
                    item(key = "authors") {
                        AuthorListCard(
                            authors = authors,
                            isInitialLoading = isInitialLoading,
                            onAuthorClick = { author ->
                                if (author.authorId <= 0) {
                                    Toast.makeText(context, "暂无歌手信息", Toast.LENGTH_SHORT).show()
                                } else {
                                    navController.navigate(
                                        Screens.MusicAuthor.createRoute(
                                            authorId = author.authorId,
                                            authorName = author.authorName,
                                            avatar = author.avatar
                                        )
                                    )
                                }
                            },
                            onFollowClick = { viewModel.toggleFollow(it) }
                        )
                    }

                    // 上拉加载更多
                    item(key = "load_more") {
                        AuthorLoadMoreFooter(
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
 * 歌手分类网格：一行四个，选中的分类高亮为橙色边框 + 橙色文字
 */
@Composable
private fun CategoryGrid(
    categories: List<String>,
    activeIndex: Int,
    onSelect: (Int) -> Unit
) {
    val rows = categories.chunked(4)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Dimens.moduleBorderRadius))
            .background(Color.White)
            .padding(Dimens.middleGap),
        verticalArrangement = Arrangement.spacedBy(Dimens.smallGap)
    ) {
        rows.forEachIndexed { rowIndex, row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Dimens.smallGap)
            ) {
                row.forEachIndexed { columnIndex, name ->
                    val index = rowIndex * 4 + columnIndex
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
                            text = name,
                            color = if (selected) Color.Primary else Color.Black,
                            fontSize = Dimens.normalFontSize,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                // 不足四个时补空位，保证每列等宽
                repeat(4 - row.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

/**
 * 歌手列表卡片：头像 + 名字 + 歌曲数 + 播放 / 关注 / 更多
 */
@Composable
private fun AuthorListCard(
    authors: List<MusicAuthor>,
    isInitialLoading: Boolean,
    onAuthorClick: (MusicAuthor) -> Unit,
    onFollowClick: (MusicAuthor) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Dimens.moduleBorderRadius))
            .background(Color.White)
            .padding(horizontal = Dimens.middleGap)
    ) {
        when {
            isInitialLoading && authors.isEmpty() -> {
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

            authors.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = Dimens.bigGap),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "该分类暂无歌手",
                        color = Color.Secondary,
                        fontSize = Dimens.normalFontSize
                    )
                }
            }

            else -> {
                authors.forEachIndexed { index, author ->
                    AuthorRow(
                        author = author,
                        showDivider = index != authors.lastIndex,
                        onClick = { onAuthorClick(author) },
                        onFollowClick = { onFollowClick(author) }
                    )
                }
            }
        }
    }
}

/**
 * 歌手列表单项
 */
@Composable
private fun AuthorRow(
    author: MusicAuthor,
    showDivider: Boolean,
    onClick: () -> Unit,
    onFollowClick: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick() }
                .padding(vertical = Dimens.middleGap),
            verticalAlignment = Alignment.CenterVertically
        ) {
            MusicCover(
                coverUrl = author.avatar,
                size = Dimens.middleAvatar,
                name = author.authorName?.take(1) ?: ""
            )

            Spacer(modifier = Modifier.width(Dimens.middleGap))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = author.authorName ?: "",
                    color = Color.Black,
                    fontSize = Dimens.normalFontSize,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(Dimens.smallGap))
                Text(
                    text = "${author.total}首",
                    color = Color.Secondary,
                    fontSize = Dimens.normalFontSize,
                    fontWeight = FontWeight.Bold
                )
            }

            Image(
                painter = painterResource(R.drawable.icon_music_play),
                contentDescription = "播放",
                modifier = Modifier.size(Dimens.listIconSize)
            )

            Spacer(modifier = Modifier.width(Dimens.middleGap))

            Image(
                painter = painterResource(
                    if (author.isLike == 1) R.drawable.icon_like_active else R.drawable.icon_like
                ),
                contentDescription = if (author.isLike == 1) "取消关注" else "关注",
                modifier = Modifier
                    .size(Dimens.listIconSize)
                    .clickable { onFollowClick() }
            )

            Spacer(modifier = Modifier.width(Dimens.middleGap))

            Image(
                painter = painterResource(R.drawable.icon_music_menu),
                contentDescription = "更多操作",
                modifier = Modifier.size(Dimens.listIconSize)
            )
        }

        if (showDivider) {
            Divider(color = Color.PageBackground, thickness = Dimens.borderSize)
        }
    }
}

/**
 * 列表底部：进入可见区域自动加载下一页（相当于 Flutter 的上拉加载），到底后展示"已经到底了"
 */
@Composable
private fun AuthorLoadMoreFooter(
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
