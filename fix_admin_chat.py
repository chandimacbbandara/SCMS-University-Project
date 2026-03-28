import re

with open('src/main/resources/templates/admin-community-chat.html', 'r', encoding='utf-8') as f:
    text = f.read()

# Fix reply structure to handle null student
# We will use sed-like logic.

text = text.replace('''th:src="@{'/student/photo/' + ${reply.student.userId}}"''', 
                    '''th:src="${reply.student != null} ? @{'/student/photo/' + ${reply.student.userId}} : '/images/img1.jpeg'"''')

text = text.replace('''th:text="${#strings.substring(reply.student.user.firstName, 0, 1)}"''', 
                    '''th:text="${reply.student != null} ? ${#strings.substring(reply.student.user.firstName, 0, 1)} : 'A'"''')

text = text.replace('''<span th:text="${reply.student.user.firstName + ' ' + reply.student.user.lastName}">Student</span>''', 
                    '''<span th:text="${reply.student != null} ? ${reply.student.user.firstName + ' ' + reply.student.user.lastName} : ${reply.adminName + ' (Admin)'}">Student</span>''')


with open('src/main/resources/templates/admin-community-chat.html', 'w', encoding='utf-8') as f:
    f.write(text)

print("Fixed")
