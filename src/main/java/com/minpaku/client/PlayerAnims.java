package com.minpaku.client;

import com.minpaku.MinpakuJinrou;
import dev.kosmx.playerAnim.api.IPlayable;
import dev.kosmx.playerAnim.api.layered.IAnimation;
import dev.kosmx.playerAnim.api.layered.KeyframeAnimationPlayer;
import dev.kosmx.playerAnim.api.layered.ModifierLayer;
import dev.kosmx.playerAnim.api.layered.modifier.AbstractFadeModifier;
import dev.kosmx.playerAnim.core.data.KeyframeAnimation;
import dev.kosmx.playerAnim.core.util.Ease;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationAccess;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

import java.util.UUID;

/**
 * プレイヤーの体の動き（playerAnimator）。タスクの作業・キル・呼び鈴・シェリフの射撃などで、
 * その人の体がみんなの画面で動く。動きは assets/minpaku/player_animations/*.json（tools/blockbench/player_anims.py）
 */
public final class PlayerAnims {
    private PlayerAnims() {
    }

    private static final ResourceLocation LAYER = MinpakuJinrou.id("action");

    public static void register() {
        PlayerAnimationAccess.REGISTER_ANIMATION_EVENT.register((player, stack) -> {
            ModifierLayer<IAnimation> layer = new ModifierLayer<>();
            stack.addAnimLayer(1000, layer);
            PlayerAnimationAccess.getPlayerAssociatedData(player).set(LAYER, layer);
        });
    }

    /** data = "uuid|名前"（名前が stop なら止める） */
    @SuppressWarnings("unchecked")
    public static void play(String data) {
        int bar = data.indexOf('|');
        if (bar < 0) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return;
        }
        UUID id;
        try {
            id = UUID.fromString(data.substring(0, bar));
        } catch (IllegalArgumentException e) {
            return;
        }
        String name = data.substring(bar + 1);
        Player p = mc.level.getPlayerByUUID(id);
        if (!(p instanceof AbstractClientPlayer acp)) {
            return;
        }
        IAnimation a = PlayerAnimationAccess.getPlayerAssociatedData(acp).get(LAYER);
        if (!(a instanceof ModifierLayer<?> l)) {
            return;
        }
        ModifierLayer<IAnimation> layer = (ModifierLayer<IAnimation>) l;
        if (name.equals("stop")) {
            layer.replaceAnimationWithFade(AbstractFadeModifier.standardFadeIn(6, Ease.INOUTSINE), null);
            return;
        }
        IPlayable anim = PlayerAnimationRegistry.getAnimation(MinpakuJinrou.id(name));
        if (anim instanceof KeyframeAnimation ka) {
            layer.replaceAnimationWithFade(AbstractFadeModifier.standardFadeIn(4, Ease.INOUTSINE), new KeyframeAnimationPlayer(ka));
        }
    }
}
