package com.player.music.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.player.music.model.FavoriteDirectory
import com.player.music.model.Music
import com.player.music.model.MusicAuthor
import com.player.music.repository.MusicRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 我的页面 ViewModel
 * 对应 Flutter 端 lib/pages/MusicUserPage.dart：
 * 我的收藏夹 / 我喜欢的歌曲 / 我关注的歌手 / 我最近播放的歌曲，支持折叠、刷新、删除
 */
@HiltViewModel
class MusicUserViewModel @Inject constructor(
    private val musicRepository: MusicRepository
) : ViewModel() {

    companion object {
        /** 各模块首页展示条数（与 Flutter 端一致） */
        private const val LIKE_PAGE_SIZE = 5
        private const val AUTHOR_PAGE_SIZE = 5
        private const val RECORD_PAGE_SIZE = 10
    }

    // ============ 收藏夹 ============
    private val _directories = MutableStateFlow<List<FavoriteDirectory>>(emptyList())
    val directories: StateFlow<List<FavoriteDirectory>> = _directories.asStateFlow()

    private val _foldDirectory = MutableStateFlow(false)
    val foldDirectory: StateFlow<Boolean> = _foldDirectory.asStateFlow()

    // ============ 我喜欢的歌曲 ============
    private val _likedSongs = MutableStateFlow<List<Music>>(emptyList())
    val likedSongs: StateFlow<List<Music>> = _likedSongs.asStateFlow()

    private val _likedTotal = MutableStateFlow(0)
    val likedTotal: StateFlow<Int> = _likedTotal.asStateFlow()

    private val _foldLiked = MutableStateFlow(false)
    val foldLiked: StateFlow<Boolean> = _foldLiked.asStateFlow()

    // ============ 我关注的歌手 ============
    private val _authors = MutableStateFlow<List<MusicAuthor>>(emptyList())
    val authors: StateFlow<List<MusicAuthor>> = _authors.asStateFlow()

    private val _authorTotal = MutableStateFlow(0)
    val authorTotal: StateFlow<Int> = _authorTotal.asStateFlow()

    private val _foldAuthor = MutableStateFlow(false)
    val foldAuthor: StateFlow<Boolean> = _foldAuthor.asStateFlow()

    // ============ 最近播放 ============
    private val _records = MutableStateFlow<List<Music>>(emptyList())
    val records: StateFlow<List<Music>> = _records.asStateFlow()

    private val _recordTotal = MutableStateFlow(0)
    val recordTotal: StateFlow<Int> = _recordTotal.asStateFlow()

    private val _foldRecord = MutableStateFlow(false)
    val foldRecord: StateFlow<Boolean> = _foldRecord.asStateFlow()

    // ============ 通用状态 ============
    private val _isInitialLoading = MutableStateFlow(true)
    val isInitialLoading: StateFlow<Boolean> = _isInitialLoading.asStateFlow()

    private val _refreshing = MutableStateFlow<String?>(null)
    val refreshing: StateFlow<String?> = _refreshing.asStateFlow()

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    /** 查询收藏夹需要的音乐ID（后端要求 > 0） */
    private var directoryMusicId: Long = 0

    init {
        loadAll()
    }

    /**
     * 首屏加载全部模块
     */
    fun loadAll() {
        viewModelScope.launch {
            _isInitialLoading.value = true
            loadLikedInternal()
            loadRecordsInternal()
            loadAuthorsInternal()
            loadDirectoriesInternal()
            _isInitialLoading.value = false
        }
    }

    // ============ 折叠开关 ============
    fun toggleFoldDirectory() { _foldDirectory.value = !_foldDirectory.value }
    fun toggleFoldLiked() { _foldLiked.value = !_foldLiked.value }
    fun toggleFoldAuthor() { _foldAuthor.value = !_foldAuthor.value }
    fun toggleFoldRecord() { _foldRecord.value = !_foldRecord.value }

    // ============ 刷新 ============
    fun refreshDirectories() {
        viewModelScope.launch {
            _refreshing.value = "directory"
            loadDirectoriesInternal(force = true)
            _refreshing.value = null
        }
    }

    fun refreshLiked() {
        viewModelScope.launch {
            _refreshing.value = "liked"
            loadLikedInternal()
            _refreshing.value = null
        }
    }

    fun refreshAuthors() {
        viewModelScope.launch {
            _refreshing.value = "author"
            loadAuthorsInternal()
            _refreshing.value = null
        }
    }

    fun refreshRecords() {
        viewModelScope.launch {
            _refreshing.value = "record"
            loadRecordsInternal()
            _refreshing.value = null
        }
    }

    // ============ 内部加载 ============
    private suspend fun loadLikedInternal() {
        val result = musicRepository.getMusicLike(pageNum = 1, pageSize = LIKE_PAGE_SIZE)
        if (result.isSuccess) {
            val page = result.getOrNull()
            _likedSongs.value = page?.list ?: emptyList()
            _likedTotal.value = page?.total ?: 0
        } else {
            _toastMessage.value = result.exceptionOrNull()?.message ?: "获取喜欢的歌曲失败"
        }
    }

    private suspend fun loadRecordsInternal() {
        val result = musicRepository.getMusicRecord(pageNum = 1, pageSize = RECORD_PAGE_SIZE)
        if (result.isSuccess) {
            val page = result.getOrNull()
            _records.value = page?.list ?: emptyList()
            _recordTotal.value = page?.total ?: 0
        } else {
            _toastMessage.value = result.exceptionOrNull()?.message ?: "获取播放记录失败"
        }
    }

    private suspend fun loadAuthorsInternal() {
        val result = musicRepository.getFavoriteAuthors()
        if (result.isSuccess) {
            val list = result.getOrNull() ?: emptyList()
            _authors.value = list.take(AUTHOR_PAGE_SIZE)
            _authorTotal.value = list.size
        } else {
            _toastMessage.value = result.exceptionOrNull()?.message ?: "获取关注的歌手失败"
        }
    }

    /**
     * 加载收藏夹：后端要求必须传 musicId > 0，这里用"喜欢的歌曲/播放记录/推荐歌曲"的ID兜底
     */
    private suspend fun loadDirectoriesInternal(force: Boolean = false) {
        if (directoryMusicId <= 0 || force) {
            directoryMusicId = _likedSongs.value.firstOrNull()?.id
                ?: _records.value.firstOrNull()?.id
                ?: musicRepository.getKeywordMusic().getOrNull()?.id
                ?: 0
        }
        if (directoryMusicId <= 0) {
            _directories.value = emptyList()
            return
        }
        val result = musicRepository.getFavoriteDirectories(directoryMusicId)
        if (result.isSuccess) {
            _directories.value = result.getOrNull() ?: emptyList()
        } else {
            _toastMessage.value = result.exceptionOrNull()?.message ?: "获取收藏夹失败"
        }
    }

    // ============ 操作 ============
    /**
     * 创建收藏夹
     */
    fun createDirectory(name: String) {
        if (name.isBlank()) {
            _toastMessage.value = "收藏夹名称不能为空"
            return
        }
        viewModelScope.launch {
            val result = musicRepository.createFavoriteDirectory(name.trim())
            if (result.isSuccess) {
                val created = result.getOrNull()
                if (created != null) {
                    _directories.value = listOf(created) + _directories.value
                } else {
                    loadDirectoriesInternal(force = true)
                }
                _toastMessage.value = "创建成功"
            } else {
                _toastMessage.value = result.exceptionOrNull()?.message ?: "创建收藏夹失败"
            }
        }
    }

    /**
     * 删除收藏夹
     */
    fun deleteDirectory(directory: FavoriteDirectory) {
        viewModelScope.launch {
            val result = musicRepository.deleteFavoriteDirectory(directory.id)
            if (result.isSuccess) {
                _directories.value = _directories.value.filterNot { it.id == directory.id }
                _toastMessage.value = "删除成功"
            } else {
                _toastMessage.value = result.exceptionOrNull()?.message ?: "删除收藏夹失败"
            }
        }
    }

    /**
     * 取消喜欢（删除喜欢的歌曲）
     */
    fun deleteLikedSong(music: Music) {
        viewModelScope.launch {
            val result = musicRepository.unlikeMusic(music.id)
            if (result.isSuccess) {
                _likedSongs.value = _likedSongs.value.filterNot { it.id == music.id }
                if (_likedTotal.value > 0) _likedTotal.value -= 1
                _toastMessage.value = "删除成功"
            } else {
                _toastMessage.value = result.exceptionOrNull()?.message ?: "删除失败"
            }
        }
    }

    /**
     * 取消关注歌手
     */
    fun deleteAuthor(author: MusicAuthor) {
        viewModelScope.launch {
            val result = musicRepository.deleteFavoriteAuthor(author.authorId)
            if (result.isSuccess) {
                _authors.value = _authors.value.filterNot { it.authorId == author.authorId }
                if (_authorTotal.value > 0) _authorTotal.value -= 1
                _toastMessage.value = "删除成功"
            } else {
                _toastMessage.value = result.exceptionOrNull()?.message ?: "取消关注失败"
            }
        }
    }

    fun clearToast() {
        _toastMessage.value = null
    }
}
