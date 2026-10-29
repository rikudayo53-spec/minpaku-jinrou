"""
高精細 Blockbench モデルを作る（シェリフの銃・家具の見本）。

  python3 tools/blockbench/make_hires.py [名前 ...]
    → src/main/resources/assets/minpaku/hires/<名前>.bbmodel
"""
import os
import sys

sys.path.insert(0, os.path.dirname(__file__))
from hires import HiRes, MATS, Mat, shade  # noqa: E402

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
OUT = os.path.join(ROOT, "src/main/resources/assets/minpaku/hires")


# ================================================================ 銃（グロック風。銃口は -Z＝北、にぎりは下）

def _slide_decal(dark):
    def decal(draw, face, x0, y0, w, h):
        if face in ("east", "west"):
            # 後ろのセレーション（縦の溝）：east は u0 が後ろ（+Z）
            n = 7
            for k in range(n):
                u = 3 + k * 4
                xx = x0 + (u if face == "east" else w - 1 - u)
                draw.line([(xx, y0 + 3), (xx, y0 + h - 4)], fill=dark + (255,), width=2)
            if face == "east":
                # 排莢口
                draw.rectangle([x0 + int(w * 0.42), y0 + 2, x0 + int(w * 0.66), y0 + int(h * 0.55)], fill=shade(dark, 0.6) + (255,))
        if face == "up":
            # 上面の排莢口
            draw.rectangle([x0 + 3, y0 + int(h * 0.36), x0 + w - 4, y0 + int(h * 0.6)], fill=shade(dark, 0.5) + (255,))
        if face == "north":
            # 銃口
            cx, cy = x0 + w // 2, y0 + h // 2
            draw.ellipse([cx - 4, cy - 4, cx + 4, cy + 4], fill=(8, 8, 10, 255))
    return decal


def _grip_decal(base):
    def decal(draw, face, x0, y0, w, h):
        if face == "north":
            # 指のかかり（前の3つのくぼみ）
            for k in range(3):
                yy = y0 + int(h * (0.25 + k * 0.22))
                draw.line([(x0 + 1, yy), (x0 + w - 2, yy)], fill=shade(base, 0.6) + (255,), width=2)
        if face in ("east", "west"):
            # 親指のくぼみと枠
            draw.rectangle([x0 + 2, y0 + 2, x0 + w - 3, y0 + h - 3], outline=shade(base, 0.7) + (255,), width=1)
    return decal


def glock(name, slide, frame, grip, D=10):
    m = HiRes(name, density=D)
    m.group("gun", (0, 4, 2))
    sb = MATS[slide].base
    fb = MATS[frame].base
    # スライド（上の箱）と前のテーパー
    m.cube("slide", (-0.85, 5.8, -5.4), (0.85, 7.6, 6.0), slide, group="gun", decal=_slide_decal(shade(sb, 0.55)))
    m.cube("slide_nose", (-0.75, 5.9, -6.0), (0.75, 7.4, -5.4), slide, group="gun", decal=_slide_decal(shade(sb, 0.55)))
    m.cube("barrel_hood", (-0.55, 7.6, -1.6), (0.55, 7.75, 0.6), "steel", group="gun")
    # 照準
    m.cube("front_sight", (-0.18, 7.6, -5.4), (0.18, 8.1, -4.9), "sight", group="gun")
    m.cube("rear_sight_l", (-0.7, 7.6, 5.0), (-0.25, 8.15, 5.7), slide, group="gun")
    m.cube("rear_sight_r", (0.25, 7.6, 5.0), (0.7, 8.15, 5.7), slide, group="gun")
    m.cube("rear_dot", (-0.25, 7.6, 5.3), (0.25, 7.9, 5.7), "sight", group="gun")
    # フレーム（スライドの下・レール）
    m.cube("dust_cover", (-0.8, 4.9, -5.3), (0.8, 5.8, 1.0), frame, group="gun")
    m.cube("rail", (-0.85, 5.0, -4.8), (0.85, 5.35, -2.2), frame, group="gun")
    m.cube("frame_rear", (-0.82, 4.9, 1.0), (0.82, 5.8, 5.6), frame, group="gun")
    # 用心金と引き金
    m.cube("guard_front", (-0.45, 3.3, -2.1), (0.45, 4.9, -1.6), frame, rot=(-12, 0, 0), pivot=(0, 4.9, -1.85), group="gun")
    m.cube("guard_bottom", (-0.45, 3.0, -1.9), (0.45, 3.4, 1.4), frame, group="gun")
    m.cube("trigger", (-0.18, 3.6, 0.0), (0.18, 4.9, 0.45), "steel", rot=(14, 0, 0), pivot=(0, 4.9, 0.2), group="gun")
    # にぎり（後ろへ少し傾く）＋弾倉の底
    # にぎりの上と本体のすき間をうめる台（傾いたにぎりの上端を包む）
    m.cube("grip_top", (-0.88, 3.6, 0.9), (0.88, 4.95, 5.4), frame, group="gun")
    m.cube("grip", (-0.9, -1.8, 1.2), (0.9, 5.2, 4.8), grip, rot=(-18, 0, 0), pivot=(0, 4.9, 3.0), group="gun",
           decal=_grip_decal(MATS[grip].base))
    m.cube("mag_base", (-0.95, -2.4, 1.0), (0.95, -1.8, 4.9), frame, rot=(-18, 0, 0), pivot=(0, 4.9, 3.0), group="gun")
    m.cube("beavertail", (-0.82, 4.4, 4.8), (0.82, 4.9, 6.0), frame, group="gun")
    # スライドストップ・マガジンキャッチ
    m.cube("slide_stop", (0.82, 5.1, -0.4), (0.95, 5.4, 1.4), "steel", group="gun")
    m.cube("mag_catch", (0.82, 3.9, 1.0), (0.95, 4.5, 1.6), frame, group="gun")
    return m


