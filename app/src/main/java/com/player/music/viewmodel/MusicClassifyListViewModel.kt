package com.player.music.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.player.music.model.Music
import com.player.music.player.MusicPlayerManager
import com.player.music.repository.MusicRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 分类歌曲列表页 ViewModel（首页各分类模块点「更多」进入）
 * 对应 Flutter 端 lib/pages/MusicClassifyListPage.dart：
 * 按分类 id 分页加载歌曲，点赞/取消点赞，点击播放整页列表
 */
@HiltViewModel
class MusicClassifyListViewModel @Inject constructor(
    private val musicRepository: MusicRepository,
    val player: MusicPlayerManager,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    companion object {
        private const val PAGE_SIZE = 20
    }

    /** 分类 id 与名称由路由参数传入 */
    private val classifyId: Int = savedStateHandle.get<String>("classifyId")?.toIntOrNull() ?: 0
    val classifyName: String = savedStateHandle.get<String>("classifyName") ?: "分类歌曲"

    private val _songs = MutableStateFlow<List<Music>>(emptyList())
    val songs: StateFlow<List<Music>> = _songs.asStateFlow()

    private val _total = MutableStateFlow(0)
    val total: StateFlow<Int> = _total.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isLoadingMore = MutableStateFlow(false)
    val isLoadingMore: StateFlow<Boolean> = _isLoadingMore.asStateFlow()

    private val _reachedEnd = MutableStateFlow(false)
    val reachedEnd: StateFlow<Boolean> = _reachedEnd.asStateFlow()

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    init {
        loadSongs(reset = true)
    }

    /** 上拉加载下一页 */
    fun loadMore() {
        if (_isLoading.value || _isLoadingMore.value || _reachedEnd.value) return
        if (_songs.value.isEmpty()) return
        loadSongs(reset = false)
    }

    private fun loadSongs(reset: Boolean) {
        if (classifyId <= 0) {
            _isLoading.value = false
            _reachedEnd.value = true
            return
        }
        val pageNum = if (reset) 1 else _songs.value.size / PAGE_SIZE + 1

        if (reset) {
            _isLoading.value = true
            _reachedEnd.value = false
            _songs.value = emptyList()
        } else {
            _isLoadingMore.value = true
        }

        viewModelScope.launch {
            val result = musicRepository.getMusicPageByCategoryId(
                categoryId = classifyId,
                pageNum = pageNum,
                pageSize = PAGE_SIZE
            )
            if (result.isSuccess) {
                val page = result.getOrNull()
                val list = page?.list ?: emptyList()
                val existIds = _songs.value.map { it.id }.toSet()
                val newSongs = if (reset) list else list.filterNot { existIds.contains(it.id) }
                _songs.value = if (reset) newSongs else _songs.value + newSongs
                _total.value = page?.total ?: 0
                _reachedEnd.value = newSongs.isEmpty() ||
                    (_total.value > 0 && _songs.value.size >= _total.value)
            } else {
                _toastMessage.value = result.exceptionOrNull()?.message ?: "获取歌曲列表失败"
                if (!reset) _reachedEnd.value = true
            }
            _isLoading.value = false
            _isLoadingMore.value = false
        }
    }

    /** 点赞 / 取消点赞：先本地切换，失败回滚 */
    fun toggleLike(music: Music) {
        val targetLike = if (music.isLike == 1) 0 else 1
        updateLikeState(music.id, targetLike)

        viewModelScope.launch {
            val result = if (targetLike == 1) {
                musicRepository.likeMusic(music.id)
            } else {
                musicRepository.unlikeMusic(music.id)
            }
            val rows = result.getOrNull() ?: 0
            if (result.isFailure || rows <= 0) {
                updateLikeState(music.id, music.isLike)
                _toastMessage.value = result.exceptionOrNull()?.message
                    ?: if (targetLike == 1) "点赞失败" else "取消点赞失败"
            }
        }
    }

    private fun updateLikeState(musicId: Long, isLike: Int) {
        _songs.value = _songs.value.map { item ->
            if (item.id == musicId) item.copy(isLike = isLike) else item
        }
    }

    /** 播放当前分类的歌曲列表，从第 index 首开始 */
    fun play(index: Int) {
        player.playList(_songs.value, index)
    }

    fun clearToast() {
        _toastMessage.value = null
    }
}
