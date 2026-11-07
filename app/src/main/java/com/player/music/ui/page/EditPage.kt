package com.player.music.ui.page

import android.util.Patterns
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.player.music.R
import com.player.music.model.User
import com.player.music.ui.theme.Color
import com.player.music.ui.theme.Dimens
import com.player.music.viewmodel.MainViewModel
import com.player.music.viewmodel.UpdateResult
import com.player.music.viewmodel.UserViewModel

/**
 * 通用「编辑并保存单个用户字段」页面
 * 对应 Flutter 端 lib/pages/EditPage.dart：顶部返回 + 标题 + 保存按钮（未修改时置灰），
 * 中间是编辑区，保存时调用 updateUserInfo 接口，成功后返回上一页。
 *
 * 路由参数：
 * - title     页面标题（如「编辑昵称」）
 * - field     要保存到 User 的字段：username / telephone / email / region / sign
 * - value     当前值
 * - allowEmpty 是否允许为空
 *
 * 说明：性别、出生日期在本项目里仍然是用户信息页内的行内控件（单选 / 日期选择），
 * 与 Flutter 端 EditPage 中被注释掉的日期选择分支保持一致的行为。
 */
@Composable
fun EditPage(
    navController: NavHostController,
    title: String,
    field: String,
    initialValue: String,
    allowEmpty: Boolean,
    mainViewModel: MainViewModel = hiltViewModel(),
    userViewModel: UserViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val currentUser by mainViewModel.currentUser.collectAsState()
    val updateResult by userViewModel.updateResult.collectAsState()

    var value by remember { mutableStateOf(initialValue) }

    val hasChange = value != initialValue
    val hint = when (field) {
        "username" -> "请输入昵称"
        "telephone" -> "请输入电话号码"
        "email" -> "请输入邮箱地址"
        "region" -> "请输入地区"
        "sign" -> "请输入个性签名"
        else -> ""
    }
    val keyboardType = when (field) {
        "telephone" -> KeyboardType.Phone
        "email" -> KeyboardType.Email
        else -> KeyboardType.Text
    }

    // 保存结果回调
    LaunchedEffect(updateResult) {
        when (val result = updateResult) {
            is UpdateResult.Success -> {
                Toast.makeText(context, result.message, Toast.LENGTH_SHORT).show()
                userViewModel.resetUpdateResult()
                navController.popBackStack()
            }
            is UpdateResult.Error -> {
                Toast.makeText(context, result.message, Toast.LENGTH_SHORT).show()
                userViewModel.resetUpdateResult()
            }
            else -> Unit
        }
    }

    fun save() {
        if (!hasChange) return
        val newValue = value.trim()
        val error = validate(title, field, newValue, allowEmpty)
        if (error != null) {
            Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
            return
        }
        val user = currentUser ?: run {
            Toast.makeText(context, "用户信息未加载，请稍后重试", Toast.LENGTH_SHORT).show()
            return
        }
        userViewModel.updateUserInfo(applyField(user, field, newValue))
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.PageBackground)
            .imePadding()
    ) {
        // 顶部：返回 + 标题 + 保存
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .statusBarsPadding()
                .padding(horizontal = Dimens.middleGap, vertical = Dimens.smallGap),
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
                text = title,
                color = Color.Black,
                fontSize = Dimens.bigFontSize,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Box(
                modifier = Modifier
                    .size(width = Dimens.btnWidth, height = Dimens.btnHeight)
                    .clip(RoundedCornerShape(Dimens.minBtnRadius))
                    .background(if (hasChange) Color.Primary else Color.DisableColor)
                    .clickable(enabled = hasChange) { save() },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "保存",
                    color = if (hasChange) Color.White else Color.Secondary,
                    fontSize = Dimens.normalFontSize
                )
            }
        }

        // 编辑区
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimens.middleGap)
                .clip(RoundedCornerShape(Dimens.moduleBorderRadius))
                .background(Color.White)
                .padding(Dimens.middleGap)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(Dimens.inputHeight),
                contentAlignment = Alignment.CenterStart
            ) {
                if (value.isEmpty()) {
                    Text(
                        text = hint,
                        color = Color.Secondary,
                        fontSize = Dimens.normalFontSize
                    )
                }
                BasicTextField(
                    value = value,
                    onValueChange = { value = it },
                    singleLine = true,
                    textStyle = TextStyle(
                        color = Color.Black,
                        fontSize = Dimens.normalFontSize
                    ),
                    cursorBrush = SolidColor(Color.Primary),
                    keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start
            ) {
                Text(
                    text = if (allowEmpty) "选填" else "必填",
                    color = Color.Secondary,
                    fontSize = Dimens.normalFontSize
                )
            }
        }

        Spacer(modifier = Modifier.navigationBarsPadding())
    }
}

/**
 * 字段校验：返回错误文案，null 表示通过
 */
private fun validate(title: String, field: String, value: String, allowEmpty: Boolean): String? {
    if (value.isEmpty()) {
        if (!allowEmpty) return "${title.replace("编辑", "")}不能为空"
        return null
    }
    return when (field) {
        "telephone" -> if (Patterns.PHONE.matcher(value).matches()) null else "电话号码格式不正确"
        "email" -> if (Patterns.EMAIL_ADDRESS.matcher(value).matches()) null else "邮箱格式不正确"
        else -> null
    }
}

/**
 * 把编辑结果写回 User 对应字段
 */
private fun applyField(user: User, field: String, value: String): User {
    return when (field) {
        "username" -> user.copy(username = value)
        "telephone" -> user.copy(telephone = value)
        "email" -> user.copy(email = value)
        "region" -> user.copy(region = value)
        "sign" -> user.copy(sign = value)
        else -> user
    }
}
