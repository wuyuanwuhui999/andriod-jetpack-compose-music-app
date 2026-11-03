package com.player.music.repository

import com.player.music.model.Music
import com.player.music.model.MusicAuthor
import com.player.music.model.MusicCategory
import com.player.music.network.ApiService
import javax.inject.Inject

/**
 * 音乐模块仓库：负责调用 /service/music 下的接口，统一返回 Result
 */
class MusicRepository @Inject constructor(
    private val apiService: ApiService
) {

    /**
     * 搜索框推荐音乐（热门歌曲）
     */
    suspend fun getKeywordMusic(): Result<Music?> {
        return try {
            val response = apiService.getKeywordMusic()
            val body = response.body()
            if (response.isSuccessful && body?.status == "SUCCESS") {
                Result.success(body.data)
            } else {
                Result.failure(Exception(body?.msg ?: "获取推荐音乐失败"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 获取全部分类
     */
    suspend fun getMusicCategory(): Result<List<MusicCategory>> {
        return try {
            val response = apiService.getMusicCategory()
            val body = response.body()
            if (response.isSuccessful && body?.status == "SUCCESS") {
                Result.success(body.data ?: emptyList())
            } else {
                Result.failure(Exception(body?.msg ?: "获取音乐分类失败"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 按分类获取音乐列表
     */
    suspend fun getMusicListByCategoryId(
        categoryId: Int,
        pageNum: Int = 1,
        pageSize: Int = 10
    ): Result<List<Music>> {
        return try {
            val response = apiService.getMusicListByCategoryId(categoryId, pageNum, pageSize)
            val body = response.body()
            if (response.isSuccessful && body?.status == "SUCCESS") {
                Result.success(body.data ?: emptyList())
            } else {
                Result.failure(Exception(body?.msg ?: "获取分类音乐失败"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 按分类获取歌手列表（categoryId = 0 为推荐歌手）
     */
    suspend fun getMusicAuthorListByCategoryId(
        categoryId: Int = 0,
        pageNum: Int = 1,
        pageSize: Int = 5
    ): Result<List<MusicAuthor>> {
        return try {
            val response = apiService.getMusicAuthorListByCategoryId(categoryId, pageNum, pageSize)
            val body = response.body()
            if (response.isSuccessful && body?.status == "SUCCESS") {
                Result.success(body.data ?: emptyList())
            } else {
                Result.failure(Exception(body?.msg ?: "获取歌手列表失败"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
