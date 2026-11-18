package com.swun.campus.data

import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.ProtocolException
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * 正方教务系统（V9）接口。
 * 所有请求都依赖 CAS 登录后建立的会话 Cookie，会话失效时抛出 [SessionExpiredException]。
 */
class CampusApi(private val client: OkHttpClient) {
    @Volatile
    private var csrfToken: String? = null

    fun invalidate() {
        csrfToken = null
    }

    // ---------------------------------------------------------------- 基本信息

    /** 不抛异常版本，用于登录结果校验 */
    fun fetchProfile(): UserProfile? = try {
        parseProfile(executeGet(Endpoints.USER_INFO))
    } catch (e: Exception) {
        null
    }

    fun profile(): UserProfile {
        val html = executeGet(Endpoints.USER_INFO)
        return parseProfile(html) ?: throw NetworkException("无法获取个人信息")
    }

    private fun parseProfile(html: String): UserProfile? {
        val name = Regex("<h4[^>]*class=\"media-heading\"[^>]*>([^<]+)</h4>")
            .find(html)?.groupValues?.get(1)
            ?.replace("&nbsp;", " ")
            ?.substringBefore("  ")
            ?.trim()

        val line = Regex("<p>([^<]+)</p>").find(html)?.groupValues?.get(1)
            ?.replace("&nbsp;", " ")
            ?.trim()
            .orEmpty()
        val parts = line.split(Regex("\\s+"), limit = 2)
        val college = parts.getOrElse(0) { "" }.trim()
        val className = parts.getOrElse(1) { "" }.trim()

        if (name.isNullOrBlank()) return null

        val grade = Regex("(\\d{4})").find(className)?.groupValues?.get(1)?.let {
            val yy = it.take(2).toIntOrNull()
            if (yy != null && yy <= 49) "20${it.take(2)}" else it
        }.orEmpty()
        val major = className.replace(Regex("\\d+.*$"), "").trim()

        return UserProfile(
            name = name,
            studentNo = "",
            college = college,
            className = className,
            major = major,
            grade = grade
        )
    }

    // ---------------------------------------------------------------- 课表

    /**
     * 课表。正方教务的学期编码：xqm = "3" 表示第一学期，"12" 表示第二学期。
     */
    fun timetable(term: Term): Timetable {
        val body = FormBody.Builder()
            .add("xnm", term.year)
            .add("xqm", term.half)
            .add("kzlx", "ck")
            .add("xsdm", "")
            .add("kclbdm", "")
            .add("kclxdm", "")
            .build()

        val request = Request.Builder()
            .url(Endpoints.TIMETABLE)
            .header("User-Agent", Endpoints.UA)
            .header("X-Requested-With", "XMLHttpRequest")
            .header("Referer", Endpoints.INDEX_MENU)
            .post(body)
            .build()

        val json = JSONObject(execute(request))

        val xsxx = json.optJSONObject("xsxx") ?: JSONObject()
        val year = xsxx.optString("XNM").ifBlank { term.year }
        val half = xsxx.optString("XQM").ifBlank { term.half }
        val termLabelRaw = xsxx.optString("XNMC")

        val profile = UserProfile(
            name = xsxx.optString("XM"),
            studentNo = xsxx.optString("XH"),
            major = xsxx.optString("ZYMC"),
            className = xsxx.optString("BJMC"),
            grade = xsxx.optString("NJDM_ID")
        )

        val list = json.optJSONArray("kbList") ?: JSONArray()
        val courses = ArrayList<Course>(list.length())
        for (i in 0 until list.length()) {
            val item = list.optJSONObject(i) ?: continue
            val name = item.optString("kcmc").trim()
            if (name.isEmpty()) continue

            val sectionText = item.optString("jcs").ifBlank { item.optString("jcor") }
            val (start, end) = parseSections(sectionText)
            val weekRaw = item.optString("zcd").ifBlank { item.optString("zcmc") }
            val (ranges, parity) = parseWeeks(weekRaw)

            courses.add(
                Course(
                    name = name,
                    teacher = item.optString("xm").ifBlank { item.optString("jsxm") },
                    room = item.optString("cdmc"),
                    campus = item.optString("xqmc"),
                    dayOfWeek = item.optString("xqj").toIntOrNull() ?: 1,
                    startSection = start,
                    endSection = end,
                    weekRanges = ranges,
                    parity = parity,
                    weekRaw = weekRaw,
                    credit = item.optString("xf")
                )
            )
        }

        return Timetable(
            term = Term.of(year, half),
            termLabelRaw = termLabelRaw,
            courses = courses,
            profile = profile
        )
    }

