package com.player.music.ui.page

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import com.player.music.R
import com.player.music.model.Circle
import com.player.music.model.Comment
import com.player.music.ui.components.Avatar
import com.player.music.ui.components.AvatarSize
import com.player.music.ui.components.MusicCover
import com.player.music.ui.theme.Color
import com.player.music.ui.theme.Dimens
import com.player.music.utils.CommonUtils
import com.player.music.viewmodel.MusicCircleViewModel

/**
 * 音乐圈（音乐圈 Tab）
 * 对应 Flutter 端 lib/pages/MusicCirclePage.dart：
 * 动态列表（头像 + 昵称 + 内容 + 关联歌曲 + 时间 + 菜单）、点赞人列表、评论与回复、上拉加载更多
 */
@Composable
fun MusicCirclePage(
    listState: LazyListState,
    currentUserId: String?,
    currentUsername: String?,
    currentUserAvatar: String? = null,
    viewModel: MusicCircleViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val circles by viewModel.circles.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isLoadingMore by viewModel.isLoadingMore.collectAsState()
    val reachedEnd by viewModel.reachedEnd.collectAsState()
    val toastMessage by viewModel.toastMessage.collectAsState()

    // 当前弹出菜单的动态
    var menuCircle by remember { mutableStateOf<Circle?>(null) }
    // 当前正在评论的动态
    var commentCircle by remember { mutableStateOf<Circle?>(null) }
    // 正在回复的一级评论 / 被回复的评论
    var replyTopComment by remember { mutableStateOf<Comment?>(null) }
    var replyComment by remember { mutableStateOf<Comment?>(null) }
    var inputText by remember { mutableStateOf("") }

    LaunchedEffect(toastMessage) {
        toastMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearToast()
        }
    }

    when {
        isLoading && circles.isEmpty() -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.PageBackground),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color.Primary)
            }
        }

        circles.isEmpty() -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.PageBackground),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "暂无音乐圈动态",
                    color = Color.Secondary,
                    fontSize = Dimens.normalFontSize
                )
            }
        }

        else -> {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.PageBackground),
                contentPadding = PaddingValues(
                    start = Dimens.middleGap,
                    end = Dimens.middleGap,
                    top = Dimens.middleGap,
                    bottom = Dimens.miniPlayerSize + Dimens.middleGap
                ),
                verticalArrangement = Arrangement.spacedBy(Dimens.middleGap)
            ) {
                items(circles, key = { it.id }) { circle ->
                    CircleItem(
                        circle = circle,
                        onMenuClick = { menuCircle = circle },
                        onMusicClick = {
                            Toast.makeText(context, "播放器功能开发中，敬请期待", Toast.LENGTH_SHORT).show()
                        },
                        onReplyClick = { topComment, clicked ->
                            commentCircle = circle
                            replyTopComment = topComment
                            replyComment = clicked
                            inputText = ""
                        }
                    )
                }

                item(key = "load_more") {
                    CircleLoadMoreFooter(
                        isLoadingMore = isLoadingMore,
                        reachedEnd = reachedEnd,
                        onLoadMore = { viewModel.loadMore() }
                    )
                }
            }
        }
    }

    // 点赞 / 评论 菜单
    menuCircle?.let { circle ->
        CircleActionMenu(
            liked = viewModel.isLikedByMe(circle, currentUserId),
            onDismiss = { menuCircle = null },
            onLikeClick = {
                viewModel.toggleLike(circle, currentUserId, currentUsername)
                menuCircle = null
            },
            onCommentClick = {
                commentCircle = circle
                replyTopComment = null
                replyComment = null
                inputText = ""
                menuCircle = null
            }
        )
    }

    // 评论输入框（底部弹层）
    commentCircle?.let { circle ->
        val hint = when {
            replyComment != null -> "回复${replyComment?.username ?: ""}"
            replyTopComment != null -> "回复${replyTopComment?.username ?: ""}"
            else -> "评论"
        }
        CommentInputSheet(
            hint = hint,
            value = inputText,
            onValueChange = { inputText = it },
            onDismiss = {
                commentCircle = null
                replyTopComment = null
                replyComment = null
                inputText = ""
            },
            onSend = {
                viewModel.submitComment(
                    circle = circle,
                    content = inputText,
                    topComment = replyTopComment,
                    replyComment = replyComment,
                    userId = currentUserId,
                    username = currentUsername,
                    avatar = currentUserAvatar
                )
                commentCircle = null
                replyTopComment = null
                replyComment = null
                inputText = ""
            }
        )
    }
}

