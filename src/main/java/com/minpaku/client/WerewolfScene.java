package com.minpaku.client;

import com.minpaku.client.model.MeshCache;
import com.minpaku.client.model.StaticMesh;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexSorting;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;

/**
 * 襲われた人に見せる「人狼に襲われる」3Dアニメーション。
 * Blender で作った人狼（体・頭・あご・右腕・左腕）を、暗闇の中でカメラに向かって動かす。
 *
 * 流れ（tick）:
 *   0〜12  暗闇の奥から赤い目が光り、前かがみの人狼が近づく
 *   12〜20 飛びかかる：頭を上げて吠え、右腕を振り上げる
 *   20〜25 右腕でひっかく（1本目の爪あと）
 *   25〜30 左腕でひっかく（2本目）
 *   30〜36 噛みつきながら目の前に迫り、暗転（3本目）
 */
public final class WerewolfScene {
    public static final int END = 36;

    // 関節の位置（マイクラ座標。正面 +Z）
    private static final Vector3f BODY = new Vector3f(0f, 1.0f, 0f);
    private static final Vector3f HEAD = new Vector3f(0f, 2.0f, 0.26f);
    private static final Vector3f JAW = new Vector3f(0f, 1.97f, 0.42f);
    private static final Vector3f ARM_R = new Vector3f(0.46f, 1.86f, 0.06f);   // 画面の右側
    private static final Vector3f ARM_L = new Vector3f(-0.46f, 1.86f, 0.06f);  // 画面の左側

    private WerewolfScene() {
    }

    /** なめらかにつなぐキーフレーム。keys = {時間, 値, 時間, 値, ...} */
    static float track(float t, float... keys) {
        if (t <= keys[0]) {
            return keys[1];
        }
        for (int i = 0; i + 3 < keys.length; i += 2) {
            float t0 = keys[i];
            float t1 = keys[i + 2];
            if (t <= t1) {
                float f = (t - t0) / Math.max(0.0001f, t1 - t0);
                f = f * f * (3 - 2 * f);
                return Mth.lerp(f, keys[i + 1], keys[i + 3]);
            }
        }
        return keys[keys.length - 1];
    }

