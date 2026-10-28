package com.minpaku.client;

import com.minpaku.game.GameManager;
import com.minpaku.game.MarkerType;
import com.minpaku.game.Role;
import com.minpaku.network.Packets;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;

/** ゲーム中の画面表示（左上の役職・タスク、上のタスクバー、中央のお知らせなど） */
public final class GameHud {
    private GameHud() {
    }

    public static void render(GuiGraphics g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui) {
            return;
        }
        Packets.State s = ClientGame.state;
        int w = g.guiWidth();
        int h = g.guiHeight();
        Font font = mc.font;

        if (ClientGame.inGame() && !(mc.screen instanceof com.minpaku.client.screen.MeetingScreen)) {
            renderPanel(g, font, s);
            renderTaskBar(g, font, s, w);
            renderCenter(g, font, s, w, h);
        }
        renderKillFx(g, font, w, h, delta.getGameTimeDeltaPartialTick(false));
        renderAnnounce(g, font, w, h);
    }

    private static void renderPanel(GuiGraphics g, Font font, Packets.State s) {
        Role role = ClientGame.role();
        if (role == null) {
            return;
        }
        int x = 6;
        int y = 22;
        int lines = 4 + s.tasks().size() + (s.mates().isEmpty() ? 0 : 1);
        g.fill(x - 3, y - 3, x + 150, y + lines * 11 + 2, 0x88000000);

        g.drawString(font, role.name + (s.alive() ? "" : "（幽霊）"), x, y, 0xFF000000 | role.color, true);
        y += 11;
        g.drawString(font, role.team.label, x, y, 0xFF000000 | role.team.color, false);
        y += 11;
        if (!s.mates().isEmpty()) {
            StringBuilder sb = new StringBuilder("仲間: ");
            for (int i = 0; i < s.mates().size(); i++) {
                if (i > 0) {
                    sb.append(", ");
                }
                sb.append(s.mates().get(i).name());
            }
            g.drawString(font, sb.toString(), x, y, 0xFFFF7777, false);
            y += 11;
        }
        String ability = abilityLine(role, s);
        g.drawString(font, ability, x, y, 0xFFDDDDDD, false);
        y += 13;

        g.drawString(font, s.fakeTasks() ? "タスク（ふりをするだけ）" : "タスク", x, y, 0xFFFFFFAA, false);
        y += 11;
        for (Packets.TaskInfo t : s.tasks()) {
            String name = ClientGame.taskName(t.type());
            int color = t.done() ? 0xFF55FF55 : 0xFFFFFFFF;
            g.drawString(font, (t.done() ? "✔ " : "・ ") + name, x + 2, y, color, false);
            y += 11;
        }
    }

    private static String abilityLine(Role role, Packets.State s) {
        if (!s.alive()) {
            return "幽霊：タスクを続けよう";
        }
        if (role.canKill()) {
            String kill = s.killCooldown() > 0 ? "キル あと" + (s.killCooldown() / 20 + 1) + "秒" : "キル OK";
            String sab = s.sabotageCooldown() > 0 ? "  妨害 あと" + (s.sabotageCooldown() / 20 + 1) + "秒" : "  妨害 OK";
            return kill + sab;
        }
        if (role == Role.SEER) {
            return "占い 残り" + s.seerLeft() + "回  呼び鈴 " + s.emergencyLeft();
        }
        if (role == Role.KNIGHT) {
            return "守り 残り" + s.knightLeft() + "回  呼び鈴 " + s.emergencyLeft();
        }
        return "呼び鈴 残り" + s.emergencyLeft() + "回";
    }

    private static void renderTaskBar(GuiGraphics g, Font font, Packets.State s, int w) {
        int bw = 200;
        int x = w / 2 - bw / 2;
        int y = 4;
        g.fill(x - 1, y - 1, x + bw + 1, y + 9, 0xAA000000);
        g.fill(x, y, x + (int) (bw * Mth.clamp(s.taskProgress(), 0f, 1f)), y + 8, 0xFF43A047);
        g.drawCenteredString(font, "みんなのタスク " + (int) (s.taskProgress() * 100) + "%", w / 2, y, 0xFFFFFFFF);
    }

    private static void renderCenter(GuiGraphics g, Font font, Packets.State s, int w, int h) {
        Minecraft mc = Minecraft.getInstance();
        int cy = h / 2;
        // 作業中の進み具合
        if (s.activeType() >= 0) {
            int bw = 100;
            int x = w / 2 - bw / 2;
            int y = cy + 14;
            g.fill(x - 1, y - 1, x + bw + 1, y + 6, 0xAA000000);
            g.fill(x, y, x + (int) (bw * Mth.clamp(s.activeProgress(), 0f, 1f)), y + 5, 0xFFFFC107);
            g.drawCenteredString(font, ClientGame.taskName(s.activeType()), w / 2, y + 8, 0xFFFFFFFF);
        }
        // ベント
        if (s.inVent()) {
            g.drawCenteredString(font, "ベントの中  [A] [D] で移動  [スニーク] で出る", w / 2, h - 60, 0xFFE1BEE7);
        }
        // 幽霊
        if (!s.alive()) {
            g.drawCenteredString(font, "幽霊（生きている人には見えない）  ブロックを右クリックでタスク", w / 2, h - 48, 0xFFB0BEC5);
        }
        // ガス漏れ
        if (s.gasTicks() >= 0) {
            boolean blink = (mc.level != null && mc.level.getGameTime() / 6 % 2 == 0);
            if (blink) {
                g.fill(0, 0, w, h, 0x22FF0000);
            }
        }
        if (s.blackout()) {
            g.drawCenteredString(font, "停電中！ 配電盤を直そう", w / 2, 18, 0xFFFFD54F);
        }
        // キルできる相手
        Role role = ClientGame.role();
        if (role != null && role.canKill() && s.alive() && !s.inVent() && s.phase() == 1
                && mc.crosshairPickEntity instanceof Player target && mc.player != null
                && !ClientGame.isMate(target.getUUID()) && !target.isSpectator()) {
            boolean behind = GameManager.isBehind(mc.player, target);
            String text;
            int color;
            if (s.killCooldown() > 0) {
                text = "キルまで " + (s.killCooldown() / 20 + 1) + "秒";
                color = 0xFFAAAAAA;
            } else if (behind) {
                text = "▶ 左クリックでキル ◀";
                color = 0xFFFF3333;
            } else {
                text = "背後に回りこめ";
                color = 0xFFFFAA00;
            }
            g.drawCenteredString(font, text, w / 2, cy + 30, color);
        }
    }

    private static void renderKillFx(GuiGraphics g, Font font, int w, int h, float pt) {
        if (ClientGame.killFxTicks <= 0) {
            return;
        }
        float t = (ClientGame.KILL_FX_LEN - ClientGame.killFxTicks + pt) / ClientGame.KILL_FX_LEN;
        float alpha = t < 0.15f ? t / 0.15f : (t > 0.75f ? (1 - t) / 0.25f : 1f);
        int a = Math.max(8, (int) (Mth.clamp(alpha, 0f, 1f) * 255));
        if (ClientGame.killFxKind == 0) {
            g.fill(0, 0, w, h, (int) (a * 0.75f) << 24 | 0x400000);
            drawBig(g, font, "襲われた…", w / 2, h / 2 - 20, 4f, a << 24 | 0xFF3333);
            g.drawCenteredString(font, "あなたは幽霊になった", w / 2, h / 2 + 22, a << 24 | 0xFFFFFF);
        } else if (ClientGame.killFxKind == 1) {
            g.fill(0, 0, w, h, (int) (a * 0.35f) << 24 | 0x300000);
            drawBig(g, font, "キル！", w / 2, h / 2 - 40, 3f, a << 24 | 0xFF3333);
            g.drawCenteredString(font, ClientGame.killFxOther + " を襲った", w / 2, h / 2 - 8, a << 24 | 0xFFFFFF);
        } else {
            drawBig(g, font, "守られていた！", w / 2, h / 2 - 40, 3f, a << 24 | 0xF2C94C);
            g.drawCenteredString(font, "騎士の守りでキルが防がれた", w / 2, h / 2 - 8, a << 24 | 0xFFFFFF);
        }
    }

    private static void renderAnnounce(GuiGraphics g, Font font, int w, int h) {
        if (ClientGame.announceTicks <= 0) {
            return;
        }
        int t = ClientGame.ANNOUNCE_LEN - ClientGame.announceTicks;
        float alpha = t < 6 ? t / 6f : (ClientGame.announceTicks < 15 ? ClientGame.announceTicks / 15f : 1f);
        int a = Math.max(8, (int) (alpha * 255));
        if (!ClientGame.announceTitle.isEmpty()) {
            float scale = 3.5f - Math.min(1f, t / 6f) * 0.5f;
            drawBig(g, font, ClientGame.announceTitle, w / 2, h / 2 - 50, scale, a << 24 | (ClientGame.announceColor & 0xFFFFFF));
        }
        if (!ClientGame.announceSub.isEmpty()) {
            drawBig(g, font, ClientGame.announceSub, w / 2, h / 2 - 14, 1.5f, a << 24 | 0xFFFFFF);
        }
    }

    /** 大きな文字を中央ぞろえで描く */
    public static void drawBig(GuiGraphics g, Font font, String text, int cx, int cy, float scale, int argb) {
        g.pose().pushPose();
        g.pose().translate(cx, cy, 0);
        g.pose().scale(scale, scale, 1f);
        g.drawString(font, text, -font.width(text) / 2, 0, argb, true);
        g.pose().popPose();
    }

    public static MarkerType type(int i) {
        return MarkerType.byOrdinal(i);
    }
}
