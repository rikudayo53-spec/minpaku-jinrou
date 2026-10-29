"""
民泊人狼：フィニッシャー9種類の Blockbench プロジェクトを作る。

  python3 tools/blockbench/make_finishers.py [id ...]
    → src/main/resources/assets/minpaku/finishers/<id>.bbmodel
  --preview で build/finisher_preview/<id>.png（コマ割り）も出す

できた .bbmodel は Blockbench（Free モデル）で開いて、そのまま手直しできる。
  - killer / victim：犯人と自分（ゲームでは本物のスキン）
  - camera / camera_target：カメラ（camera の大きさ × 50 = 画角）
  - それ以外のグループ：小道具（手のグループの子にすると手にくっついて動く）
  - 効果のキーフレーム：sound「音の名前|高さ」、timeline「hit」「text:文字:ボーン:色:大きさ」
"""
import math
import os
import sys

import numpy as np

sys.path.insert(0, os.path.dirname(__file__))
from bb import HAND_L, HAND_R, Project  # noqa: E402

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
OUT = os.path.join(ROOT, "src/main/resources/assets/minpaku/finishers")

PARTS = {"head": "head", "body": "body", "ra": "right_arm", "la": "left_arm", "rl": "right_leg", "ll": "left_leg"}


def K(p, who, t, at=None, root=None, i="catmullrom", **parts):
    """ある時刻のポーズ。at=位置（ブロック）・root=体全体の回転・head/body/ra/la/rl/ll=部位の回転（度）"""
    if at is not None:
        p.pos(who, t, *at, interp=i)
    if root is not None:
        p.rot(who, t, *root, interp=i)
    for k, v in parts.items():
        p.rot(f"{who}_{PARTS[k]}", t, *v, interp=i)


def cam(p, t, eye, target, fov=50, i="linear"):
    p.pos("camera", t, *eye, interp=i)
    p.pos("camera_target", t, *target, interp=i)
    p.scale("camera", t, fov / 50, interp=i)


def cut(p, t, eye, target, fov=50):
    """カメラの切りかえ（直前の位置をそのまま残して、t でぱっと変える）"""
    for bone in ("camera", "camera_target"):
        for ch in ("position", "scale"):
            keys = p.tracks.get(bone, {}).get(ch, [])
            if keys:
                before = p.sample(bone, ch, t - 0.01, 0)
                p.key(bone, ch, t - 0.01, before, "linear")
    cam(p, t, eye, target, fov)


def prop(p, name, parent, origin):
    return p.group(name, origin, parent)


def base(name, length):
    p = Project(name)
    p.length = length
    p.camera()
    p.actor("killer")
    p.actor("victim")
    return p


def hand_group(p, who, side, name):
    """手の先にくっつく小道具のグループ"""
    hand = HAND_R if side == "r" else HAND_L
    return prop(p, name, f"{who}_{'right' if side == 'r' else 'left'}_arm", hand)


def walk(p, who, t0, t1, step=0.25, amp=35, arm=25, **extra):
    """歩く（足と腕を振る）"""
    n = int(round((t1 - t0) / step))
    for k in range(n + 1):
        t = t0 + k * step
        s = 1 if k % 2 == 0 else -1
        K(p, who, t, rl=(amp * s, 0, 0), ll=(-amp * s, 0, 0), ra=(-arm * s, 0, 0), la=(arm * s, 0, 0), **extra)


def show(p, bone, t, on):
    """小道具を出す・消す（大きさをぱっと変える）"""
    p.scale(bone, t, 1.0 if on else 0.001, interp="step")


def world_pos(p, bone, t):
    v = p.sample(bone, "position", t, 0.0)
    return np.array([-v[0] / 16, v[1] / 16, v[2] / 16])


def align(p, t, mover, group, px, tgroup, tpx, axes=(0, 2), off=(0, 0, 0)):
    """t のとき、mover（いちばん上のグループ）を動かして、group の点 px を tgroup の点 tpx に合わせる"""
    a = p.point(t, group, px)
    b = p.point(t, tgroup, tpx) + np.array(off)
    keys = p.tracks.get(mover, {}).get("position", [])
    interp = next((k[2] for k in keys if abs(k[0] - t) < 1e-4), "catmullrom")
    cur = world_pos(p, mover, t)
    for ax in axes:
        cur[ax] += b[ax] - a[ax]
    p.pos(mover, t, *cur, interp=interp)


def put(p, bone, t, xyz, i="catmullrom"):
    """いちばん上のグループ（ボール・枕など）を世界の位置へ"""
    p.pos(bone, t, *xyz, interp=i)


def at_point(p, t, group, px):
    return [float(v) for v in p.point(t, group, px)]


# ================================================================ 人狼：獣の爪

def wolf():
    p = base("wolf", 3.8)
    # 爪（両手に3本ずつ、光る）
    for side in "rl":
        g = hand_group(p, "killer", side, f"claw_{side}")
        hx = HAND_R[0] if side == "r" else HAND_L[0]
        for k, dx in enumerate((-1.2, 0, 1.2)):
            p.cube(g, f"claw{k}", [hx + dx - 0.35, 7.5, -2.4], [hx + dx + 0.35, 12, -1.7], "glow_white_concrete",
                   rotation=(-18, 0, (dx * 6)), pivot=[hx + dx, 12, -2])
    # 赤く光る目
    eyes = prop(p, "wolf_eyes", "killer_head", [0, 24, 0])
    p.cube(eyes, "eye_r", [1, 28, -4.3], [3, 29, -4.05], "glow_red_concrete")
    p.cube(eyes, "eye_l", [-3, 28, -4.3], [-1, 29, -4.05], "glow_red_concrete")

    # 自分：背を向けて立つ → 気配でふり向く → 押し倒されてあお向け
    K(p, "victim", 0.0, at=(0, 0, 0.6), root=(0, 180, 0), head=(0, 0, 0), ra=(0, 0, 0), la=(0, 0, 0), rl=(0, 0, 0), ll=(0, 0, 0))
    K(p, "victim", 0.7, at=(0, 0, 0.6), root=(0, 180, 0), head=(0, 0, 0))
    K(p, "victim", 0.85, root=(0, 120, 0), head=(-5, 20, 0))
    K(p, "victim", 1.0, at=(0, 0, 0.6), root=(-6, 0, 0), head=(-10, 0, 0), ra=(-40, 0, 20), la=(-40, 0, -20))
    K(p, "victim", 1.18, at=(0, 0, 0.6), root=(-8, 0, 0), head=(-15, 0, 0), ra=(-110, 0, 25), la=(-110, 0, -25),
      rl=(0, 0, 0), ll=(0, 0, 0))
    K(p, "victim", 1.38, at=(0, 0.3, 0.9), root=(-55, 0, 0), head=(-20, 0, 0), ra=(-150, 0, 30), la=(-150, 0, -30),
      rl=(-30, 0, 0), ll=(-10, 0, 0))
    K(p, "victim", 1.55, at=(0, 0.13, 1.05), root=(-90, 0, 0), head=(-10, 0, 0), ra=(-160, 0, 40), la=(-160, 0, -40),
      rl=(-20, 0, 5), ll=(-5, 0, -5))
    for k, t in enumerate((1.75, 2.05, 2.35)):
        s = 1 if k % 2 == 0 else -1
        K(p, "victim", t, head=(-10, 25 * s, 0), ra=(-170, 0, 50), la=(-170, 0, -50), rl=(-35, 0, 5), ll=(-5, 0, -5))
        K(p, "victim", t + 0.12, head=(-5, -10 * s, 0), ra=(-150, 0, 35), la=(-150, 0, -35), rl=(-10, 0, 5), ll=(-25, 0, -5))
    K(p, "victim", 2.8, at=(0, 0.13, 1.05), root=(-90, 0, 0), head=(0, 30, 0), ra=(-175, 0, 70), la=(-175, 0, -70),
      rl=(-5, 0, 8), ll=(-5, 0, -8))
    K(p, "victim", 3.8, at=(0, 0.13, 1.05), root=(-90, 0, 0), head=(0, 40, 0), ra=(-178, 0, 75), la=(-178, 0, -75),
      rl=(-3, 0, 8), ll=(-3, 0, -8))

    # 人狼：奥から四つんばいで忍び寄る → 飛びかかる → のしかかって3回引っかく → 起き上がって遠吠え
    for k in range(5):
        t = k * 0.2
        s = 1 if k % 2 == 0 else -1
        K(p, "killer", t, at=(0, 0.45, -3.4 + k * 0.32), root=(72, 180, 0), head=(-75, 0, 0),
          ra=(-80 + 22 * s, 0, 0), la=(-80 - 22 * s, 0, 0), rl=(-60 - 18 * s, 0, 0), ll=(-60 + 18 * s, 0, 0))
    K(p, "killer", 1.0, at=(0, 0.45, -2.0), root=(72, 180, 0), head=(-75, 0, 0), ra=(-80, 0, 0), la=(-80, 0, 0),
      rl=(-70, 0, 0), ll=(-70, 0, 0))
    # ため
    K(p, "killer", 1.12, at=(0, 0.25, -1.9), root=(70, 180, 0), head=(-70, 0, 0), ra=(-60, 0, 10), la=(-60, 0, -10),
      rl=(-95, 0, 0), ll=(-95, 0, 0))
    # 飛ぶ
    K(p, "killer", 1.3, at=(0, 1.1, -0.9), root=(40, 180, 0), head=(-35, 0, 0), ra=(-165, 0, 20), la=(-165, 0, -20),
      rl=(20, 0, 0), ll=(30, 0, 0))
    # のしかかる（自分の胸の上にまたがる）
    pin = (0, 0.25, 1.75)
    K(p, "killer", 1.55, at=pin, root=(55, 180, 0), head=(-35, 0, 0), ra=(-120, 0, 15), la=(-120, 0, -15),
      rl=(-80, 0, 15), ll=(-80, 0, -15))
    # 引っかく（右・左・両手）
    swings = [(1.75, "ra"), (2.05, "la"), (2.35, "both")]
    for t, arm in swings:
        up, down = (-200, 0, 25), (-60, 0, 0)
        upl, downl = (-200, 0, -25), (-60, 0, 0)
        ra_up = up if arm in ("ra", "both") else (-110, 0, 15)
        la_up = upl if arm in ("la", "both") else (-110, 0, -15)
        ra_dn = down if arm in ("ra", "both") else (-110, 0, 15)
        la_dn = downl if arm in ("la", "both") else (-110, 0, -15)
        K(p, "killer", t - 0.14, root=(40, 180, 0), head=(-25, 0, 0), ra=ra_up, la=la_up, at=(0, 0.3, 1.7))
        K(p, "killer", t, root=(62, 180, 0), head=(-40, 0, 0), ra=ra_dn, la=la_dn, at=(0, 0.22, 1.8))
        K(p, "killer", t + 0.1, root=(58, 180, 0), ra=(-75, 0, 10), la=(-75, 0, -10))
        p.timeline(t, "hit")
        p.sound(t, "minecraft:entity.player.attack.crit", 0.7 + (t - 1.7))
        p.sound(t, "minecraft:entity.player.hurt", 0.8)
    # 起き上がって遠吠え
    K(p, "killer", 2.7, at=(0, 0.0, 1.35), root=(10, 180, 0), head=(-20, 0, 0), ra=(-40, 0, 20), la=(-40, 0, -20),
      rl=(-10, 0, 10), ll=(-10, 0, -10))
    K(p, "killer", 3.0, at=(0, 0.0, 1.3), root=(-8, 180, 0), head=(-55, 0, 0), ra=(-35, 0, 35), la=(-35, 0, -35),
      rl=(0, 0, 8), ll=(0, 0, -8))
    K(p, "killer", 3.8, at=(0, 0.0, 1.3), root=(-10, 180, 0), head=(-60, 0, 0), ra=(-40, 0, 38), la=(-40, 0, -38),
      rl=(0, 0, 8), ll=(0, 0, -8))
    p.timeline(3.0, "text:ワオーン！:killer_head:FFE08A:2.4")
    p.sound(0.1, "minecraft:entity.wolf.growl", 0.6)
    p.sound(0.85, "minecraft:entity.player.breath", 1.4)
    p.sound(1.15, "minecraft:entity.player.attack.sweep", 0.6)
    p.sound(1.5, "minecraft:entity.player.big_fall", 0.8)
    p.sound(3.0, "minecraft:entity.wolf.howl", 0.75)

    # カメラ
    cam(p, 0.0, (1.7, 1.5, 3.8), (-0.2, 0.8, -1.5), 50)
    cam(p, 0.8, (1.4, 1.5, 3.3), (-0.2, 0.7, -1.6), 46)
    cut(p, 0.85, (1.0, 1.75, -1.1), (0, 1.5, 0.6), 44)          # ふり向いた顔
    cam(p, 1.08, (0.85, 1.7, -0.9), (0, 1.5, 0.6), 40)
    cut(p, 1.1, (3.6, 1.1, 0.0), (0, 0.8, 0.0), 52)               # 横から飛びかかり
    cam(p, 1.6, (3.2, 1.0, 0.4), (0, 0.6, 0.7), 50)
    cut(p, 1.62, (2.8, 2.2, 2.8), (0, 0.4, 2.0), 48)              # 斜め上から引っかき
    cam(p, 2.6, (2.3, 1.9, 2.6), (0, 0.5, 2.0), 44)
    cut(p, 2.65, (2.4, 0.45, 3.9), (0, 1.5, 1.3), 55)             # 下から遠吠え
    cam(p, 3.8, (2.0, 0.4, 3.5), (0, 1.8, 1.3), 52)
    return p


