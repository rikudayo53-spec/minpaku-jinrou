package com.minpaku.game;

import com.minpaku.entity.BodyEntity;
import com.minpaku.entity.BotEntity;
import com.minpaku.network.Packets;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

/**
 * ボット1人ぶんの「考え」。
 *   ふだん … 自分のタスクを回る。終わったら家の中をうろうろ
 *   人狼   … ひとりでいる人を見つけたら背後に回ってキル。ときどき妨害
 *   村人側 … 死体を見たら通報。停電・ガス漏れは近い人が直しに行く
 *   会議   … 見たこと・占い結果などをチャットで話して、投票する
 */
public class BotBrain {
    private enum Mode { TASK, WANDER, FIX, HUNT, FLEE, BELL }

    private final GameManager gm;
    private final PlayerState ps;
    private final Random r;

    /** 疑いの強さ（大きいほど人狼っぽいと思っている） */
    private final Map<UUID, Integer> suspicion = new HashMap<>();
    /** 確信していること（true = 人狼、false = 白） */
    private final Map<UUID, Boolean> known = new HashMap<>();
    @Nullable
    private UUID witnessKiller;
    private String witnessVictim = "";

    private Mode mode = Mode.TASK;
    @Nullable
    private BlockPos goal;
    @Nullable
    private PlayerState.TaskEntry currentTask;
    private int work;
    private int idle;
    private int sinceRepath;
    private int stuckTicks;
    private Vec3 lastPos = Vec3.ZERO;
    @Nullable
    private UUID huntTarget;
    private int huntTicks;
    private MarkerType fixType;

    // 会議
    private int speakAt = -1;
    private int voteAt = -1;
    private boolean spoke;
    private boolean voted;
    private int meetingReason;
    private boolean iCalled;
    private String bodyName = "";
    @Nullable
    private PlayerState mediumTarget;
    private boolean mediumWolf;
    @Nullable
    private UUID seerSaid;

    /** 強さ 1（よわい）〜 3（つよい） */
    private final int level;

    BotBrain(GameManager gm, PlayerState ps) {
        this.gm = gm;
        this.ps = ps;
        this.r = gm.random;
        this.level = gm.settings().get("bot_level");
    }

    private boolean villageSide() {
        return ps.role.team == Role.Team.VILLAGE;
    }

    private boolean isMate(PlayerState o) {
        return ps.role.knowsWolves() && o.role.knowsWolves();
    }

    // ================================================================= ふだんの動き

    /** さっきまでいた部屋（会議で話す用） */
    private String lastRoom = "";

    void tickPlaying(BotEntity bot) {
        long t = gm.ticks + (ps.id.hashCode() & 7);
        if (t % 20 == 0) {
            String room = gm.map().roomAt(bot.blockPosition());
            if (!room.isEmpty()) {
                lastRoom = room;
            }
        }

        // 人狼：狩りの最中は毎tickキルを試す
        if (mode == Mode.HUNT) {
            tickHunt(bot);
            return;
        }

        if (t % 10 == 0 && lookForBodies(bot)) {
            return;
        }
        if (t % 20 == 0) {
            useAbilities(bot);
            if (ps.role.canKill()) {
                considerHunt(bot);
                considerSabotage();
            }
            considerFix(bot);
            considerBell();
        }
        if (mode == Mode.HUNT) {
            return;
        }
        move(bot);
    }

    /** 死体を見つけたら通報（人狼はたまにしか通報しない） */
    private boolean lookForBodies(BotEntity bot) {
        for (BodyEntity body : new ArrayList<>(gm.bodies)) {
            double sight = (gm.blackout ? 3 : 5) + level * (gm.blackout ? 1 : 2);
            if (!body.isAlive() || bot.distanceTo(body) > sight || !bot.hasLineOfSight(body)) {
                continue;
            }
            if (ps.role.canKill() && r.nextFloat() > 0.05f) {
                continue;
            }
            bot.getNavigation().stop();
            gm.botReport(ps, body);
            return true;
        }
        return false;
    }

