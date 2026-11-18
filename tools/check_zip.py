import zipfile

p = r"D:\EasySWUN\dist\西南民族大学校园总览-源码.zip"
z = zipfile.ZipFile(p)
names = z.namelist()
target = [n for n in names if n.replace("\\", "/").endswith("data/CampusApi.kt")][0]
text = z.read(target).decode("utf-8")
print("files:", len(names))
print("new grade logic:", "yy <= 49" in text)
print("profile split:", "split(Regex(" in text)
print("README:", any(n.lower().endswith("readme.md") for n in names))
print("gradlew.bat:", any(n.lower().endswith("gradlew.bat") for n in names))
print("wrapper jar:", any("gradle-wrapper.jar" in n for n in names))
