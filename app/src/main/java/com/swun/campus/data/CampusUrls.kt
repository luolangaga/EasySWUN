package com.swun.campus.data

object CampusUrls {
    /** 一卡通首页（瑞数 WAF，只能在 WebView 里打开） */
    const val CARD_HOME = "https://card.swun.edu.cn/"

    /**
     * 带 CAS service 的登录地址。
     *
     * 关键：一卡通的 CAS 入口不是根路径，而是 /casLogin.action。
     * 之前用根路径做 service，CAS 票据会落到公共首页然后被丢弃；
     * 用 /casLogin.action 作为 service，票据才能被一卡通后台兑换成会话。
     */
    const val CARD_SSO =
        "https://authserver.swun.edu.cn/authserver/login?service=https%3A%2F%2Fcard.swun.edu.cn%2FcasLogin.action"

    /** 一卡通登录后的个人信息 / 余额 / 官方缴费页 */
    const val CARD_CAS_LOGIN = "https://card.swun.edu.cn/casLogin.action"
    const val CARD_INFO = "https://card.swun.edu.cn/queryUserInfo.action"
    const val CARD_BALANCE = "https://card.swun.edu.cn/queryUserBalances.action"

    /**
     * 一卡通官方 H5。
     *
     * 学校一卡通的付款码和充值不是 card.swun.edu.cn 的 Struts 页面，
     * 而是 ykt H5 的两个 menu：
     * - menu=qrcode  付款码
     * - menu=transfer 充值/转账
     *
     * 该 H5 在官方 App 里通过 MCK 原生桥拿 getUserInfo；独立浏览器打开会白屏。
     * App 内由 WebView 注入学生信息后跳到 function.do，不伪造二维码。
     */
    const val CARD_PAYCODE = "https://ykth5.swun.edu.cn/menu/menu.do?menu=qrcode"
    const val CARD_PAY = "https://ykth5.swun.edu.cn/menu/menu.do?menu=transfer"
    const val CARD_ELECTRIC = "https://ykth5.swun.edu.cn/menu/menu.do?menu=electric"
    const val CARD_WATER = "https://ykth5.swun.edu.cn/menu/menu.do?menu=watertransfer"
    const val CARD_BILL = "https://ykth5.swun.edu.cn/menu/menu.do?menu=bill"

    /** 后勤处交通服务 / 通勤车时刻表 */
    const val CAMPUS_BUS = "https://hkghq.swun.edu.cn/oldhq/fwpt/ysfw.htm"

    /** 一站式服务大厅 */
    const val EHALL = "https://ehall.swun.edu.cn/index.html#/"

    /** 校园地图（H5 需要定位权限） */
    const val CAMPUS_MAP = "https://gis.swun.edu.cn/cmipsh5/#/"

    /**
     * 学工/晚归的官方移动端入口。
     *
     * 这是官方 App 使用的「学生管理平台移动端」CAS service：
     * zxtjfx.swun.edu.cn/warnvisit-api/.../swun-stu-manage-platform-mobile
     * App 已有统一身份认证，带 ticket 回跳后不再需要单独登录学工。
     */
    const val STUDENT_PLATFORM_SSO =
        "https://authserver.swun.edu.cn/authserver/login?service=https://zxtjfx.swun.edu.cn/warnvisit-api/authority/noToken/casLogin/swun-stu-manage-platform-mobile"

    const val CHECKIN_WEB = STUDENT_PLATFORM_SSO

    /**
     * 晚归打卡的统一认证入口。
     *
     * 直接打开 CHECKIN_PAGE 时，一旦 gyglxt 自己的 JSESSIONID 过期，
     * 页面会停在 gyglxt 的「系统登录」账号/密码/验证码页。
     * 从这里进会先跳 school CAS（service 是 gyglxt 的 appcas），
     * App 已持久化的 authserver CASTGC 还在，就能自动换票回来，
     * 再由 ssoMobileLogin.jsp 建立 gyglxt 的新会话。
     */
    const val CHECKIN_SSO = "https://gyglxt.swun.edu.cn/appcas/ssoMobileLogin.jsp"

    /** 已登录时可直接进入的官方晚归打卡页。 */
    const val CHECKIN_PAGE = "https://gyglxt.swun.edu.cn/wxweb/#/PositioningClock"

