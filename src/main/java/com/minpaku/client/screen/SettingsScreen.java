package com.minpaku.client.screen;

import com.minpaku.client.ClientGame;
import com.minpaku.client.Ui;
import com.minpaku.game.GameSettings;
import com.minpaku.game.MarkerType;
import com.minpaku.game.Role;
import com.minpaku.network.Packets;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;

import java.util.ArrayList;
import java.util.List;

/** 管理人の帳簿：設定をタブで切りかえて変える。ゲームの開始・中止、マップを建てるもここ */
public class SettingsScreen extends Screen {
    public static int tab;
    private boolean confirmBuild;
    private int px, py, pw, ph;

    /** クリックできる場所 */
    private record Hit(int x, int y, int w, int h, Runnable action) {
    }

    private final List<Hit> hits = new ArrayList<>();

    public SettingsScreen() {
        super(Component.literal("民泊人狼の設定"));
    }

    @Override
    protected void init() {
        pw = Math.min(width - 16, 440);
        ph = Math.min(height - 16, 300);
        px = (width - pw) / 2;
        py = (height - ph) / 2;
    }

    public void refresh() {
    }

    private boolean canEdit() {
        return ClientGame.settings != null && ClientGame.settings.canEdit();
    }

    private boolean running() {
        return ClientGame.settings != null && ClientGame.settings.running();
    }

    private int value(GameSettings.Def d) {
        Packets.Settings s = ClientGame.settings;
        return s == null ? d.def() : s.settings().get(d.id());
    }

    private void change(GameSettings.Def d, int v) {
        v = Math.max(d.min(), Math.min(d.max(), v));
        ClientPlayNetworking.send(new Packets.SetSetting(d.id(), v));
        ClientGame.play(SoundEvents.UI_BUTTON_CLICK, 1.2f);
    }

