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
    m = _F(name, d)
    m.light = True
    return m


def _F(name, d=6):
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

# ---------------------------------------------------------------- 9〜12 で使う材質
MATS.update({
    "terminal": Mat((60, 62, 64), "polymer", noise=3),
    "brass_s": Mat((200, 164, 70), "metal", noise=6),
    "wire_red": Mat((196, 40, 36), "polymer", noise=3),
    "wire_blue": Mat((40, 80, 180), "polymer", noise=3),
    "wire_yellow": Mat((228, 190, 40), "polymer", noise=3),
    "wire_green": Mat((60, 150, 60), "polymer", noise=3),
    "lens_red": Mat((240, 60, 50), "plain", noise=4, glow=True),
    "lens_green": Mat((90, 230, 90), "plain", noise=4, glow=True),
    "lens_off": Mat((90, 40, 36), "metal", noise=4),
    "desk": Mat((96, 62, 38), "wood", accent=(72, 44, 26), noise=6),
    "desk_top": Mat((122, 82, 50), "wood", accent=(94, 60, 34), noise=6),
    "book_cover": Mat((60, 70, 110), "stipple", noise=5),
    "page": Mat((242, 236, 218), "paper", noise=3),
    "page_edge": Mat((222, 212, 186), "paper", noise=4),
    "suzuri": Mat((40, 40, 44), "stone", noise=5),
    "sumi": Mat((10, 10, 12), "plain", noise=2),
    "fude": Mat((180, 150, 90), "bamboo", accent=(150, 120, 70), noise=4),
    "fude_tip": Mat((24, 22, 22), "stipple", noise=4),
    "candle": Mat((244, 240, 230), "plaster", noise=3),
    "tatami_n": Mat((176, 172, 104), "bamboo", accent=(150, 146, 84), noise=4),
    "heri": Mat((40, 56, 46), "stipple", noise=4),
    "shikibuton": Mat((246, 244, 238), "stipple", noise=4),
    "kakebuton": Mat((70, 104, 160), "stipple", noise=6),
    "kakebuton_pat": Mat((238, 232, 214), "stipple", noise=4),
    "makura": Mat((236, 230, 214), "stipple", noise=5),
    "shikkui": Mat((230, 222, 206), "plaster", noise=6),
    "kamado_tile": Mat((150, 80, 52), "tile", accent=(118, 60, 38), noise=6),
    "hagama": Mat((50, 46, 46), "metal", noise=6),
    "hagama_old": Mat((70, 60, 54), "metal", noise=12),
    "lid_wood": Mat((198, 162, 112), "wood", accent=(162, 124, 80), noise=4),
    "log_bark": Mat((92, 70, 50), "stone", accent=(70, 52, 36), noise=10),
    "log_cut": Mat((206, 170, 120), "wood", accent=(170, 130, 86), noise=4),
})


def _text(chars, color=(30, 30, 30, 255), size=0.55, faces=("north",)):
    from items_hires import text_decal
    return text_decal(chars, color=color, faces=faces, size=size, vertical=False)


def _rings(draw, face, x0, y0, w, h):
    """丸太の切り口（年輪）"""
    if face not in ("east", "west", "north", "south"):
        return
    cx, cy = x0 + w / 2, y0 + h / 2
    for k in range(1, 5):
        r = min(w, h) * k / 10
        draw.ellipse([cx - r, cy - r, cx + r, cy + r], outline=(150, 110, 70, 255))


def wire_sag(m, name, x0, x1, y, z, mat, sag=1.2, group=None, n=4):
    """たるんだ電線（短い線をつなぐ）"""
    pts = []
    for k in range(n + 1):
        t = k / n
        pts.append((x0 + (x1 - x0) * t, y - sag * 4 * t * (1 - t)))
    for k in range(n):
        (xa, ya), (xb, yb) = pts[k], pts[k + 1]
        ln = math.hypot(xb - xa, yb - ya)
        ang = math.degrees(math.atan2(yb - ya, xb - xa))
        cx, cy = (xa + xb) / 2, (ya + yb) / 2
        m.cube(name, (cx - ln / 2 - 0.05, cy - 0.35, z - 0.35), (cx + ln / 2 + 0.05, cy + 0.35, z + 0.35), mat,
               rot=[0, 0, ang], pivot=[cx, cy, z], group=group)


# ---------------------------------------------------------------- 9 配線盤

def wiring_panel():
    m = F("wiring_panel", 5)
    for x in (2, 12):
        bevel(m, "leg", (x, 0, 7), (x + 2, 4, 13), "cabinet_dark", b=0.2)
    bevel(m, "box", (1, 4, 6), (15, 28, 14), "cabinet", b=0.4)
    bevel(m, "box_top", (0.6, 28, 5.6), (15.4, 28.8, 14.4), "cabinet_dark", b=0.25)
    m.cube("board", (2, 5, 6.1), (14, 27, 6.4), "panel_black")
    cols = ["wire_red", "wire_blue", "wire_yellow", "wire_green"]
    for i in range(4):
        y = 9 + i * 4.5
        # 左と右の端子台（ねじが2本ずつ浮き出る）
        for (xa, col) in ((2.6, cols[i]), (10.4, cols[(i + 1) % 4])):
            bevel(m, "terminal", (xa, y - 0.6, 5.3), (xa + 3.0, y + 1.6, 6.1), "terminal", b=0.15)
            m.cube("terminal_tag", (xa + 0.3, y + 1.0, 5.15), (xa + 2.7, y + 1.5, 5.3), col)
            for sx in (xa + 0.8, xa + 2.2):
                m.cube("screw", (sx - 0.35, y - 0.3, 5.0), (sx + 0.35, y + 0.4, 5.3), "brass_s")
        # つないだ電線（たるむ）
        m.group(f"wire{i}", (8, y, 5.5))
        wire_sag(m, "wire", 5.6, 10.4, y, 5.0, cols[i], sag=1.0 + i * 0.15, group=f"wire{i}")
    # ランプ（縁のある丸いレンズ）
    cylz(m, "lamp_rim", 13, 26, 5.0, 6.1, 1.4, "cabinet_dark", sides=4)
    m.group("lamp_off", (13, 26, 5))
    cylz(m, "lamp_lens", 13, 26, 4.6, 5.0, 1.0, "lens_off", sides=4, group="lamp_off")
    m.group("lamp_on", (13, 26, 5))
    cylz(m, "glow_lamp", 13, 26, 4.5, 5.0, 1.05, "lens_green", sides=4, group="lamp_on")
    # 扉（右の辺がちょうつがい）：一段へこんだ板、通風のすき間、札、取っ手
    m.group("door", (15, 16, 3.8))
    m.cube("door", (1.4, 4.4, 3.6), (14.6, 27.6, 4.2), "cabinet", group="door")
    for (a, b) in (((1, 4), (15, 5)), ((1, 27), (15, 28)), ((1, 5), (2, 27)), ((14, 5), (15, 27))):
        bevel(m, "door_rim", (a[0], a[1], 3.1), (b[0], b[1], 4.2), "cabinet", b=0.15, group="door")
    for k in range(5):
        y = 6 + k * 0.9
        m.cube("vent", (4, y, 3.3), (12, y + 0.45, 3.65), "cabinet_dark", rot=[25, 0, 0], pivot=[8, y + 0.2, 3.5], group="door")
    bevel(m, "sign", (5.5, 18, 3.25), (10.5, 21.5, 3.6), "warn_yellow", b=0.1, group="door", decal=_bolt)
    bevel(m, "handle", (2.0, 14, 2.5), (2.8, 18, 3.2), "iron_fit", b=0.12, group="door")
    rivets(m, "rivet", [(1.5, 4.5), (14.5, 4.5), (1.5, 27.5), (14.5, 27.5)], 3.1, "cabinet_dark", s=0.2, group="door")
    # 箱のふち（扉が閉まる所まで前に出す）
    for (a, b) in (((1, 4), (2, 28)), ((14, 4), (15, 28))):
        m.cube("box_lip", (a[0], a[1], 4.2), (b[0], b[1], 6), "cabinet")
    return m


# ---------------------------------------------------------------- 10 予約台帳

def ledger():
    m = F("ledger", 5)
    # 文机：天板のふちが少し立ち上がる、脚と貫
    bevel(m, "desk", (-2, 9, 2), (18, 10.6, 14), "desk_top", b=0.3)
    for z in (2, 13.4):
        bevel(m, "lip", (-2, 10.6, z), (18, 11.0, z + 0.6), "desk", b=0.1)
    for (x, z) in ((-1.4, 2.6), (15.6, 2.6), (-1.4, 11.6), (15.6, 11.6)):
        bevel(m, "leg", (x, 0, z), (x + 1.8, 9, z + 1.8), "desk", b=0.25)
    bevel(m, "nuki", (-1, 2.2, 6.6), (17, 3.2, 7.4), "desk", b=0.15)
    # 台帳：表紙の板、ページの束（段々）、しおりの紐
    bevel(m, "cover", (1, 10.6, 3.8), (15, 11.2, 12.2), "book_cover", b=0.15)
    for k in range(4):
        y = 11.2 + k * 0.32
        m.cube("pages", (1.4 + k * 0.05, y, 4.2 + k * 0.05), (7.8, y + 0.3, 11.8 - k * 0.05), "page_edge" if k % 2 else "page")
        m.cube("pages", (8.2, y, 4.2 + k * 0.05), (14.6 - k * 0.05, y + 0.3, 11.8 - k * 0.05), "page" if k % 2 else "page_edge")
    m.cube("spine", (7.7, 10.9, 4.0), (8.3, 12.5, 12.0), "book_cover")
    m.cube("ribbon", (7.85, 12.4, 11.6), (8.15, 12.5, 14.4), "red_cloth")
    m.cube("ribbon_end", (7.8, 11.0, 14.0), (8.2, 12.5, 14.3), "red_cloth")
    m.group("flip", (8, 12.6, 8))
    m.cube("page", (8.1, 12.5, 4.4), (14.4, 12.75, 11.6), "page", group="flip")
    # 硯（すずり）：へこみに墨、筆置きに筆
    bevel(m, "suzuri", (14.6, 10.6, 8.6), (17.6, 11.6, 13.4), "suzuri", b=0.2)
    m.cube("sumi_pool", (15.0, 11.45, 11.6), (17.2, 11.55, 13.0), "sumi")
    m.cube("sumi_stick", (15.2, 11.6, 9.0), (16.0, 12.2, 11.0), "sumi")
    m.cube("fude_rest", (10, 10.6, 2.4), (11, 11.4, 3.2), "suzuri")
    m.cube("fude_rest", (14, 10.6, 2.4), (15, 11.4, 3.2), "suzuri")
    cylx(m, "fude", 11.6, 2.8, 9.4, 16.0, 0.3, "fude", sides=3)
    cylx(m, "fude_tip", 11.6, 2.8, 8.4, 9.4, 0.36, "fude_tip", sides=3)
    # ろうそく立てとろうそく（垂れたろう）
    m.cyl("candle_stand", -0.2, 10.8, 10.6, 11.0, 1.2, "brass_s", sides=4)
    m.cyl("candle_stand2", -0.2, 10.8, 11.0, 12.0, 0.4, "brass_s", sides=3)
    m.cyl("candle", -0.2, 10.8, 12.0, 15.2, 0.65, "candle", sides=4)
    m.cube("drip", (0.3, 13.4, 10.6), (0.6, 15.0, 10.9), "candle")
    m.cube("wick", (-0.3, 15.2, 10.7), (-0.1, 15.6, 10.9), "sumi")
    m.cube("glow_flame", (-0.55, 15.5, 10.45), (0.15, 16.9, 11.15), "glow_fire")
    return m


# ---------------------------------------------------------------- 11 布団

def futon():
    m = F("futon", 5)
    # 畳：へり（縁）が一段高い
    m.cube("tatami", (-2, 0, -6), (18, 0.45, 22), "tatami_n")
    for (a, b) in (((-2, -6), (-1, 22)), ((17, -6), (18, 22))):
        m.cube("heri", (a[0], 0, a[1]), (b[0], 0.6, b[1]), "heri")

    def seg(name, z0, z1, g):
        # 敷き布団（ふっくら：角を大きく落とす）と、掛け布団（柄と綴じ糸の玉）
        bevel(m, name, (1, 0.45, z0), (15, 1.9, z1), "shikibuton", b=0.45, group=g)
        bevel(m, name + "_cover", (1.4, 1.9, z0 + 0.1), (14.6, 2.6, z1 - 0.1), "kakebuton", b=0.3, group=g)
        for x in (4.5, 8, 11.5):
            for z in (z0 + 2, z0 + 6):
                m.cube("tuft", (x - 0.25, 2.6, z - 0.25), (x + 0.25, 2.8, z + 0.25), "kakebuton_pat", group=g)
        m.cube("stripe", (1.5, 2.6, z0 + 3.8), (14.5, 2.65, z0 + 4.2), "kakebuton_pat", group=g)
    seg("far", 12, 20, None)
    m.group("fold1", (8, 2, 12))
    seg("mid", 4, 12, "fold1")
    m.group("fold2", (8, 2, 4), "fold1")
    seg("near", -4, 4, "fold2")
    # 枕：そば殻のふくらみ（まん中が高い）とカバーの縫い目
    m.group("pillow", (8, 2, 17))
    bevel(m, "pillow", (4, 2.6, 15), (12, 4.4, 19.5), "makura", b=0.6, group="pillow")
    bevel(m, "pillow_top", (5, 4.2, 15.6), (11, 4.9, 18.9), "makura", b=0.3, group="pillow")
    m.cube("seam", (4.2, 3.4, 15.0), (11.8, 3.6, 15.05), "kakebuton", group="pillow")
    return m


# ---------------------------------------------------------------- 12 かまど

def kamado():
    import random
    rng = random.Random(12)
    m = F("kamado", 5)
    # 本体：漆喰、角を大きく落とす、上はタイル張りのへり
    rough(m, "base", (-4.6, 0, 1.2), (20.6, 1.2, 15.8), "granite_dark", rng, lumps=8, depth=0.15)
    bevel(m, "body", (-4, 1.2, 2), (20, 10.6, 15.2), "shikkui", b=0.7)
    bevel(m, "top", (-4.6, 10.4, 1.4), (20.6, 11.8, 15.6), "kamado_tile", b=0.3)
    # たき口：石のアーチ（ごつごつ）、奥に灰と炎
    for x in (-1.2, 10.8):
        m.cube("mouth", (x, 1.6, 1.7), (x + 6.4, 6.6, 2.1), "soot")
        for k, (dx0, dy0, dx1, dy1) in enumerate(((-0.8, 1.2, 0.3, 6.8), (6.1, 1.2, 7.2, 6.8))):
            rough(m, "arch", (x + dx0, dy0, 1.3), (x + dx1, dy1, 2.2), "granite", rng, lumps=2, depth=0.1)
        for k in range(5):   # 石を並べたアーチの上
            xa = x - 0.8 + k * 1.6
            bevel(m, "arch_stone", (xa, 6.6 + (0.4 if k in (1, 2, 3) else 0), 1.3), (xa + 1.55, 7.8 + (0.5 if k == 2 else 0), 2.2),
                  "granite", b=0.2)
        m.cube("ash", (x + 0.3, 1.4, 1.9), (x + 6.1, 2.1, 2.0), "ash_w")
    m.group("fire", (8, 4, 1.6))
    for x in (-1.2, 10.8):
        m.cube("glow_fire", (x + 0.8, 1.9, 1.75), (x + 5.6, 5.6, 1.85), "glow_fire", group="fire")
        m.cube("glow_core", (x + 1.4, 1.9, 1.6), (x + 5.0, 3.4, 1.75), "ember_g", group="fire")
    # 羽釜：つば（羽）が張り出し、厚い木の蓋に取っ手の桟
    m.cyl("pot", 2.5, 8.5, 11.8, 16.5, 4.4, "hagama", sides=8)
    m.cyl("pot_bottom", 2.5, 8.5, 11.2, 11.8, 3.6, "hagama", sides=8)
    m.cyl("brim", 2.5, 8.5, 13.8, 14.6, 5.8, "hagama_old", sides=8)
    m.group("lid", (2.5, 16.5, 8.5))
    m.cyl("lid", 2.5, 8.5, 16.5, 17.4, 4.2, "lid_wood", sides=8, group="lid")
    for dz in (-1.6, 1.6):
        bevel(m, "lid_bar", (-1.0, 17.4, 8.5 + dz - 0.4), (6.0, 18.1, 8.5 + dz + 0.4), "hinoki_dark", b=0.12, group="lid")
    bevel(m, "knob", (1.8, 18.1, 7.0), (3.2, 18.9, 10.0), "hinoki_dark", b=0.2, group="lid")
    # 銅の鍋（取っ手つき）
    m.cyl("pan", 13.5, 8.5, 11.8, 15.4, 3.8, "copper_r", sides=8)
    m.cyl("pan_rim", 13.5, 8.5, 15.0, 15.7, 4.2, "copper_r", sides=8)
    m.cyl("pan_lid", 13.5, 8.5, 15.7, 16.2, 3.4, "lid_wood", sides=8)
    m.cube("pan_lid_knob", (12.9, 16.2, 7.9), (14.1, 16.9, 9.1), "hinoki_dark")
    bevel(m, "pan_handle", (17.2, 14.4, 8.1), (21.4, 15.1, 8.9), "hinoki_dark", b=0.15)
    # 薪：樹皮の丸太と切り口の年輪
    for k, (z, y) in enumerate(((-2.4, 0.9), (-0.4, 0.9), (-1.4, 2.6))):
        cylx(m, "log", y, z, -3.5 + k * 0.6, 4.5 + k * 0.6, 0.9, "log_bark", sides=4)
        m.cube("log_cut", (4.45 + k * 0.6, y - 0.8, z - 0.8), (4.6 + k * 0.6, y + 0.8, z + 0.8), "log_cut", decal=_rings)
    return m

# ---------------------------------------------------------------- 13〜16 で使う材質
MATS.update({
    "sawara": Mat((210, 176, 128), "wood", accent=(182, 146, 98), noise=4),
    "hoop_copper": Mat((150, 96, 60), "metal", noise=8),
    "yu": Mat((150, 196, 210), "water", accent=(200, 230, 236), alpha=170),
    "brick": Mat((150, 74, 52), "stone", accent=(120, 56, 40), noise=10),
    "stump_bark": Mat((88, 66, 46), "stone", accent=(66, 48, 32), noise=12),
    "stump_top": Mat((196, 156, 106), "wood", accent=(160, 120, 76), noise=5),
    "split_wood": Mat((214, 178, 126), "wood", accent=(184, 146, 96), noise=5),
    "axe_head": Mat((80, 82, 86), "metal", noise=6),
    "axe_edge": Mat((196, 198, 202), "metal", noise=4),
    "axe_handle": Mat((176, 134, 84), "wood", accent=(146, 106, 62), noise=5),
    "soil_dark": Mat((84, 58, 38), "stone", accent=(66, 44, 28), noise=12),
    "soil_ridge": Mat((104, 72, 46), "stone", accent=(84, 58, 36), noise=12),
    "daikon": Mat((242, 240, 228), "plaster", noise=4),
    "ninjin": Mat((232, 116, 36), "plaster", noise=6),
    "leaf_g": Mat((76, 134, 52), "stone", accent=(58, 110, 40), noise=10),
    "leaf_l": Mat((110, 168, 70), "stone", accent=(88, 146, 54), noise=10),
    "kago": Mat((190, 160, 100), "rope", accent=(150, 122, 70)),
    "dust_g": Mat((150, 146, 138), "stipple", noise=14),
    "dust_d": Mat((112, 108, 102), "stipple", noise=14),
    "broom_straw": Mat((196, 164, 86), "rope", accent=(160, 128, 60)),
    "broom_tie": Mat((40, 90, 60), "stipple", noise=4),
    "chiritori": Mat((60, 66, 74), "metal", noise=6),
})


def _wood_rings(draw, face, x0, y0, w, h):
    """切り株・薪の上の年輪とひび"""
    if face != "up":
        return
    cx, cy = x0 + w / 2, y0 + h / 2
    for k in range(1, 7):
        r = min(w, h) * k / 14
        draw.ellipse([cx - r, cy - r, cx + r, cy + r], outline=(150, 110, 66, 255))
    draw.line([(cx, cy), (x0 + w * 0.9, y0 + h * 0.2)], fill=(90, 60, 36, 255), width=1)


# ---------------------------------------------------------------- 13 風呂釜（五右衛門風呂）

