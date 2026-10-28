package com.minpaku.registry;

import com.minpaku.MinpakuJinrou;
import com.minpaku.entity.BodyEntity;
import com.minpaku.entity.BotEntity;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

public final class ModEntities {
    public static final EntityType<BodyEntity> BODY = Registry.register(BuiltInRegistries.ENTITY_TYPE,
            MinpakuJinrou.id("body"),
            EntityType.Builder.<BodyEntity>of(BodyEntity::new, MobCategory.MISC)
                    .sized(1.2f, 0.35f)
                    .clientTrackingRange(10)
                    .build("body"));

    public static final EntityType<BotEntity> BOT = Registry.register(BuiltInRegistries.ENTITY_TYPE,
            MinpakuJinrou.id("bot"),
            EntityType.Builder.<BotEntity>of(BotEntity::new, MobCategory.MISC)
                    .sized(0.6f, 1.8f)
                    .clientTrackingRange(10)
                    .build("bot"));

    private ModEntities() {
    }

    public static void init() {
        FabricDefaultAttributeRegistry.register(BOT, BotEntity.createAttributes());
    }
}
