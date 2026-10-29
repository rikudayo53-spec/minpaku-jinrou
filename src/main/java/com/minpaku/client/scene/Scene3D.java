package com.minpaku.client.scene;

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
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.opengl.GL11;

/**
 * 画面（GUI）の中に、遠近のある3Dの場面を描くための道具。
 * マイクラのブロック・ブロックのテクスチャを貼った箱・プレイヤー（スキン）を置ける。
 *
 *   Scene3D s = Scene3D.begin(g, eye, target, fov);
 *   s.block(...); s.player(...);
 *   s.end();
 */
public final class Scene3D {
    public final PoseStack ps = new PoseStack();
    public final MultiBufferSource.BufferSource buf;
    private final Matrix4f oldProj;
    private final VertexSorting oldSort;
    private final Matrix4f proj;
    private final Matrix4f view;
    private final int guiW;
    private final int guiH;
    /** 色の明るさ（0〜1） */
    public float bright = 1f;

    private static PlayerModel<?> wide;
    private static PlayerModel<?> slim;

    private Scene3D(GuiGraphics g, Vector3f eye, Vector3f target, float fov) {
        Minecraft mc = Minecraft.getInstance();
        g.flush();
        guiW = g.guiWidth();
        guiH = g.guiHeight();
        oldProj = new Matrix4f(RenderSystem.getProjectionMatrix());
        oldSort = RenderSystem.getVertexSorting();
        float aspect = (float) mc.getWindow().getWidth() / Math.max(1, mc.getWindow().getHeight());
        proj = new Matrix4f().setPerspective((float) Math.toRadians(fov), aspect, 0.05f, 200f);
        RenderSystem.setProjectionMatrix(proj, VertexSorting.DISTANCE_TO_ORIGIN);
        Matrix4fStack mv = RenderSystem.getModelViewStack();
        mv.pushMatrix();
        mv.identity();
        RenderSystem.applyModelViewMatrix();
        RenderSystem.clear(GL11.GL_DEPTH_BUFFER_BIT, Minecraft.ON_OSX);
        RenderSystem.setShaderLights(new Vector3f(0.3f, 1f, 0.6f).normalize(), new Vector3f(-0.6f, 0.4f, 0.7f).normalize());
        buf = mc.renderBuffers().bufferSource();
        view = new Matrix4f().lookAt(eye, target, new Vector3f(0, 1, 0));
        ps.last().pose().mul(view);
        ps.last().normal().mul(view.get3x3(new Matrix3f()));
    }

    public static Scene3D begin(GuiGraphics g, Vector3f eye, Vector3f target, float fov) {
        return new Scene3D(g, eye, target, fov);
    }

    public void end() {
        buf.endBatch();
        Matrix4fStack mv = RenderSystem.getModelViewStack();
        mv.popMatrix();
        RenderSystem.applyModelViewMatrix();
        RenderSystem.setProjectionMatrix(oldProj, oldSort);
        RenderSystem.clear(GL11.GL_DEPTH_BUFFER_BIT, Minecraft.ON_OSX);
        Lighting.setupFor3DItems();
    }

    /** 3D の位置を画面（GUI）の座標に。後ろ側なら null */
    public float[] toScreen(float x, float y, float z) {
        Vector4f v = new Vector4f(x, y, z, 1f);
        view.transform(v);
        proj.transform(v);
        if (v.w <= 0.0001f) {
            return null;
        }
        return new float[]{(v.x / v.w * 0.5f + 0.5f) * guiW, (1f - (v.y / v.w * 0.5f + 0.5f)) * guiH};
    }

    private int tint(int argb) {
        float b = Mth.clamp(bright, 0f, 1f);
        int a = argb >>> 24;
        int r = (int) (((argb >> 16) & 0xFF) * b);
        int g = (int) (((argb >> 8) & 0xFF) * b);
        int bl = (int) ((argb & 0xFF) * b);
        return (a << 24) | (r << 16) | (g << 8) | bl;
    }

    // ------------------------------------------------------------ ブロック

    /** マイクラのブロックを (x,y,z) の位置（角）に1つ置く */
    public void block(BlockState state, float x, float y, float z) {
        ps.pushPose();
        ps.translate(x, y, z);
        Minecraft.getInstance().getBlockRenderer().renderSingleBlock(state, ps, buf, LightTexture.FULL_BRIGHT,
                OverlayTexture.NO_OVERLAY);
        ps.popPose();
    }

    /** 同じブロックを箱の形に並べる（x0〜x1 は含む） */
    public void fill(BlockState state, int x0, int y0, int z0, int x1, int y1, int z1) {
        for (int x = x0; x <= x1; x++) {
            for (int y = y0; y <= y1; y++) {
                for (int z = z0; z <= z1; z++) {
                    block(state, x, y, z);
                }
            }
        }
    }

    // ------------------------------------------------------------ テクスチャを貼った箱

    public static TextureAtlasSprite sprite(String id) {
        return Minecraft.getInstance().getTextureAtlas(TextureAtlas.LOCATION_BLOCKS).apply(ResourceLocation.parse(id));
    }

