package com.minpaku.mixin.client;

import com.minpaku.client.ClientGame;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.FogRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** マップのまわりに濃い霧をかける（竹林の向こうが見えないように） */
@Mixin(FogRenderer.class)
public abstract class FogMixin {
    @Shadow
    private static float fogRed;
    @Shadow
    private static float fogGreen;
    @Shadow
    private static float fogBlue;

    /** 霧の濃さ（0 = 霧なし）。カメラが敷地の近くにいるときだけ */
    private static float minpaku$end(Camera camera) {
        int[] b = ClientGame.bounds;
        // 霧はプレイ中だけ。見えるブロック数は設定で変えられる（0 で霧なし）
        if (b == null || b.length != 6 || !ClientGame.inGame() || ClientGame.fogDistance <= 0) {
            return -1;
        }
        double x = camera.getPosition().x;
        double z = camera.getPosition().z;
        // 敷地の外側の縁までの距離（内側にいれば正）
        double d = Math.min(Math.min(x - b[0], b[3] + 1 - x), Math.min(z - b[2], b[5] + 1 - z));
        if (d < -48) {
            return -1;
        }
        return ClientGame.fogDistance;
    }

    @Inject(method = "setupFog", at = @At("TAIL"))
    private static void minpaku$setupFog(Camera camera, FogRenderer.FogMode mode, float far, boolean thick, float partial,
                                         CallbackInfo ci) {
        float end = minpaku$end(camera);
        if (end <= 0) {
            return;
        }
        if (mode == FogRenderer.FogMode.FOG_SKY) {
            RenderSystem.setShaderFogStart(0f);
            RenderSystem.setShaderFogEnd(Math.min(far, end * 0.6f));
        } else {
            // 濃さ：霧が始まる近さ（とてもうすい＝遠くから、とてもこい＝すぐ目の前から）
            float[] startK = {0.85f, 0.6f, 0.35f, 0.15f, 0.0f};
            float k = startK[Math.max(0, Math.min(4, ClientGame.fogDensity - 1))];
            RenderSystem.setShaderFogStart(Math.min(RenderSystem.getShaderFogStart(), end * k));
            RenderSystem.setShaderFogEnd(Math.min(RenderSystem.getShaderFogEnd(), end));
        }
    }

    @Inject(method = "setupColor", at = @At("TAIL"))
    private static void minpaku$setupColor(Camera camera, float partial, net.minecraft.client.multiplayer.ClientLevel level,
                                           int dist, float boss, CallbackInfo ci) {
        if (minpaku$end(camera) <= 0) {
            return;
        }
        // 白っぽい、少し緑がかった霧の色に寄せる（夜は暗く）
        float day = Math.max(0.15f, Math.min(1f, (fogRed + fogGreen + fogBlue) / 1.6f));
        float k = 0.7f;
        fogRed = fogRed * (1 - k) + 0.80f * day * k;
        fogGreen = fogGreen * (1 - k) + 0.85f * day * k;
        fogBlue = fogBlue * (1 - k) + 0.82f * day * k;
        RenderSystem.clearColor(fogRed, fogGreen, fogBlue, 0f);
    }
}
