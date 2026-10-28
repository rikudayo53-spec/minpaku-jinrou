package com.minpaku.client.model;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.minpaku.MinpakuJinrou;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * Blender で作った3Dモデル（tools/blender/minpaku_models.py で書き出した JSON）。
 * 座標はブロック単位で、底の中心が原点、正面が北（-Z）。
 * 三角形ごとに頂点・法線を持ち、パーツ（マテリアル）ごとに色・透明度・光るかを持つ。
 */
public final class StaticMesh {
    public static final ResourceLocation WHITE = MinpakuJinrou.id("textures/misc/white.png");

    private record Part(int r, int g, int b, int a, boolean emissive, float[] pos, float[] nrm) {
    }

    private final List<Part> solid = new ArrayList<>();
    private final List<Part> translucent = new ArrayList<>();

    private StaticMesh() {
    }

    public static StaticMesh parse(JsonObject json) {
        StaticMesh mesh = new StaticMesh();
        for (JsonElement el : GsonHelper.getAsJsonArray(json, "parts")) {
            JsonObject p = el.getAsJsonObject();
            JsonArray c = GsonHelper.getAsJsonArray(p, "color");
            float alpha = GsonHelper.getAsFloat(p, "alpha", 1f);
            float[] pos = floats(GsonHelper.getAsJsonArray(p, "p"));
            float[] nrm = floats(GsonHelper.getAsJsonArray(p, "n"));
            if (pos.length != nrm.length || pos.length % 9 != 0) {
                continue;
            }
            Part part = new Part(to255(c.get(0).getAsFloat()), to255(c.get(1).getAsFloat()), to255(c.get(2).getAsFloat()),
                    to255(alpha), GsonHelper.getAsBoolean(p, "emissive", false), pos, nrm);
            (alpha < 0.99f ? mesh.translucent : mesh.solid).add(part);
        }
        return mesh;
    }

    private static int to255(float v) {
        return Math.max(0, Math.min(255, Math.round(v * 255f)));
    }

    private static float[] floats(JsonArray arr) {
        float[] out = new float[arr.size()];
        for (int i = 0; i < out.length; i++) {
            out[i] = arr.get(i).getAsFloat();
        }
        return out;
    }

    /** 原点（底の中心）に描く。light は明るさ（光るパーツは常に最大） */
    public void render(PoseStack ps, MultiBufferSource buffers, int light, int overlay) {
        render(ps, buffers, light, overlay, 1f, 1f, 1f);
    }

    /** 色をかけて描く（光るパーツはそのまま）。暗がりから現れる演出などに使う */
    public void render(PoseStack ps, MultiBufferSource buffers, int light, int overlay, float tr, float tg, float tb) {
        tint[0] = tr;
        tint[1] = tg;
        tint[2] = tb;
        PoseStack.Pose pose = ps.last();
        if (!solid.isEmpty()) {
            VertexConsumer vc = buffers.getBuffer(RenderType.entityCutoutNoCull(WHITE));
            for (Part p : solid) {
                emit(vc, pose, p, p.emissive ? LightTexture.FULL_BRIGHT : light, overlay);
            }
        }
        if (!translucent.isEmpty()) {
            VertexConsumer vc = buffers.getBuffer(RenderType.entityTranslucent(WHITE));
            for (Part p : translucent) {
                emit(vc, pose, p, p.emissive ? LightTexture.FULL_BRIGHT : light, overlay);
            }
        }
    }

    private static void emit(VertexConsumer vc, PoseStack.Pose pose, Part p, int light, int overlay) {
        float[] pos = p.pos;
        float[] n = p.nrm;
        for (int i = 0; i < pos.length; i += 9) {
            // 描画は四角形単位なので、3つ目の頂点をもう1回出して三角形にする
            vertex(vc, pose, p, pos, n, i, light, overlay);
            vertex(vc, pose, p, pos, n, i + 3, light, overlay);
            vertex(vc, pose, p, pos, n, i + 6, light, overlay);
            vertex(vc, pose, p, pos, n, i + 6, light, overlay);
        }
    }

    private static final float[] tint = {1f, 1f, 1f};

    private static void vertex(VertexConsumer vc, PoseStack.Pose pose, Part p, float[] pos, float[] n, int k,
                               int light, int overlay) {
        vc.addVertex(pose, pos[k], pos[k + 1], pos[k + 2])
                .setColor(p.emissive ? p.r : (int) (p.r * tint[0]), p.emissive ? p.g : (int) (p.g * tint[1]),
                        p.emissive ? p.b : (int) (p.b * tint[2]), p.a)
                .setUv(0.5f, 0.5f)
                .setOverlay(overlay)
                .setLight(light)
                .setNormal(pose, n[k], n[k + 1], n[k + 2]);
    }
}