def glock_black():
    return glock("glock_black", "slide_black", "frame_black", "grip_black")


def glock_tan():
    return glock("glock_tan", "slide_black", "frame_tan", "grip_tan")


# ================================================================ 家具の見本

def kamado():
    m = HiRes("kamado", density=3, mc=True)
    # 漆喰のかまど本体（2口）
    m.cube("body", (-4, 0, 2), (20, 11, 15), "plaster")
    m.cube("base", (-4.4, 0, 1.6), (20.4, 1.4, 15.4), "clay")
    m.cube("top", (-4.4, 10.6, 1.6), (20.4, 12, 15.4), "clay")
    for x in (-1, 11):
        m.cube("mouth", (x, 2, 1.7), (x + 6, 7, 2.1), "charcoal")
        m.cube("fire", (x + 0.6, 2.2, 1.5), (x + 5.4, 6.4, 1.7), "fire")
        m.cube("arch", (x - 0.4, 7, 1.5), (x + 6.4, 7.8, 2.1), "stone_dark")
    # 羽釜
    m.cube("pot", (-2, 12, 4), (7, 17, 13), "iron_black")
    m.cube("brim", (-3, 14, 3), (8, 15, 14), "iron_black")
    m.cube("lid", (-1.5, 17, 4.5), (6.5, 18, 12.5), "wood")
    m.cube("knob", (1.5, 18, 7.5), (3.5, 19.3, 9.5), "wood_dark")
    # 銅の鍋
    m.cube("pan", (10, 12, 5), (17, 16, 12), "copper")
    m.cube("pan_lid", (10.5, 16, 5.5), (16.5, 16.8, 11.5), "wood")
    # 薪
    m.cube("log1", (-3, 0, -3), (5, 2, -1), "wood_dark", along="u")
    m.cube("log2", (0, 2, -3), (7, 3.8, -1.2), "wood")
    return m


def andon():
    m = HiRes("andon", density=3, mc=True)
    m.cube("base", (3, 0, 3), (13, 1.5, 13), "wood_dark")
    for (x, z) in ((3, 3), (12, 3), (3, 12), (12, 12)):
        m.cube("post", (x, 1.5, z), (x + 1, 20, z + 1), "wood_dark")
    m.cube("top", (3, 19.5, 3), (13, 20.5, 13), "wood_dark")
    m.cube("shelf", (3, 9, 3), (13, 9.6, 13), "wood_dark")
    m.cube("dish", (6.5, 9.6, 6.5), (9.5, 10.3, 9.5), "iron_black")
    m.cube("flame", (7.3, 10.3, 7.3), (8.7, 13.5, 8.7), "fire")
    m.cube("paper", (4, 10, 3.8), (12, 19.5, 12.2), "paper_lit")
    m.cube("handle", (7.5, 20.5, 3.5), (8.5, 24, 4.5), "wood_dark")
    m.cube("handle2", (7.5, 20.5, 11.5), (8.5, 24, 12.5), "wood_dark")
    m.cube("handle_top", (7.5, 23.5, 3.5), (8.5, 24.5, 12.5), "wood_dark")
    return m


def well():
    m = HiRes("well", density=3, mc=True)
    # 井筒（石を積んだ八角）
    for k, (a, b) in enumerate(((0, 0), (12, 0), (0, 12), (12, 12))):
        pass
    m.cube("wall_n", (2, 0, 1), (14, 9, 3), "stone")
    m.cube("wall_s", (2, 0, 13), (14, 9, 15), "moss_stone")
    m.cube("wall_w", (1, 0, 2), (3, 9, 14), "moss_stone")
    m.cube("wall_e", (13, 0, 2), (15, 9, 14), "stone")
    m.cube("rim_n", (1, 9, 1), (15, 10, 3), "stone_dark")
    m.cube("rim_s", (1, 9, 13), (15, 10, 15), "stone_dark")
    m.cube("rim_w", (1, 9, 3), (3, 10, 13), "stone_dark")
    m.cube("rim_e", (13, 9, 3), (15, 10, 13), "stone_dark")
    m.cube("water", (3, 3, 3), (13, 4, 13), "water")
    # 柱と屋根
    m.cube("post_l", (-2, 0, 7), (0, 26, 9), "wood_dark")
    m.cube("post_r", (16, 0, 7), (18, 26, 9), "wood_dark")
    m.cube("beam", (-3, 24, 6), (19, 25.5, 10), "wood")
    m.cube("roof_n", (-4, 26, 2), (20, 27.5, 8.2), "tile", rot=(-25, 0, 0), pivot=(8, 27, 8))
    m.cube("roof_s", (-4, 26, 7.8), (20, 27.5, 14), "tile", rot=(25, 0, 0), pivot=(8, 27, 8))
    m.cube("pulley", (7, 21, 7), (9, 24, 9), "iron_black")
    m.cube("rope", (7.8, 10, 7.8), (8.2, 21, 8.2), "rope")
    m.cube("bucket", (6, 6, 6), (10, 10, 10), "wood")
    m.cube("tub", (18, 0, -6), (26, 4, 2), "wood")
    return m


ITEMS = {"glock_black": glock_black}
# 家具28種類（furniture_hires.py）
from furniture_hires import ALL as _FURN  # noqa: E402
for _fn in _FURN.values():
    ITEMS[_fn.__name__] = _fn


def main():
    os.makedirs(OUT, exist_ok=True)
    names = sys.argv[1:] or list(ITEMS)
    for n in names:
        m = ITEMS[n]()
        m.save(os.path.join(OUT, n + ".bbmodel"))
        print("書き出し:", n, f"テクスチャ {m.size}px")


if __name__ == "__main__":
    main()
