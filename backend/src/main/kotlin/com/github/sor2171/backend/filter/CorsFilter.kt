package com.github.sor2171.backend.filter

import com.github.sor2171.backend.utils.Const
import org.springframework.core.annotation.Order
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilter
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Mono

@Component
@Order(Const.CORS_ORDER)
class CorsFilter : WebFilter {
    override fun filter(exchange: ServerWebExchange, chain: WebFilterChain): Mono<Void> {
        val request = exchange.request
        val response = exchange.response

        val origin = request.headers.getFirst(HttpHeaders.ORIGIN)
        if (origin != null) {
            response.headers.add(
                HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN,
                origin
            )
        }
        response.headers.add(
            HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS,
            "GET, POST, PUT, DELETE, OPTIONS"
        )
        response.headers.add(
            HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS,
            "Authorization, Content-Type"
        )

        // 处理预检请求直接返回 200
        if (request.method == HttpMethod.OPTIONS) {
            response.statusCode = org.springframework.http.HttpStatus.OK
            return response.setComplete()
        }

        return chain.filter(exchange)
    }
}
