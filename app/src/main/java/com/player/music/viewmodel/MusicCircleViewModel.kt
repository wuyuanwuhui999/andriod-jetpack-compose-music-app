package com.player.music.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.player.music.model.Circle
import com.player.music.model.CircleLike
import com.player.music.model.Comment
import com.player.music.model.Music
import com.player.music.player.MusicPlayerManager
import com.player.music.repository.CircleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 音乐圈 ViewModel
 * 对应 Flutter 端 lib/pages/MusicCirclePage.dart：
 * 分页加载音乐圈动态、点赞/取消点赞、发表评论与回复
 */
@HiltViewModel
class MusicCircleViewModel @Inject constructor(
    private val circleRepository: CircleRepository,
    private val player: MusicPlayerManager
) : ViewModel() {

    companion object {
        private const val PAGE_SIZE = 10
    }

    // 音乐圈动态列表
    private val _circles = MutableStateFlow<List<Circle>>(emptyList())
    val circles: StateFlow<List<Circle>> = _circles.asStateFlow()

    private val _total = MutableStateFlow(0)

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isLoadingMore = MutableStateFlow(false)
    val isLoadingMore: StateFlow<Boolean> = _isLoadingMore.asStateFlow()

    private val _reachedEnd = MutableStateFlow(false)
    val reachedEnd: StateFlow<Boolean> = _reachedEnd.asStateFlow()

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    private var pageNum = 1

    /** 正在提交评论，避免重复提交 */
    private var submittingComment = false

    init {
        loadInitialData()
    }

    /**
     * 首屏加载第一页
     */
    fun loadInitialData() {
        viewModelScope.launch {
            _isLoading.value = true
            pageNum = 1
            val result = circleRepository.getCircleList(pageNum = pageNum, pageSize = PAGE_SIZE)
            if (result.isSuccess) {
                val page = result.getOrNull()
                _circles.value = page?.list ?: emptyList()
                _total.value = page?.total ?: 0
                _reachedEnd.value = _total.value > 0 && _circles.value.size >= _total.value
            } else {
                _toastMessage.value = result.exceptionOrNull()?.message ?: "获取音乐圈失败"
            }
            _isLoading.value = false
        }
    }

    /**
     * 上拉加载下一页
     */
    fun loadMore() {
        if (_isLoadingMore.value || _isLoading.value || _reachedEnd.value) return
        if (_total.value > 0 && _circles.value.size >= _total.value) {
            _reachedEnd.value = true
            return
        }

        viewModelScope.launch {
            _isLoadingMore.value = true
            val nextPage = pageNum + 1
            val result = circleRepository.getCircleList(pageNum = nextPage, pageSize = PAGE_SIZE)
            if (result.isSuccess) {
                val page = result.getOrNull()
                val existIds = _circles.value.map { it.id }.toSet()
                val newItems = (page?.list ?: emptyList()).filterNot { existIds.contains(it.id) }
                if (newItems.isEmpty()) {
                    _reachedEnd.value = true
                } else {
                    _circles.value = _circles.value + newItems
                    pageNum = nextPage
                    val total = page?.total ?: 0
                    if (total > 0) _total.value = total
                    _reachedEnd.value = _total.value > 0 && _circles.value.size >= _total.value
                }
            } else {
                _toastMessage.value = result.exceptionOrNull()?.message ?: "加载更多失败"
            }
            _isLoadingMore.value = false
        }
    }

    /**
     * 判断当前用户是否已点赞该条动态
     */
    fun isLikedByMe(circle: Circle, userId: String?): Boolean {
        if (userId.isNullOrBlank()) return false
        return circle.circleLikes?.any { it.userId == userId } == true
    }

    /**
     * 点赞 / 取消点赞
     * @param circle 目标动态
     * @param userId 当前用户ID
     * @param username 当前用户昵称
     */
    fun toggleLike(circle: Circle, userId: String?, username: String?) {
        if (userId.isNullOrBlank()) {
            _toastMessage.value = "请先登录"
            return
        }
        val liked = isLikedByMe(circle, userId)

        viewModelScope.launch {
            if (liked) {
                val result = circleRepository.deleteLike(circle.id)
                if (result.isSuccess) {
                    updateCircle(circle.id) { item ->
                        item.copy(circleLikes = item.circleLikes?.filterNot { it.userId == userId } ?: emptyList())
                    }
                } else {
                    _toastMessage.value = result.exceptionOrNull()?.message ?: "取消点赞失败"
                }
            } else {
                val result = circleRepository.saveLike(circle.id)
                if (result.isSuccess) {
                    val newLike = result.getOrNull()?.takeIf { !it.username.isNullOrBlank() }
                        ?: CircleLike(
                            id = 0,
                            relationId = circle.id,
                            type = CircleRepository.TYPE_MUSIC_CIRCLE,
                            userId = userId,
                            username = username ?: ""
                        )
                    updateCircle(circle.id) { item ->
                        item.copy(circleLikes = (item.circleLikes ?: emptyList()) + newLike)
                    }
                } else {
                    _toastMessage.value = result.exceptionOrNull()?.message ?: "点赞失败"
                }
            }
        }
    }

    /**
     * 发表评论 / 回复
     * @param circle 目标动态
     * @param content 评论内容
     * @param topComment 一级评论（为空表示发表一级评论）
     * @param replyComment 被回复的评论（为空表示直接评论一级评论）
     */
    fun submitComment(
        circle: Circle,
        content: String,
        topComment: Comment? = null,
        replyComment: Comment? = null,
        userId: String? = null,
        username: String? = null,
        avatar: String? = null
    ) {
        if (content.isBlank()) {
            _toastMessage.value = "评论内容不能为空"
            return
        }
        if (submittingComment) return
        submittingComment = true

        viewModelScope.launch {
            try {
                val topId = topComment?.id ?: 0
                val parentId = (replyComment ?: topComment)?.id ?: 0
                val result = circleRepository.insertComment(
                    relationId = circle.id,
                    content = content.trim(),
                    topId = topId,
                    parentId = parentId
                )
                if (result.isSuccess) {
                    val returned = result.getOrNull()
                    // 后端插入评论的响应里没带用户信息，这里用当前登录用户补全，避免评论显示空白昵称
                    val newComment = returned?.let {
                        if (it.username.isNullOrBlank()) {
                            it.copy(
                                userId = userId,
                                username = username ?: "",
                                avater = avatar
                            )
                        } else {
                            it
                        }
                    }
                    if (newComment != null) {
                        if (topComment == null) {
                            // 一级评论直接追加
                            updateCircle(circle.id) { item ->
                                item.copy(circleComments = (item.circleComments ?: emptyList()) + newComment)
                            }
                        } else {
                            // 回复追加到一级评论的 replyList
                            updateCircle(circle.id) { item ->
                                item.copy(
                                    circleComments = item.circleComments?.map { comment ->
                                        if (comment.id == topComment.id) {
                                            comment.copy(replyList = (comment.replyList ?: emptyList()) + newComment)
                                        } else {
                                            comment
                                        }
                                    } ?: emptyList()
                                )
                            }
                        }
                    }
                    _toastMessage.value = "评论成功"
                } else {
                    _toastMessage.value = result.exceptionOrNull()?.message ?: "评论失败"
                }
            } finally {
                submittingComment = false
            }
        }
    }

    /**
     * 更新列表中的某条动态
     */
    private fun updateCircle(circleId: Long, transform: (Circle) -> Circle) {
        _circles.value = _circles.value.map { circle ->
            if (circle.id == circleId) transform(circle) else circle
        }
    }

    /**
     * 播放音乐圈动态里关联的歌曲（点击歌曲播放按钮时）
     * @return 是否成功开始播放（歌曲 id 或播放地址缺失时返回 false）
     */
    fun playCircleMusic(circle: Circle): Boolean {
        val musicId = circle.musicAudioId ?: 0L
        val playUrl = circle.musicLocalPlayUrl ?: circle.musicPlayUrl
        if (musicId <= 0L || playUrl.isNullOrBlank()) {
            _toastMessage.value = "该歌曲暂无可播放的地址"
            return false
        }
        val music = Music(
            id = musicId,
            songName = circle.musicSongName,
            authorName = circle.musicAuthorName,
            albumName = circle.musicAlbumName,
            cover = circle.musicCover,
            playUrl = circle.musicPlayUrl,
            localPlayUrl = circle.musicLocalPlayUrl,
            lyrics = circle.musicLyrics
        )
        player.playList(listOf(music), 0)
        return true
    }

    fun clearToast() {
        _toastMessage.value = null
    }
}