# ================================================================ 忍び狼：神隠し（口をふさいで押し入れへ）

def shinobi():
    p = base("shinobi", 3.8)
    # 押し入れ（奥の壁の前。左の戸が最後に閉まる）
    o = p.group("oshiire", [0, 0, 0])
    p.cube(o, "inside", [-18, 0, -80], [18, 37, -79], "black_concrete")
    p.cube(o, "inside_l", [-19, 0, -80], [-18, 37, -58], "black_concrete")
    p.cube(o, "inside_r", [18, 0, -80], [19, 37, -58], "black_concrete")
    p.cube(o, "inside_top", [-19, 36, -80], [19, 37, -58], "black_concrete")
    p.cube(o, "frame_top", [-20, 37, -59], [20, 40, -57], "dark_oak_planks")
    p.cube(o, "frame_l", [-20, 0, -59], [-18, 37, -57], "dark_oak_planks")
    p.cube(o, "frame_r", [18, 0, -59], [20, 37, -57], "dark_oak_planks")
    p.cube(o, "sill", [-20, 0, -59], [20, 1, -57], "dark_oak_planks")
    p.cube(o, "wall_top", [-20, 40, -80], [20, 64, -58], "calcite")
    p.cube(o, "wall_l", [-36, 0, -80], [-20, 64, -58], "calcite")
    p.cube(o, "wall_r", [20, 0, -80], [36, 64, -58], "calcite")
    for side, x0 in (("r", 0.5), ("l", -18)):
        d = p.group(f"oshiire_door_{side}", [0, 0, -58], o)
        z0 = -58.6 if side == "r" else -57.6
        p.cube(d, "paper", [x0, 1, z0 - 0.6], [x0 + 17.5, 36, z0], "white_wool")
        p.cube(d, "frame_top", [x0, 35, z0 - 0.8], [x0 + 17.5, 36.2, z0 + 0.2], "spruce_planks")
        p.cube(d, "frame_bottom", [x0, 1, z0 - 0.8], [x0 + 17.5, 2.2, z0 + 0.2], "spruce_planks")
        p.cube(d, "frame_a", [x0, 1, z0 - 0.8], [x0 + 1, 36, z0 + 0.2], "spruce_planks")
        p.cube(d, "frame_b", [x0 + 16.5, 1, z0 - 0.8], [x0 + 17.5, 36, z0 + 0.2], "spruce_planks")
        hx = x0 + 3 if side == "r" else x0 + 14
        p.cube(d, "hikite", [hx - 1, 17, z0 + 0.1], [hx + 1, 20, z0 + 0.4], "black_concrete")
    # 左の戸：最初は右の戸のうしろ（開いている）→ 3.15 で閉まる
    p.pos("oshiire_door_l", 0.0, 1.15, 0, -0.06, interp="linear")
    p.pos("oshiire_door_l", 2.95, 1.15, 0, -0.06, interp="linear")
    p.pos("oshiire_door_l", 3.15, 0, 0, 0, interp="linear")

    X = -0.55
    # 自分：前を向いて立つ（ときどきあたりを見回す）
    K(p, "victim", 0.0, at=(X, 0, -1.2), root=(0, 180, 0), head=(0, 0, 0), ra=(0, 0, 0), la=(0, 0, 0), rl=(0, 0, 0), ll=(0, 0, 0))
    K(p, "victim", 0.45, head=(0, -25, 0))
    K(p, "victim", 0.85, head=(5, 15, 0), at=(X, 0, -1.2))
    # つかまる
    K(p, "victim", 1.1, at=(X, 0, -1.2), root=(-6, 180, 0), head=(-15, 0, 0), ra=(-30, 0, 15), la=(-30, 0, -15))
    K(p, "victim", 1.3, root=(-14, 180, 0), head=(-25, 0, 0), ra=(-80, 0, 30), la=(-70, 0, -30), rl=(-30, 0, 0))
    # 引きずられる（足をばたばた）
    for k in range(9):
        t = 1.45 + k * 0.17
        sgn = 1 if k % 2 == 0 else -1
        z = -1.2 - min(1.0, k / 8) * 3.05
        K(p, "victim", t, at=(X, 0.05, z), root=(-22, 180, 0), head=(-25, 12 * sgn, 0),
          ra=(-75 + 20 * sgn, 0, 25), la=(-70 - 20 * sgn, 0, -25), rl=(-45 * (sgn > 0), 0, 0), ll=(-45 * (sgn < 0), 0, 0))
    K(p, "victim", 3.8, at=(X, 0.05, -4.25), root=(-22, 180, 0))

    # 忍び狼：音もなく背後にあらわれる → 口をふさぐ → 押し入れへ引きずりこむ
    show(p, "killer", 0.0, False)
    show(p, "killer", 0.5, True)
    K(p, "killer", 0.5, at=(X, 0, -2.0), root=(12, 180, 0), head=(10, 0, 0), ra=(-20, 0, 10), la=(-20, 0, -10), rl=(0, 0, 0), ll=(0, 0, 0))
    K(p, "killer", 0.85, at=(X, 0, -1.95), root=(8, 180, 0), head=(15, 0, 0), ra=(-50, 0, 0), la=(-60, 0, 0))
    K(p, "killer", 1.1, at=(X, 0, -1.78), root=(6, 180, 0), head=(20, 0, 0), ra=(-75, 0, -30), la=(-118, 0, 28))
    K(p, "killer", 1.3, at=(X, 0, -1.78), root=(-4, 180, 0), head=(15, 0, 0), ra=(-75, 0, -30), la=(-118, 0, 28))
    for k in range(9):
        t = 1.45 + k * 0.17
        sgn = 1 if k % 2 == 0 else -1
        z = -1.2 - min(1.0, k / 8) * 3.05 - 0.55
        K(p, "killer", t, at=(X, 0, z), root=(-10, 180, 0), ra=(-75, 0, -30), la=(-118, 0, 28),
          rl=(20 * sgn, 0, 0), ll=(-20 * sgn, 0, 0))
    K(p, "killer", 3.8, at=(X, 0, -4.8), root=(-10, 180, 0), rl=(0, 0, 0), ll=(0, 0, 0))
    # 口をふさぐ手を顔の前に合わせる
    for t in [1.3] + [1.45 + k * 0.17 for k in range(9)]:
        align(p, t, "killer", "killer_left_arm", HAND_L, "victim_head", [0, 27, -3.5], axes=(2,))

    p.timeline(1.3, "hit")
    p.timeline(1.3, "text:んむっ…！:victim_head:FFFFFF:1.8")
    p.group("door_mark", [-9, 30, -58], "oshiire")
    p.timeline(3.15, "text:ピシャッ:door_mark:FFFFFF:2.4")
    p.sound(0.5, "minecraft:entity.enderman.teleport", 0.6)
    p.sound(1.3, "minecraft:entity.player.hurt", 0.6)
    for t in (1.8, 2.3, 2.8):
        p.sound(t, "minecraft:block.wool.step", 0.8)
    p.sound(3.15, "minecraft:block.wooden_door.close", 1.2)

    cam(p, 0.0, (1.9, 1.6, 2.4), (X, 1.2, -1.6), 48)
    cam(p, 1.05, (1.6, 1.6, 1.7), (X, 1.3, -1.6), 44)
    cut(p, 1.1, (1.3, 1.75, -0.6), (X, 1.65, -1.5), 40)        # 口をふさぐ
    cam(p, 1.55, (1.2, 1.7, -0.7), (X, 1.6, -1.8), 40)
    cut(p, 1.6, (1.6, 1.55, 1.2), (X + 0.2, 1.1, -3.4), 50)     # 押し入れへ
    cam(p, 3.0, (1.0, 1.5, 0.6), (X + 0.3, 1.1, -4.2), 46)
    cam(p, 3.8, (0.7, 1.45, 0.2), (0, 1.1, -4.3), 44)
    return p


# ================================================================ 怪力狼：ちゃぶ台返し

