"""
プレイヤーの体のアニメーション（playerAnimator の emote 形式 .json）を書く。
書き出し先：src/main/resources/assets/minpaku/player_animations/<名前>.json

角度は度。部位：head / torso（上半身ごと）/ body / rightArm / leftArm / rightLeg / leftLeg
  pitch … 前後（腕なら −90 で前にまっすぐ、−180 で真上。torso・head は + で前かがみ・下を向く）
  yaw   … 左右に回す
  roll  … 横に倒す
  bend  … ひじ・ひざを曲げる
easing はそのキーフレームに「着くまで」の動き方（INOUTSINE, OUTQUAD, INQUAD, OUTBACK, OUTBOUNCE …）

  python3 tools/blockbench/player_anims.py
"""
import json
import os

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
OUT = os.path.join(ROOT, "src/main/resources/assets/minpaku/player_animations")

ANIMS = {}


class A:
    def __init__(self, name, length, loop=False, ret=0):
        self.name = name
        self.length = length
        self.loop = loop
        self.ret = ret
        self.moves = []
        ANIMS[name] = self

    def key(self, tick, ease="INOUTSINE", **parts):
        """key(4, rightArm=dict(pitch=-90), torso=dict(pitch=20))"""
        for part, vals in parts.items():
            self.moves.append({"tick": int(tick), "easing": ease, part: {k: float(v) for k, v in vals.items()}})
        return self

    def to_json(self):
        emote = {"beginTick": 0, "endTick": self.length, "stopTick": self.length + (0 if self.loop else 4),
                 "degrees": True, "easeBeforeKeyframe": True, "moves": self.moves}
        if self.loop:
            emote["isLoop"] = True
            emote["returnTick"] = self.ret
        return {"version": 3, "name": self.name, "author": "minpaku", "description": "民泊人狼", "emote": emote}


Z = dict(pitch=0, yaw=0, roll=0)


def neutral(a, t, ease="INOUTSINE", parts=("head", "torso", "rightArm", "leftArm")):
    a.key(t, ease, **{p: dict(Z, bend=0) if "Arm" in p else dict(Z) for p in parts})


# ---------------------------------------------------------------- タスク（ブロックを見ているあいだループ）

def stir():
    a = A("stir", 16, loop=True)
    a.key(0, torso=dict(pitch=14), head=dict(pitch=24), leftArm=dict(pitch=-42, yaw=18, bend=30))
    for t, (p, y) in zip((0, 4, 8, 12, 16), ((-55, -12), (-63, 0), (-55, 12), (-47, 0), (-55, -12))):
        a.key(t, rightArm=dict(pitch=p, yaw=y, bend=42))


def sweep():
    a = A("sweep", 20, loop=True)
    a.key(0, head=dict(pitch=30))
    for t, s in ((0, -1), (10, 1), (20, -1)):
        a.key(t, torso=dict(pitch=20, yaw=14 * s), rightArm=dict(pitch=-48, yaw=22 * s, bend=18),
              leftArm=dict(pitch=-42, yaw=22 * s, bend=24))


def scrub():
    a = A("scrub", 10, loop=True)
    a.key(0, torso=dict(pitch=30), head=dict(pitch=34))
    for t, s in ((0, 1), (5, -1), (10, 1)):
        a.key(t, rightArm=dict(pitch=-45 - 8 * s, bend=32), leftArm=dict(pitch=-45 + 8 * s, bend=32))


def turn_valve():
    a = A("turn_valve", 16, loop=True)
    a.key(0, head=dict(pitch=14))
    for t, s in ((0, 1), (8, -1), (16, 1)):
        a.key(t, rightArm=dict(pitch=-76, roll=18 * s, bend=20), leftArm=dict(pitch=-74, roll=18 * s, bend=20))


def reach_work():
    a = A("reach_work", 20, loop=True)
    a.key(0, head=dict(pitch=10))
    for t, s in ((0, 1), (5, -1), (10, 1), (15, -1), (20, 1)):
        a.key(t, rightArm=dict(pitch=-80 + 7 * s, bend=26), leftArm=dict(pitch=-80 - 7 * s, bend=26))


