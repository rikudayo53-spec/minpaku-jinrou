"""
役職ミニキャラ（和風の擬人化・二頭身）のドット絵を作る。
形（楕円・四角・多角形）を 0〜1 の座標で置いて、指定の大きさのドットにする。
最後に外側へ1ドットのふち（輪郭）をつける。
"""
import math

from PIL import Image


def hexc(h):
    h = h.lstrip("#")
    return tuple(int(h[i:i + 2], 16) for i in (0, 2, 4)) + (255,)


def dark(c, k=0.6):
    return tuple(int(v * k) for v in c[:3]) + (255,)


def light(c, k=0.35):
    return tuple(int(v + (255 - v) * k) for v in c[:3]) + (255,)


BLINK = [False]
SKIN = hexc("#f7d2b0")
SKIN_S = hexc("#e3a982")
EYE = hexc("#2a1d2e")
WHITE = hexc("#fbf7ee")
BLUSH = hexc("#f19a9a")


class Canvas:
    def __init__(self, n):
        self.n = n
        self.px = [[None] * n for _ in range(n)]
        self.noline = set()

    def _put(self, x, y, c, line=True):
        if 0 <= x < self.n and 0 <= y < self.n:
            self.px[y][x] = c
            if not line:
                self.noline.add((x, y))

    def shape(self, test, c):
        n = self.n
        for y in range(n):
            for x in range(n):
                u, v = (x + 0.5) / n, (y + 0.5) / n
                if test(u, v):
                    self.px[y][x] = c

    def ellipse(self, cx, cy, rx, ry, c):
        self.shape(lambda u, v: ((u - cx) / rx) ** 2 + ((v - cy) / ry) ** 2 <= 1, c)

    def rect(self, x0, y0, x1, y1, c):
        self.shape(lambda u, v: x0 <= u <= x1 and y0 <= v <= y1, c)

    def poly(self, pts, c, clip=None):
        def inside(u, v):
            j = len(pts) - 1
            ins = False
            for i in range(len(pts)):
                xi, yi = pts[i]
                xj, yj = pts[j]
                if (yi > v) != (yj > v) and u < (xj - xi) * (v - yi) / (yj - yi + 1e-9) + xi:
                    ins = not ins
                j = i
            return ins and (clip is None or clip(u, v))
        self.shape(inside, c)

    def line(self, x0, y0, x1, y1, w, c):
        def near(u, v):
            dx, dy = x1 - x0, y1 - y0
            t = max(0, min(1, ((u - x0) * dx + (v - y0) * dy) / (dx * dx + dy * dy + 1e-9)))
            return math.hypot(u - (x0 + t * dx), v - (y0 + t * dy)) <= w / 2
        self.shape(near, c)

    def dot(self, u, v, c, w=1, h=1):
        x, y = int(u * self.n), int(v * self.n)
        for dy in range(h):
            for dx in range(w):
                self._put(x + dx, y + dy, c)

    def shade(self):
        """色のかたまりごとに、右下を暗く・左上を明るく（光は左上から）"""
        n = self.n
        seen = [[False] * n for _ in range(n)]
        size = {}
        for y in range(n):
            for x in range(n):
                if self.px[y][x] is None or seen[y][x]:
                    continue
                col = self.px[y][x]
                stack = [(x, y)]
                seen[y][x] = True
                cells = []
                while stack:
                    a, b = stack.pop()
                    cells.append((a, b))
                    for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                        aa, bb = a + dx, b + dy
                        if 0 <= aa < n and 0 <= bb < n and not seen[bb][aa] and self.px[bb][aa] == col:
                            seen[bb][aa] = True
                            stack.append((aa, bb))
                for cell in cells:
                    size[cell] = len(cells)
        src = [row[:] for row in self.px]
        for y in range(n):
            for x in range(n):
                col = src[y][x]
                if col is None or size.get((x, y), 0) < 10 or col in (EYE,):
                    continue

                def diff(a, b):
                    return not (0 <= a < n and 0 <= b < n) or src[b][a] != col
                if col == SKIN:
                    # 前髪の下に影
                    if y > 0 and src[y - 1][x] not in (None, SKIN) and src[y - 1][x] != BLUSH:
                        self.px[y][x] = SKIN_S
                    continue
                if diff(x + 1, y) or diff(x, y + 1):
                    self.px[y][x] = dark(col, 0.82)
                elif diff(x - 1, y) or diff(x, y - 1):
                    self.px[y][x] = light(col, 0.18)

    def outline(self, col=hexc("#2b1d12")):
        n = self.n
        add = []
        for y in range(n):
            for x in range(n):
                if self.px[y][x] is None:
                    for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                        xx, yy = x + dx, y + dy
                        if 0 <= xx < n and 0 <= yy < n and self.px[yy][xx] is not None:
                            add.append((x, y))
                            break
        for x, y in add:
            # となりの色を暗くしたふち（やわらかく見える）
            nb = None
            for dx, dy in ((0, 1), (0, -1), (1, 0), (-1, 0)):
                xx, yy = x + dx, y + dy
                if 0 <= xx < n and 0 <= yy < n and self.px[yy][xx] is not None:
                    nb = self.px[yy][xx]
                    break
            self.px[y][x] = dark(nb, 0.38) if nb else col

    def image(self, scale=1):
        img = Image.new("RGBA", (self.n, self.n), (0, 0, 0, 0))
        for y in range(self.n):
            for x in range(self.n):
                if self.px[y][x]:
                    img.putpixel((x, y), self.px[y][x])
        if scale > 1:
            img = img.resize((self.n * scale, self.n * scale), Image.NEAREST)
        return img


