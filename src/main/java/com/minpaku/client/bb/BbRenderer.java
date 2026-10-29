package com.minpaku.client.bb;

import com.minpaku.client.model.Cubes;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

/**
 * .bbmodel を Blockbench の画面と同じ計算で描く。
 *  - グループ：原点を中心に、アニメーションの回転（Z→Y→X の順）・位置・大きさ
 *    （Blockbench の表示と同じく、回転の X と Y、位置の X は向きが逆）
 *  - テクスチャ名 killer_skin / victim_skin の立方体は、犯人・自分のスキンで描く（UV はマイクラのプレイヤーと同じ）
 *    名前の最後が #slim / #wide の立方体は、スキンの腕の太さに合うほうだけ描く
 *  - そのほかのテクスチャ名は、同じ名前のマイクラのブロックのテクスチャで描く（glow_ で始まると光る）
 *  - ref_ で始まるグループは描かない（Blockbench で見るための目印）
 */
public final class BbRenderer {
    private BbRenderer() {
    }

    /** 1コマ分の結果：グループの世界座標の行列 */
    public static final class Pose {
        public final Map<String, Matrix4f> world = new HashMap<>();

        public Vector3f origin(String group, BbModel m) {
            Matrix4f w = world.get(group);
            BbModel.Group g = m.groups.get(group);
            if (w == null || g == null) {
                return null;
            }
            Vector4f v = new Vector4f(0, 0, 0, 1);
            w.transform(v);
            return new Vector3f(v.x, v.y, v.z);
        }

        public Vector3f point(String group, BbModel m, float x, float y, float z) {
            Matrix4f w = world.get(group);
            BbModel.Group g = m.groups.get(group);
            if (w == null || g == null) {
                return null;
            }
            Vector4f v = new Vector4f((x - g.origin.x) / 16f, (y - g.origin.y) / 16f, (z - g.origin.z) / 16f, 1);
            w.transform(v);
            return new Vector3f(v.x, v.y, v.z);
        }
    }

    /**
     * 時刻 time（秒）の姿勢を計算する。extra はグループ名ごとに、さらにかける変換（おにぎり・猫又の演出など）。
     */
    public static Pose pose(BbModel m, float time, BiConsumer<String, Matrix4f> extra) {
        Pose p = new Pose();
        for (BbModel.Group g : m.roots) {
            walk(m, g, new Matrix4f(), new Vector3f(), time, extra, p);
        }
        return p;
    }

    private static void walk(BbModel m, BbModel.Group g, Matrix4f parent, Vector3f parentOrigin, float time,
                             BiConsumer<String, Matrix4f> extra, Pose out) {
        Vector3f rot = m.sample(g.name, "rotation", time, 0f);
        Vector3f pos = m.sample(g.name, "position", time, 0f);
        Vector3f scl = m.sample(g.name, "scale", time, 1f);
        Matrix4f w = new Matrix4f(parent);
        w.translate((g.origin.x - parentOrigin.x) / 16f, (g.origin.y - parentOrigin.y) / 16f, (g.origin.z - parentOrigin.z) / 16f);
        w.translate(-pos.x / 16f, pos.y / 16f, pos.z / 16f);
        if (extra != null) {
            extra.accept(g.name, w);
        }
        float rx = (float) Math.toRadians(g.rotation.x - rot.x);
        float ry = (float) Math.toRadians(g.rotation.y - rot.y);
        float rz = (float) Math.toRadians(g.rotation.z + rot.z);
        w.rotateZYX(rz, ry, rx);
        w.scale(scl.x, scl.y, scl.z);
        out.world.put(g.name, w);
        for (BbModel.Group c : g.groups) {
            walk(m, c, w, g.origin, time, extra, out);
        }
    }

    /** 描く。base はシーンの行列（PoseStack の今の状態）にかける */
    public static void render(BbModel m, Pose p, PoseStack ps, MultiBufferSource buf, PlayerSkin killer, PlayerSkin victim,
                              float bright) {
        for (BbModel.Group g : m.roots) {
            draw(m, g, p, ps, buf, killer, victim, bright);
        }
    }

