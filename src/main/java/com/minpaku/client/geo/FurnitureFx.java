package com.minpaku.client.geo;

import com.minpaku.block.MarkerBlockEntity;
import com.minpaku.client.model.FurnitureArt;
import com.minpaku.client.model.MeshCache;
import com.minpaku.client.model.StaticMesh;
import com.minpaku.game.MarkerType;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.model.GeoModel;

import java.util.List;
import java.util.Map;

/**
 * 家具の炎と水を、Blender で作ったメッシュ（mesh/fx_*.json、tools/blender/fx_meshes.py）で描く。
 * 位置はピクセル（Blockbench の座標：ブロックの角が原点、正面が北）。
 * bone があるものは、その骨が見えているときだけ描く（タスクの進み具合で火がつく など）。
 */
public final class FurnitureFx {
    private FurnitureFx() {
    }

    /** kind: 0 = 炎、1 = 丸い水面、2 = 四角い水面、3 = 流れる水 */
    private record Fx(int kind, String bone, float x, float y, float z, float a, float b, float c) {
    }

    /** 炎：x,y,z = 根もと、a = 高さ、b = 太さ（どちらもピクセル） */
    private static Fx flame(String bone, float x, float y, float z, float h, float w) {
        return new Fx(0, bone, x, y, z, h, w, 0);
    }

    /** 丸い水面：x,y,z = 中心（y は水面の高さ）、a = 半径 */
    private static Fx disc(String bone, float x, float y, float z, float r) {
        return new Fx(1, bone, x, y, z, r, 0, 0);
    }

    /** 四角い水面：x0,y,z0 〜 x1,z1 */
    private static Fx rect(String bone, float x0, float y, float z0, float x1, float z1) {
        return new Fx(2, bone, x0, y, z0, x1, z1, 0);
    }

    /** 流れ落ちる水：x,z = 位置、y0 → y1 */
    private static Fx stream(String bone, float x, float z, float y0, float y1) {
        return new Fx(3, bone, x, y0, z, y1, 0, 0);
    }

    private static final Map<MarkerType, List<Fx>> TABLE = Map.ofEntries(
            Map.entry(MarkerType.ANDON, List.of(flame("flame", 8, 10.9f, 8, 3.0f, 1.3f))),
            Map.entry(MarkerType.TOUROU, List.of(flame("flame", 8, 15.1f, 8, 2.6f, 1.4f))),
            Map.entry(MarkerType.CHOCHIN, List.of(flame("flame", 8, 12.9f, 7, 2.0f, 1.0f))),
            Map.entry(MarkerType.LEDGER, List.of(flame(null, -0.25f, 15.4f, 10.8f, 1.6f, 0.8f))),
            Map.entry(MarkerType.BOILER, List.of(flame("fire", 6.2f, 4.5f, 1.4f, 2.8f, 1.8f), flame("fire", 8, 4.5f, 1.3f, 3.2f, 2.0f),
                    flame("fire", 9.8f, 4.5f, 1.4f, 2.6f, 1.8f))),
            Map.entry(MarkerType.KAMADO, List.of(flame("fire", 2.4f, 1.9f, 2.6f, 3.2f, 2.0f), flame("fire", 4.4f, 1.9f, 2.6f, 2.6f, 1.8f),
                    flame("fire", 11.6f, 1.9f, 2.6f, 3.0f, 2.0f), flame("fire", 13.6f, 1.9f, 2.6f, 2.5f, 1.8f))),
            Map.entry(MarkerType.BATH, List.of(flame("fire", 6.6f, 0.7f, -1.2f, 2.4f, 1.8f), flame("fire", 9.4f, 0.7f, -1.2f, 2.1f, 1.6f),
                    disc("water", 8, 11.6f, 7.3f, 6.9f))),
            Map.entry(MarkerType.IRORI, List.of(flame("fire", 7.2f, 2.4f, 8.2f, 5.2f, 2.6f), flame("fire", 9.0f, 2.4f, 7.6f, 4.0f, 2.0f),
                    flame("fire", 8.0f, 2.4f, 9.4f, 3.4f, 1.8f))),
            Map.entry(MarkerType.WELL, List.of(rect(null, 3, 3.0f, 3, 13, 13), disc("tubwater", 22, 3.6f, -2, 3.5f))),
            Map.entry(MarkerType.WASHTUB, List.of(disc(null, 8, 4.4f, 8, 6.0f))),
            Map.entry(MarkerType.TEOKE, List.of(rect(null, 2.6f, 5.5f, 5.6f, 10.4f, 12.4f))),
            Map.entry(MarkerType.SINK, List.of(rect(null, 1, 16.0f, 5, 11, 13), stream("stream", 6, 8, 16, 25))),
            Map.entry(MarkerType.MOCHI, List.of(disc(null, 19.5f, 4.4f, 4.5f, 2.35f))));

