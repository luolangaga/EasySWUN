"""完整验证：登录 -> 课表 -> 成绩 -> 校历 -> 个人资料，输出 JSON 供 App 数据层设计参考"""
import json, re, random, base64, requests, sys
from Crypto.Cipher import AES
from Crypto.Util.Padding import pad
import urllib.parse as up

UA = "Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Mobile Safari/537.36"
SSO, JWXT = "https://authserver.swun.edu.cn", "https://jwxt.swun.edu.cn"
import os
USER, PWD = os.environ.get("SWUN_USER", ""), os.environ.get("SWUN_PASSWORD", "")
if not USER or not PWD:
    raise SystemExit("请先设置环境变量 SWUN_USER / SWUN_PASSWORD")
CHARS = "ABCDEFGHJKMNPQRSTWXYZabcdefhijkmnprstwxyz2345678"
SERVICE = "http://jwxt.swun.edu.cn/sso/jziotlogin"

def rnd(n): return "".join(random.choice(CHARS) for _ in range(n))
def enc(pwd, salt):
    c = AES.new(salt.encode(), AES.MODE_CBC, rnd(16).encode())
    return base64.b64encode(c.encrypt(pad((rnd(64) + pwd).encode(), 16))).decode()

s = requests.Session(); s.headers.update({"User-Agent": UA})
url = SSO + "/authserver/login?service=" + up.quote(SERVICE, safe="")
r = s.get(url, timeout=30)
salt = re.search(r'id="pwdEncryptSalt"\s+value="([^"]*)"', r.text).group(1)
ex = re.search(r'name="execution"[^>]*value="([^"]*)"', r.text).group(1)
resp = s.post(url, data={"username": USER, "password": enc(PWD, salt), "captcha": "", "lt": "",
    "_eventId": "submit", "cllt": "userNameLogin", "dllt": "generalLogin", "execution": ex, "rmShown": "1"},
    allow_redirects=False, timeout=30)
print("CAS:", resp.status_code, resp.headers.get("Location", "")[:80])
s.get(resp.headers["Location"], timeout=30, allow_redirects=True)
print("JWXT cookies:", list(s.cookies.get_dict().keys()))

H = {"X-Requested-With": "XMLHttpRequest", "Referer": JWXT + "/jwglxt/xtgl/index_initMenu.html?jsdm=xs"}

# ---------- 课表 ----------
kb = s.post(JWXT + "/jwglxt/kbcx/xskbcx_cxXsgrkb.html?gnmkdm=N2151",
            data={"xnm": "2026", "xqm": "3", "kzlx": "ck", "xsdm": "", "kclbdm": "", "kclxdm": ""},
            headers=H, timeout=30).json()
print("\n[课表] 学生:", kb["xsxx"]["XM"], kb["xsxx"]["BJMC"], "课程数:", len(kb.get("kbList", [])))
for c in kb.get("kbList", [])[:4]:
    print("   ", c.get("kcmc"), "|", c.get("xm"), "|", c.get("cdmc"), "|", c.get("xqjmc"), "|",
          c.get("jcs") or c.get("jcor"), "| 周次:", c.get("zcd"), "| 节次:", c.get("jc"))

# ---------- 成绩 ----------
pg = s.get(JWXT + "/jwglxt/cjcx/cjcx_cxDgXscj.html?gnmkdm=N305005&layout=default",
           headers={"Referer": JWXT + "/jwglxt/xtgl/index_initMenu.html"}, timeout=30)
csrf = re.search(r'id="csrftoken"[^>]*value="([^"]*)"', pg.text).group(1)
all_items = []
for xnm in ("2026", "2025"):
    for xqm in ("12", "3"):
        rr = s.post(JWXT + "/jwglxt/cjcx/cjcx_cxXsgrcj.html?doType=query&gnmkdm=N305005",
                    data={"xnm": xnm, "xqm": xqm, "kcmc": "", "_search": "false", "nd": "1794885000000",
                          "queryModel.showCount": "100", "queryModel.currentPage": "1",
                          "queryModel.sortName": "", "queryModel.sortOrder": "asc", "time": "0",
                          "csrftoken": csrf},
                    headers={**H, "Referer": pg.url, "X-CSRF-TOKEN": csrf}, timeout=30)
        try:
            j = rr.json()
        except Exception:
            continue
        print(f"[成绩] {xnm}-{xqm}: total={j.get('totalResult')}")
        all_items += j.get("items", [])
print("成绩样例:", json.dumps(all_items[:2], ensure_ascii=False)[:600])
if all_items:
    print("字段:", sorted(all_items[0].keys()))

# ---------- 校历 ----------
xl = s.get(JWXT + "/jwglxt/pkgl/xlglMobile_cxXlIndexForxs.html?gnmkdm=Y210501",
           headers={"Referer": JWXT + "/jwglxt/xtgl/index_initMenu.html"}, timeout=30)
txt = xl.text
ks = re.search(r'id="ksrq"\s+value="([^"]*)"', txt)
js = re.search(r'id="jsrq"\s+value="([^"]*)"', txt)
events = re.findall(r"rc\.push\(\{\s*type: *'type(?:'+\w*)[^,]*,\s*bgdate: *'([^']*)',\s*eddate: *'([^']*)',\s*title:'([^']*)'", txt)
skrq = re.findall(r"skrq\.push\('([^']*)'\)", txt)
print(f"\n[校历] {ks.group(1) if ks else '?'} ~ {js.group(1) if js else '?'} | 上课日 {len(skrq)} 天 | 事件 {len(events)}")

# ---------- 个人信息 ----------
yh = s.get(JWXT + "/jwglxt/xtgl/index_cxYhxxIndex.html?xt=jw&localeKey=zh_CN&gnmkdm=index",
           headers={"Referer": JWXT + "/jwglxt/xtgl/index_initMenu.html"}, timeout=30)
print("\n[个人信息]", yh.status_code, len(yh.text))
print(re.sub(r"<[^>]+>", " ", yh.text).strip()[:300])

json.dump({"kb": kb, "grades": all_items[:3]}, open("api_sample.json", "w", encoding="utf-8"), ensure_ascii=False, indent=1)
