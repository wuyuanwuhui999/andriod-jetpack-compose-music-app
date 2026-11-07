package com.player.music.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.player.music.model.Music
import com.player.music.repository.MusicRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 选择音乐页 ViewModel（分享到音乐圈时挑一首歌）
 * 对应 uni-app 端 pages/MusicCirCleCheckPage.vue：关键词搜索音乐，点一条即选中返回
 */
@HiltViewModel
class MusicPickViewModel @Inject constructor(
    private val musicRepository: MusicRepository
) : ViewModel() {

    companion object {
        /** 每页条数，与 uni-app 端 PAGE_SIZE 一致 */
        private const val PAGE_SIZE = 20
    }

    private val gson = com.google.gson.Gson()

    /** 选中歌曲序列化（用于回传给分享页） */
    fun toJson(music: Music): String = gson.toJson(music)

    /** 输入框内容 */
    private val _input = MutableStateFlow("")
    val input: StateFlow<String> = _input.asStateFlow()

    private val _results = MutableStateFlow<List<Music>>(emptyList())
    val results: StateFlow<List<Music>> = _results.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    /** 是否已经搜索过（用于区分「未搜索」和「无结果」） */
    private val _hasSearched = MutableStateFlow(false)
    val hasSearched: StateFlow<Boolean> = _hasSearched.asStateFlow()

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    fun onInputChange(value: String) {
        _input.value = value
    }

    /** 搜索音乐（关键词为空时提示，不请求接口） */
    fun search() {
        val keyword = _input.value.trim()
        if (keyword.isEmpty()) {
            _toastMessage.value = "请输入歌曲或者歌手名称"
            return
        }
        if (_isSearching.value) return
        _isSearching.value = true
        viewModelScope.launch {
            val result = musicRepository.searchMusic(keyword, pageNum = 1, pageSize = PAGE_SIZE)
            if (result.isSuccess) {
                _results.value = result.getOrNull()?.list ?: emptyList()
            } else {
                _toastMessage.value = result.exceptionOrNull()?.message ?: "搜索失败"
            }
            _hasSearched.value = true
            _isSearching.value = false
        }
    }

    fun clearToast() {
        _toastMessage.value = null
    }
}
