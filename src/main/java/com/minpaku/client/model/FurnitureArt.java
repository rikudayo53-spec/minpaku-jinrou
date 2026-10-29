package com.minpaku.client.model;

import com.minpaku.game.MarkerType;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.util.Mth;

/**
 * 家具の見た目（マイクラのブロックのテクスチャを貼った箱で作る。形はリアル寄り）と、タスク中の動き。
 * 座標はピクセル（1ブロック = 16）。ブロックの角が原点、正面は北（-Z）。1ブロックより少し大きくてもよい。
 */
public final class FurnitureArt {
    private FurnitureArt() {
    }

    /**
     * 動きの状態。
     * work = 作業の進み具合（0〜1）、since = 最後に手を動かしてからの tick、time = 時刻（tick）
     */
    public record Anim(float work, float since, float time) {
        public static final Anim IDLE = new Anim(0f, 9999f, 0f);

        /** 今まさに作業中か（ミニゲームは画面を開いているあいだ長めに） */
        public boolean active(MarkerType t) {
            boolean mini = t.use == MarkerType.Use.WIRES || t.use == MarkerType.Use.NUMBERS || t.use == MarkerType.Use.HOLES
                    || t == MarkerType.BREAKER;
            return since < (mini ? 240 : 25) && !(mini && work >= 1f);
        }

        /** 手を動かした瞬間の動き（1→0 に減る） */
        public float hit(float len) {
            return since < len ? 1f - since / len : 0f;
        }
    }

    /** 描くための道具 */
    static final class Art {
        final PoseStack ps;
        final VertexConsumer solid;
        final VertexConsumer clear;
        final int light;

        Art(PoseStack ps, MultiBufferSource buf, int light) {
            this.ps = ps;
            this.solid = buf.getBuffer(RenderType.entityCutoutNoCull(TextureAtlas.LOCATION_BLOCKS));
            this.clear = buf.getBuffer(RenderType.entityTranslucentCull(TextureAtlas.LOCATION_BLOCKS));
            this.light = light;
        }

        void box(String tex, float x0, float y0, float z0, float x1, float y1, float z1) {
            tint(tex, x0, y0, z0, x1, y1, z1, 0xFFFFFFFF);
        }

        void tint(String tex, float x0, float y0, float z0, float x1, float y1, float z1, int argb) {
            Cubes.box(ps.last(), solid, Cubes.sprite(tex), x0 / 16f, y0 / 16f, z0 / 16f, x1 / 16f, y1 / 16f, z1 / 16f, argb, light);
        }

        /** 半透明（水・湯気など） */
        void glass(String tex, float x0, float y0, float z0, float x1, float y1, float z1, int argb) {
            Cubes.box(ps.last(), clear, Cubes.sprite(tex), x0 / 16f, y0 / 16f, z0 / 16f, x1 / 16f, y1 / 16f, z1 / 16f, argb, light);
        }

        /** 光る（火など） */
        void glow(String tex, float x0, float y0, float z0, float x1, float y1, float z1, int argb) {
            Cubes.box(ps.last(), clear, Cubes.sprite(tex), x0 / 16f, y0 / 16f, z0 / 16f, x1 / 16f, y1 / 16f, z1 / 16f, argb,
                    LightTexture.FULL_BRIGHT);
        }

        void push() {
            ps.pushPose();
        }

        void pop() {
            ps.popPose();
        }

        /** ピクセル座標の点 (px,py,pz) を中心に回す。axis = 'x' / 'y' / 'z' */
        void rot(char axis, float deg, float px, float py, float pz) {
            ps.translate(px / 16f, py / 16f, pz / 16f);
            ps.mulPose(switch (axis) {
                case 'x' -> Axis.XP.rotationDegrees(deg);
                case 'y' -> Axis.YP.rotationDegrees(deg);
                default -> Axis.ZP.rotationDegrees(deg);
            });
            ps.translate(-px / 16f, -py / 16f, -pz / 16f);
        }

        void move(float dx, float dy, float dz) {
            ps.translate(dx / 16f, dy / 16f, dz / 16f);
        }

        void scale(float s, float px, float py, float pz) {
            ps.translate(px / 16f, py / 16f, pz / 16f);
            ps.scale(s, s, s);
            ps.translate(-px / 16f, -py / 16f, -pz / 16f);
        }
    }

    private static float smooth(float x) {
        x = Mth.clamp(x, 0f, 1f);
        return x * x * (3 - 2 * x);
    }

    /** 湯気（下から上へ。active のときだけ） */
    private static void steam(Art a, float time, float x, float y, float z, float amount) {
        if (amount <= 0.01f) {
            return;
        }
        for (int k = 0; k < 6; k++) {
            float ph = ((time * 0.7f + k * 6.7f) % 40f) / 40f;
            float sz = 1.2f + ph * 3f;
            float dx = (float) Math.sin(time * 0.05f + k * 2.1f) * 2f * ph;
            int alpha = (int) (amount * (1f - ph) * 150);
            if (alpha <= 4) {
                continue;
            }
            a.glass("white_concrete", x + dx - sz / 2, y + ph * 16, z - sz / 2 + (k % 3 - 1) * 1.5f, x + dx + sz / 2,
                    y + ph * 16 + sz, z + sz / 2 + (k % 3 - 1) * 1.5f, (alpha << 24) | 0xFFFFFF);
        }
    }

