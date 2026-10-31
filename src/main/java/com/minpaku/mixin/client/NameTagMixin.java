package com.minpaku.mixin.client;

import com.minpaku.client.ClientGame;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** 人狼のプレイ中は、だれの頭の上にも名前を出さない（プレイヤーもボットも） */
@Mixin(LivingEntityRenderer.class)
public abstract class NameTagMixin {
    @Inject(method = "shouldShowName(Lnet/minecraft/world/entity/LivingEntity;)Z", at = @At("HEAD"), cancellable = true, require = 0)
    private void minpaku$hideName(LivingEntity entity, CallbackInfoReturnable<Boolean> cir) {
        if (ClientGame.inGame()) {
            cir.setReturnValue(false);
        }
    }
}