def kairiki():
    p = base("kairiki", 3.6)
    # ちゃぶ台（奥のふちを軸に、手前を持ち上げて返す）
    tb = p.group("chabudai", [3, 7, 0])
    p.cube(tb, "top", [-17, 7, -10], [3, 8.5, 10], "spruce_planks")
    p.cube(tb, "rim", [-17.3, 6.4, -10.3], [3.3, 7.2, 10.3], "dark_oak_planks")
    for lx, lz in ((-14, -7), (-14, 6), (0, -7), (0, 6)):
        p.cube(tb, "leg", [lx, 0, lz], [lx + 1.2, 6.4, lz + 1.2], "dark_oak_planks")
    # 湯のみ・急須（台といっしょに飛ぶ）
    for k, (cx, cz) in enumerate(((-11, -4), (-6, 4), (-2, -3))):
        c = p.group(f"cup{k}", [cx, 8.5, cz], tb)
        p.cube(c, "cup", [cx - 1, 8.5, cz - 1], [cx + 1, 11, cz + 1], "white_terracotta")
        p.cube(c, "tea", [cx - 0.8, 10.6, cz - 0.8], [cx + 0.8, 10.9, cz + 0.8], "green_terracotta")
    p.pos("chabudai", 0.0, -0.4, 0, 0)

    # 犯人：-X 側でかがんで台のふちをつかむ
    K(p, "killer", 0.0, at=(-2.0, 0, 0), root=(32, 90, 0), head=(-20, 0, 0), ra=(-55, 0, 0), la=(-55, 0, 0),
      rl=(-20, 0, 0), ll=(-20, 0, 0))
    K(p, "killer", 0.35, at=(-2.0, 0, 0), root=(38, 90, 0), head=(-25, 0, 0), ra=(-50, 0, 0), la=(-50, 0, 0),
      rl=(-28, 0, 0), ll=(-28, 0, 0))
    # 返す！
    K(p, "killer", 0.6, at=(-1.85, 0, 0), root=(0, 90, 0), head=(-20, 0, 0), ra=(-150, 0, 10), la=(-150, 0, -10),
      rl=(0, 0, 0), ll=(-10, 0, 0))
    K(p, "killer", 0.85, at=(-1.6, 0, 0), root=(-8, 90, 0), head=(-15, 0, 0), ra=(-175, 0, 15), la=(-175, 0, -15),
      rl=(15, 0, 0), ll=(-25, 0, 0))
    K(p, "killer", 1.2, at=(-1.2, 0, 0), root=(20, 90, 0), head=(-10, 0, 0), ra=(-60, 0, 0), la=(-60, 0, 0),
      rl=(-25, 0, 0), ll=(20, 0, 0))
    K(p, "killer", 1.7, at=(-1.2, 0, 0), root=(5, 90, 0), head=(0, 0, 0), ra=(-20, 0, 10), la=(-20, 0, -10),
      rl=(0, 0, 0), ll=(0, 0, 0))
    # ポーズ（力こぶ）
    K(p, "killer", 2.2, at=(-1.2, 0, 0), root=(-3, 90, 0), head=(-10, 0, 0), ra=(-15, 0, 120), la=(-15, 0, -120))
    K(p, "killer", 2.45, ra=(-20, 0, 140), la=(-20, 0, -140), head=(-20, 0, 0))
    K(p, "killer", 3.6, at=(-1.2, 0, 0), root=(-3, 90, 0), ra=(-20, 0, 142), la=(-20, 0, -142), head=(-20, 0, 0))

    # 台の動き
    def tbk(t, x, y, kz, i="catmullrom"):
        p.pos("chabudai", t, x, y, 0, interp=i)
        p.rot("chabudai", t, 0, 0, kz, interp=i)
    tbk(0.0, -0.4, 0, 0)
    tbk(0.35, -0.4, 0, 0)
    tbk(0.6, -0.35, 0.35, -95)
    tbk(0.85, 0.3, 1.5, -150)
    tbk(1.08, 1.25, 1.55, -195)
    tbk(1.2, 1.7, 0.75, -200)
    tbk(1.35, 1.95, 0.12, -180)
    tbk(3.6, 1.95, 0.12, -180)
    # 湯のみが散らばる
    for k, (dx, dy, dz) in enumerate(((0.9, 1.4, -0.8), (1.6, 1.8, 0.9), (2.2, 1.0, -0.4))):
        b = f"cup{k}"
        p.pos(b, 0.6, 0, 0, 0)
        p.pos(b, 0.95, dx * 0.4, dy, dz * 0.6)
        p.pos(b, 1.35, dx, dy * 0.5, dz)
        p.rot(b, 0.6, 0, 0, 0)
        p.rot(b, 1.35, 300 * (1 if k % 2 else -1), 120, 200)

    # 自分：台がふってきて下じきに
    K(p, "victim", 0.0, at=(1.75, 0, 0), root=(0, -90, 0), head=(0, 0, 0), ra=(0, 0, 0), la=(0, 0, 0), rl=(0, 0, 0), ll=(0, 0, 0))
    K(p, "victim", 0.6, at=(1.75, 0, 0), root=(-5, -90, 0), head=(-10, 0, 0), ra=(-30, 0, 15), la=(-30, 0, -15))
    K(p, "victim", 0.95, at=(1.75, 0, 0), root=(-8, -90, 0), head=(-30, 0, 0), ra=(-150, 0, 20), la=(-150, 0, -20))
    K(p, "victim", 1.12, at=(1.75, 0, 0), root=(-10, -90, 0), head=(-35, 0, 0), ra=(-165, 0, 30), la=(-165, 0, -30),
      rl=(0, 0, 0), ll=(0, 0, 0))
    K(p, "victim", 1.3, at=(1.8, 0.08, 0), root=(-80, -90, 0), head=(0, 0, 0), ra=(-160, 0, 60), la=(-160, 0, -60),
      rl=(-20, 0, 0), ll=(-5, 0, 0))
    K(p, "victim", 1.4, at=(1.8, 0.13, 0), root=(-90, -90, 0), ra=(-178, 0, 85), la=(-178, 0, -85), rl=(-3, 0, 8), ll=(-3, 0, -8))
    K(p, "victim", 3.6, at=(1.8, 0.13, 0), root=(-90, -90, 0), ra=(-178, 0, 88), la=(-178, 0, -88))
    for k in range(4):
        t = 2.4 + k * 0.18
        K(p, "victim", t, rl=(-3 - 10 * (k % 2), 0, 8), ll=(-3 - 10 * ((k + 1) % 2), 0, -8))

    p.timeline(0.45, "text:ふんっ！！:killer_head:FFE08A:2.4")
    p.timeline(1.2, "hit")
    p.timeline(1.25, "text:ドンガラガッシャーン:screen:FFFFFF:2.4")
    p.sound(0.3, "minecraft:entity.iron_golem.attack", 0.6)
    p.sound(0.6, "minecraft:entity.player.attack.sweep", 0.5)
    p.sound(0.9, "minecraft:block.decorated_pot.break", 1.2)
    p.sound(1.0, "minecraft:block.decorated_pot.break", 1.4)
    p.sound(1.2, "minecraft:block.anvil.land", 0.9)
    p.sound(1.2, "minecraft:entity.player.hurt", 0.8)
    p.sound(1.35, "minecraft:block.wood.break", 0.8)
    p.sound(2.2, "minecraft:entity.iron_golem.repair", 0.7)

    cam(p, 0.0, (0.2, 1.3, 3.4), (-1.3, 0.7, 0), 46)
    cam(p, 0.5, (0.1, 1.25, 3.0), (-1.4, 0.9, 0), 44)
    cut(p, 0.55, (0.2, 1.3, 4.4), (0.2, 1.2, 0), 55)            # 全体：台が飛ぶ
    cam(p, 1.15, (0.4, 1.3, 4.0), (0.6, 1.0, 0), 52)
    cut(p, 1.2, (3.2, 2.1, 2.6), (2.2, 0.2, 0), 46)             # 下じき
    cam(p, 1.95, (3.0, 1.9, 2.3), (2.2, 0.2, 0), 44)
    cut(p, 2.0, (1.0, 1.3, 4.8), (0.4, 0.9, 0), 58)             # 勝ちほこる（下じきの自分も）
    cam(p, 3.6, (0.7, 1.3, 4.4), (0.3, 1.0, 0), 55)
    return p


# ================================================================ 呪狼：鬼火の呪い（お札をはって呪う）