    /** 1つの家具を描く（ブロックの角が原点、まだ向きは回していない状態で呼ぶ） */
    public static void draw(MarkerType type, Anim an, PoseStack ps, MultiBufferSource buf, int light) {
        Art a = new Art(ps, buf, light);
        float w = an.work();
        float t = an.time();
        boolean act = an.active(type);
        switch (type) {
            case SPAWN -> entranceMat(a);
            case BELL -> bell(a, an, t);
            case VENT -> hatch(a, an);
            case BREAKER -> breaker(a, w, act, t);
            case GAS_VALVE -> gasValve(a, w, act, t);
            case BOILER -> boiler(a, w, act, t);
            case WIRING -> wiring(a, w, act);
            case LEDGER -> ledger(a, act, t);
            case FUTON -> futon(a, w);
            case KAMADO -> kamado(a, w, act, t);
            case BATH -> bath(a, w, act, t);
            case CHOPPING -> chopping(a, w, an);
            case FIELD -> field(a, w, an);
            case DUST -> dust(a, w, act, t);
            case LAUNDRY -> laundry(a, w, t);
            case VASE -> vase(a, w, an, act, t);
            case SINK -> sink(a, w, act, t);
            case SHOE_RACK -> shoes(a, w, an);
            case SHOJI -> shoji(a, w, act, t);
        }
    }

    // ================================================================ 設備

    private static void entranceMat(Art a) {
        a.box("red_wool", -3, 0, 1, 19, 0.8f, 15);
        a.box("yellow_wool", -3, 0, 1, 19, 1f, 2);
        a.box("yellow_wool", -3, 0, 14, 19, 1f, 15);
        a.box("yellow_wool", -3, 0, 2, -2, 1f, 14);
        a.box("yellow_wool", 18, 0, 2, 19, 1f, 14);
        // 下駄
        for (float x : new float[]{2, 9.5f}) {
            a.box("spruce_planks", x, 1, 4, x + 4, 2, 11);
            a.box("dark_oak_planks", x + 0.5f, 0.8f, 5, x + 3.5f, 1, 6);
            a.box("dark_oak_planks", x + 0.5f, 0.8f, 9, x + 3.5f, 1, 10);
            a.box("red_wool", x + 0.4f, 2, 6, x + 3.6f, 2.8f, 7);
            a.box("red_wool", x + 1.6f, 2, 4.8f, x + 2.4f, 2.6f, 6.2f);
        }
    }

    private static void bell(Art a, Anim an, float t) {
        a.box("polished_andesite", -3, 0, 3, 19, 2, 12);
        a.box("dark_oak_log", -2, 2, 6, 1, 30, 9);
        a.box("dark_oak_log", 15, 2, 6, 18, 30, 9);
        a.box("dark_oak_planks", -3, 27, 5, 19, 30, 10);
        a.box("deepslate_tiles", -5, 30, 2, 21, 31.5f, 13);
        a.box("deepslate_tiles", -3, 31.5f, 4, 19, 33, 11);
        // 鳴らされたらゆれる
        float swing = an.since() < 50 ? (float) Math.sin(an.since() * 0.45f) * 22f * (1f - an.since() / 50f) : 0f;
        a.push();
        a.rot('x', swing, 8, 27, 7.5f);
        a.box("chain", 7.5f, 23, 7, 8.5f, 27, 8);
        a.box("weathered_copper", 4, 13, 3.5f, 12, 23, 11.5f);
        a.box("copper_block", 3, 12, 2.5f, 13, 14, 12.5f);
        a.box("weathered_copper", 5.5f, 23, 5, 10.5f, 24.5f, 10);
        a.box("copper_block", 3.8f, 17, 3.3f, 12.2f, 18, 11.7f);
        a.pop();
        // 撞木（打つ木）
        a.push();
        float strike = an.hit(10) * 20f;
        a.rot('x', -strike, 8, 27, 7.5f);
        a.box("chain", 7.7f, 18, -2, 8.3f, 27, -1.4f);
        a.box("oak_log", 6, 16, -6, 10, 18, 0);
        a.pop();
    }

    private static void hatch(Art a, Anim an) {
        a.box("dark_oak_planks", 0, 0, 0, 16, 1.5f, 2);
        a.box("dark_oak_planks", 0, 0, 14, 16, 1.5f, 16);
        a.box("dark_oak_planks", 0, 0, 2, 2, 1.5f, 14);
        a.box("dark_oak_planks", 14, 0, 2, 16, 1.5f, 14);
        a.box("black_concrete", 2, 0.1f, 2, 14, 0.4f, 14);
        // ベントを使うとふたが開く
        float open = an.since() < 30 ? smooth(an.since() < 6 ? an.since() / 6f : (30 - an.since()) / 10f) : 0f;
        a.push();
        a.rot('x', -open * 75f, 8, 1.5f, 14);
        a.box("spruce_trapdoor", 2, 0.5f, 2, 14, 1.5f, 14);
        a.box("iron_block", 7, 1.5f, 3, 9, 2.2f, 4.2f);
        a.pop();
    }

