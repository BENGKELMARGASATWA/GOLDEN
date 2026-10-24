import subprocess
import os

mvg_code = """
viewbox 0 0 512 512
push defs
push gradient 'pebbleGrad' linear 80,40 430,440
stop-color '#FFDA33' 0.0
stop-color '#EFBF04' 0.4
stop-color '#D69E00' 0.75
stop-color '#A87900' 1.0
pop gradient
push gradient 'wingGrad' linear 140,135 255,340
stop-color '#FFFFFF' 0.0
stop-color '#FFF7D6' 0.5
stop-color '#FFE680' 1.0
pop gradient
pop defs

push graphic-context
fill 'pebbleGrad'
stroke none
path 'M 180,55 C 225,56 275,72 335,102 C 395,132 455,182 482,216 C 502,242 505,268 488,294 C 462,334 408,382 350,418 C 292,454 225,460 168,440 C 115,420 58,370 30,310 C 6,260 8,212 32,164 C 58,112 118,54 180,55 Z'
pop graphic-context

push graphic-context
fill '#FFFFFF'
stroke none
path 'M 456,224 C 440,217 424,213 410,210 C 400,207 392,206 386,208 C 388,214 394,222 396,228 C 398,236 388,248 372,260 C 342,282 302,284 260,272 C 210,256 162,204 136,132 C 138,162 154,218 190,262 C 222,300 255,328 255,344 C 255,350 228,348 185,332 C 136,314 96,292 78,284 C 112,330 182,374 252,374 C 316,374 366,342 398,295 C 422,260 442,236 456,224 Z'
pop graphic-context

push graphic-context
fill 'wingGrad'
stroke none
path 'M 136,132 C 168,210 216,258 260,272 C 244,286 238,312 255,344 C 230,312 200,272 170,228 C 150,192 139,162 136,132 Z'
pop graphic-context
"""

with open('goldenbird.mvg', 'w') as f:
    f.write(mvg_code)

# 1. Render high-res base logo (1024x1024)
subprocess.run(['convert', '-size', '1024x1024', '-background', 'none', 'goldenbird.mvg', 'goldenbird_raw_1024.png'], check=True)

# 2. Generate goldenbird_logo_official.png (512x512)
subprocess.run(['convert', 'goldenbird_raw_1024.png', '-resize', '512x512', 'app/src/main/res/drawable/goldenbird_logo_official.png'], check=True)

# 3. Generate goldenbird_app_icon_official.png (512x512 with white rounded rect background and subtle gold accent)
app_icon_mvg = """
viewbox 0 0 512 512
push graphic-context
fill '#FFFFFF'
stroke '#EFBF04'
stroke-width 6
roundrectangle 16,16 496,496 110,110
pop graphic-context
"""
with open('app_icon_bg.mvg', 'w') as f:
    f.write(app_icon_mvg)

subprocess.run(['convert', '-size', '512x512', '-background', 'none', 'app_icon_bg.mvg', 'app_icon_bg.png'], check=True)
subprocess.run(['convert', 'goldenbird_raw_1024.png', '-resize', '380x380', 'goldenbird_inner.png'], check=True)
subprocess.run(['convert', 'app_icon_bg.png', 'goldenbird_inner.png', '-gravity', 'center', '-composite', 'app/src/main/res/drawable/goldenbird_app_icon_official.png'], check=True)

# 4. Generate Mipmap launcher icons
sizes = {
    'mdpi': 48,
    'hdpi': 72,
    'xhdpi': 96,
    'xxhdpi': 144,
    'xxxhdpi': 192
}

for density, px in sizes.items():
    corner_r = int(px * 0.22)
    # Standard adaptive square with rounded corners
    sq_mvg = f"""
viewbox 0 0 {px} {px}
push graphic-context
fill '#FFFFFF'
stroke none
roundrectangle 0,0 {px},{px} {corner_r},{corner_r}
pop graphic-context
"""
    with open('tmp_sq.mvg', 'w') as f:
        f.write(sq_mvg)
    subprocess.run(['convert', '-size', f'{px}x{px}', '-background', 'none', 'tmp_sq.mvg', 'tmp_sq.png'], check=True)
    
    # Inner logo scaled to 75%
    inner_px = int(px * 0.72)
    subprocess.run(['convert', 'goldenbird_raw_1024.png', '-resize', f'{inner_px}x{inner_px}', 'tmp_inner.png'], check=True)
    
    out_sq = f'app/src/main/res/mipmap-{density}/ic_launcher.png'
    subprocess.run(['convert', 'tmp_sq.png', 'tmp_inner.png', '-gravity', 'center', '-composite', out_sq], check=True)

    # Round circle
    half = px / 2
    r_val = (px / 2) - 0.5
    circle_mvg = f"""
viewbox 0 0 {px} {px}
push graphic-context
fill '#FFFFFF'
stroke none
circle {half},{half} {half},{r_val}
pop graphic-context
"""
    with open('tmp_circ.mvg', 'w') as f:
        f.write(circle_mvg)
    subprocess.run(['convert', '-size', f'{px}x{px}', '-background', 'none', 'tmp_circ.mvg', 'tmp_circ.png'], check=True)
    
    out_circ = f'app/src/main/res/mipmap-{density}/ic_launcher_round.png'
    subprocess.run(['convert', 'tmp_circ.png', 'tmp_inner.png', '-gravity', 'center', '-composite', out_circ], check=True)
    print(f"Generated {density} ({px}px)")

print("Golden Bird asset generation completed successfully!")
