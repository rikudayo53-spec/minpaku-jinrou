package com.minpaku.client.screen;

import com.minpaku.client.ClientGame;
import com.minpaku.client.Ui;
import com.minpaku.network.Packets;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * 囲炉裏ばた会議：議論 → 投票 → 結果 → 追放の演出 → 閉じる。
 * 右側に「話し合い」（ボットや人の発言）と発言欄。
 */
public class MeetingScreen extends Screen {
    private static final int SPLASH = 45;
    private static final int RESULT_SHOW = 70;
    private static final int EJECT_LEN = com.minpaku.client.scene.EjectScene.LEN;

    private final Packets.Meeting meeting;
    private int age;
    private final Set<UUID> voted = new HashSet<>();
    @Nullable
    private UUID selected;
    private boolean myVoteSent;
    @Nullable
    private Packets.MeetingResult result;
    private int resultAge;
    private EditBox input;

    // レイアウト
    private int px, py, pw, ph, cardW, cardH, chatX, chatW;

    public MeetingScreen(Packets.Meeting meeting) {
        super(Component.literal("会議"));
        this.meeting = meeting;
    }

    public void onVoted(List<UUID> voters) {
        voted.clear();
        voted.addAll(voters);
    }

    public void onResult(Packets.MeetingResult r) {
        this.result = r;
        this.resultAge = 0;
        ClientGame.play(SoundEvents.NOTE_BLOCK_BELL, 0.8f);
    }

    @Override
    protected void init() {
        pw = Math.min(width - 20, 560);
        ph = Math.min(height - 20, 320);
        px = (width - pw) / 2;
        py = (height - ph) / 2;
        chatW = Math.max(120, pw * 30 / 100);
        chatX = px + pw - chatW - 10;
        int gridW = chatX - px - 22;
        cardW = (gridW - 6) / 2;
        cardH = 28;
        input = new EditBox(font, chatX + 4, py + ph - 26, chatW - 8, 14, Component.literal("発言"));
        input.setMaxLength(100);
        input.setBordered(false);
        input.setTextColor(0xFF2B1D12);
        input.setHint(Ui.sans("ここに発言（Enterで送信）").copy().withStyle(s -> s.withColor(0x8D7A62)));
        addRenderableWidget(input);
    }

    @Override
    public void tick() {
        age++;
        if (age == 1) {
            if (meeting.reason() == 1) {
                ClientGame.play(SoundEvents.ANVIL_LAND, 0.5f);
                ClientGame.play(SoundEvents.ELDER_GUARDIAN_CURSE, 1.2f);
            } else {
                ClientGame.play(SoundEvents.BELL_BLOCK, 1.0f);
                ClientGame.play(SoundEvents.BELL_BLOCK, 1.3f);
            }
        }
        if (result != null) {
            resultAge++;
            if (resultAge >= RESULT_SHOW) {
                com.minpaku.client.scene.EjectScene.tick(resultAge - RESULT_SHOW, cast());
            }
            if (resultAge >= RESULT_SHOW + EJECT_LEN) {
                onClose();
            }
        }
    }

    private int meetingTime() {
        return Math.max(0, age - SPLASH);
    }

    private boolean votingOpen() {
        return result == null && age >= SPLASH && meetingTime() >= meeting.discussionTicks() - SPLASH;
    }

