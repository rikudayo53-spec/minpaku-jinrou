package com.minpaku.client.geo;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.minpaku.MinpakuJinrou;
import com.minpaku.block.GeoHooks;
import com.minpaku.block.MarkerBlock;
import com.minpaku.block.MarkerBlockEntity;
import com.minpaku.client.model.FurnitureArt;
import com.minpaku.client.render.MarkerRenderer;
import com.minpaku.game.MarkerType;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.Mth;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.model.GeoModel;

import java.io.Reader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * 家具を GeckoLib で動かす。
 *  - Blockbench で作った動き（idle / work / use）をいつ流すか
 *  - タスクの進み具合で変わる形（布団が広がる、野菜が増える など）は骨を直接動かす
 * どの家具にどの動きがあるかは assets/minpaku/geo_furniture.json（make_hires.py が書く）
 */
public final class FurnitureGeo {
    private FurnitureGeo() {
    }

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation WORK = RawAnimation.begin().thenLoop("work");
    private static final RawAnimation USE = RawAnimation.begin().thenPlay("use");

    /** 家具の id → 持っている動きの名前 */
    private static final Map<String, Set<String>> INDEX = new HashMap<>();

    public static void register() {
        GeoHooks.handler = FurnitureGeo::handle;
        ResourceManagerHelper.get(PackType.CLIENT_RESOURCES).registerReloadListener(new SimpleSynchronousResourceReloadListener() {
            @Override
            public ResourceLocation getFabricId() {
                return MinpakuJinrou.id("geo_furniture");
            }

            @Override
            public void onResourceManagerReload(ResourceManager manager) {
                INDEX.clear();
                manager.getResource(MinpakuJinrou.id("geo_furniture.json")).ifPresent(res -> {
                    try (Reader r = res.openAsReader()) {
                        JsonObject o = JsonParser.parseReader(r).getAsJsonObject();
                        for (Map.Entry<String, JsonElement> e : o.entrySet()) {
                            Set<String> names = new HashSet<>();
                            for (JsonElement n : (JsonArray) e.getValue()) {
                                names.add(n.getAsString());
                            }
                            INDEX.put(e.getKey(), names);
                        }
                    } catch (Exception ex) {
                        MinpakuJinrou.LOGGER.error("geo_furniture.json を読めませんでした", ex);
                    }
                });
            }
        });
    }

    /** GeckoLib のモデルがある家具か */
    public static boolean hasModel(MarkerType type) {
        return INDEX.containsKey(type.blockId);
    }

    private static boolean has(MarkerType type, String anim) {
        Set<String> s = INDEX.get(type.blockId);
        return s != null && s.contains(anim);
    }

    static MarkerType typeOf(MarkerBlockEntity be) {
        return be.getBlockState().getBlock() instanceof MarkerBlock mb ? mb.type() : null;
    }

    // ------------------------------------------------------------ どの動きを流すか

    private static PlayState handle(MarkerBlockEntity be, AnimationState<MarkerBlockEntity> state, String kind) {
        MarkerType type = typeOf(be);
        if (type == null || !has(type, kind)) {
            return PlayState.STOP;
        }
        FurnitureArt.Anim an = MarkerRenderer.anim(be, state.getPartialTick());
        switch (kind) {
            case "idle" -> {
                return state.setAndContinue(IDLE);
            }
            case "work" -> {
                return an.active(type) ? state.setAndContinue(WORK) : PlayState.STOP;
            }
            default -> {
                // 手を動かすたびに最初から（ワールドに入ったときの古い記録では流さない）
                if (be.lastPulse != be.seenPulse) {
                    boolean first = be.seenPulse == Long.MIN_VALUE;
                    be.seenPulse = be.lastPulse;
                    if (!first || an.since() < 20) {
                        state.getController().forceAnimationReset();
                    }
                }
                return an.since() < 60 ? state.setAndContinue(USE) : PlayState.STOP;
            }
        }
    }

    // ------------------------------------------------------------ 進み具合で骨を動かす

    private static float sm(float x) {
        x = Mth.clamp(x, 0f, 1f);
        return x * x * x * (x * (x * 6 - 15) + 10);
    }

    private static float hit(FurnitureArt.Anim an, float len) {
        float s = an.since();
        if (s >= len) {
            return 0f;
        }
        float k = 1f - s / len;
        return k * k * sm(s / 2f);
    }

    private static float door(FurnitureArt.Anim an, MarkerType t) {
        boolean act = an.active(t) || an.work() >= 1f;
        float s = an.since();
        if (act && s < 240) {
            return sm(s / 8f);
        }
        return 1f - sm((s - 240) / 14f);
    }

