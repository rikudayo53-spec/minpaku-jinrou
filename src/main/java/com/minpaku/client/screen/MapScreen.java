package com.minpaku.client.screen;

import com.minpaku.client.GameHud;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** プレイ中に M で開く見取り図（今いる場所・タスク・妨害を直す場所） */
public class MapScreen extends Screen {
    private final MapView view = new MapView(true);

    public MapScreen() {
        super(Component.literal("見取り図"));
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float pt) {
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float pt) {
        view.render(g, width, height, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (view.click(mx, my)) {
            onClose();
        }
        return true;
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (GameHud.MAP_KEY != null && GameHud.MAP_KEY.matches(key, scan)) {
            onClose();
            return true;
        }
        return super.keyPressed(key, scan, mods);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
