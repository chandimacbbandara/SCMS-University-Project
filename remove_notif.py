import re

filepath = "/home/chandima-bandara/Desktop/SCMS-University-Project/src/main/resources/templates/admin-community-chat.html"
with open(filepath, "r") as f:
    content = f.read()

pattern = re.compile(r'<div class="notif-overlay".*?</script>', re.DOTALL)
new_content = pattern.sub('', content)

with open(filepath, "w") as f:
    f.write(new_content)

print("Done")