    /** 骨の操作（角度は度、位置はピクセル。どちらもマイクラの向き＝Blockbench で見たとおり） */
    static final class Bones {
        private final GeoModel<MarkerBlockEntity> model;

        Bones(GeoModel<MarkerBlockEntity> model) {
            this.model = model;
        }

        GeoBone b(String name) {
            return model.getBone(name).orElse(null);
        }

        void rot(String name, float rx, float ry, float rz) {
            GeoBone b = b(name);
            if (b == null) {
                return;
            }
            var s = b.getInitialSnapshot();
            b.setRotX(s.getRotX() + (float) Math.toRadians(rx));
            b.setRotY(s.getRotY() + (float) Math.toRadians(ry));
            b.setRotZ(s.getRotZ() + (float) Math.toRadians(rz));
            b.markRotationAsChanged();
        }

        void pos(String name, float x, float y, float z) {
            GeoBone b = b(name);
            if (b == null) {
                return;
            }
            // GeckoLib は X を左右反転して使う
            b.setPosX(-x);
            b.setPosY(y);
            b.setPosZ(z);
            b.markPositionAsChanged();
        }

        void scale(String name, float x, float y, float z) {
            GeoBone b = b(name);
            if (b == null) {
                return;
            }
            b.setScaleX(x);
            b.setScaleY(y);
            b.setScaleZ(z);
            b.markScaleAsChanged();
        }

        void show(String name, boolean visible) {
            GeoBone b = b(name);
            if (b != null) {
                b.setHidden(!visible);
            }
        }
    }