    private static boolean in(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float pt) {
        renderBackground(g, mouseX, mouseY, pt);
        hits.clear();
        Packets.Settings s = ClientGame.settings;
        Ui.sprite(g, "panel", px, py, pw, ph);

        // 見出し
        Ui.stamp(g, "管理人", px + 22, py + 22, 26, Ui.VERMILION, -8, true);
        Ui.serif(g, "民泊人狼 ― 管理人の帳簿", px + 42, py + 10, Ui.INK, 1.4f, -1);
        String sub = s == null ? "読み込み中…" : !s.canEdit() ? "設定を変えられるのは管理人（OP）だけです"
                : s.running() ? "ゲーム中（変えた設定は次のゲームから）" : "設定を決めて「開店」でゲーム開始";
        Ui.sans(g, sub, px + 42, py + 26, s != null && !s.canEdit() ? Ui.VERMILION : Ui.INK_SOFT, 0.8f, -1);

        // マップを建てる
        String mapName = s == null ? "マップ" : com.minpaku.game.Plans.byIndex(s.settings().get("map")).name;
        String bl = confirmBuild ? "本当に建てる？" : "「" + mapName + "」を建てる";
        int bw = Math.max(104, (int) (Ui.width(Ui.sans(bl)) * 0.8f) + 16);
        int bx = px + pw - bw - 10;
        boolean bh = in(mouseX, mouseY, bx, py + 9, bw, 16);
        Ui.sprite(g, canEdit() && !running() ? "btn_dark" : "btn_off", bx, py + 9, bw, 16);
        Ui.sans(g, bl, bx + bw / 2f, py + 12.5f, bh ? 0xFFFFFFFF : Ui.CREAM, 0.8f, 0);
        if (canEdit() && !running()) {
            hits.add(new Hit(bx, py + 9, bw, 16, () -> {
                if (confirmBuild) {
                    ClientPlayNetworking.send(new Packets.Control(2));
                    onClose();
                } else {
                    confirmBuild = true;
                }
            }));
        }

        // マップの状態のチップ
        if (s != null) {
            int cx = px + 14;
            int cy = py + 40;
            for (String[] c : new String[][]{{"スタート", cnt(s, MarkerType.SPAWN)}, {"呼び鈴", cnt(s, MarkerType.BELL)},
                    {"ベント", cnt(s, MarkerType.VENT)}, {"配電盤", cnt(s, MarkerType.BREAKER)},
                    {"元栓", cnt(s, MarkerType.GAS_VALVE)}, {"ボイラー", cnt(s, MarkerType.BOILER)},
                    {"タスク", String.valueOf(taskCount(s))}}) {
                String label = c[0] + " " + c[1];
                int w = (int) (Ui.width(Ui.sans(label)) * 0.75f) + 10;
                boolean ok = !c[1].equals("0");
                g.fill(cx, cy, cx + w, cy + 10, ok ? 0xFF3F8F3A : 0xFFB0413A);
                Ui.sans(g, label, cx + w / 2f, cy + 1.5f, 0xFFFFFFFF, 0.75f, 0);
                cx += w + 4;
            }
        }

        // タブ
        int tx = px + 12;
        int ty = py + 56;
        for (int i = 0; i < GameSettings.GROUPS.length; i++) {
            String name = GameSettings.GROUPS[i];
            int w = (int) (Ui.width(Ui.sans(name)) * 0.9f) + 16;
            boolean on = i == tab;
            Ui.sprite(g, on ? "tab_on" : "tab_off", tx, ty + (on ? 0 : 2), w, on ? 16 : 14);
            Ui.sans(g, name, tx + w / 2f, ty + 4, on ? Ui.INK : 0xFF5A4330, 0.9f, 0);
            final int idx = i;
            hits.add(new Hit(tx, ty, w, 16, () -> {
                tab = idx;
                ClientGame.play(SoundEvents.BOOK_PAGE_TURN, 1.3f);
            }));
            tx += w + 2;
        }
        int lx = px + 12;
        int ly = ty + 16;
        int lw = pw - 24;
        int lh = ph - (ly - py) - 40;
        Ui.sprite(g, "box_dim", lx, ly, lw, lh);

        // 選んでいるタブの設定
        String group = GameSettings.GROUPS[Math.max(0, Math.min(tab, GameSettings.GROUPS.length - 1))];
        int ry = ly + 6;
        for (GameSettings.Def d : GameSettings.defs()) {
            if (!d.group().equals(group)) {
                continue;
            }
            row(g, d, lx + 8, ry, lw - 16, mouseX, mouseY);
            ry += 20;
        }
        // タブごとの補足
        if (group.equals("フィニッシャー")) {
            finisherTab(g, lx + 8, ry, lw - 16, ly + lh - ry - 6, mouseX, mouseY);
        }
        if (group.equals("マップ") && s != null) {
            mapTab(g, s, lx + 8, ry, lw - 16, ly + lh - ry - 6, mouseX, mouseY);
        }
        if (group.equals("役職") && s != null) {
            int special = 0;
            for (Role r : Role.special()) {
                special += s.settings().roleCount(r);
            }
            int total = s.settings().flag("bots_enabled") ? Math.max(1, s.settings().get("bot_fill")) : 1;
            Ui.sans(g, "特別な役職 " + special + " 人（残りは村人）　・　ボット込みで最大 " + total + " 人", lx + 8, ry + 2,
                    Ui.INK_SOFT, 0.75f, -1);
        }
        if (group.equals("ボット") && s != null && s.settings().flag("bots_enabled")) {
            String[] names = {"たろう", "はなこ", "けんた", "さくら", "ゆうき", "みお", "そうた", "あおい", "りく", "ひなた", "かいと"};
            Ui.sans(g, "人が足りないぶんを、ボットがうめます（ソロでも遊べる）", lx + 8, ry + 2, Ui.INK_SOFT, 0.75f, -1);
            int n = Math.min(names.length, Math.max(0, s.settings().get("bot_fill") - 1));
            for (int i = 0; i < n; i++) {
                int fx = lx + 10 + i * 30;
                if (fx + 24 > lx + lw) {
                    break;
                }
                g.fill(fx, ry + 14, fx + 20, ry + 34, 0xFFD8CCB2);
                Ui.sans(g, names[i].substring(0, 1), fx + 10, ry + 19, Ui.INK, 1f, 0);
                Ui.sans(g, names[i], fx + 10, ry + 37, Ui.INK_SOFT, 0.6f, 0);
            }
        }

        // 下のボタン
        int by = py + ph - 30;
        int cx = px + pw / 2;
        boolean canStart = canEdit() && !running();
        boolean h1 = in(mouseX, mouseY, cx - 150, by, 130, 22);
        Ui.button(g, cx - 150, by, 130, 22, "開店（ゲーム開始）", true, h1 && canStart, canStart, 1f);
        if (canStart) {
            hits.add(new Hit(cx - 150, by, 130, 22, () -> {
                ClientPlayNetworking.send(new Packets.Control(0));
                onClose();
            }));
        }
        boolean canStop = canEdit() && running();
        boolean h2 = in(mouseX, mouseY, cx - 12, by, 76, 22);
        Ui.button(g, cx - 12, by, 76, 22, "中止", false, h2 && canStop, canStop, 1f);
        if (canStop) {
            hits.add(new Hit(cx - 12, by, 76, 22, () -> {
                ClientPlayNetworking.send(new Packets.Control(1));
                onClose();
            }));
        }
        boolean h3 = in(mouseX, mouseY, cx + 72, by, 76, 22);
        Ui.button(g, cx + 72, by, 76, 22, "閉じる", false, h3, true, 1f);
        hits.add(new Hit(cx + 72, by, 76, 22, this::onClose));
    }

