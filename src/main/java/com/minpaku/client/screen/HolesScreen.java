package com.minpaku.client.screen;

import com.minpaku.client.ClientGame;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;

import java.util.Random;

/** 障子の穴をふさぐ：穴をクリックして紙を貼る */
public class HolesScreen extends TaskGameScreen {
    private static final int COLS = 4;
    private static final int ROWS = 3;
    private final boolean[] hole = new boolean[COLS * ROWS];
    private final int[] hx = new int[COLS * ROWS];
    private final int[] hy = new int[COLS * ROWS];

    public HolesScreen(BlockPos pos) {
        super("障子の穴をふさぐ", pos);
        Random r = new Random();
        int made = 0;
        while (made < 6) {
            int i = r.nextInt(hole.length);
            if (!hole[i]) {
                hole[i] = true;
                hx[i] = 8 + r.nextInt(30);
                hy[i] = 8 + r.nextInt(22);
                made++;
            }
        }
    }

    private int cellX(int i) {
        return left() + 16 + (i % COLS) * 52;
    }

    private int cellY(int i) {
        return top() + 14 + (i / COLS) * 42;
    }

    @Override
    protected void renderGame(GuiGraphics g, int mouseX, int mouseY, float pt) {
        // 木の枠
        g.fill(left() + 12, top() + 10, left() + 12 + COLS * 52, top() + 14 + ROWS * 42, 0xFF8D6E63);
        for (int i = 0; i < hole.length; i++) {
            int x = cellX(i);
            int y = cellY(i);
            g.fill(x, y, x + 48, y + 38, 0xFFF7F3E8);
            if (hole[i]) {
                boolean hover = inside(mouseX, mouseY, x + hx[i] - 8, y + hy[i] - 8, 16, 16);
                g.fill(x + hx[i] - 6, y + hy[i] - 5, x + hx[i] + 6, y + hy[i] + 5, hover ? 0xFF5D4037 : 0xFF3E2723);
                g.fill(x + hx[i] - 4, y + hy[i] - 7, x + hx[i] + 4, y + hy[i] + 7, hover ? 0xFF5D4037 : 0xFF3E2723);
            }
        }
        int left = 0;
        for (boolean h : hole) {
            if (h) {
                left++;
            }
        }
        g.drawCenteredString(font, "穴をクリックして紙を貼ろう（残り " + left + "）", width / 2, top() + panelH - 12, 0xFFAAAAAA);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (finished()) {
            return true;
        }
        for (int i = 0; i < hole.length; i++) {
            if (hole[i] && inside(mx, my, cellX(i) + hx[i] - 8, cellY(i) + hy[i] - 8, 16, 16)) {
                hole[i] = false;
                ClientGame.play(SoundEvents.BOOK_PAGE_TURN, 1.3f);
                boolean any = false;
                for (boolean h : hole) {
                    any |= h;
                }
                if (!any) {
                    complete();
                }
                return true;
            }
        }
        return super.mouseClicked(mx, my, button);
    }
}
