import re

with open('src/main/resources/templates/admin-community-chat.html', 'r', encoding='utf-8') as f:
    text = f.read()

# Replace student CSS links with admin font.
text = text.replace('<link rel="stylesheet" href="/CSS/main.css">', '')
text = text.replace('<link rel="stylesheet" href="/CSS/student-theme.css">', '')
text = text.replace('<link rel="stylesheet" href="/CSS/student-tour.css">', '')

added_font = '<link href="https://fonts.googleapis.com/css2?family=Inter:wght@300;400;500;600;700;800;900&display=swap" rel="stylesheet">\n'
text = text.replace('<link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.5.0/css/all.min.css">', 
                    '<link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.5.0/css/all.min.css">\n    ' + added_font)


with open('src/main/resources/templates/admin-community-chat.html', 'w', encoding='utf-8') as f:
    f.write(text)

print("Updated links")
