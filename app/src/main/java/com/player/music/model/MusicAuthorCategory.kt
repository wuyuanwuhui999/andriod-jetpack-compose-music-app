package com.player.music.model

import com.google.gson.annotations.SerializedName

/**
 * 歌手分类（华语 / 欧美 / 韩国 / 日本 ...）
 * 对应 Flutter 端 model/MusicAuthorCategoryModel.dart
 */
data class MusicAuthorCategory(
    @SerializedName("id") val id: Int = 0,
    @SerializedName("categoryName") val categoryName: String = "",
    @SerializedName("rank") val rank: Int? = null,
    @SerializedName("disabled") val disabled: Int = 0,
    @SerializedName("createTime") val createTime: String? = null,
    @SerializedName("updateTime") val updateTime: String? = null
)
