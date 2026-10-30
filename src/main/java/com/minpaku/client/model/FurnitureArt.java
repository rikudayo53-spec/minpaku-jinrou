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
            boolean mini = t.miniGame() || t == MarkerType.BREAKER;
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
        final MultiBufferSource buf;
        final int light;

        Art(PoseStack ps, MultiBufferSource buf, int light) {
            this.ps = ps;
            this.buf = buf;
            this.light = light;
        }

        // 使うたびに取りに行く（同時に2つ取ると先の方が閉じられることがある）
        VertexConsumer solid() {
            return buf.getBuffer(RenderType.entityCutoutNoCull(TextureAtlas.LOCATION_BLOCKS));
        }

        VertexConsumer clear() {
            return buf.getBuffer(RenderType.entityTranslucentCull(TextureAtlas.LOCATION_BLOCKS));
        }

        void box(String tex, float x0, float y0, float z0, float x1, float y1, float z1) {
            tint(tex, x0, y0, z0, x1, y1, z1, 0xFFFFFFFF);
        }

        void tint(String tex, float x0, float y0, float z0, float x1, float y1, float z1, int argb) {
            Cubes.box(ps.last(), solid(), Cubes.sprite(tex), x0 / 16f, y0 / 16f, z0 / 16f, x1 / 16f, y1 / 16f, z1 / 16f, argb, light);
        }

        /** 半透明（水・湯気など） */
        void glass(String tex, float x0, float y0, float z0, float x1, float y1, float z1, int argb) {
            Cubes.box(ps.last(), clear(), Cubes.sprite(tex), x0 / 16f, y0 / 16f, z0 / 16f, x1 / 16f, y1 / 16f, z1 / 16f, argb, light);
        }

        /** 光る（火など） */
        void glow(String tex, float x0, float y0, float z0, float x1, float y1, float z1, int argb) {
            Cubes.box(ps.last(), clear(), Cubes.sprite(tex), x0 / 16f, y0 / 16f, z0 / 16f, x1 / 16f, y1 / 16f, z1 / 16f, argb,
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

    static float smooth(float x) {
        x = Mth.clamp(x, 0f, 1f);
        return x * x * (3 - 2 * x);
    }

    /** 湯気（下から上へ。active のときだけ） */
    static void steam(Art a, float time, float x, float y, float z, float amount) {
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

    /** 家具ごとの大きさ（足もとの中心を基準に大きくする） */
    public static float size(MarkerType type) {
        // 実物の大きさに近づける（大きいものは大きく）。1ブロック ≒ 1m、プレイヤーの背丈 ≒ 1.8m
        return switch (type) {
            case ZEN -> 1.6f;
            case KANE -> 1.5f;             // 半鐘の火の見やぐら
            case AMADO, MAKI -> 1.3f;
            case TAIKO, OZEN -> 1.2f;              // 枯山水：2.4m 四方
            case FIELD -> 1.45f;           // 畑
            case LAUNDRY, WELL, TOUROU -> 1.4f;   // 物干し 2.6m・井戸 2.4m・石灯籠 2.2m
            case BELL, KAMADO -> 1.35f;    // 鐘楼 3m・かまど 2.2m
            case BATH, FUTON, HOKORA, BYOBU -> 1.3f;
            case BOILER, SINK -> 1.25f;
            case CHOPPING, TEA, SHOGI, WASHTUB -> 1.2f; case TANSU -> 1.3f;
            case IRORI, MOCHI, CHABUDAI, SAKE, TSUBO -> 1.15f; case TANUKI -> 1.25f; case HIBACHI -> 1.1f;
            case SHOJI -> 1.05f;
            default -> 1f;
        };
    }

    /** 1つの家具を描く（ブロックの角が原点、まだ向きは回していない状態で呼ぶ） */
    public static void draw(MarkerType type, Anim an, PoseStack ps, MultiBufferSource buf, int light) {
        // 高精細モデル（Blockbench）があればそちらで描く
        if (FurnitureHiRes.draw(type, an, ps, buf, light)) {
            return;
        }
        float sz = size(type);
        ps.pushPose();
        ps.translate(0.5, 0, 0.5);
        ps.scale(sz, sz, sz);
        ps.translate(-0.5, 0, -0.5);
        drawRaw(type, an, ps, buf, light);
        ps.popPose();
    }

    private static void drawRaw(MarkerType type, Anim an, PoseStack ps, MultiBufferSource buf, int light) {
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
            case MEETING -> meetingTable(a, t);
            case HOKORA -> hokora(a, w, act, t);
            case ANDON -> andon(a, w, act, t);
            case IRORI -> irori(a, w, act, t);
            case BONSAI -> bonsai(a, w, an);
            case TEA -> tea(a, w, act, t);
            case WELL -> well(a, w, an, t);
            case SHOGI -> shogi(a, w, an);
            case KAKEJIKU -> kakejiku(a, w, an, t);
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

    // ================================================================ 第3弾

    /** 会議の座卓（大きな黒い座卓に湯のみと急須。会議はここに集まる） */
    private static void meetingTable(Art a, float t) {
        a.box("dark_oak_planks", -12, 6, -12, 28, 8, 28);
        a.box("black_concrete", -12.3f, 5.6f, -12.3f, 28.3f, 6.2f, 28.3f);
        for (float[] l : new float[][]{{-10, -10}, {24, -10}, {-10, 24}, {24, 24}}) {
            a.box("dark_oak_log", l[0], 0, l[1], l[0] + 3, 6, l[1] + 3);
        }
        // 真ん中に囲炉裏風の炭（ほんのり光る）
        a.box("polished_blackstone", 3, 8, 3, 13, 9, 13);
        a.glow("magma", 4.5f, 8.6f, 4.5f, 11.5f, 9.3f, 11.5f, 0xC0FFFFFF);
        // 湯のみ
        float[][] cups = {{-8, -8}, {20, -8}, {-8, 20}, {20, 20}, {6, -9}, {6, 21}, {-9, 6}, {21, 6}};
        for (float[] c : cups) {
            a.box("white_terracotta", c[0], 8, c[1], c[0] + 2.5f, 10.5f, c[1] + 2.5f);
            a.box("green_terracotta", c[0] + 0.4f, 10.2f, c[1] + 0.4f, c[0] + 2.1f, 10.4f, c[1] + 2.1f);
        }
        a.box("cut_copper", 14, 8, 14, 19, 12, 19);
        a.box("cut_copper", 18.5f, 10, 16, 21, 10.8f, 17);
        steam(a, t, 16.5f, 12, 16.5f, 0.4f);
    }

    /** 祠：石の台に小さな社。しめ縄と紙垂。work が 1 になると封印が解けて壊れる */
    private static void hokora(Art a, float w, boolean act, float t) {
        boolean broken = w >= 0.99f;
        a.box("stone_bricks", -2, 0, -2, 18, 4, 18);
        a.box("mossy_stone_bricks", 0, 4, 0, 16, 6, 16);
        // 社（壊れると傾く）
        a.push();
        if (broken) {
            a.rot('z', 14, 2, 6, 8);
            a.rot('x', -8, 8, 6, 2);
        }
        a.box("spruce_planks", 2, 6, 3, 14, 18, 14);
        a.box("dark_oak_log", 1.5f, 6, 2.5f, 3, 19, 4);
        a.box("dark_oak_log", 13, 6, 2.5f, 14.5f, 19, 4);
        // 扉（壊れると開く）
        float open = broken ? 70 : (act ? (float) Math.sin(t * 0.8f) * 4 : 0);
        a.push();
        a.rot('y', -open, 3, 8, 2.8f);
        a.box("dark_oak_planks", 3, 7, 2.6f, 8, 17, 3.2f);
        a.pop();
        a.push();
        a.rot('y', open, 13, 8, 2.8f);
        a.box("dark_oak_planks", 8, 7, 2.6f, 13, 17, 3.2f);
        a.pop();
        if (broken) {
            // 中から紫の瘴気
            float pulse = 0.6f + 0.4f * (float) Math.sin(t * 0.3f);
            a.glow("purple_concrete", 4, 7, 3.2f, 12, 16, 12, ((int) (pulse * 200) << 24) | 0xC060FF);
            for (int k = 0; k < 5; k++) {
                float ph = ((t * 0.6f + k * 7) % 36f) / 36f;
                float sz = 1.5f + ph * 3;
                int al = (int) ((1 - ph) * 170);
                a.glow("purple_concrete", 8 - sz / 2 + (float) Math.sin(t * 0.1f + k) * 2, 14 + ph * 20, 8 - sz / 2, 8 + sz / 2,
                        14 + ph * 20 + sz, 8 + sz / 2, (al << 24) | 0xA040FF);
            }
        }
        // 屋根
        a.push();
        if (broken) {
            a.move(4, -1, 0);
            a.rot('z', 32, 8, 19, 8);
        }
        a.box("deepslate_tiles", -0.5f, 18, 0.5f, 16.5f, 19.5f, 16);
        a.box("deepslate_tiles", 1, 19.5f, 1.5f, 15, 21, 15);
        a.box("deepslate_tiles", 3, 21, 3, 13, 22, 13);
        a.box("gold_block", 7, 22, 7, 9, 23, 9);
        a.pop();
        // しめ縄と紙垂（壊れると切れて垂れる）
        if (!broken) {
            a.box("birch_planks", 1, 16, 1.6f, 15, 17.2f, 2.4f);
            for (float x : new float[]{4, 8, 12}) {
                a.box("white_concrete", x - 0.8f, 13, 1.7f, x + 0.8f, 16, 1.9f);
            }
        } else {
            a.push();
            a.rot('z', -60, 1, 16.5f, 2);
            a.box("birch_planks", 1, 16, 1.6f, 9, 17.2f, 2.4f);
            a.pop();
        }
        a.pop();
        // お供え（割れた皿）
        a.box("white_terracotta", 6, 6, -1.5f, 10, 6.5f, 1.5f);
    }

    /** 行灯：木の枠に和紙。火を入れるとだんだん明るく */
    private static void andon(Art a, float w, boolean act, float t) {
        a.box("dark_oak_planks", 3, 0, 3, 13, 1.5f, 13);
        for (float[] c : new float[][]{{3, 3}, {12, 3}, {3, 12}, {12, 12}}) {
            a.box("dark_oak_log", c[0], 1.5f, c[1], c[0] + 1, 20, c[1] + 1);
        }
        a.box("dark_oak_planks", 3, 19.5f, 3, 13, 20.5f, 13);
        a.box("dark_oak_planks", 3, 9, 3, 13, 9.6f, 13);
        // 火皿と炎
        a.box("iron_block", 6.5f, 9.6f, 6.5f, 9.5f, 10.3f, 9.5f);
        float lit = smooth(w);
        float flick = act ? 0.85f + 0.15f * (float) Math.sin(t * 2.3f) : 1f;
        if (lit > 0.05f) {
            float fh = 1 + lit * 2.5f * flick;
            a.glow("fire_0", 7.3f, 10.3f, 7.3f, 8.7f, 10.3f + fh, 8.7f, ((int) (255 * Math.max(0.3f, lit)) << 24) | 0xFFFFFF);
        }
        // 和紙（明るさで色が変わる）
        int c = (int) (150 + 105 * lit * flick);
        int paper = 0xD0000000 | (Math.min(255, c + 40) << 16) | (Math.min(255, c + 20) << 8) | Math.max(0, c - 60);
        if (lit > 0.3f) {
            a.glow("white_concrete", 4, 10, 3.8f, 12, 19.5f, 12.2f, paper);
        } else {
            a.glass("white_concrete", 4, 10, 3.8f, 12, 19.5f, 12.2f, 0xD0F0EAD8);
        }
        // 取っ手
        a.box("dark_oak_planks", 7.5f, 20.5f, 3.5f, 8.5f, 24, 12.5f);
        a.box("dark_oak_planks", 7.5f, 23.5f, 3.5f, 8.5f, 24.5f, 12.5f);
    }

    /** 囲炉裏：床に埋めた木の枠に灰と炭。自在鉤で鉄瓶をつるす */
    private static void irori(Art a, float w, boolean act, float t) {
        a.box("spruce_planks", -6, 0, -6, 22, 2, -3);
        a.box("spruce_planks", -6, 0, 19, 22, 2, 22);
        a.box("spruce_planks", -6, 0, -3, -3, 2, 19);
        a.box("spruce_planks", 19, 0, -3, 22, 2, 19);
        a.box("light_gray_concrete_powder", -3, 0, -3, 19, 1.2f, 19);
        // 炭（火をおこすほど赤く光る）
        float fire = Math.max(smooth(w), act ? 0.5f : 0f);
        float[][] coals = {{4, 5}, {8, 4}, {11, 6}, {6, 9}, {10, 10}, {5, 12}};
        for (float[] c : coals) {
            a.box("black_concrete", c[0], 1.2f, c[1], c[0] + 2.5f, 2.6f, c[1] + 2.5f);
            if (fire > 0.05f) {
                a.glow("magma", c[0] + 0.2f, 2.5f, c[1] + 0.2f, c[0] + 2.3f, 2.7f, c[1] + 2.3f,
                        ((int) (255 * fire) << 24) | 0xFFFFFF);
            }
        }
        if (fire > 0.3f) {
            float fh = (2 + fire * 4) * (act ? 0.8f + 0.2f * (float) Math.sin(t * 2f) : 1f);
            a.glow("fire_0", 6, 2.6f, 7, 10, 2.6f + fh, 7.2f, ((int) (230 * fire) << 24) | 0xFFFFFF);
            a.glow("fire_0", 7.9f, 2.6f, 5, 8.1f, 2.6f + fh, 9.5f, ((int) (230 * fire) << 24) | 0xFFFFFF);
        }
        // 自在鉤と鉄瓶
        a.box("bamboo_block", 7.4f, 12, 7.4f, 8.6f, 36, 8.6f);
        a.box("spruce_planks", 4, 16, 7.5f, 12, 18, 8.5f);
        a.box("polished_blackstone", 4.5f, 6, 4.5f, 11.5f, 11, 11.5f);
        a.box("polished_blackstone", 5.5f, 11, 5.5f, 10.5f, 12, 10.5f);
        a.box("polished_blackstone", 11.5f, 8, 7.5f, 14, 9, 8.5f);
        steam(a, t, 8, 12, 8, fire > 0.6f ? fire : 0f);
        // 座布団
        a.box("purple_wool", -12, 0, 4, -7, 1.2f, 12);
        a.box("purple_wool", 23, 0, 4, 28, 1.2f, 12);
    }

    /** 盆栽：手入れするほど枝ぶりが整う（はみ出した葉が引っこむ）。ハサミが動く */
    private static void bonsai(Art a, float w, Anim an) {
        a.box("dark_oak_planks", 1, 0, 3, 15, 4, 13);
        a.box("dark_oak_planks", 2, 0, 4, 3, 1, 12);
        a.box("blue_terracotta", 2.5f, 4, 4.5f, 13.5f, 8, 11.5f);
        a.box("coarse_dirt", 3, 7.8f, 5, 13, 8.2f, 11);
        a.box("moss_block", 3, 8.2f, 5, 13, 8.5f, 11);
        // 幹（くねっとした松）
        a.box("spruce_log", 7, 8.5f, 7, 9, 12, 9);
        a.push();
        a.rot('z', -20, 8, 12, 8);
        a.box("spruce_log", 7, 12, 7.2f, 8.8f, 16, 8.8f);
        a.pop();
        a.box("spruce_log", 4, 15, 7.4f, 9, 16.4f, 8.6f);
        a.box("spruce_log", 8, 16, 7.4f, 12, 17.4f, 8.6f);
        // 葉の雲（整うほど平らでまるく）
        float neat = smooth(w);
        float snip = an.hit(6);
        int leaf = 0xFF3C7A2A;
        float[][] clouds = {{2, 15.5f, 5}, {10, 17, 6}, {5, 19, 5.5f}};
        for (int i = 0; i < clouds.length; i++) {
            float[] c = clouds[i];
            a.tint("spruce_leaves", c[0], c[1], 5.5f, c[0] + c[2], c[1] + 2.4f, 10.5f, leaf);
            // はみ出した枝葉（手入れで消える）
            float wild = (1 - neat) * (3 + i);
            if (wild > 0.2f) {
                a.tint("spruce_leaves", c[0] + 1, c[1] + 2.4f, 6.5f, c[0] + 2.5f, c[1] + 2.4f + wild, 8, leaf);
                a.tint("spruce_leaves", c[0] + c[2] - 1, c[1], 8, c[0] + c[2] + wild * 0.6f, c[1] + 1.5f, 9.5f, leaf);
            }
        }
        // 剪定ばさみ
        a.push();
        a.rot('z', 20 + snip * 25, 16, 14, 8);
        a.box("iron_block", 13, 14, 7.6f, 19, 14.6f, 8.4f);
        a.pop();
        a.push();
        a.rot('z', 20 - snip * 25, 16, 14, 8);
        a.box("iron_block", 13, 13.4f, 7.6f, 19, 14, 8.4f);
        a.pop();
        a.box("red_concrete", 18.5f, 12.5f, 7.5f, 21, 15.5f, 8.5f);
    }

    /** 茶道具：茶碗の中で茶筅を振ると、だんだん泡が立つ。釜から湯気 */
    private static void tea(Art a, float w, boolean act, float t) {
        a.box("red_wool", -4, 0, -2, 20, 0.6f, 18);
        // 風炉と釜
        a.box("polished_blackstone", -2, 0.6f, 9, 6, 6, 17);
        a.box("cut_copper", -1, 6, 10, 5, 10, 16);
        a.box("cut_copper", 0, 10, 11, 4, 11, 15);
        steam(a, t, 2, 11, 13, 0.7f);
        // 茶碗（黒楽）
        a.box("black_terracotta", 8, 0.6f, 2, 15, 5, 9);
        a.box("black_terracotta", 7.5f, 3, 1.5f, 15.5f, 5.2f, 9.5f);
        float foam = smooth(w);
        a.box("lime_terracotta", 8.5f, 3.5f, 2.5f, 14.5f, 4.6f, 8.5f);
        if (foam > 0.05f) {
            a.box("lime_concrete_powder", 8.6f, 4.6f, 2.6f, 14.4f, 4.6f + foam * 0.6f, 8.4f);
        }
        // 茶筅（作業中は左右にシャカシャカ）
        float shake = act ? (float) Math.sin(t * 3.1f) * 2.2f : 0f;
        a.push();
        a.move(shake, act ? 0 : 6, 0);
        if (!act) {
            a.move(4, -6, 4);
        }
        a.box("bamboo_planks", 10.7f, 4, 4.7f, 12.3f, 7, 6.3f);
        a.box("bamboo_block", 11.2f, 7, 5.2f, 11.8f, 12, 5.8f);
        a.pop();
        // 茶杓となつめ
        a.box("black_concrete", 16, 0.6f, 3, 19, 4, 6);
        a.box("bamboo_planks", 15, 0.6f, 9, 20, 1, 9.6f);
    }

    /** 井戸：石の井筒に屋根と滑車。くむたびに釣瓶が上下して、たらいに水がたまる */
    private static void well(Art a, float w, Anim an, float t) {
        for (int k = 0; k < 8; k++) {
            a.push();
            a.rot('y', k * 45, 8, 0, 8);
            a.box(k % 2 == 0 ? "cobblestone" : "mossy_cobblestone", 3, 0, -1, 13, 9, 2);
            a.pop();
        }
        a.glass("water_still", 1, 3, 1, 15, 4, 15, 0xC03F76E4);
        a.box("black_concrete", 2, 0.5f, 2, 14, 3, 14);
        // 柱と屋根
        a.box("spruce_log", -2, 0, 7, 0, 26, 9);
        a.box("spruce_log", 16, 0, 7, 18, 26, 9);
        a.box("spruce_planks", -3, 24, 6, 19, 25.5f, 10);
        a.box("deepslate_tiles", -4, 26, 2, 20, 27.5f, 8.2f);
        a.box("deepslate_tiles", -4, 26, 7.8f, 20, 27.5f, 14);
        // 滑車
        a.box("iron_block", 7, 21, 7, 9, 24, 9);
        // 釣瓶（くんだ直後は上がってくる）
        float up = an.hit(16);
        float by = 5 + up * 12;
        a.box("iron_block", 7.8f, by + 4, 7.8f, 8.2f, 21, 8.2f);
        a.box("spruce_planks", 6, by, 6, 10, by + 4, 10);
        if (up > 0.2f) {
            a.glass("water_still", 6.3f, by + 3, 6.3f, 9.7f, by + 3.6f, 9.7f, 0xC03F76E4);
        }
        // たらい（くむほど満ちる）
        a.box("spruce_planks", 18, 0, -6, 26, 4, 2);
        float lvl = 0.6f + smooth(w) * 3f;
        a.glass("water_still", 18.5f, 0.5f, -5.5f, 25.5f, lvl, 1.5f, 0xC03F76E4);
    }

    /** 将棋盤：散らばった駒が、ならべるほど盤の上の正しい位置へ */
    private static void shogi(Art a, float w, Anim an) {
        a.box("birch_planks", 0, 4, 0, 16, 9, 16);
        for (float[] l : new float[][]{{1, 1}, {13, 1}, {1, 13}, {13, 13}}) {
            a.box("dark_oak_planks", l[0], 0, l[1], l[0] + 2, 4, l[1] + 2);
        }
        for (int i = 0; i <= 9; i++) {
            float p = 1 + i * 1.55f;
            a.box("black_concrete", p - 0.06f, 9, 1, p + 0.06f, 9.05f, 15);
            a.box("black_concrete", 1, 9, p - 0.06f, 15, 9.05f, p + 0.06f);
        }
        int total = 18;
        int placed = Math.round(smooth(w) * total);
        for (int i = 0; i < total; i++) {
            int row = i < 9 ? 0 : 2;
            int col = i % 9;
            float px;
            float pz;
            float py = 9;
            if (i < placed) {
                px = 1.2f + col * 1.55f;
                pz = 1.2f + row * 1.55f;
            } else {
                // 盤の横に散らばっている
                px = 17 + (i * 37 % 7) * 1.2f;
                pz = (i * 53 % 13) * 1.1f;
                py = 0;
            }
            float pop = i == placed - 1 ? an.hit(6) * 1.5f : 0f;
            a.box("birch_planks", px, py + pop, pz, px + 1.15f, py + 0.6f + pop, pz + 1.3f);
            a.box("black_concrete", px + 0.4f, py + 0.6f + pop, pz + 0.4f, px + 0.75f, py + 0.62f + pop, pz + 0.9f);
        }
        a.box("purple_wool", -6, 0, 3, -1, 1.2f, 13);
    }

    /** 掛け軸（壁は +Z 側）：曲がっているのを直すほどまっすぐ。触ると少しゆれる */
    private static void kakejiku(Art a, float w, Anim an, float t) {
        float tilt = (1 - smooth(w)) * 18f + an.hit(14) * (float) Math.sin(an.since() * 1.2f) * 6f;
        a.box("iron_block", 7.6f, 30, 15.2f, 8.4f, 31, 16);
        a.push();
        a.rot('z', tilt, 8, 30, 15);
        a.box("dark_oak_planks", 1, 28, 14.6f, 15, 29, 15.4f);
        a.box("brown_wool", 2, 6, 14.7f, 14, 28, 15.1f);
        a.box("white_concrete", 3.5f, 9, 14.6f, 12.5f, 26, 14.7f);
        // 墨絵（山と赤い日）
        a.box("gray_concrete", 4, 11, 14.55f, 9, 15, 14.6f);
        a.box("gray_concrete", 6, 15, 14.55f, 8, 17, 14.6f);
        a.box("light_gray_concrete", 8, 11, 14.55f, 12, 14, 14.6f);
        a.box("red_concrete", 9.5f, 20, 14.55f, 11.5f, 22, 14.6f);
        a.box("black_concrete", 5, 23, 14.55f, 5.6f, 25.5f, 14.6f);
        a.box("dark_oak_planks", 0.5f, 5, 14.4f, 15.5f, 6.2f, 15.6f);
        a.pop();
        // 床の間の台と香炉
        a.box("dark_oak_planks", -2, 0, 9, 18, 2, 16);
        a.box("gold_block", 6, 2, 11, 10, 4, 14);
    }
}
