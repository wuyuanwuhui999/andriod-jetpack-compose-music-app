package com.player.music.ui.page

import android.widget.Toast
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.player.music.R
import com.player.music.model.Comment
import com.player.music.model.FavoriteDirectory
import com.player.music.model.LoopMode
import com.player.music.model.Music
import com.player.music.navigation.Screens
import com.player.music.ui.components.Avatar
import com.player.music.ui.components.AvatarSize
import com.player.music.ui.components.InputDialog
import com.player.music.ui.theme.Color
import com.player.music.ui.theme.Dimens
import com.player.music.utils.CommonUtils
import com.player.music.utils.LyricLine
import com.player.music.utils.LyricParser
import com.player.music.viewmodel.MusicPlayerViewModel
import kotlinx.coroutines.launch

/**
 * 音乐播放页
 * 对应 Flutter 端 lib/pages/MusicPlayerPage.dart：
 * 模糊封面背景 + 旋转唱片 + 歌词滚动 + 喜欢/分享/评论/收藏夹 + 进度条 + 上一首/播放暂停/下一首/循环模式
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MusicPlayerPage(
    navController: NavHostController,
    viewModel: MusicPlayerViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val currentMusic by viewModel.currentMusic.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val positionMs by viewModel.positionMs.collectAsState()
    val durationMs by viewModel.durationMs.collectAsState()
    val loopMode by viewModel.loopMode.collectAsState()
    val isFavorite by viewModel.isFavorite.collectAsState()
    val comments by viewModel.comments.collectAsState()
    val commentTotal by viewModel.commentTotal.collectAsState()
    val favoriteDirectories by viewModel.favoriteDirectories.collectAsState()
    val toastMessage by viewModel.toastMessage.collectAsState()
    val playlist by viewModel.playlist.collectAsState()

    var showCommentSheet by remember { mutableStateOf(false) }
    var showFavoriteSheet by remember { mutableStateOf(false) }
    var showPlaylistSheet by remember { mutableStateOf(false) }
    var showCreateDirectory by remember { mutableStateOf(false) }
    var showLoopMenu by remember { mutableStateOf(false) }
    var isDragging by remember { mutableStateOf(false) }
    var dragValue by remember { mutableStateOf(0f) }

    LaunchedEffect(toastMessage) {
        toastMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearToast()
        }
    }

    val music = currentMusic
    val lyrics = remember(music?.id, music?.lyrics) { LyricParser.parse(music?.lyrics) }
    val currentLyricIndex = remember(lyrics, positionMs) {
        LyricParser.currentIndex(lyrics, positionMs)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // 背景：模糊封面
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
        // 压暗，保证白色文字可读
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.45f))
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = Dimens.middleGap * 2, vertical = Dimens.middleGap),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(Dimens.middleGap * 2))

            // 歌名
            Text(
                text = music?.songName ?: "暂无播放歌曲",
                color = Color.White,
                fontSize = Dimens.bigFontSize,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(Dimens.middleGap))

            // 旋转唱片
            RotatingDisc(coverUrl = coverUrl, isPlaying = isPlaying)

            Spacer(modifier = Modifier.height(Dimens.middleGap))

            // 歌词
            LyricView(
                lyrics = lyrics,
                currentIndex = currentLyricIndex,
                onLineClick = { line ->
                    viewModel.seekTo(line.timeMs)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            )

            // 歌手（点击进入歌手详情页）
            Text(
                text = music?.authorName ?: "",
                color = Color.White,
                fontSize = Dimens.normalFontSize,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textDecoration = TextDecoration.Underline,
                modifier = Modifier.clickable {
                    val authorId = music?.authorId?.toLongOrNull() ?: 0L
                    if (authorId <= 0L) {
                        Toast.makeText(context, "暂无歌手信息", Toast.LENGTH_SHORT).show()
                    } else {
                        navController.navigate(
                            Screens.MusicAuthor.createRoute(
                                authorId = authorId,
                                authorName = music?.authorName,
                                avatar = null
                            )
                        )
                    }
                }
            )

            Spacer(modifier = Modifier.height(Dimens.middleGap))

            // 喜欢 / 分享 / 评论 / 收藏夹
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                PlayerAction(
                    iconRes = if ((music?.isLike ?: 0) == 0) R.drawable.icon_music_collect else R.drawable.icon_collection_active,
                    description = "喜欢",
                    modifier = Modifier.weight(1f)
                ) { viewModel.toggleLike() }

                PlayerAction(
                    iconRes = R.drawable.icon_share_music,
                    description = "分享",
                    modifier = Modifier.weight(1f)
                ) { navController.navigate(Screens.MusicShare.route) }

                PlayerAction(
                    iconRes = R.drawable.icon_music_comments,
                    description = "评论",
                    modifier = Modifier.weight(1f)
                ) {
                    viewModel.loadComments()
                    showCommentSheet = true
                }

                PlayerAction(
                    iconRes = if (isFavorite) R.drawable.icon_full_star else R.drawable.icon_favorite,
                    description = "收藏夹",
                    modifier = Modifier.weight(1f)
                ) {
                    viewModel.loadFavoriteDirectories()
                    showFavoriteSheet = true
                }
            }

            Spacer(modifier = Modifier.height(Dimens.middleGap))

            // 进度条
            ProgressBar(
                positionMs = positionMs,
                durationMs = durationMs,
                isDragging = isDragging,
                dragValue = dragValue,
                onDragStart = { isDragging = true; dragValue = it },
                onDrag = { dragValue = it },
                onDragFinished = {
                    isDragging = false
                    viewModel.seekTo((dragValue * durationMs).toInt())
                }
            )

            Spacer(modifier = Modifier.height(Dimens.middleGap))

            // 播放控制
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 循环模式
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Image(
                        painter = painterResource(
                            when (loopMode) {
                                LoopMode.ORDER -> R.drawable.icon_music_order
                                LoopMode.REPEAT -> R.drawable.icon_music_loop
                                LoopMode.RANDOM -> R.drawable.icon_music_random
                            }
                        ),
                        contentDescription = "循环模式",
                        modifier = Modifier
                            .size(Dimens.middleIconSize)
                            .clickable { showLoopMenu = true }
                    )
                    DropdownMenu(
                        expanded = showLoopMenu,
                        onDismissRequest = { showLoopMenu = false },
                        modifier = Modifier.background(Color.SubTitle)
                    ) {
                        LoopMenuItem(
                            iconRes = R.drawable.icon_music_order,
                            text = "顺序播放",
                            selected = loopMode == LoopMode.ORDER
                        ) {
                            viewModel.setLoopMode(LoopMode.ORDER)
                            showLoopMenu = false
                        }
                        LoopMenuItem(
                            iconRes = R.drawable.icon_music_loop,
                            text = "单曲循环",
                            selected = loopMode == LoopMode.REPEAT
                        ) {
                            viewModel.setLoopMode(LoopMode.REPEAT)
                            showLoopMenu = false
                        }
                        LoopMenuItem(
                            iconRes = R.drawable.icon_music_random,
                            text = "随机播放",
                            selected = loopMode == LoopMode.RANDOM
                        ) {
                            viewModel.setLoopMode(LoopMode.RANDOM)
                            showLoopMenu = false
                        }
                    }
                }

                // 上一首
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Image(
                        painter = painterResource(R.drawable.icon_music_prev),
                        contentDescription = "上一首",
                        modifier = Modifier
                            .size(Dimens.middleIconSize)
                            .clickable { viewModel.prev() }
                    )
                }

                // 播放 / 暂停
                Box(
                    modifier = Modifier
                        .weight(2f)
                        .aspectRatio(1f, matchHeightConstraintsFirst = true),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(Dimens.bigAvatar)
                            .clip(CircleShape)
                            .border(2.dp, Color.White, CircleShape)
                            .clickable { viewModel.togglePlay() },
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(
                                if (isPlaying) R.drawable.icon_music_playing
                                else R.drawable.icon_music_play_white
                            ),
                            contentDescription = if (isPlaying) "暂停" else "播放",
                            modifier = Modifier.size(Dimens.middleIconSize)
                        )
                    }
                }

                // 下一首
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Image(
                        painter = painterResource(R.drawable.icon_music_next),
                        contentDescription = "下一首",
                        modifier = Modifier
                            .size(Dimens.middleIconSize)
                            .clickable { viewModel.next() }
                    )
                }

                // 播放列表
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Image(
                        painter = painterResource(R.drawable.icon_music_play_menu),
                        contentDescription = "播放列表",
                        modifier = Modifier
                            .size(Dimens.middleIconSize)
                            .clickable { showPlaylistSheet = true }
                    )
                }
            }

            Spacer(modifier = Modifier.height(Dimens.middleGap))
        }

        // 返回
        Image(
            painter = painterResource(R.drawable.icon_back),
            contentDescription = "返回",
            colorFilter = ColorFilter.tint(Color.White.copy(alpha = 0.7f)),
            modifier = Modifier
                .statusBarsPadding()
                .padding(Dimens.middleGap)
                .size(Dimens.middleIconSize)
                .clickable { navController.popBackStack() }
        )
    }

    // 评论弹层
    if (showCommentSheet) {
        CommentSheet(
            comments = comments,
            total = commentTotal,
            onDismiss = { showCommentSheet = false },
            onSend = { content -> viewModel.sendComment(content) }
        )
    }

    // 收藏夹弹层
    if (showFavoriteSheet) {
        FavoriteSheet(
            directories = favoriteDirectories,
            onDismiss = { showFavoriteSheet = false },
            onConfirm = { ids ->
                viewModel.saveFavoriteDirectories(ids) { showFavoriteSheet = false }
            },
            onCreateDirectory = { showCreateDirectory = true }
        )
    }

    // 新建收藏夹弹窗
    if (showCreateDirectory) {
        InputDialog(
            title = "新建收藏夹",
            hint = "收藏夹名称",
            isRequired = true,
            onConfirm = { name ->
                viewModel.createFavoriteDirectory(name)
                showCreateDirectory = false
            },
            onDismiss = { showCreateDirectory = false }
        )
    }

    // 播放列表弹层
    if (showPlaylistSheet) {
        PlaylistSheet(
            playlist = playlist,
            currentMusicId = music?.id,
            onDismiss = { showPlaylistSheet = false },
            onSelect = { index ->
                viewModel.playAt(index)
                showPlaylistSheet = false
            },
            onRemove = { index -> viewModel.removeFromPlaylist(index) },
            onClear = {
                viewModel.clearPlaylist()
                showPlaylistSheet = false
            }
        )
    }
}

/**
 * 旋转唱片
 */
