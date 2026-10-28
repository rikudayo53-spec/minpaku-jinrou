package com.minpaku.client.screen;

import com.minpaku.client.ClientGame;
import com.minpaku.client.GameHud;
import com.minpaku.game.Role;
import com.minpaku.network.Packets;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.PlayerFaceRenderer;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;

/** ゲーム終了の結果画面（全員の役職と勝敗） */
public class ResultScreen extends Screen {
    private final Packets.Result result;
    private final Role.Team winner;
    private final boolean iWon;
    private int age;

    public ResultScreen(Packets.Result result) {
        super(Component.literal("結果"));
        this.result = result;
        this.winner = Role.Team.values()[Mth.clamp(result.winner(), 0, Role.Team.values().length - 1)];
        Role mine = ClientGame.role();
        this.iWon = mine != null && mine.team == winner;
    }

    @Override
    protected void init() {
        addRenderableWidget(Button.builder(Component.literal("閉じる"), b -> onClose())
                .bounds(width / 2 - 40, height - 28, 80, 20).build());
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
        g.fill(0, 0, width, height, 0xE8000000);
        int c = winner.color;
        g.fillGradient(0, 0, width, 90, 0x80000000 | (c & 0xFFFFFF), 0x00000000);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float pt) {
        super.render(g, mouseX, mouseY, pt);
        float t = age + pt;
        int cx = width / 2;
        float pop = t < 10 ? 0.5f + t / 10f * 0.5f : 1f;
        GameHud.drawBig(g, font, winner.label + "の勝利！", cx, 18, 4f * pop, 0xFF000000 | winner.color);
        g.drawCenteredString(font, result.reason(), cx, 58, 0xFFFFFFFF);
        if (ClientGame.role() != null) {
            GameHud.drawBig(g, font, iWon ? "あなたの勝ち" : "あなたの負け", cx, 72, 1.3f, iWon ? 0xFFFFEE58 : 0xFF90A4AE);
        }

        int n = result.players().size();
        int cols = width > 420 ? 3 : 2;
        int cw = 130;
        int total = cols * cw + (cols - 1) * 8;
        int x0 = cx - total / 2;
        for (int i = 0; i < n; i++) {
            if (t < 12 + i * 3) {
                break;
            }
            Packets.Participant p = result.players().get(i);
            Role role = Role.byId(p.role());
            int x = x0 + (i % cols) * (cw + 8);
            int y = 96 + (i / cols) * 30;
            boolean win = role.team == winner;
            g.fill(x, y, x + cw, y + 26, win ? 0xAA1B3A1B : 0xAA2A2A2A);
            g.renderOutline(x, y, cw, 26, win ? 0xFFFFD54F : 0xFF555555);
            PlayerFaceRenderer.draw(g, ClientGame.skin(p.id()), x + 3, y + 3, 20);
            g.drawString(font, p.name(), x + 27, y + 4, p.alive() ? 0xFFFFFFFF : 0xFF999999, false);
            g.drawString(font, role.name + (p.alive() ? "" : "（死亡）"), x + 27, y + 15, 0xFF000000 | role.color, false);
        }
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
