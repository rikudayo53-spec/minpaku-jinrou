"""
高精細な Blockbench モデル（.bbmodel）を作る道具。

ふつうのマイクラは 1ブロック = 16ピクセルだが、ここでは 1ピクセルの中に D×D のテクセルを描く
（D = 8 なら 1ブロック = 128 ピクセル相当）。形は箱（マイクラらしさ）、模様は細かく描く（リアル寄り）。

  m = HiRes("glock_black", density=8)
  m.cube("slide", (-0.9, 5.6, -6), (0.9, 7.6, 6), "slide_black")
  m.save("out.bbmodel")

テクスチャは箱ごとに自動で並べた1枚の絵（アトラス）にまとめて、.bbmodel の中に埋めこむ。
"""
import base64
import io
import json
import math
import random
import uuid as _uuid

from PIL import Image, ImageDraw


def uid():
    return str(_uuid.uuid4())


def clamp(v):
    return max(0, min(255, int(v)))


def mix(c1, c2, t):
    return tuple(clamp(a + (b - a) * t) for a, b in zip(c1, c2))


def shade(c, k):
    return tuple(clamp(v * k) for v in c[:3])


# ------------------------------------------------------------------ 材質（面の模様の描き方）

class Mat:
    """base = 色、kind = 模様の種類"""

    def __init__(self, base, kind="plain", accent=None, noise=10, edge=0.78, glow=False, alpha=255):
        self.base = base
        self.kind = kind
        self.accent = accent or shade(base, 0.7)
        self.noise = noise
        self.edge = edge
        self.glow = glow
        self.alpha = alpha


MATS = {
    # 銃
    "slide_black": Mat((40, 41, 45), "metal", noise=6),
    "slide_silver": Mat((176, 180, 186), "metal", noise=8),
    "frame_black": Mat((30, 30, 33), "polymer", noise=5),
    "frame_tan": Mat((160, 138, 100), "polymer", noise=6),
    "grip_black": Mat((32, 32, 35), "stipple", noise=8),
    "grip_tan": Mat((156, 134, 96), "stipple", noise=8),
    "steel": Mat((90, 92, 98), "metal", noise=6),
    "sight": Mat((235, 235, 225), "plain", noise=2),
    # 家具
    "wood": Mat((150, 108, 64), "wood", accent=(118, 82, 46)),
    "wood_dark": Mat((84, 56, 34), "wood", accent=(62, 40, 24)),
    "wood_light": Mat((206, 172, 120), "wood", accent=(178, 142, 94)),
    "bamboo": Mat((176, 168, 92), "bamboo", accent=(138, 132, 66)),
    "plaster": Mat((226, 214, 196), "plaster", noise=8),
    "clay": Mat((196, 132, 92), "plaster", noise=12),
    "stone": Mat((136, 136, 132), "stone", noise=16),
    "stone_dark": Mat((92, 92, 90), "stone", noise=14),
    "moss_stone": Mat((112, 122, 96), "stone", accent=(70, 110, 52), noise=16),
    "iron_black": Mat((52, 52, 56), "metal", noise=8),
    "copper": Mat((184, 110, 72), "metal", noise=10),
    "paper": Mat((240, 232, 210), "paper", noise=5),
    "paper_lit": Mat((255, 214, 140), "paper", noise=6, glow=True),
    "fire": Mat((255, 150, 40), "fire", accent=(255, 230, 120), glow=True),
    "ember": Mat((200, 60, 20), "ember", accent=(255, 160, 60), glow=True),
    "charcoal": Mat((34, 30, 30), "stone", noise=10),
    "ash": Mat((150, 146, 140), "plaster", noise=14),
    "water": Mat((60, 120, 200), "water", accent=(120, 180, 240), alpha=200),
    "rope": Mat((196, 170, 110), "rope", accent=(150, 124, 76)),
    "tile": Mat((60, 64, 72), "tile", accent=(40, 42, 48)),
    "rice": Mat((246, 244, 236), "stipple", noise=10),
    "red": Mat((180, 40, 36), "plain", noise=8),
}


