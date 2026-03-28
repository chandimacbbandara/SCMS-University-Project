import re

with open('src/main/resources/templates/admin-dashboard.html', 'r', encoding='utf-8') as f:
    admin_dash = f.read()

with open('src/main/resources/templates/admin-community-chat.html', 'r', encoding='utf-8') as f:
    student_comm = f.read()

header_match = re.search(r'<nav class="navbar">.*?</nav>', admin_dash, re.DOTALL)
if not header_match:
    print("Could not find admin nav")
    exit(1)
admin_header = header_match.group(0)

# Replace active class in admin nav
admin_header = admin_header.replace('href="/admin/dashboard" class="active"', 'href="/admin/dashboard"')
admin_header = admin_header.replace('href="/admin/community"', 'href="/admin/community" class="active"')

# Replace student header (which is inside <header>) with admin nav
new_html = re.sub(r'<header>.*?</header>', admin_header, student_comm, flags=re.DOTALL)

# Delete the create post form
new_html = re.sub(r'<form class="compose-form".*?</form>', '', new_html, flags=re.DOTALL)

# Change URL paths for posts/replies to admin paths
new_html = re.sub(r'/student/community/posts/', '/admin/community/post/', new_html)
new_html = re.sub(r'/student/community/replies/', '/admin/community/reply/', new_html)

new_html = new_html.replace('Add your solution or guidance...', 'Reply as admin...')

# Update post delete buttons
post_delete_btn = '''
                        <div class="post-actions" style="margin-top:10px;">
                            <form th:action="@{'/admin/community/post/' + ${post.postId} + '/delete'}" method="post" style="display:inline;" onsubmit="return confirm('Delete this post as admin?');">
                                <button type="submit" class="danger-btn" style="background:#e11d48;color:white;border:none;padding:5px 10px;border-radius:4px;cursor:pointer;">Delete Post</button>
                            </form>
                        </div>
'''
new_html = re.sub(r'<div class="post-actions".*?</div>', post_delete_btn, new_html, flags=re.DOTALL)

reply_delete_btn = '''
                            <div class="reply-actions" style="margin-top:5px;text-align:right;">
                                <form th:action="@{'/admin/community/reply/' + ${reply.replyId} + '/delete'}" method="post" style="display:inline;" onsubmit="return confirm('Delete this reply as admin?');">
                                    <button type="submit" class="danger-btn" style="background:#e11d48;color:white;border:none;padding:3px 8px;border-radius:4px;cursor:pointer;font-size:12px;">Delete Reply</button>
                                </form>
                            </div>
'''
new_html = re.sub(r'<div class="reply-actions".*?</div>', reply_delete_btn, new_html, flags=re.DOTALL)

# Hide student specific sections like "You", etc.
new_html = re.sub(r'<div class="current-user".*?</div>.*?</div></div>', '', new_html, flags=re.DOTALL)


with open('src/main/resources/templates/admin-community-chat.html', 'w', encoding='utf-8') as f:
    f.write(new_html)

print("Done")
