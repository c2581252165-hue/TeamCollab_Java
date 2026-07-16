import os

def replace_in_files(directory, search, replace, exts):
    for root, _, files in os.walk(directory):
        for file in files:
            if any(file.endswith(ext) for ext in exts):
                filepath = os.path.join(root, file)
                try:
                    with open(filepath, 'r', encoding='utf-8') as f:
                        content = f.read()
                    if search in content:
                        content = content.replace(search, replace)
                        with open(filepath, 'w', encoding='utf-8') as f:
                            f.write(content)
                        print(f"Updated {filepath}")
                except Exception as e:
                    print(f"Error processing {filepath}: {e}")

replace_in_files("d:/ruanjian/frontend", "TeamCollab", "CloudOffice", [".vue", ".js", ".html"])
