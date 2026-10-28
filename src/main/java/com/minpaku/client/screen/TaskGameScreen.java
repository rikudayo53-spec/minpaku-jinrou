package com.minpaku.client.screen;

import com.minpaku.client.ClientGame;
import com.minpaku.client.GameHud;
import com.minpaku.network.Packets;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;

/** ミニゲーム画面の共通部分（板の背景、完了の演出） */
public abstract class TaskGameScreen extends Screen {
    protected final BlockPos pos;
    protected int panelW = 240;
    protected int panelH = 160;
    private int doneTimer = -1;

    protected TaskGameScreen(String title, BlockPos pos) {
        super(Component.literal(title));
        this.pos = pos;
    }

    protected int left() {
        return width / 2 - panelW / 2;
    }

    protected int top() {
        return height / 2 - panelH / 2;
    }

    protected boolean finished() {
        return doneTimer >= 0;
    }

    protected void complete() {
        if (doneTimer >= 0) {
            return;
        }
        doneTimer = 15;
        ClientPlayNetworking.send(new Packets.TaskDone(pos));
        ClientGame.play(SoundEvents.PLAYER_LEVELUP, 1.6f);
    }

    @Override
    public void tick() {
        if (doneTimer > 0 && --doneTimer == 0) {
            onClose();
        }
        // 会議が始まったら閉じる
        if (ClientGame.state.phase() != 1) {
            onClose();
        }
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float pt) {
        g.fill(0, 0, width, height, 0x99000000);
        int x = left();
        int y = top();
        g.fill(x - 6, y - 22, x + panelW + 6, y + panelH + 6, 0xFF4E342E);
        g.fill(x - 3, y - 19, x + panelW + 3, y + panelH + 3, 0xFF6D4C41);
        g.fill(x, y, x + panelW, y + panelH, 0xFF2B2B2B);
        g.drawCenteredString(font, title, width / 2, y - 15, 0xFFFFE0B2);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float pt) {
        super.render(g, mouseX, mouseY, pt);
        renderGame(g, mouseX, mouseY, pt);
        if (finished()) {
            g.fill(left(), top(), left() + panelW, top() + panelH, 0x8800AA00);
            GameHud.drawBig(g, font, "完了！", width / 2, height / 2 - 10, 3f, 0xFFFFFFFF);
        }
    }

    protected abstract void renderGame(GuiGraphics g, int mouseX, int mouseY, float pt);

    protected static boolean inside(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
