package com.minpaku.game;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * ゲームの設定（管理人の鍵の画面で変えられる）。
 *
 * ★ 設定を増やすときは DEFS に1行足すだけで画面にも出てくる。
 */
public final class GameSettings {

    public enum Kind { NUMBER, SECONDS, TOGGLE, CHOICE }

    public record Def(String id, String label, String group, Kind kind, int min, int max, int step, int def,
                      String[] choices) {
        public Def(String id, String label, String group, Kind kind, int min, int max, int step, int def) {
            this(id, label, group, kind, min, max, step, def, null);
        }

        public String format(int value) {
            return switch (kind) {
                case TOGGLE -> value != 0 ? "ON" : "OFF";
                case SECONDS -> value + "秒";
                case NUMBER -> String.valueOf(value);
                case CHOICE -> choices != null && value - min >= 0 && value - min < choices.length
                        ? choices[value - min] : String.valueOf(value);
            };
        }
    }

    /** 設定画面のタブ（この順に並ぶ） */
    public static final String[] GROUPS = {"マップ", "役職", "能力", "人狼", "会議", "タスク", "ボット"};

    private static final List<Def> DEFS = new ArrayList<>();

    static {
        // マップ（建てるときに使う。小さい順）
        DEFS.add(new Def("map", "建てるマップ", "マップ", Kind.CHOICE, 1, Plans.ALL.size(), 1, 3, Plans.labels()));
        // 役職の人数（Role に登録した役職ぶん自動で作る）
        for (Role role : Role.special()) {
            int def = role == Role.WOLF ? 1 : (role == Role.MADMAN ? 0 : 1);
            int min = role == Role.WOLF ? 1 : 0;
            int max = role == Role.WOLF ? 3 : 2;
            DEFS.add(new Def("role." + role.id, role.name + "の人数", "役職", Kind.NUMBER, min, max, 1, def));
        }
        DEFS.add(new Def("seer_uses", "占いの回数（会議ごと）", "能力", Kind.NUMBER, 0, 5, 1, 1));
        DEFS.add(new Def("knight_uses", "騎士が守れる回数（会議ごと）", "能力", Kind.NUMBER, 0, 5, 1, 1));
        DEFS.add(new Def("medium_result", "霊媒師が追放結果を知れる", "能力", Kind.TOGGLE, 0, 1, 1, 1));
        DEFS.add(new Def("madman_vent", "狂人もベントを使える", "能力", Kind.TOGGLE, 0, 1, 1, 1));
        DEFS.add(new Def("kill_cooldown", "キルのクールダウン", "人狼", Kind.SECONDS, 10, 60, 5, 25));
        DEFS.add(new Def("kill_behind", "背後からのみキルできる", "人狼", Kind.TOGGLE, 0, 1, 1, 1));
        DEFS.add(new Def("kill_range", "キルできる距離（ブロック）", "人狼", Kind.NUMBER, 1, 4, 1, 2));
        DEFS.add(new Def("sabotage_cooldown", "妨害のクールダウン", "人狼", Kind.SECONDS, 10, 90, 5, 30));
        DEFS.add(new Def("gas_time", "ガス漏れの制限時間", "人狼", Kind.SECONDS, 20, 90, 5, 45));
        DEFS.add(new Def("discussion_time", "議論の時間", "会議", Kind.SECONDS, 0, 180, 15, 45));
        DEFS.add(new Def("voting_time", "投票の時間", "会議", Kind.SECONDS, 15, 300, 15, 60));
        DEFS.add(new Def("emergency_uses", "緊急ボタンの回数（1人あたり）", "会議", Kind.NUMBER, 0, 5, 1, 1));
        DEFS.add(new Def("confirm_eject", "追放時に人狼だったか全員に表示", "会議", Kind.TOGGLE, 0, 1, 1, 0));
        DEFS.add(new Def("anonymous_votes", "匿名投票", "会議", Kind.TOGGLE, 0, 1, 1, 0));
        DEFS.add(new Def("tasks", "タスクの数（1人あたり）", "タスク", Kind.NUMBER, 1, 12, 1, 5));
        DEFS.add(new Def("bots_enabled", "ボットを参加させる", "ボット", Kind.TOGGLE, 0, 1, 1, 1));
        DEFS.add(new Def("bot_fill", "ボットで何人までうめるか（合計人数）", "ボット", Kind.NUMBER, 2, 12, 1, 8));
        DEFS.add(new Def("bot_level", "ボットの強さ", "ボット", Kind.CHOICE, 1, 3, 1, 2,
                new String[]{"よわい", "ふつう", "つよい"}));
        DEFS.add(new Def("bot_talk", "ボットが会議でしゃべる", "ボット", Kind.TOGGLE, 0, 1, 1, 1));
    }

    public static List<Def> defs() {
        return Collections.unmodifiableList(DEFS);
    }

    public static Def def(String id) {
        for (Def d : DEFS) {
            if (d.id.equals(id)) {
                return d;
            }
        }
        return null;
    }

    private final Map<String, Integer> values = new LinkedHashMap<>();

    public GameSettings() {
        for (Def d : DEFS) {
            values.put(d.id, d.def);
        }
    }

    public int get(String id) {
        Integer v = values.get(id);
        if (v == null) {
            Def d = def(id);
            return d == null ? 0 : d.def;
        }
        return v;
    }

    public boolean flag(String id) {
        return get(id) != 0;
    }

    public int roleCount(Role role) {
        return get("role." + role.id);
    }

    /** 範囲内に収めて設定する。変わったら true */
    public boolean set(String id, int value) {
        Def d = def(id);
        if (d == null) {
            return false;
        }
        int v = Math.max(d.min, Math.min(d.max, value));
        Integer old = values.put(id, v);
        return old == null || old != v;
    }

    public Map<String, Integer> values() {
        return Collections.unmodifiableMap(values);
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        values.forEach(tag::putInt);
        return tag;
    }

    public void load(CompoundTag tag) {
        for (Def d : DEFS) {
            if (tag.contains(d.id)) {
                set(d.id, tag.getInt(d.id));
            }
        }
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeVarInt(values.size());
        values.forEach((k, v) -> {
            buf.writeUtf(k);
            buf.writeVarInt(v);
        });
    }

    public static GameSettings read(FriendlyByteBuf buf) {
        GameSettings s = new GameSettings();
        int n = buf.readVarInt();
        for (int i = 0; i < n; i++) {
            String k = buf.readUtf();
            int v = buf.readVarInt();
            s.values.put(k, v);
        }
        return s;
    }
}
