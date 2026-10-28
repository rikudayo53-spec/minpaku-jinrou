package com.minpaku;

import com.minpaku.game.GameCommands;
import com.minpaku.game.GameEvents;
import com.minpaku.network.ModNetworking;
import com.minpaku.registry.ModBlockEntities;
import com.minpaku.registry.ModBlocks;
import com.minpaku.registry.ModEntities;
import com.minpaku.registry.ModItems;
import net.fabricmc.api.ModInitializer;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 民泊人狼のエントリーポイント（サーバー/クライアント共通）。
 */
public class MinpakuJinrou implements ModInitializer {
    public static final String MOD_ID = "minpaku";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }

    @Override
    public void onInitialize() {
        ModBlocks.init();
        ModBlockEntities.init();
        ModItems.init();
        ModEntities.init();
        ModNetworking.registerCommon();
        GameEvents.register();
        GameCommands.register();
        LOGGER.info("民泊人狼 loaded");
    }
}
