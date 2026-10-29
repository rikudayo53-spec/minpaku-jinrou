"""
家具（マップ用ブロック）28種類の高精細モデル。マイクラ寄り（1ブロック ≈ 48 ピクセル）。
座標はピクセル（ブロックの角が原点、正面は北 = -Z）。FurnitureArt と同じ置き方。
動かす部分はグループに分ける（名前は Java の HiResFurniture と合わせる）。
箱の名前が glow_ で始まると光る、glass_ で始まると半透明。
"""
from hires import HiRes, MATS, Mat

MATS.update({
    "white": Mat((236, 234, 228), "plain", noise=6),
    "black": Mat((30, 30, 32), "plain", noise=6),
    "gray": Mat((120, 122, 126), "plain", noise=8),
    "light_gray": Mat((176, 176, 172), "plaster", noise=8),
    "red_cloth": Mat((168, 44, 40), "plain", noise=10),
    "yellow_cloth": Mat((232, 190, 60), "plain", noise=10),
    "blue_cloth": Mat((60, 92, 170), "plain", noise=10),
    "sky_cloth": Mat((120, 176, 222), "plain", noise=10),
    "pink_cloth": Mat((234, 150, 176), "plain", noise=10),
    "green_cloth": Mat((90, 130, 60), "plain", noise=10),
    "purple_cloth": Mat((110, 60, 140), "plain", noise=10),
    "tatami": Mat((170, 170, 96), "bamboo", accent=(140, 140, 74), noise=8),
    "leaf": Mat((70, 128, 50), "stone", accent=(52, 100, 36), noise=18),
    "pine": Mat((50, 96, 44), "stone", accent=(36, 76, 32), noise=16),
    "soil": Mat((96, 66, 42), "stone", accent=(76, 52, 32), noise=14),
    "moss": Mat((90, 120, 56), "stone", accent=(70, 100, 44), noise=16),
    "gold": Mat((232, 186, 70), "metal", noise=10),
    "copper_old": Mat((96, 150, 120), "metal", noise=12),
    "steel_light": Mat((190, 192, 196), "metal", noise=8),
    "veg_white": Mat((238, 236, 222), "plain", noise=8),
    "veg_orange": Mat((232, 120, 40), "plain", noise=10),
    "hay": Mat((200, 170, 80), "rope", accent=(170, 140, 60)),
    "glow_fire": Mat((255, 150, 40), "fire", accent=(255, 230, 120), glow=True),
    "glow_red": Mat((240, 60, 50), "plain", noise=6, glow=True),
    "glow_green": Mat((100, 230, 90), "plain", noise=6, glow=True),
    "glow_purple": Mat((170, 80, 240), "plain", noise=10, glow=True),
    "matcha": Mat((120, 170, 60), "stipple", noise=10),
    "foam": Mat((176, 214, 110), "stipple", noise=14),
    "lacquer": Mat((26, 22, 22), "metal", noise=6),
    "lacquer_red": Mat((150, 30, 26), "metal", noise=8),
})


MATS.update({
    "cedar": Mat((168, 120, 74), "wood", accent=(130, 88, 52), noise=6),
    "cedar_dark": Mat((92, 62, 40), "wood", accent=(66, 44, 28), noise=6),
    "plaster_w": Mat((224, 214, 196), "plaster", noise=6),
    "clay_r": Mat((178, 112, 76), "plaster", noise=8),
    "cast_iron": Mat((48, 46, 48), "metal", noise=6),
    "cast_iron_old": Mat((70, 62, 58), "metal", noise=12),
    "copper_r": Mat((186, 104, 64), "metal", noise=8),
    "stone_r": Mat((128, 126, 120), "stone", noise=8),
    "washi": Mat((236, 226, 200), "paper", noise=4),
    "washi_lit": Mat((255, 206, 130), "paper", noise=4, glow=True),
    "urushi": Mat((30, 22, 20), "metal", noise=4),
    "urushi_red": Mat((140, 28, 24), "metal", noise=5),
    "felt_red": Mat((164, 36, 34), "plain", noise=6),
    "bamboo_r": Mat((196, 180, 120), "bamboo", accent=(160, 142, 86), noise=5),
    "matcha_r": Mat((110, 160, 56), "stipple", noise=6),
    "foam_r": Mat((172, 210, 104), "stipple", noise=8),
    "wood_lid": Mat((196, 158, 108), "wood", accent=(160, 120, 76), noise=5),
    "charcoal_r": Mat((30, 28, 28), "stone", noise=6),
})


def F(name):
    # リアル寄り：1ピクセルに 5×5 テクセル、なめらかな模様＋焼き込んだ陰
    return HiRes(name, density=5, real=True)


def legs4(m, x0, z0, x1, z1, h, mat, t=1.5, g=None):
    for (x, z) in ((x0, z0), (x1 - t, z0), (x0, z1 - t), (x1 - t, z1 - t)):
        m.cube("leg", (x, 0, z), (x + t, h, z + t), mat, group=g)


# ================================================================ 設備

def entrance_mat():
    m = F("entrance_mat")
    m.cube("mat", (-3, 0, 1), (19, 0.8, 15), "red_cloth")
    m.cube("trim_n", (-3, 0, 1), (19, 1, 2), "yellow_cloth")
    m.cube("trim_s", (-3, 0, 14), (19, 1, 15), "yellow_cloth")
    m.cube("trim_w", (-3, 0, 2), (-2, 1, 14), "yellow_cloth")
    m.cube("trim_e", (18, 0, 2), (19, 1, 14), "yellow_cloth")
    for x in (2, 9.5):
        m.cube("geta", (x, 1, 4), (x + 4, 2, 11), "wood_light")
        m.cube("tooth", (x + 0.5, 0.8, 5), (x + 3.5, 1, 6), "wood_dark")
        m.cube("tooth", (x + 0.5, 0.8, 9), (x + 3.5, 1, 10), "wood_dark")
        m.cube("strap", (x + 0.4, 2, 6), (x + 3.6, 2.8, 7), "red_cloth")
        m.cube("strap2", (x + 1.6, 2, 4.8), (x + 2.4, 2.6, 6.2), "red_cloth")
    return m


def call_bell():
    m = F("call_bell")
    m.cube("base", (-3, 0, 3), (19, 2, 12), "stone")
    m.cube("post_l", (-2, 2, 6), (1, 30, 9), "wood_dark")
    m.cube("post_r", (15, 2, 6), (18, 30, 9), "wood_dark")
    m.cube("beam", (-3, 27, 5), (19, 30, 10), "wood_dark")
    m.cube("roof", (-5, 30, 2), (21, 31.5, 13), "tile")
    m.cube("roof2", (-3, 31.5, 4), (19, 33, 11), "tile")
    m.group("bell", (8, 27, 7.5))
    m.cube("chain", (7.5, 23, 7), (8.5, 27, 8), "iron_black", group="bell")
    m.cyl("top", 8, 7.5, 23, 24.5, 2.6, "copper_old", sides=4, group="bell")
    m.cyl("body", 8, 7.5, 14, 23, 4.2, "copper_old", sides=6, group="bell")
    m.cyl("band", 8, 7.5, 17, 18, 4.45, "copper", sides=6, group="bell")
    m.cyl("lip", 8, 7.5, 12, 14, 5.1, "copper", sides=6, group="bell")
    m.group("striker", (8, 27, 7.5))
    m.cube("rope", (7.7, 18, -2), (8.3, 27, -1.4), "rope", group="striker")
    m.cube("log", (6, 16, -6), (10, 18, 0), "wood", group="striker")
    return m


def floor_hatch():
    m = F("floor_hatch")
    m.cube("frame_n", (0, 0, 0), (16, 1.5, 2), "wood_dark")
    m.cube("frame_s", (0, 0, 14), (16, 1.5, 16), "wood_dark")
    m.cube("frame_w", (0, 0, 2), (2, 1.5, 14), "wood_dark")
    m.cube("frame_e", (14, 0, 2), (16, 1.5, 14), "wood_dark")
    m.cube("hole", (2, 0.1, 2), (14, 0.4, 14), "black")
    m.group("lid", (8, 1.5, 14))
    m.cube("lid", (2, 0.5, 2), (14, 1.5, 14), "wood", group="lid")
    m.cube("handle", (7, 1.5, 3), (9, 2.2, 4.2), "iron_black", group="lid")
    return m


def breaker():
    m = F("breaker")
    m.cube("leg_l", (2, 0, 6), (4, 6, 12), "iron_black")
    m.cube("leg_r", (12, 0, 6), (14, 6, 12), "iron_black")
    m.cube("box", (1, 6, 4), (15, 26, 14), "light_gray")
    m.cube("pipe", (7, 26, 8), (9, 34, 10), "steel")
    m.cube("panel", (2, 7, 4.6), (14, 25, 5), "black")
    for i in range(4):
        m.cube("switch", (3 + i * 3, 19, 4.2), (5 + i * 3, 23, 4.8), "white")
    m.cube("lever_base", (6, 10, 3.8), (10, 14, 4.6), "steel")
    m.group("lever", (8, 12, 4.4))
    m.cube("lever", (7, 12, 1.4), (9, 13, 4.6), "red_cloth", group="lever")
    m.group("door", (1, 16, 4))
    m.cube("door", (1, 6, 3), (15, 26, 4), "steel_light", group="door")
    m.cube("sign", (6, 17, 2.7), (10, 21, 3), "yellow_cloth", group="door")
    m.cube("bolt", (7.5, 17.5, 2.6), (8.5, 20.5, 2.7), "black", group="door")
    return m