    private void row(GuiGraphics g, GameSettings.Def d, int x, int y, int w, int mouseX, int mouseY) {
        Ui.sans(g, d.label(), x + 2, y + 4, Ui.INK, 0.9f, -1);
        g.fill(x, y + 17, x + w, y + 18, 0x55C9B48E);
        int v = value(d);
        boolean edit = canEdit();
        if (d.kind() == GameSettings.Kind.TOGGLE) {
            int sx = x + w - 34;
            boolean on = v != 0;
            g.fill(sx, y + 2, sx + 30, y + 15, 0xFF3B2416);
            g.fill(sx + 1, y + 3, sx + 29, y + 14, on ? 0xFF3F8F3A : 0xFFB9AB92);
            int kx = on ? sx + 17 : sx + 2;
            g.fill(kx, y + 4, kx + 11, y + 13, 0xFFFFFFFF);
            Ui.sans(g, on ? "ON" : "OFF", sx - 4, y + 4.5f, on ? 0xFF2E7D32 : 0xFF8D7A62, 0.75f, 1);
            if (edit) {
                hits.add(new Hit(sx, y, 30, 17, () -> change(d, on ? 0 : 1)));
            }
            return;
        }
        int bx = x + w - 90;
        boolean hm = in(mouseX, mouseY, bx, y + 1, 16, 15);
        boolean hp = in(mouseX, mouseY, bx + 72, y + 1, 16, 15);
        Ui.button(g, bx, y + 1, 16, 15, "−", false, hm && edit, edit && v > d.min(), 1f);
        Ui.button(g, bx + 72, y + 1, 16, 15, "＋", false, hp && edit, edit && v < d.max(), 1f);
        Ui.sans(g, d.format(v), bx + 44, y + 3.5f, Ui.INK, 1.1f, 0);
        if (edit) {
            hits.add(new Hit(bx, y + 1, 16, 15, () -> change(d, v - d.step())));
            hits.add(new Hit(bx + 72, y + 1, 16, 15, () -> change(d, v + d.step())));
        }
    }

    private static String cnt(Packets.Settings s, MarkerType t) {
        return String.valueOf(s.counts().getOrDefault(t.ordinal(), 0));
    }

