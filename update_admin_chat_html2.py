import re

with open('src/main/resources/templates/admin-community-chat.html', 'r', encoding='utf-8') as f:
    text = f.read()

# Match the body
text = text.replace('<body class="community-body admin-theme">', '<body>')

# Add missing link from admin dashboard if any, maybe admin-tour.css
text = text.replace('href="/CSS/student-community.css">', 'href="/CSS/student-community.css">\n    <link rel="stylesheet" href="/CSS/admin-tour.css">')

with open('src/main/resources/templates/admin-community-chat.html', 'w', encoding='utf-8') as f:
    f.write(text)

