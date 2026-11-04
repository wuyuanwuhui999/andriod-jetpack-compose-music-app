package com.player.music.model

import com.google.gson.annotations.SerializedName

/**
 * 评论数据模型（支持二级回复嵌套）
 */
data class Comment(
    @SerializedName("id") val id: Long = 0,
    @SerializedName("type") val type: String? = null,
    @SerializedName("content") val content: String? = null,
    @SerializedName("parentId") val parentId: Long? = null,
    @SerializedName("topId") val topId: Long? = null,
    @SerializedName("relationId") val relationId: Long = 0,
    @SerializedName("createTime") val createTime: String? = null,
    @SerializedName("updateTime") val updateTime: String? = null,
    @SerializedName("replyCount") val replyCount: Int? = null,
    @SerializedName("userId") val userId: String? = null,
    @SerializedName("username") val username: String? = null,
    @SerializedName("avater") val avater: String? = null,
    @SerializedName("replyUserId") val replyUserId: String? = null,
    @SerializedName("replyUserName") val replyUserName: String? = null,
    @SerializedName("replyList") val replyList: List<Comment>? = emptyList()
) {
    /** 是否为一级评论（topId 为空或 0） */
    val isTopComment: Boolean get() = topId == null || topId == 0L
}
