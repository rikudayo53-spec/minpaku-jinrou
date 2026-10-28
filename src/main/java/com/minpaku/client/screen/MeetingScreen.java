package com.minpaku.client.screen;

import com.minpaku.client.ClientGame;
import com.minpaku.client.GameHud;
import com.minpaku.network.Packets;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.PlayerFaceRenderer;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * 会議と投票の画面。
 *   はじめ … 「緊急会議！」「死体発見！」の演出
 *   議論 → 投票 → 結果（誰が誰に入れたか）→ 追放の演出 → 閉じる
 */
public class MeetingScreen extends Screen {
    private static final int SPLASH = 45;
    private static final int RESULT_SHOW = 70;
    private static final int EJECT_LEN = 110;

    private static final int CARD_W = 160;
    private static final int CARD_H = 30;

    private final Packets.Meeting meeting;
    private int age;
    private final Set<UUID> voted = new HashSet<>();
    @Nullable
    private UUID selected;
    private boolean myVoteSent;
    @Nullable
    private Packets.MeetingResult result;
    private int resultAge;

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
            if (resultAge == RESULT_SHOW) {
                ClientGame.play(SoundEvents.ENDERMAN_TELEPORT, 0.6f);
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
        g.fill(0, 0, width, height, 0xE0101418);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float pt) {
        super.render(g, mouseX, mouseY, pt);
        if (age < SPLASH) {
            renderSplash(g, age + pt);
            return;
        }
        if (result != null && resultAge >= RESULT_SHOW) {
            renderEject(g, resultAge - RESULT_SHOW + pt);
            return;
        }
        renderTable(g, mouseX, mouseY);
    }

    private void renderSplash(GuiGraphics g, float t) {
        int cx = width / 2;
        int cy = height / 2;
        boolean body = meeting.reason() == 1;
        float pop = t < 8 ? 0.5f + t / 8f * 0.7f : (t < 14 ? 1.2f - (t - 8) / 6f * 0.2f : 1f);
        int a = Math.max(8, (int) (Mth.clamp(t / 4f, 0f, 1f) * (t > SPLASH - 8 ? (SPLASH - t) / 8f : 1f) * 255));
        int color = body ? 0xFF4444 : 0xFFC107;
        g.fill(0, cy - 50, width, cy + 50, (int) (a * 0.6f) << 24 | (body ? 0x400000 : 0x403000));
        GameHud.drawBig(g, font, body ? "死体発見！" : "緊急会議！", cx, cy - 30, 5f * pop, a << 24 | color);
        String sub = body ? meeting.caller() + " が " + meeting.bodyName() + " の死体を見つけた"
                : meeting.caller() + " が呼び鈴を鳴らした";
        g.drawCenteredString(font, sub, cx, cy + 22, a << 24 | 0xFFFFFF);
    }

    private int columns() {
        return width >= CARD_W * 2 + 30 ? 2 : 1;
    }

    private int cardX(int i) {
        int cols = columns();
        int total = cols * CARD_W + (cols - 1) * 10;
        return width / 2 - total / 2 + (i % cols) * (CARD_W + 10);
    }

    private int cardY(int i) {
        return 50 + (i / columns()) * (CARD_H + 6);
    }

    private void renderTable(GuiGraphics g, int mouseX, int mouseY) {
        int cx = width / 2;
        // 上の見出しとタイマー
        String head;
        int remain;
        if (result != null) {
            head = "投票結果";
            remain = -1;
        } else if (!votingOpen()) {
            head = "議論の時間";
            remain = meeting.discussionTicks() - SPLASH - meetingTime();
        } else {
            head = meeting.canVote() ? (myVoteSent ? "投票しました。ほかの人を待っています" : "投票の時間 ― 追放したい人を選ぼう") : "投票の時間（幽霊は投票できません）";
            remain = meeting.discussionTicks() + meeting.votingTicks() - SPLASH - meetingTime();
        }
        GameHud.drawBig(g, font, head, cx, 12, 1.6f, 0xFFFFFFFF);
        if (remain >= 0) {
            g.drawCenteredString(font, "あと " + (remain / 20 + 1) + " 秒", cx, 32, remain < 200 ? 0xFFFF5555 : 0xFFCCCCCC);
        }

        List<Packets.Participant> ps = meeting.participants();
        for (int i = 0; i < ps.size(); i++) {
            renderCard(g, ps.get(i), cardX(i), cardY(i), mouseX, mouseY);
        }

        // スキップ
        int rows = (ps.size() + columns() - 1) / columns();
        int by = cardY(rows * columns()) + 4;
        boolean skipHover = inside(mouseX, mouseY, cx - 50, by, 100, 18);
        boolean canPress = votingOpen() && meeting.canVote() && !myVoteSent;
        g.fill(cx - 50, by, cx + 50, by + 18, canPress ? (skipHover ? 0xFF607D8B : 0xFF455A64) : 0xFF263238);
        g.drawCenteredString(font, "スキップ", cx, by + 5, canPress ? 0xFFFFFFFF : 0xFF777777);

        if (result != null) {
            // スキップした人
            List<UUID> skips = new ArrayList<>();
            for (Packets.Vote v : result.votes()) {
                if (v.target().equals(Packets.NONE)) {
                    skips.add(v.voter());
                }
            }
            drawVoters(g, skips, cx + 56, by + 2);
        }
    }

