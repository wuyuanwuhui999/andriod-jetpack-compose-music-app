package com.player.music.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.player.music.model.Music
import com.player.music.model.MusicCategory
import com.player.music.player.MusicPlayerManager
import com.player.music.repository.MusicRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 歌曲分类页 ViewModel
 * 对应 Flutter 端 lib/pages/MusicCategoryPage.dart：
 * 加载全部分类 → 默认展示前 9 个 + 展开/收起 → 按分类分页加载歌曲 → 点赞/播放
 */
@HiltViewModel
class MusicCategoryViewModel @Inject constructor(
    private val musicRepository: MusicRepository,
    val player: MusicPlayerManager
) : ViewModel() {

    companion object {
        /** 每页条数（与 Flutter 端 PAGE_SIZE 一致） */
        private const val PAGE_SIZE = 20

        /** 收起时展示的分类个数 */
        const val COLLAPSED_COUNT = 9
    }

    /** 全部歌曲分类 */
    private val _categories = MutableStateFlow<List<MusicCategory>>(emptyList())
    val categories: StateFlow<List<MusicCategory>> = _categories.asStateFlow()

    /** 是否展开全部分类 */
    private val _expanded = MutableStateFlow(false)
    val expanded: StateFlow<Boolean> = _expanded.asStateFlow()

    /** 当前选中的分类下标（下标相对全部分类） */
    private val _activeIndex = MutableStateFlow(0)
    val activeIndex: StateFlow<Int> = _activeIndex.asStateFlow()

    /** 当前分类下的歌曲 */
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
        loadCategories()
    }

    private fun loadCategories() {
        viewModelScope.launch {
            val result = musicRepository.getMusicCategory()
            if (result.isSuccess) {
                _categories.value = result.getOrNull() ?: emptyList()
                if (_categories.value.isEmpty()) {
                    _isLoading.value = false
                } else {
                    loadSongs(reset = true)
                }
            } else {
                _isLoading.value = false
                _toastMessage.value = result.exceptionOrNull()?.message ?: "获取歌曲分类失败"
            }
        }
    }

    /** 展开 / 收起分类 */
    fun toggleExpand() {
        _expanded.value = !_expanded.value
    }

    /** 切换分类：重置分页并重新加载 */
    fun selectCategory(index: Int) {
        if (index !in _categories.value.indices) return
        if (index == _activeIndex.value && _songs.value.isNotEmpty()) return
        _activeIndex.value = index
        loadSongs(reset = true)
    }

    /** 上拉加载下一页 */
    fun loadMore() {
        if (_isLoading.value || _isLoadingMore.value || _reachedEnd.value) return
        if (_songs.value.isEmpty()) return
        loadSongs(reset = false)
    }

    private fun loadSongs(reset: Boolean) {
        val category = _categories.value.getOrNull(_activeIndex.value) ?: return
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
                categoryId = category.id,
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
