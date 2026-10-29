package com.minpaku.client.scene;

import com.minpaku.client.ClientGame;
import com.minpaku.client.Keyframes;
import com.minpaku.client.Ui;
import com.minpaku.client.model.MeshCache;
import com.minpaku.client.model.StaticMesh;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.model.CatModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Blocks;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * フィニッシャー（襲われたときの場面）。どれも約4秒。
 *   役職専用：wolf（人狼）/ shinobi（忍び狼）/ kairiki（怪力狼）/ juro（呪狼）
 *   共通（各自が選ぶ）：pingpong（卓球ラケット）/ pillow（枕投げ）/ fusuma（ふすまドーン）/ seoi（背負い投げ）
 *   特別：neko（猫又の祟り。猫又を襲った犯人が見る）
 * variant: 0 = ふつう / 1 = 板前のおにぎりで耐える（途中で止まる） / 2 = 猫又の反撃（最後に犯人も倒れる）
 *
 * ★ 増やすときは Finisher を継承したクラスを1つ書いて ALL に入れる。
 */
public final class FinisherScenes {
    private FinisherScenes() {
    }

    /** 名前の一覧（確認コマンドの候補にも使う） */
    public static final Map<String, String> NAMES = new LinkedHashMap<>();
    private static final Map<String, Finisher> ALL = new HashMap<>();

    static {
        add(new Wolf());
        add(new Shinobi());
        add(new Kairiki());
        add(new Juro());
        add(new PingPong());
        add(new Pillow());
        add(new Fusuma());
        add(new Seoi());
        add(new Neko());
        // Blockbench だけで作ったフィニッシャー（コードの版はない）
        NAMES.put("sheriff", "シェリフ ― 早撃ち");
    }

    private static void add(Finisher f) {
        ALL.put(f.id, f);
        NAMES.put(f.id, f.title);
    }

    /** Blockbench のプロジェクト（assets/minpaku/finishers/<id>.bbmodel）があればそれで再生する */
    public static Finisher get(String id) {
        com.minpaku.client.bb.BbModel m = com.minpaku.client.bb.BbCache.get(id);
        if (m != null) {
            Bb b = BB.get(id);
            if (b == null || b.m != m) {
                b = new Bb(id, NAMES.getOrDefault(id, id), m);
                BB.put(id, b);
            }
            return b;
        }
        return ALL.getOrDefault(id, ALL.get("wolf"));
    }

    private static final Map<String, Bb> BB = new HashMap<>();

    private static float[] hitsOf(com.minpaku.client.bb.BbModel m) {
        java.util.List<Float> list = new java.util.ArrayList<>();
        for (com.minpaku.client.bb.BbModel.Effect e : m.effects) {
            if (e.channel().equals("timeline") && e.value().trim().startsWith("hit")) {
                list.add(e.time() * 20f);
            }
        }
        float[] out = new float[list.size()];
        for (int i = 0; i < out.length; i++) {
            out[i] = list.get(i);
        }
        return out;
    }

    /**
     * Blockbench で作ったフィニッシャー。
     *  - グループ killer / victim（＋ killer_head などの部位）：犯人と自分（スキンは自動で差しかえ）
     *  - グループ camera / camera_target：カメラの位置と見る先（camera の大きさ × 50 = 画角）
     *  - 効果のキーフレーム：sound「entity.wolf.howl|0.75」（音|高さ）、
     *    timeline「hit」（当たった瞬間）・「text:ワオーン！:killer_head:FFE08A:2.4」（文字:場所:色:大きさ）・「noroom」
     */
    static final class Bb extends Finisher {
        final com.minpaku.client.bb.BbModel m;
        private com.minpaku.client.bb.BbRenderer.Pose base;

        Bb(String id, String title, com.minpaku.client.bb.BbModel m) {
            super(id, title, Math.max(1, Math.round(m.length * 20f)), hitsOf(m));
            this.m = m;
        }

        private com.minpaku.client.bb.BbRenderer.Pose base(float t) {
            base = com.minpaku.client.bb.BbRenderer.pose(m, t / 20f, null);
            return base;
        }

        @Override
        float camera(float t, Vector3f eye, Vector3f target) {
            com.minpaku.client.bb.BbRenderer.Pose p = base(t);
            Vector3f e = p.origin("camera", m);
            Vector3f g = p.origin("camera_target", m);
            eye.set(e != null ? e : new Vector3f(0, 1.6f, 4));
            target.set(g != null ? g : new Vector3f(0, 1.2f, 0));
            return 50f * m.sample("camera", "scale", t / 20f, 1f).x;
        }

        @Override
        void scene(Scene3D s, float t, Ctx c) {
            boolean room = true;
            for (com.minpaku.client.bb.BbModel.Effect e : m.effects) {
                if (e.value().trim().equals("noroom")) {
                    room = false;
                }
            }
            if (room) {
                room(s, false);
            }
            com.minpaku.client.bb.BbRenderer.Pose p = base != null ? base : base(t);
            Vector3f k = p.origin("killer", m);
            Vector3f v = p.origin("victim", m);
            if (k != null) {
                c.killerPos.set(k);
            }
            if (v != null) {
                c.victimPos.set(v);
            }
            // 当たった所（血しぶき）は胸：首の付け根から少し下（寝ていても胸になる）
            Vector3f neck = p.origin("victim_body", m);
            if (neck != null) {
                c.victimPos.set(neck.x, neck.y - 1.55f, neck.z);
            }
            final float push = c.push;
            final float fall = c.fall;
            com.minpaku.client.bb.BbRenderer.Pose drawn = p;
            if ((push > 0 || fall > 0) && k != null && v != null) {
                float dx = k.x - v.x;
                float dz = k.z - v.z;
                float len = (float) Math.max(0.001, Math.sqrt(dx * dx + dz * dz));
                float nx = dx / len;
                float nz = dz / len;
                drawn = com.minpaku.client.bb.BbRenderer.pose(m, t / 20f, (name, w) -> {
                    if (!name.equals("killer")) {
                        return;
                    }
                    float off = push * 2.2f;
                    float up = push > 0 ? (float) Math.sin(Math.min(1f, push) * Math.PI) * 0.6f : 0f;
                    w.translate(nx * off, up, nz * off);
                    float back = (float) Math.toRadians(Math.max(push, fall) * 85f);
                    // 被害者から遠ざかる向きにあお向けに倒れる
                    w.rotate(back, nz, 0, -nx);
                });
            }
            com.minpaku.client.bb.BbRenderer.render(m, drawn, s.ps, s.buf, c.killerSkin, c.mySkin, s.bright);
            base = null;
        }

        @Override
        void marks(Scene3D s, float t, Ctx c, Map<String, float[]> out) {
            com.minpaku.client.bb.BbRenderer.Pose p = com.minpaku.client.bb.BbRenderer.pose(m, t / 20f, null);
            for (com.minpaku.client.bb.BbModel.Effect e : m.effects) {
                String v = e.value().trim();
                if (!v.startsWith("text:")) {
                    continue;
                }
                String[] parts = v.split(":");
                String bone = parts.length > 2 ? parts[2] : "killer_head";
                Vector3f o = p.origin(bone, m);
                if (o != null && !out.containsKey(bone)) {
                    out.put(bone, s.toScreen(o.x, o.y + 0.55f, o.z));
                }
            }
        }

        @Override
        void overlay(GuiGraphics g, Scene3D s, float t, Ctx c, Map<String, float[]> marks) {
            for (com.minpaku.client.bb.BbModel.Effect e : m.effects) {
                String v = e.value().trim();
                if (!v.startsWith("text:")) {
                    continue;
                }
                String[] parts = v.split(":");
                String text = parts.length > 1 ? parts[1] : "";
                String bone = parts.length > 2 ? parts[2] : "killer_head";
                int color = 0xFFFFFFFF;
                float size = 2.2f;
                try {
                    if (parts.length > 3) {
                        color = 0xFF000000 | Integer.parseInt(parts[3], 16);
                    }
                    if (parts.length > 4) {
                        size = Float.parseFloat(parts[4]);
                    }
                } catch (NumberFormatException ignored) {
                }
                if (bone.equals("screen")) {
                    float st = t - e.time() * 20f;
                    if (st > 0 && st < 22) {
                        float a = st < 16 ? 1f : 1f - (st - 16) / 6f;
                        float sc = size * (st < 4 ? 1.3f - st / 4f * 0.3f : 1f);
                        Ui.text(g, Ui.serif(text), g.guiWidth() / 2f, g.guiHeight() * 0.24f, Ui.alpha(color, a), sc, 0, true);
                    }
                    continue;
                }
                pop(g, marks.get(bone), t, e.time() * 20f, text, color, size);
            }
        }

        @Override
        void sound(int t) {
            for (com.minpaku.client.bb.BbModel.Effect e : m.effects) {
                if (!e.channel().equals("sound") || Math.round(e.time() * 20f) != t) {
                    continue;
                }
                String[] parts = e.value().split("\\|");
                float pitch = 1f;
                try {
                    if (parts.length > 1) {
                        pitch = Float.parseFloat(parts[1]);
                    }
                } catch (NumberFormatException ignored) {
                }
                net.minecraft.resources.ResourceLocation id = net.minecraft.resources.ResourceLocation.tryParse(parts[0].trim());
                if (id == null) {
                    continue;
                }
                final float p = pitch;
                net.minecraft.core.registries.BuiltInRegistries.SOUND_EVENT.getOptional(id).ifPresent(se -> play(se, p));
            }
        }
    }

    public static final int GUARD_LEN = 56;
    public static final int COUNTER_LEN = 40;

    /** 場面の全体の長さ（tick） */
    public static int total(String id, int variant) {
        Finisher f = get(id);
        return switch (variant) {
            case 1 -> (int) f.guardAt() + GUARD_LEN;
            case 2 -> f.len + COUNTER_LEN;
            default -> f.len;
        };
    }

    // ================================================================ 描くときの状態

