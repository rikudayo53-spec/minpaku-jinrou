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
        g.fill(0, 0, width, height, 0xA0100C08);
        int x = left();
        int y = top();
        com.minpaku.client.Ui.sprite(g, "board", x - 12, y - 28, panelW + 24, panelH + 40);
        com.minpaku.client.Ui.serif(g, title.getString(), width / 2f, y - 21, com.minpaku.client.Ui.CREAM, 1.2f, 0);
        g.fill(x - 2, y - 2, x + panelW + 2, y + panelH + 2, com.minpaku.client.Ui.GOLD);
        g.fill(x, y, x + panelW, y + panelH, 0xFF1B2A3A);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float pt) {
        renderBackground(g, mouseX, mouseY, pt);
        renderGame(g, mouseX, mouseY, pt);
        if (finished()) {
            g.fill(left(), top(), left() + panelW, top() + panelH, 0x8800801A);
            com.minpaku.client.Ui.stamp(g, "完了", width / 2f, height / 2f, 48, 0xFFFFFFFF, -8, false);
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
