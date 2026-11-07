package com.player.music.ui.page

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.player.music.R
import com.player.music.ui.components.MusicCover
import com.player.music.ui.theme.Color
import com.player.music.ui.theme.Dimens
import com.player.music.viewmodel.MusicShareViewModel

/**
 * 分享到音乐圈
 * 对应 Flutter 端 lib/pages/MusicSharePage.dart：
 * 输入这一刻的想法 + 展示待分享歌曲 + 选择谁可以看 + 发布
 */
@Composable
fun MusicSharePage(
    navController: NavHostController,
    viewModel: MusicShareViewModel = hiltViewModel()
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val music by viewModel.currentMusic.collectAsState()
    val content by viewModel.content.collectAsState()
    val permission by viewModel.permission.collectAsState()
    val publishing by viewModel.publishing.collectAsState()
    val toastMessage by viewModel.toastMessage.collectAsState()

    var showPermissionDialog by remember { mutableStateOf(false) }

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
        // 顶部操作栏
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .statusBarsPadding()
                .padding(horizontal = Dimens.middleGap, vertical = Dimens.middleGap),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(Dimens.btnBorderRadius))
                    .background(Color.PageBackground)
                    .clickable { navController.popBackStack() }
                    .padding(horizontal = Dimens.middleGap * 2, vertical = Dimens.smallGap),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "取消",
                    color = Color.Secondary,
                    fontSize = Dimens.normalFontSize
                )
            }

            Text(
                text = "分享到音乐圈",
                color = Color.Black,
                fontSize = Dimens.bigFontSize,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f)
            )

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(Dimens.btnBorderRadius))
                    .background(if (publishing) Color.DisableColor else Color.Primary)
                    .clickable(enabled = !publishing) {
                        viewModel.publish { navController.popBackStack() }
                    }
                    .padding(horizontal = Dimens.middleGap * 2, vertical = Dimens.smallGap),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (publishing) "发布中" else "发布",
                    color = Color.White,
                    fontSize = Dimens.normalFontSize
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimens.middleGap)
        ) {
            // 这一刻的想法
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(Dimens.textareaHeight)
                    .clip(RoundedCornerShape(Dimens.moduleBorderRadius))
                    .background(Color.White)
                    .padding(Dimens.middleGap)
            ) {
                BasicTextField(
                    value = content,
                    onValueChange = { viewModel.updateContent(it) },
                    textStyle = TextStyle(
                        color = Color.Black,
                        fontSize = Dimens.normalFontSize
                    ),
                    modifier = Modifier.fillMaxSize(),
                    decorationBox = { inner ->
                        if (content.isEmpty()) {
                            Text(
                                text = "这一刻的想法",
                                color = Color.Secondary,
                                fontSize = Dimens.normalFontSize
                            )
                        }
                        inner()
                    }
                )
            }

            Spacer(modifier = Modifier.height(Dimens.middleGap))

            // 待分享的歌曲
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(Dimens.moduleBorderRadius))
                    .background(Color.White)
                    .padding(Dimens.middleGap),
                verticalAlignment = Alignment.CenterVertically
            ) {
                MusicCover(
                    coverUrl = music?.cover,
                    size = Dimens.middleAvatar
                )
                Spacer(modifier = Modifier.width(Dimens.middleGap))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = music?.songName ?: "暂无播放歌曲",
                        color = Color.Black,
                        fontSize = Dimens.normalFontSize,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = music?.authorName ?: "",
                        color = Color.Secondary,
                        fontSize = Dimens.normalFontSize,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(Dimens.middleGap))

            // 谁可以看
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(Dimens.moduleBorderRadius))
                    .background(Color.White)
                    .clickable { showPermissionDialog = true }
                    .padding(Dimens.middleGap),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(R.drawable.icon_permission),
                    contentDescription = null,
                    modifier = Modifier.size(Dimens.middleIconSize)
                )
                Spacer(modifier = Modifier.width(Dimens.middleGap))
                Text(
                    text = "谁可以看",
                    color = Color.Black,
                    fontSize = Dimens.normalFontSize,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = if (permission == 0) "私密" else "公开",
                    color = Color.Secondary,
                    fontSize = Dimens.normalFontSize
                )
                Spacer(modifier = Modifier.width(Dimens.smallGap))
                Image(
                    painter = painterResource(R.drawable.icon_arrow),
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(Color.Secondary),
                    modifier = Modifier.size(Dimens.smallIconSize)
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))
        Spacer(modifier = Modifier.navigationBarsPadding())
    }

    if (showPermissionDialog) {
        PermissionDialog(
            permission = permission,
            onSelect = { value ->
                viewModel.setPermission(value)
                showPermissionDialog = false
            },
            onDismiss = { showPermissionDialog = false }
        )
    }
}

/**
 * 谁可以看：私密 / 公开
 */
@Composable
private fun PermissionDialog(
    permission: Int,
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
                .clip(RoundedCornerShape(Dimens.moduleBorderRadius))
                .background(Color.White)
                .padding(Dimens.middleGap)
        ) {
            Text(
                text = "谁可以看",
                color = Color.Black,
                fontSize = Dimens.bigFontSize,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = Dimens.middleGap)
            )

            listOf(1 to "公开", 0 to "私密").forEach { (value, label) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelect(value) }
                        .padding(vertical = Dimens.middleGap),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = label,
                        color = if (value == permission) Color.Primary else Color.Black,
                        fontSize = Dimens.normalFontSize
                    )
                    if (value == permission) {
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
