# ============================================================
#  民泊人狼 - 3Dブロック・アイテムを Blender で作るスクリプト
#
#  使い方:
#    Scripting タブでこのファイルを開いて ▶
#    → 「MP_<名前>」コレクションが横に並んで作られます（ブロック19種＋アイテム4種）
#    → 形を整えたら、下の EXPORT = True にして ▶ すると
#       デスクトップの minpaku_models フォルダに <名前>.json が書き出されます
#       （mod の assets/minpaku/mesh/ に入れる、または
#         .minecraft/config/minpaku/mesh/ に入れて F3+T で差し替え）
#
#  決まり:
#    ・1 Blender単位 = 1ブロック
#    ・原点 = ブロックの底の中心、+Z = 上
#    ・正面は -Y（Blender の正面ビューで見える側）。壁に付けるものは +Y 側が壁
#    ・色はマテリアルのベースカラー。「pm_emissive」が 1 のものは光る、
#      アルファが 1 未満のものは半透明（ガラスや水）
# ============================================================

import bpy
import bmesh
import json
import math
import os
import random
from mathutils import Vector, Matrix, Euler

EXPORT = False
OUTPUT_DIR = ""        # 空ならデスクトップ/minpaku_models
ONLY = []              # 例: ["call_bell"] でそれだけ作り直す
SPACING = 1.6

R = random.Random(11)


# ============================================================ 色
def material(name, color, alpha=1.0, emissive=False, rough=0.6, metal=0.0):
    full = "MPmat_" + name
    mat = bpy.data.materials.get(full) or bpy.data.materials.new(full)
    mat.use_nodes = True
    bsdf = mat.node_tree.nodes.get("Principled BSDF")
    if bsdf:
        bsdf.inputs["Base Color"].default_value = (*[c ** 2.2 for c in color], 1)
        bsdf.inputs["Roughness"].default_value = rough
        bsdf.inputs["Metallic"].default_value = metal
        bsdf.inputs["Alpha"].default_value = alpha
        for key in ("Emission Color", "Emission"):
            if key in bsdf.inputs and emissive:
                bsdf.inputs[key].default_value = (*[c ** 2.2 for c in color], 1)
        if emissive and "Emission Strength" in bsdf.inputs:
            bsdf.inputs["Emission Strength"].default_value = 3.0
    if alpha < 1 and hasattr(mat, "blend_method"):
        try:
            mat.blend_method = 'BLEND'
        except Exception:
            pass
    if alpha < 1 and hasattr(mat, "surface_render_method"):
        mat.surface_render_method = 'BLENDED'
    mat.diffuse_color = (*color, alpha)
    mat["pm_color"] = list(color)
    mat["pm_alpha"] = alpha
    mat["pm_emissive"] = 1 if emissive else 0
    return mat


def palette():
    return {
        "wood_dark": material("wood_dark", (0.33, 0.21, 0.12)),
        "wood": material("wood", (0.62, 0.45, 0.28)),
        "wood_light": material("wood_light", (0.84, 0.70, 0.48)),
        "bamboo": material("bamboo", (0.78, 0.74, 0.42)),
        "gold": material("gold", (0.98, 0.77, 0.28), rough=0.3, metal=1.0),
        "silver": material("silver", (0.82, 0.84, 0.88), rough=0.3, metal=1.0),
        "iron": material("iron", (0.42, 0.44, 0.47), rough=0.5, metal=0.8),
        "copper": material("copper", (0.82, 0.48, 0.32), rough=0.4, metal=1.0),
        "red": material("red", (0.80, 0.14, 0.14)),
        "white": material("white", (0.95, 0.95, 0.93)),
        "paper": material("paper", (0.98, 0.95, 0.86)),
        "grey": material("grey", (0.62, 0.64, 0.67)),
        "dark": material("dark", (0.10, 0.10, 0.11)),
        "yellow": material("yellow", (0.98, 0.80, 0.16)),
        "blue": material("blue", (0.20, 0.38, 0.75)),
        "navy": material("navy", (0.12, 0.18, 0.42)),
        "sky": material("sky", (0.55, 0.75, 0.95)),
        "green": material("green", (0.40, 0.68, 0.24)),
        "leaf": material("leaf", (0.24, 0.50, 0.16)),
        "orange": material("orange", (0.97, 0.52, 0.12)),
        "pink": material("pink", (0.95, 0.55, 0.70)),
        "purple": material("purple", (0.55, 0.30, 0.80)),
        "clay": material("clay", (0.80, 0.56, 0.40)),
        "plaster": material("plaster", (0.92, 0.88, 0.80)),
        "brick": material("brick", (0.62, 0.30, 0.24)),
        "soil": material("soil", (0.38, 0.24, 0.14)),
        "stone": material("stone", (0.58, 0.58, 0.56)),
        "bark": material("bark", (0.40, 0.28, 0.16)),
        "fire": material("fire", (1.0, 0.58, 0.16), emissive=True),
        "glow": material("glow", (1.0, 0.25, 0.20), emissive=True),
        "water": material("water", (0.45, 0.72, 0.95), alpha=0.75),
        "glass": material("glass", (0.70, 0.88, 1.0), alpha=0.45, rough=0.05),
        "magic": material("magic", (0.75, 0.50, 1.0), emissive=True),
        "dust": material("dust", (0.56, 0.54, 0.52)),
        "fur": material("fur", (0.26, 0.24, 0.27), rough=0.9),
        "fur_dark": material("fur_dark", (0.13, 0.12, 0.14), rough=0.9),
        "fur_light": material("fur_light", (0.56, 0.51, 0.48), rough=0.9),
        "claw": material("claw", (0.90, 0.86, 0.74), rough=0.4),
        "tooth": material("tooth", (0.98, 0.96, 0.89), rough=0.3),
        "gum": material("gum", (0.42, 0.06, 0.09)),
    }


