package com.player.music.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.player.music.model.MusicAuthor
import com.player.music.model.MusicAuthorCategory
import com.player.music.repository.MusicRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 歌手分类页 ViewModel
 * 对应 Flutter 端 lib/pages/MusicAuthorCategoryPage.dart：
 * 加载歌手分类 → 按分类分页加载歌手 → 关注/取消关注歌手
 */
@HiltViewModel
class MusicAuthorCategoryViewModel @Inject constructor(
    private val musicRepository: MusicRepository
) : ViewModel() {

    companion object {
        /** 每页条数，与 Flutter 端 PAGE_SIZE 保持一致 */
        private const val PAGE_SIZE = 10
    }

    /** 歌手分类列表 */
    private val _categories = MutableStateFlow<List<MusicAuthorCategory>>(emptyList())
    val categories: StateFlow<List<MusicAuthorCategory>> = _categories.asStateFlow()

    /** 当前选中的分类下标 */
    private val _activeIndex = MutableStateFlow(0)
    val activeIndex: StateFlow<Int> = _activeIndex.asStateFlow()

    /** 当前分类下的歌手列表 */
    private val _authors = MutableStateFlow<List<MusicAuthor>>(emptyList())
    val authors: StateFlow<List<MusicAuthor>> = _authors.asStateFlow()

    private val _isInitialLoading = MutableStateFlow(true)
    val isInitialLoading: StateFlow<Boolean> = _isInitialLoading.asStateFlow()

    private val _isLoadingMore = MutableStateFlow(false)
    val isLoadingMore: StateFlow<Boolean> = _isLoadingMore.asStateFlow()

    private val _reachedEnd = MutableStateFlow(false)
    val reachedEnd: StateFlow<Boolean> = _reachedEnd.asStateFlow()

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    /** 已请求到第几页 */
    private var pageNum = 1

    init {
        loadCategories()
    }

    /**
     * 加载歌手分类，成功后自动加载第一个分类的歌手
     */
    private fun loadCategories() {
        viewModelScope.launch {
            val result = musicRepository.getMusicAuthorCategories()
            if (result.isSuccess) {
                _categories.value = result.getOrNull() ?: emptyList()
                if (_categories.value.isEmpty()) {
                    _isInitialLoading.value = false
                } else {
                    loadAuthors(reset = true)
                }
            } else {
                _isInitialLoading.value = false
                _toastMessage.value = result.exceptionOrNull()?.message ?: "获取歌手分类失败"
            }
        }
    }

    /**
     * 切换分类：重置分页并重新加载
     */
    fun selectCategory(index: Int) {
        if (index !in _categories.value.indices) return
        if (index == _activeIndex.value && _authors.value.isNotEmpty()) return
        _activeIndex.value = index
        loadAuthors(reset = true)
    }

    /**
     * 上拉加载下一页
     */
    fun loadMore() {
        if (_reachedEnd.value || _isLoadingMore.value || _isInitialLoading.value) return
        if (_authors.value.isEmpty()) return
        loadAuthors(reset = false)
    }

    /**
     * 加载歌手列表
     * @param reset true 表示切换分类后的首页加载
     */
    private fun loadAuthors(reset: Boolean) {
        val category = _categories.value.getOrNull(_activeIndex.value) ?: return
        if (reset) {
            pageNum = 1
            _reachedEnd.value = false
            _isInitialLoading.value = true
            _authors.value = emptyList()
        } else {
            _isLoadingMore.value = true
        }

        viewModelScope.launch {
            val result = musicRepository.getMusicAuthorListByCategoryId(
                categoryId = category.id,
                pageNum = pageNum,
                pageSize = PAGE_SIZE
            )
            if (result.isSuccess) {
                val list = result.getOrNull() ?: emptyList()
                _authors.value = if (reset) list else _authors.value + list
                // 返回条数不足一页，说明已经是最后一页
                _reachedEnd.value = list.size < PAGE_SIZE
                if (!_reachedEnd.value) pageNum += 1
            } else {
                _toastMessage.value = result.exceptionOrNull()?.message ?: "获取歌手列表失败"
                _reachedEnd.value = true
            }
            _isInitialLoading.value = false
            _isLoadingMore.value = false
        }
    }

    /**
     * 关注 / 取消关注歌手（列表内即时更新红心状态）
     */
    fun toggleFollow(author: MusicAuthor) {
        if (author.authorId <= 0) {
            _toastMessage.value = "暂无歌手信息"
            return
        }
        viewModelScope.launch {
            val followed = author.isLike == 1
            val result = if (followed) {
                musicRepository.deleteFavoriteAuthor(author.authorId)
            } else {
                musicRepository.followAuthor(author.authorId)
            }
            if (result.isSuccess) {
                val newValue = if (followed) 0 else 1
                _authors.value = _authors.value.map {
                    if (it.id == author.id) it.copy(isLike = newValue) else it
                }
                _toastMessage.value = if (newValue == 1) "关注成功" else "已取消关注"
            } else {
                _toastMessage.value = result.exceptionOrNull()?.message ?: "操作失败"
            }
        }
    }

    fun clearToast() {
        _toastMessage.value = null
    }
}
