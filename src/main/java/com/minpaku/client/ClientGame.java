package com.minpaku.client;

import com.minpaku.game.MarkerType;
import com.minpaku.game.Role;
import com.minpaku.network.Packets;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.Holder;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.client.resources.PlayerSkin;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/** クライアントが持っているゲームの状態 */
public final class ClientGame {
    public static Packets.State state = Packets.State.lobby();
    @Nullable
    public static Packets.Settings settings;

    // 画面中央のお知らせ
    public static String announceTitle = "";
    public static String announceSub = "";
    public static int announceColor = 0xFFFFFF;
    public static int announceTicks;
    public static final int ANNOUNCE_LEN = 70;

    // キルされた・キルした演出
    public static int killFxTicks;
    public static int killFxKind;
    public static String killFxOther = "";
    public static UUID killFxOtherId = new UUID(0, 0);
    public static final int KILL_FX_LEN = 60;
    /** 襲われた側の暗転演出の長さ */
    public static final int VICTIM_FX_LEN = 140;
    /** 襲われた側：フィニッシャーの種類と、0 = ふつう / 1 = おにぎりで耐えた / 2 = 猫又の反撃 */
    public static String killFxFinisher = "wolf";
    public static int killFxVariant;
    public static com.minpaku.client.scene.FinisherScenes.Ctx killFxCtx;
    /** フィニッシャーのあとの文字（顔・幽霊になった）にかける時間 */
    public static final int VICTIM_TAIL = 64;

    /** 襲われた演出を始める（ゲーム中・確認コマンド共通） */
    public static void startVictimFx(String other, UUID otherId, String finisher, int variant) {
        killFxKind = 0;
        killFxOther = other;
        killFxOtherId = otherId;
        killFxFinisher = finisher;
        killFxVariant = variant;
        killFxCtx = new com.minpaku.client.scene.FinisherScenes.Ctx(otherId, other, variant);
        killFxLen = com.minpaku.client.scene.FinisherScenes.total(finisher, variant) + (variant == 1 ? 40 : VICTIM_TAIL);
        killFxTicks = killFxLen;
    }
    public static int killFxLen = KILL_FX_LEN;

    // 番頭：部屋ごとの人数
    public static java.util.List<String> roomNames = java.util.List.of();
    public static java.util.List<Integer> roomCounts = java.util.List.of();
    /** 自分が選んだ共通フィニッシャー */
    public static String myFinisher = "pingpong";

    /** 見取り図の部屋（名前と範囲） */
    public record MapRoom(String name, int x1, int y1, int z1, int x2, int y2, int z2) {
    }

    public static final java.util.List<MapRoom> rooms = new java.util.ArrayList<>();
    /** 会議の前（ゲーム中）に最後にいた場所 */
    @Nullable
    public static net.minecraft.world.phys.Vec3 lastPlayPos;

    /** 井戸端会議（会議中も自由に動ける）か */
    public static boolean meetingFree;
    /** 今の会議の画面（井戸端会議では閉じても裏で進む） */
    @Nullable
    public static com.minpaku.client.screen.MeetingScreen currentMeeting;

    /** ゲーム前にベントに入っている（お試し） */
    public static boolean lobbyVent;
    /** 役職が見える人（神）：UUID → 役職 */
    public static final java.util.Map<UUID, String> knownRoles = new java.util.HashMap<>();
    /** マップの範囲（x1,y1,z1,x2,y2,z2）。外は時空のゆがみで見えない */
    @Nullable
    public static int[] bounds;
    /** 祠の悪神が目覚めた演出（残り tick） */
    public static int evilGodTicks;
    public static final int EVIL_GOD_LEN = 90;

