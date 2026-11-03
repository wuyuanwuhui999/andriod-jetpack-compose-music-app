package com.player.music.model

/**
 * 音乐分页结果：列表 + 总条数
 * 后端分页接口把总数放在 ApiResponse.total 中
 */
data class MusicListResult(
    val list: List<Music> = emptyList(),
    val total: Int = 0
)