    private void renderCard(GuiGraphics g, Packets.Participant p, int x, int y, int mouseX, int mouseY) {
        boolean hover = inside(mouseX, mouseY, x, y, CARD_W, CARD_H);
        boolean isSelected = p.id().equals(selected);
        int bg = !p.alive() ? 0xFF2A2A2A : isSelected ? 0xFF8D6E63 : hover && canVoteFor(p) ? 0xFF5D4037 : 0xFF3E2723;
        g.fill(x, y, x + CARD_W, y + CARD_H, bg);
        g.renderOutline(x, y, CARD_W, CARD_H, isSelected ? 0xFFFFE082 : 0xFF6D4C41);
        PlayerFaceRenderer.draw(g, ClientGame.skin(p.id()), x + 4, y + 4, 22);
        if (!p.alive()) {
            g.fill(x + 4, y + 4, x + 26, y + 26, 0xAA000000);
            g.drawString(font, "✖", x + 11, y + 11, 0xFFFF4444, false);
        }
        int nameColor = !p.alive() ? 0xFF777777 : ClientGame.isMate(p.id()) ? 0xFFFF6666 : 0xFFFFFFFF;
        g.drawString(font, p.name(), x + 32, y + 5, nameColor, false);
        if (meeting.caller().equals(p.name())) {
            g.drawString(font, "★", x + CARD_W - 14, y + 4, 0xFFFFFFFF, false);
        }
        if (result == null) {
            if (voted.contains(p.id())) {
                g.drawString(font, "投票済み", x + 32, y + 17, 0xFF81C784, false);
            }
            if (isSelected && !myVoteSent) {
                g.fill(x + CARD_W - 74, y + 15, x + CARD_W - 38, y + 27, 0xFF2E7D32);
                g.drawCenteredString(font, "投票", x + CARD_W - 56, y + 17, 0xFFFFFFFF);
                g.fill(x + CARD_W - 34, y + 15, x + CARD_W - 4, y + 27, 0xFF616161);
                g.drawCenteredString(font, "やめる", x + CARD_W - 19, y + 17, 0xFFFFFFFF);
            }
        } else {
            List<UUID> voters = new ArrayList<>();
            for (Packets.Vote v : result.votes()) {
                if (v.target().equals(p.id())) {
                    voters.add(v.voter());
                }
            }
            drawVoters(g, voters, x + 32, y + 16);
        }
    }

    private void drawVoters(GuiGraphics g, List<UUID> voters, int x, int y) {
        for (int i = 0; i < voters.size(); i++) {
            UUID v = voters.get(i);
            int vx = x + i * 11;
            if (result != null && result.anonymous()) {
                g.fill(vx, y, vx + 10, y + 10, 0xFF9E9E9E);
            } else {
                PlayerFaceRenderer.draw(g, ClientGame.skin(v), vx, y, 10);
            }
        }
    }

    private void renderEject(GuiGraphics g, float t) {
        int cx = width / 2;
        int cy = height / 2;
        g.fill(0, 0, width, height, 0xFF05060A);
        // 星
        for (int i = 0; i < 60; i++) {
            int sx = (int) ((i * 97 + t * (1 + i % 3)) % width);
            int sy = (i * 53) % height;
            g.fill(sx, sy, sx + 1, sy + 1, 0xFFFFFFFF);
        }
        if (result == null) {
            return;
        }
        boolean someone = !result.ejected().equals(Packets.NONE);
        if (someone) {
            // 顔が回りながら横切る
            float x = -40 + (width + 80) * Mth.clamp(t / (EJECT_LEN * 0.8f), 0f, 1f);
            g.pose().pushPose();
            g.pose().translate(x, cy - 50, 0);
            g.pose().mulPose(com.mojang.math.Axis.ZP.rotationDegrees(t * 8f));
            PlayerFaceRenderer.draw(g, ClientGame.skin(result.ejected()), -16, -16, 32);
            g.pose().popPose();
        }
        // 文字を1文字ずつ出す
        String text = result.ejectText();
        int shown = Mth.clamp((int) ((t - 10) / 2.5f), 0, text.length());
        GameHud.drawBig(g, font, text.substring(0, shown), cx, cy, 2f, 0xFFFFFFFF);
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
        if (age < SPLASH || result != null || !votingOpen() || !meeting.canVote() || myVoteSent) {
            return super.mouseClicked(mx, my, button);
        }
        List<Packets.Participant> ps = meeting.participants();
        for (int i = 0; i < ps.size(); i++) {
            Packets.Participant p = ps.get(i);
            int x = cardX(i);
            int y = cardY(i);
            if (p.id().equals(selected)) {
                if (inside(mx, my, x + CARD_W - 74, y + 15, 36, 12)) {
                    sendVote(p.id());
                    return true;
                }
                if (inside(mx, my, x + CARD_W - 34, y + 15, 30, 12)) {
                    selected = null;
                    return true;
                }
            }
            if (inside(mx, my, x, y, CARD_W, CARD_H) && canVoteFor(p)) {
                selected = p.id();
                ClientGame.play(SoundEvents.UI_BUTTON_CLICK, 1.0f);
                return true;
            }
        }
        int rows = (ps.size() + columns() - 1) / columns();
        int by = cardY(rows * columns()) + 4;
        if (inside(mx, my, width / 2 - 50, by, 100, 18)) {
            sendVote(Packets.NONE);
            return true;
        }
        return super.mouseClicked(mx, my, button);
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
