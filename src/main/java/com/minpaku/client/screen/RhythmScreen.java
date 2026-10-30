package com.minpaku.client.screen;

import com.minpaku.client.ClientGame;
import com.minpaku.client.Ui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * タイミングのミニゲーム。クリックかスペースで叩く。
 *   太鼓：右から流れてくる音符が、左の輪に重なった瞬間に叩く（need 回当てたら完了）
 *   鐘  ：撞木（しゅもく）が大きく振れて、いちばん奥に来た瞬間につく（need 回）
 */
public class RhythmScreen extends TaskGameScreen {
    private final boolean bell;
    private final int need;
    private int hits;
    private int misses;
    private float t;            // 経過（tick、なめらか）
    private long lastNano;
    private final List<Float> notes = new ArrayList<>();   // 太鼓：音符が輪に来る時刻
    private final List<Boolean> rim = new ArrayList<>();   // 太鼓：ふち（カッ）か面（ドン）か
    private float flash = -99;
    private boolean flashGood;
    private String judge = "";

    private static final float SPEED = 3.2f;    // 太鼓：1tick に進むピクセル
    private static final float WINDOW = 3.2f;   // 当たりの幅（tick）

    public RhythmScreen(BlockPos pos, boolean bell, int need) {
        super(bell ? "半鐘を鳴らす" : "太鼓を叩く", pos);
        this.bell = bell;
        this.need = need;
        if (!bell) {
            Random r = new Random();
            float at = 30;
            for (int i = 0; i < 64; i++) {
                notes.add(at);
                rim.add(r.nextInt(4) == 0);
                at += r.nextInt(3) == 0 ? 7 : 14;
            }
        }
    }

    // ------------------------------------------------------------ 鐘：撞木の振れ（-1〜1）。1 がいちばん奥
    private float swing() {
        return (float) Math.sin(t / 14f * Math.PI);
    }

    @Override
    public void tick() {
        super.tick();
    }

    private void advance() {
        long now = System.nanoTime();
        if (lastNano != 0 && !finished()) {
            t += Math.min(0.25f, (now - lastNano) / 1.0e9f) * 20f;
        }
        lastNano = now;
        // 太鼓：通りすぎた音符は「見逃し」
        if (!bell) {
            while (!notes.isEmpty() && notes.get(0) < t - WINDOW) {
                notes.remove(0);
                rim.remove(0);
                misses++;
                show(false, "おしい");
            }
            if (notes.isEmpty()) {
                notes.add(t + 20);
                rim.add(false);
            }
        }
    }

    private void show(boolean good, String text) {
        flash = t;
        flashGood = good;
        judge = text;
    }

    @Override
    protected void renderGame(GuiGraphics g, int mouseX, int mouseY, float pt) {
        advance();
        int x0 = left();
        int y0 = top();
        if (bell) {
            renderBell(g, x0, y0);
        } else {
            renderTaiko(g, x0, y0);
        }
        // 当たった数
        for (int i = 0; i < need; i++) {
            int cx = width / 2 - need * 7 + i * 14 + 7;
            int cy = y0 + panelH - 26;
            g.fill(cx - 5, cy - 5, cx + 5, cy + 5, i < hits ? 0xFFE8B84A : 0xFF3A4A5A);
        }
        if (t - flash < 10) {
            float a = 1f - (t - flash) / 10f;
            Ui.serif(g, judge, width / 2f, y0 + 12, Ui.alpha(flashGood ? 0xFFFFE08A : 0xFFB0B8C0, a), 1.4f, 0);
        }
        Ui.sansCentered(g, bell ? "撞木がいちばん奥に来た瞬間にクリック（スペース）" : "音符が輪に重なった瞬間にクリック（スペース）",
                width / 2, y0 + panelH - 12, 0xFFAAAAAA);
    }