def gas_valve():
    m = F("gas_valve")
    m.cube("base", (3, 0, 7), (13, 1, 15), "stone")
    m.cube("riser", (6, 1, 9), (10, 20, 13), "copper")
    m.cube("pipe", (-4, 16, 9), (20, 20, 13), "copper")
    m.cube("joint", (5, 15, 8), (11, 21, 14), "copper_old")
    m.cube("stem", (7, 17, 4), (9, 19, 8), "steel")
    m.cube("gauge", (12, 21, 9), (15, 24, 10), "white")
    m.cube("tick", (13.2, 22, 8.8), (13.8, 23.6, 9), "red_cloth")
    m.group("wheel", (8, 18, 4))
    for (a, b) in (((2.5, 17.4, 3), (13.5, 18.6, 4)), ((7.4, 12.5, 3), (8.6, 23.5, 4)), ((3, 12.5, 3), (13, 13.5, 4)),
                   ((3, 22.5, 3), (13, 23.5, 4)), ((2.5, 13, 3), (3.5, 23, 4)), ((12.5, 13, 3), (13.5, 23, 4))):
        m.cube("spoke", a, b, "red_cloth", group="wheel")
    return m


def boiler():
    m = F("boiler")
    m.group("body", (8, 0, 8))
    m.cyl("tank", 8, 8, 0, 26, 7, "steel_light", sides=6, group="body")
    m.cyl("band1", 8, 8, 8, 9, 7.3, "copper", sides=6, group="body")
    m.cyl("band2", 8, 8, 18, 19, 7.3, "copper", sides=6, group="body")
    m.cyl("cap", 8, 8, 26, 28, 4.5, "steel", sides=6, group="body")
    m.cyl("chimney", 8, 8, 28, 34, 2.2, "iron_black", sides=4, group="body")
    m.cube("window", (5, 2, 0.6), (11, 6, 1), "black", group="body")
    m.group("fire", (8, 4, 0.5), "body")
    m.cube("glow_fire", (5.5, 2.3, 0.4), (10.5, 5.7, 0.6), "glow_fire", group="fire")
    m.cube("dial", (5, 14, 0.4), (11, 20, 1), "white", group="body")
    m.group("needle", (8, 17, 0), "body")
    m.cube("needle", (7.6, 17, -0.1), (8.4, 19.6, 0.4), "red_cloth", group="needle")
    return m


def meeting_table():
    m = F("meeting_table")
    m.cube("top", (-12, 6, -12), (28, 8, 28), "lacquer")
    m.cube("apron", (-12.3, 5.6, -12.3), (28.3, 6.2, 28.3), "lacquer_red")
    for (x, z) in ((-10, -10), (24, -10), (-10, 24), (24, 24)):
        m.cube("leg", (x, 0, z), (x + 3, 6, z + 3), "lacquer")
    m.cube("hearth", (3, 8, 3), (13, 9, 13), "iron_black")
    m.cube("glow_coal", (4.5, 8.6, 4.5), (11.5, 9.3, 11.5), "ember")
    for (x, z) in ((-8, -8), (20, -8), (-8, 20), (20, 20), (6, -9), (6, 21), (-9, 6), (21, 6)):
        m.cyl("cup", x + 1.25, z + 1.25, 8, 10.5, 1.3, "white", sides=4)
        m.cyl("tea", x + 1.25, z + 1.25, 10.2, 10.55, 1.0, "matcha", sides=4)
    m.cyl("pot", 16.5, 16.5, 8, 12, 2.6, "copper_r", sides=6)
    m.cube("spout", (18.5, 10, 16), (21, 10.8, 17), "copper")
    return m


def hokora():
    m = F("hokora")
    m.cube("base", (-2, 0, -2), (18, 4, 18), "stone")
    m.cube("base2", (0, 4, 0), (16, 6, 16), "moss_stone")
    m.group("shrine", (2, 6, 8))
    m.cube("walls", (2, 6, 3), (14, 18, 14), "wood", group="shrine")
    m.cube("pillar_l", (1.5, 6, 2.5), (3, 19, 4), "wood_dark", group="shrine")
    m.cube("pillar_r", (13, 6, 2.5), (14.5, 19, 4), "wood_dark", group="shrine")
    m.group("evil", (8, 11, 3.2), "shrine")
    m.cube("glow_inside", (4, 7, 3.1), (12, 16, 3.3), "glow_purple", group="evil")
    m.group("door_l", (3, 8, 2.8), "shrine")
    m.cube("door", (3, 7, 2.6), (8, 17, 3.2), "wood_dark", group="door_l")
    m.group("door_r", (13, 8, 2.8), "shrine")
    m.cube("door", (8, 7, 2.6), (13, 17, 3.2), "wood_dark", group="door_r")
    m.group("roof", (8, 19, 8), "shrine")
    m.cube("roof1", (-0.5, 18, 0.5), (16.5, 19.5, 16), "tile", group="roof")
    m.cube("roof2", (1, 19.5, 1.5), (15, 21, 15), "tile", group="roof")
    m.cube("roof3", (3, 21, 3), (13, 22, 13), "tile", group="roof")
    m.cube("finial", (7, 22, 7), (9, 23, 9), "gold", group="roof")
    m.group("rope", (1, 16.5, 2), "shrine")
    m.cube("rope", (1, 16, 1.6), (15, 17.2, 2.4), "rope", group="rope")
    for x in (4, 8, 12):
        m.cube("shide", (x - 0.8, 13, 1.7), (x + 0.8, 16, 1.9), "paper", group="rope")
    m.cube("offering", (6, 6, -1.5), (10, 6.5, 1.5), "white")
    return m


# ================================================================ タスク（ミニゲーム）

def wiring_panel():
    m = F("wiring_panel")
    m.cube("leg_l", (2, 0, 7), (4, 4, 13), "iron_black")
    m.cube("leg_r", (12, 0, 7), (14, 4, 13), "iron_black")
    m.cube("box", (1, 4, 6), (15, 28, 14), "stone")
    m.cube("board", (2, 5, 5.6), (14, 27, 6), "black")
    cols = ["red_cloth", "blue_cloth", "yellow_cloth", "green_cloth"]
    for i in range(4):
        y = 9 + i * 4.5
        m.cube("left", (3, y, 5.2), (6, y + 1, 5.8), cols[i])
        m.cube("right", (10, y, 5.2), (13, y + 1, 5.8), cols[(i + 1) % 4])
        m.group(f"wire{i}", (8, y, 5.5))
        m.cube("wire", (6, y, 5.3), (10, y + 1, 5.7), cols[i], group=f"wire{i}")
    m.group("lamp_off", (13, 26, 5))
    m.cube("glow_lamp", (12, 25, 4.6), (14, 27, 5.2), "glow_red", group="lamp_off")
    m.group("lamp_on", (13, 26, 5))
    m.cube("glow_lamp", (12, 25, 4.6), (14, 27, 5.2), "glow_green", group="lamp_on")
    m.group("door", (15, 16, 5))
    m.cube("door", (1, 4, 5), (15, 28, 6), "light_gray", group="door")
    m.cube("sign", (6, 18, 4.7), (10, 22, 5), "yellow_cloth", group="door")
    return m


def ledger():
    m = F("ledger")
    m.cube("desk", (-2, 9, 2), (18, 11, 14), "wood_dark")
    legs4(m, -1, 3, 17, 13, 9, "wood_dark", t=2)
    m.cube("rail", (-1, 2, 4), (17, 3, 12), "wood_dark")
    m.cube("cover", (1, 11, 4), (15, 11.6, 12), "red_cloth")
    m.cube("page_l", (1.5, 11.6, 4.5), (7.8, 12.6, 11.5), "paper")
    m.cube("page_r", (8.2, 11.6, 4.5), (14.5, 12.6, 11.5), "paper")
    m.group("flip", (8, 12.6, 8))
    m.cube("page", (8.1, 12.6, 4.7), (14.3, 12.9, 11.3), "paper", group="flip")
    m.cube("ink", (15, 11, 9), (17.5, 12, 13), "iron_black")
    m.cube("brush", (10, 12.6, 2.5), (16, 13.3, 3.2), "bamboo")
    m.cube("brush_tip", (9, 12.6, 2.5), (10, 13.3, 3.2), "black")
    m.cube("candle", (-1, 11, 10), (0.5, 15, 11.5), "white")
    m.cube("glow_flame", (-0.6, 15, 10.4), (0.1, 16.5, 11.1), "glow_fire")
    return m


def shoji_screen():
    m = F("shoji_screen")
    m.group("screen", (8, 0, 8))
    m.cube("post_l", (-1, 0, 6.5), (1.5, 32, 9.5), "wood_light", group="screen")
    m.cube("post_r", (14.5, 0, 6.5), (17, 32, 9.5), "wood_light", group="screen")
    m.cube("top", (-1, 30, 6.5), (17, 32.5, 9.5), "wood_light", group="screen")
    m.cube("sill", (-1, 0, 6.5), (17, 2, 9.5), "wood_light", group="screen")
    m.cube("paper", (1.5, 2, 7.6), (14.5, 30, 8.4), "paper", group="screen")
    for x in (5, 8, 11):
        m.cube("kumiko", (x - 0.35, 2, 7.3), (x + 0.35, 30, 8.7), "wood", group="screen")
    y = 6.0
    while y < 30:
        m.cube("kumiko", (1.5, y - 0.35, 7.3), (14.5, y + 0.35, 8.7), "wood", group="screen")
        y += 4.5
    for k, (hx, hy) in enumerate(((3, 9), (9.5, 14), (6, 20.5), (12, 25))):
        m.group(f"hole{k}", (hx, hy, 8), "screen")
        m.cube("hole", (hx - 0.9, hy - 0.9, 7.5), (hx + 0.9, hy + 0.9, 8.5), "black", group=f"hole{k}")
        m.group(f"patch{k}", (hx, hy, 8), "screen")
        m.cube("petal", (hx - 1.3, hy - 1.3, 7.25), (hx + 1.3, hy + 1.3, 8.75), "pink_cloth", group=f"patch{k}")
        m.cube("petal", (hx - 0.5, hy - 1.9, 7.25), (hx + 0.5, hy + 1.9, 8.75), "pink_cloth", group=f"patch{k}")
        m.cube("petal", (hx - 1.9, hy - 0.5, 7.25), (hx + 1.9, hy + 0.5, 8.75), "pink_cloth", group=f"patch{k}")
    return m


