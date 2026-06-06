package com.aptiv.deeplink

import android.content.Context
import android.content.Intent

/**
 * DeepLink 拦截器接口
 */
interface DeepLinkInterceptor {
    /**
     * 拦截 DeepLink 处理
     * @return 返回 true 表示拦截链中断，不继续处理；返回 false 继续传递
     */
    fun intercept(chain: Chain): Boolean

    interface Chain {
        fun request(): DeepLinkRequest
        fun proceed(request: DeepLinkRequest): Boolean
    }
}

/**
 * DeepLink 请求数据
 */
data class DeepLinkRequest(
    val context: Context,
    val uri: DeepLinkUri,
    val intent: Intent?
)

/**
 * 真实的拦截器链实现
 */
internal class RealInterceptorChain(
    private val interceptors: List<DeepLinkInterceptor>,
    private val index: Int,
    private val request: DeepLinkRequest
) : DeepLinkInterceptor.Chain {

    override fun request(): DeepLinkRequest = request

    override fun proceed(request: DeepLinkRequest): Boolean {
        if (index >= interceptors.size) {
            return false
        }

        val next = RealInterceptorChain(interceptors, index + 1, request)
        val interceptor = interceptors[index]

        return interceptor.intercept(next)
    }
}

/**
 * 日志拦截器示例
 */
class LoggingInterceptor : DeepLinkInterceptor {
    override fun intercept(chain: DeepLinkInterceptor.Chain): Boolean {
        val request = chain.request()
        val startTime = System.currentTimeMillis()

        android.util.Log.d(
            "DeepLink",
            "DeepLink triggered: \${request.uri.toUri()}"
        )

        val result = chain.proceed(request)

        val duration = System.currentTimeMillis() - startTime
        android.util.Log.d(
            "DeepLink",
            "DeepLink processed in \${duration}ms, result: \$result"
        )

        return result
    }
}

/**
 * 权限检查拦截器示例
 */
class PermissionInterceptor(
    private val requiredPermissions: List<String> = emptyList()
) : DeepLinkInterceptor {
    override fun intercept(chain: DeepLinkInterceptor.Chain): Boolean {
        val request = chain.request()
        val context = request.context

        // 检查权限
        val hasPermission = requiredPermissions.all { permission ->
            context.checkSelfPermission(permission) == android.content.pm.PackageManager.PERMISSION_GRANTED
        }

        if (!hasPermission) {
            android.util.Log.w("DeepLink", "Missing required permissions")
            return true // 拦截
        }

        return chain.proceed(request)
    }
}
