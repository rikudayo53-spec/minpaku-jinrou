package com.minpaku.client.render;

import com.minpaku.block.MarkerBlock;
import com.minpaku.block.MarkerBlockEntity;
import com.minpaku.client.model.FurnitureArt;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

/** 家具（専用ブロック）を描く。向き（FACING）に合わせて回し、タスク中は動かす */
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

    /** ブロックエンティティの状態から、今の動き */
    public static FurnitureArt.Anim anim(MarkerBlockEntity be, float pt) {
        if (be.getLevel() == null) {
            return FurnitureArt.Anim.IDLE;
        }
        float now = be.getLevel().getGameTime() + pt;
        float since = now - be.lastPulse;
        float work = be.work;
        // しばらく誰も触っていなければ元に戻る（終わった直後は少しのあいだ完成の形を見せる）
        if (since > 80) {
            work = 0f;
        }
        // 進み具合はタスクの区切りごとに飛ぶので、画面ではなめらかに追いつかせる
        long nano = System.nanoTime();
        float dt = be.shownAt == 0 ? 1f : Math.min(1f, (nano - be.shownAt) / 1.0e9f);
        be.shownAt = nano;
        float rate = work < be.shown ? 3f : 6f;
        be.shown += (work - be.shown) * Math.min(1f, dt * rate);
        if (Math.abs(work - be.shown) < 0.002f) {
            be.shown = work;
        }
        return new FurnitureArt.Anim(be.shown, Math.max(0f, since), now);
    }

    @Override
    public void render(MarkerBlockEntity be, float partialTick, PoseStack ps, MultiBufferSource buffers, int light, int overlay) {
        BlockState state = be.getBlockState();
        if (!(state.getBlock() instanceof MarkerBlock block)) {
            return;
        }
        ps.pushPose();
        ps.translate(0.5, 0.0, 0.5);
        ps.mulPose(Axis.YP.rotationDegrees(yaw(state.getValue(MarkerBlock.FACING))));
        ps.translate(-0.5, 0.0, -0.5);
        FurnitureArt.draw(block.type(), anim(be, partialTick), ps, buffers, light);
        ps.popPose();
    }

    @Override
    public boolean shouldRenderOffScreen(MarkerBlockEntity be) {
        return true;
    }
}