    // ------------------------------------------------------------ 描画

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float pt) {
        g.fill(0, 0, width, height, 0xC0100C08);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float pt) {
        renderBackground(g, mouseX, mouseY, pt);
        if (age < SPLASH) {
            splash(g, age + pt);
            return;
        }
        if (result != null && resultAge >= RESULT_SHOW) {
            eject(g, resultAge - RESULT_SHOW + pt);
            return;
        }
        table(g, mouseX, mouseY);
        input.visible = true;
        input.render(g, mouseX, mouseY, pt);
    }

    private void splash(GuiGraphics g, float t) {
        input.visible = false;
        int cx = width / 2;
        int cy = height / 2;
        boolean body = meeting.reason() == 1;
        float pop = t < 8 ? 0.5f + t / 8f * 0.7f : (t < 14 ? 1.2f - (t - 8) / 6f * 0.2f : 1f);
        float a = Mth.clamp(t / 4f, 0f, 1f) * (t > SPLASH - 8 ? (SPLASH - t) / 8f : 1f);
        int color = body ? 0xFFFF5A4A : 0xFFFFC94A;
        g.fill(0, cy - 46, width, cy + 46, Ui.alpha(body ? 0xFF3A0806 : 0xFF3A2A06, a * 0.8f));
        g.fill(0, cy - 46, width, cy - 45, Ui.alpha(Ui.GOLD, a));
        g.fill(0, cy + 45, width, cy + 46, Ui.alpha(Ui.GOLD, a));
        Ui.text(g, Ui.serif(body ? "死体発見" : "緊急会議"), cx, cy - 34, Ui.alpha(color, a), 4.4f * pop, 0, true);
        String sub = body ? meeting.caller() + " が " + meeting.bodyName() + " の死体を見つけた"
                : meeting.caller() + " が呼び鈴を鳴らした";
        Ui.text(g, Ui.sans(sub), cx, cy + 22, Ui.alpha(0xFFFFFFFF, a), 1.2f, 0, false);
    }

    private int cardX(int i) {
        return px + 12 + (i % 2) * (cardW + 6);
    }

    private int cardY(int i) {
        return py + 52 + (i / 2) * (cardH + 5);
    }

    private void table(GuiGraphics g, int mouseX, int mouseY) {
        Ui.sprite(g, "panel", px, py, pw, ph);
        boolean body = meeting.reason() == 1;
        // 見出し
        Ui.stamp(g, body ? "死体発見" : "緊急会議", px + 26, py + 24, 30, Ui.VERMILION, -8, false);
        Ui.serif(g, "囲炉裏ばた会議", px + 48, py + 11, Ui.INK, 1.6f, -1);
        String sub = body ? meeting.caller() + " が " + meeting.bodyName() + " の死体を見つけた"
                : meeting.caller() + " が呼び鈴を鳴らした";
        Ui.sans(g, sub, px + 48, py + 30, Ui.INK_SOFT, 0.85f, -1);
        g.fill(px + 10, py + 43, px + pw - 10, py + 44, Ui.INK);
        g.fill(px + 10, py + 45, px + pw - 10, py + 46, Ui.INK);

        // タイマー
        String head;
        int remain;
        int total;
        if (result != null) {
            head = "投票結果";
            remain = 0;
            total = 1;
        } else if (!votingOpen()) {
            head = "議論の時間";
            remain = meeting.discussionTicks() - SPLASH - meetingTime();
            total = Math.max(1, meeting.discussionTicks() - SPLASH);
        } else {
            head = "投票の時間";
            remain = meeting.discussionTicks() + meeting.votingTicks() - SPLASH - meetingTime();
            total = Math.max(1, meeting.votingTicks());
        }
        float tx = px + pw - 26;
        float ty = py + 22;
        Ui.disc(g, tx, ty, 15, 0xFFF4ECD8);
        Ui.ring(g, tx, ty, 16, 12.5f, Mth.clamp(remain / (float) total, 0f, 1f), Ui.VERMILION, 0xFFD8CCB2);
        if (result == null) {
            Ui.sans(g, String.valueOf(Math.max(0, remain / 20 + 1)), tx, ty - 5, Ui.INK, 1.1f, 0);
        }
        Ui.sans(g, head, tx - 22, ty - 4, Ui.VERMILION, 0.85f, 1);

        // 参加者のカード
        List<Packets.Participant> ps = meeting.participants();
        for (int i = 0; i < ps.size(); i++) {
            card(g, ps.get(i), cardX(i), cardY(i), mouseX, mouseY);
        }

        // スキップ
        int rows = (ps.size() + 1) / 2;
        int by = cardY(rows * 2) + 2;
        boolean canPress = votingOpen() && meeting.canVote() && !myVoteSent;
        boolean hover = inside(mouseX, mouseY, px + 12, by, 70, 18);
        Ui.button(g, px + 12, by, 70, 18, "スキップ", false, hover && canPress, canPress, 1f);
        String info;
        if (result != null) {
            info = "スキップした人";
        } else if (!meeting.canVote()) {
            info = "幽霊は投票できません";
        } else if (myVoteSent) {
            info = "投票しました。ほかの人を待っています";
        } else if (!votingOpen()) {
            info = "まずは話し合おう（投票はあとで）";
        } else {
            info = "投票済み " + voted.size() + " 人　・　選んでから「投票」で決定";
        }
        Ui.sans(g, info, px + 88, by + 5, Ui.INK_SOFT, 0.8f, -1);
        if (result != null) {
            List<UUID> skips = new ArrayList<>();
            for (Packets.Vote v : result.votes()) {
                if (v.target().equals(Packets.NONE)) {
                    skips.add(v.voter());
                }
            }
            drawVoters(g, skips, px + 150, by + 3);
        }

        chat(g);
    }

    private void card(GuiGraphics g, Packets.Participant p, int x, int y, int mouseX, int mouseY) {
        boolean hover = inside(mouseX, mouseY, x, y, cardW, cardH) && canVoteFor(p);
        boolean sel = p.id().equals(selected);
        String sp = !p.alive() ? "card_dead" : sel ? "card_selected" : hover ? "card_hover" : "card";
        Ui.sprite(g, sp, x, y, cardW, cardH);
        Ui.face(g, p.id(), x + 5, y + 5, 18);
        if (!p.alive()) {
            g.fill(x + 5, y + 5, x + 23, y + 23, 0x88000000);
            Ui.sans(g, "✕", x + 14, y + 8, 0xFFFF4444, 1.2f, 0);
        }
        int nameColor = !p.alive() ? 0xFF7A6A58 : ClientGame.isMate(p.id()) ? 0xFFA8231B : Ui.INK;
        Ui.sans(g, p.name(), x + 28, y + 5, nameColor, 1f, -1);
        Ui.sans(g, p.alive() ? "生存" : "死亡", x + 28, y + 16, 0xFF8D7A62, 0.7f, -1);
        if (meeting.caller().equals(p.name())) {
            Ui.sans(g, meeting.reason() == 1 ? "通報" : "呼び鈴", x + 28 + (int) (Ui.width(Ui.sans(p.name()))) + 4, y + 6,
                    Ui.VERMILION, 0.7f, -1);
        }
        if (result == null) {
            if (voted.contains(p.id())) {
                Ui.stamp(g, "済", x + cardW - 14, y + cardH / 2f, 16, Ui.VERMILION, -8, false);
            }
            if (sel && !myVoteSent) {
                boolean hv = inside(mouseX, mouseY, x + cardW - 76, y + 7, 34, 14);
                boolean hc = inside(mouseX, mouseY, x + cardW - 40, y + 7, 34, 14);
                Ui.button(g, x + cardW - 76, y + 7, 34, 14, "投票", true, hv, true, 0.8f);
                Ui.button(g, x + cardW - 40, y + 7, 34, 14, "やめる", false, hc, true, 0.8f);
            }
        } else {
            List<UUID> voters = new ArrayList<>();
            for (Packets.Vote v : result.votes()) {
                if (v.target().equals(p.id())) {
                    voters.add(v.voter());
                }
            }
            drawVoters(g, voters, x + cardW - 6 - voters.size() * 11, y + 9);
        }
    }

    private void drawVoters(GuiGraphics g, List<UUID> voters, int x, int y) {
        for (int i = 0; i < voters.size(); i++) {
            int vx = x + i * 11;
            if (result != null && result.anonymous()) {
                g.fill(vx, y, vx + 10, y + 10, 0xFF9E9E9E);
            } else {
                Ui.face(g, voters.get(i), vx, y, 10);
            }
        }
    }

    private void chat(GuiGraphics g) {
        int x = chatX;
        int y = py + 52;
        int h = ph - 62;
        Ui.sprite(g, "box_dim", x, y, chatW, h);
        Ui.serif(g, "話し合い", x + 6, y + 5, Ui.INK, 1f, -1);
        g.fill(x + 5, y + 16, x + chatW - 5, y + 17, Ui.GOLD);
        // 下から上へ、入るぶんだけ
        int bottom = py + ph - 32;
        int yy = bottom;
        List<Packets.Talk> list = ClientGame.talk;
        for (int i = list.size() - 1; i >= 0 && yy > y + 22; i--) {
            Packets.Talk t = list.get(i);
            List<FormattedCharSequence> lines = Ui.split(t.text(), chatW - 22);
            int need = 10 + lines.size() * 9 + 3;
            if (yy - need < y + 20) {
                break;
            }
            yy -= need;
            Ui.face(g, t.id(), x + 5, yy, 8);
            Ui.sans(g, t.name(), x + 16, yy, Ui.INK, 0.8f, -1);
            int ly = yy + 10;
            for (FormattedCharSequence l : lines) {
                g.pose().pushPose();
                g.pose().translate(x + 16, ly, 0);
                g.pose().scale(0.85f, 0.85f, 1f);
                g.drawString(font, l, 0, 0, 0xFF3E2C1C, false);
                g.pose().popPose();
                ly += 9;
            }
        }
        if (list.isEmpty()) {
            Ui.sans(g, "まだ誰も話していません", x + chatW / 2f, y + 30, 0xFF8D7A62, 0.8f, 0);
        }
        Ui.sprite(g, "box", x, py + ph - 30, chatW, 20);
    }

    private void eject(GuiGraphics g, float t) {
        input.visible = false;
        if (result == null) {
            return;
        }
        com.minpaku.client.scene.EjectScene.render(g, t, cast());
    }

    @Nullable
    private com.minpaku.client.scene.EjectScene.Cast cast;

    /** 追放の場面に出す人（担ぐのは生き残り。自分が生きていれば自分も入る） */
    private com.minpaku.client.scene.EjectScene.Cast cast() {
        if (cast == null && result != null) {
            java.util.List<UUID> carriers = new java.util.ArrayList<>();
            UUID me = minecraft != null && minecraft.player != null ? minecraft.player.getUUID() : null;
            for (Packets.Participant p : meeting.participants()) {
                if (p.alive() && !p.id().equals(result.ejected()) && p.id().equals(me)) {
                    carriers.add(p.id());
                }
            }
            for (Packets.Participant p : meeting.participants()) {
                if (p.alive() && !p.id().equals(result.ejected()) && !p.id().equals(me) && carriers.size() < 4) {
                    carriers.add(p.id());
                }
            }
            cast = new com.minpaku.client.scene.EjectScene.Cast(result.ejected(), result.ejectedName(), result.ejectText(),
                    carriers);
        }
        return cast;
    }

    // ------------------------------------------------------------ 操作

    private boolean canVoteFor(Packets.Participant p) {
        return votingOpen() && meeting.canVote() && !myVoteSent && p.alive();
    }

    private static boolean inside(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (input.isMouseOver(mx, my) || inside(mx, my, chatX, py + ph - 30, chatW, 20)) {
            setFocused(input);
            input.setFocused(true);
            return true;
        }
        input.setFocused(false);
        if (age < SPLASH || result != null || !votingOpen() || !meeting.canVote() || myVoteSent) {
            return super.mouseClicked(mx, my, button);
        }
        List<Packets.Participant> ps = meeting.participants();
        for (int i = 0; i < ps.size(); i++) {
            Packets.Participant p = ps.get(i);
            int x = cardX(i);
            int y = cardY(i);
            if (p.id().equals(selected)) {
                if (inside(mx, my, x + cardW - 76, y + 7, 34, 14)) {
                    sendVote(p.id());
                    return true;
                }
                if (inside(mx, my, x + cardW - 40, y + 7, 34, 14)) {
                    selected = null;
                    return true;
                }
            }
            if (inside(mx, my, x, y, cardW, cardH) && canVoteFor(p)) {
                selected = p.id();
                ClientGame.play(SoundEvents.UI_BUTTON_CLICK, 1.0f);
                return true;
            }
        }
        int rows = (ps.size() + 1) / 2;
        int by = cardY(rows * 2) + 2;
        if (inside(mx, my, px + 12, by, 70, 18)) {
            sendVote(Packets.NONE);
            return true;
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (input.isFocused() && (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER)) {
            String text = input.getValue().trim();
            if (!text.isEmpty() && minecraft != null && minecraft.player != null) {
                minecraft.player.connection.sendChat(text);
            }
            input.setValue("");
            return true;
        }
        if (!input.isFocused() && (key == GLFW.GLFW_KEY_T || key == GLFW.GLFW_KEY_ENTER)) {
            setFocused(input);
            input.setFocused(true);
            return true;
        }
        return super.keyPressed(key, scan, mods);
    }

    private void sendVote(UUID target) {
        myVoteSent = true;
        selected = target.equals(Packets.NONE) ? null : target;
        ClientPlayNetworking.send(new Packets.CastVote(target));
        ClientGame.play(SoundEvents.UI_BUTTON_CLICK, 1.4f);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(null);
    }
}