    /** t = 襲われてからの tick（小数） */
    public static void render(GuiGraphics g, float t, float alpha) {
        if (t > END + 2) {
            return;
        }
        StaticMesh body = MeshCache.get("werewolf_body");
        StaticMesh head = MeshCache.get("werewolf_head");
        StaticMesh jaw = MeshCache.get("werewolf_jaw");
        StaticMesh armR = MeshCache.get("werewolf_arm_b");
        StaticMesh armL = MeshCache.get("werewolf_arm_a");
        if (body == null || head == null || jaw == null || armR == null || armL == null) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();

        // ---- 動き
        float dist = track(t, 0, 7.5f, 12, 4.6f, 20, 1.75f, 30, 1.45f, 36, 0.35f);
        float bright = track(t, 0, 0f, 10, 0.55f, 18, 1f, 30, 1f, 36, 0.2f) * alpha;
        float lean = track(t, 0, 28, 12, 24, 18, 6, 23, 18, 28, 14, 33, 30);
        float twist = track(t, 0, 0, 18, 18, 24, -20, 29, 18, 34, 0);
        float headX = track(t, 0, 12, 12, 8, 17, -28, 22, -10, 30, -6, 34, 18);
        float headY = track(t, 0, 0, 22, -12, 28, 10, 34, 0);
        float jawOpen = track(t, 0, 4, 12, 8, 16, 42, 21, 22, 30, 30, 34, 58);
        float rX = track(t, 0, 8, 12, 14, 19, 155, 21, 150, 24.5f, 18, 30, 30, 34, 70);
        float rZ = track(t, 0, 6, 19, 42, 21, 40, 24.5f, -48, 30, -20);
        float lX = track(t, 0, 8, 19, 30, 23, 120, 25.5f, 145, 29.5f, 16, 34, 60);
        float lZ = track(t, 0, -6, 23, -30, 25.5f, -40, 29.5f, 48, 34, 20);
        float bob = (float) Math.sin(t * 0.9f) * track(t, 0, 0.05f, 12, 0.05f, 20, 0.0f);
        // ゆれ（ひっかかれた瞬間）
        float shake = 0f;
        for (float hit : new float[]{22.5f, 27.5f, 33f}) {
            if (t > hit && t < hit + 5) {
                shake = Math.max(shake, 0.09f * (1f - (t - hit) / 5f));
            }
        }
        float sx = (float) Math.sin(t * 7.3f) * shake;
        float sy = (float) Math.cos(t * 9.1f) * shake;

        // ---- 3D で描く準備（遠近のある投影に切り替える）
        g.flush();
        Matrix4f oldProj = new Matrix4f(RenderSystem.getProjectionMatrix());
        VertexSorting oldSort = RenderSystem.getVertexSorting();
        float aspect = (float) mc.getWindow().getWidth() / Math.max(1, mc.getWindow().getHeight());
        RenderSystem.setProjectionMatrix(new Matrix4f().setPerspective((float) Math.toRadians(70), aspect, 0.05f, 100f),
                VertexSorting.DISTANCE_TO_ORIGIN);
        Matrix4fStack mv = RenderSystem.getModelViewStack();
        mv.pushMatrix();
        mv.identity();
        RenderSystem.applyModelViewMatrix();
        RenderSystem.clear(GL11.GL_DEPTH_BUFFER_BIT, Minecraft.ON_OSX);
        // 上からの赤みがかった光と、正面からの弱い光
        RenderSystem.setShaderLights(new Vector3f(0.35f, 1f, 0.45f).normalize(), new Vector3f(-0.5f, 0.1f, 1f).normalize());

        MultiBufferSource.BufferSource buf = mc.renderBuffers().bufferSource();
        PoseStack ps = new PoseStack();
        int light = LightTexture.FULL_BRIGHT;
        int ov = OverlayTexture.NO_OVERLAY;
        float tr = bright;
        float tg = bright * 0.78f;
        float tb = bright * 0.8f;

        // カメラ（目の高さ 1.6）から見て、前方 dist の位置に人狼を置く
        ps.translate(sx, -1.6f + bob + sy, -dist);
        // 体
        ps.pushPose();
        ps.translate(BODY.x, BODY.y, BODY.z);
        ps.mulPose(Axis.YP.rotationDegrees(twist));
        ps.mulPose(Axis.XP.rotationDegrees(lean));
        body.render(ps, buf, light, ov, tr, tg, tb);
        // 頭とあご
        ps.pushPose();
        ps.translate(HEAD.x - BODY.x, HEAD.y - BODY.y, HEAD.z - BODY.z);
        ps.mulPose(Axis.YP.rotationDegrees(headY));
        ps.mulPose(Axis.XP.rotationDegrees(headX - lean * 0.6f));
        head.render(ps, buf, light, ov, tr, tg, tb);
        ps.pushPose();
        ps.translate(JAW.x - HEAD.x, JAW.y - HEAD.y, JAW.z - HEAD.z);
        ps.mulPose(Axis.XP.rotationDegrees(jawOpen));
        jaw.render(ps, buf, light, ov, tr, tg, tb);
        ps.popPose();
        ps.popPose();
        // 腕（腕は下に垂れている。X で前→上へ振り上げ、Z で外へ開く）
        arm(ps, buf, armR, ARM_R, -rX, rZ, light, ov, tr, tg, tb);
        arm(ps, buf, armL, ARM_L, -lX, lZ, light, ov, tr, tg, tb);
        ps.popPose();
        buf.endBatch();

        // ---- 元に戻す
        mv.popMatrix();
        RenderSystem.applyModelViewMatrix();
        RenderSystem.setProjectionMatrix(oldProj, oldSort);
        RenderSystem.clear(GL11.GL_DEPTH_BUFFER_BIT, Minecraft.ON_OSX);
        Lighting.setupFor3DItems();
    }

    private static void arm(PoseStack ps, MultiBufferSource buf, StaticMesh mesh, Vector3f pivot, float rotX, float rotZ,
                            int light, int ov, float tr, float tg, float tb) {
        ps.pushPose();
        ps.translate(pivot.x - BODY.x, pivot.y - BODY.y, pivot.z - BODY.z);
        ps.mulPose(Axis.ZP.rotationDegrees(rotZ));
        ps.mulPose(Axis.XP.rotationDegrees(rotX));
        mesh.render(ps, buf, light, ov, tr, tg, tb);
        ps.popPose();
    }
}