# ---------------------------------------------------------------- からだの基本（二頭身）

HEAD = (0.5, 0.36, 0.30, 0.27)   # 頭の中心と半径


def body(c, top, bottom=None, sash=None, sleeve=None, legs=hexc("#3b2b2b"), hakama=None):
    """着物の体：top=上衣、bottom=袴（なければ着流し）、sash=帯"""
    sleeve = sleeve or top
    # 袖（ひし形にふくらむ）
    c.poly([(0.2, 0.62), (0.32, 0.6), (0.34, 0.8), (0.16, 0.82)], sleeve)
    c.poly([(0.8, 0.62), (0.68, 0.6), (0.66, 0.8), (0.84, 0.82)], sleeve)
    c.poly([(0.18, 0.78), (0.34, 0.78), (0.34, 0.83), (0.18, 0.84)], dark(sleeve, 0.8))
    c.poly([(0.82, 0.78), (0.66, 0.78), (0.66, 0.83), (0.82, 0.84)], dark(sleeve, 0.8))
    # 胴
    c.poly([(0.33, 0.6), (0.67, 0.6), (0.7, 0.84), (0.3, 0.84)], top)
    # 下（袴か着物のすそ）
    lower = hakama or bottom or top
    c.poly([(0.3, 0.8), (0.7, 0.8), (0.76, 0.95), (0.24, 0.95)], lower)
    if hakama:
        c.rect(0.49, 0.85, 0.51, 0.95, dark(hakama, 0.75))
    # 足
    c.rect(0.33, 0.95, 0.44, 0.985, legs)
    c.rect(0.56, 0.95, 0.67, 0.985, legs)
    # しわ・ひだ
    c.line(0.24, 0.66, 0.22, 0.78, 0.015, dark(sleeve, 0.78))
    c.line(0.76, 0.66, 0.78, 0.78, 0.015, dark(sleeve, 0.78))
    if hakama:
        for hx in (0.36, 0.43, 0.57, 0.64):
            c.line(hx, 0.84, hx - (0.5 - hx) * 0.25, 0.94, 0.012, dark(hakama, 0.8))
    else:
        c.line(0.42, 0.62, 0.36, 0.94, 0.012, dark(lower, 0.8))
    # えり（白い V）
    c.poly([(0.42, 0.6), (0.5, 0.71), (0.58, 0.6), (0.54, 0.6), (0.5, 0.66), (0.46, 0.6)], WHITE)
    if sash:
        c.rect(0.31, 0.75, 0.69, 0.8, sash)
    # 手
    c.ellipse(0.22, 0.83, 0.045, 0.035, SKIN)
    c.ellipse(0.78, 0.83, 0.045, 0.035, SKIN)


