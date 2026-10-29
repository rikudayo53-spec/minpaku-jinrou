"""
能力アイテム（役職の道具）8種の高精細モデル。Blockbench で開ける .bbmodel に書き出す。
座標はピクセル。左右・前後の中心が 0、底が y=0（Blender で作っていたときと同じ置き方）。正面は北（-Z）。
"""
import math
import os

from PIL import ImageFont

from hires import HiRes, MATS, Mat

FONT = os.path.join(os.path.dirname(__file__), "..", "..", "src", "main", "resources", "assets", "minpaku", "font",
                    "ui_serif.ttf")

MATS.update({
    "gold_i": Mat((226, 178, 64), "metal", noise=6),
    "gold_dark_i": Mat((170, 122, 36), "metal", noise=6),
    "iron_i": Mat((92, 94, 100), "metal", noise=6),
    "copper_i": Mat((184, 108, 66), "metal", noise=8),
    "brass_i": Mat((214, 168, 76), "metal", noise=5),
    "wood_i": Mat((176, 132, 84), "wood", accent=(140, 100, 60), noise=5),
    "wood_dark_i": Mat((96, 64, 40), "wood", accent=(70, 46, 28), noise=5),
    "bamboo_skin": Mat((200, 176, 110), "bamboo", accent=(166, 140, 80), noise=5),
    "washi_i": Mat((244, 236, 214), "paper", noise=4),
    "red_i": Mat((186, 36, 32), "plain", noise=5),
    "red_silk": Mat((196, 44, 40), "stipple", noise=6),
    "white_cord": Mat((240, 236, 228), "rope", accent=(214, 208, 196)),
    "red_cord": Mat((196, 40, 36), "rope", accent=(150, 26, 24)),
    "purple_i": Mat((96, 48, 128), "stipple", noise=6),
    "glass_blue": Mat((170, 214, 255), "water", accent=(220, 240, 255), alpha=120),
    "glass_lens": Mat((200, 230, 255), "water", accent=(240, 250, 255), alpha=90),
    "glow_core": Mat((200, 120, 255), "fire", accent=(250, 220, 255), glow=True),
    "rice_i": Mat((248, 246, 238), "stipple", noise=8),
    "nori": Mat((28, 40, 30), "stipple", noise=5),
    "black_i": Mat((26, 24, 26), "plain", noise=3),
})


def I(name, d=24):
    return HiRes(name, density=d, real=True)


def ring(m, name, cx, cy, cz, r, thick, depth, mat, seg=20, plane="xy", group=None):
    """輪（短い板を円に並べる）。plane = "xy"（正面から見て丸）/ "xz"（上から見て丸）"""
    ln = 2 * (r + thick / 2) * math.tan(math.pi / seg) + thick * 0.15
    for k in range(seg):
        a = 2 * math.pi * k / seg
        depth_k = depth + (0.06 if k % 2 else 0.0)     # となりと面が重なってちらつかないよう、少し厚みを変える
        if plane == "xy":
            x, y = cx + r * math.cos(a), cy + r * math.sin(a)
            m.cube(name, (x - thick / 2, y - ln / 2, cz - depth_k / 2), (x + thick / 2, y + ln / 2, cz + depth_k / 2), mat,
                   rot=[0, 0, math.degrees(a)], pivot=[x, y, cz], group=group)
        else:
            x, z = cx + r * math.cos(a), cz + r * math.sin(a)
            m.cube(name, (x - thick / 2, cy - depth_k / 2, z - ln / 2), (x + thick / 2, cy + depth_k / 2, z + ln / 2), mat,
                   rot=[0, -math.degrees(a), 0], pivot=[x, cy, z], group=group)


def text_decal(chars, color=(70, 44, 24, 255), faces=("north", "south"), size=0.42, vertical=True):
    """面に文字を書く（縦書き）"""
    def decal(draw, face, x0, y0, w, h):
        if face not in faces:
            return
        fs = max(6, int(min(w, h / max(1, len(chars))) * size * 2))
        font = ImageFont.truetype(FONT, fs)
        n = len(chars)
        for k, ch in enumerate(chars):
            bx = draw.textbbox((0, 0), ch, font=font)
            tw, th = bx[2] - bx[0], bx[3] - bx[1]
            cx = x0 + w / 2
            cy = y0 + h * (k + 0.5) / n if vertical else y0 + h / 2
            draw.text((cx - tw / 2 - bx[0], cy - th / 2 - bx[1]), ch, font=font, fill=color)
    return decal


