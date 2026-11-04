package com.player.music.model

import com.google.gson.annotations.SerializedName

/**
 * 音乐圈动态数据模型
 */
data class Circle(
    @SerializedName("id") val id: Long = 0,
    @SerializedName("relationId") val relationId: Long = 0,
    @SerializedName("content") val content: String? = null,
    @SerializedName("imgs") val imgs: String? = null,
    @SerializedName("type") val type: String? = null,
    @SerializedName("userId") val userId: String? = null,
    @SerializedName("username") val username: String? = null,
    @SerializedName("useravater") val useravater: String? = null,
    @SerializedName("createTime") val createTime: String? = null,
    @SerializedName("updateTime") val updateTime: String? = null,
    @SerializedName("permission") val permission: Int = 1,
    // 关联的音乐信息
    @SerializedName("musicSongName") val musicSongName: String? = null,
    @SerializedName("musicAudioId") val musicAudioId: Long? = null,
    @SerializedName("musicAuthorName") val musicAuthorName: String? = null,
    @SerializedName("musicAlbumName") val musicAlbumName: String? = null,
    @SerializedName("musicCover") val musicCover: String? = null,
    @SerializedName("musicPlayUrl") val musicPlayUrl: String? = null,
    @SerializedName("musicLocalPlayUrl") val musicLocalPlayUrl: String? = null,
    // 点赞与评论
    @SerializedName("circleLikes") val circleLikes: List<CircleLike>? = emptyList(),
    @SerializedName("circleComments") val circleComments: List<Comment>? = emptyList()
)
