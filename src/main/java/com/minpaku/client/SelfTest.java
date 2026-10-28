package com.minpaku.client;

import com.minpaku.MinpakuJinrou;
import com.minpaku.client.screen.HolesScreen;
import com.minpaku.client.screen.ResultScreen;
import com.minpaku.client.screen.SabotageScreen;
import com.minpaku.client.screen.SettingsScreen;
import com.minpaku.client.screen.WiresScreen;
import com.minpaku.game.GameManager;
import com.minpaku.game.MapData;
import com.minpaku.game.MarkerType;
import com.minpaku.network.Packets;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;

import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * 自動テスト（環境変数 MINPAKU_SELFTEST=1 のときだけ動く）。
 * テスト用の世界を開き、古民家マップを建ててゲームを進め、要所でスクリーンショットを撮る。
 */
public final class SelfTest {
    public static final boolean ENABLED = "1".equals(System.getenv("MINPAKU_SELFTEST"));
    private static int t = -1;
    private static int menuTicks;
    private static boolean opened;
    private static BlockPos spawn = BlockPos.ZERO;
    private static int meetAt = -1;

    private SelfTest() {
    }

    private static void shot(Minecraft mc, String name) {
        Screenshot.grab(mc.gameDirectory, "selftest_" + name + ".png", mc.getMainRenderTarget(),
                c -> MinpakuJinrou.LOGGER.info("SELFTEST screenshot {}", name));
    }

    private static void server(Minecraft mc, Consumer<ServerPlayer> task) {
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
        if (!ENABLED) {
            return;
        }
        if (mc.level == null) {
            if (!opened && mc.screen != null && ++menuTicks > 60) {
                opened = true;
                mc.createWorldOpenFlows().openWorld("selftest", () -> MinpakuJinrou.LOGGER.error("SELFTEST open failed"));
            }
            return;
        }
        if (mc.player == null) {
            return;
        }
        t++;
        try {
            step(mc);
        } catch (Exception e) {
            MinpakuJinrou.LOGGER.error("SELFTEST failed at {}", t, e);
        }
    }

    private static void tp(Minecraft mc, double dx, double dy, double dz, float yaw, float pitch) {
        server(mc, sp -> sp.teleportTo(sp.serverLevel(), spawn.getX() + 0.5 + dx, spawn.getY() + dy, spawn.getZ() + 0.5 + dz, yaw, pitch));
    }

