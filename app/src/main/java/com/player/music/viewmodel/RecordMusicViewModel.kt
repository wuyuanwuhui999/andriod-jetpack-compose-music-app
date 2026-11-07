package com.player.music.viewmodel

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
 * 播放记录页 ViewModel（我的页「播放记录」更多进入）
 * 对应 Flutter 端 lib/pages/RecordMusicPage.dart：
 * 分页加载最近听过的歌曲（按音乐去重，含播放次数 times），点击播放
 */
@HiltViewModel
class RecordMusicViewModel @Inject constructor(
    private val musicRepository: MusicRepository,
    val player: MusicPlayerManager
) : ViewModel() {

    companion object {
        /** 与 Flutter 端 PAGE_SIZE 保持一致 */
        private const val PAGE_SIZE = 20
    }

    private val _songs = MutableStateFlow<List<Music>>(emptyList())
    val songs: StateFlow<List<Music>> = _songs.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isLoadingMore = MutableStateFlow(false)
    val isLoadingMore: StateFlow<Boolean> = _isLoadingMore.asStateFlow()

    private val _reachedEnd = MutableStateFlow(false)
    val reachedEnd: StateFlow<Boolean> = _reachedEnd.asStateFlow()

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    init {
        loadRecords(reset = true)
    }

    /** 上拉加载下一页 */
    fun loadMore() {
        if (_isLoading.value || _isLoadingMore.value || _reachedEnd.value) return
        if (_songs.value.isEmpty()) return
        loadRecords(reset = false)
    }

    private fun loadRecords(reset: Boolean) {
        val pageNum = if (reset) 1 else _songs.value.size / PAGE_SIZE + 1

        if (reset) {
            _isLoading.value = true
            _reachedEnd.value = false
            _songs.value = emptyList()
        } else {
            _isLoadingMore.value = true
        }

        viewModelScope.launch {
            val result = musicRepository.getMusicRecord(pageNum, PAGE_SIZE)
            if (result.isSuccess) {
                val page = result.getOrNull()
                val list = page?.list ?: emptyList()
                val existIds = _songs.value.map { it.id }.toSet()
                val newSongs = if (reset) list else list.filterNot { existIds.contains(it.id) }
                _songs.value = if (reset) newSongs else _songs.value + newSongs
                val total = page?.total ?: 0
                _reachedEnd.value = newSongs.isEmpty() ||
                    (total > 0 && _songs.value.size >= total)
            } else {
                _toastMessage.value = result.exceptionOrNull()?.message ?: "获取播放记录失败"
                if (!reset) _reachedEnd.value = true
            }
            _isLoading.value = false
            _isLoadingMore.value = false
        }
    }

    /** 播放：整个播放记录作为播放列表，从第 index 首开始 */
    fun play(index: Int) {
        player.playList(_songs.value, index)
    }

    fun clearToast() {
        _toastMessage.value = null
    }
}
