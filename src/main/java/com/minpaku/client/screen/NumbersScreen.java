package com.minpaku.client.screen;

import com.minpaku.client.ClientGame;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** 予約台帳を整理する：1〜10 のページ番号を順番に押す */
public class NumbersScreen extends TaskGameScreen {
    private final List<Integer> layout = new ArrayList<>();
    private int next = 1;

    public NumbersScreen(BlockPos pos) {
        super("予約台帳を整理する", pos);
        for (int i = 1; i <= 10; i++) {
            layout.add(i);
        }
        Collections.shuffle(layout);
    }

    private int cellX(int i) {
        return left() + 20 + (i % 5) * 42;
    }

    private int cellY(int i) {
        return top() + 30 + (i / 5) * 50;
    }

    @Override
    protected void renderGame(GuiGraphics g, int mouseX, int mouseY, float pt) {
        for (int i = 0; i < 10; i++) {
            int n = layout.get(i);
            int x = cellX(i);
            int y = cellY(i);
            boolean done = n < next;
            boolean hover = inside(mouseX, mouseY, x, y, 36, 40);
            g.fill(x, y, x + 36, y + 40, done ? 0xFF558B2F : hover ? 0xFFFFF8E1 : 0xFFEFE6D0);
            g.renderOutline(x, y, 36, 40, 0xFF8D6E63);
            g.pose().pushPose();
            g.pose().translate(x + 18, y + 13, 0);
            g.pose().scale(2f, 2f, 1f);
            String s = String.valueOf(n);
            g.drawString(font, s, -font.width(s) / 2, 0, done ? 0xFFFFFFFF : 0xFF3E2723, false);
            g.pose().popPose();
        }
        g.drawCenteredString(font, "台帳のページを 1 から順番に押そう（次: " + Math.min(next, 10) + "）",
                width / 2, top() + panelH - 12, 0xFFAAAAAA);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (finished()) {
            return true;
        }
        for (int i = 0; i < 10; i++) {
            if (inside(mx, my, cellX(i), cellY(i), 36, 40)) {
                int n = layout.get(i);
                if (n == next) {
                    next++;
                    ClientGame.play(SoundEvents.BOOK_PAGE_TURN, 1.0f + n * 0.05f);
                    if (next > 10) {
                        complete();
                    }
                } else if (n >= next) {
                    next = 1;
                    ClientGame.play(SoundEvents.VILLAGER_NO, 1.0f);
                }
                return true;
            }
        }
        return super.mouseClicked(mx, my, button);
    }
}
