package com.minpaku.client;

import com.minpaku.client.model.StaticMesh;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexSorting;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.util.Mth;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.opengl.GL11;

import java.util.UUID;

/**
 * 襲われた人に見せるキルの場面（Apex のフィニッシャー風）。
 * ダウンしてひざをついた自分（自分のスキン）に、犯人（襲った人のスキン）が後ろから近づき、
 * つかんで → ためて一撃 → 蹴り倒す → こちらを見て決めポーズ。カメラは場面ごとに切り替わって回り込む。
 *
 * 流れ（tick）:
 *   0〜10  正面の低い位置から：ひざをついた自分の後ろに犯人が歩いてくる
 *   10〜22 横の寄り：頭をつかんで引き起こし、右腕をゆっくり振り上げる（スロー）
 *   22〜30 犯人の肩ごし：振り下ろして一撃（白く光る）
 *   30〜40 正面の引き：背中を蹴り、自分がこちらへ倒れる
 *   40〜56 犯人の顔へ寄る：見下ろしてから、こちらを見る。「仕留められた」の帯
 */
public final class KillScene {
    public static final int END = 56;
    /** 当たった瞬間（一撃・蹴り）。爪あと・血しぶき・光の位置に使う */
    public static final float[] HITS = {24f, 31f};
    public static final float[] hitX = new float[2];
    public static final float[] hitY = new float[2];

    // カメラの場面: {開始, 終了, 目x,y,z(始), 目x,y,z(終), 見る先x,y,z(始), 見る先x,y,z(終)}
    private static final float[][] SHOTS = {
            {0, 10, 1.5f, 0.55f, 2.7f, 0.95f, 0.6f, 2.15f, 0f, 0.9f, -0.5f, 0f, 0.85f, -0.3f},
            {10, 22, 2.1f, 1.25f, -0.1f, 1.55f, 1.3f, 0.35f, 0f, 1.05f, -0.35f, 0f, 1.2f, -0.35f},
            {22, 30, -0.95f, 2.25f, -2.15f, -0.75f, 2.0f, -1.85f, 0f, 0.75f, 0.1f, 0f, 0.65f, 0.2f},
            {30, 40, 0.5f, 0.45f, 3.4f, 0.3f, 0.5f, 3.0f, 0f, 0.7f, 0f, 0f, 0.6f, 0.3f},
            {40, 58, 1.0f, 1.4f, 1.9f, 0.45f, 1.55f, 0.75f, 0f, 1.45f, -0.45f, 0f, 1.6f, -0.45f},
    };

    private static PlayerModel<?> wide;
    private static PlayerModel<?> slim;

    private KillScene() {
    }

    private static PlayerModel<?> model(PlayerSkin skin) {
        Minecraft mc = Minecraft.getInstance();
        if (wide == null) {
            wide = new PlayerModel<>(mc.getEntityModels().bakeLayer(ModelLayers.PLAYER), false);
            slim = new PlayerModel<>(mc.getEntityModels().bakeLayer(ModelLayers.PLAYER_SLIM), true);
        }
        return skin.model() == PlayerSkin.Model.SLIM ? slim : wide;
    }

    static float track(float t, float... keys) {
        return Keyframes.track(t, keys);
    }

    public static void render(GuiGraphics g, float t, float alpha, UUID killerId) {
        if (t > END + 2) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }
        PlayerSkin killerSkin = ClientGame.skin(killerId);
        PlayerSkin mySkin = mc.player.getSkin();
        float bright = track(t, 0, 0.5f, 5, 1f, 50, 1f, 56, 0f) * alpha;

        // ---- カメラ
        float[] s = SHOTS[SHOTS.length - 1];
        for (float[] shot : SHOTS) {
            if (t < shot[1]) {
                s = shot;
                break;
            }
        }
        float f = Mth.clamp((t - s[0]) / (s[1] - s[0]), 0f, 1f);
        f = f * f * (3 - 2 * f);
        Vector3f eye = new Vector3f(Mth.lerp(f, s[2], s[5]), Mth.lerp(f, s[3], s[6]), Mth.lerp(f, s[4], s[7]));
        Vector3f target = new Vector3f(Mth.lerp(f, s[8], s[11]), Mth.lerp(f, s[9], s[12]), Mth.lerp(f, s[10], s[13]));
        float shake = 0f;
        for (float hit : HITS) {
            if (t > hit && t < hit + 6) {
                shake = Math.max(shake, 0.07f * (1f - (t - hit) / 6f));
            }
        }
        target.add((float) Math.sin(t * 7.3f) * shake, (float) Math.cos(t * 9.1f) * shake, 0);