    private void renderTaiko(GuiGraphics g, int x0, int y0) {
        int lane = y0 + 70;
        int hx = x0 + 44;
        g.fill(x0 + 10, lane - 18, x0 + panelW - 10, lane + 18, 0xFF2A1A12);
        g.fill(x0 + 10, lane - 18, x0 + panelW - 10, lane - 16, 0xFF6D4C41);
        g.fill(x0 + 10, lane + 16, x0 + panelW - 10, lane + 18, 0xFF6D4C41);
        // 叩く輪
        ring(g, hx, lane, 15, 0xFFE0D6C0);
        ring(g, hx, lane, 12, 0xFF8D6E63);
        if (t - flash < 4 && flashGood) {
            ring(g, hx, lane, 18, 0xFFFFE08A);
        }
        for (int i = notes.size() - 1; i >= 0; i--) {
            float dx = (notes.get(i) - t) * SPEED;
            int nx = (int) (hx + dx);
            if (nx > x0 + panelW - 14 || nx < x0 + 12) {
                continue;
            }
            int col = rim.get(i) ? 0xFF4FA3D9 : 0xFFD9472B;
            disc(g, nx, lane, 11, 0xFFFFFFFF);
            disc(g, nx, lane, 9, col);
        }
    }

    private void renderBell(GuiGraphics g, int x0, int y0) {
        int cx = width / 2;
        int cy = y0 + 64;
        // 鐘
        g.fill(cx + 22, cy - 34, cx + 66, cy + 26, 0xFF5D6B4E);
        g.fill(cx + 18, cy + 20, cx + 70, cy + 28, 0xFF4A5640);
        g.fill(cx + 40, cy - 44, cx + 48, cy - 34, 0xFF4A5640);
        // 撞木：縄で吊られて左右にゆれる。いちばん右（鐘に近い）で当たり
        float s = swing();
        int lx = (int) (cx - 20 + s * 36);
        g.fill(cx - 60, cy - 50, cx + 20, cy - 46, 0xFF6D4C41);
        line(g, cx - 40, cy - 46, lx - 22, cy - 6, 0xFFC8B48A);
        line(g, cx - 4, cy - 46, lx + 14, cy - 6, 0xFFC8B48A);
        g.fill(lx - 30, cy - 8, lx + 18, cy + 2, 0xFFB58A5A);
        // 当たりの目印
        int zone = (int) (cx - 20 + 36 * 0.82f);
        g.fill(zone, cy + 10, cx - 20 + 36 + 1, cy + 14, 0x88FFE08A);
    }

    private static void ring(GuiGraphics g, int cx, int cy, int r, int col) {
        for (int a = 0; a < 48; a++) {
            double ang = a / 48.0 * Math.PI * 2;
            int x = cx + (int) Math.round(Math.cos(ang) * r);
            int y = cy + (int) Math.round(Math.sin(ang) * r);
            g.fill(x - 1, y - 1, x + 1, y + 1, col);
        }
    }

    private static void disc(GuiGraphics g, int cx, int cy, int r, int col) {
        for (int dy = -r; dy <= r; dy++) {
            int w = (int) Math.sqrt(r * r - dy * dy);
            g.fill(cx - w, cy + dy, cx + w + 1, cy + dy + 1, col);
        }
    }

    private static void line(GuiGraphics g, int x0, int y0, int x1, int y1, int col) {
        int n = Math.max(Math.abs(x1 - x0), Math.abs(y1 - y0));
        for (int i = 0; i <= n; i++) {
            int x = x0 + (x1 - x0) * i / Math.max(1, n);
            int y = y0 + (y1 - y0) * i / Math.max(1, n);
            g.fill(x, y, x + 1, y + 1, col);
        }
    }

    private void strike() {
        if (finished()) {
            return;
        }
        boolean good;
        if (bell) {
            good = swing() > 0.82f;
            if (good) {
                ClientGame.play(SoundEvents.BELL_BLOCK, 0.7f);
            } else {
                ClientGame.play(SoundEvents.WOOD_HIT, 0.8f);
            }
        } else {
            good = !notes.isEmpty() && Math.abs(notes.get(0) - t) <= WINDOW;
            if (good) {
                boolean r = rim.get(0);
                notes.remove(0);
                rim.remove(0);
                ClientGame.play(r ? SoundEvents.NOTE_BLOCK_HAT : SoundEvents.NOTE_BLOCK_BASEDRUM, r ? 1.4f : 0.7f);
            } else {
                ClientGame.play(SoundEvents.NOTE_BLOCK_BASEDRUM, 1.6f);
            }
        }
        if (good) {
            hits++;
            show(true, hits >= need ? "見事！" : "よし！");
            if (hits >= need) {
                complete();
            }
        } else {
            misses++;
            show(false, "ずれた");
        }
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        strike();
        return true;
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (key == GLFW.GLFW_KEY_SPACE) {
            strike();
            return true;
        }
        return super.keyPressed(key, scan, mods);
    }
}
