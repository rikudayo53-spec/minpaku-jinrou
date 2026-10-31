package com.minpaku.client.render;

import com.minpaku.client.ClientGame;
import com.minpaku.game.MarkerType;
import com.minpaku.network.Packets;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/**
 * 妨害（停電・ガス漏れ）が起きているあいだ、直す場所の方向へ短いレーザーを出す。
 * 自分の画面にだけ描く（ほかの人には見えない）。人狼側には出さない。
 */
public final class SabotageLaser {
    private SabotageLaser() {
    }

    private static final float LENGTH = 3.2f;

    public static void render(WorldRenderContext ctx) {
        Minecraft mc = Minecraft.getInstance();
        if (!ClientGame.inGame() || mc.player == null || ctx.matrixStack() == null || ctx.consumers() == null) {
            return;
        }
        Packets.State s = ClientGame.state;
        if (!s.blackout() && s.gasTicks() < 0) {
            return;
        }
        com.minpaku.game.Role role = ClientGame.role();
        if (role != null && role.canSabotage()) {
            return;
        }
        Vec3 cam = ctx.camera().getPosition();
        float pt = ctx.tickCounter().getGameTimeDeltaPartialTick(false);
        Vec3 from = mc.player.getPosition(pt).add(0, 1.1, 0);
        PoseStack ps = ctx.matrixStack();
        VertexConsumer vc = ctx.consumers().getBuffer(RenderType.lightning());
        float pulse = 0.65f + 0.35f * (float) Math.sin(System.currentTimeMillis() / 180.0);
        for (ClientGame.MapFix f : ClientGame.fixes) {
            boolean need = switch (f.type()) {
                case BREAKER -> s.blackout();
                case GAS_VALVE -> s.gasTicks() >= 0 && !s.valveFixed();
                case BOILER -> s.gasTicks() >= 0 && !s.boilerFixed();
                default -> false;
            };
            if (!need) {
                continue;
            }
            Vec3 target = Vec3.atCenterOf(f.pos());
            Vec3 dir = target.subtract(from);
            double dist = dir.length();
            if (dist < 1.5) {
                continue;
            }
            dir = dir.normalize();
            Vec3 a = from.add(dir.scale(0.6));
            Vec3 b = from.add(dir.scale(Math.min(LENGTH, dist - 0.8)));
            int col = f.type() == MarkerType.BREAKER ? 0xFFD54F : 0xFF5A3C;
            beam(ps, vc, a.subtract(cam), b.subtract(cam), cam, 0.06f, col, pulse);
            beam(ps, vc, a.subtract(cam), b.subtract(cam), cam, 0.022f, 0xFFFFFF, pulse);
        }
    }

    /** 2点のあいだの細い帯（カメラの方を向く） */
    private static void beam(PoseStack ps, VertexConsumer vc, Vec3 a, Vec3 b, Vec3 cam, float width, int rgb, float alpha) {
        Vec3 d = b.subtract(a);
        Vec3 mid = a.add(d.scale(0.5));
        Vec3 side = d.cross(mid).normalize().scale(width);
        if (!Double.isFinite(side.x)) {
            return;
        }
        Matrix4f m = ps.last().pose();
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int bl = rgb & 0xFF;
        int al = (int) (alpha * 200);
        // 先に向かって細く・薄く
        vc.addVertex(m, (float) (a.x - side.x), (float) (a.y - side.y), (float) (a.z - side.z)).setColor(r, g, bl, al);
        vc.addVertex(m, (float) (a.x + side.x), (float) (a.y + side.y), (float) (a.z + side.z)).setColor(r, g, bl, al);
        vc.addVertex(m, (float) (b.x + side.x * 0.3), (float) (b.y + side.y * 0.3), (float) (b.z + side.z * 0.3)).setColor(r, g, bl, 0);
        vc.addVertex(m, (float) (b.x - side.x * 0.3), (float) (b.y - side.y * 0.3), (float) (b.z - side.z * 0.3)).setColor(r, g, bl, 0);
    }
}
