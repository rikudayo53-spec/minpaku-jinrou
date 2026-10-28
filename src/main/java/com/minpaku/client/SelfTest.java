package com.minpaku.client;

import com.minpaku.MinpakuJinrou;
import com.minpaku.block.MarkerBlock;
import com.minpaku.client.screen.BreakerScreen;
import com.minpaku.client.screen.NumbersScreen;
import com.minpaku.client.screen.ResultScreen;
import com.minpaku.client.screen.SettingsScreen;
import com.minpaku.client.screen.WiresScreen;
import com.minpaku.entity.BodyEntity;
import com.minpaku.game.GameManager;
import com.minpaku.game.MarkerType;
import com.minpaku.network.Packets;
import com.minpaku.registry.ModBlocks;
import com.minpaku.registry.ModEntities;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;

import java.util.List;
import java.util.UUID;

/**
 * 自動テスト（環境変数 MINPAKU_SELFTEST=1 のときだけ動く）。
 * シングルプレイの世界に入ったら、ブロックを並べてゲームを進め、要所でスクリーンショットを撮る。
 */
public final class SelfTest {
    public static final boolean ENABLED = "1".equals(System.getenv("MINPAKU_SELFTEST"));
    private static int t = -1;
    private static BlockPos origin;

    private SelfTest() {
    }

    private static void shot(Minecraft mc, String name) {
        Screenshot.grab(mc.gameDirectory, "selftest_" + name + ".png", mc.getMainRenderTarget(),
                c -> MinpakuJinrou.LOGGER.info("SELFTEST screenshot {}", name));
    }

    private static void server(Minecraft mc, java.util.function.Consumer<ServerPlayer> task) {
        if (mc.getSingleplayerServer() == null || mc.player == null) {
            return;
        }
        UUID id = mc.player.getUUID();
        mc.getSingleplayerServer().execute(() -> {
            ServerPlayer sp = mc.getSingleplayerServer().getPlayerList().getPlayer(id);
            if (sp != null) {
                task.accept(sp);
            }
        });
    }

    public static void tick(Minecraft mc) {
        if (!ENABLED || mc.player == null || mc.level == null) {
            return;
        }
        t++;
        try {
            step(mc);
        } catch (Exception e) {
            MinpakuJinrou.LOGGER.error("SELFTEST failed at {}", t, e);
        }
    }