def hang_up():
    a = A("hang_up", 30, loop=True)
    a.key(0, head=dict(pitch=-22))
    for t, s in ((0, 1), (15, -1), (30, 1)):
        a.key(t, rightArm=dict(pitch=-150 + 10 * s, roll=-6, bend=12), leftArm=dict(pitch=-150 - 10 * s, roll=6, bend=12))


def kneel_work():
    a = A("kneel_work", 24, loop=True)
    a.key(0, torso=dict(pitch=34), head=dict(pitch=38))
    for t, s in ((0, 1), (12, -1), (24, 1)):
        a.key(t, rightArm=dict(pitch=-44 + 6 * s, bend=36), leftArm=dict(pitch=-44 - 6 * s, bend=36))


# ---------------------------------------------------------------- タスク（クリックのたびに1回）

def chop():
    a = A("chop", 12)
    a.key(0, rightArm=dict(pitch=-30, bend=10), leftArm=dict(pitch=-30, bend=10))
    a.key(4, "OUTQUAD", rightArm=dict(pitch=-172, bend=30), leftArm=dict(pitch=-168, bend=30), torso=dict(pitch=-10))
    a.key(7, "INQUAD", rightArm=dict(pitch=-38, bend=0), leftArm=dict(pitch=-36, bend=0), torso=dict(pitch=26),
          head=dict(pitch=30))
    neutral(a, 12, "OUTSINE")


def pull_up():
    a = A("pull_up", 14)
    a.key(4, "OUTQUAD", torso=dict(pitch=42), head=dict(pitch=40), rightArm=dict(pitch=-62, bend=10),
          leftArm=dict(pitch=-58, bend=10))
    a.key(9, "OUTBACK", torso=dict(pitch=4), head=dict(pitch=-6), rightArm=dict(pitch=-118, bend=40),
          leftArm=dict(pitch=-112, bend=40))
    neutral(a, 14)


def sweep_once():
    a = A("sweep_once", 12)
    a.key(2, torso=dict(pitch=20, yaw=-16), rightArm=dict(pitch=-48, yaw=-26, bend=18), leftArm=dict(pitch=-42, yaw=-26, bend=24),
          head=dict(pitch=28))
    a.key(7, "OUTQUAD", torso=dict(pitch=20, yaw=16), rightArm=dict(pitch=-48, yaw=26, bend=18),
          leftArm=dict(pitch=-42, yaw=26, bend=24))
    neutral(a, 12)


def pour():
    a = A("pour", 16)
    a.key(4, "OUTQUAD", rightArm=dict(pitch=-72, roll=-8, bend=20), head=dict(pitch=18))
    a.key(7, rightArm=dict(pitch=-70, roll=-30, bend=20))
    a.key(11, rightArm=dict(pitch=-70, roll=-30, bend=20))
    neutral(a, 16)


def crouch_tap():
    a = A("crouch_tap", 12)
    a.key(4, "OUTQUAD", torso=dict(pitch=36), head=dict(pitch=40), rightArm=dict(pitch=-52, bend=20),
          leftArm=dict(pitch=-48, bend=20))
    a.key(7, rightArm=dict(pitch=-40, bend=30))
    neutral(a, 12)


def snip():
    a = A("snip", 8)
    a.key(2, "OUTQUAD", rightArm=dict(pitch=-70, bend=40), head=dict(pitch=14))
    a.key(4, "INQUAD", rightArm=dict(pitch=-72, bend=8))
    a.key(6, "OUTQUAD", rightArm=dict(pitch=-70, bend=40))
    neutral(a, 8)


def pull_rope():
    a = A("pull_rope", 16)
    a.key(4, "OUTQUAD", rightArm=dict(pitch=-124, bend=20), leftArm=dict(pitch=-110, bend=20), head=dict(pitch=-10))
    a.key(10, "INOUTQUAD", rightArm=dict(pitch=-42, bend=60), leftArm=dict(pitch=-36, bend=60), torso=dict(pitch=10))
    neutral(a, 16)


