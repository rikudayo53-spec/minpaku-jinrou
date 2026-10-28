package com.minpaku.client.screen;

import com.minpaku.client.ClientGame;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** 配線をつなぐ：左の線を押して、同じ色の右の端子を押す */
public class WiresScreen extends TaskGameScreen {
    private static final int[] COLORS = {0xFFE53935, 0xFF1E88E5, 0xFFFDD835, 0xFFD81B60};
    private final List<Integer> leftOrder = new ArrayList<>();
    private final List<Integer> rightOrder = new ArrayList<>();
    private final boolean[] connected = new boolean[4];
    private int holding = -1;

    public WiresScreen(BlockPos pos) {
        super("配線をつなぐ", pos);
        for (int i = 0; i < 4; i++) {
            leftOrder.add(i);
            rightOrder.add(i);
        }
        Collections.shuffle(leftOrder);
        Collections.shuffle(rightOrder);
    }

    private int rowY(int i) {
        return top() + 22 + i * 34;
    }

    @Override
    protected void renderGame(GuiGraphics g, int mouseX, int mouseY, float pt) {
        int lx = left() + 20;
        int rx = left() + panelW - 36;
        for (int i = 0; i < 4; i++) {
            int c = COLORS[leftOrder.get(i)];
            g.fill(left(), rowY(i) + 4, lx + 16, rowY(i) + 12, c);
            g.fill(lx, rowY(i), lx + 16, rowY(i) + 16, 0xFF9E9E9E);
            g.fill(lx + 3, rowY(i) + 3, lx + 13, rowY(i) + 13, c);
            int rc = COLORS[rightOrder.get(i)];
            g.fill(rx + 16, rowY(i) + 4, left() + panelW, rowY(i) + 12, rc);
            g.fill(rx, rowY(i), rx + 16, rowY(i) + 16, 0xFF9E9E9E);
            g.fill(rx + 3, rowY(i) + 3, rx + 13, rowY(i) + 13, rc);
        }
        // つないだ線
        for (int color = 0; color < 4; color++) {
            if (connected[color]) {
                int li = leftOrder.indexOf(color);
                int ri = rightOrder.indexOf(color);
                line(g, lx + 16, rowY(li) + 8, rx, rowY(ri) + 8, COLORS[color]);
            }
        }
        if (holding >= 0) {
            int li = leftOrder.indexOf(holding);
            line(g, lx + 16, rowY(li) + 8, mouseX, mouseY, COLORS[holding]);
        }
        g.drawCenteredString(font, "左の線をクリック → 同じ色の右の端子をクリック", width / 2, top() + panelH - 12, 0xFFAAAAAA);
    }

    /** 太めの線を点で描く */
    private static void line(GuiGraphics g, int x0, int y0, int x1, int y1, int color) {
        int steps = Math.max(Math.abs(x1 - x0), Math.abs(y1 - y0));
        for (int s = 0; s <= steps; s += 1) {
            int x = x0 + (x1 - x0) * s / Math.max(1, steps);
            int y = y0 + (y1 - y0) * s / Math.max(1, steps);
            g.fill(x - 2, y - 2, x + 2, y + 2, color);
        }
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (finished()) {
            return true;
        }
        int lx = left() + 20;
        int rx = left() + panelW - 36;
        for (int i = 0; i < 4; i++) {
            if (inside(mx, my, lx - 4, rowY(i) - 4, 24, 24)) {
                int color = leftOrder.get(i);
                if (!connected[color]) {
                    holding = color;
                    ClientGame.play(SoundEvents.LEVER_CLICK, 1.4f);
                }
                return true;
            }
            if (inside(mx, my, rx - 4, rowY(i) - 4, 24, 24) && holding >= 0) {
                if (rightOrder.get(i) == holding) {
                    connected[holding] = true;
                    ClientGame.play(SoundEvents.COPPER_BULB_TURN_ON, 1.2f);
                    boolean all = true;
                    for (boolean c : connected) {
                        all &= c;
                    }
                    if (all) {
                        complete();
                    }
                } else {
                    ClientGame.play(SoundEvents.VILLAGER_NO, 1.2f);
                }
                holding = -1;
                return true;
            }
        }
        holding = -1;
        return super.mouseClicked(mx, my, button);
    }
}