def juro():
    p = base("juro", 3.8)
    # 手に持つお札
    h = hand_group(p, "killer", "r", "ofuda_hand")
    p.cube(h, "paper", [4.6, 5.5, -2.3], [7.4, 12.5, -2.1], "white_concrete")
    p.cube(h, "ink", [5.6, 6.2, -2.4], [6.4, 11.8, -2.3], "red_concrete")
    p.cube(h, "ink2", [5.0, 10.4, -2.4], [7.0, 10.9, -2.3], "red_concrete")
    # おでこにはりつくお札
    f = p.group("ofuda_face", [0, 31, -4.2], "victim_head")
    p.cube(f, "paper", [-1.6, 24.5, -4.5], [1.6, 32, -4.25], "white_concrete")
    p.cube(f, "ink", [-0.4, 25.2, -4.6], [0.4, 31.3, -4.5], "red_concrete")
    p.cube(f, "ink2", [-1.1, 29.8, -4.6], [1.1, 30.3, -4.5], "red_concrete")
    show(p, "ofuda_face", 0.0, False)
    show(p, "ofuda_face", 0.9, True)
    show(p, "ofuda_hand", 0.0, True)
    show(p, "ofuda_hand", 0.9, False)
    # 鬼火（3つ）
    for k in range(3):
        g = p.group(f"onibi{k}", [0, 0, 0])
        p.cube(g, "fire", [-2, -2, -2], [2, 2, 2], "glow_light_blue_concrete")
        p.cube(g, "core", [-1, -1, -1], [1, 1, 1], "glow_white_concrete")
        p.cube(g, "tail", [-1.2, 1.5, -1.2], [1.2, 4.5, 1.2], "glow_cyan_concrete")
        show(p, f"onibi{k}", 0.0, False)
        show(p, f"onibi{k}", 1.15 + k * 0.12, True)
        show(p, f"onibi{k}", 2.85, False)

    VZ = 0.55
    # 自分（-Z を向いて立つ）→ お札をはられて硬直（キョンシーのように手が前へ）→ 呪いで倒れる
    K(p, "victim", 0.0, at=(0, 0, VZ), root=(0, 0, 0), head=(0, 0, 0), ra=(0, 0, 0), la=(0, 0, 0), rl=(0, 0, 0), ll=(0, 0, 0))
    K(p, "victim", 0.7, at=(0, 0, VZ), root=(0, 0, 0), head=(0, 10, 0), ra=(-10, 0, 0), la=(-10, 0, 0))
    K(p, "victim", 0.9, at=(0, 0, VZ + 0.05), root=(-8, 0, 0), head=(-15, 0, 0), ra=(-20, 0, 10), la=(-20, 0, -10))
    K(p, "victim", 1.2, at=(0, 0, VZ + 0.05), root=(-2, 0, 0), head=(-5, 0, 0), ra=(-90, 0, 0), la=(-90, 0, 0), rl=(0, 0, 0), ll=(0, 0, 0))
    for k in range(4):  # ぴょんぴょん（呪いではねる）
        t = 1.4 + k * 0.32
        K(p, "victim", t, at=(0, 0, VZ + 0.05), ra=(-90, 0, 0), la=(-90, 0, 0))
        K(p, "victim", t + 0.16, at=(0, 0.28, VZ + 0.05), ra=(-95, 0, 0), la=(-95, 0, 0))
    K(p, "victim", 2.75, at=(0, 0, VZ + 0.05), root=(-2, 0, 0), head=(-5, 0, 0), ra=(-90, 0, 0), la=(-90, 0, 0))
    K(p, "victim", 2.95, at=(0, 0, VZ + 0.05), root=(-15, 0, 0), head=(-30, 0, 0), ra=(-140, 0, 20), la=(-140, 0, -20),
      rl=(0, 0, 0), ll=(0, 0, 0))
    K(p, "victim", 3.25, at=(0, 0.13, VZ + 0.15), root=(-90, 0, 0), head=(0, 25, 0), ra=(-175, 0, 60), la=(-175, 0, -60),
      rl=(-15, 0, 5), ll=(-5, 0, -5))
    K(p, "victim", 3.8, at=(0, 0.13, VZ + 0.15), root=(-90, 0, 0), head=(0, 30, 0), ra=(-178, 0, 70), la=(-178, 0, -70),
      rl=(-3, 0, 8), ll=(-3, 0, -8))

    # 呪狼：お札をかまえて踏みこむ → ペタッ → 下がって印を結ぶ
    K(p, "killer", 0.0, at=(0, 0, -1.6), root=(0, 180, 0), head=(0, 0, 0), ra=(-40, 0, 0), la=(0, 0, 0), rl=(0, 0, 0), ll=(0, 0, 0))
    K(p, "killer", 0.45, at=(0, 0, -1.5), root=(-5, 180, 0), ra=(-150, 0, -10), la=(-10, 0, 0), rl=(0, 0, 0), ll=(0, 0, 0))
    K(p, "killer", 0.7, at=(0, 0, -1.2), root=(5, 180, 0), ra=(-160, 0, -10), rl=(-35, 0, 0), ll=(20, 0, 0))
    K(p, "killer", 0.9, at=(0, 0, -0.75), root=(12, 180, 0), head=(5, 0, 0), ra=(-95, 0, -12), la=(20, 0, 0),
      rl=(-45, 0, 0), ll=(25, 0, 0))
    K(p, "killer", 1.05, at=(0, 0, -0.75), root=(10, 180, 0), ra=(-95, 0, -12), rl=(-40, 0, 0), ll=(20, 0, 0))
    K(p, "killer", 1.35, at=(0, 0, -1.5), root=(0, 180, 0), head=(0, 0, 0), ra=(-60, 0, -20), la=(-60, 0, 20),
      rl=(0, 0, 0), ll=(0, 0, 0))
    K(p, "killer", 1.6, at=(0, 0, -1.6), ra=(-105, -30, -38), la=(-105, 30, 38), head=(10, 0, 0))
    K(p, "killer", 2.6, at=(0, 0, -1.6), ra=(-105, -30, -38), la=(-105, 30, 38), head=(10, 0, 0))
    K(p, "killer", 2.8, ra=(-170, 0, -25), la=(-170, 0, 25), head=(-15, 0, 0))
    K(p, "killer", 3.8, at=(0, 0, -1.6), root=(0, 180, 0), ra=(-160, 0, -30), la=(-160, 0, 30), head=(-10, 0, 0))
    align(p, 0.9, "killer", "killer_right_arm", HAND_R, "victim_head", [0, 29, -4.4], axes=(0, 2))
    v = world_pos(p, "killer", 0.9)
    p.pos("killer", 1.05, *v)

    # 鬼火：自分のまわりを回って、最後に胸へ吸いこまれる
    c = np.array([0, 1.25, VZ])
    for k in range(3):
        for j in range(15):
            t = 1.15 + k * 0.12 + j * 0.1
            if t > 2.6:
                break
            a = j * 0.75 + k * 2.094
            r = 1.05 - 0.15 * math.sin(j * 0.5)
            put(p, f"onibi{k}", t, (c[0] + math.cos(a) * r, c[1] + 0.35 * math.sin(j * 0.9 + k), c[2] + math.sin(a) * r))
        put(p, f"onibi{k}", 2.85, (c[0], c[1] + 0.1, c[2] - 0.05))
        p.scale(f"onibi{k}", 2.7, 1.0)
        p.scale(f"onibi{k}", 2.84, 0.4)

    p.timeline(0.9, "hit")
    p.timeline(0.9, "text:ペタッ:victim_head:FFFFFF:2.0")
    p.timeline(2.85, "hit")
    p.timeline(2.85, "text:呪:screen:9FB4FF:4.0")
    p.sound(0.2, "minecraft:entity.evoker.prepare_attack", 0.8)
    p.sound(0.7, "minecraft:entity.player.attack.sweep", 1.4)
    p.sound(0.9, "minecraft:entity.slime.attack", 1.4)
    p.sound(1.15, "minecraft:particle.soul_escape", 0.8)
    p.sound(1.2, "minecraft:entity.evoker.cast_spell", 0.9)
    p.sound(1.9, "minecraft:particle.soul_escape", 0.6)
    p.sound(2.85, "minecraft:item.firecharge.use", 0.6)
    p.sound(2.85, "minecraft:entity.player.hurt", 0.7)
    p.sound(3.25, "minecraft:entity.player.big_fall", 0.8)

    cam(p, 0.0, (3.6, 1.5, -0.3), (0, 1.1, -0.4), 50)
    cam(p, 0.85, (3.2, 1.55, -0.2), (0, 1.3, -0.1), 46)
    cut(p, 0.88, (1.7, 1.85, -0.2), (0, 1.7, 0.15), 42)          # おでこにペタッ（横顔）
    cam(p, 1.3, (1.6, 1.8, -0.1), (0, 1.65, 0.2), 42)
    cut(p, 1.35, (2.2, 2.4, -3.4), (0, 1.1, 0.5), 55)            # 鬼火がまわる（犯人の後ろから）
    cam(p, 2.7, (0.6, 2.1, -3.5), (0, 1.1, 0.5), 52)
    cut(p, 2.75, (2.4, 1.3, 2.6), (0, 0.8, 0.8), 50)             # 倒れる
    cam(p, 3.8, (2.1, 1.1, 2.3), (0, 0.4, 1.2), 48)
    return p


# ================================================================ 温泉卓球：必殺スマッシュ

def racket(p, who):
    g = hand_group(p, who, "r", f"{who}_racket")
    p.cube(g, "grip", [5.4, 8.5, -0.6], [6.6, 12.5, 0.6], "birch_planks")
    p.cube(g, "blade", [5.55, 2.5, -2.8], [6.45, 8.5, 2.8], "red_concrete")
    p.cube(g, "blade_back", [5.45, 3, -2.3], [5.55, 8, 2.3], "black_concrete")
    return g


RACKET = [6, 5.5, 0]


def pingpong():
    p = base("pingpong", 3.4)
    tb = p.group("pp_table", [0, 0, 0])
    p.cube(tb, "top", [-24, 11, -13], [24, 12.4, 13], "green_concrete")
    p.cube(tb, "line", [-24, 12.4, -0.25], [24, 12.5, 0.25], "white_concrete")
    p.cube(tb, "edge_a", [-24, 12.4, -13], [24, 12.5, -12.5], "white_concrete")
    p.cube(tb, "edge_b", [-24, 12.4, 12.5], [24, 12.5, 13], "white_concrete")
    p.cube(tb, "net", [-0.3, 12.4, -14], [0.3, 15.4, 14], "white_wool")
    for lx in (-21, 20):
        for lz in (-11, 10):
            p.cube(tb, "leg", [lx, 0, lz], [lx + 1, 11, lz + 1], "gray_concrete")
    racket(p, "killer")
    racket(p, "victim")
    ball = p.group("ball", [0, 0, 0])
    p.cube(ball, "ball", [-0.8, -0.8, -0.8], [0.8, 0.8, 0.8], "glow_white_concrete")

    # 犯人（-X 側）
    K(p, "killer", 0.0, at=(-2.3, 0, 0.2), root=(15, 90, 0), head=(-15, 0, 0), ra=(-40, 0, 25), la=(-30, 0, -20),
      rl=(-20, 0, 0), ll=(-10, 0, 0))
    K(p, "killer", 0.2, at=(-2.3, 0, 0.2), root=(18, 90, 0), ra=(-85, 30, 0))
    K(p, "killer", 0.5, at=(-2.3, 0, 0.1), root=(15, 90, 0), ra=(-40, -10, 30))
    K(p, "killer", 0.82, at=(-2.25, 0, 0.0), root=(18, 90, 0), ra=(-95, 25, 0))
    # 跳んでスマッシュ
    K(p, "killer", 1.0, at=(-2.1, 0, 0.0), root=(10, 90, 0), head=(-25, 0, 0), ra=(-60, 0, 30), la=(-60, 0, -10),
      rl=(-50, 0, 0), ll=(-50, 0, 0))
    K(p, "killer", 1.2, at=(-1.95, 0.85, 0.0), root=(-18, 90, 0), head=(-35, 0, 0), ra=(-200, 0, 5), la=(-120, 0, -10),
      rl=(-30, 0, 0), ll=(10, 0, 0))
    K(p, "killer", 1.32, at=(-1.85, 0.95, 0.0), root=(20, 90, 0), head=(-10, 0, 0), ra=(-95, 0, 0), la=(-30, 0, -15),
      rl=(-40, 0, 0), ll=(20, 0, 0))
    K(p, "killer", 1.55, at=(-1.75, 0, 0.0), root=(25, 90, 0), head=(-15, 0, 0), ra=(-20, 0, -10), la=(-20, 0, -10),
      rl=(-45, 0, 0), ll=(15, 0, 0))
    # 決めポーズ（ラケットを肩に）
    K(p, "killer", 2.1, at=(-1.75, 0, 0.0), root=(0, 90, 0), head=(-5, 0, 0), ra=(-95, 0, 0), la=(-10, 0, -10),
      rl=(0, 0, 0), ll=(0, 0, 0))
    K(p, "killer", 3.4, at=(-1.75, 0, 0.0), root=(0, 90, 0), head=(-8, 0, 0), ra=(-98, 0, 0), la=(-8, 0, -10))

    # 自分（+X 側）
    K(p, "victim", 0.0, at=(2.3, 0, -0.1), root=(15, -90, 0), head=(-15, 0, 0), ra=(-40, 0, 25), la=(-30, 0, -20),
      rl=(-20, 0, 0), ll=(-10, 0, 0))
    K(p, "victim", 0.3, at=(2.3, 0, 0.0), ra=(-40, -20, 30))
    K(p, "victim", 0.52, at=(2.3, 0, 0.0), root=(18, -90, 0), ra=(-95, 30, 0))
    K(p, "victim", 0.8, at=(2.3, 0, 0.0), root=(15, -90, 0), ra=(-40, 0, 25))
    K(p, "victim", 1.3, at=(2.3, 0, 0.0), root=(8, -90, 0), head=(-20, 0, 0), ra=(-60, 0, 30))
    K(p, "victim", 1.45, at=(2.32, 0, 0.0), root=(-5, -90, 0), head=(-35, 0, 0), ra=(-80, 0, 40), la=(-60, 0, -30),
      rl=(0, 0, 0), ll=(0, 0, 0))
    K(p, "victim", 1.65, at=(2.55, 0.3, 0.0), root=(-55, -90, 0), head=(-20, 0, 0), ra=(-160, 0, 45), la=(-160, 0, -45),
      rl=(-30, 0, 0), ll=(-10, 0, 0))
    K(p, "victim", 1.85, at=(2.75, 0.13, 0.0), root=(-90, -90, 0), head=(0, 0, 0), ra=(-178, 0, 70), la=(-178, 0, -70),
      rl=(-10, 0, 8), ll=(-5, 0, -8))
    K(p, "victim", 3.4, at=(2.75, 0.13, 0.0), root=(-90, -90, 0), head=(0, 30, 0), ra=(-178, 0, 75), la=(-178, 0, -75),
      rl=(-3, 0, 8), ll=(-3, 0, -8))

    # ボール：ラケットに当たる時刻にラケットの面へ
    def on(t, who):
        return at_point(p, t, f"{who}_racket", RACKET)
    k1 = on(0.2, "killer")
    v1 = on(0.52, "victim")
    k2 = on(0.82, "killer")
    k3 = on(1.32, "killer")
    face = at_point(p, 1.45, "victim_head", [0, 30, -4.3])
    put(p, "ball", 0.0, (k1[0], k1[1] + 0.3, k1[2]))
    put(p, "ball", 0.2, k1, "linear")
    put(p, "ball", 0.37, (1.0, 0.8, 0.05), "linear")
    put(p, "ball", 0.52, v1, "linear")
    put(p, "ball", 0.68, (-0.9, 0.8, 0.05), "linear")
    put(p, "ball", 0.82, k2, "linear")
    put(p, "ball", 0.97, (0.9, 0.8, 0.0), "catmullrom")
    put(p, "ball", 1.12, (0.3, 2.1, 0.0), "catmullrom")
    put(p, "ball", 1.32, k3, "linear")
    put(p, "ball", 1.45, face, "linear")
    put(p, "ball", 1.7, (face[0] + 0.3, face[1] + 1.4, face[2] + 0.3), "catmullrom")
    put(p, "ball", 2.0, (face[0] + 0.7, 0.85, face[2] + 0.6), "catmullrom")
    put(p, "ball", 2.2, (face[0] + 0.9, 0.05, face[2] + 0.8), "linear")
    put(p, "ball", 2.4, (face[0] + 1.1, 0.4, face[2] + 0.9), "linear")
    put(p, "ball", 2.6, (face[0] + 1.25, 0.05, face[2] + 1.0), "linear")
    put(p, "ball", 3.4, (face[0] + 1.4, 0.05, face[2] + 1.1), "linear")

    p.timeline(1.2, "text:スマーッシュ！！:killer_head:FFE08A:2.4")
    p.timeline(1.45, "hit")
    p.timeline(1.45, "text:カコーン:victim_head:FFFFFF:2.0")
    for t in (0.2, 0.52, 0.82):
        p.sound(t, "minecraft:block.bamboo_wood.hit", 1.8)
    for t in (0.37, 0.68, 0.97):
        p.sound(t, "minecraft:block.note_block.hat", 1.6)
    p.sound(1.32, "minecraft:entity.player.attack.strong", 1.4)
    p.sound(1.32, "minecraft:block.bamboo_wood.hit", 2.0)
    p.sound(1.45, "minecraft:block.note_block.hat", 0.6)
    p.sound(1.45, "minecraft:entity.player.hurt", 0.9)
    p.sound(1.85, "minecraft:entity.player.big_fall", 0.8)

    cam(p, 0.0, (0.0, 2.0, 4.2), (0, 0.9, 0), 55)
    cam(p, 0.95, (0.0, 2.1, 3.9), (0, 0.9, 0), 53)
    cut(p, 1.0, (-0.6, 0.9, 1.8), (-1.95, 1.7, 0.0), 50)         # 下から跳ぶ犯人
    cam(p, 1.3, (-0.7, 0.8, 1.7), (-1.9, 2.2, 0.0), 50)
    cut(p, 1.35, (0.6, 1.9, 1.3), (2.3, 1.7, 0.0), 40)           # 顔に当たる
    cam(p, 1.6, (0.8, 1.6, 1.4), (2.5, 1.0, 0.0), 44)
    cut(p, 1.9, (0.6, 1.9, 5.0), (0.7, 0.6, 0), 58)              # 全体
    cam(p, 3.4, (0.4, 1.8, 4.6), (0.7, 0.6, 0), 55)
    return p


