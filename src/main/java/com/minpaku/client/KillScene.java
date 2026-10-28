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
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.opengl.GL11;

import java.util.UUID;

/**
 * 襲われた人に見せるキルの場面（アモアス風に横から見る）。
 * 襲った人（そのスキン）が走ってきて、自分（自分のスキン）に飛びかかって倒し、2回切りつける。
 *
 * 流れ（tick）:
 *   0〜10  自分が立っている。左から犯人が走ってくる
 *   10〜16 犯人が飛びかかる（自分は気づいて振り向く）
 *   16〜24 体当たりで自分が前に倒れる
 *   24〜34 犯人がのしかかって2回切りつける
 *   34〜40 犯人が立ち上がり、こちら（カメラ）を見る
 *   40〜46 暗転
 */
public final class KillScene {
    public static final int END = 46;
    /** 当たった瞬間（体当たり・1回目・2回目）。爪あとと血の位置に使う */
    public static final float[] HITS = {16.5f, 26f, 31f};
    public static final float[] hitX = new float[3];
    public static final float[] hitY = new float[3];

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
        float bright = track(t, 0, 0.35f, 6, 1f, 38, 1f, 46, 0f) * alpha;

        // ---- ゆれ
        float shake = 0f;
        for (float hit : HITS) {
            if (t > hit && t < hit + 5) {
                shake = Math.max(shake, 0.06f * (1f - (t - hit) / 5f));
            }
        }
        float sx = (float) Math.sin(t * 7.3f) * shake;
        float sy = (float) Math.cos(t * 9.1f) * shake;

        // ---- 投影を遠近ありに切り替える
        g.flush();
        Matrix4f oldProj = new Matrix4f(RenderSystem.getProjectionMatrix());
        VertexSorting oldSort = RenderSystem.getVertexSorting();
        float aspect = (float) mc.getWindow().getWidth() / Math.max(1, mc.getWindow().getHeight());
        Matrix4f proj = new Matrix4f().setPerspective((float) Math.toRadians(45), aspect, 0.05f, 100f);
        RenderSystem.setProjectionMatrix(proj, VertexSorting.DISTANCE_TO_ORIGIN);
        Matrix4fStack mv = RenderSystem.getModelViewStack();
        mv.pushMatrix();
        mv.identity();
        RenderSystem.applyModelViewMatrix();
        RenderSystem.clear(GL11.GL_DEPTH_BUFFER_BIT, Minecraft.ON_OSX);
        RenderSystem.setShaderLights(new Vector3f(0.3f, 1f, 0.6f).normalize(), new Vector3f(-0.6f, 0.4f, 0.7f).normalize());

        MultiBufferSource.BufferSource buf = mc.renderBuffers().bufferSource();
        PoseStack ps = new PoseStack();
        // カメラ：横から少し見下ろす
        ps.translate(sx - 0.95f, sy - 0.95f, -3.9f);
        ps.mulPose(Axis.XP.rotationDegrees(9));

        int light = LightTexture.FULL_BRIGHT;
        int ov = OverlayTexture.NO_OVERLAY;
        floor(ps, buf, bright);

        // ---- 自分（右側。最初は右＝奥の方を向いている）
        float vx = track(t, 0, 0.75f, 16, 0.75f, 24, 1.15f);
        float fall = track(t, 0, 0, 16.5f, 0, 18, -20, 23, -90);
        float vy = track(t, 0, 0, 18, 0.05f, 23, 0.14f);
        float look = track(t, 0, 0, 9, 0, 13, 1.3f, 17, 0.6f, 22, 0.3f);
        PlayerModel<?> vm = model(mySkin);
        reset(vm);
        vm.head.yRot = -look;
        if (t > 10 && t < 17) {
            // 驚いて腕を上げる
            vm.rightArm.zRot = 0.6f;
            vm.leftArm.zRot = -0.6f;
            vm.rightArm.xRot = -0.5f;
        }
        if (t >= 17) {
            vm.rightArm.xRot = track(t, 17, -0.4f, 23, -2.9f);
            vm.leftArm.xRot = track(t, 17, -0.2f, 23, -2.8f);
            vm.rightLeg.xRot = track(t, 17, 0.3f, 23, 0.1f);
            vm.leftLeg.xRot = track(t, 17, -0.3f, 23, -0.1f);
        }
        // 倒れたあと切りつけられて体がはねる
        for (float hit : new float[]{HITS[1], HITS[2]}) {
            if (t > hit && t < hit + 3) {
                vy += 0.05f * (1f - (t - hit) / 3f);
            }
        }
        drawPlayer(ps, buf, vm, mySkin, vx, vy, -90f, fall, light, ov, bright);

