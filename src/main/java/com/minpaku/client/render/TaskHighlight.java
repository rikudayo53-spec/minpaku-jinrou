package com.minpaku.client.render;

import com.minpaku.client.ClientGame;
import com.minpaku.network.Packets;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** 自分のタスクのブロックを黄色い枠で光らせる */
public final class TaskHighlight {
    private TaskHighlight() {
    }

    public static void render(WorldRenderContext ctx) {
        if (!ClientGame.inGame() || ctx.matrixStack() == null || ctx.consumers() == null) {
            return;
        }
        PoseStack ps = ctx.matrixStack();
        MultiBufferSource buffers = ctx.consumers();
        Vec3 cam = ctx.camera().getPosition();
        VertexConsumer lines = buffers.getBuffer(RenderType.lines());
        float pulse = 0.6f + 0.4f * (float) Math.sin(System.currentTimeMillis() / 250.0);
        for (Packets.TaskInfo t : ClientGame.state.tasks()) {
            if (t.done()) {
                continue;
            }
            AABB box = new AABB(t.pos()).inflate(0.02).move(-cam.x, -cam.y, -cam.z);
            LevelRenderer.renderLineBox(ps, lines, box, 1f, 0.85f, 0.2f, pulse);
        }
    }
}