    private static void breaker(Art a, float w, boolean act, float t) {
        a.box("polished_blackstone", 2, 0, 6, 4, 6, 12);
        a.box("polished_blackstone", 12, 0, 6, 14, 6, 12);
        a.box("light_gray_concrete", 1, 6, 4, 15, 26, 14);
        a.box("iron_block", 7, 26, 8, 9, 34, 10);
        // 中のスイッチ
        a.box("black_concrete", 2, 7, 4.6f, 14, 25, 5);
        for (int i = 0; i < 4; i++) {
            a.box("white_concrete", 3 + i * 3, 19, 4.2f, 5 + i * 3, 23, 4.8f);
        }
        float lever = smooth(w);
        a.push();
        a.rot('x', 50 - lever * 100, 8, 12, 4.4f);
        a.box("red_concrete", 7, 12, 1.4f, 9, 13, 4.6f);
        a.pop();
        a.box("iron_block", 6, 10, 3.8f, 10, 14, 4.6f);
        // 扉（直しているあいだ開く）
        float open = act || w >= 1f ? 1f : 0f;
        a.push();
        a.rot('y', -open * 115f, 1, 16, 4);
        a.box("iron_block", 1, 6, 3, 15, 26, 4);
        a.box("yellow_wool", 6, 17, 2.7f, 10, 21, 3);
        a.box("black_concrete", 7.5f, 17.5f, 2.6f, 8.5f, 20.5f, 2.7f);
        a.pop();
    }

    private static void gasValve(Art a, float w, boolean act, float t) {
        a.box("stone", 3, 0, 7, 13, 1, 15);
        a.box("cut_copper", 6, 1, 9, 10, 20, 13);
        a.box("cut_copper", -4, 16, 9, 20, 20, 13);
        a.box("copper_block", 5, 15, 8, 11, 21, 14);
        a.box("iron_block", 7, 17, 4, 9, 19, 8);
        a.box("white_concrete", 12, 21, 9, 15, 24, 10);
        a.box("red_concrete", 13.2f, 22, 8.8f, 13.8f, 23.6f, 9);
        // ハンドル（閉めると回る）
        float ang = w * 540f + (act ? (float) Math.sin(t * 0.6f) * 3f : 0f);
        a.push();
        a.rot('z', ang, 8, 18, 4);
        a.box("red_concrete", 2.5f, 17.4f, 3, 13.5f, 18.6f, 4);
        a.box("red_concrete", 7.4f, 12.5f, 3, 8.6f, 23.5f, 4);
        a.box("red_concrete", 3, 12.5f, 3, 13, 13.5f, 4);
        a.box("red_concrete", 3, 22.5f, 3, 13, 23.5f, 4);
        a.box("red_concrete", 2.5f, 13, 3, 3.5f, 23, 4);
        a.box("red_concrete", 12.5f, 13, 3, 13.5f, 23, 4);
        a.pop();
    }

    private static void boiler(Art a, float w, boolean act, float t) {
        a.push();
        if (act) {
            a.move((float) Math.sin(t * 2.3f) * 0.25f, 0, (float) Math.cos(t * 1.9f) * 0.25f);
        }
        a.box("iron_block", 2, 0, 2, 14, 26, 14);
        a.box("iron_block", 1, 2, 3, 15, 24, 13);
        a.box("iron_block", 3, 2, 1, 13, 24, 15);
        a.box("cut_copper", 1, 8, 1, 15, 9, 15);
        a.box("cut_copper", 1, 18, 1, 15, 19, 15);
        a.box("iron_block", 4, 26, 4, 12, 28, 12);
        a.box("polished_blackstone", 6, 28, 6, 10, 34, 10);
        // 火の窓（止めるほど暗くなる）
        a.box("black_concrete", 5, 2, 0.6f, 11, 6, 1);
        int fire = (int) ((1f - w) * 230) + 20;
        a.glow("fire_0", 5.5f, 2.3f, 0.4f, 10.5f, 5.7f, 0.6f, (fire << 24) | 0xFFFFFF);
        // 圧力計（止めると針が下がる）
        a.box("white_concrete", 5, 14, 0.4f, 11, 20, 1);
        float needle = 60 - smooth(w) * 120 + (act ? (float) Math.sin(t * 3.1f) * 6f : 0f);
        a.push();
        a.rot('z', needle, 8, 17, 0);
        a.box("red_concrete", 7.6f, 17, -0.1f, 8.4f, 19.6f, 0.4f);
        a.pop();
        a.pop();
    }

    // ================================================================ タスク（ミニゲーム）

