package com.player.music.network

import com.player.music.model.AccountLoginRequest
import com.player.music.model.ApiResponse
import com.player.music.model.EmailLoginRequest
import com.player.music.model.Music
import com.player.music.model.MusicAuthor
import com.player.music.model.MusicCategory
import com.player.music.model.ResetPasswordRequest
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
     * 点赞音乐
     * @param id 音乐ID
     */
    @POST("/service/music/insertMusicLike/{id}")
    suspend fun insertMusicLike(@Path("id") id: Long): Response<ApiResponse<Int>>

    /**
     * 取消点赞音乐
     * @param id 音乐ID
     */
    @DELETE("/service/music/deleteMusicLike/{id}")
    suspend fun deleteMusicLike(@Path("id") id: Long): Response<ApiResponse<Int>>
}
