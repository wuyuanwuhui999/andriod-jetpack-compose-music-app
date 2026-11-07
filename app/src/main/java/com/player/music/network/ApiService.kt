package com.player.music.network

import com.player.music.model.AccountLoginRequest
import com.player.music.model.ApiResponse
import com.player.music.model.Circle
import com.player.music.model.CircleLike
import com.player.music.model.Comment
import com.player.music.model.CreateFavoriteDirectoryRequest
import com.player.music.model.FavoriteDirectory
import com.player.music.model.EmailLoginRequest
import com.player.music.model.InsertCommentRequest
import com.player.music.model.InsertCircleRequest
import com.player.music.model.InsertMusicRecordRequest
import com.player.music.model.Music
import com.player.music.model.MusicAuthor
import com.player.music.model.MusicAuthorCategory
import com.player.music.model.MusicCategory
import com.player.music.model.ResetPasswordRequest
import com.player.music.model.SaveLikeRequest
import com.player.music.model.SendEmailRequest
import com.player.music.model.UpdatePasswordRequest
import com.player.music.model.User
import com.player.music.model.VerifyUserRequest
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * 所有后端接口统一放在此文件中
 * 用户模块接口前缀：/service/user
 */
interface ApiService {

    /** 账号密码登录 */
    @POST("/service/user/login")
    suspend fun loginByUserAccount(@Body request: AccountLoginRequest): Response<ApiResponse<User>>

    /** 获取用户信息（同时验证 token，并返回新 token） */
    @GET("/service/user/getUserData")
    suspend fun getUserData(): Response<ApiResponse<User>>

    /** 邮箱验证码登录 */
    @POST("/service/user/loginByEmail")
    suspend fun loginByEmail(@Body request: EmailLoginRequest): Response<ApiResponse<User>>

    /** 发送邮箱验证码 */
    @POST("/service/user/sendEmailVertifyCode")
    suspend fun sendEmailVerifyCode(@Body request: SendEmailRequest): Response<ApiResponse<Int>>

    /** 用户注册 */
    @POST("/service/user/register")
    suspend fun register(@Body user: User): Response<ApiResponse<User>>

    /** 校验用户是否存在（账号或邮箱） */
    @POST("/service/user/vertifyUser")
    suspend fun verifyUser(@Body request: VerifyUserRequest): Response<ApiResponse<Int>>

    /** 重置密码（邮箱验证码） */
    @POST("/service/user/resetPassword")
    suspend fun resetPassword(@Body request: ResetPasswordRequest): Response<ApiResponse<User>>

    /** 修改密码 */
    @PUT("/service/user/updatePassword")
    suspend fun updatePassword(@Body request: UpdatePasswordRequest): Response<ApiResponse<Int>>

    /** 更新用户信息 */
    @PUT("/service/user/updateUser")
    suspend fun updateUser(@Body user: User): Response<ApiResponse<Any>>

    /** 更新用户头像 */
    @POST("/service/user/updateAvater")
    @Multipart
    suspend fun updateAvatar(@Part file: MultipartBody.Part): Response<ApiResponse<String>>

    // ==================== 音乐模块 /service/music ====================

    /**
     * 搜索框推荐音乐（热门歌曲，含当前用户点赞状态）
     */
    @GET("/service/music/getKeywordMusic")
    suspend fun getKeywordMusic(): Response<ApiResponse<Music>>

    /**
     * 音乐分类（全部榜单分类）
     */
    @GET("/service/music/getMusicCategory")
    suspend fun getMusicCategory(): Response<ApiResponse<List<MusicCategory>>>

    /**
     * 按分类取音乐列表
     * @param categoryId 分类ID
     * @param pageNum 页码
     * @param pageSize 每页数量
     */
    @GET("/service/music/getMusicListByCategoryId")
    suspend fun getMusicListByCategoryId(
        @Query("categoryId") categoryId: Int,
        @Query("pageNum") pageNum: Int = 1,
        @Query("pageSize") pageSize: Int = 10
    ): Response<ApiResponse<List<Music>>>

    /**
     * 按分类取歌手列表
     * @param categoryId 分类ID（0 表示推荐歌手）
     */
    @GET("/service/music/getMusicAuthorListByCategoryId")
    suspend fun getMusicAuthorListByCategoryId(
        @Query("categoryId") categoryId: Int,
        @Query("pageNum") pageNum: Int = 1,
        @Query("pageSize") pageSize: Int = 5
    ): Response<ApiResponse<List<MusicAuthor>>>

    /**
     * 歌手分类（华语 / 欧美 / 韩国 / 日本 ...）
     */
    @GET("/service/music/getMusicAuthorCategory")
    suspend fun getMusicAuthorCategory(): Response<ApiResponse<List<MusicAuthorCategory>>>

    /**
     * 点赞音乐
     * @param id 音乐ID
     */
    @POST("/service/music/insertMusicLike/{id}")
    suspend fun insertMusicLike(@Path("id") id: Long): Response<ApiResponse<Int>>

    /**
     * 按歌手取音乐（歌手详情页）
     * @param authorId 歌手 authorId（注意不是主键 id）
     */
    @GET("/service/music/getMusicListByAuthorId")
    suspend fun getMusicListByAuthorId(
        @Query("authorId") authorId: Long,
        @Query("pageNum") pageNum: Int = 1,
        @Query("pageSize") pageSize: Int = 20
    ): Response<ApiResponse<List<Music>>>

