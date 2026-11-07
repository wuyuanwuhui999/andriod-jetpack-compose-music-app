package com.player.music.navigation

import android.net.Uri

/**
 * 全局路由表
 */
sealed class Screens(val route: String) {
    object Launch : Screens("launch")
    object Login : Screens("login")
    object MusicIndex : Screens("music_index")
    object MusicPlayer : Screens("music_player")
    object User : Screens("user")
    object UpdatePassword : Screens("update_password")
    object ForgetPassword : Screens("forget_password")
    object ResetPassword : Screens("reset_password")
    object Register : Screens("register")

    /** 分享到音乐圈（分享当前播放歌曲） */
    object MusicShare : Screens("music_share")

    /** 歌手分类页（首页「歌手」快捷入口） */
    object MusicAuthorCategory : Screens("music_author_category")

    /** 歌手详情页：authorId / authorName / avatar 作为参数传递 */
    object MusicAuthor : Screens(
        "music_author?authorId={authorId}&authorName={authorName}&avatar={avatar}"
    ) {
        fun createRoute(authorId: Long, authorName: String?, avatar: String?): String {
            return "music_author?authorId=$authorId" +
                "&authorName=${Uri.encode(authorName ?: "")}" +
                "&avatar=${Uri.encode(avatar ?: "")}"
        }
    }
}
