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


def F(name):
    return HiRes(name, density=3, mc=True)


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
    m.cube("top", (5.5, 23, 5), (10.5, 24.5, 10), "copper_old", group="bell")
    m.cube("body", (4, 13, 3.5), (12, 23, 11.5), "copper_old", group="bell")
    m.cube("band", (3.8, 17, 3.3), (12.2, 18, 11.7), "copper", group="bell")
    m.cube("lip", (3, 12, 2.5), (13, 14, 12.5), "copper", group="bell")
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
    m.cube("tank", (2, 0, 2), (14, 26, 14), "steel_light", group="body")
    m.cube("tank_x", (1, 2, 3), (15, 24, 13), "steel_light", group="body")
    m.cube("tank_z", (3, 2, 1), (13, 24, 15), "steel_light", group="body")
    m.cube("band1", (1, 8, 1), (15, 9, 15), "copper", group="body")
    m.cube("band2", (1, 18, 1), (15, 19, 15), "copper", group="body")
    m.cube("cap", (4, 26, 4), (12, 28, 12), "steel", group="body")
    m.cube("chimney", (6, 28, 6), (10, 34, 10), "iron_black", group="body")
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
        m.cube("cup", (x, 8, z), (x + 2.5, 10.5, z + 2.5), "white")
        m.cube("tea", (x + 0.4, 10.2, z + 0.4), (x + 2.1, 10.4, z + 2.1), "matcha")
    m.cube("pot", (14, 8, 14), (19, 12, 19), "copper")
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
    m.cube("body", (-4, 0, 2), (20, 11, 15), "plaster")
    m.cube("base", (-4.4, 0, 1.6), (20.4, 1.4, 15.4), "clay")
    m.cube("top", (-4.4, 10.6, 1.6), (20.4, 12, 15.4), "clay")
    for x in (-1, 11):
        m.cube("mouth", (x, 2, 1.7), (x + 6, 7, 2.1), "charcoal")
        m.cube("arch", (x - 0.4, 7, 1.5), (x + 6.4, 7.8, 2.1), "stone_dark")
    m.group("fire", (8, 4, 1.6))
    for x in (-1, 11):
        m.cube("glow_fire", (x + 0.6, 2.2, 1.5), (x + 5.4, 6.4, 1.7), "glow_fire", group="fire")
    m.cube("pot", (-2, 12, 4), (7, 17, 13), "iron_black")
    m.cube("brim", (-3, 14, 3), (8, 15, 14), "iron_black")
    m.group("lid", (2.5, 17, 8.5))
    m.cube("lid", (-1.5, 17, 4.5), (6.5, 18, 12.5), "wood", group="lid")
    m.cube("knob", (1.5, 18, 7.5), (3.5, 19.3, 9.5), "wood_dark", group="lid")
    m.cube("pan", (10, 12, 5), (17, 16, 12), "copper")
    m.cube("pan_rim", (9.5, 15, 4.5), (17.5, 16, 12.5), "copper")
    m.cube("pan_lid", (10.5, 16, 5.5), (16.5, 16.8, 11.5), "wood")
    m.cube("log1", (-3, 0, -3), (5, 2, -1), "wood_dark")
    m.cube("log2", (0, 2, -3), (7, 3.8, -1.2), "wood")
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
    m.cube("pail", (19, 0, 1), (23, 4, 5), "wood")
    m.cube("ladle", (20.5, 4, 2.5), (21.5, 7, 3.5), "bamboo")
    return m


