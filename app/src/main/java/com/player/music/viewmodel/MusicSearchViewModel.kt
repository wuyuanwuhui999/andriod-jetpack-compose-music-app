package com.player.music.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.player.music.local.DataStoreManager
import com.player.music.local.PreferenceKeys
import com.player.music.model.Music
import com.player.music.player.MusicPlayerManager
import com.player.music.repository.MusicRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 音乐搜索页 ViewModel
 * 对应 Flutter 端 lib/pages/MusicSearchPage.dart：
 * 关键词搜索（歌名/歌手/专辑模糊匹配）→ 分页上拉加载更多 → 点赞/取消点赞 → 播放
 * 历史搜索记录保存在 DataStore（与 Flutter 端 SharedPreferences 的 historyMusicLabels 等价）
 */
@HiltViewModel
class MusicSearchViewModel @Inject constructor(
    private val musicRepository: MusicRepository,
    private val dataStoreManager: DataStoreManager,
    val player: MusicPlayerManager
) : ViewModel() {

    companion object {
        /** 每页条数（与 Flutter 端 pageSize = 20 一致） */
        private const val PAGE_SIZE = 20

        /** 历史搜索最多保留的条数（与 Flutter 端一致） */
        private const val MAX_HISTORY = 20
    }

    /** 输入框内容 */
    private val _input = MutableStateFlow("")
    val input: StateFlow<String> = _input.asStateFlow()

    /** 已提交的搜索关键词 */
    private val _keyword = MutableStateFlow("")
    val keyword: StateFlow<String> = _keyword.asStateFlow()

    /** 搜索结果 */
    private val _results = MutableStateFlow<List<Music>>(emptyList())
    val results: StateFlow<List<Music>> = _results.asStateFlow()

    private val _total = MutableStateFlow(0)
    val total: StateFlow<Int> = _total.asStateFlow()

    /** 首屏搜索中 */
    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    /** 上拉加载中 */
    private val _isLoadingMore = MutableStateFlow(false)
    val isLoadingMore: StateFlow<Boolean> = _isLoadingMore.asStateFlow()

    private val _reachedEnd = MutableStateFlow(false)
    val reachedEnd: StateFlow<Boolean> = _reachedEnd.asStateFlow()

    /** 是否已经执行过搜索（true 展示结果列表，false 展示历史搜索） */
    private val _hasSearched = MutableStateFlow(false)
    val hasSearched: StateFlow<Boolean> = _hasSearched.asStateFlow()

    /** 历史搜索关键词 */
    private val _history = MutableStateFlow<List<String>>(emptyList())
    val history: StateFlow<List<String>> = _history.asStateFlow()

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    init {
        loadHistory()
    }

    /** 输入框文字变化 */
    fun onInputChange(text: String) {
        _input.value = text
    }

    /** 清空输入框：回到历史搜索状态（与 Flutter 端清除按钮一致） */
    fun clearInput() {
        _input.value = ""
        _keyword.value = ""
        _results.value = emptyList()
        _total.value = 0
        _reachedEnd.value = false
        _hasSearched.value = false
    }

    /**
     * 执行搜索
     * @param text 搜索词，默认取输入框内容（首页带入的关键词、历史标签点击都走这里）
     */
    fun search(text: String = _input.value) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) {
            _toastMessage.value = "请输入搜索内容"
            return
        }
        if (_isSearching.value) return

        _input.value = trimmed
        _keyword.value = trimmed
        saveHistory(trimmed)
        _hasSearched.value = true
        _results.value = emptyList()
        _total.value = 0
        _reachedEnd.value = false
        requestPage(pageNum = 1, reset = true)
    }

    /** 上拉加载下一页 */
    fun loadMore() {
        if (_isSearching.value || _isLoadingMore.value || _reachedEnd.value) return
        if (_results.value.isEmpty()) return
        val nextPage = _results.value.size / PAGE_SIZE + 1
        requestPage(pageNum = nextPage, reset = false)
    }

    private fun requestPage(pageNum: Int, reset: Boolean) {
        if (reset) _isSearching.value = true else _isLoadingMore.value = true
        viewModelScope.launch {
            val result = musicRepository.searchMusic(
                keyword = _keyword.value,
                pageNum = pageNum,
                pageSize = PAGE_SIZE
            )
            if (result.isSuccess) {
                val page = result.getOrNull()
                val list = page?.list ?: emptyList()
                val existIds = _results.value.map { it.id }.toSet()
                val newSongs = if (reset) list else list.filterNot { existIds.contains(it.id) }
                _results.value = if (reset) newSongs else _results.value + newSongs
                _total.value = page?.total ?: 0
                _reachedEnd.value = newSongs.isEmpty() ||
                    (_total.value > 0 && _results.value.size >= _total.value)
            } else {
                _toastMessage.value = result.exceptionOrNull()?.message ?: "搜索失败"
                if (!reset) _reachedEnd.value = true
            }
            _isSearching.value = false
            _isLoadingMore.value = false
        }
    }

    /**
     * 点赞 / 取消点赞：先本地切换保证交互即时，接口失败再回滚并提示
     */
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
        _results.value = _results.value.map { item ->
            if (item.id == musicId) item.copy(isLike = isLike) else item
        }
    }

    /** 播放搜索结果第 index 首（整页作为播放列表） */
    fun play(index: Int) {
        player.playList(_results.value, index)
    }

    private fun loadHistory() {
        viewModelScope.launch {
            val joined = dataStoreManager.getString(PreferenceKeys.SEARCH_HISTORY).firstOrNull()
            _history.value = joined
                ?.split(",")
                ?.filter { it.isNotBlank() }
                ?.take(MAX_HISTORY)
                ?: emptyList()
        }
    }

    /** 搜索词插到最前面，去重后最多保留 MAX_HISTORY 条 */
    private fun saveHistory(keyword: String) {
        val updated = (listOf(keyword) + _history.value.filter { it != keyword }).take(MAX_HISTORY)
        _history.value = updated
        viewModelScope.launch {
            dataStoreManager.saveString(PreferenceKeys.SEARCH_HISTORY, updated.joinToString(","))
        }
    }

    fun clearToast() {
        _toastMessage.value = null
    }
}
