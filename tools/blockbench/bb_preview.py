"""
Blockbench プロジェクト（bb.Project）を、ゲームと同じカメラで簡単に描いて確かめる道具。
  preview(project, times, path)  → 時刻ごとのコマを並べた PNG
"""
import math

import numpy as np
from PIL import Image, ImageDraw

from bb import MATERIAL_COLORS, Rzyx

W, H = 400, 225


def _tex_color(p, e, face, uv):
    name, img = p.textures[e["texture"]]
    if "skin" in name:
        u0, v0, u1, v1 = uv
        x0, x1 = sorted((int(u0), int(u1)))
        y0, y1 = sorted((int(v0), int(v1)))
        x1 = max(x1, x0 + 1)
        y1 = max(y1, y0 + 1)
        a = np.asarray(img.crop((x0, y0, x1, y1)).convert("RGBA"), dtype=float).reshape(-1, 4)
        if a[:, 3].mean() < 128:
            return None
        m = a[a[:, 3] > 128][:, :3].mean(axis=0)
        return tuple(int(v) for v in m)
    key = name.replace("glow_", "").replace("glass_", "")
    return MATERIAL_COLORS.get(name, MATERIAL_COLORS.get(key, (200, 200, 200)))


def _look(eye, target):
    f = target - eye
    f = f / (np.linalg.norm(f) + 1e-9)
    up = np.array([0, 1.0, 0])
    r = np.cross(f, up)
    r = r / (np.linalg.norm(r) + 1e-9)
    u = np.cross(r, f)
    return r, u, f


FACES = {  # 立方体の面（角の番号）と向き
    "north": ([0, 1, 5, 4], (0, 0, -1)), "south": ([2, 6, 7, 3], (0, 0, 1)),
    "west": ([0, 2, 3, 1], (-1, 0, 0)), "east": ([4, 5, 7, 6], (1, 0, 0)),
    "down": ([0, 4, 6, 2], (0, -1, 0)), "up": ([1, 3, 7, 5], (0, 1, 0)),
}


def frame(p, t, room=True, slim=False, view=None):
    Wm = p.world(t)
    eye = (Wm["camera"] @ np.array([0, 0, 0, 1.0]))[:3] if "camera" in Wm else np.array([0, 1.6, 4])
    tgt = (Wm["camera_target"] @ np.array([0, 0, 0, 1.0]))[:3] if "camera_target" in Wm else np.array([0, 1.2, 0])
    fov = 50 * p.sample("camera", "scale", t, 1.0)[0]
    if view:
        eye, tgt, fov = np.array(view[0], float), np.array(view[1], float), view[2]
    r, u, f = _look(eye, tgt)
    foc = (H / 2) / math.tan(math.radians(fov) / 2)
    light = np.array([0.3, 0.9, 0.5])
    light /= np.linalg.norm(light)

    polys = []

    def proj(v):
        d = v - eye
        z = d @ f
        if z < 0.05:
            return None
        return (W / 2 + (d @ r) / z * foc, H / 2 - (d @ u) / z * foc, z)

    def add_quad(pts, col, normal):
        if col is None:
            return
        if (pts.mean(axis=0) - eye) @ normal > 0:
            return
        pr = [proj(q) for q in pts]
        if any(q is None for q in pr):
            return
        sh = 0.55 + 0.45 * max(0.0, float(normal @ light))
        c = tuple(int(min(255, v * sh)) for v in col)
        polys.append((sum(q[2] for q in pr) / 4, [(q[0], q[1]) for q in pr], c))

    if room:
        floor = (150, 160, 90)
        for x in range(-6, 6):
            for z in range(-6, 6):
                c = floor if (x + z) % 2 else (140, 150, 84)
                add_quad(np.array([[x, 0, z], [x, 0, z + 1], [x + 1, 0, z + 1], [x + 1, 0, z]], float), c, np.array([0, 1.0, 0]))
        for x in range(-6, 7):
            c = (110, 80, 50) if x % 3 == 0 else (225, 220, 205)
            add_quad(np.array([[x, 0, -5], [x + 1, 0, -5], [x + 1, 4, -5], [x, 4, -5]], float), c, np.array([0, 0, 1.0]))
        for z in range(-5, 6):
            c = (110, 80, 50) if z % 3 == 0 else (225, 220, 205)
            add_quad(np.array([[-6, 0, z], [-6, 4, z], [-6, 4, z + 1], [-6, 0, z + 1]], float), c, np.array([1.0, 0, 0]))
            add_quad(np.array([[7, 0, z], [7, 0, z + 1], [7, 4, z + 1], [7, 4, z]], float), c, np.array([-1.0, 0, 0]))

    def hidden(g):
        while g is not None:
            if g.name.startswith("ref_"):
                return True
            g = g.parent
        return False

    for g in p.groups.values():
        if hidden(g) or g.name not in Wm:
            continue
        M = Wm[g.name]
        for e in g.cubes:
            n = e["name"]
            if n.endswith("#slim") and not slim or n.endswith("#wide") and slim:
                continue
            a = np.array(e["from"]) - e["inflate"]
            b = np.array(e["to"]) + e["inflate"]
            corners = []
            rot = e.get("rotation", [0, 0, 0])
            Rc = Rzyx(*[math.radians(v) for v in (rot[2], rot[1], rot[0])])[:3, :3]
            piv = np.array(e["origin"])
            for i in range(8):
                c = np.array([b[0] if i & 4 else a[0], b[1] if i & 1 else a[1], b[2] if i & 2 else a[2]])
                c = piv + Rc @ (c - piv)
                corners.append((M @ np.append((c - np.array(g.origin)) / 16, 1))[:3])
            corners = np.array(corners)
            uvs = p.face_uvs(e)
            R = M[:3, :3] @ Rc
            for face, (idx, nrm) in FACES.items():
                nn = R @ np.array(nrm, float)
                nn /= np.linalg.norm(nn) + 1e-9
                add_quad(corners[idx], _tex_color(p, e, face, uvs[face]), nn)

    img = Image.new("RGB", (W, H), (20, 20, 24))
    d = ImageDraw.Draw(img)
    for _, pts, c in sorted(polys, key=lambda q: -q[0]):
        d.polygon(pts, fill=c)
    # 効果（hit・文字）
    for (et, ch, v) in p.effects:
        if ch == "timeline" and abs(et - t) < 0.06:
            d.rectangle([0, 0, W - 1, H - 1], outline=(255, 255, 255), width=4)
    d.text((4, 4), f"{t:.2f}s", fill=(255, 255, 0))
    return img


def preview(p, times, path, cols=4, room=True, view=None):
    rows = (len(times) + cols - 1) // cols
    sheet = Image.new("RGB", (W * cols, H * rows))
    for i, t in enumerate(times):
        sheet.paste(frame(p, t, room, view=view), ((i % cols) * W, (i // cols) * H))
    sheet.save(path)
