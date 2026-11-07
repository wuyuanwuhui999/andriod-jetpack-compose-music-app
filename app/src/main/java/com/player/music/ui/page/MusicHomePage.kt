package com.player.music.ui.page

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.player.music.R
import com.player.music.model.Music
import com.player.music.model.MusicAuthor
import com.player.music.model.MusicCategory
import com.player.music.navigation.Screens
import com.player.music.ui.components.Avatar
import com.player.music.ui.components.AvatarSize
import com.player.music.ui.components.MusicCover
import com.player.music.ui.theme.Color
import com.player.music.ui.theme.Dimens
import com.player.music.viewmodel.MusicIndexViewModel

/**
 * 音乐首页（首页 Tab）
 * 对应 Flutter 端 lib/pages/MusicHomePage.dart：
 * 顶部搜索栏（头像 + 推荐关键字 + AI 入口）、四个快捷入口、分类歌曲/推荐歌手模块、上拉加载更多
 */
@Composable
fun MusicHomePage(
    navController: NavHostController,
    viewModel: MusicIndexViewModel,
    currentUserAvatar: String?,
    listState: LazyListState
) {
    val context = LocalContext.current
    val keyword by viewModel.keyword.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val visibleCount by viewModel.visibleCount.collectAsState()
    val isInitialLoading by viewModel.isInitialLoading.collectAsState()

    val visibleCategories = remember(categories, visibleCount) {
        categories.take(visibleCount)
    }

    /** 未开发功能的统一提示 */
    fun comingSoon(feature: String) {
        Toast.makeText(context, "${feature}功能开发中，敬请期待", Toast.LENGTH_SHORT).show()
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // 顶部搜索栏
        MusicSearchBar(
            avatar = currentUserAvatar,
            keyword = keyword,
            onSearchClick = { comingSoon("搜索") },
            onAiClick = { comingSoon("AI 聊天") }
        )

        if (isInitialLoading && categories.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color.Primary)
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = Dimens.middleGap,
                    end = Dimens.middleGap,
                    top = Dimens.middleGap,
                    // 底部留出迷你播放器的高度，避免最后一个模块被悬浮按钮遮挡
                    bottom = Dimens.miniPlayerSize + Dimens.middleGap
                ),
                verticalArrangement = Arrangement.spacedBy(Dimens.middleGap)
            ) {
                // 歌手 / 分类歌曲 / 排行榜 / 经典老歌
                item(key = "quick_entry") {
                    QuickEntryCard(
                        onItemClick = { feature ->
                            if (feature == "歌手") {
                                navController.navigate(Screens.MusicAuthorCategory.route)
                            } else {
                                comingSoon(feature)
                            }
                        }
                    )
                }

                // 分类歌曲模块
                items(visibleCategories, key = { it.id }) { category ->
                    MusicCategoryModule(
                        category = category,
                        viewModel = viewModel,
                        onMoreClick = {
                            if (category.displayName == "推荐歌手") {
                                navController.navigate(Screens.MusicAuthorCategory.route)
                            } else {
                                comingSoon(category.displayName)
                            }
                        },
                        onMusicClick = { index, _ ->
                            viewModel.playCategorySong(category.id, index)
                            navController.navigate(Screens.MusicPlayer.route)
                        },
                        onAuthorClick = { author ->
                            if (author.authorId <= 0) {
                                comingSoon("歌手详情")
                            } else {
                                navController.navigate(
                                    Screens.MusicAuthor.createRoute(
                                        authorId = author.authorId,
                                        authorName = author.authorName,
                                        avatar = author.avatar
                                    )
                                )
                            }
                        }
                    )
                }

                // 上拉加载更多（滚动到底部时自动追加下一个分类，与 Flutter 端 EasyRefresh 行为一致）
                item(key = "load_more") {
                    LoadMoreFooter(
                        hasMore = viewModel.hasMoreCategories(),
                        onClick = { viewModel.loadMoreCategories() }
                    )
                }
            }
        }
    }
}

