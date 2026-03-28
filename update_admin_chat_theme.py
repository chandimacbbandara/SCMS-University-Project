import re

with open('src/main/resources/templates/admin-community-chat.html', 'r', encoding='utf-8') as f:
    text = f.read()

# Remove the student-theme class from body
text = text.replace('<body class="community-body student-theme">', '<body class="community-body admin-theme">')

# Modify the profile pictures to correctly use student IDs or fallbacks
# The pictures use '/student/photo/{id}' logic. Sometimes we might want to check the profile URL syntax in admin pages.
# We'll make sure it's using the correct syntax.
# Looking closely at the image tags:
text = re.sub(
    r"""th:src="\$\{reply\.student != null\} \? @\{'/student/photo/' \+ \$\{reply\.student\.userId\}\} : '/images/img1\.jpeg'"""",
    r"""th:src="${reply.student != null} ? @{'/student/photo/' + ${reply.student.userId}} : '/images/img1.jpeg'"""",
    text
)

# And modifying any generic buttons to red admin style if not already
text = text.replace('class="btn-red"', 'class="nav-logout"')

with open('src/main/resources/templates/admin-community-chat.html', 'w', encoding='utf-8') as f:
    f.write(text)

print("Updated theme and logout buttons")
