package com.minpaku.game;

import com.minpaku.registry.ModItems;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/**
 * /minpaku start | stop | key | status
 */
public final class GameCommands {
    private GameCommands() {
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
                        .then(Commands.literal("key").executes(ctx -> {
                            ServerPlayer p = ctx.getSource().getPlayerOrException();
                            p.getInventory().add(new ItemStack(ModItems.KEY));
                            return 1;
                        }))
                        .then(Commands.literal("status").executes(ctx -> {
                            ctx.getSource().sendSuccess(() -> Component.literal(
                                    GameManager.get(ctx.getSource().getServer()).status()).withStyle(ChatFormatting.AQUA), false);
                            return 1;
                        }))));
    }
}