# ================================================================ 修学旅行：枕投げ

def pillow_cubes(p, g, c):
    x, y, z = c
    p.cube(g, "pillow", [x - 4.5, y - 2, z - 3], [x + 4.5, y + 2, z + 3], "white_wool")
    p.cube(g, "pillow_band", [x - 0.6, y - 2.1, z - 3.1], [x + 0.6, y + 2.1, z + 3.1], "light_blue_wool")


def pillow():
    p = base("pillow", 3.6)
    # 布団（2組）
    fu = p.group("futon", [0, 0, 0])
    for x0, x1 in ((-50, -16), (24, 64)):
        p.cube(fu, "shikibuton", [x0, 0, -15], [x1, 1.5, 15], "white_wool")
        p.cube(fu, "makura", [x0 + 2, 1.5, -5] if x0 < 0 else [x1 - 7, 1.5, -5], [x0 + 7, 3.5, 5] if x0 < 0 else [x1 - 2, 3.5, 5], "white_wool")
    # 手の枕
    h = hand_group(p, "killer", "r", "pillow_hand")
    pillow_cubes(p, h, (6, 13, 0))
    # 飛ぶ枕（1つ目・2つ目）
    for k in range(2):
        g = p.group(f"pillow_fly{k}", [0, 0, 0])
        pillow_cubes(p, g, (0, 0, 0))

    KX, VX = -2.3, 1.9
    # 犯人（-X 側の布団の上）
    K(p, "killer", 0.0, at=(KX, 0.09, 0), root=(0, 90, 0), head=(0, 0, 0), ra=(-30, 0, 10), la=(-20, 0, -10), rl=(0, 0, 0), ll=(0, 0, 0))
    K(p, "killer", 0.55, at=(KX, 0.09, 0), root=(-12, 90, 0), head=(-10, 0, 0), ra=(-200, 0, 10), la=(-60, 0, -20),
      rl=(-10, 0, 0), ll=(20, 0, 0))
    K(p, "killer", 0.75, at=(KX + 0.15, 0.09, 0), root=(-15, 90, 0), ra=(-215, 0, 10), la=(-70, 0, -20))
    K(p, "killer", 0.9, at=(KX + 0.3, 0.09, 0), root=(15, 90, 0), head=(-15, 0, 0), ra=(-80, 0, 0), la=(-10, 0, -20),
      rl=(-35, 0, 0), ll=(25, 0, 0))
    K(p, "killer", 1.15, at=(KX + 0.3, 0.09, 0), root=(10, 90, 0), ra=(-30, 0, 0), la=(-10, 0, -10))
    # 2つ目：かがんで拾って、跳んで投げる
    K(p, "killer", 1.4, at=(KX + 0.25, 0.09, 0), root=(45, 90, 0), head=(-30, 0, 0), ra=(-60, 0, 0), la=(-40, 0, 0),
      rl=(-40, 0, 0), ll=(-40, 0, 0))
    K(p, "killer", 1.65, at=(KX + 0.25, 0.09, 0), root=(-5, 90, 0), head=(-10, 0, 0), ra=(-190, 0, 5), la=(-80, 0, -20),
      rl=(-30, 0, 0), ll=(-30, 0, 0))
    K(p, "killer", 1.82, at=(KX + 0.45, 0.6, 0), root=(-20, 90, 0), head=(-15, 0, 0), ra=(-225, 0, 5), la=(-90, 0, -25),
      rl=(-50, 0, 0), ll=(10, 0, 0))
    K(p, "killer", 1.98, at=(KX + 0.7, 0.55, 0), root=(25, 90, 0), head=(-20, 0, 0), ra=(-70, 0, 0), la=(-20, 0, -20),
      rl=(-60, 0, 0), ll=(30, 0, 0))
    K(p, "killer", 2.2, at=(KX + 0.85, 0.09, 0), root=(15, 90, 0), ra=(-25, 0, 0), la=(-15, 0, -10), rl=(-30, 0, 0), ll=(10, 0, 0))
    # 勝ったー！（両手を上げてぴょんぴょん）
    K(p, "killer", 2.6, at=(KX + 0.85, 0.09, 0), root=(0, 90, 0), head=(-20, 0, 0), ra=(-170, 0, 25), la=(-170, 0, -25),
      rl=(0, 0, 0), ll=(0, 0, 0))
    for k in range(3):
        t = 2.75 + k * 0.28
        K(p, "killer", t, at=(KX + 0.85, 0.35, 0), ra=(-175, 0, 30), la=(-175, 0, -30), rl=(-15, 0, 0), ll=(-15, 0, 0))
        K(p, "killer", t + 0.14, at=(KX + 0.85, 0.09, 0), ra=(-165, 0, 20), la=(-165, 0, -20), rl=(0, 0, 0), ll=(0, 0, 0))

    # 手の枕：1つ目を投げたら消えて、拾うときにまた出る
    show(p, "pillow_hand", 0.0, True)
    show(p, "pillow_hand", 0.9, False)
    show(p, "pillow_hand", 1.4, True)
    show(p, "pillow_hand", 1.98, False)

    # 自分（+X 側の布団の上）
    K(p, "victim", 0.0, at=(VX, 0.09, 0), root=(0, -90, 0), head=(0, 0, 0), ra=(0, 0, 0), la=(0, 0, 0), rl=(0, 0, 0), ll=(0, 0, 0))
    K(p, "victim", 0.9, at=(VX, 0.09, 0), root=(0, -90, 0), head=(0, 0, 0), ra=(-20, 0, 10), la=(-20, 0, -10))
    K(p, "victim", 1.15, at=(VX + 0.05, 0.09, 0), root=(-18, -90, 0), head=(-35, 0, 0), ra=(-60, 0, 40), la=(-60, 0, -40))
    K(p, "victim", 1.35, at=(VX + 0.05, 0.09, 0), root=(-4, -90, 0), head=(-5, 0, 0), ra=(-40, 0, 25), la=(-40, 0, -25))
    K(p, "victim", 1.9, at=(VX + 0.05, 0.09, 0), root=(0, -90, 0), head=(-10, 0, 0), ra=(-70, 0, 20), la=(-70, 0, -20),
      rl=(0, 0, 0), ll=(0, 0, 0))
    K(p, "victim", 2.12, at=(VX + 0.05, 0.09, 0), root=(-8, -90, 0), head=(-40, 0, 0), ra=(-120, 0, 40), la=(-120, 0, -40))
    K(p, "victim", 2.35, at=(VX + 0.35, 0.4, 0), root=(-60, -90, 0), head=(-20, 0, 0), ra=(-170, 0, 50), la=(-170, 0, -50),
      rl=(-40, 0, 0), ll=(-20, 0, 0))
    K(p, "victim", 2.55, at=(VX + 0.55, 0.22, 0), root=(-90, -90, 0), head=(0, 0, 0), ra=(-178, 0, 70), la=(-178, 0, -70),
      rl=(-10, 0, 8), ll=(-5, 0, -8))
    K(p, "victim", 3.6, at=(VX + 0.55, 0.22, 0), root=(-90, -90, 0), head=(0, 25, 0), ra=(-178, 0, 75), la=(-178, 0, -75),
      rl=(-3, 0, 8), ll=(-3, 0, -8))

    # 飛ぶ枕
    for k, (t0, t1, spin) in enumerate(((0.9, 1.15, 1), (1.98, 2.12, 2))):
        b = f"pillow_fly{k}"
        show(p, b, 0.0, False)
        show(p, b, t0, True)
        a = at_point(p, t0, "pillow_hand", (6, 13, 0))
        face = at_point(p, t1, "victim_head", [0, 28, -4.5])
        mid = [(a[0] + face[0]) / 2, max(a[1], face[1]) + 0.35 * (2 - k), (a[2] + face[2]) / 2]
        put(p, b, t0, a)
        put(p, b, (t0 + t1) / 2, mid)
        put(p, b, t1, face)
        p.rot(b, t0, 0, 0, 0)
        p.rot(b, t1, 0, 0, -360 * spin)
        # はね返って布団に落ちる
        put(p, b, t1 + 0.25, (face[0] - 0.6, face[1] + 0.5, face[2] + 0.4))
        put(p, b, t1 + 0.5, (face[0] - 0.9, 0.2, face[2] + 0.6))
        put(p, b, 3.6, (face[0] - 0.9, 0.2, face[2] + 0.6))
        p.rot(b, t1 + 0.5, 0, 40, -450 * spin)
        p.rot(b, 3.6, 0, 40, -450 * spin)

    p.timeline(0.55, "text:くらえっ！:killer_head:FFE08A:2.0")
    p.timeline(1.15, "hit")
    p.timeline(1.15, "text:ボフッ:victim_head:FFFFFF:2.0")
    p.timeline(2.12, "hit")
    p.timeline(2.12, "text:ボフッ！！:victim_head:FFFFFF:2.6")
    p.timeline(2.75, "text:勝ったー！:killer_head:FFE08A:2.0")
    p.sound(0.9, "minecraft:entity.player.attack.sweep", 1.2)
    p.sound(1.98, "minecraft:entity.player.attack.sweep", 1.0)
    for t in (1.15, 2.12):
        p.sound(t, "minecraft:block.wool.place", 0.6)
        p.sound(t, "minecraft:entity.player.hurt", 1.0)
    p.sound(2.55, "minecraft:block.wool.fall", 0.6)
    p.sound(2.75, "minecraft:entity.villager.celebrate", 1.2)

    cam(p, 0.0, (-0.6, 1.5, 3.4), (-0.4, 1.1, 0), 55)
    cam(p, 0.85, (-0.9, 1.45, 3.0), (-0.6, 1.2, 0), 52)
    cut(p, 0.9, (-0.2, 1.7, 1.6), (VX, 1.6, 0), 42)              # 顔へ飛ぶ枕
    cam(p, 1.35, (0.1, 1.65, 1.5), (VX, 1.5, 0), 42)
    cut(p, 1.4, (-3.0, 1.1, 2.6), (KX + 0.4, 1.2, 0), 50)       # 拾って跳ぶ
    cam(p, 1.95, (-2.6, 0.9, 2.5), (KX + 0.6, 1.6, 0), 50)
    cut(p, 2.0, (0.4, 1.4, 2.6), (VX + 0.2, 0.9, 0), 46)        # 布団にどさっ
    cam(p, 2.6, (0.3, 1.5, 2.6), (VX + 0.3, 0.6, 0), 46)
    cut(p, 2.65, (0.6, 1.3, 3.0), (KX + 1.2, 1.2, 0), 55)      # 勝ったー
    cam(p, 3.6, (0.4, 1.3, 2.7), (KX + 1.1, 1.3, 0), 52)
    return p


