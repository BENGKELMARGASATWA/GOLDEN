import subprocess
import os

# We test convert with -size 1024x1024 xc:none -draw "..."
cmd = [
    'convert', '-size', '1024x1024', 'xc:none',
    '-fill', '#9A9A9A',
    '-draw', "path 'M 360,110 C 450,112 550,144 670,204 C 790,264 910,364 964,432 C 1004,484 1010,536 976,588 C 924,668 816,764 700,836 C 584,908 450,920 336,880 C 230,840 116,740 60,620 C 12,520 16,424 64,328 C 116,224 236,108 360,110 Z'",
    '-fill', '#FFFFFF',
    '-draw', "path 'M 912,448 C 880,434 848,426 820,420 C 800,414 784,412 772,416 C 776,428 788,444 792,456 C 796,472 776,496 744,520 C 684,564 604,568 520,544 C 420,512 324,408 272,264 C 276,324 308,436 380,524 C 444,600 510,656 510,688 C 510,700 456,696 370,664 C 272,628 192,584 156,568 C 224,660 364,748 504,748 C 632,748 732,684 796,590 C 844,520 884,472 912,448 Z'",
    '-fill', '#DCDCDC',
    '-draw', "path 'M 272,264 C 336,420 432,516 520,544 C 488,572 476,624 510,688 C 460,624 400,544 340,456 C 300,384 278,324 272,264 Z'",
    '-resize', '512x512',
    'silverbird_perfect.png'
]

res = subprocess.run(cmd, capture_output=True, text=True)
print("Exit code:", res.returncode)
print("Stdout:", res.stdout)
print("Stderr:", res.stderr)
if os.path.exists('silverbird_perfect.png'):
    print("File created successfully:", subprocess.check_output(['identify', 'silverbird_perfect.png']).decode().strip())