    private static void step(Minecraft mc) {
        switch (t) {
            case 60 -> server(mc, sp -> {
                GameManager.selfTestNoWin = true;
                ServerLevel level = sp.serverLevel();
                level.setDayTime(6000);
                sp.setGameMode(GameType.CREATIVE);
                BlockPos base = sp.blockPosition();
                origin = base;
                // 床と壁
                for (int x = -8; x <= 8; x++) {
                    for (int z = -2; z <= 8; z++) {
                        level.setBlockAndUpdate(base.offset(x, -1, z), Blocks.OAK_PLANKS.defaultBlockState());
                        for (int y = 0; y < 4; y++) {
                            level.setBlockAndUpdate(base.offset(x, y, z), Blocks.AIR.defaultBlockState());
                        }
                    }
                    level.setBlockAndUpdate(base.offset(x, 0, 6), Blocks.WHITE_TERRACOTTA.defaultBlockState());
                    level.setBlockAndUpdate(base.offset(x, 1, 6), Blocks.WHITE_TERRACOTTA.defaultBlockState());
                    level.setBlockAndUpdate(base.offset(x, 2, 6), Blocks.WHITE_TERRACOTTA.defaultBlockState());
                }
                // ブロックを2列に並べる（プレイヤーの方を向ける = 北向きに置いて南側から見る）
                int i = 0;
                for (MarkerType type : MarkerType.values()) {
                    int col = i % 7;
                    int row = i / 7;
                    BlockPos pos = base.offset(-6 + col * 2, 0, 3 + row * 2);
                    level.setBlockAndUpdate(pos, ModBlocks.BLOCKS.get(type).defaultBlockState()
                            .setValue(MarkerBlock.FACING, Direction.NORTH));
                    i++;
                }
                sp.teleportTo(level, base.getX() + 0.5, base.getY(), base.getZ() - 1.5, 0f, 25f);
            });
            case 140 -> shot(mc, "01_blocks");
            case 150 -> server(mc, sp -> sp.teleportTo(sp.serverLevel(), origin.getX() - 5.5, origin.getY() + 1.2,
                    origin.getZ() + 1.0, 0f, 35f));
            case 190 -> shot(mc, "02_blocks_close_left");
            case 200 -> server(mc, sp -> sp.teleportTo(sp.serverLevel(), origin.getX() + 4.5, origin.getY() + 1.2,
                    origin.getZ() + 1.0, 0f, 35f));
            case 240 -> shot(mc, "03_blocks_close_right");
            case 250 -> server(mc, sp -> {
                BodyEntity body = ModEntities.BODY.create(sp.serverLevel());
                body.moveTo(origin.getX() + 0.5, origin.getY(), origin.getZ() + 1.5, 30f, 0);
                body.setOwner(sp.getUUID(), sp.getGameProfile().getName());
                sp.serverLevel().addFreshEntity(body);
                sp.teleportTo(sp.serverLevel(), origin.getX() + 0.5, origin.getY() + 0.5, origin.getZ() - 1.5, 0f, 40f);
            });
            case 300 -> shot(mc, "04_body");
            case 310 -> server(mc, sp -> GameManager.get(sp.server).sendSettings(sp));
            case 320 -> mc.setScreen(new SettingsScreen());
            case 350 -> shot(mc, "05_settings");
            case 360 -> {
                mc.setScreen(null);
                server(mc, sp -> {
                    sp.teleportTo(sp.serverLevel(), origin.getX() + 0.5, origin.getY(), origin.getZ() - 1.5, 0f, 20f);
                    GameManager.get(sp.server).start(sp);
                });
            }
            case 385 -> shot(mc, "06_reveal_title");
            case 440 -> shot(mc, "07_reveal_role");
            case 520 -> shot(mc, "08_hud");
            case 530 -> mc.setScreen(new WiresScreen(BlockPos.ZERO));
            case 545 -> shot(mc, "09_wires");
            case 550 -> mc.setScreen(new NumbersScreen(BlockPos.ZERO));
            case 565 -> shot(mc, "10_numbers");
            case 570 -> mc.setScreen(new BreakerScreen(BlockPos.ZERO));
            case 585 -> shot(mc, "11_breaker");
            case 590 -> {
                mc.setScreen(null);
                ClientGame.killFxKind = 0;
                ClientGame.killFxOther = "テスト";
                ClientGame.killFxTicks = ClientGame.KILL_FX_LEN;
            }
            case 610 -> shot(mc, "12_killed");
            case 660 -> server(mc, sp -> {
                List<BlockPos> bells = com.minpaku.game.MapData.get(sp.serverLevel()).get(MarkerType.BELL);
                if (!bells.isEmpty()) {
                    GameManager.get(sp.server).onUseMarker(sp, bells.get(0), MarkerType.BELL);
                }
            });
            case 680 -> shot(mc, "13_meeting_splash");
            case 1640 -> shot(mc, "14_meeting_table");
            case 1650 -> ClientPlayNetworking.send(new Packets.CastVote(Packets.NONE));
            case 1665 -> shot(mc, "15_meeting_result");
            case 1760 -> shot(mc, "16_eject");
            case 1900 -> mc.setScreen(new ResultScreen(new Packets.Result(0, "すべてのタスクを終えた", List.of(
                    new Packets.Participant(mc.player.getUUID(), mc.player.getGameProfile().getName(), true, "wolf"),
                    new Packets.Participant(UUID.randomUUID(), "村人A", true, "villager"),
                    new Packets.Participant(UUID.randomUUID(), "占い師B", false, "seer"),
                    new Packets.Participant(UUID.randomUUID(), "騎士C", true, "knight")))));
            case 1960 -> shot(mc, "17_result");
            case 1980 -> mc.stop();
            default -> {
            }
        }
    }
}