# ================================================================ ふすまドーン

def fusuma():
    p = base("fusuma", 3.6)
    f = p.group("fusuma_panel", [0, 0, 0])
    p.cube(f, "paper", [-14, 1, -0.7], [14, 46, 0.7], "white_wool")
    p.cube(f, "paper_pattern", [-12, 6, 0.7], [12, 40, 0.8], "light_blue_terracotta")
    p.cube(f, "paper_pattern_b", [-12, 6, -0.8], [12, 40, -0.7], "light_blue_terracotta")
    p.cube(f, "frame_top", [-15, 45, -1], [15, 47, 1], "dark_oak_planks")
    p.cube(f, "frame_bottom", [-15, 0, -1], [15, 2, 1], "dark_oak_planks")
    p.cube(f, "frame_l", [-15, 0, -1], [-13, 47, 1], "dark_oak_planks")
    p.cube(f, "frame_r", [13, 0, -1], [15, 47, 1], "dark_oak_planks")
    p.cube(f, "hikite", [9, 21, 0.9], [11, 24, 1.2], "black_concrete")
    p.cube(f, "hikite_b", [9, 21, -1.2], [11, 24, -0.9], "black_concrete")
    # 鴨居（上の枠・まわりの柱）
    fr = p.group("kamoi", [0, 0, 0])
    p.cube(fr, "kamoi", [-24, 47, -2], [24, 50, 2], "dark_oak_planks")
    p.cube(fr, "post_l", [-24, 0, -2], [-21, 47, 2], "stripped_spruce_log")
    p.cube(fr, "post_r", [21, 0, -2], [24, 47, 2], "stripped_spruce_log")
    p.cube(fr, "wall_l", [-21, 0, -1], [-15, 47, 1], "calcite")
    p.cube(fr, "wall_r", [15, 0, -1], [21, 47, 1], "calcite")
    p.cube(fr, "ranma", [-24, 50, -1.5], [24, 60, 1.5], "calcite")
    FZ = -0.3
    p.pos("kamoi", 0.0, 0, 0, FZ)
    p.pos("fusuma_panel", 0.0, 0, 0, FZ)

    # ふすまが前に倒れる（下のふちを軸に）
    p.rot("fusuma_panel", 0.0, 0, 0, 0)
    p.rot("fusuma_panel", 1.25, 0, 0, 0)
    p.rot("fusuma_panel", 1.32, -12, 0, 0, interp="linear")
    p.rot("fusuma_panel", 1.55, -86, 0, 0, interp="linear")
    p.rot("fusuma_panel", 1.62, -84, 0, 0)
    p.rot("fusuma_panel", 1.75, -86, 0, 0)
    p.rot("fusuma_panel", 3.6, -86, 0, 0)
    p.pos("fusuma_panel", 1.25, 0, 0, FZ)
    p.pos("fusuma_panel", 1.55, 0, 0.05, FZ + 0.15)
    p.pos("fusuma_panel", 3.6, 0, 0.05, FZ + 0.15)

    VZ = 1.0
    # 自分：ふすまの前で「？」→ 下じき
    K(p, "victim", 0.0, at=(0, 0, VZ), root=(0, 0, 0), head=(0, 0, 0), ra=(0, 0, 0), la=(0, 0, 0), rl=(0, 0, 0), ll=(0, 0, 0))
    K(p, "victim", 0.5, at=(0, 0, VZ), head=(5, -15, 0))
    K(p, "victim", 0.9, at=(0, 0, VZ - 0.05), root=(8, 0, 0), head=(10, 0, 20), ra=(-15, 0, 0), la=(-10, 0, 0))
    K(p, "victim", 1.25, at=(0, 0, VZ - 0.05), root=(10, 0, 0), head=(10, 0, 22), ra=(-15, 0, 0), la=(-10, 0, 0))
    K(p, "victim", 1.38, at=(0, 0, VZ), root=(-15, 0, 0), head=(-35, 0, 0), ra=(-150, 0, 30), la=(-150, 0, -30),
      rl=(0, 0, 0), ll=(0, 0, 0))
    K(p, "victim", 1.55, at=(0, 0.13, VZ + 0.15), root=(-90, 0, 0), head=(-10, 0, 0), ra=(-180, 0, 88), la=(-180, 0, -88),
      rl=(-5, 0, 8), ll=(-5, 0, -8))
    K(p, "victim", 3.6, at=(0, 0.13, VZ + 0.15), root=(-90, 0, 0), head=(-10, 0, 0), ra=(-180, 0, 88), la=(-180, 0, -88))
    for k in range(6):  # ピクピク
        t = 2.35 + k * 0.13
        K(p, "victim", t, ra=(-180, 0, 88 + 8 * (k % 2)), la=(-180, 0, -88 - 8 * ((k + 1) % 2)))

    # 犯人：ふすまの向こうから走ってきて、とび蹴り → ふすまの上に着地して腕組み
    for k in range(6):
        t = k * 0.22
        sgn = 1 if k % 2 == 0 else -1
        K(p, "killer", t, at=(0, 0, -4.0 + k * 0.55), root=(15, 180, 0), head=(-15, 0, 0),
          ra=(-50 * sgn, 0, 0), la=(50 * sgn, 0, 0), rl=(45 * sgn, 0, 0), ll=(-45 * sgn, 0, 0))
    K(p, "killer", 1.3, at=(0, 0.75, -0.75), root=(-25, 180, 0), head=(-10, 0, 0), ra=(-40, 0, 40), la=(-40, 0, -40),
      rl=(-95, 0, 0), ll=(-20, 0, 0))
    K(p, "killer", 1.55, at=(0, 0.9, 0.3), root=(-10, 180, 0), ra=(-60, 0, 50), la=(-60, 0, -50), rl=(-60, 0, 0), ll=(-30, 0, 0))
    K(p, "killer", 1.75, at=(0, 0.2, 1.1), root=(10, 180, 0), head=(0, 0, 0), ra=(-30, 0, 30), la=(-30, 0, -30),
      rl=(-25, 0, 0), ll=(-25, 0, 0))
    K(p, "killer", 2.05, at=(0, 0.17, 1.15), root=(0, 180, 0), head=(-10, 0, 0), ra=(-70, 40, -10), la=(-70, -40, 10),
      rl=(0, 0, 5), ll=(0, 0, -5))
    K(p, "killer", 3.6, at=(0, 0.17, 1.15), root=(0, 180, 0), head=(-12, 0, 0), ra=(-72, 40, -10), la=(-72, -40, 10),
      rl=(0, 0, 5), ll=(0, 0, -5))

    p.timeline(0.9, "text:？:victim_head:FFFFFF:2.4")
    p.timeline(1.55, "hit")
    p.timeline(1.55, "text:ドーン！！:screen:FFE08A:3.2")
    p.timeline(2.4, "text:ピクピク…:victim_right_arm:FFFFFF:1.6")
    for t in (0.3, 0.55, 0.8, 1.0, 1.15):
        p.sound(t, "minecraft:block.wood.step", 0.9 + t * 0.2)
    p.sound(1.3, "minecraft:entity.player.attack.knockback", 0.8)
    p.sound(1.55, "minecraft:block.anvil.land", 0.6)
    p.sound(1.55, "minecraft:block.wood.break", 0.6)
    p.sound(1.75, "minecraft:entity.player.big_fall", 1.0)
    p.sound(2.6, "minecraft:entity.chicken.egg", 1.4)

    cam(p, 0.0, (2.4, 1.6, 3.2), (0, 1.4, 0.2), 50)
    cam(p, 1.2, (2.1, 1.6, 2.8), (0, 1.5, 0.3), 46)
    cut(p, 1.25, (3.0, 1.3, 1.0), (0, 1.0, 0.4), 55)            # 横から：倒れるふすま
    cam(p, 1.6, (3.0, 1.2, 1.2), (0, 0.6, 0.8), 55)
    cut(p, 1.65, (2.3, 2.9, 5.0), (0, 0.6, 1.3), 52)            # 上から：ふすまの上に立つ
    cam(p, 3.6, (1.8, 2.7, 4.6), (0, 0.8, 1.4), 50)
    return p


# ================================================================ 柔の道：背負い投げ

