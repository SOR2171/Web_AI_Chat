package com.github.sor2171.backend.utils

object Const {
    const val CORS_ORDER = -102
    const val FLOW_LIMIT_ORDER = -101

    const val JWT_BLACK_LIST = "jwt:blacklist:"
    const val VERIFY_EMAIL_LIMIT = "verify:email:limit:"
    const val VERIFY_EMAIL_DATA = "verify:email:data:"

    const val VERIFY_CHAT_LIMIT = "verify:chat:limit:"
    const val VERIFY_CHAT_SESSION = "verify:chat:session:"

    const val FLOW_LIMIT_COUNTER = "flow:counter:"
    const val FLOW_LIMIT_BLOCK = "flow:block:"

    const val ST_QUEUE_NAME = "st.chat"
    const val ST_EXCHANGE_NAME = "st.exchange"
    const val ST_ROUTING_KEY = "st.request"

    const val MAIL_QUEUE_NAME = "mail"
    const val MAIL_EXCHANGE_NAME = "mail.exchange"
    const val MAIL_ROUTING_KEY = "mail.send"

    const val EMITTER_TIMEOUT = 30 * 60 * 1000L
}