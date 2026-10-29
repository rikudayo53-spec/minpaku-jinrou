package com.minpaku.client.model;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.minpaku.MinpakuJinrou;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.GsonHelper;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.io.ByteArrayInputStream;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 高精細な Blockbench モデル（細かいテクスチャを貼った箱）。家具・銃に使う。
 *   1) .minecraft/config/minpaku/hires/<名前>.bbmodel があればそれ
 *   2) なければ mod の中の assets/minpaku/hires/<名前>.bbmodel
 * 箱の名前が glow_ で始まると光る、glass_ で始まると半透明。グループごとに動かせる（Animator）。
 * F3+T で読み直す。
 */
public final class HiRes {
    /** グループを動かす：ps に変形をかける（原点はグループの origin）。false を返すとそのグループは描かない */
    public interface Animator {
        boolean apply(String group, PoseStack ps, float ox, float oy, float oz);
    }

    public static final Animator STILL = (g, ps, x, y, z) -> true;

    static final class Face {
        float u0, v0, u1, v1;
    }

    static final class Cube {
        String name;
        Vector3f from, to, origin, rot;
        Face[] faces = new Face[6];   // north, east, south, west, up, down
        boolean glow, glass;
    }

    static final class Group {
        String name;
        Vector3f origin;
        final List<Cube> cubes = new ArrayList<>();
        final List<Group> groups = new ArrayList<>();
    }

    private final Group root = new Group();
    private ResourceLocation texture;
    private int size = 16;

    private static final String[] FACE_NAMES = {"north", "east", "south", "west", "up", "down"};

    // ------------------------------------------------------------ 読み込み

    private static final Map<String, Optional<HiRes>> CACHE = new HashMap<>();

    public static void register() {
        ResourceManagerHelper.get(PackType.CLIENT_RESOURCES).registerReloadListener(new SimpleSynchronousResourceReloadListener() {
            @Override
            public ResourceLocation getFabricId() {
                return MinpakuJinrou.id("hires_cache");
            }

            @Override
            public void onResourceManagerReload(ResourceManager manager) {
                CACHE.clear();
            }
        });
    }

    @Nullable
    public static HiRes get(String name) {
        return CACHE.computeIfAbsent(name, HiRes::load).orElse(null);
    }

    private static Optional<HiRes> load(String name) {
        try {
            JsonObject json = null;
            Path file = FabricLoader.getInstance().getConfigDir().resolve("minpaku/hires/" + name + ".bbmodel");
            if (Files.isRegularFile(file)) {
                try (Reader r = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                    json = JsonParser.parseReader(r).getAsJsonObject();
                }
            }
            if (json == null) {
                Optional<Resource> res = Minecraft.getInstance().getResourceManager()
                        .getResource(MinpakuJinrou.id("hires/" + name + ".bbmodel"));
                if (res.isEmpty()) {
                    return Optional.empty();
                }
                try (Reader r = res.get().openAsReader()) {
                    json = JsonParser.parseReader(r).getAsJsonObject();
                }
            }
            return Optional.of(parse(name, json));
        } catch (Exception e) {
            MinpakuJinrou.LOGGER.error("高精細モデル {} を読み込めませんでした", name, e);
            return Optional.empty();
        }
    }

    private static Vector3f v3(JsonObject o, String key) {
        if (!o.has(key)) {
            return new Vector3f();
        }
        JsonArray a = o.getAsJsonArray(key);
        return new Vector3f(a.get(0).getAsFloat(), a.get(1).getAsFloat(), a.get(2).getAsFloat());
    }

