import os
import re

admin_dir = r"C:\workspace\dogo\src\main\resources\templates\admin"

svg_inquiry = """<svg viewBox="0 0 24 24" class="w-5 h-5" fill="currentColor">
                        <path d="M4 4h16v12H7l-3 4V4Zm2 2v8h10v-2H8v-2h8V8H8V6H6Z"/>
                    </svg>"""

svg_notice = """<svg viewBox="0 0 24 24" class="w-5 h-5" fill="currentColor">
                        <path d="M4 10v4h3l5 4V6l-5 4H4Zm11.5 2a3.5 3.5 0 0 0-1.5-2.9v5.8a3.5 3.5 0 0 0 1.5-2.9ZM16 4.8v2.4a6 6 0 0 1 0 9.6v2.4a8 8 0 0 0 0-14.4Z"/>
                    </svg>"""

svg_faq = """<svg viewBox="0 0 24 24" class="w-5 h-5" fill="currentColor">
                        <path d="M11 18h2v-2h-2v2Zm1-16a8 8 0 1 0 0 16h1v4l4-4.7A8 8 0 0 0 12 2Zm0 2a6 6 0 0 1 3.6 10.8l-.2.2-1.6 1.9V16H12a6 6 0 0 1 0-12Zm0 2.5A3 3 0 0 0 9 9.4h2A1 1 0 1 1 12 10.5c-1.2 0-2 1-2 2.1V14h2v-1.4c0-.1.1-.1.2-.1A3 3 0 0 0 12 6.5Z"/>
                    </svg>"""

# Regular expressions to find the target <a> tags and replace their inner <i> tag
# Example:
# <a href="/admin/inquiries" class="...">
#     <i data-lucide="message-square" class="w-5 h-5"></i>
#     1:1 문의
# </a>

pattern_inquiries = re.compile(
    r'(<a\s+href="/admin/inquiries"[^>]*>\s*)<i\s+data-lucide="message-square"[^>]*></i>',
    re.IGNORECASE | re.DOTALL
)
pattern_notice = re.compile(
    r'(<a\s+href="/admin/notice"[^>]*>\s*)<i\s+data-lucide="message-square"[^>]*></i>',
    re.IGNORECASE | re.DOTALL
)
pattern_faq = re.compile(
    r'(<a\s+href="/admin/faq"[^>]*>\s*)<i\s+data-lucide="message-square"[^>]*></i>',
    re.IGNORECASE | re.DOTALL
)

count = 0
for root, dirs, files in os.walk(admin_dir):
    for file in files:
        if file.endswith('.html'):
            filepath = os.path.join(root, file)
            with open(filepath, 'r', encoding='utf-8') as f:
                content = f.read()
            
            new_content = content
            # Replace inquiries
            new_content, n_inq = pattern_inquiries.subn(rf'\1{svg_inquiry}', new_content)
            # Replace notice
            new_content, n_not = pattern_notice.subn(rf'\1{svg_notice}', new_content)
            # Replace faq
            new_content, n_faq = pattern_faq.subn(rf'\1{svg_faq}', new_content)
            
            if n_inq > 0 or n_not > 0 or n_faq > 0:
                with open(filepath, 'w', encoding='utf-8') as f:
                    f.write(new_content)
                print(f"Updated: {filepath} (inquiries: {n_inq}, notice: {n_not}, faq: {n_faq})")
                count += 1

print(f"Total updated files: {count}")
