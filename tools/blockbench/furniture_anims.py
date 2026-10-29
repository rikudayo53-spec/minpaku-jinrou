"""
家具の GeckoLib アニメーション（.animation.json）。Blockbench（GeckoLib プラグイン）で開いて直せる。

アニメーションの名前（Java の FurnitureGeo が使う）:
  idle … いつも流れているゆれ（炎のゆらぎ、金魚が泳ぐ など）。ループ
  work … タスク中（ブロックを見ている／作業中）のあいだループ
  use  … 手を動かした瞬間（クリック・呼び鈴を鳴らした など）に1回

タスクの「どこまで進んだか」で形が変わる部分（布団の広がり、野菜の数 など）は、
Java（FurnitureGeo）が進み具合に合わせて骨を動かす。ここではそれ以外の動きを書く。
"""
import math

from geo_anim import Anims


def flicker(clip, bone, amp=0.15, period=0.35, axis_y_only=True):
    """炎のゆらぎ（大きさを少しずつ変える）"""
    n = 6
    frames = []
    for k in range(n + 1):
        t = clip.length * k / n
        s = 1 + amp * math.sin(2 * math.pi * t / period) * (0.6 + 0.4 * math.sin(k * 1.7))
        frames.append((t, (1, s, 1) if axis_y_only else (s, s, s), "easeInOutSine" if k else "linear"))
    clip.scale(bone, frames)


def bell(a):
    use = a.add("use", 2.6)
    use.damped("bell", 0, 24, 1.3, 1.7, steps=20)
    use.rot("striker", [(0, (0, 0, 0)), (0.12, (-22, 0, 0), "easeOutQuad"), (0.7, (0, 0, 0), "easeOutBack")])


def vent(a):
    use = a.add("use", 2.0)
    use.rot("lid", [(0, (0, 0, 0)), (0.35, (-78, 0, 0), "easeOutBack"), (1.1, (-75, 0, 0)),
                    (1.6, (0, 0, 0), "easeInQuad"), (1.75, (-6, 0, 0), "easeOutQuad"), (2.0, (0, 0, 0), "easeInQuad")])


def boiler(a):
    w = a.add("work", 0.4, loop=True)
    w.pos("body", [(0, (0, 0, 0)), (0.1, (0.25, 0, -0.15), "easeInOutSine"), (0.2, (-0.2, 0.05, 0.2), "easeInOutSine"),
                   (0.3, (0.15, 0, -0.2), "easeInOutSine"), (0.4, (0, 0, 0), "easeInOutSine")])
    flicker(a.add("idle", 1.4, loop=True), "fire", 0.18, 0.47)


def hokora(a):
    i = a.add("idle", 2.4, loop=True)
    i.scale("evil", [(0, 1.0), (1.2, 1.12, "easeInOutSine"), (2.4, 1.0, "easeInOutSine")])


def ledger(a):
    w = a.add("work", 1.1, loop=True)
    w.rot("flip", [(0, (0, 0, 0)), (0.2, (0, 0, 10), "easeInSine"), (0.8, (0, 0, 175), "easeInOutCubic"),
                   (1.1, (0, 0, 180), "easeOutSine")])


def shoji(a):
    w = a.add("work", 1.2, loop=True)
    w.wobble("screen", 0, 0.8, 0.6)


def kamado(a):
    w = a.add("work", 0.7, loop=True)
    w.pos("lid", [(0, (0, 0, 0)), (0.18, (0, 1.3, 0), "easeOutQuad"), (0.35, (0, 0, 0), "easeInQuad"),
                  (0.5, (0, 0.5, 0), "easeOutQuad"), (0.7, (0, 0, 0), "easeInQuad")])
    w.rot("lid", [(0, (0, 0, 0)), (0.18, (0, 0, 4), "easeOutQuad"), (0.35, (0, 0, -2), "easeInOutSine"),
                  (0.7, (0, 0, 0), "easeInOutSine")])
    flicker(a.add("idle", 1.4, loop=True), "fire", 0.2, 0.35)


