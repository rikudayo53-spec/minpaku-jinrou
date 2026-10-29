package com.minpaku.client.scene;

import com.minpaku.client.ClientGame;
import com.minpaku.client.Keyframes;
import com.minpaku.client.Ui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.core.Direction;
import org.joml.Vector3f;

import java.util.List;
import java.util.UUID;

/**
 * 追放の場面（コミカル）。
 *   追放あり：生き残りが神輿のように担いで「わっしょい」→ 門で「せーの！」→ 門の外へ放り出して、門がバタン！ → 結果
 *   追放なし：誰もいない中庭で鹿威しが「カコーン」
 *
 * 流れ（追放あり, tick）:
 *   0〜45   横から：担いで玄関を出て、門へ（わっしょい）
 *   45〜76  門の外から：「せーの！」で2回ゆらす
 *   76〜100 横から：門の外へ放り出す（ポイッ）→ 道に落ちる
 *   100〜   門の外から：門がバタン！と閉まり、ぽつーん… → 結果の文字
 */
public final class EjectScene {
    public static final int LEN = 160;

    /** 場面に出す人たち */
    public record Cast(UUID ejected, String name, String text, List<UUID> carriers) {
        public boolean none() {
            return ejected == null || ejected.equals(new UUID(0, 0));
        }

        /** てるてる（追放されて大喜び） */
        public boolean teru() {
            return text != null && text.contains("てるてる");
        }
    }

    private EjectScene() {
    }

    private static float track(float t, float... k) {
        return Keyframes.track(t, k);
    }

    /** 毎 tick 呼ぶ（音） */
    public static void tick(int t, Cast cast) {
        if (cast.none()) {
            if (t == 1) {
                ClientGame.play(SoundEvents.WATER_AMBIENT, 1.4f);
            }
            if (t == 64) {
                ClientGame.play(SoundEvents.BAMBOO_WOOD_HIT, 0.6f);
                ClientGame.play(SoundEvents.NOTE_BLOCK_COW_BELL.value(), 0.6f);
            }
            if (t == 78) {
                ClientGame.play(SoundEvents.NOTE_BLOCK_CHIME.value(), 0.7f);
            }
            return;
        }
        for (int beat : new int[]{6, 16, 26, 36}) {
            if (t == beat) {
                ClientGame.play(SoundEvents.NOTE_BLOCK_BASEDRUM.value(), 0.8f);
                ClientGame.play(SoundEvents.VILLAGER_CELEBRATE, beat % 20 == 6 ? 1.0f : 1.2f);
            }
        }
        if (t == 50 || t == 62) {
            ClientGame.play(SoundEvents.NOTE_BLOCK_BIT.value(), t == 50 ? 0.9f : 1.2f);
        }
        if (t == 76) {
            ClientGame.play(SoundEvents.PLAYER_ATTACK_SWEEP, 0.7f);
            ClientGame.play(SoundEvents.VILLAGER_CELEBRATE, 1.3f);
        }
        if (t == 92) {
            ClientGame.play(SoundEvents.PLAYER_BIG_FALL, 0.9f);
            ClientGame.play(SoundEvents.GRAVEL_BREAK, 0.8f);
        }
        if (t == 104) {
            ClientGame.play(SoundEvents.WOODEN_DOOR_CLOSE, 0.6f);
            ClientGame.play(SoundEvents.ANVIL_LAND, 0.5f);
        }
        if (t == 116) {
            ClientGame.play(SoundEvents.NOTE_BLOCK_BELL.value(), 0.5f);
        }
    }

    public static void render(GuiGraphics g, float t, Cast cast) {
        int w = g.guiWidth();
        int h = g.guiHeight();
        // 空
        g.fillGradient(0, 0, w, h, 0xFF6FB3F0, 0xFFD8ECFF);
        if (cast.none()) {
            renderNone(g, t, cast, w, h);
        } else {
            renderEject(g, t, cast, w, h);
        }
        // 上下の黒帯
        g.fill(0, 0, w, h / 12, 0xFF000000);
        g.fill(0, h - h / 12, w, h, 0xFF000000);
        // はじめと終わりの暗転
        float fade = Math.max(Mth.clamp(1f - t / 6f, 0f, 1f), Mth.clamp((t - (LEN - 8)) / 8f, 0f, 1f));
        if (fade > 0) {
            g.fill(0, 0, w, h, Ui.alpha(0xFF000000, fade));
        }
    }

