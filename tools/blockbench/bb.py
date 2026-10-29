"""
民泊人狼：フィニッシャーの Blockbench プロジェクト（.bbmodel）を作る道具。

  python3 tools/blockbench/make_finishers.py   → src/main/resources/assets/minpaku/finishers/*.bbmodel

座標・回転の決まり（Blockbench の画面とゲームで同じ）：
  - 1ピクセル = 1/16 ブロック。Y が上。人は最初 -Z（北）を向いている。
  - キーフレームの回転（Blockbench の数値）：
      腕・足  x = -90 で前へ上がる、-180 で真上
      頭      x = +  で下を向く、- で上を向く
      体全体  x = +  で前へ倒れる、- で後ろへ倒れる
      向き    y = 0 で -Z、90 で +X、180 で +Z、-90 で -X を向く
  - キーフレームの位置：place(x, y, z)（ブロック単位の世界座標）を使う（Blockbench では x が逆向きに入る）
"""
import base64
import io
import json
import math
import uuid as _uuid

import numpy as np
from PIL import Image


def uid():
    return str(_uuid.uuid4())


# ------------------------------------------------------------------ テクスチャ

MATERIAL_COLORS = {
    "white_wool": (234, 236, 236), "light_blue_wool": (58, 175, 217), "blue_wool": (53, 57, 157), "red_wool": (161, 39, 34),
    "pink_wool": (237, 141, 172), "green_wool": (84, 109, 27), "yellow_wool": (248, 197, 39), "black_wool": (20, 21, 25),
    "white_concrete": (207, 213, 214), "red_concrete": (142, 32, 32), "green_concrete": (73, 91, 36), "black_concrete": (8, 10, 15),
    "gray_concrete": (54, 57, 61), "orange_concrete": (224, 97, 0), "yellow_concrete": (240, 175, 21),
    "dark_oak_planks": (66, 43, 20), "oak_planks": (162, 130, 78), "spruce_planks": (114, 84, 48), "birch_planks": (192, 175, 121),
    "bamboo_block": (127, 144, 58), "iron_block": (220, 220, 220), "gold_block": (246, 208, 61),
    "light_blue_terracotta": (113, 108, 137), "green_terracotta": (76, 83, 42), "soul_fire_0": (80, 220, 230),
    "glow_soul_fire_0": (80, 220, 230), "glow_shroomlight": (240, 146, 70), "white_terracotta": (209, 178, 161),
    "purple_wool": (122, 42, 172), "glow_yellow_concrete": (240, 220, 40), "glow_purple_concrete": (180, 90, 255),
    "magenta_wool": (189, 68, 179), "red_concrete_powder": (168, 54, 50), "brown_wool": (114, 71, 40),
    "white_wool_": (234, 236, 236), "glow_white_concrete": (240, 240, 240), "glow_red_concrete": (230, 30, 30),
    "light_gray_concrete": (125, 125, 115), "stripped_spruce_log": (115, 89, 52), "calcite": (223, 224, 220),
    "paper": (240, 232, 200), "birch_log": (216, 215, 210), "cyan_concrete": (21, 119, 136), "lime_concrete": (94, 168, 24),
    "orange_wool": (240, 118, 19), "light_gray_wool": (142, 142, 134), "glow_light_blue_concrete": (120, 210, 255),
    "glow_cyan_concrete": (80, 230, 230), "glow_pink_concrete": (255, 130, 200), "glow_orange_concrete": (255, 150, 40),
    "terracotta": (152, 94, 67), "smooth_stone": (158, 158, 158), "quartz_block_side": (235, 229, 222),
    "glow_magenta_concrete": (220, 80, 220), "glow_yellow_concrete_": (240, 220, 40), "mud_bricks": (137, 103, 79),
    "packed_mud": (142, 106, 79), "bamboo_planks": (194, 173, 80), "dried_kelp_side": (50, 59, 37),
}


def png_data(img):
    b = io.BytesIO()
    img.save(b, "PNG")
    return "data:image/png;base64," + base64.b64encode(b.getvalue()).decode()


def material_texture(name):
    c = MATERIAL_COLORS.get(name, (200, 200, 200))
    img = Image.new("RGBA", (16, 16), c + (255,))
    px = img.load()
    for y in range(16):
        for x in range(16):
            if (x * 7 + y * 13) % 11 == 0:
                px[x, y] = tuple(max(0, v - 18) for v in c) + (255,)
    return img


