package com.player.music.utils

/**
 * 一行歌词
 */
data class LyricLine(
    /** 该行开始时间（毫秒） */
    val timeMs: Int,
    /** 歌词文本 */
    val text: String
)

/**
 * LRC 歌词解析（对应 Flutter 端 flutter_lyric 的基本能力）
 *
 * 支持格式：[00:12.34]歌词  [00:12]歌词  一行多个时间标签  [ti:] [ar:] 等元信息自动忽略
 */
object LyricParser {

    private val TIME_TAG = Regex("\\[(\\d{1,3}):(\\d{1,2})(?:[.:](\\d{1,3}))?]")

    /**
     * 解析 LRC 文本为按时间排序的歌词行
     */
    fun parse(lrc: String?): List<LyricLine> {
        if (lrc.isNullOrBlank()) return emptyList()

        val result = mutableListOf<LyricLine>()
        lrc.lineSequence().forEach { rawLine ->
            val line = rawLine.trim()
            if (line.isEmpty()) return@forEach

            val matches = TIME_TAG.findAll(line).toList()
            if (matches.isEmpty()) return@forEach

            val text = line.substring(matches.last().range.last + 1).trim()
            if (text.isEmpty()) return@forEach

            matches.forEach { match ->
                val minutes = match.groupValues[1].toIntOrNull() ?: return@forEach
                val seconds = match.groupValues[2].toIntOrNull() ?: return@forEach
                val fractionRaw = match.groupValues[3]
                // 毫秒部分可能是 1~3 位，补齐成 3 位
                val fraction = when (fractionRaw.length) {
                    0 -> 0
                    1 -> fractionRaw.toIntOrNull()?.times(100) ?: 0
                    2 -> fractionRaw.toIntOrNull()?.times(10) ?: 0
                    else -> fractionRaw.take(3).toIntOrNull() ?: 0
                }
                result.add(
                    LyricLine(
                        timeMs = minutes * 60_000 + seconds * 1_000 + fraction,
                        text = text
                    )
                )
            }
        }
        return result.sortedBy { it.timeMs }
    }

    /**
     * 当前播放时间对应的歌词行下标，未开始时返回 0
     */
    fun currentIndex(lines: List<LyricLine>, positionMs: Int): Int {
        if (lines.isEmpty()) return -1
        var index = 0
        for (i in lines.indices) {
            if (lines[i].timeMs <= positionMs) index = i else break
        }
        return index
    }
}
