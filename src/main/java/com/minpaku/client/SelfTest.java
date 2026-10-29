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

    /** マップを建てて、玄関に立つ */
    private static void buildPlan(Minecraft mc, int i) {
        server(mc, sp -> {
            sp.setGameMode(GameType.SPECTATOR);
            sp.teleportTo(sp.serverLevel(), spawn.getX() + 0.5, spawn.getY(), spawn.getZ() + 0.5, 180f, 0f);
            GameManager gm = GameManager.get(sp.server);
            gm.stop(null);
            if (sp.isDeadOrDying()) {
                sp.setHealth(sp.getMaxHealth());
            }
            gm.buildMap(sp, com.minpaku.game.Plans.ALL.get(i));
            sp.setGameMode(GameType.SPECTATOR);
            sp.teleportTo(sp.serverLevel(), sp.getX(), sp.getY() + 0.2, sp.getZ(), 180f, 10f);
        });
    }

    /** 上から見下ろす */
    private static void aerial(Minecraft mc, int i) {
        com.minpaku.game.MapPlan p = com.minpaku.game.Plans.ALL.get(i);
        server(mc, sp -> {
            sp.setGameMode(GameType.SPECTATOR);
            double h = Math.max(p.w, p.d) * 0.75 + 12;
            sp.teleportTo(sp.serverLevel(), sp.getX(), sp.getY() + h, sp.getZ() + 8, 180f, 62f);
        });
    }

    /** 家具の前に立って、タスクの動きを再生する */
    private static void furniture(Minecraft mc, MarkerType type) {
        if (mc.screen != null) {
            mc.setScreen(null);
        }
        server(mc, sp -> {
            GameManager.get(sp.server).stop(null);
            List<BlockPos> list = MapData.get(sp.serverLevel()).get(type);
            if (list.isEmpty()) {
                return;
            }
            BlockPos b = list.get(0);
            var st = sp.serverLevel().getBlockState(b);
            net.minecraft.core.Direction f = st.hasProperty(com.minpaku.block.MarkerBlock.FACING)
                    ? st.getValue(com.minpaku.block.MarkerBlock.FACING) : net.minecraft.core.Direction.NORTH;
            double ex = b.getX() + 0.5 + f.getStepX() * 2.4 + f.getClockWise().getStepX() * 0.8;
            double ez = b.getZ() + 0.5 + f.getStepZ() * 2.4 + f.getClockWise().getStepZ() * 0.8;
            double ey = b.getY() + 0.6;
            double dx = b.getX() + 0.5 - ex;
            double dz = b.getZ() + 0.5 - ez;
            float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
            sp.setGameMode(GameType.SPECTATOR);
            sp.teleportTo(sp.serverLevel(), ex, ey, ez, yaw, 28f);
            GameManager.get(sp.server).previewTasks(sp);
        });
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
            case 5130 -> mc.setScreen(new com.minpaku.client.screen.PreviewScreen(new com.minpaku.client.scene.EjectScene.Cast(
                    mc.player.getUUID(), "Player", "Player は人狼だった", List.of(
                    UUID.nameUUIDFromBytes("c0".getBytes()), UUID.nameUUIDFromBytes("c1".getBytes()),
                    UUID.nameUUIDFromBytes("c2".getBytes()), UUID.nameUUIDFromBytes("c3".getBytes())))));
            case 5150 -> shot(mc, "40a_eject_carry");
            case 5168 -> shot(mc, "40b_eject_carry2");
            case 5186 -> shot(mc, "40c_eject_seeno");
            case 5198 -> shot(mc, "40d_eject_seeno2");
            case 5210 -> shot(mc, "40e_eject_throw");
            case 5230 -> shot(mc, "40f_eject_fly");
            case 5254 -> shot(mc, "40g_eject_star");
            case 5285 -> shot(mc, "40h_eject_result");
            case 5300 -> mc.setScreen(new com.minpaku.client.screen.PreviewScreen(new com.minpaku.client.scene.EjectScene.Cast(
                    Packets.NONE, "", "誰も追放されなかった", List.of())));
            case 5340 -> shot(mc, "41a_skip");
            case 5361 -> shot(mc, "41b_skip_tip");
            case 5368 -> shot(mc, "41c_skip_kakon");
            case 5410 -> shot(mc, "41d_skip_result");
            case 5470 -> furniture(mc, MarkerType.KAMADO);
            case 5484 -> shot(mc, "50_00_kamado_a");
            case 5510 -> shot(mc, "50_00_kamado_b");
            case 5530 -> furniture(mc, MarkerType.CHOPPING);
            case 5544 -> shot(mc, "50_01_chopping_a");
            case 5570 -> shot(mc, "50_01_chopping_b");
            case 5590 -> furniture(mc, MarkerType.FUTON);
            case 5604 -> shot(mc, "50_02_futon_a");
            case 5630 -> shot(mc, "50_02_futon_b");
            case 5650 -> furniture(mc, MarkerType.LAUNDRY);
            case 5664 -> shot(mc, "50_03_laundry_a");
            case 5690 -> shot(mc, "50_03_laundry_b");
            case 5710 -> furniture(mc, MarkerType.VASE);
            case 5724 -> shot(mc, "50_04_vase_a");
            case 5750 -> shot(mc, "50_04_vase_b");
            case 5770 -> furniture(mc, MarkerType.SINK);
            case 5784 -> shot(mc, "50_05_sink_a");
            case 5810 -> shot(mc, "50_05_sink_b");
            case 5830 -> furniture(mc, MarkerType.FIELD);
            case 5844 -> shot(mc, "50_06_field_a");
            case 5870 -> shot(mc, "50_06_field_b");
            case 5890 -> furniture(mc, MarkerType.DUST);
            case 5904 -> shot(mc, "50_07_dust_a");
            case 5930 -> shot(mc, "50_07_dust_b");
            case 5950 -> furniture(mc, MarkerType.SHOE_RACK);
            case 5964 -> shot(mc, "50_08_shoe_rack_a");
            case 5990 -> shot(mc, "50_08_shoe_rack_b");
            case 6010 -> furniture(mc, MarkerType.BATH);
            case 6024 -> shot(mc, "50_09_bath_a");
            case 6050 -> shot(mc, "50_09_bath_b");
            case 6070 -> furniture(mc, MarkerType.BELL);
            case 6084 -> shot(mc, "50_10_bell_a");
            case 6110 -> shot(mc, "50_10_bell_b");
            case 6130 -> furniture(mc, MarkerType.BOILER);
            case 6144 -> shot(mc, "50_11_boiler_a");
            case 6170 -> shot(mc, "50_11_boiler_b");
            case 6190 -> buildPlan(mc, 0);
            case 6250 -> shot(mc, "60_0_inside");
            case 6256 -> aerial(mc, 0);
            case 6330 -> shot(mc, "61_0_aerial");
            case 6340 -> buildPlan(mc, 1);
            case 6400 -> shot(mc, "60_1_inside");
            case 6406 -> aerial(mc, 1);
            case 6480 -> shot(mc, "61_1_aerial");
            case 6490 -> buildPlan(mc, 2);
            case 6550 -> shot(mc, "60_2_inside");
            case 6556 -> aerial(mc, 2);
            case 6630 -> shot(mc, "61_2_aerial");
            case 6640 -> buildPlan(mc, 3);
            case 6700 -> shot(mc, "60_3_inside");
            case 6706 -> aerial(mc, 3);
            case 6780 -> shot(mc, "61_3_aerial");
            case 6790 -> buildPlan(mc, 4);
            case 6850 -> shot(mc, "60_4_inside");
            case 6856 -> aerial(mc, 4);
            case 6930 -> shot(mc, "61_4_aerial");
            case 6940 -> buildPlan(mc, 5);
            case 7000 -> shot(mc, "60_5_inside");
            case 7006 -> aerial(mc, 5);
            case 7080 -> shot(mc, "61_5_aerial");
            case 7090 -> { mc.setScreen(null); ClientGame.startVictimFx("はなこ", UUID.fromString("00000000-0000-0000-0000-000000000005"), "wolf", 0); }
            case 7107 -> shot(mc, "70_00_wolf_0");
            case 7130 -> shot(mc, "70_00_wolf_1");
            case 7152 -> shot(mc, "70_00_wolf_2");
            case 7178 -> { mc.setScreen(null); ClientGame.startVictimFx("はなこ", UUID.fromString("00000000-0000-0000-0000-000000000005"), "shinobi", 0); }
            case 7195 -> shot(mc, "70_01_shinobi_0");
            case 7217 -> shot(mc, "70_01_shinobi_1");
            case 7238 -> shot(mc, "70_01_shinobi_2");
            case 7264 -> { mc.setScreen(null); ClientGame.startVictimFx("はなこ", UUID.fromString("00000000-0000-0000-0000-000000000005"), "kairiki", 0); }
            case 7281 -> shot(mc, "70_02_kairiki_0");
            case 7304 -> shot(mc, "70_02_kairiki_1");
            case 7326 -> shot(mc, "70_02_kairiki_2");
            case 7352 -> { mc.setScreen(null); ClientGame.startVictimFx("はなこ", UUID.fromString("00000000-0000-0000-0000-000000000005"), "juro", 0); }
            case 7369 -> shot(mc, "70_03_juro_0");
            case 7392 -> shot(mc, "70_03_juro_1");
            case 7414 -> shot(mc, "70_03_juro_2");
            case 7440 -> { mc.setScreen(null); ClientGame.startVictimFx("はなこ", UUID.fromString("00000000-0000-0000-0000-000000000005"), "pingpong", 0); }
            case 7457 -> shot(mc, "70_04_pingpong_0");
            case 7479 -> shot(mc, "70_04_pingpong_1");
            case 7500 -> shot(mc, "70_04_pingpong_2");
            case 7526 -> { mc.setScreen(null); ClientGame.startVictimFx("はなこ", UUID.fromString("00000000-0000-0000-0000-000000000005"), "pillow", 0); }
            case 7542 -> shot(mc, "70_05_pillow_0");
            case 7564 -> shot(mc, "70_05_pillow_1");
            case 7585 -> shot(mc, "70_05_pillow_2");
            case 7610 -> { mc.setScreen(null); ClientGame.startVictimFx("はなこ", UUID.fromString("00000000-0000-0000-0000-000000000005"), "fusuma", 0); }
            case 7626 -> shot(mc, "70_06_fusuma_0");
            case 7647 -> shot(mc, "70_06_fusuma_1");
            case 7667 -> shot(mc, "70_06_fusuma_2");
            case 7692 -> { mc.setScreen(null); ClientGame.startVictimFx("はなこ", UUID.fromString("00000000-0000-0000-0000-000000000005"), "seoi", 0); }
            case 7708 -> shot(mc, "70_07_seoi_0");
            case 7730 -> shot(mc, "70_07_seoi_1");
            case 7751 -> shot(mc, "70_07_seoi_2");
            case 7776 -> { mc.setScreen(null); ClientGame.startVictimFx("はなこ", UUID.fromString("00000000-0000-0000-0000-000000000005"), "neko", 0); }
            case 7791 -> shot(mc, "70_08_neko_0");
            case 7811 -> shot(mc, "70_08_neko_1");
            case 7830 -> shot(mc, "70_08_neko_2");
            case 7854 -> { mc.setScreen(null); ClientGame.startVictimFx("はなこ", UUID.fromString("00000000-0000-0000-0000-000000000005"), "wolf", 1); }
            case 7879 -> shot(mc, "70_09_wolf_guard_0");
            case 7889 -> shot(mc, "70_09_wolf_guard_1");
            case 7902 -> shot(mc, "70_09_wolf_guard_2");
            case 7949 -> { mc.setScreen(null); ClientGame.startVictimFx("はなこ", UUID.fromString("00000000-0000-0000-0000-000000000005"), "wolf", 2); }
            case 8009 -> shot(mc, "70_10_wolf_counter_0");
            case 8044 -> shot(mc, "70_10_wolf_counter_1");
            case 8059 -> shot(mc, "70_10_wolf_counter_2");
            case 8077 -> mc.setScreen(new com.minpaku.client.screen.PreviewScreen(new com.minpaku.client.scene.EjectScene.Cast(mc.player.getUUID(), "Player", "Player はてるてるだった！", List.of(UUID.nameUUIDFromBytes("c0".getBytes()), UUID.nameUUIDFromBytes("c1".getBytes()), UUID.nameUUIDFromBytes("c2".getBytes())))));
            case 8097 -> shot(mc, "71_teru_0");
            case 8137 -> shot(mc, "71_teru_1");
            case 8172 -> shot(mc, "71_teru_2");
            case 8202 -> shot(mc, "71_teru_3");
            case 8227 -> shot(mc, "71_teru_4");
            case 8242 -> com.minpaku.client.Previews.start("result", "fox");
            case 8282 -> shot(mc, "72_result_fox");
            case 8292 -> com.minpaku.client.Previews.start("reveal", "shinobi");
            case 8372 -> shot(mc, "72_reveal_shinobi");
            case 8382 -> { com.minpaku.client.screen.SettingsScreen.tab = 7; mc.setScreen(new com.minpaku.client.screen.SettingsScreen()); }
            case 8392 -> shot(mc, "72_settings");
            case 8402 -> mc.stop();
            default -> {
            }
        }
    }
}
