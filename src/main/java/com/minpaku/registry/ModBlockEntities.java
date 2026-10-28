package com.minpaku.registry;

import com.minpaku.MinpakuJinrou;
import com.minpaku.block.MarkerBlockEntity;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;

public final class ModBlockEntities {
    public static BlockEntityType<MarkerBlockEntity> MARKER;

    private ModBlockEntities() {
    }

    /** ブロックを登録したあとに呼ぶ */
    public static void init() {
        Block[] blocks = ModBlocks.BLOCKS.values().toArray(new Block[0]);
        MARKER = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, MinpakuJinrou.id("marker"),
                BlockEntityType.Builder.of(MarkerBlockEntity::new, blocks).build(null));
    }
}
