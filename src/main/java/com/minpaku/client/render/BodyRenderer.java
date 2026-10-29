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

/** 死体のかわりの遺影（持ち主の顔の写真と、花瓶とお線香） */
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

    /**
     * 死体のかわりに「遺影」を置く：黒い額に持ち主の顔、黒いリボン、両わきに菊の花瓶とお線香（Blender のモデル iei）。
     * 当たり判定（通報・探偵の調べ）は今までどおり。
     */
    @Override
    public void render(BodyEntity body, float yaw, float pt, PoseStack ps, MultiBufferSource buffers, int light) {
        PlayerSkin skin = skin(body);
        ps.pushPose();
        ps.mulPose(Axis.YP.rotationDegrees(180f - body.getYRot()));
        float s = 0.9f;
        ps.scale(s, s, s);
        com.minpaku.client.model.StaticMesh mesh = com.minpaku.client.model.MeshCache.get("iei");
        if (mesh != null) {
            mesh.render(ps, buffers, light, OverlayTexture.NO_OVERLAY);
        }
        // 写真（額の白い板の前に、顔を少しモノクロっぽく）
        ps.translate(0, 0.6, 0.059);
        ps.mulPose(Axis.XP.rotationDegrees(8f));
        ps.translate(0, 0.02, -0.004);
        var buf = buffers.getBuffer(RenderType.entityTranslucent(skin.texture()));
        face(buf, ps.last(), 0.17f, 8, 8, 0xFFD9D4CC, light, 0f);
        face(buf, ps.last(), 0.18f, 40, 8, 0xFFD9D4CC, light, -0.002f);
        ps.popPose();
        super.render(body, yaw, pt, ps, buffers, light);
    }

    /** スキンの顔（u,v = 64×64 の中の左上）を、手前（-Z）向きの四角に貼る */
    private static void face(com.mojang.blaze3d.vertex.VertexConsumer buf, PoseStack.Pose pose, float r, int u, int v, int argb,
                             int light, float dz) {
        float u0 = u / 64f;
        float u1 = (u + 8) / 64f;
        float v0 = v / 64f;
        float v1 = (v + 8) / 64f;
        buf.addVertex(pose, r, r, dz).setColor(argb).setUv(u0, v0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, 0, 0, -1);
        buf.addVertex(pose, r, -r, dz).setColor(argb).setUv(u0, v1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, 0, 0, -1);
        buf.addVertex(pose, -r, -r, dz).setColor(argb).setUv(u1, v1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, 0, 0, -1);
        buf.addVertex(pose, -r, r, dz).setColor(argb).setUv(u1, v0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, 0, 0, -1);
    }

    @Override
    public ResourceLocation getTextureLocation(BodyEntity body) {
        return skin(body).texture();
    }
}
