package com.minpaku.mixin.client;

import com.minpaku.client.ClientGame;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** ゲーム中は Tab のプレイヤー一覧を出さない（かわりに GameHud がタスクの進み具合を出す） */
@Mixin(Gui.class)
public abstract class TabListMixin {
    @Inject(method = "renderTabList", at = @At("HEAD"), cancellable = true)
    private void minpaku$hideTab(GuiGraphics g, DeltaTracker delta, CallbackInfo ci) {
        if (ClientGame.inGame()) {
            ci.cancel();
        }
    }
}