    private void useAbilities(BotEntity bot) {
        if (ps.role == Role.SEER && ps.seerLeft > 0) {
            PlayerState near = nearestOther(bot, 3.5);
            if (near != null && !known.containsKey(near.id)) {
                ps.seerLeft--;
                if (gm.divineEffects(ps, near)) {
                    return;
                }
                boolean wolf = near.role.isWolf();
                known.put(near.id, wolf);
                if (wolf) {
                    suspicion.merge(near.id, 200, Integer::sum);
                }
            }
        }
        if (ps.role == Role.KNIGHT && ps.knightLeft > 0 && r.nextFloat() < 0.3f) {
            PlayerState near = nearestOther(bot, 3.5);
            if (near != null && !near.protectedByKnight && !Boolean.TRUE.equals(known.get(near.id))) {
                ps.knightLeft--;
                near.protectedByKnight = true;
            }
        }
    }

    @Nullable
    private PlayerState nearestOther(BotEntity bot, double range) {
        PlayerState best = null;
        double bestD = range;
        for (PlayerState o : gm.living()) {
            if (o == ps) {
                continue;
            }
            LivingEntity e = gm.entityOf(o);
            if (e == null) {
                continue;
            }
            double d = bot.distanceTo(e);
            if (d <= bestD) {
                bestD = d;
                best = o;
            }
        }
        return best;
    }

    // ----------------------------------------------------------------- 人狼

    private void considerHunt(BotEntity bot) {
        if (ps.killCooldown > 0 || gm.ticks < 200 || r.nextFloat() > 0.25f + 0.25f * level) {
            return;
        }
        PlayerState best = null;
        double bestD = 22;
        for (PlayerState o : gm.living()) {
            if (o == ps || o.role.isWolf() || isMate(o)) {
                continue;
            }
            LivingEntity te = gm.entityOf(o);
            if (te == null || bot.distanceTo(te) > bestD) {
                continue;
            }
            if (!isolated(o, te)) {
                continue;
            }
            bestD = bot.distanceTo(te);
            best = o;
        }
        if (best != null) {
            mode = Mode.HUNT;
            huntTarget = best.id;
            huntTicks = 0;
        }
    }

    /** まわりに他の人がいない（＝見られない）か */
    private boolean isolated(PlayerState target, LivingEntity te) {
        for (PlayerState o : gm.living()) {
            if (o == ps || o == target || isMate(o)) {
                continue;
            }
            LivingEntity e = gm.entityOf(o);
            if (e != null && e.distanceTo(te) < 8) {
                return false;
            }
        }
        return true;
    }

    private void tickHunt(BotEntity bot) {
        PlayerState target = huntTarget == null ? null : gm.players.get(huntTarget);
        LivingEntity te = target == null ? null : gm.entityOf(target);
        if (target == null || te == null || !target.alive || ++huntTicks > 400 || ps.killCooldown > 0) {
            mode = Mode.TASK;
            goal = null;
            return;
        }
        if (huntTicks % 20 == 0 && !isolated(target, te)) {
            // 人が来たらあきらめる
            mode = Mode.TASK;
            goal = null;
            return;
        }
        if (gm.attemptKill(ps, bot, target, te)) {
            // キルしたら（失敗しても）その場を離れる
            mode = Mode.FLEE;
            goal = randomSpot();
            idle = 0;
            return;
        }
        // 背中側に回りこむ
        Vec3 look = Vec3.directionFromRotation(0, te.getYHeadRot());
        Vec3 behind = te.position().subtract(look.scale(1.1));
        if (huntTicks % 5 == 1) {
            bot.getNavigation().moveTo(behind.x, behind.y, behind.z, 1.25);
        }
        bot.getLookControl().setLookAt(te);
    }

    private void considerSabotage() {
        if (gm.sabotageCooldown > 0 || gm.blackout || gm.gasTicks >= 0 || r.nextFloat() > 0.08f) {
            return;
        }
        gm.sabotageBy(ps, null, r.nextFloat() < 0.6f ? 0 : 1);
    }

