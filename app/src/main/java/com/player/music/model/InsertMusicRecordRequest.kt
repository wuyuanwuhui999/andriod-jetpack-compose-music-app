package com.player.music.model

import com.google.gson.annotations.SerializedName

/**
 * 插入播放记录请求
 */
data class InsertMusicRecordRequest(
    @SerializedName("musicId") val musicId: Long,
    @SerializedName("platform") val platform: String,
    @SerializedName("version") val version: String,
    @SerializedName("device") val device: String
)
