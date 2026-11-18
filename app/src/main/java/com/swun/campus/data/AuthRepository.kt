package com.swun.campus.data

import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URLEncoder
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * 统一身份认证（金智 authserver）的密码加密。
 * 与登录页 encrypt.js 等价：AES/CBC/PKCS7，明文 = 64 位随机前缀 + 密码，密钥为页面下发的 salt，IV 随机 16 位。
 */
object CasCrypto {
    private const val CHARS = "ABCDEFGHJKMNPQRSTWXYZabcdefhijkmnprstwxyz2345678"
    private val random = SecureRandom()

    private fun randomString(length: Int): String {
        val sb = StringBuilder(length)
        repeat(length) { sb.append(CHARS[random.nextInt(CHARS.length)]) }
        return sb.toString()
    }

    fun encryptPassword(password: String, salt: String): String {
        val plain = randomString(64) + password
        val iv = randomString(16)
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        cipher.init(
            Cipher.ENCRYPT_MODE,
            SecretKeySpec(salt.toByteArray(Charsets.UTF_8), "AES"),
            IvParameterSpec(iv.toByteArray(Charsets.UTF_8))
        )
        return Base64.getEncoder().encodeToString(cipher.doFinal(plain.toByteArray(Charsets.UTF_8)))
    }
}

object Endpoints {
    const val SSO = "https://authserver.swun.edu.cn"
    const val JWXT = "https://jwxt.swun.edu.cn"
    const val SERVICE = "http://jwxt.swun.edu.cn/sso/jziotlogin"
    const val INDEX_MENU = "$JWXT/jwglxt/xtgl/index_initMenu.html?jsdm=xs"
    const val USER_INFO = "$JWXT/jwglxt/xtgl/index_cxYhxxIndex.html?xt=jw&localeKey=zh_CN&gnmkdm=index"
    const val TIMETABLE = "$JWXT/jwglxt/kbcx/xskbcx_cxXsgrkb.html?gnmkdm=N2151"
    const val GRADE_PAGE = "$JWXT/jwglxt/cjcx/cjcx_cxDgXscj.html?gnmkdm=N305005&layout=default"
    const val GRADE_QUERY = "$JWXT/jwglxt/cjcx/cjcx_cxXsgrcj.html?doType=query&gnmkdm=N305005"
    const val CALENDAR = "$JWXT/jwglxt/pkgl/xlglMobile_cxXlIndexForxs.html?gnmkdm=Y210501"
    const val UA =
        "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) " +
            "Chrome/131.0.0.0 Mobile Safari/537.36"
}

sealed class LoginResult {
    data class Success(val profile: UserProfile) : LoginResult()
    data class Failure(val message: String) : LoginResult()
}

class AuthRepository(
    private val store: AccountStore,
    private val jar: PersistentCookieJar,
    private val client: OkHttpClient,
    private val api: CampusApi
) {

    fun hasSession(): Boolean = !jar.isEmpty()

    fun savedAccount(): String = store.lastAccount

    fun savedName(): String = store.savedName

    /** 当前统一的 CAS / 教务 Cookie，供应用内 WebView 复用登录态。 */
    fun sessionCookies(): List<okhttp3.Cookie> = jar.snapshot()

    /**
     * 用 WebView 完成统一认证后，把浏览器 Cookie 导入原生会话。
     * 这是主登录路径：学校 CAS 对非浏览器 TLS 指纹不稳定，网页登录更可靠。
     */
    fun importWebSession(cookies: List<okhttp3.Cookie>) {
        jar.importCookies(cookies)
        api.invalidate()
    }

    /** 网页登录重定向到教务系统后，用教务接口校验会话并补齐个人信息。 */
    suspend fun completeWebLogin(username: String): LoginResult {
        return try {
            val profile = api.fetchProfile()?.copy(studentNo = username)
            if (profile == null || !profile.isValid) {
                jar.clear()
                return LoginResult.Failure("网页登录未建立有效教务会话，请重试")
            }
            store.lastAccount = username
            store.savedName = profile.name
            LoginResult.Success(profile)
        } catch (e: Exception) {
            LoginResult.Failure(e.message ?: "登录后读取个人信息失败，请重试")
        }
    }

    /**
     * 登录：CAS 认证通过后，CAS 会带着 ticket 跳转到正方教务的 /sso/jziotlogin，
     * 教务系统据此建立自己的会话，因此一次登录即可拿到门户与教务两套 Cookie。
     */
    suspend fun login(username: String, password: String): LoginResult {
        if (username.isBlank() || password.isBlank()) return LoginResult.Failure("请输入学号和密码")
        return try {
            val service = URLEncoder.encode(Endpoints.SERVICE, "UTF-8")
            val loginUrl = "${Endpoints.SSO}/authserver/login?service=$service"

            val page = client.newCall(
                Request.Builder().url(loginUrl).header("User-Agent", Endpoints.UA).build()
            ).execute().use { it.body?.string().orEmpty() }

            val salt = firstGroup(page, "<input[^>]*id=\"pwdEncryptSalt\"[^>]*value=\"([^\"]*)\"")
                ?: firstGroup(page, "<input[^>]*value=\"([^\"]*)\"[^>]*id=\"pwdEncryptSalt\"")
                ?: return LoginResult.Failure("统一身份认证页面解析失败，请稍后重试")

            val execution = firstGroup(page, "<input[^>]*name=\"execution\"[^>]*value=\"([^\"]*)\"")
                ?: return LoginResult.Failure("统一身份认证页面解析失败，请稍后重试")

            val form = FormBody.Builder()
                .add("username", username)
                .add("password", CasCrypto.encryptPassword(password, salt))
                .add("captcha", "")
                .add("lt", "")
                .add("_eventId", "submit")
                .add("cllt", "userNameLogin")
                .add("dllt", "generalLogin")
                .add("execution", execution)
                .add("rmShown", "1")
                .build()

            val response = client.newCall(
                Request.Builder()
                    .url(loginUrl)
                    .header("User-Agent", Endpoints.UA)
                    .header("Referer", loginUrl)
                    .post(form)
                    .build()
            ).execute()

            val finalHost = response.request.url.host
            val body = response.body?.string().orEmpty()
            response.close()

            if (finalHost != "jwxt.swun.edu.cn") {
                return LoginResult.Failure(parseLoginError(body))
            }

            // 教务会话建立后立刻校验，避免“看起来登录成功但接口仍要求认证”
            val profile = api.fetchProfile()?.copy(studentNo = username)
            if (profile == null || !profile.isValid) {
                jar.clear()
                return LoginResult.Failure("登录未完成，请确认账号密码是否正确")
            }
            store.lastAccount = username
            store.savedName = profile.name
            LoginResult.Success(profile)
        } catch (e: Exception) {
            LoginResult.Failure(e.message ?: "网络异常，请检查网络后重试")
        }
    }

    fun logout() {
        jar.clear()
        api.invalidate()
    }

    private fun parseLoginError(body: String): String {
        firstGroup(body, "id=\"showErrorTip\"[^>]*>([^<]{1,80})<")?.let {
            if (it.isNotBlank()) return it.trim()
        }
        firstGroup(body, "toastr\\.error\\(\\s*[\"']([^\"']{1,80})")?.let { return it.trim() }
        if (body.contains("验证码")) return "登录失败，可能需要输入验证码，请稍后重试"
        return "学号或密码错误，请重新输入"
    }

    private fun firstGroup(text: String, pattern: String): String? =
        Regex(pattern, RegexOption.DOT_MATCHES_ALL).find(text)?.groupValues?.getOrNull(1)
}
