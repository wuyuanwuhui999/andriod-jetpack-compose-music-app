package com.player.music.model

import com.google.gson.annotations.SerializedName

/**
 * 音乐（歌曲）数据模型
 * 只声明界面需要的字段，其余字段 Gson 自动忽略
 */
data class Music(
    @SerializedName("id") val id: Long = 0,
    @SerializedName("songName") val songName: String? = null,
    @SerializedName("authorId") val authorId: String? = null,
    @SerializedName("authorName") val authorName: String? = null,
    @SerializedName("albumName") val albumName: String? = null,
    @SerializedName("cover") val cover: String? = null,
    @SerializedName("playUrl") val playUrl: String? = null,
    @SerializedName("localPlayUrl") val localPlayUrl: String? = null,
    @SerializedName("label") val label: String? = null,
    @SerializedName("isHot") val isHot: Int? = null,
    @SerializedName("isLike") val isLike: Int = 0
)