    // ================================================================ 追放あり

    private static void renderEject(GuiGraphics g, float t, Cast cast, int w, int h) {
        // 担ぐ人の並び（最大4人。2列×2）
        int n = Math.max(1, Math.min(4, cast.carriers().size()));
        // 全体の位置：玄関（x=5）を出て、門（x=12）の手前で止まる
        float cx = track(t, 0, -5.5f, 44, 9.6f, 76, 9.6f);
        // せーの！（前後にゆらす）
        float swing = track(t, 44, 0, 52, -0.55f, 58, 0.35f, 64, -0.7f, 74, 0.6f, 76, 0.7f);
        float walk = t < 44 ? t : 0;
        float bob = t < 44 ? (float) Math.abs(Math.sin(walk * 0.42f)) * 0.12f : 0f;
        // 門の戸：100〜106 でバタンと閉まる（開いた角度 85°→0°、閉まったあと少しはね返る）
        float door = track(t, 0, 85, 100, 85, 105, 0, 108, 6, 111, 0);

        // ---- 放り出したあとの追放された人の位置（門の外の道へ落ちる）
        float tau = Mth.clamp((t - 76) / 16f, 0f, 1f);
        float fx = Mth.lerp(tau, cx + 0.9f, 16.2f);
        float fy = 2.1f + (float) Math.sin(tau * Math.PI) * 1.6f - tau * 2.05f;
        float fz = -0.5f;

        // ---- カメラ
        Vector3f eye;
        Vector3f target;
        float fov;
        if (t < 44) {
            eye = new Vector3f(cx - 0.2f, 1.9f, 4.6f);
            target = new Vector3f(cx + 0.4f, 1.55f, -0.5f);
            fov = 55;
        } else if (t < 76) {
            float f = Mth.clamp((t - 44) / 32f, 0f, 1f);
            eye = new Vector3f(Mth.lerp(f, 17.5f, 16.0f), 1.9f, Mth.lerp(f, 0.6f, 0.2f));
            target = new Vector3f(10.4f, 1.8f, -0.5f);
            fov = 52;
        } else if (t < 100) {
            eye = new Vector3f(14.0f, 1.6f, 5.6f);
            target = new Vector3f(Mth.lerp(tau, 12.0f, 15.0f), 1.3f, -0.5f);
            fov = 58;
        } else {
            float f = Mth.clamp((t - 100) / 60f, 0f, 1f);
            eye = new Vector3f(Mth.lerp(f, 21.5f, 20.2f), Mth.lerp(f, 1.5f, 1.25f), Mth.lerp(f, 1.6f, 1.0f));
            target = new Vector3f(14.5f, 1.4f, -0.5f);
            fov = 52;
        }

        Scene3D s = Scene3D.begin(g, eye, target, fov);
        house(s);
        gate(s, door);

        // ---- 担ぐ人たち（+X を向いて歩く。腕は真上）
        float[] cxs = n == 1 ? new float[]{0f} : n == 2 ? new float[]{-0.55f, 0.55f} : new float[]{-0.55f, -0.55f, 0.55f, 0.55f};
        float[] czs = n <= 2 ? new float[]{-0.5f, -0.5f} : new float[]{-0.85f, -0.15f, -0.85f, -0.15f};
        if (n == 3) {
            cxs = new float[]{-0.6f, 0.0f, 0.6f};
            czs = new float[]{-0.85f, -0.15f, -0.85f};
        }
        boolean thrown = t >= 76;
        for (int i = 0; i < n; i++) {
            UUID id = cast.carriers().isEmpty() ? new UUID(1, i) : cast.carriers().get(i);
            PlayerSkin skin = ClientGame.skin(id);
            PlayerModel<?> m = Scene3D.model(skin);
            float px = cx + cxs[i];
            float ph = (float) Math.sin(walk * 0.42f + i * 1.3f);
            if (!thrown) {
                m.rightArm.xRot = -3.0f + swing * 0.5f;
                m.leftArm.xRot = -3.0f + swing * 0.5f;
                m.rightLeg.xRot = t < 44 ? ph * 0.7f : 0f;
                m.leftLeg.xRot = t < 44 ? -ph * 0.7f : 0f;
                m.head.xRot = -0.25f;
            } else {
                // 放り出したあとは手をはたいて、やれやれ
                float clap = (float) Math.sin(t * 1.4f + i);
                m.rightArm.xRot = -1.3f;
                m.leftArm.xRot = -1.3f;
                m.rightArm.yRot = -0.5f + clap * 0.25f;
                m.leftArm.yRot = 0.5f - clap * 0.25f;
                m.head.xRot = 0.2f;
            }
            float lean = thrown ? 4f : swing * 14f;
            s.player(m, skin, px + (thrown ? 0 : swing * 0.25f), bob * (i % 2 == 0 ? 1 : 0.6f), czs[i], -90f, lean, 0f);
            s.shadow(px, 0f, czs[i], 0.3f);
        }

        // ---- 追放される人
        PlayerSkin es = ClientGame.skin(cast.ejected());
        PlayerModel<?> em = Scene3D.model(es);
        float flail = (float) Math.sin(t * 1.3f);
        boolean landed = t >= 92;
        if (cast.teru()) {
            // てるてる：大喜びで両手を上げてばんざい
            float joy = (float) Math.sin(t * 0.9f);
            em.rightArm.xRot = -2.9f + joy * 0.3f;
            em.leftArm.xRot = -2.9f - joy * 0.3f;
            em.rightArm.zRot = 0.4f;
            em.leftArm.zRot = -0.4f;
            em.rightLeg.xRot = landed ? 0f : joy * 0.4f;
            em.leftLeg.xRot = landed ? 0f : -joy * 0.4f;
            em.head.xRot = -0.6f;
        } else if (landed) {
            // 道にぺたんと座りこむ（うなだれる）
            em.rightLeg.xRot = -1.45f;
            em.leftLeg.xRot = -1.45f;
            em.rightLeg.yRot = 0.3f;
            em.leftLeg.yRot = -0.3f;
            em.rightArm.xRot = -0.5f;
            em.leftArm.xRot = -0.5f;
            em.head.xRot = 0.55f + (t > 112 ? (float) Math.sin(t * 0.15f) * 0.05f : 0f);
        } else {
            em.rightArm.xRot = -1.5f + flail * 0.8f;
            em.leftArm.xRot = -1.5f - flail * 0.8f;
            em.rightLeg.xRot = flail * 0.6f;
            em.leftLeg.xRot = -flail * 0.6f;
            em.head.xRot = -0.3f;
        }
        if (!thrown) {
            // あお向け：足が +X、頭が -X
            float ex = cx + 0.9f + swing;
            float ey = 2.05f + bob + Math.abs(swing) * 0.15f;
            s.player(em, es, ex, ey, -0.5f, -90f, -90f, 0f);
        } else if (!landed) {
            // 弧をえがいて門の外へ（くるっと1回転）
            s.ps.pushPose();
            s.ps.translate(fx, fy + 0.9f, fz);
            s.ps.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(-tau * 360f));
            s.ps.translate(0, -0.9f, 0);
            s.player(em, es, 0, 0, 0, -90f, 0f, 0f);
            s.ps.popPose();
        } else if (cast.teru()) {
            // てるてる：門の外でぴょんぴょん
            float hop = (float) Math.abs(Math.sin((t - 92) * 0.35f)) * 0.5f;
            s.player(em, es, 16.2f, hop, fz, -90f, 0f, 0f);
            s.shadow(16.2f, 0f, fz, 0.35f);
        } else {
            s.player(em, es, 16.2f, -0.5f, fz, -90f, 0f, 0f);
            s.shadow(16.2f, 0f, fz, 0.4f);
        }
        float[] eHead = s.toScreen(thrown ? (landed ? 16.2f : fx) : cx + 0.9f, landed ? 1.5f : (thrown ? fy + 1.2f : 2.9f), -0.5f);
        float[] gateTop = s.toScreen(12f, 4.6f, -0.5f);
        float[][] heads = new float[n][];
        for (int i = 0; i < n; i++) {
            heads[i] = s.toScreen(cx + cxs[i], 2.6f, czs[i]);
        }
        s.end();

