package com.minpaku.game;

import com.minpaku.network.Packets;
import com.minpaku.registry.ModItems;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * /minpaku start | stop | build | key | status
 * /minpaku preview <種類> [オプション] [人]  … アニメーションを自分の画面だけで再生する（確認・撮影用）
 *   finisher <wolf|shinobi|kairiki|juro|pingpong|pillow|fusuma|seoi|neko> [normal|guard|counter] [犯人]
 *   eject [wolf|villager|hidden|teru] [追放される人]
 *   skip / reveal <役職> / meeting <report|bell> / result <village|wolf|fox|teru>
 *   kill / guard <knight|onigiri|fox> / sabotage <blackout|gas> / task（近くの家具）/ all（全部を順番に）
 * /minpaku finisher [犯人] … 前からあるもの（preview finisher wolf と同じ）
 */
public final class GameCommands {
    private GameCommands() {
    }

    private static final String[] FINISHERS = {"wolf", "shinobi", "kairiki", "juro", "pingpong", "pillow", "fusuma", "seoi", "neko", "sheriff"};

    private static SuggestionProvider<CommandSourceStack> words(String... w) {
        return (c, sb) -> {
            for (String s : w) {
                sb.suggest(s);
            }
            return sb.buildFuture();
        };
    }

    private static int send(ServerPlayer p, String kind, String opt, ServerPlayer other) {
        String name = other == null ? "" : other.getGameProfile().getName();
        UUID id = other == null ? new UUID(0, 0) : other.getUUID();
        List<UUID> carriers = new ArrayList<>();
        for (ServerPlayer o : p.server.getPlayerList().getPlayers()) {
            if (o != p && carriers.size() < 4) {
                carriers.add(o.getUUID());
            }
        }
        ServerPlayNetworking.send(p, new Packets.PreviewAnim(kind, opt, name, id, carriers));
        return 1;
    }

    /** preview <kind> <opt...>：opt を1つ選ばせる種類 */
    private static LiteralArgumentBuilder<CommandSourceStack> simple(String kind, String def, String... opts) {
        return Commands.literal(kind)
                .executes(ctx -> send(ctx.getSource().getPlayerOrException(), kind, def, null))
                .then(Commands.argument("opt", StringArgumentType.word()).suggests(words(opts))
                        .executes(ctx -> send(ctx.getSource().getPlayerOrException(), kind, StringArgumentType.getString(ctx, "opt"), null)));
    }

    public static void register() {
        String[] roleIds = Role.all().stream().map(r -> r.id).toArray(String[]::new);
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
                                .executes(ctx -> send(ctx.getSource().getPlayerOrException(), "finisher", "wolf", null))
                                .then(Commands.argument("killer", EntityArgument.player()).executes(ctx ->
                                        send(ctx.getSource().getPlayerOrException(), "finisher", "wolf", EntityArgument.getPlayer(ctx, "killer")))))
                        .then(Commands.literal("preview")
                                .then(Commands.literal("all").executes(ctx -> {
                                    ctx.getSource().sendSuccess(() -> Component.literal("アニメーションを全部、順番に流します（Esc で各画面を閉じられます）")
                                            .withStyle(ChatFormatting.AQUA), false);
                                    return send(ctx.getSource().getPlayerOrException(), "all", "", null);
                                }))
                                .then(Commands.literal("finisher")
                                        .executes(ctx -> send(ctx.getSource().getPlayerOrException(), "finisher", "wolf", null))
                                        .then(Commands.argument("id", StringArgumentType.word()).suggests(words(FINISHERS))
                                                .executes(ctx -> send(ctx.getSource().getPlayerOrException(), "finisher",
                                                        StringArgumentType.getString(ctx, "id"), null))
                                                .then(Commands.argument("variant", StringArgumentType.word())
                                                        .suggests(words("normal", "guard", "counter"))
                                                        .executes(ctx -> send(ctx.getSource().getPlayerOrException(), "finisher",
                                                                StringArgumentType.getString(ctx, "id") + ":" + StringArgumentType.getString(ctx, "variant"), null))
                                                        .then(Commands.argument("killer", EntityArgument.player())
                                                                .executes(ctx -> send(ctx.getSource().getPlayerOrException(), "finisher",
                                                                        StringArgumentType.getString(ctx, "id") + ":"
                                                                                + StringArgumentType.getString(ctx, "variant"),
                                                                        EntityArgument.getPlayer(ctx, "killer")))))))
                                .then(simple("eject", "wolf", "wolf", "villager", "hidden", "teru"))
                                .then(Commands.literal("skip").executes(ctx -> send(ctx.getSource().getPlayerOrException(), "skip", "", null)))
                                .then(simple("reveal", "wolf", roleIds))
                                .then(simple("meeting", "report", "report", "bell"))
                                .then(simple("result", "village", "village", "wolf", "fox", "teru"))
                                .then(Commands.literal("kill").executes(ctx -> send(ctx.getSource().getPlayerOrException(), "kill", "", null)))
                                .then(simple("guard", "knight", "knight", "onigiri", "fox"))
                                .then(simple("sabotage", "blackout", "blackout", "gas"))
                                .then(Commands.literal("task").executes(ctx -> {
                                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                                    int n = GameManager.get(p.server).previewTasks(p);
                                    ctx.getSource().sendSuccess(() -> Component.literal(n == 0 ? "近くに家具がありません"
                                            : "近くの家具 " + n + " 個を動かします").withStyle(ChatFormatting.AQUA), false);
                                    return 1;
                                })))
                        .then(Commands.literal("status").executes(ctx -> {
                            ctx.getSource().sendSuccess(() -> Component.literal(
                                    GameManager.get(ctx.getSource().getServer()).status()).withStyle(ChatFormatting.AQUA), false);
                            return 1;
                        }))));
    }
}