    private static void draw(BbModel m, BbModel.Group g, Pose p, PoseStack ps, MultiBufferSource buf, PlayerSkin killer,
                             PlayerSkin victim, float bright) {
        if (!g.visible || g.name.startsWith("ref_")) {
            return;
        }
        Matrix4f w = p.world.get(g.name);
        if (w == null) {
            return;
        }
        Vector3f scl = m.sample(g.name, "scale", 0, 1f);
        ps.pushPose();
        ps.last().pose().mul(w);
        ps.last().normal().mul(new org.joml.Matrix3f(w).invert().transpose());
        for (BbModel.Cube c : g.cubes) {
            if (!c.visible) {
                continue;
            }
            cube(m, g, c, ps, buf, killer, victim, bright);
        }
        ps.popPose();
        for (BbModel.Group c : g.groups) {
            draw(m, c, p, ps, buf, killer, victim, bright);
        }
    }

    private static int shade(float bright) {
        int c = (int) (Mth.clamp(bright, 0f, 1f) * 255);
        return 0xFF000000 | (c << 16) | (c << 8) | c;
    }

    private static void cube(BbModel m, BbModel.Group g, BbModel.Cube c, PoseStack ps, MultiBufferSource buf, PlayerSkin killer,
                             PlayerSkin victim, float bright) {
        String tex = c.texture >= 0 && c.texture < m.textureNames.size() ? m.textureNames.get(c.texture) : "white_concrete";
        tex = tex.replace(".png", "");
        PlayerSkin skin = tex.equals("killer_skin") ? killer : tex.equals("victim_skin") ? victim : null;
        if (skin != null && (c.name.endsWith("#slim") || c.name.endsWith("#wide"))) {
            boolean slim = skin.model() == PlayerSkin.Model.SLIM;
            if (c.name.endsWith("#slim") != slim) {
                return;
            }
        }
        ps.pushPose();
        // 立方体の回転（原点を中心に）
        if (c.rotation.x != 0 || c.rotation.y != 0 || c.rotation.z != 0) {
            ps.translate((c.origin.x - g.origin.x) / 16f, (c.origin.y - g.origin.y) / 16f, (c.origin.z - g.origin.z) / 16f);
            ps.last().pose().rotateZYX((float) Math.toRadians(c.rotation.z), (float) Math.toRadians(c.rotation.y),
                    (float) Math.toRadians(c.rotation.x));
            ps.last().normal().rotateZYX((float) Math.toRadians(c.rotation.z), (float) Math.toRadians(c.rotation.y),
                    (float) Math.toRadians(c.rotation.x));
            ps.translate(-(c.origin.x - g.origin.x) / 16f, -(c.origin.y - g.origin.y) / 16f, -(c.origin.z - g.origin.z) / 16f);
        }
        if (skin != null) {
            VertexConsumer vc = buf.getBuffer(RenderType.entityTranslucent(skin.texture()));
            skinCube(vc, ps.last(), c, g.origin, m.texW, m.texH, shade(bright));
        } else {
            boolean glow = tex.startsWith("glow_");
            boolean glass = tex.startsWith("glass_");
            String block = tex.replace("glow_", "").replace("glass_", "").replace("mat_", "");
            VertexConsumer vc = buf.getBuffer(glass || glow ? RenderType.entityTranslucentCull(TextureAtlas.LOCATION_BLOCKS)
                    : RenderType.entityCutoutNoCull(TextureAtlas.LOCATION_BLOCKS));
            float in = c.inflate;
            int color = glass ? (0xB0000000 | (shade(bright) & 0xFFFFFF)) : shade(bright);
            Cubes.box(ps.last(), vc, Cubes.sprite(block),
                    (c.from.x - in - g.origin.x) / 16f, (c.from.y - in - g.origin.y) / 16f, (c.from.z - in - g.origin.z) / 16f,
                    (c.to.x + in - g.origin.x) / 16f, (c.to.y + in - g.origin.y) / 16f, (c.to.z + in - g.origin.z) / 16f,
                    color, glow ? LightTexture.FULL_BRIGHT : LightTexture.FULL_BRIGHT);
        }
        ps.popPose();
    }

