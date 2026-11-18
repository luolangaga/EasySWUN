package com.swun.campus.data

import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import java.util.concurrent.TimeUnit

/**
 * 真实链路集成测试：CAS 登录 -> 正方 SSO -> 课表 / 成绩 / 校历 / 个人信息。
 *
 * 需要真实账号才能跑，凭据从环境变量读取，不写进代码：
 *   PowerShell:  $env:SWUN_USER="学号"; $env:SWUN_PASSWORD="密码"
 *   ./gradlew testDebugUnitTest --tests "*CampusApiIntegrationTest*"
 * 未设置凭据时测试自动跳过。
 */
class CampusApiIntegrationTest {

    private val account: String? = System.getenv("SWUN_USER")
    private val password: String? = System.getenv("SWUN_PASSWORD")

    @Before
    fun requireCredentials() {
        assumeTrue("未设置 SWUN_USER / SWUN_PASSWORD，跳过联调测试", !account.isNullOrBlank() && !password.isNullOrBlank())
    }

    private class MemoryAccountStore : AccountStore {
        override var lastAccount: String = ""
        override var savedName: String = ""
    }

    private fun newClient(jar: PersistentCookieJar): OkHttpClient =
        OkHttpClient.Builder()
            .cookieJar(jar)
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()

    @Test
    fun loginAndLoadAllData() {
        val jar = PersistentCookieJar(null)
        val client = newClient(jar)
        val api = CampusApi(client)
        val auth = AuthRepository(MemoryAccountStore(), jar, client, api)

        val result = runBlocking { auth.login(account!!, password!!) }
        assertTrue("登录失败: $result", result is LoginResult.Success)
        val profile = (result as LoginResult.Success).profile
        assertTrue("未解析出姓名", profile.name.isNotBlank())
        assertEquals(account, profile.studentNo)
        println("个人信息: ${profile.name} / ${profile.college} / 专业 ${profile.major} / 班级 ${profile.className} / 年级 ${profile.grade}")
        assertTrue("未解析出学院", profile.college.isNotBlank())

        val term = Term("2026", Term.HALF_FIRST)
        val timetable = api.timetable(term)
        assertTrue("课表为空", timetable.courses.isNotEmpty())
        val first = timetable.courses.first()
        println("课表: ${first.name} / ${first.teacher} / ${first.room} / 周${first.dayOfWeek} ${first.startSection}-${first.endSection}节 / ${first.weekRaw}")

        val grades = api.grades(term)
        println("成绩 ${grades.size} 条: " + grades.take(3).joinToString { "${it.courseName}=${it.score}" })

        val calendar = api.calendar()
        assertTrue("校历缺少学期起止", calendar.startDate != null && calendar.endDate != null)
        println("校历: ${calendar.startDate} ~ ${calendar.endDate}, 教学日 ${calendar.classDays.size} 天, 事件 ${calendar.events.size} 个")

        val profileAgain = api.profile()
        assertTrue(profileAgain.name.isNotBlank())
    }

    @Test
    fun wrongPasswordIsRejected() {
        val jar = PersistentCookieJar(null)
        val client = newClient(jar)
        val api = CampusApi(client)
        val auth = AuthRepository(MemoryAccountStore(), jar, client, api)

        val result = runBlocking { auth.login(account!!, "definitely-wrong-password") }
        assertTrue("错误密码不应登录成功", result is LoginResult.Failure)
        assertTrue("错误提示为空", (result as LoginResult.Failure).message.isNotBlank())
        println("错误密码返回: ${result.message}")
    }
}
