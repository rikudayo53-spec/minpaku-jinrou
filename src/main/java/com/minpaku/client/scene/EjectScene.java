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
 *   追放あり：生き残りが神輿のように担いで「わっしょい」→ 玄関で「せーの！」→ 放り投げて空の星になる → 結果
 *   追放なし：誰もいない中庭で鹿威しが「カコーン」
 *
 * 流れ（追放あり, tick）:
 *   0〜45   横から：担いで廊下を玄関へ（わっしょい）
 *   45〜76  外から玄関を見る：「せーの！」で2回ゆらす
 *   76〜122 外の低い位置から：放り投げて、回りながら空へ
 *   122〜   空：キラーン☆ → 結果の文字
 */
public final class EjectScene {
    public static final int LEN = 160;

    /** 場面に出す人たち */
    public record Cast(UUID ejected, String name, String text, List<UUID> carriers) {
        public boolean none() {
            return ejected == null || ejected.equals(new UUID(0, 0));
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
            ClientGame.play(SoundEvents.FIREWORK_ROCKET_LAUNCH, 0.8f);
            ClientGame.play(SoundEvents.PLAYER_ATTACK_SWEEP, 0.7f);
        }
        if (t == 122) {
            ClientGame.play(SoundEvents.NOTE_BLOCK_CHIME.value(), 1.9f);
            ClientGame.play(SoundEvents.AMETHYST_BLOCK_CHIME, 1.5f);
        }
        if (t == 132) {
            ClientGame.play(SoundEvents.NOTE_BLOCK_BELL.value(), 0.8f);
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
        // 全体の位置：廊下を歩いて玄関（x=3 あたり）で止まる
        float cx = track(t, 0, -7.5f, 44, 2.2f, 76, 2.2f);
        // せーの！（前後にゆらす）
        float swing = track(t, 44, 0, 52, -0.55f, 58, 0.35f, 64, -0.7f, 74, 0.6f, 76, 0.7f);
        float walk = t < 44 ? t : 0;
        float bob = t < 44 ? (float) Math.abs(Math.sin(walk * 0.42f)) * 0.12f : 0f;

        // ---- カメラ
        Vector3f eye;
        Vector3f target;
        float fov;
        // 投げたあとの追放された人の位置
        float tau = Math.max(0, t - 76);
        float fx = 2.2f + 0.7f + 0.55f * tau;
        float fy = 2.25f + 0.62f * tau - 0.0025f * tau * tau;
        float fz = -0.5f - 0.08f * tau;
        if (t < 44) {
            eye = new Vector3f(cx - 0.2f, 1.9f, 4.3f);
            target = new Vector3f(cx + 0.4f, 1.55f, -0.5f);
            fov = 55;
        } else if (t < 76) {
            float f = Mth.clamp((t - 44) / 32f, 0f, 1f);
            eye = new Vector3f(Mth.lerp(f, 9.6f, 8.0f), 1.8f, Mth.lerp(f, 0.4f, 0.0f));
            target = new Vector3f(2.6f, 1.75f, -0.5f);
            fov = 50;
        } else {
            eye = new Vector3f(7.8f, 0.8f, 2.4f);
            float k = Mth.clamp(tau / 30f, 0f, 1f);
            float ty = Math.min(fy, 26f);
            target = new Vector3f(Mth.lerp(k, 4f, Math.min(fx, 22f)), Mth.lerp(k, 2.2f, ty), Mth.lerp(k, -0.5f, fz));
            fov = track(t, 76, 60, 120, 48);
        }

        Scene3D s = Scene3D.begin(g, eye, target, fov);
        house(s);

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
                // 投げたあとは手を振る
                float wave = (float) Math.sin(t * 0.9f + i);
                m.rightArm.xRot = -2.8f;
                m.rightArm.zRot = 0.4f + wave * 0.35f;
                m.leftArm.xRot = -2.8f;
                m.leftArm.zRot = -0.4f - wave * 0.35f;
                m.head.xRot = -0.6f;
            }
            float lean = thrown ? -6f : swing * 14f;
            s.player(m, skin, px + (thrown ? 0 : swing * 0.25f), bob * (i % 2 == 0 ? 1 : 0.6f), czs[i], -90f, lean, 0f);
            s.shadow(px, 0f, czs[i], 0.3f);
        }

