package com.minpaku.client;

import com.minpaku.MinpakuJinrou;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.PlayerFaceRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** 民泊人狼の画面を描く道具（同梱フォント・和紙パネル・ハンコ・丸いゲージなど） */
public final class Ui {
    public static final ResourceLocation SANS = MinpakuJinrou.id("sans");
    public static final ResourceLocation SERIF = MinpakuJinrou.id("serif");

    // 色
    public static final int INK = 0xFF2B1D12;
    public static final int INK_SOFT = 0xFF6B4A2E;
    public static final int VERMILION = 0xFFC8372D;
    public static final int GOLD = 0xFFC9A14A;
    public static final int PAPER = 0xFFF4ECD8;
    public static final int CREAM = 0xFFFFE9B8;

    private Ui() {
    }

    private static Font font() {
        return Minecraft.getInstance().font;
    }

    public static Component sans(String s) {
        return Component.literal(s).withStyle(Style.EMPTY.withFont(SANS));
    }

    public static Component serif(String s) {
        return Component.literal(s).withStyle(Style.EMPTY.withFont(SERIF));
    }

    public static int width(Component c) {
        return font().width(c);
    }

    /** 文字を描く。align: -1 左 / 0 中央 / 1 右 */
    public static void text(GuiGraphics g, Component c, float x, float y, int color, float scale, int align, boolean shadow) {
        g.pose().pushPose();
        g.pose().translate(x, y, 0);
        g.pose().scale(scale, scale, 1f);
        int w = font().width(c);
        int dx = align == 0 ? -w / 2 : align > 0 ? -w : 0;
        g.drawString(font(), c, dx, 0, color, shadow);
        g.pose().popPose();
    }

    public static void sans(GuiGraphics g, String s, float x, float y, int color, float scale, int align) {
        text(g, sans(s), x, y, color, scale, align, false);
    }

    public static void sansShadow(GuiGraphics g, String s, float x, float y, int color, float scale, int align) {
        text(g, sans(s), x, y, color, scale, align, true);
    }

    public static void serif(GuiGraphics g, String s, float x, float y, int color, float scale, int align) {
        text(g, serif(s), x, y, color, scale, align, false);
    }

    /** drawCenteredString の代わり（同梱フォントで） */
    public static void sansCentered(GuiGraphics g, String s, int cx, int y, int color) {
        text(g, sans(s), cx, y, color, 0.9f, 0, false);
    }

    /** 折り返して描く。描いた行数を返す */
    public static int wrapped(GuiGraphics g, String s, int x, int y, int maxWidth, int color) {
        List<FormattedCharSequence> lines = font().split(sans(s), maxWidth);
        int yy = y;
        for (FormattedCharSequence l : lines) {
            g.drawString(font(), l, x, yy, color, false);
            yy += 10;
        }
        return lines.size();
    }

    public static List<FormattedCharSequence> split(String s, int maxWidth) {
        return new ArrayList<>(font().split(sans(s), maxWidth));
    }

    /** 9分割で伸ばすパネル */
    public static void sprite(GuiGraphics g, String name, int x, int y, int w, int h) {
        g.blitSprite(MinpakuJinrou.id(name), x, y, w, h);
    }

    /** ボタン（hover なら明るく） */
    public static void button(GuiGraphics g, int x, int y, int w, int h, String label, boolean red, boolean hover,
                              boolean active, float scale) {
        String sp = !active ? "btn_off" : red ? (hover ? "btn_red_hover" : "btn_red") : (hover ? "btn_paper_hover" : "btn_paper");
        sprite(g, sp, x, y, w, h);
        int color = !active ? 0xFF7A6A58 : red ? 0xFFFFFFFF : INK;
        text(g, sans(label), x + w / 2f, y + (h - 2) / 2f - 4.5f * scale, color, scale, 0, false);
    }

    /** 朱色のハンコ（少し傾けた四角に文字） */
    public static void stamp(GuiGraphics g, String text, float cx, float cy, int size, int color, float angle, boolean fill) {
        g.pose().pushPose();
        g.pose().translate(cx, cy, 0);
        g.pose().mulPose(Axis.ZP.rotationDegrees(angle));
        int h = size / 2;
        if (fill) {
            g.fill(-h, -h, h, h, 0xFFF4ECD8);
        }
        g.fill(-h, -h, h, -h + 2, color);
        g.fill(-h, h - 2, h, h, color);
        g.fill(-h, -h, -h + 2, h, color);
        g.fill(h - 2, -h, h, h, color);
        String[] lines = text.length() > 2 ? new String[]{text.substring(0, 2), text.substring(2)} : new String[]{text};
        float scale = lines.length == 1 ? (text.length() == 1 ? size / 13f : size / 22f) : size / 24f;
        float lineH = 10 * scale;
        float top = -lines.length * lineH / 2f;
        for (int i = 0; i < lines.length; i++) {
            text(g, serif(lines[i]), 0, top + i * lineH, color, scale, 0, false);
        }
        g.pose().popPose();
    }

