# ============================================================
#  民泊人狼 - 炎と水のメッシュ（Blender）
#
#    python3.11 tools/blender/fx_meshes.py      （bpy モジュールがあれば、Blender を開かずに書き出せる）
#    → src/main/resources/assets/minpaku/mesh/fx_*.json
#
#  ・1 Blender単位 = 1ブロック。原点は底の中心、+Z が上
#  ・家具（GeckoLib）の炎・水の箱のかわりに、Java（FurnitureFx）がこのメッシュを大きさを変えて描く
#    fx_flame       炎（高さ 1、根もとの半径 0.2 くらい）。外側・中・芯の3重
#    fx_water_disc  丸い水面（半径 0.5）。波紋つき
#    fx_water_rect  四角い水面（1 × 1）。さざ波つき
#    fx_stream      流れ落ちる水（高さ 1）
# ============================================================
import json
import math
import os

import bpy  # noqa: F401  （bmesh より先に）
import bmesh
from mathutils import Vector

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
OUT = os.path.join(ROOT, "src/main/resources/assets/minpaku/mesh")


def clear():
    for ob in list(bpy.data.objects):
        bpy.data.objects.remove(ob, do_unlink=True)


def lathe(profile, seg=24, twist=0.0, wobble=0.0, phase=0.0):
    """profile = [(高さ, 半径), ...]。twist：上ほどねじる、wobble：上ほどゆらぐ（炎の先）"""
    bm = bmesh.new()
    rings = []
    for (h, r) in profile:
        ring = []
        t = h
        for k in range(seg):
            a = 2 * math.pi * k / seg + twist * t
            rr = r * (1 + wobble * t * math.sin(3 * a + phase + t * 6))
            ring.append(bm.verts.new((rr * math.cos(a) + wobble * 0.15 * t * t * math.sin(phase + t * 4), rr * math.sin(a), h)))
        rings.append(ring)
    for r0, r1 in zip(rings, rings[1:]):
        for k in range(seg):
            bm.faces.new((r0[k], r0[(k + 1) % seg], r1[(k + 1) % seg], r1[k]))
    top = bm.verts.new((wobble * 0.15 * math.sin(phase + profile[-1][0] * 4), 0, profile[-1][0] + 0.02))
    for k in range(seg):
        bm.faces.new((rings[-1][k], rings[-1][(k + 1) % seg], top))
    bot = bm.verts.new((0, 0, profile[0][0]))
    for k in range(seg):
        bm.faces.new((rings[0][(k + 1) % seg], rings[0][k], bot))
    bmesh.ops.recalc_face_normals(bm, faces=bm.faces)
    return bm


def flame_profile(height, width, n=9):
    pts = []
    for i in range(n + 1):
        t = i / n
        # 根もとは丸く、まん中が太く、先は細くとがる
        r = width * (math.sin(math.pi * min(1.0, t * 1.15)) ** 0.7) * (1 - 0.55 * t)
        pts.append((t * height, max(0.004, r)))
    return pts


def part(name, color, alpha, emissive):
    return {"name": name, "color": color, "alpha": alpha, "emissive": emissive, "p": [], "n": []}


def add_bm(pt, bm, offset=(0, 0, 0), scale=(1, 1, 1)):
    bm.normal_update()
    bmesh.ops.triangulate(bm, faces=bm.faces)
    for f in bm.faces:
        for v in f.verts:
            co = Vector((v.co.x * scale[0] + offset[0], v.co.y * scale[1] + offset[1], v.co.z * scale[2] + offset[2]))
            n = v.normal
            # Blender（Z上・-Y前）→ マイクラ（Y上・-Z前）。x は反転
            pt["p"].extend([round(-co.x, 4), round(co.z, 4), round(co.y, 4)])
            pt["n"].extend([round(-n.x, 3), round(n.z, 3), round(n.y, 3)])
    bm.free()