    private static void wiring(Art a, float w, boolean act) {
        a.box("polished_blackstone", 2, 0, 7, 4, 4, 13);
        a.box("polished_blackstone", 12, 0, 7, 14, 4, 13);
        a.box("smooth_stone", 1, 4, 6, 15, 28, 14);
        a.box("black_concrete", 2, 5, 5.6f, 14, 27, 6);
        String[] c = {"red_wool", "blue_wool", "yellow_wool", "lime_wool"};
        for (int i = 0; i < 4; i++) {
            float y = 9 + i * 4.5f;
            a.box(c[i], 3, y, 5.2f, 6, y + 1, 5.8f);
            a.box(c[(i + 1 + (w >= 1 ? 3 : 0)) % 4], 10, y, 5.2f, 13, y + 1, 5.8f);
            if (w >= 1) {
                a.box(c[i], 6, y, 5.3f, 10, y + 1, 5.7f);
            }
        }
        a.glow(w >= 1 ? "lime_wool" : "red_wool", 12, 25, 4.6f, 14, 27, 5.2f, 0xFFFFFFFF);
        // 扉
        float open = act || w >= 1f ? 1f : 0f;
        a.push();
        a.rot('y', open * 110f, 15, 16, 5);
        a.box("light_gray_concrete", 1, 4, 5, 15, 28, 6);
        a.box("yellow_wool", 6, 18, 4.7f, 10, 22, 5);
        a.pop();
    }

    private static void ledger(Art a, boolean act, float t) {
        a.box("dark_oak_planks", -2, 9, 2, 18, 11, 14);
        a.box("dark_oak_planks", -1, 0, 3, 1, 9, 5);
        a.box("dark_oak_planks", 15, 0, 3, 17, 9, 5);
        a.box("dark_oak_planks", -1, 0, 11, 1, 9, 13);
        a.box("dark_oak_planks", 15, 0, 11, 17, 9, 13);
        a.box("dark_oak_planks", -1, 2, 4, 17, 3, 12);
        // 開いた帳面
        a.box("red_wool", 1, 11, 4, 15, 11.6f, 12);
        a.box("white_wool", 1.5f, 11.6f, 4.5f, 7.8f, 12.6f, 11.5f);
        a.box("white_wool", 8.2f, 11.6f, 4.5f, 14.5f, 12.6f, 11.5f);
        // めくれるページ
        float flip = act ? (t * 9f) % 180f : 0f;
        a.push();
        a.rot('z', flip, 8, 12.6f, 8);
        a.box("white_wool", 8.1f, 12.6f, 4.7f, 14.3f, 12.9f, 11.3f);
        a.pop();
        // 硯と筆、ろうそく
        a.box("polished_blackstone", 15, 11, 9, 17.5f, 12, 13);
        a.box("bamboo_block", 10, 12.6f, 2.5f, 16, 13.3f, 3.2f);
        a.box("black_concrete", 9, 12.6f, 2.5f, 10, 13.3f, 3.2f);
        a.box("white_wool", -1, 11, 10, 0.5f, 15, 11.5f);
        a.glow("shroomlight", -0.6f, 15, 10.4f, 0.1f, 16.5f, 11.1f, 0xFFFFFFFF);
    }

    private static void shoji(Art a, float w, boolean act, float t) {
        a.push();
        if (act) {
            a.rot('x', (float) Math.sin(t * 1.7f) * 0.8f, 8, 0, 8);
        }
        a.box("stripped_spruce_log", -1, 0, 6.5f, 1.5f, 32, 9.5f);
        a.box("stripped_spruce_log", 14.5f, 0, 6.5f, 17, 32, 9.5f);
        a.box("stripped_spruce_log", -1, 30, 6.5f, 17, 32.5f, 9.5f);
        a.box("stripped_spruce_log", -1, 0, 6.5f, 17, 2, 9.5f);
        a.box("white_wool", 1.5f, 2, 7.6f, 14.5f, 30, 8.4f);
        for (float x : new float[]{5, 8, 11}) {
            a.box("spruce_planks", x - 0.35f, 2, 7.3f, x + 0.35f, 30, 8.7f);
        }
        for (float y = 6; y < 30; y += 4.5f) {
            a.box("spruce_planks", 1.5f, y - 0.35f, 7.3f, 14.5f, y + 0.35f, 8.7f);
        }
        // 穴 → ふさぐと桜の形の紙
        float[][] holes = {{3, 9}, {9.5f, 14}, {6, 20.5f}, {12, 25}};
        for (float[] h : holes) {
            if (w < 1f) {
                a.box("black_concrete", h[0] - 0.9f, h[1] - 0.9f, 7.5f, h[0] + 0.9f, h[1] + 0.9f, 8.5f);
            } else {
                a.box("pink_wool", h[0] - 1.3f, h[1] - 1.3f, 7.25f, h[0] + 1.3f, h[1] + 1.3f, 8.75f);
                a.box("pink_wool", h[0] - 0.5f, h[1] - 1.9f, 7.25f, h[0] + 0.5f, h[1] + 1.9f, 8.75f);
                a.box("pink_wool", h[0] - 1.9f, h[1] - 0.5f, 7.25f, h[0] + 1.9f, h[1] + 0.5f, 8.75f);
            }
        }
        a.pop();
    }

    // ================================================================ タスク（マイクラらしい作業）