# ================================================================ タスク（マイクラらしい作業）

def futon():
    m = F("futon")
    m.cube("tatami", (-2, 0, -6), (18, 0.5, 22), "tatami")

    def seg(name, z0, z1, g):
        m.cube(name, (1, 0.5, z0), (15, 2, z1), "white", group=g)
        m.cube(name + "_cover", (1.5, 2, z0), (14.5, 2.5, z1), "sky_cloth", group=g)
    seg("far", 12, 20, None)
    m.group("fold1", (8, 2, 12))
    seg("mid", 4, 12, "fold1")
    m.group("fold2", (8, 2, 4), "fold1")
    seg("near", -4, 4, "fold2")
    m.group("pillow", (8, 2, 17))
    m.cube("pillow", (4, 2, 15), (12, 4.5, 19.5), "white", group="pillow")
    m.cube("band", (4, 2.2, 16.5), (12, 4.7, 17), "sky_cloth", group="pillow")
    return m


def kamado():
    m = F("kamado")
    # 漆喰の本体（角を面取り）
    m.cube("body", (-4, 0.8, 2.4), (20, 10.6, 15), "plaster_w")
    m.cube("body_cham", (-3.6, 0.8, 2), (19.6, 10.6, 15.2), "plaster_w")
    m.cube("base", (-4.6, 0, 1.4), (20.6, 1.2, 15.6), "stone_r")
    m.cube("top", (-4.6, 10.4, 1.4), (20.6, 11.8, 15.6), "clay_r")
    m.cube("top_lip", (-4.9, 11.4, 1.1), (20.9, 12, 15.9), "clay_r")
    for x in (-1.2, 10.8):
        m.cube("mouth", (x, 1.6, 1.85), (x + 6.4, 6.6, 2.1), "charcoal_r")
        m.cube("arch", (x - 0.6, 6.6, 1.6), (x + 7.0, 7.6, 2.1), "stone_r")
        m.cube("arch_l", (x - 0.6, 1.2, 1.6), (x + 0.2, 6.6, 2.1), "stone_r")
        m.cube("arch_r", (x + 6.2, 1.2, 1.6), (x + 7.0, 6.6, 2.1), "stone_r")
        m.cube("ash", (x + 0.3, 1.4, 1.95), (x + 6.1, 2.0, 2.05), "ash")
    m.group("fire", (8, 4, 1.6))
    for x in (-1.2, 10.8):
        m.cube("glow_fire", (x + 0.8, 1.9, 1.75), (x + 5.6, 5.6, 1.9), "glow_fire", group="fire")
        m.cube("glow_core", (x + 2.2, 1.9, 1.7), (x + 4.2, 4.0, 1.8), "ember", group="fire")
    # 羽釜（丸く）
    m.cyl("pot", 2.5, 8.5, 12, 16.5, 4.6, "cast_iron", sides=6)
    m.cyl("brim", 2.5, 8.5, 14, 14.9, 5.8, "cast_iron_old", sides=6)
    m.group("lid", (2.5, 16.5, 8.5))
    m.cyl("lid", 2.5, 8.5, 16.5, 17.3, 4.3, "wood_lid", sides=6, group="lid")
    m.cube("knob", (0.2, 17.3, 8.0), (4.8, 18.4, 9.0), "cedar_dark", group="lid")
    # 銅の鍋
    m.cyl("pan", 13.5, 8.5, 12, 15.4, 3.8, "copper_r", sides=6)
    m.cyl("pan_rim", 13.5, 8.5, 15.0, 15.6, 4.2, "copper_r", sides=4)
    m.cyl("pan_lid", 13.5, 8.5, 15.6, 16.2, 3.4, "wood_lid", sides=4)
    m.cube("pan_handle", (17.2, 14.4, 8.1), (21, 15, 8.9), "cedar_dark")
    # 薪
    m.cyl("log1", 1, -2, 0, 2, 1.1, "cedar_dark", sides=4)
    for k, (x, z) in enumerate(((-3, -2.5), (0.5, -1.8))):
        m.cube("log", (x, 0, z), (x + 7.5, 1.6, z + 1.6), "cedar" if k else "cedar_dark", rot=[0, 8 - k * 14, 0])
    m.cube("log_top", (-1, 1.6, -2.4), (6, 3.0, -1.0), "cedar", rot=[0, -6, 0])
    return m


def bath_stove():
    m = F("bath_stove")
    m.cube("wall_n", (-2, 0, 0), (18, 12, 2), "wood_light")
    m.cube("wall_s", (-2, 0, 14), (18, 12, 16), "wood_light")
    m.cube("wall_w", (-2, 0, 2), (0, 12, 14), "wood_light")
    m.cube("wall_e", (16, 0, 2), (18, 12, 14), "wood_light")
    m.cube("floor", (0, 0, 2), (16, 1, 14), "wood_light")
    for y in (3, 9):
        m.cube("hoop_n", (-2.2, y, -0.2), (18.2, y + 0.8, 0.2), "steel")
        m.cube("hoop_s", (-2.2, y, 15.8), (18.2, y + 0.8, 16.2), "steel")
    m.group("water", (8, 1, 8))
    m.cube("glass_water", (0, 1, 2), (16, 9, 14), "water", group="water")
    m.cube("stove", (5, 0, -4), (11, 5, 0), "clay")
    m.cube("mouth", (6, 1, -4.2), (10, 4, -3.9), "black")
    m.group("fire", (8, 2.5, -4.3))
    m.cube("glow_fire", (6.3, 1.2, -4.4), (9.7, 3.8, -4.2), "glow_fire", group="fire")
    m.cube("chimney", (17, 0, 10), (20, 20, 13), "iron_black")
    m.cyl("pail", 21, 3, 0, 4, 2.1, "wood", sides=4)
    m.cube("ladle", (20.5, 4, 2.5), (21.5, 7, 3.5), "bamboo")
    return m


def chopping_block():
    m = F("chopping_block")
    m.cyl("stump", 8, 8, 0, 8, 6.2, "wood", sides=6)
    m.group("half_l", (5, 8, 8))
    m.cube("log", (5, 8, 5), (8, 16, 11), "wood_light", group="half_l")
    m.group("half_r", (11, 8, 8))
    m.cube("log", (8, 8, 5), (11, 16, 11), "wood_light", group="half_r")
    m.group("axe", (21, 9, 8), rot=(0, 0, 50))
    m.cube("handle", (20.4, 9, 7.4), (21.6, 23, 8.6), "wood", group="axe")
    m.cube("head", (16.5, 19.5, 7.2), (21.5, 23.5, 8.8), "steel", group="axe")
    m.cube("edge", (15.5, 20.2, 7.5), (16.5, 22.8, 8.5), "steel_light", group="axe")
    m.cube("pile1", (-4, 0, 10), (1, 2.5, 18), "wood_light")
    m.cube("pile2", (-3.5, 2.5, 11), (1.5, 5, 17), "wood_light")
    m.cube("pile3", (-4.5, 0, 2), (0, 2.5, 9), "wood")
    return m


def vegetable_patch():
    m = F("vegetable_patch")
    m.cube("bed", (-4, 0, -2), (20, 2.5, 18), "soil")
    m.cube("ridge1", (-3.5, 2.5, 0.5), (19.5, 4, 6.5), "soil")
    m.cube("ridge2", (-3.5, 2.5, 9.5), (19.5, 4, 15.5), "soil")
    k = 0
    for z in (2, 11):
        for x in (-2, 3, 8, 13):
            g = m.group(f"veg{k}", (x + 1.5, 4, z + 1.5))
            root = "veg_white" if k % 2 == 0 else "veg_orange"
            m.cube("root", (x, -1, z), (x + 3, 4.5, z + 3), root, group=g)
            m.cube("tip", (x + 0.8, -2.5, z + 0.8), (x + 2.2, -1, z + 2.2), root, group=g)
            m.cube("leaf", (x + 0.2, 4.5, z + 0.2), (x + 2.8, 8.5, z + 2.8), "leaf", group=g)
            m.cube("leaf2", (x - 0.8, 6.5, z + 1), (x + 3.8, 9, z + 2), "leaf", group=g)
            k += 1
    m.cube("basket", (5, 0, -6), (13, 1.5, -2), "bamboo")
    return m


def dust_pile():
    m = F("dust_pile")
    m.group("dust", (6, 0, 7))
    m.cube("pile1", (1, 0, 3), (9, 2.5, 9), "ash", group="dust")
    m.cube("pile2", (2.5, 0, 4.5), (7.5, 3.8, 7.5), "light_gray", group="dust")
    m.cube("pile3", (9, 0, 8), (13, 1.6, 12), "ash", group="dust")
    m.cube("pile4", (-2, 0, 9), (2, 1.2, 13), "light_gray", group="dust")
    m.cube("pan", (12, 0, 1), (18, 0.6, 6), "light_gray")
    m.cube("pan_back", (12, 0.6, 5.4), (18, 3, 6), "light_gray")
    m.cube("pan_handle", (14.5, 1, 6), (15.5, 2, 11), "bamboo")
    m.group("broom", (8, 22, 9))
    m.cube("stick", (7.5, 5, 8.5), (8.5, 26, 9.5), "bamboo", group="broom")
    m.cube("bristle", (4.5, 0, 7.5), (11.5, 6, 10.5), "hay", group="broom")
    m.cube("tie", (7, 5, 8), (9, 6, 10), "red_cloth", group="broom")
    return m