    // ----------------------------------------------------------------- 妨害を直す

    private void considerFix(BotEntity bot) {
        if (!villageSide()) {
            return;
        }
        MarkerType need = null;
        if (gm.blackout && nearestFixer(MarkerType.BREAKER, null) == ps) {
            need = MarkerType.BREAKER;
        } else if (gm.gasTicks >= 0) {
            PlayerState valve = gm.valveFixed ? null : nearestFixer(MarkerType.GAS_VALVE, null);
            PlayerState boiler = gm.boilerFixed ? null : nearestFixer(MarkerType.BOILER, valve);
            if (valve == ps) {
                need = MarkerType.GAS_VALVE;
            } else if (boiler == ps) {
                need = MarkerType.BOILER;
            }
        }
        if (need == null) {
            if (mode == Mode.FIX) {
                mode = Mode.TASK;
                goal = null;
            }
            return;
        }
        List<BlockPos> spots = gm.map().get(need);
        if (spots.isEmpty()) {
            return;
        }
        if (mode != Mode.FIX || fixType != need) {
            mode = Mode.FIX;
            fixType = need;
            goal = spots.get(0);
            work = 0;
            sinceRepath = 999;
        }
    }

    @Nullable
    private PlayerState nearestFixer(MarkerType type, @Nullable PlayerState except) {
        List<BlockPos> spots = gm.map().get(type);
        if (spots.isEmpty()) {
            return null;
        }
        Vec3 c = Vec3.atCenterOf(spots.get(0));
        PlayerState best = null;
        double bestD = Double.MAX_VALUE;
        for (PlayerState o : gm.living()) {
            if (!o.bot || o == except || o.role.team != Role.Team.VILLAGE) {
                continue;
            }
            LivingEntity e = gm.entityOf(o);
            if (e == null) {
                continue;
            }
            double d = e.position().distanceToSqr(c);
            if (d < bestD) {
                bestD = d;
                best = o;
            }
        }
        return best;
    }

    // ----------------------------------------------------------------- 呼び鈴

    private void considerBell() {
        if (!villageSide() || ps.emergencyLeft <= 0 || mode == Mode.BELL || mode == Mode.FIX || gm.gasTicks >= 0
                || gm.ticks - gm.lastMeetingEnd < 900) {
            return;
        }
        // キルを目撃したときだけ呼び鈴を鳴らしに行く（占い結果は次の会議で話す）
        PlayerState killer = witnessKiller == null ? null : gm.players.get(witnessKiller);
        boolean sure = killer != null && killer.alive;
        if (sure) {
            List<BlockPos> bells = gm.map().get(MarkerType.BELL);
            if (!bells.isEmpty()) {
                mode = Mode.BELL;
                goal = bells.get(0);
                sinceRepath = 999;
            }
        }
    }

    // ----------------------------------------------------------------- 移動とタスク

    private void move(BotEntity bot) {
        if (goal == null) {
            chooseGoal();
            if (goal == null) {
                return;
            }
        }
        Vec3 target = Vec3.atBottomCenterOf(goal);
        double dx = bot.getX() - target.x;
        double dz = bot.getZ() - target.z;
        boolean arrived = dx * dx + dz * dz <= 2.4 * 2.4 && Math.abs(bot.getY() - target.y) < 2.5;

        if (!arrived) {
            work = 0;
            if (++sinceRepath > 40 || (bot.getNavigation().isDone() && sinceRepath > 10)) {
                sinceRepath = 0;
                bot.getNavigation().moveTo(target.x, target.y, target.z, mode == Mode.FLEE ? 1.2 : 1.0);
            }
            // 動けなくなったら目標を変える
            if (bot.position().distanceToSqr(lastPos) < 0.01) {
                if (++stuckTicks > 120) {
                    stuckTicks = 0;
                    goal = null;
                    currentTask = null;
                    if (mode == Mode.FLEE || mode == Mode.BELL) {
                        mode = Mode.TASK;
                    }
                }
            } else {
                stuckTicks = 0;
            }
            lastPos = bot.position();
            return;
        }

        bot.getNavigation().stop();
        bot.getLookControl().setLookAt(target.x, target.y + 0.5, target.z);
        work++;
        switch (mode) {
            case TASK -> doTask(bot);
            case FIX -> {
                if (work % 15 == 0) {
                    bot.swing(InteractionHand.MAIN_HAND, true);
                    gm.workFx(goal, fixType, 15f / 50f);
                }
                if (work >= 50) {
                    if (fixType == MarkerType.BREAKER) {
                        gm.fixBlackout(null);
                    } else {
                        gm.fixGas(null, fixType);
                    }
                    mode = Mode.TASK;
                    goal = null;
                }
            }
            case BELL -> {
                if (work >= 10) {
                    mode = Mode.TASK;
                    goal = null;
                    gm.botEmergency(ps);
                }
            }
            default -> {
                // うろうろ・逃げる：しばらく立ち止まってから次へ
                if (work >= idle) {
                    mode = Mode.TASK;
                    goal = null;
                }
            }
        }
    }

