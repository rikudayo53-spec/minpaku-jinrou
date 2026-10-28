package com.minpaku.client.render;

import com.minpaku.block.MarkerBlock;
import com.minpaku.block.MarkerBlockEntity;
import com.minpaku.client.model.MeshCache;
import com.minpaku.client.model.StaticMesh;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

/** 専用ブロックを Blender の3Dモデルで描く。向き（FACING）に合わせて回す */
public class MarkerRenderer implements BlockEntityRenderer<MarkerBlockEntity> {

    public MarkerRenderer(BlockEntityRendererProvider.Context ctx) {
    }

    public static float yaw(Direction facing) {
        return switch (facing) {
            case SOUTH -> 180f;
            case EAST -> -90f;
            case WEST -> 90f;
            default -> 0f;
        };
    }

    @Override
    public void render(MarkerBlockEntity be, float partialTick, PoseStack ps, MultiBufferSource buffers, int light, int overlay) {
        BlockState state = be.getBlockState();
        if (!(state.getBlock() instanceof MarkerBlock block)) {
            return;
        }
        StaticMesh mesh = MeshCache.get(block.type().blockId);
        if (mesh == null) {
            return;
        }
        ps.pushPose();
        ps.translate(0.5, 0.0, 0.5);
        ps.mulPose(Axis.YP.rotationDegrees(yaw(state.getValue(MarkerBlock.FACING))));
        mesh.render(ps, buffers, light, overlay);
        ps.popPose();
    }
}