def placeholder_skin(shirt, pants, hair):
    """Blockbench で見るための仮のスキン（ゲームでは本物のスキンに差しかわる）"""
    img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    d = img.load()

    def rect(x0, y0, x1, y1, c):
        for y in range(y0, y1):
            for x in range(x0, x1):
                d[x, y] = c + (255,)

    skin = (232, 182, 140)
    rect(0, 0, 32, 16, skin)
    rect(8, 0, 16, 8, hair)       # 頭の上
    rect(0, 8, 32, 10, hair)      # 髪
    rect(8, 8, 16, 16, skin)      # 顔
    rect(9, 11, 11, 13, (30, 30, 60))
    rect(13, 11, 15, 13, (30, 30, 60))
    rect(10, 14, 14, 15, (150, 80, 70))
    rect(16, 16, 40, 32, shirt)   # 胴
    rect(40, 16, 56, 32, shirt)   # 右腕
    rect(40, 28, 56, 32, skin)
    rect(32, 48, 48, 64, shirt)   # 左腕
    rect(32, 60, 48, 64, skin)
    rect(0, 16, 16, 32, pants)    # 右足
    rect(16, 48, 32, 64, pants)   # 左足
    rect(0, 28, 16, 32, (60, 50, 40))
    rect(16, 60, 32, 64, (60, 50, 40))
    return img


# ------------------------------------------------------------------ モデル（グループと立方体）

class Group:
    def __init__(self, name, origin, parent=None):
        self.name = name
        self.origin = list(origin)
        self.uuid = uid()
        self.children = []
        self.cubes = []
        self.parent = parent
        if parent:
            parent.children.append(self)


