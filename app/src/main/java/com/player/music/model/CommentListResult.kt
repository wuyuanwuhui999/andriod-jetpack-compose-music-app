package com.player.music.model

/**
 * 评论列表分页结果
 */
data class CommentListResult(
    val list: List<Comment>,
    val total: Int
)
