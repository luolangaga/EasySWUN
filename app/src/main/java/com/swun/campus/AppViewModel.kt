package com.swun.campus

import android.app.Application
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.swun.campus.data.Grade
import com.swun.campus.data.KtkqParser
import com.swun.campus.data.KtkqSchedule
import com.swun.campus.data.LoginResult
import com.swun.campus.data.SchoolCalendar
import com.swun.campus.data.SessionExpiredException
import com.swun.campus.data.Term
import com.swun.campus.data.Timetable
import com.swun.campus.data.UserProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Cookie
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/** 学年学期工具：正方教务用 xqm=3 表示第一学期、12 表示第二学期 */
object TermMath {

    fun current(today: LocalDate = LocalDate.now()): Term {
        val month = today.monthValue
        val year = today.year
        return when {
            month >= 8 -> Term(year.toString(), Term.HALF_FIRST)
            month <= 1 -> Term((year - 1).toString(), Term.HALF_FIRST)
            else -> Term((year - 1).toString(), Term.HALF_SECOND)
        }
    }

    fun recent(count: Int = 6, today: LocalDate = LocalDate.now()): List<Term> {
        var term = current(today)
        val list = ArrayList<Term>(count)
        repeat(count) {
            list.add(term)
            term = previous(term)
        }
        return list
    }

    private fun previous(term: Term): Term {
        val year = term.year.toIntOrNull() ?: return term
        return if (term.half == Term.HALF_FIRST) {
            Term((year - 1).toString(), Term.HALF_SECOND)
        } else {
            Term(year.toString(), Term.HALF_FIRST)
        }
    }
}

class AppViewModel(app: Application) : AndroidViewModel(app) {

    private val container = (app as CampusApp).container
    private val auth = container.auth
    private val api = container.api
    private val cachePrefs = app.getSharedPreferences("swun_ktkq_cache", Context.MODE_PRIVATE)

    // ---------------- 登录状态 ----------------
    var loggedIn by mutableStateOf(auth.hasSession())
        private set
    var savedAccount by mutableStateOf(auth.savedAccount())
        private set
    val savedName: String get() = auth.savedName()
    var loginLoading by mutableStateOf(false)
        private set
    var loginError by mutableStateOf<String?>(null)
        private set

    var profile by mutableStateOf<UserProfile?>(null)
        private set

    // ---------------- 业务数据 ----------------
    var currentTerm by mutableStateOf(TermMath.current())
        private set
    var termOptions by mutableStateOf(TermMath.recent())
        private set

    var timetable by mutableStateOf<Timetable?>(null)
        private set
    var timetableLoading by mutableStateOf(false)
        private set
    var timetableError by mutableStateOf<String?>(null)
        private set

    // ---------------- ktkq 移动端课表（课堂考勤/课表共用） ----------------
    var ktkqSchedule by mutableStateOf<KtkqSchedule?>(null)
        private set
    var ktkqLoading by mutableStateOf(false)
        private set
    var ktkqError by mutableStateOf<String?>(null)
        private set
    var ktkqAttempt by mutableIntStateOf(0)
        private set

    var calendar by mutableStateOf<SchoolCalendar?>(null)
        private set
    var calendarLoading by mutableStateOf(false)
        private set
    var calendarError by mutableStateOf<String?>(null)
        private set

    var gradeTerm by mutableStateOf(TermMath.current())
        private set
    var grades by mutableStateOf<List<Grade>?>(null)
        private set
    var gradesLoading by mutableStateOf(false)
        private set
    var gradesError by mutableStateOf<String?>(null)
        private set

    var globalMessage by mutableStateOf<String?>(null)
        private set

    /** 手动设置的开学日；为空时跟随校历自动识别 */
    var semesterStartOverride by mutableStateOf(loadSemesterStartOverride())
        private set

    /** 实际生效的开学日：手动优先，其次校历 */
    val semesterStart: LocalDate?
        get() = semesterStartOverride ?: calendar?.startDate

    private val gradeCache = HashMap<String, List<Grade>>()
    private var bootstrapped = false

