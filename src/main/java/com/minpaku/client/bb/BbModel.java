package com.minpaku.client.bb;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.util.GsonHelper;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Blockbench のプロジェクト（.bbmodel）を読む。
 * 形（立方体）・グループの親子・アニメーション（キーフレーム、効果音・タイムライン）を持つ。
 *
 * 座標は Blockbench の画面と同じ（1ピクセル = 1/16 ブロック、Y が上）。
 */
public final class BbModel {
    /** 立方体 */
    public static final class Cube {
        public String name;
        public Vector3f from;
        public Vector3f to;
        public Vector3f origin;
        public Vector3f rotation;
        public float inflate;
        public boolean boxUv;
        public float uvU;
        public float uvV;
        public boolean mirror;
        public int texture = -1;
        public boolean visible = true;
    }

    /** グループ（ボーン） */
    public static final class Group {
        public String name;
        public String uuid;
        public Vector3f origin;
        public Vector3f rotation;
        public Group parent;
        public final List<Group> groups = new ArrayList<>();
        public final List<Cube> cubes = new ArrayList<>();
        public boolean visible = true;
    }

    /** 1つのキーフレーム */
    public record Key(float time, float x, float y, float z, String interp) {
    }

    /** 効果（音・タイムライン） */
    public record Effect(float time, String channel, String value) {
    }

    public final Map<String, Group> groups = new HashMap<>();
    public final List<Group> roots = new ArrayList<>();
    public final List<String> textureNames = new ArrayList<>();
    public int texW = 64;
    public int texH = 64;
    /** ボーン名 → チャンネル（rotation / position / scale）→ キー */
    public final Map<String, Map<String, List<Key>>> anim = new HashMap<>();
    public final List<Effect> effects = new ArrayList<>();
    public float length = 4f;

    private static float f(JsonElement e) {
        if (e == null || e.isJsonNull()) {
            return 0f;
        }
        if (e.isJsonPrimitive() && e.getAsJsonPrimitive().isNumber()) {
            return e.getAsFloat();
        }
        String s = e.getAsString().trim();
        if (s.isEmpty()) {
            return 0f;
        }
        try {
            return Float.parseFloat(s);
        } catch (NumberFormatException ex) {
            return 0f;
        }
    }

    private static Vector3f v3(JsonObject o, String key, float def) {
        if (!o.has(key)) {
            return new Vector3f(def, def, def);
        }
        JsonArray a = o.getAsJsonArray(key);
        return new Vector3f(f(a.get(0)), f(a.get(1)), f(a.get(2)));
    }

