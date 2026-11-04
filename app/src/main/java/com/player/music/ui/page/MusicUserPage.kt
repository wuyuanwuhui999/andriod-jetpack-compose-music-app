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
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SwipeToDismiss
import androidx.compose.material3.DismissValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDismissState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.player.music.R
import com.player.music.model.FavoriteDirectory
import com.player.music.model.Music
import com.player.music.model.MusicAuthor
import com.player.music.model.User
import com.player.music.navigation.Screens
import com.player.music.ui.components.Avatar
import com.player.music.ui.components.AvatarSize
import com.player.music.ui.components.CustomAlertDialog
import com.player.music.ui.components.InputDialog
import com.player.music.ui.components.MusicCover
import com.player.music.ui.theme.Color
import com.player.music.ui.theme.Dimens
import com.player.music.viewmodel.MusicUserViewModel

/**
 * 我的（我的 Tab）
 * 对应 Flutter 端 lib/pages/MusicUserPage.dart：
 * 用户信息、我的收藏夹、我喜欢的歌曲、我关注的歌手、我最近播放的歌曲
 * 每个模块可折叠、可刷新，列表项支持左滑删除
 */
@Composable
fun MusicUserPage(
    navController: NavHostController,
    currentUser: User?,
    listState: LazyListState,
    viewModel: MusicUserViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val directories by viewModel.directories.collectAsState()
    val likedSongs by viewModel.likedSongs.collectAsState()
    val likedTotal by viewModel.likedTotal.collectAsState()
    val authors by viewModel.authors.collectAsState()
    val authorTotal by viewModel.authorTotal.collectAsState()
    val records by viewModel.records.collectAsState()
    val recordTotal by viewModel.recordTotal.collectAsState()

    val foldDirectory by viewModel.foldDirectory.collectAsState()
    val foldLiked by viewModel.foldLiked.collectAsState()
    val foldAuthor by viewModel.foldAuthor.collectAsState()
    val foldRecord by viewModel.foldRecord.collectAsState()

    val isInitialLoading by viewModel.isInitialLoading.collectAsState()
    val toastMessage by viewModel.toastMessage.collectAsState()

    var showCreateDirectory by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<PendingDelete?>(null) }

    LaunchedEffect(toastMessage) {
        toastMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearToast()
        }
    }

    fun comingSoon(feature: String) {
        Toast.makeText(context, "${feature}功能开发中，敬请期待", Toast.LENGTH_SHORT).show()
    }

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .background(Color.PageBackground),
        contentPadding = PaddingValues(
            start = Dimens.middleGap,
            end = Dimens.middleGap,
            top = Dimens.middleGap,
            // 底部预留迷你播放器高度，避免被悬浮按钮遮挡
            bottom = Dimens.miniPlayerSize + Dimens.middleGap
        ),
        verticalArrangement = Arrangement.spacedBy(Dimens.middleGap)
    ) {
        // 用户信息
        item(key = "user_info") {
            UserInfoCard(
                user = currentUser,
                onEditClick = { navController.navigate(Screens.User.route) }
            )
        }

        // 我的收藏夹
        item(key = "favorite_directory") {
            SectionCard {
                SectionHeader(
                    title = "我的收藏夹",
                    folded = foldDirectory,
                    onFoldClick = { viewModel.toggleFoldDirectory() }
                ) {
                    Image(
                        painter = painterResource(R.drawable.icon_add),
                        contentDescription = "新建收藏夹",
                        colorFilter = ColorFilter.tint(Color.Secondary),
                        modifier = Modifier
                            .size(Dimens.listIconSize)
                            .clickable { showCreateDirectory = true }
                    )
                    Spacer(modifier = Modifier.width(Dimens.middleGap))
                    Image(
                        painter = painterResource(R.drawable.icon_refresh),
                        contentDescription = "刷新",
                        colorFilter = ColorFilter.tint(Color.Secondary),
                        modifier = Modifier
                            .size(Dimens.listIconSize)
                            .clickable { viewModel.refreshDirectories() }
                    )
                }

                if (!foldDirectory) {
                    if (isInitialLoading && directories.isEmpty()) {
                        SectionLoading()
                    } else if (directories.isEmpty()) {
                        SectionEmpty()
                    } else {
                        directories.forEachIndexed { index, directory ->
                            SwipeDeleteRow(
                                onDelete = { pendingDelete = PendingDelete.Directory(directory) }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = Dimens.middleGap),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    MusicCover(
                                        coverUrl = directory.cover,
                                        size = Dimens.middleAvatar,
                                        name = directory.name.take(1)
                                    )
                                    Spacer(modifier = Modifier.width(Dimens.middleGap))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = directory.name,
                                            color = Color.Black,
                                            fontSize = Dimens.normalFontSize,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.height(Dimens.smallGap))
                                        Text(
                                            text = "${directory.total}首",
                                            color = Color.Secondary,
                                            fontSize = Dimens.normalFontSize
                                        )
                                    }
                                    Image(
                                        painter = painterResource(R.drawable.icon_music_play),
                                        contentDescription = "播放",
                                        modifier = Modifier
                                            .size(Dimens.listIconSize)
                                            .clickable { comingSoon("收藏夹播放") }
                                    )
                                    Spacer(modifier = Modifier.width(Dimens.middleGap * 2))
                                    Image(
                                        painter = painterResource(R.drawable.icon_music_menu),
                                        contentDescription = "更多操作",
                                        modifier = Modifier.size(Dimens.listIconSize)
                                    )
                                }
                            }
                            if (index < directories.size - 1) {
                                Divider(
                                    color = Color.Gray.copy(alpha = 0.4f),
                                    thickness = Dimens.borderSize
                                )
                            }
                        }
                    }
                }
            }
        }

        // 我喜欢的歌曲
        item(key = "liked_music") {
            SectionCard {
                SectionHeader(
                    title = "我喜欢的歌曲",
                    folded = foldLiked,
                    onFoldClick = { viewModel.toggleFoldLiked() }
                ) {
                    Image(
                        painter = painterResource(R.drawable.icon_refresh),
                        contentDescription = "刷新",
                        colorFilter = ColorFilter.tint(Color.Secondary),
                        modifier = Modifier
                            .size(Dimens.listIconSize)
                            .clickable { viewModel.refreshLiked() }
                    )
                    if (likedTotal > LIKE_MORE_THRESHOLD) {
                        Spacer(modifier = Modifier.width(Dimens.middleGap))
                        MoreText { comingSoon("喜欢的歌曲列表") }
                    }
                }

                if (!foldLiked) {
                    if (isInitialLoading && likedSongs.isEmpty()) {
                        SectionLoading()
                    } else if (likedSongs.isEmpty()) {
                        SectionEmpty()
                    } else {
                        likedSongs.forEachIndexed { index, music ->
                            SwipeDeleteRow(
                                onDelete = { pendingDelete = PendingDelete.LikedSong(music) }
                            ) {
                                MusicRowContent(
                                    coverUrl = music.cover,
                                    title = music.songName,
                                    subtitle = music.authorName ?: "",
                                    onPlayClick = { comingSoon("播放") }
                                )
                            }
                            if (index < likedSongs.size - 1) {
                                Divider(
                                    color = Color.Gray.copy(alpha = 0.4f),
                                    thickness = Dimens.borderSize
                                )
                            }
                        }
                    }
                }
            }
        }

        // 我关注的歌手
        item(key = "favorite_author") {
            SectionCard {
                SectionHeader(
                    title = "我关注的歌手",
                    folded = foldAuthor,
                    onFoldClick = { viewModel.toggleFoldAuthor() }
                ) {
                    Image(
                        painter = painterResource(R.drawable.icon_refresh),
                        contentDescription = "刷新",
                        colorFilter = ColorFilter.tint(Color.Secondary),
                        modifier = Modifier
                            .size(Dimens.listIconSize)
                            .clickable { viewModel.refreshAuthors() }
                    )
                    if (authorTotal > AUTHOR_MORE_THRESHOLD) {
                        Spacer(modifier = Modifier.width(Dimens.middleGap))
                        MoreText { comingSoon("关注的歌手列表") }
                    }
                }

                if (!foldAuthor) {
                    if (isInitialLoading && authors.isEmpty()) {
                        SectionLoading()
                    } else if (authors.isEmpty()) {
                        SectionEmpty()
                    } else {
                        authors.forEachIndexed { index, author ->
                            SwipeDeleteRow(
                                onDelete = { pendingDelete = PendingDelete.Author(author) }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
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
                                        // 关注的歌手接口不带歌曲数时（total=0）不展示"0首"
                                        if (author.total > 0) {
                                            Spacer(modifier = Modifier.height(Dimens.smallGap))
                                            Text(
                                                text = "${author.total}首",
                                                color = Color.Secondary,
                                                fontSize = Dimens.normalFontSize
                                            )
                                        }
                                    }
                                    Image(
                                        painter = painterResource(R.drawable.icon_music_play),
                                        contentDescription = "播放",
                                        modifier = Modifier
                                            .size(Dimens.listIconSize)
                                            .clickable { comingSoon("歌手歌曲播放") }
                                    )
                                    Spacer(modifier = Modifier.width(Dimens.middleGap * 2))
                                    Image(
                                        painter = painterResource(R.drawable.icon_music_menu),
                                        contentDescription = "更多操作",
                                        modifier = Modifier.size(Dimens.listIconSize)
                                    )
                                }
                            }
                            if (index < authors.size - 1) {
                                Divider(
                                    color = Color.Gray.copy(alpha = 0.4f),
                                    thickness = Dimens.borderSize
                                )
                            }
                        }
                    }
                }
            }
        }

        // 最近播放的歌曲
        item(key = "play_record") {
            SectionCard {
                SectionHeader(
                    title = "我最近播放的歌曲",
                    folded = foldRecord,
                    onFoldClick = { viewModel.toggleFoldRecord() }
                ) {
                    Image(
                        painter = painterResource(R.drawable.icon_refresh),
                        contentDescription = "刷新",
                        colorFilter = ColorFilter.tint(Color.Secondary),
                        modifier = Modifier
                            .size(Dimens.listIconSize)
                            .clickable { viewModel.refreshRecords() }
                    )
                    if (recordTotal > RECORD_MORE_THRESHOLD) {
                        Spacer(modifier = Modifier.width(Dimens.middleGap))
                        MoreText { comingSoon("播放记录列表") }
                    }
                }

                if (!foldRecord) {
                    if (isInitialLoading && records.isEmpty()) {
                        SectionLoading()
                    } else if (records.isEmpty()) {
                        SectionEmpty()
                    } else {
                        records.forEachIndexed { index, music ->
                            if (index > 0) {
                                Divider(
                                    color = Color.Gray.copy(alpha = 0.4f),
                                    thickness = Dimens.borderSize
                                )
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = Dimens.middleGap),
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
                                    Spacer(modifier = Modifier.height(Dimens.smallGap))
                                    Text(
                                        text = "听过${music.times}次",
                                        color = Color.Secondary,
                                        fontSize = Dimens.normalFontSize
                                    )
                                }
                                Image(
                                    painter = painterResource(R.drawable.icon_music_play),
                                    contentDescription = "播放",
                                    modifier = Modifier
                                        .size(Dimens.listIconSize)
                                        .clickable { comingSoon("播放") }
                                )
                                Spacer(modifier = Modifier.width(Dimens.middleGap * 2))
                                Image(
                                    painter = painterResource(R.drawable.icon_delete),
                                    contentDescription = "删除记录",
                                    modifier = Modifier
                                        .size(Dimens.listIconSize)
                                        .clickable { comingSoon("删除播放记录") }
                                )
                                Spacer(modifier = Modifier.width(Dimens.middleGap * 2))
                                Image(
                                    painter = painterResource(R.drawable.icon_music_menu),
                                    contentDescription = "更多操作",
                                    modifier = Modifier.size(Dimens.listIconSize)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // 新建收藏夹
    if (showCreateDirectory) {
        InputDialog(
            title = "新建收藏夹",
            initialValue = "",
            hint = "请输入收藏夹名称",
            isRequired = true,
            onConfirm = { name ->
                viewModel.createDirectory(name)
                showCreateDirectory = false
            },
            onDismiss = { showCreateDirectory = false }
        )
    }

    // 删除确认
    pendingDelete?.let { target ->
        val message = when (target) {
            is PendingDelete.Directory -> "确定删除收藏夹「${target.directory.name}」？"
            is PendingDelete.LikedSong -> "确定删除《${target.music.songName ?: ""}》？"
            is PendingDelete.Author -> "确定取消关注「${target.author.authorName ?: ""}」？"
        }
        CustomAlertDialog(
            title = "提示",
            onConfirm = {
                when (target) {
                    is PendingDelete.Directory -> viewModel.deleteDirectory(target.directory)
                    is PendingDelete.LikedSong -> viewModel.deleteLikedSong(target.music)
                    is PendingDelete.Author -> viewModel.deleteAuthor(target.author)
                }
                pendingDelete = null
            },
            onDismiss = { pendingDelete = null }
        ) {
            Text(
                text = message,
                color = Color.Black,
                fontSize = Dimens.normalFontSize
            )
        }
    }
}

/** 各模块"更多"阈值（与 Flutter 端一致） */
private const val LIKE_MORE_THRESHOLD = 5
private const val AUTHOR_MORE_THRESHOLD = 5
private const val RECORD_MORE_THRESHOLD = 10

/**
 * 待删除目标
 */
private sealed class PendingDelete {
    data class Directory(val directory: FavoriteDirectory) : PendingDelete()
    data class LikedSong(val music: Music) : PendingDelete()
    data class Author(val author: MusicAuthor) : PendingDelete()
}

/**
 * 用户信息卡片：头像 + 昵称 + 个性签名 + 编辑入口
 */
@Composable
private fun UserInfoCard(user: User?, onEditClick: () -> Unit) {
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
            Avatar(avatarUrl = user?.avatar, size = AvatarSize.BIG)

            Spacer(modifier = Modifier.width(Dimens.middleGap))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = user?.username ?: user?.userAccount ?: "未登录",
                    color = Color.Black,
                    fontSize = Dimens.bigFontSize,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(Dimens.smallGap))
                Text(
                    text = user?.sign ?: "",
                    color = Color.Secondary,
                    fontSize = Dimens.normalFontSize,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Image(
                painter = painterResource(R.drawable.icon_edit),
                contentDescription = "编辑资料",
                modifier = Modifier
                    .size(Dimens.middleIconSize)
                    .clickable { onEditClick() }
            )
        }
    }
}

/**
 * 模块卡片容器
 */
@Composable
private fun SectionCard(content: @Composable ColumnScope.() -> Unit) {
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

/**
 * 模块标题：折叠箭头 + 标题 + 右侧操作区
 */
@Composable
private fun SectionHeader(
    title: String,
    folded: Boolean,
    onFoldClick: () -> Unit,
    actions: @Composable RowScope.() -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(R.drawable.icon_down),
            contentDescription = if (folded) "展开" else "折叠",
            colorFilter = ColorFilter.tint(Color.Secondary),
            modifier = Modifier
                .size(Dimens.smallIconSize)
                .rotate(if (folded) -90f else 0f)
                .clickable { onFoldClick() }
        )
        Spacer(modifier = Modifier.width(Dimens.smallGap))
        Text(
            text = title,
            color = Color.Black,
            fontSize = Dimens.normalFontSize
        )
        Spacer(modifier = Modifier.weight(1f))
        actions()
    }
}

/**
 * "更多"文字
 */
@Composable
private fun MoreText(onClick: () -> Unit) {
    Text(
        text = "更多",
        color = Color.Secondary,
        fontSize = Dimens.normalFontSize,
        modifier = Modifier.clickable { onClick() }
    )
}

/**
 * 歌曲行（封面 + 歌名 + 副标题 + 播放/菜单）
 */
@Composable
private fun MusicRowContent(
    coverUrl: String?,
    title: String?,
    subtitle: String,
    onPlayClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Dimens.middleGap),
        verticalAlignment = Alignment.CenterVertically
    ) {
        MusicCover(coverUrl = coverUrl, size = Dimens.middleAvatar)
        Spacer(modifier = Modifier.width(Dimens.middleGap))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title ?: "",
                color = Color.Black,
                fontSize = Dimens.normalFontSize,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(Dimens.smallGap))
            Text(
                text = subtitle,
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
        Spacer(modifier = Modifier.width(Dimens.middleGap * 2))
        Image(
            painter = painterResource(R.drawable.icon_music_menu),
            contentDescription = "更多操作",
            modifier = Modifier.size(Dimens.listIconSize)
        )
    }
}

