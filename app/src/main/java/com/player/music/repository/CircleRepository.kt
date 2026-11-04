package com.player.music.repository

import com.player.music.model.Circle
import com.player.music.model.CircleLike
import com.player.music.model.CircleListResult
import com.player.music.model.Comment
import com.player.music.model.InsertCommentRequest
import com.player.music.model.SaveLikeRequest
import com.player.music.network.ApiService
import javax.inject.Inject

/**
 * 音乐圈仓库：朋友圈列表 + 评论 + 点赞
 */
class CircleRepository @Inject constructor(
    private val apiService: ApiService
) {

    companion object {
        /** 音乐圈类型 */
        const val TYPE_MUSIC_CIRCLE = "MUSIC_CIRCLE"
    }

    /**
     * 分页获取音乐圈动态
     * @param type 类型：MUSIC
     */
    suspend fun getCircleList(
        type: String = "MUSIC",
        pageNum: Int = 1,
        pageSize: Int = 10
    ): Result<CircleListResult> {
        return try {
            val response = apiService.getCircleListByType(type, pageNum, pageSize)
            val body = response.body()
            if (response.isSuccessful && body?.status == "SUCCESS") {
                Result.success(
                    CircleListResult(
                        list = body.data ?: emptyList(),
                        total = body.total ?: 0
                    )
                )
            } else {
                Result.failure(Exception(body?.msg ?: "获取音乐圈失败"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 新增评论 / 回复
     */
    suspend fun insertComment(
        relationId: Long,
        content: String,
        topId: Long = 0,
        parentId: Long = 0
    ): Result<Comment?> {
        return try {
            val request = InsertCommentRequest(
                content = content,
                parentId = parentId,
                topId = topId,
                relationId = relationId,
                type = TYPE_MUSIC_CIRCLE
            )
            val response = apiService.insertComment(request)
            val body = response.body()
            if (response.isSuccessful && body?.status == "SUCCESS") {
                Result.success(body.data)
            } else {
                Result.failure(Exception(body?.msg ?: "评论失败"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 点赞朋友圈
     */
    suspend fun saveLike(relationId: Long): Result<CircleLike?> {
        return try {
            val response = apiService.saveLike(SaveLikeRequest(relationId, TYPE_MUSIC_CIRCLE))
            val body = response.body()
            if (response.isSuccessful && body?.status == "SUCCESS") {
                Result.success(body.data)
            } else {
                Result.failure(Exception(body?.msg ?: "点赞失败"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 取消点赞朋友圈
     */
    suspend fun deleteLike(relationId: Long): Result<Int> {
        return try {
            val response = apiService.deleteLike(relationId, TYPE_MUSIC_CIRCLE)
            val body = response.body()
            if (response.isSuccessful && body?.status == "SUCCESS") {
                Result.success(body.data ?: 0)
            } else {
                Result.failure(Exception(body?.msg ?: "取消点赞失败"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