    init {
        // 晚归打卡只走学生管理平台官方页面，旧版本留下的本地打卡记录在这里清掉。
        app.getSharedPreferences("swun_check_in", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .apply()
        // 先用上次成功抓到的课表顶上，再在后台刷新，避免每次进来都干等 WAF/CAS。
        loadCachedKtkq()
        if (loggedIn) bootstrap()
    }

    // ---------------- 登录 ----------------

    fun login(username: String, password: String) {
        if (loginLoading) return
        viewModelScope.launch {
            loginLoading = true
            loginError = null
            val result = withContext(Dispatchers.IO) { auth.login(username.trim(), password) }
            loginLoading = false
            when (result) {
                is LoginResult.Success -> {
                    profile = result.profile.copy(studentNo = username.trim())
                    savedAccount = username.trim()
                    loggedIn = true
                    bootstrapped = true
                    loadTimetable(currentTerm)
                    loadCalendar()
                }
                is LoginResult.Failure -> loginError = result.message
            }
        }
    }

    /**
     * WebView 统一认证完成后的主登录入口。
     * Cookie 由登录 WebView 的 CookieManager 采集，导入后仍用原生接口读取资料与课表。
     */
    fun loginWithWebSession(username: String, cookies: List<okhttp3.Cookie>) {
        if (loginLoading) return
        if (cookies.isEmpty()) {
            loginError = "网页登录没有拿到有效 Cookie，请重新登录"
            return
        }
        viewModelScope.launch {
            loginLoading = true
            loginError = null
            val cleanName = username.trim()
            val result = withContext(Dispatchers.IO) {
                auth.importWebSession(cookies)
                auth.completeWebLogin(cleanName)
            }
            loginLoading = false
            when (result) {
                is LoginResult.Success -> {
                    profile = result.profile.copy(studentNo = cleanName)
                    savedAccount = cleanName
                    loggedIn = true
                    bootstrapped = true
                    loadTimetable(currentTerm)
                    loadCalendar()
                }
                is LoginResult.Failure -> loginError = result.message
            }
        }
    }

    fun logout() {
        auth.logout()
        profile = null
        timetable = null
        timetableError = null
        ktkqSchedule = null
        ktkqLoading = false
        ktkqError = null
        cachePrefs.edit().clear().apply()
        calendar = null
        calendarError = null
        grades = null
        gradesError = null
        gradeCache.clear()
        bootstrapped = false
        loggedIn = false
        loginError = null
    }

    fun dismissMessage() {
        globalMessage = null
    }

    /** 启动时校验本地会话是否还有效，并补齐个人信息与课表 */
    private fun bootstrap() {
        viewModelScope.launch {
            val info = runCatching { withContext(Dispatchers.IO) { api.profile() } }
            info.onSuccess { fetched ->
                profile = fetched.copy(
                    studentNo = fetched.studentNo.ifBlank { savedAccount },
                    name = fetched.name.ifBlank { savedNameSnapshot }
                )
            }.onFailure { e ->
                if (e is SessionExpiredException) {
                    auth.logout()
                    loggedIn = false
                    loginError = "登录状态已过期，请重新登录"
                    return@launch
                }
            }
            bootstrapped = true
            loadTimetable(currentTerm)
            loadCalendar()
        }
    }

    private val savedNameSnapshot: String get() = auth.savedName()

    // ---------------- 课表 ----------------

    fun loadTimetable(term: Term) {
        if (timetableLoading) return
        viewModelScope.launch {
            timetableLoading = true
            timetableError = null
            val result = runCatching { withContext(Dispatchers.IO) { api.timetable(term) } }
            timetableLoading = false
            result.onSuccess {
                currentTerm = it.term
                applyTimetable(it)
            }.onFailure {
                timetableError = errorMessage(it)
                handleError(it)
            }
        }
    }

    private fun applyTimetable(data: Timetable) {
        timetable = data
        data.profile?.let { p ->
            val merged = (profile ?: p).copy(
                name = p.name.ifBlank { profile?.name.orEmpty() },
                studentNo = p.studentNo.ifBlank { profile?.studentNo.orEmpty() },
                major = p.major.ifBlank { profile?.major.orEmpty() },
                className = p.className.ifBlank { profile?.className.orEmpty() },
                grade = p.grade.ifBlank { profile?.grade.orEmpty() }
            )
            profile = merged
        }
    }

    // ---------------- ktkq 课表 ----------------

    private fun loadCachedKtkq() {
        val raw = cachePrefs.getString("raw", null) ?: return
        runCatching { KtkqParser.parse(raw) }
            .onSuccess { cached ->
                ktkqSchedule = cached
                ktkqLoading = false
                ktkqError = null
                if (profile == null && cached.studentName.isNotBlank()) {
                    profile = UserProfile(name = cached.studentName, studentNo = savedAccount)
                }
            }
    }

    /** 由隐藏 WebView 桥接触发；原生 UI 不直接发 WAF 请求 */
    fun beginKtkqLoad() {
        if (ktkqLoading) return
        ktkqAttempt += 1
        ktkqLoading = true
        ktkqError = null
    }

    fun acceptKtkqSchedule(raw: String) {
        runCatching { KtkqParser.parse(raw) }
            .onSuccess { data ->
                ktkqSchedule = data
                ktkqLoading = false
                ktkqError = null
                cachePrefs.edit().putString("raw", raw).apply()
                if (profile == null && data.studentName.isNotBlank()) {
                    profile = UserProfile(name = data.studentName, studentNo = savedAccount)
                } else if (data.studentName.isNotBlank() && profile?.name.isNullOrBlank()) {
                    profile = profile?.copy(name = data.studentName)
                }
            }
            .onFailure { failKtkq("课表数据解析失败：${it.message.orEmpty()}") }
    }

    fun failKtkq(message: String) {
        ktkqLoading = false
        ktkqError = message.ifBlank { "课堂考勤课表加载失败" }
    }

    fun resetKtkq() {
        ktkqSchedule = null
        ktkqLoading = false
        ktkqError = null
    }

    // ---------------- 成绩 ----------------

    fun selectGradeTerm(term: Term) {
        if (gradeTerm == term && grades != null) return
        gradeTerm = term
        gradeCache[term.key]?.let {
            grades = it
            gradesError = null
            return
        }
        grades = null
        loadGrades(term)
    }

    fun loadGrades(term: Term = gradeTerm) {
        if (gradesLoading) return
        viewModelScope.launch {
            gradesLoading = true
            gradesError = null
            val result = runCatching { withContext(Dispatchers.IO) { api.grades(term) } }
            gradesLoading = false
            result.onSuccess {
                gradeCache[term.key] = it
                grades = it
            }.onFailure {
                gradesError = errorMessage(it)
                handleError(it)
            }
        }
    }

    // ---------------- 校历 ----------------

    fun loadCalendar() {
        if (calendarLoading) return
        viewModelScope.launch {
            calendarLoading = true
            calendarError = null
            val result = runCatching { withContext(Dispatchers.IO) { api.calendar() } }
            calendarLoading = false
            result.onSuccess { calendar = it }.onFailure {
                calendarError = errorMessage(it)
                handleError(it)
            }
        }
    }

    fun refreshProfile() {
        viewModelScope.launch {
            val result = runCatching { withContext(Dispatchers.IO) { api.profile() } }
            result.onSuccess {
                profile = it.copy(
                    studentNo = profile?.studentNo ?: savedAccount,
                    major = profile?.major ?: it.major
                )
            }
        }
    }

    /** 给 WebView 注入 CAS / 学工登录态用 */
    fun sessionCookies(): List<Cookie> = container.cookieJar.snapshot()

    // ---------------- 通用 ----------------

    /** 某学期的第几周：只有当前学期才能按开学日推算，其它学期返回 null */
    fun weekNumber(term: Term, today: LocalDate = LocalDate.now()): Int? {
        if (term != TermMath.current(today)) return null
        val start = semesterStart ?: return null
        if (today.isBefore(start)) return null
        val days = ChronoUnit.DAYS.between(start, today)
        return (days / 7).toInt() + 1
    }

    fun setSemesterStart(date: LocalDate?) {
        semesterStartOverride = date
        container.store.semesterStart = date?.toString().orEmpty()
    }

    private fun loadSemesterStartOverride(): LocalDate? =
        runCatching { LocalDate.parse(container.store.semesterStart) }.getOrNull()

    private fun errorMessage(e: Throwable): String = when (e) {
        is SessionExpiredException -> "登录状态已过期，请重新登录"
        else -> e.message?.takeIf { it.isNotBlank() } ?: "网络请求失败，请稍后重试"
    }

    private fun handleError(e: Throwable) {
        if (e is SessionExpiredException) {
            auth.logout()
            loggedIn = false
            loginError = "登录状态已过期，请重新登录"
        } else {
            globalMessage = errorMessage(e)
        }
    }

    private val Term.key: String get() = "$year-$half"
}
