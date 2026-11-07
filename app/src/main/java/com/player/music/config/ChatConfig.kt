package com.player.music.config

/**
 * AI 聊天相关配置（对应 uni-app 端 common/constant.ts）
 */
object ChatConfig {
    /** 企业 ID：getModelList 接口必传（与 uni-app 端 COMPANY_ID 一致） */
    const val COMPANY_ID = "99e1428abccb11f1a63efea5404fce1b"

    /** AI 对话 WebSocket 路径（token 以查询参数 ?token=Bearer%20xxx 传入） */
    const val WS_PATH = "/service/chat/ws/chat"

    /** 会话记录每页条数 */
    const val PAGE_SIZE = 10
}
