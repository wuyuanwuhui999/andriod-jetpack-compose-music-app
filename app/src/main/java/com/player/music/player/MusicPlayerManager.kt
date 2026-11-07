package com.player.music.player

import android.media.MediaPlayer
import com.player.music.model.LoopMode
import com.player.music.model.Music
import com.player.music.utils.CommonUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

/**
 * 全局音乐播放管理器（对应 Flutter 端 PlayerMusicProvider）
 *
 * 使用系统 MediaPlayer，不引入额外依赖；通过 StateFlow 把播放状态暴露给
 * 播放页和首页的迷你播放器。
 */
@Singleton
class MusicPlayerManager @Inject constructor() {

    private var mediaPlayer: MediaPlayer? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var tickerJob: Job? = null

    private val _currentMusic = MutableStateFlow<Music?>(null)
    /** 当前播放的歌曲 */
    val currentMusic: StateFlow<Music?> = _currentMusic.asStateFlow()

    private val _playlist = MutableStateFlow<List<Music>>(emptyList())
    /** 当前播放列表 */
    val playlist: StateFlow<List<Music>> = _playlist.asStateFlow()

    private val _playIndex = MutableStateFlow(0)
    val playIndex: StateFlow<Int> = _playIndex.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _positionMs = MutableStateFlow(0)
    /** 当前播放进度（毫秒） */
    val positionMs: StateFlow<Int> = _positionMs.asStateFlow()

    private val _durationMs = MutableStateFlow(0)
    /** 总时长（毫秒） */
    val durationMs: StateFlow<Int> = _durationMs.asStateFlow()

