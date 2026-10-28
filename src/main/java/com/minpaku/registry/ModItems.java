package com.minpaku.registry;

import com.minpaku.MinpakuJinrou;
import com.minpaku.game.MarkerType;
import com.minpaku.item.RoleItem;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;

public final class ModItems {
    public static final RoleItem KEY = register("manager_key", RoleItem.Kind.KEY, Rarity.EPIC);
    public static final RoleItem CRYSTAL = register("seer_crystal", RoleItem.Kind.CRYSTAL, Rarity.RARE);
    public static final RoleItem CHARM = register("knight_charm", RoleItem.Kind.CHARM, Rarity.RARE);
    public static final RoleItem TALISMAN = register("sabotage_talisman", RoleItem.Kind.TALISMAN, Rarity.RARE);

    private ModItems() {
    }

    private static RoleItem register(String name, RoleItem.Kind kind, Rarity rarity) {
        return Registry.register(BuiltInRegistries.ITEM, MinpakuJinrou.id(name),
                new RoleItem(new Item.Properties().stacksTo(1).rarity(rarity), kind));
    }

    public static void init() {
        Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, MinpakuJinrou.id("main"), FabricItemGroup.builder()
                .title(Component.literal("民泊人狼"))
                .icon(() -> new ItemStack(ModBlocks.BLOCKS.get(MarkerType.BELL)))
                .displayItems((params, output) -> {
                    output.accept(KEY);
                    for (MarkerType t : MarkerType.values()) {
                        output.accept(ModBlocks.BLOCKS.get(t));
                    }
                    output.accept(CRYSTAL);
                    output.accept(CHARM);
                    output.accept(TALISMAN);
                })
                .build());
    }
}
