package com.minpaku.client.render;

import com.minpaku.client.ClientGame;
import com.minpaku.entity.BodyEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.client.resources.PlayerSkin;

/** 死体：持ち主のスキンで、あお向けに倒れた姿を描く */
public class BodyRenderer extends EntityRenderer<BodyEntity> {
    private final PlayerModel<LivingEntity> wide;
    private final PlayerModel<LivingEntity> slim;

    public BodyRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
        this.wide = new PlayerModel<>(ctx.bakeLayer(ModelLayers.PLAYER), false);
        this.slim = new PlayerModel<>(ctx.bakeLayer(ModelLayers.PLAYER_SLIM), true);
        this.shadowRadius = 0.5f;
    }

    private PlayerSkin skin(BodyEntity body) {
        return body.owner().map(ClientGame::skin).orElseGet(() -> DefaultPlayerSkin.get(body.getUUID()));
    }

    @Override
    public void render(BodyEntity body, float yaw, float pt, PoseStack ps, MultiBufferSource buffers, int light) {
        PlayerSkin skin = skin(body);
        PlayerModel<LivingEntity> model = skin.model() == PlayerSkin.Model.SLIM ? slim : wide;
        model.young = false;
        model.crouching = false;
        model.riding = false;
        // 手足を少し広げて倒れている感じにする
        model.head.xRot = 0.3f;
        model.head.yRot = 0.4f;
        model.rightArm.zRot = 0.9f;
        model.leftArm.zRot = -1.1f;
        model.rightArm.xRot = 0;
        model.leftArm.xRot = 0;
        model.rightLeg.zRot = 0.25f;
        model.leftLeg.zRot = -0.2f;
        model.rightLeg.xRot = 0;
        model.leftLeg.xRot = 0;
        model.body.xRot = 0;
        model.hat.copyFrom(model.head);
        model.jacket.copyFrom(model.body);
        model.rightSleeve.copyFrom(model.rightArm);
        model.leftSleeve.copyFrom(model.leftArm);
        model.rightPants.copyFrom(model.rightLeg);
        model.leftPants.copyFrom(model.leftLeg);

        ps.pushPose();
        ps.mulPose(Axis.YP.rotationDegrees(180f - body.getYRot()));
        ps.translate(0.0, 0.13, 0.9);
        ps.mulPose(Axis.XP.rotationDegrees(-90f));
        ps.scale(-1f, -1f, 1f);
        ps.translate(0.0, -1.501, 0.0);
        model.renderToBuffer(ps, buffers.getBuffer(RenderType.entityCutoutNoCull(skin.texture())), light, OverlayTexture.NO_OVERLAY);
        ps.popPose();

        // 足もとの血だまり（赤い平らな板）
        ps.pushPose();
        ps.translate(0, 0.02, 0);
        var buf = buffers.getBuffer(RenderType.entityTranslucent(ResourceLocation.withDefaultNamespace("textures/block/red_concrete.png")));
        var pose = ps.last();
        float r = 0.7f;
        int a = 170;
        buf.addVertex(pose, -r, 0, -r).setColor(170, 20, 20, a).setUv(0, 0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, 0, 1, 0);
        buf.addVertex(pose, -r, 0, r).setColor(170, 20, 20, a).setUv(0, 1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, 0, 1, 0);
        buf.addVertex(pose, r, 0, r).setColor(170, 20, 20, a).setUv(1, 1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, 0, 1, 0);
        buf.addVertex(pose, r, 0, -r).setColor(170, 20, 20, a).setUv(1, 0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, 0, 1, 0);
        ps.popPose();

        super.render(body, yaw, pt, ps, buffers, light);
    }

    @Override
    public ResourceLocation getTextureLocation(BodyEntity body) {
        return skin(body).texture();
    }
}
