package com.player.music.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.player.music.model.Comment
import com.player.music.model.FavoriteDirectory
import com.player.music.model.LoopMode
import com.player.music.model.Music
import com.player.music.player.MusicPlayerManager
import com.player.music.repository.MusicRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 音乐播放页 ViewModel
 * 对应 Flutter 端 lib/pages/MusicPlayerPage.dart + PlayerMusicProvider
 */
@HiltViewModel
class MusicPlayerViewModel @Inject constructor(
    private val musicRepository: MusicRepository,
    val player: MusicPlayerManager
) : ViewModel() {

    /** 当前歌曲 */
    val currentMusic: StateFlow<Music?> = player.currentMusic
    val isPlaying: StateFlow<Boolean> = player.isPlaying
    val positionMs: StateFlow<Int> = player.positionMs
    val durationMs: StateFlow<Int> = player.durationMs
    val loopMode: StateFlow<LoopMode> = player.loopMode
    val playlist: StateFlow<List<Music>> = player.playlist

    /** 是否已加入收藏夹（isMusicFavorite 接口） */
    private val _isFavorite = MutableStateFlow(false)
    val isFavorite: StateFlow<Boolean> = _isFavorite.asStateFlow()

    /** 评论 */
    private val _comments = MutableStateFlow<List<Comment>>(emptyList())
    val comments: StateFlow<List<Comment>> = _comments.asStateFlow()

    private val _commentTotal = MutableStateFlow(0)
    val commentTotal: StateFlow<Int> = _commentTotal.asStateFlow()

    /** 收藏夹列表（收藏弹层用） */
    private val _favoriteDirectories = MutableStateFlow<List<FavoriteDirectory>>(emptyList())
    val favoriteDirectories: StateFlow<List<FavoriteDirectory>> = _favoriteDirectories.asStateFlow()

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    /** 当前歌曲 id，用于去重 */
    val currentMusicId: StateFlow<Long?> = player.currentMusic
        .map { it?.id }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    init {
        // 切歌时：写入播放记录 + 刷新收藏状态
        viewModelScope.launch {
            player.currentMusic.collect { music ->
                music ?: return@collect
                refreshFavoriteStatus(music.id)
                musicRepository.insertMusicRecord(music.id)
            }
        }
        // 播放器错误提示
        viewModelScope.launch {
            player.messages.collect { _toastMessage.value = it }
        }
    }

    // ============ 播放控制 ============
    fun togglePlay() = player.togglePlay()

    fun next() = player.next()

    fun prev() = player.prev()

    fun seekTo(positionMs: Int) = player.seekTo(positionMs)

    fun setLoopMode(mode: LoopMode) = player.setLoopMode(mode)

    /** 播放列表点击某首切歌 */
    fun playAt(index: Int) = player.playAt(index)

    /** 从播放列表移除某首 */
    fun removeFromPlaylist(index: Int) = player.removeAt(index)

    /** 清空播放列表 */
    fun clearPlaylist() = player.clearPlaylist()

    // ============ 喜欢（红心） ============
    /**
     * 收藏红心：isLike == 0 时点赞，否则取消
     */
    fun toggleLike() {
        val music = currentMusic.value ?: return
        viewModelScope.launch {
            val result = if (music.isLike == 0) {
                musicRepository.likeMusic(music.id)
            } else {
                musicRepository.unlikeMusic(music.id)
            }
            if (result.isSuccess) {
                val newValue = if (music.isLike == 0) 1 else 0
                player.updateLike(music.id, newValue)
                _toastMessage.value = if (newValue == 1) "已添加到喜欢的歌曲" else "已取消喜欢"
            } else {
                _toastMessage.value = result.exceptionOrNull()?.message ?: "操作失败"
            }
        }
    }

    // ============ 收藏夹 ============
    /**
     * 查询当前歌曲是否已收藏到收藏夹
     */
    fun refreshFavoriteStatus(musicId: Long) {
        viewModelScope.launch {
            val result = musicRepository.isMusicFavorite(musicId)
            _isFavorite.value = (result.getOrNull() ?: 0) > 0
        }
    }

    /**
     * 加载收藏夹列表（带 checked 状态）
     */
    fun loadFavoriteDirectories() {
        val musicId = currentMusic.value?.id ?: return
        viewModelScope.launch {
            val result = musicRepository.getFavoriteDirectories(musicId)
            if (result.isSuccess) {
                _favoriteDirectories.value = result.getOrNull() ?: emptyList()
            } else {
                _toastMessage.value = result.exceptionOrNull()?.message ?: "获取收藏夹失败"
            }
        }
    }

    /**
     * 新建收藏夹，成功后重新加载列表
     */
    fun createFavoriteDirectory(name: String) {
        if (name.isBlank()) {
            _toastMessage.value = "收藏夹名称不能为空"
            return
        }
        viewModelScope.launch {
            val result = musicRepository.createFavoriteDirectory(name.trim())
            if (result.isSuccess) {
                _toastMessage.value = "创建成功"
                loadFavoriteDirectories()
            } else {
                _toastMessage.value = result.exceptionOrNull()?.message ?: "创建收藏夹失败"
            }
        }
    }

    /**
     * 保存收藏夹勾选结果
     */
    fun saveFavoriteDirectories(favoriteIds: List<Long>, onSuccess: () -> Unit) {
        val musicId = currentMusic.value?.id ?: return
        viewModelScope.launch {
            val result = musicRepository.saveMusicFavorite(musicId, favoriteIds)
            if (result.isSuccess) {
                _isFavorite.value = favoriteIds.isNotEmpty()
                _toastMessage.value = "收藏成功"
                onSuccess()
            } else {
                _toastMessage.value = result.exceptionOrNull()?.message ?: "收藏失败"
            }
        }
    }

    // ============ 评论 ============
    fun loadComments() {
        val musicId = currentMusic.value?.id ?: return
        viewModelScope.launch {
            val result = musicRepository.getTopComments(musicId, pageNum = 1, pageSize = 20)
            if (result.isSuccess) {
                val page = result.getOrNull()
                _comments.value = page?.list ?: emptyList()
                _commentTotal.value = page?.total ?: 0
            } else {
                _toastMessage.value = result.exceptionOrNull()?.message ?: "获取评论失败"
            }
        }
    }

    fun sendComment(content: String) {
        if (content.isBlank()) {
            _toastMessage.value = "评论内容不能为空"
            return
        }
        val musicId = currentMusic.value?.id ?: return
        viewModelScope.launch {
            val result = musicRepository.insertMusicComment(musicId, content.trim())
            if (result.isSuccess) {
                _toastMessage.value = "评论成功"
                loadComments()
            } else {
                _toastMessage.value = result.exceptionOrNull()?.message ?: "评论失败"
            }
        }
    }

    fun clearToast() {
        _toastMessage.value = null
    }
}