/**
 * 单条音乐圈动态
 */
@Composable
private fun CircleItem(
    circle: Circle,
    onMenuClick: () -> Unit,
    onMusicClick: () -> Unit,
    onReplyClick: (topComment: Comment?, clicked: Comment) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Dimens.moduleBorderRadius),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimens.middleGap),
            verticalAlignment = Alignment.Top
        ) {
            Avatar(
                avatarUrl = circle.useravater,
                size = AvatarSize.MIDDLE
            )

            Spacer(modifier = Modifier.width(Dimens.middleGap))

            Column(modifier = Modifier.weight(1f)) {
                // 昵称
                Text(
                    text = circle.username ?: "",
                    color = Color.LinkBlue,
                    fontSize = Dimens.normalFontSize,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(Dimens.smallGap))

                // 动态内容
                Text(
                    text = circle.content ?: "",
                    color = Color.Black,
                    fontSize = Dimens.normalFontSize,
                    maxLines = 5,
                    overflow = TextOverflow.Ellipsis
                )

                // 关联歌曲（没有关联歌曲时不展示，避免出现空白的播放条）
                if (!circle.musicSongName.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(Dimens.middleGap))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = Color.PageBackground,
                                shape = RoundedCornerShape(Dimens.btnBorderRadius)
                            )
                            .padding(vertical = Dimens.smallGap),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        MusicCover(
                            coverUrl = circle.musicCover,
                            size = Dimens.middleAvatar
                        )
                        Spacer(modifier = Modifier.width(Dimens.middleGap))
                        Text(
                            text = "${circle.musicSongName ?: ""} - ${circle.musicAuthorName ?: ""}",
                            color = Color.Black,
                            fontSize = Dimens.normalFontSize,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Image(
                            painter = painterResource(R.drawable.icon_music_play),
                            contentDescription = "播放",
                            modifier = Modifier
                                .size(Dimens.listIconSize)
                                .clickable { onMusicClick() }
                        )
                        Spacer(modifier = Modifier.width(Dimens.middleGap))
                    }
                }

                Spacer(modifier = Modifier.height(Dimens.middleGap))

                // 时间 + 菜单
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = CommonUtils.formatRelativeTime(circle.createTime ?: ""),
                        color = Color.Secondary,
                        fontSize = Dimens.normalFontSize
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Image(
                        painter = painterResource(R.drawable.icon_music_menu),
                        contentDescription = "更多操作",
                        modifier = Modifier
                            .size(Dimens.listIconSize)
                            .clickable { onMenuClick() }
                    )
                }

                val likes = circle.circleLikes ?: emptyList()
                val comments = circle.circleComments ?: emptyList()

                if (likes.isNotEmpty() || comments.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(Dimens.middleGap))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = Color.PageBackground,
                                shape = RoundedCornerShape(Dimens.moduleBorderRadius)
                            )
                            .padding(Dimens.middleGap)
                    ) {
                        if (likes.isNotEmpty()) {
                            LikeUsersRow(likes.mapNotNull { it.username })
                        }
                        if (comments.isNotEmpty()) {
                            if (likes.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(Dimens.smallGap))
                            }
                            comments.filter { it.isTopComment }.forEach { comment ->
                                CommentRow(
                                    comment = comment,
                                    onReplyClick = { onReplyClick(comment, comment) },
                                    onReplyToReply = { reply -> onReplyClick(comment, reply) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * 点赞用户列表（逗号分隔展示）
 */
@Composable
private fun LikeUsersRow(usernames: List<String>) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Image(
            painter = painterResource(R.drawable.icon_music_like),
            contentDescription = "点赞",
            modifier = Modifier.size(Dimens.smallIconSize)
        )
        Spacer(modifier = Modifier.width(Dimens.smallGap))
        Text(
            text = usernames.joinToString("、"),
            color = Color.LinkBlue,
            fontSize = Dimens.normalFontSize,
            maxLines = 5,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * 评论（含二级回复）
 */
@Composable
private fun CommentRow(
    comment: Comment,
    onReplyClick: () -> Unit,
    onReplyToReply: (Comment) -> Unit,
    isReply: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = if (isReply) Dimens.smallGap else 0.dp),
        verticalAlignment = Alignment.Top
    ) {
        Avatar(
            avatarUrl = comment.avater,
            size = if (isReply) AvatarSize.SMALL else AvatarSize.SMALL
        )

        Spacer(modifier = Modifier.width(Dimens.smallGap))

        Column(modifier = Modifier.weight(1f)) {
            // 昵称（回复时展示 "A▶B"）
            Text(
                text = if (!comment.replyUserName.isNullOrBlank()) {
                    "${comment.username ?: ""}▶${comment.replyUserName}"
                } else {
                    comment.username ?: ""
                },
                color = Color.LinkBlue,
                fontSize = Dimens.normalFontSize,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(Dimens.smallGap))
            Text(
                text = comment.content ?: "",
                color = Color.Black,
                fontSize = Dimens.normalFontSize,
                modifier = Modifier.clickable { onReplyClick() }
            )
            val commentTime = CommonUtils.formatRelativeTime(comment.createTime ?: "")
            if (commentTime.isNotBlank()) {
                Spacer(modifier = Modifier.height(Dimens.smallGap))
                Text(
                    text = commentTime,
                    color = Color.Secondary,
                    fontSize = Dimens.normalFontSize
                )
            }

            // 二级回复
            comment.replyList?.forEach { reply ->
                CommentRow(
                    comment = reply,
                    onReplyClick = { onReplyToReply(reply) },
                    onReplyToReply = onReplyToReply,
                    isReply = true
                )
            }
        }
    }
}

/**
 * 点赞 / 评论 弹出菜单
 */
@Composable
private fun CircleActionMenu(
    liked: Boolean,
    onDismiss: () -> Unit,
    onLikeClick: () -> Unit,
    onCommentClick: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Row(
            modifier = Modifier
                .width(180.dp)
                .height(40.dp)
                .background(
                    color = Color.SubTitle,
                    shape = RoundedCornerShape(Dimens.moduleBorderRadius)
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onLikeClick() },
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(R.drawable.icon_like_white),
                    contentDescription = if (liked) "取消赞" else "赞",
                    modifier = Modifier.size(Dimens.smallIconSize)
                )
                Spacer(modifier = Modifier.width(Dimens.smallGap))
                Text(
                    text = if (liked) "取消赞" else "赞",
                    color = Color.White,
                    fontSize = Dimens.normalFontSize
                )
            }
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onCommentClick() },
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(R.drawable.icon_comment_white),
                    contentDescription = "评论",
                    modifier = Modifier.size(Dimens.smallIconSize)
                )
                Spacer(modifier = Modifier.width(Dimens.smallGap))
                Text(
                    text = "评论",
                    color = Color.White,
                    fontSize = Dimens.normalFontSize
                )
            }
        }
    }
}