    private static void futon(Art a, float w) {
        a.box("green_terracotta", -2, 0, -6, 18, 0.5f, 22);
        float a1 = 180f * (1f - smooth(w * 2f));
        float a2 = -180f * (1f - smooth(w * 2f - 1f));
        // 奥の1枚
        futonSeg(a, 12, 20);
        // まくら（敷き終わると出る）
        if (w > 0.9f) {
            float s = smooth((w - 0.9f) * 10f);
            a.push();
            a.scale(s, 8, 2, 17);
            a.box("white_wool", 4, 2, 15, 12, 4.5f, 19.5f);
            a.box("light_blue_wool", 4, 2.2f, 16.5f, 12, 4.7f, 17);
            a.pop();
        }
        a.push();
        a.rot('x', -a1, 8, 2, 12);
        futonSeg(a, 4, 12);
        a.push();
        a.rot('x', -a2, 8, 0, 4);
        futonSeg(a, -4, 4);
        a.pop();
        a.pop();
    }

    private static void futonSeg(Art a, float z0, float z1) {
        a.box("white_wool", 1, 0.5f, z0, 15, 2, z1);
        a.box("blue_wool", 1.2f, 2, z0 + 0.2f, 14.8f, 2.4f, z1 - 0.2f);
        a.box("light_blue_wool", 6, 2.05f, z0 + 2, 10, 2.45f, z1 - 2);
    }

    private static void kamado(Art a, float w, boolean act, float t) {
        a.box("white_terracotta", -4, 0, 2, 20, 12, 15);
        a.box("terracotta", -4.3f, 0, 1.7f, 20.3f, 1.5f, 15.3f);
        a.box("terracotta", -4.3f, 11, 1.7f, 20.3f, 12.3f, 15.3f);
        // 焚き口と火
        int fire = (int) (90 + 165 * (act ? 1f : w * 0.5f));
        for (float x : new float[]{-1, 11}) {
            a.box("black_concrete", x, 2, 1.7f, x + 6, 7, 2);
            a.glow("fire_0", x + 0.6f, 2.2f, 1.5f, x + 5.4f, 6.8f, 1.7f, (Math.min(255, fire) << 24) | 0xFFFFFF);
        }
        // 羽釜（ふたがゆれる）
        a.box("polished_blackstone", -2, 12, 4, 7, 17, 13);
        a.box("polished_blackstone", -3, 14, 3, 8, 15, 14);
        float bounce = act ? Math.abs((float) Math.sin(t * 0.9f)) * 1.2f : 0f;
        a.push();
        a.move(0, bounce, 0);
        a.rot('z', act ? (float) Math.sin(t * 0.9f) * 3f : 0f, 2.5f, 17, 8.5f);
        a.box("spruce_planks", -1.5f, 17, 4.5f, 6.5f, 18, 12.5f);
        a.box("dark_oak_planks", 1.5f, 18, 7.5f, 3.5f, 19.3f, 9.5f);
        a.pop();
        // 小さい鍋
        a.box("cut_copper", 10, 12, 5, 17, 16, 12);
        a.box("cut_copper", 9.5f, 15, 4.5f, 17.5f, 16, 12.5f);
        a.box("spruce_planks", 10.5f, 16, 5.5f, 16.5f, 16.8f, 11.5f);
        // 薪
        a.box("oak_log", -3, 0, -3, 5, 2, -1);
        a.box("birch_log", 0, 2, -3, 7, 3.8f, -1.2f);
        steam(a, t, 2.5f, 18, 8.5f, act ? 1f : 0f);
    }

    private static void bath(Art a, float w, boolean act, float t) {
        // 檜の湯船
        a.box("birch_planks", -2, 0, 0, 18, 12, 2);
        a.box("birch_planks", -2, 0, 14, 18, 12, 16);
        a.box("birch_planks", -2, 0, 2, 0, 12, 14);
        a.box("birch_planks", 16, 0, 2, 18, 12, 14);
        a.box("birch_planks", 0, 0, 2, 16, 1, 14);
        for (float y : new float[]{3, 9}) {
            a.box("iron_block", -2.2f, y, -0.2f, 18.2f, y + 0.8f, 0.2f);
            a.box("iron_block", -2.2f, y, 15.8f, 18.2f, y + 0.8f, 16.2f);
        }
        float lvl = 9 + w * 1.5f + (act ? (float) Math.sin(t * 0.5f) * 0.2f : 0f);
        a.glass("water_still", 0, 1, 2, 16, lvl, 14, 0xD03F76E4);
        // 焚き口
        a.box("bricks", 5, 0, -4, 11, 5, 0);
        a.box("black_concrete", 6, 1, -4.2f, 10, 4, -3.9f);
        int fire = (int) (60 + 195 * Math.max(w, act ? 0.8f : 0f));
        a.glow("fire_0", 6.3f, 1.2f, -4.4f, 9.7f, 3.8f, -4.2f, (fire << 24) | 0xFFFFFF);
        a.box("polished_blackstone", 17, 0, 10, 20, 20, 13);
        // 手桶
        a.box("spruce_planks", 19, 0, 1, 23, 4, 5);
        a.box("bamboo_block", 20.5f, 4, 2.5f, 21.5f, 7, 3.5f);
        steam(a, t, 8, lvl, 8, Math.max(w, act ? 1f : 0f));
    }