def seoi():
    p = base("seoi", 3.4)
    belt = p.group("obi", [0, 24, 0], "killer_body")
    p.cube(belt, "obi", [-4.4, 11.5, -2.4], [4.4, 13.2, 2.4], "black_wool")
    p.cube(belt, "knot", [-1.2, 9.5, -2.8], [1.2, 13.2, -2.4], "black_wool")

    # 犯人：+X を向いて組む → くるっと背を向けて → 投げる
    K(p, "killer", 0.0, at=(-0.35, 0, 0), root=(10, 90, 0), head=(-10, 0, 0), ra=(-40, 0, 0), la=(-40, 0, 0),
      rl=(-15, 0, 10), ll=(-15, 0, -10))
    K(p, "killer", 0.4, at=(0.0, 0, 0), root=(4, 90, 0), ra=(-80, 0, -10), la=(-90, 0, 10))
    K(p, "killer", 0.65, at=(0.0, 0, 0), root=(6, 90, 0), ra=(-85, 0, -10), la=(-95, 0, 10))
    K(p, "killer", 0.85, at=(0.35, 0, 0.05), root=(20, -30, 0), head=(-15, 0, 0), ra=(-120, 0, -20), la=(-100, 0, 20),
      rl=(-30, 0, 10), ll=(-30, 0, -10))
    K(p, "killer", 1.0, at=(0.45, 0, 0.0), root=(28, -90, 0), head=(-20, 0, 0), ra=(-150, 0, -15), la=(-130, 0, 15),
      rl=(-40, 0, 10), ll=(-40, 0, -10))
    K(p, "killer", 1.4, at=(0.45, 0, 0.0), root=(55, -90, 0), head=(-45, 0, 0), ra=(-90, 0, -10), la=(-80, 0, 10),
      rl=(-50, 0, 10), ll=(-50, 0, -10))
    K(p, "killer", 1.75, at=(0.4, 0, 0.0), root=(65, -90, 0), head=(-55, 0, 0), ra=(-30, 0, -5), la=(-25, 0, 5),
      rl=(-55, 0, 10), ll=(-55, 0, -10))
    K(p, "killer", 1.9, at=(0.4, 0, 0.0), root=(60, -90, 0), head=(-50, 0, 0), ra=(-15, 0, -5), la=(-15, 0, 5))
    # 残心（そで を持ったまま、すっと立つ）
    K(p, "killer", 2.4, at=(0.4, 0, 0.0), root=(12, -90, 0), head=(20, 0, 0), ra=(-45, 0, -5), la=(-20, 0, 20),
      rl=(-10, 0, 10), ll=(-10, 0, -10))
    K(p, "killer", 3.4, at=(0.4, 0, 0.0), root=(8, -90, 0), head=(25, 0, 0), ra=(-48, 0, -5), la=(-15, 0, 20),
      rl=(-5, 0, 8), ll=(-5, 0, -8))

    # 自分：-X を向いて立つ → 背中にのせられて → くるっと一回転して背中から落ちる
    K(p, "victim", 0.0, at=(1.1, 0, 0), root=(5, -90, 0), head=(0, 0, 0), ra=(-30, 0, 0), la=(-30, 0, 0), rl=(0, 0, 0), ll=(0, 0, 0))
    K(p, "victim", 0.65, at=(1.05, 0, 0), root=(3, -90, 0), head=(-10, 0, 0), ra=(-50, 0, 10), la=(-50, 0, -10))
    K(p, "victim", 1.0, at=(1.05, 0, 0), root=(25, -90, 0), head=(-25, 0, 0), ra=(-100, 0, 10), la=(-80, 0, -10))
    arc = [(1.2, 1.1, 0.35, 55), (1.4, 1.15, 1.75, 130), (1.55, 0.45, 2.2, 190), (1.72, -1.0, 1.7, 235), (1.9, -1.9, 0.13, 270)]
    for (t, x, y, a) in arc:
        K(p, "victim", t, at=(x, y, 0), root=(a, -90, 0), head=(-20, 0, 0), ra=(-160, 0, 30), la=(-150, 0, -30),
          rl=(-20, 0, 10), ll=(-10, 0, -10))
    K(p, "victim", 2.05, at=(-1.9, 0.18, 0), root=(266, -90, 0), head=(-15, 0, 0), ra=(-175, 0, 60), la=(-170, 0, -60),
      rl=(-25, 0, 10), ll=(-10, 0, -10))
    K(p, "victim", 2.2, at=(-1.9, 0.13, 0), root=(270, -90, 0), head=(0, 0, 0), ra=(-178, 0, 75), la=(-178, 0, -75),
      rl=(-5, 0, 8), ll=(-5, 0, -8))
    K(p, "victim", 3.4, at=(-1.9, 0.13, 0), root=(270, -90, 0), head=(0, 30, 0), ra=(-178, 0, 78), la=(-178, 0, -78),
      rl=(-3, 0, 8), ll=(-3, 0, -8))

    p.timeline(1.0, "text:せいっ！:killer_head:FFE08A:2.2")
    p.timeline(1.9, "hit")
    p.timeline(2.25, "text:一本！:screen:FFFFFF:3.2")
    p.sound(0.45, "minecraft:item.armor.equip_leather", 0.8)
    p.sound(1.2, "minecraft:entity.player.attack.sweep", 0.7)
    p.sound(1.9, "minecraft:entity.player.big_fall", 0.6)
    p.sound(1.9, "minecraft:block.note_block.basedrum", 0.5)
    p.sound(1.9, "minecraft:entity.player.hurt", 0.8)
    p.sound(2.25, "minecraft:block.bell.use", 1.2)

    cam(p, 0.0, (0.5, 1.5, 3.2), (0.4, 1.2, 0), 46)
    cam(p, 0.95, (0.5, 1.5, 2.8), (0.5, 1.2, 0), 44)
    cut(p, 1.0, (0.2, 0.6, 3.3), (0.1, 1.5, 0), 60)             # 下から：大きく回る
    cam(p, 1.85, (-0.1, 0.6, 3.1), (-0.4, 1.2, 0), 60)
    cut(p, 1.9, (-1.0, 2.3, 3.0), (-0.7, 0.4, 0), 52)           # 斜め上から：一本
    cam(p, 3.4, (-0.9, 2.2, 2.7), (-0.6, 0.5, 0), 50)
    return p


# ================================================================ 猫又の祟り（大きな化け猫）

def cat_model(p):
    root = p.group("cat", [0, 0, 0])
    body = p.group("cat_body", [0, 16, 0], root)
    p.cube(body, "body", [-5, 11, -11], [5, 21, 11], "black_wool")
    p.cube(body, "belly", [-4, 10.5, -9], [4, 11.5, 9], "purple_wool")
    head = p.group("cat_head", [0, 19, -11], root)
    p.cube(head, "head", [-5, 15, -19], [5, 24, -10], "black_wool")
    p.cube(head, "snout", [-2.5, 15, -20.5], [2.5, 18.5, -19], "black_wool")
    p.cube(head, "nose", [-0.8, 17.6, -20.7], [0.8, 18.6, -20.4], "pink_wool")
    p.cube(head, "ear_r", [1.5, 24, -16], [4.5, 27.5, -13], "black_wool")
    p.cube(head, "ear_l", [-4.5, 24, -16], [-1.5, 27.5, -13], "black_wool")
    p.cube(head, "ear_in_r", [2.2, 24, -16.2], [3.8, 26.5, -16], "magenta_wool")
    p.cube(head, "ear_in_l", [-3.8, 24, -16.2], [-2.2, 26.5, -16], "magenta_wool")
    p.cube(head, "eye_r", [1.2, 20, -19.25], [3.8, 22, -19.05], "glow_yellow_concrete")
    p.cube(head, "eye_l", [-3.8, 20, -19.25], [-1.2, 22, -19.05], "glow_yellow_concrete")
    p.cube(head, "pupil_r", [2.2, 20, -19.35], [2.8, 22, -19.25], "black_concrete")
    p.cube(head, "pupil_l", [-2.8, 20, -19.35], [-2.2, 22, -19.25], "black_concrete")
    p.cube(head, "fang_r", [0.6, 14, -20.4], [1.4, 15.5, -19.8], "white_concrete")
    p.cube(head, "fang_l", [-1.4, 14, -20.4], [-0.6, 15.5, -19.8], "white_concrete")
    for name, x, z in (("cat_leg_fr", 3, -8), ("cat_leg_fl", -3, -8), ("cat_leg_br", 3, 8), ("cat_leg_bl", -3, 8)):
        g = p.group(name, [x, 13, z], root)
        p.cube(g, "leg", [x - 1.5, 0, z - 1.5], [x + 1.5, 13, z + 1.5], "black_wool")
        if "_f" in name:
            for c in (-1, 0, 1):
                p.cube(g, "claw", [x + c - 0.3, 0, z - 2.4], [x + c + 0.3, 1.2, z - 1.5], "glow_white_concrete")
    # しっぽ2本（猫又）
    for name, x, zr in (("cat_tail_r", 2, 15), ("cat_tail_l", -2, -15)):
        g = p.group(name, [x, 20, 11], root)
        p.cube(g, "tail", [x - 1, 19, 11], [x + 1, 21, 25], "black_wool")
        p.cube(g, "tip", [x - 1.3, 18.7, 23], [x + 1.3, 21.3, 26], "glow_purple_concrete")
        p.rot(name, 0.0, 50, zr, 0)
    return root


