package com.minpaku.client.render;

import com.minpaku.entity.BotEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.resources.ResourceLocation;

/** ボット：ふつうのプレイヤーと同じ見た目（標準スキンのどれか） */
public class BotRenderer extends LivingEntityRenderer<BotEntity, PlayerModel<BotEntity>> {
    private final PlayerModel<BotEntity> wide;
    private final PlayerModel<BotEntity> slim;

    public BotRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new PlayerModel<>(ctx.bakeLayer(ModelLayers.PLAYER), false), 0.5f);
        this.wide = this.model;
        this.slim = new PlayerModel<>(ctx.bakeLayer(ModelLayers.PLAYER_SLIM), true);
    }

    @Override
    public void render(BotEntity bot, float yaw, float pt, PoseStack ps, MultiBufferSource buffers, int light) {
        PlayerSkin skin = DefaultPlayerSkin.get(bot.getUUID());
        this.model = skin.model() == PlayerSkin.Model.SLIM ? slim : wide;
        super.render(bot, yaw, pt, ps, buffers, light);
    }

    @Override
    protected void scale(BotEntity bot, PoseStack ps, float pt) {
        ps.scale(0.9375f, 0.9375f, 0.9375f);
    }

    @Override
    public ResourceLocation getTextureLocation(BotEntity bot) {
        return DefaultPlayerSkin.get(bot.getUUID()).texture();
    }
}
