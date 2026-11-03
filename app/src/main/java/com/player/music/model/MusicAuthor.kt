package com.player.music.model

import com.google.gson.annotations.SerializedName

/**
 * 歌手数据模型
 */
data class MusicAuthor(
    @SerializedName("id") val id: Long = 0,
    @SerializedName("authorId") val authorId: Long = 0,
    @SerializedName("authorName") val authorName: String? = null,
    @SerializedName("avatar") val avatar: String? = null,
    @SerializedName("categoryId") val categoryId: Int? = null,
    /** 该歌手的歌曲数量 */
    @SerializedName("total") val total: Int = 0,
    @SerializedName("isLike") val isLike: Int = 0
)
