"""
家具の作り直し（第2版）。furniture_hires.py の同じ名前の家具をこちらで置きかえる。
動かす部分のグループ名は前と同じ（Java の FurnitureGeo・furniture_anims.py と合わせる）。
"""
import math

from hires import HiRes, MATS, Mat

MATS.update({
    # 玄関
    "igusa": Mat((190, 176, 116), "bamboo", accent=(160, 146, 88), noise=4),
    "mat_edge": Mat((52, 46, 70), "plain", noise=4),
    "granite": Mat((150, 148, 142), "stone", noise=8),
    "granite_dark": Mat((112, 110, 106), "stone", noise=8),
    "kiri_light": Mat((222, 196, 150), "wood", accent=(196, 166, 118), noise=4),
    "hanao_red": Mat((178, 34, 40), "stipple", noise=6),
    "hanao_navy": Mat((40, 52, 96), "stipple", noise=6),
    # 鐘・金物
    "bronze": Mat((112, 120, 92), "metal", accent=(80, 110, 90), noise=12),
    "bronze_dark": Mat((76, 84, 66), "metal", noise=10),
    "kawara": Mat((70, 74, 82), "tile", accent=(46, 48, 54), noise=6),
    "hinoki": Mat((214, 176, 128), "wood", accent=(186, 146, 98), noise=4),
    "hinoki_dark": Mat((120, 84, 52), "wood", accent=(92, 62, 36), noise=5),
    "shimenawa": Mat((206, 182, 120), "rope", accent=(168, 142, 86)),
    # 床下
    "floor_board": Mat((156, 112, 68), "wood", accent=(124, 84, 48), noise=5),
    "deep_dark": Mat((14, 12, 12), "plain", noise=2),
    "iron_fit": Mat((44, 42, 44), "metal", noise=5),
    # 配電盤
    "cabinet": Mat((150, 156, 150), "metal", noise=5),
    "cabinet_dark": Mat((96, 102, 98), "metal", noise=5),
    "panel_black": Mat((32, 34, 36), "polymer", noise=3),
    "switch_white": Mat((236, 234, 226), "polymer", noise=2),
    "conduit": Mat((120, 124, 120), "metal", noise=5),
    "warn_yellow": Mat((240, 196, 40), "plain", noise=3),
    "lever_red": Mat((196, 40, 32), "polymer", noise=3),
})


def F(name, d=6):
    return HiRes(name, density=d, real=True)


def cylx(m, name, cy, cz, x0, x1, r, mat, sides=4, group=None):
    """横に寝かせた丸太（X 方向）"""
    half_w = r * math.tan(math.radians(90.0 / sides))
    for k in range(sides):
        ang = k * (180.0 / sides)
        if ang > 90:
            ang -= 180
        e = k * 0.015
        m.cube(name, (x0 + e, cy - half_w, cz - r), (x1 - e, cy + half_w, cz + r), mat,
               rot=[ang, 0, 0] if ang else None, pivot=[x0, cy, cz], group=group)


def cylz(m, name, cx, cy, z0, z1, r, mat, sides=4, group=None):
    """横に寝かせた丸太（Z 方向）"""
    half_w = r * math.tan(math.radians(90.0 / sides))
    for k in range(sides):
        ang = k * (180.0 / sides)
        if ang > 90:
            ang -= 180
        e = k * 0.015
        m.cube(name, (cx - r, cy - half_w, z0 + e), (cx + r, cy + half_w, z1 - e), mat,
               rot=[0, 0, ang] if ang else None, pivot=[cx, cy, z0], group=group)


# ---------------------------------------------------------------- 凹凸をつける道具

def bevel(m, name, frm, to, mat, b=0.3, group=None, rot=None, pivot=None, decal=None):
    """角を面取りした箱（3つの箱を重ねて、12本の辺をすべて落とす）"""
    x0, y0, z0 = frm
    x1, y1, z1 = to
    b = min(b, (x1 - x0) / 3, (y1 - y0) / 3, (z1 - z0) / 3)
    piv = pivot or [(x0 + x1) / 2, (y0 + y1) / 2, (z0 + z1) / 2]
    kw = dict(group=group, rot=rot, pivot=piv if rot else None)
    m.cube(name, (x0 + b, y0, z0 + b), (x1 - b, y1, z1 - b), mat, decal=decal, **kw)
    m.cube(name, (x0, y0 + b, z0 + b), (x1, y1 - b, z1 - b), mat, decal=decal, **kw)
    m.cube(name, (x0 + b, y0 + b, z0), (x1 - b, y1 - b, z1), mat, decal=decal, **kw)


