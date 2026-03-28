import re

filepath = "/home/chandima-bandara/Desktop/SCMS-University-Project/src/main/resources/templates/admin-community-chat.html"
with open(filepath, "r") as f:
    content = f.read()

tour_script = """
<script>
    window.adminTourConfig = {
        pageId: 'community-chat',
        autoStart: false,
        storageKey: 'akb_admin_tour_completed',
        steps: [
            {
                selector: '.nav-links',
                title: 'Community Navigation',
                content: 'Use this menu to return to Dashboard, Education Dashboard, or other admin areas.'
            },
            {
                selector: '.community-hero',
                title: 'Student Community',
                content: 'Welcome to the Student Community. Here you can observe public discussions among students.'
            },
            {
                selector: '.feed-stats',
                title: 'Community Activity',
                content: 'See the total number of active posts and discussions happening right now.'
            },
            {
                selector: '.feed-stream',
                title: 'Moderate Posts',
                content: 'Review student posts. You can delete inappropriate posts or provide official replies as an Admin.'
            }
        ]
    };
</script>
<script src="/js/admin-tour.js"></script>
"""

content = content.replace('<script src="/js/student-community.js"></script>', tour_script + '\n<script src="/js/student-community.js"></script>')

with open(filepath, "w") as f:
    f.write(content)

print("Added tour to admin-community-chat.html")