    private static int taskCount(Packets.Settings s) {
        int n = 0;
        for (MarkerType t : MarkerType.values()) {
            if (t.isTask()) {
                n += s.counts().getOrDefault(t.ordinal(), 0);
            }
        }
        return n;
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        for (Hit h : new ArrayList<>(hits)) {
            if (in(mx, my, h.x, h.y, h.w, h.h)) {
                h.action.run();
                return true;
            }
        }
        confirmBuild = false;
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double dx, double dy) {
        int n = GameSettings.GROUPS.length;
        tab = Math.floorMod(tab - (int) Math.signum(dy), n);
        return true;
    }

    /** フィニッシャーのタブ：自分の共通フィニッシャーを選ぶ・全部を見られる（だれでも使える） */
    private void finisherTab(GuiGraphics g, int x, int y, int w, int h, int mouseX, int mouseY) {
        Ui.sans(g, "自分のフィニッシャー（設定が「各自が選んだもの」のときに使われます）", x, y, Ui.INK_SOFT, 0.75f, -1);
        String[] common = {"pingpong", "pillow", "fusuma", "seoi"};
        int bw = (w - 12) / 4;
        for (int i = 0; i < common.length; i++) {
            String id = common[i];
            int bx = x + i * (bw + 4);
            int by = y + 12;
            boolean on = id.equals(com.minpaku.client.ClientGame.myFinisher);
            boolean hov = in(mouseX, mouseY, bx, by, bw, 34);
            g.fill(bx, by, bx + bw, by + 34, on ? 0xFFC8372D : hov ? 0x40C8372D : 0x20000000);
            String title = com.minpaku.client.scene.FinisherScenes.NAMES.get(id);
            Ui.sans(g, title, bx + bw / 2f, by + 5, on ? 0xFFFFFFFF : Ui.INK, 0.72f, 0);
            Ui.sans(g, on ? "選択中" : "クリックで選ぶ", bx + bw / 2f, by + 18, on ? 0xFFFFE9B8 : Ui.INK_SOFT, 0.65f, 0);
            hits.add(new Hit(bx, by, bw, 26, () -> {
                com.minpaku.client.ClientGame.myFinisher = id;
                ClientPlayNetworking.send(new Packets.SetFinisher(id));
                ClientGame.play(SoundEvents.BOOK_PAGE_TURN, 1.3f);
            }));
            previewButton(g, bx + bw / 2 - 18, by + 36, id, 0, mouseX, mouseY);
        }
        // 役職の専用フィニッシャーと、特別な版
        int ry = y + 64;
        Ui.sans(g, "役職の専用フィニッシャー・特別な演出（見るだけ）", x, ry, Ui.INK_SOFT, 0.75f, -1);
        String[][] list = {{"wolf", "0", "人狼"}, {"shinobi", "0", "忍び狼"}, {"kairiki", "0", "怪力狼"}, {"juro", "0", "呪狼"},
                {"wolf", "1", "おにぎり"}, {"wolf", "2", "猫又反撃"}, {"neko", "0", "猫又祟り"}};
        int col = 4;
        int cw = (w - 12) / col;
        for (int i = 0; i < list.length; i++) {
            int bx = x + (i % col) * (cw + 4);
            int by = ry + 12 + (i / col) * 22;
            Ui.sans(g, list[i][2], bx + 4, by + 4, Ui.INK, 0.75f, -1);
            previewButton(g, bx + cw - 40, by, list[i][0], Integer.parseInt(list[i][1]), mouseX, mouseY);
        }
    }

