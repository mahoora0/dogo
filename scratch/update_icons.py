import os
import re

ADMIN_DIR = r"c:\workspace\dogo\src\main\resources\templates\admin"
ADMIN_BASE_FILE = r"c:\workspace\dogo\src\main\resources\templates\layout\admin_base.html"

# SVG definitions to replace
# 1. 1:1 Inquiry Path
inquiry_pattern = re.compile(
    r'<svg\s+viewBox="0\s+0\s+24\s+24"\s+class="w-5\s+h-5"\s+fill="currentColor">\s*<path\s+d="M4\s+4h16v12H7l-3\s+4V4Zm2\s+2v8h10v-2H8v-2h8V8H8V6H6Z"/>\s*</svg>',
    re.IGNORECASE | re.DOTALL
)
inquiry_replacement = """<svg viewBox="0 0 24 24" style="width: 25px; height: 25px; flex-shrink: 0;" fill="currentColor">
                        <path d="M4 4h16v12H7l-3 4V4Zm2 2v8h10v-2H8v-2h8V8H8V6H6Z"/>
                    </svg>"""

# 2. Notice Path
notice_pattern = re.compile(
    r'<svg\s+viewBox="0\s+0\s+24\s+24"\s+class="w-5\s+h-5"\s+fill="currentColor">\s*<path\s+d="M4\s+10v4h3l5\s+4V6l-5\s+4H4Zm11.5\s+2a3.5\s+3.5\s+0\s+0\s+0-1.5-2.9v5.8a3.5\s+3.5\s+0\s+0\s+0\s+1.5-2.9ZM16\s+4.8v2.4a6\s+6\s+0\s+0\s+1\s+0\s+9.6v2.4a8\s+8\s+0\s+0\s+0\s+0-14.4Z"/>\s*</svg>',
    re.IGNORECASE | re.DOTALL
)
notice_replacement = """<svg viewBox="0 0 24 24" style="width: 25px; height: 25px; flex-shrink: 0;" fill="currentColor">
                        <path d="M4 10v4h3l5 4V6l-5 4H4Zm11.5 2a3.5 3.5 0 0 0-1.5-2.9v5.8a3.5 3.5 0 0 0 1.5-2.9ZM16 4.8v2.4a6 6 0 0 1 0 9.6v2.4a8 8 0 0 0 0-14.4Z"/>
                    </svg>"""

# 3. FAQ Path
faq_pattern = re.compile(
    r'<svg\s+viewBox="0\s+0\s+24\s+24"\s+class="w-5\s+h-5"\s+fill="currentColor">\s*<path\s+d="M11\s+18h2v-2h-2v2Zm1-16a8\s+8\s+0\s+1\s+0\s+0\s+16h1v4l4-4.7A8\s+8\s+0\s+0\s+0\s+12\s+2Zm0\s+2a6\s+6\s+0\s+0\s+1\s+3.6\s+10.8l-\.2\.2-1.6\s+1.9V16H12a6\s+6\s+0\s+0\s+1\s+0-12Zm0\s+2.5A3\s+3\s+0\s+0\s+0\s+9\s+9.4h2A1\s+1\s+0\s+1\s+1\s+12\s+10.5c-1.2\s+0-2\s+1-2\s+2.1V14h2v-1.4c0-\.1\.1-\.1\.2-\.1A3\s+3\s+0\s+0\s+0\s+12\s+6.5Z"/>\s*</svg>',
    re.IGNORECASE | re.DOTALL
)
faq_replacement = """<svg viewBox="0 0 24 24" style="width: 25px; height: 25px; flex-shrink: 0;" fill="currentColor">
                        <path d="M11 18h2v-2h-2v2Zm1-16a8 8 0 1 0 0 16h1v4l4-4.7A8 8 0 0 0 12 2Zm0 2a6 6 0 0 1 3.6 10.8l-.2.2-1.6 1.9V16H12a6 6 0 0 1 0-12Zm0 2.5A3 3 0 0 0 9 9.4h2A1 1 0 1 1 12 10.5c-1.2 0-2 1-2 2.1V14h2v-1.4c0-.1.1-.1.2-.1A3 3 0 0 0 12 6.5Z"/>
                    </svg>"""

modified_files = []

# Gather target files exclusively under admin
target_files = []
for root, _, files in os.walk(ADMIN_DIR):
    for file in files:
        if file.endswith(".html"):
            target_files.append(os.path.join(root, file))

if os.path.exists(ADMIN_BASE_FILE):
    target_files.append(ADMIN_BASE_FILE)

# Apply replacement to target files
for filepath in target_files:
    with open(filepath, "r", encoding="utf-8") as f:
        content = f.read()
    
    new_content = inquiry_pattern.sub(inquiry_replacement, content)
    new_content = notice_pattern.sub(notice_replacement, new_content)
    new_content = faq_pattern.sub(faq_replacement, new_content)
    
    if new_content != content:
        with open(filepath, "w", encoding="utf-8") as f:
            f.write(new_content)
        modified_files.append(filepath)

print(f"Successfully modified {len(modified_files)} files:")
for f in modified_files:
    print(f"  - {f}")
