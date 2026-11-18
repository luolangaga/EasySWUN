# 民大校园 · EasySWUN

西南民族大学非官方 Android 校园客户端。使用 Kotlin + Jetpack Compose 编写，把学校教务、一卡通、学工和后勤的常用页面集中到一个应用内。

> 当前版本：**1.0.1**（versionCode 2）
>
> 开源仓库：<https://github.com/luolangaga/EasySWUN>

## 功能

| 板块 | 功能 |
|---|---|
| 教务 | 今日课表、完整周课表、成绩与绩点、校历、教学周定位 |
| 一卡通 | 官方动态付款码（内嵌）、余额、充值、账单查询 |
| 校园缴费 | 电费充值、淋浴 / 热水充值 |
| 学工 | 晚归打卡、请销假（直达学校官方移动页） |
| 课堂考勤 | 学校官方课堂考勤页内嵌，支持课堂签到 |
| 出行 | 三校区校车时刻、下一班倒计时、工作日 / 周末 / 节假日 |
| 账号 | 个人信息、退出登录、关于与 GitHub 仓库入口 |

## 设计说明

- 登录使用学校统一身份认证 CAS，在应用内 WebView 完成，不依赖官方 App。
- 一卡通余额和付款码使用两个独立 WebView：进入一卡通页先加载官方付款码，二维码出现后再同步余额，避免两个请求同时触发瑞数 WAF。
- 付款码使用学校 `ykth5` 官方页面生成，每分钟自动刷新；不伪造二维码。
- 电费、淋浴、账单、课堂考勤、晚归打卡和请销假均直接嵌入 / 直达学校官方页面。
- 充值会进入学校官方建行收银台。学校 `poscardcircle.do` 返回的订单只包含建行商户参数，第三方客户端无法直接拉起微信支付。
- 校车时刻为原生 Compose 页面，数据取自学校后勤公布的时刻表。
- 底部导航是悬浮的「液态玻璃」胶囊：用 [Haze](https://github.com/chrisbanes/haze) 对底栏盖住的那部分内容做真实高斯模糊，再叠镜面渐变、1dp 光边和细噪声；选中态胶囊用两条不同阻尼的弹簧驱动，切换时会先拉长再回弹。

## 技术栈

- Kotlin 2.2 + Jetpack Compose（Compose BOM 2025.06）
- Material Design 3，自定义浅色 / 深色配色
- 单 Activity + Navigation Compose
- OkHttp 4.12 + 持久化 CookieJar
- WebView 处理 CAS 登录、瑞数 WAF 后的学校 H5
- Haze 1.6（背景模糊，做底栏的液态玻璃）
- compileSdk 36 / minSdk 26 / targetSdk 36

## 构建

```powershell
.\gradlew.bat assembleDebug
# 产物：app\build\outputs\apk\debug\app-debug.apk
```

需要 JDK 17+、Android SDK 36。`local.properties` 中的 SDK 路径请按本机修改，不要提交。

## 目录

```
app/src/main/java/com/swun/campus/
├── AppViewModel.kt              # 登录、课表、成绩、状态
├── data/                        # CAS、Cookie、教务 / 一卡通数据
├── ui/
│   ├── academic/                # 课表、成绩、校历、课堂考勤
│   ├── account/                 # 我的、关于
│   ├── life/                    # 一卡通、缴费、校车、学工
│   └── web/                     # 通用校园 WebView / Cookie 同步
└── res/drawable-nodpi/          # 手绘版校徽
```

## 免责声明

本项目为个人学习与校园生活辅助工具，与西南民族大学官方无关。所有数据来自学校官方系统，使用前请确认符合学校相关管理规定；请勿用于商业用途或大规模分发。账号、密码和 Cookie 仅保存在本机应用私有存储中。

## License

仅用于个人学习交流，未经作者许可请勿将本项目或其衍生版本用于商业用途。
