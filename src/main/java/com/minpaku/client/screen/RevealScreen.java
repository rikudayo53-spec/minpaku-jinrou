package com.minpaku.client.screen;

import com.minpaku.client.ClientGame;
import com.minpaku.client.GameHud;
import com.minpaku.game.Role;
import com.minpaku.network.Packets;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.PlayerFaceRenderer;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;

/** ゲーム開始時の役職発表（数秒で自動で閉じる） */
public class RevealScreen extends Screen {
    private static final int LENGTH = 120;
    private final Packets.Reveal reveal;
    private final Role role;
    private int age;

    public RevealScreen(Packets.Reveal reveal) {
        super(Component.literal("役職発表"));
        this.reveal = reveal;
        this.role = Role.byId(reveal.role());
    }

    @Override
    public void tick() {
        age++;
        if (age == 1) {
            ClientGame.play(SoundEvents.BELL_BLOCK, 0.6f);
        }
        if (age == 40) {
            if (role.team == Role.Team.WOLF) {
                ClientGame.play(SoundEvents.WOLF_HOWL, 0.8f);
                ClientGame.play(SoundEvents.WITHER_SPAWN, 1.6f);
            } else {
                ClientGame.play(SoundEvents.AMETHYST_BLOCK_RESONATE, 1.0f);
                ClientGame.play(SoundEvents.PLAYER_LEVELUP, 0.7f);
            }
        }
        if (age >= LENGTH) {
            onClose();
        }
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float pt) {
        g.fill(0, 0, width, height, 0xF0000000);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float pt) {
        super.render(g, mouseX, mouseY, pt);
        float t = age + pt;
        int cx = width / 2;
        int cy = height / 2;

        // 1) タイトル
        float a1 = Mth.clamp(t / 10f, 0f, 1f) * (t > 34 ? Mth.clamp((40 - t) / 6f, 0f, 1f) : 1f);
        if (t < 40) {
            int a = Math.max(8, (int) (a1 * 255));
            GameHud.drawBig(g, font, "民泊人狼", cx, cy - 40, 4f, a << 24 | 0xFFE0B2);
            GameHud.drawBig(g, font, reveal.players() + "人の宿泊客の中に、人狼がまぎれこんでいる…", cx, cy + 10, 1.2f, a << 24 | 0xCCCCCC);
            return;
        }

        // 2) 役職
        float t2 = t - 40;
        float pop = t2 < 8 ? 0.6f + 0.4f * (t2 / 8f) + (float) Math.sin(t2 / 8f * Math.PI) * 0.25f : 1f;
        float fade = age > LENGTH - 12 ? Mth.clamp((LENGTH - t) / 12f, 0f, 1f) : 1f;
        int a = Math.max(8, (int) (Mth.clamp(t2 / 6f, 0f, 1f) * fade * 255));

        // 背景の帯
        int band = (int) (60 * pop);
        g.fill(0, cy - band, width, cy + band, (int) (a * 0.5f) << 24 | (role.color >> 2 & 0x3F3F3F));

        GameHud.drawBig(g, font, "あなたの役職は", cx, cy - 58, 1.4f, a << 24 | 0xDDDDDD);
        GameHud.drawBig(g, font, role.name, cx, cy - 34, 5f * pop, a << 24 | role.color);
        GameHud.drawBig(g, font, role.team.label, cx, cy + 14, 1.5f, a << 24 | role.team.color);
        g.drawCenteredString(font, role.description, cx, cy + 34, a << 24 | 0xFFFFFF);

        // 人狼の仲間
        if (!reveal.mates().isEmpty()) {
            g.drawCenteredString(font, "仲間の人狼", cx, cy + 56, a << 24 | 0xFF7777);
            int n = reveal.mates().size();
            int x0 = cx - n * 40 / 2;
            for (int i = 0; i < n; i++) {
                Packets.Mate m = reveal.mates().get(i);
                int x = x0 + i * 40 + 8;
                PlayerFaceRenderer.draw(g, ClientGame.skin(m.id()), x, cy + 68, 24);
                g.drawCenteredString(font, m.name(), x + 12, cy + 96, a << 24 | 0xFFFFFF);
            }
        }
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
