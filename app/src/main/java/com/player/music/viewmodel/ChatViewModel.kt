package com.player.music.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.player.music.model.AiModel
import com.player.music.model.ChatHistoryGroup
import com.player.music.model.ChatHistoryItem
import com.player.music.model.ChatHistoryResult
import com.player.music.model.ChatMessage
import com.player.music.model.ChatRole
import com.player.music.model.ChatSession
import com.player.music.model.Music
import com.player.music.player.MusicPlayerManager
import com.player.music.config.ChatConfig
import com.player.music.repository.ChatRepository
import com.player.music.local.DataStoreManager
import com.player.music.utils.ChatContentParser
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import javax.inject.Inject

/**
 * AI 聊天页 ViewModel
 * 对应 Flutter 端 lib/pages/ChatPage.dart + uni-app 端 pages/ChatPage.vue：
 * WebSocket 流式对话（深度思考 <think> 分流）、切换模型、切换语言、新建会话、
 * 会话记录（按时间分组、分页、点击进入）、AI 回复里的 <music> 歌单卡片播放
 */
@HiltViewModel
class ChatViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val dataStoreManager: DataStoreManager,
    val player: MusicPlayerManager
) : ViewModel() {

    companion object {
        const val GREETING = "你好，我是智能音乐助手小吴同学，请问有什么可以帮助您？"
    }

    // ============ 对话 ============

    private val _messages = MutableStateFlow(
        listOf(ChatMessage(role = ChatRole.AI, responseContent = GREETING))
    )
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _input = MutableStateFlow("")
    val input: StateFlow<String> = _input.asStateFlow()

    /** 正在等待/接收回复（输入框右侧显示"发送中"，且禁止重复发送） */
    private val _thinking = MutableStateFlow(false)
    val thinking: StateFlow<Boolean> = _thinking.asStateFlow()

    /** 深度思考开关 */
    private val _showThink = MutableStateFlow(false)
    val showThink: StateFlow<Boolean> = _showThink.asStateFlow()

    /** 回复语言：zh / en */
    private val _language = MutableStateFlow("zh")
    val language: StateFlow<String> = _language.asStateFlow()

    // ============ 模型 ============

    private val _models = MutableStateFlow<List<AiModel>>(emptyList())
    val models: StateFlow<List<AiModel>> = _models.asStateFlow()

    private val _activeModelIndex = MutableStateFlow(0)
    val activeModelIndex: StateFlow<Int> = _activeModelIndex.asStateFlow()

    private val _userAvatar = MutableStateFlow<String?>(null)
    val userAvatar: StateFlow<String?> = _userAvatar.asStateFlow()

    // ============ 会话记录 ============

    private val _historyGroups = MutableStateFlow<List<ChatHistoryGroup>>(emptyList())
    val historyGroups: StateFlow<List<ChatHistoryGroup>> = _historyGroups.asStateFlow()

    private val _showHistory = MutableStateFlow(false)
    val showHistory: StateFlow<Boolean> = _showHistory.asStateFlow()

    private val _historyLoading = MutableStateFlow(false)
    val historyLoading: StateFlow<Boolean> = _historyLoading.asStateFlow()

    private val _historyHasMore = MutableStateFlow(false)
    val historyHasMore: StateFlow<Boolean> = _historyHasMore.asStateFlow()

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    // ============ 内部状态 ============

    private var socket: WebSocket? = null
    private val streamBuffer = StringBuilder()
    private var pendingPrompt: String = ""
    private var chatId: String = ""
    private var historyPage = 1
    private var historyTotal = 0
    /** 已累积的历史记录（分页合并后重新分组，避免同一会话被拆到两页） */
    private val historyItems = mutableListOf<ChatHistoryItem>()

    init {
        loadModels()
        loadUserAvatar()
    }

    private fun loadModels() {
        viewModelScope.launch {
            val result = chatRepository.getModelList()
            if (result.isSuccess) {
                _models.value = result.getOrNull() ?: emptyList()
                _activeModelIndex.value = 0
            } else {
                _toastMessage.value = result.exceptionOrNull()?.message ?: "获取模型列表失败"
            }
        }
    }

    private fun loadUserAvatar() {
        viewModelScope.launch {
            _userAvatar.value = dataStoreManager.getUser().firstOrNull()?.avatar
        }
    }

    // ============ 输入 / 开关 ============

    fun onInputChange(value: String) {
        _input.value = value
    }

    fun clearInput() {
        _input.value = ""
    }

    fun toggleThink() {
        _showThink.value = !_showThink.value
    }

    fun toggleLanguage() {
        _language.value = if (_language.value == "zh") "en" else "zh"
    }

    fun selectModel(index: Int) {
        if (index in _models.value.indices) _activeModelIndex.value = index
    }

    /** 新建会话：清空对话、换新的 chatId */
    fun newChat() {
        closeSocket()
        chatId = ""
        streamBuffer.setLength(0)
        _thinking.value = false
        _input.value = ""
        _messages.value = listOf(ChatMessage(role = ChatRole.AI, responseContent = GREETING))
    }

    // ============ 发送 ============

    fun send() {
        val prompt = _input.value.trim()
        if (prompt.isEmpty()) {
            _toastMessage.value = "请输入聊天内容"
            return
        }
        if (_thinking.value) return
        val model = _models.value.getOrNull(_activeModelIndex.value)
        if (model == null) {
            _toastMessage.value = "模型列表加载中，请稍候"
            return
        }
        if (chatId.isBlank()) chatId = UUID.randomUUID().toString().replace("-", "")

        pendingPrompt = prompt
        streamBuffer.setLength(0)
        _messages.value = _messages.value +
            ChatMessage(role = ChatRole.USER, text = prompt) +
            ChatMessage(role = ChatRole.AI, streaming = true)
        _input.value = ""
        _thinking.value = true

        viewModelScope.launch {
            val token = dataStoreManager.getToken().firstOrNull()
            if (token.isNullOrBlank()) {
                _thinking.value = false
                _toastMessage.value = "登录状态已失效，请重新登录"
                return@launch
            }
            // 每次提问新开一个连接（与 Flutter / uni-app 端一致）
            closeSocket()
            socket = chatRepository.openChatSocket(token, socketListener)
            if (socket == null) {
                _thinking.value = false
                _toastMessage.value = "连接 AI 服务失败"
            }
        }
    }

    private val socketListener = object : WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: Response) {
            val payload = JSONObject().apply {
                put("modelId", _models.value.getOrNull(_activeModelIndex.value)?.id ?: "")
                put("chatId", chatId)
                put("prompt", pendingPrompt)
                put("showThink", _showThink.value)
                put("language", _language.value)
                // 后端 ChatParamsEntity 要求 companyId 必须为字符串（Flutter 端未传，这里按接口文档补上）
                put("companyId", ChatConfig.COMPANY_ID)
                put("type", "")
            }
            webSocket.send(payload.toString())
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            if (text == "[completed]") {
                finishStream()
                webSocket.close(1000, "done")
                return
            }
            streamBuffer.append(text)
            val (think, response) = ChatContentParser.splitThink(streamBuffer.toString())
            updateLastAiMessage(thinkContent = think, responseContent = response)
            _thinking.value = false
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            _thinking.value = false
            updateLastAiMessage(streaming = false)
            _toastMessage.value = "连接错误：${t.message ?: "网络异常"}"
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            finishStream()
        }
    }

    private fun updateLastAiMessage(
        thinkContent: String? = null,
        responseContent: String? = null,
        streaming: Boolean? = null
    ) {
        val list = _messages.value
        val index = list.indexOfLast { it.role == ChatRole.AI }
        if (index < 0) return
        val old = list[index]
        val updated = old.copy(
            thinkContent = thinkContent ?: old.thinkContent,
            responseContent = responseContent ?: old.responseContent,
            streaming = streaming ?: old.streaming
        )
        _messages.value = list.toMutableList().also { it[index] = updated }
    }

    private fun finishStream() {
        _thinking.value = false
        updateLastAiMessage(streaming = false)
        closeSocket()
    }

    private fun closeSocket() {
        runCatching { socket?.close(1000, null) }
        socket = null
    }

    // ============ 会话记录 ============

    fun openHistory() {
        _showHistory.value = true
        historyPage = 1
        loadHistory(reset = true)
    }

    fun closeHistory() {
        _showHistory.value = false
    }

    /** 会话记录上拉加载下一页 */
    fun loadMoreHistory() {
        if (_historyLoading.value || !_historyHasMore.value) return
        loadHistory(reset = false)
    }

    private fun loadHistory(reset: Boolean) {
        if (reset) {
            historyItems.clear()
            historyPage = 1
            _historyGroups.value = emptyList()
        }
        _historyLoading.value = true
        viewModelScope.launch {
            val result = chatRepository.getChatHistory(historyPage, ChatConfig.PAGE_SIZE)
            if (result.isSuccess) {
                val page = result.getOrNull() ?: ChatHistoryResult()
                historyTotal = page.total
                val existIds = historyItems.map { it.id }.toSet()
                // 合并所有已加载记录后重新分组，避免同一会话跨页被拆开
                historyItems.addAll(page.list.filterNot { it.id in existIds })
                _historyGroups.value = groupHistory(historyItems)
                _historyHasMore.value = historyItems.size < historyTotal
                historyPage += 1
            } else {
                _toastMessage.value = result.exceptionOrNull()?.message ?: "获取会话记录失败"
            }
            _historyLoading.value = false
        }
    }

    /**
     * 历史记录 → 会话（按 chatId 聚合）→ 按时间分组（与 uni-app 端一致）
     */
    private fun groupHistory(items: List<ChatHistoryItem>): List<ChatHistoryGroup> {
        val byChat = LinkedHashMap<String, MutableList<ChatHistoryItem>>()
        items.forEach { item ->
            byChat.getOrPut(item.chatId) { mutableListOf() }.add(item)
        }
        val groups = LinkedHashMap<String, MutableList<ChatSession>>()
        byChat.forEach { (id, list) ->
            val ordered = list.sortedBy { it.createTime ?: "" }
            val timeAgo = formatTimeAgo(ordered.firstOrNull()?.createTime)
            val title = ordered.firstOrNull()?.prompt?.takeIf { it.isNotBlank() } ?: "(无标题会话)"
            groups.getOrPut(timeAgo) { mutableListOf() }
                .add(ChatSession(chatId = id, title = title, timeAgo = timeAgo, items = ordered))
        }
        return groups.map { (timeAgo, sessions) -> ChatHistoryGroup(timeAgo, sessions) }
    }

    /** 打开某个历史会话 */
    fun openSession(session: ChatSession) {
        closeSocket()
        chatId = session.chatId
        streamBuffer.setLength(0)
        _thinking.value = false
        val list = mutableListOf<ChatMessage>()
        session.items.forEach { item ->
            list.add(ChatMessage(role = ChatRole.USER, text = item.prompt ?: ""))
            list.add(
                ChatMessage(
                    role = ChatRole.AI,
                    thinkContent = item.thinkContent ?: "",
                    responseContent = item.responseContent ?: ""
                )
            )
        }
        _messages.value = list
        _showHistory.value = false
    }

    // ============ 播放 AI 推荐的歌曲 ============

    fun playMusic(list: List<Music>, index: Int) {
        if (list.isEmpty()) return
        player.playList(list, index.coerceIn(0, list.lastIndex))
    }

    fun clearToast() {
        _toastMessage.value = null
    }

    /** 相对时间：刚刚 / x分钟前 / x小时前 / x天前 / x个月前 / x年前 */
    private fun formatTimeAgo(createTime: String?): String {
        if (createTime.isNullOrBlank()) return "较早"
        val format = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA)
        val time = runCatching { format.parse(createTime)?.time }.getOrNull() ?: return "较早"
        val diff = System.currentTimeMillis() - time
        if (diff < 0) return "刚刚"
        val minutes = diff / 60_000
        val hours = minutes / 60
        val days = hours / 24
        return when {
            minutes < 1 -> "刚刚"
            minutes < 60 -> "${minutes}分钟前"
            hours < 24 -> "${hours}小时前"
            days < 30 -> "${days}天前"
            days < 365 -> "${days / 30}个月前"
            else -> "${days / 365}年前"
        }
    }

    override fun onCleared() {
        super.onCleared()
        closeSocket()
    }
}