    /**
     * 学工（请销假）。
     *
     * 直接从学生管理平台移动端进入；不再走 szjy 的独立账号登录，
     * 也不再把用户丢到 ehall 首页。
     */
    const val LEAVE_HOME = STUDENT_PLATFORM_SSO
    const val LEAVE_APPLY = STUDENT_PLATFORM_SSO
    const val LEAVE_MOBILE = "https://zxtjfx.swun.edu.cn/swun-stu-manage-platform-mobile/#/leave/apply"
    const val LEAVE_DAILY = STUDENT_PLATFORM_SSO
    const val LEAVE_LOGIN = "https://szjy.swun.edu.cn/ServiceHall/Web/Account/Login"

    /**
     * 旧的 szjy CAS 回调。
     *
     * CAS 对
     * `https://szjy.swun.edu.cn/ServiceHall/Web/Account/CasLogin`
     * 返回 “Application Not Authorized to Use IDS”，说明该 service 未注册。
     * 保留常量仅作记录，不再作为主路径。
     */
    const val LEAVE_SSO =
        "https://authserver.swun.edu.cn/authserver/login?service=https%3A%2F%2Fszjy.swun.edu.cn%2FServiceHall%2FWeb%2FAccount%2FCasLogin"

    /** 课堂考勤官方说明（微信小程序） */
    const val ATTENDANCE_NOTICE = "https://jwc.swun.edu.cn/info/1037/4846.htm"
    const val ATTENDANCE_HANDOFF = "https://jwc.swun.edu.cn/info/1037/4806.htm"

    /**
     * 课堂考勤官方移动端（ktkq）。
     *
     * 这是教务处的课堂考勤系统，不是晚归打卡；入口走学校统一认证，
     * 登录态与当前 App 的 CAS 会话共用。页面使用手机宽度，不再用 AIOCP 的 PC 端口。
     */
    const val ATTENDANCE_OFFICIAL =
        "https://ktkq.swun.edu.cn/jwmobile/index#/index/kb/course/list"
    const val ATTENDANCE_AUTH = "https://ktkq.swun.edu.cn/jwmobile/auth/index"

    /**
     * 用 App 的统一认证会话给 ktkq 换自己的登录态。
     * 直接打开 ktkq 首页不会自动走 CAS，接口会返回 401「认证失败」。
     */
    const val ATTENDANCE_CAS =
        "https://authserver.swun.edu.cn/authserver/login?service=https%3A%2F%2Fktkq.swun.edu.cn%2Fjwmobile%2Fauth%2Findex"

    /** 一卡通教学考勤系统（AIOCP），需校园网 / 学校 VPN */
    const val ATTENDANCE_WEB = "http://218.194.81.247:8080/AIOCP/"
    const val ATTENDANCE_MINI = "西南民大课堂考勤系统"

    /** 官方「和合民大」App，晚归打卡的最终提交入口 */
    const val OFFICIAL_APP_PACKAGE = "com.lantu.MobileCampus.swun"
    const val OFFICIAL_APP_SCHEME = "lantumcampus://"

    /**
     * 快递身份码。
     *
     * 淘宝：官方末端取件「身份码」H5，指定用淘宝自己的 BrowserActivity 打开，
     * 避免落到系统浏览器后丢失淘宝登录态。
     * 拼多多：官方 App 的驿站身份码深链，直接进入「身份码」Tab。
     */
    const val TAOBAO_IDENTITY =
        "https://pages-fast.m.taobao.com/wow/z/uniapp/1011717/last-mile-fe/end-collect-platform/identity-code?x-ssr=true"
    const val PINDUODUO_IDENTITY =
        "pinduoduo://com.xunmeng.pinduoduo/mdkd/package?tab=ID_CODE&entry_source=11"
    const val TAOBAO_PACKAGE = "com.taobao.taobao"
    const val TAOBAO_BROWSER_ACTIVITY = "com.taobao.browser.BrowserActivity"
    const val PINDUODUO_PACKAGE = "com.xunmeng.pinduoduo"

    /** 航空港校区中心点，用于判断是否在校园附近 */
    const val CAMPUS_LAT = 30.5566
    const val CAMPUS_LNG = 103.9868
}
