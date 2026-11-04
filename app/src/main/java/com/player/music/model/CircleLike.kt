package com.player.music.model

import com.google.gson.annotations.SerializedName

/**
 * 音乐圈点赞数据模型
 */
data class CircleLike(
    @SerializedName("id") val id: Long = 0,
    @SerializedName("relationId") val relationId: Long = 0,
    @SerializedName("type") val type: String? = null,
    @SerializedName("userId") val userId: String? = null,
    @SerializedName("username") val username: String? = null,
    @SerializedName("createTime") val createTime: String? = null,
    @SerializedName("updateTime") val updateTime: String? = null
)
