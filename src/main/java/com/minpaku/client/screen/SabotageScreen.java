package com.minpaku.client.screen;

import com.minpaku.client.ClientGame;
import com.minpaku.network.Packets;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** 人狼の妨害メニュー */
public class SabotageScreen extends Screen {
    public SabotageScreen() {
        super(Component.literal("妨害"));
    }

    @Override
    protected void init() {
        int cx = width / 2;
        int cy = height / 2;
        addRenderableWidget(Button.builder(Component.literal("停電させる"), b -> pick(0))
                .bounds(cx - 105, cy - 10, 100, 30).build());
        addRenderableWidget(Button.builder(Component.literal("ガス漏れを起こす"), b -> pick(1))
                .bounds(cx + 5, cy - 10, 100, 30).build());
    }

    private void pick(int kind) {
        ClientPlayNetworking.send(new Packets.Sabotage(kind));
        onClose();
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float pt) {
        super.render(g, mouseX, mouseY, pt);
        int cx = width / 2;
        int cy = height / 2;
        g.drawCenteredString(font, "妨害を選ぶ", cx, cy - 50, 0xFFFF5555);
        int cd = ClientGame.state.sabotageCooldown();
        String info = ClientGame.state.blackout() || ClientGame.state.gasTicks() >= 0 ? "いま妨害中"
                : cd > 0 ? "あと " + (cd / 20 + 1) + " 秒で使える" : "使える！";
        g.drawCenteredString(font, info, cx, cy - 34, 0xFFDDDDDD);
        g.drawCenteredString(font, "停電: 村人の視界が暗くなる（配電盤で直される）", cx, cy + 32, 0xFFAAAAAA);
        g.drawCenteredString(font, "ガス漏れ: 時間内に元栓とボイラーを止められなければ人狼の勝ち", cx, cy + 44, 0xFFAAAAAA);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
