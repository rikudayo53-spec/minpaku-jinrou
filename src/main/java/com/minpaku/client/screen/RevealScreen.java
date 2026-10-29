package com.minpaku.client.screen;

import com.minpaku.client.ClientGame;
import com.minpaku.client.Ui;
import com.minpaku.game.Role;
import com.minpaku.network.Packets;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;

/** ゲーム開始時の役職発表：巻物が開いて、筆文字で役職が出る（数秒で自動で閉じる） */
public class RevealScreen extends Screen {
    private static final int LENGTH = 130;
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
        if (age == 30) {
            ClientGame.play(SoundEvents.BOOK_PAGE_TURN, 0.7f);
        }
        if (age == 48) {
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
        int c = role.team == Role.Team.WOLF ? 0x00500A06 : 0x00102A10;
        g.fillGradient(0, 0, width, height / 2, 0xF0000000, 0xF0000000 | c);
        g.fillGradient(0, height / 2, width, height, 0xF0000000 | c, 0xF0000000);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float pt) {
        super.render(g, mouseX, mouseY, pt);
        float t = age + pt;
        int cx = width / 2;
        int cy = height / 2;
        float fadeOut = age > LENGTH - 14 ? Mth.clamp((LENGTH - t) / 14f, 0f, 1f) : 1f;

        // 1) 最初の文字
        if (t < 30) {
            float a = Mth.clamp(t / 8f, 0f, 1f) * Mth.clamp((30 - t) / 6f, 0f, 1f);
            if (reveal.players() < 0) {
                Ui.text(g, Ui.serif("悪神の祟り"), cx, cy - 34, Ui.alpha(0xFFD9A6FF, a), 4f, 0, false);
                Ui.text(g, Ui.sans("祠の封印が解けて、みんなの役職が入れかわった…"), cx, cy + 14,
                        Ui.alpha(0xFFDDDDDD, a), 1.1f, 0, false);
            } else {
                Ui.text(g, Ui.serif("民泊人狼"), cx, cy - 34, Ui.alpha(0xFFFFE0B2, a), 4f, 0, false);
                Ui.text(g, Ui.sans(reveal.players() + "人の宿泊客の中に、人狼がまぎれこんでいる…"), cx, cy + 14,
                        Ui.alpha(0xFFDDDDDD, a), 1.1f, 0, false);
            }
            return;
        }

        // 2) 巻物が開く
        float open = Mth.clamp((t - 30) / 16f, 0f, 1f);
        open = 1 - (1 - open) * (1 - open);
        int sw = Math.min(width - 40, 300);
        int fullH = Math.min(height - 24, 300);
        // ミニキャラの大きさ（画面が低いときは小さく）
        int chara = fullH >= 280 ? 64 : 48;
        int sh = (int) (fullH * open);
        int x = cx - sw / 2;
        int top = cy - sh / 2;
        g.pose().pushPose();
        if (fadeOut < 1f) {
            g.pose().translate(0, (1 - fadeOut) * 10, 0);
        }
        Ui.sprite(g, "panel", x + 8, top, sw - 16, sh);
        // 軸（上下の木の棒）
        Ui.sprite(g, "board", x, top - 8, sw, 12);
        Ui.sprite(g, "board", x, top + sh - 4, sw, 12);
        if (open >= 1f) {
            float t2 = t - 46;
            float a = Mth.clamp(t2 / 6f, 0f, 1f) * fadeOut;
            float pop = t2 < 8 ? 0.7f + 0.3f * (t2 / 8f) + (float) Math.sin(t2 / 8f * Math.PI) * 0.15f : 1f;
            // 役職のミニキャラ（ドット絵）：ぽんと出て、ときどき跳ねて、まばたきする
            drawChara(g, cx, top + 10, chara, t2, a);
            int y0 = top + 14 + chara;
            float big = chara >= 64 ? 1f : 0.8f;
            Ui.text(g, Ui.sans(reveal.players() < 0 ? "あなたの新しい役職は" : "あなたの役職は"), cx, y0, Ui.alpha(Ui.INK_SOFT, a), 1f, 0, false);
            float nameScale = (role.name.length() > 4 ? 3.0f : role.name.length() > 2 ? 4.0f : 5.0f) * big;
            Ui.text(g, Ui.serif(role.name), cx, y0 + 14 + (1 - pop) * 20, Ui.alpha(0xFF000000 | role.color, a),
                    nameScale * pop, 0, false);
            int chipY = y0 + 14 + (int) (12 * nameScale) + 6;
            Ui.chip(g, role.team.label, cx, chipY, Ui.alpha(0xFF000000 | role.team.color, a * 0.9f), Ui.alpha(0xFFFFFFFF, a));
            int ly = chipY + 18;
            for (var line : Ui.split(role.description, sw - 50)) {
                int lw = font.width(line);
                g.drawString(font, line, cx - lw / 2, ly, Ui.alpha(Ui.INK, a), false);
                ly += 10;
            }
            // 仲間の人狼
            if (!reveal.mates().isEmpty()) {
                Ui.text(g, Ui.sans(role == Role.KYOYU ? "相方の共有者" : "仲間の人狼"), cx, ly + 4, Ui.alpha(Ui.VERMILION, a), 0.9f, 0, false);
                int n = reveal.mates().size();
                int fx = cx - n * 34 / 2;
                for (int i = 0; i < n; i++) {
                    Packets.Mate m = reveal.mates().get(i);
                    Ui.face(g, m.id(), fx + i * 34 + 7, ly + 16, 20);
                    Ui.text(g, Ui.sans(m.name()), fx + i * 34 + 17, ly + 38, Ui.alpha(Ui.INK, a), 0.8f, 0, false);
                }
            }
            if (t2 > 10) {
                float st = Mth.clamp((t2 - 10) / 5f, 0f, 1f);
                float scale = 1.6f - 0.6f * st;
                g.pose().pushPose();
                g.pose().translate(x + sw - 40, top + sh - 40, 0);
                g.pose().scale(scale, scale, 1f);
                Ui.stamp(g, "民泊人狼", 0, 0, 34, Ui.alpha(Ui.VERMILION, st * fadeOut), -10, false);
                g.pose().popPose();
            }
        }
        g.pose().popPose();
    }

