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
    public static final int VICTIM_FX_LEN = 125;
    public static int killFxLen = KILL_FX_LEN;

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
        if (announceTicks > 0) {
            announceTicks--;
        }
        if (killFxTicks > 0) {
            killFxTicks--;
            if (killFxKind == 0) {
                // 人狼のうなり → 遠吠え → 3回ひっかく → 噛みつく → 心臓の音
                int t = killFxLen - killFxTicks;
                if (t == 2) {
                    play(net.minecraft.sounds.SoundEvents.WOLF_GROWL, 0.6f);
                }
                if (t == 14) {
                    play(net.minecraft.sounds.SoundEvents.RAVAGER_ROAR, 1.25f);
                }
                if (t == 22 || t == 27) {
                    play(net.minecraft.sounds.SoundEvents.PLAYER_ATTACK_SWEEP, t == 22 ? 0.75f : 0.9f);
                }
                if (t == 32) {
                    play(net.minecraft.sounds.SoundEvents.PLAYER_ATTACK_CRIT, 0.6f);
                    play(net.minecraft.sounds.SoundEvents.PLAYER_HURT, 0.6f);
                }
                if (t == 50 || t == 70) {
                    play(net.minecraft.sounds.SoundEvents.WARDEN_HEARTBEAT, 0.8f);
                }
            }
        }
    }

    public static void reset() {
        state = Packets.State.lobby();
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