    /**
     * ブロックのテクスチャを貼った箱（ブロック単位）。1ブロックより大きい面はテクスチャをくり返す。
     * argb で色をかける（草や水の色づけ、半透明など）
     */
    public void box(TextureAtlasSprite sp, float x0, float y0, float z0, float x1, float y1, float z1, int argb) {
        VertexConsumer vc = buf.getBuffer(RenderType.entityTranslucentCull(TextureAtlas.LOCATION_BLOCKS));
        int c = tint(argb);
        PoseStack.Pose p = ps.last();
        // 6面：法線と、面の上の2つの向き
        face(vc, p, sp, c, x0, y1, z0, x1 - x0, 0, 0, 0, 0, z1 - z0, 0, 1, 0);   // 上
        face(vc, p, sp, c, x0, y0, z1, x1 - x0, 0, 0, 0, 0, z0 - z1, 0, -1, 0);  // 下
        face(vc, p, sp, c, x0, y1, z1, x1 - x0, 0, 0, 0, y0 - y1, 0, 0, 0, 1);   // 南
        face(vc, p, sp, c, x1, y1, z0, x0 - x1, 0, 0, 0, y0 - y1, 0, 0, 0, -1);  // 北
        face(vc, p, sp, c, x1, y1, z1, 0, 0, z0 - z1, 0, y0 - y1, 0, 1, 0, 0);   // 東
        face(vc, p, sp, c, x0, y1, z0, 0, 0, z1 - z0, 0, y0 - y1, 0, -1, 0, 0);  // 西
    }

    private static void face(VertexConsumer vc, PoseStack.Pose p, TextureAtlasSprite sp, int c,
                             float ox, float oy, float oz, float ux, float uy, float uz, float vx, float vy, float vz,
                             float nx, float ny, float nz) {
        float lu = Math.abs(ux) + Math.abs(uy) + Math.abs(uz);
        float lv = Math.abs(vx) + Math.abs(vy) + Math.abs(vz);
        if (lu < 1e-5f || lv < 1e-5f) {
            return;
        }
        int nu = Math.max(1, (int) Math.ceil(lu - 1e-3f));
        int nv = Math.max(1, (int) Math.ceil(lv - 1e-3f));
        for (int i = 0; i < nu; i++) {
            float a0 = i / lu;
            float a1 = Math.min(lu, i + 1) / lu;
            for (int j = 0; j < nv; j++) {
                float b0 = j / lv;
                float b1 = Math.min(lv, j + 1) / lv;
                float tu = (a1 - a0) * lu;
                float tv = (b1 - b0) * lv;
                float u0 = sp.getU(0f);
                float u1 = sp.getU(tu);
                float v0 = sp.getV(0f);
                float v1 = sp.getV(tv);
                corner(vc, p, ox + ux * a0 + vx * b0, oy + uy * a0 + vy * b0, oz + uz * a0 + vz * b0, u0, v0, c, nx, ny, nz);
                corner(vc, p, ox + ux * a0 + vx * b1, oy + uy * a0 + vy * b1, oz + uz * a0 + vz * b1, u0, v1, c, nx, ny, nz);
                corner(vc, p, ox + ux * a1 + vx * b1, oy + uy * a1 + vy * b1, oz + uz * a1 + vz * b1, u1, v1, c, nx, ny, nz);
                corner(vc, p, ox + ux * a1 + vx * b0, oy + uy * a1 + vy * b0, oz + uz * a1 + vz * b0, u1, v0, c, nx, ny, nz);
            }
        }
    }

    private static void corner(VertexConsumer vc, PoseStack.Pose p, float x, float y, float z, float u, float v, int c,
                               float nx, float ny, float nz) {
        vc.addVertex(p, x, y, z).setColor(c).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightTexture.FULL_BRIGHT).setNormal(p, nx, ny, nz);
    }

    // ------------------------------------------------------------ プレイヤー

    public static PlayerModel<?> model(PlayerSkin skin) {
        Minecraft mc = Minecraft.getInstance();
        if (wide == null) {
            wide = new PlayerModel<>(mc.getEntityModels().bakeLayer(ModelLayers.PLAYER), false);
            slim = new PlayerModel<>(mc.getEntityModels().bakeLayer(ModelLayers.PLAYER_SLIM), true);
        }
        PlayerModel<?> m = skin.model() == PlayerSkin.Model.SLIM ? slim : wide;
        for (ModelPart part : new ModelPart[]{m.head, m.hat, m.body, m.rightArm, m.leftArm, m.rightLeg, m.leftLeg}) {
            part.resetPose();
        }
        m.crouching = false;
        m.young = false;
        m.riding = false;
        return m;
    }

    /**
     * プレイヤーを描く。x,y,z = 足もと。yaw = 向き（0 で +Z、-90 で +X を向く）。
     * pitch = 足もとを中心に前（+）／後ろ（-）へ倒す。roll = 横に倒す
     */
    public void player(PlayerModel<?> m, PlayerSkin skin, float x, float y, float z, float yaw, float pitch, float roll) {
        m.hat.copyFrom(m.head);
        m.jacket.copyFrom(m.body);
        m.leftSleeve.copyFrom(m.leftArm);
        m.rightSleeve.copyFrom(m.rightArm);
        m.leftPants.copyFrom(m.leftLeg);
        m.rightPants.copyFrom(m.rightLeg);
        ps.pushPose();
        ps.translate(x, y, z);
        ps.mulPose(Axis.YP.rotationDegrees(180f - yaw));
        ps.mulPose(Axis.XP.rotationDegrees(-pitch));
        ps.mulPose(Axis.ZP.rotationDegrees(roll));
        ps.scale(-0.9375f, -0.9375f, 0.9375f);
        ps.translate(0, -1.501f, 0);
        VertexConsumer vc = buf.getBuffer(RenderType.entityTranslucent(skin.texture()));
        m.renderToBuffer(ps, vc, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, tint(0xFFFFFFFF));
        ps.popPose();
    }

    /** 足もとの丸い影 */
    public void shadow(float x, float y, float z, float r) {
        box(sprite("minecraft:block/black_concrete"), x - r, y + 0.002f, z - r, x + r, y + 0.004f, z + r, 0x66000000);
    }
}