    public static BbModel parse(JsonObject json) {
        BbModel m = new BbModel();
        if (json.has("resolution")) {
            JsonObject r = json.getAsJsonObject("resolution");
            m.texW = GsonHelper.getAsInt(r, "width", 64);
            m.texH = GsonHelper.getAsInt(r, "height", 64);
        }
        if (json.has("textures")) {
            for (JsonElement t : json.getAsJsonArray("textures")) {
                m.textureNames.add(GsonHelper.getAsString(t.getAsJsonObject(), "name", ""));
            }
        }
        // 立方体（uuid で引く）
        Map<String, Cube> cubes = new HashMap<>();
        if (json.has("elements")) {
            for (JsonElement el : json.getAsJsonArray("elements")) {
                JsonObject o = el.getAsJsonObject();
                if (!GsonHelper.getAsString(o, "type", "cube").equals("cube")) {
                    continue;
                }
                Cube c = new Cube();
                c.name = GsonHelper.getAsString(o, "name", "");
                c.from = v3(o, "from", 0);
                c.to = v3(o, "to", 0);
                c.origin = v3(o, "origin", 0);
                c.rotation = v3(o, "rotation", 0);
                c.inflate = GsonHelper.getAsFloat(o, "inflate", 0f);
                c.boxUv = GsonHelper.getAsBoolean(o, "box_uv", false);
                c.mirror = GsonHelper.getAsBoolean(o, "mirror_uv", false);
                c.visible = GsonHelper.getAsBoolean(o, "visibility", true);
                if (o.has("uv_offset")) {
                    JsonArray uv = o.getAsJsonArray("uv_offset");
                    c.uvU = f(uv.get(0));
                    c.uvV = f(uv.get(1));
                }
                if (o.has("faces")) {
                    for (Map.Entry<String, JsonElement> fe : o.getAsJsonObject("faces").entrySet()) {
                        JsonObject face = fe.getValue().getAsJsonObject();
                        if (face.has("texture") && !face.get("texture").isJsonNull()) {
                            c.texture = (int) f(face.get("texture"));
                            break;
                        }
                    }
                }
                cubes.put(GsonHelper.getAsString(o, "uuid", c.name), c);
            }
        }
        // グループの親子
        if (json.has("outliner")) {
            for (JsonElement el : json.getAsJsonArray("outliner")) {
                if (el.isJsonObject()) {
                    m.roots.add(m.readGroup(el.getAsJsonObject(), null, cubes));
                }
            }
        }
        // アニメーション（最初の1つ）
        if (json.has("animations") && json.getAsJsonArray("animations").size() > 0) {
            JsonObject a = json.getAsJsonArray("animations").get(0).getAsJsonObject();
            m.length = GsonHelper.getAsFloat(a, "length", 4f);
            if (a.has("animators")) {
                for (Map.Entry<String, JsonElement> e : a.getAsJsonObject("animators").entrySet()) {
                    JsonObject an = e.getValue().getAsJsonObject();
                    String type = GsonHelper.getAsString(an, "type", "bone");
                    String name = GsonHelper.getAsString(an, "name", "");
                    if (!an.has("keyframes")) {
                        continue;
                    }
                    for (JsonElement ke : an.getAsJsonArray("keyframes")) {
                        JsonObject k = ke.getAsJsonObject();
                        String channel = GsonHelper.getAsString(k, "channel", "rotation");
                        float time = GsonHelper.getAsFloat(k, "time", 0f);
                        JsonArray dps = k.getAsJsonArray("data_points");
                        if (dps == null || dps.isEmpty()) {
                            continue;
                        }
                        JsonObject dp = dps.get(0).getAsJsonObject();
                        if (type.equals("effect") || channel.equals("sound") || channel.equals("timeline") || channel.equals("particle")) {
                            String v = dp.has("script") ? dp.get("script").getAsString()
                                    : dp.has("effect") ? dp.get("effect").getAsString() : "";
                            m.effects.add(new Effect(time, channel, v));
                            continue;
                        }
                        Group g = m.groups.get(e.getKey());
                        String bone = g != null ? g.name : name;
                        String interp = GsonHelper.getAsString(k, "interpolation", "linear");
                        m.anim.computeIfAbsent(bone, b -> new HashMap<>()).computeIfAbsent(channel, c -> new ArrayList<>())
                                .add(new Key(time, f(dp.get("x")), f(dp.get("y")), f(dp.get("z")), interp));
                    }
                }
            }
            for (Map<String, List<Key>> ch : m.anim.values()) {
                for (List<Key> list : ch.values()) {
                    list.sort((p, q) -> Float.compare(p.time(), q.time()));
                }
            }
            m.effects.sort((p, q) -> Float.compare(p.time(), q.time()));
        }
        return m;
    }

    private Group readGroup(JsonObject o, Group parent, Map<String, Cube> cubes) {
        Group g = new Group();
        g.name = GsonHelper.getAsString(o, "name", "");
        g.uuid = GsonHelper.getAsString(o, "uuid", g.name);
        g.origin = v3(o, "origin", 0);
        g.rotation = v3(o, "rotation", 0);
        g.visible = GsonHelper.getAsBoolean(o, "visibility", true);
        g.parent = parent;
        groups.put(g.uuid, g);
        groups.put(g.name, g);
        if (o.has("children")) {
            for (JsonElement c : o.getAsJsonArray("children")) {
                if (c.isJsonPrimitive()) {
                    Cube cube = cubes.get(c.getAsString());
                    if (cube != null) {
                        g.cubes.add(cube);
                    }
                } else if (c.isJsonObject()) {
                    g.groups.add(readGroup(c.getAsJsonObject(), g, cubes));
                }
            }
        }
        return g;
    }