    /** ミニキャラ：textures/gui/role/<役職>.png（左＝ふつう、右＝まばたき。各32×32） */
    private void drawChara(GuiGraphics g, int cx, int y, int size, float t2, float a) {
        net.minecraft.resources.ResourceLocation tex = com.minpaku.MinpakuJinrou.id("textures/gui/role/" + role.id + ".png");
        float pop = t2 < 6 ? (float) Math.sin(Mth.clamp(t2 / 6f, 0f, 1f) * Math.PI * 0.5) * 1.15f
                : t2 < 10 ? 1.15f - (t2 - 6) / 4f * 0.15f : 1f;
        // 40tick ごとに小さく跳ねる
        float ph = (t2 % 40f) / 40f;
        float hop = ph < 0.2f ? (float) Math.sin(ph / 0.2f * Math.PI) * 3f : 0f;
        boolean blink = (t2 % 52f) > 47f;
        g.pose().pushPose();
        g.pose().translate(cx, y + size - hop, 0);
        g.pose().scale(pop, pop, 1f);
        // 足もとの影
        g.fill(-size / 3, -2, size / 3, 0, Ui.alpha(0x30000000, a));
        com.mojang.blaze3d.systems.RenderSystem.enableBlend();
        g.setColor(1f, 1f, 1f, a);
        g.blit(tex, -size / 2, -size, size, size, blink ? 32 : 0, 0, 32, 32, 64, 32);
        g.setColor(1f, 1f, 1f, 1f);
        g.pose().popPose();
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
