import glob
import re
import os

files = glob.glob('c:/workspace/dogo/src/main/resources/templates/**/*.html', recursive=True)
pattern = re.compile(r"'sm': '0px',\s*'md': '0px',\s*'lg': '0px',\s*'xl': '0px',\s*'2xl': '0px',")
replacement = "'sm': '1px',\n            'md': '2px',\n            'lg': '3px',\n            'xl': '4px',\n            '2xl': '5px',"

for f in files:
    try:
        with open(f, 'r', encoding='utf-8') as file:
            content = file.read()
        if pattern.search(content):
            new_content = pattern.sub(replacement, content)
            with open(f, 'w', encoding='utf-8') as file:
                file.write(new_content)
            print(f"Updated {f}")
    except Exception as e:
        print(f"Failed {f}: {e}")