def neko():
    p = base("neko", 3.5)
    cat_model(p)
    # 猫又（killer）：もう倒れている。そこから化け猫がわき上がる
    K(p, "killer", 0.0, at=(0, 0.13, -0.6), root=(90, 0, 0), head=(-20, 30, 0), ra=(-170, 0, 30), la=(-170, 0, -30),
      rl=(0, 0, 8), ll=(0, 0, -8))
    K(p, "killer", 3.5, at=(0, 0.13, -0.6), root=(90, 0, 0), head=(-20, 30, 0), ra=(-170, 0, 30), la=(-170, 0, -30))

    VZ = 1.4
    # 自分（人狼）：倒した相手を見下ろす → 化け猫にびっくり → 押し倒される
    K(p, "victim", 0.0, at=(0, 0, VZ), root=(5, 0, 0), head=(30, 0, 0), ra=(-10, 0, 0), la=(-10, 0, 0), rl=(0, 0, 0), ll=(0, 0, 0))
    K(p, "victim", 0.6, at=(0, 0, VZ), root=(5, 0, 0), head=(25, 0, 0))
    K(p, "victim", 0.95, at=(0, 0, VZ + 0.25), root=(-12, 0, 0), head=(-35, 0, 0), ra=(-120, 0, 30), la=(-120, 0, -30),
      rl=(-20, 0, 0), ll=(10, 0, 0))
    K(p, "victim", 1.5, at=(0, 0, VZ + 0.45), root=(-10, 0, 0), head=(-40, 0, 0), ra=(-130, 0, 35), la=(-130, 0, -35),
      rl=(15, 0, 0), ll=(-15, 0, 0))
    K(p, "victim", 1.8, at=(0, 0, VZ + 0.45), root=(-20, 0, 0), head=(-45, 0, 0), ra=(-150, 0, 40), la=(-150, 0, -40))
    K(p, "victim", 2.0, at=(0, 0.3, VZ + 0.8), root=(-60, 0, 0), head=(-20, 0, 0), ra=(-165, 0, 45), la=(-165, 0, -45),
      rl=(-30, 0, 0), ll=(-10, 0, 0))
    K(p, "victim", 2.2, at=(0, 0.13, VZ + 1.0), root=(-90, 0, 0), head=(-10, 0, 0), ra=(-175, 0, 60), la=(-175, 0, -60),
      rl=(-15, 0, 5), ll=(-5, 0, -5))
    K(p, "victim", 2.45, head=(0, 25, 0), ra=(-178, 0, 70), la=(-178, 0, -70))
    K(p, "victim", 3.5, at=(0, 0.13, VZ + 1.0), root=(-90, 0, 0), head=(0, 35, 0), ra=(-178, 0, 75), la=(-178, 0, -75),
      rl=(-3, 0, 8), ll=(-3, 0, -8))

    # 化け猫：猫又の体からわき上がり（大きくなる）→ 威嚇 → 飛びかかって2回ひっかく → 上にのって見下ろす
    S = 2.0
    p.pos("cat", 0.0, 0, 0, -1.6)
    p.rot("cat", 0.0, 0, 180, 0)
    p.scale("cat", 0.0, 0.01)
    p.scale("cat", 0.25, 0.01)
    p.scale("cat", 0.8, S)
    p.scale("cat", 3.5, S)
    p.pos("cat", 0.8, 0, 0, -1.9)
    # 威嚇（体をそらせて前足を上げる）
    p.rot("cat", 0.85, -20, 180, 0)
    p.rot("cat_head", 0.85, -10, 0, 0)
    p.rot("cat_head", 1.05, 15, 0, 0)
    p.rot("cat_leg_fr", 0.85, -30, 0, 0)
    p.rot("cat_leg_fl", 0.85, -20, 0, 0)
    p.pos("cat", 1.5, 0, 0, -1.9)
    p.rot("cat", 1.5, -25, 180, 0)
    p.rot("cat_leg_fr", 1.5, -60, 0, 0)
    p.rot("cat_leg_fl", 1.5, -50, 0, 0)
    # 飛びかかる
    p.pos("cat", 1.65, 0, 0, -1.7)
    p.rot("cat", 1.65, 10, 180, 0)
    p.rot("cat_leg_fr", 1.65, 30, 0, 0)
    p.rot("cat_leg_fl", 1.65, 30, 0, 0)
    p.pos("cat", 1.85, 0, 1.0, -0.3)
    p.rot("cat", 1.85, -15, 180, 0)
    p.rot("cat_leg_fr", 1.85, -110, 0, 0)
    p.rot("cat_leg_fl", 1.85, -100, 0, 0)
    p.pos("cat", 2.05, 0, 0.2, 0.6)
    p.rot("cat", 2.05, 15, 180, 0)
    p.rot("cat_leg_fr", 2.05, -30, 0, 0)
    p.rot("cat_leg_fl", 2.05, -25, 0, 0)
    # 2回目のひっかき（右前足）
    p.rot("cat_leg_fr", 2.25, -120, 0, 0)
    p.rot("cat_leg_fr", 2.4, 10, 0, 0)
    p.rot("cat_leg_fl", 2.4, -10, 0, 0)
    p.pos("cat", 2.4, 0, 0.05, 0.8)
    p.rot("cat", 2.4, 9, 180, 0)
    p.rot("cat_head", 2.4, 25, 0, 0)
    p.pos("cat", 3.5, 0, 0.05, 0.8)
    p.rot("cat", 3.5, 8, 180, 0)
    p.rot("cat_head", 3.5, 30, 0, 0)
    p.rot("cat_leg_fr", 3.5, 0, 0, 0)
    p.rot("cat_leg_fl", 3.5, -5, 0, 0)
    for name in ("cat_tail_r", "cat_tail_l"):
        zr = 15 if name.endswith("r") else -15
        for k in range(8):
            t = 0.4 + k * 0.4
            p.rot(name, t, 60 + 10 * (k % 2), zr + 12 * (1 if (k + (name[-1] == "l")) % 2 else -1), 0)

    p.timeline(0.95, "text:シャーッ！！:cat_head:E1B3FF:2.6")
    p.timeline(1.95, "hit")
    p.timeline(2.4, "hit")
    p.sound(0.2, "minecraft:entity.cat.ambient", 0.5)
    p.sound(0.3, "minecraft:particle.soul_escape", 0.6)
    p.sound(0.95, "minecraft:entity.cat.hiss", 0.5)
    for t in (1.95, 2.4):
        p.sound(t, "minecraft:entity.player.attack.sweep", 0.8)
        p.sound(t, "minecraft:entity.player.hurt", 0.8)
    p.sound(2.2, "minecraft:entity.player.big_fall", 0.8)
    p.sound(2.6, "minecraft:entity.cat.purreow", 0.5)

    cam(p, 0.0, (1.9, 2.2, 3.4), (0, 0.5, -0.4), 50)
    cam(p, 0.8, (2.0, 1.9, 3.8), (0, 1.3, -1.2), 55)
    cut(p, 0.85, (2.0, 0.8, 3.8), (0, 1.9, -1.4), 60)            # 見上げる：威嚇
    cam(p, 1.6, (1.8, 0.7, 3.6), (0, 2.2, -1.4), 60)
    cut(p, 1.65, (5.4, 1.9, 2.0), (0, 1.0, 0.8), 55)             # 横から：飛びかかる
    cam(p, 2.45, (5.0, 1.8, 2.4), (0, 0.8, 1.4), 52)
    cut(p, 2.5, (3.0, 0.6, 4.6), (0, 1.3, 1.2), 58)              # 下から：見下ろす化け猫
    cam(p, 3.5, (2.7, 0.6, 4.3), (0, 1.5, 1.2), 56)
    return p


# ================================================================ シェリフ：早撃ち（短筒で一発）

def sheriff():
    p = base("sheriff", 2.8)
    g = hand_group(p, "killer", "r", "gun")
    p.cube(g, "grip", [5.3, 9.5, -0.9], [6.7, 12.6, 0.9], "dark_oak_planks")
    p.cube(g, "barrel", [5.5, 2.5, -2.4], [6.5, 11.0, -1.4], "iron_block")
    p.cube(g, "band1", [5.35, 4.0, -2.55], [6.65, 4.6, -1.25], "gold_block")
    p.cube(g, "band2", [5.35, 7.5, -2.55], [6.65, 8.1, -1.25], "gold_block")
    p.cube(g, "stock", [5.4, 8.0, -1.5], [6.6, 11.5, -0.8], "dark_oak_planks")
    fl = p.group("muzzle_flash", [6, 2.5, -1.9], "gun")
    p.cube(fl, "flash", [4.5, 0.0, -3.4], [7.5, 2.5, -0.4], "glow_orange_concrete")
    p.cube(fl, "flash2", [5.2, -1.5, -2.6], [6.8, 0.2, -1.2], "glow_yellow_concrete")
    show(p, "muzzle_flash", 0.0, False)
    show(p, "muzzle_flash", 0.85, True)
    show(p, "muzzle_flash", 0.97, False)
    smoke = p.group("smoke", [6, 2.5, -1.9], "gun")
    for k in range(3):
        p.cube(smoke, f"puff{k}", [5.2 - k * 0.3, -1 - k * 2.2, -2.6 - k * 0.2], [6.8 + k * 0.3, 0.6 - k * 2.2, -1.2 + k * 0.2],
               "glass_white_concrete")
    show(p, "smoke", 0.0, False)
    show(p, "smoke", 1.7, True)

    KX, VX = -2.4, 1.2
    # シェリフ：腰に手 → 早撃ち → 銃を上に向けて煙をふっと吹く
    K(p, "killer", 0.0, at=(KX, 0, 0), root=(0, 90, 0), head=(5, 0, 0), ra=(-10, 0, 12), la=(-10, 0, -12),
      rl=(0, 0, 6), ll=(0, 0, -6))
    K(p, "killer", 0.55, at=(KX, 0, 0), root=(4, 90, 0), head=(8, 0, 0), ra=(-20, 0, 18), la=(-15, 0, -12))
    K(p, "killer", 0.72, ra=(-92, -8, 0), root=(2, 90, 0), head=(0, -5, 0), i="linear")
    K(p, "killer", 0.85, ra=(-92, -8, 0), la=(-30, 0, -10))
    K(p, "killer", 0.92, ra=(-120, -8, 0), root=(-4, 90, 0))
    K(p, "killer", 1.2, ra=(-95, -8, 0), root=(0, 90, 0))
    K(p, "killer", 1.6, ra=(-150, 0, -25), la=(-10, 0, -10), head=(-10, 0, 0))
    K(p, "killer", 2.8, at=(KX, 0, 0), root=(0, 90, 0), ra=(-152, 0, -28), head=(-12, -10, 0))
    # 自分：撃たれて後ろへ倒れる
    K(p, "victim", 0.0, at=(VX, 0, 0), root=(0, -90, 0), head=(0, 0, 0), ra=(-15, 0, 10), la=(-15, 0, -10), rl=(0, 0, 0), ll=(0, 0, 0))
    K(p, "victim", 0.7, at=(VX, 0, 0), root=(5, -90, 0), head=(5, 0, 0), ra=(-60, 0, 10), la=(-60, 0, -10))
    K(p, "victim", 0.85, at=(VX, 0, 0), root=(5, -90, 0), head=(5, 0, 0), ra=(-60, 0, 10), la=(-60, 0, -10))
    K(p, "victim", 0.95, at=(VX + 0.2, 0.1, 0), root=(-25, -90, 0), head=(-30, 0, 0), ra=(-150, 0, 40), la=(-150, 0, -40),
      rl=(-25, 0, 0), ll=(0, 0, 0))
    K(p, "victim", 1.25, at=(VX + 0.55, 0.13, 0), root=(-90, -90, 0), head=(-5, 0, 0), ra=(-175, 0, 60), la=(-175, 0, -60),
      rl=(-10, 0, 8), ll=(-5, 0, -8))
    K(p, "victim", 2.8, at=(VX + 0.55, 0.13, 0), root=(-90, -90, 0), head=(0, 30, 0), ra=(-178, 0, 72), la=(-178, 0, -72),
      rl=(-3, 0, 8), ll=(-3, 0, -8))
    p.timeline(0.85, "hit")
    p.timeline(0.86, "text:バーン！！:screen:FFE08A:3.4")
    p.timeline(1.75, "text:フッ…:killer_head:FFFFFF:1.8")
    p.sound(0.1, "minecraft:entity.phantom.flap", 0.5)
    p.sound(0.7, "minecraft:item.armor.equip_iron", 1.4)
    p.sound(0.85, "minecraft:entity.generic.explode", 1.8)
    p.sound(0.85, "minecraft:entity.firework_rocket.blast", 0.6)
    p.sound(0.9, "minecraft:entity.player.hurt", 0.8)
    p.sound(1.25, "minecraft:entity.player.big_fall", 0.8)
    p.sound(1.75, "minecraft:block.fire.extinguish", 1.6)

    cam(p, 0.0, (KX - 1.0, 1.75, 1.3), (VX, 1.2, 0), 48)                # 肩ごしに相手を見る
    cam(p, 0.6, (KX - 0.8, 1.75, 1.1), (VX, 1.25, 0), 42)
    cut(p, 0.65, (KX + 1.2, 1.5, 1.1), (KX + 0.6, 1.35, 0), 40)         # 早撃ち
    cam(p, 0.88, (KX + 1.3, 1.45, 1.0), (KX + 0.8, 1.4, 0), 44)
    cut(p, 0.9, (VX - 0.6, 1.1, 3.0), (VX + 0.6, 0.8, 0), 52)           # 倒れる
    cam(p, 1.5, (VX - 0.4, 1.3, 2.7), (VX + 0.9, 0.4, 0), 50)
    cut(p, 1.55, (KX + 2.4, 0.7, 2.8), (KX + 0.3, 1.5, 0), 55)          # 下から決め
    cam(p, 2.8, (KX + 2.1, 0.65, 2.5), (KX + 0.2, 1.6, 0), 52)
    return p


SCENES = {"wolf": wolf, "shinobi": shinobi, "kairiki": kairiki, "juro": juro, "pingpong": pingpong,
          "pillow": pillow, "fusuma": fusuma, "seoi": seoi, "neko": neko, "sheriff": sheriff}


def main():
    args = [a for a in sys.argv[1:] if not a.startswith("--")]
    os.makedirs(OUT, exist_ok=True)
    for name in args or list(SCENES):
        p = SCENES[name]()
        p.save(os.path.join(OUT, name + ".bbmodel"))
        print("書き出し:", name, f"{p.length:.2f}s")
        if "--preview" in sys.argv:
            from bb_preview import preview
            d = os.path.join(ROOT, "build/finisher_preview")
            os.makedirs(d, exist_ok=True)
            n = 16
            preview(p, [p.length * k / (n - 1) for k in range(n)], os.path.join(d, name + ".png"))


if __name__ == "__main__":
    main()
