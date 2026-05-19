package com.mroldl001.mimochat.domain.model

enum class ApiErrorCode(val code: Int, val displayMessage: String) {
    BAD_REQUEST(400, "请求格式错误(400)"),
    UNAUTHORIZED(401, "认证失败(401)"),
    PAYMENT_REQUIRED(402, "余额不足(402)"),
    FORBIDDEN(403, "拒绝访问(403)"),
    NOT_FOUND(404, "资源未找到(404)"),
    CONTENT_BLOCKED(421, "内容拦截(421)"),
    TOO_MANY_REQUESTS(429, "请求超限(429)"),
    INTERNAL_SERVER_ERROR(500, "服务器错误(500)"),
    SERVICE_UNAVAILABLE(503, "服务器繁忙(503)");

    companion object {
        fun fromCode(code: Int): ApiErrorCode? {
            return values().find { it.code == code }
        }

        fun getDisplayMessage(code: Int): String {
            return fromCode(code)?.displayMessage ?: "请求失败($code)"
        }
    }
}