def laundry_pole():
    m = F("laundry_pole")
    m.cube("stone_l", (-4, 0, 6), (0, 2, 10), "stone")
    m.cube("stone_r", (16, 0, 6), (20, 2, 10), "stone")
    m.cube("post_l", (-3, 2, 7), (-1, 30, 9), "bamboo")
    m.cube("post_r", (17, 2, 7), (19, 30, 9), "bamboo")
    m.cube("pole", (-3, 26, 7.4), (19, 27.5, 8.6), "bamboo")
    cloths = ["white", "sky_cloth", "pink_cloth", "blue_cloth"]
    spans = ((-0.5, 3.5, 9), (4.5, 10.5, 11), (11.5, 14.5, 8), (15, 17.5, 12))
    for i, (x0, x1, h) in enumerate(spans):
        m.group(f"cloth{i}", ((x0 + x1) / 2, 26, 8))
        m.cube("cloth", (x0, 26 - h, 7.6), (x1, 26, 8.4), cloths[i], group=f"cloth{i}")
    m.cube("basket", (2, 0, -4), (12, 4, 2), "bamboo")
    return m


def flower_vase():
    m = F("flower_vase")
    m.cube("stand", (1, 0, 1), (15, 3, 15), "wood_dark")
    m.cube("stand_top", (0, 3, 0), (16, 4, 16), "wood_dark")
    m.cyl("vase", 8, 8, 4, 12, 3.0, "sky_cloth", sides=6)
    m.cyl("belly", 8, 8, 6, 10, 3.6, "sky_cloth", sides=6)
    m.cyl("neck", 8, 8, 12, 15, 1.9, "sky_cloth", sides=4)
    m.cyl("lip", 8, 8, 15, 16, 2.5, "white", sides=4)
    m.group("flowers", (8, 15, 8))
    heads = ("pink_cloth", "red_cloth", "yellow_cloth")
    for i, (x, z) in enumerate(((7.2, 7.2), (8.6, 8.4), (7.6, 9.2))):
        ln = 9 + i * 2
        m.cube("stem", (x - 0.3, 15, z - 0.3), (x + 0.3, 15 + ln, z + 0.3), "green_cloth", group="flowers",
               rot=(0, 0, (i - 1) * 12), pivot=(x, 15, z))
        m.cube("head", (x - 1.8 + (i - 1) * 2, 15 + ln - 0.5, z - 1.8), (x + 1.8 + (i - 1) * 2, 15 + ln + 2, z + 1.8),
               heads[i], group="flowers")
    m.group("can", (16, 22, 8), rot=(0, 0, 25))
    m.cube("can", (13, 19, 6), (19, 24, 10), "copper", group="can")
    m.cube("spout", (9, 21, 7.4), (13, 22.2, 8.6), "copper", group="can")
    return m


def kitchen_sink():
    m = F("kitchen_sink")
    m.cube("cabinet", (-2, 0, 2), (18, 14, 16), "wood")
    m.cube("seam", (7.7, 1, 1.7), (8.3, 13, 2), "wood_dark")
    m.cube("counter", (-3, 14, 1), (19, 15, 17), "stone")
    m.cube("tub_n", (0, 15, 4), (12, 18, 5), "steel_light")
    m.cube("tub_s", (0, 15, 13), (12, 18, 14), "steel_light")
    m.cube("tub_w", (0, 15, 5), (1, 18, 13), "steel_light")
    m.cube("tub_e", (11, 15, 5), (12, 18, 13), "steel_light")
    m.cube("glass_water", (1, 15, 5), (11, 16.5, 13), "water")
    m.cube("pipe_up", (13, 15, 7), (15, 27, 9), "bamboo")
    m.cube("pipe_arm", (5, 25, 7.4), (15, 26.4, 8.6), "bamboo")
    m.group("stream", (6, 25, 8))
    m.cube("glass_stream", (5.5, 16, 7.7), (6.5, 25, 8.3), "water", group="stream")
    for i in range(5):
        m.group(f"dirty{i}", (6, 16 + i * 0.8, 9))
        m.cube("dish", (2.5 + (i % 2) * 0.6, 16 + i * 0.8, 6.5), (9.5 + (i % 2) * 0.6, 16.6 + i * 0.8, 11.5), "white",
               group=f"dirty{i}")
        m.group(f"clean{i}", (14 + i * 1.1, 19, 13))
        m.cube("dish", (13.5 + i * 1.1, 16, 10.8), (14.1 + i * 1.1, 22, 15.8), "white", group=f"clean{i}")
    m.cube("rack", (13, 15, 10), (19, 16, 16.5), "bamboo")
    return m


def shoe_rack():
    m = F("shoe_rack")
    m.cube("back", (0, 0, 15), (16, 22, 16), "wood")
    m.cube("side_l", (0, 0, 4), (1, 22, 15), "wood")
    m.cube("side_r", (15, 0, 4), (16, 22, 15), "wood")
    m.cube("top", (0, 21, 3.5), (16, 22.5, 16), "wood")
    m.cube("bottom", (0, 0, 4), (16, 1, 15), "wood")
    m.cube("shelf", (1, 10.5, 4.5), (15, 11.5, 15), "wood")
    m.cube("divider", (7.5, 1, 4.5), (8.5, 21, 15), "wood")
    m.cyl("pot", 12.5, 9.5, 22.5, 26, 1.7, "sky_cloth", sides=4)
    m.cube("plant", (10.5, 26, 7.5), (14.5, 29, 11.5), "leaf")
    m.cube("cloth", (2, 22.5, 7), (7, 23, 12), "red_cloth")
    pairs = (("wood_light", "red_cloth"), ("white", "blue_cloth"), ("wood_dark", "wood_dark"), ("pink_cloth", "white"))
    cub = ((1.5, 1), (9, 1), (1.5, 11.5), (9, 11.5))
    k = 0
    for i in range(4):
        for s in range(2):
            x = cub[i][0] + s * 3.3
            y = cub[i][1]
            m.group(f"shoe{k}", (x + 1.3, y, 9))
            m.cube("sole", (x, y, 6), (x + 2.6, y + 1.6, 13), pairs[i][0], group=f"shoe{k}")
            m.cube("top", (x + 0.2, y + 1.6, 6.2), (x + 2.4, y + 2.8, 9), pairs[i][1], group=f"shoe{k}")
            k += 1
    return m


# ================================================================ 第3弾のタスク

def andon():
    m = F("andon")
    m.cube("base", (2.6, 0, 2.6), (13.4, 1.0, 13.4), "cedar_dark")
    m.cube("base_top", (3, 1.0, 3), (13, 1.6, 13), "cedar_dark")
    for (x, z) in ((3, 3), (12.1, 3), (3, 12.1), (12.1, 12.1)):
        m.cube("post", (x, 1.6, z), (x + 0.9, 20, z + 0.9), "cedar_dark")
    for y in (10, 14.6, 19.2):
        for (a, b, c, d) in ((3, 3, 13, 3.9), (3, 12.1, 13, 13), (3, 3, 3.9, 13), (12.1, 3, 13, 13)):
            m.cube("rail", (a, y, b), (c, y + 0.5, d), "cedar_dark")
    m.cube("top", (2.7, 19.6, 2.7), (13.3, 20.4, 13.3), "cedar_dark")
    m.cube("shelf", (3.6, 9, 3.6), (12.4, 9.5, 12.4), "cedar")
    m.cyl("dish", 8, 8, 9.5, 10.1, 1.8, "urushi_red", sides=4)
    m.cube("oil", (6.6, 9.9, 6.6), (9.4, 10.15, 9.4), "lacquer")
    m.cube("wick", (7.8, 10.1, 7.8), (8.2, 10.6, 8.2), "rope")
    m.group("flame", (8, 10.4, 8))
    m.cube("glow_flame", (7.4, 10.4, 7.4), (8.6, 12.4, 8.6), "glow_fire", group="flame")
    m.cube("glow_tip", (7.7, 12.4, 7.7), (8.3, 13.4, 8.3), "glow_fire", group="flame")
    m.group("paper_off", (8, 15, 8))
    m.group("paper_on", (8, 15, 8))
    for gname, mat in (("paper_off", "washi"), ("paper_on", "washi_lit")):
        pre = "glow_" if gname == "paper_on" else ""
        m.cube(pre + "pn", (3.9, 10.5, 3.6), (12.1, 19.2, 3.75), mat, group=gname)
        m.cube(pre + "ps", (3.9, 10.5, 12.25), (12.1, 19.2, 12.4), mat, group=gname)
        m.cube(pre + "pw", (3.6, 10.5, 3.9), (3.75, 19.2, 12.1), mat, group=gname)
        m.cube(pre + "pe", (12.25, 10.5, 3.9), (12.4, 19.2, 12.1), mat, group=gname)
    m.cube("handle_l", (7.6, 20.4, 3.4), (8.4, 23.6, 4.2), "cedar_dark")
    m.cube("handle_r", (7.6, 20.4, 11.8), (8.4, 23.6, 12.6), "cedar_dark")
    m.cube("handle_t", (7.5, 23.4, 3.3), (8.5, 24.3, 12.7), "cedar_dark")
    return m


