package com.player.music.model

import com.google.gson.annotations.SerializedName

/**
 * AI 模型（/service/chat/getModelList）
 */
data class AiModel(
    @SerializedName("id") val id: String = "",
    @SerializedName("modelName") val modelName: String = "",
    @SerializedName("companyId") val companyId: String? = null,
    @SerializedName("type") val type: String? = null,
    @SerializedName("createTime") val createTime: String? = null,
    @SerializedName("updateTime") val updateTime: String? = null
)