@Composable
private fun RotatingDisc(coverUrl: String?, isPlaying: Boolean) {
    val angle = remember { Animatable(0f) }

    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            while (true) {
                angle.animateTo(
                    targetValue = angle.value + 360f,
                    animationSpec = tween(durationMillis = 10000, easing = LinearEasing)
                )
                angle.snapTo(angle.value - 360f)
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth(0.78f)
            .aspectRatio(1f)
            .clip(CircleShape)
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        androidx.compose.ui.graphics.Color.Black,
                        androidx.compose.ui.graphics.Color(0xFF363938),
                        androidx.compose.ui.graphics.Color(0xFF363938),
                        androidx.compose.ui.graphics.Color.Black
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .rotate(angle.value)
                .padding(Dimens.middleGap * 4)
                .clip(CircleShape)
        ) {
            if (coverUrl != null) {
                AsyncImage(
                    model = coverUrl,
                    contentDescription = "唱片封面",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Image(
                    painter = painterResource(R.drawable.default_cover),
                    contentDescription = "默认封面",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
        }
    }
}

/**
 * 歌词列表：当前行高亮，自动滚动
 */
@Composable
private fun LyricView(
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

    val listState = rememberLazyListState()

    LaunchedEffect(currentIndex) {
        if (currentIndex >= 0 && currentIndex < lyrics.size) {
            val target = (currentIndex - 2).coerceAtLeast(0)
            listState.animateScrollToItem(target)
        }
    }

    LazyColumn(
        state = listState,
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Dimens.smallGap)
    ) {
        itemsIndexed(lyrics) { index, line ->
            val isCurrent = index == currentIndex
            Text(
                text = line.text,
                color = if (isCurrent) Color.White else Color.White.copy(alpha = 0.55f),
                fontSize = if (isCurrent) Dimens.bigFontSize else Dimens.normalFontSize,
                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = Dimens.smallGap)
                    .clickable { onLineClick(line) }
            )
        }
    }
}