        // ---- 犯人（左から来る）
        float kx = track(t, 0, -3.2f, 10, -0.9f, 13, -0.15f, 16.5f, 0.4f, 22, 1.3f, 24, 1.8f, 34, 1.8f, 38, 1.35f);
        float ky = track(t, 0, 0, 10, 0, 12.5f, 0.55f, 16.5f, 0.15f, 20, 0f, 24, 0f);
        float kTilt = track(t, 0, -8, 10, -10, 13, -30, 16.5f, -20, 22, -5, 24, 0, 34, 0, 38, 0);
        PlayerModel<?> km = model(killerSkin);
        reset(km);
        boolean crouch = t >= 21 && t < 35;
        if (t < 10) {
            // 走る
            float c = (float) Math.cos(t * 1.1f);
            km.rightArm.xRot = c * 1.2f;
            km.leftArm.xRot = -c * 1.2f;
            km.rightLeg.xRot = -c * 1.3f;
            km.leftLeg.xRot = c * 1.3f;
        } else if (t < 21) {
            // 飛びかかる（両腕を前へ）
            km.rightArm.xRot = track(t, 10, -1.0f, 13, -2.6f, 17, -1.7f);
            km.leftArm.xRot = track(t, 10, -1.0f, 13, -2.4f, 17, -1.6f);
            km.rightLeg.xRot = track(t, 10, 0.4f, 13, 0.9f, 18, 0.2f);
            km.leftLeg.xRot = track(t, 10, -0.6f, 13, 0.5f, 18, -0.2f);
        } else if (crouch) {
            // のしかかって切りつける（右腕を振り上げて振り下ろす×2）
            km.rightArm.xRot = track(t, 21, -1.4f, 24, -3.0f, 26, -0.5f, 28.5f, -3.1f, 31, -0.4f, 34, -1.0f);
            km.leftArm.xRot = track(t, 21, -1.2f, 26, -0.9f, 31, -0.7f);
            km.rightLeg.xRot = -1.2f;
            km.leftLeg.xRot = 0.5f;
            km.head.xRot = 0.4f;
        } else {
            // 立ち上がってこちらを見る
            km.head.yRot = track(t, 35, 0, 38, 1.45f);
            km.rightArm.xRot = track(t, 34, -1.0f, 37, 0f);
        }
        float kdy = crouch ? -0.38f : 0f;
        if (crouch) {
            km.body.xRot = 0.5f;
            km.body.y = 3.2f;
            km.head.y = 4.2f;
            km.rightArm.y = 5.2f;
            km.leftArm.y = 5.2f;
            km.rightLeg.z = 4.0f;
            km.leftLeg.z = 4.0f;
            km.rightLeg.y = 12.2f;
            km.leftLeg.y = 12.2f;
        }
        drawPlayer(ps, buf, km, killerSkin, kx, ky + kdy, -90f, kTilt, light, ov, bright);

        // 当たった位置を画面の座標にしておく（爪あと・血しぶき用）
        Vector3f[] at = {new Vector3f(0.75f, 1.25f, 0f), new Vector3f(2.15f, 0.4f, 0f), new Vector3f(2.4f, 0.4f, 0f)};
        for (int i = 0; i < 3; i++) {
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
    }

    /** x,y = 足もとの位置。yaw = 向き（-90 で右向き）。tilt = 足もとを中心に前後へ倒す角度 */
    private static void drawPlayer(PoseStack ps, MultiBufferSource buf, PlayerModel<?> m, PlayerSkin skin, float x, float y,
                                   float yaw, float tilt, int light, int ov, float bright) {
        m.hat.copyFrom(m.head);
        m.jacket.copyFrom(m.body);
        m.leftSleeve.copyFrom(m.leftArm);
        m.rightSleeve.copyFrom(m.rightArm);
        m.leftPants.copyFrom(m.leftLeg);
        m.rightPants.copyFrom(m.rightLeg);
        ps.pushPose();
        ps.translate(x, y, 0);
        ps.mulPose(Axis.ZP.rotationDegrees(tilt));
        ps.mulPose(Axis.YP.rotationDegrees(180f - yaw));
        ps.scale(-0.9375f, -0.9375f, 0.9375f);
        ps.translate(0, -1.501f, 0);
        int c = (int) (Mth.clamp(bright, 0f, 1f) * 255);
        int color = 0xFF000000 | (c << 16) | ((int) (c * 0.9f) << 8) | (int) (c * 0.9f);
        VertexConsumer vc = buf.getBuffer(RenderType.entityTranslucent(skin.texture()));
        m.renderToBuffer(ps, vc, light, ov, color);
        ps.popPose();
        // 足もとの影
        ps.pushPose();
        ps.translate(x + (tilt < -45 ? 0.8f : 0f), 0.01f, 0);
        quad(ps, buf, tilt < -45 ? 1.0f : 0.35f, 0.3f, 0x99000000);
        ps.popPose();
    }

    /** 床（暗い板の間）と、まんなかを照らすスポットライト */
    private static void floor(PoseStack ps, MultiBufferSource buf, float bright) {
        int c = (int) (Mth.clamp(bright, 0f, 1f) * 255);
        for (int i = -8; i < 8; i++) {
            float shade = (i & 1) == 0 ? 1f : 0.86f;
            int r = (int) (0x3A * shade * c / 255f);
            int gg = (int) (0x26 * shade * c / 255f);
            int b = (int) (0x18 * shade * c / 255f);
            ps.pushPose();
            ps.translate(0, 0, i * 0.45f + 0.225f);
            quad(ps, buf, 7f, 0.225f, 0xFF000000 | (r << 16) | (gg << 8) | b);
            ps.popPose();
        }
        ps.pushPose();
        ps.translate(0.2f, 0.005f, 0);
        int spot = (int) (0x40 * Mth.clamp(bright, 0f, 1f));
        quad(ps, buf, 2.6f, 1.2f, (spot << 24) | 0xFFD9A0);
        ps.popPose();
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