def bath_stove():
    import random
    rng = random.Random(13)
    m = F("bath_stove", 5)
    # 下はれんがの焚き口台、上に木の桶（板を並べて銅のたが）
    for row in range(4):
        y = row * 1.0
        off = 0.9 if row % 2 else 0
        x = -2.2 - off
        while x < 18.2:
            x1 = min(18.2, x + 1.9)
            bevel(m, "brick", (max(-2.2, x), y, -1.0), (x1, y + 0.95, 15.6), "brick", b=0.12)
            x += 2.0
    m.cube("brick_core", (-2.0, 0, -0.8), (18.0, 4.0, 15.4), "brick")
    # 桶：板を一周並べた壁（中が見える）と底
    from items_hires import ring
    m.cyl("oke_bottom", 8, 7.3, 4.0, 4.4, 7.4, "sawara", sides=8)
    ring(m, "oke", 8, 8.65, 7.3, 7.35, 0.7, 8.5, "sawara", seg=24, plane="xz")
    for y in (5.6, 11.4):
        ring(m, "taga", 8, y, 7.3, 7.75, 0.25, 0.9, "hoop_copper", seg=24, plane="xz")
    ring(m, "rim", 8, 13.0, 7.3, 7.35, 0.9, 0.5, "hinoki", seg=24, plane="xz")
    m.group("water", (8, 4, 7.3))
    m.cyl("glass_water", 8, 7.3, 4.4, 11.6, 6.9, "yu", sides=8, group="water")
    # 浮き板（底板。お湯に浮かぶ小さめの板）
    m.cyl("ukiita", 6.5, 8.0, 11.55, 11.75, 2.6, "sawara", sides=6, group="water")
    boards(m, "lid", 10, 17.5, 9, 15.5, 13.3, 13.9, 4, "sawara", rng, along="z", gap=0.15)
    # 焚き口（手前）：鉄の扉枠、奥で火
    bevel(m, "takiguchi", (4.4, 0.2, -1.8), (11.6, 3.8, -0.8), "iron_fit", b=0.2)
    m.cube("takiguchi_in", (5.2, 0.6, -1.95), (10.8, 3.2, -1.8), "soot")
    m.group("fire", (8, 2.5, -1.9))
    m.cube("glow_fire", (5.4, 0.7, -1.85), (10.6, 3.0, -1.75), "glow_fire", group="fire")
    m.cube("glow_coal", (5.4, 0.6, -1.9), (10.6, 1.3, -1.2), "ember_g", group="fire")
    # 煙突（れんが）と手おけ・ひしゃく
    for y in range(0, 20, 2):
        bevel(m, "chimney", (16.6, y, 11), (20.0, y + 1.95, 14.4), "brick", b=0.15)
    m.cyl("teoke", 21.5, 2.5, 0, 3.2, 1.9, "sawara", sides=6)
    m.cyl("teoke_hoop", 21.5, 2.5, 0.6, 1.1, 2.0, "hoop_copper", sides=6)
    cylx(m, "hishaku_handle", 3.6, 2.5, 22.4, 26.4, 0.25, "hinoki", sides=3)
    m.cyl("hishaku_cup", 21.8, 2.5, 3.0, 4.2, 0.8, "sawara", sides=4)
    return m


# ---------------------------------------------------------------- 14 薪割り台

def chopping_block():
    import random
    rng = random.Random(14)
    m = F("chopping_block", 5)
    # 切り株：樹皮のでこぼこ、上は年輪と斧の傷
    m.cyl("stump", 8, 8, 0, 7.6, 5.6, "stump_bark", sides=8)
    for k in range(12):
        a = 2 * math.pi * k / 12 + rng.uniform(-0.1, 0.1)
        x, z = 8 + math.cos(a) * 5.7, 8 + math.sin(a) * 5.7
        m.cube("bark", (x - 0.5, rng.uniform(0, 1.5), z - 0.5), (x + 0.5, rng.uniform(5.5, 7.4), z + 0.5), "stump_bark")
    m.cyl("stump_top", 8, 8, 7.6, 8.0, 5.3, "stump_top", sides=8)
    m.cube("rings", (3.6, 7.98, 3.6), (12.4, 8.02, 12.4), "stump_top", decal=_wood_rings)
    m.cube("scar", (5, 8.0, 7.8), (11, 8.08, 8.2), "hinoki_dark")
    m.cube("root", (1.6, 0, 6.6), (3.4, 1.2, 9.4), "stump_bark")
    m.cube("root", (12.6, 0, 7.0), (14.6, 1.0, 9.6), "stump_bark")
    # 割る薪（左右に割れる）
    for name, x0, x1 in (("half_l", 5, 8), ("half_r", 8, 11)):
        m.group(name, ((x0 + x1) / 2 + (-1.5 if name == "half_l" else 1.5), 8, 8))
        bevel(m, "log", (x0 + (0.05 if name == "half_r" else 0), 8, 5), (x1 - (0.05 if name == "half_l" else 0), 16, 11), "split_wood",
              b=0.3, group=name)
        m.cube("log_bark", (x0 - 0.15 if name == "half_l" else x1 - 0.2, 8.2, 5.2), (x0 + 0.2 if name == "half_l" else x1 + 0.15, 15.8, 10.8),
               "stump_bark", group=name)
        m.cube("log_top", (x0 + 0.2, 15.95, 5.3), (x1 - 0.2, 16.05, 10.7), "stump_top", decal=_wood_rings, group=name)
    # 斧：柄は少し曲がり、刃は厚みと光る刃先
    m.group("axe", (21, 9, 8), rot=(0, 0, 50))
    bevel(m, "handle", (20.4, 9, 7.4), (21.6, 18, 8.6), "axe_handle", b=0.3, group="axe")
    bevel(m, "handle2", (20.2, 17.6, 7.4), (21.4, 23.2, 8.6), "axe_handle", b=0.3, rot=[0, 0, -6], pivot=[20.8, 17.6, 8],
          group="axe")
    bevel(m, "head", (17.0, 19.6, 7.2), (21.8, 23.4, 8.8), "axe_head", b=0.25, group="axe")
    m.cube("bevel_edge", (16.0, 19.2, 7.5), (17.0, 23.8, 8.5), "axe_edge", group="axe")
    m.cube("wedge", (21.0, 22.6, 7.0), (22.2, 23.6, 9.0), "axe_head", group="axe")
    # 割った薪の山
    for k, (x, y, z, rz) in enumerate(((-4.5, 0, 10, 0), (-4.0, 0, 13.4, 0), (-3.8, 1.8, 11.6, 4), (-5.0, 0, 3.0, -8))):
        bevel(m, "pile", (x, y, z), (x + 5.0, y + 1.8, z + 2.2), "split_wood", b=0.3, rot=[0, rz, 0], pivot=[x + 2.5, y, z + 1.1])
    return m


# ---------------------------------------------------------------- 15 畑

def vegetable_patch():
    import random
    rng = random.Random(15)
    m = F("vegetable_patch", 5)
    # 畝（うね）：盛り上がった土の山と、土くれ
    m.cube("bed", (-4, 0, -2), (20, 1.8, 18), "soil_dark")
    for z0 in (0.2, 9.2):
        bevel(m, "ridge", (-3.6, 1.8, z0), (19.6, 3.4, z0 + 6.6), "soil_ridge", b=0.7)
        for _ in range(10):
            x, z = rng.uniform(-3, 18.6), rng.uniform(z0 + 0.4, z0 + 5.8)
            m.cube("clod", (x, 3.3, z), (x + rng.uniform(0.5, 1.1), 3.3 + rng.uniform(0.2, 0.5), z + rng.uniform(0.5, 1.0)), "soil_dark")
    # 野菜（大根と人参が交互）：根・肩・広がる葉
    k = 0
    for z in (2, 11):
        for x in (-2, 3, 8, 13):
            g = m.group(f"veg{k}", (x + 1.5, 4, z + 1.5))
            daikon = k % 2 == 0
            root = "daikon" if daikon else "ninjin"
            m.cyl("root", x + 1.5, z + 1.5, -2.6, 3.4, 1.2 if daikon else 0.9, root, sides=4, group=g)
            m.cyl("tip", x + 1.5, z + 1.5, -4.2, -2.6, 0.6 if daikon else 0.4, root, sides=3, group=g)
            m.cyl("shoulder", x + 1.5, z + 1.5, 3.4, 4.2, 1.0 if daikon else 0.7, "leaf_l" if daikon else root, sides=4, group=g)
            for j in range(5):
                a = j * 72 + k * 13
                m.cube("leaf", (x + 1.3, 4.2, z + 1.3), (x + 1.7, 8.6 - (j % 2) * 1.2, z + 1.7), "leaf_g" if j % 2 else "leaf_l",
                       rot=[18, a, 0], pivot=[x + 1.5, 4.2, z + 1.5], group=g)
                m.cube("leaflet", (x + 0.9, 6.5 - (j % 2), z + 1.4), (x + 2.1, 7.8 - (j % 2), z + 1.6), "leaf_g",
                       rot=[18, a, 0], pivot=[x + 1.5, 4.2, z + 1.5], group=g)
            k += 1
    # 収穫かご
    m.cyl("kago", 9, -4, 0, 2.6, 3.0, "kago", sides=6)
    m.cyl("kago_rim", 9, -4, 2.4, 2.9, 3.2, "hinoki_dark", sides=6)
    return m


# ---------------------------------------------------------------- 16 ほこり（掃除）

def dust_pile():
    import random
    rng = random.Random(16)
    m = F("dust_pile", 5)
    # ほこりの山：ふわふわ（小さな塊を重ねる）
    m.group("dust", (6, 0, 7))
    for k in range(18):
        cx, cz = 6 + rng.uniform(-4, 4), 7 + rng.uniform(-3.5, 3.5)
        r = rng.uniform(0.8, 2.0)
        h = rng.uniform(0.5, 2.4) * (1 - (abs(cx - 6) + abs(cz - 7)) / 12)
        m.cube("dust", (cx - r, 0, cz - r), (cx + r, max(0.3, h), cz + r), "dust_g" if k % 3 else "dust_d", group="dust")
    for k in range(5):
        x, z = rng.uniform(-3, 13), rng.uniform(0, 14)
        m.cube("fluff", (x, 0, z), (x + 0.8, 0.3, z + 0.8), "dust_g", group="dust")
    # ちり取り（ふちが立ち上がる）
    m.cube("pan", (12, 0, 1), (18, 0.4, 6), "chiritori")
    m.cube("pan_back", (12, 0.4, 5.4), (18, 3.2, 6), "chiritori")
    for x in (12, 17.6):
        m.cube("pan_side", (x, 0.4, 2.6), (x + 0.4, 2.6, 5.4), "chiritori", rot=[-20, 0, 0], pivot=[x, 0.4, 5.4])
    cylz(m, "pan_handle", 15, 1.6, 6, 11.4, 0.45, "fude", sides=3)
    # ほうき：穂は細いわらの束（すき間あり）、緑の糸で巻く
    m.group("broom", (8, 22, 9))
    m.cyl("stick", 8, 9, 5.2, 26, 0.55, "fude", sides=4, group="broom")
    for k in range(9):
        x = 4.8 + k * 0.75
        m.cube("straw", (x, 0, 8.3 + (k % 2) * 0.3), (x + 0.6, 6.0 - abs(k - 4) * 0.2, 9.5 + (k % 2) * 0.3), "broom_straw",
               rot=[0, 0, (k - 4) * 2.5], pivot=[8, 6, 9], group="broom")
    for y in (4.6, 5.6):
        m.cube("tie", (6.6, y, 8.1), (9.4, y + 0.5, 9.9), "broom_tie", group="broom")
    m.cube("cap", (7.3, 6.0, 8.5), (8.7, 7.2, 9.5), "broom_tie", group="broom")
    return m

# ---------------------------------------------------------------- 17〜20 で使う材質
MATS.update({
    "take": Mat((190, 176, 104), "bamboo", accent=(150, 138, 70), noise=4),
    "take_node": Mat((150, 138, 74), "metal", noise=4),
    "cloth_w": Mat((240, 238, 230), "stipple", noise=4),
    "yukata": Mat((60, 84, 150), "stipple", noise=5),
    "yukata_pat": Mat((240, 236, 226), "stipple", noise=3),
    "tenugui": Mat((226, 120, 120), "stipple", noise=5),
    "pin": Mat((200, 170, 120), "wood", noise=4),
    "celadon": Mat((150, 190, 172), "metal", noise=5),
    "celadon_d": Mat((112, 152, 136), "metal", noise=5),
    "petal_pink": Mat((240, 150, 176), "stipple", noise=6),
    "petal_red": Mat((206, 46, 52), "stipple", noise=6),
    "petal_y": Mat((246, 206, 70), "stipple", noise=6),
    "flower_c": Mat((250, 220, 90), "stipple", noise=6),
    "stem_g": Mat((80, 130, 56), "plain", noise=4),
    "leaf_dk": Mat((60, 110, 46), "stone", noise=8),
    "copper_can": Mat((182, 108, 66), "metal", noise=8),
    "cab_wood": Mat((150, 108, 66), "wood", accent=(120, 82, 46), noise=5),
    "cab_panel": Mat((170, 126, 80), "wood", accent=(138, 98, 58), noise=5),
    "counter_s": Mat((140, 140, 136), "stone", noise=8),
    "sink_steel": Mat((196, 200, 204), "metal", noise=5),
    "plate": Mat((246, 244, 238), "plaster", noise=3),
    "plate_rim": Mat((60, 90, 150), "plain", noise=3),
    "plate_dirty": Mat((214, 196, 160), "stipple", noise=10),
    "water_f": Mat((120, 176, 220), "water", accent=(190, 225, 245), alpha=150),
    "getabako": Mat((132, 92, 56), "wood", accent=(104, 70, 40), noise=5),
    "sneaker": Mat((236, 236, 232), "stipple", noise=4),
    "sneaker_line": Mat((210, 50, 40), "plain", noise=3),
    "leather": Mat((70, 44, 30), "metal", noise=6),
})


def flower(m, name, x, y, z, color, g, r=1.2):
    """花：まん中の芯と、十字と斜めに重ねた花びら"""
    m.cube(name, (x - r, y - 0.25, z - 0.4), (x + r, y + 0.25, z + 0.4), color, group=g)
    m.cube(name, (x - 0.4, y - 0.25, z - r), (x + 0.4, y + 0.25, z + r), color, group=g)
    m.cube(name, (x - r * 0.8, y - 0.2, z - 0.35), (x + r * 0.8, y + 0.2, z + 0.35), color, rot=[0, 45, 0], pivot=[x, y, z], group=g)
    m.cube(name, (x - r * 0.8, y - 0.2, z - 0.35), (x + r * 0.8, y + 0.2, z + 0.35), color, rot=[0, -45, 0], pivot=[x, y, z], group=g)
    m.cube(name + "_c", (x - 0.4, y, z - 0.4), (x + 0.4, y + 0.5, z + 0.4), "flower_c", group=g)


def bent(m, name, pts, w, mat, group=None):
    """折れ線（茎・取っ手など）：点 (x,y,z) を細い棒でつなぐ（xy 平面の向きだけ曲げる）"""
    for (xa, ya, za), (xb, yb, zb) in zip(pts, pts[1:]):
        ln = math.dist((xa, ya, za), (xb, yb, zb))
        cx, cy, cz = (xa + xb) / 2, (ya + yb) / 2, (za + zb) / 2
        rz = math.degrees(math.atan2(-(xb - xa), yb - ya))
        rx = math.degrees(math.atan2(zb - za, math.hypot(xb - xa, yb - ya)))
        m.cube(name, (cx - w / 2, cy - ln / 2 - 0.05, cz - w / 2), (cx + w / 2, cy + ln / 2 + 0.05, cz + w / 2), mat,
               rot=[rx, 0, rz], pivot=[cx, cy, cz], group=group)


# ---------------------------------------------------------------- 17 物干し

def laundry_pole():
    import random
    rng = random.Random(17)
    m = F("laundry_pole", 5)
    for x in (-3, 17):
        rough(m, "stone", (x - 1, 0, 6), (x + 3, 2, 10), "granite", rng, lumps=4, depth=0.2)
        m.cyl("post", x + 1, 8, 2, 28, 0.9, "take", sides=4)
        for y in (8, 14, 20):
            m.cyl("node", x + 1, 8, y, y + 0.4, 1.05, "take_node", sides=4)
        # 二股（Y）の先
        m.cube("fork", (x + 0.6, 26, 7.6), (x + 1.4, 29, 8.4), "take", rot=[0, 0, 20], pivot=[x + 1, 26, 8])
        m.cube("fork", (x + 0.6, 26, 7.6), (x + 1.4, 29, 8.4), "take", rot=[0, 0, -20], pivot=[x + 1, 26, 8])
    cylx(m, "pole", 27, 8, -4, 20, 0.75, "take", sides=4)
    for x in (2, 8, 14):
        cylx(m, "pole_node", 27, 8, x, x + 0.4, 0.9, "take_node", sides=4)
    # 洗濯物：しわ（細い帯を前後にずらして並べる）、洗濯ばさみ
    cloths = [("cloth_w", -0.5, 3.5, 9), ("yukata", 4.5, 10.5, 11), ("tenugui", 11.5, 14.5, 8), ("cloth_w", 15, 17.5, 12)]
    for i, (mat, x0, x1, h) in enumerate(cloths):
        g = m.group(f"cloth{i}", ((x0 + x1) / 2, 26, 8))
        n = max(3, int((x1 - x0) / 0.8))
        for k in range(n):
            xa = x0 + (x1 - x0) * k / n
            xb = x0 + (x1 - x0) * (k + 1) / n
            dz = 0.25 * math.sin(k * 1.7)
            hh = h - (0.6 if k % 3 == 1 else 0)
            m.cube("cloth", (xa, 26 - hh, 7.65 + dz), (xb + 0.02, 26.2, 8.35 + dz), mat, group=g)
        if mat == "yukata":
            for k in range(6):
                y = 26 - 1.5 - k * 1.5
                m.cube("pattern", (x0 + 0.6 + (k % 2) * 2, y, 7.5), (x0 + 1.4 + (k % 2) * 2, y + 0.8, 7.6), "yukata_pat", group=g)
        for xp in (x0 + 0.3, x1 - 0.7):
            m.cube("pin", (xp, 25.2, 7.4), (xp + 0.4, 27.6, 8.6), "pin", group=g)
    # 洗濯かごとたたんだ服
    m.cyl("basket", 7, -1, 0, 3.6, 4.0, "kago", sides=6)
    m.cyl("basket_rim", 7, -1, 3.4, 3.9, 4.2, "hinoki_dark", sides=6)
    for k, mat in enumerate(("yukata", "cloth_w", "tenugui")):
        bevel(m, "folded", (4.2 + k * 0.3, 3.0 + k * 0.6, -3.4), (9.8 - k * 0.3, 3.6 + k * 0.6, 1.2), mat, b=0.15)
    return m


# ---------------------------------------------------------------- 18 花瓶

def flower_vase():
    m = F("flower_vase", 6)
    # 台：漆の花台、脚
    bevel(m, "stand", (0, 3, 0), (16, 4.2, 16), "urushi_k", b=0.3)
    for (x, z) in ((0.6, 0.6), (13.6, 0.6), (0.6, 13.6), (13.6, 13.6)):
        bevel(m, "stand_leg", (x, 0, z), (x + 1.8, 3, z + 1.8), "urushi_k", b=0.25)
    m.cube("shikiita", (4, 4.2, 4), (12, 4.5, 12), "hinoki")
    # 花瓶：青磁、ふくらみと首、口の縁（輪切りでなめらかに）
    prof = [(4.5, 2.6), (5.5, 3.2), (6.5, 3.6), (7.5, 3.7), (8.5, 3.5), (9.5, 3.0), (10.5, 2.3), (11.5, 1.7), (12.5, 1.5),
            (13.5, 1.6), (14.2, 2.0)]
    for (ya, ra), (yb, rb) in zip(prof, prof[1:]):
        m.cyl("vase", 8, 8, ya, yb, (ra + rb) / 2, "celadon", sides=8)
    m.cyl("vase_lip", 8, 8, 14.2, 14.7, 2.2, "celadon_d", sides=8)
    m.cyl("vase_mouth", 8, 8, 14.68, 14.72, 1.6, "deep_dark", sides=6)
    # 花（茎は曲がり、葉がつき、花びらが重なる）
    m.group("flowers", (8, 15, 8))
    for i, (dx, dz, ln, col) in enumerate(((-1.2, -0.6, 9, "petal_pink"), (1.0, 0.4, 11, "petal_red"), (0.0, 1.0, 13, "petal_y"),
                                           (-0.6, 0.8, 8, "petal_pink"))):
        top = (8 + dx * 2.6, 14.5 + ln, 8 + dz * 2.0)
        mid = (8 + dx * 1.2, 14.5 + ln * 0.5, 8 + dz)
        bent(m, "stem", [(8 + dx * 0.4, 14, 8 + dz * 0.3), mid, top], 0.4, "stem_g", group="flowers")
        flower(m, "petal", top[0], top[1] + 0.3, top[2], col, "flowers", r=1.3 if i != 2 else 1.6)
        m.cube("leaf", (mid[0], mid[1], mid[2] - 0.2), (mid[0] + 2.2, mid[1] + 0.3, mid[2] + 0.6), "leaf_dk",
               rot=[0, i * 80, 25], pivot=[mid[0], mid[1], mid[2]], group="flowers")
    # じょうろ（銅）：胴・注ぎ口・はす口・取っ手
    m.group("can", (16, 22, 8), rot=(0, 0, 25))
    m.cyl("can_body", 16, 8, 19, 24, 2.6, "copper_can", sides=8, group="can")
    m.cyl("can_top", 16, 8, 24, 24.6, 1.8, "copper_can", sides=6, group="can")
    bent(m, "spout", [(13.6, 20.2, 8), (11.5, 21.6, 8), (9.6, 22.6, 8)], 0.6, "copper_can", group="can")
    m.cube("rose", (8.6, 22.0, 7.2), (9.8, 23.4, 8.8), "copper_can", group="can")
    bent(m, "handle", [(18.4, 20, 8), (19.6, 22.5, 8), (18.6, 25.2, 8), (16.6, 25.4, 8)], 0.5, "copper_can", group="can")
    return m