/**
 * 进度条 + 时间
 */
@Composable
private fun ProgressBar(
    positionMs: Int,
    durationMs: Int,
    isDragging: Boolean,
    dragValue: Float,
    onDragStart: (Float) -> Unit,
    onDrag: (Float) -> Unit,
    onDragFinished: () -> Unit
) {
    val fraction = if (isDragging) {
        dragValue
    } else if (durationMs > 0) {
        (positionMs.toFloat() / durationMs).coerceIn(0f, 1f)
    } else {
        0f
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = CommonUtils.formatDuration(if (isDragging) (dragValue * durationMs).toInt() else positionMs),
            color = Color.White,
            fontSize = Dimens.normalFontSize
        )
        Slider(
            value = fraction,
            onValueChange = { value ->
                if (!isDragging) onDragStart(value) else onDrag(value)
            },
            onValueChangeFinished = { onDragFinished() },
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = Dimens.middleGap),
            colors = SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = Color.White.copy(alpha = 0.9f),
                inactiveTrackColor = Color.White.copy(alpha = 0.3f)
            )
        )
        Text(
            text = CommonUtils.formatDuration(durationMs),
            color = Color.White,
            fontSize = Dimens.normalFontSize
        )
    }
}

/**
 * 播放操作图标（喜欢/分享/评论/收藏夹）
 */
