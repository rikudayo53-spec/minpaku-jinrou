package com.minpaku.client.screen;

import com.minpaku.client.ClientGame;
import com.minpaku.client.Ui;
import com.minpaku.game.Role;
import com.minpaku.network.Packets;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;

/** ゲーム終了の結果画面（全員の役職をハンコで見せる） */
public class ResultScreen extends Screen {
    private final Packets.Result result;
    private final Role.Team winner;
    private final boolean iWon;
    private final boolean played;
    private int age;
    private int bx, by, bw = 90, bh = 20;

    public ResultScreen(Packets.Result result) {
        super(Component.literal("結果"));
        this.result = result;
        this.winner = Role.Team.values()[Mth.clamp(result.winner(), 0, Role.Team.values().length - 1)];
        Role mine = ClientGame.role();
        this.played = mine != null;
        this.iWon = mine != null && mine.team == winner;
    }

    @Override
    public void tick() {
        age++;
        if (age == 1) {
            if (iWon) {
                ClientGame.play(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 1f);
            } else {
                ClientGame.play(SoundEvents.WITHER_DEATH, 1.2f);
            }
        }
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float pt) {
        int c = switch (winner) {
            case WOLF -> 0x780F0A;
            case FOX -> 0x6B4A05;
            case TERU -> 0x1D4A6B;
            default -> 0x0F4A1A;
        };
        g.fillGradient(0, 0, width, height, 0xE0000000 | c, 0xF0000000);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float pt) {
        renderBackground(g, mouseX, mouseY, pt);
        float t = age + pt;
        int cx = width / 2;
        float pop = t < 10 ? 0.6f + t / 10f * 0.4f : 1f;
        Ui.text(g, Ui.sans(result.reason()), cx, 14, 0xFFFFCF9A, 1f, 0, false);
        Ui.text(g, Ui.serif(winner.label + "の勝利"), cx, 28, 0xFFFFFFFF, 3.4f * pop, 0, true);
        if (played) {
            Ui.chip(g, iWon ? "あなたの勝ち！" : "あなたの負け…", cx, 66, iWon ? 0xFFFFD54F : 0xFF6E6E6E, iWon ? Ui.INK : 0xFFFFFFFF);
        }

        int n = result.players().size();
        int cols = width > 460 ? 4 : 3;
        int pw = Math.min(width - 20, cols * 104 + 24);
        int cw = (pw - 24 - (cols - 1) * 6) / cols;
        int rows = (n + cols - 1) / cols;
        int ph = 24 + rows * 34 + 30;
        int px = cx - pw / 2;
        int py = Math.max(84, (height - ph) / 2 + 30);
        Ui.sprite(g, "panel", px, py, pw, ph);
        for (int i = 0; i < n; i++) {
            if (t < 12 + i * 3) {
                break;
            }
            Packets.Participant p = result.players().get(i);
            Role role = Role.byId(p.role());
            int x = px + 12 + (i % cols) * (cw + 6);
            int y = py + 12 + (i / cols) * 34;
            boolean win = role.team == winner;
            Ui.sprite(g, win ? "card_win" : p.alive() ? "card" : "card_dead", x, y, cw, 30);
            Ui.face(g, p.id(), x + 5, y + 5, 20);
            Ui.sans(g, p.name(), x + 29, y + 5, Ui.INK, 0.95f, -1);
            int kills = i < result.kills().size() ? result.kills().get(i) : 0;
            Ui.sans(g, (p.alive() ? "生存" : "死亡") + (kills > 0 ? "・キル" + kills : ""), x + 29, y + 17, 0xFF8D7A62, 0.7f, -1);
            float st = Mth.clamp((t - 12 - i * 3) / 4f, 0f, 1f);
            g.pose().pushPose();
            g.pose().translate(x + cw - 17, y + 15, 0);
            float sc = 1.5f - 0.5f * st;
            g.pose().scale(sc, sc, 1f);
            Ui.stamp(g, role.name, 0, 0, 22, Ui.alpha(0xFF000000 | role.color, st), -8, false);
            g.pose().popPose();
        }
        String stats = "みんなのタスク " + result.taskPercent() + "%　・　会議 " + result.meetings() + "回";
        Ui.sans(g, stats, px + 14, py + ph - 22, Ui.INK_SOFT, 0.85f, -1);
        bx = px + pw - bw - 12;
        by = py + ph - bh - 8;
        Ui.button(g, bx, by, bw, bh, "ロビーへ", false, inside(mouseX, mouseY), true, 1f);
    }

    private boolean inside(double mx, double my) {
        return mx >= bx && mx < bx + bw && my >= by && my < by + bh;
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (inside(mx, my)) {
            onClose();
            return true;
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(null);
    }
}
