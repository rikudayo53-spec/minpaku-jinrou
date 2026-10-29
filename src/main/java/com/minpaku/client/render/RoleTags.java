package com.minpaku.client.render;

import com.minpaku.client.ClientGame;
import com.minpaku.client.Ui;
import com.minpaku.game.Role;
import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/** 神（女将）には、みんなの頭の上に役職が見える */
public final class RoleTags {
    private RoleTags() {
    }

    public static void render(WorldRenderContext ctx) {
        Minecraft mc = Minecraft.getInstance();
        if (!ClientGame.inGame() || ClientGame.knownRoles.isEmpty() || mc.level == null || mc.player == null
                || ctx.matrixStack() == null || ctx.consumers() == null) {
            return;
        }
        PoseStack ps = ctx.matrixStack();
        Vec3 cam = ctx.camera().getPosition();
        Font font = mc.font;
        float pt = ctx.tickCounter().getGameTimeDeltaPartialTick(false);
        for (Entity e : mc.level.entitiesForRendering()) {
            String id = ClientGame.knownRoles.get(e.getUUID());
            if (id == null || e == mc.player || e.isSpectator() || e.distanceToSqr(cam) > 48 * 48) {
                continue;
            }
            Role r = Role.byId(id);
            Vec3 p = e.getPosition(pt);
            ps.pushPose();
            ps.translate(p.x - cam.x, p.y + e.getBbHeight() + 0.75 - cam.y, p.z - cam.z);
            ps.mulPose(ctx.camera().rotation());
            ps.scale(0.025f, -0.025f, 0.025f);
            Component text = Ui.sans(r.name);
            float w = font.width(text);
            font.drawInBatch(text, -w / 2f, 0, 0xFF000000 | r.color, false, ps.last().pose(), ctx.consumers(),
                    Font.DisplayMode.SEE_THROUGH, 0x60000000, LightTexture.FULL_BRIGHT);
            ps.popPose();
        }
    }
}