# ---------------------------------------------------------------- 管理人の鍵（名札「民泊」）
def manager_key():
    m = I("manager_key")
    ring(m, "bow", 0, 12.5, 0, 2.0, 0.9, 0.9, "gold_i", seg=24)
    m.cube("collar", (-0.9, 9.6, -0.55), (0.9, 10.3, 0.55), "gold_dark_i")
    m.cube("shaft", (-0.5, 2.4, -0.45), (0.5, 10.4, 0.45), "gold_i")
    m.cube("tip", (-0.6, 2.0, -0.5), (0.6, 2.6, 0.5), "gold_dark_i")
    m.cube("bit1", (0.5, 2.6, -0.45), (2.2, 3.6, 0.45), "gold_i")
    m.cube("bit2", (0.5, 4.2, -0.45), (1.7, 5.0, 0.45), "gold_i")
    m.cube("notch", (1.5, 2.6, -0.46), (1.8, 3.2, 0.46), "gold_dark_i")
    # 赤いひもと木の名札
    m.cube("cord", (-2.4, 12.6, -0.15), (-1.6, 14.2, 0.15), "red_cord", rot=[0, 0, 35], pivot=[-2, 13.4, 0])
    m.cube("tag", (-4.9, 7.4, -0.35), (-2.1, 12.4, 0.35), "wood_i", rot=[0, 0, -8], pivot=[-3.5, 12.4, 0],
           decal=text_decal("民泊"))
    m.cube("tag_hole", (-3.8, 11.7, -0.4), (-3.2, 12.2, 0.4), "black_i", rot=[0, 0, -8], pivot=[-3.5, 12.4, 0])
    return m


# ---------------------------------------------------------------- 占い師の水晶玉
def seer_crystal():
    m = I("seer_crystal")
    m.cyl("base", 0, 0, 0, 0.8, 3.8, "gold_i", sides=12)
    m.cyl("base2", 0, 0, 0.8, 1.6, 3.2, "gold_dark_i", sides=12)
    m.cyl("stem", 0, 0, 1.6, 2.4, 2.2, "gold_i", sides=12)
    for k in range(4):           # 4本の爪で玉を支える
        a = math.radians(45 + k * 90)
        x, z = math.cos(a) * 2.3, math.sin(a) * 2.3
        m.cube("claw", (x - 0.35, 2.0, z - 0.35), (x + 0.35, 4.6, z + 0.35), "gold_i")
    m.cyl("cushion", 0, 0, 2.3, 3.1, 2.6, "purple_i", sides=12)
    # 中で光る芯と、まわりのガラス（段を重ねて丸く）
    m.cyl("glow_core", 0, 0, 4.9, 7.3, 1.2, "glow_core", sides=8)
    m.cube("glow_core2", (-0.7, 5.4, -0.7), (0.7, 6.8, 0.7), "glow_core", rot=[45, 0, 45], pivot=[0, 6.1, 0])
    # ガラス玉：薄い輪切りを重ねて丸く（中心 y=6.0、半径 3.6）
    n = 16
    for k in range(n):
        y0 = 3.0 + 6.0 * k / n
        y1 = 3.0 + 6.0 * (k + 1) / n
        yc = (y0 + y1) / 2 - 6.0
        r = math.sqrt(max(0.05, 3.6 ** 2 * (1 - (yc / 3.0) ** 2)))
        m.cyl("glass_ball", 0, 0, y0, y1, max(0.6, r), "glass_blue", sides=12)
    return m