/**
 * 顶部搜索栏：用户头像 + 推荐关键字搜索框 + AI 入口
 */
@Composable
private fun MusicSearchBar(
    avatar: String?,
    keyword: String,
    onSearchClick: () -> Unit,
    onAiClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            // 左右外边距（白色区域不贴屏幕边缘，与页面其他卡片对齐）
            .padding(horizontal = Dimens.middleGap)
            .background(Color.White)
            // 内边距
            .padding(Dimens.middleGap),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Avatar(
            avatarUrl = avatar,
            size = AvatarSize.MIDDLE
        )

        // 搜索框
        Box(
            modifier = Modifier
                .weight(1f)
                .padding(start = Dimens.middleGap)
                .height(Dimens.inputHeight)
                .background(
                    color = Color.PageBackground,
                    shape = RoundedCornerShape(Dimens.inputHeight / 2)
                )
                .clickable { onSearchClick() }
                .padding(horizontal = Dimens.middleGap),
            contentAlignment = Alignment.CenterStart
        ) {
            Text(
                text = keyword.ifBlank { "搜索歌曲、歌手" },
                color = Color.Secondary,
                fontSize = Dimens.normalFontSize,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.width(Dimens.middleGap))

        // AI 聊天入口
        Image(
            painter = painterResource(R.drawable.icon_ai),
            contentDescription = "AI 聊天",
            modifier = Modifier
                .size(Dimens.middleIconSize)
                .clickable { onAiClick() }
        )
    }
}

/**
 * 四个快捷入口：歌手 / 分类歌曲 / 排行榜 / 经典老歌
 */
@Composable
private fun QuickEntryCard(onItemClick: (String) -> Unit) {
    HomeCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            QuickEntryItem(
                iconRes = R.drawable.icon_music_singer,
                title = "歌手",
                modifier = Modifier.weight(1f),
                onClick = { onItemClick("歌手") }
            )
            QuickEntryItem(
                iconRes = R.drawable.icon_music_classify,
                title = "分类歌曲",
                modifier = Modifier.weight(1f),
                onClick = { onItemClick("分类歌曲") }
            )
            QuickEntryItem(
                iconRes = R.drawable.icon_music_rank,
                title = "排行榜",
                modifier = Modifier.weight(1f),
                onClick = { onItemClick("排行榜") }
            )
            QuickEntryItem(
                iconRes = R.drawable.icon_music_classics,
                title = "经典老歌",
                modifier = Modifier.weight(1f),
                onClick = { onItemClick("经典老歌") }
            )
        }
    }
}

@Composable
private fun QuickEntryItem(
    iconRes: Int,
    title: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier.clickable { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(iconRes),
            contentDescription = title,
            modifier = Modifier.size(Dimens.bigAvatar)
        )
        Spacer(modifier = Modifier.height(Dimens.smallGap))
        Text(
            text = title,
            color = Color.Black,
            fontSize = Dimens.normalFontSize
        )
    }
}

/**
 * 分类模块：标题（分类名 + 更多）+ 歌曲列表 / 推荐歌手
 */
