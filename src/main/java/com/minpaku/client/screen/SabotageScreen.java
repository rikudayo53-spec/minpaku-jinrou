package com.minpaku.client.screen;

import com.minpaku.client.ClientGame;
import com.minpaku.client.Ui;
import com.minpaku.network.Packets;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** 人狼の妨害メニュー（お札） */
public class SabotageScreen extends Screen {
    private int px, py, pw = 260, ph = 150;

    public SabotageScreen() {
        super(Component.literal("妨害"));
    }

    @Override
    protected void init() {
        px = (width - pw) / 2;
        py = (height - ph) / 2;
    }

    private boolean ready() {
        Packets.State s = ClientGame.state;
        return !s.blackout() && s.gasTicks() < 0 && s.sabotageCooldown() <= 0;
    }

    private static boolean in(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float pt) {
        renderBackground(g, mouseX, mouseY, pt);
        Ui.sprite(g, "panel", px, py, pw, ph);
        Ui.stamp(g, "妨害", px + 22, py + 22, 26, Ui.VERMILION, -8, true);
        Ui.serif(g, "妨害のお札", px + 42, py + 12, Ui.INK, 1.5f, -1);
        Packets.State s = ClientGame.state;
        String info = s.blackout() || s.gasTicks() >= 0 ? "いま妨害が起きている"
                : s.sabotageCooldown() > 0 ? "あと " + (s.sabotageCooldown() / 20 + 1) + " 秒で使える" : "使える！";
        Ui.sans(g, info, px + 42, py + 30, ready() ? 0xFF2E7D32 : Ui.INK_SOFT, 0.85f, -1);
        boolean r = ready();
        int bw = (pw - 36) / 2;
        Ui.button(g, px + 12, py + 50, bw, 30, "停電させる", true, r && in(mouseX, mouseY, px + 12, py + 50, bw, 30), r, 1.1f);
        Ui.button(g, px + 24 + bw, py + 50, bw, 30, "ガス漏れ", true, r && in(mouseX, mouseY, px + 24 + bw, py + 50, bw, 30), r, 1.1f);
        Ui.sans(g, "村人の視界が暗くなる", px + 12 + bw / 2f, py + 86, Ui.INK_SOFT, 0.7f, 0);
        Ui.sans(g, "時間内に止められないと人狼の勝ち", px + 24 + bw * 1.5f, py + 86, Ui.INK_SOFT, 0.7f, 0);
        Ui.button(g, px + pw / 2 - 40, py + ph - 30, 80, 20, "閉じる", false, in(mouseX, mouseY, px + pw / 2 - 40, py + ph - 30, 80, 20), true, 1f);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        int bw = (pw - 36) / 2;
        if (ready() && in(mx, my, px + 12, py + 50, bw, 30)) {
            ClientPlayNetworking.send(new Packets.Sabotage(0));
            onClose();
            return true;
        }
        if (ready() && in(mx, my, px + 24 + bw, py + 50, bw, 30)) {
            ClientPlayNetworking.send(new Packets.Sabotage(1));
            onClose();
            return true;
        }
        if (in(mx, my, px + pw / 2 - 40, py + ph - 30, 80, 20)) {
            onClose();
            return true;
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
