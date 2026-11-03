package com.player.music.model

import com.google.gson.annotations.SerializedName

/**
 * 音乐分类数据模型
 * 后端 getMusicCategory 返回 category（分类名）、classify（大分类名）、img（分类图片）
 */
data class MusicCategory(
    @SerializedName("id") val id: Int = 0,
    @SerializedName("category") val category: String? = null,
    @SerializedName("classify") val classify: String? = null,
    @SerializedName("img") val img: String? = null,
    @SerializedName("createTime") val createTime: String? = null,
    @SerializedName("updateTime") val updateTime: String? = null
) {
    /** 界面展示名称（与 Flutter 端 classifyName 保持一致） */
    val displayName: String get() = category ?: classify ?: ""
}