    private void doTask(BotEntity bot) {
        if (currentTask == null || currentTask.done) {
            goal = null;
            return;
        }
        MarkerType type = currentTask.type;
        int need = switch (type.use) {
            case HOLD -> type.amount;
            case CLICK -> type.amount * 14;
            case CARRY, PATROL -> 24;
            default -> 90;
        };
        if (work % 14 == 0) {
            bot.swing(InteractionHand.MAIN_HAND, true);
            if (!type.miniGame()) {
                gm.workFx(currentTask.pos, type, 14f / need);
            } else {
                gm.animate(currentTask.pos, 14f / need);
            }
        }
        if (work >= need) {
            if (type.use == MarkerType.Use.CARRY || type.use == MarkerType.Use.PATROL) {
                // 運ぶ・見回り：次の場所へ（タスクの場所が変わる）
                if (!gm.botAdvance(ps, currentTask)) {
                    goal = currentTask.pos;
                    work = 0;
                    sinceRepath = 999;
                    return;
                }
            } else {
                gm.botCompleteTask(ps, currentTask);
            }
            currentTask = null;
            goal = null;
        }
    }

    private void chooseGoal() {
        work = 0;
        sinceRepath = 999;
        // 残っているタスクのうち、ランダムに1つ
        List<PlayerState.TaskEntry> left = new ArrayList<>();
        for (PlayerState.TaskEntry t : ps.tasks) {
            if (!t.done) {
                left.add(t);
            }
        }
        if (!left.isEmpty() && r.nextFloat() < 0.85f) {
            currentTask = left.get(r.nextInt(left.size()));
            goal = currentTask.pos;
            mode = Mode.TASK;
            return;
        }
        mode = Mode.WANDER;
        goal = randomSpot();
        idle = 30 + r.nextInt(90);
    }

    @Nullable
    private BlockPos randomSpot() {
        List<BlockPos> spots = new ArrayList<>();
        for (MarkerType t : MarkerType.values()) {
            if (t != MarkerType.SPAWN) {
                spots.addAll(gm.map().get(t));
            }
        }
        return spots.isEmpty() ? null : spots.get(r.nextInt(spots.size()));
    }

    // ================================================================= 見た・聞いた

    void sawKill(PlayerState killer, PlayerState victim) {
        if (killer == ps) {
            return;
        }
        if (isMate(killer)) {
            return;
        }
        witnessKiller = killer.id;
        witnessVictim = victim.name;
        known.put(killer.id, true);
        suspicion.merge(killer.id, 200, Integer::sum);
    }

    /** 誰かの発言を聞いた（delta > 0 で疑う、< 0 で信じる） */
    void heard(PlayerState speaker, PlayerState target, int delta) {
        if (target == ps || speaker == ps) {
            return;
        }
        // 自分が白と知っている人への疑いは聞き流す
        if (delta > 0 && Boolean.FALSE.equals(known.get(target.id))) {
            return;
        }
        // 疑ってきた人が自分を疑っているなら、その人を少し疑い返す
        suspicion.merge(target.id, delta, Integer::sum);
    }