# ---------------------------------------------------------------- 騎士のお守り
def knight_charm():
    m = I("knight_charm")
    m.cube("pouch", (-2.6, 1.0, -0.75), (2.6, 9.0, 0.75), "red_silk")
    m.cube("shoulder", (-2.2, 9.0, -0.7), (2.2, 9.9, 0.7), "red_silk")
    m.cube("top", (-1.6, 9.9, -0.65), (1.6, 10.5, 0.65), "red_silk")
    m.cube("band", (-2.7, 8.2, -0.82), (2.7, 8.8, 0.82), "gold_i")
    m.cube("label", (-1.1, 2.4, -0.85), (1.1, 7.6, -0.75), "washi_i", decal=text_decal("御守", faces=("north",)))
    # 白いひもの結び目と輪
    m.cube("knot", (-0.6, 10.4, -0.6), (0.6, 11.3, 0.6), "white_cord")
    ring(m, "loop", 0, 12.4, 0, 1.1, 0.45, 0.45, "white_cord", seg=16)
    return m


# ---------------------------------------------------------------- 妨害のお札
def _ofuda(draw, face, x0, y0, w, h):
    if face not in ("north", "south"):
        return
    red = (186, 30, 28, 255)
    draw.rectangle([x0 + 1, y0 + 1, x0 + w - 2, y0 + int(h * 0.12)], fill=red)
    cx = x0 + w / 2
    draw.line([(cx, y0 + h * 0.18), (cx + w * 0.04, y0 + h * 0.85)], fill=red, width=max(2, w // 9))
    for vv, ww in ((0.26, 0.30), (0.38, 0.24), (0.5, 0.34)):
        draw.line([(cx - w * ww, y0 + h * vv), (cx + w * ww, y0 + h * vv + 1)], fill=red, width=max(2, w // 12))
    draw.line([(cx - w * 0.06, y0 + h * 0.6), (cx - w * 0.3, y0 + h * 0.78)], fill=red, width=max(2, w // 12))
    draw.line([(cx + w * 0.06, y0 + h * 0.6), (cx + w * 0.3, y0 + h * 0.78)], fill=red, width=max(2, w // 12))
    draw.ellipse([cx - w * 0.16, y0 + h * 0.86, cx + w * 0.16, y0 + h * 0.86 + w * 0.32], outline=red, width=2)


def sabotage_talisman():
    m = I("sabotage_talisman")
    m.cube("paper", (-2.3, 2.4, -0.2), (2.3, 16.6, 0.0), "washi_i", decal=_ofuda)
    m.cube("curl", (-2.2, 2.0, -0.55), (2.2, 2.6, -0.1), "washi_i", rot=[-30, 0, 0], pivot=[0, 2.6, -0.1])
    m.cube("cord", (-0.3, 16.6, -0.3), (0.3, 17.4, 0.3), "red_cord")
    for k in range(5):
        m.cube("tassel", (-0.9 + k * 0.4, 14.5, 0.0), (-0.7 + k * 0.4, 16.6, 0.25), "red_cord")
    return m


# ---------------------------------------------------------------- 探偵の虫めがね
def detective_lens():
    m = I("detective_lens")
    ring(m, "rim", 0, 10.0, 0, 3.4, 0.8, 0.9, "brass_i", seg=28)
    # レンズ：枠の内側（半径 3.0）にぴったり収まる円。細い横の帯を並べて丸くする（枠からはみ出さない）
    R = 3.0
    n = 24
    for k in range(n):
        y0 = -R + 2 * R * k / n
        y1 = -R + 2 * R * (k + 1) / n
        yc = max(abs(y0), abs(y1))
        half = math.sqrt(max(0.0, R * R - yc * yc))
        if half > 0.05:
            m.cube("glass_lens", (-half, 10.0 + y0, -0.15), (half, 10.0 + y1, 0.15), "glass_lens")
    m.cube("shine", (-2.0, 11.2, -0.2), (-1.4, 12.4, -0.16), "washi_i")
    m.cube("neck", (-0.55, 5.6, -0.55), (0.55, 6.8, 0.55), "brass_i")
    m.cyl("ferrule", 0, 0, 4.8, 5.8, 0.75, "brass_i", sides=8)
    m.cyl("handle", 0, 0, 0.0, 4.8, 0.7, "wood_dark_i", sides=8)
    m.cyl("end", 0, 0, -0.4, 0.2, 0.85, "brass_i", sides=8)
    return m


# ---------------------------------------------------------------- 女将の鍵束
def okami_keyring():
    m = I("okami_keyring")
    ring(m, "ring", 0, 11.5, 0, 2.6, 0.5, 0.5, "iron_i", seg=28)
    for k, ang in enumerate((-35, 0, 35)):
        a = math.radians(ang)
        x = math.sin(a) * 2.6
        y = 11.5 - math.cos(a) * 2.6
        ring(m, "bow", x, y - 0.9, 0, 0.7, 0.3, 0.3, "copper_i", seg=16)
        m.cube("shaft", (x - 0.25, y - 7.6, -0.25), (x + 0.25, y - 1.6, 0.25), "copper_i",
               rot=[0, 0, ang * 0.6], pivot=[x, y - 1.6, 0])
        m.cube("bit", (x + 0.25, y - 7.4, -0.22), (x + 1.2, y - 6.6, 0.22), "copper_i", rot=[0, 0, ang * 0.6], pivot=[x, y - 1.6, 0])
        m.cube("bit2", (x + 0.25, y - 6.2, -0.22), (x + 0.9, y - 5.6, 0.22), "copper_i", rot=[0, 0, ang * 0.6], pivot=[x, y - 1.6, 0])
    m.cube("cord", (-0.25, 14.0, -0.25), (0.25, 16.0, 0.25), "red_cord")
    m.cube("knot", (-0.6, 15.8, -0.6), (0.6, 16.6, 0.6), "red_cord")
    for k in range(6):
        a = 2 * math.pi * k / 6
        x, z = math.cos(a) * 0.5, math.sin(a) * 0.5
        m.cube("fringe", (x - 0.15, 16.6, z - 0.15), (x + 0.15, 18.8, z + 0.15), "red_cord")
    return m


# ---------------------------------------------------------------- 板前のおにぎり（竹の皮にのせて）
def itamae_onigiri():
    m = I("itamae_onigiri")
    m.cube("leaf", (-5.0, 0.0, -3.4), (5.0, 0.35, 3.4), "bamboo_skin")
    m.cube("leaf_edge", (-5.2, 0.0, -2.6), (-4.6, 0.6, 2.6), "bamboo_skin", rot=[0, 0, -20], pivot=[-4.6, 0.3, 0])
    m.cube("leaf_edge", (4.6, 0.0, -2.6), (5.2, 0.6, 2.6), "bamboo_skin", rot=[0, 0, 20], pivot=[4.6, 0.3, 0])
    # 三角のごはん（下から少しずつ細くする）
    for k, (w, y0, y1) in enumerate(((7.0, 0.35, 1.6), (6.0, 1.6, 2.8), (4.8, 2.8, 3.9), (3.4, 3.9, 4.8), (2.0, 4.8, 5.5))):
        m.cube("rice", (-w / 2, y0, -1.5), (w / 2, y1, 1.5), "rice_i")
    m.cube("rice_round", (-3.1, 0.35, -1.75), (3.1, 2.4, 1.75), "rice_i")
    # 海苔（下を巻く）
    m.cube("nori", (-1.6, 0.3, -1.85), (1.6, 3.2, 1.85), "nori")
    return m


# ---------------------------------------------------------------- ナイスボタナーの呼び鈴
def buttoner_bell():
    m = I("buttoner_bell")
    m.cyl("base", 0, 0, 0, 0.9, 3.8, "wood_dark_i", sides=12)
    m.cyl("felt", 0, 0, 0.9, 1.2, 3.4, "red_i", sides=12)
    # 鐘の丸い屋根：なめらかな輪切り
    n = 12
    for k in range(n):
        y0 = 1.2 + 3.2 * k / n
        y1 = 1.2 + 3.2 * (k + 1) / n
        f = (y0 + y1) / 2 - 1.2
        r = 3.2 * math.sqrt(max(0.02, 1 - (f / 3.3) ** 2))
        m.cyl("dome", 0, 0, y0, y1, r, "brass_i", sides=12)
    m.cyl("ring", 0, 0, 1.4, 1.7, 3.3, "gold_i", sides=12)
    m.cyl("stem", 0, 0, 4.4, 5.1, 0.35, "iron_i", sides=8)
    m.cyl("button", 0, 0, 5.1, 5.8, 0.75, "red_i", sides=8)
    return m


ALL = {f.__name__: f for f in (manager_key, seer_crystal, knight_charm, sabotage_talisman, detective_lens, okami_keyring,
                               itamae_onigiri, buttoner_bell)}
