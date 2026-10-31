package com.minpaku.registry;

import com.minpaku.MinpakuJinrou;
import com.minpaku.block.MarkerBlock;
import com.minpaku.game.MarkerType;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;

import java.util.EnumMap;
import java.util.Map;

/** マップ用ブロックの登録（MarkerType ごとに1つ） */
public final class ModBlocks {
    public static final Map<MarkerType, MarkerBlock> BLOCKS = new EnumMap<>(MarkerType.class);

    private ModBlocks() {
    }

    public static void init() {
        for (MarkerType type : MarkerType.values()) {
            BlockBehaviour.Properties props = BlockBehaviour.Properties.of()
                    .strength(1.0f, 3600000f)
                    .noOcclusion()
                    .sound(soundFor(type));
            if (type == MarkerType.TOUROU || type == MarkerType.HIBACHI) {
                props = props.lightLevel(s -> 10);
            }
            if (type == MarkerType.BOILER || type == MarkerType.KAMADO || type == MarkerType.BATH) {
                props = props.lightLevel(s -> 7);
            }
            MarkerBlock block = Registry.register(BuiltInRegistries.BLOCK, MinpakuJinrou.id(type.blockId),
                    new MarkerBlock(props, type));
            Registry.register(BuiltInRegistries.ITEM, MinpakuJinrou.id(type.blockId),
                    new BlockItem(block, new Item.Properties()));
            BLOCKS.put(type, block);
        }
    }

    private static SoundType soundFor(MarkerType type) {
        return switch (type) {
            case BELL, BREAKER, GAS_VALVE, BOILER, VENT, VENT_B, VENT_C -> SoundType.METAL;
            case FUTON, SPAWN, DUST -> SoundType.WOOL;
            case FIELD -> SoundType.GRASS;
            case KAMADO, BATH -> SoundType.STONE;
            default -> SoundType.WOOD;
        };
    }
}
