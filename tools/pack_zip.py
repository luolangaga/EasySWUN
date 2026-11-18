import os
import zipfile

root = r"D:\EasySWUN"
out = r"D:\EasySWUN\dist\西南民族大学校园总览-源码.zip"
os.makedirs(os.path.dirname(out), exist_ok=True)
exclude_dirs = {"build", ".gradle", ".idea", ".kotlin", ".git", "dist"}
exclude_files = {"local.properties"}

if os.path.exists(out):
    os.remove(out)

count = 0
with zipfile.ZipFile(out, "w", zipfile.ZIP_DEFLATED) as z:
    for dirpath, dirnames, filenames in os.walk(root):
        dirnames[:] = [d for d in dirnames if d not in exclude_dirs]
        for f in filenames:
            if f in exclude_files:
                continue
            p = os.path.join(dirpath, f)
            rel = os.path.relpath(p, os.path.dirname(root))
            z.write(p, rel)
            count += 1

print("files:", count)
print("size MB:", round(os.path.getsize(out) / 1024 / 1024, 2))
