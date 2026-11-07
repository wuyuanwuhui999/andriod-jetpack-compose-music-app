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

    /** 搜索页：keyword 为首页搜索栏带入的推荐关键词（仅作输入框提示） */
    object MusicSearch : Screens("music_search?keyword={keyword}") {
        fun createRoute(keyword: String): String {
            return "music_search?keyword=${Uri.encode(keyword)}"
        }
    }

    /** 选择音乐页（分享到音乐圈时挑一首歌） */
    object MusicPick : Screens("music_pick")

    /** 歌词页（播放页点击歌词进入） */
    object MusicLyric : Screens("music_lyric")

    /** AI 聊天页（首页搜索栏右侧 AI 图标进入） */
    object Chat : Screens("chat")

    /** 歌曲分类页（首页「分类歌曲」快捷入口） */
    object MusicCategory : Screens("music_category")

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
