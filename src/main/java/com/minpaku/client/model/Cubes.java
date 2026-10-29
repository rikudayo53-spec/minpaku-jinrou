package com.minpaku.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;

/** ブロックのテクスチャを貼った箱を描く（1ブロックより大きい面はテクスチャをくり返す） */
public final class Cubes {
    private static final Map<String, TextureAtlasSprite> SPRITES = new HashMap<>();

    private Cubes() {
    }

    public static TextureAtlasSprite sprite(String name) {
        return SPRITES.computeIfAbsent(name, n -> Minecraft.getInstance().getTextureAtlas(TextureAtlas.LOCATION_BLOCKS)
                .apply(ResourceLocation.parse(n.contains(":") ? n : "minecraft:block/" + n)));
    }

    /** テクスチャの再読み込み時に呼ぶ */
    public static void clear() {
        SPRITES.clear();
    }

    /** 箱（ブロック単位の座標）。argb で色をかける */
    public static void box(PoseStack.Pose p, VertexConsumer vc, TextureAtlasSprite sp, float x0, float y0, float z0,
                           float x1, float y1, float z1, int argb, int light) {
        face(vc, p, sp, argb, light, x0, y1, z0, x1 - x0, 0, 0, 0, 0, z1 - z0, 0, 1, 0);
        face(vc, p, sp, argb, light, x0, y0, z1, x1 - x0, 0, 0, 0, 0, z0 - z1, 0, -1, 0);
        face(vc, p, sp, argb, light, x0, y1, z1, x1 - x0, 0, 0, 0, y0 - y1, 0, 0, 0, 1);
        face(vc, p, sp, argb, light, x1, y1, z0, x0 - x1, 0, 0, 0, y0 - y1, 0, 0, 0, -1);
        face(vc, p, sp, argb, light, x1, y1, z1, 0, 0, z0 - z1, 0, y0 - y1, 0, 1, 0, 0);
        face(vc, p, sp, argb, light, x0, y1, z0, 0, 0, z1 - z0, 0, y0 - y1, 0, -1, 0, 0);
    }

    private static void face(VertexConsumer vc, PoseStack.Pose p, TextureAtlasSprite sp, int c, int light,
                             float ox, float oy, float oz, float ux, float uy, float uz, float vx, float vy, float vz,
                             float nx, float ny, float nz) {
        float lu = Math.abs(ux) + Math.abs(uy) + Math.abs(uz);
        float lv = Math.abs(vx) + Math.abs(vy) + Math.abs(vz);
        if (lu < 1e-5f || lv < 1e-5f) {
            return;
        }
        int nu = Math.max(1, (int) Math.ceil(lu - 1e-3f));
        int nv = Math.max(1, (int) Math.ceil(lv - 1e-3f));
        for (int i = 0; i < nu; i++) {
            float a0 = i / lu;
            float a1 = Math.min(lu, i + 1) / lu;
            for (int j = 0; j < nv; j++) {
                float b0 = j / lv;
                float b1 = Math.min(lv, j + 1) / lv;
                float u0 = sp.getU(0f);
                float u1 = sp.getU((a1 - a0) * lu);
                float v0 = sp.getV(0f);
                float v1 = sp.getV((b1 - b0) * lv);
                corner(vc, p, ox + ux * a0 + vx * b0, oy + uy * a0 + vy * b0, oz + uz * a0 + vz * b0, u0, v0, c, light, nx, ny, nz);
                corner(vc, p, ox + ux * a0 + vx * b1, oy + uy * a0 + vy * b1, oz + uz * a0 + vz * b1, u0, v1, c, light, nx, ny, nz);
                corner(vc, p, ox + ux * a1 + vx * b1, oy + uy * a1 + vy * b1, oz + uz * a1 + vz * b1, u1, v1, c, light, nx, ny, nz);
                corner(vc, p, ox + ux * a1 + vx * b0, oy + uy * a1 + vy * b0, oz + uz * a1 + vz * b0, u1, v0, c, light, nx, ny, nz);
            }
        }
    }

    private static void corner(VertexConsumer vc, PoseStack.Pose p, float x, float y, float z, float u, float v, int c,
                               int light, float nx, float ny, float nz) {
        vc.addVertex(p, x, y, z).setColor(c).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light)
                .setNormal(p, nx, ny, nz);
    }
}