def chopping_block():
    m = F("chopping_block")
    m.cube("stump", (2, 0, 2), (14, 8, 14), "wood")
    m.group("half_l", (5, 8, 8))
    m.cube("log", (5, 8, 5), (8, 16, 11), "wood_light", group="half_l")
    m.group("half_r", (11, 8, 8))
    m.cube("log", (8, 8, 5), (11, 16, 11), "wood_light", group="half_r")
    m.group("axe", (21, 9, 8))
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
    m.cube("vase", (5, 4, 5), (11, 12, 11), "sky_cloth")
    m.cube("belly", (4.5, 6, 4.5), (11.5, 10, 11.5), "sky_cloth")
    m.cube("neck", (6, 12, 6), (10, 15, 10), "sky_cloth")
    m.cube("lip", (5.5, 15, 5.5), (10.5, 16, 10.5), "white")
    m.group("flowers", (8, 15, 8))
    heads = ("pink_cloth", "red_cloth", "yellow_cloth")
    for i, (x, z) in enumerate(((7.2, 7.2), (8.6, 8.4), (7.6, 9.2))):
        ln = 9 + i * 2
        m.cube("stem", (x - 0.3, 15, z - 0.3), (x + 0.3, 15 + ln, z + 0.3), "green_cloth", group="flowers",
               rot=(0, 0, (i - 1) * 12), pivot=(x, 15, z))
        m.cube("head", (x - 1.8 + (i - 1) * 2, 15 + ln - 0.5, z - 1.8), (x + 1.8 + (i - 1) * 2, 15 + ln + 2, z + 1.8),
               heads[i], group="flowers")
    m.group("can", (16, 22, 8))
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
    m.cube("pot", (11, 22.5, 8), (14, 26, 11), "sky_cloth")
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
    m.cube("base", (3, 0, 3), (13, 1.5, 13), "wood_dark")
    for (x, z) in ((3, 3), (12, 3), (3, 12), (12, 12)):
        m.cube("post", (x, 1.5, z), (x + 1, 20, z + 1), "wood_dark")
    m.cube("top", (3, 19.5, 3), (13, 20.5, 13), "wood_dark")
    m.cube("shelf", (3, 9, 3), (13, 9.6, 13), "wood_dark")
    m.cube("dish", (6.5, 9.6, 6.5), (9.5, 10.3, 9.5), "iron_black")
    m.group("flame", (8, 10.3, 8))
    m.cube("glow_flame", (7.3, 10.3, 7.3), (8.7, 13.5, 8.7), "glow_fire", group="flame")
    m.group("paper_off", (8, 15, 8))
    m.cube("paper", (4, 10, 3.8), (12, 19.5, 12.2), "paper", group="paper_off")
    m.group("paper_on", (8, 15, 8))
    m.cube("glow_paper", (4, 10, 3.8), (12, 19.5, 12.2), "paper_lit", group="paper_on")
    m.cube("handle_l", (7.5, 20.5, 3.5), (8.5, 24, 4.5), "wood_dark")
    m.cube("handle_r", (7.5, 20.5, 11.5), (8.5, 24, 12.5), "wood_dark")
    m.cube("handle_t", (7.5, 23.5, 3.5), (8.5, 24.5, 12.5), "wood_dark")
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
    m.cube("kettle", (4.5, 6, 4.5), (11.5, 11, 11.5), "iron_black")
    m.cube("kettle_top", (5.5, 11, 5.5), (10.5, 12, 10.5), "iron_black")
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
    m.group("blade_a", (16, 14, 8))
    m.cube("blade", (13, 14, 7.6), (19, 14.6, 8.4), "steel_light", group="blade_a")
    m.group("blade_b", (16, 14, 8))
    m.cube("blade", (13, 13.4, 7.6), (19, 14, 8.4), "steel_light", group="blade_b")
    m.cube("grip", (18.5, 12.5, 7.5), (21, 15.5, 8.5), "red_cloth")
    return m


def tea_set():
    m = F("tea_set")
    m.cube("mat", (-4, 0, -2), (20, 0.6, 18), "red_cloth")
    m.cube("brazier", (-2, 0.6, 9), (6, 6, 17), "iron_black")
    m.cube("kama", (-1, 6, 10), (5, 10, 16), "copper")
    m.cube("kama_lid", (0, 10, 11), (4, 11, 15), "copper_old")
    m.cube("bowl", (8, 0.6, 2), (15, 5, 9), "lacquer")
    m.cube("bowl_rim", (7.5, 3, 1.5), (15.5, 5.2, 9.5), "lacquer")
    m.cube("tea", (8.5, 3.5, 2.5), (14.5, 4.6, 8.5), "matcha")
    m.group("foam", (11.5, 4.6, 5.5))
    m.cube("foam", (8.6, 4.6, 2.6), (14.4, 5.2, 8.4), "foam", group="foam")
    m.group("whisk", (11.5, 5, 5.5))
    m.cube("tines", (10.7, 4, 4.7), (12.3, 7, 6.3), "bamboo", group="whisk")
    m.cube("handle", (11.2, 7, 5.2), (11.8, 12, 5.8), "bamboo", group="whisk")
    m.cube("caddy", (16, 0.6, 3), (19, 4, 6), "lacquer")
    m.cube("scoop", (15, 0.6, 9), (20, 1, 9.6), "bamboo")
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
    m.cube("bucket", (6, 6, 6), (10, 10, 10), "wood", group="bucket")
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