        // ---- 文字（2D）
        if (t < 44) {
            for (int b = 0; b < 4; b++) {
                float bt = t - (6 + b * 10);
                if (bt > 0 && bt < 9 && heads[b % n] != null) {
                    float a = bt < 6 ? 1f : 1f - (bt - 6) / 3f;
                    float sc = 1.2f + Math.min(1f, bt / 2f) * 0.4f;
                    Ui.text(g, Ui.serif("わっしょい！"), heads[b % n][0], heads[b % n][1] - 18 - bt * 2, Ui.alpha(0xFFFFFFFF, a),
                            sc, 0, true);
                }
            }
        }
        for (int k = 0; k < 2; k++) {
            float st = t - (50 + k * 12);
            if (st > 0 && st < 10) {
                float a = st < 7 ? 1f : 1f - (st - 7) / 3f;
                String word = k == 0 ? "せーの…" : "せーのっ！";
                Ui.text(g, Ui.serif(word), w / 2f, h * 0.22f, Ui.alpha(0xFFFFE08A, a), 2.6f + st * 0.05f, 0, true);
            }
        }
        if (t >= 76 && t < 90) {
            // （擬音は出さない）
        }
        if (t >= 104 && t < 122 && gateTop != null) {
            float st = t - 104;
            float a = st < 12 ? 1f : 1f - (st - 12) / 6f;
            float pop = st < 3 ? 0.6f + st / 3f * 0.7f : 1.3f - Math.min(0.3f, (st - 3) / 10f);
            // （擬音は出さない）
        }
        if (!cast.teru() && t >= 112 && t < 150 && eHead != null) {
            float st = t - 112;
            float a = st < 30 ? Math.min(1f, st / 4f) : 1f - (st - 30) / 8f;
            // （擬音は出さない）
        }
        // てるてるの喜びの声
        if (cast.teru() && eHead != null) {
            for (int k = 0; k < 2; k++) {
                float st = t - (12 + k * 20);
                if (st > 0 && st < 14) {
                    float a = st < 10 ? 1f : 1f - (st - 10) / 4f;
                    Ui.text(g, Ui.serif(k == 0 ? "やったー！" : "もっと遠くへー！"), eHead[0], eHead[1] - 14 - st, Ui.alpha(0xFFFFE08A, a),
                            1.6f, 0, true);
                }
            }
        }
        if (cast.teru() && t >= 96 && t < 124) {
            float a = t < 118 ? 1f : 1f - (t - 118) / 6f;
            Ui.text(g, Ui.serif("計画どおり〜！"), w / 2f, h * 0.3f, Ui.alpha(0xFFFFE08A, a), 2.2f, 0, true);
        }
        // 結果
        if (t >= 128) {
            result(g, t - 128, cast.text(), w, h);
        }
    }

    /** 屋敷の門（x=12。z=-3〜2 が通り道）。door = 戸の開き具合（度、0 で閉まる） */
    private static void gate(Scene3D s, float door) {
        BlockState post = Blocks.DARK_OAK_LOG.defaultBlockState();
        BlockState beam = Blocks.DARK_OAK_PLANKS.defaultBlockState();
        BlockState mud = Blocks.MUD_BRICKS.defaultBlockState();
        // 門の柱と冠木
        for (int y = 0; y <= 3; y++) {
            s.block(post, 12, y, -4);
            s.block(post, 12, y, 3);
        }
        for (int z = -5; z <= 4; z++) {
            s.block(beam, 12, 4, z);
            s.block(Blocks.DEEPSLATE_TILE_STAIRS.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.EAST)
                    .setValue(BlockStateProperties.HALF, Half.BOTTOM), 11, 5, z);
            s.block(Blocks.DEEPSLATE_TILE_STAIRS.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.WEST)
                    .setValue(BlockStateProperties.HALF, Half.BOTTOM), 13, 5, z);
        }
        // 両わきの土塀
        for (int z = -14; z <= 13; z++) {
            if (z >= -4 && z <= 3) {
                continue;
            }
            s.block(mud, 12, 0, z);
            s.block(mud, 12, 1, z);
            s.block(Blocks.DEEPSLATE_TILE_SLAB.defaultBlockState(), 12, 2, z);
        }
        // 門の戸（2枚。外側へ開いている → 閉まる）
        TextureAtlasSprite wood = Scene3D.sprite("minecraft:block/spruce_planks");
        TextureAtlasSprite dark = Scene3D.sprite("minecraft:block/dark_oak_planks");
        TextureAtlasSprite iron = Scene3D.sprite("minecraft:block/iron_block");
        for (int side = 0; side < 2; side++) {
            float hingeZ = side == 0 ? -3.0f : 3.0f;
            float sign = side == 0 ? 1f : -1f;
            s.ps.pushPose();
            s.ps.translate(12.5f, 0, hingeZ);
            s.ps.mulPose(com.mojang.math.Axis.YP.rotationDegrees(sign * door));
            // 戸の板（ちょうつがい側から中央へ 3 マス）
            float z0 = side == 0 ? 0f : -3.0f;
            float z1 = side == 0 ? 3.0f : 0f;
            s.box(wood, -0.08f, 0f, z0, 0.08f, 3.9f, z1, 0xFFFFFFFF);
            for (float yy : new float[]{0.5f, 1.9f, 3.3f}) {
                s.box(dark, -0.12f, yy, z0, 0.12f, yy + 0.18f, z1, 0xFFFFFFFF);
            }
            s.box(iron, -0.14f, 1.85f, side == 0 ? 2.6f : -2.85f, 0.14f, 2.15f, side == 0 ? 2.85f : -2.6f, 0xFFFFFFFF);
            s.ps.popPose();
        }
        // 門の外：道
        s.fill(Blocks.DIRT_PATH.defaultBlockState(), 13, -1, -3, 30, -1, 2);
    }

    private static void koiPart(Scene3D s, String name) {
        // Blockbench の高精細な鯉（なければ Blender の鯉）
        com.minpaku.client.model.HiRes hr = com.minpaku.client.model.HiRes.get(name);
        if (hr != null) {
            hr.render(s.ps, s.buf, net.minecraft.client.renderer.LightTexture.FULL_BRIGHT,
                    net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY, com.minpaku.client.model.HiRes.STILL);
            return;
        }
        com.minpaku.client.model.StaticMesh m = com.minpaku.client.model.MeshCache.get(name);
        if (m != null) {
            m.render(s.ps, s.buf, net.minecraft.client.renderer.LightTexture.FULL_BRIGHT,
                    net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY);
        }
    }

    /** 4つの角の星 */
    private static void star(GuiGraphics g, float x, float y, float r, float rot, int color) {
        for (int k = 0; k < 4; k++) {
            double a = Math.toRadians(rot + k * 90);
            double b = a + Math.PI / 2;
            float tx = x + (float) Math.cos(a) * r;
            float ty = y + (float) Math.sin(a) * r;
            float lx = x + (float) Math.cos(a - 0.6) * r * 0.22f;
            float ly = y + (float) Math.sin(a - 0.6) * r * 0.22f;
            float rx = x + (float) Math.cos(a + 0.6) * r * 0.22f;
            float ry = y + (float) Math.sin(a + 0.6) * r * 0.22f;
            Ui.quad(g, x, y, lx, ly, tx, ty, rx, ry, color);
        }
    }

    /** 結果の文字（和紙の帯に1文字ずつ） */
    private static void result(GuiGraphics g, float rt, String text, int w, int h) {
        boolean wolf = text.endsWith("人狼だった");
        boolean teru = text.contains("てるてる");
        float a = Mth.clamp(rt / 5f, 0f, 1f);
        int bandH = 44;
        int y = (int) (h * 0.62f);
        g.fill(0, y, w, y + bandH, Ui.alpha(0xEE140C06, a));
        g.fill(0, y, w, y + 2, Ui.alpha(wolf ? 0xFFC8372D : 0xFFC9A14A, a));
        g.fill(0, y + bandH - 2, w, y + bandH, Ui.alpha(wolf ? 0xFFC8372D : 0xFFC9A14A, a));
        int shown = Mth.clamp((int) (rt / 1.6f), 0, text.length());
        Ui.text(g, Ui.serif(text.substring(0, shown)), w / 2f, y + 12,
                Ui.alpha(teru ? 0xFFFFD54F : wolf ? 0xFFFF6A5A : 0xFFFFFFFF, a), 2.2f, 0, true);
        if (teru && shown >= text.length()) {
            Ui.text(g, Ui.sans("てるてるの一人勝ち！"), w / 2f, y + bandH + 6, Ui.alpha(0xFFFFE08A, a), 1.2f, 0, true);
        }
    }

    /** 玄関まわり（廊下・土間・玄関の戸口・外の庭） */
    private static void house(Scene3D s) {
        BlockState floor = Blocks.OAK_PLANKS.defaultBlockState();
        BlockState wall = Blocks.CALCITE.defaultBlockState();
        BlockState post = Blocks.STRIPPED_SPRUCE_LOG.defaultBlockState();
        BlockState beam = Blocks.SPRUCE_PLANKS.defaultBlockState();
        // 廊下の床と奥の壁
        s.fill(floor, -14, -1, -3, 3, -1, 2);
        s.fill(Blocks.POLISHED_ANDESITE.defaultBlockState(), 4, -1, -3, 4, -1, 2);
        for (int x = -14; x <= 4; x++) {
            s.fill(x % 3 == 0 ? post : wall, x, 0, -4, x, 2, -4);
            s.block(beam, x, 3, -4);
        }
        // ふすま風の飾り（奥の壁）
        for (int x = -12; x <= 1; x += 3) {
            s.block(Blocks.BOOKSHELF.defaultBlockState(), x + 1, 0, -3);
        }
        // 玄関の戸口（x=5 の壁、z=-3〜1 があいている）
        for (int z = -5; z <= 3; z++) {
            boolean open = z >= -3 && z <= 1;
            for (int y = 0; y <= 3; y++) {
                if (open && y <= 2) {
                    continue;
                }
                s.block(y == 3 ? beam : (z == -4 || z == 2 ? post : wall), 5, y, z);
            }
        }
        // 開いた引き戸（右へ寄せてある）
        for (int y = 0; y <= 2; y++) {
            s.block(Blocks.SPRUCE_TRAPDOOR.defaultBlockState().setValue(BlockStateProperties.OPEN, true)
                    .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.EAST), 4, y, 2);
        }
        // 屋根のひさし
        for (int z = -4; z <= 2; z++) {
            s.block(Blocks.DEEPSLATE_TILE_STAIRS.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.WEST)
                    .setValue(BlockStateProperties.HALF, Half.BOTTOM), 6, 4, z);
            s.block(Blocks.DEEPSLATE_TILE_SLAB.defaultBlockState(), 5, 4, z);
        }
        // 外：庭と石の道
        s.fill(Blocks.GRASS_BLOCK.defaultBlockState(), 6, -1, -9, 30, -1, 6);
        s.fill(Blocks.GRAVEL.defaultBlockState(), 6, -1, -2, 30, -1, 0);
        for (int x = 7; x <= 10; x += 2) {
            s.block(Blocks.SMOOTH_STONE_SLAB.defaultBlockState(), x, 0, -1);
        }
        // 外の家の壁（玄関の左右）と植木・灯籠
        for (int z = -9; z <= 6; z++) {
            if (z < -5 || z > 3) {
                for (int y = 0; y <= 3; y++) {
                    s.block(y == 3 ? beam : (z % 3 == 0 ? post : wall), 5, y, z);
                }
            }
        }
        s.block(Blocks.STONE_BRICK_WALL.defaultBlockState(), 10, 0, -6);
        s.block(Blocks.LANTERN.defaultBlockState(), 10, 1, -6);
        s.block(Blocks.AZALEA.defaultBlockState(), 11, 0, -4);
        s.block(Blocks.FLOWERING_AZALEA.defaultBlockState(), 14, 0, 3);
        s.block(Blocks.CHERRY_LEAVES.defaultBlockState(), 16, 0, -5);
        s.block(Blocks.CHERRY_LEAVES.defaultBlockState(), 16, 1, -5);
        s.block(Blocks.CHERRY_LEAVES.defaultBlockState(), 17, 1, -5);
        s.block(Blocks.CHERRY_LOG.defaultBlockState(), 16, 0, -6);
        // 外の竹垣
        for (int x = 7; x <= 30; x++) {
            s.block(Blocks.BAMBOO_FENCE.defaultBlockState(), x, 0, -8);
        }
    }

    // ================================================================ 追放なし（鹿威し）

    private static void renderNone(GuiGraphics g, float t, Cast cast, int w, int h) {
        // ゆっくり寄るカメラ
        float k = Mth.clamp(t / LEN, 0f, 1f);
        Vector3f eye = new Vector3f(Mth.lerp(k, 2.1f, 1.6f), Mth.lerp(k, 1.25f, 1.0f), Mth.lerp(k, 2.9f, 2.2f));
        Vector3f target = new Vector3f(0.9f, 0.35f, 0.1f);
        Scene3D s = Scene3D.begin(g, eye, target, 50);
        // 中庭：苔と砂利、奥に縁側
        for (int x = -8; x <= 8; x++) {
            for (int z = -6; z <= 6; z++) {
                // 鹿威しの横の池（x=1〜3, z=-2〜2）と、そのまわりの石の縁
                boolean pond = x >= 1 && x <= 3 && z >= -2 && z <= 2;
                boolean rim = !pond && x >= 0 && x <= 4 && z >= -3 && z <= 3;
                if (pond) {
                    s.block(Blocks.MUD.defaultBlockState(), x, -2, z);
                } else if (rim) {
                    s.block((x + z) % 2 == 0 ? Blocks.MOSSY_COBBLESTONE.defaultBlockState() : Blocks.COBBLESTONE.defaultBlockState(),
                            x, -1, z);
                } else {
                    s.block(x >= -4 && x <= -1 && z >= 2 && z <= 5 ? Blocks.GRAVEL.defaultBlockState()
                            : Blocks.MOSS_BLOCK.defaultBlockState(), x, -1, z);
                }
            }
        }
        s.fill(Blocks.SPRUCE_PLANKS.defaultBlockState(), -8, 0, -6, 8, 0, -5);
        for (int x = -8; x <= 8; x++) {
            s.fill(x % 3 == 0 ? Blocks.STRIPPED_SPRUCE_LOG.defaultBlockState() : Blocks.CALCITE.defaultBlockState(), x, 1, -6, x, 3, -6);
            s.block(Blocks.DEEPSLATE_TILES.defaultBlockState(), x, 4, -6);
        }
        // 池（水）と石
        TextureAtlasSprite water = Scene3D.sprite("minecraft:block/water_still");
        // 池の鯉（Blender の鯉。体をくねらせて泳ぐ：頭・胴・尾の3つを少しずつ曲げる）
        for (int i = 0; i < 2; i++) {
            String kind = i == 0 ? "koi_kohaku" : "koi_gold";
            double ang = t * 0.03 * (i == 0 ? 1 : -1.15) + i * Math.PI;
            double rx = i == 0 ? 0.72 : 0.6;
            double rz = i == 0 ? 1.25 : 1.0;
            float kx = 2.5f + (float) (Math.cos(ang) * rx);
            float kz = 0.5f + (float) (Math.sin(ang) * rz);
            double dir = i == 0 ? 1 : -1;
            double vx = -Math.sin(ang) * rx * dir;
            double vz = Math.cos(ang) * rz * dir;
            float yaw = (float) Math.toDegrees(Math.atan2(-vx, -vz));
            float wag = (float) Math.sin(t * 0.45f + i * 2f);
            s.ps.pushPose();
            s.ps.translate(kx, -0.52f + (float) Math.sin(t * 0.08f + i) * 0.02f, kz);
            s.ps.mulPose(com.mojang.math.Axis.YP.rotationDegrees(yaw + wag * 4f));
            s.ps.scale(0.9f, 0.9f, 0.9f);
            koiPart(s, kind + "_front");
            s.ps.translate(0, 0, -0.09f);
            s.ps.mulPose(com.mojang.math.Axis.YP.rotationDegrees(wag * 12f));
            s.ps.translate(0, 0, 0.09f);
            koiPart(s, kind + "_mid");
            s.ps.translate(0, 0, 0.06f);
            s.ps.mulPose(com.mojang.math.Axis.YP.rotationDegrees((float) Math.sin(t * 0.45f + i * 2f - 0.9f) * 22f));
            s.ps.translate(0, 0, -0.06f);
            koiPart(s, kind + "_tail");
            s.ps.popPose();
        }
        // 倒れた竹から水が落ちると水面がゆれる
        float ripple = t > 59 && t < 75 ? (float) Math.sin((t - 59) * 1.2f) * 0.03f * (1f - (t - 59) / 16f) : 0f;
        s.box(water, 1f, -0.95f, -2f, 4f, -0.2f + ripple, 3f, 0xB83F76E4);
        s.block(Blocks.LILY_PAD.defaultBlockState(), 3, -0.24f, 2);
        s.block(Blocks.LILY_PAD.defaultBlockState(), 2, -0.24f, -2);
        s.block(Blocks.MOSSY_COBBLESTONE.defaultBlockState(), 4, 0, -3);
        s.block(Blocks.STONE.defaultBlockState(), -3, -1, 0);
        s.block(Blocks.FERN.defaultBlockState(), -4, 0, -1);
        s.block(Blocks.FERN.defaultBlockState(), 3, 0, -2);
        s.block(Blocks.AZALEA.defaultBlockState(), -6, 0, -3);
        s.block(Blocks.STONE_BRICK_WALL.defaultBlockState(), 4, 0, -2);
        s.block(Blocks.LANTERN.defaultBlockState(), 4, 1, -2);
        for (int x = -8; x <= 8; x++) {
            s.block(Blocks.BAMBOO_FENCE.defaultBlockState(), x, 0, 6);
        }

        // ---- 鹿威し
        TextureAtlasSprite bamboo = Scene3D.sprite("minecraft:block/bamboo_block");
        TextureAtlasSprite bambooTop = Scene3D.sprite("minecraft:block/stripped_bamboo_block");
        TextureAtlasSprite stone = Scene3D.sprite("minecraft:block/stone");
        // 支え（2本の竹）と軸
        s.box(bamboo, -0.06f, 0f, -0.42f, 0.06f, 0.75f, -0.30f, 0xFFFFFFFF);
        s.box(bamboo, -0.06f, 0f, 0.30f, 0.06f, 0.75f, 0.42f, 0xFFFFFFFF);
        s.box(bambooTop, -0.03f, 0.6f, -0.42f, 0.03f, 0.66f, 0.42f, 0xFFFFFFFF);
        // 打つ石
        s.box(stone, -0.95f, 0f, -0.18f, -0.65f, 0.16f, 0.18f, 0xFFFFFFFF);
        // 筧（池の向こうから水を引く竹）
        s.box(bamboo, 3.05f, -0.2f, -0.08f, 3.17f, 1.2f, 0.04f, 0xFFFFFFFF);
        s.box(bamboo, 1.0f, 1.08f, -0.06f, 3.12f, 1.16f, 0.02f, 0xFFFFFFFF);
        // 傾き：ふだんは口が上（+18°）。水がたまって下がり、一気に倒れて戻る→石を打つ（64）
        float ang = track(t, 0, 18, 40, 16, 56, 2, 60, -28, 63, 22, 66, 15, 70, 18);
        s.ps.pushPose();
        s.ps.translate(0, 0.63f, -0.02f);
        s.ps.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(ang));
        s.box(bamboo, -0.85f, -0.07f, -0.07f, 1.2f, 0.07f, 0.07f, 0xFFFFFFFF);
        s.box(bambooTop, 1.19f, -0.065f, -0.065f, 1.21f, 0.065f, 0.065f, 0xFFFFFFFF);
        s.ps.popPose();
        // 筧から落ちる水
        s.box(water, 1.03f, Mth.lerp(Mth.clamp(ang / 18f, 0f, 1f), 0.75f, 1.0f), -0.03f, 1.07f, 1.08f, 0.01f, 0xAA6FA8FF);
        // 倒れたときに池へこぼれる水
        if (t > 58 && t < 67) {
            s.box(water, 1.08f, -0.2f, -0.05f, 1.18f, 0.12f, 0.05f, 0xAA6FA8FF);
        }
        float[] hit = s.toScreen(-0.8f, 0.4f, 0f);
        s.end();

        if (t >= 64 && hit != null) {
            float st = t - 64;
            float a = st < 14 ? 1f : Mth.clamp(1f - (st - 14) / 8f, 0f, 1f);
            // （擬音は出さない）
        }
        if (t >= 80) {
            result(g, t - 80, cast.text(), w, h);
        }
    }
}
