import urllib.request
import json
import os
import subprocess

# Let's inspect icon_sb.png in the filesystem if present or check /tmp
for root, dirs, files in os.walk('.'):
    for f in files:
        if 'icon_sb' in f:
            print("Found in curdir:", os.path.join(root, f))
