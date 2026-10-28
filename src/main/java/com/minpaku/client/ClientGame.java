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
    public static final int KILL_FX_LEN = 60;

    private ClientGame() {
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
