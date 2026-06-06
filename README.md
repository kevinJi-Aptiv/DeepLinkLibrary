# DeepLink Library

一个使用 Kotlin 编写的轻量级、灵活的 Android DeepLink 路由库。

## 功能特性

- ✅ 支持灵活的路由匹配（包括路径参数）
- ✅ 拦截器链设计，支持请求拦截和处理
- ✅ DSL 风格的 API，易于使用
- ✅ 支持查询参数提取
- ✅ 支持自定义默认处理
- ✅ 轻量级实现，无外部依赖

## 快速开始

### 1. 基本使用

```kotlin
val router = DeepLinkRouter.create {
    route("myapp", "home", "home") { context, uri ->
        Intent(context, HomeActivity::class.java)
    }
    
    route("myapp", "product", "product/{id}") { context, uri ->
        val intent = Intent(context, ProductActivity::class.java)
        // 提取路径参数
        val pathParams = uri.getPathSegments()
        intent.putExtra("product_id", pathParams.getOrNull(1))
        intent
    }
    
    route("myapp", "user", "user/{userId}/profile") { context, uri ->
        Intent(context, UserProfileActivity::class.java).apply {
            putExtra("user_id", uri.getPathSegments().getOrNull(1))
        }
    }
}

// 处理 DeepLink
router.handle(context, "myapp://product/123")
```

### 2. 提取查询参数

```kotlin
route("myapp", "search", "search") { context, uri ->
    Intent(context, SearchActivity::class.java).apply {
        val query = uri.getQueryParam("q")
        val page = uri.getQueryParamOrDefault("page", "1")
        putExtra("query", query)
        putExtra("page", page)
    }
}

// myapp://search?q=kotlin&page=2
```

### 3. 使用拦截器

```kotlin
// 添加日志拦截器
val loggingInterceptor = LoggingInterceptor()

// 自定义拦截器
class AuthInterceptor : DeepLinkInterceptor {
    override fun intercept(chain: DeepLinkInterceptor.Chain): Boolean {
        val request = chain.request()
        
        // 检查用户是否已登录
        if (!isUserLoggedIn()) {
            // 重定向到登录页面
            val loginIntent = Intent(request.context, LoginActivity::class.java)
            request.context.startActivity(loginIntent)
            return true // 拦截，阻止继续处理
        }
        
        return chain.proceed(request) // 继续处理
    }
}

val router = DeepLinkRouter.create {
    interceptor(loggingInterceptor)
    interceptor(AuthInterceptor())
    
    route("myapp", "profile", "profile") { context, uri ->
        Intent(context, ProfileActivity::class.java)
    }
}
```

### 4. 默认处理

```kotlin
val router = DeepLinkRouter.create {
    route("myapp", "home", "home") { context, uri ->
        Intent(context, HomeActivity::class.java)
    }
    
    // 如果没有匹配的路由，使用默认处理
    defaultAction { context, uri ->
        Intent(context, MainActivity::class.java).apply {
            putExtra("deep_link_uri", uri.toUri().toString())
        }
    }
}
```

### 5. 在 Activity 中集成

```kotlin
class MainActivity : AppCompatActivity() {
    private lateinit var router: DeepLinkRouter
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        // 初始化路由
        router = DeepLinkRouter.create {
            route("myapp", "home", "home") { context, uri ->
                Intent(context, HomeActivity::class.java)
            }
            route("myapp", "product", "product/{id}") { context, uri ->
                Intent(context, ProductActivity::class.java).apply {
                    putExtra("product_id", uri.getPathSegments().getOrNull(1))
                }
            }
        }
        
        // 处理通过 Intent 传入的 DeepLink
        handleDeepLink(intent)
    }
    
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleDeepLink(intent)
    }
    
    private fun handleDeepLink(intent: Intent) {
        intent.data?.let { uri ->
            router.handle(this, uri)
        }
    }
}
```

### 6. 在 Manifest 中声明

```xml
<activity
    android:name=".MainActivity"
    android:exported="true">
    <intent-filter>
        <action android:name="android.intent.action.VIEW" />
        <category android:name="android.intent.category.DEFAULT" />
        <category android:name="android.intent.category.BROWSABLE" />
        <data
            android:scheme="myapp"
            android:host="home" />
        <data
            android:scheme="myapp"
            android:host="product" />
    </intent-filter>
</activity>
```

## API 文档

### DeepLinkUri

```kotlin
// 解析 URI
val uri = DeepLinkUri.parse("myapp://product/123?discount=10")

// 获取信息
val scheme = uri.scheme        // "myapp"
val host = uri.host            // "product"
val path = uri.path            // "/product/123"
val segments = uri.getPathSegments()  // ["product", "123"]

// 获取查询参数
val discount = uri.getQueryParam("discount")  // "10"
val page = uri.getQueryParamOrDefault("page", "1")

// 转换为 Uri
val androidUri = uri.toUri()
```

### Route

```kotlin
val route = RouteBuilder("myapp", "home", "home")
    .action { context, uri ->
        Intent(context, HomeActivity::class.java)
    }
    .build()

// 检查是否匹配
val matches = route.matches(deepLinkUri)

// 提取路径参数
val params = route.extractPathParams("/user/123/profile")
```

### DeepLinkRouter

```kotlin
val router = DeepLinkRouter.create {
    route("myapp", "home", "home") { context, uri ->
        Intent(context, HomeActivity::class.java)
    }
}

// 处理 DeepLink
router.handle(context, "myapp://home")
router.handle(context, Uri.parse("myapp://home"))
router.handle(context, deepLinkUri)
```

## 拦截器链

拦截器按照添加顺序执行，形成一条链：

```
Request
  ↓
Interceptor 1 - can intercept and stop
  ↓
Interceptor 2 - can intercept and stop
  ↓
Route Handler
  ↓
Activity Start
```

## 高级用法

### 自定义拦截器

```kotlin
class RateLimitInterceptor(val maxRequests: Int = 10) : DeepLinkInterceptor {
    private val requestTimestamps = mutableListOf<Long>()
    
    override fun intercept(chain: DeepLinkInterceptor.Chain): Boolean {
        val now = System.currentTimeMillis()
        requestTimestamps.add(now)
        
        // 只保留最近 1 秒的请求
        requestTimestamps.removeAll { now - it > 1000 }
        
        if (requestTimestamps.size > maxRequests) {
            android.util.Log.w("DeepLink", "Rate limit exceeded")
            return true // 拦截
        }
        
        return chain.proceed(chain.request())
    }
}
```

### 动态添加路由

```kotlin
val router = DeepLinkRouter.create()

// 运行时添加路由
router
    .route("myapp", "home", "home") { context, uri ->
        Intent(context, HomeActivity::class.java)
    }
    .route("myapp", "product", "product/{id}") { context, uri ->
        Intent(context, ProductActivity::class.java)
    }
```

## 许可证

MIT License

## 贡献

欢迎提交 Issue 和 Pull Request！
