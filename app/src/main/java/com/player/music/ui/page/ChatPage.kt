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
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
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
import com.player.music.model.ChatHistoryGroup
import com.player.music.model.ChatMessage
import com.player.music.model.ChatRole
import com.player.music.model.ChatSession
import com.player.music.model.Music
import com.player.music.navigation.Screens
import com.player.music.ui.components.Avatar
import com.player.music.ui.components.AvatarSize
import com.player.music.ui.components.MusicCover
import com.player.music.ui.theme.Color
import com.player.music.ui.theme.Dimens
import com.player.music.utils.ChatContentParser
import com.player.music.viewmodel.ChatViewModel

/**
 * AI 聊天页（首页搜索栏右侧 AI 图标进入）
 * 对应 Flutter 端 lib/pages/ChatPage.dart + uni-app 端 pages/ChatPage.vue：
 * 顶部模型切换 / 会话记录，气泡列表（深度思考内容 + 正文 + <music> 歌单卡片），
 * 底部深度思考 / 中英文切换 + 输入框 + 发送，新会话按钮，会话记录抽屉（按时间分组、分页）
 */
@Composable
fun ChatPage(
    navController: NavHostController,
    viewModel: ChatViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val messages by viewModel.messages.collectAsState()
    val input by viewModel.input.collectAsState()
    val thinking by viewModel.thinking.collectAsState()
    val showThink by viewModel.showThink.collectAsState()
    val language by viewModel.language.collectAsState()
    val models by viewModel.models.collectAsState()
    val activeModelIndex by viewModel.activeModelIndex.collectAsState()
    val userAvatar by viewModel.userAvatar.collectAsState()
    val historyGroups by viewModel.historyGroups.collectAsState()
    val showHistory by viewModel.showHistory.collectAsState()
    val historyLoading by viewModel.historyLoading.collectAsState()
    val historyHasMore by viewModel.historyHasMore.collectAsState()
    val currentMusic by viewModel.player.currentMusic.collectAsState()
    val toastMessage by viewModel.toastMessage.collectAsState()

    var showMenu by remember { mutableStateOf(false) }
    var showModelDialog by remember { mutableStateOf(false) }

    LaunchedEffect(toastMessage) {
        toastMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearToast()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.PageBackground)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // 顶部栏
            ChatHeader(
                avatar = userAvatar,
                modelName = models.getOrNull(activeModelIndex)?.modelName ?: "加载中…",
                onBack = { navController.popBackStack() },
                onMenuClick = { showMenu = !showMenu }
            )

            // 对话列表
            ChatList(
                messages = messages,
                thinking = thinking,
                userAvatar = userAvatar,
                currentMusicId = currentMusic?.id,
                onPlayMusic = { list, index ->
                    viewModel.playMusic(list, index)
                    navController.navigate(Screens.MusicPlayer.route)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            )

            // 深度思考 / 语言切换
            TypeBar(
                showThink = showThink,
                language = language,
                onToggleThink = { viewModel.toggleThink() },
                onToggleLanguage = { viewModel.toggleLanguage() }
            )

            // 输入栏
            InputBar(
                input = input,
                thinking = thinking,
                onInputChange = { viewModel.onInputChange(it) },
                onClear = { viewModel.clearInput() },
                onNewChat = { viewModel.newChat() },
                onSend = { viewModel.send() }
            )
        }

        // 顶部菜单（会话记录 / 切换模型）
        if (showMenu) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable { showMenu = false }
            )
            MenuPopup(
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(top = Dimens.bigGap, end = Dimens.middleGap)
                    .align(Alignment.TopEnd),
                onHistoryClick = {
                    showMenu = false
                    viewModel.openHistory()
                },
                onSwitchModelClick = {
                    showMenu = false
                    showModelDialog = true
                }
            )
        }

        // 会话记录抽屉
        if (showHistory) {
            HistoryDrawer(
                groups = historyGroups,
                loading = historyLoading,
                hasMore = historyHasMore,
                onLoadMore = { viewModel.loadMoreHistory() },
                onSessionClick = { viewModel.openSession(it) },
                onClose = { viewModel.closeHistory() }
            )
        }
    }

    // 切换模型
    if (showModelDialog) {
        ModelDialog(
            models = models.map { it.modelName },
            activeIndex = activeModelIndex,
            onSelect = {
                viewModel.selectModel(it)
                showModelDialog = false
            },
            onDismiss = { showModelDialog = false }
        )
    }
}

/**
 * 顶部：返回 + 头像 + 当前接入模型 + 菜单
 */