    private static void step(Minecraft mc) {
        if (meetAt > 0) {
            int m = t - meetAt;
            switch (m) {
                case 30 -> shot(mc, "20_meeting_splash");
                case 500 -> shot(mc, "21_meeting_talk");
                case 1000 -> shot(mc, "22_meeting_vote");
                case 1010 -> ClientPlayNetworking.send(new Packets.CastVote(Packets.NONE));
                default -> {
                }
            }
            if (m == 1300) {
                shot(mc, "23_after");
                meetAt = -1;
                t = 5000;
            }
            return;
        }
        switch (t) {
            case 60 -> server(mc, sp -> {
                GameManager.selfTestNoWin = true;
                sp.setGameMode(GameType.CREATIVE);
                sp.setYRot(180f);
                GameManager.get(sp.server).buildMap(sp);
                spawn = sp.blockPosition();
            });
            case 160 -> shot(mc, "01_entrance");
            case 170 -> tp(mc, 0, 38, 30, 180f, 42f);
            case 230 -> shot(mc, "02_aerial");
            case 240 -> tp(mc, 0, 2, -8, 180f, 20f);
            case 290 -> shot(mc, "03_corridor");
            case 300 -> server(mc, sp -> {
                List<BlockPos> bells = MapData.get(sp.serverLevel()).get(MarkerType.BELL);
                if (!bells.isEmpty()) {
                    BlockPos b = bells.get(0);
                    sp.teleportTo(sp.serverLevel(), b.getX() + 0.5, b.getY() + 1.5, b.getZ() + 4.5, 180f, 25f);
                }
            });
            case 350 -> shot(mc, "04_hall");
            case 360 -> server(mc, sp -> {
                sp.teleportTo(sp.serverLevel(), spawn.getX() + 0.5, spawn.getY(), spawn.getZ() + 0.5, 180f, 0f);
                sp.setGameMode(GameType.ADVENTURE);
                GameManager.get(sp.server).sendSettings(sp);
            });
            case 362 -> server(mc, sp -> {
                sp.getInventory().selected = 0;
                sp.getInventory().setItem(0, new net.minecraft.world.item.ItemStack(com.minpaku.registry.ModItems.CLAW));
            });
            case 366 -> mc.options.setCameraType(net.minecraft.client.CameraType.THIRD_PERSON_FRONT);
            case 380 -> shot(mc, "05a_claw_3rd");
            case 384 -> mc.options.setCameraType(net.minecraft.client.CameraType.FIRST_PERSON);
            case 392 -> shot(mc, "05b_claw_hand");
            case 395 -> mc.setScreen(new SettingsScreen());
            case 405 -> shot(mc, "05_settings");
            case 412 -> {
                mc.setScreen(null);
                server(mc, sp -> GameManager.get(sp.server).start(sp));
            }
            case 435 -> shot(mc, "06_reveal_title");
            case 520 -> shot(mc, "07_reveal_role");
            case 640 -> shot(mc, "08_hud");
            case 650 -> {
                if (GameHud.LEDGER_KEY != null) {
                    GameHud.LEDGER_KEY.setDown(true);
                }
            }
            case 660 -> shot(mc, "09_hud_ledger");
            case 665 -> {
                if (GameHud.LEDGER_KEY != null) {
                    GameHud.LEDGER_KEY.setDown(false);
                }
                mc.setScreen(new WiresScreen(BlockPos.ZERO));
            }
            case 680 -> shot(mc, "10_wires");
            case 685 -> mc.setScreen(new HolesScreen(BlockPos.ZERO));
            case 700 -> shot(mc, "11_shoji");
            case 705 -> mc.setScreen(new SabotageScreen());
            case 720 -> shot(mc, "12_sabotage");
            case 725 -> {
                mc.setScreen(null);
                ClientGame.announce("停電！", "配電盤を直そう", 0xFFD54F);
            }
            case 745 -> shot(mc, "13_announce");
            case 800 -> {
                ClientGame.killFxKind = 0;
                ClientGame.killFxOther = "はなこ";
                ClientGame.killFxOtherId = mc.player.getUUID();
                ClientGame.killFxLen = ClientGame.VICTIM_FX_LEN;
                ClientGame.killFxTicks = ClientGame.killFxLen;
            }
            case 812 -> shot(mc, "15a_victim_slash");
            case 865 -> shot(mc, "15b_victim_face");
            case 1100 -> shot(mc, "14_bots_moving");
            case 1110 -> server(mc, sp -> {
                List<BlockPos> bells = MapData.get(sp.serverLevel()).get(MarkerType.BELL);
                if (!bells.isEmpty()) {
                    GameManager.get(sp.server).onUseMarker(sp, bells.get(0), MarkerType.BELL);
                }
                meetAt = t + 1;
            });
            case 5040 -> mc.setScreen(new ResultScreen(new Packets.Result(1, "人狼が民泊を乗っ取った", List.of(
                    new Packets.Participant(mc.player.getUUID(), mc.player.getGameProfile().getName(), true, "wolf"),
                    new Packets.Participant(UUID.randomUUID(), "はなこ", false, "wolf"),
                    new Packets.Participant(UUID.randomUUID(), "たろう", true, "villager"),
                    new Packets.Participant(UUID.randomUUID(), "みお", true, "seer"),
                    new Packets.Participant(UUID.randomUUID(), "けんた", false, "knight"),
                    new Packets.Participant(UUID.randomUUID(), "さくら", false, "medium"),
                    new Packets.Participant(UUID.randomUUID(), "ゆうき", true, "villager"),
                    new Packets.Participant(UUID.randomUUID(), "そうた", true, "madman")),
                    List.of(2, 1, 0, 0, 0, 0, 0, 0), 74, 3)));
            case 5100 -> shot(mc, "30_result");
            case 5120 -> mc.stop();
            default -> {
            }
        }
    }
}