    // ------------------------------------------------------------ キーフレームの補間（Blockbench と同じ）

    /** ボーンのチャンネルの値（time は秒）。なければ def */
    public Vector3f sample(String bone, String channel, float time, float def) {
        Map<String, List<Key>> ch = anim.get(bone);
        List<Key> keys = ch == null ? null : ch.get(channel);
        if (keys == null || keys.isEmpty()) {
            return new Vector3f(def, def, def);
        }
        if (time <= keys.get(0).time()) {
            Key k = keys.get(0);
            return new Vector3f(k.x(), k.y(), k.z());
        }
        Key last = keys.get(keys.size() - 1);
        if (time >= last.time()) {
            return new Vector3f(last.x(), last.y(), last.z());
        }
        int i = 0;
        while (i < keys.size() - 2 && time >= keys.get(i + 1).time()) {
            i++;
        }
        Key a = keys.get(i);
        Key b = keys.get(i + 1);
        float alpha = (time - a.time()) / Math.max(1e-5f, b.time() - a.time());
        if (a.interp().equals("step")) {
            return new Vector3f(a.x(), a.y(), a.z());
        }
        if (a.interp().equals("catmullrom") || b.interp().equals("catmullrom")) {
            Key bp = i > 0 ? keys.get(i - 1) : null;
            Key ap = i + 2 < keys.size() ? keys.get(i + 2) : null;
            return new Vector3f(catmull(bp, a, b, ap, alpha, 0), catmull(bp, a, b, ap, alpha, 1), catmull(bp, a, b, ap, alpha, 2));
        }
        return new Vector3f(a.x() + (b.x() - a.x()) * alpha, a.y() + (b.y() - a.y()) * alpha, a.z() + (b.z() - a.z()) * alpha);
    }

    private static float val(Key k, int axis) {
        return axis == 0 ? k.x() : axis == 1 ? k.y() : k.z();
    }

    /** three.js の SplineCurve と同じ計算（Blockbench の catmullrom） */
    private static float catmull(Key bp, Key a, Key b, Key ap, float alpha, int axis) {
        List<float[]> pts = new ArrayList<>();
        if (bp != null) {
            pts.add(new float[]{bp.time(), val(bp, axis)});
        }
        pts.add(new float[]{a.time(), val(a, axis)});
        pts.add(new float[]{b.time(), val(b, axis)});
        if (ap != null) {
            pts.add(new float[]{ap.time(), val(ap, axis)});
        }
        float t = (alpha + (bp != null ? 1 : 0)) / (pts.size() - 1);
        float p = (pts.size() - 1) * t;
        int ip = (int) Math.floor(p);
        float w = p - ip;
        int n = pts.size();
        float[] p0 = pts.get(ip == 0 ? ip : ip - 1);
        float[] p1 = pts.get(Math.min(ip, n - 1));
        float[] p2 = pts.get(Math.min(ip + 1, n - 1));
        float[] p3 = pts.get(Math.min(ip + 2, n - 1));
        return cr(w, p0[1], p1[1], p2[1], p3[1]);
    }

    private static float cr(float t, float p0, float p1, float p2, float p3) {
        float v0 = (p2 - p0) * 0.5f;
        float v1 = (p3 - p1) * 0.5f;
        float t2 = t * t;
        float t3 = t * t2;
        return (2 * p1 - 2 * p2 + v0 + v1) * t3 + (-3 * p1 + 3 * p2 - 2 * v0 - v1) * t2 + v0 * t + p1;
    }
}