@Composable
private fun ChatHeader(
    avatar: String?,
    modelName: String,
    onBack: () -> Unit,
    onMenuClick: () -> Unit
) {
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
                .clickable { onBack() }
        )
        Spacer(modifier = Modifier.width(Dimens.middleGap))
        Avatar(avatarUrl = avatar, size = AvatarSize.SMALL)
        Spacer(modifier = Modifier.width(Dimens.middleGap))
        Text(
            text = "当前接入模型：$modelName",
            color = Color.Black,
            fontSize = Dimens.normalFontSize,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Image(
            painter = painterResource(R.drawable.icon_menu),
            contentDescription = "菜单",
            colorFilter = ColorFilter.tint(Color.Secondary),
            modifier = Modifier
                .size(Dimens.middleIconSize)
                .clickable { onMenuClick() }
        )
    }
}

/**
 * 右上角弹出菜单
 */
@Composable
private fun MenuPopup(
    modifier: Modifier = Modifier,
    onHistoryClick: () -> Unit,
    onSwitchModelClick: () -> Unit
) {
    Column(
        modifier = modifier
            .width(120.dp)
            .clip(RoundedCornerShape(Dimens.moduleBorderRadius))
            .background(Color.White)
            .padding(vertical = Dimens.smallGap)
    ) {
        Text(
            text = "会话记录",
            color = Color.Secondary,
            fontSize = Dimens.normalFontSize,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onHistoryClick() }
                .padding(horizontal = Dimens.middleGap, vertical = Dimens.middleGap)
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(Dimens.borderSize)
                .background(Color.PageBackground)
        )
        Text(
            text = "切换模型",
            color = Color.Secondary,
            fontSize = Dimens.normalFontSize,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onSwitchModelClick() }
                .padding(horizontal = Dimens.middleGap, vertical = Dimens.middleGap)
        )
    }
}

/**
 * 对话列表：AI 气泡（思考内容 + 正文 + 歌单卡片）/ 用户气泡 / 正在思考中
 */
@Composable
private fun ChatList(
    messages: List<ChatMessage>,
    thinking: Boolean,
    userAvatar: String?,
    currentMusicId: Long?,
    onPlayMusic: (List<Music>, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    val lastMessage = messages.lastOrNull()

    // 新内容到达时自动滚到底部
    LaunchedEffect(
        messages.size,
        lastMessage?.responseContent?.length,
        lastMessage?.thinkContent?.length,
        thinking
    ) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.lastIndex)
        }
    }

    LazyColumn(
        state = listState,
        modifier = modifier,
        contentPadding = PaddingValues(Dimens.middleGap),
        verticalArrangement = Arrangement.spacedBy(Dimens.middleGap)
    ) {
        messages.forEachIndexed { index, message ->
            item(key = message.id) {
                if (message.role == ChatRole.AI) {
                    AiBubble(
                        message = message,
                        showThinkingTip = thinking &&
                            index == messages.lastIndex &&
                            message.responseContent.isBlank() &&
                            message.thinkContent.isBlank(),
                        currentMusicId = currentMusicId,
                        onPlayMusic = onPlayMusic
                    )
                } else {
                    UserBubble(text = message.text, avatar = userAvatar)
                }
            }
        }
    }
}

/**
 * AI 气泡：左侧 AI 图标 + 白色卡片（思考内容置灰，正文按片段渲染）
 */
@Composable
private fun AiBubble(
    message: ChatMessage,
    showThinkingTip: Boolean,
    currentMusicId: Long?,
    onPlayMusic: (List<Music>, Int) -> Unit
) {
    val segments = remember(message.responseContent, message.streaming) {
        ChatContentParser.parseSegments(message.responseContent, !message.streaming)
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Image(
            painter = painterResource(R.drawable.icon_ai),
            contentDescription = "AI",
            modifier = Modifier
                .padding(top = Dimens.smallGap)
                .size(Dimens.middleIconSize)
        )
        Spacer(modifier = Modifier.width(Dimens.smallGap))
        Column(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(Dimens.moduleBorderRadius))
                .background(Color.White)
                .padding(Dimens.middleGap)
        ) {
            if (message.thinkContent.isNotBlank()) {
                Text(
                    text = message.thinkContent,
                    color = Color.Secondary,
                    fontSize = Dimens.normalFontSize
                )
                if (message.responseContent.isNotBlank()) {
                    Spacer(modifier = Modifier.height(Dimens.smallGap))
                }
            }

            if (showThinkingTip) {
                Text(
                    text = "正在思考中",
                    color = Color.Secondary,
                    fontSize = Dimens.normalFontSize
                )
            }

            segments.forEach { segment ->
                if (segment.isMusic) {
                    MusicCard(
                        musicList = segment.musicList,
                        currentMusicId = currentMusicId,
                        onPlayMusic = onPlayMusic
                    )
                } else {
                    Text(
                        text = ChatContentParser.stripMarkdown(segment.text),
                        color = Color.Black,
                        fontSize = Dimens.normalFontSize
                    )
                }
            }
        }
    }
}

