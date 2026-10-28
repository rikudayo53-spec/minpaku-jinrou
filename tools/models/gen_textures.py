"""民泊人狼のオリジナルテクスチャ（16x16 / 32x32）を作る"""
import os, sys, random
from PIL import Image, ImageDraw

OUT = sys.argv[1]
R = random.Random(3)


def save(img, path):
    full = os.path.join(OUT, path)
    os.makedirs(os.path.dirname(full), exist_ok=True)
    img.save(full)


def noise(img, amt=10):
    px = img.load()
    for y in range(img.height):
        for x in range(img.width):
            r, g, b, a = px[x, y]
            d = R.randint(-amt, amt)
            px[x, y] = (max(0, min(255, r + d)), max(0, min(255, g + d)), max(0, min(255, b + d)), a)
    return img


# 配電盤の正面：ブレーカーのつまみが並ぶ
img = Image.new("RGBA", (16, 16), (150, 155, 160, 255))
d = ImageDraw.Draw(img)
d.rectangle([0, 0, 15, 15], outline=(95, 100, 105, 255))
d.rectangle([2, 2, 13, 4], fill=(60, 60, 64, 255))
d.text((3, 1), "", fill=(255, 255, 255, 255))
for i in range(5):
    x = 2 + i * 2 + (1 if i > 2 else 0)
    d.rectangle([x, 6, x + 1, 12], fill=(45, 45, 48, 255))
    on = i % 2 == 0
    d.rectangle([x, 6 if on else 10, x + 1, 8 if on else 12], fill=(230, 230, 230, 255))
d.rectangle([12, 2, 13, 3], fill=(230, 60, 50, 255))
save(noise(img, 6), "assets/minpaku/textures/block/breaker_face.png")

# 配線盤：色とりどりの配線
img = Image.new("RGBA", (16, 16), (60, 45, 35, 255))
d = ImageDraw.Draw(img)
cols = [(229, 57, 53), (30, 136, 229), (253, 216, 53), (216, 27, 96)]
for i, c in enumerate(cols):
    y = 2 + i * 3 + 1
    d.line([1, y, 14, y], fill=c + (255,))
    d.point([1, y - 1], fill=(200, 200, 200, 255))
    d.point([14, y - 1], fill=(200, 200, 200, 255))
d.rectangle([0, 0, 15, 15], outline=(40, 28, 20, 255))
save(noise(img, 5), "assets/minpaku/textures/block/wiring_face.png")

# 台帳のページ（罫線）
img = Image.new("RGBA", (16, 16), (245, 238, 220, 255))
d = ImageDraw.Draw(img)
for y in range(3, 15, 2):
    d.line([1, y, 14, y], fill=(170, 160, 150, 255))
d.line([3, 1, 3, 14], fill=(220, 120, 120, 255))
for y in range(3, 15, 2):
    w = R.randint(5, 10)
    d.line([5, y - 1, 5 + w, y - 1], fill=(70, 70, 90, 255))
save(img, "assets/minpaku/textures/block/ledger_page.png")

# 布団の柄（紺と白の格子）
img = Image.new("RGBA", (16, 16), (235, 235, 240, 255))
d = ImageDraw.Draw(img)
for i in range(0, 16, 4):
    d.line([i, 0, i, 15], fill=(40, 60, 120, 255))
    d.line([0, i, 15, i], fill=(40, 60, 120, 255))
for x in range(2, 16, 4):
    for y in range(2, 16, 4):
        d.point([x, y], fill=(200, 60, 60, 255))
save(noise(img, 4), "assets/minpaku/textures/block/futon_top.png")

# 玄関マット
img = Image.new("RGBA", (16, 16), (150, 110, 60, 255))
d = ImageDraw.Draw(img)
d.rectangle([0, 0, 15, 15], outline=(90, 60, 30, 255))
d.rectangle([2, 2, 13, 13], outline=(120, 30, 30, 255))
# 「ようこそ」っぽい模様
for x, y in [(5, 6), (6, 6), (7, 6), (9, 6), (10, 6), (5, 9), (7, 9), (8, 9), (10, 9)]:
    d.point([x, y], fill=(240, 230, 200, 255))
save(noise(img, 12), "assets/minpaku/textures/block/entrance_mat.png")

