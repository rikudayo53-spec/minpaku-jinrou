package com.minpaku.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.client.resources.PlayerSkin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** 妨害「スキン入れ替え」：だれのスキンがだれのものに見えるか（次の会議まで） */
public final class SkinSwap {
    private SkinSwap() {
    }

    public static final Map<UUID, UUID> MAP = new HashMap<>();

    /** data = "見た目を変える人>スキンの持ち主;..."（空なら元に戻す） */
    public static void set(String data) {
        MAP.clear();
        if (data == null || data.isEmpty()) {
            return;
        }
        for (String part : data.split(";")) {
            int i = part.indexOf('>');
            if (i > 0) {
                try {
                    MAP.put(UUID.fromString(part.substring(0, i)), UUID.fromString(part.substring(i + 1)));
                } catch (IllegalArgumentException ignored) {
                }
            }
        }
    }

    /** 入れ替わったスキン（入れ替わっていなければ null） */
    public static PlayerSkin skinOf(UUID who) {
        UUID src = MAP.get(who);
        if (src == null || !ClientGame.inGame()) {
            return null;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.getConnection() != null) {
            PlayerInfo info = mc.getConnection().getPlayerInfo(src);
            if (info != null) {
                return info.getSkin();
            }
        }
        return DefaultPlayerSkin.get(src);
    }
}
