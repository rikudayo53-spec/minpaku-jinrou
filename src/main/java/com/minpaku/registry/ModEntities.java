package com.minpaku.registry;

import com.minpaku.MinpakuJinrou;
import com.minpaku.entity.BodyEntity;
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

    private ModEntities() {
    }

    public static void init() {
    }
}
