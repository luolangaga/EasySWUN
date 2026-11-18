# tools

联调与排查脚本，都是在这台机器上真实跑通过的。凭据一律走环境变量，代码里不保存。

```powershell
$env:SWUN_USER="学号"
$env:SWUN_PASSWORD="密码"
```

| 脚本 | 用途 |
|---|---|
| api_full.py | 全链路验证：CAS 登录、课表、成绩、校历、个人信息，打印真实返回，用于确认学校接口有没有变 |
| menu_probe.py | 抓 xtgl/index_initMenu.html，列出学生端全部模块与 gnmkdm（当前抓到 33 个模块） |
| module_probe.py | 探测候选模块接口：考试安排 N358105、学业情况 N105515、空闲教室 N2155 |
| pack_zip.py | 打包源码 zip（排除 build / .gradle / .kotlin） |
| scan_dex.py | 扫 APK 里的字符串常量，排查发布包是否漏了某段逻辑 |
| check_zip.py | 校验源码 zip 内容完整性 |
| enc.js | 早先在浏览器里对照加密结果的 JS 片段（AES/CBC 加随机 IV） |
| RegexCheck.java | 复现 Android ICU 正则不接受孤立右花括号的对照用例 |

## 注意

- 跑 api_full.py 或 menu_probe.py 都会真实登录一次统一身份认证，别高频重复，多次失败可能触发验证码。
- 登录加密是 AES/CBC/PKCS7：明文等于 64 位随机前缀加真实密码，key 是 pwdEncryptSalt（16 字节 UTF-8），iv 每次随机。ECB 或固定 IV 一定登录失败。
- 课表 xqm=3 是第一学期、12 是第二学期；成绩必须先 GET 成绩页取 csrftoken 再 POST。
- 抓下来的 HTML 里含会话信息，不要提交到仓库。
