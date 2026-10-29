package com.minpaku.client.geo;

import com.minpaku.block.MarkerBlock;
import com.minpaku.block.MarkerBlockEntity;
import com.minpaku.client.model.FurnitureArt;
import com.minpaku.client.model.FurnitureHiRes;
import com.minpaku.client.render.MarkerRenderer;
import com.minpaku.game.MarkerType;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoBlockRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

import java.util.HashMap;
import java.util.Map;

/** 家具を GeckoLib で描く（GeckoLib のモデルがない家具は今までの描き方） */
public class FurnitureGeoRenderer extends GeoBlockRenderer<MarkerBlockEntity> {
    private final MarkerRenderer fallback;
    private static final Map<ResourceLocation, Boolean> GLOW = new HashMap<>();

    public FurnitureGeoRenderer(BlockEntityRendererProvider.Context ctx) {
        super(new FurnitureGeoModel());
        this.fallback = new MarkerRenderer(ctx);
        // 名前が glow_ の箱は光る（_glowmask の絵があるときだけ）
        addRenderLayer(new AutoGlowingGeoLayer<>(this) {
            @Override
            public void render(PoseStack ps, MarkerBlockEntity be, BakedGeoModel model, RenderType renderType,
                               MultiBufferSource buf, com.mojang.blaze3d.vertex.VertexConsumer vc, float partial,
                               int light, int overlay) {
                ResourceLocation tex = getTextureResource(be);
                ResourceLocation mask = ResourceLocation.fromNamespaceAndPath(tex.getNamespace(),
                        tex.getPath().replace(".png", "_glowmask.png"));
                boolean has = GLOW.computeIfAbsent(mask, m -> Minecraft.getInstance().getResourceManager().getResource(m).isPresent());
                if (has) {
                    super.render(ps, be, model, renderType, buf, vc, partial, light, overlay);
                }
            }
        });
    }

    @Override
    public void render(MarkerBlockEntity be, float partialTick, PoseStack ps, MultiBufferSource buffers, int light, int overlay) {
        MarkerType type = FurnitureGeo.typeOf(be);
        if (type == null || !FurnitureGeo.hasModel(type)) {
            fallback.render(be, partialTick, ps, buffers, light, overlay);
            return;
        }
        super.render(be, partialTick, ps, buffers, light, overlay);
        // 湯気・煙など（今までと同じ描き方）
        Direction facing = be.getBlockState().getValue(MarkerBlock.FACING);
        float sz = FurnitureArt.size(type);
        ps.pushPose();
        ps.translate(0.5, 0, 0.5);
        ps.mulPose(Axis.YP.rotationDegrees(MarkerRenderer.yaw(facing)));
        ps.scale(sz, sz, sz);
        ps.translate(-0.5, 0, -0.5);
        FurnitureHiRes.extras(type, MarkerRenderer.anim(be, partialTick), ps, buffers, light);
        ps.popPose();
    }

    @Override
    protected Direction getFacing(MarkerBlockEntity be) {
        return be.getBlockState().getValue(MarkerBlock.FACING);
    }

    @Override
    protected void rotateBlock(Direction facing, PoseStack ps) {
        ps.mulPose(Axis.YP.rotationDegrees(MarkerRenderer.yaw(facing)));
    }

    @Override
    public RenderType getRenderType(MarkerBlockEntity be, ResourceLocation texture, MultiBufferSource buf, float partial) {
        // 半透明（水・ガラス・ひれ）も描けるように
        return RenderType.entityTranslucent(texture);
    }

    @Override
    public boolean shouldRenderOffScreen(MarkerBlockEntity be) {
        return true;
    }
}
