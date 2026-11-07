package com.player.music.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
 * 分享页 ViewModel
 * 对应 Flutter 端 MusicSharePage：把当前播放的歌曲发布到音乐圈
 */
@HiltViewModel
class MusicShareViewModel @Inject constructor(
    private val circleRepository: CircleRepository,
    val player: MusicPlayerManager
) : ViewModel() {

    /** 要分享的歌曲（当前播放歌曲） */
    val currentMusic: StateFlow<Music?> = player.currentMusic

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

    fun updateContent(value: String) {
        _content.value = value
    }

    fun setPermission(value: Int) {
        _permission.value = value
    }

    /**
     * 发布到音乐圈
     */
    fun publish(onSuccess: () -> Unit) {
        if (_publishing.value) return
        val music = currentMusic.value
        if (music == null) {
            _toastMessage.value = "没有可分享的歌曲"
            return
        }
        if (_content.value.isBlank()) {
            _toastMessage.value = "说点什么吧"
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
