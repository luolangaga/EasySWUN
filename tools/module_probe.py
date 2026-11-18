"""验证三个候选模块的真实接口：考试安排 / 学业情况 / 空闲教室"""
import re
import json
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
print("CAS:", resp.status_code)
s.get(resp.headers["Location"], timeout=30, allow_redirects=True)

H = {"X-Requested-With": "XMLHttpRequest",
     "Referer": JWXT + "/jwglxt/xtgl/index_initMenu.html?jsdm=xs"}


def csrf_of(html):
    m = re.search(r'id="csrftoken"[^>]*value="([^"]*)"', html)
    return m.group(1) if m else ""


# ---------------- 1. 考试安排 ----------------
print("\n========== 考试安排 N358105")
pg = s.get(JWXT + "/jwglxt/kwgl/kscx_cxXsksxxIndex.html?gnmkdm=N358105&layout=default",
           headers=H, timeout=30)
print("page:", pg.status_code, len(pg.text), "csrf:", bool(csrf_of(pg.text)))
cands = sorted(set(re.findall(r"[\w./]*kscx[\w.]*\.html", pg.text)))
print("urls in page:", cands[:6])
token = csrf_of(pg.text)
for u in ["/jwglxt/kwgl/kscx_cxXsksxxList.html?doType=query&gnmkdm=N358105",
          "/jwglxt/kwgl/kscx_cxXsksxx.html?doType=query&gnmkdm=N358105"]:
    try:
        rr = s.post(JWXT + u, data={
            "xnm": "2026", "xqm": "3", "ksmcdmb_id": "", "kch": "", "kc": "", "ksrq": "",
            "_search": "false", "nd": "1", "queryModel.showCount": "100",
            "queryModel.currentPage": "1", "queryModel.sortName": "",
            "queryModel.sortOrder": "asc", "time": "0", "csrftoken": token,
        }, headers={**H, "Referer": pg.url, "X-CSRF-TOKEN": token}, timeout=30)
        print(" POST", u.split("/")[-1], "->", rr.status_code, rr.text[:160].replace("\n", " "))
        if rr.headers.get("content-type", "").startswith("application/json"):
            j = rr.json()
            print("   total:", j.get("totalResult"), "items:", len(j.get("items", [])))
            if j.get("items"):
                print("   fields:", sorted(j["items"][0].keys())[:40])
                print("   sample:", json.dumps(j["items"][0], ensure_ascii=False)[:400])
            break
    except Exception as e:
        print("  err", e)

# ---------------- 2. 学业情况 ----------------
print("\n========== 学业情况 N105515")
pg2 = s.get(JWXT + "/jwglxt/xsxy/xsxyqk_cxXsxyqkIndex.html?gnmkdm=N105515&layout=default",
            headers=H, timeout=30)
print("page:", pg2.status_code, len(pg2.text), "csrf:", bool(csrf_of(pg2.text)))
cands2 = sorted(set(re.findall(r"[\w./]*xsxyqk[\w.]*\.html", pg2.text)))
print("urls in page:", cands2[:6])
token2 = csrf_of(pg2.text)
for u in ["/jwglxt/xsxy/xsxyqk_cxXsxyqk.html?gnmkdm=N105515",
          "/jwglxt/xsxy/xsxyqk_cxXsxyqkList.html?doType=query&gnmkdm=N105515"]:
    try:
        rr = s.post(JWXT + u, data={"csrftoken": token2}, headers={**H, "X-CSRF-TOKEN": token2}, timeout=30)
        print(" POST", u.split("/")[-1], "->", rr.status_code,
              rr.headers.get("content-type", "")[:30], rr.text[:180].replace("\n", " "))
    except Exception as e:
        print("  err", e)

# ---------------- 3. 空闲教室 ----------------
print("\n========== 空闲教室 N2155")
pg3 = s.get(JWXT + "/jwglxt/cdjy/cdjy_cxKxcdlb.html?gnmkdm=N2155&layout=default",
            headers=H, timeout=30)
print("page:", pg3.status_code, len(pg3.text), "csrf:", bool(csrf_of(pg3.text)))
print("urls in page:", sorted(set(re.findall(r"[\w./]*cdjy[\w.]*\.html", pg3.text)))[:6])
print("text:", re.sub(r"\s+", " ", re.sub(r"<[^>]+>", " ", pg3.text))[:300])