/**
 * 底部评论输入框（模态底部弹层）
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CommentInputSheet(
    hint: String,
    value: String,
    onValueChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onSend: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
        dragHandle = null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimens.middleGap),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(Dimens.inputHeight)
                    .background(
                        color = Color.PageBackground,
                        shape = RoundedCornerShape(Dimens.inputHeight / 2)
                    )
                    .padding(horizontal = Dimens.middleGap),
                contentAlignment = Alignment.CenterStart
            ) {
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = TextStyle(
                        color = Color.Black,
                        fontSize = Dimens.normalFontSize
                    ),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Send
                    ),
                    singleLine = true,
                    decorationBox = { innerTextField ->
                        if (value.isEmpty()) {
                            Text(
                                text = hint,
                                color = Color.Secondary,
                                fontSize = Dimens.normalFontSize
                            )
                        }
                        innerTextField()
                    }
                )
            }

            Spacer(modifier = Modifier.width(Dimens.middleGap))

            Box(
                modifier = Modifier
                    .height(Dimens.inputHeight)
                    .background(
                        color = Color.Primary,
                        shape = RoundedCornerShape(Dimens.inputHeight / 2)
                    )
                    .clickable { onSend() }
                    .padding(horizontal = Dimens.middleGap),
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

/**
 * 列表底部：自动加载下一页
 */
@Composable
private fun CircleLoadMoreFooter(
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