def irori():
    m = F("irori")
    m.cube("frame_n", (-6, 0, -6), (22, 2, -3), "wood")
    m.cube("frame_s", (-6, 0, 19), (22, 2, 22), "wood")
    m.cube("frame_w", (-6, 0, -3), (-3, 2, 19), "wood")
    m.cube("frame_e", (19, 0, -3), (22, 2, 19), "wood")
    m.cube("ash", (-3, 0, -3), (19, 1.2, 19), "ash")
    for (x, z) in ((4, 5), (8, 4), (11, 6), (6, 9), (10, 10), (5, 12)):
        m.cube("coal", (x, 1.2, z), (x + 2.5, 2.6, z + 2.5), "charcoal")
    m.group("ember", (8, 2.6, 8))
    for (x, z) in ((4, 5), (8, 4), (11, 6), (6, 9), (10, 10), (5, 12)):
        m.cube("glow_ember", (x + 0.2, 2.5, z + 0.2), (x + 2.3, 2.7, z + 2.3), "ember", group="ember")
    m.group("fire", (8, 2.6, 8))
    m.cube("glow_fire", (6, 2.6, 7), (10, 7.5, 7.2), "glow_fire", group="fire")
    m.cube("glow_fire2", (7.9, 2.6, 5), (8.1, 7.5, 9.5), "glow_fire", group="fire")
    m.cube("hook", (7.4, 12, 7.4), (8.6, 36, 8.6), "bamboo")
    m.cube("fish", (4, 16, 7.5), (12, 18, 8.5), "wood")
    m.cyl("kettle", 8, 8, 6, 11, 3.6, "cast_iron", sides=6)
    m.cyl("kettle_top", 8, 8, 11, 12, 2.6, "cast_iron_old", sides=4)
    m.cube("kettle_spout", (11.5, 8, 7.5), (14, 9, 8.5), "iron_black")
    m.cube("cushion_w", (-12, 0, 4), (-7, 1.2, 12), "purple_cloth")
    m.cube("cushion_e", (23, 0, 4), (28, 1.2, 12), "purple_cloth")
    return m


def bonsai():
    m = F("bonsai")
    m.cube("stand", (1, 0, 3), (15, 4, 13), "wood_dark")
    m.cube("pot", (2.5, 4, 4.5), (13.5, 8, 11.5), "blue_cloth")
    m.cube("soil", (3, 7.8, 5), (13, 8.5, 11), "moss")
    m.cube("trunk", (7, 8.5, 7), (9, 12, 9), "wood_dark")
    m.cube("trunk2", (7, 12, 7.2), (8.8, 16, 8.8), "wood_dark", rot=(0, 0, -20), pivot=(8, 12, 8))
    m.cube("branch_l", (4, 15, 7.4), (9, 16.4, 8.6), "wood_dark")
    m.cube("branch_r", (8, 16, 7.4), (12, 17.4, 8.6), "wood_dark")
    for i, (x, y, w) in enumerate(((2, 15.5, 5), (10, 17, 6), (5, 19, 5.5))):
        m.cube("cloud", (x, y, 5.5), (x + w, y + 2.4, 10.5), "pine")
        m.group(f"wild{i}", (x + 1, y + 2.4, 7))
        m.cube("wild", (x + 1, y + 2.4, 6.5), (x + 2.5, y + 2.4 + 3 + i, 8), "pine", group=f"wild{i}")
        m.cube("wild2", (x + w - 1, y, 8), (x + w + 2, y + 1.5, 9.5), "pine", group=f"wild{i}")
    m.group("blade_a", (16, 14, 8), rot=(0, 0, 20))
    m.cube("blade", (13, 14, 7.6), (19, 14.6, 8.4), "steel_light", group="blade_a")
    m.group("blade_b", (16, 14, 8), rot=(0, 0, 20))
    m.cube("blade", (13, 13.4, 7.6), (19, 14, 8.4), "steel_light", group="blade_b")
    m.cube("grip", (18.5, 12.5, 7.5), (21, 15.5, 8.5), "red_cloth")
    return m


def tea_set():
    m = F("tea_set")
    m.cube("mat", (-4, 0, -2), (20, 0.4, 18), "felt_red")
    m.cube("mat_edge", (-4.2, 0, -2.2), (20.2, 0.3, 18.2), "felt_red")
    # 風炉と釜
    m.cyl("brazier", 2, 13, 0.4, 5.6, 3.9, "cast_iron_old", sides=6)
    m.cyl("brazier_rim", 2, 13, 5.2, 6.0, 4.3, "cast_iron", sides=6)
    m.cyl("kama", 2, 13, 6, 10, 3.4, "cast_iron", sides=6)
    m.cyl("kama_lid", 2, 13, 10, 10.6, 2.3, "copper_r", sides=4)
    m.cube("kama_knob", (1.5, 10.6, 12.5), (2.5, 11.3, 13.5), "copper_r")
    for z in (9.4, 16.1):
        m.cube("ear", (1.4, 8.4, z), (2.6, 9.6, z + 0.5), "cast_iron_old")
    # 茶碗（丸く、内側に抹茶）
    m.cyl("bowl_foot", 11.5, 5.5, 0.4, 1.2, 1.8, "urushi", sides=4)
    m.cyl("bowl", 11.5, 5.5, 1.2, 4.4, 3.4, "urushi", sides=6)
    m.cyl("bowl_rim", 11.5, 5.5, 3.8, 4.8, 3.7, "urushi", sides=6)
    m.cyl("tea", 11.5, 5.5, 3.9, 4.75, 3.2, "matcha_r", sides=6)
    m.group("foam", (11.5, 4.75, 5.5))
    m.cyl("foam", 11.5, 5.5, 4.75, 5.05, 3.1, "foam_r", sides=6, group="foam")
    # 茶筅
    m.group("whisk", (11.5, 5, 5.5))
    m.cyl("tines", 11.5, 5.5, 4.2, 6.8, 0.9, "bamboo_r", sides=4, group="whisk")
    m.cyl("tines_top", 11.5, 5.5, 6.8, 7.4, 0.6, "bamboo_r", sides=4, group="whisk")
    m.cyl("handle", 11.5, 5.5, 7.4, 12, 0.35, "bamboo_r", sides=4, group="whisk")
    # 棗と茶杓
    m.cyl("caddy", 17.5, 4.5, 0.4, 3.6, 1.6, "urushi", sides=6)
    m.cyl("caddy_lid", 17.5, 4.5, 3.6, 4.2, 1.65, "urushi", sides=6)
    m.cube("scoop", (14.5, 0.4, 9.2), (20, 0.75, 9.8), "bamboo_r", rot=[0, -10, 0])
    m.cube("scoop_tip", (14.2, 0.4, 9.1), (15, 0.95, 9.9), "bamboo_r", rot=[0, -10, 0])
    return m


def well():
    m = F("well")
    m.cube("wall_n", (2, 0, 1), (14, 9, 3), "stone")
    m.cube("wall_s", (2, 0, 13), (14, 9, 15), "moss_stone")
    m.cube("wall_w", (1, 0, 2), (3, 9, 14), "moss_stone")
    m.cube("wall_e", (13, 0, 2), (15, 9, 14), "stone")
    m.cube("rim_n", (1, 9, 1), (15, 10, 3), "stone_dark")
    m.cube("rim_s", (1, 9, 13), (15, 10, 15), "stone_dark")
    m.cube("rim_w", (1, 9, 3), (3, 10, 13), "stone_dark")
    m.cube("rim_e", (13, 9, 3), (15, 10, 13), "stone_dark")
    m.cube("glass_water", (3, 3, 3), (13, 4, 13), "water")
    m.cube("post_l", (-2, 0, 7), (0, 26, 9), "wood_dark")
    m.cube("post_r", (16, 0, 7), (18, 26, 9), "wood_dark")
    m.cube("beam", (-3, 24, 6), (19, 25.5, 10), "wood")
    m.cube("roof_n", (-4, 26, 2), (20, 27.5, 8.2), "tile", rot=(-25, 0, 0), pivot=(8, 27, 8))
    m.cube("roof_s", (-4, 26, 7.8), (20, 27.5, 14), "tile", rot=(25, 0, 0), pivot=(8, 27, 8))
    m.cube("pulley", (7, 21, 7), (9, 24, 9), "iron_black")
    m.group("bucket", (8, 8, 8))
    m.cube("rope", (7.8, 10, 7.8), (8.2, 21, 8.2), "rope", group="bucket")
    m.cyl("bucket", 8, 8, 6, 10, 2.1, "wood", sides=4, group="bucket")
    m.cube("tub", (18, 0, -6), (26, 4, 2), "wood")
    m.group("tubwater", (22, 0.5, -2))
    m.cube("glass_water", (18.5, 0.5, -5.5), (25.5, 3.6, 1.5), "water", group="tubwater")
    return m