        // ---- 投影を遠近ありに切り替える
        g.flush();
        Matrix4f oldProj = new Matrix4f(RenderSystem.getProjectionMatrix());
        VertexSorting oldSort = RenderSystem.getVertexSorting();
        float aspect = (float) mc.getWindow().getWidth() / Math.max(1, mc.getWindow().getHeight());
        float fov = track(t, 0, 50, 22, 50, 24, 42, 30, 50, 40, 48, 56, 36);
        Matrix4f proj = new Matrix4f().setPerspective((float) Math.toRadians(fov), aspect, 0.05f, 100f);
        RenderSystem.setProjectionMatrix(proj, VertexSorting.DISTANCE_TO_ORIGIN);
        Matrix4fStack mv = RenderSystem.getModelViewStack();
        mv.pushMatrix();
        mv.identity();
        RenderSystem.applyModelViewMatrix();
        RenderSystem.clear(GL11.GL_DEPTH_BUFFER_BIT, Minecraft.ON_OSX);
        RenderSystem.setShaderLights(new Vector3f(0.3f, 1f, 0.6f).normalize(), new Vector3f(-0.6f, 0.4f, 0.7f).normalize());

        MultiBufferSource.BufferSource buf = mc.renderBuffers().bufferSource();
        PoseStack ps = new PoseStack();
        Matrix4f view = new Matrix4f().lookAt(eye, target, new Vector3f(0, 1, 0));
        ps.last().pose().mul(view);
        ps.last().normal().mul(view.get3x3(new Matrix3f()));

        int light = LightTexture.FULL_BRIGHT;
        int ov = OverlayTexture.NO_OVERLAY;
        floor(ps, buf, bright);

        // ---- 自分（ダウンしてひざをついている。正面＝+Z）
        PlayerModel<?> vm = model(mySkin);
        reset(vm);
        float vLean = track(t, 0, 12, 11, 12, 14, -12, 24, -12, 25, 8, 31, 8, 36, 90);
        float vy = track(t, 0, -0.58f, 31, -0.58f, 36, 0.12f);
        float vz = track(t, 0, 0f, 31, 0f, 36, 0.35f);
        float legs = track(t, 0, 1.5f, 31, 1.5f, 35, 0f);
        vm.rightLeg.xRot = legs;
        vm.leftLeg.xRot = legs;
        vm.head.xRot = track(t, 0, 0.5f, 11, 0.5f, 14, -0.55f, 24, -0.55f, 25.5f, 0.7f, 31, 0.6f, 36, 0.2f);
        vm.rightArm.xRot = track(t, 0, -0.35f, 11, -0.35f, 14, 0.1f, 31, 0.1f, 35, -2.9f);
        vm.leftArm.xRot = track(t, 0, -0.6f, 11, -0.6f, 14, 0.15f, 31, 0.15f, 35, -2.8f);
        vm.rightArm.zRot = 0.12f;
        vm.leftArm.zRot = -0.12f;
        // 一撃を受けた瞬間に体がはねる
        if (t > HITS[0] && t < HITS[0] + 3) {
            vy -= 0.04f * (1f - (t - HITS[0]) / 3f);
        }
        drawPlayer(ps, buf, vm, mySkin, 0f, vy, vz, 0f, vLean, light, ov, bright);

        // ---- 犯人（後ろから歩いてくる）
        PlayerModel<?> km = model(killerSkin);
        reset(km);
        float kz = track(t, 0, -3.0f, 10, -0.62f, 28, -0.62f, 31, -0.45f, 38, -0.5f);
        float kLean = track(t, 0, 0, 14, 0, 22, -8, 24, 16, 28, 4, 30, -4, 31.5f, -12, 36, 0, 40, 0);
        if (t < 10) {
            float c = (float) Math.cos(t * 0.85f);
            km.rightArm.xRot = c * 0.7f;
            km.leftArm.xRot = -c * 0.7f;
            km.rightLeg.xRot = -c * 0.8f;
            km.leftLeg.xRot = c * 0.8f;
        } else {
            // 左手で頭をつかむ → 離す
            km.leftArm.xRot = track(t, 10, 0, 13, -1.05f, 26, -1.05f, 29, 0f);
            // 右腕：ゆっくり振り上げ（スロー）→ 一気に振り下ろす → 下ろす → 最後は少し前へ
            km.rightArm.xRot = track(t, 10, 0, 14, -0.4f, 22, -3.05f, 24, -0.55f, 28, -0.3f, 36, 0f, 44, 0f, 48, -0.35f);
            km.rightArm.zRot = track(t, 14, 0, 22, -0.35f, 24, 0.15f, 28, 0f);
            // 蹴り（右足を後ろに引いてから前へ）
            km.rightLeg.xRot = track(t, 27, 0, 30, 0.7f, 31.5f, -1.5f, 34, -0.6f, 37, 0f);
            km.leftLeg.xRot = track(t, 30, 0, 31.5f, 0.25f, 34, 0f);
            // 見下ろしてから、こちら（カメラ）を見る
            km.head.xRot = track(t, 10, 0.55f, 24, 0.35f, 36, 0.6f, 42, 0.6f, 45, -0.08f);
            km.head.yRot = track(t, 40, 0, 45, 0.25f);
        }
        drawPlayer(ps, buf, km, killerSkin, 0f, 0f, kz, 0f, kLean, light, ov, bright);

