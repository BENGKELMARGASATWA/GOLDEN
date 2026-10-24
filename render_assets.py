import subprocess
import os

# Convert SVG directly using ImageMagick
# We test if ImageMagick can render the SVG file
res = subprocess.run(['convert', '-background', 'none', 'silverbird_base.svg', '-resize', '512x512', 'test_render.png'], capture_output=True, text=True)
print("Return code:", res.returncode)
print("Stdout:", res.stdout)
print("Stderr:", res.stderr)

if res.returncode == 0:
    print("Direct SVG render succeeded!")
else:
    print("Will use fallback drawing commands")
