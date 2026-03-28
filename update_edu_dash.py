import sys

filepath = "/home/chandima-bandara/Desktop/SCMS-University-Project/src/main/resources/templates/admin-edu-dashboard.html"

with open(filepath, "r") as f:
    content = f.read()

# Replace storageKey: 'akb_admin_tour_completed', with storageKey: 'akb_admin_tour_completed',\n            nextPage: '/admin/community',
content = content.replace("storageKey: 'akb_admin_tour_completed',", "storageKey: 'akb_admin_tour_completed',\n            nextPage: '/admin/community',")

with open(filepath, "w") as f:
    f.write(content)

print("Updated admin-edu-dashboard.html")