# ---------------------------------------------------------------- 19 流し台

def kitchen_sink():
    import random
    rng = random.Random(19)
    m = F("kitchen_sink", 5)
    # 下の戸棚：框（かまち）が出っぱり、鏡板がへこむ、つまみ
    bevel(m, "cabinet", (-2, 0, 2.6), (18, 14, 16), "cab_wood", b=0.3)
    for x0 in (-1.6, 8.2):
        for (a, b) in (((x0, 0.8), (x0 + 9.4, 1.6)), ((x0, 12.4), (x0 + 9.4, 13.2)), ((x0, 1.6), (x0 + 0.8, 12.4)),
                       ((x0 + 8.6, 1.6), (x0 + 9.4, 12.4))):
            bevel(m, "stile", (a[0], a[1], 2.0), (b[0], b[1], 2.6), "cab_wood", b=0.12)
        m.cube("panel", (x0 + 0.8, 1.6, 2.35), (x0 + 8.6, 12.4, 2.6), "cab_panel")
        bevel(m, "knob", (x0 + (8.0 if x0 < 0 else 0.9), 6.4, 1.4), (x0 + (8.5 if x0 < 0 else 1.4), 8.6, 2.0), "hinoki_dark", b=0.1)
    m.cube("kick", (-1.6, 0, 2.4), (17.6, 0.8, 2.7), "hinoki_dark")
    # 石の天板（ふちを落とす）と、くぼんだ流し（壁と底が別）
    bevel(m, "counter", (-3, 14, 1), (19, 15.4, 17), "counter_s", b=0.3)
    m.cube("basin_floor", (1, 14.6, 5), (11, 15.0, 13), "sink_steel")
    for (a, b) in (((0, 4), (12, 5)), ((0, 13), (12, 14)), ((0, 5), (1, 13)), ((11, 5), (12, 13))):
        bevel(m, "basin_wall", (a[0], 14.6, a[1]), (b[0], 17.6, b[1]), "sink_steel", b=0.15)
    m.cube("drain", (5.4, 15.0, 8.4), (6.6, 15.05, 9.6), "deep_dark")
    m.cube("glass_water", (1, 15.0, 5), (11, 16.0, 13), "water_f")
    # 筧（かけい）：竹の管が上から水を引く
    m.cyl("pipe_up", 14, 8, 15.4, 27, 0.9, "take", sides=4)
    for y in (18, 22, 26):
        m.cyl("pipe_node", 14, 8, y, y + 0.4, 1.05, "take_node", sides=4)
    cylx(m, "pipe_arm", 25.6, 8, 5.4, 14.8, 0.75, "take", sides=4)
    m.cube("pipe_cut", (5.0, 24.8, 7.2), (5.4, 26.4, 8.8), "take_node")
    m.group("stream", (6, 25, 8))
    m.cube("glass_stream", (5.6, 16, 7.6), (6.4, 25, 8.4), "water_f", group="stream")
    m.cube("glass_splash", (4.6, 16.0, 6.6), (7.4, 16.3, 9.4), "water_f", group="stream")
    # 汚れた皿（流しの中に重ねる）と、きれいな皿（水切りかごに立てる）
    for i in range(5):
        g = m.group(f"dirty{i}", (6, 16 + i * 0.7, 9))
        y = 16 + i * 0.7
        ox = (i % 2) * 0.5
        m.cyl("dish", 6 + ox, 9, y, y + 0.35, 2.8, "plate_dirty", sides=6, group=g)
        m.cyl("dish_rim", 6 + ox, 9, y + 0.3, y + 0.55, 3.0, "plate", sides=6, group=g)
    bevel(m, "rack", (12.6, 15.4, 9.6), (19, 16.2, 16.8), "take", b=0.15)
    for k in range(7):
        x = 12.8 + k * 0.95
        m.cube("rack_bar", (x, 16.2, 9.8), (x + 0.3, 19.4, 10.2), "take")
        m.cube("rack_bar", (x, 16.2, 16.2), (x + 0.3, 19.4, 16.6), "take")
    for i in range(5):
        g = m.group(f"clean{i}", (14 + i * 1.1, 19, 13))
        x = 13.6 + i * 1.1
        m.cube("dish", (x, 16.2, 10.6), (x + 0.4, 21.6, 15.8), "plate", rot=[0, 0, -8], pivot=[x, 16.2, 13], group=g)
        m.cube("dish_rim", (x - 0.05, 16.4, 10.4), (x + 0.45, 21.8, 10.7), "plate_rim", rot=[0, 0, -8], pivot=[x, 16.2, 13],
               group=g)
    # 洗い道具：たわしとふきん
    bevel(m, "tawashi", (-2.4, 15.4, 2.0), (0.2, 16.6, 4.0), "kago", b=0.3)
    m.cube("fukin", (-2.6, 15.4, 13.0), (0.2, 15.7, 16.4), "cloth_w")
    return m


# ---------------------------------------------------------------- 20 下駄箱

def shoe_rack():
    m = F("shoe_rack", 5)
    # 本体：背板、側板（ふちを落とす）、棚板、仕切り。上に一輪挿しと敷物
    bevel(m, "back", (0, 0, 15), (16, 22, 16), "getabako", b=0.15)
    for x in (0, 15):
        bevel(m, "side", (x, 0, 3.6), (x + 1, 22.6, 16), "getabako", b=0.2)
    bevel(m, "top", (-0.4, 21.6, 3.2), (16.4, 22.8, 16.2), "getabako", b=0.3)
    bevel(m, "bottom", (0.6, 0, 3.8), (15.4, 1.2, 15.4), "getabako", b=0.15)
    bevel(m, "shelf", (1, 10.4, 4.0), (15, 11.4, 15.2), "getabako", b=0.15)
    bevel(m, "divider", (7.5, 1.2, 4.0), (8.5, 21.6, 15.2), "getabako", b=0.15)
    m.cube("kick", (0.6, 0, 3.4), (15.4, 1.0, 3.8), "hinoki_dark")
    m.cube("cloth", (2, 22.8, 6.8), (7, 23.0, 12.2), "red_cloth")
    m.cyl("ichirin", 4.5, 9.5, 23.0, 26.4, 0.9, "celadon", sides=6)
    m.cyl("ichirin_neck", 4.5, 9.5, 26.4, 27.4, 0.45, "celadon_d", sides=4)
    bent(m, "stem", [(4.5, 27, 9.5), (5.4, 30, 9.2)], 0.3, "stem_g")
    flower(m, "petal", 5.4, 30.4, 9.2, "petal_red", None, r=0.9)
    m.cyl("pot", 12.5, 9.5, 22.8, 25.8, 1.6, "celadon_d", sides=6)
    for k in range(5):
        m.cube("plant", (11.6, 25.6, 9.3), (13.4, 29 - (k % 2), 9.7), "leaf_dk", rot=[15, k * 72, 0], pivot=[12.5, 25.6, 9.5])
    # 靴：下駄・草履・運動靴・革靴（左右1足ずつ）
    cub = ((1.5, 1.2), (9, 1.2), (1.5, 11.4), (9, 11.4))
    kinds = ("geta", "sneaker", "zori", "leather")
    k = 0
    for i in range(4):
        for s_ in range(2):
            x = cub[i][0] + s_ * 3.2
            y = cub[i][1]
            g = m.group(f"shoe{k}", (x + 1.3, y, 9))
            kind = kinds[i]
            if kind == "geta":
                bevel(m, "geta", (x, y + 0.5, 6), (x + 2.6, y + 1.2, 13), "kiri_light", b=0.2, group=g)
                for z in (7, 11):
                    m.cube("tooth", (x + 0.2, y, z), (x + 2.4, y + 0.5, z + 0.8), "hinoki_dark", group=g)
                m.cube("hanao", (x + 1.1, y + 1.2, 7), (x + 1.5, y + 1.8, 9), "hanao_red", group=g)
            elif kind == "zori":
                bevel(m, "zori", (x, y, 6), (x + 2.6, y + 0.5, 13), "igusa", b=0.12, group=g)
                m.cube("hanao", (x + 1.1, y + 0.5, 7), (x + 1.5, y + 1.0, 9), "hanao_navy", group=g)
            elif kind == "sneaker":
                bevel(m, "sole", (x, y, 6), (x + 2.6, y + 0.6, 13), "switch_white", b=0.15, group=g)
                bevel(m, "upper", (x + 0.15, y + 0.6, 6.4), (x + 2.45, y + 2.4, 12.4), "sneaker", b=0.4, group=g)
                m.cube("line", (x + 0.1, y + 1.0, 7.5), (x + 2.5, y + 1.3, 11.5), "sneaker_line", group=g)
                m.cube("lace", (x + 0.6, y + 2.3, 7.0), (x + 2.0, y + 2.5, 9.2), "switch_white", group=g)
            else:
                bevel(m, "sole", (x, y, 6), (x + 2.6, y + 0.5, 13), "leather", b=0.12, group=g)
                bevel(m, "upper", (x + 0.15, y + 0.5, 6.3), (x + 2.45, y + 2.1, 12.6), "leather", b=0.5, group=g)
                m.cube("heel", (x + 0.2, y, 11.6), (x + 2.4, y + 0.8, 13), "deep_dark", group=g)
            k += 1
    return m

# ---------------------------------------------------------------- 21〜24 で使う材質
MATS.update({
    "shoji_wood": Mat((222, 196, 152), "wood", accent=(196, 166, 120), noise=3),
    "shoji_paper": Mat((246, 242, 228), "paper", noise=3),
    "kumiko": Mat((206, 178, 132), "wood", accent=(180, 150, 108), noise=3),
    "koshi": Mat((178, 136, 88), "wood", accent=(148, 108, 64), noise=5),
    "andon_paper": Mat((240, 230, 200), "paper", noise=3),
    "andon_lit": Mat((255, 214, 140), "paper", accent=(255, 240, 200), glow=True),
    "ash_r": Mat((176, 170, 160), "stone", accent=(140, 134, 126), noise=12),
    "iron_r": Mat((54, 50, 48), "metal", noise=8),
    "fish_skin": Mat((150, 140, 110), "stipple", accent=(110, 90, 60), noise=10),
    "fish_belly": Mat((226, 214, 186), "stipple", noise=5),
    "bonsai_bark": Mat((92, 70, 54), "stone", accent=(70, 52, 40), noise=12),
    "pine_d": Mat((40, 92, 52), "stone", accent=(30, 70, 40), noise=14),
    "pine_l": Mat((76, 132, 70), "stone", accent=(56, 108, 52), noise=14),
    "bonsai_pot": Mat((70, 92, 120), "metal", noise=6),
    "pebble": Mat((182, 176, 166), "stone", noise=10),
})


# ---------------------------------------------------------------- 21 障子

def shoji_screen():
    m = F("shoji_screen", 6)
    # 敷居（溝つき）と鴨居、柱：障子の外側に立つので骨（screen）の外
    bevel(m, "shikii", (-2, 0, 5.6), (18, 0.8, 10.4), "hinoki_dark", b=0.2)
    m.cube("shikii_groove", (-2, 0.8, 7.4), (18, 0.85, 8.6), "deep_dark")
    bevel(m, "kamoi", (-2, 32.4, 5.6), (18, 34, 10.4), "hinoki_dark", b=0.2)
    for x in (-3.2, 17.2):
        bevel(m, "hashira", (x, 0, 5.2), (x + 2, 34.8, 10.8), "hinoki", b=0.3)
    m.group("screen", (8, 0, 8))
    g = "screen"
    # 框（かまち）：まわりの太い枠。ふちを落とす
    bevel(m, "kamachi_l", (-1, 0.8, 6.8), (1.4, 32.4, 9.2), "shoji_wood", b=0.25, group=g)
    bevel(m, "kamachi_r", (14.6, 0.8, 6.8), (17, 32.4, 9.2), "shoji_wood", b=0.25, group=g)
    bevel(m, "kamachi_t", (1.4, 30.6, 6.8), (14.6, 32.4, 9.2), "shoji_wood", b=0.2, group=g)
    bevel(m, "kamachi_b", (1.4, 0.8, 6.8), (14.6, 2.6, 9.2), "shoji_wood", b=0.2, group=g)
    # 腰板：下の木の板（へこんだ鏡板と見切り）
    bevel(m, "koshi_rail", (1.4, 7.4, 6.9), (14.6, 8.4, 9.1), "shoji_wood", b=0.15, group=g)
    m.cube("koshi", (1.4, 2.6, 7.4), (14.6, 7.4, 8.6), "koshi", group=g)
    for x in (4.6, 8, 11.4):
        m.cube("koshi_bead", (x - 0.15, 2.6, 7.25), (x + 0.15, 7.4, 8.75), "shoji_wood", group=g)
    # 障子紙は奥にへこみ、組子が前後に出る
    m.cube("paper", (1.4, 8.4, 7.85), (14.6, 30.6, 8.15), "shoji_paper", group=g)
    for x in (4.7, 8, 11.3):
        m.cube("kumiko", (x - 0.3, 8.4, 7.2), (x + 0.3, 30.6, 8.8), "kumiko", group=g)
    y = 12.8
    while y < 30.4:
        m.cube("kumiko", (1.4, y - 0.3, 7.2), (14.6, y + 0.3, 8.8), "kumiko", group=g)
        y += 4.4
    # 引手（手をかけるくぼみ）
    m.cube("hikite", (14.9, 14.5, 6.75), (16.1, 16.9, 6.85), "urushi_k", group=g)
    m.cube("hikite", (14.9, 14.5, 9.15), (16.1, 16.9, 9.25), "urushi_k", group=g)
    # 穴と、そこに貼る桜の切り紙
    for k, (hx, hy) in enumerate(((3, 10.6), (9.6, 15), (6.3, 21.2), (12.9, 25.6))):
        m.group(f"hole{k}", (hx, hy, 8), g)
        m.cube("hole", (hx - 0.9, hy - 0.9, 7.8), (hx + 0.9, hy + 0.9, 8.2), "deep_dark", group=f"hole{k}")
        m.cube("tear", (hx - 1.2, hy - 0.4, 7.75), (hx - 0.9, hy + 0.6, 8.25), "shoji_paper", rot=[0, 0, 25], pivot=[hx, hy, 8],
               group=f"hole{k}")
        m.group(f"patch{k}", (hx, hy, 8), g)
        for a in range(5):
            m.cube("petal", (hx - 0.55, hy, 7.7), (hx + 0.55, hy + 1.5, 8.3), "pink_cloth", rot=[0, 0, a * 72],
                   pivot=[hx, hy, 8], group=f"patch{k}")
        m.cube("petal_c", (hx - 0.35, hy - 0.35, 7.65), (hx + 0.35, hy + 0.35, 8.35), "petal_red", group=f"patch{k}")
    return m


# ---------------------------------------------------------------- 22 行灯

def andon():
    m = F("andon", 6)
    # 台：引き出しつきの箱、反った足
    bevel(m, "base", (2.2, 1.0, 2.2), (13.8, 3.6, 13.8), "cedar_dark", b=0.3)
    bevel(m, "drawer", (4, 1.5, 1.9), (12, 3.1, 2.3), "cedar", b=0.12)
    m.cube("drawer_knob", (7.5, 2.0, 1.5), (8.5, 2.6, 1.9), "brass_s")
    for (x, z) in ((2.0, 2.0), (12.6, 2.0), (2.0, 12.6), (12.6, 12.6)):
        bevel(m, "foot", (x, 0, z), (x + 1.4, 1.2, z + 1.4), "cedar_dark", b=0.2)
    # 柱と横木（ほぞが少し出る）
    for (x, z) in ((3, 3), (12.1, 3), (3, 12.1), (12.1, 12.1)):
        bevel(m, "post", (x, 3.6, z), (x + 0.9, 20.4, z + 0.9), "cedar_dark", b=0.15)
    for y in (10, 19.4):
        for (a, b, c, d) in ((2.8, 2.9, 13.2, 4.0), (2.8, 12.0, 13.2, 13.1), (2.9, 2.8, 4.0, 13.2), (12.0, 2.8, 13.1, 13.2)):
            bevel(m, "rail", (a, y, b), (c, y + 0.6, d), "cedar_dark", b=0.12)
    bevel(m, "top", (2.4, 20.4, 2.4), (13.6, 21.2, 13.6), "cedar_dark", b=0.25)
    # 中の棚と油皿、灯心
    bevel(m, "shelf", (3.9, 9.2, 3.9), (12.1, 9.7, 12.1), "cedar", b=0.1)
    m.cyl("dish_stand", 8, 8, 9.7, 10.2, 1.0, "urushi_r", sides=4)
    m.cyl("dish", 8, 8, 10.2, 10.7, 1.9, "urushi_r", sides=6)
    m.cyl("oil", 8, 8, 10.6, 10.75, 1.5, "lacquer", sides=6)
    m.cube("wick", (7.8, 10.7, 7.0), (8.2, 11.0, 9.4), "rope")
    m.group("flame", (8, 11, 8))
    m.cube("glow_flame", (7.45, 11, 7.45), (8.55, 12.8, 8.55), "glow_fire", group="flame")
    m.cube("glow_tip", (7.7, 12.8, 7.7), (8.3, 13.9, 8.3), "glow_fire", group="flame")
    # 和紙：桟（細い格子）を外に出し、紙は少し奥
    m.group("paper_off", (8, 15, 8))
    m.group("paper_on", (8, 15, 8))
    for gname, mat in (("paper_off", "andon_paper"), ("paper_on", "andon_lit")):
        pre = "glow_" if gname == "paper_on" else ""
        m.cube(pre + "pn", (3.9, 10.6, 3.55), (12.1, 19.4, 3.7), mat, group=gname)
        m.cube(pre + "ps", (3.9, 10.6, 12.3), (12.1, 19.4, 12.45), mat, group=gname)
        m.cube(pre + "pw", (3.55, 10.6, 3.9), (3.7, 19.4, 12.1), mat, group=gname)
        m.cube(pre + "pe", (12.3, 10.6, 3.9), (12.45, 19.4, 12.1), mat, group=gname)
    for y in (13.0, 16.2):
        for (a, b, c, d) in ((3.9, 3.3, 12.1, 3.55), (3.9, 12.45, 12.1, 12.7), (3.3, 3.9, 3.55, 12.1), (12.45, 3.9, 12.7, 12.1)):
            m.cube("san", (a, y, b), (c, y + 0.3, d), "cedar_dark")
    for x in (8,):
        for (a, b, c, d) in ((x - 0.15, 3.3, x + 0.15, 3.55), (x - 0.15, 12.45, x + 0.15, 12.7)):
            m.cube("san", (a, 10.6, b), (c, 19.4, d), "cedar_dark")
        for (a, b, c, d) in ((3.3, x - 0.15, 3.55, x + 0.15), (12.45, x - 0.15, 12.7, x + 0.15)):
            m.cube("san", (a, 10.6, b), (c, 19.4, d), "cedar_dark")
    # 取っ手（弓なり）と金具
    bent(m, "handle", [(8, 21.2, 3.6), (8, 23.6, 4.4), (8, 24.6, 6.4), (8, 24.8, 8), (8, 24.6, 9.6), (8, 23.6, 11.6), (8, 21.2, 12.4)],
         0.8, "cedar_dark")
    for z in (3.4, 12.0):
        m.cube("fitting", (7.4, 21.2, z), (8.6, 21.6, z + 0.6), "brass_s")
    return m


# ---------------------------------------------------------------- 23 囲炉裏