# 床下点検口（木のふたと金属のふち）
img = Image.new("RGBA", (16, 16), (160, 120, 75, 255))
d = ImageDraw.Draw(img)
for y in range(0, 16, 4):
    d.line([0, y, 15, y], fill=(120, 85, 50, 255))
d.rectangle([0, 0, 15, 15], outline=(150, 150, 155, 255))
d.rectangle([1, 1, 14, 14], outline=(110, 110, 115, 255))
d.rectangle([6, 7, 9, 8], fill=(60, 60, 60, 255))
save(noise(img, 8), "assets/minpaku/textures/block/floor_hatch.png")

# ほこり
img = Image.new("RGBA", (16, 16), (150, 145, 140, 255))
save(noise(img, 25), "assets/minpaku/textures/block/dust.png")

# 畑の土
img = Image.new("RGBA", (16, 16), (95, 65, 40, 255))
d = ImageDraw.Draw(img)
for y in range(1, 16, 5):
    d.line([0, y, 15, y], fill=(70, 45, 25, 255))
save(noise(img, 14), "assets/minpaku/textures/block/soil.png")

# メーター（ボイラーの圧力計）
img = Image.new("RGBA", (16, 16), (240, 240, 235, 255))
d = ImageDraw.Draw(img)
d.ellipse([1, 1, 14, 14], outline=(40, 40, 40, 255))
d.line([8, 8, 12, 4], fill=(220, 40, 40, 255))
d.arc([3, 3, 12, 12], 200, 340, fill=(220, 80, 40, 255))
save(img, "assets/minpaku/textures/block/gauge.png")

# 炎ののぞき窓
img = Image.new("RGBA", (16, 16), (40, 20, 10, 255))
d = ImageDraw.Draw(img)
for x in range(16):
    h = R.randint(5, 12)
    d.line([x, 15, x, 15 - h], fill=(255, 120, 20, 255))
    d.line([x, 15, x, 15 - h // 2], fill=(255, 220, 80, 255))
save(img, "assets/minpaku/textures/block/fire_window.png")

# お札（妨害）：白い紙に赤い文様
img = Image.new("RGBA", (16, 16), (240, 232, 205, 255))
d = ImageDraw.Draw(img)
d.rectangle([0, 0, 15, 15], outline=(200, 40, 40, 255))
d.line([8, 2, 8, 13], fill=(180, 20, 20, 255))
d.line([5, 4, 11, 4], fill=(180, 20, 20, 255))
d.line([4, 7, 12, 7], fill=(180, 20, 20, 255))
d.line([5, 10, 11, 10], fill=(180, 20, 20, 255))
d.ellipse([6, 11, 10, 14], outline=(30, 30, 30, 255))
save(img, "assets/minpaku/textures/item/talisman_paper.png")

# お守りの布（赤地に金の模様）
img = Image.new("RGBA", (16, 16), (190, 30, 40, 255))
d = ImageDraw.Draw(img)
for y in range(0, 16, 4):
    for x in range((y // 4) % 2 * 2, 16, 4):
        d.point([x, y], fill=(240, 200, 80, 255))
d.rectangle([5, 4, 10, 11], fill=(245, 235, 210, 255))
d.line([7, 5, 7, 10], fill=(40, 40, 40, 255))
d.line([8, 5, 8, 10], fill=(40, 40, 40, 255))
save(img, "assets/minpaku/textures/item/charm_cloth.png")
print("textures ok")

# 障子（白い紙と木の桟）
img = Image.new("RGBA", (16, 16), (246, 242, 230, 255))
d = ImageDraw.Draw(img)
for x in (0, 5, 10, 15):
    d.line([x, 0, x, 15], fill=(150, 110, 70, 255))
for y in (0, 4, 8, 12, 15):
    d.line([0, y, 15, y], fill=(150, 110, 70, 255))
d.ellipse([6, 5, 8, 7], fill=(70, 50, 35, 255))
save(img, "assets/minpaku/textures/block/shoji.png")

# 手ぬぐい（藍染め）
img = Image.new("RGBA", (16, 16), (40, 70, 140, 255))
d = ImageDraw.Draw(img)
for y in range(1, 16, 3):
    d.line([0, y, 15, y], fill=(230, 235, 245, 255))
save(noise(img, 6), "assets/minpaku/textures/block/towel.png")
print("textures2 ok")