def paint(img, x0, y0, w, h, mat, face, rng, along="u", mc=False):
    """1つの面（x0,y0 から w×h テクセル）を材質で塗る"""
    px = img.load()
    base = mat.base
    for j in range(h):
        for i in range(w):
            n = rng.uniform(-mat.noise, mat.noise) * (2.2 if mc else 1.0)
            c = base
            u = i / max(1, w - 1)
            v = j / max(1, h - 1)
            k = mat.kind
            if k == "wood":
                # 木目：長い方向に流れる年輪
                t = (j if along == "u" else i)
                wave = math.sin(t * (1.1 if mc else 0.55) + math.sin((i if along == "u" else j) * 0.08) * 3.0)
                c = mix(base, mat.accent, 0.35 + 0.35 * wave)
                if mc:
                    # マイクラの板材のように、数テクセルごとに板の継ぎ目
                    if (t % 4) == 3:
                        c = shade(c, 0.82)
            elif k == "bamboo":
                t = i if along == "v" else j
                ring = (t % 24) < 2
                c = mat.accent if ring else mix(base, mat.accent, 0.15 * math.sin(t * 0.4))
            elif k == "metal":
                # 上から下へゆるい明暗と、細い光の帯
                c = shade(base, 1.12 - 0.22 * v)
                if abs(v - 0.25) < 0.04:
                    c = mix(c, (255, 255, 255), 0.18)
            elif k == "polymer":
                c = shade(base, 1.05 - 0.1 * v)
            elif k == "stipple":
                c = shade(base, 0.85 + 0.3 * ((i * 7 + j * 13) % 5) / 5)
            elif k == "stone":
                cell = (i // 5 + (j // 4) * 3) % 4
                c = shade(base, 0.9 + 0.05 * cell)
                if (i % 9 == 0 and rng.random() < 0.3) or (j % 7 == 0 and rng.random() < 0.3):
                    c = shade(base, 0.72)
                if mat.accent and rng.random() < 0.12 and mat.accent != shade(base, 0.7):
                    c = mat.accent
            elif k == "plaster":
                c = shade(base, 0.96 + 0.06 * math.sin(i * 0.3 + j * 0.17))
            elif k == "paper":
                fiber = 0.97 + 0.04 * math.sin(i * 1.7 + j * 0.3)
                c = shade(base, fiber)
            elif k == "fire":
                c = mix(base, mat.accent, max(0.0, 1.0 - v * 1.3) * 0.8)
            elif k == "ember":
                c = mix(base, mat.accent, 0.5 + 0.5 * math.sin(i * 0.9 + j * 1.3))
            elif k == "water":
                c = mix(base, mat.accent, 0.3 + 0.3 * math.sin(i * 0.5 + j * 0.2))
            elif k == "rope":
                c = mix(base, mat.accent, 0.5 + 0.5 * math.sin((i + j) * 1.2))
            elif k == "tile":
                c = mat.accent if (j % 6 == 0) else shade(base, 0.95 + 0.08 * ((i // 8) % 2))
            r, g, b = (clamp(c[0] + n), clamp(c[1] + n), clamp(c[2] + n))
            # ふち（角を少し暗く：形がはっきりする）
            if i == 0 or j == 0 or i == w - 1 or j == h - 1:
                r, g, b = shade((r, g, b), mat.edge)
            px[x0 + i, y0 + j] = (r, g, b, mat.alpha)


def _vnoise(rng_seed, i, j, scale):
    """なめらかなノイズ（値ノイズ）"""
    def h(a, b):
        n = (a * 374761393 + b * 668265263 + rng_seed * 2246822519) & 0xFFFFFFFF
        n = ((n ^ (n >> 13)) * 1274126177) & 0xFFFFFFFF
        return ((n ^ (n >> 16)) & 0xFFFF) / 65535.0
    x, y = i / scale, j / scale
    x0, y0 = int(math.floor(x)), int(math.floor(y))
    fx, fy = x - x0, y - y0
    fx, fy = fx * fx * (3 - 2 * fx), fy * fy * (3 - 2 * fy)
    a = h(x0, y0) + (h(x0 + 1, y0) - h(x0, y0)) * fx
    b = h(x0, y0 + 1) + (h(x0 + 1, y0 + 1) - h(x0, y0 + 1)) * fx
    return a + (b - a) * fy


def paint_real(img, x0, y0, w, h, mat, face, rng, along="u", seed=1):
    """リアル寄りの塗り：なめらかな模様＋焼き込んだ陰（下ほど暗い）＋ふちの光と影＋すみの汚れ"""
    px = img.load()
    base = mat.base
    k = mat.kind
    side = face in ("north", "south", "east", "west")
    for j in range(h):
        for i in range(w):
            u = i / max(1, w - 1)
            v = j / max(1, h - 1)
            n1 = _vnoise(seed, i, j, 6.0) - 0.5
            n2 = _vnoise(seed + 7, i, j, 2.0) - 0.5
            c = base
            if k == "wood":
                t = (j if along == "u" else i)
                s_ = (i if along == "u" else j)
                warp = _vnoise(seed + 3, s_, t, 14.0) * 6.0
                ring = math.sin((t + warp) * 0.9)
                c = mix(base, mat.accent, 0.3 + 0.28 * ring + 0.25 * n1)
                if abs(math.sin((t + warp * 2) * 0.35)) > 0.985:
                    c = shade(c, 0.8)          # 濃い木目の線
                # 節（まれに）
                kn = _vnoise(seed + 11, i, j, 9.0)
                if kn > 0.86:
                    c = mix(c, shade(mat.accent, 0.6), (kn - 0.86) * 5)
            elif k == "bamboo":
                t = i if along == "v" else j
                ring = (t % 30) < 2
                c = shade(mat.accent, 0.9) if ring else mix(base, mat.accent, 0.2 + 0.25 * n1)
                c = shade(c, 1.0 + 0.12 * math.sin(u * math.pi))
            elif k == "metal":
                brushed = _vnoise(seed + 5, i, j * 8, 3.0) - 0.5
                c = shade(base, 1.12 - 0.26 * v + 0.12 * brushed)
                c = mix(c, (255, 255, 255), max(0.0, 0.22 - abs(v - 0.22) * 3))
                if mat.noise >= 10:    # 古い金属は錆やくもり
                    c = mix(c, shade(base, 0.7), max(0.0, n1) * 0.6)
            elif k == "polymer":
                c = shade(base, 1.06 - 0.12 * v + 0.05 * n2)
            elif k == "stipple":
                c = shade(base, 0.86 + 0.28 * _vnoise(seed + 2, i, j, 1.2))
            elif k == "stone":
                cell = _vnoise(seed + 9, i, j, 7.0)
                c = shade(base, 0.84 + 0.3 * cell)
                crack = abs(_vnoise(seed + 4, i, j, 10.0) - 0.5) < 0.02
                if crack:
                    c = shade(base, 0.62)
                if mat.accent != shade(base, 0.7) and _vnoise(seed + 13, i, j, 4.0) > 0.68:
                    c = mix(c, mat.accent, 0.7)
            elif k == "plaster":
                c = shade(base, 0.97 + 0.08 * n1 + 0.03 * n2)
                if side:  # 下の方に煤や汚れ
                    c = mix(c, shade(base, 0.62), max(0.0, v - 0.65) * 1.2 * (0.6 + n1))
            elif k == "paper":
                c = shade(base, 0.97 + 0.035 * math.sin(i * 1.7 + j * 0.3) + 0.04 * n1)
            elif k == "fire":
                flick = _vnoise(seed + 1, i, j, 3.0)
                c = mix(base, mat.accent, max(0.0, 1.0 - v * 1.2) * (0.6 + 0.5 * flick))
                c = mix(c, (255, 255, 230), max(0.0, 0.6 - abs(u - 0.5) * 2 - v) * 0.8)
            elif k == "ember":
                c = mix(shade(base, 0.6), mat.accent, _vnoise(seed + 6, i, j, 2.5) ** 1.5 * 1.3)
            elif k == "water":
                c = mix(base, mat.accent, 0.3 + 0.4 * _vnoise(seed + 8, i, j, 5.0))
            elif k == "rope":
                c = mix(base, mat.accent, 0.5 + 0.5 * math.sin((i + j) * 1.0 + n1 * 2))
            elif k == "tile":
                c = shade(mat.accent, 0.9) if (j % 8 == 0) else shade(base, 0.92 + 0.12 * n1 + 0.05 * ((i // 10) % 2))
            else:
                c = shade(base, 1.0 + 0.06 * n1)
            # 焼き込んだ陰：側面は下ほど暗く、上面は少し明るく、底は暗く（なめらかな曲面の材質は陰もふちも描かない）
            smooth = getattr(mat, "smooth", False)
            if smooth and k in ("metal", "polymer", "plaster", "plain"):
                c = shade(base, 1.0 + 0.03 * n1)   # 面ごとの明暗の帯が出ないよう、平らな色に
            if smooth:
                pass
            elif side:
                c = shade(c, 1.04 - 0.16 * v)
            elif face == "up":
                c = shade(c, 1.06)
            else:
                c = shade(c, 0.7)
            if not mat.glow:
                c = tuple(clamp(a + (n2 * mat.noise * 0.5)) for a in c[:3])
            # ふち：上と左は光、下と右は影（面取りしたように見える）。透明なもの（ガラス）はふちを描かない
            if mat.alpha < 128 or smooth:
                pass
            elif j == 0 or i == 0:
                c = mix(c, (255, 255, 255), 0.12) if not mat.glow else c
            elif j == h - 1 or i == w - 1:
                c = shade(c, 0.8)
            px[x0 + i, y0 + j] = (clamp(c[0]), clamp(c[1]), clamp(c[2]), mat.alpha)


# ------------------------------------------------------------------ モデル

FACES = ["north", "east", "south", "west", "up", "down"]


class HiRes:
    def __init__(self, name, density=8, seed=7, mc=False, real=False):
        self.real = real
        self.name = name
        self.D = density
        self.cubes = []          # dict(name, frm, to, mat, rot, pivot, decals, group)
        self.groups = {}         # name -> (origin, parent)
        self.group_rot = {}      # name -> [rx, ry, rz]
        self.group_order = []
        self.rng = random.Random(seed)
        self.mc = mc

    def cyl(self, name, cx, cz, y0, y1, r, mat, sides=4, group=None, rx=None):
        """丸いもの（つぼ・鍋・柱など）。
        real（家具）のときは、外側にそって薄い板を輪に並べた「殻」で作る（長方形を重ねない）。
        同じ中心・材質・グループで、すぐ上に続けて呼んだ輪切りは1つの回転体にまとめ、輪郭をなめらかにつなぐ。
        細いもの（半径 0.7 未満）と rx つきは、今までどおり板を重ねる"""
        if not self.real or rx is not None or r < 0.7 or getattr(self, "boxy", False):
            self._cyl_boxes(name, cx, cz, y0, y1, r, mat, sides, group)
            return
        if not hasattr(self, "_lathes"):
            self._lathes = []
        if self._lathes:
            L = self._lathes[-1]
            if (L["cx"], L["cz"], L["mat"], L["group"]) == (cx, cz, mat, group) and abs(L["sl"][-1][1] - y0) < 0.08:
                L["sl"].append((y0, y1, r))
                return
        self._lathes.append(dict(name=name, cx=cx, cz=cz, mat=mat, group=group, sl=[(y0, y1, r)]))

    def _cyl_boxes(self, name, cx, cz, y0, y1, r, mat, sides, group, skip=(), smooth=False):
        if getattr(self, "light", False):
            sides = min(sides, 2 if r < 0.8 else 3 if r < 1.8 else 4 if r < 3.2 else 6 if r < 5.5 else sides)
        half_w = r * math.tan(math.radians(90.0 / sides))
        for k in range(sides):
            ang = k * (180.0 / sides)
            if ang > 90:
                ang -= 180
            e = k * 0.015   # 上面が重なってちらつかないように少しずらす
            self.cube(name, (cx - r, y0 + e, cz - half_w), (cx + r, y1 - e, cz + half_w), mat,
                      rot=[0, ang, 0] if ang else None, pivot=[cx, y0, cz], group=group, skip=skip, smooth=smooth)

    def _flush_lathes(self):
        """まとめた回転体を、傾けた板の輪＋上下のふたにする"""
        for L in getattr(self, "_lathes", []):
            sl = L["sl"]
            if len(sl) == 1:
                pts = [(sl[0][0], sl[0][2]), (sl[0][1], sl[0][2])]
            else:
                pts = [(sl[0][0], sl[0][2])] + [((a + b) / 2, r) for (a, b, r) in sl] + [(sl[-1][1], sl[-1][2])]
            rmax = max(p[1] for p in pts)
            seg = max(10, min(28, int(2 * math.pi * rmax / 1.1)))
            thick = min(0.35, rmax * 0.25)
            for (ya, ra), (yb, rb) in zip(pts, pts[1:]):
                dy, dr = yb - ya, rb - ra
                ln = math.hypot(dy, dr)
                if ln < 0.01:
                    continue
                rmid, ymid = (ra + rb) / 2, (ya + yb) / 2
                tilt = math.degrees(math.atan2(dr, dy))
                w = 2 * max(rmid, 0.05) * math.tan(math.pi / seg) * 1.06 + 0.04
                for k in range(seg):
                    a = 2 * math.pi * (k + 0.5) / seg
                    x, z = L["cx"] + (rmid - thick / 2) * math.cos(a), L["cz"] + (rmid - thick / 2) * math.sin(a)
                    # 外向きの面（south）だけ描く
                    self.cube(L["name"], (x - w / 2, ymid - ln / 2 - 0.04, z - thick / 2), (x + w / 2, ymid + ln / 2 + 0.04, z + thick / 2),
                              L["mat"], rot=[tilt, 90 - math.degrees(a), 0], pivot=[x, ymid, z], group=L["group"],
                              skip=("north", "east", "west", "up", "down"), smooth=True)
            # ふた（上と下）：上は上の面だけ、下は下の面だけ
            (y0, r0), (y1, r1) = pts[0], pts[-1]
            sides = 4 if max(r0, r1) < 3 else 6
            if r1 > 0.3:
                self._cyl_boxes(L["name"], L["cx"], L["cz"], y1 - 0.1, y1, r1 - thick * 0.2, L["mat"], sides, L["group"],
                                skip=("north", "east", "south", "west", "down"), smooth=True)
            if r0 > 0.3:
                self._cyl_boxes(L["name"], L["cx"], L["cz"], y0, y0 + 0.1, r0 - thick * 0.2, L["mat"], sides, L["group"],
                                skip=("north", "east", "south", "west", "up"), smooth=True)
        self._lathes = []

    def group(self, name, origin=(0, 0, 0), parent=None, rot=None):
        """rot = 元から傾けておく角度（度）。アニメーションはこの傾きからの変化になる"""
        self.groups[name] = (list(origin), parent)
        if rot:
            self.group_rot[name] = list(rot)
        self.group_order.append(name)
        return name

    def cube(self, name, frm, to, mat, rot=None, pivot=None, group=None, decal=None, along=None, skip=(), smooth=False):
        """decal = 面ごとに追加で描く関数 decal(draw, face, x0, y0, w, h)。smooth = 陰やふちを描かない（曲面の板）"""
        self.cubes.append(dict(name=name, frm=list(frm), to=list(to), mat=mat, rot=rot, pivot=pivot, group=group,
                               decal=decal, along=along, skip=tuple(skip), smooth=smooth))

    # ---- アトラス
    def _face_size(self, c, f):
        sx = c["to"][0] - c["frm"][0]
        sy = c["to"][1] - c["frm"][1]
        sz = c["to"][2] - c["frm"][2]
        if f in ("north", "south"):
            a, b = sx, sy
        elif f in ("east", "west"):
            a, b = sz, sy
        else:
            a, b = sx, sz
        return max(1, int(round(a * self.D))), max(1, int(round(b * self.D)))

    def build_texture(self):
        self._flush_lathes()
        rects = []
        for ci, c in enumerate(self.cubes):
            for f in FACES:
                if f in c.get("skip", ()):
                    continue
                w, h = self._face_size(c, f)
                rects.append((ci, f, w, h))
        # 棚に並べる（高い順）
        rects.sort(key=lambda r: -r[3])
        W = 64
        while True:
            x = y = row = 0
            pos = {}
            ok = True
            for (ci, f, w, h) in rects:
                if w > W:
                    ok = False
                    break
                if x + w > W:
                    x = 0
                    y += row
                    row = 0
                pos[(ci, f)] = (x, y)
                x += w
                row = max(row, h)
            H = y + row
            if ok and H <= W:
                break
            W *= 2
        img = Image.new("RGBA", (W, W), (0, 0, 0, 0))
        draw = ImageDraw.Draw(img)
        for (ci, f, w, h) in rects:
            c = self.cubes[ci]
            x0, y0 = pos[(ci, f)]
            mat = MATS[c["mat"]] if isinstance(c["mat"], str) else c["mat"]
            if c.get("smooth") and not getattr(mat, "smooth", False):
                import copy
                key = id(mat)
                if not hasattr(self, "_smooth_mats"):
                    self._smooth_mats = {}
                if key not in self._smooth_mats:
                    sm_ = copy.copy(mat)
                    sm_.smooth = True
                    self._smooth_mats[key] = sm_
                mat = self._smooth_mats[key]
            along = c["along"] or ("u" if f in ("north", "south", "up", "down") else "u")
            if self.real:
                paint_real(img, x0, y0, w, h, mat, f, self.rng, along, seed=ci * 31 + FACES.index(f) * 7 + 1)
            else:
                paint(img, x0, y0, w, h, mat, f, self.rng, along, self.mc)
            if c["decal"]:
                c["decal"](draw, f, x0, y0, w, h)
        self.atlas = img
        self.uv = {k: (v[0], v[1]) for k, v in pos.items()}
        self.size = W
        return img

    # ---- 書き出し（Blockbench Free 形式、面ごとの UV）
    def to_json(self):
        self.build_texture()
        elements = []
        el_uuid = []
        for ci, c in enumerate(self.cubes):
            faces = {}
            for f in FACES:
                w, h = self._face_size(c, f)
                if (ci, f) not in self.uv:
                    faces[f] = {"uv": [0, 0, 0, 0], "texture": None}
                    continue
                x0, y0 = self.uv[(ci, f)]
                faces[f] = {"uv": [x0, y0, x0 + w, y0 + h], "texture": 0 if f not in c.get("skip", ()) else None}
            u = uid()
            el_uuid.append(u)
            pivot = c["pivot"] or [(a + b) / 2 for a, b in zip(c["frm"], c["to"])]
            elements.append({"name": c["name"], "box_uv": False, "rescale": False, "locked": False,
                             "render_order": "default", "from": c["frm"], "to": c["to"], "autouv": 0, "color": 0,
                             "origin": pivot, "rotation": list(c["rot"] or [0, 0, 0]), "faces": faces,
                             "type": "cube", "uuid": u})
        buf = io.BytesIO()
        self.atlas.save(buf, "PNG")
        tex = {"path": "", "name": self.name + ".png", "folder": "", "namespace": "", "id": "0", "group": "",
               "width": self.size, "height": self.size, "uv_width": self.size, "uv_height": self.size,
               "particle": False, "use_as_default": True, "layers_enabled": False, "render_mode": "default",
               "render_sides": "auto", "frame_time": 1, "frame_order_type": "loop", "frame_order": "",
               "frame_interpolate": False, "visible": True, "internal": True, "saved": True, "uuid": uid(),
               "source": "data:image/png;base64," + base64.b64encode(buf.getvalue()).decode()}
        # グループ（なければ1つにまとめる）
        outl = []
        gmap = {}
        for gname in self.group_order:
            origin, parent = self.groups[gname]
            node = {"name": gname, "origin": origin, "color": 0, "uuid": uid(), "export": True, "isOpen": True,
                    "locked": False, "visibility": True, "autouv": 0, "children": []}
            if gname in self.group_rot:
                node["rotation"] = self.group_rot[gname]
            gmap[gname] = node
            (gmap[parent]["children"] if parent else outl).append(node)
        loose = []
        for ci, c in enumerate(self.cubes):
            if c["group"] and c["group"] in gmap:
                gmap[c["group"]]["children"].append(el_uuid[ci])
            else:
                loose.append(el_uuid[ci])
        outl.extend(loose)
        return {"meta": {"format_version": "4.10", "model_format": "free", "box_uv": False},
                "name": self.name, "model_identifier": "", "visible_box": [1, 1, 0],
                "resolution": {"width": self.size, "height": self.size},
                "elements": elements, "outliner": outl, "textures": [tex], "animations": []}

    # ---- GeckoLib 用の書き出し（.geo.json ＋ テクスチャ ＋ 光る部分のマスク）
    def to_geo(self, center=True):
        """Bedrock 形式のモデル。GeckoLib は X を左右反転して読むので、ここで反転して書く
        （読みこまれた後はマイクラの座標と同じになる）。center=True ならブロックの中心を原点にする"""
        if not hasattr(self, "atlas"):
            self.build_texture()
        ox = 8.0 if center else 0.0
        oz = 8.0 if center else 0.0

        def X(x):
            return round(-(x - ox), 4)

        bones = [{"name": "root", "pivot": [0, 0, 0]}]
        index = {"root": bones[0]}
        for gname in self.group_order:
            origin, parent = self.groups[gname]
            b = {"name": gname, "parent": parent or "root",
                 "pivot": [X(origin[0]), round(origin[1], 4), round(origin[2] - oz, 4)]}
            if gname in self.group_rot:
                r = self.group_rot[gname]
                b["rotation"] = [-r[0], -r[1], r[2]]
            bones.append(b)
            index[gname] = b
        for ci, c in enumerate(self.cubes):
            frm, to = c["frm"], c["to"]
            size = [round(to[i] - frm[i], 4) for i in range(3)]
            cube = {"origin": [X(to[0]), round(frm[1], 4), round(frm[2] - oz, 4)], "size": size, "uv": {}}
            if c["rot"] and any(c["rot"]):
                piv = c["pivot"] or [(a + b) / 2 for a, b in zip(frm, to)]
                cube["pivot"] = [X(piv[0]), round(piv[1], 4), round(piv[2] - oz, 4)]
                cube["rotation"] = [-c["rot"][0], -c["rot"][1], c["rot"][2]]
            for f in FACES:
                if f in c.get("skip", ()):
                    continue
                w, h = self._face_size(c, f)
                x0, y0 = self.uv[(ci, f)]
                if f in ("up", "down"):
                    # Blockbench の Bedrock 書き出しと同じ：上と下の面は UV を上下左右反転して書く
                    cube["uv"][f] = {"uv": [x0 + w, y0 + h], "uv_size": [-w, -h]}
                else:
                    cube["uv"][f] = {"uv": [x0, y0], "uv_size": [w, h]}
            bone = index.get(c["group"]) if c["group"] else None
            if bone is None:
                bone = bones[0]
            bone.setdefault("cubes", []).append(cube)
        return {"format_version": "1.12.0", "minecraft:geometry": [{
            "description": {"identifier": "geometry." + self.name, "texture_width": self.size, "texture_height": self.size,
                            "visible_bounds_width": 4, "visible_bounds_height": 4, "visible_bounds_offset": [0, 1.5, 0]},
            "bones": bones}]}

    def glowmask(self):
        """光る箱（名前が glow_）の面だけを残した絵（GeckoLib の _glowmask）"""
        if not hasattr(self, "atlas"):
            self.build_texture()
        mask = Image.new("RGBA", self.atlas.size, (0, 0, 0, 0))
        any_glow = False
        for ci, c in enumerate(self.cubes):
            if not c["name"].startswith("glow_"):
                continue
            any_glow = True
            for f in FACES:
                if (ci, f) not in self.uv:
                    continue
                w, h = self._face_size(c, f)
                x0, y0 = self.uv[(ci, f)]
                mask.paste(self.atlas.crop((x0, y0, x0 + w, y0 + h)), (x0, y0))
        return mask if any_glow else None

    def save_geo(self, root, kind="block", center=True):
        """assets/minpaku/ の下に geo/<kind>/<name>.geo.json と textures/<kind>/geo/<name>.png を書く"""
        import os
        geo = self.to_geo(center)
        gdir = os.path.join(root, "geo", kind)
        tdir = os.path.join(root, "textures", kind, "geo")
        os.makedirs(gdir, exist_ok=True)
        os.makedirs(tdir, exist_ok=True)
        with open(os.path.join(gdir, self.name + ".geo.json"), "w", encoding="utf-8") as f:
            json.dump(geo, f, ensure_ascii=False, separators=(",", ":"))
        self.atlas.save(os.path.join(tdir, self.name + ".png"))
        gm = self.glowmask()
        gpath = os.path.join(tdir, self.name + "_glowmask.png")
        if gm is not None:
            gm.save(gpath)
        elif os.path.exists(gpath):
            os.remove(gpath)

    def save(self, path):
        with open(path, "w", encoding="utf-8") as f:
            json.dump(self.to_json(), f, ensure_ascii=False, separators=(",", ":"))
