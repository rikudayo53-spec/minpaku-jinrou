"""
.bbmodel（高精細テクスチャつきの箱）を Blender で描いて PNG にする（見本を見せる用）。

  python3.11 tools/blockbench/render_bb.py in.bbmodel out.png [yaw1,yaw2,...] [pitch]
"""
import base64
import io
import json
import math
import os
import sys
import tempfile

import bpy
from mathutils import Vector, Matrix

args = sys.argv[sys.argv.index("--") + 1:] if "--" in sys.argv else sys.argv[1:]
src, out = args[0], args[1]
yaws = [float(v) for v in args[2].split(",")] if len(args) > 2 else [-35, 145]
pitch = float(args[3]) if len(args) > 3 else 22

bpy.ops.wm.read_factory_settings(use_empty=True)
d = json.load(open(src, encoding="utf-8"))
MESH = "parts" in d   # Blender から書き出した 3D モデル（mesh/*.json）

verts, faces, uvs = [], [], []
if MESH:
    for k, part in enumerate(d["parts"]):
        pm = bpy.data.materials.new(f"p{k}")
        pm.use_nodes = True
        b = pm.node_tree.nodes["Principled BSDF"]
        b.inputs["Base Color"].default_value = tuple(part["color"]) + (1,)
        b.inputs["Roughness"].default_value = 0.45
        if part.get("emissive"):
            b.inputs["Emission Color"].default_value = tuple(part["color"]) + (1,)
            b.inputs["Emission Strength"].default_value = 2.0
        pts = part["p"]
        pv = []
        pf = []
        for i in range(0, len(pts), 9):
            base = len(pv)
            for j in range(3):
                x, y, z = pts[i + j * 3:i + j * 3 + 3]
                pv.append((-x, z, y))
            pf.append((base, base + 1, base + 2))
        me = bpy.data.meshes.new(f"p{k}")
        me.from_pydata(pv, [], pf)
        me.materials.append(pm)
        for poly in me.polygons:
            poly.use_smooth = True
        o = bpy.data.objects.new(f"p{k}", me)
        bpy.context.scene.collection.objects.link(o)
        verts.extend(Vector(v) for v in pv)

# テクスチャ
tex = d["textures"][0] if not MESH else None
if MESH:
    d["elements"] = []
    d["textures"] = [{"source": "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwAEhQGAhKmMIQAAAABJRU5ErkJggg==", "width": 1}]
    tex = d["textures"][0]
raw = base64.b64decode(tex["source"].split(",", 1)[1])
tmp = os.path.join(tempfile.gettempdir(), "bbtex_" + os.path.basename(src) + ".png")
open(tmp, "wb").write(raw)
img = bpy.data.images.load(tmp)
S = tex.get("uv_width", tex["width"])

mat = bpy.data.materials.new("bb")
mat.use_nodes = True
nt = mat.node_tree
bsdf = nt.nodes["Principled BSDF"]
ti = nt.nodes.new("ShaderNodeTexImage")
ti.image = img
ti.interpolation = "Closest"
nt.links.new(ti.outputs["Color"], bsdf.inputs["Base Color"])
nt.links.new(ti.outputs["Alpha"], bsdf.inputs["Alpha"])
bsdf.inputs["Roughness"].default_value = 0.6
em = nt.nodes.new("ShaderNodeEmission")


def rot_matrix(r):
    rx, ry, rz = [math.radians(v) for v in r]
    return Matrix.Rotation(rz, 3, "Z") @ Matrix.Rotation(ry, 3, "Y") @ Matrix.Rotation(rx, 3, "X")


def to_bl(p):
    # マイクラ（Y 上、-Z 北）→ Blender（Z 上、-Y 前）。ピクセル → ブロック
    return Vector((-p[0] / 16, p[2] / 16, p[1] / 16))


