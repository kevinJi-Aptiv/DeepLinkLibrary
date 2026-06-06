package com.aptiv.deeplink

/**
 * DeepLink URI 数据类，用于解析和存储 DeepLink 信息
 */
data class DeepLinkUri(
    val scheme: String,
    val host: String,
    val path: String,
    val queryParams: Map<String, String> = emptyMap(),
    val fragment: String? = null
) {
    companion object {
        /**
         * 从 URI 字符串解析 DeepLinkUri
         */
        fun parse(uri: String): DeepLinkUri? {
            return try {
                val url = android.net.Uri.parse(uri)
                DeepLinkUri(
                    scheme = url.scheme.orEmpty(),
                    host = url.host.orEmpty(),
                    path = url.path.orEmpty(),
                    queryParams = extractQueryParams(url),
                    fragment = url.fragment
                )
            } catch (e: Exception) {
                null
            }
        }

        private fun extractQueryParams(uri: android.net.Uri): Map<String, String> {
            val params = mutableMapOf<String, String>()
            uri.queryParameterNames.forEach { name ->
                uri.getQueryParameter(name)?.let {
                    params[name] = it
                }
            }
            return params
        }
    }

    fun getPathSegments(): List<String> {
        return path.split("/").filter { it.isNotEmpty() }
    }

    fun getQueryParam(key: String): String? = queryParams[key]

    fun getQueryParamOrDefault(key: String, default: String): String = queryParams[key] ?: default

    fun toUri(): android.net.Uri {
        val builder = android.net.Uri.Builder()
            .scheme(scheme)
            .authority(host)
            .path(path)
        
        queryParams.forEach { (key, value) ->
            builder.appendQueryParameter(key, value)
        }
        
        fragment?.let { builder.fragment(it) }
        
        return builder.build()
    }
}
