package com.player.music.local

object PreferenceKeys {
    const val TOKEN = "token"
    const val USER_DATA = "user_data"
    const val IS_LOGGED_IN = "is_logged_in"

    /** 搜索页历史关键词（逗号分隔，与 Flutter 端 historyMusicLabels 一致） */
    const val SEARCH_HISTORY = "history_music_labels"
}
