import re

with open('src/main/resources/templates/admin-community-chat.html', 'r', encoding='utf-8') as f:
    text = f.read()

# Fix image fallback mappings
text = text.replace("th:src=\"${reply.student != null} ? @{'/student/photo/' + ${reply.student.userId}} : '/images/img1.jpeg'\"", 
                    "th:src=\"${reply.student != null ? '/student/photo/' + reply.student.userId : '/images/img1.jpeg'}\"")

text = text.replace("th:src=\"@{'/student/photo/' + ${post.student.userId}}\"",
                    "th:src=\"${'/student/photo/' + post.student.userId}\"")

# Make sure buttons look like admin style
text = text.replace('<button type="submit" class="secondary-submit">', '<button type="submit" class="btn-apply" style="border:none; cursor:pointer;">')


with open('src/main/resources/templates/admin-community-chat.html', 'w', encoding='utf-8') as f:
    f.write(text)

