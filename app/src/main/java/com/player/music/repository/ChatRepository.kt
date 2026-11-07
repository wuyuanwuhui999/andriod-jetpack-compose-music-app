package com.player.music.repository

import android.net.Uri
import com.player.music.config.ChatConfig
import com.player.music.config.Config
import com.player.music.model.AiModel
import com.player.music.model.ChatHistoryItem
import com.player.music.model.ChatHistoryResult
import com.player.music.network.ApiService
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import javax.inject.Inject

/**
 * AI 聊天仓库：模型列表、聊天历史、WebSocket 流式对话
 */
class ChatRepository @Inject constructor(
    private val apiService: ApiService,
    private val okHttpClient: OkHttpClient
) {

    /** 可用模型列表 */
    suspend fun getModelList(): Result<List<AiModel>> {
        return try {
            val response = apiService.getAiModelList(ChatConfig.COMPANY_ID)
            val body = response.body()
            if (response.isSuccessful && body?.status == "SUCCESS") {
                Result.success(body.data ?: emptyList())
            } else {
                Result.failure(Exception(body?.msg ?: "获取模型列表失败"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** 聊天历史（分页） */
    suspend fun getChatHistory(
        pageNum: Int,
        pageSize: Int = ChatConfig.PAGE_SIZE
    ): Result<ChatHistoryResult> {
        return try {
            val response = apiService.getChatHistory(pageNum, pageSize)
            val body = response.body()
            if (response.isSuccessful && body?.status == "SUCCESS") {
                Result.success(
                    ChatHistoryResult(
                        list = body.data ?: emptyList<ChatHistoryItem>(),
                        total = body.total ?: 0
                    )
                )
            } else {
                Result.failure(Exception(body?.msg ?: "获取会话记录失败"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 建立 AI 对话 WebSocket
     * 网关要求 ?token=Bearer%20xxx（token 必须带 Bearer 前缀），用户 id 由网关注入
     */
    fun openChatSocket(token: String, listener: WebSocketListener): WebSocket? {
        return try {
            val wsBase = Config.BASE_URL.replaceFirst("http", "ws")
            val url = "$wsBase${ChatConfig.WS_PATH}?token=${Uri.encode("Bearer $token")}"
            okHttpClient.newWebSocket(Request.Builder().url(url).build(), listener)
        } catch (e: Exception) {
            null
        }
    }
}