def rough(m, name, frm, to, mat, rng, lumps=6, depth=0.35, group=None):
    """ごつごつした石：少し小さい芯に、ふくらみを散らす"""
    x0, y0, z0 = frm
    x1, y1, z1 = to
    bevel(m, name, frm, to, mat, b=min(0.6, (y1 - y0) / 3), group=group)
    for _ in range(lumps):
        w = rng.uniform(0.25, 0.5) * (x1 - x0)
        d = rng.uniform(0.25, 0.5) * (z1 - z0)
        cx = rng.uniform(x0 + w / 2, x1 - w / 2)
        cz = rng.uniform(z0 + d / 2, z1 - d / 2)
        h = rng.uniform(0.3, 1.0) * depth
        m.cube(name, (cx - w / 2, y1 - 0.05, cz - d / 2), (cx + w / 2, y1 + h, cz + d / 2), mat)
    # 横にも少しでこぼこ
    for _ in range(lumps // 2):
        side = rng.choice((0, 1))
        y = rng.uniform(y0 + 0.2, y1 - 0.4)
        hh = rng.uniform(0.3, 0.6) * (y1 - y0)
        if side == 0:
            cx = rng.uniform(x0 + 0.5, x1 - 0.5)
            zz = rng.choice((z0, z1))
            m.cube(name, (cx - 0.8, y, zz - depth * 0.5), (cx + 0.8, min(y1, y + hh), zz + depth * 0.5), mat)
        else:
            cz = rng.uniform(z0 + 0.5, z1 - 0.5)
            xx = rng.choice((x0, x1))
            m.cube(name, (xx - depth * 0.5, y, cz - 0.8), (xx + depth * 0.5, min(y1, y + hh), cz + 0.8), mat)


def boards(m, name, x0, x1, z0, z1, y0, y1, n, mat, rng, along="z", gap=0.18, group=None):
    """すき間のある板の並び（1枚ずつ高さも少しちがう）"""
    for k in range(n):
        jit = rng.uniform(-0.06, 0.06)
        if along == "z":
            a = x0 + (x1 - x0) * k / n + gap / 2
            b = x0 + (x1 - x0) * (k + 1) / n - gap / 2
            bevel(m, name, (a, y0, z0), (b, y1 + jit, z1), mat, b=0.12, group=group)
        else:
            a = z0 + (z1 - z0) * k / n + gap / 2
            b = z0 + (z1 - z0) * (k + 1) / n - gap / 2
            bevel(m, name, (x0, y0, a), (x1, y1 + jit, b), mat, b=0.12, group=group)


def kawara_row(m, name, x0, x1, z0, z1, y, mat, pitch=1.6, group=None):
    """瓦の1列：丸瓦（丸くもり上がる）と平瓦がかわるがわる並ぶ"""
    m.cube(name, (x0, y, z0), (x1, y + 0.5, z1), mat, group=group)
    x = x0 + pitch / 2
    while x < x1 - pitch / 4:
        cylz(m, name, x, y + 0.55, z0 - 0.15, z1, 0.5, mat, sides=3, group=group)
        x += pitch


def rivets(m, name, pts, z, mat, s=0.28, face=-1, group=None):
    for (x, y) in pts:
        m.cube(name, (x - s, y - s, z + (face * 0.18 if face < 0 else 0)), (x + s, y + s, z + (0 if face < 0 else 0.18)), mat,
               group=group)


# ---------------------------------------------------------------- 絵（デカール）

def _weave(draw, face, x0, y0, w, h):
    """い草の編み目（上の面だけ）"""
    if face != "up":
        return
    for j in range(0, h, 3):
        draw.line([(x0, y0 + j), (x0 + w - 1, y0 + j)], fill=(150, 136, 80, 255))
    for i in range(0, w, 14):
        draw.line([(x0 + i, y0), (x0 + i, y0 + h - 1)], fill=(170, 156, 96, 255))


def _bolt(draw, face, x0, y0, w, h):
    """注意の札（黄色の三角に稲妻）"""
    if face != "north":
        return
    draw.polygon([(x0 + w / 2, y0 + 2), (x0 + w - 2, y0 + h - 2), (x0 + 2, y0 + h - 2)], fill=(30, 28, 26, 255))
    draw.polygon([(x0 + w / 2, y0 + 5), (x0 + w - 5, y0 + h - 4), (x0 + 5, y0 + h - 4)], fill=(240, 196, 40, 255))
    cx, cy = x0 + w / 2, y0 + h * 0.62
    s = w * 0.12
    draw.polygon([(cx + s * 0.4, cy - s * 2.0), (cx - s * 0.8, cy + s * 0.2), (cx + s * 0.1, cy + s * 0.2),
                  (cx - s * 0.5, cy + s * 2.0), (cx + s * 0.9, cy - s * 0.4), (cx, cy - s * 0.4)], fill=(30, 28, 26, 255))


def _meter(draw, face, x0, y0, w, h):
    if face != "north":
        return
    draw.ellipse([x0 + 1, y0 + 1, x0 + w - 2, y0 + h - 2], fill=(240, 238, 228, 255), outline=(40, 40, 40, 255))
    for k in range(7):
        a = math.radians(200 + k * 140 / 6)
        cx, cy, r = x0 + w / 2, y0 + h * 0.62, w * 0.38
        draw.line([(cx + math.cos(a) * r * 0.8, cy + math.sin(a) * r * 0.8), (cx + math.cos(a) * r, cy + math.sin(a) * r)],
                  fill=(40, 40, 40, 255))
    draw.line([(x0 + w / 2, y0 + h * 0.62), (x0 + w * 0.72, y0 + h * 0.3)], fill=(200, 30, 30, 255), width=2)


def _louver(draw, face, x0, y0, w, h):
    if face not in ("east", "west"):
        return
    for j in range(int(h * 0.15), int(h * 0.45), 4):
        draw.line([(x0 + w * 0.25, y0 + j), (x0 + w * 0.75, y0 + j)], fill=(70, 74, 72, 255), width=2)


def _planks(n, along="x"):
    def decal(draw, face, x0, y0, w, h):
        if face != "up":
            return
        for k in range(1, n):
            if along == "x":
                y = y0 + h * k / n
                draw.line([(x0, y), (x0 + w - 1, y)], fill=(70, 46, 26, 255), width=1)
            else:
                x = x0 + w * k / n
                draw.line([(x, y0), (x, y0 + h - 1)], fill=(70, 46, 26, 255), width=1)
    return decal


# ---------------------------------------------------------------- 1 玄関マット（スタート地点）

def entrance_mat():
    import random
    rng = random.Random(1)
    m = F("entrance_mat")
    # 沓脱ぎ石：ごつごつした御影石
    rough(m, "step", (-2, 0, 12), (18, 2.4, 17), "granite", rng, lumps=8, depth=0.3)
    # ござ：へりは一段高く、中のい草は細い畝（うね）を並べて本当に凹凸をつける
    m.cube("under", (-3.2, 0, -1.2), (19.2, 0.55, 11.6), "mat_edge")
    for (a0, a1) in (((-3.2, -1.2), (19.2, -0.4)), ((-3.2, 10.8), (19.2, 11.6)), ((-3.2, -0.4), (-2.4, 10.8)), ((18.4, -0.4), (19.2, 10.8))):
        bevel(m, "edge", (a0[0], 0, a0[1]), (a1[0], 1.0, a1[1]), "mat_edge", b=0.2)
    m.cube("goza_base", (-2.4, 0.55, -0.4), (18.4, 0.7, 10.8), "igusa")
    z = -0.4
    while z < 10.6:
        m.cube("goza_rib", (-2.4, 0.7, z + 0.05), (18.4, 0.86, z + 0.45), "igusa")
        z += 0.6
    # 下駄：角を落とした台、前後の歯、V字の鼻緒（ふくらみのある緒）
    for x in (10.2, 13.6):
        bevel(m, "geta", (x, 1.1, 1.0), (x + 2.8, 1.9, 8.6), "kiri_light", b=0.25)
        m.cube("tooth", (x + 0.25, 0.86, 2.2), (x + 2.55, 1.1, 3.1), "hinoki_dark")
        m.cube("tooth", (x + 0.25, 0.86, 6.4), (x + 2.55, 1.1, 7.3), "hinoki_dark")
        m.cube("knot", (x + 1.15, 1.9, 1.9), (x + 1.65, 2.6, 2.5), "hanao_red")
        for side in (-1, 1):
            # 前の結び目から、かかと側の左右へ V の字に開く
            cx = x + 1.4
            m.cube("hanao", (cx - 0.22, 1.9, 2.2), (cx + 0.22, 2.55, 5.4), "hanao_red",
                   rot=[0, side * 21, 0], pivot=[cx, 2.2, 2.2])
            m.cube("hanao_root", (cx + side * 1.15 - 0.3, 1.9, 4.9), (cx + side * 1.15 + 0.3, 2.05, 5.5), "hinoki_dark")
    # 草履：い草を編んだ底（畝）＋鼻緒
    for x in (1.2, 4.6):
        bevel(m, "zori_sole", (x, 0.86, 1.4), (x + 2.8, 1.15, 8.4), "hinoki_dark", b=0.12)
        zz = 1.5
        while zz < 8.2:
            m.cube("zori_rib", (x + 0.15, 1.15, zz), (x + 2.65, 1.38, zz + 0.4), "igusa")
            zz += 0.55
        m.cube("knot", (x + 1.15, 1.38, 2.3), (x + 1.65, 2.0, 2.9), "hanao_navy")
        for side in (-1, 1):
            cx = x + 1.4
            m.cube("hanao", (cx - 0.22, 1.38, 2.6), (cx + 0.22, 1.95, 5.8), "hanao_navy",
                   rot=[0, side * 21, 0], pivot=[cx, 1.6, 2.6])
    return m


# ---------------------------------------------------------------- 2 呼び鈴（緊急ボタン）＝ 小さな鐘楼

def call_bell():
    import random
    rng = random.Random(2)
    m = F("call_bell", 5)
    # 石の基壇（2段、ごつごつ）
    rough(m, "base", (-4, 0, 1), (20, 1.4, 14), "granite_dark", rng, lumps=10, depth=0.2)
    bevel(m, "base2", (-3, 1.4, 2), (19, 2.4, 13), "granite", b=0.3)
    # 柱：礎石の上に面取りした柱、上に斗（ます）、貫と頭貫
    for x in (-1.5, 15.5):
        rough(m, "plinth", (x - 0.6, 2.4, 5.4), (x + 2.6, 3.4, 9.6), "granite_dark", rng, lumps=3, depth=0.15)
        bevel(m, "post", (x, 3.4, 6), (x + 2, 27, 9), "hinoki_dark", b=0.3)
        bevel(m, "masu", (x - 0.4, 26.4, 5.6), (x + 2.4, 27.2, 9.4), "hinoki", b=0.2)
    bevel(m, "nuki", (-2, 22, 6.8), (18, 23.2, 8.2), "hinoki_dark", b=0.2)
    m.cube("nuki_wedge", (-2.6, 21.8, 7.1), (-1.6, 23.4, 7.9), "hinoki")
    m.cube("nuki_wedge", (17.6, 21.8, 7.1), (18.6, 23.4, 7.9), "hinoki")
    bevel(m, "beam", (-3.6, 27.2, 5), (19.6, 29, 10), "hinoki_dark", b=0.3)
    # 屋根：垂木がのぞく軒、瓦は丸瓦と平瓦の列を段々に
    bevel(m, "roof_board", (-5.4, 29, 0.2), (21.4, 29.6, 14.8), "hinoki_dark", b=0.15)
    for k in range(9):
        x = -4.6 + k * 3.1
        m.cube("taruki", (x, 28.6, -0.3), (x + 0.7, 29.2, 15.3), "hinoki")
    for k, (z0, z1, y) in enumerate(((0.0, 15.0, 29.6), (1.3, 13.7, 30.7), (2.7, 12.3, 31.7), (4.2, 10.8, 32.6))):
        kawara_row(m, "kawara", -5.6 + k * 0.5, 21.6 - k * 0.5, z0, z1, y, "kawara", pitch=1.7)
    bevel(m, "ridge", (-4.6, 33.4, 6.2), (20.6, 34.6, 8.8), "kawara", b=0.3)
    for x in (-5.2, 19.8):
        bevel(m, "onigawara", (x, 33.0, 6.0), (x + 1.4, 35.6, 9.0), "kawara", b=0.3)
    # しめ縄：太さに変化（ねじり）
    for k in range(10):
        x = -1.5 + k * 1.9
        m.cube("shimenawa", (x, 25.4 + (k % 2) * 0.15, 5.3), (x + 1.9, 26.6 - (k % 2) * 0.15, 6.3), "shimenawa",
               rot=[0, 0, (k % 2) * 12 - 6], pivot=[x + 0.95, 26, 5.8])
    # 鐘（梵鐘）：龍頭・笠・乳（いぼ）の列・縦横の帯・撞座・口の厚み
    m.group("bell", (8, 27, 7.5))
    m.cube("chain", (7.5, 24.8, 7.0), (8.5, 27, 8.0), "iron_fit", group="bell")
    bevel(m, "ryuzu", (6.4, 23.8, 6.6), (9.6, 25.2, 8.4), "bronze_dark", b=0.3, group="bell")
    m.cyl("kasa", 8, 7.5, 22.8, 23.8, 3.3, "bronze", sides=8, group="bell")
    m.cyl("body", 8, 7.5, 14.4, 22.8, 4.0, "bronze", sides=8, group="bell")
    for y in (21.8, 19.0, 15.8):
        m.cyl("band", 8, 7.5, y, y + 0.45, 4.2, "bronze_dark", sides=8, group="bell")
    for k in range(8):   # 縦の帯
        a = math.radians(22.5 + k * 45)
        x, z = 8 + math.cos(a) * 4.05, 7.5 + math.sin(a) * 4.05
        m.cube("band_v", (x - 0.22, 14.4, z - 0.22), (x + 0.22, 22.8, z + 0.22), "bronze_dark", group="bell")
    for side_z, sgn in ((3.45, -1), (11.55, 1)):
        for row in range(3):
            for col in range(3):
                x = 6.6 + col * 1.4
                y = 19.8 + row * 0.7
                m.cube("chi", (x - 0.3, y - 0.3, side_z - (0.4 if sgn < 0 else 0)), (x + 0.3, y + 0.3, side_z + (0.4 if sgn > 0 else 0)),
                       "bronze_dark", group="bell")
    m.cyl("tsukiza", 8, 3.4, 16.6, 18.2, 0.9, "bronze_dark", sides=4, group="bell")
    m.cyl("lip", 8, 7.5, 13.2, 14.4, 4.5, "bronze", sides=8, group="bell")
    m.cyl("mouth", 8, 7.5, 13.15, 13.3, 3.6, "deep_dark", sides=8, group="bell")
    # 撞木：縄で吊った丸太
    m.group("striker", (8, 27, 7.5))
    for x in (5.6, 9.9):
        for k in range(8):
            y = 18.8 + k * 1.0
            m.cube("rope", (x + (k % 2) * 0.08, y, -3.4), (x + 0.5 - (k % 2) * 0.08, y + 1.0, -2.9), "shimenawa", group="striker")
    cylz(m, "log", 8, 17.6, -8, 1.6, 1.1, "hinoki", sides=4, group="striker")
    m.cube("log_ring", (6.7, 16.3, -1.4), (9.3, 18.9, -0.9), "iron_fit", group="striker")
    m.cube("log_end", (6.9, 16.5, 1.6), (9.1, 18.7, 1.9), "hinoki_dark", group="striker")
    bevel(m, "beam_fwd", (5, 27, -4), (11, 28.2, 5), "hinoki_dark", b=0.2)
    return m


# ---------------------------------------------------------------- 3 床下点検口（ベント）

def floor_hatch():
    import random
    rng = random.Random(3)
    m = F("floor_hatch")
    # 枠：面取りした黒い木、四隅に鋲を打った鉄の金物
    bevel(m, "frame_n", (0, 0, 0), (16, 1.6, 2), "hinoki_dark", b=0.25)
    bevel(m, "frame_s", (0, 0, 14), (16, 1.6, 16), "hinoki_dark", b=0.25)
    bevel(m, "frame_w", (0, 0, 2), (2, 1.6, 14), "hinoki_dark", b=0.25)
    bevel(m, "frame_e", (14, 0, 2), (16, 1.6, 14), "hinoki_dark", b=0.25)
    for (x, z) in ((0, 0), (13.4, 0), (0, 13.4), (13.4, 13.4)):
        m.cube("corner", (x - 0.1, 1.6, z - 0.1), (x + 2.7, 1.75, z + 2.7), "iron_fit")
        m.cube("tack", (x + 1.0, 1.75, z + 1.0), (x + 1.6, 1.95, z + 1.6), "iron_fit")
    # 穴の中：暗がり、根太、くもの巣のような土
    m.cube("hole", (2, -3, 2), (14, 0.2, 14), "deep_dark")
    for z in (6.5, 10.5):
        bevel(m, "joist", (2, -1.4, z), (14, -0.4, z + 1.0), "hinoki_dark", b=0.15)
    # ふた：すき間のある板4枚、裏に桟、鉄の取っ手（輪が浮いている）、ちょうつがい
    m.group("lid", (8, 1.5, 14))
    boards(m, "lid", 2.15, 13.85, 2.15, 13.85, 0.55, 1.5, 4, "floor_board", rng, along="z", group="lid")
    for z in (4, 11):
        bevel(m, "batten", (2.6, 0.15, z), (13.4, 0.55, z + 1.0), "hinoki_dark", b=0.12, group="lid")
    m.cube("pull_plate", (6.3, 1.5, 3.0), (9.7, 1.62, 4.9), "iron_fit", group="lid")
    m.cube("pull_ring", (7.0, 1.62, 3.3), (9.0, 1.95, 3.7), "iron_fit", group="lid")
    m.cube("pull_ring", (7.0, 1.62, 4.2), (9.0, 1.95, 4.6), "iron_fit", group="lid")
    m.cube("pull_ring", (7.0, 1.62, 3.3), (7.4, 1.95, 4.6), "iron_fit", group="lid")
    m.cube("pull_ring", (8.6, 1.62, 3.3), (9.0, 1.95, 4.6), "iron_fit", group="lid")
    for x in (3.5, 11.5):
        m.cube("hinge", (x, 1.5, 12.8), (x + 1.2, 1.68, 14.6), "iron_fit")
        cylx(m, "hinge_pin", 1.5, 14.6, x - 0.2, x + 1.4, 0.3, "iron_fit", sides=3)
    return m


# ---------------------------------------------------------------- 4 配電盤（停電を直す）

def breaker():
    m = F("breaker")
    # 台と脚
    bevel(m, "stand", (2.5, 0, 9), (13.5, 1.0, 14), "cabinet_dark", b=0.2)
    for x in (3, 11.5):
        bevel(m, "leg", (x, 1, 10), (x + 1.5, 6, 13), "cabinet_dark", b=0.2)
    # 鉄の箱：面取り、側面に本物のすき間のあるガラリ（よろい板）
    bevel(m, "box", (1, 6, 5), (15, 26, 14), "cabinet", b=0.4)
    for side_x, d in ((0.6, -1), (15.4, 1)):
        for k in range(6):
            y = 18 + k * 1.1
            m.cube("louver", (side_x - (0.3 if d < 0 else 0), y, 8), (side_x + (0 if d < 0 else 0.3), y + 0.5, 12), "cabinet_dark",
                   rot=[0, 0, -25 * d], pivot=[side_x, y + 0.25, 10])
    bevel(m, "box_top", (0.5, 26, 4.5), (15.5, 26.9, 14.5), "cabinet_dark", b=0.25)
    # 上の配管と分岐箱（ねじどめのふた）
    bevel(m, "junction", (5.5, 26.9, 8), (10.5, 29.2, 12), "cabinet_dark", b=0.25)
    rivets(m, "screw", ((6.1, 28.6), (9.9, 28.6), (6.1, 27.5), (9.9, 27.5)), 8, "iron_fit", s=0.2)
    m.cyl("conduit", 8, 10, 29.2, 34, 0.9, "conduit", sides=4)
    m.cyl("conduit_ring", 8, 10, 30.6, 31.2, 1.1, "cabinet_dark", sides=4)
    cylx(m, "conduit_side", 10, 10, 10.5, 16.5, 0.6, "conduit", sides=3)
    # 中のパネル：奥まった黒い板、ブレーカーの列（つまみが飛び出す）、メーター（縁が出っぱる）、主幹レバー
    m.cube("panel", (2, 7, 5.9), (14, 25, 6.2), "panel_black")
    for row in range(2):
        for i in range(5):
            x = 2.8 + i * 2.2
            y = 20.6 - row * 3.4
            bevel(m, "switch_body", (x, y, 5.0), (x + 1.6, y + 2.4, 5.9), "switch_white", b=0.12)
            up = (i + row) % 2
            m.cube("switch_tab", (x + 0.5, y + 1.3 - up * 0.8, 4.4), (x + 1.1, y + 2.0 - up * 0.8, 5.0), "panel_black")
    m.cyl("meter_rim", 11.3, 5.6, 9.0, 12.8, 2.0, "cabinet_dark", sides=4)
    m.cube("meter", (9.6, 9.3, 5.25), (13.0, 12.5, 5.4), "switch_white", decal=_meter)
    bevel(m, "lever_base", (4.0, 9.4, 4.6), (8.0, 13.6, 5.9), "cabinet_dark", b=0.25)
    m.group("lever", (6, 11.5, 5.0))
    m.cube("lever_arm", (5.5, 11.0, 2.0), (6.5, 12.0, 5.0), "iron_fit", group="lever")
    bevel(m, "lever_grip", (4.6, 10.7, 1.0), (7.4, 12.3, 2.4), "lever_red", b=0.25, group="lever")
    # 扉：外周が出っぱった縁、まん中は一段へこんだ板、取っ手・鍵穴・注意の札・リベット
    m.group("door", (1, 16, 4.4))
    m.cube("door", (1.4, 6.6, 4.0), (14.6, 25.4, 4.6), "cabinet", group="door")
    for (a, b) in (((1, 6.2), (15, 7.2)), ((1, 24.8), (15, 25.8)), ((1, 7.2), (2, 24.8)), ((14, 7.2), (15, 24.8))):
        bevel(m, "door_rim", (a[0], a[1], 3.6), (b[0], b[1], 4.6), "cabinet", b=0.15, group="door")
    rivets(m, "rivet", [(1.5, 6.7), (14.5, 6.7), (1.5, 25.3), (14.5, 25.3), (8, 6.7), (8, 25.3)], 3.6, "cabinet_dark", s=0.22,
           group="door")
    m.cube("sign", (5.5, 16.5, 3.75), (10.5, 21.5, 4.0), "warn_yellow", group="door", decal=_bolt)
    bevel(m, "handle_base", (12.4, 13.0, 3.5), (13.8, 18.0, 4.0), "cabinet_dark", b=0.15, group="door")
    bevel(m, "handle", (12.7, 13.6, 2.6), (13.5, 17.4, 3.5), "iron_fit", b=0.15, group="door")
    m.cube("keyhole", (12.85, 11.8, 3.75), (13.35, 12.6, 4.0), "deep_dark", group="door")
    for y in (8.5, 23.5):
        bevel(m, "hinge", (0.5, y, 3.5), (1.6, y + 1.6, 4.9), "iron_fit", b=0.15)
    return m



# ---------------------------------------------------------------- 5〜8 で使う材質
MATS.update({
    "gas_pipe": Mat((222, 182, 52), "metal", noise=6),
    "gas_pipe_dark": Mat((172, 136, 34), "metal", noise=6),
    "valve_red": Mat((182, 36, 30), "metal", noise=5),
    "concrete": Mat((150, 148, 140), "stone", noise=10),
    "boiler_steel": Mat((176, 180, 178), "metal", noise=6),
    "boiler_dark": Mat((84, 88, 86), "metal", noise=6),
    "soot": Mat((40, 36, 34), "stone", noise=6),
    "grate": Mat((30, 28, 28), "metal", noise=4),
    "urushi_k": Mat((26, 20, 18), "metal", noise=4),
    "urushi_r": Mat((128, 26, 22), "metal", noise=5),
    "ash_w": Mat((178, 172, 164), "plaster", noise=10),
    "coal": Mat((36, 30, 28), "stone", noise=6),
    "ember_g": Mat((230, 90, 30), "ember", accent=(255, 190, 80), glow=True),
    "tetsubin": Mat((44, 42, 44), "metal", noise=8),
    "yunomi": Mat((222, 214, 196), "plaster", noise=6),
    "yunomi_glaze": Mat((96, 120, 110), "metal", noise=6),
    "matcha_c": Mat((116, 150, 60), "stipple", noise=6),
    "moss": Mat((86, 118, 54), "stone", accent=(64, 98, 40), noise=12),
    "old_wood": Mat((112, 92, 70), "wood", accent=(86, 68, 50), noise=8),
    "old_wood_dark": Mat((70, 56, 44), "wood", accent=(52, 40, 30), noise=8),
    "shide": Mat((246, 244, 236), "paper", noise=3),
    "glow_evil": Mat((150, 60, 230), "fire", accent=(230, 180, 255), glow=True),
    "sake_white": Mat((236, 232, 222), "plaster", noise=4),
})


def _gauge_face(draw, face, x0, y0, w, h):
    if face != "north":
        return
    draw.ellipse([x0, y0, x0 + w - 1, y0 + h - 1], fill=(238, 236, 226, 255), outline=(30, 30, 30, 255))
    cx, cy, r = x0 + w / 2, y0 + h / 2, w * 0.42
    for k in range(9):
        a = math.radians(135 + k * 270 / 8)
        draw.line([(cx + math.cos(a) * r * 0.78, cy + math.sin(a) * r * 0.78), (cx + math.cos(a) * r, cy + math.sin(a) * r)],
                  fill=(30, 30, 30, 255))
    a = math.radians(135 + 270 * 0.8)
    draw.arc([cx - r, cy - r, cx + r, cy + r], math.degrees(a), 405, fill=(200, 40, 30, 255), width=2)


def _gas_tag(draw, face, x0, y0, w, h):
    if face != "north":
        return
    from items_hires import text_decal
    text_decal("ガス", color=(30, 30, 30, 255), faces=("north",), size=0.55, vertical=False)(draw, face, x0, y0, w, h)


def _lattice(draw, face, x0, y0, w, h):
    """格子戸のすき間の奥（暗く）"""
    if face != "north":
        return


def flange(m, name, axis, c, r, t, mat, bolts=6, group=None):
    """管のつなぎ目のフランジ（輪＋ボルトの頭）。axis = "x" / "y" / "z"、c = 中心"""
    x, y, z = c
    if axis == "y":
        m.cyl(name, x, z, y - t / 2, y + t / 2, r, mat, sides=6, group=group)
        for k in range(bolts):
            a = 2 * math.pi * k / bolts
            bx, bz = x + math.cos(a) * r * 0.78, z + math.sin(a) * r * 0.78
            m.cube(name + "_bolt", (bx - 0.22, y + t / 2, bz - 0.22), (bx + 0.22, y + t / 2 + 0.3, bz + 0.22), "iron_fit", group=group)
    elif axis == "x":
        cylx(m, name, y, z, x - t / 2, x + t / 2, r, mat, sides=6, group=group)
        for k in range(bolts):
            a = 2 * math.pi * k / bolts
            by, bz = y + math.cos(a) * r * 0.78, z + math.sin(a) * r * 0.78
            m.cube(name + "_bolt", (x + t / 2, by - 0.22, bz - 0.22), (x + t / 2 + 0.3, by + 0.22, bz + 0.22), "iron_fit", group=group)
    else:
        cylz(m, name, x, y, z - t / 2, z + t / 2, r, mat, sides=6, group=group)
        for k in range(bolts):
            a = 2 * math.pi * k / bolts
            bx, by = x + math.cos(a) * r * 0.78, y + math.sin(a) * r * 0.78
            m.cube(name + "_bolt", (bx - 0.22, by - 0.22, z - t / 2 - 0.3), (bx + 0.22, by + 0.22, z - t / 2), "iron_fit", group=group)


# ---------------------------------------------------------------- 5 ガスの元栓

def gas_valve():
    import random
    from items_hires import ring
    rng = random.Random(5)
    m = F("gas_valve", 6)
    rough(m, "pad", (2, 0, 6), (14, 1.2, 16), "concrete", rng, lumps=6, depth=0.15)
    # 立ち上がりの管（黄色いガス管）とフランジ
    m.cyl("riser", 8, 11, 1.2, 16, 1.6, "gas_pipe", sides=6)
    flange(m, "flange", "y", (8, 2.0, 11), 2.4, 0.6, "gas_pipe_dark")
    flange(m, "flange", "y", (8, 12.0, 11), 2.3, 0.6, "gas_pipe_dark")
    # 横に走る管（左右へ）と、エルボ
    cylx(m, "pipe", 18, 11, -4, 20, 1.4, "gas_pipe", sides=6)
    flange(m, "flange", "x", (1.0, 18, 11), 2.1, 0.6, "gas_pipe_dark")
    flange(m, "flange", "x", (15.0, 18, 11), 2.1, 0.6, "gas_pipe_dark")
    # 弁の本体（ふくらんだ胴）とボンネット、手前へ出る軸
    bevel(m, "valve_body", (5, 15, 8), (11, 21, 14), "gas_pipe_dark", b=0.6)
    m.cyl("bonnet", 8, 11, 21, 23, 1.8, "gas_pipe_dark", sides=6)
    cylz(m, "stem", 8, 18, 3.6, 8, 0.45, "iron_fit", sides=3)
    m.cube("packing_nut", (7.0, 17.0, 6.6), (9.0, 19.0, 8.0), "iron_fit")
    # 圧力計（縁が出っぱる）
    m.cyl("gauge_stem", 8, 11, 23, 24.4, 0.4, "iron_fit", sides=3)
    cylz(m, "gauge_case", 8, 26, 9.6, 11.4, 2.0, "boiler_dark", sides=6)
    m.cube("gauge_face", (6.4, 24.4, 9.45), (9.6, 27.6, 9.6), "switch_white", decal=_gauge_face)
    # 札
    bevel(m, "tag", (11.2, 12.0, 8.6), (14.2, 14.2, 8.9), "warn_yellow", b=0.1, decal=_gas_tag)
    m.cube("tag_wire", (12.6, 14.2, 8.7), (12.8, 15.8, 8.8), "iron_fit")
    # ハンドル（赤い輪に4本のスポーク）：回る部分
    m.group("wheel", (8, 18, 4))
    ring(m, "rim", 8, 18, 4, 4.4, 0.8, 0.9, "valve_red", seg=20, group="wheel")
    for k in range(4):
        a = 45 + k * 90
        m.cube("spoke", (7.65, 18, 3.75), (8.35, 22.0, 4.25), "valve_red", rot=[0, 0, a], pivot=[8, 18, 4], group="wheel")
    cylz(m, "hub", 8, 18, 3.2, 4.8, 0.9, "valve_red", sides=4, group="wheel")
    m.cube("hub_nut", (7.4, 17.4, 2.9), (8.6, 18.6, 3.2), "iron_fit", group="wheel")
    return m


# ---------------------------------------------------------------- 6 ボイラー

def boiler():
    import random
    rng = random.Random(6)
    m = F("boiler", 5)
    rough(m, "pad", (0, 0, 0.5), (16, 1.0, 15.5), "concrete", rng, lumps=6, depth=0.12)
    m.group("body", (8, 0, 8))
    for (x, z) in ((2.5, 2.5), (12, 2.5), (2.5, 12), (12, 12)):
        bevel(m, "foot", (x, 1, z), (x + 1.5, 3, z + 1.5), "boiler_dark", b=0.2, group="body")
    m.cyl("tank", 8, 8, 3, 27, 6.6, "boiler_steel", sides=8, group="body")
    for y in (3.2, 9.5, 16.5, 23.5, 26.6):
        m.cyl("band", 8, 8, y, y + 0.7, 6.9, "boiler_dark", sides=8, group="body")
        for k in range(16):    # 帯のリベット
            a = 2 * math.pi * k / 16
            x, z = 8 + math.cos(a) * 6.95, 8 + math.sin(a) * 6.95
            m.cube("rivet", (x - 0.18, y + 0.2, z - 0.18), (x + 0.18, y + 0.5, z + 0.18), "iron_fit", group="body")
    m.cyl("dome", 8, 8, 27, 28.4, 5.2, "boiler_steel", sides=8, group="body")
    m.cyl("dome2", 8, 8, 28.4, 29.2, 3.6, "boiler_dark", sides=8, group="body")
    # 煙突と雨よけの笠
    m.cyl("chimney", 8, 8, 29.2, 35, 1.6, "boiler_dark", sides=6, group="body")
    m.cyl("chimney_cap", 8, 8, 35, 35.5, 2.6, "boiler_dark", sides=6, group="body")
    m.cyl("chimney_top", 8, 8, 35.5, 36.6, 0.9, "boiler_dark", sides=4, group="body")
    # 安全弁と配管
    m.cyl("safety", 12, 9, 27.6, 30.4, 0.7, "gas_pipe_dark", sides=4, group="body")
    m.cyl("safety_top", 12, 9, 30.4, 31.0, 1.0, "valve_red", sides=4, group="body")
    cylx(m, "pipe_out", 22, 9, 14.6, 19, 0.9, "boiler_dark", sides=4, group="body")
    flange(m, "flange", "x", (15.2, 22, 9), 1.5, 0.5, "boiler_dark", bolts=4, group="body")
    # たき口：厚い扉の枠、のぞき窓に火床の格子、奥で火が燃える
    bevel(m, "firebox", (3.6, 3.4, 0.6), (12.4, 8.6, 2.2), "boiler_dark", b=0.3, group="body")
    m.cube("firebox_in", (4.8, 4.4, 0.45), (11.2, 7.6, 0.6), "soot", group="body")
    m.group("fire", (8, 4, 0.5), "body")
    m.cube("glow_fire", (5.0, 4.5, 0.62), (11.0, 7.4, 0.7), "glow_fire", group="fire")
    m.cube("glow_coal", (5.0, 4.4, 0.6), (11.0, 5.2, 1.4), "ember_g", group="fire")
    for k in range(6):
        x = 5.2 + k * 1.1
        m.cube("grate", (x, 4.4, 0.3), (x + 0.35, 7.6, 0.45), "grate", group="body")
    bevel(m, "fire_door_hinge", (12.4, 4.0, 0.4), (13.2, 8.0, 1.4), "iron_fit", b=0.1, group="body")
    # 圧力計（縁と針）
    cylz(m, "dial_case", 8, 17, 0.6, 1.6, 3.2, "boiler_dark", sides=6, group="body")
    m.cube("dial", (5.4, 14.4, 0.45), (10.6, 19.6, 0.6), "switch_white", decal=_gauge_face, group="body")
    m.group("needle", (8, 17, 0), "body")
    m.cube("needle", (7.75, 17, 0.2), (8.25, 19.4, 0.45), "lever_red", group="needle")
    m.cube("needle_cap", (7.6, 16.6, 0.1), (8.4, 17.4, 0.45), "iron_fit", group="needle")
    # 水面計（ガラス管）
    m.cube("glass_gauge", (12.4, 11, 1.4), (13.0, 15, 2.0), "glass_bowl", group="body")
    m.cube("gauge_cap", (12.2, 10.6, 1.2), (13.2, 11, 2.2), "iron_fit", group="body")
    m.cube("gauge_cap", (12.2, 15, 1.2), (13.2, 15.4, 2.2), "iron_fit", group="body")
    return m


# ---------------------------------------------------------------- 7 会議の座卓

def meeting_table():
    m = F("meeting_table", 4)
    # 天板：黒漆、ふちは朱で一段高く、角を落とす
    bevel(m, "top", (-12, 6, -12), (28, 7.8, 28), "urushi_k", b=0.4)
    for (a, b) in (((-12, -12), (28, -11)), ((-12, 27), (28, 28)), ((-12, -11), (-11, 27)), ((27, -11), (28, 27))):
        bevel(m, "rim", (a[0], 7.8, a[1]), (b[0], 8.3, b[1]), "urushi_r", b=0.15)
    bevel(m, "apron", (-11.2, 4.8, -11.2), (27.2, 6.0, 27.2), "urushi_k", b=0.2)
    # 脚：外にふくらむ猫脚
    for (x, z) in ((-10.5, -10.5), (24.5, -10.5), (-10.5, 24.5), (24.5, 24.5)):
        bevel(m, "leg", (x, 1.2, z), (x + 2.2, 4.8, z + 2.2), "urushi_k", b=0.3)
        bevel(m, "leg_foot", (x - 0.4, 0, z - 0.4), (x + 2.6, 1.2, z + 2.6), "urushi_k", b=0.3)
    # まん中の炉：銅の縁、灰、炭、五徳に鉄瓶
    bevel(m, "hearth", (2.5, 7.8, 2.5), (13.5, 8.6, 13.5), "copper_r", b=0.25)
    m.cube("ash", (3.4, 8.0, 3.4), (12.6, 8.7, 12.6), "ash_w")
    for (x, z) in ((5, 5), (8.6, 4.6), (10.4, 8.4), (5.6, 9.8), (8, 7.4)):
        bevel(m, "coal", (x, 8.7, z), (x + 1.8, 9.6, z + 1.4), "coal", b=0.2)
        m.cube("glow_coal", (x + 0.3, 9.5, z + 0.2), (x + 1.5, 9.7, z + 1.2), "ember_g")
    for k in range(3):
        a = math.radians(90 + k * 120)
        x, z = 8 + math.cos(a) * 3.2, 8 + math.sin(a) * 3.2
        m.cube("gotoku", (x - 0.25, 8.7, z - 0.25), (x + 0.25, 11.2, z + 0.25), "tetsubin")
    m.cyl("tetsubin", 8, 8, 11.2, 14.4, 3.0, "tetsubin", sides=8)
    m.cyl("tetsubin_lid", 8, 8, 14.4, 15.0, 1.8, "tetsubin", sides=6)
    m.cube("lid_knob", (7.5, 15.0, 7.5), (8.5, 15.8, 8.5), "copper_r")
    m.cube("spout", (10.6, 12.6, 7.4), (13.4, 13.4, 8.6), "tetsubin", rot=[0, 0, 25], pivot=[10.6, 12.6, 8])
    for x in (5.2, 10.8):
        m.cube("handle", (x - 0.25, 14.4, 7.75), (x + 0.25, 17.4, 8.25), "tetsubin")
    m.cube("handle_top", (5.2, 17.0, 7.75), (10.8, 17.5, 8.25), "tetsubin")
    # 湯のみ（茶托にのせて）8人ぶん
    for (x, z) in ((-8, -8), (20, -8), (-8, 20), (20, 20), (6.75, -9), (6.75, 21.5), (-9.5, 6.75), (21.5, 6.75)):
        m.cyl("saucer", x + 1.25, z + 1.25, 7.8, 8.3, 2.0, "urushi_r", sides=6)
        m.cyl("yunomi", x + 1.25, z + 1.25, 8.3, 11.0, 1.3, "yunomi", sides=6)
        m.cyl("glaze", x + 1.25, z + 1.25, 9.9, 11.0, 1.35, "yunomi_glaze", sides=6)
        m.cyl("tea", x + 1.25, z + 1.25, 10.7, 10.95, 1.05, "matcha_c", sides=6)
    # 急須（湯気が出る所）
    m.cyl("pot", 16.5, 16.5, 7.8, 11.6, 2.4, "copper_r", sides=8)
    m.cyl("pot_lid", 16.5, 16.5, 11.6, 12.2, 1.6, "copper_r", sides=6)
    m.cube("pot_spout", (18.6, 9.2, 16.0), (21.0, 10.0, 17.0), "copper_r", rot=[0, 0, 20], pivot=[18.6, 9.2, 16.5])
    m.cube("pot_handle", (16.0, 9.0, 12.2), (17.0, 10.0, 14.4), "old_wood_dark")
    return m


# ---------------------------------------------------------------- 8 祠

def hokora():
    import random
    rng = random.Random(8)
    m = F("hokora", 5)
    # 苔むした石の基壇（2段）と石段
    rough(m, "base", (-2, 0, -2), (18, 3.6, 18), "granite_dark", rng, lumps=12, depth=0.3)
    rough(m, "base2", (0, 3.6, 0), (16, 5.8, 16), "granite", rng, lumps=8, depth=0.25)
    for k in range(10):
        x, z = rng.uniform(-1.6, 16.6), rng.uniform(-1.6, 16.6)
        m.cube("moss", (x, 3.55, z), (x + rng.uniform(1, 2.5), 3.9, z + rng.uniform(1, 2)), "moss")
    bevel(m, "stair", (5, 0, -3.6), (11, 1.6, -2), "granite", b=0.2)
    bevel(m, "stair", (5.5, 1.6, -2.6), (10.5, 3.2, -1.2), "granite", b=0.2)
    # 社（やしろ）：古い木の箱、柱、縁
    m.group("shrine", (2, 6, 8))
    bevel(m, "floor", (1, 5.8, 1.6), (15, 6.6, 15), "old_wood_dark", b=0.15, group="shrine")
    boards(m, "wall", 2.4, 13.6, 3.6, 14, 6.6, 17.6, 6, "old_wood", rng, along="z", gap=0.12, group="shrine")
    for x in (1.6, 13.4):
        bevel(m, "pillar", (x, 6.6, 2.4), (x + 1.4, 18.4, 3.8), "old_wood_dark", b=0.2, group="shrine")
        bevel(m, "pillar", (x, 6.6, 13.2), (x + 1.4, 18.4, 14.6), "old_wood_dark", b=0.2, group="shrine")
    bevel(m, "lintel", (1.2, 17.0, 2.2), (14.8, 18.4, 4.0), "old_wood_dark", b=0.2, group="shrine")
    # 中の暗がりと、封じられた悪神の光
    m.cube("inside", (3.0, 6.6, 3.7), (13.0, 16.8, 4.0), "deep_dark", group="shrine")
    m.group("evil", (8, 11, 3.2), "shrine")
    m.cube("glow_inside", (4, 7, 3.6), (12, 16, 3.8), "glow_evil", group="evil")
    m.cube("glow_eyes", (6.2, 12.4, 3.4), (7.4, 13.0, 3.6), "glow_red", group="evil")
    m.cube("glow_eyes", (8.6, 12.4, 3.4), (9.8, 13.0, 3.6), "glow_red", group="evil")
    # 格子の扉（左右に開く）：枠と縦横の格子
    for side, (x0, x1, pivx) in (("door_l", (3.0, 8.0, 3.0)), ("door_r", (8.0, 13.0, 13.0))):
        m.group(side, (pivx, 8, 2.8), "shrine")
        for (a, b) in (((x0, 7.0), (x1, 7.6)), ((x0, 16.2), (x1, 16.8)), ((x0, 7.6), (x0 + 0.6, 16.2)), ((x1 - 0.6, 7.6), (x1, 16.2))):
            m.cube("door_frame", (a[0], a[1], 2.6), (b[0], b[1], 3.3), "old_wood_dark", group=side)
        for k in range(1, 4):
            x = x0 + (x1 - x0) * k / 4
            m.cube("koshi", (x - 0.2, 7.6, 2.75), (x + 0.2, 16.2, 3.15), "old_wood", group=side)
        for k in range(1, 6):
            y = 7.6 + 8.6 * k / 6
            m.cube("koshi", (x0 + 0.6, y - 0.18, 2.8), (x1 - 0.6, y + 0.18, 3.1), "old_wood", group=side)
    # 屋根：板の上に瓦の列、棟と千木・鰹木
    m.group("roof", (8, 19, 8), "shrine")
    bevel(m, "roof_board", (-0.6, 18.4, 0.4), (16.6, 19.0, 15.8), "old_wood_dark", b=0.15, group="roof")
    for k, (z0, z1, y) in enumerate(((0.2, 16.0, 19.0), (1.4, 14.8, 19.9), (2.8, 13.4, 20.8), (4.4, 11.8, 21.6))):
        kawara_row(m, "kawara", -1.0 + k * 0.5, 17.0 - k * 0.5, z0, z1, y, "kawara", pitch=1.6, group="roof")
    bevel(m, "ridge", (-0.4, 22.4, 6.6), (16.4, 23.4, 9.2), "kawara", b=0.25, group="roof")
    for x in (2.5, 6.5, 10.5):
        cylx(m, "katsuogi", 23.9, 7.9, x, x + 2.8, 0.6, "old_wood_dark", sides=4, group="roof")
    for x, a in ((0.2, 20), (15.0, -20)):
        m.cube("chigi", (x, 22.4, 7.4), (x + 0.8, 26.0, 8.4), "old_wood_dark", rot=[0, 0, a], pivot=[x + 0.4, 22.4, 7.9], group="roof")
    # しめ縄としで（紙垂）：太いねじれた縄、ジグザグの白い紙
    m.group("rope", (1, 16.5, 2), "shrine")
    for k in range(8):
        x = 1.0 + k * 1.75
        m.cube("shimenawa", (x, 16.0 + (k % 2) * 0.12, 1.5), (x + 1.75, 17.1 - (k % 2) * 0.12, 2.5), "shimenawa",
               rot=[0, 0, (k % 2) * 14 - 7], pivot=[x + 0.88, 16.5, 2], group="rope")
    for x in (4, 8, 12):
        for k in range(4):
            dx = 0.5 if k % 2 else -0.5
            m.cube("shide", (x + dx - 0.6, 15.6 - k * 0.9, 1.8), (x + dx + 0.6, 16.0 - k * 0.9 + 0.1, 1.95), "shide", group="rope")
    # お供え：三方に盃と白い徳利
    bevel(m, "sanbo", (5.4, 5.8, -1.2), (10.6, 6.6, 1.6), "hinoki", b=0.15)
    m.cube("sanbo_leg", (6.0, 5.8, -0.6), (10.0, 6.0, 1.0), "hinoki")
    m.cyl("tokkuri", 6.6, 0.2, 6.6, 8.4, 0.7, "sake_white", sides=4)
    m.cyl("tokkuri_neck", 6.6, 0.2, 8.4, 9.0, 0.35, "sake_white", sides=4)
    m.cyl("sakazuki", 9.0, 0.2, 6.6, 7.0, 0.8, "urushi_r", sides=4)
    return m


OVERRIDES = {"SPAWN": entrance_mat, "BELL": call_bell, "VENT": floor_hatch, "BREAKER": breaker,
             "GAS_VALVE": gas_valve, "BOILER": boiler, "MEETING": meeting_table, "HOKORA": hokora}
