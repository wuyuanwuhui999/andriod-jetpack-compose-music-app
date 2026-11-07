package com.player.music.model

import com.google.gson.annotations.SerializedName

/**
 * 发布朋友圈（音乐圈动态）请求体
 * 对应后端 circle.md 中的 InsertCircleSchema
 */
data class InsertCircleRequest(
    /** 动态内容 */
    @SerializedName("content") val content: String = "",
    /** 图片（base64，多张逗号分隔），无图片传空串 */
    @SerializedName("imgs") val imgs: String = "",
    /** 关联音乐 id */
    @SerializedName("relationId") val relationId: Long = 0,
    /** 类型：MUSIC / MOVIE */
    @SerializedName("type") val type: String = "MUSIC",
    /** 权限：0-私密，1-公开 */
    @SerializedName("permission") val permission: Int = 1
)
