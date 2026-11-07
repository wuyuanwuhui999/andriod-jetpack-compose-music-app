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

    /** 分类歌曲列表页（首页分类模块点「更多」进入）：classifyId + classifyName 作为参数 */
    object MusicClassifyList : Screens(
        "music_classify?classifyId={classifyId}&classifyName={classifyName}"
    ) {
        fun createRoute(classifyId: Int, classifyName: String): String {
            return "music_classify?classifyId=$classifyId" +
                "&classifyName=${Uri.encode(classifyName)}"
        }
    }

    /** 我的收藏夹歌曲列表页（我的页点收藏夹进入）：directoryId + directoryName + cover + total */
    object MusicFavoriteList : Screens(
        "music_favorite?directoryId={directoryId}&directoryName={directoryName}" +
            "&cover={cover}&total={total}"
    ) {
        fun createRoute(
            directoryId: Long,
            directoryName: String,
            cover: String?,
            total: Int
        ): String {
            return "music_favorite?directoryId=$directoryId" +
                "&directoryName=${Uri.encode(directoryName)}" +
                "&cover=${Uri.encode(cover ?: "")}" +
                "&total=$total"
        }
    }

    /** 播放记录页（我的页「播放记录」更多进入） */
    object MusicRecord : Screens("music_record")

    /** 通用编辑页（用户信息页各字段进入）：title / field / value / allowEmpty */
    object Edit : Screens(
        "edit?title={title}&field={field}&value={value}&allowEmpty={allowEmpty}"
    ) {
        fun createRoute(
            title: String,
            field: String,
            value: String,
            allowEmpty: Boolean
        ): String {
            return "edit?title=${Uri.encode(title)}" +
                "&field=$field" +
                "&value=${Uri.encode(value)}" +
                "&allowEmpty=$allowEmpty"
        }
    }

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
