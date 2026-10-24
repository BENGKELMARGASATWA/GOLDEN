import subprocess
import os

# We generate:
# 1. Drawable image: app/src/main/res/drawable/silverbird_logo_official.png (512x512 transparent background)
# 2. App Icon: app/src/main/res/drawable/silverbird_app_icon_official.png (512x512 with white circular or rounded background)
# 3. Mipmap standard icons:
#    - mdpi: 48x48
#    - hdpi: 72x72
#    - xhdpi: 96x96
#    - xxhdpi: 144x144
#    - xxxhdpi: 192x192
#    Both ic_launcher.png (adaptive round rect) and ic_launcher_round.png (circle)

# Step 1: Generate high-res base logo (1024x1024)
subprocess.run([
    'convert', '-size', '1024x1024', 'xc:none',
    '-fill', '#949494',
    '-draw', "path 'M 360,110 C 450,112 550,144 670,204 C 790,264 910,364 964,432 C 1004,484 1010,536 976,588 C 924,668 816,764 700,836 C 584,908 450,920 336,880 C 230,840 116,740 60,620 C 12,520 16,424 64,328 C 116,224 236,108 360,110 Z'",
    '-fill', '#FFFFFF',
    '-draw', "path 'M 912,448 C 880,434 848,426 820,420 C 800,414 784,412 772,416 C 776,428 788,444 792,456 C 796,472 776,496 744,520 C 684,564 604,568 520,544 C 420,512 324,408 272,264 C 276,324 308,436 380,524 C 444,600 510,656 510,688 C 510,700 456,696 370,664 C 272,628 192,584 156,568 C 224,660 364,748 504,748 C 632,748 732,684 796,590 C 844,520 884,472 912,448 Z'",
    '-fill', '#D4D4D4',
    '-draw', "path 'M 272,264 C 336,420 432,516 520,544 C 488,572 476,624 510,688 C 460,624 400,544 340,456 C 300,384 278,324 272,264 Z'",
    'silverbird_raw_1024.png'
], check=True)

# Generate silverbird_logo_official.png (512x512)
subprocess.run(['convert', 'silverbird_raw_1024.png', '-resize', '512x512', 'app/src/main/res/drawable/silverbird_logo_official.png'], check=True)

# Generate silverbird_app_icon_official.png (512x512 with subtle white background badge)
subprocess.run([
    'convert', '-size', '512x512', 'xc:#FFFFFF',
    '(', 'silverbird_raw_1024.png', '-resize', '420x420', ')',
    '-gravity', 'center',
    '-composite',
    'app/src/main/res/drawable/silverbird_app_icon_official.png'
], check=True)

# Generate mipmaps for all densities
densities = {
    'mdpi': 48,
    'hdpi': 72,
    'xhdpi': 96,
    'xxhdpi': 144,
    'xxxhdpi': 192
}

for density, size in densities.items():
    icon_inner = int(size * 0.82)
    radius = int(size * 0.22)
    dir_path = f'app/src/main/res/mipmap-{density}'
    os.makedirs(dir_path, exist_ok=True)
    
    # 1. ic_launcher.png (rounded square)
    # Create rounded rect mask
    subprocess.run([
        'convert', '-size', f'{size}x{size}', 'xc:none',
        '-fill', '#FFFFFF',
        '-draw', f'roundrectangle 0,0,{size-1},{size-1},{radius},{radius}',
        '(', 'silverbird_raw_1024.png', '-resize', f'{icon_inner}x{icon_inner}', ')',
        '-gravity', 'center',
        '-composite',
        f'{dir_path}/ic_launcher.png'
    ], check=True)
    
    # 2. ic_launcher_round.png (circle)
    center = size // 2
    r_circle = size // 2
    subprocess.run([
        'convert', '-size', f'{size}x{size}', 'xc:none',
        '-fill', '#FFFFFF',
        '-draw', f'circle {center},{center} {center},{size-1}',
        '(', 'silverbird_raw_1024.png', '-resize', f'{icon_inner}x{icon_inner}', ')',
        '-gravity', 'center',
        '-composite',
        f'{dir_path}/ic_launcher_round.png'
    ], check=True)
    
    print(f"Generated {density} ({size}x{size})")

print("All icons successfully generated!")