    private val _loopMode = MutableStateFlow(LoopMode.ORDER)
    val loopMode: StateFlow<LoopMode> = _loopMode.asStateFlow()

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 4)
    /** 错误提示（页面收集后 Toast） */
    val messages = _messages.asSharedFlow()

    /**
     * 播放一批歌曲（列表页点击某一首时调用）
     * @param list 播放列表
     * @param index 点击的歌曲在列表中的下标
     */
    fun playList(list: List<Music>, index: Int) {
        if (list.isEmpty()) return
        _playlist.value = list
        playAt(index.coerceIn(0, list.size - 1))
    }

    /**
     * 播放列表中的第 index 首
     */
    fun playAt(index: Int, autoPlay: Boolean = true) {
        val list = _playlist.value
        if (index !in list.indices) return

        val music = list[index]
        _playIndex.value = index
        _currentMusic.value = music
        _positionMs.value = 0
        _durationMs.value = 0

        val url = CommonUtils.formatImageUrl(music.localPlayUrl)
            ?: CommonUtils.formatImageUrl(music.playUrl)

        if (url.isNullOrBlank()) {
            _messages.tryEmit("该歌曲没有可播放的地址")
            return
        }

        releasePlayer()
        try {
            val player = MediaPlayer().apply {
                setDataSource(url)
                setOnPreparedListener {
                    _durationMs.value = it.duration.coerceAtLeast(0)
                    it.isLooping = _loopMode.value == LoopMode.REPEAT
                    if (autoPlay) {
                        it.start()
                        _isPlaying.value = true
                        startTicker()
                    }
                }
                setOnCompletionListener {
                    if (_loopMode.value == LoopMode.REPEAT) {
                        // MediaPlayer 的 isLooping 已处理，这里兜底重新播放
                        it.start()
                    } else {
                        next(auto = true)
                    }
                }
                setOnErrorListener { _, what, extra ->
                    _messages.tryEmit("播放失败（$what/$extra）")
                    _isPlaying.value = false
                    stopTicker()
                    true
                }
            }
            mediaPlayer = player
            player.prepareAsync()
        } catch (e: Exception) {
            _messages.tryEmit("播放失败：${e.message}")
        }
    }

    /**
     * 从播放列表中移除第 index 首
     * - 删除的是当前歌曲：自动播放新列表中同位置（越界则最后一首）的歌曲
     * - 删除的是当前之前的歌曲：当前下标前移一位
     * - 删空后停止播放并清空当前歌曲
     */
    fun removeAt(index: Int) {
        val list = _playlist.value
        if (index !in list.indices) return

        val newList = list.toMutableList().apply { removeAt(index) }
        _playlist.value = newList

        if (newList.isEmpty()) {
            releasePlayer()
            _currentMusic.value = null
            _playIndex.value = 0
            _positionMs.value = 0
            _durationMs.value = 0
            return
        }

        when {
            index < _playIndex.value -> _playIndex.value -= 1
            index == _playIndex.value ->
                playAt(index.coerceAtMost(newList.size - 1))
        }
    }

    /**
     * 清空播放列表并停止播放
     */
    fun clearPlaylist() {
        releasePlayer()
        _playlist.value = emptyList()
        _currentMusic.value = null
        _playIndex.value = 0
        _positionMs.value = 0
        _durationMs.value = 0
    }

    /**
     * 播放 / 暂停
     */
    fun togglePlay() {
        val player = mediaPlayer ?: run {
            // 还没开始播放时，从当前列表第一首开始
            if (_playlist.value.isNotEmpty()) playAt(_playIndex.value)
            return
        }
        try {
            if (player.isPlaying) {
                player.pause()
                _isPlaying.value = false
                stopTicker()
            } else {
                player.start()
                _isPlaying.value = true
                startTicker()
            }
        } catch (e: Exception) {
            _messages.tryEmit("操作失败：${e.message}")
        }
    }

    /**
     * 下一首
     * @param auto 是否由播放完成自动触发
     */
    fun next(auto: Boolean = false) {
        val list = _playlist.value
        if (list.isEmpty()) return

        val nextIndex = when (_loopMode.value) {
            LoopMode.RANDOM -> {
                if (list.size <= 1) 0 else {
                    var target = _playIndex.value
                    while (target == _playIndex.value) target = Random.nextInt(list.size)
                    target
                }
            }
            else -> if (_playIndex.value < list.size - 1) _playIndex.value + 1 else 0
        }
        playAt(nextIndex)
    }

    /**
     * 上一首
     */
    fun prev() {
        val list = _playlist.value
        if (list.isEmpty()) return

        val prevIndex = when (_loopMode.value) {
            LoopMode.RANDOM -> {
                if (list.size <= 1) 0 else Random.nextInt(list.size)
            }
            else -> if (_playIndex.value > 0) _playIndex.value - 1 else list.size - 1
        }
        playAt(prevIndex)
    }

    /**
     * 拖动进度条
     */
    fun seekTo(positionMs: Int) {
        val player = mediaPlayer ?: return
        val safe = positionMs.coerceIn(0, _durationMs.value.coerceAtLeast(0))
        try {
            player.seekTo(safe)
            _positionMs.value = safe
        } catch (e: Exception) {
            _messages.tryEmit("跳转失败：${e.message}")
        }
    }

    /**
     * 切换循环模式
     */
    fun setLoopMode(mode: LoopMode) {
        _loopMode.value = mode
        mediaPlayer?.isLooping = mode == LoopMode.REPEAT
    }

    /**
     * 更新当前歌曲的喜欢状态（点赞 / 取消点赞后同步 UI）
     */
    fun updateLike(musicId: Long, isLike: Int) {
        _playlist.value = _playlist.value.map {
            if (it.id == musicId) it.copy(isLike = isLike) else it
        }
        if (_currentMusic.value?.id == musicId) {
            _currentMusic.value = _currentMusic.value?.copy(isLike = isLike)
        }
    }

    /**
     * 更新当前歌曲的封面等信息（详情接口返回后）
     */
    fun updateCurrentMusic(music: Music) {
        _currentMusic.value = music
        _playlist.value = _playlist.value.map { if (it.id == music.id) music else it }
    }

    private fun startTicker() {
        stopTicker()
        tickerJob = scope.launch {
            while (isActive) {
                mediaPlayer?.let { player ->
                    try {
                        if (player.isPlaying) {
                            _positionMs.value = player.currentPosition
                            val duration = player.duration
                            if (duration > 0) _durationMs.value = duration
                        }
                    } catch (_: Exception) {
                        // 播放器已释放，忽略
                    }
                }
                delay(300)
            }
        }
    }

    private fun stopTicker() {
        tickerJob?.cancel()
        tickerJob = null
    }

    private fun releasePlayer() {
        stopTicker()
        mediaPlayer?.let {
            try {
                if (it.isPlaying) it.stop()
                it.release()
            } catch (_: Exception) {
                // ignore
            }
        }
        mediaPlayer = null
        _isPlaying.value = false
    }
}