/**
 * 用户气泡：白色卡片靠右 + 用户头像
 */
@Composable
private fun UserBubble(text: String, avatar: String?) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = text,
            color = Color.Black,
            fontSize = Dimens.normalFontSize,
            modifier = Modifier
                .weight(1f, fill = false)
                .clip(RoundedCornerShape(Dimens.moduleBorderRadius))
                .background(Color.White)
                .padding(Dimens.middleGap)
        )
        Spacer(modifier = Modifier.width(Dimens.smallGap))
        Avatar(avatarUrl = avatar, size = AvatarSize.SMALL)
    }
}

/**
 * AI 回复里的歌单卡片（对应 uni-app 端 MusicListCardComponent）
 */
@Composable
private fun MusicCard(
    musicList: List<Music>,
    currentMusicId: Long?,
    onPlayMusic: (List<Music>, Int) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = Dimens.smallGap)
            .clip(RoundedCornerShape(Dimens.moduleBorderRadius))
            .background(Color.PageBackground)
            .padding(Dimens.smallGap)
    ) {
        musicList.forEachIndexed { index, music ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onPlayMusic(musicList, index) }
                    .padding(vertical = Dimens.smallGap),
                verticalAlignment = Alignment.CenterVertically
            ) {
                MusicCover(coverUrl = music.cover, size = Dimens.smallAvatar)
                Spacer(modifier = Modifier.width(Dimens.smallGap))
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
                Image(
                    painter = painterResource(
                        if (currentMusicId == music.id) R.drawable.icon_music_playing_grey
                        else R.drawable.icon_music_play
                    ),
                    contentDescription = "播放",
                    modifier = Modifier.size(Dimens.listIconSize)
                )
            }
        }
    }
}

/**
 * 深度思考 / 语言切换
 */
@Composable
private fun TypeBar(
    showThink: Boolean,
    language: String,
    onToggleThink: () -> Unit,
    onToggleLanguage: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.PageBackground)
            .padding(horizontal = Dimens.middleGap, vertical = Dimens.smallGap),
        horizontalArrangement = Arrangement.spacedBy(Dimens.middleGap),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Chip(
            text = "深度思考",
            active = showThink,
            onClick = onToggleThink
        )
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(Dimens.bigBorderRadius))
                .background(Color.White)
                .border(
                    width = Dimens.borderSize,
                    color = Color.DisableColor,
                    shape = RoundedCornerShape(Dimens.bigBorderRadius)
                )
                .clickable { onToggleLanguage() }
                .padding(horizontal = Dimens.middleGap, vertical = Dimens.smallGap),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (language == "zh") "中文" else "En",
                color = Color.Black,
                fontSize = Dimens.normalFontSize
            )
            Spacer(modifier = Modifier.width(Dimens.smallGap))
            Image(
                painter = painterResource(R.drawable.icon_switch),
                contentDescription = "切换语言",
                colorFilter = ColorFilter.tint(Color.Secondary),
                modifier = Modifier.size(Dimens.smallIconSize)
            )
        }
    }
}

@Composable
private fun Chip(text: String, active: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(Dimens.bigBorderRadius))
            .background(Color.White)
            .border(
                width = Dimens.borderSize,
                color = if (active) Color.Primary else Color.DisableColor,
                shape = RoundedCornerShape(Dimens.bigBorderRadius)
            )
            .clickable { onClick() }
            .padding(horizontal = Dimens.middleGap, vertical = Dimens.smallGap),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (active) Color.Primary else Color.Secondary,
            fontSize = Dimens.normalFontSize
        )
    }
}

/**
 * 输入栏：新建会话 + 输入框（清空）+ 发送
 */
