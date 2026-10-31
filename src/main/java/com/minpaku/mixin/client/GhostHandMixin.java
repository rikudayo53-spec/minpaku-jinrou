package com.minpaku.mixin.client;

import com.minpaku.client.ClientGame;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.level.GameType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** 幽霊（スペクテイター）でも、自分の手を描く（手は GhostArmMixin で半透明にする） */
@Mixin(GameRenderer.class)
public abstract class GhostHandMixin {
    @Redirect(method = "renderItemInHand", require = 0,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/MultiPlayerGameMode;getPlayerMode()Lnet/minecraft/world/level/GameType;"))
    private GameType minpaku$ghostHands(MultiPlayerGameMode mode) {
        GameType t = mode.getPlayerMode();
        if (t == GameType.SPECTATOR && ClientGame.isGhost()) {
            return GameType.ADVENTURE;
        }
        return t;
    }
}