    /** 丸いゲージ（fraction = 0〜1。時計回りに減る/増える） */
    public static void ring(GuiGraphics g, float cx, float cy, float rOuter, float rInner, float fraction, int color, int bg) {
        Matrix4f m = g.pose().last().pose();
        VertexConsumer vc = g.bufferSource().getBuffer(RenderType.gui());
        int seg = 48;
        for (int i = 0; i < seg; i++) {
            float t0 = i / (float) seg;
            float t1 = (i + 1) / (float) seg;
            int c = t0 < fraction ? color : bg;
            if ((c >>> 24) == 0) {
                continue;
            }
            double a0 = -Math.PI / 2 + t0 * Math.PI * 2;
            double a1 = -Math.PI / 2 + t1 * Math.PI * 2;
            float c0 = (float) Math.cos(a0), s0 = (float) Math.sin(a0);
            float c1 = (float) Math.cos(a1), s1 = (float) Math.sin(a1);
            // 画面の向き（裏向きは描かれない）に合わせた順番で頂点を出す
            vc.addVertex(m, cx + c0 * rInner, cy + s0 * rInner, 0).setColor(c);
            vc.addVertex(m, cx + c1 * rInner, cy + s1 * rInner, 0).setColor(c);
            vc.addVertex(m, cx + c1 * rOuter, cy + s1 * rOuter, 0).setColor(c);
            vc.addVertex(m, cx + c0 * rOuter, cy + s0 * rOuter, 0).setColor(c);
        }
    }

    /** 四角形（4点を順に）。向きに関係なく描けるよう表と裏の両方を出す */
    public static void quad(GuiGraphics g, float x0, float y0, float x1, float y1, float x2, float y2,
                            float x3, float y3, int color) {
        Matrix4f m = g.pose().last().pose();
        VertexConsumer vc = g.bufferSource().getBuffer(RenderType.gui());
        vc.addVertex(m, x0, y0, 0).setColor(color);
        vc.addVertex(m, x1, y1, 0).setColor(color);
        vc.addVertex(m, x2, y2, 0).setColor(color);
        vc.addVertex(m, x3, y3, 0).setColor(color);
        vc.addVertex(m, x3, y3, 0).setColor(color);
        vc.addVertex(m, x2, y2, 0).setColor(color);
        vc.addVertex(m, x1, y1, 0).setColor(color);
        vc.addVertex(m, x0, y0, 0).setColor(color);
    }

    /** 爪あと（a→b に向かって太くなり、また細くなる筋）。grow = 0〜1 で伸びる */
    public static void slash(GuiGraphics g, float ax, float ay, float bx, float by, float width, float grow, int color) {
        if (grow <= 0) {
            return;
        }
        float ex = ax + (bx - ax) * grow;
        float ey = ay + (by - ay) * grow;
        float mx = ax + (bx - ax) * 0.45f * Math.min(1f, grow / 0.45f);
        float my = ay + (by - ay) * 0.45f * Math.min(1f, grow / 0.45f);
        float len = (float) Math.hypot(bx - ax, by - ay);
        float nx = -(by - ay) / len * width / 2f;
        float ny = (bx - ax) / len * width / 2f;
        quad(g, ax, ay, mx + nx, my + ny, ex, ey, mx - nx, my - ny, color);
    }

    /** 塗りつぶした円 */
    public static void disc(GuiGraphics g, float cx, float cy, float r, int color) {
        ring(g, cx, cy, r, 0.01f, 1f, color, 0);
    }

    /** プレイヤー（ボット）の顔 */
    public static void face(GuiGraphics g, UUID id, int x, int y, int size) {
        PlayerFaceRenderer.draw(g, ClientGame.skin(id), x, y, size);
    }

    /** 小さな色つきの札（チップ） */
    public static void chip(GuiGraphics g, String s, float cx, float y, int bg, int fg) {
        Component c = sans(s);
        int w = width(c) + 12;
        int x = Math.round(cx - w / 2f);
        g.fill(x + 2, (int) y, x + w - 2, (int) y + 12, bg);
        g.fill(x, (int) y + 2, x + w, (int) y + 10, bg);
        g.drawString(font(), c, x + 6, (int) y + 2, fg, false);
    }

    /** 0xAARRGGBB の不透明度だけ変える */
    public static int alpha(int argb, float a) {
        int al = Math.max(8, Math.min(255, Math.round(((argb >>> 24) & 0xFF) * a)));
        return (al << 24) | (argb & 0xFFFFFF);
    }

    /** 役職を1文字で（ハンコ用） */
    public static String roleMark(String roleName) {
        return switch (roleName) {
            case "人狼" -> "狼";
            case "村人" -> "村";
            case "占い師" -> "占";
            case "騎士" -> "騎";
            case "霊媒師" -> "霊";
            case "狂人" -> "狂";
            case "妖狐" -> "狐";
            case "女将" -> "将";
            case "てるてる" -> "晴";
            case "忍び狼" -> "忍";
            case "怪力狼" -> "怪";
            case "呪狼" -> "呪";
            default -> roleName.isEmpty() ? "？" : roleName.substring(0, 1);
        };
    }
}
