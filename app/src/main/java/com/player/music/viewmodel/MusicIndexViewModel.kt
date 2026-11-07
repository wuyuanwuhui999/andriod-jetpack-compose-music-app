package com.player.music.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.player.music.model.Music
import com.player.music.model.MusicAuthor
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
 * 音乐首页 ViewModel
 * 负责：搜索框推荐歌曲、分类列表、分类下的歌曲/歌手、上拉加载更多分类
 */
@HiltViewModel
class MusicIndexViewModel @Inject constructor(
    private val musicRepository: MusicRepository,
    val player: MusicPlayerManager
) : ViewModel() {

    companion object {
        /** 首屏展示的分类数量（与 Flutter 端一致） */
        private const val FIRST_PAGE_CATEGORY_COUNT = 4

        /** 首页每个分类展示的歌曲数量 */
        private const val MUSIC_COUNT_PER_CATEGORY = 3

        /** 推荐歌手模块的分类名称 */
        private const val CATEGORY_RECOMMEND_AUTHOR = "推荐歌手"
    }

    // 搜索框推荐关键字
    private val _keyword = MutableStateFlow("")
    val keyword: StateFlow<String> = _keyword.asStateFlow()

    // 搜索框推荐歌曲（用于首页迷你播放器展示封面）
    private val _recommendMusic = MutableStateFlow<Music?>(null)
    val recommendMusic: StateFlow<Music?> = _recommendMusic.asStateFlow()

    // 全部音乐分类
    private val _categories = MutableStateFlow<List<MusicCategory>>(emptyList())
    val categories: StateFlow<List<MusicCategory>> = _categories.asStateFlow()

    // 当前展示的分类数量（上拉加载时递增）
    private val _visibleCount = MutableStateFlow(FIRST_PAGE_CATEGORY_COUNT)
    val visibleCount: StateFlow<Int> = _visibleCount.asStateFlow()

    // 分类 -> 歌曲列表
    private val _songsByCategory = MutableStateFlow<Map<Int, List<Music>>>(emptyMap())
    val songsByCategory: StateFlow<Map<Int, List<Music>>> = _songsByCategory.asStateFlow()

    // 推荐歌手列表
    private val _authors = MutableStateFlow<List<MusicAuthor>>(emptyList())
    val authors: StateFlow<List<MusicAuthor>> = _authors.asStateFlow()

    // 歌曲列表是否为空（分类下没有歌曲）
    private val _emptyCategories = MutableStateFlow<Set<Int>>(emptySet())
    val emptyCategories: StateFlow<Set<Int>> = _emptyCategories.asStateFlow()

    private val _isInitialLoading = MutableStateFlow(true)
    val isInitialLoading: StateFlow<Boolean> = _isInitialLoading.asStateFlow()

    private val _isLoadingMore = MutableStateFlow(false)
    val isLoadingMore: StateFlow<Boolean> = _isLoadingMore.asStateFlow()

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    /** 正在请求中的分类，避免重复请求 */
    private val loadingCategoryIds = mutableSetOf<Int>()

    init {
        loadInitialData()
    }

    /**
     * 首屏加载：推荐歌曲 + 分类列表
     */
    fun loadInitialData() {
        viewModelScope.launch {
            _isInitialLoading.value = true
            loadKeywordMusic()
            loadCategories()
            _isInitialLoading.value = false
        }
    }

    /**
     * 加载搜索框推荐歌曲，并拼接成 "歌手 - 歌名" 作为搜索框提示
     */
    private suspend fun loadKeywordMusic() {
        val result = musicRepository.getKeywordMusic()
        if (result.isSuccess) {
            val music = result.getOrNull()
            _recommendMusic.value = music
            _keyword.value = if (music != null) {
                "${music.authorName ?: ""} - ${music.songName ?: ""}"
            } else {
                ""
            }
        }
    }

    /**
     * 加载全部音乐分类
     */
    private suspend fun loadCategories() {
        val result = musicRepository.getMusicCategory()
        if (result.isSuccess) {
            _categories.value = result.getOrNull() ?: emptyList()
            if (_categories.value.size < FIRST_PAGE_CATEGORY_COUNT) {
                _visibleCount.value = _categories.value.size
            }
        } else {
            _toastMessage.value = result.exceptionOrNull()?.message ?: "获取音乐分类失败"
        }
    }

    /**
     * 确保某个分类的歌曲已加载（已加载过则直接返回缓存）
     * @param category 分类
     */
    fun ensureCategoryLoaded(category: MusicCategory) {
        val categoryId = category.id
        if (_songsByCategory.value.containsKey(categoryId)) return
        if (loadingCategoryIds.contains(categoryId)) return

        loadingCategoryIds.add(categoryId)
        viewModelScope.launch {
            try {
                if (category.displayName == CATEGORY_RECOMMEND_AUTHOR) {
                    loadAuthors()
                } else {
                    val result = musicRepository.getMusicListByCategoryId(
                        categoryId = categoryId,
                        pageNum = 1,
                        pageSize = MUSIC_COUNT_PER_CATEGORY
                    )
                    if (result.isSuccess) {
                        val songs = result.getOrNull() ?: emptyList()
                        _songsByCategory.value = _songsByCategory.value + (categoryId to songs)
                        if (songs.isEmpty()) {
                            _emptyCategories.value = _emptyCategories.value + categoryId
                        }
                    } else {
                        _toastMessage.value = result.exceptionOrNull()?.message ?: "获取分类音乐失败"
                    }
                }
            } finally {
                loadingCategoryIds.remove(categoryId)
            }
        }
    }

    /**
     * 加载推荐歌手（首页"推荐歌手"模块）
     */
    private suspend fun loadAuthors() {
        if (_authors.value.isNotEmpty()) return
        val result = musicRepository.getMusicAuthorListByCategoryId(
            categoryId = 0,
            pageNum = 1,
            pageSize = 5
        )
        if (result.isSuccess) {
            _authors.value = result.getOrNull() ?: emptyList()
        }
    }

    /**
     * 上拉加载更多分类（每次追加一个，与 Flutter 端一致）
     * @return true 表示还有更多，false 表示已全部加载
     */
    fun loadMoreCategories(): Boolean {
        val total = _categories.value.size
        if (_visibleCount.value >= total) return false
        if (_isLoadingMore.value) return true

        viewModelScope.launch {
            _isLoadingMore.value = true
            _visibleCount.value = (_visibleCount.value + 1).coerceAtMost(total)
            _isLoadingMore.value = false
        }
        return _visibleCount.value < total
    }

    /** 是否还有更多分类未展示 */
    fun hasMoreCategories(): Boolean = _visibleCount.value < _categories.value.size

    /**
     * 播放某个分类下的第 index 首歌曲（首页分类模块点击时调用）
     */
    fun playCategorySong(categoryId: Int, index: Int) {
        val songs = _songsByCategory.value[categoryId].orEmpty()
        if (songs.isEmpty()) return
        player.playList(songs, index.coerceIn(0, songs.size - 1))
    }

    fun clearToast() {
        _toastMessage.value = null
    }
}