    private fun parseSections(text: String): Pair<Int, Int> {
        val nums = Regex("\\d+").findAll(text).map { it.value.toInt() }.toList()
        return when {
            nums.isEmpty() -> 1 to 1
            nums.size == 1 -> nums[0] to nums[0]
            else -> nums[0] to nums[1]
        }
    }

    private fun parseWeeks(text: String): Pair<List<IntRange>, Parity> {
        val ranges = ArrayList<IntRange>()
        var parity = Parity.ALL
        if (text.isBlank()) return ranges to parity

        Regex("(\\d+)\\s*(?:-\\s*(\\d+))?\\s*周(?:[(（]([单双])[)）])?").findAll(text).forEach { m ->
            val a = m.groupValues[1].toIntOrNull() ?: return@forEach
            val b = m.groupValues[2].toIntOrNull() ?: a
            ranges.add(a..b)
            when (m.groupValues[3]) {
                "单" -> parity = Parity.ODD
                "双" -> parity = Parity.EVEN
            }
        }
        if (ranges.isEmpty()) {
            Regex("(\\d+)").findAll(text).firstOrNull()?.let {
                ranges.add(it.value.toInt()..it.value.toInt())
            }
        }
        return ranges to parity
    }

    // ---------------------------------------------------------------- 成绩

    fun grades(term: Term): List<Grade> {
        val token = ensureCsrf()
        val body = FormBody.Builder()
            .add("xnm", term.year)
            .add("xqm", term.half)
            .add("kcmc", "")
            .add("_search", "false")
            .add("nd", System.currentTimeMillis().toString())
            .add("queryModel.showCount", "100")
            .add("queryModel.currentPage", "1")
            .add("queryModel.sortName", "")
            .add("queryModel.sortOrder", "asc")
            .add("time", "0")
            .add("csrftoken", token)
            .build()

        val request = Request.Builder()
            .url(Endpoints.GRADE_QUERY)
            .header("User-Agent", Endpoints.UA)
            .header("X-Requested-With", "XMLHttpRequest")
            .header("Referer", Endpoints.GRADE_PAGE)
            .header("X-CSRF-TOKEN", token)
            .post(body)
            .build()

        val json = JSONObject(execute(request))
        val items = json.optJSONArray("items") ?: JSONArray()
        val out = ArrayList<Grade>(items.length())
        for (i in 0 until items.length()) {
            val it = items.optJSONObject(i) ?: continue
            out.add(
                Grade(
                    courseName = it.optString("kcmc"),
                    courseCode = it.optString("kch"),
                    credit = it.optString("xf").toDoubleOrNull() ?: 0.0,
                    score = it.optString("cj").ifBlank { it.optString("bfzcj") },
                    gpa = it.optString("jd").toDoubleOrNull() ?: -1.0,
                    category = it.optString("kclbmc").ifBlank { it.optString("kcxzmc") },
                    termYear = it.optString("xnm").ifBlank { term.year },
                    termHalfRaw = it.optString("xqm").ifBlank { term.half },
                    teacher = it.optString("xm").ifBlank { it.optString("jsxm") },
                    examType = it.optString("khfsmc"),
                    college = it.optString("jgmc"),
                    isRetake = it.optString("kcbj").contains("重修")
                )
            )
        }
        return out
    }

    /** 成绩查询需要页面里的 csrftoken，与当前会话绑定，所以每次会话只取一次 */
    private fun ensureCsrf(): String {
        csrfToken?.let { return it }
        val html = executeGet(Endpoints.GRADE_PAGE)
        val token = Regex("id=\"csrftoken\"[^>]*value=\"([^\"]*)\"")
            .find(html)?.groupValues?.get(1)
            ?: throw NetworkException("无法获取查询令牌，请重新登录")
        csrfToken = token
        return token
    }

    // ---------------------------------------------------------------- 校历