def bath(a):
    flicker(a.add("idle", 1.4, loop=True), "fire", 0.2, 0.4)
    w = a.add("work", 2.0, loop=True)
    w.pos("water", [(0, (0, 0, 0)), (1.0, (0, 0.25, 0), "easeInOutSine"), (2.0, (0, 0, 0), "easeInOutSine")])


def chopping(a):
    use = a.add("use", 0.75)
    use.rot("axe", [(0, (0, 0, 0)), (0.08, (0, 0, -18), "easeOutQuad"), (0.2, (0, 0, 30), "easeInQuad"),
                    (0.75, (0, 0, 0), "easeOutBounce")])


def dust(a):
    w = a.add("work", 0.8, loop=True)
    w.rot("broom", [(0, (0, 0, 0)), (0.2, (0, 28, 6), "easeInOutSine"), (0.4, (0, 0, 0), "easeInOutSine"),
                    (0.6, (0, -28, 6), "easeInOutSine"), (0.8, (0, 0, 0), "easeInOutSine")])


def laundry(a):
    i = a.add("idle", 4.0, loop=True)
    for k in range(4):
        i.wobble(f"cloth{k}", 0, 7, 2.0, phase=k * 1.3)


def vase(a):
    i = a.add("idle", 5.0, loop=True)
    i.wobble("flowers", 2, 2.5, 5.0)
    use = a.add("use", 0.9)
    use.rot("can", [(0, (0, 0, 0)), (0.2, (0, 0, 22), "easeOutQuad"), (0.55, (0, 0, 18)), (0.9, (0, 0, 0), "easeInOutSine")])


def andon(a):
    flicker(a.add("idle", 1.2, loop=True), "flame", 0.2, 0.3, axis_y_only=False)


def irori(a):
    flicker(a.add("idle", 1.4, loop=True), "fire", 0.25, 0.35)


def bonsai(a):
    use = a.add("use", 0.45)
    use.rot("blade_a", [(0, (0, 0, 0)), (0.1, (0, 0, 26), "easeOutQuad"), (0.45, (0, 0, 0), "easeOutBack")])
    use.rot("blade_b", [(0, (0, 0, 0)), (0.1, (0, 0, -26), "easeOutQuad"), (0.45, (0, 0, 0), "easeOutBack")])


def tea(a):
    w = a.add("work", 0.32, loop=True)
    w.pos("whisk", [(0, (0, 0, 0)), (0.08, (2.2, 0, 0), "easeInOutSine"), (0.16, (0, 0, 0.4), "easeInOutSine"),
                    (0.24, (-2.2, 0, 0), "easeInOutSine"), (0.32, (0, 0, 0), "easeInOutSine")])
    w.rot("whisk", [(0, (0, 0, 0)), (0.08, (0, 0, -10), "easeInOutSine"), (0.24, (0, 0, 10), "easeInOutSine"),
                    (0.32, (0, 0, 0), "easeInOutSine")])


def well(a):
    use = a.add("use", 1.1)
    use.pos("bucket", [(0, (0, 0, 0)), (0.45, (0, 12, 0), "easeOutCubic"), (0.7, (0, 11.5, 0)), (1.1, (0, 0, 0), "easeInQuad")])
    use.rot("bucket", [(0, (0, 0, 0)), (0.45, (0, 0, 8), "easeOutSine"), (0.8, (0, 0, -5), "easeInOutSine"),
                       (1.1, (0, 0, 0), "easeInOutSine")])


# ---------------------------------------------------------------- 新しいタスク

def mailbox(a):
    use = a.add("use", 0.7)
    use.rot("lid", [(0, (0, 0, 0)), (0.12, (-50, 0, 0), "easeOutQuad"), (0.7, (0, 0, 0), "easeOutBounce")])


def goldfish(a):
    i = a.add("idle", 6.0, loop=True)
    i.rot("fish0", [(0, (0, 0, 0)), (1.5, (0, 90, 0)), (3.0, (0, 180, 0)), (4.5, (0, 270, 0)), (6.0, (0, 360, 0))])
    i.rot("fish1", [(0, (0, 0, 0)), (1.5, (0, -120, 0)), (3.0, (0, -240, 0)), (4.5, (0, -360, 0)), (6.0, (0, -480, 0))])
    use = a.add("use", 1.0)
    use.pos("food", [(0, (0, 0, 0)), (0.25, (0, -2.5, 0), "easeInQuad"), (1.0, (0, -6.2, 0), "easeOutSine")])