    public static boolean has(MarkerType type) {
        return TABLE.containsKey(type);
    }

    /** ps はブロックの角が原点（1 = 1ブロック）、家具の向きと大きさがかかった状態 */
    public static void render(MarkerType type, MarkerBlockEntity be, GeoModel<MarkerBlockEntity> model, FurnitureArt.Anim an,
                              PoseStack ps, MultiBufferSource buf, int light) {
        List<Fx> list = TABLE.get(type);
        if (list == null) {
            return;
        }
        float t = an.time();
        int i = 0;
        for (Fx f : list) {
            i++;
            GeoBone bone = f.bone() == null ? null : model.getBone(f.bone()).orElse(null);
            if (bone != null && isHidden(bone)) {
                continue;
            }
            switch (f.kind()) {
                case 0 -> drawFlame(f, bone, t, i, ps, buf);
                case 1, 2 -> drawWater(f, bone, ps, buf, light);
                default -> drawStream(f, ps, buf, light);
            }
        }
    }

    private static boolean isHidden(GeoBone b) {
        for (GeoBone p = b; p != null; p = p.getParent()) {
            if (p.isHidden()) {
                return true;
            }
        }
        return false;
    }

    private static void drawFlame(Fx f, GeoBone bone, float t, int i, PoseStack ps, MultiBufferSource buf) {
        StaticMesh m = MeshCache.get("fx_flame");
        if (m == null) {
            return;
        }
        // 骨の大きさ（ゆらぎのアニメーション）もかける
        float bs = bone != null ? bone.getScaleY() : 1f;
        float flick = 1f + 0.13f * Mth.sin(t * 0.83f + i * 1.7f) + 0.06f * Mth.sin(t * 2.1f + i);
        float sway = 0.06f * Mth.sin(t * 1.3f + i * 2.3f);
        ps.pushPose();
        ps.translate(f.x() / 16f, f.y() / 16f, f.z() / 16f);
        ps.mulPose(Axis.YP.rotationDegrees(t * 6f + i * 77f));
        ps.mulPose(Axis.ZP.rotation(sway));
        float w = f.b() / 16f / 0.4f * (1f + 0.05f * Mth.sin(t * 1.7f + i));
        ps.scale(w, f.a() / 16f * flick * bs, w);
        m.render(ps, buf, 0xF000F0, net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY);
        ps.popPose();
    }

    private static void drawWater(Fx f, GeoBone bone, PoseStack ps, MultiBufferSource buf, int light) {
        StaticMesh m = MeshCache.get(f.kind() == 1 ? "fx_water_disc" : "fx_water_rect");
        if (m == null) {
            return;
        }
        // 水位が変わるもの（骨の縦の大きさ）：骨の原点から水面までをのばす
        float y = f.y();
        if (bone != null && Math.abs(bone.getScaleY() - 1f) > 0.001f) {
            float py = bone.getPivotY();
            y = py + (y - py) * bone.getScaleY();
        }
        ps.pushPose();
        if (f.kind() == 1) {
            ps.translate(f.x() / 16f, y / 16f + 0.002f, f.z() / 16f);
            float d = f.a() * 2f / 16f;
            ps.scale(d, 1f, d);
        } else {
            ps.translate((f.x() + f.a()) / 32f, y / 16f + 0.002f, (f.z() + f.b()) / 32f);
            ps.scale((f.a() - f.x()) / 16f, 1f, (f.b() - f.z()) / 16f);
        }
        m.render(ps, buf, light, net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY);
        ps.popPose();
    }

    private static void drawStream(Fx f, PoseStack ps, MultiBufferSource buf, int light) {
        StaticMesh m = MeshCache.get("fx_stream");
        if (m == null) {
            return;
        }
        ps.pushPose();
        ps.translate(f.x() / 16f, f.y() / 16f, f.z() / 16f);
        ps.scale(1f, (f.a() - f.y()) / 16f, 1f);
        m.render(ps, buf, light, net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY);
        ps.popPose();
    }
}
