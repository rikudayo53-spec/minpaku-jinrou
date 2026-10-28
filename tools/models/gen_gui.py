"""民泊人狼のUI用の画像（9分割で伸ばせるパネルなど）を作る"""
import json, os, random, sys
from PIL import Image, ImageDraw
OUT = sys.argv[1] + "/assets/minpaku/textures/gui/sprites"
os.makedirs(OUT, exist_ok=True)
R = random.Random(9)

def save(name, img, border):
    img.save(f"{OUT}/{name}.png")
    json.dump({"gui": {"scaling": {"type": "nine_slice", "width": img.width, "height": img.height, "border": border}}},
              open(f"{OUT}/{name}.png.mcmeta", "w"))

def noise(img, amt, box=None):
    px = img.load()
    x0, y0, x1, y1 = box or (0, 0, img.width, img.height)
    for y in range(y0, y1):
        for x in range(x0, x1):
            r, g, b, a = px[x, y]
            if a == 0:
                continue
            d = R.randint(-amt, amt)
            px[x, y] = (max(0, min(255, r + d)), max(0, min(255, g + d)), max(0, min(255, b + d)), a)

def rounded(w, h, rad, fill, outline=None, width=0):
    img = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    d.rounded_rectangle([0, 0, w - 1, h - 1], radius=rad, fill=fill, outline=outline, width=width)
    return img

# 和紙のパネル（こげ茶の枠＋金の細線）
p = rounded(48, 48, 4, (59, 36, 22, 255))
d = ImageDraw.Draw(p)
d.rectangle([3, 3, 44, 44], fill=(244, 236, 216, 255))
noise(p, 6, (3, 3, 45, 45))
d.rectangle([4, 4, 43, 43], outline=(201, 161, 74, 255))
save("panel", p, 8)

# 木の盤
p = rounded(48, 48, 5, (34, 20, 11, 255))
d = ImageDraw.Draw(p)
d.rectangle([3, 3, 44, 44], fill=(92, 58, 34, 255))
for y in range(3, 45, 2):
    c = R.randint(-12, 12)
    d.line([3, y, 44, y], fill=(92 + c, 58 + c, 34 + c, 255))
d.rectangle([4, 4, 43, 43], outline=(201, 161, 74, 255))
save("board", p, 8)

# 半透明の札（HUD 用）
save("pill", rounded(24, 14, 6, (20, 12, 6, 150), (201, 161, 74, 150), 1), 6)
save("pill_dark", rounded(24, 14, 6, (0, 0, 0, 140)), 6)

# ボタン
def button(name, face, edge, text_shadow=(0, 0, 0, 0)):
    img = Image.new("RGBA", (24, 18), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    d.rounded_rectangle([0, 2, 23, 17], radius=3, fill=edge)
    d.rounded_rectangle([0, 0, 23, 15], radius=3, fill=edge)
    d.rounded_rectangle([2, 2, 21, 13], radius=2, fill=face)
    noise(img, 4, (2, 2, 22, 14))
    save(name, img, 5)
button("btn_red", (200, 55, 45, 255), (90, 20, 15, 255))
button("btn_red_hover", (226, 78, 66, 255), (90, 20, 15, 255))
button("btn_paper", (230, 220, 198, 255), (59, 36, 22, 255))
button("btn_paper_hover", (248, 240, 222, 255), (59, 36, 22, 255))
button("btn_off", (170, 160, 145, 255), (90, 80, 70, 255))
button("btn_dark", (59, 36, 22, 255), (25, 15, 8, 255))

# 名前のカード
def card(name, face, edge, width=2):
    img = rounded(24, 24, 3, edge)
    d = ImageDraw.Draw(img)
    d.rounded_rectangle([width, width, 23 - width, 23 - width], radius=2, fill=face)
    noise(img, 4, (width, width, 24 - width, 24 - width))
    save(name, img, 5)
card("card", (244, 236, 216, 255), (59, 36, 22, 255))
card("card_hover", (252, 244, 226, 255), (120, 80, 40, 255))
card("card_selected", (255, 226, 201, 255), (200, 55, 45, 255), 3)
card("card_dead", (189, 178, 160, 255), (110, 95, 80, 255))
card("card_win", (255, 244, 214, 255), (201, 161, 74, 255), 3)

# タブ
def tab(name, face, edge):
    img = Image.new("RGBA", (24, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    d.rounded_rectangle([0, 0, 23, 19], radius=4, fill=edge)
    d.rounded_rectangle([2, 2, 21, 19], radius=3, fill=face)
    save(name, img, 5)
tab("tab_on", (244, 236, 216, 255), (59, 36, 22, 255))
tab("tab_off", (201, 180, 142, 255), (59, 36, 22, 255))

# 入力欄・紙の箱
card("box", (255, 252, 244, 255), (59, 36, 22, 255))
card("box_dim", (240, 232, 214, 200), (120, 90, 60, 255), 1)
print("gui ok")