    fun calendar(): SchoolCalendar {
        val html = executeGet(Endpoints.CALENDAR)
        val dateFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd")

        val start = readHiddenValue(html, "ksrq")?.toDateOrNull(dateFmt)
        val end = readHiddenValue(html, "jsrq")?.toDateOrNull(dateFmt)

        val termText = Regex("<title>([^<]*)</title>").find(html)?.groupValues?.get(1).orEmpty()

        // Android 的 ICU 正则对孤立的 "}" 会报语法错误，这里全部用字符串扫描，避免踩坑
        val events = ArrayList<CalendarEvent>()
        var cursor = 0
        while (true) {
            val begin = html.indexOf("rc.push(", cursor)
            if (begin < 0) break
            val pairEnd = html.indexOf("})", begin)
            val close = if (pairEnd >= 0) pairEnd else html.indexOf(")", begin)
            if (close < 0) break
            val block = html.substring(begin, close)
            val title = quotedField(block, "title")
            if (!title.isNullOrBlank()) {
                events.add(
                    CalendarEvent(
                        startDate = quotedField(block, "bgdate")?.toDateOrNull(dateFmt),
                        endDate = quotedField(block, "eddate")?.toDateOrNull(dateFmt),
                        title = title
                    )
                )
            }
            cursor = close + 1
        }

        val classDays = ArrayList<LocalDate>()
        cursor = 0
        val classPrefix = "skrq.push('"
        while (true) {
            val begin = html.indexOf(classPrefix, cursor)
            if (begin < 0) break
            val from = begin + classPrefix.length
            val endQuote = html.indexOf('\'', from)
            if (endQuote < 0) break
            html.substring(from, endQuote).toDateOrNull(dateFmt)?.let { classDays.add(it) }
            cursor = endQuote + 1
        }

        return SchoolCalendar(
            startDate = start,
            endDate = end,
            events = events,
            classDays = classDays.distinct().sorted(),
            rawTermText = termText
        )
    }

    private fun readHiddenValue(html: String, id: String): String? {
        val idIdx = html.indexOf("id=\"$id\"")
        if (idIdx < 0) return null
        val valueIdx = html.indexOf("value=\"", idIdx)
        if (valueIdx < 0) return null
        val from = valueIdx + "value=\"".length
        val close = html.indexOf('"', from)
        if (close < 0) return null
        return html.substring(from, close)
    }

    /** 从 "{bgdate:'2026-08-24',title:'x'}" 这类片段里取出单/双引号包裹的字段值 */
    private fun quotedField(block: String, key: String): String? {
        val keyIdx = block.indexOf(key)
        if (keyIdx < 0) return null
        var i = keyIdx + key.length
        while (i < block.length && (block[i] == ':' || block[i].isWhitespace())) i++
        if (i >= block.length) return null
        val quote = block[i]
        if (quote != '\'' && quote != '"') return null
        val close = block.indexOf(quote, i + 1)
        if (close < 0) return null
        return block.substring(i + 1, close)
    }

    private fun String.toDateOrNull(fmt: DateTimeFormatter): LocalDate? =
        runCatching { LocalDate.parse(this, fmt) }.getOrNull()

    // ---------------------------------------------------------------- 底层请求

    private fun executeGet(url: String): String {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", Endpoints.UA)
            .header("Referer", Endpoints.INDEX_MENU)
            .get()
            .build()
        return execute(request)
    }

    private fun execute(request: Request): String {
        try {
            client.newCall(request).execute().use { response ->
                val finalUrl = response.request.url
                if (finalUrl.host.contains("authserver") ||
                    finalUrl.encodedPath.contains("login_slogin")
                ) {
                    throw SessionExpiredException()
                }

                val body = response.body?.string().orEmpty()
                if (looksLikeLoginPage(body)) throw SessionExpiredException()
                if (response.code >= 400) throw NetworkException("服务器返回 ${response.code}")

                return body
            }
        } catch (e: SessionExpiredException) {
            throw e
        } catch (e: ProtocolException) {
            throw SessionExpiredException()
        } catch (e: IOException) {
            throw NetworkException("网络请求失败：${e.message}", e)
        }
    }

    private fun looksLikeLoginPage(body: String): Boolean =
        body.contains("pwdEncryptSalt") && body.contains("统一身份认证")
}
