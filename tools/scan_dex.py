import zipfile, re

apk = r"D:\EasySWUN\dist\西南民族大学校园总览-debug.apk"
z = zipfile.ZipFile(apk)
dex = [n for n in z.namelist() if n.endswith(".dex")]
for name in dex:
    data = z.read(name)
    for m in re.finditer(rb"rc\\\.push[^\x00]{0,40}", data):
        print(name, "->", m.group(0)[:60])
    for m in re.finditer(rb"skrq\\\.push[^\x00]{0,30}", data):
        print(name, "->", m.group(0)[:50])
z.close()