    static void apply(MarkerBlockEntity be, GeoModel<MarkerBlockEntity> model, float partial) {
        MarkerType type = typeOf(be);
        if (type == null) {
            return;
        }
        Bones B = new Bones(model);
        float sz = FurnitureArt.size(type);
        B.scale("root", sz, sz, sz);
        FurnitureArt.Anim an = MarkerRenderer.anim(be, partial);
        float w = an.work();
        float t = an.time();
        float s = an.since();
        boolean act = an.active(type);
        switch (type) {
            case BREAKER -> {
                B.rot("lever", 50 - sm(w) * 100, 0, 0);
                B.rot("door", 0, door(an, type) * 115f, 0);   // 手前に開く
            }
            case GAS_VALVE -> B.rot("wheel", 0, 0, sm(w) * 540f + (act ? (float) Math.sin(t * 0.6f) * 3f : 0f));
            case BOILER -> {
                B.show("fire", w < 0.95f);
                B.rot("needle", 0, 0, 60 - sm(w) * 120 + (act ? (float) Math.sin(t * 3.1f) * 6f : 0f));
            }
            case HOKORA -> {
                boolean broken = w >= 0.99f;
                float b = broken ? sm(s / 10f) : 0f;
                float creak = act && !broken ? (float) Math.sin(t * 0.8f) * 4 : 0;
                B.rot("shrine", -8 * b, 0, 14 * b);
                B.rot("door_l", 0, -70 * b + creak, 0);
                B.rot("door_r", 0, 70 * b - creak, 0);
                B.pos("roof", 4 * b, -b, 0);
                B.rot("roof", 0, 0, 32 * b);
                B.rot("rope", 0, 0, -60 * b);
                B.show("evil", broken);
            }
            case WIRING -> {
                for (int i = 0; i < 4; i++) {
                    B.show("wire" + i, w >= 1f);
                }
                B.show("lamp_off", w < 1f);
                B.show("lamp_on", w >= 1f);
                B.rot("door", 0, -door(an, type) * 110f, 0);   // 手前に開く
            }
            case SHOJI -> {
                float pop = s < 10 ? 0.4f + sm(s / 6f) * 0.75f - (s > 6 ? (s - 6) / 4f * 0.15f : 0f) : 1f;
                for (int k = 0; k < 4; k++) {
                    B.show("hole" + k, w < 1f);
                    B.show("patch" + k, w >= 1f);
                    B.scale("patch" + k, pop, pop, 1);
                }
            }
            case FUTON -> {
                B.rot("fold1", -180f * (1f - sm(w * 2f)), 0, 0);
                B.rot("fold2", 180f * (1f - sm(w * 2f - 1f)), 0, 0);
                float k = sm((w - 0.85f) * 6.7f);
                B.show("pillow", k > 0.01f);
                B.scale("pillow", k, k, k);
            }
            case KAMADO -> B.show("fire", act || w > 0.4f);
            case BATH -> {
                float lvl = 9 + sm(w) * 1.5f;
                B.scale("water", 1, (lvl - 1) / 8f, 1);
                B.show("fire", act || w > 0.2f);
            }
            case CHOPPING -> {
                float open = sm(w);
                B.rot("half_l", 0, 0, open * 70f);
                B.rot("half_r", 0, 0, -open * 70f);
            }
            case FIELD -> {
                for (int k = 0; k < 8; k++) {
                    float rise = Mth.clamp(w * 8f - k, 0f, 1f);
                    float pop = (rise > 0 && rise < 1) ? hit(an, 10) : 0f;
                    B.pos("veg" + k, 0, sm(rise) * 6f + pop * 1.5f, 0);
                    B.rot("veg" + k, rise >= 1f ? -80 : 0, 0, 0);
                }
            }
            case DUST -> {
                float left = 1f - sm(w);
                B.show("dust", left >= 0.03f);
                B.scale("dust", 0.6f + 0.4f * left, Math.max(0.01f, left), 0.6f + 0.4f * left);
            }
            case LAUNDRY -> {
                for (int i = 0; i < 4; i++) {
                    float k = sm(w * 4f - i);
                    B.show("cloth" + i, k > 0.02f);
                    B.scale("cloth" + i, 1, Math.max(0.01f, k), 1);
                }
            }
            case VASE -> {
                float grow = 0.45f + 0.55f * sm(w);
                B.scale("flowers", 1, grow, 1);
                B.show("can", act);
            }
            case SINK -> {
                int clean = Math.round(sm(w) * 5);
                for (int i = 0; i < 5; i++) {
                    B.show("dirty" + i, i < 5 - clean);
                    B.show("clean" + i, i < clean);
                }
                B.show("stream", act);
            }
            case SHOE_RACK -> {
                float neat = sm(w);
                float jig = hit(an, 8);
                for (int k = 0; k < 8; k++) {
                    float seed = (k / 2 * 7 + k % 2 * 3) % 5 - 2;
                    B.pos("shoe" + k, 0, 0, -(1f - neat) * (3 + Math.abs(seed)));
                    B.rot("shoe" + k, 0, (1f - neat) * seed * 18f + jig * seed * 3f, 0);
                }
            }
            case ANDON -> {
                float lit = sm(w);
                B.show("flame", lit > 0.05f);
                B.show("paper_off", lit <= 0.3f);
                B.show("paper_on", lit > 0.3f);
            }
            case IRORI -> {
                float fire = Math.max(sm(w), act ? 0.5f : 0f);
                B.show("ember", fire > 0.05f);
                B.show("fire", fire > 0.3f);
            }
            case BONSAI -> {
                float k = 1f - sm(w);
                for (int i = 0; i < 3; i++) {
                    B.show("wild" + i, k >= 0.04f);
                    B.scale("wild" + i, k, k, k);
                }
            }
            case TEA -> {
                float f = sm(w);
                B.show("foam", f >= 0.04f);
                B.scale("foam", 1, Math.max(0.01f, f), 1);
                if (!act) {
                    B.pos("whisk", 4, 0, 4);
                }
            }
            case WELL -> B.scale("tubwater", 1, (0.6f + sm(w) * 3f) / 3.1f, 1);
            case SHOGI -> {
                int placed = Math.round(sm(w) * 18);
                for (int i = 0; i < 18; i++) {
                    B.show("piece" + i, i < placed);
                    B.pos("piece" + i, 0, i == placed - 1 ? hit(an, 8) * 1.5f : 0f, 0);
                }
            }
            case KAKEJIKU -> {
                float tilt = (1f - sm(w)) * 18f + hit(an, 18) * (float) Math.sin(s * 1.2f) * 6f;
                B.rot("scroll", 0, 0, tilt);
            }
            case MAILBOX -> {
                int placed = Math.round(sm(w) * 4);
                for (int k = 0; k < 4; k++) {
                    B.show("pile" + k, k >= placed);
                    B.show("sorted" + k, k < placed);
                }
            }
            case GOLDFISH -> B.show("food", s < 20);
            case FURIN -> {
                B.show("hung", w >= 1f);
                B.show("boxed", w < 1f);
            }
            case ZEN -> {
                int lines = Math.round(sm(w) * 6);
                for (int k = 0; k < 6; k++) {
                    B.show("line" + k, k < lines);
                }
            }
            case WASHTUB -> {
                float k = 0.4f + 0.6f * sm(w);
                B.show("bubbles", act || w > 0.05f);
                B.scale("bubbles", k, k, k);
            }
            default -> {
            }
        }
    }
}