def irori():
    from items_hires import ring
    import random
    rng = random.Random(23)
    m = F("irori", 6)
    # 炉縁（ろぶち）：ふちを落とした太い木、角は留め
    for (a, b) in (((-6, -6), (22, -3)), ((-6, 19), (22, 22)), ((-6, -3), (-3, 19)), ((19, -3), (22, 19))):
        bevel(m, "robuchi", (a[0], 0, a[1]), (b[0], 2.2, b[1]), "hinoki_dark", b=0.4)
    for (x, z) in ((-6, -6), (19, -6), (-6, 19), (19, 19)):
        m.cube("miter", (x + 0.2, 2.2, z + 0.2), (x + 2.8, 2.25, z + 2.8), "hinoki")
    # 灰：まん中がへこみ、まわりは盛り上がる。灰ならしの筋
    m.cube("ash", (-3, 0, -3), (19, 0.9, 19), "ash_r")
    for (a, b) in (((-3, -3), (19, 0)), ((-3, 16), (19, 19)), ((-3, 0), (0, 16)), ((16, 0), (19, 16))):
        m.cube("ash_mound", (a[0], 0.9, a[1]), (b[0], 1.5, b[1]), "ash_r")
    for k in range(5):
        m.cube("ash_line", (-2.6, 1.5, -2.4 + k * 0.6), (18.6, 1.55, -2.2 + k * 0.6), "ash_w")
    # 炭：ごつごつした塊を積む
    coals = ((4, 5), (8, 4), (11, 6), (6, 9), (10, 10), (5, 12), (8, 7.2))
    for (x, z) in coals:
        rough(m, "coal", (x, 0.9, z), (x + 2.6, 2.4, z + 2.2), "charcoal", rng, lumps=2, depth=0.2)
    m.group("ember", (8, 2.4, 8))
    for (x, z) in coals:
        m.cube("glow_ember", (x + 0.3, 2.35, z + 0.3), (x + 2.3, 2.55, z + 1.9), "ember", group="ember")
        m.cube("glow_ember", (x - 0.05, 1.2, z + 0.5), (x + 0.05, 2.0, z + 1.7), "ember", group="ember")
    m.group("fire", (8, 2.6, 8))
    for i, (dx, h) in enumerate(((-1.4, 4.2), (0, 5.6), (1.3, 4.0))):
        m.cube("glow_fire", (8 + dx - 0.9, 2.4, 7.9), (8 + dx + 0.9, 2.4 + h, 8.1), "glow_fire", group="fire")
        m.cube("glow_fire", (8 + dx - 0.1, 2.4, 6.4 + i), (8 + dx + 0.1, 2.4 + h * 0.8, 9.6 - i * 0.5), "glow_fire", group="fire")
    # 五徳（鉄の輪と脚）
    ring(m, "gotoku", 8, 6.2, 8, 3.1, 0.5, 0.4, "iron_r", seg=12, plane="xz")
    for a in range(3):
        ang = math.radians(a * 120 + 30)
        x, z = 8 + 3.15 * math.cos(ang), 8 + 3.15 * math.sin(ang)
        m.cube("gotoku_leg", (x - 0.25, 0.9, z - 0.25), (x + 0.25, 6.4, z + 0.25), "iron_r")
    # 自在鉤：竹の筒、横木（魚の形）、鉤
    m.cyl("jizai", 8, 8, 18, 40, 0.8, "bamboo", sides=4)
    for y in (22, 28, 34):
        m.cyl("jizai_node", 8, 8, y, y + 0.4, 0.95, "take_node", sides=4)
    m.cube("jizai_rod", (7.7, 12.6, 7.7), (8.3, 18, 8.3), "iron_r")
    bevel(m, "fish", (3.6, 18.6, 7.3), (11.4, 20.6, 8.7), "hinoki_dark", b=0.5)
    m.cube("fish_tail", (2.2, 18.2, 7.5), (3.8, 21.0, 8.5), "hinoki_dark")
    m.cube("fish_eye", (10.2, 19.8, 7.2), (10.7, 20.3, 8.8), "deep_dark")
    m.cube("hook", (7.6, 12.0, 7.6), (8.4, 12.8, 9.0), "iron_r")
    # 鉄瓶：まるい胴、注ぎ口、つる
    for (ya, yb, r) in ((6.4, 7.2, 2.8), (7.2, 9.2, 3.6), (9.2, 10.4, 3.2), (10.4, 11.0, 2.4)):
        m.cyl("tetsubin", 8, 8, ya, yb, r, "tetsubin", sides=8)
    m.cyl("tetsubin_lid", 8, 8, 11.0, 11.4, 1.8, "iron_r", sides=6)
    m.cube("lid_knob", (7.6, 11.4, 7.6), (8.4, 11.9, 8.4), "brass_s")
    bent(m, "spout", [(11.2, 8.4, 8), (12.8, 9.4, 8), (13.6, 10.6, 8)], 0.8, "tetsubin")
    bent(m, "tsuru", [(5.0, 10.8, 8), (5.4, 12.4, 8), (8, 12.9, 8), (10.6, 12.4, 8), (11.0, 10.8, 8)], 0.4, "iron_r")
    # 串に刺した魚を灰に立てる
    for k, (x, z) in enumerate(((0.4, 2), (0.4, 8), (0.4, 14), (15.6, 4), (15.6, 12))):
        lean = 14 if x < 8 else -14
        m.cube("kushi", (x - 0.15, 1.2, z - 0.15), (x + 0.15, 9, z + 0.15), "bamboo", rot=[0, 0, lean], pivot=[x, 1.2, z])
        bevel(m, "ayu", (x - 0.55, 4.2, z - 0.35), (x + 0.55, 8.2, z + 0.35), "fish_skin", b=0.2, rot=[0, 0, lean],
              pivot=[x, 1.2, z])
        m.cube("ayu_belly", (x - 0.6, 4.6, z - 0.2), (x - 0.5, 7.6, z + 0.2), "fish_belly", rot=[0, 0, lean], pivot=[x, 1.2, z])
    # 座布団（厚み・ふくらみ・房）
    for x0 in (-13, 23):
        bevel(m, "zabuton", (x0, 0, 3.5), (x0 + 6, 1.4, 12.5), "purple_cloth", b=0.45)
        bevel(m, "zabuton_top", (x0 + 0.5, 1.4, 4.0), (x0 + 5.5, 1.7, 12.0), "purple_cloth", b=0.3)
        for (x, z) in ((x0 + 0.2, 3.7), (x0 + 5.4, 3.7), (x0 + 0.2, 11.9), (x0 + 5.4, 11.9)):
            m.cube("fusa", (x, 0.8, z), (x + 0.4, 1.4, z + 0.4), "yellow_cloth")
    return m


# ---------------------------------------------------------------- 24 盆栽

def bonsai():
    from items_hires import ring
    import random
    rng = random.Random(24)
    m = F("bonsai", 6)
    # 台：脚つき、天板のふちを落とす
    bevel(m, "stand_top", (0.5, 3.0, 2.5), (15.5, 4.0, 13.5), "wood_dark", b=0.3)
    bevel(m, "stand_apron", (1.2, 2.2, 3.2), (14.8, 3.0, 12.8), "wood_dark", b=0.15)
    for (x, z) in ((1.2, 3.2), (13.4, 3.2), (1.2, 11.4), (13.4, 11.4)):
        bevel(m, "stand_leg", (x, 0, z), (x + 1.4, 2.2, z + 1.4), "wood_dark", b=0.2)
    # 鉢：長方形、足、口のふち（中は土）
    bevel(m, "pot", (2.6, 4.6, 4.6), (13.4, 7.6, 11.4), "bonsai_pot", b=0.3)
    for (a, b) in (((2.3, 4.3), (13.7, 5.0)), ((2.3, 11.0), (13.7, 11.7)), ((2.3, 5.0), (3.0, 11.0)), ((13.0, 5.0), (13.7, 11.0))):
        bevel(m, "pot_rim", (a[0], 7.6, a[1]), (b[0], 8.2, b[1]), "bonsai_pot", b=0.12)
    for (x, z) in ((3.0, 5.0), (11.8, 5.0), (3.0, 9.8), (11.8, 9.8)):
        m.cube("pot_foot", (x, 4.0, z), (x + 1.2, 4.6, z + 1.2), "bonsai_pot")
    m.cube("soil", (3.0, 7.5, 5.0), (13.0, 7.9, 11.0), "soil_dark")
    for (x, z, w) in ((3.4, 5.4, 3), (8.6, 8.2, 3.6), (5.2, 8.8, 2.4), (10.4, 5.6, 2.2)):
        rough(m, "moss", (x, 7.9, z), (x + w, 8.5, z + 1.8), "moss", rng, lumps=2, depth=0.1)
    rough(m, "rock", (9.6, 7.9, 6.0), (12.0, 9.4, 7.8), "pebble", rng, lumps=3, depth=0.2)
    # 幹：根張りからくねって伸びる
    for (x, z) in ((6.4, 7.2), (8.4, 7.4), (7.4, 8.6)):
        m.cube("nebari", (x - 0.9, 7.9, z - 0.6), (x + 0.9, 8.6, z + 0.6), "bonsai_bark")
    trunk = [(7.4, 8.0, 8), (7.0, 10.0, 8), (8.2, 12.2, 8), (7.2, 14.6, 8), (8.0, 17.0, 8), (7.4, 19.0, 8)]
    for k, ((xa, ya, za), (xb, yb, zb)) in enumerate(zip(trunk, trunk[1:])):
        bent(m, "trunk", [(xa, ya, za), (xb, yb, zb)], 1.6 - k * 0.2, "bonsai_bark")
    bent(m, "branch_l", [(7.4, 13.8, 8), (5.0, 14.8, 7.8), (2.8, 15.6, 7.6)], 0.6, "bonsai_bark")
    bent(m, "branch_r", [(8.0, 16.0, 8), (10.4, 17.0, 8.2), (12.4, 17.4, 8.4)], 0.6, "bonsai_bark")
    bent(m, "branch_t", [(7.6, 18.6, 8), (6.6, 19.6, 7.8)], 0.5, "bonsai_bark")
    # 葉の雲：濃い色の下段と明るい上段を重ねて、ふくらみを出す
    pads = ((1.0, 15.8, 6.0, 5.4), (9.6, 17.4, 6.4, 5.2), (4.2, 19.6, 5.8, 4.8))
    for i, (x, y, w, d) in enumerate(pads):
        z0 = 8 - d / 2
        rough(m, "pad", (x, y, z0), (x + w, y + 1.4, z0 + d), "pine_d", rng, lumps=3, depth=0.25)
        rough(m, "pad_top", (x + 0.6, y + 1.4, z0 + 0.6), (x + w - 0.6, y + 2.3, z0 + d - 0.6), "pine_l", rng, lumps=3, depth=0.2)
        # 伸びすぎた枝（はさみで切る）
        m.group(f"wild{i}", (x + 1, y + 2.3, 8))
        m.cube("wild", (x + 0.8, y + 2.2, 7.4), (x + 2.0, y + 2.3 + 3 + i, 8.6), "pine_l", group=f"wild{i}")
        m.cube("wild_tip", (x + 0.6, y + 2.3 + 3 + i, 7.2), (x + 2.2, y + 3.0 + 3 + i, 8.8), "pine_d", group=f"wild{i}")
        m.cube("wild2", (x + w - 0.6, y + 0.4, 7.4), (x + w + 2.2, y + 1.4, 8.6), "pine_l", group=f"wild{i}")
    # 剪定ばさみ：刃2枚と、輪の持ち手
    for gname, (y0, y1) in (("blade_a", (14.0, 14.5)), ("blade_b", (13.5, 14.0))):
        m.group(gname, (16, 14, 8), rot=(0, 0, 20))
        bevel(m, "blade", (12.6, y0, 7.6), (16.4, y1, 8.4), "steel_light", b=0.1, group=gname)
        m.cube("edge", (12.2, y0 + 0.1, 7.75), (12.6, y1 - 0.1, 8.25), "steel_light", group=gname)
    m.cube("pivot_pin", (15.7, 13.3, 7.3), (16.3, 14.9, 8.7), "brass_s")
    ring(m, "grip", 19.6, 15.8, 8, 1.1, 0.5, 0.7, "red_cloth", seg=10)
    ring(m, "grip", 19.6, 12.2, 8, 1.1, 0.5, 0.7, "red_cloth", seg=10)
    m.cube("shank", (16.2, 14.4, 7.7), (18.2, 14.9, 8.3), "steel_light")
    m.cube("shank", (16.2, 13.1, 7.7), (18.2, 13.6, 8.3), "steel_light")
    return m

# ---------------------------------------------------------------- 25〜28 で使う材質
MATS.update({
    "mosen": Mat((176, 34, 38), "stipple", accent=(150, 24, 30), noise=6),
    "raku": Mat((40, 34, 32), "stone", accent=(70, 60, 54), noise=10),
    "kama_iron": Mat((52, 48, 44), "stone", accent=(74, 68, 60), noise=14),
    "natsume": Mat((30, 22, 18), "metal", noise=4),
    "natsume_gold": Mat((210, 170, 80), "metal", noise=6),
    "mizusashi": Mat((210, 200, 176), "stone", accent=(150, 120, 90), noise=10),
    "well_stone": Mat((134, 130, 120), "stone", accent=(104, 100, 92), noise=12),
    "well_wood": Mat((112, 82, 54), "wood", accent=(86, 60, 38), noise=6),
    "oke": Mat((196, 160, 112), "wood", accent=(170, 132, 86), noise=5),
    "kaya": Mat((214, 176, 96), "wood", accent=(192, 150, 72), noise=4),
    "koma": Mat((232, 204, 140), "wood", accent=(210, 178, 112), noise=3),
    "zabuton_n": Mat((50, 60, 110), "stipple", noise=5),
    "toko_wood": Mat((122, 86, 56), "wood", accent=(96, 64, 40), noise=5),
    "kinran": Mat((168, 136, 70), "stipple", accent=(200, 168, 96), noise=8),
    "hyogu": Mat((96, 120, 110), "stipple", noise=5),
    "jiku": Mat((40, 30, 24), "metal", noise=4),
    "smoke": Mat((220, 220, 220), "plain", alpha=110),
})


# ---------------------------------------------------------------- 25 茶道具

def tea_set():
    from items_hires import ring
    m = F("tea_set", 6)
    # 毛氈：ふちを折り返して厚みを出す
    bevel(m, "mat", (-4, 0, -2), (20, 0.35, 18), "mosen", b=0.12)
    for (a, b) in (((-4, -2), (20, -1.4)), ((-4, 17.4), (20, 18)), ((-4, -1.4), (-3.4, 17.4)), ((19.4, -1.4), (20, 17.4))):
        m.cube("mat_hem", (a[0], 0.35, a[1]), (b[0], 0.5, b[1]), "mosen")
    # 風炉：脚と口、釜（羽と鐶）
    for a in range(3):
        ang = math.radians(90 + a * 120)
        x, z = 2 + 3.2 * math.cos(ang), 13 + 3.2 * math.sin(ang)
        bevel(m, "furo_leg", (x - 0.6, 0.35, z - 0.6), (x + 0.6, 1.4, z + 0.6), "cast_iron_old", b=0.2)
    for (ya, yb, r) in ((1.4, 2.2, 3.4), (2.2, 4.4, 4.0), (4.4, 5.4, 4.2)):
        m.cyl("furo", 2, 13, ya, yb, r, "kama_iron", sides=8)
    ring(m, "furo_rim", 2, 5.7, 13, 3.8, 0.7, 0.6, "cast_iron", seg=16, plane="xz")
    m.cube("furo_window", (-0.6, 2.0, 9.0), (4.6, 3.8, 9.2), "deep_dark")
    m.cube("glow_ember", (0.2, 2.2, 9.1), (3.8, 2.6, 9.25), "ember_g")
    for (ya, yb, r) in ((5.4, 6.2, 2.6), (6.2, 7.6, 3.5), (7.6, 9.4, 3.6), (9.4, 10.2, 3.0)):
        m.cyl("kama", 2, 13, ya, yb, r, "kama_iron", sides=8)
    ring(m, "hane", 2, 7.6, 13, 3.9, 0.7, 0.3, "kama_iron", seg=16, plane="xz")
    m.cyl("kama_lid", 2, 13, 10.2, 10.6, 2.1, "copper_r", sides=6)
    m.cyl("kama_knob", 2, 13, 10.6, 11.2, 0.5, "copper_r", sides=4)
    for z in (9.2, 16.8):
        m.cube("kan_ear", (1.6, 8.4, z - 0.2), (2.4, 9.4, z + 0.2), "kama_iron")
        ring(m, "kan", 2, 8.0, z, 0.8, 0.25, 0.3, "iron_black", seg=8)
    # 茶碗：楽茶碗（黒、厚い口、高台）。内側に抹茶、上に泡
    m.cyl("kodai", 11.5, 5.5, 0.35, 1.1, 1.6, "raku", sides=6)
    for (ya, yb, r) in ((1.1, 1.8, 2.8), (1.8, 4.2, 3.4)):
        m.cyl("bowl", 11.5, 5.5, ya, yb, r, "raku", sides=8)
    ring(m, "bowl_rim", 11.5, 4.4, 5.5, 3.2, 0.6, 0.5, "raku", seg=16, plane="xz")
    m.cyl("tea", 11.5, 5.5, 4.0, 4.6, 2.9, "matcha_r", sides=8)
    m.group("foam", (11.5, 4.6, 5.5))
    m.cyl("foam", 11.5, 5.5, 4.6, 4.9, 2.8, "foam_r", sides=8, group="foam")
    # 茶筅：穂は細い棒を輪に並べ、内穂と柄
    m.group("whisk", (11.5, 5, 5.5))
    for k in range(10):
        a = 2 * math.pi * k / 10
        x, z = 11.5 + 0.85 * math.cos(a), 5.5 + 0.85 * math.sin(a)
        m.cube("tine", (x - 0.12, 4.4, z - 0.12), (x + 0.12, 7.2, z + 0.12), "bamboo_r", rot=[math.degrees(math.sin(a)) * -0.12,
               0, math.degrees(math.cos(a)) * 0.12], pivot=[x, 7.2, z], group="whisk")
    m.cyl("tines_in", 11.5, 5.5, 5.4, 7.2, 0.45, "bamboo_r", sides=4, group="whisk")
    m.cyl("tie", 11.5, 5.5, 7.2, 7.6, 0.75, "deep_dark", sides=4, group="whisk")
    m.cyl("handle", 11.5, 5.5, 7.6, 11.6, 0.45, "bamboo_r", sides=4, group="whisk")
    # 棗（金蒔絵の帯）と茶杓
    for (ya, yb, r, mt) in ((0.35, 0.9, 1.3, "natsume"), (0.9, 2.6, 1.7, "natsume"), (2.6, 2.9, 1.75, "natsume_gold"),
                            (2.9, 3.6, 1.6, "natsume"), (3.6, 4.0, 1.2, "natsume")):
        m.cyl("natsume", 17.5, 4.5, ya, yb, r, mt, sides=8)
    bent(m, "chashaku", [(14.4, 0.6, 9.5), (17.0, 0.6, 9.8), (19.6, 0.9, 10.2)], 0.4, "bamboo_r")
    m.cube("chashaku_tip", (19.4, 0.6, 9.9), (20.0, 1.3, 10.5), "bamboo_r")
    # 水指（焼き物）と柄杓、蓋置
    for (ya, yb, r) in ((0.35, 4.0, 2.4), (4.0, 4.6, 2.6)):
        m.cyl("mizusashi", 16, 14.5, ya, yb, r, "mizusashi", sides=8)
    m.cyl("mizusashi_lid", 16, 14.5, 4.6, 4.9, 2.2, "urushi_k", sides=8)
    m.cyl("futaoki", 7.5, 15, 0.35, 1.8, 0.9, "take", sides=4)
    m.cyl("hishaku_cup", 7.5, 15, 1.8, 3.0, 1.0, "hinoki", sides=6)
    bent(m, "hishaku_handle", [(7.5, 2.8, 14.0), (8.5, 2.2, 9.5), (9.0, 0.6, 6.2)], 0.4, "hinoki")
    return m


# ---------------------------------------------------------------- 26 井戸

