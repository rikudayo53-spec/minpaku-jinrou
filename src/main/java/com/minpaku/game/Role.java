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
        WOLF("人狼陣営", 0xFF5555),
        FOX("妖狐", 0xFFB300),
        TERU("てるてる", 0xBFE6FF),
        GOD("神（女将）", 0xFFE58A);

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

    // ---- 人狼陣営（それぞれ専用のフィニッシャー）
    public static final Role SHINOBI = register(new Role("shinobi", "忍び狼", Team.WOLF, 0x5C6BC0,
            "キルした死体は、しばらく誰にも見えない（通報されにくい）。")
            .markKill().markVent().markSabotage().markKnows().markSeenWolf().finisher("shinobi"));
    public static final Role KAIRIKI = register(new Role("kairiki", "怪力狼", Team.WOLF, 0xD84315,
            "正面からでもキルできる。そのかわりキルの待ち時間が長い。")
            .markKill().markVent().markSabotage().markKnows().markSeenWolf().finisher("kairiki"));
    public static final Role JURO = register(new Role("juro", "呪狼", Team.WOLF, 0x7E57C2,
            "占い師に占われると、占った占い師が呪いで倒れる。")
            .markKill().markVent().markSabotage().markKnows().markSeenWolf().finisher("juro"));
    public static final Role EVIL_GUESSER = register(new Role("evil_guesser", "イビルゲッサー", Team.WOLF, 0xB71C1C,
            "人狼の仲間。会議中に役職を当てると、その人を撃ち抜ける。外れたら自分が倒れる。")
            .markKill().markVent().markSabotage().markKnows().markSeenWolf());

    // ---- 村人陣営
    public static final Role DETECTIVE = register(new Role("detective", "探偵", Team.VILLAGE, 0x8D6E63,
            "虫めがねで死体を調べると、犯人の役職（倒し方）が分かる。")
            .markTasks());
    public static final Role NEKOMATA = register(new Role("nekomata", "猫又", Team.VILLAGE, 0xF48FB1,
            "人狼に襲われると、犯人を道連れにする。")
            .markTasks());
    public static final Role ITAMAE = register(new Role("itamae", "板前", Team.VILLAGE, 0x26A69A,
            "おにぎりを人に渡すと、その人は1回だけ人狼のキルを耐える。")
            .markTasks());
    public static final Role BANTO = register(new Role("banto", "番頭", Team.VILLAGE, 0xA1887F,
            "宿帳（J）で、どの部屋に何人いるかが分かる。")
            .markTasks());
    public static final Role KYOYU = register(new Role("kyoyu", "共有者", Team.VILLAGE, 0x4DB6AC,
            "2人組。おたがいが村人だと知っている。")
            .markTasks());
    public static final Role NICE_GUESSER = register(new Role("nice_guesser", "ナイスゲッサー", Team.VILLAGE, 0x43A047,
            "会議中に人狼陣営の役職を当てると、その人を撃ち抜ける。外れたら自分が倒れる。")
            .markTasks());
    public static final Role NICE_BUTTONER = register(new Role("nice_buttoner", "ナイスボタナー", Team.VILLAGE, 0x1E88E5,
            "手持ちの呼び鈴で、どこからでも緊急会議を開ける。")
            .markTasks());
    public static final Role SHERIFF = register(new Role("sheriff", "シェリフ", Team.VILLAGE, 0x546E7A,
            "銃で人狼陣営を撃てる。村人を撃つと、自分が倒れる。")
            .markTasks());

    // ---- 第三陣営
    public static final Role TERU = register(new Role("teru", "てるてる", Team.TERU, 0x81D4FA,
            "会議で追放されたら一人勝ち。うまく疑われよう。"));
    public static final Role FOX = register(new Role("fox", "妖狐", Team.FOX, 0xFFB300,
            "人狼に襲われても死なない。占われると消える。最後まで生き残れば勝ち。"));
    public static final Role OKAMI = register(new Role("okami", "女将（神）", Team.GOD, 0xE8C46A,
            "全員の役職が見える。最後まで生き残れば、どの陣営が勝っても勝ちを横取りする。"));

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
    private String finisher = "";

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
    private Role finisher(String id) { finisher = id; return this; }

    public boolean canKill() { return canKill; }
    public boolean canVent() { return canVent; }
    public boolean canSabotage() { return canSabotage; }
    public boolean knowsWolves() { return knowsWolves; }
    /** 占い師に「人狼」と判定されるか / 勝敗の人数計算で人狼として数えるか */
    public boolean isWolf() { return seenAsWolf; }
    /** タスクが村のタスク進行に数えられるか */
    public boolean hasRealTasks() { return realTasks; }
    /** キル役職の専用フィニッシャー（人狼は "wolf"）。キルできない役職は "" */
    public String finisherId() { return !finisher.isEmpty() ? finisher : canKill ? "wolf" : ""; }

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
