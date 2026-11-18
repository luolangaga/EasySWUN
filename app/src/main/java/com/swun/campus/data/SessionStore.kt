package com.swun.campus.data

import android.content.Context
import android.util.Base64
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

/**
 * 账号记忆（最近一次登录的学号与姓名），与 Cookie 一起保存在应用私有目录。
 */
interface AccountStore {
    var lastAccount: String
    var savedName: String
}

/**
 * 会话 Cookie 本地持久化：App 重启后无需重新登录。
 * 仅保存在应用私有目录，退出登录时清空。
 */
class SessionStore(context: Context) : AccountStore {

    private val prefs = context.getSharedPreferences("swun_session", Context.MODE_PRIVATE)

    override var lastAccount: String
        get() = prefs.getString(KEY_ACCOUNT, "") ?: ""
        set(value) = prefs.edit().putString(KEY_ACCOUNT, value).apply()

    override var savedName: String
        get() = prefs.getString(KEY_NAME, "") ?: ""
        set(value) = prefs.edit().putString(KEY_NAME, value).apply()

    /** 用户手动指定的开学日（ISO yyyy-MM-dd），为空表示跟随校历自动识别 */
    var semesterStart: String
        get() = prefs.getString(KEY_SEMESTER_START, "") ?: ""
        set(value) = prefs.edit().putString(KEY_SEMESTER_START, value).apply()

    fun loadCookies(): List<Cookie> {
        val raw = prefs.getString(KEY_COOKIES, null) ?: return emptyList()
        if (raw.isBlank()) return emptyList()
        val out = ArrayList<Cookie>()
        raw.split('\n').forEach { line ->
            if (line.isNotBlank()) {
                try {
                    val text = String(Base64.decode(line, Base64.NO_WRAP))
                    val parts = text.split('|', limit = 2)
                    if (parts.size == 2) {
                        val url = parts[0].toHttpUrlOrNull() ?: return@forEach
                        Cookie.parse(url, parts[1])?.let { out.add(it) }
                    }
                } catch (_: Exception) {
                }
            }
        }
        return out
    }

    fun saveCookies(cookies: List<Cookie>) {
        val sb = StringBuilder()
        cookies.forEach { c ->
            val origin = if (c.domain.startsWith(".")) c.domain.substring(1) else c.domain
            val probe = "https://$origin${c.path}".toHttpUrlOrNull() ?: return@forEach
            val encoded = Base64.encodeToString(
                (probe.toString() + "|" + c.toString()).toByteArray(),
                Base64.NO_WRAP
            )
            if (sb.isNotEmpty()) sb.append('\n')
            sb.append(encoded)
        }
        prefs.edit().putString(KEY_COOKIES, sb.toString()).apply()
    }

    fun clearSession() {
        prefs.edit().remove(KEY_COOKIES).remove(KEY_NAME).apply()
    }

    fun hasSession(): Boolean = loadCookies().isNotEmpty()

    private companion object {
        const val KEY_COOKIES = "cookies"
        const val KEY_ACCOUNT = "account"
        const val KEY_NAME = "name"
        const val KEY_SEMESTER_START = "semester_start"
    }
}

/** 把 cookie 存到 SessionStore 的 CookieJar；store 为 null 时退化为纯内存会话 */
class PersistentCookieJar(private val store: SessionStore?) : CookieJar {

    private val memory: MutableList<Cookie> = (store?.loadCookies() ?: emptyList()).toMutableList()

    @Synchronized
    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        cookies.forEach { new ->
            memory.removeAll {
                it.name == new.name && it.domain == new.domain && it.path == new.path
            }
            memory.add(new)
        }
        store?.saveCookies(memory)
    }

    @Synchronized
    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        val now = System.currentTimeMillis()
        memory.removeAll { it.expiresAt < now }
        return memory.filter { it.matches(url) }
    }

    @Synchronized
    fun clear() {
        memory.clear()
        store?.clearSession()
    }

    /** 网页登录成功后，把 WebView CookieManager 里的 Cookie 导入同一套会话。 */
    @Synchronized
    fun importCookies(cookies: List<Cookie>) {
        cookies.forEach { new ->
            memory.removeAll {
                it.name == new.name && it.domain == new.domain && it.path == new.path
            }
            memory.add(new)
        }
        store?.saveCookies(memory)
    }

    @Synchronized
    fun snapshot(): List<Cookie> = memory.toList()

    @Synchronized
    fun isEmpty(): Boolean = memory.isEmpty()
}