def tap():
    a = A("tap", 8)
    a.key(3, "OUTQUAD", rightArm=dict(pitch=-52, bend=30), head=dict(pitch=24))
    a.key(5, "INQUAD", rightArm=dict(pitch=-36, bend=20))
    neutral(a, 8)


def reach_tap():
    a = A("reach_tap", 10)
    a.key(4, "OUTQUAD", rightArm=dict(pitch=-102, bend=10))
    a.key(6, rightArm=dict(pitch=-88, bend=20))
    neutral(a, 10)


def sprinkle():
    a = A("sprinkle", 12)
    a.key(3, "OUTQUAD", rightArm=dict(pitch=-62, roll=-12, bend=20), head=dict(pitch=24))
    a.key(6, rightArm=dict(pitch=-62, roll=12, bend=20))
    a.key(9, rightArm=dict(pitch=-62, roll=-12, bend=20))
    neutral(a, 12)


def reach_up():
    a = A("reach_up", 14)
    a.key(5, "OUTBACK", rightArm=dict(pitch=-162, bend=10), leftArm=dict(pitch=-150, bend=10), head=dict(pitch=-24))
    a.key(9, rightArm=dict(pitch=-158, bend=10), leftArm=dict(pitch=-146, bend=10))
    neutral(a, 14)


# ---------------------------------------------------------------- そのほかの動き

def ring_bell():
    a = A("ring_bell", 22)
    a.key(4, "OUTQUAD", rightArm=dict(pitch=-150, bend=20), head=dict(pitch=-14))
    for t, s in ((7, 1), (10, -1), (13, 1), (16, -1)):
        a.key(t, rightArm=dict(pitch=-150, yaw=16 * s, bend=20))
    neutral(a, 22)


def kill_pounce():
    a = A("kill_pounce", 16)
    a.key(3, "OUTQUAD", torso=dict(pitch=22), head=dict(pitch=10), rightArm=dict(pitch=-172, roll=-12, bend=30),
          leftArm=dict(pitch=-168, roll=12, bend=30))
    a.key(6, "INQUAD", torso=dict(pitch=40), head=dict(pitch=34), rightArm=dict(pitch=-30, roll=-20, bend=10),
          leftArm=dict(pitch=-34, roll=20, bend=10))
    a.key(10, torso=dict(pitch=30), rightArm=dict(pitch=-60, bend=30), leftArm=dict(pitch=-60, bend=30))
    neutral(a, 16)


def shoot():
    a = A("shoot", 26)
    a.key(3, "OUTQUAD", rightArm=dict(pitch=-90, yaw=-4, bend=0), leftArm=dict(pitch=-86, yaw=34, bend=24))
    a.key(6, "OUTEXPO", rightArm=dict(pitch=-118, yaw=-4, bend=10), leftArm=dict(pitch=-108, yaw=34, bend=30),
          head=dict(pitch=-8))
    a.key(10, "OUTBACK", rightArm=dict(pitch=-90, yaw=-4, bend=0), leftArm=dict(pitch=-86, yaw=34, bend=24), head=dict(pitch=0))
    a.key(18, rightArm=dict(pitch=-88, yaw=-4, bend=0), leftArm=dict(pitch=-84, yaw=34, bend=24))
    neutral(a, 26)


def vent_dive():
    a = A("vent_dive", 10)
    a.key(4, "OUTQUAD", torso=dict(pitch=60), head=dict(pitch=50), rightArm=dict(pitch=-170), leftArm=dict(pitch=-170))
    a.key(10, torso=dict(pitch=70))


for _fn in (stir, sweep, scrub, turn_valve, reach_work, hang_up, kneel_work, chop, pull_up, sweep_once, pour, crouch_tap, snip,
            pull_rope, tap, reach_tap, sprinkle, reach_up, ring_bell, kill_pounce, shoot, vent_dive):
    _fn()


def main():
    os.makedirs(OUT, exist_ok=True)
    for name, a in ANIMS.items():
        with open(os.path.join(OUT, name + ".json"), "w", encoding="utf-8") as f:
            json.dump(a.to_json(), f, ensure_ascii=False, indent=1)
        print("書き出し:", name, f"{a.length} tick", "（ループ）" if a.loop else "")


if __name__ == "__main__":
    main()