# ============================================================ 形を作る道具
class Model:
    def __init__(self, name, index):
        self.name = name
        cname = "MP_" + name
        old = bpy.data.collections.get(cname)
        if old:
            for ob in list(old.all_objects):
                bpy.data.objects.remove(ob, do_unlink=True)
            bpy.data.collections.remove(old)
        self.coll = bpy.data.collections.new(cname)
        bpy.context.scene.collection.children.link(self.coll)
        self.coll["mp_name"] = name
        self.root = bpy.data.objects.new(cname + "_Root", None)
        self.root.empty_display_size = 0.2
        self.root.location = ((index % 8) * SPACING, (index // 8) * SPACING * 1.3, 0)
        self.coll.objects.link(self.root)
        self.n = 0

    def _obj(self, bm, mat, loc=(0, 0, 0), rot=(0, 0, 0), scale=(1, 1, 1), smooth=True, bevel=0.0,
             angle=35, segments=2):
        self.n += 1
        mesh = bpy.data.meshes.new(f"MP_{self.name}_{self.n}")
        bm.to_mesh(mesh)
        bm.free()
        ob = bpy.data.objects.new(f"MP_{self.name}_{self.n}", mesh)
        self.coll.objects.link(ob)
        ob.parent = self.root
        ob.location = loc
        ob.rotation_euler = Euler([math.radians(a) for a in rot])
        ob.scale = scale
        mesh.materials.append(mat)
        if bevel > 0:
            m = ob.modifiers.new("Bevel", 'BEVEL')
            m.width = bevel
            m.segments = segments
            m.limit_method = 'ANGLE'
        if smooth:
            for p in mesh.polygons:
                p.use_smooth = True
            if hasattr(mesh, "set_sharp_from_angle"):
                mesh.set_sharp_from_angle(angle=math.radians(angle))
        return ob

    def box(self, size, loc, mat, rot=(0, 0, 0), bevel=0.012):
        bm = bmesh.new()
        bmesh.ops.create_cube(bm, size=1.0)
        bmesh.ops.scale(bm, vec=Vector(size), verts=bm.verts)
        return self._obj(bm, mat, loc, rot, bevel=bevel)

    def cyl(self, r, h, loc, mat, rot=(0, 0, 0), verts=24, r2=None, bevel=0.008):
        bm = bmesh.new()
        bmesh.ops.create_cone(bm, cap_ends=True, segments=verts, radius1=r, radius2=r if r2 is None else r2, depth=h)
        bmesh.ops.translate(bm, vec=Vector((0, 0, h / 2)), verts=bm.verts)
        return self._obj(bm, mat, loc, rot, bevel=bevel)

    def sphere(self, r, loc, mat, scale=(1, 1, 1), rot=(0, 0, 0), seg=20, rings=12):
        bm = bmesh.new()
        bmesh.ops.create_uvsphere(bm, u_segments=seg, v_segments=rings, radius=r)
        return self._obj(bm, mat, loc, rot, scale, angle=80)

    def ico(self, r, loc, mat, scale=(1, 1, 1), sub=2, smooth=True):
        bm = bmesh.new()
        bmesh.ops.create_icosphere(bm, subdivisions=sub, radius=r)
        for v in bm.verts:
            v.co *= 1 + R.uniform(-0.12, 0.12)
        return self._obj(bm, mat, loc, (0, 0, R.uniform(0, 360)), scale, smooth=smooth, angle=80)

    def torus(self, R1, r, loc, mat, rot=(0, 0, 0), seg=24, ring=10, arc=360.0):
        bm = bmesh.new()
        verts_rings = []
        n = seg if arc >= 360 else seg + 1
        for i in range(n):
            a = math.radians(arc) * i / seg
            ring_v = []
            for j in range(ring):
                b = 2 * math.pi * j / ring
                x = (R1 + r * math.cos(b)) * math.cos(a)
                y = (R1 + r * math.cos(b)) * math.sin(a)
                z = r * math.sin(b)
                ring_v.append(bm.verts.new((x, y, z)))
            verts_rings.append(ring_v)
        pairs = range(n) if arc >= 360 else range(n - 1)
        for i in pairs:
            a_ring = verts_rings[i]
            b_ring = verts_rings[(i + 1) % n]
            for j in range(ring):
                bm.faces.new((a_ring[j], a_ring[(j + 1) % ring], b_ring[(j + 1) % ring], b_ring[j]))
        return self._obj(bm, mat, loc, rot, angle=80)

    def lathe(self, profile, loc, mat, rot=(0, 0, 0), seg=28):
        """profile = [(半径, 高さ), ...] を Z 軸まわりに回した形"""
        bm = bmesh.new()
        rings = []
        for rad, z in profile:
            ring = []
            for i in range(seg):
                a = 2 * math.pi * i / seg
                ring.append(bm.verts.new((rad * math.cos(a), rad * math.sin(a), z)))
            rings.append(ring)
        for k in range(len(rings) - 1):
            for i in range(seg):
                bm.faces.new((rings[k][i], rings[k][(i + 1) % seg], rings[k + 1][(i + 1) % seg], rings[k + 1][i]))
        if profile[0][0] > 0:
            bm.faces.new(list(reversed(rings[0])))
        if profile[-1][0] > 0:
            bm.faces.new(rings[-1])
        return self._obj(bm, mat, loc, rot, angle=50)

    def tube(self, points, radius, mat, res=6):
        """points を通る管（ベジェ曲線＋太さ）"""
        self.n += 1
        cu = bpy.data.curves.new(f"MP_{self.name}_c{self.n}", 'CURVE')
        cu.dimensions = '3D'
        cu.bevel_depth = radius
        cu.bevel_resolution = 2
        cu.use_fill_caps = True
        sp = cu.splines.new('BEZIER')
        sp.bezier_points.add(len(points) - 1)
        for bp, p in zip(sp.bezier_points, points):
            bp.co = p
            bp.handle_left_type = 'AUTO'
            bp.handle_right_type = 'AUTO'
        sp.resolution_u = res
        ob = bpy.data.objects.new(f"MP_{self.name}_c{self.n}", cu)
        self.coll.objects.link(ob)
        ob.parent = self.root
        cu.materials.append(mat)
        return ob

    def seg(self, a, b, r1, r2, mat, verts=14):
        """点 a から点 b へ伸びる、先細りの筒（手足など）"""
        a, b = Vector(a), Vector(b)
        d = b - a
        e = Vector((0, 0, 1)).rotation_difference(d.normalized()).to_euler()
        ob = self.cyl(r1, d.length, tuple(a), mat, verts=verts, r2=r2, bevel=0)
        ob.rotation_euler = e
        return ob

    def spike(self, base, tip, r, mat):
        """とげ（毛の房・耳・歯など）"""
        return self.seg(base, tip, r, 0.004, mat, verts=8)

    def blade(self, x, pts, width, thick, mat):
        """平たい刃（YZ平面の折れ線 pts に沿って、先に向かって細くなる）"""
        bm = bmesh.new()
        n = len(pts)
        rows = []
        for i, (y, z) in enumerate(pts):
            a = pts[min(i + 1, n - 1)]
            b = pts[max(i - 1, 0)]
            ty, tz = a[0] - b[0], a[1] - b[1]
            ln = math.hypot(ty, tz) or 1.0
            ny, nz = -tz / ln, ty / ln
            f = 1.0 - i / (n - 1)
            w = max(width * (f ** 0.7), 0.002)
            tw = thick * max(f, 0.3)
            row = []
            for sx, s in ((-1, -1), (-1, 1), (1, 1), (1, -1)):
                # 刃の背（s=-1）は厚く、刃先（s=1）は薄く
                t = tw if s < 0 else tw * 0.25
                row.append(bm.verts.new((x + sx * t / 2, y + ny * w * s * 0.5 + ny * w * 0.5,
                                         z + nz * w * s * 0.5 + nz * w * 0.5)))
            rows.append(row)
        for i in range(n - 1):
            r0, r1 = rows[i], rows[i + 1]
            for k in range(4):
                bm.faces.new((r0[k], r0[(k + 1) % 4], r1[(k + 1) % 4], r1[k]))
        bm.faces.new(list(reversed(rows[0])))
        bm.faces.new(rows[-1])
        bmesh.ops.recalc_face_normals(bm, faces=bm.faces)
        return self._obj(bm, mat, smooth=False)

    def cloth(self, w, h, loc, mat, rot=(0, 0, 0), wave=0.02, thick=0.008):
        """ひらひらした布（波打つ薄い板）"""
        bm = bmesh.new()
        nx, nz = 8, 8
        grid = []
        for i in range(nx + 1):
            col = []
            for j in range(nz + 1):
                x = -w / 2 + w * i / nx
                z = -h * j / nz
                y = math.sin(i / nx * math.pi * 2.0) * wave * (j / nz)
                col.append(bm.verts.new((x, y, z)))
            grid.append(col)
        for i in range(nx):
            for j in range(nz):
                bm.faces.new((grid[i][j], grid[i + 1][j], grid[i + 1][j + 1], grid[i][j + 1]))
        ob = self._obj(bm, mat, loc, rot, angle=80)
        s = ob.modifiers.new("Solid", 'SOLIDIFY')
        s.thickness = thick
        return ob


# ============================================================ ブロック
def call_bell(m, P):
    m.cyl(0.40, 0.08, (0, 0, 0), P["wood_dark"], verts=32, bevel=0.015)
    m.cyl(0.34, 0.05, (0, 0, 0.08), P["red"], verts=32, bevel=0.02)
    m.lathe([(0.27, 0.13), (0.28, 0.15), (0.27, 0.20), (0.24, 0.27), (0.18, 0.33), (0.10, 0.365), (0.0, 0.375)],
            (0, 0, 0), P["gold"], seg=36)
    m.torus(0.27, 0.012, (0, 0, 0.14), P["gold"], seg=36)
    m.cyl(0.02, 0.08, (0, 0, 0.37), P["silver"], verts=12)
    m.lathe([(0.0, 0.0), (0.06, 0.0), (0.065, 0.015), (0.05, 0.03), (0.0, 0.035)], (0, 0, 0.445), P["silver"], seg=20)


def floor_hatch(m, P):
    m.box((1.0, 1.0, 0.03), (0, 0, 0.015), P["iron"], bevel=0.008)
    for i in range(5):
        x = -0.36 + i * 0.18
        m.box((0.17, 0.86, 0.03), (x, 0, 0.04), P["wood"], bevel=0.006)
    m.torus(0.07, 0.012, (0, -0.02, 0.06), P["iron"], rot=(90, 0, 0), arc=180, seg=12, ring=8)
    m.box((0.06, 0.06, 0.012), (0.42, 0.42, 0.06), P["iron"])
    m.box((0.06, 0.06, 0.012), (-0.42, -0.42, 0.06), P["iron"])


def breaker(m, P):
    m.box((0.76, 0.26, 0.86), (0, 0.37, 0.52), P["grey"], bevel=0.03)
    m.box((0.68, 0.03, 0.74), (0, 0.235, 0.52), P["white"], bevel=0.01)
    for i in range(5):
        x = -0.24 + i * 0.12
        m.box((0.07, 0.03, 0.18), (x, 0.215, 0.46), P["dark"], bevel=0.008)
        m.box((0.05, 0.05, 0.07), (x, 0.195, 0.50 if i % 2 == 0 else 0.42), P["white"], bevel=0.01)
    m.sphere(0.03, (0.26, 0.21, 0.78), P["glow"])
    m.box((0.22, 0.02, 0.06), (-0.16, 0.22, 0.78), P["yellow"], bevel=0.005)
    m.cyl(0.05, 0.12, (0, 0.40, 0.95), P["grey"], verts=16)


def gas_valve(m, P):
    m.cyl(0.14, 0.03, (0, 0.05, 0), P["iron"], verts=24)
    m.cyl(0.045, 0.55, (0, 0.05, 0.03), P["yellow"], verts=20)
    m.torus(0.10, 0.045, (0, 0.15, 0.58), P["yellow"], rot=(0, 90, 0), arc=90, seg=10, ring=14)
    m.cyl(0.045, 0.40, (0, 0.15, 0.68), P["yellow"], rot=(-90, 0, 0), verts=20)
    m.sphere(0.09, (0, 0.05, 0.40), P["silver"], scale=(1, 1, 1.2))
    m.cyl(0.015, 0.12, (0, 0.05, 0.48), P["silver"], verts=12)
    m.box((0.30, 0.05, 0.035), (0.06, 0.05, 0.60), P["red"], rot=(0, 0, 20), bevel=0.012)


def boiler(m, P):
    m.cyl(0.36, 0.06, (0, 0, 0), P["iron"], verts=32)
    m.cyl(0.33, 0.78, (0, 0, 0.06), P["white"], verts=40, bevel=0.03)
    m.lathe([(0.33, 0.84), (0.30, 0.90), (0.20, 0.95), (0.0, 0.97)], (0, 0, 0), P["white"], seg=40)
    m.torus(0.335, 0.012, (0, 0, 0.30), P["grey"], seg=40)
    m.torus(0.335, 0.012, (0, 0, 0.70), P["grey"], seg=40)
    # 炎ののぞき窓
    m.box((0.18, 0.06, 0.12), (0, -0.31, 0.18), P["dark"], bevel=0.015)
    m.box((0.14, 0.02, 0.08), (0, -0.345, 0.18), P["fire"], bevel=0.005)
    # 圧力計
    m.cyl(0.08, 0.04, (0, -0.31, 0.58), P["silver"], rot=(90, 0, 0), verts=24)
    m.cyl(0.065, 0.01, (0, -0.352, 0.58), P["white"], rot=(90, 0, 0), verts=24, bevel=0)
    m.box((0.008, 0.008, 0.05), (0.015, -0.36, 0.595), P["red"], rot=(0, 30, 0), bevel=0)
    # 配管
    m.cyl(0.05, 0.12, (0, 0, 0.96), P["copper"], verts=16)
    m.cyl(0.035, 0.25, (0.30, 0, 0.45), P["copper"], rot=(0, 90, 0), verts=16)
    m.cyl(0.035, 0.25, (0.30, 0, 0.25), P["copper"], rot=(0, 90, 0), verts=16)


def wiring_panel(m, P):
    m.box((0.90, 0.10, 0.90), (0, 0.44, 0.50), P["wood_dark"], bevel=0.02)
    m.box((0.80, 0.02, 0.80), (0, 0.385, 0.50), P["navy"], bevel=0.005)
    cols = ["red", "blue", "yellow", "pink"]
    for i, c in enumerate(cols):
        z = 0.75 - i * 0.16
        m.cyl(0.03, 0.04, (-0.32, 0.36, z), P["silver"], rot=(90, 0, 0), verts=12)
        m.cyl(0.03, 0.04, (0.32, 0.36, z), P["silver"], rot=(90, 0, 0), verts=12)
        sag = 0.05 + 0.03 * (i % 2)
        m.tube([(-0.32, 0.34, z), (-0.1, 0.30, z - sag), (0.12, 0.30, z - sag * 0.8), (0.32, 0.34, z)], 0.018, P[c])


def ledger(m, P):
    m.box((0.86, 0.70, 0.05), (0, 0, 0.58), P["wood"], bevel=0.015)
    for x in (-0.38, 0.38):
        for y in (-0.29, 0.29):
            m.box((0.06, 0.06, 0.56), (x, y, 0.28), P["wood_dark"], bevel=0.01)
    m.box((0.7, 0.04, 0.04), (0, 0.29, 0.15), P["wood_dark"], bevel=0.008)
    # 開いた帳面（ページはゆるく曲げる）
    m.box((0.56, 0.38, 0.015), (0, 0, 0.61), P["navy"], bevel=0.004)
    for side in (-1, 1):
        m.box((0.26, 0.35, 0.02), (side * 0.135, 0, 0.63), P["paper"], rot=(0, side * 6, 0), bevel=0.006)
    for k in range(4):
        m.box((0.16, 0.008, 0.004), (0.13, -0.10 + k * 0.06, 0.645), P["dark"], rot=(0, 6, 0), bevel=0)
    # 筆とすずり
    m.cyl(0.012, 0.28, (0.30, -0.20, 0.62), P["bamboo"], rot=(0, 90, 30), verts=10)
    m.cyl(0.012, 0.05, (0.30, -0.20, 0.62), P["dark"], rot=(0, -90, 30), verts=10, r2=0.0)
    m.box((0.12, 0.18, 0.03), (-0.34, -0.20, 0.62), P["dark"], bevel=0.008)


def futon(m, P):
    m.box((0.92, 0.66, 0.15), (0, 0, 0.075), P["white"], bevel=0.05)
    m.box((0.90, 0.64, 0.12), (0, 0, 0.21), P["navy"], bevel=0.05)
    m.box((0.92, 0.66, 0.15), (0, 0, 0.34), P["white"], bevel=0.05)
    m.box((0.90, 0.02, 0.06), (0, -0.33, 0.34), P["blue"], bevel=0.008)
    m.box((0.44, 0.24, 0.12), (0, 0.05, 0.47), P["white"], rot=(0, 0, -12), bevel=0.05)


def kamado(m, P):
    m.box((0.90, 0.86, 0.50), (0, 0, 0.25), P["plaster"], bevel=0.05)
    m.box((0.94, 0.90, 0.06), (0, 0, 0.50), P["clay"], bevel=0.02)
    m.box((0.36, 0.06, 0.26), (0, -0.42, 0.18), P["dark"], bevel=0.03)
    m.box((0.28, 0.02, 0.16), (0, -0.452, 0.16), P["fire"], bevel=0.01)
    m.box((0.44, 0.08, 0.04), (0, -0.44, 0.33), P["clay"], bevel=0.01)
    # 羽釜
    m.lathe([(0.0, 0.53), (0.20, 0.53), (0.27, 0.60), (0.29, 0.70), (0.25, 0.76)], (0, 0, 0), P["dark"], seg=32)
    m.torus(0.30, 0.03, (0, 0, 0.66), P["dark"], seg=32)
    m.cyl(0.27, 0.04, (0, 0, 0.76), P["wood_light"], verts=32, bevel=0.01)
    m.box((0.18, 0.05, 0.05), (0, 0, 0.82), P["wood"], bevel=0.015)


def bath_stove(m, P):
    m.box((0.74, 0.70, 0.66), (0, 0.10, 0.33), P["brick"], bevel=0.04)
    m.box((0.78, 0.74, 0.05), (0, 0.10, 0.68), P["stone"], bevel=0.015)
    m.box((0.34, 0.06, 0.24), (0, -0.25, 0.22), P["dark"], bevel=0.03)
    m.box((0.26, 0.02, 0.15), (0, -0.282, 0.20), P["fire"], bevel=0.008)
    m.cyl(0.07, 0.30, (0.20, 0.25, 0.70), P["copper"], verts=16)
    m.cyl(0.085, 0.03, (0.20, 0.25, 0.98), P["copper"], verts=16)
    for i, (x, rz) in enumerate([(-0.25, 10), (0.0, -8), (0.24, 15)]):
        m.cyl(0.05, 0.36, (x, -0.38, 0.05), P["bark"], rot=(90, 0, rz), verts=10)
    m.cyl(0.05, 0.36, (-0.12, -0.36, 0.13), P["bark"], rot=(90, 0, -5), verts=10)


def chopping_block(m, P):
    m.cyl(0.36, 0.42, (0, 0, 0), P["bark"], verts=18, bevel=0.02)
    m.cyl(0.33, 0.012, (0, 0, 0.42), P["wood_light"], verts=24, bevel=0)
    m.torus(0.20, 0.004, (0, 0, 0.433), P["wood"], seg=24, ring=4)
    m.torus(0.10, 0.004, (0, 0, 0.433), P["wood"], seg=24, ring=4)
    # 斧
    m.cyl(0.022, 0.55, (0.02, 0, 0.40), P["wood_light"], rot=(0, 25, 0), verts=10)
    m.box((0.20, 0.03, 0.12), (-0.04, 0, 0.44), P["silver"], rot=(0, 25, 0), bevel=0.01)
    # 割った薪
    m.box((0.08, 0.30, 0.08), (-0.38, -0.30, 0.04), P["wood_light"], rot=(0, 0, 30), bevel=0.01)
    m.box((0.08, 0.30, 0.08), (0.40, -0.22, 0.04), P["wood_light"], rot=(0, 0, -20), bevel=0.01)


def vegetable_patch(m, P):
    m.box((1.0, 1.0, 0.22), (0, 0, 0.11), P["wood"], bevel=0.02)
    m.box((0.90, 0.90, 0.04), (0, 0, 0.22), P["soil"], bevel=0.01)
    spots = [(-0.3, -0.3, "c"), (0, -0.3, "k"), (0.3, -0.3, "r"), (-0.3, 0, "k"), (0, 0, "c"),
             (0.3, 0, "c"), (-0.3, 0.3, "r"), (0, 0.3, "c"), (0.3, 0.3, "k")]
    for x, y, kind in spots:
        if kind == "c":
            m.cyl(0.04, 0.06, (x, y, 0.22), P["orange"], verts=10, r2=0.025)
            for a in (0, 120, 240):
                m.box((0.02, 0.05, 0.16), (x, y, 0.32), P["leaf"], rot=(20, 0, a), bevel=0.005)
        elif kind == "k":
            m.ico(0.11, (x, y, 0.30), P["green"], scale=(1, 1, 0.85))
            m.ico(0.13, (x, y, 0.27), P["leaf"], scale=(1, 1, 0.55))
        else:
            m.sphere(0.05, (x, y, 0.27), P["white"], scale=(1, 1, 1.3))
            m.box((0.025, 0.025, 0.14), (x, y, 0.38), P["leaf"], bevel=0.005)


def dust_pile(m, P):
    for x, y, s in [(-0.2, -0.1, 0.16), (0.15, 0.05, 0.13), (0.0, 0.25, 0.10), (0.3, -0.25, 0.07), (-0.3, 0.3, 0.06)]:
        m.ico(s, (x, y, 0.0), P["dust"], scale=(1.2, 1.0, 0.45))
    for x, y in [(0.35, 0.3), (-0.35, -0.35)]:
        m.ico(0.035, (x, y, 0.03), P["grey"])


def entrance_mat(m, P):
    m.box((0.92, 0.62, 0.025), (0, 0, 0.0125), P["red"], bevel=0.01)
    m.box((0.80, 0.50, 0.012), (0, 0, 0.03), P["wood_light"], bevel=0.004)
    for k in range(6):
        m.box((0.80, 0.008, 0.004), (0, -0.2 + k * 0.08, 0.037), P["wood"], bevel=0)
    # げた
    for x in (0.18, 0.32):
        m.box((0.10, 0.24, 0.025), (x, -0.08, 0.06), P["wood_light"], bevel=0.008)
        m.box((0.08, 0.025, 0.035), (x, -0.16, 0.03), P["wood"], bevel=0.004)
        m.box((0.08, 0.025, 0.035), (x, 0.0, 0.03), P["wood"], bevel=0.004)
        m.torus(0.035, 0.006, (x, -0.12, 0.075), P["red"], rot=(90, 0, 90), arc=180, seg=8, ring=6)


def laundry_pole(m, P):
    for x in (-0.40, 0.40):
        m.cyl(0.08, 0.06, (x, 0, 0), P["stone"], verts=12)
        m.cyl(0.03, 0.92, (x, 0, 0.05), P["bamboo"], verts=12)
        for z in (0.30, 0.62):
            m.torus(0.031, 0.006, (x, 0, z), P["wood_light"], seg=12, ring=6)
    m.cyl(0.025, 0.86, (-0.43, 0, 0.90), P["bamboo"], rot=(0, 90, 0), verts=12)
    m.cloth(0.26, 0.36, (-0.20, 0, 0.88), P["navy"], wave=0.03)
    m.cloth(0.20, 0.30, (0.06, 0, 0.88), P["white"], wave=0.025)
    m.cloth(0.14, 0.24, (0.27, 0, 0.88), P["pink"], wave=0.02)
    m.cyl(0.16, 0.16, (0, -0.30, 0), P["wood"], verts=20)
    m.ico(0.12, (0, -0.30, 0.16), P["white"], scale=(1, 1, 0.5))


def flower_vase(m, P):
    m.box((0.50, 0.50, 0.05), (0, 0, 0.025), P["wood_dark"], bevel=0.015)
    m.lathe([(0.0, 0.05), (0.10, 0.05), (0.16, 0.12), (0.18, 0.24), (0.14, 0.36), (0.07, 0.44), (0.08, 0.50),
             (0.06, 0.50)], (0, 0, 0), P["sky"], seg=32)
    m.torus(0.17, 0.012, (0, 0, 0.24), P["navy"], seg=32)
    for (dx, dy, h, c) in [(0.0, 0.0, 0.42, "red"), (-0.08, 0.04, 0.34, "yellow"), (0.09, -0.03, 0.30, "white"),
                           (0.03, 0.08, 0.36, "pink")]:
        m.tube([(0, 0, 0.48), (dx * 0.5, dy * 0.5, 0.48 + h * 0.5), (dx, dy, 0.48 + h)], 0.008, P["leaf"])
        m.sphere(0.045, (dx, dy, 0.48 + h), P[c], scale=(1, 1, 0.7))
        m.sphere(0.018, (dx, dy, 0.50 + h), P["yellow"])


def kitchen_sink(m, P):
    m.box((1.0, 0.86, 0.78), (0, 0.07, 0.39), P["wood"], bevel=0.02)
    for x in (-0.24, 0.24):
        m.box((0.44, 0.02, 0.56), (x, -0.365, 0.36), P["wood_light"], bevel=0.01)
        m.cyl(0.015, 0.03, (x * 0.2, -0.38, 0.52), P["silver"], rot=(90, 0, 0), verts=10)
    m.box((1.0, 0.90, 0.06), (0, 0.05, 0.81), P["stone"], bevel=0.015)
    m.box((0.50, 0.50, 0.04), (-0.08, 0.05, 0.825), P["iron"], bevel=0.02)
    m.box((0.44, 0.44, 0.01), (-0.08, 0.05, 0.835), P["water"], bevel=0)
    m.tube([(-0.08, 0.40, 0.84), (-0.08, 0.40, 1.08), (-0.08, 0.24, 1.10), (-0.08, 0.16, 1.02)], 0.022, P["silver"])
    for k in range(3):
        m.cyl(0.11, 0.015, (0.33, -0.10, 0.84 + k * 0.022), P["white"], verts=24, r2=0.13)
    m.cyl(0.05, 0.08, (0.33, 0.22, 0.84), P["navy"], verts=16, r2=0.06)


def shoe_rack(m, P):
    m.box((0.96, 0.50, 0.74), (0, 0.24, 0.37), P["wood_dark"], bevel=0.02)
    for x in (-0.24, 0.24):
        m.box((0.44, 0.02, 0.64), (x, -0.015, 0.37), P["wood"], bevel=0.008)
        m.box((0.03, 0.03, 0.10), (x * 0.25, -0.03, 0.42), P["silver"], bevel=0.008)
    m.box((1.0, 0.54, 0.04), (0, 0.24, 0.76), P["wood"], bevel=0.01)
    # 上の靴（ばらばら）
    m.box((0.10, 0.24, 0.03), (-0.25, 0.20, 0.80), P["wood_light"], rot=(0, 0, 25), bevel=0.01)
    m.torus(0.035, 0.006, (-0.25, 0.15, 0.83), P["red"], rot=(90, 0, 115), arc=180, seg=8, ring=6)
    m.sphere(0.08, (0.22, 0.22, 0.82), P["dark"], scale=(1.4, 0.7, 0.55), rot=(0, 0, -20))
    m.sphere(0.08, (0.30, 0.30, 0.82), P["dark"], scale=(1.4, 0.7, 0.55), rot=(0, 0, -35))
    # 床に脱ぎっぱなしの下駄
    m.box((0.10, 0.24, 0.03), (-0.20, -0.30, 0.03), P["wood_light"], rot=(0, 0, -20), bevel=0.01)
    m.box((0.10, 0.24, 0.03), (0.10, -0.32, 0.03), P["wood_light"], rot=(0, 0, 40), bevel=0.01)


def shoji_screen(m, P):
    m.box((1.0, 0.06, 1.0), (0, 0.44, 0.5), P["paper"], bevel=0.0)
    m.box((1.0, 0.09, 0.06), (0, 0.44, 0.03), P["wood"], bevel=0.008)
    m.box((1.0, 0.09, 0.06), (0, 0.44, 0.97), P["wood"], bevel=0.008)
    for x in (-0.47, 0.47):
        m.box((0.06, 0.09, 1.0), (x, 0.44, 0.5), P["wood"], bevel=0.008)
    for x in (-0.16, 0.16):
        m.box((0.025, 0.08, 0.92), (x, 0.43, 0.5), P["wood_light"], bevel=0.004)
    for z in (0.26, 0.5, 0.74):
        m.box((0.92, 0.08, 0.025), (0, 0.43, z), P["wood_light"], bevel=0.004)
    # 穴
    m.ico(0.035, (0.05, 0.40, 0.62), P["dark"], scale=(1, 0.2, 1), smooth=False)


# ============================================================ アイテム（手に持つもの）
def manager_key(m, P):
    m.torus(0.13, 0.03, (0, 0, 0.78), P["gold"], rot=(90, 0, 0), seg=28, ring=10)
    m.cyl(0.03, 0.52, (0, 0, 0.13), P["gold"], verts=14)
    m.box((0.12, 0.04, 0.05), (0.06, 0, 0.17), P["gold"], bevel=0.01)
    m.box((0.08, 0.04, 0.05), (0.05, 0, 0.27), P["gold"], bevel=0.01)
    m.torus(0.045, 0.012, (0, 0, 0.65), P["gold"], seg=16, ring=8)
    m.box((0.16, 0.025, 0.24), (-0.26, 0, 0.62), P["wood_light"], rot=(0, -15, 0), bevel=0.012)
    m.tube([(-0.12, 0, 0.88), (-0.18, 0, 0.82), (-0.24, 0, 0.75)], 0.008, P["red"])


def seer_crystal(m, P):
    m.lathe([(0.0, 0.0), (0.22, 0.0), (0.24, 0.03), (0.18, 0.06), (0.12, 0.10), (0.16, 0.14), (0.17, 0.16),
             (0.0, 0.16)], (0, 0, 0), P["gold"], seg=32)
    m.cyl(0.19, 0.02, (0, 0, 0.16), P["purple"], verts=32, bevel=0.008)
    m.sphere(0.12, (0, 0, 0.42), P["magic"], seg=20, rings=12)
    m.sphere(0.25, (0, 0, 0.42), P["glass"], seg=32, rings=20)


def knight_charm(m, P):
    m.box((0.34, 0.10, 0.48), (0, 0, 0.30), P["red"], bevel=0.04)
    m.box((0.30, 0.08, 0.10), (0, 0, 0.58), P["red"], bevel=0.035)
    m.box((0.36, 0.11, 0.04), (0, 0, 0.52), P["gold"], bevel=0.01)
    m.box((0.14, 0.012, 0.26), (0, -0.055, 0.30), P["paper"], bevel=0.004)
    for k in range(3):
        m.box((0.08, 0.004, 0.012), (0, -0.062, 0.38 - k * 0.07), P["dark"], bevel=0)
    m.torus(0.07, 0.012, (0, 0, 0.72), P["white"], rot=(90, 0, 0), seg=16, ring=6)
    m.sphere(0.03, (0, 0, 0.64), P["white"])


def sabotage_talisman(m, P):
    m.box((0.32, 0.015, 0.40), (0, 0, 0.62), P["paper"], bevel=0.003)
    m.box((0.32, 0.015, 0.40), (0, -0.04, 0.24), P["paper"], rot=(12, 0, 0), bevel=0.003)
    m.box((0.32, 0.02, 0.05), (0, 0, 0.84), P["red"], bevel=0.004)
    for z in (0.70, 0.60, 0.50):
        m.box((0.20, 0.006, 0.02), (0, -0.01, z), P["red"], bevel=0)
    m.box((0.02, 0.006, 0.34), (0, -0.012, 0.58), P["red"], bevel=0)
    m.torus(0.05, 0.008, (0, -0.06, 0.20), P["dark"], rot=(78, 0, 0), seg=16, ring=6)


# ============================================================ 人狼（襲われる演出用。部位ごとに動かす）
# 正面は +Y（演出ではカメラの方を向く）。各部位は回転の中心（関節）が原点になるよう書き出す。
WOLF_PIVOTS = {
    "werewolf_body": (0.0, 0.0, 1.0),
    "werewolf_head": (0.0, 0.26, 2.0),
    "werewolf_jaw": (0.0, 0.42, 1.97),
    "werewolf_arm_a": (0.46, 0.06, 1.86),
    "werewolf_arm_b": (-0.46, 0.06, 1.86),
}


def _to_pivot(m, name):
    piv = Vector(WOLF_PIVOTS[name])
    for ob in m.coll.objects:
        if ob is not m.root:
            ob.location = ob.location - piv
            if ob.type == 'CURVE':
                for sp in ob.data.splines:
                    for bp in sp.bezier_points:
                        bp.co -= piv
                        bp.handle_left -= piv
                        bp.handle_right -= piv
                ob.location = ob.location + piv


def werewolf_body(m, P):
    # 腰・胴（前かがみ）
    m.sphere(0.30, (0, 0, 1.05), P["fur"], scale=(1.05, 0.85, 0.85))
    m.sphere(0.42, (0, 0.10, 1.52), P["fur"], scale=(1.0, 0.78, 1.15), rot=(-22, 0, 0))
    m.sphere(0.30, (0, 0.30, 1.56), P["fur_light"], scale=(0.95, 0.42, 1.0), rot=(-22, 0, 0))
    # 盛り上がった肩
    for x in (0.38, -0.38):
        m.sphere(0.21, (x, 0.04, 1.84), P["fur"], scale=(1.1, 1.0, 0.85))
    # 首
    m.seg((0, 0.10, 1.78), (0, 0.26, 2.04), 0.19, 0.16, P["fur"])
    # 背中・肩・首まわりの逆立った毛
    for i in range(9):
        t = i / 8
        z = 1.25 + t * 0.8
        y = -0.25 - 0.08 * math.sin(t * math.pi) + t * 0.18
        for x in (-0.12, 0.0, 0.12):
            m.spike((x * (1.2 - t * 0.5), y + 0.05, z), (x * 1.6, y - 0.22, z + 0.12 + R.uniform(-0.03, 0.05)),
                    0.06, P["fur_dark"])
    for k in range(10):
        a = math.radians(-100 + k * 22)
        bx, by = math.cos(a) * 0.24, math.sin(a) * 0.20 + 0.12
        m.spike((bx, by, 1.92), (bx * 1.8, by * 1.2 - 0.06, 1.98 + R.uniform(-0.04, 0.06)), 0.07, P["fur_dark"])
    # 足（後ろに曲がる獣の足）
    for x in (0.22, -0.22):
        m.seg((x, 0.02, 1.05), (x * 1.1, 0.24, 0.62), 0.15, 0.11, P["fur"])
        m.seg((x * 1.1, 0.24, 0.62), (x * 1.1, -0.08, 0.20), 0.10, 0.07, P["fur"])
        m.seg((x * 1.1, -0.08, 0.20), (x * 1.1, 0.10, 0.03), 0.07, 0.06, P["fur_dark"])
        for dx in (-0.05, 0.0, 0.05):
            m.spike((x * 1.1 + dx, 0.10, 0.04), (x * 1.1 + dx, 0.24, 0.0), 0.022, P["claw"])
    # しっぽ
    m.tube([(0, -0.25, 1.05), (0, -0.55, 0.95), (0, -0.85, 1.05)], 0.08, P["fur_dark"])
    _to_pivot(m, "werewolf_body")


def werewolf_head(m, P):
    # 頭・鼻先
    m.sphere(0.24, (0, 0.28, 2.12), P["fur"], scale=(1.0, 1.05, 0.88))
    m.cyl(0.13, 0.36, (0, 0.40, 2.07), P["fur"], rot=(-94, 0, 0), r2=0.075, bevel=0)
    m.sphere(0.12, (0, 0.40, 2.03), P["fur_light"], scale=(1.0, 1.4, 0.55))
    m.sphere(0.05, (0, 0.77, 2.09), P["dark"], scale=(1.15, 0.9, 0.8))
    # 口の中（赤）
    m.box((0.16, 0.30, 0.05), (0, 0.58, 1.99), P["gum"], bevel=0.01)
    # 上の歯・牙
    for k in range(5):
        y = 0.50 + k * 0.055
        for x in (0.075 - k * 0.006, -0.075 + k * 0.006):
            big = k == 4
            m.spike((x, y, 2.01), (x, y + 0.01, 1.92 if big else 1.96), 0.022 if big else 0.014, P["tooth"])
    # 赤く光る目と、つり上がった眉
    for x in (0.105, -0.105):
        m.sphere(0.04, (x, 0.47, 2.17), P["glow"], scale=(1.3, 0.6, 0.7), rot=(0, 0, 25 if x > 0 else -25))
        m.box((0.13, 0.05, 0.035), (x, 0.47, 2.215), P["fur_dark"], rot=(0, 25 if x > 0 else -25, 0), bevel=0.01)
    # 耳
    for x in (0.15, -0.15):
        m.seg((x, 0.22, 2.26), (x * 1.5, 0.12, 2.56), 0.08, 0.008, P["fur"], verts=10)
        m.seg((x, 0.25, 2.27), (x * 1.45, 0.16, 2.50), 0.045, 0.006, P["gum"], verts=8)
    # 頭の毛
    for k in range(7):
        x = (k - 3) * 0.06
        m.spike((x, 0.18, 2.26), (x * 1.4, -0.06, 2.34 + R.uniform(-0.03, 0.04)), 0.05, P["fur_dark"])
    _to_pivot(m, "werewolf_head")


def werewolf_jaw(m, P):
    m.cyl(0.095, 0.30, (0, 0.40, 1.95), P["fur"], rot=(-98, 0, 0), r2=0.055, bevel=0)
    m.box((0.13, 0.26, 0.03), (0, 0.56, 1.975), P["gum"], bevel=0.01)
    for k in range(4):
        y = 0.52 + k * 0.05
        for x in (0.06 - k * 0.006, -0.06 + k * 0.006):
            m.spike((x, y, 1.97), (x, y, 2.04 if k == 3 else 2.01), 0.018 if k == 3 else 0.012, P["tooth"])
    for k in range(4):
        x = (k - 1.5) * 0.06
        m.spike((x, 0.42, 1.90), (x * 1.3, 0.40, 1.78), 0.04, P["fur_dark"])
    _to_pivot(m, "werewolf_jaw")


def _arm(m, P, sx, name):
    sh = (0.46 * sx, 0.06, 1.86)
    el = (0.56 * sx, 0.14, 1.40)
    wr = (0.58 * sx, 0.24, 0.98)
    m.seg(sh, el, 0.13, 0.10, P["fur"])
    m.sphere(0.11, el, P["fur"])
    m.seg(el, wr, 0.10, 0.075, P["fur"])
    for k in range(4):
        t = (k + 0.5) / 4
        b = (el[0] + (wr[0] - el[0]) * t, el[1] - 0.06, el[2] + (wr[2] - el[2]) * t)
        m.spike(b, (b[0] + 0.06 * sx, b[1] - 0.16, b[2] + 0.05), 0.045, P["fur_dark"])
    # 手と爪
    m.sphere(0.10, (wr[0], wr[1] + 0.02, wr[2] - 0.06), P["fur_dark"], scale=(1.0, 0.75, 1.0))
    for k in range(4):
        x = wr[0] + (k - 1.5) * 0.05
        y0, z0 = wr[1] + 0.07, wr[2] - 0.11
        pts = [(y0, z0), (y0 + 0.04, z0 - 0.10), (y0 + 0.10, z0 - 0.20), (y0 + 0.20, z0 - 0.27)]
        m.blade(x, pts, 0.045, 0.024, P["claw"])
    _to_pivot(m, name)


def werewolf_arm_a(m, P):
    _arm(m, P, 1, "werewolf_arm_a")


def werewolf_arm_b(m, P):
    _arm(m, P, -1, "werewolf_arm_b")


WEREWOLF = [werewolf_body, werewolf_head, werewolf_jaw, werewolf_arm_a, werewolf_arm_b]


BLOCKS = [call_bell, floor_hatch, breaker, gas_valve, boiler, wiring_panel, ledger, futon, kamado, bath_stove,
          chopping_block, vegetable_patch, dust_pile, entrance_mat, laundry_pole, flower_vase, kitchen_sink,
          shoe_rack, shoji_screen]
ITEMS = [manager_key, seer_crystal, knight_charm, sabotage_talisman]


# ============================================================ 書き出し
def default_dir():
    home = os.path.expanduser("~")
    for d in ("Desktop", os.path.join("OneDrive", "Desktop"), os.path.join("OneDrive", "デスクトップ")):
        if os.path.isdir(os.path.join(home, d)):
            return os.path.join(home, d, "minpaku_models")
    return os.path.join(home, "minpaku_models")


def export_all(out_dir):
    os.makedirs(out_dir, exist_ok=True)
    dg = bpy.context.evaluated_depsgraph_get()
    count = 0
    for coll in bpy.data.collections:
        if "mp_name" not in coll:
            continue
        root = next((o for o in coll.objects if o.name.endswith("_Root")), None)
        inv = root.matrix_world.inverted() if root else Matrix.Identity(4)
        parts = {}
        for ob in coll.all_objects:
            if ob.type not in {'MESH', 'CURVE'}:
                continue
            ev = ob.evaluated_get(dg)
            mesh = ev.to_mesh()
            mesh.calc_loop_triangles()
            mw = inv @ ev.matrix_world
            nm = mw.to_3x3().inverted_safe().transposed()
            mat = ob.active_material
            key = mat.name if mat else "none"
            if key not in parts:
                color = list(mat["pm_color"]) if mat and "pm_color" in mat else [0.8, 0.8, 0.8]
                parts[key] = {"color": [round(c, 4) for c in color],
                              "alpha": round(float(mat.get("pm_alpha", 1.0)) if mat else 1.0, 3),
                              "emissive": bool(mat.get("pm_emissive", 0)) if mat else False,
                              "p": [], "n": []}
            part = parts[key]
            for tri in mesh.loop_triangles:
                for k in range(3):
                    v = mw @ mesh.vertices[tri.vertices[k]].co
                    n = (nm @ Vector(tri.split_normals[k])).normalized()
                    # Blender（Z上・-Y前）→ マイクラ（Y上・-Z前）。x は向きをそろえるため反転
                    part["p"].extend([round(-v.x, 4), round(v.z, 4), round(v.y, 4)])
                    part["n"].extend([round(-n.x, 3), round(n.z, 3), round(n.y, 3)])
            ev.to_mesh_clear()
        out = {"format": 2, "parts": list(parts.values())}
        path = os.path.join(out_dir, coll["mp_name"] + ".json")
        with open(path, "w", encoding="utf-8") as fp:
            json.dump(out, fp, separators=(",", ":"))
        tris = sum(len(p["p"]) // 9 for p in out["parts"])
        print(f"  {coll['mp_name']}: 三角形 {tris}")
        count += 1
    print(f"民泊人狼: {count} 個書き出しました → {out_dir}")


def build():
    P = palette()
    for i, fn in enumerate(BLOCKS + ITEMS + WEREWOLF):
        if ONLY and fn.__name__ not in ONLY:
            continue
        fn(Model(fn.__name__, i), P)
    cube = bpy.data.objects.get("Cube")
    if cube is not None and cube.type == 'MESH':
        try:
            cube.hide_set(True)
        except Exception:
            pass
        cube.hide_render = True
    print("民泊人狼: 3Dモデルを作りました（MP_ で始まるコレクション）")


build()
if EXPORT:
    export_all(OUTPUT_DIR or default_dir())