    private static void chopping(Art a, float w, Anim an) {
        a.box("oak_log", 2, 0, 2, 14, 7.8f, 14);
        a.box("oak_log_top", 2, 7.8f, 2, 14, 8, 14);
        // 割れる丸太（割るほど左右に倒れる）
        float open = smooth(w);
        a.push();
        a.rot('z', open * 70f, 5, 8, 8);
        a.box("birch_log", 5, 8, 5, 8, 16, 11);
        a.box("birch_log_top", 5, 16, 5, 8, 16.1f, 11);
        a.box("stripped_birch_log", 7.9f, 8, 5.1f, 8, 16, 10.9f);
        a.pop();
        a.push();
        a.rot('z', -open * 70f, 11, 8, 8);
        a.box("birch_log", 8, 8, 5, 11, 16, 11);
        a.box("birch_log_top", 8, 16, 5, 11, 16.1f, 11);
        a.box("stripped_birch_log", 8, 8, 5.1f, 8.1f, 16, 10.9f);
        a.pop();
        // 斧（手を動かすたびに振り下ろす）
        float swing = an.since() < 3 ? an.since() / 3f : an.since() < 10 ? 1f - (an.since() - 3) / 7f : 0f;
        a.push();
        a.rot('z', 50 + swing * 22, 21, 9, 8);
        a.box("oak_planks", 20.4f, 9, 7.4f, 21.6f, 23, 8.6f);
        a.box("iron_block", 16.5f, 19.5f, 7.2f, 21.5f, 23.5f, 8.8f);
        a.box("iron_block", 15.5f, 20.2f, 7.5f, 16.5f, 22.8f, 8.5f);
        a.pop();
        // 割った薪の山
        a.box("birch_log", -4, 0, 10, 1, 2.5f, 18);
        a.box("birch_log", -3.5f, 2.5f, 11, 1.5f, 5, 17);
        a.box("oak_log", -4.5f, 0, 2, 0, 2.5f, 9);
    }

    private static void field(Art a, float w, Anim an) {
        a.box("farmland_moist", -4, 0, -2, 20, 2.5f, 18);
        a.box("coarse_dirt", -3.5f, 2.5f, 0.5f, 19.5f, 4, 6.5f);
        a.box("coarse_dirt", -3.5f, 2.5f, 9.5f, 19.5f, 4, 15.5f);
        float pulled = w * 8f;
        int k = 0;
        for (float z : new float[]{2, 11}) {
            for (float x : new float[]{-2, 3, 8, 13}) {
                boolean daikon = (k % 2 == 0);
                float rise = Mth.clamp(pulled - k, 0f, 1f);
                float pop = (rise > 0 && rise < 1) ? an.hit(8) : 0f;
                a.push();
                a.move(0, rise * 6f + pop * 1.5f, 0);
                if (rise >= 1f) {
                    // 抜いたものは手前に寝かせる
                    a.rot('x', -80, x + 1.5f, 4, z + 1.5f);
                }
                String root = daikon ? "white_concrete" : "orange_concrete";
                a.box(root, x, -1, z, x + 3, 4.5f, z + 3);
                a.box(root, x + 0.8f, -2.5f, z + 0.8f, x + 2.2f, -1, z + 2.2f);
                a.tint("oak_leaves", x + 0.2f, 4.5f, z + 0.2f, x + 2.8f, 8.5f, z + 2.8f, 0xFF59AE30);
                a.tint("oak_leaves", x - 0.8f, 6.5f, z + 1, x + 3.8f, 9, z + 2, 0xFF6BBF3A);
                a.pop();
                k++;
            }
        }
        // ざる
        a.box("bamboo_planks", 5, 0, -6, 13, 1.5f, -2);
    }

    private static void dust(Art a, float w, boolean act, float t) {
        float left = 1f - smooth(w);
        if (left > 0.02f) {
            a.box("gray_concrete_powder", 1, 0, 3, 9, 0.3f + 2.2f * left, 9);
            a.box("light_gray_concrete_powder", 2.5f, 0, 4.5f, 7.5f, 0.3f + 3.5f * left, 7.5f);
            a.box("gray_concrete_powder", 9, 0, 8, 13, 0.2f + 1.4f * left, 12);
            a.box("light_gray_concrete_powder", -2, 0, 9, 2, 0.2f + 1.0f * left, 13);
        }
        // ちりとり
        a.box("light_gray_concrete", 12, 0, 1, 18, 0.6f, 6);
        a.box("light_gray_concrete", 12, 0.6f, 5.4f, 18, 3, 6);
        a.box("bamboo_block", 14.5f, 1, 6, 15.5f, 2, 11);
        // ほうき（作業中ははく）
        float sweep = act ? (float) Math.sin(t * 0.8f) * 28f : 0f;
        a.push();
        a.rot('y', sweep, 8, 22, 9);
        a.rot('z', 18, 8, 0, 9);
        a.box("bamboo_block", 7.5f, 5, 8.5f, 8.5f, 26, 9.5f);
        a.box("hay_block_side", 4.5f, 0, 7.5f, 11.5f, 6, 10.5f);
        a.box("red_wool", 7, 5, 8, 9, 6, 10);
        a.pop();
        if (act) {
            for (int i = 0; i < 4; i++) {
                float ph = ((t * 0.9f + i * 5) % 20) / 20f;
                a.glass("light_gray_concrete_powder", 4 + i * 2 + ph * 3, 1 + ph * 6, 5 + i, 5 + i * 2 + ph * 3, 2 + ph * 6, 6 + i,
                        ((int) ((1 - ph) * 180) << 24) | 0xFFFFFF);
            }
        }
    }