    /** 小さな通知を受け取る */
    public static void onInfo(Packets.Info p) {
        switch (p.kind()) {
            case "lobby_vent" -> lobbyVent = p.data().equals("1");
            case "meeting_free" -> meetingFree = p.data().equals("1");
            case "rooms" -> {
                rooms.clear();
                for (String part : p.data().split(";")) {
                    String[] v = part.split("\\|");
                    if (v.length == 7) {
                        try {
                            rooms.add(new MapRoom(v[0], Integer.parseInt(v[1]), Integer.parseInt(v[2]), Integer.parseInt(v[3]),
                                    Integer.parseInt(v[4]), Integer.parseInt(v[5]), Integer.parseInt(v[6])));
                        } catch (NumberFormatException ignored) {
                        }
                    }
                }
            }
            case "roles" -> {
                knownRoles.clear();
                for (String part : p.data().split(";")) {
                    int eq = part.indexOf('=');
                    if (eq > 0) {
                        try {
                            knownRoles.put(UUID.fromString(part.substring(0, eq)), part.substring(eq + 1));
                        } catch (IllegalArgumentException ignored) {
                        }
                    }
                }
            }
            case "bounds" -> {
                String[] v = p.data().split(",");
                if (v.length == 6) {
                    int[] b = new int[6];
                    for (int i = 0; i < 6; i++) {
                        b[i] = Integer.parseInt(v[i].trim());
                    }
                    bounds = b;
                } else {
                    bounds = null;
                }
            }
            case "evil_god" -> {
                evilGodTicks = EVIL_GOD_LEN;
                announce("悪神 降臨", "祠の封印が解けた…役職がすべて入れかわる！", 0xC060FF);
                play(net.minecraft.sounds.SoundEvents.LIGHTNING_BOLT_THUNDER, 0.7f);
            }
            case "meeting_death" -> {
                String[] v = p.data().split(";", 3);
                if (v.length == 3 && currentMeeting != null) {
                    currentMeeting.onDeath(UUID.fromString(v[0]), v[2]);
                }
            }
            default -> {
            }
        }
    }

    // 会議中の発言
    public static final java.util.List<Packets.Talk> talk = new java.util.ArrayList<>();

    // クールダウンの最大値（ゲージの割合を出すため、値が増えたときに覚える）
    public static int killCdMax = 1;
    public static int sabotageCdMax = 1;
    private static int lastKillCd;
    private static int lastSabotageCd;

    private ClientGame() {
    }

    public static void onState(Packets.State s) {
        if (s.killCooldown() > lastKillCd) {
            killCdMax = Math.max(1, s.killCooldown());
        }
        if (s.sabotageCooldown() > lastSabotageCd) {
            sabotageCdMax = Math.max(1, s.sabotageCooldown());
        }
        lastKillCd = s.killCooldown();
        lastSabotageCd = s.sabotageCooldown();
        state = s;
    }

    public static boolean inGame() {
        return state.phase() == 1 || state.phase() == 2;
    }

    public static Role role() {
        return state.role().isEmpty() ? null : Role.byId(state.role());
    }

    public static boolean isMate(UUID id) {
        for (Packets.Mate m : state.mates()) {
            if (m.id().equals(id)) {
                return true;
            }
        }
        return false;
    }

    public static void announce(String title, String sub, int color) {
        announceTitle = title;
        announceSub = sub;
        announceColor = color;
        announceTicks = ANNOUNCE_LEN;
    }

    public static void tick() {
        Minecraft mc0 = Minecraft.getInstance();
        if (state.phase() == 1 && mc0.player != null) {
            lastPlayPos = mc0.player.position();
        }
        if (evilGodTicks > 0) {
            evilGodTicks--;
        }
        if (announceTicks > 0) {
            announceTicks--;
        }
        if (killFxTicks > 0) {
            killFxTicks--;
            if (killFxKind == 0) {
                int t = killFxLen - killFxTicks;
                int end = com.minpaku.client.scene.FinisherScenes.total(killFxFinisher, killFxVariant);
                if (t <= end) {
                    com.minpaku.client.scene.FinisherScenes.tick(t, killFxFinisher, killFxVariant);
                }
                if (killFxVariant != 1 && (t == end + 8 || t == end + 26)) {
                    play(net.minecraft.sounds.SoundEvents.WARDEN_HEARTBEAT, 0.8f);
                }
                if (killFxVariant == 1 && t == end + 4) {
                    play(net.minecraft.sounds.SoundEvents.PLAYER_LEVELUP, 1.4f);
                }
            }
        }
    }

    public static void reset() {
        state = Packets.State.lobby();
        lobbyVent = false;
        currentMeeting = null;
        knownRoles.clear();
        bounds = null;
        announceTicks = 0;
        killFxTicks = 0;
    }

    public static String taskName(int type) {
        MarkerType t = MarkerType.byOrdinal(type);
        return t.taskName != null ? t.taskName : t.label;
    }

    public static PlayerSkin skin(UUID id) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.getConnection() != null) {
            PlayerInfo info = mc.getConnection().getPlayerInfo(id);
            if (info != null) {
                return info.getSkin();
            }
        }
        return DefaultPlayerSkin.get(id);
    }

    public static void play(SoundEvent sound, float pitch) {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(sound, pitch));
    }

    public static void play(Holder<SoundEvent> sound, float pitch) {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(sound, pitch));
    }
}
