package com.minpaku.client.model;

import com.minpaku.game.MarkerType;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;

/**
 * 家具を高精細モデル（assets/minpaku/hires/<ブロックID>.bbmodel）で描き、タスクの動きをつける。
 * 動きはすべて「なめらかな曲線」でつなぐ（急に止まったり跳んだりしない）。
 */
public final class FurnitureHiRes {
    private FurnitureHiRes() {
    }

    private static float sm(float x) {
        x = Mth.clamp(x, 0f, 1f);
        return x * x * x * (x * (x * 6 - 15) + 10);   // smootherstep
    }

    /** 手を動かした瞬間の動き（1 → 0 へ、ゆっくり止まる） */
    private static float hit(FurnitureArt.Anim an, float len) {
        float s = an.since();
        if (s >= len) {
            return 0f;
        }
        float k = 1f - s / len;
        // 立ち上がりもなめらかに（最初の 2tick で 0 → 1）
        return k * k * sm(s / 2f);
    }

    /** 開け閉めする扉：作業中は開き、終わってしばらくすると閉まる */
    private static float door(FurnitureArt.Anim an, MarkerType t) {
        boolean act = an.active(t) || an.work() >= 1f;
        float s = an.since();
        if (act && s < 240) {
            return sm(s / 8f);
        }
        return 1f - sm((s - 240) / 14f);
    }

    public static boolean draw(MarkerType type, FurnitureArt.Anim an, PoseStack ps, MultiBufferSource buf, int light) {
        HiRes m = HiRes.get(type.blockId);
        if (m == null) {
            return false;
        }
        float sz = FurnitureArt.size(type);
        ps.pushPose();
        ps.translate(0.5, 0, 0.5);
        ps.scale(sz, sz, sz);
        ps.translate(-0.5, 0, -0.5);
        m.render(ps, buf, light, OverlayTexture.NO_OVERLAY, animator(type, an));
        extras(type, an, ps, buf, light);
        ps.popPose();
        return true;
    }

    private static final float P = 1f / 16f;

