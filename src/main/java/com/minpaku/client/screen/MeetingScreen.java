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
    /** 会議中に倒れた人（ゲッサー） */
    private final Set<UUID> deadNow = new HashSet<>();
    private String deathText = "";
    private int deathAge = 999;
    /** 見取り図（会議中に開ける） */
    @Nullable
    private MapView mapView;

    /** ゲッサー：当てる相手を選んでいるところ／役職を選んでいるところ */
    private boolean guessMode;
    @Nullable
    private UUID guessTarget;

    // レイアウト
    private int px, py, pw, ph, cardW, cardH, chatX, chatW;

    public MeetingScreen(Packets.Meeting meeting) {
        super(Component.literal("会議"));
        this.meeting = meeting;
    }

    /** 会議中に誰かが撃ち抜かれた */
    public void onDeath(UUID id, String text) {
        deadNow.add(id);
        deathText = text;
        deathAge = 0;
        if (id.equals(selected)) {
            selected = null;
        }
        if (id.equals(guessTarget)) {
            guessTarget = null;
        }
        ClientGame.play(SoundEvents.GENERIC_EXPLODE.value(), 1.8f);
        ClientGame.play(SoundEvents.PLAYER_HURT, 0.8f);
    }

    @Nullable
    private UUID me() {
        return minecraft != null && minecraft.player != null ? minecraft.player.getUUID() : null;
    }

    private boolean alive(Packets.Participant p) {
        return p.alive() && !deadNow.contains(p.id());
    }

    private boolean canVote() {
        UUID me = me();
        return meeting.canVote() && (me == null || !deadNow.contains(me));
    }

    private boolean isGuesser() {
        com.minpaku.game.Role r = ClientGame.role();
        return canVote() && result == null && r != null
                && (r == com.minpaku.game.Role.NICE_GUESSER || r == com.minpaku.game.Role.EVIL_GUESSER);
    }

    /** 当てられる役職（ナイスは村人陣営以外、イビルは人狼陣営以外） */
    private List<com.minpaku.game.Role> guessRoles() {
        List<com.minpaku.game.Role> out = new ArrayList<>();
        boolean evil = ClientGame.role() == com.minpaku.game.Role.EVIL_GUESSER;
        for (com.minpaku.game.Role r : com.minpaku.game.Role.all()) {
            if (evil ? r.team != com.minpaku.game.Role.Team.WOLF : r.team != com.minpaku.game.Role.Team.VILLAGE) {
                out.add(r);
            }
        }
        return out;
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
        deathAge++;
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
        g.fill(0, 0, width, height, ClientGame.meetingFree && result == null ? 0x90100C08 : 0xC0100C08);
        if (ClientGame.meetingFree && result == null && age >= SPLASH) {
            Ui.sans(g, "Esc で閉じると、歩きながら話せます（V でまた開く）", width / 2f, height - 10, 0xFFFFE0B2, 0.75f, 0);
        }
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
        // 会議中に撃ち抜かれた
        if (deathAge < 70) {
            float a = Math.min(1f, deathAge / 4f) * Math.min(1f, (70 - deathAge) / 10f);
            g.fill(0, height / 2 - 24, width, height / 2 + 24, Ui.alpha(0xFF3A0806, a * 0.85f));
            Ui.text(g, Ui.serif(deathText), width / 2f, height / 2f - 12, Ui.alpha(0xFFFF8A80, a), 2.2f, 0, true);
        }
        if (guessTarget != null) {
            guessPicker(g, mouseX, mouseY);
        }
        if (mapView != null) {
            mapView.render(g, width, height, mouseX, mouseY);
        }
    }

    // ゲッサーの役職えらび
    private int gpX, gpY, gpW, gpH, gpCols;

    private void guessPicker(GuiGraphics g, int mouseX, int mouseY) {
        List<com.minpaku.game.Role> roles = guessRoles();
        gpCols = 3;
        int bw = 92;
        int bh = 16;
        int rows = (roles.size() + gpCols - 1) / gpCols;
        gpW = gpCols * (bw + 4) + 16;
        gpH = rows * (bh + 4) + 40;
        gpX = (width - gpW) / 2;
        gpY = (height - gpH) / 2;
        g.fill(0, 0, width, height, 0x80000000);
        Ui.sprite(g, "panel", gpX, gpY, gpW, gpH);
        String name = "";
        for (Packets.Participant p : meeting.participants()) {
            if (p.id().equals(guessTarget)) {
                name = p.name();
            }
        }
        Ui.fitSans(g, name + " の役職は？（外れたら自分が倒れる）", gpX + gpW / 2f, gpY + 8, gpW - 16, Ui.INK, 0.9f, 0);
        for (int i = 0; i < roles.size(); i++) {
            com.minpaku.game.Role r = roles.get(i);
            int bx = gpX + 8 + (i % gpCols) * (bw + 4);
            int by = gpY + 24 + (i / gpCols) * (bh + 4);
            boolean hv = inside(mouseX, mouseY, bx, by, bw, bh);
            g.fill(bx, by, bx + bw, by + bh, hv ? 0xFFFFF3D6 : 0xFFEADFC6);
            g.fill(bx, by, bx + 3, by + bh, 0xFF000000 | r.color);
            Ui.fitSans(g, r.name, bx + bw / 2f + 1, by + 4, bw - 8, Ui.INK, 0.85f, 0);
        }
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
        return py + 52 + (i / 2) * (cardH + 5) - Math.round(scroll);
    }

    // 参加者が多いときはスクロール（スキップの行は一番下に固定）
    private float scroll;

    private int listTop() {
        return py + 50;
    }

    private int listBottom() {
        return py + ph - 32;
    }

    private int skipY() {
        return py + ph - 28;
    }

    private float maxScroll() {
        int rows = (meeting.participants().size() + 1) / 2;
        int content = rows * (cardH + 5) + 2;
        return Math.max(0, content - (listBottom() - listTop()));
    }

    private boolean inList(double my) {
        return my >= listTop() && my < listBottom();
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double sx, double sy) {
        if (mapView == null && mx < chatX && maxScroll() > 0) {
            scroll = Mth.clamp(scroll - (float) sy * 18f, 0f, maxScroll());
            return true;
        }
        return super.mouseScrolled(mx, my, sx, sy);
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
        boolean hm = inside(mouseX, mouseY, px + pw - 150, py + 14, 46, 16);
        Ui.button(g, px + pw - 150, py + 14, 46, 16, "マップ", false, hm, true, 0.85f);

        // 参加者のカード
        List<Packets.Participant> ps = meeting.participants();
        scroll = Mth.clamp(scroll, 0f, maxScroll());
        int lmx = inList(mouseY) ? mouseX : -9999;
        g.enableScissor(px + 8, listTop(), chatX - 4, listBottom());
        for (int i = 0; i < ps.size(); i++) {
            int cy = cardY(i);
            if (cy + cardH < listTop() || cy > listBottom()) {
                continue;
            }
            card(g, ps.get(i), cardX(i), cy, lmx, mouseY);
        }
        g.disableScissor();
        if (maxScroll() > 0) {
            // スクロールバー
            int bx = chatX - 8;
            int top = listTop();
            int hgt = listBottom() - top;
            float frac = hgt / (hgt + maxScroll());
            int bh = Math.max(12, (int) (hgt * frac));
            int byy = top + (int) ((hgt - bh) * (scroll / maxScroll()));
            g.fill(bx, top, bx + 3, top + hgt, 0x40000000);
            g.fill(bx, byy, bx + 3, byy + bh, 0xFF8D6E4A);
            if (scroll < maxScroll() - 1) {
                Ui.sans(g, "▼ ホイールで下へ", px + (chatX - px) / 2f, listBottom() - 9, Ui.alpha(Ui.INK_SOFT, 0.8f), 0.7f, 0);
            }
        }

        // スキップ（一番下に固定）
        int by = skipY();
        boolean canPress = votingOpen() && canVote() && !myVoteSent;
        if (isGuesser() && age >= SPLASH) {
            boolean hg = inside(mouseX, mouseY, px + pw - chatW - 112, by, 92, 18);
            Ui.button(g, px + pw - chatW - 112, by, 92, 18, guessMode ? "やめる" : "役職を当てる", guessMode, hg, true, 0.85f);
        }
        boolean hover = inside(mouseX, mouseY, px + 12, by, 70, 18);
        Ui.button(g, px + 12, by, 70, 18, "スキップ", false, hover && canPress, canPress, 1f);
        String info;
        if (result != null) {
            info = "スキップした人";
        } else if (guessMode) {
            info = "撃ち抜く相手を選ぼう";
        } else if (!canVote()) {
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
        boolean hover = inside(mouseX, mouseY, x, y, cardW, cardH) && (canVoteFor(p) || canGuess(p));
        boolean sel = p.id().equals(selected);
        boolean living = alive(p);
        String sp = !living ? "card_dead" : sel ? "card_selected" : hover ? "card_hover" : "card";
        Ui.sprite(g, sp, x, y, cardW, cardH);
        Ui.face(g, p.id(), x + 5, y + 5, 18);
        if (!living) {
            g.fill(x + 5, y + 5, x + 23, y + 23, 0x88000000);
            Ui.sans(g, "✕", x + 14, y + 8, 0xFFFF4444, 1.2f, 0);
        }
        int nameColor = !living ? 0xFF7A6A58 : ClientGame.isMate(p.id()) ? 0xFFA8231B : Ui.INK;
        Ui.sans(g, p.name(), x + 28, y + 5, nameColor, 1f, -1);
        String known = ClientGame.knownRoles.get(p.id());
        if (known != null) {
            com.minpaku.game.Role kr = com.minpaku.game.Role.byId(known);
            Ui.sans(g, (living ? "" : "死亡・") + kr.name, x + 28, y + 16, 0xFF000000 | kr.color, 0.7f, -1);
        } else {
            Ui.sans(g, living ? "生存" : "死亡", x + 28, y + 16, 0xFF8D7A62, 0.7f, -1);
        }
        if (guessMode && canGuess(p)) {
            Ui.stamp(g, "的", x + cardW - 14, y + cardH / 2f, 16, 0xFF6A1B9A, 8, false);
        }
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
                if (alive(p) && !p.id().equals(result.ejected()) && p.id().equals(me)) {
                    carriers.add(p.id());
                }
            }
            for (Packets.Participant p : meeting.participants()) {
                if (alive(p) && !p.id().equals(result.ejected()) && !p.id().equals(me) && carriers.size() < 4) {
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
        return votingOpen() && canVote() && !myVoteSent && alive(p);
    }

    private boolean canGuess(Packets.Participant p) {
        return guessMode && isGuesser() && alive(p) && !p.id().equals(me())
                && !(ClientGame.role() == com.minpaku.game.Role.EVIL_GUESSER && ClientGame.isMate(p.id()));
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
        if (mapView != null) {
            if (mapView.click(mx, my)) {
                mapView = null;
            }
            return true;
        }
        if (age >= SPLASH && result == null && inside(mx, my, px + pw - 150, py + 14, 46, 16)) {
            mapView = new MapView();
            ClientGame.play(SoundEvents.BOOK_PAGE_TURN, 1.0f);
            return true;
        }
        List<Packets.Participant> ps = meeting.participants();
        // ゲッサー：役職えらび
        if (guessTarget != null) {
            List<com.minpaku.game.Role> roles = guessRoles();
            for (int i = 0; i < roles.size(); i++) {
                int bx = gpX + 8 + (i % gpCols) * 96;
                int by = gpY + 24 + (i / gpCols) * 20;
                if (inside(mx, my, bx, by, 92, 16)) {
                    ClientPlayNetworking.send(new Packets.Act("guess", guessTarget, roles.get(i).id));
                    ClientGame.play(SoundEvents.CROSSBOW_SHOOT, 0.8f);
                    guessTarget = null;
                    guessMode = false;
                    return true;
                }
            }
            guessTarget = null;
            return true;
        }
        if (isGuesser() && age >= SPLASH) {
            int by0 = skipY();
            if (inside(mx, my, px + pw - chatW - 112, by0, 92, 18)) {
                guessMode = !guessMode;
                ClientGame.play(SoundEvents.UI_BUTTON_CLICK, guessMode ? 0.8f : 1.2f);
                return true;
            }
            if (guessMode) {
                for (int i = 0; i < ps.size(); i++) {
                    if (inList(my) && inside(mx, my, cardX(i), cardY(i), cardW, cardH) && canGuess(ps.get(i))) {
                        guessTarget = ps.get(i).id();
                        ClientGame.play(SoundEvents.UI_BUTTON_CLICK, 1.0f);
                        return true;
                    }
                }
                return true;
            }
        }
        if (age < SPLASH || result != null || !votingOpen() || !canVote() || myVoteSent) {
            return super.mouseClicked(mx, my, button);
        }
        for (int i = 0; i < ps.size() && inList(my); i++) {
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
        int by = skipY();
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
        // 井戸端会議では閉じて歩き回れる（結果が出たら閉じられない）
        return ClientGame.meetingFree && result == null && age >= SPLASH && guessTarget == null && mapView == null;
    }

    /** 画面を閉じているときの上の表示（残り時間） */
    public String statusText() {
        if (result != null) {
            return "結果発表";
        }
        if (!votingOpen()) {
            return "議論 あと" + Math.max(0, (meeting.discussionTicks() - SPLASH - meetingTime()) / 20 + 1) + "秒";
        }
        return "投票 あと" + Math.max(0, (meeting.discussionTicks() + meeting.votingTicks() - SPLASH - meetingTime()) / 20 + 1) + "秒"
                + (myVoteSent ? "（投票済み）" : "");
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void onClose() {
        if (result != null && resultAge >= RESULT_SHOW + EJECT_LEN) {
            ClientGame.currentMeeting = null;
        }
        Minecraft.getInstance().setScreen(null);
    }
}
