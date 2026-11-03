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
 * 推荐页 ViewModel
 * 对应 Flutter 端 lib/pages/MusicRecommentPage.dart：
 * 取第一个可用分类的歌曲按榜单顺序展示，支持上拉分页、点赞/取消点赞
 */
@HiltViewModel
class MusicRecommendViewModel @Inject constructor(
    private val musicRepository: MusicRepository
) : ViewModel() {

    companion object {
        /** 分类接口不可用时的兜底分类（酷狗飙升榜） */
        private const val FALLBACK_CATEGORY_ID = 68

        /** 首屏加载条数（与 Flutter 端一致：20 条） */
        private const val FIRST_PAGE_SIZE = 20

        /** 每次上拉加载条数 */
        private const val PAGE_SIZE = 10
    }

    // 推荐歌曲列表
    private val _musicList = MutableStateFlow<List<Music>>(emptyList())
    val musicList: StateFlow<List<Music>> = _musicList.asStateFlow()

    // 总条数
    private val _total = MutableStateFlow(0)
    val total: StateFlow<Int> = _total.asStateFlow()

    // 首屏加载中
    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // 上拉加载中
    private val _isLoadingMore = MutableStateFlow(false)
    val isLoadingMore: StateFlow<Boolean> = _isLoadingMore.asStateFlow()

    // 是否已加载完全部数据
    private val _reachedEnd = MutableStateFlow(false)
    val reachedEnd: StateFlow<Boolean> = _reachedEnd.asStateFlow()

    // 统一 Toast 提示
    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    /** 实际使用的分类ID */
    private var categoryId = FALLBACK_CATEGORY_ID

    /** 首次进入是否已解析过分类 */
    private var categoryResolved = false

    init {
        loadInitialData()
    }

    /**
     * 首屏加载：先解析出可用分类，再拉取第一页（20 条）
     */
    fun loadInitialData() {
        viewModelScope.launch {
            _isLoading.value = true
            resolveCategoryId()

            val result = musicRepository.getMusicPageByCategoryId(
                categoryId = categoryId,
                pageNum = 1,
                pageSize = FIRST_PAGE_SIZE
            )
            if (result.isSuccess) {
                val page = result.getOrNull()
                _musicList.value = page?.list ?: emptyList()
                _total.value = page?.total ?: 0
                _reachedEnd.value = _total.value > 0 && _musicList.value.size >= _total.value
            } else {
                _toastMessage.value = result.exceptionOrNull()?.message ?: "获取推荐歌曲失败"
            }
            _isLoading.value = false
        }
    }

    /**
     * 解析推荐分类：取分类列表第一个可用分类，接口失败时使用兜底分类
     */
    private suspend fun resolveCategoryId() {
        if (categoryResolved) return
        val result = musicRepository.getMusicCategory()
        if (result.isSuccess) {
            val first = result.getOrNull()?.firstOrNull()
            if (first != null && first.id > 0) {
                categoryId = first.id
            }
        }
        categoryResolved = true
    }

    /**
     * 上拉加载下一页
     */
    fun loadMore() {
        if (_isLoadingMore.value || _isLoading.value || _reachedEnd.value) return
        if (_total.value > 0 && _musicList.value.size >= _total.value) {
            _reachedEnd.value = true
            return
        }

        viewModelScope.launch {
            _isLoadingMore.value = true
            // 首屏按 20 条加载，后续每页 10 条，页码按已加载条数推算，避免重复
            val nextPage = _musicList.value.size / PAGE_SIZE + 1
            val result = musicRepository.getMusicPageByCategoryId(
                categoryId = categoryId,
                pageNum = nextPage,
                pageSize = PAGE_SIZE
            )
            if (result.isSuccess) {
                val page = result.getOrNull()
                val existIds = _musicList.value.map { it.id }.toSet()
                val newSongs = (page?.list ?: emptyList()).filterNot { existIds.contains(it.id) }
                if (newSongs.isEmpty()) {
                    _reachedEnd.value = true
                } else {
                    _musicList.value = _musicList.value + newSongs
                    val total = page?.total ?: 0
                    if (total > 0) _total.value = total
                    _reachedEnd.value = _total.value > 0 && _musicList.value.size >= _total.value
                }
            } else {
                _toastMessage.value = result.exceptionOrNull()?.message ?: "加载更多失败"
            }
            _isLoadingMore.value = false
        }
    }

    /**
     * 点赞 / 取消点赞：先本地切换保证交互即时，接口失败再回滚并提示
     * @param music 当前操作的歌曲
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

    /**
     * 更新列表中某首歌的点赞状态
     */
    private fun updateLikeState(musicId: Long, isLike: Int) {
        _musicList.value = _musicList.value.map { item ->
            if (item.id == musicId) item.copy(isLike = isLike) else item
        }
    }

    fun clearToast() {
        _toastMessage.value = null
    }
}