    /** 1回の再生の状態 */
    public static final class Ctx {
        public final UUID killerId;
        public final String killerName;
        public final int variant;
        final PlayerSkin killerSkin;
        final PlayerSkin mySkin;
        /** 板前：犯人をはね返す量（0〜1）と、猫又：犯人が倒れる量 */
        float push;
        float fall;
        final Vector3f killerPos = new Vector3f();
        final Vector3f victimPos = new Vector3f();

        public Ctx(UUID killerId, String killerName, int variant) {
            Minecraft mc = Minecraft.getInstance();
            this.killerId = killerId;
            this.killerName = killerName;
            this.variant = variant;
            this.killerSkin = ClientGame.skin(killerId);
            this.mySkin = mc.player != null ? mc.player.getSkin() : ClientGame.skin(new UUID(0, 0));
        }

        /** 犯人を描く（おにぎり・猫又のときは、はね返されたり倒れたりする） */
        void killer(Scene3D s, PlayerModel<?> m, float x, float y, float z, float yaw, float pitch, float roll) {
            killerPos.set(x, y, z);
            float dx = x - victimPos.x;
            float dz = z - victimPos.z;
            float len = (float) Math.max(0.001, Math.sqrt(dx * dx + dz * dz));
            float off = push * 2.2f;
            float py = push > 0 ? (float) Math.sin(Math.min(1f, push) * Math.PI) * 0.6f : 0f;
            // 倒れる向き：被害者から遠ざかる方へあお向け
            float back = Math.max(push, fall) * 85f;
            s.player(m, killerSkin, x + dx / len * off, y + py, z + dz / len * off, yaw, pitch - back, roll);
        }

        void victim(Scene3D s, PlayerModel<?> m, float x, float y, float z, float yaw, float pitch, float roll) {
            victimPos.set(x, y, z);
            s.player(m, mySkin, x, y, z, yaw, pitch, roll);
        }

        PlayerModel<?> km() {
            return Scene3D.model(killerSkin);
        }

        PlayerModel<?> vm() {
            return Scene3D.model(mySkin);
        }
    }

    // ================================================================ 1つのフィニッシャー

    public abstract static class Finisher {
        final String id;
        final String title;
        final int len;
        /** 当たった瞬間（白く光る・血しぶき） */
        final float[] hits;

        Finisher(String id, String title, int len, float... hits) {
            this.id = id;
            this.title = title;
            this.len = len;
            this.hits = hits;
        }

        /** おにぎりで止まる時刻（最初の一撃の少し前） */
        float guardAt() {
            return hits.length > 0 ? hits[0] - 3 : len * 0.4f;
        }

        /** カメラ：eye と target を決めて fov を返す */
        abstract float camera(float t, Vector3f eye, Vector3f target);

        /** 背景・小道具・人物を描く */
        abstract void scene(Scene3D s, float t, Ctx c);

        /** 文字などの2D（3D の座標を画面へ） */
        void overlay(GuiGraphics g, Scene3D s, float t, Ctx c, Map<String, float[]> marks) {
        }

        /** 3D の座標で覚えておく印（overlay で使う） */
        void marks(Scene3D s, float t, Ctx c, Map<String, float[]> out) {
        }

        /** 音 */
        abstract void sound(int t);
    }

    // ================================================================ 再生

    /** 毎 tick（音） */
    public static void tick(int t, String id, int variant) {
        Finisher f = get(id);
        if (variant == 1 && t >= f.guardAt()) {
            int gt = t - (int) f.guardAt();
            if (gt == 1) {
                play(SoundEvents.AMETHYST_BLOCK_RESONATE, 1.4f);
            }
            if (gt == 8) {
                play(SoundEvents.TOTEM_USE, 1.3f);
            }
            if (gt == 14) {
                play(SoundEvents.PLAYER_BIG_FALL, 0.8f);
            }
            return;
        }
        if (variant == 2 && t >= f.len) {
            int ct = t - f.len;
            if (ct == 2) {
                play(SoundEvents.CAT_HISS, 0.6f);
            }
            if (ct == 12) {
                play(SoundEvents.CAT_HURT, 0.5f);
                play(SoundEvents.PLAYER_ATTACK_SWEEP, 0.8f);
            }
            if (ct == 18) {
                play(SoundEvents.PLAYER_BIG_FALL, 0.8f);
            }
            return;
        }
        f.sound(t);
    }

    static void play(SoundEvent e, float pitch) {
        ClientGame.play(e, pitch);
    }

    /**
     * 描く。t = 始まってからの tick（小数）、alpha = 全体の明るさ（最後の暗転用）。
     * 当たった所の画面座標を hitXY に入れて返す（血しぶき・爪あと用）。
     */
    public static float[][] render(GuiGraphics g, float t, float alpha, Ctx c, String id) {
        Finisher f = get(id);
        float ft = t;
        if (c.variant == 1) {
            ft = Math.min(t, f.guardAt());
            float gt = t - f.guardAt();
            c.push = gt > 8 ? Mth.clamp((gt - 8) / 14f, 0f, 1f) : 0f;
        }
        if (c.variant == 2) {
            ft = Math.min(t, f.len - 1);
            float ct = t - f.len;
            c.fall = ct > 14 ? Mth.clamp((ct - 14) / 8f, 0f, 1f) : 0f;
        }
        Vector3f eye = new Vector3f();
        Vector3f target = new Vector3f();
        float fov = f.camera(ft, eye, target);
        // 当たった瞬間のゆれ
        float shake = 0f;
        for (float h : f.hits) {
            if (ft > h && ft < h + 5) {
                shake = Math.max(shake, 0.07f * (1f - (ft - h) / 5f));
            }
        }
        if (c.variant == 1 && t > f.guardAt() + 6 && t < f.guardAt() + 14) {
            shake = 0.05f;
        }
        target.add((float) Math.sin(t * 7.3f) * shake, (float) Math.cos(t * 9.1f) * shake, 0);

        int w = g.guiWidth();
        int h = g.guiHeight();
        g.fill(0, 0, w, h, 0xFF000000);
        Scene3D s = Scene3D.begin(g, eye, target, fov);
        s.bright = alpha;
        f.scene(s, ft, c);
        if (c.variant == 1 && t >= f.guardAt()) {
            onigiri(s, t - f.guardAt(), c);
        }
        if (c.variant == 2 && t >= f.len) {
            cat(s, t - f.len, c);
        }
        Map<String, float[]> marks = new HashMap<>();
        f.marks(s, ft, c, marks);
        float[][] hitXY = new float[f.hits.length][];
        for (int i = 0; i < f.hits.length; i++) {
            hitXY[i] = s.toScreen(c.victimPos.x, c.victimPos.y + 1.4f, c.victimPos.z);
        }
        float[] mid = s.toScreen((c.killerPos.x + c.victimPos.x) / 2, 1.3f, (c.killerPos.z + c.victimPos.z) / 2);
        s.end();
        f.overlay(g, s, ft, c, marks);

        // 当たった瞬間：白い光
        for (float hh : f.hits) {
            if (ft > hh && ft < hh + 3 && c.variant != 1) {
                g.fill(0, 0, w, h, Ui.alpha(0xFFFFFFFF, (1f - (ft - hh) / 3f) * 0.5f * alpha));
            }
        }
        // 板前のおにぎり
        if (c.variant == 1 && t >= f.guardAt()) {
            float gt = t - f.guardAt();
            if (gt > 6 && gt < 11) {
                g.fill(0, 0, w, h, Ui.alpha(0xFFFFFFFF, (1f - (gt - 6) / 5f) * 0.8f));
            }
            if (gt > 8) {
                float a = Mth.clamp((gt - 8) / 4f, 0f, 1f) * alpha;
                Ui.text(g, Ui.serif("おにぎりの力！"), w / 2f, h * 0.18f, Ui.alpha(0xFFFFF3B0, a), 3.0f, 0, true);
            }
            if (mid != null && gt < 12) {
                Ui.text(g, Ui.serif("！"), mid[0], mid[1] - 40, Ui.alpha(0xFFFFFFFF, alpha), 3f, 0, true);
            }
        }
        // 猫又の反撃
        if (c.variant == 2 && t >= f.len) {
            float ct = t - f.len;
            if (ct > 12 && ct < 16) {
                g.fill(0, 0, w, h, Ui.alpha(0xFFB060FF, (1f - (ct - 12) / 4f) * 0.5f));
            }
            if (ct > 6) {
                float a = Mth.clamp((ct - 6) / 4f, 0f, 1f) * alpha;
                Ui.text(g, Ui.serif("猫又の祟り！"), w / 2f, h * 0.16f, Ui.alpha(0xFFE1B3FF, a), 3.0f, 0, true);
                Ui.text(g, Ui.sans(c.killerName + " も道連れに"), w / 2f, h * 0.16f + 34, Ui.alpha(0xFFFFFFFF, a), 1.2f, 0, true);
            }
        }
        // 上下の黒帯
        g.fill(0, 0, w, h / 10, 0xFF000000);
        g.fill(0, h - h / 10, w, h, 0xFF000000);
        // フィニッシャーの名前（左下）
        if (ft > 4) {
            float a = Mth.clamp((ft - 4) / 6f, 0f, 1f) * alpha;
            Ui.text(g, Ui.sans(f.title), 14, h - h / 10f + 4, Ui.alpha(0xFFC9A14A, a), 0.9f, -1, false);
        }
        return hitXY;
    }

    public static float[] hits(String id) {
        return get(id).hits;
    }

