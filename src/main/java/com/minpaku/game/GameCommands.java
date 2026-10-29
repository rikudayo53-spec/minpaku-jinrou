package com.minpaku.game;

import com.minpaku.registry.ModItems;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import com.minpaku.network.Packets;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.commands.arguments.EntityArgument;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import com.mojang.brigadier.arguments.StringArgumentType;

/**
 * /minpaku start | stop | build | key | status
 * /minpaku finisher [犯人]  … 襲われたときの演出を自分の画面で再生する（確認・撮影用）
 * /minpaku preview finisher [犯人] | eject [追放される人] [wolf|villager|hidden] | skip
 *   … アニメーションを自分の画面で再生する（確認・撮影用）
 */
public final class GameCommands {
    private GameCommands() {
    }

    private static int finisher(ServerPlayer p, ServerPlayer killer) {
        if (killer == null) {
            // 犯人を指定しないときは、標準スキンの「人狼」
            UUID fake = UUID.nameUUIDFromBytes(("minpaku-wolf-" + p.getRandom().nextInt(8)).getBytes());
            ServerPlayNetworking.send(p, new Packets.KillFx(0, "人狼", fake));
        } else {
            ServerPlayNetworking.send(p, new Packets.KillFx(0, killer.getGameProfile().getName(), killer.getUUID()));
        }
        return 1;
    }

    /** 追放の確認。担ぐのはほかのプレイヤー（足りなければ標準スキンの人） */
    private static int eject(ServerPlayer p, ServerPlayer target, String result) {
        ServerPlayer t = target == null ? p : target;
        String name = t.getGameProfile().getName();
        String text = switch (result) {
            case "villager" -> name + " は人狼ではなかった";
            case "hidden" -> name + " は追放された";
            default -> name + " は人狼だった";
        };
        List<UUID> carriers = new ArrayList<>();
        if (t != p) {
            carriers.add(p.getUUID());
        }
        for (ServerPlayer o : p.server.getPlayerList().getPlayers()) {
            if (o != t && o != p && carriers.size() < 4) {
                carriers.add(o.getUUID());
            }
        }
        int k = 0;
        while (carriers.size() < 4) {
            carriers.add(UUID.nameUUIDFromBytes(("minpaku-carrier-" + k++).getBytes()));
        }
        ServerPlayNetworking.send(p, new Packets.Preview(1, t.getUUID(), name, text, carriers));
        return 1;
    }

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registry, env) -> dispatcher.register(
                Commands.literal("minpaku")
                        .requires(src -> src.hasPermission(2))
                        .then(Commands.literal("start").executes(ctx -> {
                            ServerPlayer p = ctx.getSource().getPlayerOrException();
                            GameManager.get(p.server).start(p);
                            return 1;
                        }))
                        .then(Commands.literal("stop").executes(ctx -> {
                            ServerPlayer p = ctx.getSource().getPlayer();
                            GameManager.get(ctx.getSource().getServer()).stop(p);
                            return 1;
                        }))
                        .then(Commands.literal("build").executes(ctx -> {
                            ServerPlayer p = ctx.getSource().getPlayerOrException();
                            GameManager.get(p.server).buildMap(p);
                            return 1;
                        }))
                        .then(Commands.literal("key").executes(ctx -> {
                            ServerPlayer p = ctx.getSource().getPlayerOrException();
                            p.getInventory().add(new ItemStack(ModItems.KEY));
                            return 1;
                        }))
                        .then(Commands.literal("finisher")
                                .executes(ctx -> {
                                    // 犯人を指定しないときは、標準スキンの「人狼」
                                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                                    UUID fake = UUID.nameUUIDFromBytes(("minpaku-wolf-" + p.getRandom().nextInt(8)).getBytes());
                                    ServerPlayNetworking.send(p, new Packets.KillFx(0, "人狼", fake));
                                    return 1;
                                })
                                .then(Commands.argument("killer", EntityArgument.player()).executes(ctx -> {
                                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                                    ServerPlayer k = EntityArgument.getPlayer(ctx, "killer");
                                    ServerPlayNetworking.send(p, new Packets.KillFx(0, k.getGameProfile().getName(), k.getUUID()));
                                    return 1;
                                })))
                        .then(Commands.literal("preview")
                                .then(Commands.literal("finisher")
                                        .executes(ctx -> finisher(ctx.getSource().getPlayerOrException(), null))
                                        .then(Commands.argument("killer", EntityArgument.player()).executes(ctx ->
                                                finisher(ctx.getSource().getPlayerOrException(), EntityArgument.getPlayer(ctx, "killer")))))
                                .then(Commands.literal("eject")
                                        .executes(ctx -> eject(ctx.getSource().getPlayerOrException(), null, "wolf"))
                                        .then(Commands.argument("target", EntityArgument.player())
                                                .executes(ctx -> eject(ctx.getSource().getPlayerOrException(),
                                                        EntityArgument.getPlayer(ctx, "target"), "wolf"))
                                                .then(Commands.argument("result", StringArgumentType.word())
                                                        .suggests((c, sb) -> {
                                                            sb.suggest("wolf");
                                                            sb.suggest("villager");
                                                            sb.suggest("hidden");
                                                            return sb.buildFuture();
                                                        })
                                                        .executes(ctx -> eject(ctx.getSource().getPlayerOrException(),
                                                                EntityArgument.getPlayer(ctx, "target"),
                                                                StringArgumentType.getString(ctx, "result"))))))
                                .then(Commands.literal("skip").executes(ctx -> {
                                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                                    ServerPlayNetworking.send(p, new Packets.Preview(2, Packets.NONE, "", "誰も追放されなかった",
                                            List.of()));
                                    return 1;
                                })))
                        .then(Commands.literal("status").executes(ctx -> {
                            ctx.getSource().sendSuccess(() -> Component.literal(
                                    GameManager.get(ctx.getSource().getServer()).status()).withStyle(ChatFormatting.AQUA), false);
                            return 1;
                        }))));
    }
}
