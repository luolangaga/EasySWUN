"""探查正方教务 V9 学生端菜单：登录后抓 index_initMenu 页面，列出所有可用模块与 gnmkdm"""
import re
import random
import base64
import requests
import urllib.parse as up
from Crypto.Cipher import AES
from Crypto.Util.Padding import pad

UA = ("Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 "
      "(KHTML, like Gecko) Chrome/131.0.0.0 Mobile Safari/537.36")
SSO, JWXT = "https://authserver.swun.edu.cn", "https://jwxt.swun.edu.cn"
import os
USER, PWD = os.environ.get("SWUN_USER", ""), os.environ.get("SWUN_PASSWORD", "")
if not USER or not PWD:
    raise SystemExit("请先设置环境变量 SWUN_USER / SWUN_PASSWORD")
CHARS = "ABCDEFGHJKMNPQRSTWXYZabcdefhijkmnprstwxyz2345678"
SERVICE = "http://jwxt.swun.edu.cn/sso/jziotlogin"


def rnd(n):
    return "".join(random.choice(CHARS) for _ in range(n))


def enc(pwd, salt):
    c = AES.new(salt.encode(), AES.MODE_CBC, rnd(16).encode())
    return base64.b64encode(c.encrypt(pad((rnd(64) + pwd).encode(), 16))).decode()


s = requests.Session()
s.headers.update({"User-Agent": UA})
url = SSO + "/authserver/login?service=" + up.quote(SERVICE, safe="")
r = s.get(url, timeout=30)
salt = re.search(r'id="pwdEncryptSalt"\s+value="([^"]*)"', r.text).group(1)
ex = re.search(r'name="execution"[^>]*value="([^"]*)"', r.text).group(1)
resp = s.post(url, data={
    "username": USER, "password": enc(PWD, salt), "captcha": "", "lt": "",
    "_eventId": "submit", "cllt": "userNameLogin", "dllt": "generalLogin",
    "execution": ex, "rmShown": "1",
}, allow_redirects=False, timeout=30)
print("CAS:", resp.status_code, str(resp.headers.get("Location", ""))[:80])
if resp.status_code != 302:
    print("login failed, body head:", r.text[:200])
    raise SystemExit(1)
s.get(resp.headers["Location"], timeout=30, allow_redirects=True)
print("cookies:", list(s.cookies.get_dict().keys()))

menu = s.get(JWXT + "/jwglxt/xtgl/index_initMenu.html?jsdm=xs", timeout=30)
print("menu status:", menu.status_code, "len:", len(menu.text), "url:", menu.url)
open("menu.html", "w", encoding="utf-8").write(menu.text)

# 1) 所有 clickMenu 调用的参数
calls = re.findall(r"clickMenu\((.*?)\)", menu.text, re.S)
print("\n=== clickMenu calls:", len(calls))
for c in calls[:60]:
    print(" ", " | ".join(x.strip()[:40] for x in c.split(",")))

# 2) 所有模块代码
codes = sorted(set(re.findall(r"[NY]\d{4,6}", menu.text)))
print("\n=== gnmkdm codes:", codes)

# 3) 菜单文字
text = re.sub(r"<(script|style)[^>]*>.*?</\1>", " ", menu.text, flags=re.S | re.I)
text = re.sub(r"<[^>]+>", " ", text)
text = re.sub(r"\s+", " ", text).strip()
print("\n=== menu text:\n", text[:1500])
