"""
池の鯉（鹿威しの場面）の高精細モデル。紅白（kohaku）と黄金（gold）、それぞれ頭・胴・尾の3つに分ける
（泳ぐとき体をくねらせるため。Java の EjectScene がつなぎ目で少しずつ曲げる）。
座標はピクセル。頭が -Z（北）、底が y=0、左右の中心が x=0（Blender の鯉と同じ置き方）。
"""
import math
import random

from hires import HiRes, MATS, Mat

MATS.update({
    "koi_white": Mat((246, 243, 234), "plain", noise=4),
    "koi_belly": Mat((236, 230, 220), "plain", noise=3),
    "koi_gold": Mat((246, 186, 58), "metal", noise=6),
    "koi_gold_d": Mat((214, 146, 34), "metal", noise=6),
    "koi_eye": Mat((18, 16, 18), "plain", noise=2),
    "koi_lip": Mat((236, 200, 176), "plain", noise=3),
    "koi_fin_w": Mat((250, 244, 236), "paper", noise=3, alpha=200),
    "koi_fin_g": Mat((252, 214, 120), "paper", noise=3, alpha=210),
})

RED = (214, 58, 26)


def _scales(gold, red_spots=None, seed=1):
    """うろこ（細かい弧、うすく）と、紅白の赤い模様（背中側だけ。いびつな大きいまだら）"""
    def decal(draw, face, x0, y0, w, h):
        if face == "down":
            return
        rng = random.Random(seed * 7 + len(face))
        if red_spots:
            for (cu, cv, ru, rv) in red_spots:
                if face != "up":
                    cv, rv = 0.18, 0.32           # 横の面では上の方（背中寄り）
                for k in range(9):
                    du = rng.uniform(-0.12, 0.12)
                    dv = rng.uniform(-0.1, 0.1)
                    su = ru * rng.uniform(0.55, 0.95)
                    sv = rv * rng.uniform(0.55, 0.95)
                    shade = rng.randint(-14, 8)
                    col = (RED[0] + shade, RED[1] + shade // 2, RED[2], 255)
                    draw.ellipse([x0 + (cu + du - su) * w, y0 + (cv + dv - sv) * h,
                                  x0 + (cu + du + su) * w, y0 + (cv + dv + sv) * h], fill=col)
        # うろこ：3テクセルごとの小さな弧（いまの色を少しだけ暗く／明るく）
        px = draw._image.load()
        step = 3
        for j in range(1, h - 1, step):
            off = ((j // step) % 2) * 1
            for i in range(0, w - 2, step):
                for (dx, dy, k) in ((0, 1, 0.9), (1, 2, 0.88), (2, 1, 0.9), (1, 0, 1.05)):
                    xx, yy = x0 + i + off + dx, y0 + j + dy
                    if x0 <= xx < x0 + w and y0 <= yy < y0 + h:
                        r, g, b, a = px[xx, yy]
                        px[xx, yy] = (min(255, int(r * k)), min(255, int(g * k)), min(255, int(b * k)), a)
    return decal


def _body(m, gold, z0, z1, w, ylo, yhi, spots, group=None, seed=1):
    """胴の一切れ。角を落とすため、少し細い箱を上下に重ねる"""
    mat = "koi_gold" if gold else "koi_white"
    dec = _scales(gold, spots, seed)
    m.cube("body", (-w / 2, ylo + 0.25, z0), (w / 2, yhi - 0.25, z1), mat, group=group, decal=dec)
    m.cube("back", (-w / 2 + 0.25, yhi - 0.25, z0), (w / 2 - 0.25, yhi, z1), mat, group=group, decal=dec)
    m.cube("belly", (-w / 2 + 0.25, ylo, z0), (w / 2 - 0.25, ylo + 0.25, z1), "koi_gold_d" if gold else "koi_belly", group=group)


def koi(pattern, part):
    gold = pattern == "gold"
    m = HiRes(f"koi_{pattern}_{part}", density=12, real=True, seed=11 if gold else 5)
    fin = "koi_fin_g" if gold else "koi_fin_w"
    if part == "front":
        # 頭（-Z）から肩まで
        _body(m, gold, -5.3, -4.5, 1.0, 1.0, 2.2, None, seed=1)
        _body(m, gold, -4.6, -3.2, 1.6, 0.7, 2.6, [(0.5, 0.4, 0.5, 0.45)] if not gold else None, seed=2)
        _body(m, gold, -3.3, -1.4, 2.0, 0.5, 2.85, None, seed=3)
        _body(m, gold, -1.5, 1.0, 2.05, 0.45, 2.95, [(0.45, 0.5, 0.55, 0.5)] if not gold else None, seed=4)
        m.cube("lip", (-0.45, 1.15, -5.55), (0.45, 1.75, -5.25), "koi_lip")
        for sx in (-1, 1):
            # 目（少し出っぱった黒い玉）
            m.cube("eye", (sx * 0.62 - 0.2, 1.85, -4.35), (sx * 0.62 + 0.2, 2.25, -3.95), "koi_eye")
            # ひげ
            m.cube("barbel", (sx * 0.35 - 0.05, 1.2, -5.9), (sx * 0.35 + 0.05, 1.3, -5.2), "koi_lip",
                   rot=[0, sx * 25, 0], pivot=[sx * 0.35, 1.25, -5.25])
            # 胸びれ（半透明、外へ広げる）
            m.cube("glass_pec", (sx * 0.9 - (0.0 if sx > 0 else 1.5), 0.7, -2.8), (sx * 0.9 + (1.5 if sx > 0 else 0.0), 0.8, -1.9),
                   fin, rot=[0, sx * -30, sx * -12], pivot=[sx * 0.9, 0.76, -2.3])
        m.cube("glass_dorsal", (-0.06, 2.95, -1.0), (0.06, 3.7, 1.0), fin)
    elif part == "mid":
        _body(m, gold, -1.4, 0.4, 1.8, 0.55, 2.8, [(0.5, 0.45, 0.55, 0.5)] if not gold else None, seed=5)
        _body(m, gold, 0.3, 1.7, 1.4, 0.75, 2.55, None, seed=6)
        m.cube("glass_dorsal", (-0.06, 2.8, -0.6), (0.06, 3.5, 1.4), fin)
        for sx in (-1, 1):
            m.cube("glass_pelvic", (sx * 0.6 - (0.0 if sx > 0 else 1.2), 0.55, 0.2), (sx * 0.6 + (1.2 if sx > 0 else 0.0), 0.65, 1.1),
                   fin, rot=[0, sx * -25, sx * -10], pivot=[sx * 0.6, 0.6, 0.6])
    else:
        _body(m, gold, 0.9, 2.2, 1.0, 1.0, 2.25, [(0.4, 0.5, 0.45, 0.45)] if not gold else None, seed=7)
        _body(m, gold, 2.1, 3.0, 0.65, 1.2, 2.05, None, seed=8)
        # 尾びれ：上と下に開いた2枚
        m.cube("glass_tail", (-0.05, 0.3, 2.8), (0.05, 3.0, 5.4), fin, decal=_tail_shape)
    return m


def _tail_shape(draw, face, x0, y0, w, h):
    """尾びれの形：うしろに V の切れこみ、つけ根は細く。透明で切りぬく"""
    if face not in ("east", "west"):
        return
    clear = (0, 0, 0, 0)
    tip = 0.0 if face == "east" else 1.0       # east は u=0 が尾の先、west は u=1 が尾の先
    root = 1.0 - tip

    def P(u, v):
        return (x0 + u * w, y0 + v * h)
    draw.polygon([P(tip, 0.22), P(abs(tip - 0.42), 0.5), P(tip, 0.78)], fill=clear)
    draw.polygon([P(root, 0.0), P(abs(root - 0.6), 0.0), P(root, 0.36)], fill=clear)
    draw.polygon([P(root, 1.0), P(abs(root - 0.6), 1.0), P(root, 0.64)], fill=clear)
    # すじ
    for k in range(5):
        v = 0.15 + k * 0.17
        draw.line([P(root, 0.5), P(tip, v)], fill=(200, 190, 175, 120))


def preview(pattern):
    """確認用：3つをつなげた1匹"""
    m = HiRes(f"koi_{pattern}_preview", density=12, real=True)
    for part in ("front", "mid", "tail"):
        p = koi(pattern, part)
        m.cubes.extend(p.cubes)
    return m


ALL = {}
for _p in ("kohaku", "gold"):
    for _part in ("front", "mid", "tail"):
        ALL[f"koi_{_p}_{_part}"] = (lambda p=_p, q=_part: koi(p, q))
