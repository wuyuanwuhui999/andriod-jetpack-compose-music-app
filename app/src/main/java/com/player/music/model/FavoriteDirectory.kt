package com.player.music.model

import com.google.gson.annotations.SerializedName

/**
 * 音乐收藏夹数据模型
 */
data class FavoriteDirectory(
    @SerializedName("id") val id: Long = 0,
    @SerializedName("name") val name: String = "",
    @SerializedName("userId") val userId: String? = null,
    /** 收藏夹内音乐总数 */
    @SerializedName("total") val total: Int = 0,
    @SerializedName("cover") val cover: String? = null,
    /** 当前音乐是否在该收藏夹：1-是，0-否（我的页面用不到） */
    @SerializedName("checked") val checked: Int = 0,
    @SerializedName("createTime") val createTime: String? = null,
    @SerializedName("updateTime") val updateTime: String? = null
)