def well():
    from items_hires import ring
    import random
    rng = random.Random(26)
    m = F("well", 6)
    # 井筒：ごつごつした石を積み、上に石の縁。中は暗く深い
    for (a, b) in (((1, 1), (15, 3)), ((1, 13), (15, 15)), ((1, 3), (3, 13)), ((13, 3), (15, 13))):
        for k, y in enumerate((0, 3, 6)):
            rough(m, "ishi", (a[0] - 0.2 * (k % 2), y, a[1] - 0.2 * (k % 2)), (b[0] + 0.2 * (k % 2), y + 3, b[1] + 0.2 * (k % 2)),
                  "well_stone", rng, lumps=4, depth=0.3)
    for (a, b) in (((0.4, 0.4), (15.6, 3.2)), ((0.4, 12.8), (15.6, 15.6)), ((0.4, 3.2), (3.2, 12.8)), ((12.8, 3.2), (15.6, 12.8))):
        bevel(m, "rim", (a[0], 9, a[1]), (b[0], 10.2, b[1]), "granite", b=0.3)
    for (x, z) in ((2, 13.6), (13.2, 1.2), (1.4, 6)):
        m.cube("moss", (x, 10.15, z), (x + rng.uniform(1, 2), 10.3, z + rng.uniform(0.8, 1.6)), "moss")
    m.cube("shaft", (3, -2, 3), (13, 9, 13), "deep_dark")
    m.cube("glass_water", (3, 2.6, 3), (13, 3.0, 13), "water")
    # 柱・梁・屋根（瓦）
    for x in (-2.2, 16.2):
        rough(m, "post_base", (x - 0.6, 0, 6.4), (x + 2.6, 1.2, 9.6), "granite", rng, lumps=2, depth=0.15)
        bevel(m, "post", (x, 1.2, 7), (x + 2, 26, 9), "well_wood", b=0.3)
    bevel(m, "beam", (-3.4, 24, 6.4), (19.4, 25.6, 9.6), "well_wood", b=0.3)
    bevel(m, "ridge_post", (7.2, 25.6, 7.2), (8.8, 27.6, 8.8), "well_wood", b=0.2)
    for sgn in (-1, 1):
        rx = -26 * sgn
        z0, z1 = (1.6, 8.2) if sgn < 0 else (7.8, 14.4)
        m.cube("roof_board", (-4.6, 26.6, z0), (20.6, 27.2, z1), "well_wood", rot=[rx, 0, 0], pivot=[8, 27.6, 8])
        for x in range(-4, 21, 2):
            m.cube("kawara", (x - 0.1, 27.2, z0), (x + 1.4, 27.9, z1), "kawara", rot=[rx, 0, 0], pivot=[8, 27.6, 8])
            m.cube("kawara_round", (x + 1.35, 27.2, z0), (x + 2.0, 28.2, z1), "kawara", rot=[rx, 0, 0], pivot=[8, 27.6, 8])
    bevel(m, "munagawara", (-5, 27.6, 7.2), (21, 29.0, 8.8), "kawara", b=0.3)
    for x in (-5.6, 20.6):
        m.cube("onigawara", (x, 27.0, 6.8), (x + 1.0, 29.6, 9.2), "kawara")
    # 滑車（輪と軸）
    m.cube("pulley_hanger", (7.6, 22.8, 6.8), (8.4, 24, 9.2), "iron_black")
    ring(m, "pulley", 8, 22, 8, 1.2, 0.5, 0.9, "iron_black", seg=12)
    cylz(m, "pulley_axle", 8, 22, 6.8, 9.2, 0.25, "iron_black")
    # 釣瓶（つるべ）：たが付きの桶と手（group: bucket）
    m.group("bucket", (8, 8, 8))
    m.cube("rope", (6.85, 10.4, 7.85), (7.15, 22, 8.15), "rope", group="bucket")
    m.cube("rope", (9.0, 10.4, 7.85), (9.3, 22, 8.15), "rope", group="bucket")
    ring(m, "oke", 8, 8.1, 8, 1.95, 0.35, 4.2, "oke", seg=12, plane="xz", group="bucket")
    m.cyl("oke_floor", 8, 8, 6.0, 6.4, 2.0, "oke", sides=6, group="bucket")
    for y in (7.0, 9.4):
        ring(m, "taga", 8, y, 8, 2.15, 0.15, 0.5, "hoop_copper", seg=12, plane="xz", group="bucket")
    m.cyl("glass_bucketwater", 8, 8, 6.4, 9.0, 1.8, "water", sides=6, group="bucket")
    m.cube("tsuru", (5.6, 10.0, 7.7), (10.4, 10.6, 8.3), "well_wood", group="bucket")
    for x in (5.6, 9.8):
        m.cube("tsuru_side", (x, 8.6, 7.7), (x + 0.6, 10.6, 8.3), "well_wood", group="bucket")
    # たらい（水がたまる）
    ring(m, "tarai", 22, 1.9, -2, 3.8, 0.5, 3.8, "oke", seg=16, plane="xz")
    m.cyl("tarai_floor", 22, -2, 0, 0.5, 3.8, "oke", sides=8)
    ring(m, "tarai_taga", 22, 1.6, -2, 4.1, 0.2, 0.6, "hoop_copper", seg=16, plane="xz")
    m.group("tubwater", (22, 0.5, -2))
    m.cyl("glass_water", 22, -2, 0.5, 3.6, 3.5, "water", sides=8, group="tubwater")
    return m


# ---------------------------------------------------------------- 27 将棋盤

def _koma_text(draw, face, x0, y0, w, h):
    if face != "up":
        return
    draw.line([(x0 + w * 0.5, y0 + h * 0.3), (x0 + w * 0.5, y0 + h * 0.75)], fill=(30, 20, 14, 255), width=1)
    draw.line([(x0 + w * 0.3, y0 + h * 0.45), (x0 + w * 0.7, y0 + h * 0.45)], fill=(30, 20, 14, 255), width=1)


def shogi_board():
    from furniture_hires import _grid
    m = F("shogi_board", 6)
    # 盤：厚い榧の盤、ふちを落とす。上面に目盛り
    m.cube("board", (0, 4.0, 0), (16, 9, 16), "kaya", decal=_grid)
    bevel(m, "board_base", (0.3, 3.6, 0.3), (15.7, 4.0, 15.7), "kaya", b=0.15)
    m.cube("heso", (6, 3.55, 6), (10, 3.6, 10), "deep_dark")
    # 脚：くちなしの実の形（ふくらんで、くびれる）
    for (x, z) in ((3, 3), (13, 3), (3, 13), (13, 13)):
        for (ya, yb, r) in ((0, 0.6, 1.0), (0.6, 1.4, 1.6), (1.4, 2.4, 1.8), (2.4, 3.0, 1.3), (3.0, 3.6, 1.0)):
            m.cyl("kuchinashi", x, z, ya, yb, r, "kaya", sides=8)
    # 駒：五角形（胴と、斜めに削った頭）。寝かせて並べる
    for i in range(18):
        row = 0 if i < 9 else 2
        col = i % 9
        px = 1.2 + col * 1.55
        pz = 1.2 + row * 1.55
        g = m.group(f"piece{i}", (px, 9, pz))
        cx, cz = px + 0.575, pz + 0.65
        # とがった頭は相手（奥）の方を向く
        m.cube("koma", (px, 9, pz), (px + 1.15, 9.5, pz + 0.95), "koma", group=g, decal=_koma_text)
        m.cube("koma_head", (cx - 0.42, 9, cz + 0.1), (cx + 0.42, 9.45, cz + 0.62), "koma", rot=[0, 45, 0], pivot=[cx, 9, cz + 0.3],
               group=g)
    # 対局者の座布団は盤の手前と奥（駒の並びの正面）。駒台はそれぞれの右手側、駒箱は横
    for (z0, kx) in ((-12.6, 17.4), (22.6, -5.4)):
        bevel(m, "zabuton", (3, 0, z0), (13, 1.4, z0 + 6.0), "zabuton_n", b=0.45)
        bevel(m, "zabuton_top", (3.5, 1.4, z0 + 0.5), (12.5, 1.7, z0 + 5.5), "zabuton_n", b=0.3)
        kz = z0 + 1.0 if z0 < 0 else z0 + 1.0
        bevel(m, "komadai", (kx, 4.6, kz), (kx + 4, 5.2, kz + 4), "kaya", b=0.15)
        m.cyl("komadai_leg", kx + 2, kz + 2, 0, 4.6, 0.7, "kaya", sides=4)
        bevel(m, "komadai_foot", (kx + 0.6, 0, kz + 0.6), (kx + 3.4, 0.5, kz + 3.4), "kaya", b=0.15)
    for k in range(3):
        m.cube("koma", (17.8 + k * 1.2, 5.2, -11.2 + (k % 2) * 1.4), (18.8 + k * 1.2, 5.6, -10.0 + (k % 2) * 1.4), "koma")
    bevel(m, "box", (17.6, 0, 6.0), (21.6, 2.2, 10.0), "urushi_k", b=0.2)
    bevel(m, "box_lid", (17.4, 2.2, 5.8), (21.8, 2.7, 10.2), "urushi_k", b=0.15)
    m.cube("box_kamon", (18.9, 2.7, 7.3), (20.3, 2.75, 8.7), "natsume_gold")
    return m


# ---------------------------------------------------------------- 28 掛け軸

def kakejiku():
    from furniture_hires import _ink
    m = F("kakejiku", 6)
    # 床の間：床框（黒漆）、床板、床柱、落とし掛け、壁
    bevel(m, "tokobashira", (-3.6, 0, 13.4), (-1.4, 34, 16), "toko_wood", b=0.4)
    bevel(m, "tokogamachi", (-1.4, 0, 8.6), (19, 2.2, 10), "urushi_k", b=0.25)
    m.cube("tokoita", (-1.4, 0, 10), (19, 2.0, 15.6), "toko_wood")
    bevel(m, "otoshigake", (-1.4, 32.6, 13.6), (19, 34, 15.0), "toko_wood", b=0.2)
    m.cube("nail", (7.7, 30, 15.0), (8.3, 31.4, 15.6), "iron_black")
    # 掛け軸：天地（上下の布）、一文字、中廻し、本紙、風帯、軸先
    m.group("scroll", (8, 30, 15))
    g = "scroll"
    m.cube("himo", (7.85, 29.4, 14.95), (8.15, 31.2, 15.15), "rope", group=g)
    bevel(m, "hasso", (1.6, 28.4, 14.5), (14.4, 29.4, 15.3), "jiku", b=0.2, group=g)
    m.cube("ten", (2.2, 23.6, 14.75), (13.8, 28.4, 15.1), "hyogu", group=g)
    m.cube("chi", (2.2, 5.4, 14.75), (13.8, 8.2, 15.1), "hyogu", group=g)
    m.cube("naka", (2.2, 8.2, 14.7), (13.8, 23.6, 15.1), "kinran", group=g)
    for y in (21.8, 9.6):
        m.cube("ichimonji", (3.4, y, 14.62), (12.6, y + 0.9, 14.7), "natsume_gold", group=g)
    m.cube("honshi", (3.4, 10.5, 14.6), (12.6, 21.8, 14.7), "paper", group=g, decal=_ink)
    for x in (5.4, 9.8):
        m.cube("futai", (x, 20.6, 14.55), (x + 0.8, 28.4, 14.7), "kinran", group=g)
        m.cube("futai_line", (x + 0.2, 20.6, 14.5), (x + 0.6, 28.4, 14.56), "natsume_gold", group=g)
    cylx(m, "jiku", 5.0, 14.9, 1.6, 14.4, 0.7, "hyogu", group=g)
    for x0, x1 in ((0.6, 1.6), (14.4, 15.4)):
        cylx(m, "jikusaki", 5.0, 14.9, x0, x1, 0.75, "jiku", group=g)
    # 香炉（脚と蓋、煙）と一輪の花
    for (ya, yb, r) in ((2.0, 2.6, 1.4), (2.6, 4.2, 1.8), (4.2, 4.6, 1.5)):
        m.cyl("koro", 5, 12.4, ya, yb, r, "bronze", sides=8)
    m.cyl("koro_lid", 5, 12.4, 4.6, 5.2, 1.2, "bronze_dark", sides=6)
    m.cyl("koro_knob", 5, 12.4, 5.2, 5.8, 0.4, "bronze_dark", sides=4)
    for k, (dx, y) in enumerate(((0, 6.4), (0.4, 8.2), (-0.2, 10.0), (0.3, 11.8))):
        m.cube("glass_smoke", (5 + dx - 0.2, y - 0.9, 12.2), (5 + dx + 0.2, y + 0.9, 12.6), "smoke", rot=[0, 0, (k % 2 * 2 - 1) * 12],
               pivot=[5 + dx, y, 12.4])
    for (ya, yb, r) in ((2.0, 4.6, 1.1), (4.6, 7.4, 0.7), (7.4, 8.0, 0.9)):
        m.cyl("hanaire", 12, 12.4, ya, yb, r, "mizusashi", sides=6)
    bent(m, "stem", [(12, 7.6, 12.4), (12.6, 10.6, 12.2), (13.6, 12.4, 12.0)], 0.3, "stem_g")
    m.cube("leaf", (12.4, 9.4, 12.0), (14.0, 9.7, 12.6), "leaf_dk", rot=[0, 0, 20], pivot=[12.4, 9.5, 12.3])
    flower(m, "petal", 13.6, 12.7, 12.0, "plate", None, r=0.9)
    return m

# ---------------------------------------------------------------- 29〜32 で使う材質
MATS.update({
    "post_red": Mat((196, 34, 30), "metal", accent=(160, 22, 20), noise=6),
    "post_red_d": Mat((150, 24, 22), "metal", noise=6),
    "post_plate": Mat((240, 238, 230), "plain", noise=2),
    "glass_rim": Mat((180, 220, 248), "plain", noise=1, alpha=120),
    "glass_clear": Mat((225, 242, 255), "plain", noise=1, alpha=48),
    "glass_edge": Mat((200, 232, 255), "plain", noise=1, alpha=110),
    "water_thin": Mat((170, 215, 245), "plain", noise=1, alpha=30),
    "fish_fin": Mat((250, 170, 110), "paper", noise=4, alpha=200),
    "goldfish_b": Mat((255, 96, 24), "metal", accent=(255, 180, 60), noise=6),
    "pebble_w": Mat((222, 218, 206), "stone", noise=8),
    "pebble_b": Mat((90, 100, 110), "stone", noise=8),
    "weed": Mat((70, 150, 70), "plain", noise=6),
    "can_y": Mat((240, 196, 60), "metal", noise=5),
    "kiri_box": Mat((228, 208, 172), "wood", accent=(206, 184, 146), noise=3),
    "keyaki": Mat((170, 112, 60), "wood", accent=(140, 86, 42), noise=6),
    "keyaki_d": Mat((120, 76, 40), "wood", accent=(96, 58, 30), noise=6),
    "mochi_s": Mat((248, 246, 240), "plaster", noise=3),
})


# ---------------------------------------------------------------- 29 郵便受け（丸型ポスト）

def mailbox():
    import random
    rng = random.Random(29)
    m = F("mailbox", 6)
    # 台座：石
    rough(m, "base", (3, 0, 3.5), (13, 1.4, 13.5), "granite", rng, lumps=4, depth=0.2)
    # 胴：丸い筒（赤）、上は丸屋根、ひさし
    m.cyl("body_foot", 8, 8.5, 1.4, 2.4, 4.6, "post_red_d", sides=8)
    m.cyl("body", 8, 8.5, 2.4, 20.6, 4.2, "post_red", sides=8)
    for y in (6.0, 17.2):
        m.cyl("band", 8, 8.5, y, y + 0.4, 4.35, "post_red_d", sides=8)
    m.cyl("eave", 8, 8.5, 20.6, 21.4, 4.9, "post_red_d", sides=8)
    for (ya, yb, r) in ((21.4, 22.4, 4.4), (22.4, 23.2, 3.6), (23.2, 23.8, 2.6), (23.8, 24.2, 1.4)):
        m.cyl("dome", 8, 8.5, ya, yb, r, "post_red", sides=8)
    # 投函口とふた（lid）、表示板、取り出し口と鍵
    m.cube("slot_frame", (4.6, 18.6, 4.0), (11.4, 20.2, 4.4), "post_red_d")
    m.cube("slot", (5.0, 19.0, 3.95), (11.0, 19.8, 4.05), "deep_dark")
    m.group("lid", (8, 21, 4.8))
    bevel(m, "lid", (4.2, 20.2, 3.5), (11.8, 21.2, 4.1), "post_red", b=0.15, group="lid")
    bevel(m, "plate", (5.2, 13.6, 4.0), (10.8, 16.8, 4.3), "post_plate", b=0.1, decal=_text("郵便", size=0.5))
    bevel(m, "door", (5.4, 4.6, 4.0), (10.6, 11.6, 4.35), "post_red", b=0.12)
    m.cube("keyhole", (9.2, 7.6, 3.8), (9.8, 8.6, 4.05), "brass_s")
    m.cube("emblem", (7.2, 22.0, 4.0), (8.8, 22.8, 4.3), "post_plate")
    # 仕分けた手紙：てっぺんの取り出し口からのぞく
    m.cube("top_slot", (5.6, 24.15, 7.6), (10.4, 24.25, 9.4), "deep_dark")
    for k in range(4):
        m.group(f"sorted{k}", (5 + k * 2, 22, 9))
        x = 5.8 + k * 1.15
        m.cube("letter", (x, 22.6, 7.8), (x + 0.3, 26.0 + (k % 2) * 0.6, 9.2), ("letter", "letter_pink", "letter_blue", "letter")[k],
               rot=[0, 0, (k - 1.5) * 4], pivot=[x, 23, 8.5], group=f"sorted{k}")
    # 仕分ける前の手紙の山：脚つきの小机の上
    bevel(m, "stand", (16, 9, 5), (23, 10, 11.5), "hinoki", b=0.2)
    for (x, z) in ((16.4, 5.4), (21.8, 5.4), (16.4, 10.3), (21.8, 10.3)):
        bevel(m, "stand_leg", (x, 0, z), (x + 0.8, 9, z + 0.8), "hinoki_dark", b=0.12)
    m.cube("stand_nuki", (16.8, 3, 5.6), (22.2, 3.6, 6.0), "hinoki_dark")
    for k in range(4):
        m.group(f"pile{k}", (19, 10 + k * 0.5, 8))
        bevel(m, "letter", (17, 10 + k * 0.5, 6.5 + (k % 2) * 0.6), (21.5, 10.4 + k * 0.5, 9.5 + (k % 2) * 0.6),
              ("letter", "letter_pink", "letter_blue", "letter")[k], b=0.08, group=f"pile{k}", rot=(0, (k - 1.5) * 8, 0),
              pivot=(19, 10, 8))
        m.cube("stamp", (20.4, 10.42 + k * 0.5, 6.8 + (k % 2) * 0.6), (21.1, 10.45 + k * 0.5, 7.5 + (k % 2) * 0.6), "red",
               group=f"pile{k}", rot=(0, (k - 1.5) * 8, 0), pivot=(19, 10, 8))
    return m


# ---------------------------------------------------------------- 30 金魚鉢

