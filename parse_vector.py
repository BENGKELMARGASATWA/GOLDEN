import subprocess
import math

# Let's inspect mipmap ic_launcher images
for res in ['mdpi', 'hdpi', 'xhdpi', 'xxhdpi', 'xxxhdpi']:
    path = f'app/src/main/res/mipmap-{res}/ic_launcher.png'
    print(res, subprocess.check_output(['identify', path]).decode().strip())