class Project:
    def __init__(self, name):
        self.name = name
        self.textures = []          # (name, PIL image)
        self.tex_index = {}
        self.roots = []
        self.groups = {}
        self.elements = []
        self.tracks = {}            # bone -> channel -> [(time, [x,y,z], interp)]
        self.effects = []           # (time, channel, value)
        self.length = 4.0
        self.add_texture("killer_skin", placeholder_skin((170, 40, 40), (40, 40, 60), (40, 25, 15)))
        self.add_texture("victim_skin", placeholder_skin((50, 110, 200), (60, 60, 70), (90, 60, 30)))

    def add_texture(self, name, img):
        if name in self.tex_index:
            return self.tex_index[name]
        self.tex_index[name] = len(self.textures)
        self.textures.append((name, img))
        return self.tex_index[name]

    def mat(self, name):
        if name not in self.tex_index:
            self.add_texture(name, material_texture(name))
        return self.tex_index[name]

    def group(self, name, origin, parent=None):
        g = Group(name, origin, self.groups.get(parent) if isinstance(parent, str) else parent)
        if g.parent is None:
            self.roots.append(g)
        self.groups[name] = g
        return g

    def cube(self, group, name, frm, to, texture, box_uv=None, inflate=0.0, rotation=None, pivot=None):
        """立方体。frm/to はピクセル（Blockbench の座標）。box_uv=(u,v) ならスキンの箱 UV。
        rotation=(x,y,z) 度・pivot=回転の中心（なければ立方体の真ん中）"""
        g = self.groups[group] if isinstance(group, str) else group
        if isinstance(texture, str):
            texture = self.mat(texture)
        if pivot is None:
            pivot = [(a + b) / 2 for a, b in zip(frm, to)] if rotation else g.origin
        e = {"name": name, "from": list(map(float, frm)), "to": list(map(float, to)), "texture": texture,
             "box_uv": box_uv, "inflate": inflate, "uuid": uid(), "origin": [float(v) for v in pivot],
             "rotation": [float(v) for v in (rotation or (0, 0, 0))]}
        self.elements.append(e)
        g.cubes.append(e)
        return e

    # ---- 人（犯人・自分）
    def actor(self, prefix):
        skin = self.tex_index[prefix + "_skin"]
        root = self.group(prefix, [0, 0, 0])
        body = self.group(prefix + "_body", [0, 24, 0], root)
        self.cube(body, "body", [-4, 12, -2], [4, 24, 2], skin, (16, 16))
        self.cube(body, "jacket", [-4, 12, -2], [4, 24, 2], skin, (16, 32), 0.25)
        head = self.group(prefix + "_head", [0, 24, 0], root)
        self.cube(head, "head", [-4, 24, -4], [4, 32, 4], skin, (0, 0))
        self.cube(head, "hat", [-4, 24, -4], [4, 32, 4], skin, (32, 0), 0.5)
        ra = self.group(prefix + "_right_arm", [5, 22, 0], root)
        self.cube(ra, "arm#wide", [4, 12, -2], [8, 24, 2], skin, (40, 16))
        self.cube(ra, "sleeve#wide", [4, 12, -2], [8, 24, 2], skin, (40, 32), 0.25)
        self.cube(ra, "arm#slim", [4, 11.5, -2], [7, 23.5, 2], skin, (40, 16))
        self.cube(ra, "sleeve#slim", [4, 11.5, -2], [7, 23.5, 2], skin, (40, 32), 0.25)
        la = self.group(prefix + "_left_arm", [-5, 22, 0], root)
        self.cube(la, "arm#wide", [-8, 12, -2], [-4, 24, 2], skin, (32, 48))
        self.cube(la, "sleeve#wide", [-8, 12, -2], [-4, 24, 2], skin, (48, 48), 0.25)
        self.cube(la, "arm#slim", [-7, 11.5, -2], [-4, 23.5, 2], skin, (32, 48))
        self.cube(la, "sleeve#slim", [-7, 11.5, -2], [-4, 23.5, 2], skin, (48, 48), 0.25)
        rl = self.group(prefix + "_right_leg", [1.9, 12, 0], root)
        self.cube(rl, "leg", [-0.1, 0, -2], [3.9, 12, 2], skin, (0, 16))
        self.cube(rl, "pants", [-0.1, 0, -2], [3.9, 12, 2], skin, (0, 32), 0.25)
        ll = self.group(prefix + "_left_leg", [-1.9, 12, 0], root)
        self.cube(ll, "leg", [-3.9, 0, -2], [0.1, 12, 2], skin, (16, 48))
        self.cube(ll, "pants", [-3.9, 0, -2], [0.1, 12, 2], skin, (0, 48), 0.25)
        return root

    def camera(self):
        self.group("camera", [0, 0, 0])
        self.group("camera_target", [0, 0, 0])
        r = self.group("ref_camera_mark", [0, 0, 0], "camera")
        self.cube(r, "mark", [-1, -1, -1], [1, 1, 1], self.mat("yellow_concrete"))
        r2 = self.group("ref_target_mark", [0, 0, 0], "camera_target")
        self.cube(r2, "mark", [-0.5, -0.5, -0.5], [0.5, 0.5, 0.5], self.mat("red_concrete"))
        # 床の目印（Blockbench で見るときだけ）
        f = self.group("ref_floor", [0, 0, 0])
        self.cube(f, "floor", [-64, -1, -64], [64, 0, 64], self.mat("bamboo_block"))

    # ---- キーフレーム
    def key(self, bone, channel, t, xyz, interp="catmullrom"):
        self.tracks.setdefault(bone, {}).setdefault(channel, [])
        lst = self.tracks[bone][channel]
        lst[:] = [k for k in lst if abs(k[0] - t) > 1e-4]
        lst.append((round(t, 4), [round(float(v), 4) for v in xyz], interp))
        lst.sort(key=lambda k: k[0])

    def rot(self, bone, t, x=0, y=0, z=0, interp="catmullrom"):
        self.key(bone, "rotation", t, [x, y, z], interp)

    def pos(self, bone, t, x=0, y=0, z=0, interp="catmullrom"):
        """位置（ブロック単位の世界座標の“ずらす量”）。Blockbench には x を逆にして入れる"""
        self.key(bone, "position", t, [-x * 16, y * 16, z * 16], interp)

    def scale(self, bone, t, s, interp="catmullrom"):
        self.key(bone, "scale", t, [s, s, s], interp)

    def sound(self, t, name, pitch=1.0):
        self.effects.append((t, "sound", f"{name}|{pitch}"))

    def timeline(self, t, script):
        self.effects.append((t, "timeline", script))

    # ------------------------------------------------------------------ 書き出し
    def face_uvs(self, e):
        """Blockbench の箱 UV と同じ面の UV（box_uv のとき）／ほかは全面"""
        f, t = e["from"], e["to"]
        w, h, d = t[0] - f[0], t[1] - f[1], t[2] - f[2]
        if e["box_uv"]:
            u, v = e["box_uv"]
            return {
                "north": [u + d, v + d, u + d + w, v + d + h],
                "east": [u, v + d, u + d, v + d + h],
                "south": [u + d + w + d, v + d, u + d + w + d + w, v + d + h],
                "west": [u + d + w, v + d, u + d + w + d, v + d + h],
                "up": [u + d + w, v + d, u + d, v],
                "down": [u + d + w + w, v, u + d + w, v + d],
            }
        return {k: [0, 0, 16, 16] for k in ["north", "east", "south", "west", "up", "down"]}

    def to_json(self):
        elements = []
        for e in self.elements:
            uvs = self.face_uvs(e)
            el = {"name": e["name"], "box_uv": bool(e["box_uv"]), "rescale": False, "locked": False, "render_order": "default",
                  "allow_mirror_modeling": True, "from": e["from"], "to": e["to"], "autouv": 0, "color": 0,
                  "inflate": e["inflate"], "origin": e["origin"], "rotation": e["rotation"], "faces": {k: {"uv": v, "texture": e["texture"]} for k, v in uvs.items()},
                  "type": "cube", "uuid": e["uuid"]}
            if e["box_uv"]:
                el["uv_offset"] = list(e["box_uv"])
            elements.append(el)

        def outl(g):
            return {"name": g.name, "origin": g.origin, "color": 0, "uuid": g.uuid, "export": True, "mirror_uv": False,
                    "isOpen": False, "locked": False, "visibility": True, "autouv": 0,
                    "children": [c["uuid"] for c in g.cubes] + [outl(c) for c in g.children]}

        textures = []
        for i, (name, img) in enumerate(self.textures):
            textures.append({"path": "", "name": name + ".png", "folder": "", "namespace": "", "id": str(i), "group": "",
                             "width": img.width, "height": img.height, "uv_width": 64 if "skin" in name else 16,
                             "uv_height": 64 if "skin" in name else 16, "particle": False, "use_as_default": False,
                             "layers_enabled": False, "sync_to_project": "", "render_mode": "default", "render_sides": "auto",
                             "frame_time": 1, "frame_order_type": "loop", "frame_order": "", "frame_interpolate": False,
                             "visible": True, "internal": True, "saved": True, "uuid": uid(), "source": png_data(img)})
        animators = {}
        for bone, chans in self.tracks.items():
            g = self.groups.get(bone)
            if g is None:
                continue
            kfs = []
            for ch, keys in chans.items():
                for (t, xyz, interp) in keys:
                    kfs.append({"channel": ch, "data_points": [{"x": xyz[0], "y": xyz[1], "z": xyz[2]}], "uuid": uid(),
                                "time": t, "color": -1, "interpolation": interp})
            animators[g.uuid] = {"name": bone, "type": "bone", "keyframes": kfs}
        eff = []
        for (t, ch, v) in sorted(self.effects):
            dp = {"effect": v, "file": ""} if ch == "sound" else {"script": v}
            eff.append({"channel": ch, "data_points": [dp], "uuid": uid(), "time": t, "color": -1, "interpolation": "linear"})
        animators["effects"] = {"name": "Effects", "type": "effect", "keyframes": eff}
        return {
            "meta": {"format_version": "4.10", "model_format": "free", "box_uv": True},
            "name": self.name, "model_identifier": "", "visible_box": [1, 1, 0], "variable_placeholders": "",
            "variable_placeholder_buttons": [], "timeline_setups": [], "unhandled_root_fields": {},
            "resolution": {"width": 64, "height": 64},
            "elements": elements,
            "outliner": [outl(g) for g in self.roots],
            "textures": textures,
            "animations": [{"uuid": uid(), "name": "animation.finisher." + self.name, "loop": "once", "override": False,
                            "length": self.length, "snapping": 20, "selected": True, "anim_time_update": "",
                            "blend_weight": "", "start_delay": "", "loop_delay": "", "animators": animators}],
        }

    def save(self, path):
        with open(path, "w", encoding="utf-8") as f:
            json.dump(self.to_json(), f, ensure_ascii=False, separators=(",", ":"))

    # ------------------------------------------------------------------ 計算（ゲームと同じ）
    def sample(self, bone, channel, t, default):
        keys = self.tracks.get(bone, {}).get(channel)
        if not keys:
            return np.array([default] * 3, dtype=float)
        if t <= keys[0][0]:
            return np.array(keys[0][1], dtype=float)
        if t >= keys[-1][0]:
            return np.array(keys[-1][1], dtype=float)
        i = 0
        while i < len(keys) - 2 and t >= keys[i + 1][0]:
            i += 1
        a, b = keys[i], keys[i + 1]
        alpha = (t - a[0]) / max(1e-5, b[0] - a[0])
        if a[2] == "step":
            return np.array(a[1], dtype=float)
        if a[2] == "catmullrom" or b[2] == "catmullrom":
            bp = keys[i - 1] if i > 0 else None
            ap = keys[i + 2] if i + 2 < len(keys) else None
            out = []
            for ax in range(3):
                pts = ([(bp[0], bp[1][ax])] if bp else []) + [(a[0], a[1][ax]), (b[0], b[1][ax])] + ([(ap[0], ap[1][ax])] if ap else [])
                tt = (alpha + (1 if bp else 0)) / (len(pts) - 1)
                p = (len(pts) - 1) * tt
                ip = int(math.floor(p))
                w = p - ip
                n = len(pts)
                p0 = pts[ip if ip == 0 else ip - 1][1]
                p1 = pts[min(ip, n - 1)][1]
                p2 = pts[min(ip + 1, n - 1)][1]
                p3 = pts[min(ip + 2, n - 1)][1]
                v0 = (p2 - p0) * 0.5
                v1 = (p3 - p1) * 0.5
                out.append((2 * p1 - 2 * p2 + v0 + v1) * w ** 3 + (-3 * p1 + 3 * p2 - 2 * v0 - v1) * w ** 2 + v0 * w + p1)
            return np.array(out)
        return np.array(a[1], dtype=float) + (np.array(b[1], dtype=float) - np.array(a[1], dtype=float)) * alpha

    def world(self, t):
        """各グループの世界座標の行列（4x4、ブロック単位）"""
        out = {}

        def walk(g, parent, porigin):
            r = self.sample(g.name, "rotation", t, 0.0)
            p = self.sample(g.name, "position", t, 0.0)
            s = self.sample(g.name, "scale", t, 1.0)
            m = parent @ T((np.array(g.origin) - porigin) / 16.0) @ T(np.array([-p[0], p[1], p[2]]) / 16.0)
            m = m @ Rzyx(math.radians(r[2]), math.radians(-r[1]), math.radians(-r[0])) @ S(s)
            out[g.name] = m
            for c in g.children:
                walk(c, m, np.array(g.origin))

        for g in self.roots:
            walk(g, np.eye(4), np.zeros(3))
        return out

    def point(self, t, group, px, W=None):
        """グループ内の点（ピクセルの絶対座標）の世界座標"""
        W = W or self.world(t)
        g = self.groups[group]
        v = np.array([(px[0] - g.origin[0]) / 16, (px[1] - g.origin[1]) / 16, (px[2] - g.origin[2]) / 16, 1.0])
        return (W[group] @ v)[:3]


def T(v):
    m = np.eye(4)
    m[:3, 3] = v
    return m


def S(s):
    m = np.eye(4)
    m[0, 0], m[1, 1], m[2, 2] = s
    return m


def Rzyx(z, y, x):
    cz, sz, cy, sy, cx, sx = math.cos(z), math.sin(z), math.cos(y), math.sin(y), math.cos(x), math.sin(x)
    Rz = np.array([[cz, -sz, 0, 0], [sz, cz, 0, 0], [0, 0, 1, 0], [0, 0, 0, 1]])
    Ry = np.array([[cy, 0, sy, 0], [0, 1, 0, 0], [-sy, 0, cy, 0], [0, 0, 0, 1]])
    Rx = np.array([[1, 0, 0, 0], [0, cx, -sx, 0], [0, sx, cx, 0], [0, 0, 0, 1]])
    return Rz @ Ry @ Rx


# 手の先（ピクセルの絶対座標・腕のグループの中）
HAND_R = [6, 12, 0]
HAND_L = [-6, 12, 0]
HEAD_TOP = [0, 32, 0]
FACE = [0, 28, -4]
