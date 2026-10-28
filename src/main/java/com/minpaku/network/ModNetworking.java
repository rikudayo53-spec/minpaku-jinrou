package com.minpaku.network;

import com.minpaku.game.GameManager;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;

public final class ModNetworking {
    private ModNetworking() {
    }

    public static void registerCommon() {
        PayloadTypeRegistry.playS2C().register(Packets.State.TYPE, Packets.State.CODEC);
        PayloadTypeRegistry.playS2C().register(Packets.Reveal.TYPE, Packets.Reveal.CODEC);
        PayloadTypeRegistry.playS2C().register(Packets.Settings.TYPE, Packets.Settings.CODEC);
        PayloadTypeRegistry.playS2C().register(Packets.Meeting.TYPE, Packets.Meeting.CODEC);
        PayloadTypeRegistry.playS2C().register(Packets.Voted.TYPE, Packets.Voted.CODEC);
        PayloadTypeRegistry.playS2C().register(Packets.MeetingResult.TYPE, Packets.MeetingResult.CODEC);
        PayloadTypeRegistry.playS2C().register(Packets.KillFx.TYPE, Packets.KillFx.CODEC);
        PayloadTypeRegistry.playS2C().register(Packets.OpenTask.TYPE, Packets.OpenTask.CODEC);
        PayloadTypeRegistry.playS2C().register(Packets.Announce.TYPE, Packets.Announce.CODEC);
        PayloadTypeRegistry.playS2C().register(Packets.Result.TYPE, Packets.Result.CODEC);
        PayloadTypeRegistry.playS2C().register(Packets.Talk.TYPE, Packets.Talk.CODEC);

        PayloadTypeRegistry.playC2S().register(Packets.SetSetting.TYPE, Packets.SetSetting.CODEC);
        PayloadTypeRegistry.playC2S().register(Packets.Control.TYPE, Packets.Control.CODEC);
        PayloadTypeRegistry.playC2S().register(Packets.CastVote.TYPE, Packets.CastVote.CODEC);
        PayloadTypeRegistry.playC2S().register(Packets.TaskDone.TYPE, Packets.TaskDone.CODEC);
        PayloadTypeRegistry.playC2S().register(Packets.Sabotage.TYPE, Packets.Sabotage.CODEC);
        PayloadTypeRegistry.playC2S().register(Packets.VentMove.TYPE, Packets.VentMove.CODEC);
        PayloadTypeRegistry.playC2S().register(Packets.GhostUse.TYPE, Packets.GhostUse.CODEC);

        ServerPlayNetworking.registerGlobalReceiver(Packets.SetSetting.TYPE, (p, ctx) -> {
            ServerPlayer player = ctx.player();
            player.server.execute(() -> GameManager.get(player.server).setSetting(player, p.id(), p.value()));
        });
        ServerPlayNetworking.registerGlobalReceiver(Packets.Control.TYPE, (p, ctx) -> {
            ServerPlayer player = ctx.player();
            player.server.execute(() -> GameManager.get(player.server).control(player, p.action()));
        });
        ServerPlayNetworking.registerGlobalReceiver(Packets.CastVote.TYPE, (p, ctx) -> {
            ServerPlayer player = ctx.player();
            player.server.execute(() -> GameManager.get(player.server).vote(player, p.target()));
        });
        ServerPlayNetworking.registerGlobalReceiver(Packets.TaskDone.TYPE, (p, ctx) -> {
            ServerPlayer player = ctx.player();
            player.server.execute(() -> GameManager.get(player.server).finishMiniGame(player, p.pos()));
        });
        ServerPlayNetworking.registerGlobalReceiver(Packets.Sabotage.TYPE, (p, ctx) -> {
            ServerPlayer player = ctx.player();
            player.server.execute(() -> GameManager.get(player.server).sabotage(player, p.kind()));
        });
        ServerPlayNetworking.registerGlobalReceiver(Packets.VentMove.TYPE, (p, ctx) -> {
            ServerPlayer player = ctx.player();
            player.server.execute(() -> GameManager.get(player.server).ventMove(player, p.dir()));
        });
        ServerPlayNetworking.registerGlobalReceiver(Packets.GhostUse.TYPE, (p, ctx) -> {
            ServerPlayer player = ctx.player();
            player.server.execute(() -> GameManager.get(player.server).ghostUse(player, p.pos()));
        });
    }
}