    private static HiRes.Animator animator(MarkerType type, FurnitureArt.Anim an) {
        final float w = an.work();
        final float t = an.time();
        final float s = an.since();
        final boolean act = an.active(type);
        return (g, ps, ox, oy, oz) -> {
            switch (type) {
                case BELL -> {
                    if (g.equals("bell")) {
                        float k = s < 60 ? (1f - s / 60f) : 0f;
                        HiRes.rotate(ps, ox, oy, oz, (float) Math.sin(s * 0.42f) * 22f * k * k * sm(s / 3f), 0, 0);
                    } else if (g.equals("striker")) {
                        HiRes.rotate(ps, ox, oy, oz, -hit(an, 14) * 22f, 0, 0);
                    }
                }
                case VENT -> {
                    if (g.equals("lid")) {
                        float open = s < 40 ? (s < 8 ? sm(s / 8f) : 1f - sm((s - 22) / 18f)) : 0f;
                        HiRes.rotate(ps, ox, oy, oz, -open * 75f, 0, 0);
                    }
                }
                case BREAKER -> {
                    if (g.equals("lever")) {
                        HiRes.rotate(ps, ox, oy, oz, 50 - sm(w) * 100, 0, 0);
                    } else if (g.equals("door")) {
                        HiRes.rotate(ps, ox, oy, oz, 0, -door(an, type) * 115f, 0);
                    }
                }
                case GAS_VALVE -> {
                    if (g.equals("wheel")) {
                        HiRes.rotate(ps, ox, oy, oz, 0, 0, sm(w) * 540f + (act ? (float) Math.sin(t * 0.6f) * 3f : 0f));
                    }
                }
                case BOILER -> {
                    if (g.equals("body") && act) {
                        ps.translate((float) Math.sin(t * 2.3f) * 0.25f * P, 0, (float) Math.cos(t * 1.9f) * 0.25f * P);
                    } else if (g.equals("fire")) {
                        return w < 0.95f;
                    } else if (g.equals("needle")) {
                        HiRes.rotate(ps, ox, oy, oz, 0, 0, 60 - sm(w) * 120 + (act ? (float) Math.sin(t * 3.1f) * 6f : 0f));
                    }
                }
                case HOKORA -> {
                    boolean broken = w >= 0.99f;
                    float b = broken ? sm(s / 10f) : 0f;
                    switch (g) {
                        case "shrine" -> {
                            HiRes.rotate(ps, ox, oy, oz, -8 * b, 0, 14 * b);
                        }
                        case "door_l" -> HiRes.rotate(ps, ox, oy, oz, 0, -70 * b + (act && !broken ? (float) Math.sin(t * 0.8f) * 4 : 0), 0);
                        case "door_r" -> HiRes.rotate(ps, ox, oy, oz, 0, 70 * b - (act && !broken ? (float) Math.sin(t * 0.8f) * 4 : 0), 0);
                        case "roof" -> {
                            ps.translate(4 * b * P, -b * P, 0);
                            HiRes.rotate(ps, ox, oy, oz, 0, 0, 32 * b);
                        }
                        case "rope" -> HiRes.rotate(ps, ox, oy, oz, 0, 0, -60 * b);
                        case "evil" -> {
                            if (!broken) {
                                return false;
                            }
                            float pulse = 0.85f + 0.15f * (float) Math.sin(t * 0.3f);
                            HiRes.scale(ps, ox, oy, oz, pulse, pulse, 1);
                        }
                        default -> {
                        }
                    }
                }
                case WIRING -> {
                    if (g.startsWith("wire")) {
                        return w >= 1f;
                    }
                    if (g.equals("lamp_off")) {
                        return w < 1f;
                    }
                    if (g.equals("lamp_on")) {
                        return w >= 1f;
                    }
                    if (g.equals("door")) {
                        HiRes.rotate(ps, ox, oy, oz, 0, door(an, type) * 110f, 0);
                    }
                }
                case LEDGER -> {
                    if (g.equals("flip")) {
                        float f = act ? (t * 6f) % 180f : 0f;
                        // ページの端で少し止まるように
                        float e = sm(f / 180f) * 180f;
                        HiRes.rotate(ps, ox, oy, oz, 0, 0, e);
                    }
                }
                case SHOJI -> {
                    if (g.equals("screen") && act) {
                        HiRes.rotate(ps, ox, oy, oz, (float) Math.sin(t * 1.7f) * 0.8f, 0, 0);
                    } else if (g.startsWith("hole")) {
                        return w < 1f;
                    } else if (g.startsWith("patch")) {
                        if (w < 1f) {
                            return false;
                        }
                        float pop = s < 10 ? 0.4f + sm(s / 6f) * 0.75f - (s > 6 ? (s - 6) / 4f * 0.15f : 0f) : 1f;
                        HiRes.scale(ps, ox, oy, oz, pop, pop, 1);
                    }
                }
                case FUTON -> {
                    if (g.equals("fold1")) {
                        HiRes.rotate(ps, ox, oy, oz, -180f * (1f - sm(w * 2f)), 0, 0);
                    } else if (g.equals("fold2")) {
                        HiRes.rotate(ps, ox, oy, oz, 180f * (1f - sm(w * 2f - 1f)), 0, 0);
                    } else if (g.equals("pillow")) {
                        float k = sm((w - 0.85f) * 6.7f);
                        if (k <= 0.01f) {
                            return false;
                        }
                        HiRes.scale(ps, ox, oy, oz, k, k, k);
                    }
                }
                case KAMADO -> {
                    if (g.equals("lid") && act) {
                        float bounce = Math.abs((float) Math.sin(t * 0.9f)) * 1.2f;
                        ps.translate(0, bounce * P, 0);
                        HiRes.rotate(ps, ox, oy, oz, 0, 0, (float) Math.sin(t * 0.9f) * 3f);
                    } else if (g.equals("fire")) {
                        if (!act && w < 0.4f) {
                            return false;
                        }
                        float f = 0.85f + 0.15f * (float) Math.sin(t * 1.7f);
                        HiRes.scale(ps, ox, oy, oz, 1, f, 1);
                    }
                }
                case BATH -> {
                    if (g.equals("water")) {
                        float lvl = 9 + sm(w) * 1.5f + (act ? (float) Math.sin(t * 0.5f) * 0.2f : 0f);
                        HiRes.scale(ps, ox, oy, oz, 1, (lvl - 1) / 8f, 1);
                    } else if (g.equals("fire")) {
                        return act || w > 0.2f;
                    }
                }
                case CHOPPING -> {
                    float open = sm(w);
                    if (g.equals("half_l")) {
                        HiRes.rotate(ps, ox, oy, oz, 0, 0, open * 70f);
                    } else if (g.equals("half_r")) {
                        HiRes.rotate(ps, ox, oy, oz, 0, 0, -open * 70f);
                    } else if (g.equals("axe")) {
                        float sw = s < 4 ? sm(s / 4f) : s < 14 ? 1f - sm((s - 4) / 10f) : 0f;
                        HiRes.rotate(ps, ox, oy, oz, 0, 0, 50 + sw * 24);
                    }
                }
                case FIELD -> {
                    if (g.startsWith("veg")) {
                        int k = Integer.parseInt(g.substring(3));
                        float rise = Mth.clamp(w * 8f - k, 0f, 1f);
                        float pop = (rise > 0 && rise < 1) ? hit(an, 10) : 0f;
                        ps.translate(0, (sm(rise) * 6f + pop * 1.5f) * P, 0);
                        if (rise >= 1f) {
                            HiRes.rotate(ps, ox, oy, oz, -80, 0, 0);
                        }
                    }
                }
                case DUST -> {
                    if (g.equals("dust")) {
                        float left = 1f - sm(w);
                        if (left < 0.03f) {
                            return false;
                        }
                        HiRes.scale(ps, ox, oy, oz, 0.6f + 0.4f * left, left, 0.6f + 0.4f * left);
                    } else if (g.equals("broom")) {
                        float sweep = act ? (float) Math.sin(t * 0.8f) * 28f : 0f;
                        HiRes.rotate(ps, ox, oy, oz, 0, sweep, 0);
                        HiRes.rotate(ps, ox, 0, oz, 0, 0, 18);
                    }
                }
                case LAUNDRY -> {
                    if (g.startsWith("cloth")) {
                        int i = Integer.parseInt(g.substring(5));
                        float k = sm(w * 4f - i);
                        if (k <= 0.02f) {
                            return false;
                        }
                        HiRes.rotate(ps, ox, oy, oz, (float) Math.sin(t * 0.08f + i * 1.7f) * 7f, 0, 0);
                        HiRes.scale(ps, ox, oy, oz, 1, k, 1);
                    }
                }
                case VASE -> {
                    if (g.equals("flowers")) {
                        float grow = 0.45f + 0.55f * sm(w);
                        float sway = (float) Math.sin(t * 0.06f) * 2f;
                        HiRes.rotate(ps, ox, oy, oz, 0, 0, sway);
                        HiRes.scale(ps, ox, oy, oz, 1, grow, 1);
                    } else if (g.equals("can")) {
                        if (!act) {
                            return false;
                        }
                        HiRes.rotate(ps, ox, oy, oz, 0, 0, 25 + hit(an, 12) * 15f);
                    }
                }
                case SINK -> {
                    int clean = Math.round(sm(w) * 5);
                    if (g.startsWith("dirty")) {
                        int i = Integer.parseInt(g.substring(5));
                        if (i >= 5 - clean) {
                            return false;
                        }
                        if (act && i == 4 - clean) {
                            HiRes.rotate(ps, ox, oy, oz, 0, 0, (float) Math.sin(t * 1.4f) * 8f);
                        }
                    } else if (g.startsWith("clean")) {
                        return Integer.parseInt(g.substring(5)) < clean;
                    } else if (g.equals("stream")) {
                        return act;
                    }
                }
                case SHOE_RACK -> {
                    if (g.startsWith("shoe")) {
                        int k = Integer.parseInt(g.substring(4));
                        int i = k / 2;
                        int sIdx = k % 2;
                        float seed = (i * 7 + sIdx * 3) % 5 - 2;
                        float neat = sm(w);
                        float jig = hit(an, 8);
                        ps.translate(0, 0, -(1f - neat) * (3 + Math.abs(seed)) * P);
                        HiRes.rotate(ps, ox, oy, oz, 0, (1f - neat) * seed * 18f + jig * seed * 3f, 0);
                    }
                }
                case ANDON -> {
                    float lit = sm(w);
                    if (g.equals("flame")) {
                        if (lit <= 0.05f) {
                            return false;
                        }
                        float f = (0.4f + lit * 0.6f) * (act ? 0.85f + 0.15f * (float) Math.sin(t * 2.3f) : 1f);
                        HiRes.scale(ps, ox, oy, oz, f, f, f);
                    } else if (g.equals("paper_off")) {
                        return lit <= 0.3f;
                    } else if (g.equals("paper_on")) {
                        return lit > 0.3f;
                    }
                }
                case IRORI -> {
                    float fire = Math.max(sm(w), act ? 0.5f : 0f);
                    if (g.equals("ember")) {
                        return fire > 0.05f;
                    }
                    if (g.equals("fire")) {
                        if (fire <= 0.3f) {
                            return false;
                        }
                        float f = (0.4f + fire * 0.6f) * (act ? 0.8f + 0.2f * (float) Math.sin(t * 2f) : 1f);
                        HiRes.scale(ps, ox, oy, oz, 1, f, 1);
                    }
                }
                case BONSAI -> {
                    float neat = sm(w);
                    float snip = hit(an, 8);
                    if (g.startsWith("wild")) {
                        float k = 1f - neat;
                        if (k < 0.04f) {
                            return false;
                        }
                        HiRes.scale(ps, ox, oy, oz, k, k, k);
                    } else if (g.equals("blade_a")) {
                        HiRes.rotate(ps, ox, oy, oz, 0, 0, 20 + snip * 25);
                    } else if (g.equals("blade_b")) {
                        HiRes.rotate(ps, ox, oy, oz, 0, 0, 20 - snip * 25);
                    }
                }
                case TEA -> {
                    if (g.equals("foam")) {
                        float f = sm(w);
                        if (f < 0.04f) {
                            return false;
                        }
                        HiRes.scale(ps, ox, oy, oz, 1, f, 1);
                    } else if (g.equals("whisk")) {
                        if (act) {
                            ps.translate((float) Math.sin(t * 3.1f) * 2.2f * P, 0, 0);
                        } else {
                            ps.translate(4 * P, 0, 4 * P);
                        }
                    }
                }
                case WELL -> {
                    if (g.equals("bucket")) {
                        float up = hit(an, 18);
                        ps.translate(0, up * 12f * P, 0);
                    } else if (g.equals("tubwater")) {
                        float f = (0.6f + sm(w) * 3f) / 3.1f;
                        HiRes.scale(ps, ox, oy, oz, 1, f, 1);
                    }
                }
                case SHOGI -> {
                    if (g.startsWith("piece")) {
                        int i = Integer.parseInt(g.substring(5));
                        int placed = Math.round(sm(w) * 18);
                        if (i >= placed) {
                            return false;
                        }
                        if (i == placed - 1) {
                            ps.translate(0, hit(an, 8) * 1.5f * P, 0);
                        }
                    }
                }
                case KAKEJIKU -> {
                    if (g.equals("scroll")) {
                        float tilt = (1f - sm(w)) * 18f + hit(an, 18) * (float) Math.sin(s * 1.2f) * 6f;
                        HiRes.rotate(ps, ox, oy, oz, 0, 0, tilt);
                    }
                }
                default -> {
                }
            }
            return true;
        };
    }

