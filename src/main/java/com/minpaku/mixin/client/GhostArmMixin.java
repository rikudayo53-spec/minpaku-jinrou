package com.minpaku.mixin.client;

import com.minpaku.client.ClientGame;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** 幽霊のときの自分の腕（一人称）を半透明に描く */
@Mixin(PlayerRenderer.class)
public abstract class GhostArmMixin {
    @Inject(method = "renderHand", at = @At("HEAD"), cancellable = true, require = 0)
    private void minpaku$ghostArm(PoseStack ps, MultiBufferSource buffers, int light, AbstractClientPlayer player, ModelPart arm,
                                  ModelPart sleeve, CallbackInfo ci) {
        if (!ClientGame.isGhost()) {
            return;
        }
        PlayerModel<AbstractClientPlayer> model = ((PlayerRenderer) (Object) this).getModel();
        model.attackTime = 0f;
        model.crouching = false;
        model.swimAmount = 0f;
        model.setupAnim(player, 0f, 0f, 0f, 0f, 0f);
        ResourceLocation tex = player.getSkin().texture();
        int color = 0x70C8E6FF;   // うすい青白、半透明
        arm.xRot = 0f;
        arm.render(ps, buffers.getBuffer(RenderType.entityTranslucent(tex)), light, OverlayTexture.NO_OVERLAY, color);
        sleeve.xRot = 0f;
        sleeve.render(ps, buffers.getBuffer(RenderType.entityTranslucent(tex)), light, OverlayTexture.NO_OVERLAY, color);
        ci.cancel();
    }
}