    private static HiRes parse(String name, JsonObject json) throws Exception {
        HiRes m = new HiRes();
        m.root.name = "";
        m.root.origin = new Vector3f();
        // テクスチャ（埋めこまれた PNG）
        JsonObject tex = json.getAsJsonArray("textures").get(0).getAsJsonObject();
        m.size = GsonHelper.getAsInt(tex, "uv_width", GsonHelper.getAsInt(tex, "width", 16));
        String src = GsonHelper.getAsString(tex, "source", "");
        byte[] png = Base64.getDecoder().decode(src.substring(src.indexOf(',') + 1));
        NativeImage img = NativeImage.read(new ByteArrayInputStream(png));
        m.texture = MinpakuJinrou.id("hires_tex/" + name.toLowerCase());
        Minecraft.getInstance().getTextureManager().register(m.texture, new DynamicTexture(img));
        // 箱
        Map<String, Cube> byUuid = new HashMap<>();
        List<Cube> order = new ArrayList<>();
        for (JsonElement el : json.getAsJsonArray("elements")) {
            JsonObject o = el.getAsJsonObject();
            Cube c = new Cube();
            c.name = GsonHelper.getAsString(o, "name", "");
            c.from = v3(o, "from");
            c.to = v3(o, "to");
            c.origin = v3(o, "origin");
            c.rot = v3(o, "rotation");
            c.glow = c.name.startsWith("glow_");
            c.glass = c.name.startsWith("glass_");
            JsonObject faces = o.getAsJsonObject("faces");
            for (int i = 0; i < 6; i++) {
                if (faces != null && faces.has(FACE_NAMES[i])) {
                    JsonObject fo = faces.getAsJsonObject(FACE_NAMES[i]);
                    if (!fo.has("uv") || fo.get("texture").isJsonNull()) {
                        continue;
                    }
                    JsonArray uv = fo.getAsJsonArray("uv");
                    Face f = new Face();
                    f.u0 = uv.get(0).getAsFloat() / m.size;
                    f.v0 = uv.get(1).getAsFloat() / m.size;
                    f.u1 = uv.get(2).getAsFloat() / m.size;
                    f.v1 = uv.get(3).getAsFloat() / m.size;
                    c.faces[i] = f;
                }
            }
            byUuid.put(GsonHelper.getAsString(o, "uuid", c.name + order.size()), c);
            order.add(c);
        }
        // グループ（outliner）
        java.util.Set<Cube> placed = new java.util.HashSet<>();
        if (json.has("outliner")) {
            for (JsonElement ch : json.getAsJsonArray("outliner")) {
                addNode(m.root, ch, byUuid, placed);
            }
        }
        for (Cube c : order) {
            if (!placed.contains(c)) {
                m.root.cubes.add(c);
            }
        }
        return m;
    }

    private static void addNode(Group parent, JsonElement node, Map<String, Cube> byUuid, java.util.Set<Cube> placed) {
        if (node.isJsonPrimitive()) {
            Cube c = byUuid.get(node.getAsString());
            if (c != null) {
                parent.cubes.add(c);
                placed.add(c);
            }
            return;
        }
        JsonObject o = node.getAsJsonObject();
        Group g = new Group();
        g.name = GsonHelper.getAsString(o, "name", "");
        g.origin = v3(o, "origin");
        parent.groups.add(g);
        if (o.has("children")) {
            for (JsonElement ch : o.getAsJsonArray("children")) {
                addNode(g, ch, byUuid, placed);
            }
        }
    }

    // ------------------------------------------------------------ 描く（座標はピクセル、1ブロック = 16）

    public void render(PoseStack ps, MultiBufferSource buf, int light, int overlay, Animator anim) {
        VertexConsumer solid = buf.getBuffer(RenderType.entityCutoutNoCull(texture));
        VertexConsumer clear = buf.getBuffer(RenderType.entityTranslucent(texture));
        drawGroup(root, ps, solid, clear, light, overlay, anim);
    }

    private void drawGroup(Group g, PoseStack ps, VertexConsumer solid, VertexConsumer clear, int light, int overlay,
                           Animator anim) {
        ps.pushPose();
        if (!g.name.isEmpty() && !anim.apply(g.name, ps, g.origin.x / 16f, g.origin.y / 16f, g.origin.z / 16f)) {
            ps.popPose();
            return;
        }
        for (Cube c : g.cubes) {
            drawCube(c, ps, c.glass ? clear : solid, c.glow ? LightTexture.FULL_BRIGHT : light, overlay);
        }
        for (Group ch : g.groups) {
            drawGroup(ch, ps, solid, clear, light, overlay, anim);
        }
        ps.popPose();
    }