@Composable
private fun InputBar(
    input: String,
    thinking: Boolean,
    onInputChange: (String) -> Unit,
    onClear: () -> Unit,
    onNewChat: () -> Unit,
    onSend: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .navigationBarsPadding()
            .padding(Dimens.middleGap),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(R.drawable.icon_chat),
            contentDescription = "新建会话",
            colorFilter = ColorFilter.tint(Color.Secondary),
            modifier = Modifier
                .size(Dimens.middleIconSize)
                .clickable { onNewChat() }
        )

        Spacer(modifier = Modifier.width(Dimens.middleGap))

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
                        text = "有问题，尽管问",
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

        Box(
            modifier = Modifier
                .size(Dimens.inputHeight)
                .clip(RoundedCornerShape(Dimens.inputHeight / 2))
                .background(Color.PageBackground)
                .clickable(enabled = !thinking) { onSend() },
            contentAlignment = Alignment.Center
        ) {
            if (thinking) {
                CircularProgressIndicator(
                    modifier = Modifier.size(Dimens.smallIconSize),
                    color = Color.Secondary,
                    strokeWidth = 2.dp
                )
            } else {
                Image(
                    painter = painterResource(R.drawable.icon_send),
                    contentDescription = "发送",
                    modifier = Modifier.size(Dimens.listIconSize)
                )
            }
        }
    }
}

/**
 * 会话记录抽屉：左侧 70% 宽，按时间分组，滚到底部加载下一页
 */
@Composable
private fun HistoryDrawer(
    groups: List<ChatHistoryGroup>,
    loading: Boolean,
    hasMore: Boolean,
    onLoadMore: () -> Unit,
    onSessionClick: (ChatSession) -> Unit,
    onClose: () -> Unit
) {
    Row(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(0.7f)
                .background(Color.White)
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(Dimens.middleGap)
        ) {
            Text(
                text = "会话记录",
                color = Color.Black,
                fontSize = Dimens.bigFontSize,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(Dimens.middleGap))

            val listState = rememberLazyListState()
            val shouldLoadMore by remember {
                derivedStateOf {
                    val info = listState.layoutInfo
                    val last = info.visibleItemsInfo.lastOrNull()?.index ?: -1
                    info.totalItemsCount > 0 && last >= info.totalItemsCount - 2
                }
            }
            LaunchedEffect(shouldLoadMore, groups.size) {
                if (shouldLoadMore && hasMore) onLoadMore()
            }

            val rows = remember(groups) {
                buildList {
                    groups.forEach { group ->
                        add(HistoryRow.Header(group.timeAgo))
                        group.sessions.forEach { add(HistoryRow.Session(it)) }
                    }
                }
            }

            LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
                items(rows.size) { index ->
                    when (val row = rows[index]) {
                        is HistoryRow.Header -> Text(
                            text = row.timeAgo,
                            color = Color.Secondary,
                            fontSize = Dimens.normalFontSize,
                            modifier = Modifier.padding(vertical = Dimens.smallGap)
                        )
                        is HistoryRow.Session -> Text(
                            text = row.session.title,
                            color = Color.Black,
                            fontSize = Dimens.normalFontSize,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSessionClick(row.session) }
                                .padding(vertical = Dimens.middleGap)
                        )
                    }
                }
                item {
                    if (loading) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(Dimens.middleGap),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(Dimens.middleIconSize),
                                color = Color.Primary,
                                strokeWidth = 2.dp
                            )
                        }
                    } else if (groups.isEmpty()) {
                        Text(
                            text = "暂无会话记录",
                            color = Color.Secondary,
                            fontSize = Dimens.normalFontSize,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = Dimens.middleGap)
                        )
                    }
                }
            }
        }

        // 遮罩，点击关闭
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .background(Color.Black.copy(alpha = 0.4f))
                .clickable { onClose() }
        )
    }
}

private sealed class HistoryRow {
    data class Header(val timeAgo: String) : HistoryRow()
    data class Session(val session: ChatSession) : HistoryRow()
}

/**
 * 切换模型弹窗
 */
@Composable
private fun ModelDialog(
    models: List<String>,
    activeIndex: Int,
    onSelect: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
            .clickable { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .fillMaxHeight(0.6f)
                .clip(RoundedCornerShape(Dimens.moduleBorderRadius))
                .background(Color.White)
                .padding(Dimens.middleGap)
        ) {
            Text(
                text = "切换模型",
                color = Color.Black,
                fontSize = Dimens.bigFontSize,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = Dimens.middleGap)
            )
            LazyColumn {
                items(models.size) { index ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(index) }
                            .padding(vertical = Dimens.middleGap),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = models[index],
                            color = if (index == activeIndex) Color.Primary else Color.Black,
                            fontSize = Dimens.normalFontSize
                        )
                        if (index == activeIndex) {
                            Text(
                                text = "已选择",
                                color = Color.Primary,
                                fontSize = Dimens.normalFontSize
                            )
                        }
                    }
                }
            }
        }
    }
}