@Composable
private fun PlayerAction(
    iconRes: Int,
    description: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Image(
            painter = painterResource(iconRes),
            contentDescription = description,
            modifier = Modifier
                .size(Dimens.middleIconSize)
                .clickable { onClick() }
        )
    }
}

/**
 * 循环模式菜单项
 */
@Composable
private fun LoopMenuItem(
    iconRes: Int,
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    DropdownMenuItem(
        onClick = onClick,
        text = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = painterResource(iconRes),
                    contentDescription = null,
                    modifier = Modifier.size(Dimens.smallIconSize)
                )
                Spacer(modifier = Modifier.width(Dimens.smallGap))
                Text(
                    text = text,
                    color = if (selected) Color.Primary else Color.White,
                    fontSize = Dimens.normalFontSize
                )
            }
        }
    )
}

/**
 * 评论底部弹层
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CommentSheet(
    comments: List<Comment>,
    total: Int,
    onDismiss: () -> Unit,
    onSend: (String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var input by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(480.dp)
                .navigationBarsPadding()
        ) {
            Text(
                text = if (total > 0) "评论（$total）" else "评论",
                color = Color.Black,
                fontSize = Dimens.bigFontSize,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = Dimens.middleGap)
            )
            Divider(color = Color.Gray.copy(alpha = 0.4f), thickness = Dimens.borderSize)

            if (comments.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "还没有评论，快来抢沙发吧",
                        color = Color.Secondary,
                        fontSize = Dimens.normalFontSize
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = Dimens.middleGap)
                ) {
                    itemsIndexed(comments) { _, comment ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = Dimens.middleGap)
                        ) {
                            Avatar(avatarUrl = comment.avater, size = AvatarSize.SMALL)
                            Spacer(modifier = Modifier.width(Dimens.middleGap))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = comment.username ?: "",
                                    color = Color.LinkBlue,
                                    fontSize = Dimens.normalFontSize
                                )
                                Spacer(modifier = Modifier.height(Dimens.smallGap))
                                Text(
                                    text = comment.content ?: "",
                                    color = Color.Black,
                                    fontSize = Dimens.normalFontSize
                                )
                                Spacer(modifier = Modifier.height(Dimens.smallGap))
                                Text(
                                    text = CommonUtils.formatRelativeTime(comment.createTime ?: ""),
                                    color = Color.Secondary,
                                    fontSize = Dimens.normalFontSize
                                )
                            }
                        }
                        Divider(color = Color.Gray.copy(alpha = 0.3f), thickness = Dimens.borderSize)
                    }
                }
            }

            // 输入框
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Dimens.middleGap),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(Dimens.middleAvatar)
                        .clip(RoundedCornerShape(Dimens.middleAvatar))
                        .background(Color.PageBackground)
                        .padding(horizontal = Dimens.middleGap),
                    contentAlignment = Alignment.CenterStart
                ) {
                    BasicTextField(
                        value = input,
                        onValueChange = { input = it },
                        singleLine = true,
                        textStyle = androidx.compose.ui.text.TextStyle(
                            color = Color.Black,
                            fontSize = Dimens.normalFontSize
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        decorationBox = { inner ->
                            if (input.isEmpty()) {
                                Text(
                                    text = "说点什么吧",
                                    color = Color.Secondary,
                                    fontSize = Dimens.normalFontSize
                                )
                            }
                            inner()
                        }
                    )
                }
                Spacer(modifier = Modifier.width(Dimens.middleGap))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(Dimens.middleAvatar))
                        .background(Color.Primary)
                        .clickable {
                            onSend(input)
                            input = ""
                        }
                        .padding(horizontal = Dimens.middleGap, vertical = Dimens.smallGap),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "发送",
                        color = Color.White,
                        fontSize = Dimens.normalFontSize
                    )
                }
            }
        }
    }
}

/**
 * 播放列表底部弹层：当前播放高亮，可切歌 / 单首删除 / 清空
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlaylistSheet(
    playlist: List<Music>,
    currentMusicId: Long?,
    onDismiss: () -> Unit,
    onSelect: (Int) -> Unit,
    onRemove: (Int) -> Unit,
    onClear: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 480.dp)
                .navigationBarsPadding()
                .padding(horizontal = Dimens.middleGap)
                .padding(bottom = Dimens.middleGap * 2)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "播放列表（${playlist.size}）",
                    color = Color.Black,
                    fontSize = Dimens.bigFontSize,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .weight(1f)
                        .padding(vertical = Dimens.middleGap)
                )
                if (playlist.isNotEmpty()) {
                    Text(
                        text = "清空",
                        color = Color.Secondary,
                        fontSize = Dimens.normalFontSize,
                        modifier = Modifier
                            .clickable { onClear() }
                            .padding(Dimens.smallGap)
                    )
                }
            }
            Divider(color = Color.Gray.copy(alpha = 0.4f), thickness = Dimens.borderSize)

            if (playlist.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "播放列表暂无歌曲",
                        color = Color.Secondary,
                        fontSize = Dimens.normalFontSize
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp)
                ) {
                    itemsIndexed(playlist) { index, music ->
                        val isCurrent = music.id == currentMusicId
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelect(index) }
                                .padding(vertical = Dimens.middleGap),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Image(
                                painter = painterResource(
                                    if (isCurrent) R.drawable.icon_music_playing
                                    else R.drawable.icon_music_play
                                ),
                                contentDescription = null,
                                colorFilter = ColorFilter.tint(
                                    if (isCurrent) Color.Primary else Color.Secondary
                                ),
                                modifier = Modifier.size(Dimens.listIconSize)
                            )
                            Spacer(modifier = Modifier.width(Dimens.middleGap))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = music.songName ?: "",
                                    color = if (isCurrent) Color.Primary else Color.Black,
                                    fontSize = Dimens.normalFontSize,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = music.authorName ?: "",
                                    color = Color.Secondary,
                                    fontSize = Dimens.normalFontSize,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Image(
                                painter = painterResource(R.drawable.icon_delete),
                                contentDescription = "移除",
                                colorFilter = ColorFilter.tint(Color.Secondary),
                                modifier = Modifier
                                    .size(Dimens.listIconSize)
                                    .clickable { onRemove(index) }
                            )
                        }
                        Divider(color = Color.Gray.copy(alpha = 0.3f), thickness = Dimens.borderSize)
                    }
                }
            }
        }
    }
}

/**
 * 收藏夹底部弹层：勾选收藏夹后保存
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FavoriteSheet(
    directories: List<FavoriteDirectory>,
    onDismiss: () -> Unit,
    onConfirm: (List<Long>) -> Unit,
    onCreateDirectory: (() -> Unit)?
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    var selected by remember(directories) {
        mutableStateOf(directories.filter { it.checked == 1 }.map { it.id }.toSet())
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(420.dp)
                .navigationBarsPadding()
                .padding(horizontal = Dimens.middleGap)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "收藏到",
                    color = Color.Black,
                    fontSize = Dimens.bigFontSize,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .weight(1f)
                        .padding(vertical = Dimens.middleGap)
                )
                // 新建收藏夹
                if (onCreateDirectory != null) {
                    Row(
                        modifier = Modifier.clickable { onCreateDirectory() },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(
                            painter = painterResource(R.drawable.icon_add),
                            contentDescription = "新建收藏夹",
                            colorFilter = ColorFilter.tint(Color.Primary),
                            modifier = Modifier.size(Dimens.smallIconSize)
                        )
                        Spacer(modifier = Modifier.width(Dimens.smallGap))
                        Text(
                            text = "新建",
                            color = Color.Primary,
                            fontSize = Dimens.normalFontSize
                        )
                    }
                }
            }
            Divider(color = Color.Gray.copy(alpha = 0.4f), thickness = Dimens.borderSize)

            if (directories.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "暂无收藏夹",
                        color = Color.Secondary,
                        fontSize = Dimens.normalFontSize
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    itemsIndexed(directories) { _, directory ->
                        val checked = selected.contains(directory.id)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selected = if (checked) {
                                        selected - directory.id
                                    } else {
                                        selected + directory.id
                                    }
                                }
                                .padding(vertical = Dimens.middleGap),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(Dimens.listIconSize)
                                    .clip(CircleShape)
                                    .border(
                                        width = if (checked) 6.dp else 1.dp,
                                        color = if (checked) Color.Primary else Color.Secondary,
                                        shape = CircleShape
                                    )
                            )
                            Spacer(modifier = Modifier.width(Dimens.middleGap))
                            Text(
                                text = directory.name,
                                color = Color.Black,
                                fontSize = Dimens.normalFontSize,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = "${directory.total}首",
                                color = Color.Secondary,
                                fontSize = Dimens.normalFontSize
                            )
                        }
                        Divider(color = Color.Gray.copy(alpha = 0.3f), thickness = Dimens.borderSize)
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = Dimens.middleGap)
                    .clip(RoundedCornerShape(Dimens.btnBorderRadius))
                    .background(Color.Primary)
                    .clickable {
                        onConfirm(selected.toList())
                    }
                    .padding(vertical = Dimens.middleGap),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "确定",
                    color = Color.White,
                    fontSize = Dimens.normalFontSize
                )
            }
        }
    }
}