def furin(a):
    i = a.add("idle", 3.0, loop=True)
    i.rot("hung", [(0, (0, 0, 0)), (0.6, (0, 0, 6), "easeInOutSine"), (1.3, (0, 0, -4), "easeInOutSine"),
                   (2.1, (0, 0, 3), "easeInOutSine"), (3.0, (0, 0, 0), "easeInOutSine")])
    use = a.add("use", 0.6)
    use.pos("boxed", [(0, (0, 0, 0)), (0.15, (0, 1.2, 0), "easeOutQuad"), (0.6, (0, 0, 0), "easeOutBounce")])


def mochi(a):
    use = a.add("use", 0.7)
    use.rot("kine", [(0, (0, 0, 0)), (0.07, (-8, 0, 0), "easeOutQuad"), (0.16, (40, 0, 0), "easeInQuad"),
                     (0.7, (0, 0, 0), "easeOutQuad")])
    use.scale("mochi", [(0, (1, 1, 1)), (0.16, (1, 1, 1)), (0.2, (1.12, 0.78, 1.12), "easeOutQuad"),
                        (0.7, (1, 1, 1), "easeOutElastic")])


def zen(a):
    w = a.add("work", 1.6, loop=True)
    w.pos("rake", [(0, (0, 0, 0)), (0.4, (-5, 0, 0.5), "easeInOutSine"), (0.8, (0, 0, 1), "easeInOutSine"),
                   (1.2, (5, 0, 0.5), "easeInOutSine"), (1.6, (0, 0, 0), "easeInOutSine")])


def washtub(a):
    w = a.add("work", 0.5, loop=True)
    w.pos("cloth", [(0, (0, 0, 0)), (0.25, (0, 1.2, -0.5), "easeInOutSine"), (0.5, (0, 0, 0), "easeInOutSine")])
    i = a.add("idle", 2.0, loop=True)
    i.pos("bubbles", [(0, (0, 0, 0)), (1.0, (0, 0.2, 0), "easeInOutSine"), (2.0, (0, 0, 0), "easeInOutSine")])


# ---------------------------------------------------------------- 飾り

def tourou(a):
    flicker(a.add("idle", 1.3, loop=True), "flame", 0.22, 0.33, axis_y_only=False)


def maneki(a):
    i = a.add("idle", 2.2, loop=True)
    i.rot("paw", [(0, (0, 0, 0)), (0.5, (-30, 0, 0), "easeInOutSine"), (1.0, (0, 0, 0), "easeInOutSine"),
                  (1.5, (-30, 0, 0), "easeInOutSine"), (2.2, (0, 0, 0), "easeInOutSine")])


def hibachi(a):
    i = a.add("idle", 2.0, loop=True)
    i.scale("ember", [(0, (1, 1, 1)), (1.0, (1, 1.6, 1), "easeInOutSine"), (2.0, (1, 1, 1), "easeInOutSine")])


ANIMS = {
    "call_bell": bell, "floor_hatch": vent, "boiler": boiler, "hokora": hokora, "ledger": ledger,
    "shoji_screen": shoji, "kamado": kamado, "bath_stove": bath, "chopping_block": chopping, "dust_pile": dust,
    "laundry_pole": laundry, "flower_vase": vase, "andon": andon, "irori": irori, "bonsai": bonsai, "tea_set": tea,
    "well": well, "mailbox": mailbox, "goldfish_bowl": goldfish, "wind_chime": furin, "mochi_mortar": mochi,
    "zen_garden": zen, "washtub": washtub, "tourou": tourou, "maneki_neko": maneki, "hibachi": hibachi,
}


def build(name):
    a = Anims()
    fn = ANIMS.get(name)
    if fn:
        fn(a)
    return a
