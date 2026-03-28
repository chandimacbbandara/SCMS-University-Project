import re

with open('src/main/resources/templates/admin-community-chat.html', 'r', encoding='utf-8') as f:
    text = f.read()

text = re.sub(r'<h3 class="side-heading"><i class="fa-solid fa-user"></i> You</h3>\s*<div class="current-user">.*?</div>\s*</div>\s*</div>', '', text, flags=re.DOTALL)
text = re.sub(r'<hr class="side-separator">\s*<h3 class="side-heading"><i class="fa-solid fa-user"></i> You</h3>\s*<div class="current-user">.*?</div>\s*</div>\s*</div>', '', text, flags=re.DOTALL)

with open('src/main/resources/templates/admin-community-chat.html', 'w', encoding='utf-8') as f:
    f.write(text)

