package com.aptiv.deeplink

import android.content.Context
import android.content.Intent

/**
 * DeepLink 路由信息
 */
data class Route(
    val scheme: String,
    val host: String,
    val pattern: String,
    val action: (context: Context, uri: DeepLinkUri) -> Intent?
) {
    /**
     * 检查 URI 是否匹配这个路由
     */
    fun matches(uri: DeepLinkUri): Boolean {
        if (uri.scheme != scheme || uri.host != host) {
            return false
        }

        return pathMatches(uri.path)
    }

    /**
     * 路径匹配逻辑，支持通配符 {param}
     */
    private fun pathMatches(path: String): Boolean {
        val patternSegments = pattern.split("/").filter { it.isNotEmpty() }
        val pathSegments = path.split("/").filter { it.isNotEmpty() }

        if (patternSegments.size != pathSegments.size) {
            return false
        }

        patternSegments.forEachIndexed { index, segment ->
            if (!segment.startsWith("{") && segment != pathSegments[index]) {
                return false
            }
        }

        return true
    }

    /**
     * 从路径中提取参数
     */
    fun extractPathParams(path: String): Map<String, String> {
        val params = mutableMapOf<String, String>()
        val patternSegments = pattern.split("/").filter { it.isNotEmpty() }
        val pathSegments = path.split("/").filter { it.isNotEmpty() }

        patternSegments.forEachIndexed { index, segment ->
            if (segment.startsWith("{") && segment.endsWith("}")) {
                val paramName = segment.substring(1, segment.length - 1)
                params[paramName] = pathSegments[index]
            }
        }

        return params
    }

    /**
     * 创建 Intent
     */
    fun createIntent(context: Context, uri: DeepLinkUri): Intent? {
        return action.invoke(context, uri)
    }
}

/**
 * 路由构建器
 */
class RouteBuilder(
    private val scheme: String,
    private val host: String,
    private val pattern: String
) {
    private var actionBlock: (context: Context, uri: DeepLinkUri) -> Intent? = { _, _ -> null }

    fun action(block: (context: Context, uri: DeepLinkUri) -> Intent?): RouteBuilder {
        this.actionBlock = block
        return this
    }

    fun build(): Route {
        return Route(scheme, host, pattern, actionBlock)
    }
}