def goldfish_bowl():
    from items_hires import ring
    import random
    rng = random.Random(30)
    m = F("goldfish_bowl", 6)
    # 台：漆塗りの小卓（脚・ふちを落とした天板）
    bevel(m, "stand_top", (1.2, 5.6, 1.2), (14.8, 7, 14.8), "urushi_k", b=0.35)
    bevel(m, "stand_apron", (2, 4.6, 2), (14, 5.6, 14), "urushi_k", b=0.15)
    for (x, z) in ((2, 2), (12.6, 2), (2, 12.6), (12.6, 12.6)):
        bevel(m, "stand_leg", (x, 0, z), (x + 1.4, 4.6, z + 1.4), "urushi_k", b=0.2)
        m.cube("stand_foot", (x - 0.2, 0, z - 0.2), (x + 1.6, 0.4, z + 1.6), "gold")
    # 金魚：胴（ふちを落とす）・背びれ・尾びれ（ふたまた）・目（両側）
    for k in range(2):
        g = m.group(f"fish{k}", (8, 9.5 + k * 1.6, 8))
        r = 2.4 if k == 0 else 1.6
        y = 9.5 + k * 1.6
        x = 8 + r
        bevel(m, "fish", (x - 0.7, y - 0.8, 6.6), (x + 0.7, y + 0.8, 9.0), "goldfish_b", b=0.3, group=g)
        m.cube("fish_head", (x - 0.55, y - 0.6, 6.2), (x + 0.55, y + 0.6, 6.7), "goldfish_b", group=g)
        m.cube("dorsal", (x - 0.06, y + 0.6, 7.6), (x + 0.06, y + 1.2, 8.6), "fish_fin", group=g)
        for sgn in (-1, 1):
            m.cube("tail", (x - 0.06, y - 0.2, 9.0), (x + 0.06, y + 1.0, 10.6), "goldfish_tail", rot=[sgn * 22, 0, 0],
                   pivot=[x, y, 9.0], group=g)
            m.cube("pectoral", (x + sgn * 0.55 - 0.05, y - 0.6, 7.6), (x + sgn * 0.55 + 0.05, y - 0.1, 8.4), "fish_fin", group=g)
            m.cube("eye", (x + sgn * 0.56 - 0.06, y + 0.1, 7.2), (x + sgn * 0.56 + 0.06, y + 0.45, 7.55), "deep_dark", group=g)
    # 水とガラス：中の金魚がはっきり見えるよう、うすく・重ねずに作る
    #  水：上から見て丸くなる帯。帯のあいだと、前後の面は描かず、上の水面と横の面だけ
    R = 4.6
    n = 10
    for k in range(n):
        x0 = -R + 2 * R * k / n
        x1 = -R + 2 * R * (k + 1) / n
        xc = max(abs(x0), abs(x1))
        half = math.sqrt(max(0.0, R * R - xc * xc))
        if half > 0.1:
            skip = ("north", "south", "down") + (("west",) if k > 0 else ()) + (("east",) if k < n - 1 else ())
            m.cube("glass_water", (8 + x0, 7.4, 8 - half), (8 + x1, 13.2, 8 + half), "water_thin", skip=skip)
    #  ガラス：外側の面だけの板をぐるっと並べる（内側の面・板どうしの重なりなし）
    seg = 20
    for (y0, y1, r) in ((7, 8, 3.8), (8, 10, 5.1), (10, 12.6, 5.4), (12.6, 14, 4.8), (14, 14.8, 3.8)):
        ln = 2 * r * math.tan(math.pi / seg)
        for k in range(seg):
            a = 2 * math.pi * k / seg
            x, z = 8 + r * math.cos(a), 8 + r * math.sin(a)
            m.cube("glass_bowl", (x - 0.03, y0, z - ln / 2), (x + 0.03, y1, z + ln / 2), "glass_clear",
                   rot=[0, -math.degrees(a), 0], pivot=[x, y0, z], skip=("west", "north", "south", "up", "down"))
    # ガラスの縁どり：上の口と、底のふちに少し濃い輪（形がわかるように）
    ring(m, "glass_lip", 8, 14.8, 8, 3.8, 0.25, 0.25, "glass_edge", seg=18, plane="xz")
    ring(m, "glass_base", 8, 7.1, 8, 3.8, 0.25, 0.2, "glass_edge", seg=18, plane="xz")
    ring(m, "glass_belly", 8, 11.3, 8, 5.42, 0.08, 0.15, "glass_edge", seg=20, plane="xz")
    # 口のひだ（波うつ縁）
    for k in range(12):
        a = 2 * math.pi * k / 12
        x, z = 8 + 4.0 * math.cos(a), 8 + 4.0 * math.sin(a)
        m.cube("glass_frill", (x - 1.1, 14.8, z - 0.03), (x + 1.1, 15.3 + (k % 2) * 0.4, z + 0.03), "glass_rim",
               rot=[0, -math.degrees(a) + 90, 0], pivot=[x, 14.8, z], skip=("north",))
    # 底：色の違う小石、水草（くねる）
    for k in range(14):
        a = rng.uniform(0, 2 * math.pi)
        rr = rng.uniform(0, 3.0)
        x, z = 8 + rr * math.cos(a), 8 + rr * math.sin(a)
        mt = ("pebble_w", "pebble_b", "rock")[k % 3]
        m.cube("pebble", (x - 0.45, 7.1, z - 0.4), (x + 0.45, 7.7, z + 0.4), mt, rot=[0, rng.uniform(0, 90), 0], pivot=[x, 7.1, z])
    for (bx, bz, h) in ((5.8, 9.4, 4.6), (6.6, 9.9, 3.6), (10.2, 6.4, 3.0)):
        bent(m, "weed", [(bx, 7.4, bz), (bx + 0.4, 7.4 + h * 0.35, bz), (bx - 0.2, 7.4 + h * 0.7, bz), (bx + 0.3, 7.4 + h, bz)],
             0.35, "weed")
    # えさ（上から落ちる）
    m.group("food", (8, 16, 8))
    for k in range(5):
        m.cube("food", (7 + (k % 3) * 0.8, 15.5 + (k // 3) * 0.7, 7.3 + (k % 2) * 1.0),
               (7.4 + (k % 3) * 0.8, 15.9 + (k // 3) * 0.7, 7.7 + (k % 2) * 1.0), "food", group="food")
    # えさの缶（ふち・ふた・ラベル）
    m.cyl("can", 15.6, 4.4, 7, 10.4, 1.5, "can_y", sides=6)
    m.cyl("can_lid", 15.6, 4.4, 10.4, 10.9, 1.6, "steel_light", sides=6)
    m.cube("can_label", (14.1, 8.0, 2.85), (17.1, 9.6, 2.95), "goldfish")
    return m


# ---------------------------------------------------------------- 31 風鈴

def wind_chime():
    from items_hires import ring
    m = F("wind_chime", 6)
    # 軒：柱・桁・垂木の出
    for x in (-0.4, 14.4):
        bevel(m, "post", (x, 0, 12.8), (x + 2, 26, 15.2), "hinoki_dark", b=0.3)
    bevel(m, "keta", (-1.4, 24, 12), (17.4, 26, 16), "hinoki_dark", b=0.3)
    for x in range(-1, 18, 3):
        m.cube("taruki", (x, 26, 10.4), (x + 0.9, 26.8, 16.4), "hinoki")
    m.cube("noji", (-1.8, 26.8, 10.0), (17.8, 27.3, 16.6), "hinoki_dark")
    m.cube("hook", (7.6, 21.5, 13.6), (8.4, 24, 14.4), "iron_black")
    ring(m, "hook_eye", 8, 21.6, 14, 0.5, 0.2, 0.3, "iron_black", seg=8)
    # 吊るした風鈴：ガラスのお椀（中が空）、絵付け、舌、短冊
    m.group("hung", (8, 21.5, 14))
    m.cube("string", (7.9, 17.4, 13.9), (8.1, 21.5, 14.1), "plate", group="hung")
    m.cyl("glass_top", 8, 14, 17.0, 17.6, 0.9, "glass_chime", sides=6, group="hung")
    for (y0, y1, r) in ((16.2, 17.0, 1.5), (15.0, 16.2, 1.9), (14.2, 15.0, 2.1)):
        ring(m, "glass_bell", 8, (y0 + y1) / 2, 14, r, 0.18, y1 - y0, "glass_chime", seg=14, plane="xz", group="hung")
    ring(m, "glass_lip", 8, 14.25, 14, 2.15, 0.3, 0.2, "glass_chime", seg=14, plane="xz", group="hung")
    for k in range(3):
        a = math.radians(200 + k * 50)
        x, z = 8 + 2.0 * math.cos(a), 14 + 2.0 * math.sin(a)
        m.cube("paint", (x - 0.35, 14.8, z - 0.05), (x + 0.35, 15.6, z + 0.05), ("chime_paint", "goldfish", "chime_paint")[k],
               rot=[0, -math.degrees(a) + 90, 0], pivot=[x, 15.2, z], group="hung")
    m.cube("zetsu", (7.6, 14.0, 13.6), (8.4, 14.6, 14.4), "glass_chime", group="hung")
    m.cube("ito", (7.95, 9.4, 13.95), (8.05, 17.0, 14.05), "plate", group="hung")
    bevel(m, "tanzaku", (7, 5.0, 13.92), (9, 9.4, 14.08), "tanzaku", b=0.05, group="hung", decal=_text("涼", size=0.6))
    # 箱の中の風鈴：桐箱（ふたは横に立てかけ）、緩衝の紙
    bevel(m, "shelf", (1, 0, 3), (15, 3.6, 9), "hinoki", b=0.25)
    for (a, b) in (((3.6, 3.6), (11.4, 4.0)), ((3.6, 8.0), (11.4, 8.4)), ((3.6, 4.0), (4.0, 8.0)), ((11.0, 4.0), (11.4, 8.0))):
        m.cube("box_wall", (a[0], 3.6, a[1]), (b[0], 6.6, b[1]), "kiri_box")
    m.cube("box_floor", (4.0, 3.6, 4.0), (11.0, 4.0, 8.0), "kiri_box")
    m.cube("paper", (4.0, 4.0, 4.0), (11.0, 4.4, 8.0), "paper")
    bevel(m, "box_lid", (12.0, 3.6, 3.6), (12.4, 8.4, 9.0), "kiri_box", b=0.08, rot=[0, 0, -10], pivot=[12.2, 3.6, 6])
    m.cube("himo", (12.45, 3.6, 6.1), (12.55, 8.0, 6.5), "red", rot=[0, 0, -10], pivot=[12.2, 3.6, 6])
    m.group("boxed", (7.5, 7, 6))
    for (y0, y1, r) in ((5.4, 6.0, 1.4), (6.0, 6.8, 1.6), (6.8, 7.4, 1.2)):
        m.cyl("glass_bell", 7.5, 6, y0, y1, r, "glass_chime", sides=6, group="boxed")
    m.cube("tanzaku", (8.6, 4.4, 4.5), (10.6, 4.6, 7.0), "tanzaku", group="boxed")
    return m


# ---------------------------------------------------------------- 32 臼と杵

def mochi_mortar():
    from items_hires import ring
    m = F("mochi_mortar", 6)
    # 臼：けやき。上下が広く、まん中がくびれる。上はくぼむ
    prof = [(0, 0.8, 6.2), (0.8, 2.4, 6.0), (2.4, 4.4, 5.5), (4.4, 6.4, 5.4), (6.4, 8.2, 5.7), (8.2, 9.4, 6.2)]
    for (ya, yb, r) in prof:
        m.cyl("usu", 8, 8, ya, yb, r, "keyaki", sides=8)
    for y in (1.6, 8.6):
        m.cyl("usu_band", 8, 8, y, y + 0.3, 6.35 if y > 5 else 6.15, "keyaki_d", sides=8)
    ring(m, "usu_rim", 8, 9.8, 8, 5.4, 1.6, 0.8, "keyaki", seg=16, plane="xz")
    m.cyl("usu_hollow", 8, 8, 9.2, 9.5, 4.6, "keyaki_d", sides=8)
    for (x, z) in ((2.4, 8), (13.6, 8)):
        m.cube("te", (x - 0.6, 4.6, z - 1.2), (x + 0.6, 5.4, z + 1.2), "keyaki_d")
    # もち：やわらかい山（輪切りを重ね、少しはみだす）
    m.group("mochi", (8, 10, 8))
    for (ya, yb, r) in ((9.5, 10.4, 4.0), (10.4, 11.2, 3.7), (11.2, 11.8, 3.1), (11.8, 12.3, 2.2)):
        m.cyl("mochi", 8, 8, ya, yb, r, "mochi_s", sides=8, group="mochi")
    m.cube("mochi_fold", (6.0, 11.6, 6.4), (9.6, 12.0, 8.0), "mochi_s", rot=[0, 30, 0], pivot=[8, 11.8, 8], group="mochi")
    # 杵：手前に立てかけ、つくときに振り下ろす（頭は縦の筒、柄は横）
    m.group("kine", (8, 12, 18), rot=(-35, 0, 0))
    cylz(m, "handle", 8, 12.6, 10, 22.4, 0.65, "hinoki", group="kine")
    m.cube("handle_grip", (7.25, 11.85, 20.4), (8.75, 13.35, 22.4), "rope", group="kine")
    for (ya, yb, r) in ((10.2, 10.8, 2.2), (10.8, 16.0, 2.5), (16.0, 16.6, 2.2)):
        m.cyl("head", 8, 10, ya, yb, r, "keyaki_d", sides=8, group="kine")
    for y in (11.2, 15.4):
        m.cyl("head_band", 8, 10, y, y + 0.35, 2.6, "iron_black", sides=8, group="kine")
    # 手水の桶（たが・水）とひしゃく
    ring(m, "oke", 19.5, 2.6, 4.5, 2.6, 0.4, 5.2, "oke", seg=14, plane="xz")
    m.cyl("oke_floor", 19.5, 4.5, 0, 0.5, 2.6, "oke", sides=8)
    for y in (1.0, 4.2):
        ring(m, "taga", 19.5, y, 4.5, 2.85, 0.2, 0.5, "hoop_copper", seg=14, plane="xz")
    m.cyl("glass_water", 19.5, 4.5, 0.5, 4.4, 2.35, "water", sides=8)
    m.cyl("hishaku_cup", 18.6, 3.8, 3.8, 5.0, 0.9, "hinoki", sides=6)
    bent(m, "hishaku_handle", [(18.6, 4.8, 4.6), (18.0, 6.8, 7.8), (17.6, 8.2, 10.4)], 0.35, "hinoki")
    # 取り粉の板ともち
    bevel(m, "board", (15, 0, 10), (23, 0.7, 16), "hinoki", b=0.15)
    m.cube("kona", (15.4, 0.7, 10.4), (22.6, 0.75, 15.6), "plaster_w")
    for (x, z) in ((16.4, 11.2), (19.0, 11.4), (21.0, 13.2), (17.4, 13.8)):
        m.cyl("maru", x + 0.8, z + 0.8, 0.75, 1.5, 0.95, "mochi_s", sides=6)
        m.cyl("maru_top", x + 0.8, z + 0.8, 1.5, 1.8, 0.6, "mochi_s", sides=4)
    return m

# ---------------------------------------------------------------- 33〜36 で使う材質
MATS.update({
    "sand_w": Mat((226, 220, 204), "stone", accent=(206, 198, 180), noise=6),
    "sand_ridge": Mat((240, 236, 222), "stone", noise=4),
    "border_stone": Mat((96, 96, 92), "stone", accent=(70, 70, 68), noise=10),
    "garden_rock": Mat((118, 114, 106), "stone", accent=(90, 86, 80), noise=14),
    "tarai": Mat((200, 164, 112), "wood", accent=(174, 136, 88), noise=5),
    "suds": Mat((248, 250, 252), "plaster", noise=2),
    "kiri_t": Mat((214, 186, 146), "wood", accent=(190, 160, 120), noise=4),
    "kiri_t_d": Mat((180, 148, 108), "wood", accent=(150, 118, 82), noise=4),
    "tansu_iron": Mat((40, 38, 40), "metal", noise=5),
    "chabu": Mat((150, 98, 56), "wood", accent=(120, 74, 38), noise=5),
    "kyusu": Mat((150, 70, 40), "stone", accent=(120, 52, 28), noise=8),
    "senbei": Mat((196, 140, 70), "stone", accent=(150, 96, 40), noise=10),
})


def _zen_ripples(m, cx, cz, rs, mat):
    """石のまわりの丸い砂紋"""
    from items_hires import ring
    for r in rs:
        ring(m, "ripple_round", cx, 1.3, cz, r, 0.35, 0.2, mat, seg=max(10, int(r * 5)), plane="xz")


# ---------------------------------------------------------------- 33 枯山水

def zen_garden():
    import random
    rng = random.Random(33)
    m = F("zen_garden", 6)
    # 縁石：ごつごつした石を並べる
    for x in range(-4, 20, 4):
        rough(m, "edge", (x, 0, -4.4), (x + 4, 2.2, -2.8), "border_stone", rng, lumps=2, depth=0.2)
        rough(m, "edge", (x, 0, 18.8), (x + 4, 2.2, 20.4), "border_stone", rng, lumps=2, depth=0.2)
    for z in range(-3, 19, 4):
        z1 = min(z + 4, 18.8)
        rough(m, "edge", (-4.4, 0, z), (-2.8, 2.2, z1), "border_stone", rng, lumps=2, depth=0.2)
        rough(m, "edge", (18.8, 0, z), (20.4, 2.2, z1), "border_stone", rng, lumps=2, depth=0.2)
    m.cube("sand", (-2.8, 0, -2.8), (18.8, 1.1, 18.8), "sand_w")
    # 石組み：大小3つ、根もとに苔
    rough(m, "rock", (1.4, 1.1, 2.6), (5.8, 5.6, 6.4), "garden_rock", rng, lumps=6, depth=0.4)
    rough(m, "rock", (4.8, 1.1, 4.2), (7.2, 3.2, 6.6), "garden_rock", rng, lumps=4, depth=0.3)
    rough(m, "rock", (11, 1.1, 10), (14.2, 3.4, 12.8), "garden_rock", rng, lumps=5, depth=0.35)
    rough(m, "rock", (12.6, 1.1, 1.8), (14.2, 2.2, 3.4), "garden_rock", rng, lumps=2, depth=0.2)
    for (x0, z0, x1, z1) in ((1.0, 6.2, 4.6, 7.2), (10.6, 12.6, 13.2, 13.4), (7.0, 4.0, 7.8, 6.2)):
        rough(m, "moss", (x0, 1.1, z0), (x1, 1.5, z1), "moss", rng, lumps=2, depth=0.1)
    # 石のまわりの丸い砂紋（いつも見えている）
    _zen_ripples(m, 4.0, 4.6, (4.0, 5.0), "sand_ridge")
    _zen_ripples(m, 12.6, 11.4, (2.8, 3.8), "sand_ridge")
    # まっすぐな砂紋（進むと1本ずつ現れる）：盛り上がった筋を3本ずつ
    for k in range(6):
        z = -1.5 + k * 3.4
        m.group(f"line{k}", (8, 1.2, z))
        for d in (-0.9, 0, 0.9):
            m.cube("ripple", (-2.6, 1.1, z + d - 0.18), (18.6, 1.32, z + d + 0.18), "sand_ridge", group=f"line{k}")
    # 熊手：竹の柄、横木、歯
    m.group("rake", (8, 1.2, 8))
    bevel(m, "rake_head", (5.4, 1.6, 7.4), (10.6, 2.4, 8.4), "bamboo", b=0.15, group="rake")
    for k in range(7):
        x = 5.6 + k * 0.8
        m.cube("tine", (x, 1.1, 7.2), (x + 0.3, 1.8, 7.6), "bamboo", rot=[18, 0, 0], pivot=[x, 1.8, 7.6], group="rake")
    cylz(m, "rake_handle", 8, 2.2, 8.3, 22, 0.4, "bamboo", group="rake")
    for z in (12, 17):
        cylz(m, "rake_node", 8, 2.2, z, z + 0.3, 0.5, "take_node", group="rake")
    return m


# ---------------------------------------------------------------- 34 たらい

def washtub():
    from items_hires import ring
    from furniture_hires import _ridges
    m = F("washtub", 6)
    # たらい：板を輪に並べた中空の桶、たが2本、底
    ring(m, "tarai", 8, 2.6, 8, 6.4, 0.6, 5.2, "tarai", seg=24, plane="xz")
    m.cyl("tarai_floor", 8, 8, 0, 0.6, 6.3, "tarai", sides=8)
    for y in (1.2, 4.0):
        ring(m, "taga", 8, y, 8, 6.75, 0.25, 0.7, "steel", seg=24, plane="xz")
    m.cyl("glass_water", 8, 8, 0.6, 4.4, 6.0, "water_thin", sides=8)
    # 洗濯板：板と、横に並んだ山（本物の凹凸）
    m.cube("washboard", (3, 1, 9), (13, 14, 9.6), "washboard", rot=(-25, 0, 0), pivot=(8, 1, 9.5))
    for k in range(10):
        y = 2.2 + k * 1.05
        m.cube("ridge", (3.6, y, 8.7), (12.4, y + 0.45, 9.05), "washboard", rot=(-25, 0, 0), pivot=(8, 1, 9.5))
    for x in (2.6, 12.6):
        m.cube("board_side", (x, 1, 8.6), (x + 0.8, 14.4, 9.8), "hinoki_dark", rot=(-25, 0, 0), pivot=(8, 1, 9.5))
    # 洗いものの布：しわ（帯を前後にずらす）
    m.group("cloth", (8, 7, 9))
    for k in range(6):
        x0 = 4 + k * 1.35
        dz = 0.25 * math.sin(k * 1.9)
        m.cube("cloth", (x0, 5.5 - (k % 2) * 0.4, 7.6 + dz), (x0 + 1.4, 9.5, 8.4 + dz), "cloth_blue", group="cloth", rot=(-25, 0, 0),
               pivot=(8, 7, 8))
    # 泡：まるい玉（輪切りを2段）を水面に
    m.group("bubbles", (8, 4.4, 8))
    for k, (x, z, r) in enumerate(((5, 5, 1.0), (10, 6, 0.8), (7, 11, 1.2), (11, 10, 0.7), (4.5, 9, 0.6), (9, 3.4, 0.5), (12, 8, 0.9))):
        m.cyl("soap", x, z, 4.2, 4.2 + r * 0.8, r, "suds", sides=6, group="bubbles")
        m.cyl("soap", x, z, 4.2 + r * 0.8, 4.2 + r * 1.4, r * 0.65, "suds", sides=4, group="bubbles")
    # 石けん（受け皿つき）と、小さな腰掛け
    bevel(m, "soap_dish", (13.2, 0, 12.6), (17.0, 0.5, 15.4), "hinoki", b=0.12)
    bevel(m, "bar", (13.7, 0.5, 13.1), (16.5, 1.6, 14.9), "plate", b=0.3)
    bevel(m, "stool_top", (-6, 4, 4), (-1, 5, 12), "hinoki", b=0.25)
    for z in (4.4, 10.6):
        bevel(m, "stool_leg", (-5.6, 0, z), (-1.4, 4, z + 1), "hinoki_dark", b=0.15)
    return m


# ---------------------------------------------------------------- 35 箪笥

def _warabite(m, x, y, z):
    """蕨手（わらびて）の引き手：座金と、弓なりの持ち手"""
    bevel(m, "zagane", (x - 1.2, y - 0.6, z - 0.25), (x + 1.2, y + 0.6, z), "tansu_iron", b=0.1)
    bent(m, "warabite", [(x - 1.1, y + 0.1, z - 0.3), (x - 0.7, y - 0.6, z - 0.7), (x + 0.7, y - 0.6, z - 0.7), (x + 1.1, y + 0.1, z - 0.3)],
         0.28, "tansu_iron")


def tansu():
    m = F("tansu", 6)
    # 台輪と本体（ふちを落とす）。上下2段の重ね箪笥
    bevel(m, "dai", (0.2, 0, 2.6), (15.8, 1.2, 15.2), "kiri_t_d", b=0.25)
    bevel(m, "body_lower", (0.5, 1.2, 3), (15.5, 14.6, 15), "kiri_t", b=0.3)
    bevel(m, "body_upper", (0.5, 14.8, 3), (15.5, 27.8, 15), "kiri_t", b=0.3)
    bevel(m, "tenban", (0.2, 27.8, 2.6), (15.8, 28.6, 15.2), "kiri_t_d", b=0.25)
    # 下段：引き出し3つ（前板が少し出る）
    for (y0, y1) in ((1.8, 5.8), (6.2, 10.2), (10.6, 14.2)):
        bevel(m, "drawer", (1.2, y0, 2.5), (14.8, y1, 3.05), "kiri_t", b=0.15)
        for x in (4.2, 11.8):
            _warabite(m, x, (y0 + y1) / 2 + 0.3, 2.5)
    # 上段：左は小引き出し2つ、右は観音開きの戸（錠前つき）
    for (y0, y1) in ((15.4, 21.2), (21.6, 27.2)):
        bevel(m, "drawer", (1.2, y0, 2.5), (7.6, y1, 3.05), "kiri_t", b=0.15)
        _warabite(m, 4.4, (y0 + y1) / 2 + 0.3, 2.5)
    for (x0, x1) in ((8.0, 11.3), (11.5, 14.8)):
        bevel(m, "door", (x0, 15.4, 2.5), (x1, 27.2, 3.05), "kiri_t", b=0.15)
    bevel(m, "lock_plate", (10.2, 19.4, 2.2), (12.6, 23.2, 2.5), "tansu_iron", b=0.15)
    m.cube("lock", (11.0, 20.4, 1.9), (11.8, 21.8, 2.2), "brass_s")
    # 角の金具（L字）
    for (x, y) in ((0.2, 0.9), (0.2, 27.2), (14.3, 0.9), (14.3, 27.2)):
        m.cube("kanagu", (x, y - 0.4, 2.3), (x + 1.5, y + 0.6, 2.6), "tansu_iron")
    for (x, y) in ((0.2, 0.9), (0.2, 25.7), (15.5, 0.9), (15.5, 25.7)):
        m.cube("kanagu", (x, y, 2.3), (x + 0.3, y + 1.5, 4.0), "tansu_iron")
    # 横の持ち手（棹通し）
    for x in (0.1, 15.5):
        m.cube("sao", (x, 13.6, 7.4), (x + 0.4, 15.6, 10.6), "tansu_iron")
    # 上に壺と文箱
    for (ya, yb, r) in ((28.6, 29.2, 1.0), (29.2, 31.4, 1.6), (31.4, 32.4, 1.0), (32.4, 33.0, 1.2)):
        m.cyl("vase", 11.5, 8.5, ya, yb, r, "pottery", sides=8)
    bevel(m, "fubako", (2, 28.6, 6), (7.4, 30.2, 11), "urushi_k", b=0.2)
    m.cube("fubako_himo", (4.5, 28.6, 5.9), (4.9, 30.3, 11.1), "red")
    return m


# ---------------------------------------------------------------- 36 ちゃぶ台

def chabudai():
    from items_hires import ring
    import random
    rng = random.Random(36)
    m = F("chabudai", 6)
    # 天板：丸く、ふちを丸める（2段）
    m.cyl("top", 8, 8, 6.2, 7.2, 8.6, "chabu", sides=12)
    m.cyl("top_edge", 8, 8, 5.9, 6.2, 8.3, "chabu", sides=12)
    m.cyl("top_face", 8, 8, 7.2, 7.25, 8.3, "chabu", sides=12)
    # 幕板と、折りたたみ脚（脚と脚をつなぐ貫）
    ring(m, "maku", 8, 5.3, 8, 6.4, 0.5, 1.2, "chabu", seg=20, plane="xz")
    for (x, z) in ((3.4, 3.4), (11.4, 3.4), (3.4, 11.4), (11.4, 11.4)):
        bevel(m, "leg", (x, 0, z), (x + 1.2, 5.9, z + 1.2), "hinoki_dark", b=0.2)
    for z in (3.7, 11.7):
        m.cube("nuki", (4.6, 1.8, z), (11.4, 2.4, z + 0.6), "hinoki_dark")
    # 急須（横手）、湯のみ（茶たく）、せんべいの皿
    for (ya, yb, r) in ((7.25, 7.6, 1.4), (7.6, 9.2, 1.9), (9.2, 9.8, 1.4)):
        m.cyl("kyusu", 6, 7, ya, yb, r, "kyusu", sides=8)
    m.cyl("kyusu_lid", 6, 7, 9.8, 10.1, 1.2, "kyusu", sides=6)
    m.cyl("kyusu_knob", 6, 7, 10.1, 10.5, 0.35, "kyusu", sides=4)
    bent(m, "spout", [(7.7, 8.2, 7), (8.6, 8.8, 7), (9.2, 9.4, 7)], 0.5, "kyusu")
    cylz(m, "yokode", 6, 8.4, 3.0, 5.2, 0.45, "kyusu")
    for (x, z) in ((10.6, 4.8), (11.2, 10.2)):
        m.cyl("chataku", x, z, 7.25, 7.5, 1.2, "urushi_r", sides=8)
        m.cyl("cup", x, z, 7.5, 9.2, 0.85, "yunomi", sides=8)
        m.cyl("cup_tea", x, z, 9.15, 9.22, 0.7, "matcha_r", sides=6)
    m.cyl("plate", 6.4, 12, 7.25, 7.6, 2.2, "plate", sides=10)
    for k in range(4):
        a = k * 1.6
        x, z = 6.4 + 1.0 * math.cos(a), 12 + 1.0 * math.sin(a)
        m.cyl("senbei", x, z, 7.6 + k * 0.18, 7.85 + k * 0.18, 0.9, "senbei", sides=8)
    # 座布団3枚（ふくらみ・房）
    for (x0, z0, x1, z1) in ((-7, 3, -1.4, 13), (17.4, 3, 23, 13), (3, -7, 13, -1.4)):
        bevel(m, "zabuton", (x0, 0, z0), (x1, 1.4, z1), "zabuton", b=0.45)
        bevel(m, "zabuton_top", (x0 + 0.5, 1.4, z0 + 0.5), (x1 - 0.5, 1.7, z1 - 0.5), "zabuton", b=0.3)
        for (x, z) in ((x0 + 0.2, z0 + 0.2), (x1 - 0.6, z0 + 0.2), (x0 + 0.2, z1 - 0.6), (x1 - 0.6, z1 - 0.6)):
            m.cube("fusa", (x, 0.8, z), (x + 0.4, 1.4, z + 0.4), "yellow_cloth")
    return m

# ---------------------------------------------------------------- 37〜40 で使う材質
MATS.update({
    "byobu_gold": Mat((226, 196, 120), "paper", accent=(240, 214, 140), noise=4),
    "byobu_frame": Mat((30, 22, 20), "metal", noise=4),
    "byobu_fit": Mat((196, 160, 80), "metal", noise=5),
    "toro": Mat((150, 148, 138), "stone", accent=(118, 116, 108), noise=12),
    "toro_d": Mat((120, 118, 110), "stone", accent=(96, 94, 88), noise=12),
    "glow_paper": Mat((255, 214, 140), "paper", accent=(255, 236, 190), glow=True),
    "komo": Mat((206, 184, 128), "rope", accent=(176, 152, 96)),
    "komo_print": Mat((60, 50, 120), "plain", noise=3),
    "taru_wood": Mat((214, 184, 136), "wood", accent=(188, 156, 108), noise=4),
    "masu": Mat((232, 208, 160), "wood", accent=(210, 182, 130), noise=3),
    "shigaraki": Mat((156, 104, 66), "stone", accent=(110, 70, 44), noise=14),
    "glaze_g": Mat((90, 120, 80), "metal", noise=8),
    "janome_r": Mat((176, 36, 32), "paper", accent=(150, 26, 24), noise=5),
    "janome_b": Mat((44, 62, 116), "paper", accent=(30, 44, 90), noise=5),
    "janome_y": Mat((214, 170, 70), "paper", accent=(184, 140, 50), noise=5),
    "kasa_black": Mat((36, 38, 44), "stipple", noise=4),
    "handle_wood": Mat((120, 70, 36), "metal", noise=5),
})


# ---------------------------------------------------------------- 37 屏風

def byobu():
    from furniture_hires import _byobu_art
    m = F("byobu", 6)
    # 6枚の扇をジグザグにつなぐ：黒漆の縁、金地の絵、蝶番の金具
    n, L, th = 6, 5.2, 28
    pts = []
    x, z = -8.0, 9.0
    for k in range(n + 1):
        pts.append((x, z))
        dz = L * math.sin(math.radians(th)) * (1 if k % 2 == 0 else -1)
        x += L * math.cos(math.radians(th))
        z += dz
    for k in range(n):
        (xa, za), (xb, zb) = pts[k], pts[k + 1]
        cx, cz = (xa + xb) / 2, (za + zb) / 2
        ang = math.degrees(math.atan2(-(zb - za), xb - xa))
        r = dict(rot=[0, ang, 0], pivot=[cx, 0, cz])
        h = L / 2
        m.cube("panel", (cx - h + 0.35, 1.6, cz - 0.15), (cx + h - 0.35, 23.4, cz + 0.15), "byobu_gold", decal=_byobu_art(k % 4), **r)
        for (y0, y1) in ((0.4, 1.6), (23.4, 24.4)):
            m.cube("fuchi", (cx - h, y0, cz - 0.35), (cx + h, y1, cz + 0.35), "byobu_frame", **r)
        for xx in (cx - h, cx + h - 0.35):
            m.cube("fuchi", (xx, 0.4, cz - 0.35), (xx + 0.35, 24.4, cz + 0.35), "byobu_frame", **r)
        for (y0, y1) in ((0.4, 1.0), (23.8, 24.4)):
            m.cube("kado", (cx - h - 0.05, y0, cz - 0.4), (cx - h + 0.6, y1, cz + 0.4), "byobu_fit", **r)
            m.cube("kado", (cx + h - 0.6, y0, cz - 0.4), (cx + h + 0.05, y1, cz + 0.4), "byobu_fit", **r)
        m.cube("ashi", (cx - h + 0.4, 0, cz - 0.3), (cx + h - 0.4, 0.4, cz + 0.3), "byobu_frame", **r)
    for (x, z) in pts[1:-1]:
        for y in (5, 19):
            m.cube("chotsugai", (x - 0.25, y, z - 0.25), (x + 0.25, y + 1.6, z + 0.25), "byobu_fit")
    return m


# ---------------------------------------------------------------- 38 石灯籠（春日形）

def tourou():
    import random
    rng = random.Random(38)
    m = F("tourou", 6)
    # 基礎（ごつごつ）・基壇（六角）
    rough(m, "kiso", (1.4, 0, 1.4), (14.6, 1.6, 14.6), "toro_d", rng, lumps=6, depth=0.3)
    m.cyl("kidai", 8, 8, 1.6, 2.6, 5.4, "toro", sides=3)
    m.cyl("kidai2", 8, 8, 2.6, 3.4, 4.2, "toro", sides=3)
    m.cyl("kidai3", 8, 8, 3.4, 3.8, 2.8, "toro", sides=3)
    # 竿：まん中に節
    m.cyl("sao", 8, 8, 3.8, 12, 1.9, "toro", sides=4)
    m.cyl("sao_fushi", 8, 8, 7.6, 8.4, 2.2, "toro", sides=4)
    # 中台（六角、上が広がる）
    m.cyl("chudai", 8, 8, 12, 12.8, 3.0, "toro", sides=3)
    m.cyl("chudai2", 8, 8, 12.8, 14.0, 4.8, "toro", sides=3)
    # 火袋：六本の柱のあいだが窓（紙が光る）。鹿と月の彫り
    for a in range(6):
        ang = math.radians(a * 60 + 30)
        x, z = 8 + 3.4 * math.cos(ang), 8 + 3.4 * math.sin(ang)
        m.cube("hashira", (x - 0.55, 14, z - 0.55), (x + 0.55, 20, z + 0.55), "toro")
    m.cyl("hibukuro_floor", 8, 8, 14, 14.4, 3.4, "toro", sides=3)
    m.cyl("hibukuro_top", 8, 8, 19.4, 20, 3.8, "toro", sides=3)
    m.cyl("glow_paper", 8, 8, 14.4, 19.4, 2.8, "glow_paper", sides=3)
    m.group("flame", (8, 15.5, 8))
    m.cube("glow_flame", (7.4, 15.2, 7.4), (8.6, 17.4, 8.6), "glow_fire", group="flame")
    # 笠：六角の屋根。軒先がそり上がる（蕨手）
    m.cyl("kasa", 8, 8, 20, 21.0, 6.6, "toro", sides=3)
    m.cyl("kasa2", 8, 8, 21.0, 22.0, 5.0, "toro", sides=3)
    m.cyl("kasa3", 8, 8, 22.0, 22.8, 3.2, "toro", sides=3)
    for a in range(6):
        ang = math.radians(a * 60)
        x, z = 8 + 6.4 * math.cos(ang), 8 + 6.4 * math.sin(ang)
        m.cube("warabite", (x - 0.5, 20.8, z - 0.5), (x + 0.5, 22.2, z + 0.5), "toro")
    # 請花と宝珠
    m.cyl("ukebana", 8, 8, 22.8, 23.6, 1.8, "toro", sides=4)
    for (ya, yb, r) in ((23.6, 24.4, 1.3), (24.4, 25.4, 1.5), (25.4, 26.2, 1.0), (26.2, 26.8, 0.4)):
        m.cyl("hoju", 8, 8, ya, yb, r, "toro", sides=4)
    # 苔
    for (x0, y0, z0) in ((3, 1.6, 9), (10.6, 1.6, 3), (5.4, 20.9, 4.6), (9, 21.9, 10)):
        m.cube("moss", (x0, y0 - 0.05, z0), (x0 + rng.uniform(1.2, 2.4), y0 + 0.25, z0 + rng.uniform(1, 2)), "moss")
    return m


# ---------------------------------------------------------------- 39 酒樽（菰樽）

def sake_barrel():
    from items_hires import ring
    m = F("sake_barrel", 6)
    # 台
    bevel(m, "dai", (0.4, 0, 0.4), (15.6, 1.4, 15.6), "hinoki_dark", b=0.3)
    # 樽：菰（こも）を巻き、上下と胴に縄
    m.cyl("taru", 8, 8, 1.4, 16.4, 6.6, "taru_wood", sides=6)
    m.cyl("komo", 8, 8, 2.6, 15.0, 6.95, "komo", sides=6)
    for y in (3.0, 9.0, 14.4):
        m.cyl("nawa", 8, 8, y, y + 0.7, 7.25, "rope", sides=6)
    # 銘の印刷（菰に刷った紺の文字と枠）
    bevel(m, "label", (4.6, 5.0, 0.7), (11.4, 12.4, 1.0), "komo_print", b=0.1)
    m.cube("label_in", (5.2, 5.6, 0.62), (10.8, 11.8, 0.72), "plate", decal=_text("祝", color=(40, 40, 110, 255), size=0.6))
    # ふた（鏡）：板目、たが、升とひしゃく
    m.cyl("kagami", 8, 8, 16.4, 16.9, 6.3, "taru_wood", sides=6)
    ring(m, "taga", 8, 16.7, 8, 6.6, 0.35, 0.6, "take", seg=20, plane="xz")
    for x in (4.0, 8.0, 12.0):
        m.cube("kagami_line", (x - 0.05, 16.9, 2.4), (x + 0.05, 16.95, 13.6), "hinoki_dark")
    for (a, b) in (((5.2, 9.0), (8.4, 9.4)), ((5.2, 11.8), (8.4, 12.2)), ((5.2, 9.4), (5.6, 11.8)), ((8.0, 9.4), (8.4, 11.8))):
        m.cube("masu", (a[0], 16.9, a[1]), (b[0], 19.2, b[1]), "masu")
    m.cube("masu_floor", (5.6, 16.9, 9.4), (8.0, 17.2, 11.8), "masu")
    m.cyl("hishaku_cup", 11.4, 6.4, 16.9, 18.2, 1.0, "hinoki", sides=6)
    bent(m, "hishaku_handle", [(11.4, 18.0, 7.4), (11.6, 17.8, 11.0), (11.8, 17.4, 14.6)], 0.4, "hinoki")
    return m


# ---------------------------------------------------------------- 40 傘立て

def kasa_tate():
    m = F("kasa_tate", 6)
    # 信楽焼の壺：ふくらんで口が広がる、釉がたれる
    prof = [(0, 1.0, 3.6), (1.0, 4.0, 4.2), (4.0, 9.0, 4.5), (9.0, 14.0, 4.4), (14.0, 16.6, 4.1), (16.6, 18.0, 4.5)]
    for (ya, yb, r) in prof:
        m.cyl("pot", 8, 8, ya, yb, r, "shigaraki", sides=6)
    m.cyl("pot_glaze", 8, 8, 14.6, 18.0, 4.6, "glaze_g", sides=6)
    for k in range(6):
        a = math.radians(k * 60 + 15)
        x, z = 8 + 4.62 * math.cos(a), 8 + 4.62 * math.sin(a)
        m.cube("drip", (x - 0.3, 11.4 + (k % 3) * 0.9, z - 0.3), (x + 0.3, 14.8, z + 0.3), "glaze_g")
    m.cyl("pot_inside", 8, 8, 17.95, 18.05, 3.8, "deep_dark", sides=6)
    # すぼめた傘を3本：先（石突き）を壺に入れ、柄が上。布はひだになって巻かれ、留めバンドでとめる
    umbs = ((-1.6, -1.0, (6, 0, -9), "kasa_black", "crook"), (1.6, -0.4, (-4, 0, 10), "janome_b", "crook"),
            (0.0, 1.8, (-9, 0, 2), "janome_r", "wagasa"))
    for k, (dx, dz, rot, mat, kind) in enumerate(umbs):
        cx, cz = 8 + dx, 8 + dz

        def Y(v):
            return 1.0 + (v - 1.0) * 1.95
        g = m.group(f"umb{k}", (cx, 1.0, cz), rot=rot)
        m.cyl("ishizuki", cx, cz, 1.0, Y(2.8), 0.25, "steel_light" if kind == "crook" else "hinoki_dark", sides=2, group=g)
        # 布：ひだ（放射状の薄い板）＋芯。下（先）が細く、上にいくほどだんだん広がる
        prof = ((2.8, 4.0, 0.3), (4.0, 6.0, 0.4), (6.0, 8.0, 0.55), (8.0, 10.0, 0.8), (10.0, 12.0, 1.1), (12.0, 14.0, 1.45),
                (14.0, 16.0, 1.8), (16.0, 17.6, 2.15))
        for (ya, yb, r) in prof:
            m.cyl("canopy", cx, cz, Y(ya), Y(yb), r * 0.62, mat, sides=3, group=g)
            for p_ in range(8):
                ang = p_ * 45 + (12 if kind == "wagasa" else 0)
                m.cube("pleat", (cx, Y(ya), cz - 0.17), (cx + r, Y(yb), cz + 0.17), mat, rot=[0, ang, 0], pivot=[cx, Y(ya), cz], group=g)
        m.cyl("canopy_top", cx, cz, Y(17.6), Y(17.6) + 0.6, 1.2, mat, sides=3, group=g)
        if kind == "wagasa":
            # 和傘：蛇の目の白い帯、竹の骨が上に出る、頭の飾り
            m.cyl("janome", cx, cz, Y(10.0), Y(10.7), 1.25, "plate", sides=3, group=g)
            for p_ in range(8):
                m.cube("hone", (cx + 0.5, Y(17.6), cz - 0.08), (cx + 0.62, Y(19.2), cz + 0.08), "bamboo", rot=[0, p_ * 45, 0],
                       pivot=[cx, Y(17.6), cz], group=g)
            m.cyl("rokuro", cx, cz, Y(18.8), Y(18.8) + 1.0, 0.7, "hinoki", sides=3, group=g)
            m.cyl("e", cx, cz, Y(18.8) + 1.0, Y(18.8) + 6.8, 0.32, "bamboo", sides=2, group=g)
            for y in (Y(18.8) + 4.4, Y(18.8) + 5.2, Y(18.8) + 6.0):
                m.cyl("tou", cx, cz, y, y + 0.5, 0.42, "rope", sides=2, group=g)
            m.cyl("e_end", cx, cz, Y(18.8) + 6.8, Y(18.8) + 7.4, 0.45, "hinoki_dark", sides=2, group=g)
            m.cube("himo", (cx - 0.15, Y(18.8) + 2.2, cz - 0.15), (cx + 0.15, Y(18.8) + 6.8, cz + 0.15), "red_cloth", rot=[0, 0, 12],
                   pivot=[cx, Y(18.8) + 6.8, cz], group=g)
        else:
            # 洋傘：留めバンドとボタン、金属の軸、曲がった持ち手
            m.cyl("band", cx, cz, Y(11.6), Y(11.6) + 0.8, 1.32, mat, sides=3, group=g)
            m.cube("button", (cx - 0.3, Y(11.6) + 0.1, cz - 1.57), (cx + 0.3, Y(11.6) + 0.7, cz - 1.27), "steel_light", group=g)
            m.cyl("runner", cx, cz, Y(17.6), Y(17.6) + 0.8, 0.45, "steel_light", sides=2, group=g)
            m.cyl("shaft", cx, cz, Y(17.6) + 0.8, Y(17.6) + 5.6, 0.22, "steel_light", sides=2, group=g)
            hx = -1 if k == 0 else 1
            t = Y(17.6) + 5.6
            bent(m, "handle", [(cx, t, cz), (cx, t + 1.8, cz), (cx + 0.5 * hx, t + 2.9, cz), (cx + 1.5 * hx, t + 3.2, cz),
                               (cx + 2.3 * hx, t + 2.6, cz), (cx + 2.5 * hx, t + 1.4, cz)], 0.7, "handle_wood", group=g)
    return m

# ---------------------------------------------------------------- 41〜44 で使う材質
MATS.update({
    "tanuki_b": Mat((122, 86, 54), "stone", accent=(96, 64, 38), noise=12),
    "tanuki_l": Mat((214, 186, 142), "stone", accent=(190, 160, 116), noise=8),
    "tanuki_d": Mat((60, 40, 26), "stone", noise=8),
    "tanuki_hat": Mat((150, 104, 60), "stone", accent=(120, 80, 44), noise=10),
    "tokkuri": Mat((236, 230, 214), "plaster", noise=4),
    "neko": Mat((248, 246, 240), "plaster", noise=3),
    "neko_orange": Mat((226, 140, 60), "plaster", noise=5),
    "neko_black": Mat((40, 36, 34), "plaster", noise=4),
    "koban": Mat((226, 186, 70), "metal", accent=(250, 220, 120), noise=6),
    "neko_gold": Mat((214, 164, 50), "metal", accent=(246, 214, 120), noise=6),
    "koban_b": Mat((246, 214, 110), "metal", accent=(255, 240, 170), noise=4),
    "hibachi_c": Mat((228, 222, 206), "plaster", noise=4),
    "hibachi_blue": Mat((50, 76, 140), "plain", noise=3),
    "tsubo_body": Mat((96, 62, 40), "stone", accent=(70, 44, 28), noise=12),
    "tsubo_glaze": Mat((60, 96, 82), "metal", accent=(90, 130, 110), noise=8),
})


def blob(m, name, cx, cz, y0, y1, r, mat, n=8, sides=8, lo=0.55, hi=0.45, group=None, bulge=None):
    """まるいかたまり（楕円体を輪切りで重ねる）。lo/hi = 下端・上端の半径の割合（0 なら点までしぼむ）"""
    for k in range(n):
        ya = y0 + (y1 - y0) * k / n
        yb = y0 + (y1 - y0) * (k + 1) / n
        t = ((ya + yb) / 2 - y0) / (y1 - y0) * 2 - 1          # -1（下）〜 1（上）
        f = math.sqrt(max(0.0, 1 - t * t))
        edge = lo if t < 0 else hi
        rr = r * (edge + (1 - edge) * f)
        if bulge:
            rr *= bulge(t)
        m.cyl(name, cx, cz, ya, yb, rr, mat, sides=sides, group=group)


def _sometsuke(draw, face, x0, y0, w, h):
    """火鉢の染付（青い唐草の帯）"""
    if face not in ("north", "south", "east", "west"):
        return
    c = (50, 76, 140, 255)
    draw.line([(x0, y0 + 2), (x0 + w, y0 + 2)], fill=c, width=1)
    draw.line([(x0, y0 + h - 3), (x0 + w, y0 + h - 3)], fill=c, width=1)
    for k in range(0, max(1, w), 6):
        draw.arc([x0 + k, y0 + h * 0.3, x0 + k + 6, y0 + h * 0.7], 0, 270, fill=c, width=1)


# ---------------------------------------------------------------- 41 信楽たぬき

def tanuki():
    m = F("tanuki", 6)
    # 台と足
    bevel(m, "base", (2, 0, 2.4), (14, 0.8, 14.4), "tanuki_d", b=0.25)
    for x in (5.4, 10.6):
        blob(m, "foot", x, 5.4, 0.8, 2.2, 1.6, "tanuki_b", n=2, lo=0.9, hi=0.6, sides=4)
    # 胴：大きなおなか（前へ出っぱる）
    blob(m, "body", 8, 8.6, 1.4, 13.4, 5.6, "tanuki_b", n=10, lo=0.7, hi=0.55)
    blob(m, "belly", 8, 6.2, 2.6, 11.2, 4.6, "tanuki_l", n=9, lo=0.45, hi=0.35)
    m.cube("heso", (7.5, 6.6, 1.45), (8.5, 7.6, 1.55), "tanuki_d")
    # 頭：まるい顔、白い顔の模様、つき出た鼻、大きな目、耳
    blob(m, "head", 8, 8.4, 12.6, 19.8, 4.1, "tanuki_b", n=8, lo=0.6, hi=0.5)
    blob(m, "face", 8, 6.6, 13.6, 17.8, 2.8, "tanuki_l", n=6, lo=0.5, hi=0.5)
    blob(m, "snout", 8, 4.4, 14.0, 16.0, 1.3, "tanuki_l", n=4, lo=0.6, hi=0.6, sides=4)
    blob(m, "nose", 8, 3.2, 15.0, 16.0, 0.6, "tanuki_d", n=2, lo=0.7, hi=0.7, sides=4)
    for x in (6.3, 9.7):
        blob(m, "eye_ring", x, 4.3, 15.9, 17.5, 0.85, "tanuki_d", n=3, lo=0.6, hi=0.6, sides=4)
        m.cube("eye", (x - 0.3, 16.4, 3.4), (x + 0.3, 17.0, 3.5), "plate")
    for x in (4.8, 10.0):
        blob(m, "ear", x + 0.6, 8.2, 18.4, 20.2, 0.8, "tanuki_b", n=3, lo=0.8, hi=0.3, sides=4)
    # 笠：つばが広く、てっぺんがとがる
    m.cyl("kasa", 8, 8.4, 19.4, 19.9, 5.6, "tanuki_hat", sides=6)
    for (ya, yb, r) in ((19.9, 20.6, 4.0), (20.6, 21.4, 2.8), (21.4, 22.0, 1.6), (22.0, 22.4, 0.6)):
        m.cyl("kasa_top", 8, 8.4, ya, yb, r, "tanuki_hat", sides=6)
    m.cyl("kasa_himo", 8, 8.4, 19.9, 20.2, 4.1, "red", sides=6)
    # 右手（向かって左）に通い帳、左手（向かって右）に徳利
    blob(m, "arm", 3.0, 7.4, 7.8, 12.8, 1.4, "tanuki_b", n=5, lo=0.7, hi=0.6, sides=4)
    bevel(m, "book", (1.0, 6.4, 5.0), (2.8, 10.6, 8.6), "tokkuri", b=0.15, decal=_text("通", size=0.5, faces=("west",)))
    m.cube("book_tie", (0.9, 8.0, 5.0), (2.9, 8.4, 8.6), "tanuki_d")
    blob(m, "arm", 13.0, 7.4, 7.8, 12.8, 1.4, "tanuki_b", n=5, lo=0.7, hi=0.6, sides=4)
    for (ya, yb, r) in ((3.6, 5.0, 1.0), (5.0, 7.6, 1.5), (7.6, 9.0, 1.1), (9.0, 10.0, 0.5), (10.0, 10.4, 0.7)):
        m.cyl("tokkuri", 14.8, 6.4, ya, yb, r, "tokkuri", sides=6)
    m.cube("tokkuri_text", (14.2, 5.6, 4.85), (15.4, 7.2, 4.95), "tanuki_d")
    m.cube("tokkuri_rope", (14.0, 9.6, 5.8), (15.6, 10.0, 7.0), "rope")
    # しっぽ
    blob(m, "tail", 8, 14.6, 0.8, 3.6, 1.6, "tanuki_b", n=3, lo=0.7, hi=0.5, sides=4)
    return m


# ---------------------------------------------------------------- 42 招き猫

def maneki_neko():
    m = F("maneki_neko", 6)
    # 座布団（ふくらみ・房）
    bevel(m, "cushion", (1.4, 0, 1.4), (14.6, 1.6, 14.6), "red_cloth", b=0.5)
    bevel(m, "cushion_top", (2.0, 1.6, 2.0), (14.0, 2.0, 14.0), "red_cloth", b=0.3)
    for (x, z) in ((1.6, 1.6), (14.0, 1.6), (1.6, 14.0), (14.0, 14.0)):
        m.cube("fusa", (x, 0.8, z), (x + 0.4, 1.5, z + 0.4), "koban_b")
    # 胴：すわった形（下がふくらむ）、三毛のぶち
    blob(m, "body", 8, 8.6, 2.0, 11.6, 4.7, "neko_gold", n=9, lo=0.8, hi=0.55)
    # 赤い前掛け（首輪から胸に下がる）
    for (y0, y1, w, z0) in ((9.9, 10.7, 3.0, 3.9), (9.1, 9.9, 2.3, 3.7)):
        m.cube("maekake", (8 - w, y0, z0), (8 + w, y1, z0 + 0.9), "red_cloth")
    for x in (5.0, 9.4):
        blob(m, "hind", x + 0.8, 5.4, 2.0, 3.6, 1.2, "neko_gold", n=2, lo=0.9, hi=0.6, sides=4)
    # 頭：大きくまるい。三角の耳、目・鼻・口・ひげ
    blob(m, "head", 8, 8.2, 10.6, 18.6, 4.5, "neko_gold", n=8, lo=0.6, hi=0.5)
    for x, sgn in ((5.4, -1), (10.6, 1)):
        m.cube("ear", (x - 1.0, 17.0, 7.4), (x + 1.0, 19.4, 8.8), "neko_gold", rot=[0, 0, sgn * -14], pivot=[x, 17.0, 8])
        m.cube("ear_in", (x - 0.6, 17.4, 7.3), (x + 0.6, 18.9, 7.45), "red", rot=[0, 0, sgn * -14], pivot=[x, 17.0, 8])
    for x in (5.9, 9.1):
        m.cube("eye", (x, 14.6, 4.0), (x + 1.0, 15.1, 4.2), "neko_black")
        m.cube("eye_hi", (x + 0.2, 14.9, 3.95), (x + 0.4, 15.1, 4.0), "plate")
        m.cube("brow", (x - 0.1, 15.5, 4.0), (x + 1.1, 15.7, 4.15), "neko_black")
    blob(m, "muzzle", 7.2, 4.6, 12.2, 14.2, 1.1, "neko_gold", n=3, lo=0.7, hi=0.7, sides=4)
    blob(m, "muzzle", 8.8, 4.6, 12.2, 14.2, 1.1, "neko_gold", n=3, lo=0.7, hi=0.7, sides=4)
    m.cube("nose", (7.6, 13.6, 3.5), (8.4, 14.1, 3.7), "red")
    m.cube("mouth", (7.9, 12.8, 3.55), (8.1, 13.6, 3.62), "neko_black")
    for sgn in (-1, 1):
        for k in range(2):
            m.cube("hige", (8 + sgn * 2.0 - 1.4, 13.0 + k * 0.6, 3.9), (8 + sgn * 2.0 + 1.4, 13.1 + k * 0.6, 4.0), "neko_black",
                   rot=[0, 0, sgn * (8 - k * 12)], pivot=[8 + sgn * 2.0, 13.0, 4])
    # 首輪と鈴（まるい）
    m.cyl("collar", 8, 8.4, 10.6, 11.4, 3.7, "red_cloth", sides=6)
    for (ya, yb, r) in ((9.0, 9.4, 0.6), (9.4, 10.4, 0.9), (10.4, 10.8, 0.6)):
        m.cyl("bell", 8, 4.6, ya, yb, r, "koban", sides=4)
    m.cube("bell_slit", (7.4, 9.5, 3.7), (8.6, 9.65, 3.8), "neko_black")
    # 小判（だ円）と抱える左手
    for (x0, x1, y0, y1) in ((5.4, 10.6, 4.4, 8.6), (5.0, 11.0, 5.0, 8.0), (5.8, 10.2, 4.0, 9.0)):
        m.cube("koban", (x0, y0, 3.6), (x1, y1, 4.2), "koban_b")
    m.cube("koban_face", (5.9, 4.7, 3.55), (10.1, 8.3, 3.6), "koban_b", decal=_text("千万両", size=0.45))
    blob(m, "paw_l", 4.4, 5.4, 5.4, 9.0, 1.4, "neko_gold", n=4, lo=0.7, hi=0.6, sides=4)
    # 招く手（ゆっくり手まねき）：腕・まるい手・肉球
    m.group("paw", (11.5, 11, 7))
    blob(m, "arm", 11.6, 7.2, 10.4, 16.6, 1.3, "neko_gold", n=5, lo=0.8, hi=0.8, sides=4, group="paw")
    blob(m, "hand", 11.6, 7.0, 15.8, 18.2, 1.6, "neko_gold", n=4, lo=0.6, hi=0.55, sides=4, group="paw")
    m.cube("pad", (10.9, 16.4, 5.5), (12.3, 17.2, 5.65), "red", group="paw")
    for x in (10.7, 11.45, 12.2):
        m.cube("toe", (x, 17.35, 5.5), (x + 0.45, 17.75, 5.65), "red", group="paw")
    return m


# ---------------------------------------------------------------- 43 火鉢

def hibachi():
    from items_hires import ring
    import random
    rng = random.Random(43)
    m = F("hibachi", 6)
    # 木の台
    bevel(m, "dai", (0.6, 0, 0.6), (15.4, 1.0, 15.4), "hinoki_dark", b=0.25)
    # 陶器の火鉢：すこしふくらみ、口が内に入る。染付の帯
    for (ya, yb, r) in ((1.0, 1.8, 5.6), (1.8, 4.0, 6.2), (4.0, 7.6, 6.5), (7.6, 9.4, 6.3)):
        m.cyl("pot", 8, 8, ya, yb, r, "hibachi_c", sides=8)
    m.cyl("pot_band", 8, 8, 4.4, 7.2, 6.55, "hibachi_c", sides=8)
    for k in range(16):
        a = 2 * math.pi * k / 16
        x, z = 8 + 6.58 * math.cos(a), 8 + 6.58 * math.sin(a)
        m.cube("karakusa", (x - 0.06, 5.0 + (k % 2) * 0.8, z - 0.5), (x + 0.06, 6.0 + (k % 2) * 0.8, z + 0.5), "hibachi_blue",
               rot=[0, -math.degrees(a), 0], pivot=[x, 5.0, z])
    for y in (4.3, 7.2):
        ring(m, "blue_line", 8, y, 8, 6.58, 0.06, 0.2, "hibachi_blue", seg=24, plane="xz")
    ring(m, "rim", 8, 9.7, 8, 5.6, 1.2, 0.6, "hibachi_c", seg=24, plane="xz")
    # 灰：まん中が少し盛り上がる。炭（ごつごつ）と火
    m.cyl("ash", 8, 8, 8.6, 9.6, 5.2, "ash_r", sides=8)
    m.cyl("ash_mound", 8, 8, 9.6, 10.0, 3.2, "ash_r", sides=6)
    m.group("ember", (8, 9.6, 8))
    for (x, z) in ((6, 6.4), (9.2, 7), (7.2, 9.6), (9.8, 9.8)):
        rough(m, "charcoal", (x - 1.0, 9.8, z - 0.8), (x + 1.0, 10.9, z + 0.8), "charcoal", rng, lumps=2, depth=0.15)
        m.cube("glow_ember", (x - 0.8, 10.85, z - 0.6), (x + 0.8, 11.0, z + 0.6), "ember", group="ember")
        m.cube("glow_ember", (x - 1.05, 10.0, z - 0.3), (x - 0.98, 10.6, z + 0.3), "ember", group="ember")
    # 五徳と鉄瓶
    ring(m, "gotoku", 8, 11.6, 8, 2.6, 0.4, 0.35, "iron_r", seg=12, plane="xz")
    for a in range(3):
        ang = math.radians(a * 120 + 90)
        x, z = 8 + 2.6 * math.cos(ang), 8 + 2.6 * math.sin(ang)
        m.cube("gotoku_leg", (x - 0.2, 9.8, z - 0.2), (x + 0.2, 11.8, z + 0.2), "iron_r")
    for (ya, yb, r) in ((11.8, 12.4, 2.0), (12.4, 14.0, 2.7), (14.0, 14.8, 2.2), (14.8, 15.2, 1.4)):
        m.cyl("tetsubin", 8, 8, ya, yb, r, "tetsubin", sides=8)
    m.cyl("lid_knob", 8, 8, 15.2, 15.6, 0.4, "brass_s", sides=4)
    bent(m, "spout", [(10.4, 13.0, 8), (11.6, 13.8, 8), (12.2, 14.8, 8)], 0.6, "tetsubin")
    bent(m, "tsuru", [(5.8, 14.6, 8), (6.2, 16.0, 8), (8, 16.5, 8), (9.8, 16.0, 8), (10.2, 14.6, 8)], 0.35, "iron_r")
    # 火箸を灰に立てる
    for dx in (0, 0.6):
        m.cube("hibashi", (3.4 + dx, 9.0, 11.2), (3.7 + dx, 17.0, 11.5), "iron_r", rot=[0, 0, 12], pivot=[3.5 + dx, 9.0, 11.3])
    return m


# ---------------------------------------------------------------- 44 壺

def tsubo():
    from items_hires import ring
    m = F("tsubo", 6)
    # 台：黒漆の花台（脚）
    bevel(m, "stand", (1.4, 1.6, 1.4), (14.6, 2.6, 14.6), "urushi_k", b=0.3)
    for (x, z) in ((1.6, 1.6), (13.2, 1.6), (1.6, 13.2), (13.2, 13.2)):
        bevel(m, "stand_leg", (x, 0, z), (x + 1.2, 1.6, z + 1.2), "urushi_k", b=0.2)
    m.cube("shiki", (3.4, 2.6, 3.4), (12.6, 2.8, 12.6), "mosen")
    # 壺：肩が張って、首がしまり、口が開く。上半分に釉がかかり、たれる
    prof = [(2.8, 3.4, 3.0), (3.4, 5.0, 3.9), (5.0, 7.4, 4.8), (7.4, 10.6, 5.4), (10.6, 13.0, 5.5), (13.0, 14.6, 4.8),
            (14.6, 15.8, 3.6), (15.8, 17.0, 2.4), (17.0, 18.6, 2.1), (18.6, 19.6, 2.6)]
    for (ya, yb, r) in prof:
        m.cyl("pot", 8, 8, ya, yb, r, "tsubo_glaze" if ya >= 12.0 else "tsubo_body", sides=8)
    for k in range(10):
        a = 2 * math.pi * k / 10 + 0.2
        x, z = 8 + 5.48 * math.cos(a), 8 + 5.48 * math.sin(a)
        ln = 1.4 + (k * 7 % 5) * 0.6
        m.cube("drip", (x - 0.35, 12.0 - ln, z - 0.35), (x + 0.35, 12.2, z + 0.35), "tsubo_glaze")
    m.cyl("mouth", 8, 8, 19.55, 19.65, 1.8, "deep_dark", sides=6)
    ring(m, "lip", 8, 19.7, 8, 2.5, 0.35, 0.3, "tsubo_glaze", seg=16, plane="xz")
    # 肩の耳（縄を通す小さな輪）
    for a in (0, 90, 180, 270):
        ang = math.radians(a + 45)
        x, z = 8 + 4.9 * math.cos(ang), 8 + 4.9 * math.sin(ang)
        m.cube("mimi", (x - 0.5, 14.0, z - 0.5), (x + 0.5, 15.0, z + 0.5), "tsubo_glaze")
    return m


OVERRIDES = {"SPAWN": entrance_mat, "BELL": call_bell, "VENT": floor_hatch, "BREAKER": breaker,
             "GAS_VALVE": gas_valve, "BOILER": boiler, "MEETING": meeting_table, "HOKORA": hokora,
             "WIRING": wiring_panel, "LEDGER": ledger, "FUTON": futon, "KAMADO": kamado,
             "BATH": bath_stove, "CHOPPING": chopping_block, "FIELD": vegetable_patch, "DUST": dust_pile,
             "LAUNDRY": laundry_pole, "VASE": flower_vase, "SINK": kitchen_sink, "SHOE_RACK": shoe_rack,
             "SHOJI": shoji_screen, "ANDON": andon, "IRORI": irori, "BONSAI": bonsai,
             "TEA": tea_set, "WELL": well, "SHOGI": shogi_board, "KAKEJIKU": kakejiku,
             "MAILBOX": mailbox, "GOLDFISH": goldfish_bowl, "FURIN": wind_chime, "MOCHI": mochi_mortar,
             "ZEN": zen_garden, "WASHTUB": washtub, "TANSU": tansu, "CHABUDAI": chabudai,
             "BYOBU": byobu, "TOUROU": tourou, "SAKE": sake_barrel, "KASA": kasa_tate,
             "TANUKI": tanuki, "MANEKI": maneki_neko, "HIBACHI": hibachi, "TSUBO": tsubo}