    private static void drawCube(Cube c, PoseStack ps, VertexConsumer vc, int light, int overlay) {
        ps.pushPose();
        if (c.rot.x != 0 || c.rot.y != 0 || c.rot.z != 0) {
            ps.translate(c.origin.x / 16f, c.origin.y / 16f, c.origin.z / 16f);
            ps.last().pose().rotateZYX((float) Math.toRadians(c.rot.z), (float) Math.toRadians(c.rot.y), (float) Math.toRadians(c.rot.x));
            ps.last().normal().rotateZYX((float) Math.toRadians(c.rot.z), (float) Math.toRadians(c.rot.y), (float) Math.toRadians(c.rot.x));
            ps.translate(-c.origin.x / 16f, -c.origin.y / 16f, -c.origin.z / 16f);
        }
        PoseStack.Pose pose = ps.last();
        float x0 = c.from.x / 16f, y0 = c.from.y / 16f, z0 = c.from.z / 16f;
        float x1 = c.to.x / 16f, y1 = c.to.y / 16f, z1 = c.to.z / 16f;
        // 4つの角は 左上・右上・右下・左下（u0v0, u1v0, u1v1, u0v1）
        float[][][] corners = {
                {{x1, y1, z0}, {x0, y1, z0}, {x0, y0, z0}, {x1, y0, z0}},
                {{x1, y1, z1}, {x1, y1, z0}, {x1, y0, z0}, {x1, y0, z1}},
                {{x0, y1, z1}, {x1, y1, z1}, {x1, y0, z1}, {x0, y0, z1}},
                {{x0, y1, z0}, {x0, y1, z1}, {x0, y0, z1}, {x0, y0, z0}},
                {{x0, y1, z0}, {x1, y1, z0}, {x1, y1, z1}, {x0, y1, z1}},
                {{x0, y0, z1}, {x1, y0, z1}, {x1, y0, z0}, {x0, y0, z0}},
        };
        float[][] normals = {{0, 0, -1}, {1, 0, 0}, {0, 0, 1}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}};
        for (int i = 0; i < 6; i++) {
            Face f = c.faces[i];
            if (f == null) {
                continue;
            }
            float[][] cs = corners[i];
            float[] n = normals[i];
            float[][] uv = {{f.u0, f.v0}, {f.u1, f.v0}, {f.u1, f.v1}, {f.u0, f.v1}};
            // 時計まわり・反時計まわりどちらでも見えるように（NoCull）、順番に置く
            for (int k = 3; k >= 0; k--) {
                vc.addVertex(pose, cs[k][0], cs[k][1], cs[k][2]).setColor(0xFFFFFFFF).setUv(uv[k][0], uv[k][1])
                        .setOverlay(overlay).setLight(light).setNormal(pose, n[0], n[1], n[2]);
            }
        }
        ps.popPose();
    }

    /** 角度（度）でグループの原点を中心に回す */
    public static void rotate(PoseStack ps, float ox, float oy, float oz, float rx, float ry, float rz) {
        ps.translate(ox, oy, oz);
        ps.last().pose().rotateZYX((float) Math.toRadians(rz), (float) Math.toRadians(ry), (float) Math.toRadians(rx));
        ps.last().normal().rotateZYX((float) Math.toRadians(rz), (float) Math.toRadians(ry), (float) Math.toRadians(rx));
        ps.translate(-ox, -oy, -oz);
    }

    /** グループの原点を中心に大きさを変える */
    public static void scale(PoseStack ps, float ox, float oy, float oz, float sx, float sy, float sz) {
        ps.translate(ox, oy, oz);
        ps.scale(sx, sy, sz);
        ps.translate(-ox, -oy, -oz);
    }

    @SuppressWarnings("unused")
    private static Matrix4f unused(Matrix3f m) {
        return new Matrix4f();
    }
}
