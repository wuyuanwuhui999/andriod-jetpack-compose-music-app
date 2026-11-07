package com.player.music.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.player.music.model.Music
import com.player.music.player.MusicPlayerManager
import com.player.music.repository.MusicRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 歌手详情页 ViewModel
 * 对应 Flutter 端 MusicAuthorCategoryPage / MusicAuthorListPage：
 * 歌手信息 + 关注/取消关注 + 该歌手全部歌曲
 */
@HiltViewModel
class MusicAuthorViewModel @Inject constructor(
    private val musicRepository: MusicRepository,
    val player: MusicPlayerManager
) : ViewModel() {

    private val _songs = MutableStateFlow<List<Music>>(emptyList())
    val songs: StateFlow<List<Music>> = _songs.asStateFlow()

    /** 是否已关注该歌手 */
    private val _followed = MutableStateFlow(false)
    val followed: StateFlow<Boolean> = _followed.asStateFlow()

    private val _loading = MutableStateFlow(true)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    /** 已加载的歌手 authorId，避免重复请求 */
    private var loadedAuthorId: Long = -1L

    /**
     * 加载歌手歌曲与关注状态（同一个歌手只加载一次）
     */
    fun load(authorId: Long) {
        if (authorId <= 0 || loadedAuthorId == authorId) return
        loadedAuthorId = authorId
        _loading.value = true

        viewModelScope.launch {
            val songsDeferred = async {
                // 一次取足够多的歌曲，保证页面上的"N首歌曲"与实际数量一致
                musicRepository.getMusicListByAuthorId(authorId, pageNum = 1, pageSize = 200)
            }
            val favoritesDeferred = async { musicRepository.getFavoriteAuthors() }

            val songsResult = songsDeferred.await()
            if (songsResult.isSuccess) {
                _songs.value = songsResult.getOrNull() ?: emptyList()
            } else {
                _toastMessage.value = songsResult.exceptionOrNull()?.message ?: "获取歌手歌曲失败"
            }

            val favoritesResult = favoritesDeferred.await()
            if (favoritesResult.isSuccess) {
                _followed.value =
                    favoritesResult.getOrNull()?.any { it.authorId == authorId } == true
            }

            _loading.value = false
        }
    }

    /**
     * 关注 / 取消关注歌手
     */
    fun toggleFollow(authorId: Long) {
        if (authorId <= 0) return
        viewModelScope.launch {
            val followed = _followed.value
            val result = if (followed) {
                musicRepository.deleteFavoriteAuthor(authorId)
            } else {
                musicRepository.followAuthor(authorId)
            }
            if (result.isSuccess) {
                _followed.value = !followed
                _toastMessage.value = if (followed) "已取消关注" else "关注成功"
            } else {
                _toastMessage.value = result.exceptionOrNull()?.message ?: "操作失败"
            }
        }
    }

    /**
     * 播放该歌手的第 index 首（整张列表作为播放列表）
     */
    fun playSong(index: Int) {
        val list = _songs.value
        if (list.isEmpty()) return
        player.playList(list, index.coerceIn(0, list.size - 1))
    }

    fun clearToast() {
        _toastMessage.value = null
    }
}