        // ---- 追放される人（あお向けで担がれ、手足をばたばた）
        PlayerSkin es = ClientGame.skin(cast.ejected());
        PlayerModel<?> em = Scene3D.model(es);
        float flail = (float) Math.sin(t * 1.3f);
        em.rightArm.xRot = -1.5f + flail * 0.8f;
        em.leftArm.xRot = -1.5f - flail * 0.8f;
        em.rightLeg.xRot = flail * 0.6f;
        em.leftLeg.xRot = -flail * 0.6f;
        em.head.xRot = -0.3f;
        if (!thrown) {
            // あお向け：足が +X、頭が -X
            float ex = cx + 0.9f + swing;
            float ey = 2.05f + bob + Math.abs(swing) * 0.15f;
            s.player(em, es, ex, ey, -0.5f, -90f, -90f, 0f);
        } else if (t < 124) {
            // くるくる回りながら飛んでいく
            float spin = tau * 32f;
            float scale = 1f;
            s.ps.pushPose();
            s.ps.translate(fx, fy, fz);
            s.ps.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(-spin));
            s.ps.mulPose(com.mojang.math.Axis.XP.rotationDegrees(spin * 0.4f));
            s.ps.scale(scale, scale, scale);
            s.ps.translate(0, -0.9f, 0);
            s.player(em, es, 0, 0, 0, -90f, 0f, 0f);
            s.ps.popPose();
        }
        float[] star = s.toScreen(Math.min(fx, 60f), Math.min(fy, 60f), fz);
        float[][] heads = new float[n][];
        for (int i = 0; i < n; i++) {
            heads[i] = s.toScreen(cx + cxs[i], 2.6f, czs[i]);
        }
        s.end();

        // ---- 文字・星（2D）
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
            Ui.text(g, Ui.serif("それーっ！！"), w / 2f, h * 0.2f, Ui.alpha(0xFFFFFFFF, 1f - (t - 76) / 14f), 3.2f, 0, true);
        }
        if (t >= 120 && star != null) {
            float st = t - 120;
            float pop = st < 3 ? st / 3f : st < 8 ? 1f + (8 - st) / 10f : 1f;
            float a = st < 30 ? 1f : Mth.clamp(1f - (st - 30) / 10f, 0f, 1f);
            float r = 10f * pop;
            float sx = star[0];
            float sy = star[1];
            star(g, sx, sy, r, st * 6f, Ui.alpha(0xFFFFF6B0, a));
            star(g, sx, sy, r * 0.45f, st * 6f, Ui.alpha(0xFFFFFFFF, a));
            if (st < 20) {
                Ui.text(g, Ui.serif("キラーン☆"), sx + 18, sy - 18, Ui.alpha(0xFFFFFFFF, a), 1.6f, -1, true);
            }
        }
        // 結果
        if (t >= 128) {
            result(g, t - 128, cast.text(), w, h);
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
        float a = Mth.clamp(rt / 5f, 0f, 1f);
        int bandH = 44;
        int y = (int) (h * 0.62f);
        g.fill(0, y, w, y + bandH, Ui.alpha(0xEE140C06, a));
        g.fill(0, y, w, y + 2, Ui.alpha(wolf ? 0xFFC8372D : 0xFFC9A14A, a));
        g.fill(0, y + bandH - 2, w, y + bandH, Ui.alpha(wolf ? 0xFFC8372D : 0xFFC9A14A, a));
        int shown = Mth.clamp((int) (rt / 1.6f), 0, text.length());
        Ui.text(g, Ui.serif(text.substring(0, shown)), w / 2f, y + 12, Ui.alpha(wolf ? 0xFFFF6A5A : 0xFFFFFFFF, a), 2.2f, 0,
                true);
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
        for (int x = 7; x <= 28; x += 2) {
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
        s.block(Blocks.STONE_BRICK_WALL.defaultBlockState(), 12, 0, -5);
        s.block(Blocks.LANTERN.defaultBlockState(), 12, 1, -5);
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
        // 池の鯉（ゆっくり回る）
        for (int i = 0; i < 2; i++) {
            double ang = t * 0.035 + i * Math.PI;
            float kx = 2.5f + (float) Math.cos(ang) * 0.75f;
            float kz = 0.5f + (float) Math.sin(ang) * 1.2f;
            s.ps.pushPose();
            s.ps.translate(kx, -0.42f, kz);
            s.ps.mulPose(com.mojang.math.Axis.YP.rotationDegrees((float) Math.toDegrees(-ang)));
            String body = i == 0 ? "minecraft:block/orange_concrete" : "minecraft:block/white_concrete";
            s.box(Scene3D.sprite(body), -0.08f, -0.05f, -0.2f, 0.08f, 0.05f, 0.2f, 0xFFFFFFFF);
            s.box(Scene3D.sprite("minecraft:block/red_concrete"), -0.06f, 0.05f, -0.08f, 0.06f, 0.07f, 0.06f, 0xFFFFFFFF);
            s.box(Scene3D.sprite(body), -0.1f, -0.02f, 0.2f, 0.1f, 0.02f, 0.3f, 0xFFFFFFFF);
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
            Ui.text(g, Ui.serif("カコーン"), hit[0] - 6, hit[1] - 40 - st * 0.6f, Ui.alpha(0xFFFFFFFF, a), 2.4f, 0, true);
        }
        if (t >= 80) {
            result(g, t - 80, cast.text(), w, h);
        }
    }
}
