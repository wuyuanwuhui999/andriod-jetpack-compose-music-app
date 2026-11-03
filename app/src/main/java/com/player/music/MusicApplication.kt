package com.player.music

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * 应用入口 Application，用于初始化 Hilt 依赖注入
 */
@HiltAndroidApp
class MusicApplication : Application()
