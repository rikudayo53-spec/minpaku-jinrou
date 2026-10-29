package com.minpaku.game;

/**
 * マップに置く専用ブロックの種類。置くだけでマップに登録される。
 *
 * ★ タスクを増やすときは、ここに1行足して ModBlocks で登録、モデルを作る。
 */
public enum MarkerType {
    // 設備
    SPAWN("entrance_mat", "玄関マット（スタート地点）", Use.NONE, null, 0),
    BELL("call_bell", "呼び鈴（緊急ボタン）", Use.NONE, null, 0),
    VENT("floor_hatch", "床下点検口（ベント）", Use.NONE, null, 0),
    BREAKER("breaker", "配電盤（停電を直す）", Use.NONE, null, 0),
    GAS_VALVE("gas_valve", "ガスの元栓（ガス漏れを直す）", Use.HOLD, "ガスの元栓を閉める", 40),
    BOILER("boiler", "ボイラー（ガス漏れを直す）", Use.HOLD, "ボイラーを止める", 40),
    MEETING("meeting_table", "会議の座卓（会議の集合場所）", Use.NONE, null, 0),
    HOKORA("hokora", "祠（悪神が封じられている）", Use.NONE, null, 0),

    // タスク（ミニゲーム）
    WIRING("wiring_panel", "配線盤", Use.WIRES, "配線をつなぐ", 0),
    LEDGER("ledger", "予約台帳", Use.NUMBERS, "予約台帳を整理する", 0),

    // タスク（マイクラらしい作業）
    FUTON("futon", "布団", Use.HOLD, "布団を敷く", 80),
    KAMADO("kamado", "かまど", Use.HOLD, "ご飯を炊く", 100),
    BATH("bath_stove", "風呂釜", Use.HOLD, "お風呂を沸かす", 80),
    CHOPPING("chopping_block", "薪割り台", Use.CLICK, "薪を割る", 6),
    FIELD("vegetable_patch", "畑", Use.CLICK, "野菜を収穫する", 4),
    DUST("dust_pile", "ほこり", Use.CLICK, "部屋を掃除する", 5),

    // 第2弾で追加したタスク
    LAUNDRY("laundry_pole", "物干し", Use.HOLD, "洗濯物を干す", 80),
    VASE("flower_vase", "花瓶", Use.CLICK, "花に水をやる", 3),
    SINK("kitchen_sink", "流し台", Use.HOLD, "お皿を洗う", 80),
    SHOE_RACK("shoe_rack", "下駄箱", Use.CLICK, "靴をそろえる", 4),
    SHOJI("shoji_screen", "障子", Use.HOLES, "障子の穴をふさぐ", 0),

    // 第3弾で追加したタスク
    ANDON("andon", "行灯", Use.HOLD, "行灯に火を入れる", 60),
    IRORI("irori", "囲炉裏", Use.HOLD, "囲炉裏の火をおこす", 90),
    BONSAI("bonsai", "盆栽", Use.CLICK, "盆栽を手入れする", 5),
    TEA("tea_set", "茶道具", Use.HOLD, "お茶をたてる", 80),
    WELL("well", "井戸", Use.CLICK, "井戸の水をくむ", 5),
    SHOGI("shogi_board", "将棋盤", Use.CLICK, "将棋の駒を並べる", 6),
    KAKEJIKU("kakejiku", "掛け軸", Use.CLICK, "掛け軸をまっすぐにする", 3),

    // 第4弾で追加したタスク（GeckoLib のアニメーション）
    MAILBOX("mailbox", "郵便受け", Use.CLICK, "手紙を仕分ける", 4),
    GOLDFISH("goldfish_bowl", "金魚鉢", Use.CLICK, "金魚にえさをやる", 3),
    FURIN("wind_chime", "風鈴", Use.CLICK, "風鈴を吊るす", 3),
    MOCHI("mochi_mortar", "臼と杵", Use.CLICK, "餅をつく", 8),
    ZEN("zen_garden", "枯山水", Use.HOLD, "砂紋を描く", 80),
    WASHTUB("washtub", "たらい", Use.HOLD, "洗濯板で洗う", 80),

    // 飾り（タスクではない。置くだけ）
    TANSU("tansu", "箪笥（飾り）", Use.NONE, null, 0),
    CHABUDAI("chabudai", "ちゃぶ台（飾り）", Use.NONE, null, 0),
    BYOBU("byobu", "屏風（飾り）", Use.NONE, null, 0),
    TOUROU("tourou", "石灯籠（飾り）", Use.NONE, null, 0),
    SAKE("sake_barrel", "酒樽（飾り）", Use.NONE, null, 0),
    KASA("kasa_tate", "傘立て（飾り）", Use.NONE, null, 0),
    TANUKI("tanuki", "信楽たぬき（飾り）", Use.NONE, null, 0),
    MANEKI("maneki_neko", "招き猫（飾り）", Use.NONE, null, 0),
    HIBACHI("hibachi", "火鉢（飾り）", Use.NONE, null, 0),
    TSUBO("tsubo", "壺（飾り）", Use.NONE, null, 0);

    public enum Use {
        /** 何もしない（設備） */
        NONE,
        /** 近くで見続けると進む（amount = かかるtick） */
        HOLD,
        /** 何回か右クリックする（amount = 回数） */
        CLICK,
        /** 配線ミニゲーム */
        WIRES,
        /** 数字を順番に押すミニゲーム */
        NUMBERS,
        /** 障子の穴をふさぐミニゲーム */
        HOLES
    }

    public final String blockId;
    public final String label;
    public final Use use;
    public final String taskName;
    public final int amount;

    MarkerType(String blockId, String label, Use use, String taskName, int amount) {
        this.blockId = blockId;
        this.label = label;
        this.use = use;
        this.taskName = taskName;
        this.amount = amount;
    }

    public boolean isTask() {
        return taskName != null && this != GAS_VALVE && this != BOILER;
    }

    /** 飾りの家具（ゲームでは何もしない） */
    public boolean isDecor() {
        return ordinal() >= TANSU.ordinal();
    }

    public static MarkerType byOrdinal(int i) {
        MarkerType[] v = values();
        return i >= 0 && i < v.length ? v[i] : SPAWN;
    }
}
