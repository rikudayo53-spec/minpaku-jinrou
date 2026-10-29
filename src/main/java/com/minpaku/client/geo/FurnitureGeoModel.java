package com.minpaku.client.geo;

import com.minpaku.MinpakuJinrou;
import com.minpaku.block.MarkerBlockEntity;
import com.minpaku.game.MarkerType;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.model.GeoModel;

/** 家具の GeckoLib モデル（形・絵・アニメーションの場所は家具の id で決まる） */
public class FurnitureGeoModel extends GeoModel<MarkerBlockEntity> {
    private static String id(MarkerBlockEntity be) {
        MarkerType t = FurnitureGeo.typeOf(be);
        return t == null ? "kamado" : t.blockId;
    }

    @Override
    public ResourceLocation getModelResource(MarkerBlockEntity be) {
        return MinpakuJinrou.id("geo/block/" + id(be) + ".geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(MarkerBlockEntity be) {
        return MinpakuJinrou.id("textures/block/geo/" + id(be) + ".png");
    }

    @Override
    public ResourceLocation getAnimationResource(MarkerBlockEntity be) {
        return MinpakuJinrou.id("animations/block/" + id(be) + ".animation.json");
    }

    @Override
    public void setCustomAnimations(MarkerBlockEntity be, long instanceId, AnimationState<MarkerBlockEntity> state) {
        super.setCustomAnimations(be, instanceId, state);
        FurnitureGeo.apply(be, this, state.getPartialTick());
    }
}