    /** おにぎり（光って犯人をはね返す） */
    private static void onigiri(Scene3D s, float gt, Ctx c) {
        StaticMesh m = MeshCache.get("itamae_onigiri");
        if (m == null) {
            return;
        }
        float pop = Mth.clamp(gt / 6f, 0f, 1f);
        float sc = 0.9f * pop + (gt > 6 && gt < 12 ? 0.25f : 0f);
        s.ps.pushPose();
        s.ps.translate(c.victimPos.x + (c.killerPos.x - c.victimPos.x) * 0.45f, 1.3f + (float) Math.sin(gt * 0.3f) * 0.05f,
                c.victimPos.z + (c.killerPos.z - c.victimPos.z) * 0.45f);
        s.ps.mulPose(Axis.YP.rotationDegrees(gt * 12f));
        s.ps.scale(sc, sc, sc);
        s.ps.translate(0, -0.3f, 0);
        m.render(s.ps, s.buf, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
        s.ps.popPose();
        // 光の輪
        if (gt > 4) {
            float r = 0.2f + (gt - 4) * 0.12f;
            int a = (int) (Mth.clamp(1f - (gt - 4) / 20f, 0f, 1f) * 160);
            if (a > 4) {
                Vector3f p = new Vector3f(c.victimPos.x + (c.killerPos.x - c.victimPos.x) * 0.45f, 1.3f,
                        c.victimPos.z + (c.killerPos.z - c.victimPos.z) * 0.45f);
                s.box(Scene3D.sprite("minecraft:block/white_concrete"), p.x - r, p.y - 0.02f, p.z - r, p.x + r, p.y + 0.02f, p.z + r,
                        (a << 24) | 0xFFF3B0);
            }
        }
    }

    private static CatModel<?> catModel;
    private static final ResourceLocation CAT_TEX = ResourceLocation.withDefaultNamespace("textures/entity/cat/all_black.png");

    /** 猫又（大きな影の猫）。犯人の後ろから飛びかかる */
    private static void cat(Scene3D s, float ct, Ctx c) {
        if (catModel == null) {
            catModel = new CatModel<>(Minecraft.getInstance().getEntityModels().bakeLayer(ModelLayers.CAT));
        }
        float in = Mth.clamp(ct / 8f, 0f, 1f);
        float leap = Mth.clamp((ct - 8) / 6f, 0f, 1f);
        float dx = c.killerPos.x - c.victimPos.x;
        float dz = c.killerPos.z - c.victimPos.z;
        float len = (float) Math.max(0.001, Math.sqrt(dx * dx + dz * dz));
        float bx = c.killerPos.x + dx / len * Mth.lerp(leap, 2.4f, 0.3f);
        float bz = c.killerPos.z + dz / len * Mth.lerp(leap, 2.4f, 0.3f);
        float by = (float) Math.sin(leap * Math.PI) * 1.2f + (1 - leap) * 0.2f;
        float yaw = (float) Math.toDegrees(Math.atan2(-dx, -dz));
        catDraw(s, bx, by, bz, yaw, 2.4f, leap > 0 && leap < 1 ? -30 : 0, in);
    }

    static void catDraw(Scene3D s, float x, float y, float z, float yawToward, float scale, float pitch, float alpha) {
        if (catModel == null) {
            catModel = new CatModel<>(Minecraft.getInstance().getEntityModels().bakeLayer(ModelLayers.CAT));
        }
        s.ps.pushPose();
        s.ps.translate(x, y, z);
        s.ps.mulPose(Axis.YP.rotationDegrees(180f - yawToward));
        s.ps.mulPose(Axis.XP.rotationDegrees(pitch));
        s.ps.scale(-scale, -scale, scale);
        s.ps.translate(0, -1.501f, 0);
        VertexConsumer vc = s.buf.getBuffer(RenderType.entityTranslucent(CAT_TEX));
        int a = (int) (Mth.clamp(alpha, 0f, 1f) * 220);
        catModel.renderToBuffer(s.ps, vc, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, (a << 24) | 0xC9A0FF);
        s.ps.popPose();
        // 光る目
        s.ps.pushPose();
        s.ps.translate(x, y, z);
        s.ps.mulPose(Axis.YP.rotationDegrees(-yawToward));
        float ey = 0.34f * scale / 1.0f;
        for (float ex : new float[]{-0.07f * scale, 0.07f * scale}) {
            s.box(Scene3D.sprite("minecraft:block/yellow_concrete"), ex - 0.03f, ey, -0.36f * scale - 0.01f, ex + 0.03f, ey + 0.03f,
                    -0.36f * scale + 0.01f, ((int) (alpha * 255) << 24) | 0xFFFF60);
        }
        s.ps.popPose();
    }

    // ================================================================ 背景の部屋

    /** 和室（床・奥の壁・ふすま・ちょうちん） */
    static void room(Scene3D s, boolean dark) {
        s.fill(Blocks.BAMBOO_MOSAIC.defaultBlockState(), -6, -1, -6, 6, -1, 6);
        for (int x = -6; x <= 6; x++) {
            s.fill(x % 3 == 0 ? Blocks.STRIPPED_SPRUCE_LOG.defaultBlockState() : Blocks.CALCITE.defaultBlockState(), x, 0, -6, x, 3, -6);
            s.block(Blocks.SPRUCE_PLANKS.defaultBlockState(), x, 4, -6);
        }
        for (int z = -5; z <= 6; z++) {
            s.fill(z % 3 == 0 ? Blocks.STRIPPED_SPRUCE_LOG.defaultBlockState() : Blocks.CALCITE.defaultBlockState(), -7, 0, z, -7, 3, z);
            s.fill(z % 3 == 0 ? Blocks.STRIPPED_SPRUCE_LOG.defaultBlockState() : Blocks.CALCITE.defaultBlockState(), 7, 0, z, 7, 3, z);
        }
        s.block(Blocks.LANTERN.defaultBlockState(), -4, 0, -5);
        s.block(Blocks.LANTERN.defaultBlockState(), 4, 0, -5);
    }

    static void tex(Scene3D s, String name, float x0, float y0, float z0, float x1, float y1, float z1) {
        s.box(Scene3D.sprite("minecraft:block/" + name), x0, y0, z0, x1, y1, z1, 0xFFFFFFFF);
    }

    static void tex(Scene3D s, String name, float x0, float y0, float z0, float x1, float y1, float z1, int argb) {
        s.box(Scene3D.sprite("minecraft:block/" + name), x0, y0, z0, x1, y1, z1, argb);
    }

    static float tr(float t, float... k) {
        return Keyframes.track(t, k);
    }

    /** カメラの場面を時刻でえらんで、なめらかに動かす。shots = {開始, 終了, 目xyz(始), 目xyz(終), 先xyz(始), 先xyz(終), fov} */
    static float shots(float t, Vector3f eye, Vector3f target, float[][] shots) {
        float[] s = shots[shots.length - 1];
        for (float[] sh : shots) {
            if (t < sh[1]) {
                s = sh;
                break;
            }
        }
        float f = Mth.clamp((t - s[0]) / (s[1] - s[0]), 0f, 1f);
        f = f * f * (3 - 2 * f);
        eye.set(Mth.lerp(f, s[2], s[5]), Mth.lerp(f, s[3], s[6]), Mth.lerp(f, s[4], s[7]));
        target.set(Mth.lerp(f, s[8], s[11]), Mth.lerp(f, s[9], s[12]), Mth.lerp(f, s[10], s[13]));
        return s[14];
    }

    static float[] screen(Scene3D s, float x, float y, float z) {
        return s.toScreen(x, y, z);
    }

    static void pop(GuiGraphics g, float[] at, float t, float start, String text, int color, float size) {
        if (at == null || t < start || t > start + 18) {
            return;
        }
        float st = t - start;
        float a = st < 12 ? 1f : 1f - (st - 12) / 6f;
        float sc = size * (st < 3 ? 0.6f + st / 3f * 0.6f : st < 6 ? 1.2f - (st - 3) / 3f * 0.2f : 1f);
        Ui.text(g, Ui.serif(text), at[0], at[1] - st * 0.8f, Ui.alpha(color, a), sc, 0, true);
    }

    // ================================================================ 人狼：獣のように飛びかかる

    static final class Wolf extends Finisher {
        Wolf() {
            super("wolf", "人狼 ― 獣の爪", 80, 34, 44, 52);
        }

        @Override
        float camera(float t, Vector3f eye, Vector3f target) {
            return shots(t, eye, target, new float[][]{
                    {0, 22, 0.6f, 0.5f, 3.6f, 0.3f, 0.45f, 3.0f, 0, 0.6f, -2.5f, 0, 0.6f, -2f, 55},
                    {22, 38, 3.2f, 1.2f, 0.4f, 2.8f, 1.0f, 0.3f, 0, 0.8f, -0.6f, 0, 0.6f, -0.2f, 52},
                    {38, 58, 0.9f, 2.6f, 1.6f, 0.6f, 2.4f, 1.3f, 0, 0.3f, 0.3f, 0, 0.3f, 0.3f, 50},
                    {58, 82, 0.4f, 0.25f, 2.4f, 0.3f, 0.2f, 2.1f, 0, 1.6f, 0.2f, 0, 1.9f, 0.2f, 58}});
        }

        @Override
        void scene(Scene3D s, float t, Ctx c) {
            room(s, true);
            // 自分：最初は立っている → 押し倒される → あお向け
            PlayerModel<?> vm = c.vm();
            float fallen = tr(t, 0, 0, 30, 0, 37, -90);
            vm.head.xRot = tr(t, 0, 0, 24, -0.4f, 30, -0.2f, 37, -0.6f);
            vm.rightArm.xRot = tr(t, 0, 0, 26, -1.6f, 34, -2.6f, 52, -2.2f);
            vm.leftArm.xRot = tr(t, 0, 0, 26, -1.4f, 34, -2.5f, 52, -2.0f);
            vm.rightArm.zRot = tr(t, 34, 0, 40, 0.4f);
            vm.leftArm.zRot = tr(t, 34, 0, 40, -0.4f);
            for (float hh : hits) {
                if (t > hh && t < hh + 3) {
                    vm.head.yRot = (float) Math.sin(t * 3) * 0.3f;
                }
            }
            c.victim(s, vm, 0, 0, tr(t, 0, 0, 37, 0.4f), 0, fallen, 0);
            // 人狼：四つんばいで忍び寄る → 飛びかかる → のしかかって引っかく → 吠える
            PlayerModel<?> km = c.km();
            float kz = tr(t, 0, -4.2f, 22, -1.8f, 30, -0.3f, 37, 0.2f, 56, 0.2f, 66, 0.1f);
            float ky = tr(t, 0, 0, 22, 0, 26, 0.9f, 32, 0.35f, 37, 0.3f, 56, 0.3f, 62, 0f);
            float kp = tr(t, 0, 70, 22, 72, 27, 35, 37, 60, 56, 55, 64, 0, 72, -12);
            float crawl = t < 22 ? (float) Math.sin(t * 0.6f) : 0f;
            km.rightArm.xRot = t < 22 ? -1.4f + crawl * 0.5f : tr(t, 22, -2.8f, 30, -2.6f, 37, -1.6f, 40, -2.9f, 44, -0.6f, 48, -2.9f,
                    52, -0.6f, 58, -1.2f, 66, -0.3f);
            km.leftArm.xRot = t < 22 ? -1.4f - crawl * 0.5f : tr(t, 22, -2.8f, 30, -2.6f, 37, -1.8f, 44, -1.6f, 48, -0.8f, 52, -2.0f,
                    58, -1.2f, 66, -0.3f);
            km.rightLeg.xRot = t < 22 ? crawl * 0.6f : tr(t, 22, 0.8f, 30, 0.4f, 37, -1.3f, 56, -1.3f, 64, 0);
            km.leftLeg.xRot = t < 22 ? -crawl * 0.6f : tr(t, 22, 0.6f, 30, 0.5f, 37, -1.3f, 56, -1.3f, 64, 0);
            km.head.xRot = tr(t, 0, -1.0f, 22, -1.0f, 30, -0.4f, 37, 0.4f, 56, 0.4f, 64, -1.1f, 80, -1.2f);
            c.killer(s, km, 0, ky, kz, 0, kp, 0);
        }

        @Override
        void marks(Scene3D s, float t, Ctx c, Map<String, float[]> out) {
            out.put("eyes", s.toScreen(c.killerPos.x, 0.6f, c.killerPos.z + 0.6f));
            out.put("head", s.toScreen(c.killerPos.x, 2.0f, c.killerPos.z));
        }

        @Override
        void overlay(GuiGraphics g, Scene3D s, float t, Ctx c, Map<String, float[]> marks) {
            float[] e = marks.get("eyes");
            if (e != null && t < 22) {
                float a = Mth.clamp(t / 6f, 0f, 1f);
                Ui.disc(g, e[0] - 5, e[1], 2.2f, Ui.alpha(0xFFFF2020, a));
                Ui.disc(g, e[0] + 5, e[1], 2.2f, Ui.alpha(0xFFFF2020, a));
            }
            pop(g, marks.get("head"), t, 60, "ワオーン！", 0xFFFFE08A, 2.4f);
        }

        @Override
        void sound(int t) {
            if (t == 2) {
                play(SoundEvents.WOLF_GROWL, 0.6f);
            }
            if (t == 23) {
                play(SoundEvents.PLAYER_ATTACK_SWEEP, 0.6f);
            }
            if (t == 30) {
                play(SoundEvents.PLAYER_BIG_FALL, 0.8f);
            }
            for (float h : hits) {
                if (t == (int) h) {
                    play(SoundEvents.PLAYER_ATTACK_CRIT, 0.7f + h * 0.004f);
                    play(SoundEvents.PLAYER_HURT, 0.8f);
                }
            }
            if (t == 60) {
                play(SoundEvents.WOLF_HOWL, 0.75f);
            }
        }
    }

    // ================================================================ 忍び狼：口をふさいで押し入れへ

    static final class Shinobi extends Finisher {
        Shinobi() {
            super("shinobi", "忍び狼 ― 神隠し", 78, 26);
        }

        @Override
        float camera(float t, Vector3f eye, Vector3f target) {
            return shots(t, eye, target, new float[][]{
                    {0, 24, 0.2f, 1.6f, 3.2f, 0.1f, 1.6f, 2.6f, 0, 1.3f, 0, 0, 1.4f, -0.3f, 50},
                    {24, 44, 1.2f, 1.7f, 1.4f, 1.0f, 1.65f, 1.2f, 0, 1.5f, -0.4f, 0, 1.5f, -0.5f, 42},
                    {44, 80, 0.3f, 1.4f, 3.6f, 0.2f, 1.5f, 3.2f, 0, 1.2f, -2.6f, 0, 1.3f, -3.0f, 52}});
        }

        @Override
        void scene(Scene3D s, float t, Ctx c) {
            room(s, true);
            // 押し入れ（奥の壁にある。最後に戸が閉まる）
            float doorX = tr(t, 0, 0.9f, 60, 0.9f, 68, 0f);
            tex(s, "black_concrete", -0.9f, 0, -5.02f, 0.9f, 2.2f, -4.98f);
            tex(s, "dark_oak_planks", -1.0f, 0, -4.98f, 1.0f, 0.1f, -4.9f);
            tex(s, "dark_oak_planks", -1.0f, 2.2f, -4.98f, 1.0f, 2.35f, -4.9f);
            fusumaPanel(s, -0.9f - doorX, -4.95f, 0.9f, 2.2f);
            fusumaPanel(s, 0f + doorX * 0.1f, -4.93f, 0.9f, 2.2f);
            // 引きずられて押し入れへ
            float vz = tr(t, 0, 0, 40, 0, 62, -5.4f);
            float vp = tr(t, 0, 0, 26, 0, 34, -20, 62, -30);
            PlayerModel<?> vm = c.vm();
            float st = t > 26 ? (float) Math.sin(t * 1.6f) : 0f;
            vm.rightArm.xRot = t < 26 ? 0 : -1.6f + st * 0.6f;
            vm.leftArm.xRot = t < 26 ? 0 : -1.4f - st * 0.6f;
            vm.rightLeg.xRot = t > 26 ? st * 0.7f : 0;
            vm.leftLeg.xRot = t > 26 ? -st * 0.7f : 0;
            vm.head.xRot = tr(t, 0, 0, 26, -0.5f);
            s.bright = s.bright * tr(t, 0, 1, 56, 1, 62, 0.4f);
            c.victim(s, vm, 0, 0, vz, 0, vp, 0);
            // 忍び狼：背後に音もなく現れる（最初は見えない）
            PlayerModel<?> km = c.km();
            float appear = Mth.clamp((t - 10) / 10f, 0f, 1f);
            if (appear > 0) {
                km.leftArm.xRot = tr(t, 18, -0.4f, 26, -2.3f);
                km.leftArm.zRot = tr(t, 18, 0, 26, 0.5f);
                km.rightArm.xRot = tr(t, 18, -0.4f, 26, -1.3f);
                km.rightArm.zRot = tr(t, 18, 0, 26, -0.4f);
                float walk = t > 40 ? (float) Math.sin(t * 0.8f) : 0f;
                km.rightLeg.xRot = walk * 0.5f;
                km.leftLeg.xRot = -walk * 0.5f;
                float old = s.bright;
                s.bright = old * appear;
                c.killer(s, km, 0, 0, vz - 0.55f, 0, tr(t, 0, 0, 40, -8), 0);
                s.bright = old;
            }
            // けむり
            if (t > 8 && t < 24) {
                for (int i = 0; i < 8; i++) {
                    float ph = ((t - 8) / 16f + i * 0.13f) % 1f;
                    float r = 0.15f + ph * 0.3f;
                    float x = (float) Math.cos(i * 1.7f) * 0.5f;
                    float z = -0.55f + (float) Math.sin(i * 1.7f) * 0.4f;
                    tex(s, "gray_concrete", x - r, ph * 1.8f, z - r, x + r, ph * 1.8f + r, z + r, ((int) ((1 - ph) * 130) << 24) | 0x888888);
                }
            }
        }

        @Override
        void marks(Scene3D s, float t, Ctx c, Map<String, float[]> out) {
            out.put("door", s.toScreen(0, 2.6f, -4.9f));
            out.put("head", s.toScreen(c.victimPos.x, 2.1f, c.victimPos.z));
        }

        @Override
        void overlay(GuiGraphics g, Scene3D s, float t, Ctx c, Map<String, float[]> marks) {
            pop(g, marks.get("head"), t, 27, "んむっ…！", 0xFFFFFFFF, 1.8f);
            pop(g, marks.get("door"), t, 66, "ピシャッ", 0xFFFFFFFF, 2.4f);
        }

        @Override
        void sound(int t) {
            if (t == 10) {
                play(SoundEvents.ENDERMAN_TELEPORT, 0.6f);
            }
            if (t == 26) {
                play(SoundEvents.PLAYER_HURT, 0.6f);
            }
            if (t == 44 || t == 52) {
                play(SoundEvents.WOOL_STEP, 0.8f);
            }
            if (t == 66) {
                play(SoundEvents.WOODEN_DOOR_CLOSE, 1.2f);
            }
        }
    }

    static void fusumaPanel(Scene3D s, float x, float z, float w, float h) {
        tex(s, "dark_oak_planks", x, 0, z - 0.03f, x + w, h, z + 0.03f);
        tex(s, "white_wool", x + 0.06f, 0.06f, z - 0.035f, x + w - 0.06f, h - 0.06f, z + 0.035f, 0xFFF4ECD8);
        tex(s, "green_wool", x + 0.15f, 0.9f, z - 0.04f, x + w - 0.2f, 1.0f, z + 0.04f, 0xFF6F8F4A);
        tex(s, "green_wool", x + 0.3f, 1.0f, z - 0.04f, x + 0.45f, 1.3f, z + 0.04f, 0xFF6F8F4A);
        tex(s, "black_concrete", x + w - 0.16f, 1.0f, z - 0.045f, x + w - 0.1f, 1.15f, z + 0.045f);
    }

    // ================================================================ 怪力狼：ちゃぶ台ごと投げ飛ばす

    static final class Kairiki extends Finisher {
        Kairiki() {
            super("kairiki", "怪力狼 ― ちゃぶ台返し", 80, 38);
        }

        @Override
        float camera(float t, Vector3f eye, Vector3f target) {
            return shots(t, eye, target, new float[][]{
                    {0, 30, 3.6f, 1.6f, 1.2f, 3.2f, 1.4f, 0.8f, 0, 0.9f, -0.6f, 0, 1.2f, -0.6f, 55},
                    {30, 56, -2.8f, 1.0f, 2.6f, -2.6f, 1.3f, 3.6f, 0, 1.0f, 1.6f, 0, 0.7f, 3.2f, 60},
                    {56, 82, 0.6f, 0.6f, 1.3f, 0.5f, 0.5f, 1.1f, 0, 1.7f, -1.2f, 0, 1.9f, -1.2f, 55}});
        }

        @Override
        void scene(Scene3D s, float t, Ctx c) {
            room(s, false);
            // ちゃぶ台：持ち上げる → 頭の上 → 投げる
            float lift = tr(t, 0, 0, 12, 0, 26, 1.6f, 32, 1.9f, 38, 1.2f);
            float tz = tr(t, 0, 0, 32, -0.8f, 38, 0.6f, 46, 2.6f, 52, 3.4f);
            float ty = t < 32 ? lift + 0.35f : tr(t, 32, 2.25f, 38, 1.5f, 46, 1.0f, 52, 0.4f, 58, 0.1f);
            float tilt = tr(t, 0, 0, 20, -20, 32, -40, 46, 120, 52, 170, 58, 180);
            s.ps.pushPose();
            s.ps.translate(0, ty, tz - 0.2f);
            s.ps.mulPose(Axis.XP.rotationDegrees(tilt));
            tex(s, "dark_oak_planks", -0.6f, 0, -0.6f, 0.6f, 0.07f, 0.6f);
            for (float[] l : new float[][]{{-0.45f, -0.45f}, {0.4f, -0.45f}, {-0.45f, 0.4f}, {0.4f, 0.4f}}) {
                tex(s, "dark_oak_planks", l[0], -0.3f, l[1], l[0] + 0.06f, 0, l[1] + 0.06f);
            }
            s.ps.popPose();
            // お皿とお茶（はね上がって飛んでいく）
            for (int i = 0; i < 4; i++) {
                float fly = Mth.clamp((t - 14 - i) / 30f, 0f, 1f);
                float x = -0.3f + i * 0.2f + fly * (i - 1.5f) * 1.4f;
                float y = 0.42f + (float) Math.sin(fly * Math.PI) * (1.6f + i * 0.2f) + fly * (i % 2) * 0.3f;
                float z = -0.2f + fly * (i % 2 == 0 ? 1.4f : -0.9f);
                tex(s, i % 2 == 0 ? "white_concrete" : "light_blue_terracotta", x - 0.08f, y, z - 0.08f, x + 0.08f, y + 0.04f, z + 0.08f);
            }
            // 自分：ちゃぶ台の向こうに座っている → 吹っ飛ぶ → 障子に突っこむ
            PlayerModel<?> vm = c.vm();
            float vz = tr(t, 0, 0.9f, 38, 0.9f, 48, 3.6f, 52, 4.2f);
            float vy = tr(t, 0, -0.55f, 34, -0.55f, 38, -0.3f, 44, 0.8f, 50, 0.3f, 54, 0.0f);
            float vp = tr(t, 0, 0, 38, 0, 46, -70, 52, -90);
            vm.rightLeg.xRot = t < 38 ? -1.5f : tr(t, 38, -1.5f, 46, -0.3f);
            vm.leftLeg.xRot = t < 38 ? -1.5f : tr(t, 38, -1.5f, 46, 0.2f);
            vm.rightArm.xRot = tr(t, 0, -0.4f, 20, -0.4f, 30, -2.6f, 46, -2.9f);
            vm.leftArm.xRot = tr(t, 0, -0.4f, 20, -0.4f, 30, -2.4f, 46, -2.9f);
            vm.head.xRot = tr(t, 0, 0.1f, 16, -0.6f);
            c.victim(s, vm, 0, vy, vz, 180, vp, 0);
            // 障子（突き破る）
            float broken = t > 48 ? 1f : 0f;
            for (int i = -2; i <= 1; i++) {
                if (broken > 0 && i >= -1 && i <= 0) {
                    float k = Mth.clamp((t - 48) / 14f, 0f, 1f);
                    s.ps.pushPose();
                    s.ps.translate(i * 0.9f + 0.45f, 0, 4.6f + k * 0.8f);
                    s.ps.mulPose(Axis.XP.rotationDegrees(k * 80f));
                    fusumaPanel(s, -0.45f, 0, 0.9f, 2.2f);
                    s.ps.popPose();
                } else {
                    fusumaPanel(s, i * 0.9f, 4.6f, 0.9f, 2.2f);
                }
            }
            // 怪力狼：つかむ → 持ち上げる → 投げる → 腕を組む
            PlayerModel<?> km = c.km();
            km.rightArm.xRot = tr(t, 0, -1.2f, 12, -1.3f, 26, -3.0f, 32, -3.1f, 38, -1.4f, 46, -0.9f, 60, -0.9f, 66, -1.4f);
            km.leftArm.xRot = tr(t, 0, -1.2f, 12, -1.3f, 26, -3.0f, 32, -3.1f, 38, -1.4f, 46, -0.9f, 60, -0.9f, 66, -1.4f);
            km.rightArm.zRot = tr(t, 60, 0, 66, 0.9f);
            km.leftArm.zRot = tr(t, 60, 0, 66, -0.9f);
            km.rightLeg.xRot = tr(t, 30, 0, 36, -0.6f, 44, 0);
            km.head.xRot = tr(t, 0, 0.4f, 26, -0.3f, 38, 0.1f, 60, -0.2f);
            float kp = tr(t, 0, 25, 12, 30, 26, -10, 32, -14, 38, 18, 46, 4, 60, 0);
            c.killer(s, km, 0, 0, -1.1f, 0, kp, 0);
        }

        @Override
        void marks(Scene3D s, float t, Ctx c, Map<String, float[]> out) {
            out.put("head", s.toScreen(c.killerPos.x, 2.3f, c.killerPos.z));
            out.put("wall", s.toScreen(0, 1.6f, 4.6f));
        }

        @Override
        void overlay(GuiGraphics g, Scene3D s, float t, Ctx c, Map<String, float[]> marks) {
            pop(g, marks.get("head"), t, 22, "ふんっ！！", 0xFFFFE08A, 2.4f);
            pop(g, marks.get("wall"), t, 48, "バリーン", 0xFFFFFFFF, 2.6f);
        }

        @Override
        void sound(int t) {
            if (t == 14) {
                play(SoundEvents.IRON_GOLEM_ATTACK, 0.6f);
            }
            if (t == 20 || t == 26) {
                play(SoundEvents.DECORATED_POT_BREAK, 1.2f);
            }
            if (t == 34) {
                play(SoundEvents.PLAYER_ATTACK_SWEEP, 0.5f);
            }
            if (t == 38) {
                play(SoundEvents.ANVIL_LAND, 0.9f);
                play(SoundEvents.PLAYER_HURT, 0.8f);
            }
            if (t == 48) {
                play(SoundEvents.GLASS_BREAK, 0.7f);
                play(SoundEvents.WOOD_BREAK, 0.8f);
            }
            if (t == 62) {
                play(SoundEvents.IRON_GOLEM_REPAIR, 0.7f);
            }
        }
    }

    // ================================================================ 呪狼：呪符と青い鬼火

    static final class Juro extends Finisher {
        Juro() {
            super("juro", "呪狼 ― 鬼火の呪い", 80, 22, 60);
        }

        @Override
        float camera(float t, Vector3f eye, Vector3f target) {
            if (t < 22) {
                return shots(t, eye, target, new float[][]{{0, 22, -1.6f, 1.5f, -2.6f, -1.2f, 1.6f, -2.0f, 0, 1.5f, 0.2f, 0, 1.6f, 0, 50}});
            }
            // 自分のまわりをゆっくり回る
            float a = (t - 22) * 0.045f;
            float r = tr(t, 22, 3.0f, 60, 2.3f, 80, 2.8f);
            eye.set((float) Math.sin(a) * r, tr(t, 22, 1.5f, 60, 1.9f, 80, 2.4f), (float) Math.cos(a) * r);
            target.set(0, tr(t, 22, 1.5f, 60, 1.6f, 80, 0.6f), 0);
            return 50;
        }

        @Override
        void scene(Scene3D s, float t, Ctx c) {
            s.bright = s.bright * 0.75f;
            room(s, true);
            s.bright = s.bright / 0.75f;
            // 自分：呪符を貼られて固まり、浮いてから倒れる
            PlayerModel<?> vm = c.vm();
            float lift = tr(t, 0, 0, 22, 0, 40, 0.35f, 60, 0.4f, 66, 0);
            float vp = tr(t, 0, 0, 60, 0, 70, -90);
            vm.rightArm.xRot = tr(t, 0, 0, 22, -0.3f, 40, -0.5f);
            vm.leftArm.xRot = tr(t, 0, 0, 22, -0.3f, 40, -0.5f);
            vm.rightArm.zRot = tr(t, 22, 0, 40, 0.8f);
            vm.leftArm.zRot = tr(t, 22, 0, 40, -0.8f);
            vm.head.xRot = tr(t, 0, 0, 22, -0.4f, 40, -0.6f);
            c.victim(s, vm, 0, lift, 0, 180, vp, 0);
            // 呪狼
            PlayerModel<?> km = c.km();
            km.rightArm.xRot = tr(t, 0, -0.4f, 10, -2.6f, 16, -1.6f, 22, -1.4f, 30, -0.3f);
            km.leftArm.xRot = tr(t, 0, -0.2f, 24, -0.6f, 30, -0.3f);
            km.head.xRot = tr(t, 0, 0.2f, 60, 0.1f, 70, -0.4f);
            c.killer(s, km, 0, 0, -2.4f, 0, 0, 0);
            // 呪符（飛んで額に貼りつく）
            float fly = Mth.clamp((t - 14) / 8f, 0f, 1f);
            float px = 0;
            float py = Mth.lerp(fly, 2.2f, 1.75f + lift) + (float) Math.sin(fly * Math.PI) * 0.4f;
            float pz = Mth.lerp(fly, -2.1f, -0.27f);
            if (t > 6) {
                s.ps.pushPose();
                s.ps.translate(px, py, pz);
                s.ps.mulPose(Axis.YP.rotationDegrees(t < 22 ? t * 40 : 0));
                tex(s, "white_wool", -0.12f, -0.2f, -0.01f, 0.12f, 0.2f, 0.01f, 0xFFF4E7C5);
                tex(s, "red_wool", -0.02f, -0.16f, -0.015f, 0.02f, 0.16f, 0.015f);
                tex(s, "red_wool", -0.08f, 0.08f, -0.015f, 0.08f, 0.1f, 0.015f);
                s.ps.popPose();
            }
            // 青い鬼火（まわりを回りながら上がる → 最後に燃え上がる）
            if (t > 22) {
                int n = 7;
                for (int i = 0; i < n; i++) {
                    float a = (t * 0.12f) + i * (float) (Math.PI * 2 / n);
                    float r = tr(t, 22, 1.6f, 60, 0.7f, 70, 0.3f);
                    float y = 0.4f + ((t * 0.04f + i * 0.17f) % 1f) * 1.8f;
                    float x = (float) Math.cos(a) * r;
                    float z = (float) Math.sin(a) * r;
                    float sz = t > 60 ? 0.18f + (t - 60) * 0.03f : 0.18f;
                    tex(s, "soul_fire_0", x - sz / 2, y, z - sz / 2, x + sz / 2, y + sz * 1.6f, z + sz / 2, 0xE0FFFFFF);
                }
            }
        }

        @Override
        void marks(Scene3D s, float t, Ctx c, Map<String, float[]> out) {
            out.put("head", s.toScreen(c.victimPos.x, 2.3f, c.victimPos.z));
        }

        @Override
        void overlay(GuiGraphics g, Scene3D s, float t, Ctx c, Map<String, float[]> marks) {
            if (t > 24 && t < 66) {
                int w = g.guiWidth();
                int h = g.guiHeight();
                float a = Mth.clamp((t - 24) / 10f, 0f, 1f) * 0.25f;
                g.fillGradient(0, 0, w, h / 3, Ui.alpha(0xFF3040FF, a), 0x00000000);
                g.fillGradient(0, h - h / 3, w, h, 0x00000000, Ui.alpha(0xFF3040FF, a));
            }
            pop(g, marks.get("head"), t, 22, "ペタッ", 0xFFFFFFFF, 2.0f);
            pop(g, marks.get("head"), t, 60, "呪", 0xFF9FB4FF, 4.0f);
        }

        @Override
        void sound(int t) {
            if (t == 4) {
                play(SoundEvents.EVOKER_PREPARE_ATTACK, 0.8f);
            }
            if (t == 14) {
                play(SoundEvents.PLAYER_ATTACK_SWEEP, 1.4f);
            }
            if (t == 22) {
                play(SoundEvents.SOUL_ESCAPE.value(), 0.8f);
                play(SoundEvents.EVOKER_CAST_SPELL, 0.9f);
            }
            if (t == 40) {
                play(SoundEvents.SOUL_ESCAPE.value(), 0.6f);
            }
            if (t == 60) {
                play(SoundEvents.FIRECHARGE_USE, 0.6f);
                play(SoundEvents.PLAYER_HURT, 0.7f);
            }
            if (t == 68) {
                play(SoundEvents.PLAYER_BIG_FALL, 0.8f);
            }
        }
    }

    // ================================================================ 共通：卓球ラケット

    static final class PingPong extends Finisher {
        PingPong() {
            super("pingpong", "温泉卓球 ― 必殺スマッシュ", 78, 38);
        }

        @Override
        float camera(float t, Vector3f eye, Vector3f target) {
            return shots(t, eye, target, new float[][]{
                    {0, 26, 3.4f, 1.8f, 0.2f, 3.0f, 1.6f, 0.2f, 0, 1.0f, 0, 0, 1.0f, 0, 58},
                    {26, 40, -1.5f, 1.7f, -2.9f, -1.3f, 1.6f, -2.5f, 0, 1.4f, 0.2f, 0, 1.5f, 1.6f, 40},
                    {40, 80, 2.4f, 1.0f, 2.6f, 2.0f, 1.2f, 2.4f, 0, 0.6f, 1.8f, 0, 0.8f, 1.6f, 55}});
        }

        @Override
        void scene(Scene3D s, float t, Ctx c) {
            room(s, false);
            // 卓球台
            tex(s, "green_concrete", -0.7f, 0.72f, -1.35f, 0.7f, 0.78f, 1.35f, 0xFF2E6B3A);
            tex(s, "white_concrete", -0.02f, 0.781f, -1.35f, 0.02f, 0.785f, 1.35f);
            tex(s, "white_wool", -0.75f, 0.78f, -0.01f, 0.75f, 0.93f, 0.01f);
            for (float[] l : new float[][]{{-0.6f, -1.2f}, {0.55f, -1.2f}, {-0.6f, 1.15f}, {0.55f, 1.15f}}) {
                tex(s, "black_concrete", l[0], 0, l[1], l[0] + 0.05f, 0.72f, l[1] + 0.05f);
            }
            // 犯人（向こう側）：サーブ → スマッシュ
            PlayerModel<?> km = c.km();
            km.rightArm.xRot = tr(t, 0, -0.6f, 14, -0.8f, 22, -2.9f, 30, -3.1f, 34, -0.4f, 44, -0.3f, 60, -2.9f);
            km.rightArm.zRot = tr(t, 22, 0, 30, -0.5f, 34, 0.3f);
            km.leftArm.xRot = tr(t, 0, -0.9f, 8, -1.4f, 14, -0.6f, 60, -2.9f);
            km.rightLeg.xRot = tr(t, 26, 0, 32, -0.5f, 40, 0);
            c.killer(s, km, 0, 0, -1.9f, 0, tr(t, 0, 0, 26, -10, 33, 18, 44, 0), 0);
            // ラケット（右手）
            Vector3f hand = new Vector3f(-0.35f, 1.35f, -1.9f);
            float swing = tr(t, 0, 0, 22, -120, 30, -150, 34, 20, 44, 30);
            s.ps.pushPose();
            s.ps.translate(hand.x, hand.y, hand.z);
            s.ps.mulPose(Axis.XP.rotationDegrees(swing));
            tex(s, "red_concrete", -0.11f, 0.38f, -0.01f, 0.11f, 0.6f, 0.03f);
            tex(s, "oak_planks", -0.025f, 0.2f, -0.01f, 0.025f, 0.38f, 0.03f);
            s.ps.popPose();
            // 球：トスして → スマッシュで一直線に自分の額へ
            float bx;
            float by;
            float bz;
            if (t < 32) {
                bx = -0.25f;
                by = 1.2f + (float) Math.sin(Mth.clamp((t - 6) / 24f, 0f, 1f) * Math.PI) * 1.0f;
                bz = -1.8f;
            } else {
                float k = Mth.clamp((t - 32) / 6f, 0f, 1f);
                bx = Mth.lerp(k, -0.25f, 0);
                by = Mth.lerp(k, 1.8f, 1.72f);
                bz = Mth.lerp(k, -1.6f, 1.6f);
                if (t > 38) {
                    float b = (t - 38) / 20f;
                    bx = b * 1.4f;
                    by = 1.72f + (float) Math.sin(Math.min(1f, b) * Math.PI) * 0.8f - b * 1.6f;
                    bz = 1.6f + b * 0.8f;
                }
            }
            tex(s, "white_concrete", bx - 0.04f, by - 0.04f, bz - 0.04f, bx + 0.04f, by + 0.04f, bz + 0.04f);
            if (t > 32 && t < 38) {
                for (int i = 1; i < 6; i++) {
                    float k = Mth.clamp((t - 32 - i * 0.6f) / 6f, 0f, 1f);
                    float x = Mth.lerp(k, -0.25f, 0);
                    float z = Mth.lerp(k, -1.6f, 1.6f);
                    tex(s, "white_concrete", x - 0.03f, 1.75f - 0.03f, z - 0.03f, x + 0.03f, 1.75f + 0.03f, z + 0.03f,
                            ((200 - i * 35) << 24) | 0xFFFFFF);
                }
            }
            // 自分（手前側）：球が額に当たって、くるくる回って倒れる
            PlayerModel<?> vm = c.vm();
            float spin = tr(t, 0, 0, 38, 0, 52, 540);
            float vp = tr(t, 0, 0, 38, 0, 48, -30, 56, -90);
            vm.rightArm.xRot = tr(t, 0, -0.5f, 38, -0.5f, 44, -2.5f);
            vm.leftArm.xRot = tr(t, 0, -0.3f, 38, -0.3f, 44, -2.4f);
            vm.head.xRot = tr(t, 0, 0, 38, -0.9f);
            c.victim(s, vm, 0, 0, tr(t, 0, 1.9f, 38, 1.9f, 56, 2.6f), 180 + spin, vp, 0);
        }

        @Override
        void marks(Scene3D s, float t, Ctx c, Map<String, float[]> out) {
            out.put("k", s.toScreen(c.killerPos.x, 2.4f, c.killerPos.z));
            out.put("v", s.toScreen(c.victimPos.x, 2.3f, c.victimPos.z));
        }

        @Override
        void overlay(GuiGraphics g, Scene3D s, float t, Ctx c, Map<String, float[]> marks) {
            pop(g, marks.get("k"), t, 30, "スマーッシュ！！", 0xFFFFE08A, 2.4f);
            pop(g, marks.get("v"), t, 38, "カコーン", 0xFFFFFFFF, 2.0f);
        }

        @Override
        void sound(int t) {
            if (t == 8 || t == 16) {
                play(SoundEvents.BAMBOO_WOOD_HIT, 1.8f);
            }
            if (t == 31) {
                play(SoundEvents.PLAYER_ATTACK_STRONG, 1.4f);
                play(SoundEvents.BAMBOO_WOOD_HIT, 2.0f);
            }
            if (t == 38) {
                play(SoundEvents.NOTE_BLOCK_HAT.value(), 0.6f);
                play(SoundEvents.PLAYER_HURT, 0.9f);
            }
            if (t == 54) {
                play(SoundEvents.PLAYER_BIG_FALL, 0.8f);
            }
        }
    }

    // ================================================================ 共通：枕投げ

    static final class Pillow extends Finisher {
        Pillow() {
            super("pillow", "修学旅行 ― 枕投げ", 76, 30, 46);
        }

        @Override
        float camera(float t, Vector3f eye, Vector3f target) {
            return shots(t, eye, target, new float[][]{
                    {0, 24, 3.2f, 1.4f, 0.4f, 2.8f, 1.5f, 0.0f, 0, 1.0f, 0, 0, 1.1f, 0, 56},
                    {24, 50, 1.5f, 1.4f, 2.6f, 1.1f, 1.2f, 2.1f, 0, 1.2f, 0.4f, 0, 0.8f, 0.6f, 52},
                    {50, 78, -2.4f, 1.6f, 0.0f, -2.0f, 1.8f, -0.4f, 0, 1.2f, -0.6f, 0, 1.6f, -0.9f, 52}});
        }

        @Override
        void scene(Scene3D s, float t, Ctx c) {
            room(s, false);
            // 敷いた布団
            for (float z : new float[]{-2.6f, 0.4f}) {
                tex(s, "white_wool", -0.5f, 0, z, 0.5f, 0.12f, z + 1.9f);
                tex(s, "blue_wool", -0.48f, 0.12f, z + 0.5f, 0.48f, 0.17f, z + 1.88f);
            }
            PlayerModel<?> km = c.km();
            km.rightArm.xRot = tr(t, 0, -0.4f, 14, -3.0f, 22, -3.1f, 26, -0.6f, 34, -3.0f, 40, -3.1f, 43, -0.6f, 56, -2.9f, 76, -2.9f);
            km.leftArm.xRot = tr(t, 0, -0.4f, 56, -0.4f, 60, -2.9f);
            km.rightArm.zRot = tr(t, 56, 0, 62, 0.5f);
            km.leftArm.zRot = tr(t, 56, 0, 62, -0.5f);
            c.killer(s, km, 0, 0, -1.6f, 0, tr(t, 0, 0, 22, -14, 26, 16, 36, -10, 43, 14, 50, 0), 0);
            // 枕（2つ投げる）
            pillowFly(s, t, 26, -1.5f, 1.2f);
            pillowFly(s, t, 43, -1.5f, 1.2f);
            // 自分：1つ目でよろけ、2つ目で布団にあお向け
            PlayerModel<?> vm = c.vm();
            float vp = tr(t, 0, 0, 30, 0, 34, -20, 42, -10, 46, -10, 54, -90);
            vm.rightArm.xRot = tr(t, 0, -0.4f, 30, -0.4f, 34, -2.0f, 46, -2.6f);
            vm.leftArm.xRot = tr(t, 0, -0.4f, 30, -0.4f, 34, -1.8f, 46, -2.6f);
            vm.head.xRot = tr(t, 0, 0, 30, -0.7f, 40, -0.2f, 46, -0.8f);
            c.victim(s, vm, 0, tr(t, 46, 0, 54, 0.12f), tr(t, 0, 1.2f, 46, 1.2f, 54, 1.5f), 180, vp, 0);
            // 羽毛
            for (float hh : hits) {
                float ft = t - hh;
                if (ft > 0 && ft < 24) {
                    for (int i = 0; i < 10; i++) {
                        float a = i * 0.63f + hh;
                        float r = ft * 0.05f;
                        float x = (float) Math.cos(a) * r;
                        float z = 1.2f + (float) Math.sin(a) * r;
                        float y = 1.7f + ft * 0.03f - ft * ft * 0.0012f + (float) Math.sin(ft * 0.3f + i) * 0.05f;
                        tex(s, "white_wool", x - 0.03f, y, z - 0.03f, x + 0.03f, y + 0.012f, z + 0.03f,
                                ((int) ((1 - ft / 24f) * 255) << 24) | 0xFFFFFF);
                    }
                }
            }
        }

        private static void pillowFly(Scene3D s, float t, float start, float z0, float z1) {
            float k = Mth.clamp((t - start) / 4f, 0f, 1f);
            if (t < start - 14) {
                return;
            }
            float x = 0.3f;
            float y;
            float z;
            float rot;
            if (t < start) {
                // 手に持ってふりかぶる
                y = 2.2f;
                z = z0;
                rot = 0;
            } else if (k < 1f) {
                y = Mth.lerp(k, 2.2f, 1.7f);
                z = Mth.lerp(k, z0, z1);
                x = Mth.lerp(k, 0.3f, 0);
                rot = k * 360f;
            } else {
                float d = (t - start - 4) / 14f;
                if (d > 1f) {
                    return;
                }
                x = d * 0.8f;
                y = 1.7f - d * 1.6f + (float) Math.sin(d * Math.PI) * 0.3f;
                z = z1 + 0.3f;
                rot = 360 + d * 200;
            }
            s.ps.pushPose();
            s.ps.translate(x, y, z);
            s.ps.mulPose(Axis.XP.rotationDegrees(rot));
            tex(s, "white_wool", -0.28f, -0.08f, -0.17f, 0.28f, 0.08f, 0.17f);
            tex(s, "light_blue_wool", -0.28f, -0.081f, -0.02f, 0.28f, 0.081f, 0.02f);
            s.ps.popPose();
        }

        @Override
        void marks(Scene3D s, float t, Ctx c, Map<String, float[]> out) {
            out.put("k", s.toScreen(c.killerPos.x, 2.4f, c.killerPos.z));
            out.put("v", s.toScreen(c.victimPos.x, 2.2f, c.victimPos.z));
        }

        @Override
        void overlay(GuiGraphics g, Scene3D s, float t, Ctx c, Map<String, float[]> marks) {
            pop(g, marks.get("k"), t, 20, "くらえっ！", 0xFFFFE08A, 2.0f);
            pop(g, marks.get("v"), t, 30, "ボフッ", 0xFFFFFFFF, 2.0f);
            pop(g, marks.get("v"), t, 46, "ボフッ！！", 0xFFFFFFFF, 2.6f);
            pop(g, marks.get("k"), t, 58, "勝ったー！", 0xFFFFE08A, 2.0f);
        }

        @Override
        void sound(int t) {
            if (t == 26 || t == 43) {
                play(SoundEvents.PLAYER_ATTACK_SWEEP, 1.2f);
            }
            if (t == 30 || t == 46) {
                play(SoundEvents.WOOL_PLACE, 0.6f);
                play(SoundEvents.PLAYER_HURT, 1.0f);
            }
            if (t == 54) {
                play(SoundEvents.WOOL_FALL, 0.6f);
            }
            if (t == 58) {
                play(SoundEvents.VILLAGER_CELEBRATE, 1.2f);
            }
        }
    }

    // ================================================================ 共通：ふすまドーン

    static final class Fusuma extends Finisher {
        Fusuma() {
            super("fusuma", "ふすまドーン", 74, 34);
        }

        @Override
        float camera(float t, Vector3f eye, Vector3f target) {
            return shots(t, eye, target, new float[][]{
                    {0, 30, 0.4f, 1.5f, 3.4f, 0.3f, 1.5f, 3.0f, 0, 1.2f, -0.2f, 0, 1.2f, -0.4f, 52},
                    {30, 46, 2.8f, 1.3f, 1.6f, 2.6f, 1.6f, 1.8f, 0, 0.7f, 0.6f, 0, 0.5f, 0.9f, 55},
                    {46, 76, 0.2f, 3.4f, 1.6f, 0.2f, 3.6f, 1.4f, 0, 0.0f, 0.8f, 0, 0.0f, 0.7f, 56}});
        }

        @Override
        void scene(Scene3D s, float t, Ctx c) {
            room(s, false);
            // 自分の後ろのふすま（向こうから犯人が走ってきて、ふすまごと倒れこむ）
            float fall = tr(t, 0, 0, 30, 0, 38, 90);
            float shake = t > 18 && t < 30 ? (float) Math.sin(t * 2.5f) * 2f * ((t - 18) / 12f) : 0f;
            s.ps.pushPose();
            s.ps.translate(0, 0, -0.5f);
            s.ps.mulPose(Axis.XP.rotationDegrees(fall + shake));
            fusumaPanel(s, -0.9f, 0, 0.9f, 2.2f);
            fusumaPanel(s, 0f, 0, 0.9f, 2.2f);
            s.ps.popPose();
            for (float x : new float[]{-1.9f, 1.0f}) {
                fusumaPanel(s, x, -0.5f, 0.9f, 2.2f);
            }
            // 犯人：ふすまの向こうを走ってくる（影）→ ふすまの上に乗る → 決めポーズ
            PlayerModel<?> km = c.km();
            float run = t < 30 ? (float) Math.sin(t * 1.1f) : 0f;
            km.rightArm.xRot = t < 30 ? run * 1.0f : tr(t, 30, -1.6f, 38, -1.0f, 50, -1.0f, 56, -2.9f);
            km.leftArm.xRot = t < 30 ? -run * 1.0f : tr(t, 30, -1.6f, 38, -1.0f, 50, -1.0f, 56, -0.2f);
            km.rightLeg.xRot = t < 30 ? -run * 1.0f : 0;
            km.leftLeg.xRot = t < 30 ? run * 1.0f : 0;
            float kz = tr(t, 0, -4.0f, 28, -1.0f, 38, 0.4f, 44, 0.5f);
            float ky = tr(t, 0, 0, 30, 0, 38, 0.2f, 44, 0.16f);
            float kp = tr(t, 0, 10, 28, 12, 34, 40, 40, 0);
            if (t < 30) {
                // ふすまの向こう：影のように暗く
                float old = s.bright;
                s.bright = old * 0.35f;
                c.killer(s, km, 0, ky, kz, 0, kp, 0);
                s.bright = old;
            } else {
                c.killer(s, km, 0, ky, kz, 0, kp, 0);
            }
            // 自分：ふすまの下じき（足だけ出る）
            PlayerModel<?> vm = c.vm();
            float vp = tr(t, 0, 0, 32, 0, 38, 90);
            vm.rightArm.xRot = tr(t, 0, 0, 26, -0.6f, 34, -2.8f);
            vm.leftArm.xRot = tr(t, 0, 0, 26, -0.4f, 34, -2.8f);
            vm.head.yRot = tr(t, 0, 0, 22, 0, 28, 1.2f, 32, 0.4f);
            float kick = t > 40 ? (float) Math.sin(t * 1.4f) * 0.5f * Math.max(0f, 1f - (t - 40) / 30f) : 0f;
            vm.rightLeg.xRot = kick;
            vm.leftLeg.xRot = -kick;
            c.victim(s, vm, 0, 0, 0.2f, 0, vp, 0);
        }

        @Override
        void marks(Scene3D s, float t, Ctx c, Map<String, float[]> out) {
            out.put("mid", s.toScreen(0, 1.6f, 0.4f));
            out.put("v", s.toScreen(0, 2.2f, 0.2f));
        }

        @Override
        void overlay(GuiGraphics g, Scene3D s, float t, Ctx c, Map<String, float[]> marks) {
            pop(g, marks.get("v"), t, 24, "？", 0xFFFFFFFF, 2.4f);
            pop(g, marks.get("mid"), t, 34, "ドーン！！", 0xFFFFE08A, 3.2f);
            pop(g, marks.get("mid"), t, 52, "ピクピク…", 0xFFFFFFFF, 1.6f);
        }

        @Override
        void sound(int t) {
            if (t == 6 || t == 12 || t == 18 || t == 23 || t == 27) {
                play(SoundEvents.WOOD_STEP, 0.9f + t * 0.01f);
            }
            if (t == 34) {
                play(SoundEvents.ANVIL_LAND, 0.6f);
                play(SoundEvents.WOOD_BREAK, 0.6f);
                play(SoundEvents.PLAYER_HURT, 0.8f);
            }
            if (t == 52) {
                play(SoundEvents.CHICKEN_EGG, 1.4f);
            }
        }
    }

    // ================================================================ 共通：背負い投げ

    static final class Seoi extends Finisher {
        Seoi() {
            super("seoi", "柔の道 ― 背負い投げ", 76, 40);
        }

        @Override
        float camera(float t, Vector3f eye, Vector3f target) {
            return shots(t, eye, target, new float[][]{
                    {0, 24, 3.4f, 1.5f, 0.2f, 3.0f, 1.4f, 0.1f, 0, 1.2f, 0, 0, 1.2f, 0, 52},
                    {24, 42, 2.6f, 0.6f, 1.8f, 2.2f, 0.4f, 1.4f, 0, 1.6f, 0, 0, 0.8f, 0.5f, 60},
                    {42, 78, 0.1f, 4.2f, 0.9f, 0.1f, 3.8f, 0.8f, 0, 0, 0.6f, 0, 0, 0.6f, 50}});
        }

        @Override
        void scene(Scene3D s, float t, Ctx c) {
            room(s, false);
            // 畳（試合場）
            tex(s, "green_terracotta", -1.5f, 0, -1.5f, 1.5f, 0.02f, 2.5f, 0xFF9DAF6A);
            // 犯人：えりをつかむ → くるっと背を向ける → 投げる → 「一本」
            PlayerModel<?> km = c.km();
            float turn = tr(t, 0, 0, 16, 0, 24, 180, 60, 180);
            km.rightArm.xRot = tr(t, 0, -0.4f, 10, -1.6f, 24, -2.6f, 40, -0.6f);
            km.leftArm.xRot = tr(t, 0, -0.4f, 10, -1.5f, 24, -2.2f, 40, -0.8f, 54, -0.8f, 60, -3.0f);
            km.leftArm.zRot = tr(t, 54, 0, 60, -0.3f);
            km.rightLeg.xRot = tr(t, 16, 0, 24, -0.5f, 40, 0);
            km.leftLeg.xRot = tr(t, 16, 0, 24, 0.4f, 40, 0);
            float kp = tr(t, 0, 0, 24, 0, 32, 45, 40, 30, 50, 0);
            c.killer(s, km, 0, 0, -0.55f, turn, kp, 0);
            // 自分：前に引きこまれ → 背中に乗って弧をえがき → 畳にたたきつけられる
            PlayerModel<?> vm = c.vm();
            float arc = Mth.clamp((t - 24) / 16f, 0f, 1f);
            arc = arc * arc * (3 - 2 * arc);
            float ang = arc * 180f;
            float vx = 0;
            float vz = tr(t, 0, 0.4f, 16, 0.2f, 24, -0.2f);
            float vy = 0;
            float vp = tr(t, 0, 0, 16, 10, 24, 20);
            if (t >= 24) {
                // 犯人の肩を中心に、前から後ろへ大きく回る
                float r = 1.0f;
                double a = Math.toRadians(ang);
                vz = -0.55f + (float) Math.cos(a) * 0.35f - (float) Math.sin(a) * 0.4f;
                vy = (float) Math.sin(a) * r * 1.1f;
                vp = 20 + ang;
                if (t >= 40) {
                    vz = -0.55f - 0.75f - 0.6f;
                    vy = 0.1f;
                    vp = -90;
                }
            }
            vm.rightArm.xRot = tr(t, 0, -0.4f, 16, -1.6f, 40, -2.8f);
            vm.leftArm.xRot = tr(t, 0, -0.2f, 24, -2.4f, 40, -2.8f);
            vm.rightLeg.xRot = tr(t, 24, 0, 32, -0.6f, 40, 0);
            vm.head.xRot = tr(t, 0, 0, 40, -0.5f);
            c.victim(s, vm, vx, vy, vz, 180, vp, 0);
        }

        @Override
        void marks(Scene3D s, float t, Ctx c, Map<String, float[]> out) {
            out.put("k", s.toScreen(0, 2.4f, -0.55f));
        }

        @Override
        void overlay(GuiGraphics g, Scene3D s, float t, Ctx c, Map<String, float[]> marks) {
            pop(g, marks.get("k"), t, 22, "せいっ！", 0xFFFFE08A, 2.2f);
            if (t > 46) {
                int w = g.guiWidth();
                int h = g.guiHeight();
                float a = Mth.clamp((t - 46) / 4f, 0f, 1f);
                float sc = 4.2f * (t < 50 ? 1.3f - (t - 46) / 4f * 0.3f : 1f);
                Ui.text(g, Ui.serif("一本！"), w / 2f, h * 0.24f, Ui.alpha(0xFFFFFFFF, a), sc, 0, true);
            }
        }

        @Override
        void sound(int t) {
            if (t == 10) {
                play(SoundEvents.ARMOR_EQUIP_LEATHER.value(), 0.8f);
            }
            if (t == 24) {
                play(SoundEvents.PLAYER_ATTACK_SWEEP, 0.7f);
            }
            if (t == 40) {
                play(SoundEvents.PLAYER_BIG_FALL, 0.6f);
                play(SoundEvents.NOTE_BLOCK_BASEDRUM.value(), 0.5f);
                play(SoundEvents.PLAYER_HURT, 0.8f);
            }
            if (t == 46) {
                play(SoundEvents.BELL_BLOCK, 1.2f);
            }
        }
    }

    // ================================================================ 特別：猫又の祟り（猫又を襲った犯人が見る）

    static final class Neko extends Finisher {
        Neko() {
            super("neko", "猫又の祟り", 70, 36, 46);
        }

        @Override
        float camera(float t, Vector3f eye, Vector3f target) {
            return shots(t, eye, target, new float[][]{
                    {0, 28, 0.6f, 1.4f, 3.2f, 0.5f, 1.4f, 2.8f, 0, 1.4f, -0.8f, 0, 1.6f, -1.2f, 55},
                    {28, 72, 2.6f, 1.2f, 1.4f, 2.2f, 1.3f, 1.0f, 0, 0.6f, 0, 0, 0.5f, 0.1f, 55}});
        }

        @Override
        void scene(Scene3D s, float t, Ctx c) {
            s.bright = s.bright * 0.7f;
            room(s, true);
            s.bright = s.bright / 0.7f;
            PlayerModel<?> vm = c.vm();
            float vp = tr(t, 0, 0, 36, 0, 40, -30, 50, -90);
            vm.rightArm.xRot = tr(t, 0, 0, 20, -1.6f, 36, -2.6f);
            vm.leftArm.xRot = tr(t, 0, 0, 20, -1.4f, 36, -2.5f);
            vm.head.xRot = tr(t, 0, 0, 20, -0.6f);
            c.victim(s, vm, 0, 0, 0.4f, 180, vp, 0);
            // 倒れた猫又の体から、大きな影の猫が立ち上がる
            float rise = Mth.clamp((t - 4) / 14f, 0f, 1f);
            float leap = Mth.clamp((t - 30) / 6f, 0f, 1f);
            float cz = Mth.lerp(leap, -2.2f, 0.1f);
            float cy = (float) Math.sin(leap * Math.PI) * 0.9f;
            catDraw(s, 0, cy, cz, 0, 2.8f * (0.4f + rise * 0.6f), leap > 0 && leap < 1 ? -25 : 0, rise);
            c.killerPos.set(0, 0, cz);
        }

        @Override
        void marks(Scene3D s, float t, Ctx c, Map<String, float[]> out) {
            out.put("cat", s.toScreen(0, 2.0f, -2.0f));
        }

        @Override
        void overlay(GuiGraphics g, Scene3D s, float t, Ctx c, Map<String, float[]> marks) {
            pop(g, marks.get("cat"), t, 18, "シャーッ！！", 0xFFE1B3FF, 2.6f);
        }

        @Override
        void sound(int t) {
            if (t == 4) {
                play(SoundEvents.CAT_AMBIENT, 0.5f);
            }
            if (t == 18) {
                play(SoundEvents.CAT_HISS, 0.5f);
            }
            if (t == 36 || t == 46) {
                play(SoundEvents.PLAYER_ATTACK_SWEEP, 0.8f);
                play(SoundEvents.PLAYER_HURT, 0.8f);
            }
            if (t == 50) {
                play(SoundEvents.PLAYER_BIG_FALL, 0.8f);
            }
        }
    }
}