/**
 * 左滑删除容器：滑动到左侧触发删除回调（再弹确认框）
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeDeleteRow(
    onDelete: () -> Unit,
    content: @Composable RowScope.() -> Unit
) {
    val dismissState = rememberDismissState()

    // 滑动到左侧后弹确认框，并把列表项复位（删除动作在确认框里执行）
    LaunchedEffect(dismissState.currentValue) {
        if (dismissState.currentValue == DismissValue.DismissedToStart) {
            onDelete()
            dismissState.reset()
        }
    }

    SwipeToDismiss(
        state = dismissState,
        background = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Red, RoundedCornerShape(Dimens.btnBorderRadius)),
                contentAlignment = Alignment.CenterEnd
            ) {
                Row(
                    modifier = Modifier.padding(end = Dimens.middleGap),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        painter = painterResource(R.drawable.icon_delete),
                        contentDescription = "删除",
                        colorFilter = ColorFilter.tint(Color.White),
                        modifier = Modifier.size(Dimens.listIconSize)
                    )
                    Spacer(modifier = Modifier.width(Dimens.smallGap))
                    Text(
                        text = "删除",
                        color = Color.White,
                        fontSize = Dimens.normalFontSize
                    )
                }
            }
        },
        // 内容区给白底，滑动时才露出下面的红色删除背景
        dismissContent = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White),
                content = content
            )
        }
    )
}

/**
 * 模块加载中
 */
@Composable
private fun SectionLoading() {
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
 * 模块空数据
 */
@Composable
private fun SectionEmpty() {
    Text(
        text = "暂无数据",
        color = Color.Secondary,
        fontSize = Dimens.normalFontSize,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Dimens.middleGap)
    )
}