        // 当たった位置を画面の座標に（一撃＝頭、蹴り＝背中）
        Vector3f[] at = {new Vector3f(0f, 0.95f, 0f), new Vector3f(0f, 0.55f, -0.15f)};
        for (int i = 0; i < at.length; i++) {
            Vector4f v = new Vector4f(at[i], 1f);
            ps.last().pose().transform(v);
            proj.transform(v);
            float w = Math.max(0.0001f, v.w);
            hitX[i] = (v.x / w * 0.5f + 0.5f) * g.guiWidth();
            hitY[i] = (1f - (v.y / w * 0.5f + 0.5f)) * g.guiHeight();
        }

        buf.endBatch();
        mv.popMatrix();
        RenderSystem.applyModelViewMatrix();
        RenderSystem.setProjectionMatrix(oldProj, oldSort);
        RenderSystem.clear(GL11.GL_DEPTH_BUFFER_BIT, Minecraft.ON_OSX);
        Lighting.setupFor3DItems();
    }

    private static void reset(PlayerModel<?> m) {
        for (ModelPart part : new ModelPart[]{m.head, m.hat, m.body, m.rightArm, m.leftArm, m.rightLeg, m.leftLeg}) {
            part.resetPose();
        }
        m.crouching = false;
        // 初期値は子ども（頭が大きく体が小さい）なので、大人の体にする
        m.young = false;
        m.riding = false;
    }

    /**
     * x,y,z = 足もとの位置。yaw = 向き（0 で +Z ＝ 最初のカメラの方を向く）。
     * lean = 足もとを中心に前（+）／後ろ（-）へ倒す角度
     */
    private static void drawPlayer(PoseStack ps, MultiBufferSource buf, PlayerModel<?> m, PlayerSkin skin, float x, float y,
                                   float z, float yaw, float lean, int light, int ov, float bright) {
        m.hat.copyFrom(m.head);
        m.jacket.copyFrom(m.body);
        m.leftSleeve.copyFrom(m.leftArm);
        m.rightSleeve.copyFrom(m.rightArm);
        m.leftPants.copyFrom(m.leftLeg);
        m.rightPants.copyFrom(m.rightLeg);
        ps.pushPose();
        ps.translate(x, y, z);
        ps.mulPose(Axis.YP.rotationDegrees(180f - yaw));
        ps.mulPose(Axis.XP.rotationDegrees(-lean));
        ps.scale(-0.9375f, -0.9375f, 0.9375f);
        ps.translate(0, -1.501f, 0);
        int c = (int) (Mth.clamp(bright, 0f, 1f) * 255);
        int color = 0xFF000000 | (c << 16) | ((int) (c * 0.9f) << 8) | (int) (c * 0.9f);
        VertexConsumer vc = buf.getBuffer(RenderType.entityTranslucent(skin.texture()));
        m.renderToBuffer(ps, vc, light, ov, color);
        ps.popPose();
        // 足もとの影
        ps.pushPose();
        ps.translate(x, 0.01f, z + (lean > 45 ? 0.8f : 0f));
        quad(ps, buf, 0.35f, lean > 45 ? 1.0f : 0.35f, 0x99000000);
        ps.popPose();
    }

    /** 床（暗い板の間）と、まんなかを照らすスポットライト */
    private static void floor(PoseStack ps, MultiBufferSource buf, float bright) {
        int c = (int) (Mth.clamp(bright, 0f, 1f) * 255);
        for (int i = -10; i < 10; i++) {
            float shade = (i & 1) == 0 ? 1f : 0.86f;
            int r = (int) (0x3A * shade * c / 255f);
            int gg = (int) (0x26 * shade * c / 255f);
            int b = (int) (0x18 * shade * c / 255f);
            ps.pushPose();
            ps.translate(i * 0.45f + 0.225f, 0, 0);
            quad(ps, buf, 0.225f, 8f, 0xFF000000 | (r << 16) | (gg << 8) | b);
            ps.popPose();
        }
        int spot = (int) (0x48 * Mth.clamp(bright, 0f, 1f));
        for (int k = 0; k < 3; k++) {
            ps.pushPose();
            ps.translate(0f, 0.004f + k * 0.001f, 0f);
            quad(ps, buf, 1.0f + k * 0.6f, 1.0f + k * 0.6f, (spot / (k + 1) << 24) | 0xFFD9A0);
            ps.popPose();
        }
    }

    private static void quad(PoseStack ps, MultiBufferSource buf, float hx, float hz, int argb) {
        VertexConsumer vc = buf.getBuffer(RenderType.entityTranslucent(StaticMesh.WHITE));
        PoseStack.Pose p = ps.last();
        int a = argb >>> 24;
        int r = (argb >> 16) & 0xFF;
        int g = (argb >> 8) & 0xFF;
        int b = argb & 0xFF;
        float[][] v = {{-hx, -hz}, {-hx, hz}, {hx, hz}, {hx, -hz}};
        for (float[] q : v) {
            vc.addVertex(p, q[0], 0, q[1]).setColor(r, g, b, a).setUv(0.5f, 0.5f).setOverlay(OverlayTexture.NO_OVERLAY)
                    .setLight(LightTexture.FULL_BRIGHT).setNormal(p, 0, 1, 0);
        }
    }
}