    void mediumResult(PlayerState ejected, boolean wolf) {
        mediumTarget = ejected;
        mediumWolf = wolf;
    }

    void onEjected(PlayerState e) {
        suspicion.remove(e.id);
        if (e.id.equals(witnessKiller)) {
            witnessKiller = null;
        }
    }

    void onMeetingEnd() {
        mode = Mode.TASK;
        goal = null;
        currentTask = null;
        huntTarget = null;
        // 疑いは少しずつ薄れる
        suspicion.replaceAll((k, v) -> v / 2);
    }

    // ================================================================= 会議

    void onMeetingStart(int reason, PlayerState caller, @Nullable UUID bodyOwner) {
        mode = Mode.TASK;
        goal = null;
        huntTarget = null;
        spoke = false;
        voted = false;
        meetingReason = reason;
        iCalled = caller == ps;
        PlayerState body = bodyOwner == null ? null : gm.players.get(bodyOwner);
        bodyName = body == null ? "" : body.name;
        int disc = gm.discussionTicks;
        int vote = gm.votingTicks;
        int speakWindow = Math.max(40, Math.min(400, disc + vote / 3 - 40));
        speakAt = 50 + (iCalled ? 0 : r.nextInt(speakWindow));
        voteAt = disc + 30 + r.nextInt(Math.max(20, Math.min(240, vote - 80)));
    }

    void tickMeeting() {
        if (!ps.alive) {
            return;
        }
        if (!spoke && gm.meetingTicks >= speakAt) {
            spoke = true;
            speak();
        }
        if (!voted && gm.votingOpen() && gm.meetingTicks >= voteAt) {
            voted = true;
            vote();
        }
    }

    private String nameOf(UUID id) {
        PlayerState o = gm.players.get(id);
        return o == null ? "？" : o.name;
    }

    private void speak() {
        String line;
        PlayerState accused = null;
        int strength = 0;

        PlayerState killer = witnessKiller == null ? null : gm.players.get(witnessKiller);
        if (killer != null && !killer.alive) {
            killer = null;
        }
        UUID seerWolf = null;
        UUID seerWhite = null;
        if (ps.role == Role.SEER) {
            for (Map.Entry<UUID, Boolean> e : known.entrySet()) {
                PlayerState o = gm.players.get(e.getKey());
                if (o == null || !o.alive || e.getKey().equals(seerSaid)) {
                    continue;
                }
                if (e.getValue()) {
                    seerWolf = e.getKey();
                } else {
                    seerWhite = e.getKey();
                }
            }
        }

        if (iCalled && meetingReason == 1) {
            line = bodyName + " の死体を見つけた！";
            if (killer != null) {
                line += " " + killer.name + " が襲うのを見たよ！";
                accused = killer;
                strength = 90;
            } else if (ps.role.canKill()) {
                PlayerState scape = randomOther(true);
                if (scape != null && r.nextBoolean()) {
                    line += " 近くに " + scape.name + " がいた気がする…";
                    accused = scape;
                    strength = 30;
                }
            }
        } else if (iCalled) {
            if (killer != null) {
                line = "呼び鈴を鳴らしたよ。" + killer.name + " が " + witnessVictim + " を襲ってた！";
                accused = killer;
                strength = 90;
            } else {
                line = "呼び鈴を鳴らしたよ。何か見た人いる？";
            }
        } else if (killer != null) {
            line = killer.name + " が " + witnessVictim + " を襲うのを見た！ " + killer.name + " に入れよう！";
            accused = killer;
            strength = 90;
        } else if (seerWolf != null) {
            line = "占い師です。" + nameOf(seerWolf) + " を占ったら人狼だった！";
            accused = gm.players.get(seerWolf);
            strength = 80;
            seerSaid = seerWolf;
        } else if (mediumTarget != null) {
            line = "霊媒師です。" + mediumTarget.name + " は人狼" + (mediumWolf ? "だった。" : "じゃなかった…");
            mediumTarget = null;
        } else if (seerWhite != null && r.nextBoolean()) {
            line = "占い師です。" + nameOf(seerWhite) + " は白だったよ。";
            PlayerState w = gm.players.get(seerWhite);
            if (w != null) {
                for (BotBrain b : gm.brains.values()) {
                    if (b != this) {
                        b.heard(ps, w, -40);
                    }
                }
            }
            seerSaid = seerWhite;
        } else if (ps.role.canKill() || ps.role == Role.MADMAN) {
            float f = r.nextFloat();
            PlayerState scape = randomOther(true);
            if (f < 0.45f && scape != null) {
                line = scape.name + " がずっとうろうろしてて怪しいと思う。";
                accused = scape;
                strength = 25;
            } else {
                line = filler();
            }
        } else {
            PlayerState top = topSuspect(40);
            if (top != null) {
                line = top.name + " が怪しいと思う。";
                accused = top;
                strength = 20;
            } else {
                line = filler();
            }
        }
        gm.botSay(ps, line);
        if (accused != null && strength > 0) {
            for (BotBrain b : gm.brains.values()) {
                if (b != this) {
                    b.heard(ps, accused, strength);
                }
            }
        }
    }