def head(c, hair, back=None, bangs="straight", eyes="normal", mouth="smile", blush=True, eye_col=EYE):
    """頭：後ろ髪 → 顔 → 前髪 → 目"""
    cx, cy, rx, ry = HEAD
    if BLINK[0] and eyes not in ("closed",):
        eyes = "blink"
    if back == "long":
        c.poly([(0.22, 0.3), (0.78, 0.3), (0.8, 0.7), (0.2, 0.7)], dark(hair, 0.85))
    elif back == "pony":
        c.ellipse(0.82, 0.3, 0.09, 0.14, dark(hair, 0.85))
    elif back == "bun":
        c.ellipse(0.5, 0.1, 0.11, 0.08, hair)
    c.ellipse(cx, cy, rx, ry, hair)                       # 頭（髪）
    c.ellipse(cx, cy + 0.06, rx * 0.86, ry * 0.78, SKIN)    # 顔
    c.rect(0.28, cy + 0.05, 0.72, cy + 0.06, SKIN)
    def inhead(u, v):
        return ((u - cx) / (rx * 1.02)) ** 2 + ((v - cy) / ry) ** 2 <= 1 or v > 0.3

    # 前髪
    if bangs == "straight":
        c.poly([(0.22, 0.12), (0.78, 0.12), (0.78, 0.3), (0.76, 0.4), (0.66, 0.33), (0.58, 0.39), (0.5, 0.33), (0.42, 0.39),
                (0.34, 0.33), (0.24, 0.4), (0.22, 0.3)], hair, clip=inhead)
    elif bangs == "side":
        c.poly([(0.22, 0.12), (0.78, 0.12), (0.78, 0.36), (0.6, 0.3), (0.36, 0.42), (0.22, 0.42)], hair, clip=inhead)
    elif bangs == "spiky":
        c.poly([(0.2, 0.12), (0.8, 0.12), (0.8, 0.3), (0.78, 0.42), (0.7, 0.33), (0.62, 0.41), (0.54, 0.32), (0.46, 0.41),
                (0.38, 0.32), (0.3, 0.41), (0.22, 0.34), (0.2, 0.3)], hair, clip=inhead)
    elif bangs == "hood":
        pass
    # 髪の毛すじ・つや
    if bangs != "hood":
        for hx in (0.36, 0.5, 0.64):
            c.line(hx, 0.14, hx - 0.03, 0.3, 0.012, dark(hair, 0.78))
    c.line(0.3, 0.19, 0.44, 0.13, 0.025, light(hair, 0.45))
    c.line(0.56, 0.13, 0.62, 0.14, 0.02, light(hair, 0.3))
    # 目
    n = c.n
    eh = max(2, round(n / 12))
    ew = max(1, round(n / 16))
    ey = cy + 0.08
    for ex in (0.39, 0.61):
        x0 = int(ex * n) - ew // 2
        y0 = int(ey * n) - eh // 2
        if eyes in ("closed", "blink"):
            for dx in range(ew + 1):
                c._put(x0 + dx - (ew // 2), y0 + eh - 1, eye_col)
        elif eyes == "swirl":
            for dx in range(ew + 1):
                c._put(x0 + dx, y0, eye_col)
                c._put(x0 + dx, y0 + eh - 1, eye_col)
            c._put(x0, y0 + 1, eye_col)
        elif eyes == "sharp":
            for dx in range(ew + 1):
                c._put(x0 + dx, y0 + 1, eye_col)
            for dy in range(1, eh):
                for dx in range(ew):
                    c._put(x0 + dx, y0 + dy, eye_col)
        else:
            for dy in range(eh):
                for dx in range(ew):
                    col = dark(eye_col, 0.55) if dy == 0 else (light(eye_col, 0.25) if dy == eh - 1 and eye_col != EYE else eye_col)
                    c._put(x0 + dx, y0 + dy, col)
            if n >= 32:
                c._put(x0 + (ew - 1 if ex > 0.5 else 0), y0 + 1, WHITE)
    if blush:
        for ex in (0.31, 0.69):
            c.dot(ex - 0.03, ey + 0.07, BLUSH, w=max(1, n // 24) + 1)
    my = ey + 0.1
    if mouth == "smile":
        c.dot(0.48, my, dark(SKIN_S, 0.8), w=max(1, n // 16))
    elif mouth == "fang":
        c.dot(0.48, my, dark(SKIN_S, 0.8), w=max(1, n // 16))
        c.dot(0.53, my + 0.02, WHITE)
    elif mouth == "open":
        c.dot(0.48, my, hexc("#8a2b2b"), w=max(1, n // 16), h=max(1, n // 24))


# ---------------------------------------------------------------- 役職

def wolf(c):
    hair = hexc("#5b5f6b")
    # しっぽ
    c.poly([(0.72, 0.8), (0.95, 0.62), (0.97, 0.74), (0.78, 0.9)], hexc("#7d8190"))
    c.poly([(0.9, 0.62), (0.97, 0.6), (0.98, 0.7)], WHITE)
    body(c, hexc("#2b2b33"), sash=hexc("#b8282e"), hakama=hexc("#3a1f22"), sleeve=hexc("#b8282e"))
    head(c, hair, back="long", bangs="spiky", eyes="sharp", mouth="fang", eye_col=hexc("#d8262f"))
    # 狼の耳
    for s in (-1, 1):
        x = 0.5 + s * 0.2
        c.poly([(x - 0.09, 0.2), (x + s * 0.06, 0.02), (x + 0.09, 0.2)], hair)
        c.poly([(x - 0.045, 0.18), (x + s * 0.045, 0.08), (x + 0.045, 0.18)], hexc("#f0a8a8"))
    # 爪
    for s, x in ((-1, 0.18), (1, 0.82)):
        for k in range(3):
            c.line(x + s * 0.02 * k - 0.02, 0.85, x + s * 0.02 * k - 0.03, 0.9, 0.015, WHITE)


def seer(c):
    hair = hexc("#2b2440")
    body(c, WHITE, hakama=hexc("#c8372d"), sleeve=WHITE)
    head(c, hair, back="long", bangs="straight", eyes="closed")
    # 髪飾り
    c.ellipse(0.27, 0.27, 0.05, 0.04, hexc("#c8372d"))
    # 水晶玉
    c.ellipse(0.5, 0.86, 0.11, 0.1, hexc("#7fd3ff"))
    c.ellipse(0.47, 0.83, 0.035, 0.03, WHITE)
    c.rect(0.4, 0.93, 0.6, 0.97, hexc("#8a5a2b"))


def knight(c):
    hair = hexc("#3b2a1e")
    body(c, hexc("#2f4f8f"), hakama=hexc("#26304a"), sash=hexc("#d8b04a"), sleeve=hexc("#2f4f8f"))
    head(c, hair, back="pony", bangs="side", eyes="normal", mouth="smile")
    # はちまき
    c.rect(0.22, 0.24, 0.78, 0.28, WHITE)
    c.poly([(0.78, 0.24), (0.9, 0.2), (0.92, 0.26), (0.78, 0.28)], WHITE)
    # 刀（背中にななめ）
    c.line(0.12, 0.98, 0.3, 0.55, 0.035, hexc("#c9ced6"))
    c.line(0.28, 0.6, 0.34, 0.5, 0.045, hexc("#3b2a1e"))
    c.rect(0.25, 0.58, 0.33, 0.61, hexc("#d8b04a"))


def okami(c):
    hair = hexc("#1e1a1a")
    body(c, hexc("#7a3b8f"), sash=hexc("#e8c46a"), sleeve=hexc("#7a3b8f"), bottom=hexc("#7a3b8f"))
    # 後光（神）
    c.ellipse(0.5, 0.33, 0.36, 0.33, hexc("#ffe58a"))
    c.ellipse(0.5, 0.33, 0.32, 0.29, (0, 0, 0, 0))
    head(c, hair, back="bun", bangs="side", eyes="closed")
    c.line(0.38, 0.06, 0.62, 0.14, 0.02, hexc("#e8c46a"))   # かんざし
    # 天秤のような光（すべてを見通す）
    c.ellipse(0.5, 0.75, 0.04, 0.035, hexc("#ffe58a"))


def villager(c):
    body(c, hexc("#6fa8dc"), sash=hexc("#2f4f8f"), bottom=hexc("#6fa8dc"))
    head(c, hexc("#4a3020"), bangs="straight", eyes="normal")
    # うちわ
    c.ellipse(0.82, 0.68, 0.1, 0.09, hexc("#f2c94c"))
    c.ellipse(0.82, 0.68, 0.05, 0.045, hexc("#e53935"))
    c.line(0.8, 0.76, 0.79, 0.84, 0.025, hexc("#8a5a2b"))


def madman(c):
    hair = hexc("#6b2d7b")
    body(c, hexc("#4a3a4a"), sash=hexc("#8f2d8f"), hakama=hexc("#3a2a3a"), sleeve=hexc("#4a3a4a"))
    # 破れた袖
    c.poly([(0.16, 0.8), (0.2, 0.86), (0.24, 0.81)], (0, 0, 0, 0))
    head(c, hair, back="long", bangs="spiky", eyes="swirl", mouth="open")
    # はねた髪
    c.line(0.22, 0.22, 0.12, 0.12, 0.04, hair)
    c.line(0.75, 0.18, 0.86, 0.08, 0.04, hair)
    c.line(0.5, 0.1, 0.52, 0.02, 0.04, hair)


def shinobi(c):
    black = hexc("#24242c")
    body(c, black, hakama=black, sash=hexc("#5c6bc0"), sleeve=black)
    head(c, black, bangs="side", eyes="sharp", eye_col=hexc("#d8262f"), blush=True, mouth=None)
    # 口にくわえた巻物
    c.rect(0.2, 0.555, 0.82, 0.625, hexc("#efe2bf"))
    c.rect(0.2, 0.555, 0.82, 0.565, light(hexc("#efe2bf"), 0.4))
    c.rect(0.47, 0.55, 0.52, 0.63, hexc("#c8372d"))         # ひも
    c.rect(0.15, 0.545, 0.21, 0.635, hexc("#6b3a1e"))       # 軸のはし
    c.rect(0.81, 0.545, 0.87, 0.635, hexc("#6b3a1e"))
    # 耳
    for s in (-1, 1):
        x = 0.5 + s * 0.2
        c.poly([(x - 0.08, 0.18), (x + s * 0.05, 0.03), (x + 0.08, 0.18)], black)
    # 手裏剣
    c.poly([(0.82, 0.78), (0.86, 0.86), (0.94, 0.88), (0.86, 0.9), (0.82, 0.98), (0.78, 0.9), (0.7, 0.88), (0.78, 0.86)],
           hexc("#c9ced6"))


def kairiki(c):
    hair = hexc("#2b1d12")
    # 大きい体（上半身はだか＋まわし）
    c.ellipse(0.5, 0.78, 0.3, 0.2, SKIN)
    c.ellipse(0.5, 0.8, 0.22, 0.12, light(SKIN, 0.1))
    c.rect(0.25, 0.86, 0.75, 0.94, hexc("#d84315"))
    c.rect(0.46, 0.86, 0.54, 0.98, hexc("#d84315"))
    c.ellipse(0.2, 0.72, 0.08, 0.1, SKIN)
    c.ellipse(0.8, 0.72, 0.08, 0.1, SKIN)
    head(c, hair, back="bun", bangs="side", eyes="sharp", mouth="fang")
    for s in (-1, 1):
        x = 0.5 + s * 0.2
        c.poly([(x - 0.08, 0.2), (x + s * 0.05, 0.05), (x + 0.08, 0.2)], hair)
    # 力こぶの線
    c.line(0.17, 0.7, 0.22, 0.68, 0.015, SKIN_S)
    c.line(0.78, 0.68, 0.83, 0.7, 0.015, SKIN_S)


def juro(c):
    hair = hexc("#3d2b5c")
    body(c, hexc("#7e57c2"), hakama=hexc("#3a2a5c"), sleeve=hexc("#9b7ad8"))
    head(c, hair, back="long", bangs="straight", eyes="sharp", eye_col=hexc("#9fb4ff"))
    # 烏帽子
    c.poly([(0.36, 0.12), (0.62, 0.02), (0.66, 0.14)], hexc("#1e1a24"))
    for s in (-1, 1):
        x = 0.5 + s * 0.22
        c.poly([(x - 0.07, 0.2), (x + s * 0.05, 0.07), (x + 0.07, 0.2)], hair)
    # お札
    c.rect(0.76, 0.6, 0.86, 0.84, WHITE)
    c.rect(0.8, 0.63, 0.82, 0.81, hexc("#c8372d"))
    # 鬼火
    c.ellipse(0.12, 0.6, 0.05, 0.06, hexc("#80e8ff"))
    c.ellipse(0.12, 0.61, 0.025, 0.03, WHITE)


def card(c, x, y, mark, col):
    c.rect(x - 0.07, y - 0.1, x + 0.07, y + 0.1, WHITE)
    c.rect(x - 0.05, y - 0.08, x + 0.05, y + 0.08, light(col, 0.6))
    c.rect(x - 0.01, y - 0.06, x + 0.01, y + 0.02, col) if mark == "!" else None
    c.dot(x - 0.01, y + 0.04, col, w=1, h=1)
    if mark == "?":
        c.line(x - 0.03, y - 0.05, x + 0.03, y - 0.05, 0.02, col)
        c.line(x + 0.03, y - 0.05, x, y + 0.01, 0.02, col)


def evil_guesser(c):
    hair = hexc("#4b3b3b")
    body(c, hexc("#5a1e24"), hakama=hexc("#2b1418"), sash=hexc("#e8c46a"), sleeve=hexc("#5a1e24"))
    head(c, hair, back="pony", bangs="side", eyes="sharp", mouth="fang", eye_col=hexc("#d8262f"))
    for s in (-1, 1):
        x = 0.5 + s * 0.2
        c.poly([(x - 0.08, 0.2), (x + s * 0.05, 0.05), (x + 0.08, 0.2)], hair)
    card(c, 0.84, 0.7, "?", hexc("#b8282e"))
    card(c, 0.16, 0.72, "?", hexc("#b8282e"))


def nice_guesser(c):
    hair = hexc("#2b1d12")
    body(c, hexc("#3f6e4a"), hakama=hexc("#5a4630"), sleeve=hexc("#3f6e4a"))
    head(c, hair, bangs="straight", eyes="normal")
    # 学帽
    c.ellipse(0.5, 0.15, 0.26, 0.1, hexc("#1e1e26"))
    c.rect(0.24, 0.17, 0.62, 0.22, hexc("#1e1e26"))
    c.dot(0.48, 0.15, hexc("#e8c46a"), w=2)
    card(c, 0.84, 0.7, "!", hexc("#2f7d4a"))


def detective(c):
    hair = hexc("#6b4a2e")
    body(c, hexc("#8d6e63"), sash=hexc("#4a3020"), bottom=hexc("#8d6e63"), sleeve=hexc("#a1887f"))
    head(c, hair, bangs="side", eyes="normal")
    # 鳥打帽
    c.ellipse(0.5, 0.15, 0.27, 0.11, hexc("#6d4c41"))
    c.rect(0.5, 0.18, 0.78, 0.22, hexc("#5d4037"))
    # 虫めがね
    c.ellipse(0.83, 0.62, 0.08, 0.08, hexc("#3b2a1e"))
    c.ellipse(0.83, 0.62, 0.055, 0.055, hexc("#bfe8ff"))
    c.line(0.78, 0.68, 0.76, 0.82, 0.03, hexc("#3b2a1e"))


def nekomata(c):
    hair = hexc("#f2efe8")
    # 二股しっぽ
    c.line(0.72, 0.88, 0.94, 0.58, 0.05, hexc("#3a3030"))
    c.line(0.72, 0.88, 0.98, 0.72, 0.05, hexc("#3a3030"))
    c.ellipse(0.94, 0.57, 0.03, 0.03, hexc("#c9a0ff"))
    c.ellipse(0.98, 0.71, 0.03, 0.03, hexc("#c9a0ff"))
    body(c, hexc("#f48fb1"), sash=hexc("#3a3030"), bottom=hexc("#f48fb1"))
    head(c, hair, back="long", bangs="straight", eyes="normal", mouth="fang", eye_col=hexc("#c79a00"))
    for s in (-1, 1):
        x = 0.5 + s * 0.2
        c.poly([(x - 0.09, 0.2), (x + s * 0.06, 0.03), (x + 0.09, 0.2)], hexc("#3a3030"))
        c.poly([(x - 0.045, 0.18), (x + s * 0.04, 0.09), (x + 0.045, 0.18)], hexc("#f0a8a8"))
    # 鈴
    c.ellipse(0.5, 0.64, 0.04, 0.035, hexc("#f2c94c"))


def itamae(c):
    hair = hexc("#2b1d12")
    body(c, WHITE, sash=hexc("#3b6e9e"), bottom=WHITE, sleeve=WHITE)
    head(c, hair, bangs="side", eyes="normal")
    # ねじりはちまき
    c.rect(0.22, 0.22, 0.78, 0.27, hexc("#3b6e9e"))
    for k in range(5):
        c.dot(0.26 + k * 0.11, 0.23, WHITE)
    # おにぎり
    c.poly([(0.72, 0.86), (0.84, 0.64), (0.96, 0.86)], WHITE)
    c.rect(0.76, 0.79, 0.92, 0.86, hexc("#1e2a1e"))


def banto(c):
    hair = hexc("#3b3b3b")
    body(c, hexc("#a1887f"), sash=hexc("#4a3020"), bottom=hexc("#a1887f"))
    c.rect(0.34, 0.7, 0.66, 0.95, hexc("#2f4f8f"))      # 前掛け
    c.dot(0.47, 0.8, WHITE, w=2)
    head(c, hair, bangs="side", eyes="closed")
    c.rect(0.3, 0.37, 0.7, 0.38, hexc("#c9a14a"))       # 丸めがね
    # そろばん
    c.rect(0.72, 0.66, 0.98, 0.78, hexc("#5d4037"))
    for k in range(5):
        c.dot(0.74 + k * 0.05, 0.71, hexc("#e8c46a"))


def kyoyu(c):
    # 2人組（おそろいの羽織）
    for ox, hair in ((-0.14, hexc("#4a3020")), (0.14, hexc("#2b1d12"))):
        sub = Canvas(c.n)
        body(sub, hexc("#4db6ac"), sash=WHITE, bottom=hexc("#26786f"), sleeve=hexc("#4db6ac"))
        head(sub, hair, back="pony" if ox > 0 else None, bangs="straight" if ox < 0 else "side", eyes="normal")
        n = c.n
        shift = int(ox * n)
        order = range(n) if ox < 0 else range(n)
        for y in range(n):
            for x in order:
                v = sub.px[y][x]
                if v and 0 <= x + shift < n:
                    c.px[y][x + shift] = v


def nice_buttoner(c):
    hair = hexc("#3b2a1e")
    body(c, hexc("#2f6fd8"), sash=WHITE, bottom=hexc("#2f6fd8"), sleeve=hexc("#2f6fd8"))
    c.rect(0.34, 0.62, 0.38, 0.8, WHITE)     # 法被のえり
    c.rect(0.62, 0.62, 0.66, 0.8, WHITE)
    head(c, hair, bangs="spiky", eyes="normal", mouth="open")
    # 呼び鈴（ボタン）
    c.ellipse(0.84, 0.74, 0.1, 0.06, hexc("#c9a14a"))
    c.ellipse(0.84, 0.7, 0.06, 0.05, hexc("#e53935"))


def sheriff(c):
    hair = hexc("#2b1d12")
    body(c, hexc("#3a4a6b"), hakama=hexc("#26304a"), sash=hexc("#c9a14a"), sleeve=hexc("#3a4a6b"))
    head(c, hair, back="pony", bangs="side", eyes="sharp")
    # 陣笠
    c.poly([(0.16, 0.2), (0.5, 0.04), (0.84, 0.2), (0.5, 0.17)], hexc("#2a2a2a"))
    c.dot(0.48, 0.1, hexc("#c9a14a"), w=2)
    # 銃（火縄銃っぽい短銃）
    c.rect(0.7, 0.66, 0.98, 0.7, hexc("#3a3a3a"))
    c.rect(0.72, 0.68, 0.8, 0.8, hexc("#6b4a2e"))
    c.dot(0.96, 0.64, hexc("#3a3a3a"))


def medium(c):
    hair = hexc("#e8e8f0")
    body(c, WHITE, hakama=hexc("#c9cbe0"), sleeve=WHITE)
    head(c, hair, back="long", bangs="straight", eyes="closed", eye_col=hexc("#6b6b8a"))
    # 天冠（三角の布）
    c.poly([(0.42, 0.13), (0.5, 0.06), (0.58, 0.13)], WHITE)
    # 数珠
    for k in range(7):
        a = k / 7 * math.pi
        c.dot(0.5 + math.cos(a) * 0.12, 0.62 + math.sin(a) * 0.08, hexc("#5d4037"))
    # 人魂
    c.ellipse(0.86, 0.5, 0.05, 0.06, hexc("#c9e8ff"))


def teru(c):
    # てるてる坊主のずきん
    c.poly([(0.2, 0.55), (0.8, 0.55), (0.88, 0.95), (0.12, 0.95)], WHITE)
    for k in range(5):
        c.line(0.18 + k * 0.16, 0.95, 0.2 + k * 0.16, 0.86, 0.02, hexc("#d8dce6"))
    c.rect(0.3, 0.56, 0.7, 0.6, hexc("#e53935"))
    c.rect(0.32, 0.95, 0.42, 0.985, hexc("#3b2b2b"))
    c.rect(0.58, 0.95, 0.68, 0.985, hexc("#3b2b2b"))
    head(c, WHITE, bangs="hood", eyes="normal", mouth="smile")
    c.ellipse(0.5, 0.36, 0.3, 0.27, WHITE)
    c.ellipse(0.5, 0.42, 0.24, 0.18, SKIN)
    head_face_only(c)


def head_face_only(c):
    n = c.n
    ey = HEAD[1] + 0.08
    eh = max(2, round(n / 12))
    for ex in (0.41, 0.59):
        for dy in range(1 if BLINK[0] else eh):
            c._put(int(ex * n), int(ey * n) - eh // 2 + dy + (eh - 1 if BLINK[0] else 0), EYE)
    c.dot(0.48, ey + 0.09, dark(SKIN_S, 0.8), w=2)
    for ex in (0.33, 0.67):
        c.dot(ex - 0.03, ey + 0.06, BLUSH, w=2)


def fox(c):
    hair = hexc("#f2b33d")
    c.ellipse(0.85, 0.75, 0.1, 0.16, hair)                # しっぽ
    c.ellipse(0.88, 0.62, 0.05, 0.05, WHITE)
    body(c, WHITE, hakama=hexc("#ffb300"), sash=hexc("#c8372d"), sleeve=WHITE)
    head(c, hair, back="long", bangs="straight", eyes="sharp", eye_col=hexc("#c8372d"))
    for s in (-1, 1):
        x = 0.5 + s * 0.2
        c.poly([(x - 0.09, 0.2), (x + s * 0.05, 0.02), (x + 0.09, 0.2)], hair)
        c.poly([(x - 0.04, 0.17), (x + s * 0.03, 0.08), (x + 0.04, 0.17)], WHITE)
    # 狐面（頭の横）
    c.ellipse(0.24, 0.22, 0.09, 0.08, WHITE)
    c.dot(0.21, 0.21, hexc("#c8372d"))
    c.dot(0.26, 0.21, hexc("#c8372d"))


ROLES = {
    "wolf": ("人狼", wolf), "madman": ("狂人", madman), "shinobi": ("忍び狼", shinobi), "kairiki": ("怪力狼", kairiki),
    "juro": ("呪狼", juro), "evil_guesser": ("イビルゲッサー", evil_guesser),
    "villager": ("村人", villager), "seer": ("占い師", seer), "knight": ("騎士", knight), "medium": ("霊媒師", medium),
    "detective": ("探偵", detective), "nekomata": ("猫又", nekomata), "itamae": ("板前", itamae), "banto": ("番頭", banto),
    "kyoyu": ("共有者", kyoyu), "nice_guesser": ("ナイスゲッサー", nice_guesser), "nice_buttoner": ("ナイスボタナー", nice_buttoner),
    "sheriff": ("シェリフ", sheriff),
    "teru": ("てるてる", teru), "fox": ("妖狐", fox), "okami": ("女将（神）", okami),
}


def draw(fn, n, blink=False):
    BLINK[0] = blink
    c = Canvas(n)
    fn(c)
    c.shade()
    c.outline()
    BLINK[0] = False
    return c.image()
