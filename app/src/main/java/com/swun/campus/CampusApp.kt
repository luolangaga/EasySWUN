package com.swun.campus

import android.app.Application
import android.content.Context
import com.swun.campus.data.AuthRepository
import com.swun.campus.data.CampusApi
import com.swun.campus.data.PersistentCookieJar
import com.swun.campus.data.SessionStore
import okhttp3.Cache
import okhttp3.ConnectionPool
import okhttp3.Dispatcher
import okhttp3.OkHttpClient
import java.io.File
import java.util.concurrent.TimeUnit

class CampusApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}

/** 手写依赖容器：会话、Cookie、网络客户端与各仓库共享同一套实例 */
class AppContainer(context: Context) {
    val store = SessionStore(context)
    val cookieJar = PersistentCookieJar(store)

    val client: OkHttpClient = OkHttpClient.Builder()
        .cookieJar(cookieJar)
        .cache(Cache(File(context.cacheDir, "swun_http_cache"), 40L * 1024 * 1024))
        // 课表、成绩、公告会同时发多个请求；默认每主机 5 个连接会让
        // 校园网下出现“明明网速不慢，App 却一直转圈”的排队感。
        .dispatcher(Dispatcher().apply {
            maxRequests = 64
            maxRequestsPerHost = 16
        })
        .connectionPool(ConnectionPool(8, 5, TimeUnit.MINUTES))
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .retryOnConnectionFailure(true)
        .build()

    val api = CampusApi(client)
    val auth = AuthRepository(store, cookieJar, client, api)
}