    /** 特に情報がないときのひとこと（同じ会議で同じセリフはなるべく使わない） */
    private String filler() {
        String room = lastRoom.isEmpty() ? "家の中" : lastRoom;
        PlayerState other = randomOther(false);
        String on = other == null ? "だれか" : other.name;
        String[] lines = {
                room + "でタスクしてたから、何も見てないよ。",
                room + "にいたけど、変わったことはなかった。",
                "スキップでいいと思う。情報が少なすぎる。",
                "誰か怪しい人いる？",
                "まだ分からないね…。",
                "さっきまで " + on + " と一緒にいたよ。",
                on + " ってどこにいた？",
                "ずっと " + room + " のあたりを回ってた。",
                "停電のとき、誰がどこにいたか覚えてる？",
                "今回は様子見かな。",
                "タスク、あと少しで終わるよ。",
                on + " は落ち着いてるし、白っぽい気がする。"
        };
        for (int i = 0; i < 12; i++) {
            String l = lines[r.nextInt(lines.length)];
            if (gm.usedLines.add(l)) {
                return l;
            }
        }
        return lines[r.nextInt(lines.length)];
    }

    @Nullable
    private PlayerState randomOther(boolean nonWolf) {
        List<PlayerState> c = new ArrayList<>();
        for (PlayerState o : gm.living()) {
            if (o != ps && (!nonWolf || !(o.role.isWolf() || isMate(o)))) {
                c.add(o);
            }
        }
        return c.isEmpty() ? null : c.get(r.nextInt(c.size()));
    }

    @Nullable
    private PlayerState topSuspect(int threshold) {
        PlayerState best = null;
        int bestV = threshold - 1;
        for (PlayerState o : gm.living()) {
            if (o == ps || (ps.role.knowsWolves() && isMate(o))) {
                continue;
            }
            if (Boolean.FALSE.equals(known.get(o.id))) {
                continue;
            }
            int v = suspicion.getOrDefault(o.id, 0);
            if (Boolean.TRUE.equals(known.get(o.id))) {
                v += 300;
            }
            if (v > bestV) {
                bestV = v;
                best = o;
            }
        }
        return best;
    }

    private void vote() {
        PlayerState target;
        if (!villageSide()) {
            // 人狼側：みんなが疑っている村人に乗っかる。いなければ適当に
            target = topSuspect(30);
            if (target != null && (target.role.isWolf() || isMate(target))) {
                target = null;
            }
            if (target == null && r.nextFloat() < 0.5f) {
                target = randomOther(true);
            }
        } else {
            target = topSuspect(level >= 3 ? 35 : 45);
            if (target == null && r.nextFloat() < (level == 1 ? 0.45f : 0.2f)) {
                target = randomOther(false);
            }
        }
        gm.castVote(ps, target == null ? Packets.NONE : target.id);
    }
}
