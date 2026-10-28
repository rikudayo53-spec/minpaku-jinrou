package com.minpaku.block;

import com.minpaku.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** 専用ブロックの見た目（Blender で作った3Dモデル）を描くためだけのブロックエンティティ */
public class MarkerBlockEntity extends BlockEntity {
    public MarkerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MARKER, pos, state);
    }
}
