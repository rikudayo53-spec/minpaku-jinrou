package com.minpaku.game;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 役職。
 *
 * ★ 新しい役職を追加するときは、下に register(...) を1つ足して、
 *   必要なら GameManager に能力の処理を書く。
 *   人数の設定（role.<id>）は設定アイテムの画面に自動で出てくる。
 */
public final class Role {
    public enum Team {
        VILLAGE("村人陣営", 0x55FF55),
        WOLF("人狼陣営", 0xFF5555);

        public final String label;
        public final int color;

        Team(String label, int color) {
            this.label = label;
            this.color = color;
        }
    }

    private static final Map<String, Role> BY_ID = new LinkedHashMap<>();

    public static final Role VILLAGER = register(new Role("villager", "村人", Team.VILLAGE, 0x7CD66B,
            "タスクをこなして、人狼を見つけ出そう。")
            .markTasks());
    public static final Role WOLF = register(new Role("wolf", "人狼", Team.WOLF, 0xE53935,
            "背後から近づいて村人を襲え。ベントと妨害も使える。")
            .markKill().markVent().markSabotage().markKnows().markSeenWolf());
    public static final Role MADMAN = register(new Role("madman", "狂人", Team.WOLF, 0xB04BD6,
            "人狼の味方。人狼が誰かは分からない。村をかき乱そう。")
            .markVent());
    public static final Role SEER = register(new Role("seer", "占い師", Team.VILLAGE, 0x6EC6FF,
            "水晶を近くの人に使うと、人狼かどうかが分かる。")
            .markTasks());
    public static final Role KNIGHT = register(new Role("knight", "騎士", Team.VILLAGE, 0xF2C94C,
            "お守りを人に使うと、次の会議までキルから守れる。")
            .markTasks());
    public static final Role MEDIUM = register(new Role("medium", "霊媒師", Team.VILLAGE, 0x9FA8DA,
            "追放された人が人狼だったかどうかが分かる。")
            .markTasks());

    public final String id;
    public final String name;
    public final Team team;
    public final int color;
    public final String description;
    private boolean canKill;
    private boolean canVent;
    private boolean canSabotage;
    private boolean knowsWolves;
    private boolean seenAsWolf;
    private boolean realTasks;

    private Role(String id, String name, Team team, int color, String description) {
        this.id = id;
        this.name = name;
        this.team = team;
        this.color = color;
        this.description = description;
    }

    private static Role register(Role role) {
        BY_ID.put(role.id, role);
        return role;
    }

    private Role markKill() { canKill = true; return this; }
    private Role markVent() { canVent = true; return this; }
    private Role markSabotage() { canSabotage = true; return this; }
    private Role markKnows() { knowsWolves = true; return this; }
    private Role markSeenWolf() { seenAsWolf = true; return this; }
    private Role markTasks() { realTasks = true; return this; }

    public boolean canKill() { return canKill; }
    public boolean canVent() { return canVent; }
    public boolean canSabotage() { return canSabotage; }
    public boolean knowsWolves() { return knowsWolves; }
    /** 占い師に「人狼」と判定されるか / 勝敗の人数計算で人狼として数えるか */
    public boolean isWolf() { return seenAsWolf; }
    /** タスクが村のタスク進行に数えられるか */
    public boolean hasRealTasks() { return realTasks; }

    public static Role byId(String id) {
        return BY_ID.getOrDefault(id, VILLAGER);
    }

    public static List<Role> all() {
        return Collections.unmodifiableList(new ArrayList<>(BY_ID.values()));
    }

    /** 人数を設定できる役職（村人は残り全員なので除く） */
    public static List<Role> special() {
        List<Role> out = new ArrayList<>();
        for (Role r : BY_ID.values()) {
            if (r != VILLAGER) {
                out.add(r);
            }
        }
        return out;
    }
}