    /**
     * スキンの立方体。マイクラのプレイヤーの箱（ModelPart.Cube）と同じ UV で描く。
     * Blockbench の座標 S と、マイクラのモデル座標 J は J = (-x, 24 - y, z)。
     */
    private static void skinCube(VertexConsumer vc, PoseStack.Pose pose, BbModel.Cube c, Vector3f go, int texW, int texH, int color) {
        float dx = c.to.x - c.from.x;
        float dy = c.to.y - c.from.y;
        float dz = c.to.z - c.from.z;
        float g = c.inflate;
        // J 座標の箱
        float minX = -c.to.x - g;
        float maxX = -c.from.x + g;
        float minY = 24 - c.to.y - g;
        float maxY = 24 - c.from.y + g;
        float minZ = c.from.z - g;
        float maxZ = c.to.z + g;
        boolean mirror = c.mirror;
        if (mirror) {
            float t = maxX;
            maxX = minX;
            minX = t;
        }
        float[][] v = {
                {minX, minY, minZ}, {maxX, minY, minZ}, {maxX, maxY, minZ}, {minX, maxY, minZ},
                {minX, minY, maxZ}, {maxX, minY, maxZ}, {maxX, maxY, maxZ}, {minX, maxY, maxZ}};
        float u = c.uvU;
        float w0 = c.uvV;
        float u0 = u;
        float u1 = u + dz;
        float u2 = u + dz + dx;
        float u3 = u2 + dx;
        float u4 = u + dz + dx + dz;
        float u5 = u4 + dx;
        float v0 = w0;
        float v1 = w0 + dz;
        float v2 = w0 + dz + dy;
        // DOWN, UP, WEST, NORTH, EAST, SOUTH（J の向き）
        poly(vc, pose, v, new int[]{5, 4, 0, 1}, u1, v0, u2, v1, texW, texH, mirror, 0, 1, 0, color, go);
        poly(vc, pose, v, new int[]{2, 3, 7, 6}, u2, v1, u3, v0, texW, texH, mirror, 0, -1, 0, color, go);
        poly(vc, pose, v, new int[]{0, 4, 7, 3}, u0, v1, u1, v2, texW, texH, mirror, -1, 0, 0, color, go);
        poly(vc, pose, v, new int[]{1, 0, 3, 2}, u1, v1, u2, v2, texW, texH, mirror, 0, 0, -1, color, go);
        poly(vc, pose, v, new int[]{5, 1, 2, 6}, u2, v1, u4, v2, texW, texH, mirror, 1, 0, 0, color, go);
        poly(vc, pose, v, new int[]{4, 5, 6, 7}, u4, v1, u5, v2, texW, texH, mirror, 0, 0, 1, color, go);
    }

    private static void poly(VertexConsumer vc, PoseStack.Pose pose, float[][] v, int[] idx, float ua, float va, float ub, float vb,
                             int texW, int texH, boolean mirror, float nx, float ny, float nz, int color, Vector3f go) {
        // ModelPart.Polygon と同じ UV の付け方
        float[][] uv = {{ub, va}, {ua, va}, {ua, vb}, {ub, vb}};
        int[] order = mirror ? new int[]{3, 2, 1, 0} : new int[]{0, 1, 2, 3};
        if (mirror) {
            nx = -nx;
        }
        // J の法線 → S の法線
        float sx = -nx;
        float sy = -ny;
        float sz = nz;
        for (int k : order) {
            float[] p = v[idx[k]];
            // J → S（ピクセル）→ グループの原点からの位置（ブロック）
            float x = (-p[0] - go.x) / 16f;
            float y = (24 - p[1] - go.y) / 16f;
            float z = (p[2] - go.z) / 16f;
            vc.addVertex(pose, x, y, z).setColor(color).setUv(uv[k][0] / texW, uv[k][1] / texH)
                    .setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(pose, sx, sy, sz);
        }
    }
}
