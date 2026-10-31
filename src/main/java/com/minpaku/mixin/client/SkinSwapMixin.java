package com.minpaku.mixin.client;

import com.minpaku.client.SkinSwap;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.resources.PlayerSkin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** 妨害「スキン入れ替え」中は、ほかの人のスキンで描く */
@Mixin(AbstractClientPlayer.class)
public abstract class SkinSwapMixin {
    @Inject(method = "getSkin", at = @At("HEAD"), cancellable = true, require = 0)
    private void minpaku$swapSkin(CallbackInfoReturnable<PlayerSkin> cir) {
        PlayerSkin s = SkinSwap.skinOf(((AbstractClientPlayer) (Object) this).getUUID());
        if (s != null) {
            cir.setReturnValue(s);
        }
    }
}