    /** モデルのほかに足すもの（湯気・煙など） */
    private static void extras(MarkerType type, FurnitureArt.Anim an, PoseStack ps, MultiBufferSource buf, int light) {
        FurnitureArt.Art a = new FurnitureArt.Art(ps, buf, light);
        float t = an.time();
        float w = an.work();
        boolean act = an.active(type);
        switch (type) {
            case KAMADO -> FurnitureArt.steam(a, t, 2.5f, 18, 8.5f, act ? 1f : 0f);
            case BATH -> FurnitureArt.steam(a, t, 8, 10, 8, Math.max(w, act ? 1f : 0f));
            case MEETING -> FurnitureArt.steam(a, t, 16.5f, 12, 16.5f, 0.4f);
            case TEA -> FurnitureArt.steam(a, t, 2, 11, 13, 0.7f);
            case IRORI -> FurnitureArt.steam(a, t, 8, 12, 8, w > 0.6f ? w : 0f);
            case HOKORA -> {
                if (w >= 0.99f) {
                    for (int k = 0; k < 5; k++) {
                        float ph = ((t * 0.6f + k * 7) % 36f) / 36f;
                        float sz = 1.5f + ph * 3;
                        int al = (int) ((1 - ph) * 170);
                        a.glow("purple_concrete", 8 - sz / 2 + (float) Math.sin(t * 0.1f + k) * 2, 14 + ph * 20, 8 - sz / 2,
                                8 + sz / 2, 14 + ph * 20 + sz, 8 + sz / 2, (al << 24) | 0xA040FF);
                    }
                }
            }
            case VASE -> {
                if (act) {
                    for (int i = 0; i < 5; i++) {
                        float ph = ((t * 1.3f + i * 3) % 12) / 12f;
                        a.glass("water_still", 9 + i * 0.4f, 21 - ph * 7, 7.6f + (i % 2) * 0.6f, 9.5f + i * 0.4f, 21.8f - ph * 7,
                                8.2f + (i % 2) * 0.6f, 0xC06FA8FF);
                    }
                }
            }
            default -> {
            }
        }
    }
}
