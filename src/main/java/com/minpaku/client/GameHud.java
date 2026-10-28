package com.minpaku.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.minpaku.client.screen.MeetingScreen;
import com.minpaku.entity.BotEntity;
import com.minpaku.game.GameManager;
import com.minpaku.game.MarkerType;
import com.minpaku.game.Role;
import com.minpaku.network.Packets;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/**
 * ゲーム中の画面表示（すっきり版）。
 *   左上 … 小さな役職の札、つぎのタスク（［J］で宿帳＝タスク一覧）
 *   上   … 細いタスクバーと現在地、妨害のお知らせ
 *   中央 … 作業の進み具合／キルの合図（照準のまわりの輪）
 *   右下 … 能力のアイコン（クールダウンつき）
 */
public final class GameHud {
    public static KeyMapping LEDGER_KEY;
    public static KeyMapping KILL_KEY;

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
        float pt = delta.getGameTimeDeltaPartialTick(false);

        if (ClientGame.inGame() && !(mc.screen instanceof MeetingScreen)) {
            Role role = ClientGame.role();
            if (role != null) {
                roleTag(g, role, s);
                if (LEDGER_KEY != null && LEDGER_KEY.isDown()) {
                    ledger(g, s);
                } else {
                    nextTask(g, s);
                }
                abilities(g, role, s, w, h);
            }
            topBar(g, s, w);
            center(g, s, w, h, mc);
        }
        killFx(g, w, h, pt);
        announce(g, w, h);
    }

    // ------------------------------------------------------------ 左上

    private static void roleTag(GuiGraphics g, Role role, Packets.State s) {
        String mates = "";
        if (!s.mates().isEmpty()) {
            StringBuilder sb = new StringBuilder("仲間：");
            for (int i = 0; i < s.mates().size(); i++) {
                sb.append(i > 0 ? "、" : "").append(s.mates().get(i).name());
            }
            mates = sb.toString();
        }
        String name = role.name + (s.alive() ? "" : "（幽霊）");
        int w = 30 + Ui.width(Ui.serif(name)) + (mates.isEmpty() ? 6 : Ui.width(Ui.sans(mates)) + 12);
        Ui.sprite(g, "pill", 6, 6, w, 20);
        Ui.stamp(g, Ui.roleMark(role.name), 17, 16, 14, 0xFF000000 | role.color, -6, true);
        Ui.serif(g, name, 28, 10.5f, 0xFF000000 | lighten(role.color), 1f, -1);
        if (!mates.isEmpty()) {
            Ui.sans(g, mates, 28 + Ui.width(Ui.serif(name)) + 6, 11, 0xFFD8C7AD, 0.85f, -1);
        }
    }

    private static int lighten(int rgb) {
        int r = Math.min(255, ((rgb >> 16) & 0xFF) + 60);
        int gg = Math.min(255, ((rgb >> 8) & 0xFF) + 60);
        int b = Math.min(255, (rgb & 0xFF) + 60);
        return (r << 16) | (gg << 8) | b;
    }

    private static void nextTask(GuiGraphics g, Packets.State s) {
        if (s.tasks().isEmpty()) {
            return;
        }
        int done = 0;
        Packets.TaskInfo next = null;
        for (Packets.TaskInfo t : s.tasks()) {
            if (t.done()) {
                done++;
            } else if (next == null) {
                next = t;
            }
        }
        String key = LEDGER_KEY != null ? LEDGER_KEY.getTranslatedKeyMessage().getString() : "J";
        Ui.sansShadow(g, (s.fakeTasks() ? "ふりタスク " : "タスク ") + done + " / " + s.tasks().size()
                + "　［" + key + "］宿帳", 8, 31, 0xFFE8DCC8, 0.85f, -1);
        if (next != null) {
            String room = next.room().isEmpty() ? "" : "（" + next.room() + "）";
            Ui.sansShadow(g, "▸ " + ClientGame.taskName(next.type()) + room, 8, 42, 0xFFFFFFFF, 0.9f, -1);
        } else {
            Ui.sansShadow(g, "▸ 自分のタスクはすべて完了！", 8, 42, 0xFF9CE38A, 0.9f, -1);
        }
    }

    /** ［J］を押している間だけ出る宿帳（タスク一覧） */
    private static void ledger(GuiGraphics g, Packets.State s) {
        int x = 6;
        int y = 30;
        int w = 150;
        int h = 22 + s.tasks().size() * 11;
        Ui.sprite(g, "panel", x, y, w, h);
        Ui.serif(g, "宿帳", x + 8, y + 6, Ui.INK, 1f, -1);
        if (s.fakeTasks()) {
            Ui.sans(g, "ふりだけ", x + w - 8, y + 7, Ui.VERMILION, 0.8f, 1);
        }
        g.fill(x + 7, y + 17, x + w - 7, y + 18, Ui.GOLD);
        int yy = y + 21;
        for (Packets.TaskInfo t : s.tasks()) {
            String name = ClientGame.taskName(t.type());
            int c = t.done() ? 0xFF8D7A62 : Ui.INK;
            Ui.sans(g, (t.done() ? "☑ " : "☐ ") + name, x + 8, yy, c, 0.85f, -1);
            if (t.done()) {
                g.fill(x + 20, yy + 4, x + 20 + (int) (Ui.width(Ui.sans(name)) * 0.85f), yy + 5, 0xFF8D7A62);
            }
            Ui.sans(g, t.room(), x + w - 8, yy, 0xFF8D7A62, 0.8f, 1);
            yy += 11;
        }
    }

    // ------------------------------------------------------------ 上

    private static void topBar(GuiGraphics g, Packets.State s, int w) {
        int bw = 160;
        int x = w / 2 - bw / 2;
        int y = 5;
        g.fill(x - 1, y - 1, x + bw + 1, y + 5, 0x88000000);
        int fill = (int) (bw * Mth.clamp(s.taskProgress(), 0f, 1f));
        g.fillGradient(x, y, x + fill, y + 4, 0xFF7CC56B, 0xFF3F8F3A);
        String label = "みんなのタスク " + Math.round(s.taskProgress() * 100) + "%" + (s.room().isEmpty() ? "" : "　・　" + s.room());
        Ui.sansShadow(g, label, w / 2f, y + 7, 0xFFF0F0F0, 0.8f, 0);

        // 妨害のお知らせ（細い帯）
        String alert = null;
        int bg = 0;
        if (s.gasTicks() >= 0) {
            alert = "ガス漏れ！ 元栓とボイラーを止めろ　あと " + (s.gasTicks() / 20 + 1) + " 秒"
                    + (s.valveFixed() ? "　元栓OK" : "") + (s.boilerFixed() ? "　ボイラーOK" : "");
            bg = 0xCC8B1A12;
        } else if (s.blackout()) {
            alert = "停電中 ― 配電盤でブレーカーを上げよう";
            bg = 0xCC7A5200;
        }
        if (alert != null) {
            int tw = (int) (Ui.width(Ui.sans(alert)) * 0.85f) + 16;
            int ax = w / 2 - tw / 2;
            g.fill(ax + 3, 22, ax + tw - 3, 34, bg);
            g.fill(ax, 24, ax + tw, 32, bg);
            Ui.sans(g, alert, w / 2f, 24, 0xFFFFE08A, 0.85f, 0);
        }
    }

    // ------------------------------------------------------------ 中央

    private static void center(GuiGraphics g, Packets.State s, int w, int h, Minecraft mc) {
        float cx = w / 2f;
        float cy = h / 2f;
        // 作業中の進み具合
        if (s.activeType() >= 0) {
            Ui.ring(g, cx, cy, 13, 10, Mth.clamp(s.activeProgress(), 0f, 1f), 0xFFFFC94A, 0x66000000);
            Ui.sansShadow(g, ClientGame.taskName(s.activeType()), cx, cy + 17, 0xFFFFE08A, 0.85f, 0);
        }
        // キルの合図（能力：キルキー）
        LivingEntity target = KillTarget.find(mc);
        if (target != null) {
            String key = KILL_KEY != null ? KILL_KEY.getTranslatedKeyMessage().getString() : "R";
            String name = target.getName().getString();
            boolean behind = !s.killBehind() || GameManager.isBehind(mc.player, target);
            if (s.killCooldown() > 0) {
                float f = 1f - s.killCooldown() / (float) ClientGame.killCdMax;
                Ui.ring(g, cx, cy, 13, 11, f, 0x99FF5A4A, 0x44000000);
                Ui.sansShadow(g, name, cx, cy + 18, 0xAAFFFFFF, 0.75f, 0);
            } else if (behind) {
                Ui.ring(g, cx, cy, 14, 11, 1f, 0xEEFF5A4A, 0);
                Ui.sansShadow(g, "［" + key + "］ " + name + " をキル", cx, cy + 18, 0xFFFF8A7A, 0.9f, 0);
            } else {
                Ui.ring(g, cx, cy, 13, 11.5f, 1f, 0x88FFAA33, 0);
                Ui.sansShadow(g, name + " の背後に回りこもう", cx, cy + 18, 0xFFFFC266, 0.8f, 0);
            }
        }
        // 下のヒント
        String hint = null;
        if (s.inVent()) {
            hint = "ベントの中　［A］［D］で移動　［スニーク］で出る";
        } else if (!s.alive()) {
            hint = "幽霊（生きている人には見えない）　ブロックを右クリックでタスク";
        }
        if (hint != null) {
            int tw = (int) (Ui.width(Ui.sans(hint)) * 0.85f) + 16;
            Ui.sprite(g, "pill_dark", w / 2 - tw / 2, h - 66, tw, 14);
            Ui.sans(g, hint, w / 2f, h - 63, 0xFFE1D5F5, 0.85f, 0);
        }
        // ガス漏れ中は画面のふちを赤く点滅
        if (s.gasTicks() >= 0 && mc.level != null && mc.level.getGameTime() / 8 % 2 == 0) {
            g.fillGradient(0, 0, w, 30, 0x55FF0000, 0x00FF0000);
            g.fillGradient(0, h - 30, w, h, 0x00FF0000, 0x55FF0000);
        }
    }

    // ------------------------------------------------------------ 右下

    private static void abilities(GuiGraphics g, Role role, Packets.State s, int w, int h) {
        if (!s.alive()) {
            return;
        }
        float r = 12;
        float y = h - 20;
        float x = w - 20;
        // 右から順に並べる
        x = icon(g, x, y, r, "呼び鈴", String.valueOf(s.emergencyLeft()), s.emergencyLeft() > 0 ? 1f : 0f, 0xFFC9A14A);
        if (role.canSabotage()) {
            boolean busy = s.blackout() || s.gasTicks() >= 0;
            float f = s.sabotageCooldown() > 0 ? 1f - s.sabotageCooldown() / (float) ClientGame.sabotageCdMax : 1f;
            String v = busy ? "中" : s.sabotageCooldown() > 0 ? String.valueOf(s.sabotageCooldown() / 20 + 1) : "OK";
            x = icon(g, x, y, r, "妨害", v, busy ? 0f : f, 0xFFE0A030);
        }
        if (role.canKill()) {
            float f = s.killCooldown() > 0 ? 1f - s.killCooldown() / (float) ClientGame.killCdMax : 1f;
            String v = s.killCooldown() > 0 ? String.valueOf(s.killCooldown() / 20 + 1) : "OK";
            String key = KILL_KEY != null ? KILL_KEY.getTranslatedKeyMessage().getString() : "R";
            x = icon(g, x, y, r, "キル" + key, v, f, 0xFFFF5A4A);
        }
        if (role == Role.SEER) {
            x = icon(g, x, y, r, "占い", String.valueOf(s.seerLeft()), s.seerLeft() > 0 ? 1f : 0f, 0xFF6EC6FF);
        }
        if (role == Role.KNIGHT) {
            icon(g, x, y, r, "守り", String.valueOf(s.knightLeft()), s.knightLeft() > 0 ? 1f : 0f, 0xFFF2C94C);
        }
    }

    private static float icon(GuiGraphics g, float x, float y, float r, String label, String value, float fraction, int color) {
        Ui.disc(g, x, y, r, 0xAA140C06);
        Ui.ring(g, x, y, r, r - 2, fraction, color, 0x55FFFFFF);
        Ui.sans(g, label, x, y - 7.5f, 0xFFE8DCC8, 0.6f, 0);
        Ui.sans(g, value, x, y - 1.5f, 0xFFFFFFFF, 0.9f, 0);
        return x - r * 2 - 6;
    }

    // ------------------------------------------------------------ 演出

    private static void killFx(GuiGraphics g, int w, int h, float pt) {
        if (ClientGame.killFxTicks <= 0) {
            return;
        }
        if (ClientGame.killFxKind == 0) {
            victimFx(g, w, h, pt);
            return;
        }
        float t = (ClientGame.killFxLen - ClientGame.killFxTicks + pt) / ClientGame.killFxLen;
        float a = t < 0.15f ? t / 0.15f : (t > 0.75f ? (1 - t) / 0.25f : 1f);
        a = Mth.clamp(a, 0f, 1f);
        if (ClientGame.killFxKind == 1) {
            g.fill(0, 0, w, h, Ui.alpha(0xFF300000, a * 0.35f));
            Ui.text(g, Ui.serif("キル"), w / 2f, h / 2f - 50, Ui.alpha(0xFFFF4A3A, a), 3.5f, 0, false);
            Ui.text(g, Ui.sans(ClientGame.killFxOther + " を襲った"), w / 2f, h / 2f - 12, Ui.alpha(0xFFFFFFFF, a), 1.1f, 0, false);
        } else {
            Ui.text(g, Ui.serif("守られていた"), w / 2f, h / 2f - 50, Ui.alpha(0xFFF2C94C, a), 3f, 0, false);
            Ui.text(g, Ui.sans("騎士の守りでキルが防がれた"), w / 2f, h / 2f - 16, Ui.alpha(0xFFFFFFFF, a), 1.1f, 0, false);
        }
    }

    /** 襲われた人の画面：暗転 → 爪あと3本 → 襲った人の顔 → 幽霊になった */
    private static void victimFx(GuiGraphics g, int w, int h, float pt) {
        float t = ClientGame.killFxLen - ClientGame.killFxTicks + pt;
        float end = ClientGame.killFxLen;
        // 最後の 25 tick で明ける
        float all = t > end - 25 ? Mth.clamp((end - t) / 25f, 0f, 1f) : 1f;
        float cx = w / 2f;
        float cy = h / 2f;
        g.pose().pushPose();
        g.pose().translate(0, 0, 2000);
        // 真っ暗（最初の一瞬だけ赤く光る）
        g.fill(0, 0, w, h, Ui.alpha(0xFF000000, all));
        if (t < 5) {
            g.fill(0, 0, w, h, Ui.alpha(0xFFB01818, (1f - t / 5f) * 0.8f));
        }
        // 爪あと（右上から左下へ、3本）
        for (int i = 0; i < 3; i++) {
            float grow = Mth.clamp((t - 5 - i * 6) / 4f, 0f, 1f);
            float ox = (i - 1) * 34f;
            float fade = all * (t > 60 ? Mth.clamp(1f - (t - 60) / 25f, 0.25f, 1f) : 1f);
            Ui.slash(g, cx + ox + 70, cy - 95, cx + ox - 70, cy + 95, 16, grow, Ui.alpha(0xFF8E1010, fade));
            Ui.slash(g, cx + ox + 70, cy - 95, cx + ox - 70, cy + 95, 7, grow, Ui.alpha(0xFFE0392B, fade));
        }
        // 襲った人の顔と名前
        if (t > 28) {
            float a = Mth.clamp((t - 28) / 8f, 0f, 1f) * all;
            if (a > 0.05f) {
                RenderSystem.enableBlend();
                RenderSystem.setShaderColor(1f, 1f, 1f, a);
                g.fill((int) cx - 19, (int) cy - 61, (int) cx + 19, (int) cy - 23, Ui.alpha(0xFFE0392B, a));
                Ui.face(g, ClientGame.killFxOtherId, (int) cx - 17, (int) cy - 59, 34);
                RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
            }
            Ui.text(g, Ui.serif(ClientGame.killFxOther), cx, cy - 12, Ui.alpha(0xFFFFFFFF, a), 1.8f, 0, false);
            Ui.text(g, Ui.serif("に襲われた"), cx, cy + 10, Ui.alpha(0xFFE0392B, a), 1.5f, 0, false);
        }
        if (t > 50) {
            float a = Mth.clamp((t - 50) / 10f, 0f, 1f) * all;
            Ui.text(g, Ui.sans("あなたは幽霊になった"), cx, cy + 42, Ui.alpha(0xFFE1D5F5, a), 1.2f, 0, false);
            Ui.text(g, Ui.sans("生きている人には見えません。タスクは続けられます"), cx, cy + 58,
                    Ui.alpha(0xFFB8A9CF, a), 0.85f, 0, false);
        }
        g.pose().popPose();
    }

    private static void announce(GuiGraphics g, int w, int h) {
        if (ClientGame.announceTicks <= 0) {
            return;
        }
        int t = ClientGame.ANNOUNCE_LEN - ClientGame.announceTicks;
        float a = t < 6 ? t / 6f : (ClientGame.announceTicks < 15 ? ClientGame.announceTicks / 15f : 1f);
        if (!ClientGame.announceTitle.isEmpty()) {
            float scale = 3.4f - Math.min(1f, t / 6f) * 0.6f;
            Ui.text(g, Ui.serif(ClientGame.announceTitle), w / 2f, h / 2f - 60, Ui.alpha(0xFF000000 | ClientGame.announceColor, a),
                    scale, 0, true);
        }
        if (!ClientGame.announceSub.isEmpty()) {
            Ui.text(g, Ui.sans(ClientGame.announceSub), w / 2f, h / 2f - 22, Ui.alpha(0xFFFFFFFF, a), 1.3f, 0, true);
        }
    }

    public static MarkerType type(int i) {
        return MarkerType.byOrdinal(i);
    }

    /** 古い呼び出し用（大きな文字） */
    public static void drawBig(GuiGraphics g, net.minecraft.client.gui.Font font, String text, int cx, int cy, float scale, int argb) {
        Ui.text(g, Ui.serif(text), cx, cy, argb, scale, 0, true);
    }
}