    private void previewButton(GuiGraphics g, int bx, int by, String id, int variant, int mouseX, int mouseY) {
        boolean hov = in(mouseX, mouseY, bx, by, 36, 14);
        Ui.sprite(g, "btn_dark", bx, by, 36, 14);
        Ui.sans(g, "見る", bx + 18, by + 3, hov ? 0xFFFFFFFF : Ui.CREAM, 0.7f, 0);
        hits.add(new Hit(bx, by, 36, 14, () -> {
            onClose();
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null) {
                // 確認用：自分が犯人役
                com.minpaku.client.ClientGame.startVictimFx(id.equals("neko") ? "たま" : mc.player.getGameProfile().getName(),
                        mc.player.getUUID(), id, variant);
            }
        }));
    }

    /** マップのタブ：6種類の一覧（クリックで選ぶ）と、選んだマップの間取り図 */
    private void mapTab(GuiGraphics g, Packets.Settings s, int x, int y, int w, int h, int mouseX, int mouseY) {
        int sel = s.settings().get("map");
        int listW = Math.min(150, w / 2 - 4);
        java.util.List<com.minpaku.game.MapPlan> all = com.minpaku.game.Plans.ALL;
        int rowH = Math.max(14, Math.min(20, h / all.size()));
        for (int i = 0; i < all.size(); i++) {
            com.minpaku.game.MapPlan p = all.get(i);
            int ry = y + i * rowH;
            boolean on = sel == i + 1;
            boolean hov = in(mouseX, mouseY, x, ry, listW, rowH - 2);
            g.fill(x, ry, x + listW, ry + rowH - 2, on ? 0xFFC8372D : hov ? 0x40C8372D : 0x20000000);
            String size = p.w + "×" + p.d + (p.twoFloors() ? "・2階" : "");
            Ui.sans(g, (i + 1) + ". " + p.name, x + 4, ry + 2, on ? 0xFFFFFFFF : Ui.INK, 0.85f, -1);
            Ui.sans(g, p.players + "　" + size, x + listW - 4, ry + 3, on ? 0xFFFFE9B8 : Ui.INK_SOFT, 0.65f, 1);
            if (canEdit()) {
                final int value = i + 1;
                hits.add(new Hit(x, ry, listW, rowH - 2, () -> {
                    ClientPlayNetworking.send(new Packets.SetSetting("map", value));
                    ClientGame.play(SoundEvents.BOOK_PAGE_TURN, 1.3f);
                }));
            }
        }
        // 間取り図
        com.minpaku.game.MapPlan p = com.minpaku.game.Plans.byIndex(sel);
        int mx = x + listW + 10;
        int mw = w - listW - 10;
        float sc = Math.min(mw / (float) (p.w + 1), (h - 12) / (float) (p.d + 1));
        int ox = mx + (int) ((mw - (p.w + 1) * sc) / 2);
        int oy = y + 10;
        g.fill(ox, oy, ox + (int) ((p.w + 1) * sc), oy + (int) ((p.d + 1) * sc), 0xFF3B2A1C);
        for (com.minpaku.game.MapPlan.Room r : p.rooms) {
            if (r.floor() != 0) {
                continue;
            }
            int color = r.open() ? 0xFF7DAF5A : r.name().contains("廊下") || r.name().contains("回廊") ? 0xFFD9C29A : 0xFFF1E4C8;
            g.fill(ox + (int) (r.x1() * sc), oy + (int) (r.z1() * sc), ox + (int) ((r.x2() + 1) * sc), oy + (int) ((r.z2() + 1) * sc),
                    color);
        }
        // 玄関
        g.fill(ox + (int) (p.entranceX * sc), oy + (int) (p.d * sc), ox + (int) ((p.entranceX + 2) * sc), oy + (int) ((p.d + 1) * sc),
                0xFFC8372D);
        java.util.Set<String> names = new java.util.LinkedHashSet<>();
        for (com.minpaku.game.MapPlan.Room r : p.rooms) {
            names.add(r.name());
        }
        Ui.sans(g, "1階の間取り" + (p.twoFloors() ? "（2階あり）" : "") + "　部屋 " + names.size() + "　赤＝玄関・緑＝庭", mx, y,
                Ui.INK_SOFT, 0.65f, -1);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
