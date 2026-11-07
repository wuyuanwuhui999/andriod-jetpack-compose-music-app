package com.player.music.repository

import com.player.music.model.Music
import com.player.music.model.MusicAuthor
import com.player.music.model.MusicAuthorCategory
import com.player.music.model.Comment
import com.player.music.model.CommentListResult
import com.player.music.model.InsertCommentRequest
import com.player.music.model.InsertMusicRecordRequest
import com.player.music.model.CreateFavoriteDirectoryRequest
import com.player.music.model.FavoriteDirectory
import com.player.music.model.MusicCategory
import com.player.music.model.MusicListResult
import com.player.music.network.ApiService
import javax.inject.Inject

/**
 * 音乐模块仓库：负责调用 /service/music 下的接口，统一返回 Result
 */
class MusicRepository @Inject constructor(
    private val apiService: ApiService
) {

    companion object {
        /** 评论资源类型：音乐 */
        const val TYPE_MUSIC = "MUSIC"
    }

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
     * 搜索音乐（歌名 / 歌手 / 专辑 模糊匹配），同时返回总条数
     */
    suspend fun searchMusic(
        keyword: String,
        pageNum: Int = 1,
        pageSize: Int = 20
    ): Result<MusicListResult> {
        return try {
            val response = apiService.searchMusic(keyword, pageNum, pageSize)
            val body = response.body()
            if (response.isSuccessful && body?.status == "SUCCESS") {
                Result.success(
                    MusicListResult(
                        list = body.data ?: emptyList(),
                        total = body.total ?: 0
                    )
                )
            } else {
                Result.failure(Exception(body?.msg ?: "搜索音乐失败"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 按分类分页获取音乐列表（同时返回总条数，供推荐页判断是否还有更多）
     */
    suspend fun getMusicPageByCategoryId(
        categoryId: Int,
        pageNum: Int = 1,
        pageSize: Int = 10
    ): Result<MusicListResult> {
        return try {
            val response = apiService.getMusicListByCategoryId(categoryId, pageNum, pageSize)
            val body = response.body()
            if (response.isSuccessful && body?.status == "SUCCESS") {
                Result.success(
                    MusicListResult(
                        list = body.data ?: emptyList(),
                        total = body.total ?: 0
                    )
                )
            } else {
                Result.failure(Exception(body?.msg ?: "获取分类音乐失败"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 收藏夹里的歌曲列表（分页）
     * @param favoriteId 收藏夹ID
     */
    suspend fun getMusicPageByFavoriteId(
        favoriteId: Long,
        pageNum: Int = 1,
        pageSize: Int = 20
    ): Result<MusicListResult> {
        return try {
            val response = apiService.getMusicListByFavoriteId(favoriteId, pageNum, pageSize)
            val body = response.body()
            if (response.isSuccessful && body?.status == "SUCCESS") {
                Result.success(
                    MusicListResult(
                        list = body.data ?: emptyList(),
                        total = body.total ?: 0
                    )
                )
            } else {
                Result.failure(Exception(body?.msg ?: "获取收藏夹歌曲失败"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 点赞音乐
     * @param musicId 音乐ID
     * @return 影响行数，>0 表示成功
     */
    suspend fun likeMusic(musicId: Long): Result<Int> {
        return try {
            val response = apiService.insertMusicLike(musicId)
            val body = response.body()
            if (response.isSuccessful && body?.status == "SUCCESS") {
                Result.success(body.data ?: 0)
            } else {
                Result.failure(Exception(body?.msg ?: "点赞失败"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 取消点赞音乐
     * @param musicId 音乐ID
     * @return 影响行数，>0 表示成功
     */
    suspend fun unlikeMusic(musicId: Long): Result<Int> {
        return try {
            val response = apiService.deleteMusicLike(musicId)
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

    /**
     * 歌手分类列表（华语 / 欧美 / 韩国 / 日本 ...）
     */
    suspend fun getMusicAuthorCategories(): Result<List<MusicAuthorCategory>> {
        return try {
            val response = apiService.getMusicAuthorCategory()
            val body = response.body()
            if (response.isSuccessful && body?.status == "SUCCESS") {
                Result.success(body.data ?: emptyList())
            } else {
                Result.failure(Exception(body?.msg ?: "获取歌手分类失败"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 我喜欢的歌曲（分页，带总数）
     */
    suspend fun getMusicLike(pageNum: Int = 1, pageSize: Int = 5): Result<MusicListResult> {
        return try {
            val response = apiService.getMusicLike(pageNum, pageSize)
            val body = response.body()
            if (response.isSuccessful && body?.status == "SUCCESS") {
                Result.success(
                    MusicListResult(list = body.data ?: emptyList(), total = body.total ?: 0)
                )
            } else {
                Result.failure(Exception(body?.msg ?: "获取喜欢的歌曲失败"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 播放记录（分页，带总数，列表项含 times 播放次数）
     */
    suspend fun getMusicRecord(pageNum: Int = 1, pageSize: Int = 10): Result<MusicListResult> {
        return try {
            val response = apiService.getMusicRecord(pageNum, pageSize)
            val body = response.body()
            if (response.isSuccessful && body?.status == "SUCCESS") {
                Result.success(
                    MusicListResult(list = body.data ?: emptyList(), total = body.total ?: 0)
                )
            } else {
                Result.failure(Exception(body?.msg ?: "获取播放记录失败"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 我关注的歌手（后端不分页，总数取列表长度）
     */
    suspend fun getFavoriteAuthors(): Result<List<MusicAuthor>> {
        return try {
            val response = apiService.getFavoriteAuthor()
            val body = response.body()
            if (response.isSuccessful && body?.status == "SUCCESS") {
                Result.success(body.data ?: emptyList())
            } else {
                Result.failure(Exception(body?.msg ?: "获取关注的歌手失败"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 取消关注歌手
     */
    suspend fun deleteFavoriteAuthor(authorId: Long): Result<Int> {
        return try {
            val response = apiService.deleteFavoriteAuthor(authorId)
            val body = response.body()
            if (response.isSuccessful && body?.status == "SUCCESS") {
                Result.success(body.data ?: 0)
            } else {
                Result.failure(Exception(body?.msg ?: "取消关注失败"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 关注歌手
     */
    suspend fun followAuthor(authorId: Long): Result<Int> {
        return try {
            val response = apiService.insertFavoriteAuthor(authorId)
            val body = response.body()
            if (response.isSuccessful && body?.status == "SUCCESS") {
                Result.success(body.data ?: 0)
            } else {
                Result.failure(Exception(body?.msg ?: "关注失败"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 按歌手取音乐（歌手详情页）
     * @param authorId 歌手 authorId（不是主键 id）
     */
    suspend fun getMusicListByAuthorId(
        authorId: Long,
        pageNum: Int = 1,
        pageSize: Int = 20
    ): Result<List<Music>> {
        return try {
            val response = apiService.getMusicListByAuthorId(authorId, pageNum, pageSize)
            val body = response.body()
            if (response.isSuccessful && body?.status == "SUCCESS") {
                Result.success(body.data ?: emptyList())
            } else {
                Result.failure(Exception(body?.msg ?: "获取歌手歌曲失败"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 我的收藏夹列表
     * @param musicId 后端要求必传且 > 0，用于标记 checked 字段
     */
    suspend fun getFavoriteDirectories(musicId: Long): Result<List<FavoriteDirectory>> {
        return try {
            val response = apiService.getFavoriteDirectory(musicId)
            val body = response.body()
            if (response.isSuccessful && body?.status == "SUCCESS") {
                Result.success(body.data ?: emptyList())
            } else {
                Result.failure(Exception(body?.msg ?: "获取收藏夹失败"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 创建收藏夹
     */
    suspend fun createFavoriteDirectory(name: String): Result<FavoriteDirectory?> {
        return try {
            val response = apiService.insertFavoriteDirectory(CreateFavoriteDirectoryRequest(name))
            val body = response.body()
            if (response.isSuccessful && body?.status == "SUCCESS") {
                Result.success(body.data)
            } else {
                Result.failure(Exception(body?.msg ?: "创建收藏夹失败"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 删除收藏夹
     */
    suspend fun deleteFavoriteDirectory(directoryId: Long): Result<Int> {
        return try {
            val response = apiService.deleteFavoriteDirectory(directoryId)
            val body = response.body()
            if (response.isSuccessful && body?.status == "SUCCESS") {
                Result.success(body.data ?: 0)
            } else {
                Result.failure(Exception(body?.msg ?: "删除收藏夹失败"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }


    // ==================== 播放页相关 ====================

    /**
     * 音乐一级评论列表
     */
    suspend fun getTopComments(
        relationId: Long,
        pageNum: Int = 1,
        pageSize: Int = 20
    ): Result<CommentListResult> {
        return try {
            val response = apiService.getTopCommentList(
                relationId = relationId,
                type = TYPE_MUSIC,
                pageNum = pageNum,
                pageSize = pageSize
            )
            val body = response.body()
            if (response.isSuccessful && body?.status == "SUCCESS") {
                Result.success(
                    CommentListResult(
                        list = body.data ?: emptyList(),
                        total = body.total ?: 0
                    )
                )
            } else {
                Result.failure(Exception(body?.msg ?: "获取评论失败"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 发表音乐评论
     */
    suspend fun insertMusicComment(
        relationId: Long,
        content: String
    ): Result<Comment?> {
        return try {
            val response = apiService.insertComment(
                InsertCommentRequest(
                    content = content,
                    parentId = 0,
                    topId = 0,
                    relationId = relationId,
                    type = TYPE_MUSIC
                )
            )
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
     * 是否已收藏到某个收藏夹（> 0 表示已收藏）
     */
    suspend fun isMusicFavorite(musicId: Long): Result<Int> {
        return try {
            val response = apiService.isMusicFavorite(musicId)
            val body = response.body()
            if (response.isSuccessful && body?.status == "SUCCESS") {
                Result.success(body.data ?: 0)
            } else {
                Result.failure(Exception(body?.msg ?: "查询收藏状态失败"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 保存音乐到收藏夹（传空列表表示清空收藏）
     */
    suspend fun saveMusicFavorite(musicId: Long, favoriteIds: List<Long>): Result<Int> {
        return try {
            val response = apiService.insertMusicFavorite(musicId, favoriteIds)
            val body = response.body()
            if (response.isSuccessful && body?.status == "SUCCESS") {
                Result.success(body.data ?: 0)
            } else {
                Result.failure(Exception(body?.msg ?: "保存收藏失败"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 插入播放记录
     */
    suspend fun insertMusicRecord(musicId: Long): Result<Long> {
        return try {
            val response = apiService.insertMusicRecord(
                InsertMusicRecordRequest(
                    musicId = musicId,
                    platform = "android",
                    version = "1.0.0",
                    device = android.os.Build.MODEL ?: "android"
                )
            )
            val body = response.body()
            if (response.isSuccessful && body?.status == "SUCCESS") {
                Result.success(body.data ?: 0L)
            } else {
                Result.failure(Exception(body?.msg ?: "插入播放记录失败"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

}
