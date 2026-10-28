package com.minpaku.game;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;

public final class GameEvents {
    private GameEvents() {
    }

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            GameManager gm = GameManager.current();
            if (gm != null) {
                gm.tick();
            }
        });

        // ゲーム中の攻撃は全部止めて、人狼のキルだけ処理する
        AttackEntityCallback.EVENT.register((player, level, hand, entity, hit) -> {
            if (level.isClientSide() || !(player instanceof ServerPlayer sp)) {
                return InteractionResult.PASS;
            }
            GameManager gm = GameManager.current();
            if (gm == null || !gm.isParticipant(sp)) {
                return InteractionResult.PASS;
            }
            if (entity instanceof ServerPlayer target && gm.isParticipant(target)) {
                gm.tryKill(sp, target);
            }
            return InteractionResult.FAIL;
        });

        // ゲーム中はダメージを受けない（落下・窒息なども）
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            if (entity instanceof ServerPlayer sp) {
                GameManager gm = GameManager.current();
                return gm == null || !gm.isParticipant(sp);
            }
            return true;
        });

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            GameManager gm = GameManager.current();
            if (gm != null) {
                gm.onLeave(handler.getPlayer());
            }
        });
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
                GameManager.get(server).onJoin(handler.getPlayer()));

        ServerLifecycleEvents.SERVER_STOPPING.register(server -> GameManager.shutdown());
    }
}
