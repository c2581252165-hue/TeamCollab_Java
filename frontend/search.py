import os

search_texts = ["返回工作台", "新手向导"]

for root, dirs, files in os.walk("d:/ruanjian/frontend/src"):
    for file in files:
        if file.endswith(".vue"):
            filepath = os.path.join(root, file)
            with open(filepath, "r", encoding="utf-8") as f:
                content = f.read()
                for t in search_texts:
                    if t in content:
                        print(f"Found '{t}' in {filepath}")
