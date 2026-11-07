package com.player.music.utils

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.player.music.model.Music

/**
 * 大模型返回正文的解析工具
 * 对应 uni-app 端 utils/util.ts 的 splitThink / parseContent / parseMusicList：
 * - `<think>...</think>` 是深度思考内容，与正文分开展示
 * - `<music>[...]</music>` 里是歌曲 JSON 列表，渲染成音乐卡片
 */
object ChatContentParser {

    private val gson = Gson()

    private val thinkRegex = Regex("(?s)<think>(.*?)(</think>|$)")
    private val musicRegex = Regex("(?s)<music\\s*>(.*?)</music\\s*>")
    private val unclosedMusicRegex = Regex("(?s)<music\\s*>.*$")

    /** 正文片段：文本 或 歌曲列表 */
    data class Segment(
        val text: String = "",
        val musicList: List<Music> = emptyList()
    ) {
        val isMusic: Boolean get() = musicList.isNotEmpty()
    }

    /**
     * 从流式缓冲里拆出「思考内容」和「正文」
     */
    fun splitThink(raw: String): Pair<String, String> {
        if (raw.isEmpty()) return "" to ""
        val think = thinkRegex.findAll(raw)
            .joinToString(separator = "") { it.groupValues[1] }
            .trim()
        val response = thinkRegex.replace(raw, "").trim()
        return think to response
    }

    /**
     * 把正文按 `<music>` 标签拆成 文本 / 音乐列表 片段
     * @param completed 是否已生成完成；未完成时丢弃半截的 `<music>` 片段
     */
    fun parseSegments(content: String, completed: Boolean): List<Segment> {
        if (content.isBlank()) return emptyList()
        val segments = mutableListOf<Segment>()
        var lastIndex = 0
        musicRegex.findAll(content).forEach { match ->
            val prev = content.substring(lastIndex, match.range.first)
            if (prev.isNotBlank()) segments.add(Segment(text = prev))
            val musicList = parseMusicList(match.groupValues[1])
            if (musicList.isNotEmpty()) {
                segments.add(Segment(musicList = musicList))
            } else {
                // 解析失败时原样展示，避免内容丢失
                segments.add(Segment(text = match.value))
            }
            lastIndex = match.range.last + 1
        }
        var rest = content.substring(lastIndex)
        // 生成中：丢掉未闭合的 <music> 半截内容
        if (!completed) rest = unclosedMusicRegex.replace(rest, "")
        if (rest.isNotBlank()) segments.add(Segment(text = rest))
        return segments
    }

    /**
     * 解析 `<music>` 里的歌曲 JSON（兼容单引号、True/False/None、尾随逗号、外层包对象）
     */
    fun parseMusicList(raw: String): List<Music> {
        var text = raw.trim()
        text = text.replace(Regex("^```[a-zA-Z]*\\s*"), "").replace(Regex("\\s*```$"), "").trim()
        if (text.isEmpty()) return emptyList()

        val normalized = text
            .replace("'", "\"")
            .replace(Regex("\\bTrue\\b"), "true")
            .replace(Regex("\\bFalse\\b"), "false")
            .replace(Regex("\\bNone\\b"), "null")
            .replace(Regex(",\\s*([\\]}])"), "$1")

        val parsed = runCatching { gson.fromJson(text, Any::class.java) }.getOrNull()
            ?: runCatching { gson.fromJson(normalized, Any::class.java) }.getOrNull()
            ?: return emptyList()

        val list = when (parsed) {
            is List<*> -> parsed
            is Map<*, *> -> (parsed["musicList"] ?: parsed["data"] ?: parsed["list"]) as? List<*>
            else -> null
        } ?: return emptyList()

        val type = object : TypeToken<List<Music>>() {}.type
        return runCatching {
            gson.fromJson<List<Music>>(gson.toJson(list), type)
        }.getOrNull()?.filter { it.id > 0 } ?: emptyList()
    }

    /**
     * 极简富文本处理：去掉常见 markdown 标记，让纯文本气泡不至于满是 `**`
     * （完整的 markdown / HTML 渲染未实现，见 README 说明）
     */
    fun stripMarkdown(text: String): String {
        var s = text
        s = s.replace(Regex("```[a-zA-Z]*\\n?"), "")
        s = s.replace(Regex("`([^`]*)`"), "$1")
        s = s.replace(Regex("\\*\\*([^*]*)\\*\\*"), "$1")
        s = s.replace(Regex("(?m)^#{1,6}\\s*"), "")
        s = s.replace(Regex("\\[([^\\]]*)]\\([^)]*\\)"), "$1")
        s = s.replace(Regex("(?m)^\\s*[-*+]\\s+"), "· ")
        return s.trim()
    }
}
