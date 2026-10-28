package com.minpaku.client.screen;

import com.minpaku.client.ClientGame;
import com.minpaku.game.GameSettings;
import com.minpaku.game.MarkerType;
import com.minpaku.network.Packets;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

/** 管理人の鍵の画面：ゲームの設定をすべてここで変える */
public class SettingsScreen extends Screen {
    private static final int ROW = 22;
    private int scroll;

    public SettingsScreen() {
        super(Component.literal("民泊人狼の設定"));
    }

    private int listTop() {
        return 58;
    }

    private int listBottom() {
        return height - 40;
    }

    private int visibleRows() {
        return Math.max(1, (listBottom() - listTop()) / ROW);
    }

    @Override
    protected void init() {
        Packets.Settings s = ClientGame.settings;
        boolean edit = s != null && s.canEdit();
        List<GameSettings.Def> defs = GameSettings.defs();
        int maxScroll = Math.max(0, defs.size() - visibleRows());
        scroll = Math.max(0, Math.min(scroll, maxScroll));

        int cx = width / 2;
        for (int i = 0; i < visibleRows() && i + scroll < defs.size(); i++) {
            GameSettings.Def d = defs.get(i + scroll);
            int y = listTop() + i * ROW;
            Button minus = Button.builder(Component.literal("−"), b -> change(d, -d.step()))
                    .bounds(cx + 40, y, 20, 18).build();
            Button plus = Button.builder(Component.literal("＋"), b -> change(d, d.step()))
                    .bounds(cx + 120, y, 20, 18).build();
            minus.active = edit;
            plus.active = edit;
            if (d.kind() == GameSettings.Kind.TOGGLE) {
                Button toggle = Button.builder(Component.literal(d.format(value(d))), b -> change(d, value(d) != 0 ? -1 : 1))
                        .bounds(cx + 40, y, 100, 18).build();
                toggle.active = edit;
                addRenderableWidget(toggle);
            } else {
                addRenderableWidget(minus);
                addRenderableWidget(plus);
            }
        }

        boolean running = s != null && s.running();
        Button start = Button.builder(Component.literal("ゲーム開始"), b -> {
            ClientPlayNetworking.send(new Packets.Control(0));
            onClose();
        }).bounds(cx - 155, height - 30, 100, 20).build();
        start.active = edit && !running;
        Button stop = Button.builder(Component.literal("ゲームを中止"), b -> {
            ClientPlayNetworking.send(new Packets.Control(1));
            onClose();
        }).bounds(cx - 50, height - 30, 100, 20).build();
        stop.active = edit && running;
        addRenderableWidget(start);
        addRenderableWidget(stop);
        addRenderableWidget(Button.builder(Component.literal("閉じる"), b -> onClose())
                .bounds(cx + 55, height - 30, 100, 20).build());
    }

    private int value(GameSettings.Def d) {
        Packets.Settings s = ClientGame.settings;
        return s == null ? d.def() : s.settings().get(d.id());
    }

    private void change(GameSettings.Def d, int delta) {
        int v = Math.max(d.min(), Math.min(d.max(), value(d) + delta));
        ClientPlayNetworking.send(new Packets.SetSetting(d.id(), v));
    }

    /** サーバーから新しい設定が届いた */
    public void refresh() {
        rebuildWidgets();
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double dx, double dy) {
        int old = scroll;
        scroll -= (int) Math.signum(dy);
        int maxScroll = Math.max(0, GameSettings.defs().size() - visibleRows());
        scroll = Math.max(0, Math.min(scroll, maxScroll));
        if (scroll != old) {
            rebuildWidgets();
        }
        return true;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float pt) {
        super.render(g, mouseX, mouseY, pt);
        int cx = width / 2;
        g.drawCenteredString(font, "民泊人狼 ― 管理人の設定", cx, 8, 0xFFFFE0B2);
        Packets.Settings s = ClientGame.settings;
        if (s == null) {
            g.drawCenteredString(font, "読み込み中…", cx, 30, 0xFFAAAAAA);
            return;
        }
        // マップに置かれているもの
        String map = "スタート " + count(s, MarkerType.SPAWN) + "  呼び鈴 " + count(s, MarkerType.BELL)
                + "  ベント " + count(s, MarkerType.VENT) + "  配電盤 " + count(s, MarkerType.BREAKER)
                + "  元栓 " + count(s, MarkerType.GAS_VALVE) + "  ボイラー " + count(s, MarkerType.BOILER)
                + "  タスク " + taskCount(s);
        g.drawCenteredString(font, map, cx, 22, 0xFFB0BEC5);
        if (!s.canEdit()) {
            g.drawCenteredString(font, "設定を変えられるのはOP（管理人）だけです", cx, 36, 0xFFFF7777);
        } else if (s.running()) {
            g.drawCenteredString(font, "ゲーム中（変えた設定は次のゲームから）", cx, 36, 0xFFFFD54F);
        }

        List<GameSettings.Def> defs = GameSettings.defs();
        String lastGroup = scroll > 0 ? defs.get(scroll - 1).group() : "";
        for (int i = 0; i < visibleRows() && i + scroll < defs.size(); i++) {
            GameSettings.Def d = defs.get(i + scroll);
            int y = listTop() + i * ROW;
            if (!d.group().equals(lastGroup)) {
                g.drawString(font, "【" + d.group() + "】", cx - 200, y + 5, 0xFFFFCC80, false);
                lastGroup = d.group();
            }
            g.drawString(font, d.label(), cx - 140, y + 5, 0xFFFFFFFF, false);
            if (d.kind() != GameSettings.Kind.TOGGLE) {
                g.drawCenteredString(font, d.format(value(d)), cx + 90, y + 5, 0xFFFFEB3B);
            }
        }
        if (defs.size() > visibleRows()) {
            g.drawCenteredString(font, "ホイールでスクロール", cx, listBottom() + 1, 0xFF777777);
        }
    }

    private static int count(Packets.Settings s, MarkerType t) {
        return s.counts().getOrDefault(t.ordinal(), 0);
    }

    private static int taskCount(Packets.Settings s) {
        int n = 0;
        for (MarkerType t : MarkerType.values()) {
            if (t.isTask()) {
                n += count(s, t);
            }
        }
        return n;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
