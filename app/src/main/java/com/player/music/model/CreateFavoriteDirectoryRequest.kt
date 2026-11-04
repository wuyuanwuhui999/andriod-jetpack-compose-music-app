package com.player.music.model

import com.google.gson.annotations.SerializedName

/**
 * 创建收藏夹请求
 */
data class CreateFavoriteDirectoryRequest(
    @SerializedName("name") val name: String
)
