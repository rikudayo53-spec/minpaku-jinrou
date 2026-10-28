package com.minpaku.client.screen;

import com.minpaku.client.ClientGame;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;

import java.util.Random;

/** 配電盤：ブレーカーを全部「入」にする（停電を直す） */
public class BreakerScreen extends TaskGameScreen {
    private final boolean[] on = new boolean[6];

    public BreakerScreen(BlockPos pos) {
        super("配電盤 ― ブレーカーを全部上げよう", pos);
        Random r = new Random();
        boolean anyOff = false;
        for (int i = 0; i < on.length; i++) {
            on[i] = r.nextBoolean();
            anyOff |= !on[i];
        }
        if (!anyOff) {
            on[r.nextInt(on.length)] = false;
        }
    }

    private int swX(int i) {
        return left() + 18 + i * 36;
    }

    @Override
    protected void renderGame(GuiGraphics g, int mouseX, int mouseY, float pt) {
        int y = top() + 30;
        for (int i = 0; i < on.length; i++) {
            int x = swX(i);
            g.fill(x, y, x + 26, y + 80, 0xFF424242);
            g.renderOutline(x, y, 26, 80, 0xFF9E9E9E);
            int ky = on[i] ? y + 6 : y + 44;
            g.fill(x + 4, ky, x + 22, ky + 30, on[i] ? 0xFF66BB6A : 0xFFEF5350);
            com.minpaku.client.Ui.sansCentered(g, on[i] ? "入" : "切", x + 13, y + 86, on[i] ? 0xFF66BB6A : 0xFFEF5350);
        }
        com.minpaku.client.Ui.sansCentered(g, "クリックで切りかえ", width / 2, top() + panelH - 12, 0xFFAAAAAA);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (finished()) {
            return true;
        }
        int y = top() + 30;
        for (int i = 0; i < on.length; i++) {
            if (inside(mx, my, swX(i), y, 26, 80)) {
                on[i] = !on[i];
                ClientGame.play(SoundEvents.LEVER_CLICK, on[i] ? 1.2f : 0.8f);
                boolean all = true;
                for (boolean b : on) {
                    all &= b;
                }
                if (all) {
                    complete();
                }
                return true;
            }
        }
        return super.mouseClicked(mx, my, button);
    }
}
