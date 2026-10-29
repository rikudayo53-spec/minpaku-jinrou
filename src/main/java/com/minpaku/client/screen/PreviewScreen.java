package com.minpaku.client.screen;

import com.minpaku.client.Ui;
import com.minpaku.client.scene.EjectScene;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** /minpaku preview で追放のアニメーションだけを再生する画面（Esc で閉じる） */
public class PreviewScreen extends Screen {
    private final EjectScene.Cast cast;
    private int age;

    public PreviewScreen(EjectScene.Cast cast) {
        super(Component.literal("追放の確認"));
        this.cast = cast;
    }

    @Override
    public void tick() {
        age++;
        EjectScene.tick(age, cast);
        if (age >= EjectScene.LEN) {
            onClose();
        }
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float pt) {
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float pt) {
        EjectScene.render(g, age + pt, cast);
        Ui.sans(g, "確認用　Esc で閉じる", width - 6, height - 12, 0x88FFFFFF, 0.7f, 1);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
