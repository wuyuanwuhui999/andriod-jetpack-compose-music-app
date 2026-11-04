package com.player.music.model

import com.google.gson.annotations.SerializedName

/**
 * 新增评论请求
 * 一级评论 parentId / topId 传 0
 */
data class InsertCommentRequest(
    @SerializedName("content") val content: String,
    @SerializedName("parentId") val parentId: Long = 0,
    @SerializedName("topId") val topId: Long = 0,
    @SerializedName("relationId") val relationId: Long,
    @SerializedName("type") val type: String
)