bfaces, buvs = [], []
bverts = []
for e in d["elements"]:
    f0, t0 = e["from"], e["to"]
    piv = Vector(e.get("origin", [0, 0, 0]))
    R = rot_matrix(e.get("rotation", [0, 0, 0]))

    def P(x, y, z):
        v = R @ (Vector((x, y, z)) - piv) + piv
        return to_bl(v)
    x0, y0, z0 = f0
    x1, y1, z1 = t0
    # 面ごとの4つの角（u0v0, u1v0, u1v1, u0v1 の順 = 左上, 右上, 右下, 左下）
    corners = {
        "north": [(x1, y1, z0), (x0, y1, z0), (x0, y0, z0), (x1, y0, z0)],
        "south": [(x0, y1, z1), (x1, y1, z1), (x1, y0, z1), (x0, y0, z1)],
        "east": [(x1, y1, z1), (x1, y1, z0), (x1, y0, z0), (x1, y0, z1)],
        "west": [(x0, y1, z0), (x0, y1, z1), (x0, y0, z1), (x0, y0, z0)],
        "up": [(x0, y1, z0), (x1, y1, z0), (x1, y1, z1), (x0, y1, z1)],
        "down": [(x0, y0, z1), (x1, y0, z1), (x1, y0, z0), (x0, y0, z0)],
    }
    for fname, cs in corners.items():
        fd = e["faces"].get(fname)
        if not fd:
            continue
        u0, v0, u1, v1 = fd["uv"]
        base = len(bverts)
        for c in cs:
            bverts.append(P(*c))
        bfaces.append([base, base + 1, base + 2, base + 3])
        buvs.append([(u0 / S, 1 - v0 / S), (u1 / S, 1 - v0 / S), (u1 / S, 1 - v1 / S), (u0 / S, 1 - v1 / S)])

if bverts:
    me = bpy.data.meshes.new("bb")
    me.from_pydata([tuple(v) for v in bverts], [], bfaces)
    uvl = me.uv_layers.new()
    for poly, uv in zip(me.polygons, buvs):
        for k, li in enumerate(poly.loop_indices):
            uvl.data[li].uv = uv[k]
    me.materials.append(mat)
    ob = bpy.data.objects.new("bb", me)
    bpy.context.scene.collection.objects.link(ob)
    verts.extend(bverts)

# 中心と大きさ
mn = Vector((min(v.x for v in verts), min(v.y for v in verts), min(v.z for v in verts)))
mx = Vector((max(v.x for v in verts), max(v.y for v in verts), max(v.z for v in verts)))
center = (mn + mx) / 2
size = max((mx - mn).length, 0.2)

scene = bpy.context.scene
scene.render.engine = "CYCLES"
scene.cycles.samples = 24
scene.cycles.device = "CPU"
scene.render.resolution_x = 420
scene.render.resolution_y = 420
scene.render.film_transparent = False
world = bpy.data.worlds.new("w")
scene.world = world
world.use_nodes = True
world.node_tree.nodes["Background"].inputs[0].default_value = (0.93, 0.9, 0.84, 1)
world.node_tree.nodes["Background"].inputs[1].default_value = 0.45 if MESH else 0.9
sun = bpy.data.objects.new("sun", bpy.data.lights.new("sun", "SUN"))
sun.data.energy = 2.0 if MESH else 3.2
sun.rotation_euler = (math.radians(50), math.radians(10), math.radians(-35))
scene.collection.objects.link(sun)
cam = bpy.data.objects.new("cam", bpy.data.cameras.new("cam"))
cam.data.lens = 50
scene.collection.objects.link(cam)
scene.camera = cam

from PIL import Image  # noqa: E402
tiles = []
for k, yaw in enumerate(yaws):
    dist = size * float(os.environ.get("BB_DIST", "2.1"))
    a = math.radians(yaw)
    p = math.radians(pitch)
    cam.location = center + Vector((math.sin(a) * math.cos(p), -math.cos(a) * math.cos(p), math.sin(p))) * dist
    direction = center - cam.location
    cam.rotation_euler = direction.to_track_quat("-Z", "Y").to_euler()
    fp = os.path.join(tempfile.gettempdir(), f"bbrender_{k}.png")
    scene.render.filepath = fp
    bpy.ops.render.render(write_still=True)
    tiles.append(Image.open(fp).convert("RGB"))
W = sum(t.width for t in tiles)
sheet = Image.new("RGB", (W, tiles[0].height))
x = 0
for t in tiles:
    sheet.paste(t, (x, 0))
    x += t.width
sheet.save(out)
print("描きました:", out)