@Composable
private fun MusicCategoryModule(
    category: MusicCategory,
    viewModel: MusicIndexViewModel,
    onMoreClick: () -> Unit,
    onMusicClick: (Int, Music) -> Unit,
    onAuthorClick: (MusicAuthor) -> Unit
) {
    val songsMap by viewModel.songsByCategory.collectAsState()
    val authors by viewModel.authors.collectAsState()
    val emptyCategories by viewModel.emptyCategories.collectAsState()

    LaunchedEffect(category.id) {
        viewModel.ensureCategoryLoaded(category)
    }

    val isRecommendAuthor = category.displayName == "推荐歌手"
    val songs = songsMap[category.id]

    HomeCard {
        // 标题行
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(R.drawable.icon_down),
                contentDescription = null,
                modifier = Modifier.size(Dimens.smallIconSize),
                colorFilter = ColorFilter.tint(Color.Secondary)
            )
            Spacer(modifier = Modifier.width(Dimens.smallGap))
            Text(
                text = category.displayName,
                color = Color.Black,
                fontSize = Dimens.normalFontSize
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "更多",
                color = Color.Secondary,
                fontSize = Dimens.normalFontSize,
                textDecoration = TextDecoration.Underline,
                modifier = Modifier.clickable { onMoreClick() }
            )
        }

        when {
            // 推荐歌手模块
            isRecommendAuthor -> {
                if (authors.isEmpty()) {
                    LoadingRow()
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = Dimens.middleGap)
                    ) {
                        authors.forEach { author ->
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { onAuthorClick(author) },
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                MusicCover(
                                    coverUrl = author.avatar,
                                    size = Dimens.middleAvatar,
                                    name = author.authorName?.take(1) ?: ""
                                )
                                Spacer(modifier = Modifier.height(Dimens.smallGap))
                                Text(
                                    text = author.authorName ?: "",
                                    color = Color.Black,
                                    fontSize = Dimens.normalFontSize,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }

            // 歌曲列表
            songs == null -> LoadingRow()
            songs.isEmpty() || emptyCategories.contains(category.id) -> {
                Text(
                    text = "暂无歌曲",
                    color = Color.Secondary,
                    fontSize = Dimens.normalFontSize,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = Dimens.middleGap),
                    textAlign = TextAlign.Center
                )
            }
            else -> {
                Column(modifier = Modifier.fillMaxWidth()) {
                    songs.forEachIndexed { index, music ->
                        MusicRow(music = music, onClick = { onMusicClick(index, music) })
                    }
                }
            }
        }
    }
}

/**
 * 单行歌曲：封面 + 歌名 + 歌手/专辑 + 播放/点赞/菜单
 */
@Composable
private fun MusicRow(music: Music, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = Dimens.middleGap),
        verticalAlignment = Alignment.CenterVertically
    ) {
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
            music.authorName?.let {
                Text(
                    text = it,
                    color = Color.Secondary,
                    fontSize = Dimens.normalFontSize,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Image(
            painter = painterResource(R.drawable.icon_music_play),
            contentDescription = "播放",
            modifier = Modifier
                .size(Dimens.listIconSize)
                .clickable { onClick() }
        )

        Spacer(modifier = Modifier.width(Dimens.middleGap))

        Image(
            painter = painterResource(
                if (music.isLike == 1) R.drawable.icon_like_active else R.drawable.icon_like
            ),
            contentDescription = "点赞",
            modifier = Modifier.size(Dimens.listIconSize)
        )

        Spacer(modifier = Modifier.width(Dimens.middleGap))

        Image(
            painter = painterResource(R.drawable.icon_music_menu),
            contentDescription = "更多操作",
            modifier = Modifier.size(Dimens.listIconSize)
        )
    }
}

/**
 * 模块内的加载占位
 */
@Composable
private fun LoadingRow() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Dimens.middleGap),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(Dimens.middleIconSize),
            color = Color.Primary,
            strokeWidth = 2.dp
        )
    }
}

/**
 * 列表底部：上拉加载更多 / 已经到底了
 */
@Composable
private fun LoadMoreFooter(hasMore: Boolean, onClick: () -> Unit) {
    // 底部条目进入可见区域即自动加载下一个分类（相当于 Flutter 的上拉加载）
    LaunchedEffect(hasMore) {
        if (hasMore) onClick()
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = hasMore) { onClick() }
            .padding(vertical = Dimens.middleGap),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = if (hasMore) "上拉加载更多" else "已经到底了",
            color = Color.Secondary,
            fontSize = Dimens.normalFontSize
        )
    }
}

/**
 * 首页统一卡片样式（白底 + 圆角 + 内边距）
 */
@Composable
private fun HomeCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Dimens.moduleBorderRadius),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimens.middleGap),
            content = content
        )
    }
}
