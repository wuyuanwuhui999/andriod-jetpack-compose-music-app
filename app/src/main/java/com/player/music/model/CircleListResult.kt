package com.player.music.model

/**
 * 朋友圈分页结果：列表 + 总条数
 */
data class CircleListResult(
    val list: List<Circle> = emptyList(),
    val total: Int = 0
)
