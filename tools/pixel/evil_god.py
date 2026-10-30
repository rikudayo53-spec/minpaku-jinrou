"""
悪神（あくがみ）の擬人化ミニキャラのドット絵。悪神が降臨したときに画面に出る。
  python3 tools/pixel/evil_god.py  → textures/gui/role/evil_god.png（左＝ふつう、右＝まばたき。各 48×48）
"""
import os
import sys

from PIL import Image

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import chibi  # noqa: E402
from chibi import Canvas, body, dark, head, hexc, light, BLINK, WHITE, SKIN  # noqa: E402

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
OUT = os.path.join(ROOT, "src/main/resources/assets/minpaku/textures/gui/role/evil_god.png")


def evil_god(c):
    hair = hexc("#4a1a66")
    aura = hexc("#b050f0")
    # 背中からゆらめく紫の鬼火（左右に小さく）
    for (x, y) in ((0.1, 0.62), (0.9, 0.6), (0.12, 0.36), (0.88, 0.34)):
        c.ellipse(x, y, 0.05, 0.08, dark(aura, 0.6))
        c.ellipse(x, y + 0.02, 0.025, 0.04, light(aura, 0.5))
        c.poly([(x - 0.035, y - 0.04), (x, y - 0.14), (x + 0.035, y - 0.04)], dark(aura, 0.6))
    # 黒と紫の着物、赤い帯
    body(c, hexc("#2a1838"), sash=hexc("#c0202a"), hakama=hexc("#3b1a4a"), sleeve=hexc("#5a2a7a"))
    # 長い髪・鬼の角・赤い目・牙
    head(c, hair, back="long", bangs="spiky", eyes="sharp", mouth="fang", blush=False, eye_col=hexc("#ff2a2a"))
    for s_ in (-1, 1):
        x = 0.5 + s_ * 0.18
        c.poly([(x - 0.045, 0.15), (x + s_ * 0.07, 0.02), (x + 0.045, 0.14)], hexc("#efe6d4"))
    # 額のやぶれた札（封印が解けた）
    c.poly([(0.45, 0.1), (0.55, 0.1), (0.555, 0.24), (0.52, 0.22), (0.48, 0.26), (0.455, 0.2)], WHITE)
    c.rect(0.49, 0.12, 0.51, 0.2, hexc("#c0202a"))
    # 手の上の黒い炎
    c.ellipse(0.8, 0.76, 0.05, 0.06, dark(aura, 0.6))
    c.ellipse(0.8, 0.77, 0.025, 0.03, light(aura, 0.5))
    c.poly([(0.77, 0.74), (0.8, 0.64), (0.83, 0.74)], dark(aura, 0.6))


def draw(n, blink):
    BLINK[0] = blink
    c = Canvas(n)
    evil_god(c)
    c.shade()
    c.outline(hexc("#120818"))
    BLINK[0] = False
    return c.image()


def main():
    n = 48
    out = Image.new("RGBA", (n * 2, n), (0, 0, 0, 0))
    out.paste(draw(n, False), (0, 0))
    out.paste(draw(n, True), (n, 0))
    out.save(OUT)
    out.resize((n * 2 * 6, n * 6), Image.NEAREST).save(sys.argv[1] if len(sys.argv) > 1 else "/tmp/evil_god_big.png")
    print("書き出し:", OUT)


if __name__ == "__main__":
    main()