    private static void laundry(Art a, float w, float t) {
        a.box("stone", -4, 0, 6, 0, 2, 10);
        a.box("stone", 16, 0, 6, 20, 2, 10);
        a.box("bamboo_block", -3, 2, 7, -1, 30, 9);
        a.box("bamboo_block", 17, 2, 7, 19, 30, 9);
        a.box("bamboo_block", -3, 26, 7.4f, 19, 27.5f, 8.6f);
        // 干す物（作業が進むと1枚ずつ増える）
        String[] cloth = {"white_wool", "light_blue_wool", "pink_wool", "blue_wool"};
        float[][] span = {{-0.5f, 3.5f, 9}, {4.5f, 10.5f, 11}, {11.5f, 14.5f, 8}, {15, 17.5f, 12}};
        int hung = (int) (w * 4.999f);
        for (int i = 0; i < 4; i++) {
            if (i >= hung) {
                continue;
            }
            float sway = (float) Math.sin(t * 0.08f + i * 1.7f) * 7f;
            a.push();
            a.rot('x', sway, 0, 26, 8);
            float[] s = span[i];
            a.box(cloth[i], s[0], 26 - s[2], 7.6f, s[1], 26, 8.4f);
            if (i == 1) {
                a.box(cloth[i], s[0] - 2, 22, 7.6f, s[0], 26, 8.4f);
                a.box(cloth[i], s[1], 22, 7.6f, s[1] + 2, 26, 8.4f);
            }
            a.box("oak_planks", s[0] + 0.5f, 26, 7.4f, s[0] + 1.3f, 27.8f, 8.6f);
            a.pop();
        }
        // かご（干すと減る）
        a.box("bamboo_planks", 2, 0, 11, 11, 4, 18);
        float pile = 1f - w;
        if (pile > 0.05f) {
            a.box("light_blue_wool", 2.8f, 4, 11.8f, 10.2f, 4 + 2.5f * pile, 17.2f);
            a.box("pink_wool", 4, 4, 13, 8, 4 + 3.2f * pile, 16);
        }
    }

    private static void vase(Art a, float w, Anim an, boolean act, float t) {
        a.box("dark_oak_planks", 1, 0, 1, 15, 3, 15);
        a.box("dark_oak_planks", 0, 3, 0, 16, 4, 16);
        a.box("light_blue_terracotta", 5, 4, 5, 11, 12, 11);
        a.box("light_blue_terracotta", 4.5f, 6, 4.5f, 11.5f, 10, 11.5f);
        a.box("light_blue_terracotta", 6, 12, 6, 10, 15, 10);
        a.box("white_terracotta", 5.5f, 15, 5.5f, 10.5f, 16, 10.5f);
        a.box("white_terracotta", 4.4f, 8, 4.4f, 11.6f, 9, 11.6f);
        // 花（水をやるほど伸びて咲く）
        float grow = smooth(w);
        String[] head = {"pink_wool", "red_wool", "yellow_wool"};
        float[][] pos = {{7.2f, 7.2f, -12}, {8.6f, 8.4f, 10}, {7.6f, 9.2f, 0}};
        for (int i = 0; i < 3; i++) {
            float len = 5 + grow * 8 + i;
            float droop = (1f - grow) * 28f + (float) Math.sin(t * 0.06f + i) * 2f;
            a.push();
            a.rot('z', pos[i][2] + (i == 0 ? -droop : i == 1 ? droop : 0), pos[i][0], 15, pos[i][1]);
            a.rot('x', i == 2 ? droop : 0, pos[i][0], 15, pos[i][1]);
            a.box("moss_block", pos[i][0] - 0.3f, 15, pos[i][1] - 0.3f, pos[i][0] + 0.3f, 15 + len, pos[i][1] + 0.3f);
            a.tint("oak_leaves", pos[i][0] + 0.3f, 15 + len * 0.5f, pos[i][1] - 0.5f, pos[i][0] + 2.3f, 15 + len * 0.5f + 1,
                    pos[i][1] + 0.5f, 0xFF59AE30);
            float hs = 1 + grow * 2;
            a.box(head[i], pos[i][0] - hs, 15 + len, pos[i][1] - hs, pos[i][0] + hs, 15 + len + hs * 1.2f, pos[i][1] + hs);
            a.box("yellow_wool", pos[i][0] - 0.5f, 15 + len + hs * 1.2f - 0.2f, pos[i][1] - 0.5f, pos[i][0] + 0.5f,
                    15 + len + hs * 1.2f + 0.4f, pos[i][1] + 0.5f);
            a.pop();
        }
        // じょうろ（水やり中）
        if (act) {
            float tilt = 25 + an.hit(10) * 15;
            a.push();
            a.rot('z', tilt, 16, 22, 8);
            a.box("cut_copper", 13, 19, 6, 19, 24, 10);
            a.box("cut_copper", 9, 21, 7.4f, 13, 22.2f, 8.6f);
            a.box("cut_copper", 16, 24, 7.6f, 17, 26, 8.4f);
            a.pop();
            for (int i = 0; i < 5; i++) {
                float ph = ((t * 1.3f + i * 3) % 12) / 12f;
                a.glass("water_still", 9 + i * 0.4f, 21 - ph * 7, 7.6f + (i % 2) * 0.6f, 9.5f + i * 0.4f, 21.8f - ph * 7,
                        8.2f + (i % 2) * 0.6f, 0xC06FA8FF);
            }
        }
    }

