package com.minpaku.client.render;

import com.minpaku.client.ClientGame;
import com.minpaku.client.model.Cubes;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.world.phys.Vec3;

/**
 * マップの外を「時空のゆがみ」でかくす。
 * マップの範囲の中にいるとき、範囲のふちに、暗い壁と、ゆらぐ渦（ネザーゲートの模様）を立てる。
 */
public final class Distortion {
    private Distortion() {
    }

    private static final int REACH = 56;
    private static final int TILE = 8;

    public static void render(WorldRenderContext ctx) {
        int[] b = ClientGame.bounds;
        Minecraft mc = Minecraft.getInstance();
        if (b == null || mc.player == null || ctx.matrixStack() == null || ctx.consumers() == null) {
            return;
        }
        Vec3 cam = ctx.camera().getPosition();
        double x1 = b[0];
        double z1 = b[2];
        double x2 = b[3] + 1;
        double z2 = b[5] + 1;
        boolean inside = cam.x > x1 && cam.x < x2 && cam.z > z1 && cam.z < z2 && cam.y > b[1] - 8;
        if (!inside) {
            return;
        }
        PoseStack ps = ctx.matrixStack();
        MultiBufferSource buf = ctx.consumers();
        VertexConsumer solid = buf.getBuffer(RenderType.entitySolid(TextureAtlas.LOCATION_BLOCKS));
        VertexConsumer swirl = buf.getBuffer(RenderType.entityTranslucent(TextureAtlas.LOCATION_BLOCKS));
        TextureAtlasSprite dark = Cubes.sprite("black_concrete");
        TextureAtlasSprite portal = Cubes.sprite("nether_portal");
        float time = (System.currentTimeMillis() % 100000L) / 1000f;
        int yLo = (int) Math.max(b[1] - 2, Math.floor(cam.y) - 32);
        int yHi = (int) Math.floor(cam.y) + 64;
        ps.pushPose();
        ps.translate(-cam.x, -cam.y, -cam.z);
        PoseStack.Pose pose = ps.last();
        // 北と南の壁（z = 一定）、西と東の壁（x = 一定）
        for (int side = 0; side < 4; side++) {
            boolean alongX = side < 2;
            double fixed = side == 0 ? z1 : side == 1 ? z2 : side == 2 ? x1 : x2;
            double camAlong = alongX ? cam.x : cam.z;
            double lo = alongX ? x1 : z1;
            double hi = alongX ? x2 : z2;
            double dist = Math.abs((alongX ? cam.z : cam.x) - fixed);
            if (dist > REACH + 16) {
                continue;
            }
            double from = Math.max(lo, Math.floor((camAlong - REACH) / TILE) * TILE);
            double to = Math.min(hi, camAlong + REACH);
            float out = side == 0 || side == 2 ? -1 : 1;
            for (double a = from; a < to; a += TILE) {
                double a2 = Math.min(to, a + TILE);
                for (int y = yLo; y < yHi; y += TILE) {
                    int y2 = Math.min(yHi, y + TILE);
                    // ゆらぐ色（場所と時間で少しずつ変える）
                    double wave = Math.sin(a * 0.21 + y * 0.17 + time * 1.7) * 0.5 + 0.5;
                    int r = (int) (110 + wave * 80);
                    int g = (int) (40 + wave * 30);
                    int bl = (int) (200 + wave * 55);
                    int tint = 0xE0000000 | (r << 16) | (g << 8) | bl;
                    if (alongX) {
                        float zf = (float) fixed;
                        Cubes.box(pose, solid, dark, (float) a, y, zf + out * 0.6f, (float) a2, y2, zf + out * 0.62f,
                                0xFF140620, LightTexture.FULL_BRIGHT);
                        Cubes.box(pose, swirl, portal, (float) a, y, zf + out * 0.05f, (float) a2, y2, zf + out * 0.06f,
                                tint, LightTexture.FULL_BRIGHT);
                    } else {
                        float xf = (float) fixed;
                        Cubes.box(pose, solid, dark, xf + out * 0.6f, y, (float) a, xf + out * 0.62f, y2, (float) a2,
                                0xFF140620, LightTexture.FULL_BRIGHT);
                        Cubes.box(pose, swirl, portal, xf + out * 0.05f, y, (float) a, xf + out * 0.06f, y2, (float) a2,
                                tint, LightTexture.FULL_BRIGHT);
                    }
                }
            }
        }
        ps.popPose();
    }
}