def shogi_board():
    m = F("shogi_board")
    m.cube("board", (0, 4, 0), (16, 9, 16), "wood_light", decal=_grid)
    legs4(m, 1, 1, 15, 15, 4, "wood_dark", t=2)
    for i in range(18):
        row = 0 if i < 9 else 2
        col = i % 9
        px = 1.2 + col * 1.55
        pz = 1.2 + row * 1.55
        m.group(f"piece{i}", (px, 9, pz))
        m.cube("piece", (px, 9, pz), (px + 1.15, 9.6, pz + 1.3), "wood_light", group=f"piece{i}")
    m.cube("box", (17, 0, 2), (22, 2.5, 8), "wood_dark")
    for k in range(6):
        m.cube("loose", (17.3 + (k % 3) * 1.4, 2.5, 2.5 + (k // 3) * 2.5), (18.4 + (k % 3) * 1.4, 3.1, 3.8 + (k // 3) * 2.5),
               "wood_light")
    m.cube("cushion", (-6, 0, 3), (-1, 1.2, 13), "purple_cloth")
    return m


def _grid(draw, face, x0, y0, w, h):
    if face != "up":
        return
    for i in range(10):
        p = int(round((1 + i * 1.55) / 16 * w))
        draw.line([(x0 + p, y0 + 2), (x0 + p, y0 + h - 3)], fill=(40, 30, 20, 255))
        draw.line([(x0 + 2, y0 + p), (x0 + w - 3, y0 + p)], fill=(40, 30, 20, 255))


def kakejiku():
    m = F("kakejiku")
    m.cube("nail", (7.6, 30, 15.2), (8.4, 31, 16), "iron_black")
    m.group("scroll", (8, 30, 15))
    m.cube("rod", (1, 28, 14.6), (15, 29, 15.4), "wood_dark", group="scroll")
    m.cube("mount", (2, 6, 14.7), (14, 28, 15.1), "wood", group="scroll")
    m.cube("paper", (3.5, 9, 14.6), (12.5, 26, 14.7), "paper", group="scroll", decal=_ink)
    m.cube("roller", (0.5, 5, 14.4), (15.5, 6.2, 15.6), "wood_dark", group="scroll")
    m.cube("toko", (-2, 0, 9), (18, 2, 16), "wood_dark")
    m.cube("censer", (6, 2, 11), (10, 4, 14), "gold")
    return m


def _ink(draw, face, x0, y0, w, h):
    if face != "north":
        return
    # 墨絵の山と赤い日（北向きの面：右が西）
    draw.polygon([(x0 + 2, y0 + h - 3), (x0 + w * 0.35, y0 + h * 0.45), (x0 + w * 0.6, y0 + h - 3)], fill=(70, 70, 74, 255))
    draw.polygon([(x0 + w * 0.4, y0 + h - 3), (x0 + w * 0.7, y0 + h * 0.6), (x0 + w - 3, y0 + h - 3)], fill=(130, 130, 134, 255))
    draw.ellipse([x0 + w * 0.6, y0 + h * 0.18, x0 + w * 0.8, y0 + h * 0.28], fill=(200, 40, 36, 255))
    draw.line([(x0 + w * 0.2, y0 + 4), (x0 + w * 0.2, y0 + h * 0.3)], fill=(20, 20, 20, 255), width=2)


ALL = {
    "SPAWN": entrance_mat, "BELL": call_bell, "VENT": floor_hatch, "BREAKER": breaker, "GAS_VALVE": gas_valve,
    "BOILER": boiler, "MEETING": meeting_table, "HOKORA": hokora, "WIRING": wiring_panel, "LEDGER": ledger,
    "FUTON": futon, "KAMADO": kamado, "BATH": bath_stove, "CHOPPING": chopping_block, "FIELD": vegetable_patch,
    "DUST": dust_pile, "LAUNDRY": laundry_pole, "VASE": flower_vase, "SINK": kitchen_sink, "SHOE_RACK": shoe_rack,
    "SHOJI": shoji_screen, "ANDON": andon, "IRORI": irori, "BONSAI": bonsai, "TEA": tea_set, "WELL": well,
    "SHOGI": shogi_board, "KAKEJIKU": kakejiku,
}


# ================================================================ 第4弾のタスク
MATS.update({
    "letter": Mat((246, 240, 226), "paper", noise=4),
    "letter_pink": Mat((246, 214, 222), "paper", noise=4),
    "letter_blue": Mat((214, 230, 246), "paper", noise=4),
    "red_post": Mat((176, 38, 30), "metal", noise=5),
    "glass_bowl": Mat((225, 242, 255), "plain", noise=1, alpha=38),
    "water_clear": Mat((150, 200, 240), "plain", noise=2, alpha=60),
    "goldfish": Mat((236, 92, 30), "metal", noise=6),
    "goldfish_tail": Mat((246, 150, 90), "paper", noise=4, alpha=210),
    "food": Mat((150, 96, 50), "stipple", noise=8),
    "glass_chime": Mat((220, 240, 255), "water", accent=(250, 252, 255), alpha=150),
    "chime_paint": Mat((200, 60, 60), "plain", noise=4),
    "tanzaku": Mat((232, 214, 140), "paper", noise=4),
    "mochi": Mat((250, 248, 240), "plaster", noise=4),
    "sand": Mat((214, 206, 186), "stipple", noise=8),
    "sand_line": Mat((180, 170, 146), "plain", noise=4),
    "rock": Mat((112, 110, 104), "stone", noise=10),
    "soap": Mat((240, 246, 252), "stipple", noise=6, alpha=200),
    "washboard": Mat((200, 162, 108), "wood", accent=(164, 124, 76), noise=5),
    "cloth_blue": Mat((70, 96, 170), "stipple", noise=8),
})


def mailbox():
    m = F("mailbox")
    m.cube("post", (6.5, 0, 9), (9.5, 12, 12), "wood_dark")
    m.cube("box", (2, 12, 5), (14, 22, 13), "red_post")
    m.cube("roof", (1.4, 22, 4.4), (14.6, 23.2, 13.6), "red_post")
    m.cube("roof2", (2.4, 23.2, 5.4), (13.6, 24, 12.6), "red_post")
    m.cube("slot", (4.5, 19.5, 4.8), (11.5, 20.3, 5.1), "black")
    m.cube("plate", (5, 14.5, 4.8), (11, 17.5, 5.1), "white", decal=_text("郵便"))
    m.group("lid", (8, 21, 4.8))
    m.cube("lid", (4.2, 20.4, 4.4), (11.8, 21.6, 4.9), "red_post", group="lid")
    # 仕分ける前の手紙の山（台の上）と、仕分けた手紙（箱の中→少しのぞく）
    for k in range(4):
        m.group(f"pile{k}", (19, 10 + k * 0.5, 8))
        m.cube("letter", (17, 10 + k * 0.5, 6.5 + (k % 2) * 0.6), (21.5, 10.4 + k * 0.5, 9.5 + (k % 2) * 0.6),
               ("letter", "letter_pink", "letter_blue", "letter")[k], group=f"pile{k}", rot=(0, (k - 1.5) * 8, 0))
        m.group(f"sorted{k}", (5 + k * 2, 22, 9))
        m.cube("letter", (4.3 + k * 2, 21.6, 7.5), (5.6 + k * 2, 24.6, 10.5), ("letter", "letter_pink", "letter_blue", "letter")[k],
               group=f"sorted{k}")
    m.cube("stand", (16, 0, 5.5), (22.5, 10, 11), "wood")
    return m


def _text(chars, faces=("north",)):
    from items_hires import text_decal
    return text_decal(chars, color=(40, 30, 24, 255), faces=faces, vertical=False, size=0.5)


def goldfish_bowl():
    m = F("goldfish_bowl")
    m.cube("stand", (2, 0, 2), (14, 6, 14), "wood_dark")
    m.cube("stand_top", (1.4, 6, 1.4), (14.6, 7, 14.6), "wood_dark")
    # 金魚（鉢のまん中を中心にぐるぐる泳ぐ）
    for k in range(2):
        g = m.group(f"fish{k}", (8, 9.5 + k * 1.6, 8))
        r = 2.4 if k == 0 else 1.6
        y = 9.5 + k * 1.6
        m.cube("fish", (8 + r - 0.5, y - 0.6, 7.0), (8 + r + 0.5, y + 0.6, 9.0), "goldfish", group=g)
        m.cube("tail", (8 + r - 0.05, y - 0.8, 9.0), (8 + r + 0.05, y + 0.8, 10.4), "goldfish_tail", group=g)
        m.cube("eye", (8 + r - 0.55, y + 0.1, 7.2), (8 + r + 0.55, y + 0.4, 7.5), "black", group=g)
    # 水とガラスは重ねると濁って見えるので、重ならない形で作る
    #  水：上から見て丸くなるよう、細い帯を横に並べる（帯どうしは重ならない）
    import math as _m
    R = 4.6
    n = 14
    for k in range(n):
        x0 = -R + 2 * R * k / n
        x1 = -R + 2 * R * (k + 1) / n
        xc = max(abs(x0), abs(x1))
        half = _m.sqrt(max(0.0, R * R - xc * xc))
        if half > 0.1:
            skip = (("west",) if k > 0 else ()) + (("east",) if k < n - 1 else ())   # 帯のあいだの面は描かない
            m.cube("glass_water", (8 + x0, 7.4, 8 - half), (8 + x1, 13.2, 8 + half), "water_clear", skip=skip)
    #  ガラス：うすい板をぐるっと並べた殻（輪切りごと）
    from items_hires import ring
    for (y0, y1, r) in ((7, 8, 3.8), (8, 10, 5.1), (10, 12.6, 5.4), (12.6, 14, 4.8), (14, 14.8, 3.8)):
        ring(m, "glass_bowl", 8, (y0 + y1) / 2, 8, r, 0.12, y1 - y0, "glass_bowl", seg=18, plane="xz")
    m.cube("weed", (5.5, 7.4, 9), (6.3, 12, 9.8), "green_cloth", rot=(0, 0, 8), pivot=(6, 7.4, 9.4))
    m.cube("pebbles", (5, 7.2, 5), (11, 7.8, 11), "rock")
    # えさ（上から落ちる）
    m.group("food", (8, 16, 8))
    for k in range(5):
        m.cube("food", (7 + (k % 3) * 0.8, 15.5 + (k // 3) * 0.7, 7.3 + (k % 2) * 1.0), (7.4 + (k % 3) * 0.8, 15.9 + (k // 3) * 0.7, 7.7 + (k % 2) * 1.0),
               "food", group="food")
    m.cube("can", (14, 7, 3), (17, 11, 6), "yellow_cloth")
    return m


def wind_chime():
    m = F("wind_chime")
    m.cube("post_l", (0, 0, 13), (2, 26, 15), "wood_dark")
    m.cube("post_r", (14, 0, 13), (16, 26, 15), "wood_dark")
    m.cube("beam", (-1, 24, 12), (17, 26, 16), "wood_dark")
    m.cube("hook", (7.6, 21.5, 13.6), (8.4, 24, 14.4), "iron_black")
    m.cube("shelf", (1, 0, 3), (15, 4, 9), "wood")
    m.cube("box", (4, 4, 4), (11, 7, 8), "wood_light")
    # 吊るした風鈴（終わると現れる）と、箱の中の風鈴
    m.group("hung", (8, 21.5, 14))
    m.cube("string", (7.85, 17, 13.85), (8.15, 21.5, 14.15), "white", group="hung")
    for (y0, y1, r) in ((14.2, 15.2, 1.7), (15.2, 16.4, 1.5), (16.4, 17.2, 0.9)):
        m.cyl("glass_bell", 8, 14, y0, y1, r, "glass_chime", sides=4, group="hung")
    m.cube("paint", (6.4, 14.6, 12.4), (9.6, 15.6, 12.6), "chime_paint", group="hung")
    m.cube("clapper", (7.8, 11, 13.8), (8.2, 14.6, 14.2), "white", group="hung")
    m.cube("tanzaku", (7, 6, 13.95), (9, 11, 14.05), "tanzaku", group="hung")
    m.group("boxed", (7.5, 7, 6))
    m.cyl("glass_bell", 7.5, 6, 7, 8.6, 1.5, "glass_chime", sides=4, group="boxed")
    m.cube("tanzaku", (8.5, 7, 4.5), (11, 7.3, 6.5), "tanzaku", group="boxed")
    return m


def mochi_mortar():
    m = F("mochi_mortar")
    m.cyl("usu", 8, 8, 0, 9, 6.2, "wood", sides=6)
    m.cyl("usu_rim", 8, 8, 9, 10, 6.4, "wood_dark", sides=6)
    m.cyl("bowl", 8, 8, 9.2, 10.05, 4.6, "wood_dark", sides=6)
    m.group("mochi", (8, 10, 8))
    m.cyl("mochi", 8, 8, 9.6, 11.6, 3.6, "mochi", sides=6, group="mochi")
    m.cyl("mochi_top", 8, 8, 11.6, 12.4, 2.6, "mochi", sides=4, group="mochi")
    # 杵（きね）：手前に立てかけて、つくときに振り下ろす
    m.group("kine", (8, 12, 18), rot=(-35, 0, 0))
    m.cube("handle", (7.4, 12, 10), (8.6, 13.2, 22), "wood", group="kine")
    m.cyl("head", 8, 10, 10.5, 16.5, 2.4, "wood_dark", sides=4, group="kine")
    m.cube("bucket", (17, 0, 2), (22, 5, 7), "wood")
    m.cube("glass_water", (17.5, 0.5, 2.5), (21.5, 4.4, 6.5), "water")
    return m


def zen_garden():
    m = F("zen_garden")
    m.cube("frame_n", (-4, 0, -4), (20, 2, -3), "wood_dark")
    m.cube("frame_s", (-4, 0, 19), (20, 2, 20), "wood_dark")
    m.cube("frame_w", (-4, 0, -3), (-3, 2, 19), "wood_dark")
    m.cube("frame_e", (19, 0, -3), (20, 2, 19), "wood_dark")
    m.cube("sand", (-3, 0, -3), (19, 1.2, 19), "sand")
    m.cube("rock1", (2, 1.2, 3), (6, 4.5, 6.5), "rock", rot=(0, 20, 0), pivot=(4, 1.2, 4.7))
    m.cube("rock2", (11, 1.2, 10), (14, 3, 12.5), "moss_stone")
    m.cube("rock3", (12.5, 1.2, 2), (14, 2.2, 3.5), "rock")
    # 砂紋（進むと1本ずつ現れる）
    for k in range(6):
        z = -1.5 + k * 3.4
        m.group(f"line{k}", (8, 1.2, z))
        m.cube("ripple", (-2.5, 1.2, z - 0.25), (18.5, 1.32, z + 0.25), "sand_line", group=f"line{k}")
    # 熊手（作業中に左右へ動く）
    m.group("rake", (8, 1.2, 8))
    m.cube("rake_head", (6, 1.3, 7.5), (10, 2.2, 8.3), "bamboo", group="rake")
    for k in range(5):
        m.cube("tine", (6.2 + k * 0.85, 1.2, 7.3), (6.5 + k * 0.85, 1.5, 7.7), "bamboo", group="rake")
    m.cube("rake_handle", (7.6, 1.8, 8.3), (8.4, 2.6, 22), "bamboo", group="rake", rot=(-20, 0, 0), pivot=(8, 2.2, 8.3))
    return m


def washtub():
    m = F("washtub")
    m.cyl("tub", 8, 8, 0, 5, 6.6, "wood", sides=6)
    m.cyl("hoop", 8, 8, 1.2, 2, 6.8, "steel", sides=6)
    m.cyl("hoop2", 8, 8, 3.6, 4.4, 6.8, "steel", sides=6)
    m.cyl("glass_water", 8, 8, 0.5, 4.2, 6.0, "water", sides=6)
    m.cube("washboard", (3, 1, 9), (13, 14, 10), "washboard", rot=(-25, 0, 0), pivot=(8, 1, 9.5), decal=_ridges)
    m.group("cloth", (8, 7, 9))
    m.cube("cloth", (4, 5.5, 7.6), (12, 9.5, 8.6), "cloth_blue", group="cloth", rot=(-25, 0, 0), pivot=(8, 7, 8))
    m.group("bubbles", (8, 4.4, 8))
    for k, (x, z, r) in enumerate(((5, 5, 1.0), (10, 6, 0.8), (7, 11, 1.2), (11, 10, 0.7), (4.5, 9, 0.6))):
        m.cube("soap", (x - r, 4.2, z - r), (x + r, 4.2 + r * 1.4, z + r), "soap", group="bubbles")
    m.cube("bar", (13.5, 0, 13), (16.5, 1.4, 15), "white")
    return m


def _ridges(draw, face, x0, y0, w, h):
    if face not in ("north", "south"):
        return
    for k in range(4, h - 4, 4):
        draw.line([(x0 + 2, y0 + k), (x0 + w - 3, y0 + k)], fill=(150, 112, 66, 255), width=1)


# ================================================================ 飾りの家具（タスクなし）
MATS.update({
    "kiri": Mat((214, 180, 132), "wood", accent=(186, 150, 104), noise=5),
    "iron_fitting": Mat((40, 38, 40), "metal", noise=5),
    "lacquer_black": Mat((28, 24, 24), "metal", noise=4),
    "gold_leaf": Mat((222, 182, 86), "metal", noise=8),
    "byobu_paint": Mat((236, 222, 176), "paper", noise=4),
    "stone_lantern": Mat((150, 148, 140), "stone", noise=10),
    "glow_light": Mat((255, 214, 130), "paper", noise=4, glow=True),
    "sake_wood": Mat((214, 186, 140), "wood", accent=(186, 156, 112), noise=5),
    "straw": Mat((206, 180, 110), "rope", accent=(170, 146, 80)),
    "ceramic": Mat((160, 114, 70), "plaster", noise=8),
    "ceramic_dark": Mat((96, 66, 44), "plaster", noise=8),
    "tanuki": Mat((128, 92, 60), "plaster", noise=8),
    "tanuki_belly": Mat((216, 190, 150), "plaster", noise=6),
    "neko_white": Mat((246, 244, 238), "plaster", noise=4),
    "umbrella_red": Mat((190, 40, 36), "paper", noise=5),
    "umbrella_blue": Mat((50, 70, 130), "paper", noise=5),
    "zabuton": Mat((150, 60, 90), "stipple", noise=6),
    "pottery": Mat((70, 90, 120), "plaster", noise=8),
})


def tansu():
    m = F("tansu")
    m.cube("body", (0.5, 0, 3), (15.5, 26, 15), "kiri")
    for k, (y0, y1) in enumerate(((1, 7), (7.5, 13.5), (14, 20), (20.5, 25.5))):
        m.cube("drawer", (1.2, y0, 2.6), (14.8, y1, 3.1), "kiri")
        for x in (4, 12):
            m.cube("pull", (x - 1.2, (y0 + y1) / 2 - 0.4, 2.1), (x + 1.2, (y0 + y1) / 2 + 0.4, 2.6), "iron_fitting")
    for (x, y) in ((0.3, 0), (0.3, 25), (14.7, 0), (14.7, 25)):
        m.cube("corner", (x, y, 2.4), (x + 1, y + 1, 3.2), "iron_fitting")
    m.cube("vase", (10, 26, 7), (13, 30, 10), "pottery")
    m.cube("box", (2, 26, 6), (7, 28, 11), "lacquer_black")
    return m


def chabudai():
    m = F("chabudai")
    m.cyl("top", 8, 8, 6, 7.2, 8.5, "wood", sides=6)
    m.cyl("rim", 8, 8, 5.4, 6.0, 8.2, "wood_dark", sides=6)
    for (x, z) in ((3, 3), (12, 3), (3, 12), (12, 12)):
        m.cube("leg", (x, 0, z), (x + 1.4, 5.4, z + 1.4), "wood_dark")
    m.cyl("teapot", 6, 7, 7.2, 10, 1.8, "ceramic_dark", sides=4)
    m.cube("spout", (7.6, 8.4, 6.6), (9.4, 9.0, 7.4), "ceramic_dark")
    for (x, z) in ((10, 5), (10.5, 10)):
        m.cyl("cup", x, z, 7.2, 8.8, 0.9, "white", sides=4)
    for (x0, z0, x1, z1) in ((-6, 3, -1, 13), (17, 3, 22, 13), (3, -6, 13, -1)):
        m.cube("zabuton", (x0, 0, z0), (x1, 1.4, z1), "zabuton")
    return m


def byobu():
    m = F("byobu")
    for k in range(4):
        x0 = -6 + k * 7
        ang = 18 if k % 2 == 0 else -18
        m.cube("panel", (x0, 0.5, 9), (x0 + 7, 22, 9.6), "byobu_paint", rot=(0, ang, 0), pivot=(x0 + 3.5, 0, 9.3),
               decal=_byobu_art(k))
        m.cube("frame", (x0, 22, 8.8), (x0 + 7, 23, 9.8), "lacquer_black", rot=(0, ang, 0), pivot=(x0 + 3.5, 0, 9.3))
        m.cube("foot", (x0, 0, 8.8), (x0 + 7, 0.6, 9.8), "lacquer_black", rot=(0, ang, 0), pivot=(x0 + 3.5, 0, 9.3))
    return m


def _byobu_art(k):
    def decal(draw, face, x0, y0, w, h):
        if face != "north":
            return
        draw.rectangle([x0, y0, x0 + w - 1, y0 + int(h * 0.25)], fill=(222, 182, 86, 255))     # 金雲
        draw.ellipse([x0 - w * 0.3 + k * 3, y0 + h * 0.15, x0 + w * 0.7 + k * 3, y0 + h * 0.35], fill=(232, 196, 100, 255))
        # 松の枝と、遠くの山
        draw.polygon([(x0, y0 + h - 1), (x0 + w * 0.5, y0 + h * (0.55 + 0.05 * k)), (x0 + w, y0 + h - 1)], fill=(110, 130, 120, 255))
        if k in (1, 2):
            draw.line([(x0, y0 + h * 0.45), (x0 + w, y0 + h * 0.4)], fill=(70, 50, 30, 255), width=3)
            for j in range(3):
                cx = x0 + w * (0.2 + 0.3 * j)
                draw.ellipse([cx - 6, y0 + h * 0.36, cx + 6, y0 + h * 0.44], fill=(50, 96, 60, 255))
    return decal


def tourou():
    m = F("tourou")
    m.cube("base", (3, 0, 3), (13, 2, 13), "stone_lantern")
    m.cyl("pillar", 8, 8, 2, 12, 1.8, "stone_lantern", sides=4)
    m.cube("platform", (3.5, 12, 3.5), (12.5, 14, 12.5), "stone_lantern")
    m.cube("firebox", (4.5, 14, 4.5), (11.5, 20, 11.5), "stone_lantern")
    m.cube("glow_window", (6, 15.2, 4.3), (10, 18.8, 11.7), "glow_light")
    m.cube("glow_window2", (4.3, 15.2, 6), (11.7, 18.8, 10), "glow_light")
    m.cube("roof", (2, 20, 2), (14, 22, 14), "stone_lantern")
    m.cube("roof2", (4, 22, 4), (12, 23.5, 12), "stone_lantern")
    m.cube("jewel", (6.8, 23.5, 6.8), (9.2, 26, 9.2), "stone_lantern")
    m.group("flame", (8, 15.5, 8))
    m.cube("glow_flame", (7.4, 15.2, 7.4), (8.6, 17.4, 8.6), "glow_fire", group="flame")
    return m


def sake_barrel():
    m = F("sake_barrel")
    m.cyl("barrel", 8, 8, 0, 16, 6.8, "sake_wood", sides=6)
    m.cyl("straw", 8, 8, 2, 14, 7.0, "straw", sides=6)
    m.cyl("rope", 8, 8, 3, 4, 7.2, "rope", sides=6)
    m.cyl("rope2", 8, 8, 12, 13, 7.2, "rope", sides=6)
    m.cube("label", (4, 5, 0.8), (12, 11.5, 1.2), "white", decal=_text("祝", faces=("north",)))
    m.cyl("lid", 8, 8, 16, 16.6, 6.4, "sake_wood", sides=6)
    m.cube("ladle", (11, 16.6, 6), (12, 17.4, 14), "bamboo")
    return m


def kasa_tate():
    m = F("kasa_tate")
    m.cyl("pot", 8, 8, 0, 9, 4.2, "ceramic", sides=6)
    m.cyl("rim", 8, 8, 9, 10, 4.4, "ceramic_dark", sides=6)
    for k, (dx, dz, ang, mat) in enumerate(((-1.2, -1, -8, "umbrella_red"), (1.4, 0.6, 10, "umbrella_blue"), (0, 1.6, 2, "umbrella_red"))):
        m.cyl("umbrella", 8 + dx, 8 + dz, 3, 22, 1.3, mat, sides=4)
        m.cube("tip", (7.7 + dx, 22, 7.7 + dz), (8.3 + dx, 25, 8.3 + dz), "bamboo")
    return m


def tanuki():
    m = F("tanuki")
    m.cyl("feet", 8, 8, 0, 2, 4.6, "tanuki", sides=6)
    m.cyl("belly", 8, 8, 2, 11, 5.0, "tanuki", sides=6)
    m.cube("belly_front", (5, 3, 3), (11, 10, 3.6), "tanuki_belly")
    m.cyl("chest", 8, 8, 11, 14, 4.0, "tanuki", sides=6)
    m.cyl("head", 8, 8, 14, 20, 3.6, "tanuki", sides=6)
    m.cube("face", (5.5, 15, 4.2), (10.5, 18.5, 4.8), "tanuki_belly")
    m.cube("nose", (7.3, 16.2, 3.8), (8.7, 17.2, 4.4), "black")
    m.cube("eye", (5.8, 17.8, 4.0), (7.0, 18.6, 4.6), "black")
    m.cube("eye", (9.0, 17.8, 4.0), (10.2, 18.6, 4.6), "black")
    m.cube("ear", (5, 19.6, 6), (6.5, 21, 8), "tanuki")
    m.cube("ear", (9.5, 19.6, 6), (11, 21, 8), "tanuki")
    m.cube("hat", (3.5, 20, 4), (12.5, 21, 12), "straw")
    m.cube("hat_top", (5.5, 21, 6), (10.5, 23, 10), "straw")
    m.cube("bottle", (12, 3, 5), (15, 9, 8), "ceramic_dark")
    m.cube("ledger_book", (1.5, 4, 5), (4, 9, 9), "white")
    return m


def maneki_neko():
    m = F("maneki_neko")
    m.cube("cushion", (2, 0, 2), (14, 2, 14), "red_cloth")
    m.cyl("body", 8, 8.5, 2, 11, 4.2, "neko_white", sides=6)
    m.cyl("head", 8, 8, 11, 17.5, 3.8, "neko_white", sides=6)
    m.cube("ear", (4.6, 17, 6.5), (6.4, 19.4, 8.5), "neko_white")
    m.cube("ear_in", (5.0, 17.3, 6.3), (6.0, 18.8, 6.5), "pink_cloth")
    m.cube("ear", (9.6, 17, 6.5), (11.4, 19.4, 8.5), "neko_white")
    m.cube("ear_in", (10.0, 17.3, 6.3), (11.0, 18.8, 6.5), "pink_cloth")
    m.cube("eye", (5.8, 14.6, 4.1), (7.0, 15.2, 4.4), "black")
    m.cube("eye", (9.0, 14.6, 4.1), (10.2, 15.2, 4.4), "black")
    m.cube("nose", (7.5, 13.6, 4.1), (8.5, 14.2, 4.4), "pink_cloth")
    m.cube("collar", (4.2, 10.5, 4.2), (11.8, 11.4, 12.4), "red_cloth")
    m.cube("bell", (7.2, 9.4, 3.8), (8.8, 11, 4.6), "gold")
    m.cube("koban", (5, 4, 3.8), (11, 8, 4.6), "gold", decal=_text("千万両"))
    m.cube("paw_l", (3.4, 6, 5), (5.2, 9, 7), "neko_white")
    # 招く手（ゆっくり手まねき）
    m.group("paw", (11.5, 11, 7))
    m.cube("arm", (10.5, 11, 6), (12.6, 17, 8), "neko_white", group="paw")
    m.cube("pad", (10.8, 16, 5.8), (12.3, 17, 6.1), "pink_cloth", group="paw")
    return m


def hibachi():
    m = F("hibachi")
    m.cyl("pot", 8, 8, 0, 9, 6.2, "ceramic", sides=6)
    m.cyl("rim", 8, 8, 9, 10, 6.4, "ceramic_dark", sides=6)
    m.cyl("ash", 8, 8, 6, 9.6, 5.4, "ash", sides=6)
    m.group("ember", (8, 9.6, 8))
    for (x, z) in ((6, 6), (9, 7), (7, 10), (10, 10)):
        m.cube("charcoal", (x - 1, 9.6, z - 0.8), (x + 1, 10.6, z + 0.8), "charcoal")
        m.cube("glow_ember", (x - 0.8, 10.5, z - 0.6), (x + 0.8, 10.7, z + 0.6), "ember", group="ember")
    m.cube("tongs", (11, 10, 4), (11.4, 10.4, 14), "iron_black", rot=(0, 0, 20), pivot=(11, 10, 9))
    m.cube("tongs2", (11.8, 10, 4), (12.2, 10.4, 14), "iron_black", rot=(0, 0, 20), pivot=(11, 10, 9))
    m.cyl("kettle", 6.5, 7.5, 10.6, 14, 2.6, "cast_iron", sides=6)
    return m


def tsubo():
    m = F("tsubo")
    m.cube("stand", (2, 0, 2), (14, 3, 14), "lacquer_black")
    for (y0, y1, r) in ((3, 4, 3.0), (4, 7, 4.4), (7, 12, 5.2), (12, 15, 4.6), (15, 17, 3.0), (17, 19, 2.2), (19, 20, 2.8)):
        m.cyl("pot", 8, 8, y0, y1, r, "pottery", sides=6)
    m.cube("paint", (5, 9, 2.7), (11, 11, 3.0), "white", decal=None)
    return m


NEW_TASKS = {"MAILBOX": mailbox, "GOLDFISH": goldfish_bowl, "FURIN": wind_chime, "MOCHI": mochi_mortar,
             "ZEN": zen_garden, "WASHTUB": washtub}
DECOR = {"TANSU": tansu, "CHABUDAI": chabudai, "BYOBU": byobu, "TOUROU": tourou, "SAKE": sake_barrel,
         "KASA": kasa_tate, "TANUKI": tanuki, "MANEKI": maneki_neko, "HIBACHI": hibachi, "TSUBO": tsubo}
ALL.update(NEW_TASKS)
ALL.update(DECOR)