    private static void sink(Art a, float w, boolean act, float t) {
        a.box("spruce_planks", -2, 0, 2, 18, 14, 16);
        a.box("dark_oak_planks", 7.7f, 1, 1.7f, 8.3f, 13, 2);
        a.box("dark_oak_planks", 6, 7, 1.6f, 7, 8, 1.9f);
        a.box("dark_oak_planks", 9, 7, 1.6f, 10, 8, 1.9f);
        a.box("smooth_stone", -3, 14, 1, 19, 15, 17);
        // 流し（石の槽）
        a.box("iron_block", 0, 15, 4, 12, 18, 5);
        a.box("iron_block", 0, 15, 13, 12, 18, 14);
        a.box("iron_block", 0, 15, 5, 1, 18, 13);
        a.box("iron_block", 11, 15, 5, 12, 18, 13);
        a.glass("water_still", 1, 15, 5, 11, 16.5f, 13, 0xC03F76E4);
        // 筧から水
        a.box("bamboo_block", 13, 15, 7, 15, 27, 9);
        a.box("bamboo_block", 5, 25, 7.4f, 15, 26.4f, 8.6f);
        if (act) {
            a.glass("water_still", 5.5f, 16, 7.7f, 6.5f, 25, 8.3f, 0xB06FA8FF);
        }
        // お皿：洗うほど流しから水切りかごへ移る
        int total = 5;
        int clean = Math.round(w * total);
        for (int i = 0; i < total - clean; i++) {
            float wob = (act && i == total - clean - 1) ? (float) Math.sin(t * 1.4f) * 8f : 0f;
            a.push();
            a.rot('z', wob, 6, 16 + i * 0.8f, 9);
            a.box("white_concrete", 2.5f + (i % 2) * 0.6f, 16 + i * 0.8f, 6.5f, 9.5f + (i % 2) * 0.6f, 16.6f + i * 0.8f, 11.5f);
            a.box("light_blue_wool", 4.5f, 16.6f + i * 0.8f, 8.5f, 7.5f, 16.65f + i * 0.8f, 9.5f);
            a.pop();
        }
        a.box("bamboo_planks", 13, 15, 10, 19, 16, 16.5f);
        for (int i = 0; i < clean; i++) {
            a.box("white_concrete", 13.5f + i * 1.1f, 16, 10.8f, 14.1f + i * 1.1f, 22, 15.8f);
        }
    }

    private static void shoes(Art a, float w, Anim an) {
        a.box("spruce_planks", 0, 0, 15, 16, 22, 16);
        a.box("spruce_planks", 0, 0, 4, 1, 22, 15);
        a.box("spruce_planks", 15, 0, 4, 16, 22, 15);
        a.box("spruce_planks", 0, 21, 3.5f, 16, 22.5f, 16);
        a.box("spruce_planks", 0, 0, 4, 16, 1, 15);
        a.box("spruce_planks", 1, 10.5f, 4.5f, 15, 11.5f, 15);
        a.box("spruce_planks", 7.5f, 1, 4.5f, 8.5f, 21, 15);
        a.box("dark_oak_planks", 1, 1, 14.5f, 15, 21, 15);
        // 上の飾り
        a.box("light_blue_terracotta", 11, 22.5f, 8, 14, 26, 11);
        a.tint("oak_leaves", 10.5f, 26, 7.5f, 14.5f, 29, 11.5f, 0xFF59AE30);
        a.box("red_wool", 2, 22.5f, 7, 7, 23, 12);
        // 靴（そろえるほどまっすぐ・奥へ）
        String[][] pairs = {{"spruce_planks", "red_wool"}, {"white_wool", "blue_wool"}, {"brown_wool", "brown_wool"},
                {"pink_wool", "white_wool"}};
        float[][] cub = {{1.5f, 1}, {9, 1}, {1.5f, 11.5f}, {9, 11.5f}};
        float neat = smooth(w);
        float jig = an.hit(6);
        for (int i = 0; i < 4; i++) {
            for (int s = 0; s < 2; s++) {
                float seed = (i * 7 + s * 3) % 5 - 2;
                float yaw = (1f - neat) * seed * 18f + jig * seed * 3f;
                float out = (1f - neat) * (3 + Math.abs(seed));
                float x = cub[i][0] + s * 3.3f;
                float y = cub[i][1];
                a.push();
                a.move(0, 0, -out);
                a.rot('y', yaw, x + 1.3f, y, 9);
                a.box(pairs[i][0], x, y, 6, x + 2.6f, y + 1.6f, 13);
                a.box(pairs[i][1], x + 0.2f, y + 1.6f, 6.2f, x + 2.4f, y + 2.8f, 9);
                a.pop();
            }
        }
    }
}
