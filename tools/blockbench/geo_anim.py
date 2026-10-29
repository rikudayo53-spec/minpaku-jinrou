"""
GeckoLib のアニメーション（.animation.json）を書く道具。

座標はマイクラの向き（furniture_hires.py と同じ：+X が東、頭の上が +Y、正面が北 = -Z）で書けば、
GeckoLib 用に左右反転して書き出す（モデルの書き出し hires.to_geo と同じ決まり）。

  a = Anims()
  use = a.add("use", 1.2)                       # 長さ 1.2 秒、1回だけ
  use.rot("bell", [(0, (0, 0, 0)), (0.15, (20, 0, 0), "easeOutQuad"), (1.2, (0, 0, 0), "easeOutElastic")])
  work = a.add("work", 0.8, loop=True)
  work.pos("lid", [(0, (0, 0, 0)), (0.4, (0, 1, 0), "easeInOutSine"), (0.8, (0, 0, 0), "easeInOutSine")])
  a.save("assets/minpaku/animations/block/kamado.animation.json")

rot は度、pos はピクセル、scale は倍率。いずれも「元の形からの変化」。
easing は GeckoLib の名前（linear, easeInSine, easeOutSine, easeInOutSine, easeOutQuad, easeInQuad,
easeInOutQuad, easeOutBack, easeInBack, easeOutElastic, easeOutBounce, catmullrom など）。
"""
import json
import math
import os


def _r(v):
    return round(float(v), 4)


class Clip:
    def __init__(self, name, length, loop=False, hold=False):
        self.name = name
        self.length = length
        self.loop = loop
        self.hold = hold
        self.bones = {}

    def _put(self, bone, channel, frames, conv):
        ch = self.bones.setdefault(bone, {}).setdefault(channel, {})
        for fr in frames:
            t = fr[0]
            v = conv(fr[1])
            ease = fr[2] if len(fr) > 2 else None
            key = f"{_r(t)}"
            if ease and ease != "linear":
                ch[key] = {"vector": [_r(x) for x in v], "easing": ease}
            else:
                ch[key] = [_r(x) for x in v]
        return self

    def rot(self, bone, frames):
        # GeckoLib は X・Y の回転を反転して読む
        return self._put(bone, "rotation", frames, lambda v: (-v[0], -v[1], v[2]))

    def pos(self, bone, frames):
        # 位置は X だけ反転
        return self._put(bone, "position", frames, lambda v: (-v[0], v[1], v[2]))

    def scale(self, bone, frames):
        return self._put(bone, "scale", frames, lambda v: (v, v, v) if isinstance(v, (int, float)) else v)

    # ---- よく使う動き
    def wobble(self, bone, axis, amp, period, n=None, ease="easeInOutSine", phase=0.0, base=(0, 0, 0)):
        """往復（ループ用）。axis = 0/1/2、amp 度、period 秒。長さ self.length を埋める"""
        n = n or max(2, int(round(self.length / period * 4)))
        frames = []
        for k in range(n + 1):
            t = self.length * k / n
            v = list(base)
            v[axis] += amp * math.sin(2 * math.pi * (t / period) + phase)
            frames.append((t, tuple(v), ease if k else "linear"))
        return self.rot(bone, frames)

    def damped(self, bone, axis, amp, freq, decay, steps=14, kind="rot"):
        """ゆれて止まる（減衰振動）。鐘など"""
        frames = []
        for k in range(steps + 1):
            t = self.length * k / steps
            v = [0.0, 0.0, 0.0]
            v[axis] = amp * math.sin(2 * math.pi * freq * t) * math.exp(-decay * t)
            frames.append((t, tuple(v), "catmullrom"))
        return self.rot(bone, frames) if kind == "rot" else self.pos(bone, frames)

    def to_json(self):
        d = {"animation_length": _r(self.length), "bones": self.bones}
        if self.loop:
            d["loop"] = True
        elif self.hold:
            d["loop"] = "hold_on_last_frame"
        return d


class Anims:
    def __init__(self):
        self.clips = {}

    def add(self, name, length, loop=False, hold=False):
        c = Clip(name, length, loop, hold)
        self.clips[name] = c
        return c

    def to_json(self):
        return {"format_version": "1.8.0", "animations": {n: c.to_json() for n, c in self.clips.items()},
                "geckolib_format_version": 2}

    def save(self, path):
        os.makedirs(os.path.dirname(path), exist_ok=True)
        with open(path, "w", encoding="utf-8") as f:
            json.dump(self.to_json(), f, ensure_ascii=False, separators=(",", ":"))