def save(name, parts):
    out = {"format": 3, "parts": [{k: v for k, v in p.items() if k != "name"} for p in parts]}
    os.makedirs(OUT, exist_ok=True)
    with open(os.path.join(OUT, name + ".json"), "w", encoding="utf-8") as f:
        json.dump(out, f, separators=(",", ":"))
    tris = sum(len(p["p"]) // 9 for p in parts)
    print(f"書き出し: {name}  三角形 {tris}")


def flame():
    outer = part("outer", [1.0, 0.36, 0.08], 0.62, True)
    mid = part("mid", [1.0, 0.62, 0.16], 0.8, True)
    core = part("core", [1.0, 0.93, 0.62], 0.95, True)
    add_bm(outer, lathe(flame_profile(1.0, 0.2), seg=12, twist=0.9, wobble=0.35, phase=0.3))
    add_bm(mid, lathe(flame_profile(0.72, 0.15), seg=10, twist=-0.7, wobble=0.25, phase=1.7), offset=(0.01, 0, 0.01))
    add_bm(core, lathe(flame_profile(0.42, 0.09), seg=8, twist=0.4, wobble=0.1, phase=2.4), offset=(0, 0, 0.015))
    # わきの小さな炎
    for (ox, oy, h, ph) in ((0.11, 0.05, 0.5, 0.9), (-0.1, -0.06, 0.42, 2.2), (0.02, -0.12, 0.36, 4.0)):
        add_bm(outer, lathe(flame_profile(h, 0.09), seg=8, twist=1.2, wobble=0.4, phase=ph), offset=(ox, oy, 0))
    save("fx_flame", [outer, mid, core])


def water(kind):
    surf = part("surf", [0.32, 0.58, 0.82], 0.62, False)
    shine = part("shine", [0.85, 0.94, 1.0], 0.35, False)
    bm = bmesh.new()
    n = 14
    grid = {}
    for i in range(n + 1):
        for j in range(n + 1):
            x = i / n - 0.5
            y = j / n - 0.5
            if kind == "disc":
                # 正方形の格子を円に寄せる
                rr = max(abs(x), abs(y)) * 2
                d = math.hypot(x, y)
                if d > 1e-6:
                    x, y = x / d * rr * 0.5, y / d * rr * 0.5
                z = 0.006 * math.sin(math.hypot(x, y) * 40) * (1 - math.hypot(x, y) * 1.4)
            else:
                z = 0.005 * math.sin(x * 25 + y * 9) + 0.003 * math.sin(y * 31 - x * 7)
            grid[(i, j)] = bm.verts.new((x, y, z))
    for i in range(n):
        for j in range(n):
            bm.faces.new((grid[(i, j)], grid[(i + 1, j)], grid[(i + 1, j + 1)], grid[(i, j + 1)]))
    bmesh.ops.recalc_face_normals(bm, faces=bm.faces)
    for f in bm.faces:
        if f.normal.z < 0:
            f.normal_flip()
    add_bm(surf, bm)
    # 光の照り返し（細い帯を2本）
    for (cx, cy, w, l, a) in ((0.12, -0.1, 0.03, 0.22, 0.6), (-0.15, 0.12, 0.02, 0.14, 0.4)):
        b2 = bmesh.new()
        ca, sa = math.cos(a), math.sin(a)
        vs = []
        for (u, v) in ((-l, -w), (l, -w), (l, w), (-l, w)):
            vs.append(b2.verts.new((cx + u * ca - v * sa, cy + u * sa + v * ca, 0.012)))
        b2.faces.new(vs)
        add_bm(shine, b2)
    save("fx_water_" + kind, [surf, shine])


def stream():
    w = part("water", [0.6, 0.8, 1.0], 0.6, False)
    prof = [(t / 10, 0.045 + 0.02 * t / 10) for t in range(11)]
    bm = lathe(prof, seg=10, twist=4.0, wobble=0.08, phase=0.5)
    add_bm(w, bm)
    save("fx_stream", [w])


clear()
flame()
water("disc")
water("rect")
stream()
