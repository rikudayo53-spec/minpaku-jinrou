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
        pw = Math.min(width - 16, 520);
        ph = Math.min(height - 16, 330);
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
        hoverRole = null;
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
                    {"ベント", String.valueOf(s.counts().getOrDefault(MarkerType.VENT.ordinal(), 0) + s.counts().getOrDefault(MarkerType.VENT_B.ordinal(), 0) + s.counts().getOrDefault(MarkerType.VENT_C.ordinal(), 0))}, {"配電盤", cnt(s, MarkerType.BREAKER)},
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
        int ng = GameSettings.GROUPS.length;
        int tabW = (pw - 24 - (ng - 1) * 2) / ng;
        for (int i = 0; i < ng; i++) {
            String name = GameSettings.GROUPS[i];
            int w = tabW;
            boolean on = i == tab;
            Ui.sprite(g, on ? "tab_on" : "tab_off", tx, ty + (on ? 0 : 2), w, on ? 16 : 14);
            Ui.fitSans(g, name, tx + w / 2f, ty + 4, w - 6, on ? Ui.INK : 0xFF5A4330, 0.9f, 0);
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

        // 選んでいるタブの設定（はみ出すときはスクロール）
        String group = GameSettings.GROUPS[Math.max(0, Math.min(tab, GameSettings.GROUPS.length - 1))];
        int scroll = Math.max(0, Math.min(SCROLL[tab], maxScroll));
        SCROLL[tab] = scroll;
        contentX = lx;
        contentY = ly;
        contentW = lw;
        contentH = lh;
        clipTop = ly + 2;
        clipBottom = ly + lh - 2;
        scrollY = scroll;
        contentMode = true;
        int my = in(mouseX, mouseY, lx, ly, lw, lh) ? mouseY + scroll : -9999;
        g.enableScissor(lx + 2, ly + 2, lx + lw - 2, ly + lh - 2);
        g.pose().pushPose();
        g.pose().translate(0, -scroll, 0);
        int ry = ly + 6;
        int end;
        if (group.equals("役職")) {
            ry = roleTab(g, lx + 8, ry, lw - 16, mouseX, my);
        } else if (!group.equals("マップ")) {
            for (GameSettings.Def d : GameSettings.defs()) {
                if (!d.group().equals(group)) {
                    continue;
                }
                row(g, d, lx + 8, ry, lw - 16, mouseX, my);
                ry += 20;
            }
        }
        end = ry;
        // タブごとの補足
        if (group.equals("フィニッシャー")) {
            end = finisherTab(g, lx + 8, ry, lw - 16, mouseX, my);
        }
        if (group.equals("マップ") && s != null) {
            end = mapTab(g, s, lx + 8, ry, lw - 16, Math.max(110, ly + lh - ry - 8), mouseX, my);
        }
        if (group.equals("役職") && s != null) {
            int special = 0;
            for (Role r : Role.special()) {
                special += s.settings().roleCount(r);
            }
            int total = s.settings().flag("bots_enabled") ? Math.max(1, s.settings().get("bot_fill")) : 1;
            Ui.fitSans(g, "特別な役職 " + special + " 人（残りは村人）　・　ボット込みで最大 " + total + " 人", lx + 8, ry + 2,
                    lw - 16, Ui.INK_SOFT, 0.75f, -1);
            end = ry + 14;
        }
        if (group.equals("ボット") && s != null && s.settings().flag("bots_enabled")) {
            String[] names = {"たろう", "はなこ", "けんた", "さくら", "ゆうき", "みお", "そうた", "あおい", "りく", "ひなた", "かいと"};
            Ui.fitSans(g, "人が足りないぶんを、ボットがうめます（ソロでも遊べる）", lx + 8, ry + 2, lw - 16, Ui.INK_SOFT, 0.75f, -1);
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
            end = ry + 48;
        }
        g.pose().popPose();
        g.disableScissor();
        contentMode = false;
        maxScroll = Math.max(0, end + 6 - (ly + lh));
        // スクロールバー
        if (maxScroll > 0) {
            int barH = Math.max(16, (int) ((lh - 6) * (lh / (float) (lh + maxScroll))));
            int barY = ly + 3 + (int) ((lh - 6 - barH) * (scroll / (float) maxScroll));
            g.fill(lx + lw - 6, ly + 3, lx + lw - 3, ly + lh - 3, 0x30000000);
            g.fill(lx + lw - 6, barY, lx + lw - 3, barY + barH, 0xC08D6E4A);
            if (scroll < maxScroll) {
                Ui.sans(g, "▼ ホイールでスクロール", lx + lw - 4, ly + lh + 2, 0xFF8D7A62, 0.6f, 1);
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

        // 役職の説明（カードにマウスを乗せたとき）
        if (hoverRole != null) {
            String text = hoverRole.name + "：" + hoverRole.description;
            int tw = Math.min(pw - 40, (int) (Ui.width(Ui.sans(text)) * 0.8f) + 14);
            int tx2 = Math.max(px + 8, Math.min(mouseX + 8, px + pw - tw - 8));
            int ty2 = mouseY + 12;
            g.pose().pushPose();
            g.pose().translate(0, 0, 300);
            g.fill(tx2, ty2, tx2 + tw, ty2 + 16, 0xF0201510);
            g.fill(tx2, ty2, tx2 + 2, ty2 + 16, 0xFF000000 | hoverRole.color);
            Ui.fitSans(g, text, tx2 + 7, ty2 + 4, tw - 10, 0xFFF4ECD8, 0.8f, -1);
            g.pose().popPose();
        }
    }

    private void row(GuiGraphics g, GameSettings.Def d, int x, int y, int w, int mouseX, int mouseY) {
        int ctrlW = d.kind() == GameSettings.Kind.TOGGLE ? 70 : d.kind() == GameSettings.Kind.CHOICE ? Math.min(w / 2, 220) : 96;
        Ui.fitSans(g, d.label(), x + 2, y + 4, w - ctrlW - 8, Ui.INK, 0.9f, -1);
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
                hit(sx, y, 30, 17, () -> change(d, on ? 0 : 1));
            }
            return;
        }
        int bx = x + w - ctrlW + 2;
        int plusX = x + w - 18;
        boolean hm = in(mouseX, mouseY, bx, y + 1, 16, 15);
        boolean hp = in(mouseX, mouseY, plusX, y + 1, 16, 15);
        Ui.button(g, bx, y + 1, 16, 15, "−", false, hm && edit, edit && v > d.min(), 1f);
        Ui.button(g, plusX, y + 1, 16, 15, "＋", false, hp && edit, edit && v < d.max(), 1f);
        float mid = (bx + 16 + plusX) / 2f;
        Ui.fitSans(g, d.format(v), mid, y + 3.5f, plusX - bx - 22, Ui.INK, d.kind() == GameSettings.Kind.CHOICE ? 0.9f : 1.1f, 0);
        if (edit) {
            hit(bx, y + 1, 16, 15, () -> change(d, v - d.step()));
            hit(plusX, y + 1, 16, 15, () -> change(d, v + d.step()));
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
        if (maxScroll > 0 && in(mx, my, contentX, contentY, contentW, contentH)) {
            SCROLL[tab] = Math.max(0, Math.min(maxScroll, SCROLL[tab] - (int) Math.signum(dy) * 18));
            return true;
        }
        int n = GameSettings.GROUPS.length;
        tab = Math.floorMod(tab - (int) Math.signum(dy), n);
        return true;
    }

    /** 役職のタブ：陣営ごとにカードを3列で並べる。カードの −／＋ で人数を変える。下の端の y を返す */
    private int roleTab(GuiGraphics g, int x, int y, int w, int mouseX, int mouseY) {
        Role.Team[] teams = {Role.Team.WOLF, Role.Team.VILLAGE, Role.Team.FOX};
        String[] titles = {"人狼陣営", "村人陣営", "第三陣営"};
        int cols = w >= 360 ? 4 : 3;
        int gap = 4;
        int cw = (w - gap * (cols - 1)) / cols;
        int ch = 18;
        boolean edit = canEdit();
        for (int k = 0; k < teams.length; k++) {
            List<Role> list = new ArrayList<>();
            for (Role r : Role.special()) {
                boolean third = r.team == Role.Team.FOX || r.team == Role.Team.TERU || r.team == Role.Team.GOD;
                if ((teams[k] == Role.Team.FOX && third) || r.team == teams[k] && !third) {
                    list.add(r);
                }
            }
            if (list.isEmpty()) {
                continue;
            }
            int tc = k == 0 ? 0xFFC8372D : k == 1 ? 0xFF3F8F3A : 0xFFB8860B;
            g.fill(x, y + 4, x + 3, y + 11, tc);
            Ui.sans(g, titles[k], x + 6, y + 3, tc, 0.75f, -1);
            y += 13;
            for (int i = 0; i < list.size(); i++) {
                Role r = list.get(i);
                GameSettings.Def d = GameSettings.def("role." + r.id);
                if (d == null) {
                    continue;
                }
                int cx = x + (i % cols) * (cw + gap);
                int cy = y + (i / cols) * (ch + 3);
                int v = value(d);
                g.fill(cx, cy, cx + cw, cy + ch, v > 0 ? 0x30000000 | (r.color & 0xFFFFFF) : 0x18000000);
                g.fill(cx, cy, cx + 2, cy + ch, 0xFF000000 | r.color);
                int bx = cx + cw - 44;
                Ui.fitSans(g, r.name, cx + 6, cy + 5, bx - cx - 8, v > 0 ? Ui.INK : Ui.INK_SOFT, 0.85f, -1);
                boolean hm = in(mouseX, mouseY, bx, cy + 2, 13, 14);
                boolean hp = in(mouseX, mouseY, bx + 30, cy + 2, 13, 14);
                Ui.button(g, bx, cy + 2, 13, 14, "−", false, hm && edit, edit && v > d.min(), 0.85f);
                Ui.button(g, bx + 30, cy + 2, 13, 14, "＋", false, hp && edit, edit && v < d.max(), 0.85f);
                Ui.sans(g, String.valueOf(v), bx + 21.5f, cy + 4.5f, v > 0 ? Ui.VERMILION : Ui.INK_SOFT, 0.9f, 0);
                if (edit) {
                    hit(bx, cy + 2, 13, 14, () -> change(d, v - d.step()));
                    hit(bx + 30, cy + 2, 13, 14, () -> change(d, v + d.step()));
                }
                // マウスを乗せると説明
                if (in(mouseX, mouseY, cx, cy, bx - cx, ch)) {
                    hoverRole = r;
                }
            }
            y += ((list.size() + cols - 1) / cols) * (ch + 3) + 2;
        }
        return y;
    }

    private Role hoverRole;

    // ---- 中身のスクロール（タブごと）
    private static final int[] SCROLL = new int[16];
    private int maxScroll;
    private boolean contentMode;
    private int clipTop;
    private int clipBottom;
    private int scrollY;
    private int contentX;
    private int contentY;
    private int contentW;
    private int contentH;

    /** 中身の中のクリックできる場所（スクロールのぶんずらし、見えている所だけ） */
    private void hit(int x, int y, int w, int h, Runnable action) {
        if (!contentMode) {
            hits.add(new Hit(x, y, w, h, action));
            return;
        }
        int y0 = y - scrollY;
        int y1 = y0 + h;
        if (y1 <= clipTop || y0 >= clipBottom) {
            return;
        }
        int a = Math.max(y0, clipTop);
        int b = Math.min(y1, clipBottom);
        hits.add(new Hit(x, a, w, b - a, action));
    }

    /** フィニッシャーのタブ：自分の共通フィニッシャーを選ぶ・全部を見られる（だれでも使える） */
    private int finisherTab(GuiGraphics g, int x, int y, int w, int mouseX, int mouseY) {
        Ui.fitSans(g, "自分のフィニッシャー（設定が「各自が選んだもの」のときに使われます）", x, y, w, Ui.INK_SOFT, 0.75f, -1);
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
            Ui.fitSans(g, title, bx + bw / 2f, by + 5, bw - 6, on ? 0xFFFFFFFF : Ui.INK, 0.72f, 0);
            Ui.sans(g, on ? "選択中" : "クリックで選ぶ", bx + bw / 2f, by + 18, on ? 0xFFFFE9B8 : Ui.INK_SOFT, 0.65f, 0);
            hit(bx, by, bw, 26, () -> {
                com.minpaku.client.ClientGame.myFinisher = id;
                ClientPlayNetworking.send(new Packets.SetFinisher(id));
                ClientGame.play(SoundEvents.BOOK_PAGE_TURN, 1.3f);
            });
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
            Ui.fitSans(g, list[i][2], bx + 4, by + 4, cw - 48, Ui.INK, 0.75f, -1);
            previewButton(g, bx + cw - 40, by, list[i][0], Integer.parseInt(list[i][1]), mouseX, mouseY);
        }
        return ry + 12 + ((list.length + col - 1) / col) * 22;
    }

    private void previewButton(GuiGraphics g, int bx, int by, String id, int variant, int mouseX, int mouseY) {
        boolean hov = in(mouseX, mouseY, bx, by, 36, 14);
        Ui.sprite(g, "btn_dark", bx, by, 36, 14);
        Ui.sans(g, "見る", bx + 18, by + 3, hov ? 0xFFFFFFFF : Ui.CREAM, 0.7f, 0);
        hit(bx, by, 36, 14, () -> {
            onClose();
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null) {
                // 確認用：自分が犯人役
                com.minpaku.client.ClientGame.startVictimFx(id.equals("neko") ? "たま" : mc.player.getGameProfile().getName(),
                        mc.player.getUUID(), id, variant);
            }
        });
    }

    /** マップのタブ：6種類（クリックで選ぶ）と、選んだマップの間取り図。下の端の y を返す */
    private int mapTab(GuiGraphics g, Packets.Settings s, int x, int y, int w, int h, int mouseX, int mouseY) {
        int sel = s.settings().get("map");
        java.util.List<com.minpaku.game.MapPlan> all = com.minpaku.game.Plans.ALL;
        int listW = Math.min(240, w * 58 / 100);
        int cw = (listW - 4) / 2;
        int rowH = 26;
        Ui.fitSans(g, "建てるマップを選ぶ（右上のボタンで建てる）", x, y, listW, Ui.INK_SOFT, 0.7f, -1);
        int ly0 = y + 11;
        for (int i = 0; i < all.size(); i++) {
            com.minpaku.game.MapPlan p = all.get(i);
            int cx = x + (i % 2) * (cw + 4);
            int cy = ly0 + (i / 2) * (rowH + 3);
            boolean on = sel == i + 1;
            boolean hov = in(mouseX, mouseY, cx, cy, cw, rowH);
            g.fill(cx, cy, cx + cw, cy + rowH, on ? 0xFFC8372D : hov ? 0x40C8372D : 0x20000000);
            String size = p.w + "×" + p.d + (p.twoFloors() ? "・2階" : "");
            Ui.fitSans(g, (i + 1) + ". " + p.name, cx + 5, cy + 4, cw - 10, on ? 0xFFFFFFFF : Ui.INK, 0.9f, -1);
            Ui.fitSans(g, p.players + "　" + size, cx + 5, cy + 15, cw - 10, on ? 0xFFFFE9B8 : Ui.INK_SOFT, 0.65f, -1);
            if (canEdit()) {
                final int value = i + 1;
                hit(cx, cy, cw, rowH, () -> {
                    ClientPlayNetworking.send(new Packets.SetSetting("map", value));
                    ClientGame.play(SoundEvents.BOOK_PAGE_TURN, 1.3f);
                });
            }
        }
        int listEnd = ly0 + ((all.size() + 1) / 2) * (rowH + 3);
        // 祠の悪神（祠がある大きいマップで使う）：起きない／ときどき／かならず
        for (GameSettings.Def d : GameSettings.defs()) {
            if (d.id().equals("evil_god")) {
                row(g, d, x, listEnd + 4, listW, mouseX, mouseY);
                Ui.fitSans(g, "※ 祠があるマップ（大旅館）だけ", x + 2, listEnd + 24, listW, Ui.INK_SOFT, 0.65f, -1);
                listEnd += 36;
            }
        }
        // 間取り図
        com.minpaku.game.MapPlan p = com.minpaku.game.Plans.byIndex(sel);
        int mx = x + listW + 10;
        int mw = w - listW - 14;
        int mh = Math.max(80, Math.min(h - 14, 150));
        float sc = Math.min(mw / (float) (p.w + 1), mh / (float) (p.d + 1));
        int ox = mx + (int) ((mw - (p.w + 1) * sc) / 2);
        int oy = y + 12;
        g.fill(ox, oy, ox + (int) ((p.w + 1) * sc), oy + (int) ((p.d + 1) * sc), 0xFF3B2A1C);
        for (com.minpaku.game.MapPlan.Room r : p.rooms) {
            if (r.floor() != 0) {
                continue;
            }
            int color = r.open() ? 0xFF7DAF5A : r.name().contains("廊下") || r.name().contains("回廊") ? 0xFFD9C29A : 0xFFF1E4C8;
            g.fill(ox + (int) (r.x1() * sc), oy + (int) (r.z1() * sc), ox + (int) ((r.x2() + 1) * sc), oy + (int) ((r.z2() + 1) * sc),
                    color);
        }
        g.fill(ox + (int) (p.entranceX * sc), oy + (int) (p.d * sc), ox + (int) ((p.entranceX + 2) * sc), oy + (int) ((p.d + 1) * sc),
                0xFFC8372D);
        java.util.Set<String> names = new java.util.LinkedHashSet<>();
        for (com.minpaku.game.MapPlan.Room r : p.rooms) {
            names.add(r.name());
        }
        Ui.fitSans(g, p.name + "の1階" + (p.twoFloors() ? "（2階あり）" : "") + "・部屋 " + names.size() + "　赤＝玄関　緑＝庭", mx, y,
                mw, Ui.INK_SOFT, 0.65f, -1);
        return Math.max(listEnd, oy + (int) ((p.d + 1) * sc) + 4);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
