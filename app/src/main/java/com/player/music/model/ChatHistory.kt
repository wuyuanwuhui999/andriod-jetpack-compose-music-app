package com.player.music.model

import com.google.gson.annotations.SerializedName

/**
 * 一条聊天历史（/service/chat/getChatHistory）
 */
data class ChatHistoryItem(
    @SerializedName("id") val id: Long = 0,
    @SerializedName("chatId") val chatId: String = "",
    @SerializedName("modelName") val modelName: String? = null,
    @SerializedName("prompt") val prompt: String? = null,
    @SerializedName("thinkContent") val thinkContent: String? = null,
    @SerializedName("responseContent") val responseContent: String? = null,
    @SerializedName("createTime") val createTime: String? = null
)

/**
 * 聊天历史分页结果
 */
data class ChatHistoryResult(
    val list: List<ChatHistoryItem> = emptyList(),
    val total: Int = 0
)

/**
 * 会话记录按「时间分组 → 会话」两级结构（对应 uni-app 端 ChatStructure）
 */
data class ChatSession(
    val chatId: String,
    /** 会话标题：取会话第一条提问 */
    val title: String,
    val timeAgo: String,
    val items: List<ChatHistoryItem>
)

data class ChatHistoryGroup(
    val timeAgo: String,
    val sessions: List<ChatSession>
)
