import re

with open('src/main/resources/templates/admin-dashboard.html', 'r', encoding='utf-8') as f:
    admin_dash = f.read()

with open('src/main/resources/templates/admin-community-chat.html', 'r', encoding='utf-8') as f:
    admin_comm = f.read()

# Extract <style>...</style> from admin_dash
style_match = re.search(r'<style>.*?</style>', admin_dash, re.DOTALL)
if style_match:
    style_str = style_match.group(0)
    # Insert just before </head>
    admin_comm = admin_comm.replace('</head>', style_str + '\n</head>')

with open('src/main/resources/templates/admin-community-chat.html', 'w', encoding='utf-8') as f:
    f.write(admin_comm)

