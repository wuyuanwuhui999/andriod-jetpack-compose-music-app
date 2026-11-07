package com.player.music.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.player.music.model.Music
import com.player.music.player.MusicPlayerManager
import com.player.music.repository.CircleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 分享到音乐圈 ViewModel
 * 对应 Flutter 端 MusicSharePage.dart + uni-app 端 MusicSharePage.vue：
 * 输入这一刻的想法、选择要分享的歌曲（默认带上当前播放歌曲，可去「选择音乐」页换一首）、
 * 选择谁可以看（公开/私密）、发布到音乐圈
 */
@HiltViewModel
class MusicShareViewModel @Inject constructor(
    private val circleRepository: CircleRepository,
    val player: MusicPlayerManager
) : ViewModel() {

    private val gson = Gson()

    /** 要分享的歌曲：默认当前播放歌曲，可在选择音乐页换一首 */
    private val _selectedMusic = MutableStateFlow<Music?>(null)
    val selectedMusic: StateFlow<Music?> = _selectedMusic.asStateFlow()

    /** 这一刻的想法 */
    private val _content = MutableStateFlow("")
    val content: StateFlow<String> = _content.asStateFlow()

    /** 谁可以看：0-私密，1-公开 */
    private val _permission = MutableStateFlow(1)
    val permission: StateFlow<Int> = _permission.asStateFlow()

    private val _publishing = MutableStateFlow(false)
    val publishing: StateFlow<Boolean> = _publishing.asStateFlow()

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    init {
        // 与 Flutter 端一致：进页面默认带上当前播放的歌曲
        _selectedMusic.value = player.currentMusic.value
    }

    fun updateContent(value: String) {
        _content.value = value
    }

    fun setPermission(value: Int) {
        _permission.value = value
    }

    /** 选择音乐页选中的歌曲（通过路由返回的 JSON 回填） */
    fun selectMusicFromJson(json: String?) {
        if (json.isNullOrBlank()) return
        runCatching { gson.fromJson(json, Music::class.java) }
            .getOrNull()
            ?.let { _selectedMusic.value = it }
    }

    /**
     * 发布到音乐圈
     * 与参考实现一致：内容可以为空，只发歌曲；但必须有一首歌
     */
    fun publish(onSuccess: () -> Unit) {
        if (_publishing.value) return
        val music = _selectedMusic.value
        if (music == null) {
            _toastMessage.value = "请选择要分享的歌曲"
            return
        }
        _publishing.value = true
        viewModelScope.launch {
            val result = circleRepository.insertCircle(
                musicId = music.id,
                content = _content.value.trim(),
                permission = _permission.value
            )
            _publishing.value = false
            if (result.isSuccess) {
                _toastMessage.value = "发布成功"
                onSuccess()
            } else {
                _toastMessage.value = result.exceptionOrNull()?.message ?: "发布失败"
            }
        }
    }

    fun clearToast() {
        _toastMessage.value = null
    }
}