    /**
     * 关注歌手
     * @param authorId 歌手 authorId
     */
    @POST("/service/music/insertFavoriteAuthor/{authorId}")
    suspend fun insertFavoriteAuthor(@Path("authorId") authorId: Long): Response<ApiResponse<Int>>

    /**
     * 取消点赞音乐
     * @param id 音乐ID
     */
    @DELETE("/service/music/deleteMusicLike/{id}")
    suspend fun deleteMusicLike(@Path("id") id: Long): Response<ApiResponse<Int>>

    /**
     * 我喜欢的歌曲（分页）
     */
    @GET("/service/music/getMusicLike")
    suspend fun getMusicLike(
        @Query("pageNum") pageNum: Int = 1,
        @Query("pageSize") pageSize: Int = 20
    ): Response<ApiResponse<List<Music>>>

    /**
     * 播放记录（分页，含播放次数 times）
     */
    @GET("/service/music/getMusicRecord")
    suspend fun getMusicRecord(
        @Query("pageNum") pageNum: Int = 1,
        @Query("pageSize") pageSize: Int = 10
    ): Response<ApiResponse<List<Music>>>

    /**
     * 我关注的歌手（不分页）
     */
    @GET("/service/music/getFavoriteAuthor")
    suspend fun getFavoriteAuthor(): Response<ApiResponse<List<MusicAuthor>>>

    /**
     * 取消关注歌手
     */
    @DELETE("/service/music/deleteFavoriteAuthor/{authorId}")
    suspend fun deleteFavoriteAuthor(@Path("authorId") authorId: Long): Response<ApiResponse<Int>>

    /**
     * 收藏夹列表
     * @param musicId 音乐ID（后端要求 > 0，用于标记 checked）
     */
    @GET("/service/music/getFavoriteDirectory")
    suspend fun getFavoriteDirectory(
        @Query("musicId") musicId: Long
    ): Response<ApiResponse<List<FavoriteDirectory>>>

    /**
     * 创建收藏夹
     */
    @POST("/service/music/insertFavoriteDirectory")
    suspend fun insertFavoriteDirectory(
        @Body request: CreateFavoriteDirectoryRequest
    ): Response<ApiResponse<FavoriteDirectory>>

    /**
     * 一级评论列表（音乐评论）
     */
    @GET("/service/social/getTopCommentList")
    suspend fun getTopCommentList(
        @Query("relationId") relationId: Long,
        @Query("type") type: String,
        @Query("pageNum") pageNum: Int = 1,
        @Query("pageSize") pageSize: Int = 20
    ): Response<ApiResponse<List<Comment>>>

    /**
     * 是否已收藏到收藏夹（data > 0 表示已收藏）
     */
    @GET("/service/music/isMusicFavorite/{musicId}")
    suspend fun isMusicFavorite(@Path("musicId") musicId: Long): Response<ApiResponse<Int>>

    /**
     * 添加到收藏夹（先清空该音乐的所有收藏，再按 favoriteIds 批量插入）
     */
    @POST("/service/music/insertMusicFavorite/{musicId}")
    suspend fun insertMusicFavorite(
        @Path("musicId") musicId: Long,
        @Body favoriteIds: List<Long>
    ): Response<ApiResponse<Int>>

    /**
     * 插入播放记录
     */
    @POST("/service/music/insertMusicRecord")
    suspend fun insertMusicRecord(
        @Body request: InsertMusicRecordRequest
    ): Response<ApiResponse<Long>>

    /**
     * 删除收藏夹
     */
    @DELETE("/service/music/deleteFavoriteDirectory/{directoryId}")
    suspend fun deleteFavoriteDirectory(
        @Path("directoryId") directoryId: Long
    ): Response<ApiResponse<Int>>

    // ==================== 朋友圈 /service/circle ====================

    /**
     * 分页查询朋友圈列表
     * @param type 类型：MUSIC / MOVIE
     */
    @GET("/service/circle/getCircleListByType")
    suspend fun getCircleListByType(
        @Query("type") type: String,
        @Query("pageNum") pageNum: Int = 1,
        @Query("pageSize") pageSize: Int = 10
    ): Response<ApiResponse<List<Circle>>>

    /**
     * 发布朋友圈（分享歌曲到音乐圈）
     */
    @POST("/service/circle/insertCircle")
    suspend fun insertCircle(@Body request: InsertCircleRequest): Response<ApiResponse<Long>>

    // ==================== 社交（评论/点赞） /service/social ====================

    /**
     * 新增评论 / 回复
     * @param request content、parentId、topId、relationId、type
     */
    @POST("/service/social/insertComment")
    suspend fun insertComment(@Body request: InsertCommentRequest): Response<ApiResponse<Comment>>

    /**
     * 点赞（朋友圈 type = MUSIC_CIRCLE）
     */
    @POST("/service/social/saveLike")
    suspend fun saveLike(@Body request: SaveLikeRequest): Response<ApiResponse<CircleLike>>

    /**
     * 取消点赞
     * @param relationId 关联ID（朋友圈ID）
     * @param type 资源类型（MUSIC_CIRCLE）
     */
    @DELETE("/service/social/deleteLike")
    suspend fun deleteLike(
        @Query("relationId") relationId: Long,
        @Query("type") type: String
    ): Response<ApiResponse<Int>>
}
