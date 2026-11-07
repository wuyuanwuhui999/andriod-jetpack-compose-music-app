package com.player.music.model

/**
 * 播放循环模式（与 Flutter 端 LoopModeEnum 一致）
 */
enum class LoopMode {
    /** 顺序播放 */
    ORDER,

    /** 随机播放 */
    RANDOM,

    /** 单曲循环 */
    REPEAT
}
