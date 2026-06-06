package com.aptiv.deeplink

import android.content.Context
import android.content.Intent
import android.net.Uri

/**
 * DeepLink 路由管理器
 */
class DeepLinkRouter private constructor(
    private val routes: MutableList<Route> = mutableListOf(),
    private val interceptors: MutableList<DeepLinkInterceptor> = mutableListOf(),
    private val defaultAction: ((Context, DeepLinkUri) -> Intent?)? = null
) {

    /**
     * 处理 DeepLink
     */
    fun handle(context: Context, uri: String): Boolean {
        return handle(context, Uri.parse(uri))
    }

    /**
     * 处理 DeepLink
     */
    fun handle(context: Context, uri: Uri): Boolean {
        val deepLinkUri = DeepLinkUri.parse(uri.toString()) ?: return false
        return handle(context, deepLinkUri)
    }

    /**
     * 处理 DeepLink
     */
    fun handle(context: Context, deepLinkUri: DeepLinkUri): Boolean {
        // 查找匹配的路由
        val route = findRoute(deepLinkUri)
        val intent = route?.createIntent(context, deepLinkUri) 
            ?: defaultAction?.invoke(context, deepLinkUri)

        if (intent == null) {
            return false
        }

        // 通过拦截器链
        val request = DeepLinkRequest(context, deepLinkUri, intent)
        val chain = RealInterceptorChain(interceptors, 0, request)
        
        return if (chain.proceed(request)) {
            true
        } else {
            try {
                context.startActivity(intent)
                true
            } catch (e: Exception) {
                android.util.Log.e("DeepLink", "Failed to start activity", e)
                false
            }
        }
    }

    /**
     * 查找匹配的路由
     */
    private fun findRoute(deepLinkUri: DeepLinkUri): Route? {
        return routes.firstOrNull { it.matches(deepLinkUri) }
    }

    /**
     * 添加路由
     */
    fun addRoute(route: Route): DeepLinkRouter {
        routes.add(route)
        return this
    }

    /**
     * 添加路由（使用 DSL）
     */
    fun route(
        scheme: String,
        host: String,
        pattern: String,
        action: (context: Context, uri: DeepLinkUri) -> Intent?
    ): DeepLinkRouter {
        val route = RouteBuilder(scheme, host, pattern)
            .action(action)
            .build()
        routes.add(route)
        return this
    }

    /**
     * 添加拦截器
     */
    fun addInterceptor(interceptor: DeepLinkInterceptor): DeepLinkRouter {
        interceptors.add(interceptor)
        return this
    }

    companion object {
        /**
         * 创建 DeepLinkRouter 实例
         */
        fun create(block: (Builder.() -> Unit)? = null): DeepLinkRouter {
            val builder = Builder()
            block?.invoke(builder)
            return builder.build()
        }
    }

    /**
     * 构建器
     */
    class Builder {
        private val routes = mutableListOf<Route>()
        private val interceptors = mutableListOf<DeepLinkInterceptor>()
        private var defaultAction: ((Context, DeepLinkUri) -> Intent?)? = null

        fun route(
            scheme: String,
            host: String,
            pattern: String,
            action: (context: Context, uri: DeepLinkUri) -> Intent?
        ): Builder {
            routes.add(
                RouteBuilder(scheme, host, pattern)
                    .action(action)
                    .build()
            )
            return this
        }

        fun interceptor(interceptor: DeepLinkInterceptor): Builder {
            interceptors.add(interceptor)
            return this
        }

        fun defaultAction(action: (context: Context, uri: DeepLinkUri) -> Intent?): Builder {
            this.defaultAction = action
            return this
        }

        fun build(): DeepLinkRouter {
            return DeepLinkRouter(routes, interceptors, defaultAction)
        }
    }
}
