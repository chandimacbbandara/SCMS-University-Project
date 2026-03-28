import re

with open('src/main/resources/templates/admin-community-chat.html', 'r', encoding='utf-8') as f:
    text = f.read()

text = re.sub(r'<script>\s*window\.studentTourConfig.*?</script>\s*<script src="/js/student-tour\.js"></script>', '', text, flags=re.DOTALL)

with open('src/main/resources/templates/admin-community-chat.html', 'w', encoding='utf-8') as f:
    f.write(text)
