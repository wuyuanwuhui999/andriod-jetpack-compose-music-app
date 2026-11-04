package com.player.music.model

import com.google.gson.annotations.SerializedName

/**
 * 点赞请求（朋友圈/评论/音乐通用）
 */
data class SaveLikeRequest(
    @SerializedName("relationId") val relationId: Long,
    @SerializedName("type") val type: String
)
