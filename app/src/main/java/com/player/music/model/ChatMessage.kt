package com.player.music.model

import java.util.UUID

/**
 * 聊天气泡
 */
data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val role: ChatRole = ChatRole.AI,
    /** 用户提问内容（role = USER 时使用） */
    val text: String = "",
    /** 思考内容（深度思考开启时由模型返回 <think> 段） */
    val thinkContent: String = "",
    /** 正文内容（可能包含 <music> 歌单块） */
    val responseContent: String = "",
    /** 是否正在流式输出 */
    val streaming: Boolean = false
)

enum class ChatRole { USER, AI }
