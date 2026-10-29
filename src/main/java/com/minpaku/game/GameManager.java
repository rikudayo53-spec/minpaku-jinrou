package com.minpaku.game;

import com.minpaku.block.MarkerBlock;
import com.minpaku.entity.BodyEntity;
import com.minpaku.entity.BotEntity;
import com.minpaku.network.Packets;
import com.minpaku.registry.ModEntities;
import com.minpaku.registry.ModItems;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

/**
 * 民泊人狼のゲーム進行（サーバー側）。
 * 参加者は「人間のプレイヤー」と「ボット（BotEntity）」のどちらか。
 */
public final class GameManager {
    public enum Phase { LOBBY, PLAYING, MEETING, ENDED }

    private static GameManager instance;

    public static GameManager get(MinecraftServer server) {
        if (instance == null || instance.server != server) {
            instance = new GameManager(server);
        }
        return instance;
    }

    @Nullable
    public static GameManager current() {
        return instance;
    }

    public static void shutdown() {
        instance = null;
    }

    private static final int MEETING_RESULT_TICKS = 70;   // 投票結果を見せる時間
    private static final int EJECT_TICKS = 160;           // 追放演出の時間
    private static final int END_TICKS = 240;             // 結果画面からロビーに戻るまで
    private static final int FIRST_KILL_COOLDOWN = 200;

    private static final String[] BOT_NAMES = {
            "たろう", "はなこ", "けんた", "さくら", "ゆうき", "みお", "そうた", "あおい", "りく", "ひなた", "かいと"
    };

    /** 自動テスト用：勝敗判定を止める */
    public static boolean selfTestNoWin;

    final MinecraftServer server;
    final Random random = new Random();
    private Phase phase = Phase.LOBBY;
    ServerLevel level;
    @Nullable
    private UUID host;
    final Map<UUID, PlayerState> players = new LinkedHashMap<>();
    final Map<UUID, BotEntity> bots = new HashMap<>();
    final Map<UUID, BotBrain> brains = new HashMap<>();
    final List<BodyEntity> bodies = new ArrayList<>();
    long ticks;
    long lastMeetingEnd;
    /** 会議で使われたセリフ（ボットが同じことを言わないように） */
    final java.util.Set<String> usedLines = new java.util.HashSet<>();

    // 妨害
    boolean blackout;
    int gasTicks = -1;
    private int gasMax = 1;
    boolean valveFixed;
    boolean boilerFixed;
    int sabotageCooldown;
    private ServerBossEvent gasBar;

    // 「見ていると進む」作業（人間用）
    private record Active(BlockPos pos, MarkerType type, int need) {
    }

    private final Map<UUID, Active> active = new HashMap<>();
    private final Map<UUID, Integer> activeProgress = new HashMap<>();

    // 会議
    int meetingTicks;
    int discussionTicks;
    int votingTicks;
    final Map<UUID, UUID> votes = new LinkedHashMap<>();
    boolean resultShown;
    private int postTimer;
    @Nullable
    private UUID ejected;

    private int endTimer;

    private GameManager(MinecraftServer server) {
        this.server = server;
        this.level = server.overworld();
    }

    // ================================================================= 状態の問い合わせ

    public Phase phase() {
        return phase;
    }

    public boolean running() {
        return phase == Phase.PLAYING || phase == Phase.MEETING;
    }

    @Nullable
    public PlayerState state(ServerPlayer p) {
        return players.get(p.getUUID());
    }

    public boolean isParticipant(ServerPlayer p) {
        return running() && players.containsKey(p.getUUID()) && !players.get(p.getUUID()).left;
    }

    public boolean isBot(UUID id) {
        return bots.containsKey(id);
    }

    GameSettings settings() {
        return MapData.get(level).settings();
    }

    MapData map() {
        return MapData.get(level);
    }

    @Nullable
    private ServerPlayer player(UUID id) {
        return server.getPlayerList().getPlayer(id);
    }

    /** 参加者の体（人間ならプレイヤー、ボットならボット） */
    @Nullable
    LivingEntity entityOf(PlayerState ps) {
        if (ps.bot) {
            BotEntity b = bots.get(ps.id);
            return b != null && b.isAlive() ? b : null;
        }
        return player(ps.id);
    }

    private List<ServerPlayer> online() {
        List<ServerPlayer> out = new ArrayList<>();
        for (PlayerState ps : players.values()) {
            if (ps.bot) {
                continue;
            }
            ServerPlayer p = player(ps.id);
            if (p != null && !ps.left) {
                out.add(p);
            }
        }
        return out;
    }

    private void send(ServerPlayer p, CustomPacketPayload payload) {
        if (ServerPlayNetworking.canSend(p, payload.type())) {
            ServerPlayNetworking.send(p, payload);
        }
    }

    private void sendAll(CustomPacketPayload payload) {
        for (ServerPlayer p : online()) {
            send(p, payload);
        }
    }

    private static void actionBar(@Nullable ServerPlayer p, String text, ChatFormatting color) {
        if (p != null) {
            p.displayClientMessage(Component.literal(text).withStyle(color), true);
        }
    }

    private boolean canManage(ServerPlayer p) {
        return p.hasPermissions(2) || server.isSingleplayerOwner(p.getGameProfile());
    }

    /** ボットの発言をチャットに流す */
    void botSay(PlayerState ps, String text) {
        if (!settings().flag("bot_talk")) {
            return;
        }
        sendAll(new Packets.Talk(ps.id, ps.name, text));
        Component msg = Component.literal("<" + ps.name + "> ").withStyle(ChatFormatting.AQUA)
                .append(Component.literal(text).withStyle(ChatFormatting.WHITE));
        for (ServerPlayer p : online()) {
            p.sendSystemMessage(msg);
        }
    }

    List<PlayerState> living() {
        List<PlayerState> out = new ArrayList<>();
        for (PlayerState ps : players.values()) {
            if (ps.alive && !ps.left) {
                out.add(ps);
            }
        }
        return out;
    }

    // ================================================================= 設定

    public void sendSettings(ServerPlayer p) {
        Map<Integer, Integer> counts = new HashMap<>();
        for (MarkerType t : MarkerType.values()) {
            counts.put(t.ordinal(), map().count(t));
        }
        send(p, new Packets.Settings(settings(), canManage(p), running(), counts));
    }

    public void setSetting(ServerPlayer p, String id, int value) {
        if (!canManage(p)) {
            actionBar(p, "設定を変えられるのはOPだけです", ChatFormatting.RED);
            return;
        }
        if (settings().set(id, value)) {
            map().setDirty();
        }
        sendSettings(p);
    }

    public void control(ServerPlayer p, int action) {
        if (!canManage(p)) {
            actionBar(p, "ゲームを操作できるのはOPだけです", ChatFormatting.RED);
            return;
        }
        if (action == 0) {
            start(p);
        } else if (action == 2) {
            buildMap(p);
        } else {
            stop(p);
        }
    }

    /** 設定で選んだ基本マップを目の前に建てる */
    public void buildMap(ServerPlayer p) {
        buildMap(p, Plans.byIndex(settings().get("map")));
    }

    public void buildMap(ServerPlayer p, MapPlan plan) {
        if (running()) {
            p.sendSystemMessage(Component.literal("ゲーム中はマップを建てられません").withStyle(ChatFormatting.RED));
            return;
        }
        p.sendSystemMessage(Component.literal("「" + plan.name + "」（" + plan.players + "）を建てています…").withStyle(ChatFormatting.GOLD));
        exitAllLobbyVents();
        BlockPos spawn = MapBuilder.build(p, plan);
        dropClearTimer = 60;
        for (ServerPlayer o : level.players()) {
            sendBounds(o);
            sendRooms(o);
        }
        p.teleportTo(p.serverLevel(), spawn.getX() + 0.5, spawn.getY(), spawn.getZ() + 0.5, p.getYRot(), 0f);
        p.sendSystemMessage(Component.literal("完成！ 玄関がスタート地点です。管理人の鍵でゲームを始めよう。")
                .withStyle(ChatFormatting.GREEN));
        sendSettings(p);
    }

    // ================================================================= 開始・終了

    public void start(ServerPlayer starter) {
        if (phase != Phase.LOBBY) {
            starter.sendSystemMessage(Component.literal("すでにゲーム中です").withStyle(ChatFormatting.RED));
            return;
        }
        level = starter.serverLevel();
        host = starter.getUUID();
        exitAllLobbyVents();
        GameSettings s = settings();

        List<ServerPlayer> humans = new ArrayList<>();
        for (ServerPlayer p : level.players()) {
            if (!p.isSpectator()) {
                humans.add(p);
            }
        }
        if (humans.isEmpty()) {
            return;
        }

        players.clear();
        bots.clear();
        brains.clear();
        bodies.clear();
        meetingCount = 0;
        active.clear();
        activeProgress.clear();
        blackout = false;
        gasTicks = -1;
        sabotageCooldown = 300;

        // 参加者の一覧（人間＋足りないぶんのボット）
        List<PlayerState> all = new ArrayList<>();
        for (ServerPlayer p : humans) {
            PlayerState ps = new PlayerState(p.getUUID(), p.getGameProfile().getName());
            ps.previousMode = p.gameMode.getGameModeForPlayer();
            all.add(ps);
        }
        int botCount = s.flag("bots_enabled") ? Math.max(0, s.get("bot_fill") - humans.size()) : 0;
        for (int i = 0; i < botCount && i < BOT_NAMES.length; i++) {
            PlayerState ps = new PlayerState(UUID.randomUUID(), BOT_NAMES[i]);
            ps.bot = true;
            all.add(ps);
        }
        Collections.shuffle(all, random);

        // 役職を配る（人狼を先頭に。特別な役職は「人数 - 1」まで）
        List<Role> deck = new ArrayList<>();
        for (Role r : Role.special()) {
            for (int i = 0; i < s.roleCount(r); i++) {
                deck.add(r);
            }
        }
        deck.sort(Comparator.comparingInt(r -> r.canKill() ? 0 : 1));
        int limit = Math.max(1, all.size() - 1);
        for (int i = 0; i < all.size(); i++) {
            PlayerState ps = all.get(i);
            ps.role = i < deck.size() && i < limit ? deck.get(i) : Role.VILLAGER;
            ps.killCooldown = FIRST_KILL_COOLDOWN;
            ps.seerLeft = s.get("seer_uses");
            ps.knightLeft = s.get("knight_uses");
            ps.itamaeLeft = s.get("itamae_uses");
            ps.emergencyLeft = s.get("emergency_uses");
            ps.guessLeft = s.get("guesser_shots");
            ps.buttonLeft = s.get("buttoner_uses");
            ps.sheriffLeft = s.get("sheriff_shots");
        }
        // 表示の順番は人間 → ボット
        all.sort(Comparator.comparing(ps -> ps.bot));
        for (PlayerState ps : all) {
            players.put(ps.id, ps);
        }

        // タスクを配る
        List<BlockPos> taskSpots = new ArrayList<>();
        Map<BlockPos, MarkerType> taskTypes = new HashMap<>();
        for (MarkerType t : MarkerType.values()) {
            if (t.isTask()) {
                for (BlockPos pos : map().get(t)) {
                    taskSpots.add(pos);
                    taskTypes.put(pos, t);
                }
            }
        }
        int want = s.get("tasks");
        for (PlayerState ps : players.values()) {
            List<BlockPos> pick = new ArrayList<>(taskSpots);
            Collections.shuffle(pick, random);
            for (int k = 0; k < Math.min(want, pick.size()); k++) {
                ps.tasks.add(new PlayerState.TaskEntry(pick.get(k), taskTypes.get(pick.get(k))));
            }
        }

        // スタート地点へ。ボットを出す
        List<BlockPos> spawns = map().get(MarkerType.SPAWN);
        Vec3 center = spawns.isEmpty() ? starter.position() : Vec3.atBottomCenterOf(spawns.get(0)).add(0, 0.1, 0);
        int n = 0;
        for (PlayerState ps : players.values()) {
            double a = (n / (double) players.size()) * Math.PI * 2;
            double r = 1.6;
            double x = center.x + Math.cos(a) * r;
            double z = center.z + Math.sin(a) * r;
            n++;
            if (ps.bot) {
                BotEntity bot = ModEntities.BOT.create(level);
                if (bot == null) {
                    continue;
                }
                bot.setUUID(ps.id);
                bot.moveTo(x, center.y, z, random.nextFloat() * 360f, 0);
                bot.setCustomName(Component.literal(ps.name));
                level.addFreshEntity(bot);
                bots.put(ps.id, bot);
                brains.put(ps.id, new BotBrain(this, ps));
            } else {
                ServerPlayer p = player(ps.id);
                if (p != null) {
                    if (!spawns.isEmpty()) {
                        p.teleportTo(level, x, center.y, z, p.getYRot(), 0);
                    }
                    preparePlayer(p, ps);
                }
            }
        }

        phase = Phase.PLAYING;
        ticks = 0;
        lobbyVents.clear();
        // 祠を元どおりに（前のゲームで壊れていたら直す）
        for (BlockPos hp : map().get(MarkerType.HOKORA)) {
            if (level.getBlockEntity(hp) instanceof com.minpaku.block.MarkerBlockEntity be) {
                be.work = 0;
                be.setChanged();
                level.sendBlockUpdated(hp, level.getBlockState(hp), level.getBlockState(hp), 3);
            }
        }
        // 祠の悪神：起きるかどうかと、いつ起きるか
        int ev = s.get("evil_god");
        boolean happen = map().count(MarkerType.HOKORA) > 0 && (ev == 3 || (ev == 2 && random.nextBoolean()));
        evilGodAt = happen ? 20L * (70 + random.nextInt(120)) : -1;

        for (ServerPlayer p : humans) {
            PlayerState ps = players.get(p.getUUID());
            send(p, new Packets.Reveal(ps.role.id, mates(ps), players.size()));
        }
        syncAll();
        sendRoles();
        for (ServerPlayer p : humans) {
            sendRooms(p);
            sendBounds(p);
        }
        if (botCount > 0) {
            starter.sendSystemMessage(Component.literal("ボット " + botCount + " 人が参加しています。会議ではチャットで名前を出すと、ボットが参考にします。")
                    .withStyle(ChatFormatting.AQUA));
        }
        if (map().count(MarkerType.BELL) == 0) {
            starter.sendSystemMessage(Component.literal("注意: 呼び鈴（緊急ボタン）が置かれていません").withStyle(ChatFormatting.YELLOW));
        }
        if (taskSpots.isEmpty()) {
            starter.sendSystemMessage(Component.literal("注意: タスクのブロックが置かれていません").withStyle(ChatFormatting.YELLOW));
        }
    }

    private void preparePlayer(ServerPlayer p, PlayerState ps) {
        p.setGameMode(GameType.ADVENTURE);
        p.getInventory().clearContent();
        p.removeAllEffects();
        p.setHealth(p.getMaxHealth());
        p.getFoodData().setFoodLevel(20);
        p.addEffect(new MobEffectInstance(MobEffects.SATURATION, -1, 0, false, false));
        giveRoleItem(p, ps);
    }

    private void giveRoleItem(ServerPlayer p, PlayerState ps) {
        if (ps.role == Role.SEER) {
            p.getInventory().setItem(0, new ItemStack(ModItems.CRYSTAL));
        } else if (ps.role == Role.KNIGHT) {
            p.getInventory().setItem(0, new ItemStack(ModItems.CHARM));
        } else if (ps.role.canSabotage()) {
            p.getInventory().setItem(0, new ItemStack(ModItems.TALISMAN));
        } else if (ps.role == Role.DETECTIVE) {
            p.getInventory().setItem(0, new ItemStack(ModItems.LENS));
        } else if (ps.role == Role.SHERIFF) {
            p.getInventory().setItem(0, new ItemStack(ModItems.GUN));
        } else if (ps.role == Role.NICE_BUTTONER) {
            p.getInventory().setItem(0, new ItemStack(ModItems.HANDBELL));
        } else if (ps.role == Role.ITAMAE) {
            p.getInventory().setItem(0, new ItemStack(ModItems.ONIGIRI));
        }
        // 能力のアイテムをすぐ手に持つ（いちばん左のスロットを選ぶ）
        p.getInventory().selected = 0;
        p.connection.send(new net.minecraft.network.protocol.game.ClientboundSetCarriedItemPacket(0));
    }

    List<Packets.Mate> mates(PlayerState ps) {
        List<Packets.Mate> out = new ArrayList<>();
        if (ps.role.knowsWolves()) {
            for (PlayerState o : players.values()) {
                if (o != ps && o.role.knowsWolves()) {
                    out.add(new Packets.Mate(o.id, o.name));
                }
            }
        }
        if (ps.role == Role.KYOYU) {
            for (PlayerState o : players.values()) {
                if (o != ps && o.role == Role.KYOYU) {
                    out.add(new Packets.Mate(o.id, o.name));
                }
            }
        }
        return out;
    }

    public void stop(@Nullable ServerPlayer by) {
        if (phase == Phase.LOBBY) {
            if (by != null) {
                by.sendSystemMessage(Component.literal("ゲームは始まっていません").withStyle(ChatFormatting.GRAY));
            }
            return;
        }
        sendAll(new Packets.Announce("ゲーム中止", by != null ? by.getGameProfile().getName() + " がゲームを終了しました" : "", 0xAAAAAA, 0));
        resetToLobby();
    }

    private void endGame(Role.Team winner, String reason) {
        if (!running()) {
            return;
        }
        // 妖狐が生き残っていたら、妖狐の勝ち（てるてるの勝ちはそのまま）
        if (winner == Role.Team.VILLAGE || winner == Role.Team.WOLF) {
            for (PlayerState ps : players.values()) {
                if (ps.role == Role.FOX && ps.alive && !ps.left) {
                    reason = reason + "…が、妖狐 " + ps.name + " が生き残っていた";
                    winner = Role.Team.FOX;
                    break;
                }
            }
        }
        // 神（女将）が生き残っていたら、どの陣営が勝っても勝ちを横取りする
        for (PlayerState ps : players.values()) {
            if (ps.role.team == Role.Team.GOD && ps.alive && !ps.left) {
                reason = reason + "…が、神（女将） " + ps.name + " がすべてを見ていた";
                winner = Role.Team.GOD;
                break;
            }
        }
        phase = Phase.ENDED;
        endTimer = END_TICKS;
        reopenDoors();
        pendingBodies.clear();
        clearSabotage();
        removeBodies();
        for (BotEntity b : bots.values()) {
            b.getNavigation().stop();
        }
        List<Packets.Participant> list = new ArrayList<>();
        List<Integer> kills = new ArrayList<>();
        for (PlayerState ps : players.values()) {
            list.add(new Packets.Participant(ps.id, ps.name, ps.alive && !ps.left, ps.role.id));
            kills.add(ps.kills);
        }
        int taskPercent = Math.round(taskProgress() * 100);
        for (ServerPlayer p : online()) {
            PlayerState ps = players.get(p.getUUID());
            exitVent(p, ps, false);
            p.removeAllEffects();
            p.addEffect(new MobEffectInstance(MobEffects.SATURATION, -1, 0, false, false));
            send(p, new Packets.Result(winner.ordinal(), reason, list, kills, taskPercent, meetingCount));
        }
        syncAll();
    }

    private void resetToLobby() {
        clearSabotage();
        removeBodies();
        for (BotEntity b : bots.values()) {
            b.discard();
        }
        List<BlockPos> spawns = map().get(MarkerType.SPAWN);
        for (ServerPlayer p : online()) {
            PlayerState ps = players.get(p.getUUID());
            p.removeAllEffects();
            p.getInventory().clearContent();
            p.setGameMode(ps.previousMode == GameType.SPECTATOR ? GameType.ADVENTURE : ps.previousMode);
            if (!spawns.isEmpty()) {
                BlockPos sp = spawns.get(0);
                p.teleportTo(level, sp.getX() + 0.5, sp.getY() + 0.1, sp.getZ() + 0.5, p.getYRot(), 0);
            }
            if (ps.id.equals(host)) {
                p.getInventory().add(new ItemStack(ModItems.KEY));
            }
        }
        List<ServerPlayer> notify = online();
        players.clear();
        bots.clear();
        brains.clear();
        active.clear();
        activeProgress.clear();
        votes.clear();
        evilGodAt = -1;
        shuffleAt = -1;
        phase = Phase.LOBBY;
        for (ServerPlayer p : notify) {
            send(p, Packets.State.lobby());
        }
    }

    // ================================================================= 毎tick

    // ---- /minpaku preview task：近くの家具を1回分動かして見せる
    private final Map<BlockPos, Integer> previewing = new HashMap<>();

    /** 近く（半径10）の家具を、タスクを1回やったように動かす。動かした数を返す */
    public int previewTasks(ServerPlayer p) {
        BlockPos c = p.blockPosition();
        int n = 0;
        for (BlockPos pos : BlockPos.betweenClosed(c.offset(-10, -4, -10), c.offset(10, 4, 10))) {
            if (level.getBlockEntity(pos) instanceof com.minpaku.block.MarkerBlockEntity) {
                previewing.put(pos.immutable(), 0);
                n++;
            }
        }
        return n;
    }

    private void tickPreview() {
        if (previewing.isEmpty()) {
            return;
        }
        List<BlockPos> done = new ArrayList<>();
        for (Map.Entry<BlockPos, Integer> e : previewing.entrySet()) {
            int t = e.getValue() + 1;
            e.setValue(t);
            BlockPos pos = e.getKey();
            if (!(level.getBlockState(pos).getBlock() instanceof MarkerBlock mb)) {
                done.add(pos);
                continue;
            }
            MarkerType type = mb.type();
            switch (type.use) {
                case HOLD -> {
                    if (t % 5 == 0) {
                        animate(pos, 5f / Math.max(20, type.amount));
                    }
                    if (t >= Math.max(20, type.amount)) {
                        done.add(pos);
                    }
                }
                case CLICK -> {
                    if (t % 12 == 0) {
                        workFx(pos, type, 1f / type.amount);
                    }
                    if (t >= 12 * type.amount) {
                        done.add(pos);
                    }
                }
                default -> {
                    // ミニゲーム・設備：開く → しばらくして完成
                    if (t == 1) {
                        animate(pos, type.isTask() || type == MarkerType.BREAKER ? 0.2f : 0f);
                    }
                    if (t == 80) {
                        animate(pos, 1f);
                        done.add(pos);
                    }
                }
            }
        }
        done.forEach(previewing::remove);
    }

    public void tick() {
        tickPreview();
        if (phase == Phase.LOBBY) {
            tickLobby();
            return;
        }
        ticks++;
        if (phase == Phase.ENDED) {
            if (--endTimer <= 0) {
                resetToLobby();
            }
            return;
        }
        if (phase == Phase.PLAYING) {
            tickPlaying();
        } else if (phase == Phase.MEETING) {
            tickMeeting();
        }
        if (ticks % 5 == 0) {
            syncAll();
        }
    }

    private void tickPlaying() {
        if (sabotageCooldown > 0) {
            sabotageCooldown--;
        }
        tickPendingBodies();
        tickEvilGod();
        if (doorsOpenAt >= 0 && ticks >= doorsOpenAt) {
            reopenDoors();
        }
        if (ticks % 20 == 0) {
            sendRoomCounts();
        }
        for (PlayerState ps : new ArrayList<>(players.values())) {
            if (phase != Phase.PLAYING) {
                return;
            }
            if (ps.killCooldown > 0 && ps.alive) {
                ps.killCooldown--;
            }
            if (ps.bot) {
                tickBot(ps);
                continue;
            }
            ServerPlayer p = player(ps.id);
            if (p == null || ps.left) {
                continue;
            }
            // ベントの中では動けない
            if (ps.vent != null && p.distanceToSqr(Vec3.atBottomCenterOf(ps.vent)) > 0.3) {
                p.teleportTo(level, ps.vent.getX() + 0.5, ps.vent.getY() + 0.1, ps.vent.getZ() + 0.5, p.getYRot(), p.getXRot());
            }
            // 停電：人狼以外の生きている人は目の前しか見えない
            if (blackout && ps.alive && !ps.role.canSabotage() && ticks % 20 == 0) {
                p.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 60, 0, false, false));
            }
        }
        if (phase != Phase.PLAYING) {
            return;
        }

        // ガス漏れ
        if (gasTicks >= 0) {
            gasTicks--;
            if (gasBar != null) {
                gasBar.setProgress(Mth.clamp(gasTicks / (float) gasMax, 0f, 1f));
                gasBar.setName(Component.literal("ガス漏れ！ 元栓とボイラーを止めろ  " + (gasTicks / 20 + 1) + "秒"
                        + (valveFixed ? "  [元栓OK]" : "") + (boilerFixed ? "  [ボイラーOK]" : "")));
            }
            if (gasTicks <= 0) {
                endGame(Role.Team.WOLF, "ガス漏れを止められなかった");
                return;
            }
        }

        tickActive();

        if (ticks % 10 == 0) {
            checkWin();
        }
    }

    private void tickBot(PlayerState ps) {
        if (!ps.alive) {
            // 幽霊になったボットは、見えないところでタスクを少しずつ進める
            if (ps.tasksCount() && ticks % 300 == ps.id.hashCode() % 300) {
                for (PlayerState.TaskEntry t : ps.tasks) {
                    if (!t.done) {
                        t.done = true;
                        break;
                    }
                }
            }
            return;
        }
        BotEntity bot = bots.get(ps.id);
        BotBrain brain = brains.get(ps.id);
        if (bot == null || brain == null || !bot.isAlive()) {
            return;
        }
        brain.tickPlaying(bot);
    }

    private void tickActive() {
        List<UUID> finished = new ArrayList<>();
        List<UUID> cancelled = new ArrayList<>();
        for (Map.Entry<UUID, Active> e : active.entrySet()) {
            ServerPlayer p = player(e.getKey());
            PlayerState ps = players.get(e.getKey());
            Active a = e.getValue();
            if (p == null || ps == null) {
                cancelled.add(e.getKey());
                continue;
            }
            if (!lookingAt(p, a.pos)) {
                cancelled.add(e.getKey());
                actionBar(p, "作業を中断しました（ブロックを見たまま待とう）", ChatFormatting.GRAY);
                continue;
            }
            int prog = activeProgress.merge(e.getKey(), 1, Integer::sum);
            if (prog % 5 == 0) {
                animate(a.pos, 5f / a.need);
            }
            if (prog % 10 == 0) {
                workFx(a.pos, a.type);
            }
            if (prog >= a.need) {
                finished.add(e.getKey());
            }
        }
        for (UUID id : cancelled) {
            active.remove(id);
            activeProgress.remove(id);
        }
        for (UUID id : finished) {
            Active a = active.remove(id);
            activeProgress.remove(id);
            ServerPlayer p = player(id);
            PlayerState ps = players.get(id);
            if (p == null || ps == null) {
                continue;
            }
            if (a.type == MarkerType.GAS_VALVE || a.type == MarkerType.BOILER) {
                fixGas(p, a.type);
            } else {
                for (PlayerState.TaskEntry t : ps.tasks) {
                    if (t.pos.equals(a.pos) && !t.done) {
                        completeTask(p, ps, t);
                        break;
                    }
                }
            }
        }
    }

    private boolean lookingAt(ServerPlayer p, BlockPos pos) {
        if (p.distanceToSqr(Vec3.atCenterOf(pos)) > 4.5 * 4.5) {
            return false;
        }
        HitResult hit = p.pick(5.0, 1f, false);
        return hit instanceof BlockHitResult bh && hit.getType() == HitResult.Type.BLOCK && bh.getBlockPos().equals(pos);
    }

    private void syncAll() {
        float progress = taskProgress();
        for (ServerPlayer p : online()) {
            sync(p, progress);
        }
    }

    float taskProgress() {
        int total = 0;
        int done = 0;
        for (PlayerState ps : players.values()) {
            if (!ps.tasksCount()) {
                continue;
            }
            for (PlayerState.TaskEntry t : ps.tasks) {
                total++;
                if (t.done) {
                    done++;
                }
            }
        }
        return total == 0 ? 0f : done / (float) total;
    }

    private void sync(ServerPlayer p, float progress) {
        PlayerState ps = players.get(p.getUUID());
        if (ps == null) {
            return;
        }
        List<Packets.TaskInfo> tasks = new ArrayList<>();
        for (PlayerState.TaskEntry t : ps.tasks) {
            tasks.add(new Packets.TaskInfo(t.pos, t.type.ordinal(), t.done, map().roomAt(t.pos)));
        }
        Active a = active.get(ps.id);
        float ap = a == null ? 0f : activeProgress.getOrDefault(ps.id, 0) / (float) a.need;
        int at = a == null ? -1 : a.type.ordinal();
        int phaseNum = switch (phase) {
            case LOBBY -> 0;
            case PLAYING -> 1;
            case MEETING -> 2;
            case ENDED -> 3;
        };
        send(p, new Packets.State(phaseNum, ps.role.id, ps.alive, mates(ps), tasks, !ps.tasksCount(), progress,
                ps.killCooldown, ps.seerLeft, ps.knightLeft, ps.emergencyLeft, blackout, gasTicks, gasMax,
                valveFixed, boilerFixed, sabotageCooldown, ps.vent != null, at, ap,
                map().roomAt(ps.vent != null ? ps.vent : p.blockPosition()), settings().get("kill_behind") != 0,
                settings().get("kill_range")));
    }

    // ================================================================= 勝敗

    void checkWin() {
        if (!running() || selfTestNoWin) {
            return;
        }
        int wolves = 0;
        int others = 0;
        for (PlayerState ps : players.values()) {
            if (!ps.alive || ps.left) {
                continue;
            }
            if (ps.role.isWolf()) {
                wolves++;
            } else {
                others++;
            }
        }
        if (wolves == 0) {
            endGame(Role.Team.VILLAGE, "人狼をすべて追放した");
        } else if (wolves >= others) {
            endGame(Role.Team.WOLF, "人狼が民泊を乗っ取った");
        } else if (taskProgress() >= 1f) {
            endGame(Role.Team.VILLAGE, "すべてのタスクを終えた");
        }
    }

    // ================================================================= キル

    @Nullable
    private PlayerState stateOf(Entity e) {
        return players.get(e.getUUID());
    }

    /** 人間の人狼がキルキーを押した（target は参加者の UUID） */
    public void requestKill(ServerPlayer killer, UUID targetId) {
        if (phase != Phase.PLAYING) {
            return;
        }
        LivingEntity target = bots.containsKey(targetId) ? bots.get(targetId) : player(targetId);
        if (target == null || target == killer) {
            return;
        }
        tryKill(killer, target);
    }

    /** 人間の人狼のキル（能力） */
    public void tryKill(ServerPlayer killer, LivingEntity target) {
        if (phase != Phase.PLAYING) {
            return;
        }
        PlayerState k = players.get(killer.getUUID());
        PlayerState t = stateOf(target);
        if (k == null || t == null) {
            return;
        }
        if (k.killCooldown > 0 && k.alive && k.role.canKill()) {
            actionBar(killer, "キルまであと " + (k.killCooldown / 20 + 1) + " 秒", ChatFormatting.RED);
            return;
        }
        if (k.alive && k.role.canKill() && t.alive && behindOnly() && k.role != Role.KAIRIKI && !isBehind(killer, target)
                && killer.distanceTo(target) <= settings().get("kill_range") + 0.6) {
            actionBar(killer, "背後から近づかないとキルできない", ChatFormatting.GOLD);
            return;
        }
        attemptKill(k, killer, t, target);
    }

    /** キルを試す（人間・ボット共通）。成功・失敗にかかわらず条件を満たせば true */
    boolean attemptKill(PlayerState k, LivingEntity killer, PlayerState t, LivingEntity target) {
        if (phase != Phase.PLAYING || !k.alive || !t.alive || !k.role.canKill() || k.vent != null || t.vent != null) {
            return false;
        }
        if (t.role.knowsWolves() && k.role.knowsWolves()) {
            return false;
        }
        if (k.killCooldown > 0) {
            return false;
        }
        double range = settings().get("kill_range") + 0.6;
        if (killer.distanceTo(target) > range || (behindOnly() && k.role != Role.KAIRIKI && !isBehind(killer, target))) {
            return false;
        }
        k.killCooldown = settings().get("kill_cooldown") * 20 * (k.role == Role.KAIRIKI ? 3 : 2) / 2;
        killer.swing(InteractionHand.MAIN_HAND, true);
        ServerPlayer kp = k.bot ? null : player(k.id);
        String fin = finisherOf(k);
        // 騎士の守り・妖狐（効かない）・板前のおにぎり
        int blocked = t.protectedByKnight ? 0 : t.role == Role.FOX ? 2 : t.onigiri ? 1 : -1;
        if (blocked >= 0) {
            if (blocked == 0) {
                t.protectedByKnight = false;
            }
            if (blocked == 1) {
                t.onigiri = false;
            }
            // 守られた側は気づかない（演出も音も出さない）。襲った人狼だけに「守られた」と分かる
            if (kp != null) {
                send(kp, new Packets.KillFx(2, t.name, t.id, fin, blocked));
            }
            return true;
        }
        boolean counter = t.role == Role.NEKOMATA;
        kill(target, t, k, fin, counter ? 2 : 0);
        k.kills++;
        if (kp != null) {
            send(kp, new Packets.KillFx(1, t.name, t.id, fin, 0));
        }
        // 猫又：犯人を道連れにする
        if (counter && k.alive) {
            kill(killer, k, t, "neko", 0);
        }
        // 見ていたボットは覚える
        for (PlayerState o : players.values()) {
            BotBrain brain = brains.get(o.id);
            BotEntity eye = bots.get(o.id);
            if (brain != null && eye != null && o.alive && o != k && o != t) {
                double sight = blackout ? 5 : 12;
                if (eye.distanceTo(killer) <= sight && eye.hasLineOfSight(killer)) {
                    brain.sawKill(k, t);
                }
            }
        }
        checkWin();
        return true;
    }

    /** 設定「背後からのみキル」 */
    boolean behindOnly() {
        return settings().get("kill_behind") != 0;
    }

    /** killer が target の背後にいるか（target の向いている方向と反対側） */
    public static boolean isBehind(Entity killer, Entity target) {
        float yaw = target instanceof LivingEntity le ? le.getYHeadRot() : target.getYRot();
        Vec3 look = Vec3.directionFromRotation(0, yaw);
        Vec3 toKiller = new Vec3(killer.getX() - target.getX(), 0, killer.getZ() - target.getZ());
        if (toKiller.lengthSqr() < 1.0E-4) {
            return true;
        }
        return look.dot(toKiller.normalize()) < -0.25;
    }

    /** その人狼のフィニッシャー（設定で「各自が選んだもの」なら本人の好み） */
    String finisherOf(PlayerState k) {
        if (settings().get("finisher_mode") == 2) {
            String pref = finisherPrefs.get(k.id);
            if (pref == null) {
                String[] common = {"pingpong", "pillow", "fusuma", "seoi"};
                pref = common[Math.floorMod(k.id.hashCode(), common.length)];
            }
            return pref;
        }
        return k.role.finisherId().isEmpty() ? "wolf" : k.role.finisherId();
    }

    /** プレイヤーが選んだ共通フィニッシャー（ゲームをまたいで覚える） */
    private final Map<UUID, String> finisherPrefs = new HashMap<>();

    public void setFinisherPref(ServerPlayer p, String id) {
        if (List.of("pingpong", "pillow", "fusuma", "seoi").contains(id)) {
            finisherPrefs.put(p.getUUID(), id);
            actionBar(p, "フィニッシャーを選びました", ChatFormatting.GREEN);
        }
    }

    // 忍び狼の死体（しばらくしてから出てくる）
    private record PendingBody(UUID owner, String name, Vec3 pos, float yaw, String killerRole, int[] timer) {
    }

    private final List<PendingBody> pendingBodies = new ArrayList<>();

    private void spawnBody(UUID owner, String name, Vec3 pos, float yaw, String killerRole) {
        BodyEntity body = ModEntities.BODY.create(level);
        if (body != null) {
            body.moveTo(pos.x, pos.y, pos.z, yaw, 0);
            body.setOwner(owner, name);
            body.killerRole = killerRole;
            level.addFreshEntity(body);
            bodies.add(body);
        }
    }

    private void tickPendingBodies() {
        pendingBodies.removeIf(pb -> {
            if (--pb.timer()[0] <= 0) {
                spawnBody(pb.owner(), pb.name(), pb.pos(), pb.yaw(), pb.killerRole());
                return true;
            }
            return false;
        });
    }

    private void kill(LivingEntity target, PlayerState t, PlayerState killer) {
        kill(target, t, killer, finisherOf(killer), 0);
    }

    private void kill(LivingEntity target, PlayerState t, PlayerState killer, String finisher, int variant) {
        if (killer.role == Role.SHINOBI) {
            pendingBodies.add(new PendingBody(t.id, t.name, target.position(), target.getYRot(), killer.role.id,
                    new int[]{settings().get("shinobi_hide") * 20}));
        } else {
            spawnBody(t.id, t.name, target.position(), target.getYRot(), killer.role.id);
        }
        level.playSound(null, target.blockPosition(), SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 1.2f, 0.6f);
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.REDSTONE_BLOCK.defaultBlockState()),
                target.getX(), target.getY() + 0.8, target.getZ(), 40, 0.3, 0.4, 0.3, 0.1);
        if (t.bot) {
            t.alive = false;
            t.protectedByKnight = false;
            target.discard();
        } else if (target instanceof ServerPlayer sp) {
            becomeGhost(sp, t);
            send(sp, new Packets.KillFx(0, killer.name, killer.id, finisher, variant));
        }
    }

    private void becomeGhost(ServerPlayer p, PlayerState ps) {
        ps.alive = false;
        ps.protectedByKnight = false;
        active.remove(ps.id);
        activeProgress.remove(ps.id);
        exitVent(p, ps, false);
        p.removeEffect(MobEffects.BLINDNESS);
        p.setGameMode(GameType.SPECTATOR);
        // 幽霊になったことは演出と画面下のヒントで伝える（チャットは演出にかぶるので出さない）
    }

    // ================================================================= 役職の能力

    public void useSeer(ServerPlayer seer, LivingEntity target) {
        PlayerState s = players.get(seer.getUUID());
        PlayerState t = stateOf(target);
        if (phase != Phase.PLAYING || s == null || t == null || !s.alive || !t.alive || s.role != Role.SEER) {
            return;
        }
        if (s.seerLeft <= 0) {
            actionBar(seer, "次の会議まで占えません", ChatFormatting.GRAY);
            return;
        }
        if (seer.distanceTo(target) > 4) {
            actionBar(seer, "もっと近づいて占おう", ChatFormatting.GRAY);
            return;
        }
        s.seerLeft--;
        if (divineEffects(s, t)) {
            return;
        }
        boolean wolf = t.role.isWolf();
        seer.sendSystemMessage(Component.literal("【占い】" + t.name + " は ")
                .append(Component.literal(wolf ? "人狼" : "人狼ではない").withStyle(wolf ? ChatFormatting.RED : ChatFormatting.GREEN))
                .append(Component.literal(wolf ? " です！" : " です")));
        seer.playNotifySound(SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1f, 1.2f);
        send(seer, new Packets.Announce("占い結果", t.name + " は " + (wolf ? "人狼" : "人狼ではない"),
                wolf ? 0xFF5555 : 0x55FF55, 3));
    }

    /**
     * 占ったときの特別な効果。呪狼を占うと占い師が倒れる（true を返す）。妖狐を占うと妖狐が消える。
     */
    boolean divineEffects(PlayerState seer, PlayerState t) {
        if (t.role == Role.JURO) {
            LivingEntity se = entityOf(seer);
            if (se != null && seer.alive) {
                kill(se, seer, t, "juro", 0);
                checkWin();
            }
            return true;
        }
        if (t.role == Role.FOX && t.alive) {
            LivingEntity fe = entityOf(t);
            if (fe != null) {
                if (t.bot) {
                    t.alive = false;
                    fe.discard();
                } else if (fe instanceof ServerPlayer fp) {
                    becomeGhost(fp, t);
                    send(fp, new Packets.Announce("占われた", "妖狐は占われて消えてしまった", 0xFFB300, 3));
                }
                level.sendParticles(ParticleTypes.FLAME, fe.getX(), fe.getY() + 1, fe.getZ(), 30, 0.3, 0.5, 0.3, 0.02);
                checkWin();
            }
        }
        return false;
    }

    /** 探偵：虫めがねで死体を調べる */
    public void useDetective(ServerPlayer p, BodyEntity body) {
        PlayerState ps = players.get(p.getUUID());
        if (phase != Phase.PLAYING || ps == null || !ps.alive || ps.role != Role.DETECTIVE) {
            return;
        }
        Role r = Role.byId(body.killerRole);
        String how = r == Role.SHERIFF ? "短筒で撃たれた跡" : switch (r.finisherId()) {
            case "shinobi" -> "口をふさがれ、押し入れへ";
            case "kairiki" -> "ちゃぶ台ごと投げ飛ばされた";
            case "juro" -> "呪符と青い鬼火";
            default -> "獣の爪あと";
        };
        p.playNotifySound(SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 1f, 1f);
        send(p, new Packets.Announce("探偵の調べ", body.ownerName() + " を倒したのは「" + r.name + "」（" + how + "）",
                r.color, 4));
        p.sendSystemMessage(Component.literal("【探偵】" + body.ownerName() + " を倒したのは ")
                .append(Component.literal(r.name).withStyle(ChatFormatting.RED)).append(Component.literal("（" + how + "）")));
    }

    /** 板前：おにぎりを渡す */
    public void useItamae(ServerPlayer p, LivingEntity target) {
        PlayerState k = players.get(p.getUUID());
        PlayerState t = stateOf(target);
        if (phase != Phase.PLAYING || k == null || t == null || !k.alive || !t.alive || k.role != Role.ITAMAE) {
            return;
        }
        if (k.itamaeLeft <= 0) {
            actionBar(p, "次の会議までおにぎりは作れません", ChatFormatting.GRAY);
            return;
        }
        if (p.distanceTo(target) > 4) {
            actionBar(p, "もっと近づこう", ChatFormatting.GRAY);
            return;
        }
        k.itamaeLeft--;
        t.onigiri = true;
        p.playNotifySound(SoundEvents.GENERIC_EAT, SoundSource.PLAYERS, 1f, 1.2f);
        actionBar(p, t.name + " におにぎりを渡しました（1回だけキルを耐える）", ChatFormatting.YELLOW);
        ServerPlayer tp = t.bot ? null : player(t.id);
        if (tp != null) {
            actionBar(tp, "板前からおにぎりをもらった！", ChatFormatting.YELLOW);
        }
    }

    // ---- 女将：ふすまを閉める
    private record ClosedDoor(BlockPos pos, BlockState old) {
    }

    private final List<ClosedDoor> closedDoors = new ArrayList<>();
    private long doorsOpenAt = -1;

    private void reopenDoors() {
        for (ClosedDoor d : closedDoors) {
            if (level.getBlockState(d.pos()).is(Blocks.MUSHROOM_STEM)) {
                level.setBlock(d.pos(), d.old(), Block.UPDATE_CLIENTS);
            }
        }
        if (!closedDoors.isEmpty()) {
            level.playSound(null, closedDoors.get(0).pos(), SoundEvents.WOODEN_DOOR_OPEN, SoundSource.BLOCKS, 1f, 0.9f);
        }
        closedDoors.clear();
        doorsOpenAt = -1;
    }

    // ---- 番頭：部屋ごとの人数
    private void sendRoomCounts() {
        Map<String, Integer> counts = new java.util.LinkedHashMap<>();
        for (PlayerState ps : players.values()) {
            if (!ps.alive || ps.left) {
                continue;
            }
            LivingEntity e = entityOf(ps);
            if (e == null) {
                continue;
            }
            String room = map().roomAt(e.blockPosition());
            if (!room.isEmpty()) {
                counts.merge(room, 1, Integer::sum);
            }
        }
        for (PlayerState ps : players.values()) {
            if (ps.role == Role.BANTO && ps.alive && !ps.bot) {
                ServerPlayer p = player(ps.id);
                if (p != null) {
                    send(p, new Packets.RoomCounts(new ArrayList<>(counts.keySet()), new ArrayList<>(counts.values())));
                }
            }
        }
    }

    public void useKnight(ServerPlayer knight, LivingEntity target) {
        PlayerState k = players.get(knight.getUUID());
        PlayerState t = stateOf(target);
        if (phase != Phase.PLAYING || k == null || t == null || !k.alive || !t.alive || k.role != Role.KNIGHT) {
            return;
        }
        if (k.knightLeft <= 0) {
            actionBar(knight, "次の会議まで守れません", ChatFormatting.GRAY);
            return;
        }
        if (knight.distanceTo(target) > 4) {
            actionBar(knight, "もっと近づこう", ChatFormatting.GRAY);
            return;
        }
        k.knightLeft--;
        t.protectedByKnight = true;
        knight.playNotifySound(SoundEvents.ARMOR_EQUIP_IRON.value(), SoundSource.PLAYERS, 1f, 1f);
        actionBar(knight, t.name + " を次の会議まで守ります", ChatFormatting.YELLOW);
    }

    // ================================================================= ブロックを使う

    public InteractionResult onUseMarker(ServerPlayer p, BlockPos pos, MarkerType type) {
        if (!running()) {
            lobbyUse(p, pos, type);
            return InteractionResult.SUCCESS;
        }
        PlayerState ps = players.get(p.getUUID());
        if (ps == null || ps.left || phase != Phase.PLAYING) {
            return InteractionResult.SUCCESS;
        }
        useMarker(p, ps, pos, type, ps.alive);
        return InteractionResult.SUCCESS;
    }

    /** 幽霊の右クリック（タスクだけできる） */
    public void ghostUse(ServerPlayer p, BlockPos pos) {
        PlayerState ps = players.get(p.getUUID());
        if (phase != Phase.PLAYING || ps == null || ps.alive || ps.left) {
            return;
        }
        if (p.distanceToSqr(Vec3.atCenterOf(pos)) > 36) {
            return;
        }
        if (level.getBlockState(pos).getBlock() instanceof MarkerBlock mb && mb.type().isTask()) {
            useMarker(p, ps, pos, mb.type(), false);
        }
    }

    private void useMarker(ServerPlayer p, PlayerState ps, BlockPos pos, MarkerType type, boolean alive) {
        if (ps.vent != null || type.isDecor()) {
            return;
        }
        switch (type) {
            case SPAWN -> {
            }
            case BELL -> {
                if (!alive) {
                    return;
                }
                if (gasTicks >= 0) {
                    actionBar(p, "ガス漏れ中は呼び鈴を鳴らせない！", ChatFormatting.RED);
                } else if (ps.emergencyLeft <= 0) {
                    actionBar(p, "もう呼び鈴は使えません", ChatFormatting.GRAY);
                } else {
                    ps.emergencyLeft--;
                    animate(pos, 0f);
                    level.playSound(null, pos, SoundEvents.BELL_BLOCK, SoundSource.BLOCKS, 2f, 1.2f);
                    startMeeting(0, ps, null);
                }
            }
            case VENT -> {
                if (!alive) {
                    return;
                }
                boolean can = ps.role.canKill() || (ps.role.canVent() && settings().flag("madman_vent"));
                if (can) {
                    enterVent(p, ps, pos);
                } else {
                    actionBar(p, "床下点検口だ。開かない…", ChatFormatting.GRAY);
                }
            }
            case BREAKER -> {
                if (!alive) {
                    return;
                }
                if (blackout) {
                    animate(pos, 0.3f);
                    send(p, new Packets.OpenTask(pos, MarkerType.BREAKER.ordinal()));
                } else {
                    actionBar(p, "配電盤に異常はない", ChatFormatting.GRAY);
                }
            }
            case GAS_VALVE, BOILER -> {
                if (!alive) {
                    return;
                }
                boolean fixed = type == MarkerType.GAS_VALVE ? valveFixed : boilerFixed;
                if (gasTicks < 0 || fixed) {
                    actionBar(p, gasTicks < 0 ? "異常はない" : "ここはもう止めた！", ChatFormatting.GRAY);
                } else {
                    startActive(p, pos, type, type.amount);
                }
            }
            default -> useTask(p, ps, pos, type);
        }
    }

    private void useTask(ServerPlayer p, PlayerState ps, BlockPos pos, MarkerType type) {
        PlayerState.TaskEntry entry = null;
        for (PlayerState.TaskEntry t : ps.tasks) {
            if (t.pos.equals(pos)) {
                entry = t;
                break;
            }
        }
        if (entry == null) {
            actionBar(p, "これはあなたのタスクではない", ChatFormatting.GRAY);
            return;
        }
        if (entry.done) {
            actionBar(p, "このタスクは終わっている", ChatFormatting.GREEN);
            return;
        }
        switch (type.use) {
            case HOLD -> startActive(p, pos, type, type.amount);
            case CLICK -> {
                if (ticks - ps.lastClickTick < 4) {
                    return;
                }
                ps.lastClickTick = ticks;
                entry.progress++;
                workFx(pos, type, 1f / type.amount);
                actionBar(p, type.taskName + "  " + entry.progress + " / " + type.amount, ChatFormatting.YELLOW);
                if (entry.progress >= type.amount) {
                    completeTask(p, ps, entry);
                }
            }
            case WIRES, NUMBERS, HOLES -> {
                animate(pos, 0.2f);
                send(p, new Packets.OpenTask(pos, type.ordinal()));
            }
            default -> {
            }
        }
    }

    private void startActive(ServerPlayer p, BlockPos pos, MarkerType type, int need) {
        Active cur = active.get(p.getUUID());
        if (cur != null && cur.pos.equals(pos)) {
            return;
        }
        active.put(p.getUUID(), new Active(pos, type, need));
        activeProgress.put(p.getUUID(), 0);
        String name = type.taskName != null ? type.taskName : type.label;
        actionBar(p, name + "…（ブロックを見たまま待とう）", ChatFormatting.YELLOW);
    }

    /** ミニゲーム画面を終えた */
    public void finishMiniGame(ServerPlayer p, BlockPos pos) {
        if (!running()) {
            // ゲーム前のお試し：家具が完成の動きをするだけ
            if (p.distanceToSqr(Vec3.atCenterOf(pos)) <= 49 && level.getBlockState(pos).getBlock() instanceof MarkerBlock) {
                animate(pos, 1f);
                p.playNotifySound(SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.6f, 1.2f);
            }
            return;
        }
        PlayerState ps = players.get(p.getUUID());
        if (phase != Phase.PLAYING || ps == null || ps.left) {
            return;
        }
        if (p.distanceToSqr(Vec3.atCenterOf(pos)) > 49) {
            return;
        }
        if (!(level.getBlockState(pos).getBlock() instanceof MarkerBlock mb)) {
            return;
        }
        if (mb.type() == MarkerType.BREAKER) {
            if (blackout && ps.alive) {
                animate(pos, 1f);
                fixBlackout(p);
            }
            return;
        }
        animate(pos, 1f);
        for (PlayerState.TaskEntry t : ps.tasks) {
            if (t.pos.equals(pos) && !t.done && (t.type.use == MarkerType.Use.WIRES
                    || t.type.use == MarkerType.Use.NUMBERS || t.type.use == MarkerType.Use.HOLES)) {
                completeTask(p, ps, t);
                return;
            }
        }
    }

    private void completeTask(ServerPlayer p, PlayerState ps, PlayerState.TaskEntry t) {
        t.done = true;
        p.playNotifySound(SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.8f, 1.2f);
        actionBar(p, "タスク完了: " + t.type.taskName, ChatFormatting.GREEN);
        boolean all = ps.tasks.stream().allMatch(e -> e.done);
        if (all) {
            p.playNotifySound(SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.8f, 1f);
            p.sendSystemMessage(Component.literal("自分のタスクをすべて終えました！").withStyle(ChatFormatting.GREEN));
        }
        checkWin();
    }

    /** ボットがタスクを終えた */
    void botCompleteTask(PlayerState ps, PlayerState.TaskEntry t) {
        t.done = true;
        checkWin();
    }

    /** 家具を動かす（step = 進んだ割合）。タスク中の動きはまわりの全員に見える */
    void animate(BlockPos pos, float step) {
        if (level.getBlockEntity(pos) instanceof com.minpaku.block.MarkerBlockEntity be) {
            be.pulse(step);
        }
    }

    void workFx(BlockPos pos, MarkerType type, float step) {
        animate(pos, step);
        workFx(pos, type);
    }

    void workFx(BlockPos pos, MarkerType type) {
        double x = pos.getX() + 0.5;
        double y = pos.getY() + 0.6;
        double z = pos.getZ() + 0.5;
        ParticleOptions particle;
        SoundEvent sound;
        float pitch = 1f;
        switch (type) {
            case FUTON -> {
                particle = ParticleTypes.POOF;
                sound = SoundEvents.WOOL_PLACE;
            }
            case KAMADO -> {
                particle = ParticleTypes.CAMPFIRE_COSY_SMOKE;
                sound = SoundEvents.CAMPFIRE_CRACKLE;
                y += 0.4;
            }
            case BATH -> {
                particle = ParticleTypes.CLOUD;
                sound = SoundEvents.BUBBLE_COLUMN_UPWARDS_AMBIENT;
            }
            case CHOPPING -> {
                particle = new BlockParticleOption(ParticleTypes.BLOCK, Blocks.OAK_LOG.defaultBlockState());
                sound = SoundEvents.WOOD_BREAK;
                pitch = 0.8f;
            }
            case FIELD -> {
                particle = ParticleTypes.HAPPY_VILLAGER;
                sound = SoundEvents.CROP_BREAK;
            }
            case DUST -> {
                particle = ParticleTypes.POOF;
                sound = SoundEvents.BRUSH_GENERIC;
                y = pos.getY() + 0.2;
            }
            case LAUNDRY -> {
                particle = ParticleTypes.POOF;
                sound = SoundEvents.WOOL_PLACE;
                y += 0.5;
            }
            case VASE -> {
                particle = ParticleTypes.SPLASH;
                sound = SoundEvents.BOTTLE_EMPTY;
            }
            case SINK -> {
                particle = ParticleTypes.SPLASH;
                sound = SoundEvents.BUCKET_EMPTY;
                y += 0.4;
            }
            case SHOE_RACK -> {
                particle = ParticleTypes.CRIT;
                sound = SoundEvents.WOOD_PLACE;
            }
            case GAS_VALVE, BOILER -> {
                particle = ParticleTypes.SMOKE;
                sound = SoundEvents.IRON_TRAPDOOR_CLOSE;
                pitch = 0.6f;
            }
            default -> {
                particle = ParticleTypes.CRIT;
                sound = SoundEvents.LEVER_CLICK;
            }
        }
        level.sendParticles(particle, x, y, z, 8, 0.25, 0.2, 0.25, 0.02);
        level.playSound(null, pos, sound, SoundSource.BLOCKS, 0.8f, pitch);
    }

    // ================================================================= ベント（人間の人狼だけ）

    private void enterVent(ServerPlayer p, PlayerState ps, BlockPos pos) {
        animate(pos, 0f);
        ps.vent = pos.immutable();
        active.remove(ps.id);
        p.setGameMode(GameType.SPECTATOR);
        p.teleportTo(level, pos.getX() + 0.5, pos.getY() + 0.1, pos.getZ() + 0.5, p.getYRot(), p.getXRot());
        level.playSound(null, pos, SoundEvents.IRON_TRAPDOOR_OPEN, SoundSource.BLOCKS, 0.8f, 0.8f);
        actionBar(p, "ベントの中… A/D で移動、スニークで出る", ChatFormatting.LIGHT_PURPLE);
    }

    public void ventMove(ServerPlayer p, int dir) {
        if (!running() && lobbyVents.containsKey(p.getUUID())) {
            lobbyVentMove(p, dir);
            return;
        }
        PlayerState ps = players.get(p.getUUID());
        if (ps == null || ps.vent == null || phase != Phase.PLAYING) {
            return;
        }
        if (dir == 0) {
            exitVent(p, ps, true);
            return;
        }
        List<BlockPos> vents = map().get(MarkerType.VENT);
        if (vents.size() < 2) {
            actionBar(p, "つながっているベントがない", ChatFormatting.GRAY);
            return;
        }
        int idx = vents.indexOf(ps.vent);
        int next = Math.floorMod((idx < 0 ? 0 : idx) + (dir > 0 ? 1 : -1), vents.size());
        animate(ps.vent, 0f);
        ps.vent = vents.get(next);
        animate(ps.vent, 0f);
        p.teleportTo(level, ps.vent.getX() + 0.5, ps.vent.getY() + 0.1, ps.vent.getZ() + 0.5, p.getYRot(), p.getXRot());
        p.playNotifySound(SoundEvents.IRON_TRAPDOOR_CLOSE, SoundSource.BLOCKS, 0.5f, 1.4f);
        actionBar(p, "ベント " + (next + 1) + " / " + vents.size() + "  （A/D で移動、スニークで出る）", ChatFormatting.LIGHT_PURPLE);
    }

    private void exitVent(@Nullable ServerPlayer p, @Nullable PlayerState ps, boolean sound) {
        if (p == null || ps == null || ps.vent == null) {
            return;
        }
        BlockPos pos = ps.vent;
        ps.vent = null;
        animate(pos, 0f);
        if (ps.alive) {
            p.setGameMode(GameType.ADVENTURE);
        }
        p.teleportTo(level, pos.getX() + 0.5, pos.getY() + 0.1, pos.getZ() + 0.5, p.getYRot(), p.getXRot());
        if (sound) {
            level.playSound(null, pos, SoundEvents.IRON_TRAPDOOR_OPEN, SoundSource.BLOCKS, 0.8f, 1.1f);
        }
    }

    // ================================================================= 妨害

    public void sabotage(ServerPlayer p, int kind) {
        PlayerState ps = players.get(p.getUUID());
        if (ps != null) {
            sabotageBy(ps, p, kind);
        }
    }

    boolean sabotageBy(PlayerState ps, @Nullable ServerPlayer feedback, int kind) {
        if (phase != Phase.PLAYING || !ps.role.canSabotage()) {
            return false;
        }
        if (blackout || gasTicks >= 0) {
            actionBar(feedback, "今は別の妨害が起きている", ChatFormatting.GRAY);
            return false;
        }
        if (sabotageCooldown > 0) {
            actionBar(feedback, "妨害まであと " + (sabotageCooldown / 20 + 1) + " 秒", ChatFormatting.RED);
            return false;
        }
        if (kind == 0) {
            if (map().count(MarkerType.BREAKER) == 0) {
                actionBar(feedback, "配電盤が置かれていないので停電を起こせない", ChatFormatting.GRAY);
                return false;
            }
            blackout = true;
            for (ServerPlayer o : online()) {
                PlayerState os = players.get(o.getUUID());
                send(o, new Packets.Announce("停電！", os.role.canSabotage() ? "村人の視界が暗くなった" : "配電盤を直そう",
                        0xFFD54F, 1));
            }
        } else {
            if (map().count(MarkerType.GAS_VALVE) == 0 || map().count(MarkerType.BOILER) == 0) {
                actionBar(feedback, "ガスの元栓とボイラーが置かれていないので起こせない", ChatFormatting.GRAY);
                return false;
            }
            gasMax = settings().get("gas_time") * 20;
            gasTicks = gasMax;
            valveFixed = false;
            boilerFixed = false;
            gasBar = new ServerBossEvent(Component.literal("ガス漏れ！"), BossEvent.BossBarColor.RED, BossEvent.BossBarOverlay.PROGRESS);
            for (ServerPlayer o : online()) {
                gasBar.addPlayer(o);
                send(o, new Packets.Announce("ガス漏れ！", "ガスの元栓とボイラーを止めろ！", 0xFF5252, 1));
            }
        }
        sabotageCooldown = settings().get("sabotage_cooldown") * 20;
        return true;
    }

    void fixBlackout(@Nullable ServerPlayer by) {
        if (!blackout) {
            return;
        }
        blackout = false;
        for (ServerPlayer o : online()) {
            o.removeEffect(MobEffects.BLINDNESS);
            send(o, new Packets.Announce("", "電気が戻った", 0xFFFFFF, 3));
        }
        sabotageCooldown = Math.max(sabotageCooldown, settings().get("sabotage_cooldown") * 20);
    }

    void fixGas(@Nullable ServerPlayer by, MarkerType type) {
        if (gasTicks < 0) {
            return;
        }
        if (type == MarkerType.GAS_VALVE) {
            valveFixed = true;
        } else {
            boilerFixed = true;
        }
        actionBar(by, type == MarkerType.GAS_VALVE ? "ガスの元栓を閉めた！" : "ボイラーを止めた！", ChatFormatting.GREEN);
        if (valveFixed && boilerFixed) {
            gasTicks = -1;
            if (gasBar != null) {
                gasBar.removeAllPlayers();
                gasBar = null;
            }
            sendAll(new Packets.Announce("", "ガス漏れが止まった", 0xFFFFFF, 3));
            sabotageCooldown = Math.max(sabotageCooldown, settings().get("sabotage_cooldown") * 20);
        }
    }

    private void clearSabotage() {
        blackout = false;
        gasTicks = -1;
        if (gasBar != null) {
            gasBar.removeAllPlayers();
            gasBar = null;
        }
        for (ServerPlayer o : online()) {
            o.removeEffect(MobEffects.BLINDNESS);
        }
    }

    // ================================================================= 会議

    public void reportBody(ServerPlayer p, BodyEntity body) {
        PlayerState ps = players.get(p.getUUID());
        if (phase != Phase.PLAYING || ps == null || !ps.alive || ps.vent != null) {
            return;
        }
        if (gasTicks >= 0) {
            actionBar(p, "ガス漏れ中は通報できない！", ChatFormatting.RED);
            return;
        }
        if (p.distanceTo(body) > 4) {
            return;
        }
        startMeeting(1, ps, body);
    }

    /** ボットが通報する */
    void botReport(PlayerState ps, BodyEntity body) {
        if (phase != Phase.PLAYING || !ps.alive || gasTicks >= 0) {
            return;
        }
        startMeeting(1, ps, body);
    }

    /** ボットが呼び鈴を鳴らす */
    void botEmergency(PlayerState ps) {
        if (phase != Phase.PLAYING || !ps.alive || gasTicks >= 0 || ps.emergencyLeft <= 0) {
            return;
        }
        ps.emergencyLeft--;
        startMeeting(0, ps, null);
    }

    private int meetingCount;

    private void startMeeting(int reason, PlayerState caller, @Nullable BodyEntity body) {
        reopenDoors();
        pendingBodies.clear();
        meetingCount++;
        usedLines.clear();
        String bodyName = body != null ? body.ownerName() : "";
        UUID bodyOwner = body != null ? body.owner().orElse(null) : null;
        phase = Phase.MEETING;
        meetingTicks = 0;
        votes.clear();
        resultShown = false;
        ejected = null;
        GameSettings s = settings();
        discussionTicks = s.get("discussion_time") * 20;
        votingTicks = s.get("voting_time") * 20;
        active.clear();
        activeProgress.clear();
        removeBodies();

        // 集合場所：会議の座卓（なければ呼び鈴、それもなければスタート地点）
        List<BlockPos> tables = map().get(MarkerType.MEETING);
        List<BlockPos> bells = map().get(MarkerType.BELL);
        List<BlockPos> spawns = map().get(MarkerType.SPAWN);
        Vec3 center = !tables.isEmpty() ? Vec3.atBottomCenterOf(tables.get(0))
                : !bells.isEmpty() ? Vec3.atBottomCenterOf(bells.get(0))
                : !spawns.isEmpty() ? Vec3.atBottomCenterOf(spawns.get(0)) : null;
        // 会議の前にいた場所を覚える（終わったら戻す）
        for (PlayerState ps : living()) {
            LivingEntity e = entityOf(ps);
            ps.beforeMeeting = e != null ? e.position() : null;
            ps.beforeYaw = e != null ? e.getYRot() : 0f;
        }

        for (ServerPlayer p : online()) {
            PlayerState ps = players.get(p.getUUID());
            exitVent(p, ps, false);
            p.removeEffect(MobEffects.BLINDNESS);
        }
        List<PlayerState> living = living();
        for (int i = 0; i < living.size(); i++) {
            PlayerState ps = living.get(i);
            LivingEntity e = entityOf(ps);
            if (e == null) {
                continue;
            }
            if (center != null) {
                double a = i / (double) living.size() * Math.PI * 2;
                double r = freeMeeting() ? 3.4 : tables.isEmpty() ? 2.5 : 2.7;
                double x = center.x + Math.cos(a) * r;
                double z = center.z + Math.sin(a) * r;
                float yaw = (float) (Mth.atan2(center.z - z, center.x - x) * Mth.RAD_TO_DEG) - 90f;
                ps.seat = new Vec3(x, center.y, z);
                if (e instanceof ServerPlayer sp) {
                    sp.teleportTo(level, x, center.y, z, yaw, 10f);
                } else {
                    e.moveTo(x, center.y, z, yaw, 0);
                    e.setYHeadRot(yaw);
                    e.setYBodyRot(yaw);
                }
            } else {
                ps.seat = e.position();
            }
            if (e instanceof BotEntity b) {
                b.getNavigation().stop();
            }
        }

        for (PlayerState ps : players.values()) {
            ps.protectedByKnight = false;
            ps.seerLeft = s.get("seer_uses");
            ps.knightLeft = s.get("knight_uses");
            ps.itamaeLeft = s.get("itamae_uses");
        }

        List<Packets.Participant> parts = participants(false);
        sendAll(new Packets.Info("meeting_free", freeMeeting() ? "1" : "0"));
        for (ServerPlayer p : online()) {
            PlayerState ps = players.get(p.getUUID());
            send(p, new Packets.Meeting(reason, caller.name, bodyName, parts, discussionTicks, votingTicks, ps.alive));
        }
        for (BotBrain brain : brains.values()) {
            brain.onMeetingStart(reason, caller, bodyOwner);
        }
        syncAll();
    }

    private List<Packets.Participant> participants(boolean withRoles) {
        List<Packets.Participant> parts = new ArrayList<>();
        for (PlayerState ps : players.values()) {
            parts.add(new Packets.Participant(ps.id, ps.name, ps.alive && !ps.left, withRoles ? ps.role.id : ""));
        }
        return parts;
    }

    /** 会議の方式：井戸端会議（集まったあと自由に動ける）か */
    boolean freeMeeting() {
        return settings().get("meeting_style") == 2;
    }

    private void tickMeeting() {
        meetingTicks++;
        // 席から動けない（アモアス式のとき）
        for (PlayerState ps : freeMeeting() ? List.<PlayerState>of() : living()) {
            LivingEntity e = entityOf(ps);
            if (e == null || ps.seat == null) {
                continue;
            }
            if (e.position().distanceToSqr(ps.seat) > 0.25) {
                if (e instanceof ServerPlayer sp) {
                    sp.teleportTo(level, ps.seat.x, ps.seat.y, ps.seat.z, sp.getYRot(), sp.getXRot());
                } else {
                    e.moveTo(ps.seat.x, ps.seat.y, ps.seat.z, e.getYRot(), 0);
                }
            }
        }
        if (resultShown) {
            if (--postTimer <= 0) {
                finishMeeting();
            }
            return;
        }
        for (BotBrain brain : new ArrayList<>(brains.values())) {
            if (resultShown) {
                break;
            }
            brain.tickMeeting();
        }
        if (!resultShown && meetingTicks >= discussionTicks + votingTicks) {
            tally();
        }
    }

    public void vote(ServerPlayer p, UUID target) {
        PlayerState ps = players.get(p.getUUID());
        if (ps != null) {
            castVote(ps, target);
        }
    }

    boolean votingOpen() {
        return phase == Phase.MEETING && !resultShown && meetingTicks >= discussionTicks;
    }

    void castVote(PlayerState ps, UUID target) {
        if (!votingOpen() || !ps.alive || ps.left || votes.containsKey(ps.id)) {
            return;
        }
        if (!target.equals(Packets.NONE)) {
            PlayerState t = players.get(target);
            if (t == null || !t.alive || t.left) {
                return;
            }
        }
        votes.put(ps.id, target);
        LivingEntity e = entityOf(ps);
        if (e != null) {
            level.playSound(null, e.blockPosition(), SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.PLAYERS, 0.6f, 1.2f);
        }
        sendAll(new Packets.Voted(new ArrayList<>(votes.keySet())));
        if (votes.size() >= living().size()) {
            tally();
        }
    }

    private void tally() {
        if (resultShown) {
            return;
        }
        Map<UUID, Integer> counts = new HashMap<>();
        int skip = 0;
        for (UUID t : votes.values()) {
            if (t.equals(Packets.NONE)) {
                skip++;
            } else {
                counts.merge(t, 1, Integer::sum);
            }
        }
        UUID top = null;
        int best = 0;
        boolean tie = false;
        for (Map.Entry<UUID, Integer> e : counts.entrySet()) {
            if (e.getValue() > best) {
                best = e.getValue();
                top = e.getKey();
                tie = false;
            } else if (e.getValue() == best) {
                tie = true;
            }
        }
        ejected = (top != null && !tie && best > skip) ? top : null;

        String name = "";
        String text;
        if (ejected == null) {
            text = tie ? "同票のため、誰も追放されなかった" : "誰も追放されなかった";
        } else {
            PlayerState e = players.get(ejected);
            name = e.name;
            if (e.role == Role.TERU) {
                text = name + " はてるてるだった！";
            } else if (settings().flag("confirm_eject")) {
                text = e.role.isWolf() ? name + " は人狼だった" : name + " は人狼ではなかった";
            } else {
                text = name + " は追放された";
            }
        }
        boolean anon = settings().flag("anonymous_votes");
        List<Packets.Vote> list = new ArrayList<>();
        for (Map.Entry<UUID, UUID> v : votes.entrySet()) {
            list.add(new Packets.Vote(anon ? Packets.NONE : v.getKey(), v.getValue()));
        }
        resultShown = true;
        postTimer = MEETING_RESULT_TICKS + EJECT_TICKS;
        sendAll(new Packets.MeetingResult(list, anon, ejected == null ? Packets.NONE : ejected, name, text));
    }

    private void finishMeeting() {
        if (ejected != null) {
            PlayerState e = players.get(ejected);
            if (e != null) {
                if (e.bot) {
                    e.alive = false;
                    BotEntity b = bots.get(e.id);
                    if (b != null) {
                        b.discard();
                    }
                } else {
                    ServerPlayer ep = player(ejected);
                    if (ep != null) {
                        becomeGhost(ep, e);
                    } else {
                        e.alive = false;
                    }
                }
                if (e.role == Role.TERU) {
                    endGame(Role.Team.TERU, "てるてる " + e.name + " が追放されて一人勝ち！");
                    return;
                }
                boolean wolf = e.role.isWolf();
                if (settings().flag("medium_result")) {
                    for (PlayerState m : players.values()) {
                        if (m.role != Role.MEDIUM || !m.alive) {
                            continue;
                        }
                        if (m.bot) {
                            BotBrain brain = brains.get(m.id);
                            if (brain != null) {
                                brain.mediumResult(e, wolf);
                            }
                            continue;
                        }
                        ServerPlayer mp = player(m.id);
                        if (mp != null) {
                            mp.sendSystemMessage(Component.literal("【霊媒】" + e.name + " は ")
                                    .append(Component.literal(wolf ? "人狼だった" : "人狼ではなかった")
                                            .withStyle(wolf ? ChatFormatting.RED : ChatFormatting.GREEN)));
                            send(mp, new Packets.Announce("霊媒結果", e.name + " は " + (wolf ? "人狼だった" : "人狼ではなかった"),
                                    wolf ? 0xFF5555 : 0x55FF55, 3));
                        }
                    }
                }
                for (BotBrain brain : brains.values()) {
                    brain.onEjected(e);
                }
            }
        }
        phase = Phase.PLAYING;
        lastMeetingEnd = ticks;
        int cd = settings().get("kill_cooldown") * 20;
        for (PlayerState ps : players.values()) {
            ps.killCooldown = cd;
            ps.seat = null;
            // 会議の前にいた場所へ戻す
            if (ps.alive && !ps.left && ps.beforeMeeting != null) {
                LivingEntity e = entityOf(ps);
                Vec3 b = ps.beforeMeeting;
                if (e instanceof ServerPlayer sp) {
                    sp.teleportTo(level, b.x, b.y, b.z, ps.beforeYaw, 0f);
                } else if (e != null) {
                    e.moveTo(b.x, b.y, b.z, ps.beforeYaw, 0);
                }
            }
            ps.beforeMeeting = null;
        }
        for (BotBrain brain : brains.values()) {
            brain.onMeetingEnd();
        }
        syncAll();
        checkWin();
    }

    private void removeBodies() {
        for (BodyEntity b : bodies) {
            b.discard();
        }
        bodies.clear();
        for (BodyEntity b : level.getEntities(ModEntities.BODY, e -> true)) {
            b.discard();
        }
    }

    /** 会議中に人間がチャットで名前を出したら、ボットが参考にする */
    public void onChat(ServerPlayer sender, String text) {
        if (!running()) {
            return;
        }
        PlayerState s = players.get(sender.getUUID());
        if (s == null) {
            return;
        }
        if (phase == Phase.MEETING) {
            sendAll(new Packets.Talk(s.id, s.name, text));
        }
        for (PlayerState ps : players.values()) {
            if (ps == s || !ps.alive || ps.name.length() < 2 || !text.contains(ps.name)) {
                continue;
            }
            boolean clear = text.contains("白") || text.contains("違う") || text.contains("ちがう") || text.contains("味方");
            for (BotBrain brain : brains.values()) {
                brain.heard(s, ps, clear ? -30 : 35);
            }
        }
    }


    // ================================================================= ゲーム前（ロビー）：タスクとベントをお試しで使える

    /** ゲーム前にベントに入っている人と、入る前のゲームモード */
    private final Map<UUID, BlockPos> lobbyVents = new HashMap<>();
    private final Map<UUID, GameType> lobbyVentModes = new HashMap<>();

    private void lobbyUse(ServerPlayer p, BlockPos pos, MarkerType type) {
        if (lobbyVents.containsKey(p.getUUID())) {
            return;
        }
        switch (type) {
            case SPAWN, MEETING -> actionBar(p, type.label, ChatFormatting.GRAY);
            case HOKORA -> {
                level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 1f, 0.6f);
                actionBar(p, "古い祠だ。何かが封じられている…", ChatFormatting.LIGHT_PURPLE);
            }
            case BELL -> {
                animate(pos, 0f);
                level.playSound(null, pos, SoundEvents.BELL_BLOCK, SoundSource.BLOCKS, 1f, 1.2f);
                actionBar(p, "呼び鈴（ゲーム中に鳴らすと緊急会議）", ChatFormatting.GRAY);
            }
            case VENT -> {
                lobbyVentModes.put(p.getUUID(), p.gameMode.getGameModeForPlayer());
                lobbyVents.put(p.getUUID(), pos.immutable());
                animate(pos, 0f);
                p.setGameMode(GameType.SPECTATOR);
                p.teleportTo(level, pos.getX() + 0.5, pos.getY() + 0.1, pos.getZ() + 0.5, p.getYRot(), p.getXRot());
                level.playSound(null, pos, SoundEvents.IRON_TRAPDOOR_OPEN, SoundSource.BLOCKS, 0.8f, 0.8f);
                send(p, new Packets.Info("lobby_vent", "1"));
                actionBar(p, "ベントの中（お試し）… A/D で移動、スニークで出る", ChatFormatting.LIGHT_PURPLE);
            }
            case BREAKER -> {
                animate(pos, 0.3f);
                send(p, new Packets.OpenTask(pos, MarkerType.BREAKER.ordinal()));
            }
            default -> {
                String name = type.taskName != null ? type.taskName : type.label;
                switch (type.use) {
                    case HOLD -> {
                        previewing.put(pos.immutable(), 0);
                        workFx(pos, type);
                        actionBar(p, name + "（お試し）", ChatFormatting.YELLOW);
                    }
                    case CLICK -> {
                        workFx(pos, type, 1f / Math.max(1, type.amount));
                        actionBar(p, name + "（お試し）", ChatFormatting.YELLOW);
                    }
                    case WIRES, NUMBERS, HOLES -> {
                        animate(pos, 0.2f);
                        send(p, new Packets.OpenTask(pos, type.ordinal()));
                    }
                    default -> actionBar(p, type.label, ChatFormatting.GRAY);
                }
            }
        }
    }

    private int dropClearTimer;

    private void tickLobby() {
        if (dropClearTimer > 0 && --dropClearTimer % 20 == 0) {
            MapBuilder.clearDrops(level);
        }
        if (lobbyVents.isEmpty()) {
            return;
        }
        lobbyVents.entrySet().removeIf(e -> player(e.getKey()) == null);
        for (Map.Entry<UUID, BlockPos> e : lobbyVents.entrySet()) {
            ServerPlayer p = player(e.getKey());
            BlockPos v = e.getValue();
            if (p != null && p.distanceToSqr(Vec3.atBottomCenterOf(v)) > 0.3) {
                p.teleportTo(level, v.getX() + 0.5, v.getY() + 0.1, v.getZ() + 0.5, p.getYRot(), p.getXRot());
            }
        }
    }

    private void lobbyVentMove(ServerPlayer p, int dir) {
        BlockPos cur = lobbyVents.get(p.getUUID());
        if (cur == null) {
            return;
        }
        if (dir == 0) {
            exitLobbyVent(p);
            return;
        }
        List<BlockPos> vents = map().get(MarkerType.VENT);
        if (vents.size() < 2) {
            actionBar(p, "つながっているベントがない", ChatFormatting.GRAY);
            return;
        }
        int idx = vents.indexOf(cur);
        int next = Math.floorMod((idx < 0 ? 0 : idx) + (dir > 0 ? 1 : -1), vents.size());
        animate(cur, 0f);
        BlockPos nv = vents.get(next);
        lobbyVents.put(p.getUUID(), nv);
        animate(nv, 0f);
        p.teleportTo(level, nv.getX() + 0.5, nv.getY() + 0.1, nv.getZ() + 0.5, p.getYRot(), p.getXRot());
        p.playNotifySound(SoundEvents.IRON_TRAPDOOR_CLOSE, SoundSource.BLOCKS, 0.5f, 1.4f);
        actionBar(p, "ベント " + (next + 1) + " / " + vents.size() + "（お試し）  A/D で移動、スニークで出る", ChatFormatting.LIGHT_PURPLE);
    }

    private void exitLobbyVent(ServerPlayer p) {
        BlockPos v = lobbyVents.remove(p.getUUID());
        GameType mode = lobbyVentModes.remove(p.getUUID());
        if (v == null) {
            return;
        }
        animate(v, 0f);
        p.setGameMode(mode == null || mode == GameType.SPECTATOR ? GameType.ADVENTURE : mode);
        p.teleportTo(level, v.getX() + 0.5, v.getY() + 0.1, v.getZ() + 0.5, p.getYRot(), p.getXRot());
        level.playSound(null, v, SoundEvents.IRON_TRAPDOOR_OPEN, SoundSource.BLOCKS, 0.8f, 1.1f);
        send(p, new Packets.Info("lobby_vent", "0"));
    }

    private void exitAllLobbyVents() {
        for (UUID id : new ArrayList<>(lobbyVents.keySet())) {
            ServerPlayer p = player(id);
            if (p != null) {
                exitLobbyVent(p);
            }
        }
        lobbyVents.clear();
        lobbyVentModes.clear();
    }

    // ================================================================= マップの範囲（外は時空のゆがみで見えない）

    /** 見取り図（会議中のマップ）用の部屋の一覧 */
    public void sendRooms(ServerPlayer p) {
        StringBuilder sb = new StringBuilder();
        for (MapData.Room r : map().rooms()) {
            if (sb.length() > 0) {
                sb.append(';');
            }
            sb.append(r.name()).append('|').append(r.x1()).append('|').append(r.y1()).append('|').append(r.z1())
                    .append('|').append(r.x2()).append('|').append(r.y2()).append('|').append(r.z2());
        }
        send(p, new Packets.Info("rooms", sb.toString()));
        // 妨害を直す場所と呼び鈴（見取り図に出す）
        StringBuilder fx = new StringBuilder();
        for (MarkerType t : new MarkerType[]{MarkerType.BREAKER, MarkerType.GAS_VALVE, MarkerType.BOILER, MarkerType.BELL}) {
            for (net.minecraft.core.BlockPos pos : map().get(t)) {
                if (fx.length() > 0) {
                    fx.append(';');
                }
                fx.append(t.name()).append('|').append(pos.getX()).append('|').append(pos.getY()).append('|').append(pos.getZ());
            }
        }
        send(p, new Packets.Info("fixes", fx.toString()));
    }

    public void sendBounds(ServerPlayer p) {
        int[] b = map().lastBuilt;
        String data = b.length == 6 ? b[0] + "," + b[1] + "," + b[2] + "," + b[3] + "," + b[4] + "," + b[5] : "";
        send(p, new Packets.Info("bounds", data));
    }

    // ================================================================= 神（女将）：全員の役職が見える

    void sendRoles() {
        StringBuilder sb = new StringBuilder();
        for (PlayerState o : players.values()) {
            if (sb.length() > 0) {
                sb.append(';');
            }
            sb.append(o.id).append('=').append(o.role.id);
        }
        for (PlayerState ps : players.values()) {
            if (ps.bot) {
                continue;
            }
            ServerPlayer p = player(ps.id);
            if (p != null) {
                send(p, new Packets.Info("roles", ps.role.team == Role.Team.GOD ? sb.toString() : ""));
            }
        }
    }

    // ================================================================= シェリフ・ナイスボタナー・ゲッサー

    /** 見ている先の参加者（壁の向こうは撃てない） */
    @Nullable
    private LivingEntity aimed(ServerPlayer p, double range) {
        Vec3 eye = p.getEyePosition();
        Vec3 look = p.getViewVector(1f);
        HitResult block = p.pick(range, 1f, false);
        double reach = block.getType() == HitResult.Type.MISS ? range : block.getLocation().distanceTo(eye);
        Vec3 end = eye.add(look.scale(reach));
        net.minecraft.world.phys.AABB box = p.getBoundingBox().expandTowards(look.scale(reach)).inflate(1.0);
        net.minecraft.world.phys.EntityHitResult hit = net.minecraft.world.entity.projectile.ProjectileUtil.getEntityHitResult(
                p, eye, end, box, e -> e instanceof LivingEntity && e != p && players.containsKey(e.getUUID()), reach * reach);
        return hit != null && hit.getEntity() instanceof LivingEntity le ? le : null;
    }

    public void useSheriff(ServerPlayer sp) {
        PlayerState s = players.get(sp.getUUID());
        if (phase != Phase.PLAYING || s == null || !s.alive || s.role != Role.SHERIFF || s.vent != null) {
            return;
        }
        if (s.sheriffLeft <= 0) {
            actionBar(sp, "弾がもう残っていない", ChatFormatting.GRAY);
            return;
        }
        if (s.killCooldown > 0) {
            actionBar(sp, "次に撃てるまで " + (s.killCooldown / 20 + 1) + " 秒", ChatFormatting.GRAY);
            return;
        }
        LivingEntity target = aimed(sp, 14);
        PlayerState t = target == null ? null : stateOf(target);
        if (t == null || !t.alive) {
            actionBar(sp, "撃つ相手に照準を合わせよう", ChatFormatting.GRAY);
            return;
        }
        s.sheriffLeft--;
        s.killCooldown = settings().get("kill_cooldown") * 20;
        sp.swing(InteractionHand.MAIN_HAND, true);
        level.playSound(null, sp.blockPosition(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 0.7f, 1.9f);
        Vec3 muzzle = sp.getEyePosition().add(sp.getViewVector(1f).scale(0.8));
        level.sendParticles(ParticleTypes.SMOKE, muzzle.x, muzzle.y - 0.2, muzzle.z, 12, 0.05, 0.05, 0.05, 0.02);
        boolean ok = t.role.team == Role.Team.WOLF
                || (settings().flag("sheriff_neutral") && t.role.team != Role.Team.VILLAGE);
        if (ok) {
            kill(target, t, s, "sheriff", 0);
            s.kills++;
            send(sp, new Packets.KillFx(1, t.name, t.id, "sheriff", 0));
        } else {
            // 誤射：シェリフが倒れる（撃たれた人は無事）
            spawnBody(s.id, s.name, sp.position(), sp.getYRot(), "sheriff");
            becomeGhost(sp, s);
            send(sp, new Packets.Announce("誤射…", t.name + " は撃ってはいけない相手だった", 0xFF8A80, 0));
            level.playSound(null, sp.blockPosition(), SoundEvents.PLAYER_HURT, SoundSource.PLAYERS, 1f, 0.7f);
        }
        checkWin();
    }

    public void useButtoner(ServerPlayer p) {
        PlayerState ps = players.get(p.getUUID());
        if (phase != Phase.PLAYING || ps == null || !ps.alive || ps.role != Role.NICE_BUTTONER || ps.vent != null) {
            return;
        }
        if (gasTicks >= 0) {
            actionBar(p, "ガス漏れ中は呼び鈴を鳴らせない！", ChatFormatting.RED);
            return;
        }
        if (ps.buttonLeft <= 0) {
            actionBar(p, "もう呼び鈴は使えません", ChatFormatting.GRAY);
            return;
        }
        ps.buttonLeft--;
        level.playSound(null, p.blockPosition(), SoundEvents.BELL_BLOCK, SoundSource.PLAYERS, 2f, 1.5f);
        startMeeting(0, ps, null);
    }

    /** クライアントからの小さな操作 */
    public void act(ServerPlayer p, String kind, UUID target, String data) {
        if (kind.equals("guess")) {
            guess(p, target, data);
        }
    }

    /** ゲッサー：会議中に役職を当てる。当たれば相手、外れたら自分が倒れる */
    public void guess(ServerPlayer p, UUID targetId, String roleId) {
        PlayerState g = players.get(p.getUUID());
        if (phase != Phase.MEETING || resultShown || g == null || !g.alive || g.left) {
            return;
        }
        if (g.role != Role.NICE_GUESSER && g.role != Role.EVIL_GUESSER) {
            return;
        }
        if (g.guessLeft <= 0) {
            actionBar(p, "もう役職を当てる弾がない", ChatFormatting.GRAY);
            return;
        }
        PlayerState t = players.get(targetId);
        if (t == null || !t.alive || t.left || t == g) {
            return;
        }
        if (g.role == Role.EVIL_GUESSER && t.role.knowsWolves()) {
            actionBar(p, "仲間の人狼は撃てない", ChatFormatting.GRAY);
            return;
        }
        g.guessLeft--;
        boolean hit = t.role.id.equals(roleId);
        meetingKill(hit ? t : g);
    }

    /** 会議中に倒れる（だれが撃ったかは分からない） */
    private void meetingKill(PlayerState dead) {
        dead.alive = false;
        dead.protectedByKnight = false;
        LivingEntity e = entityOf(dead);
        if (dead.bot) {
            if (e != null) {
                e.discard();
            }
        } else if (e instanceof ServerPlayer sp) {
            becomeGhost(sp, dead);
        }
        if (e != null) {
            level.playSound(null, e.blockPosition(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 0.7f, 1.9f);
        }
        // 倒れた人の票は消す。倒れた人への票はスキップあつかい
        votes.remove(dead.id);
        for (Map.Entry<UUID, UUID> v : votes.entrySet()) {
            if (v.getValue().equals(dead.id)) {
                v.setValue(Packets.NONE);
            }
        }
        sendAll(new Packets.Info("meeting_death", dead.id + ";" + dead.name + ";" + dead.name + " が撃ち抜かれた"));
        sendAll(new Packets.Voted(new ArrayList<>(votes.keySet())));
        checkWin();
        if (phase == Phase.MEETING && !resultShown && votes.size() >= living().size()) {
            tally();
        }
    }

    // ================================================================= 祠の悪神：役職がすべて入れかわる

    private long evilGodAt = -1;
    private long shuffleAt = -1;

    private void tickEvilGod() {
        if (evilGodAt >= 0 && ticks >= evilGodAt) {
            evilGodAt = -1;
            awakenEvilGod();
        }
        if (shuffleAt >= 0 && ticks >= shuffleAt) {
            shuffleAt = -1;
            shuffleRoles();
        }
    }

    private void awakenEvilGod() {
        List<BlockPos> shrines = map().get(MarkerType.HOKORA);
        if (shrines.isEmpty()) {
            return;
        }
        BlockPos pos = shrines.get(random.nextInt(shrines.size()));
        animate(pos, 1f);
        net.minecraft.world.entity.LightningBolt bolt = net.minecraft.world.entity.EntityType.LIGHTNING_BOLT.create(level);
        if (bolt != null) {
            bolt.moveTo(Vec3.atBottomCenterOf(pos));
            bolt.setVisualOnly(true);
            level.addFreshEntity(bolt);
        }
        level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5, 80, 0.6, 1.2, 0.6, 0.05);
        level.sendParticles(ParticleTypes.LARGE_SMOKE, pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5, 40, 0.5, 1, 0.5, 0.02);
        for (ServerPlayer o : online()) {
            o.playNotifySound(SoundEvents.WITHER_SPAWN, SoundSource.HOSTILE, 0.8f, 0.6f);
            send(o, new Packets.Info("evil_god", pos.getX() + "," + pos.getY() + "," + pos.getZ()));
        }
        shuffleAt = ticks + 90;
    }

    /** 生きている人の役職を、すべてシャッフルして配り直す */
    private void shuffleRoles() {
        if (phase != Phase.PLAYING) {
            shuffleAt = ticks + 40;
            return;
        }
        List<PlayerState> alive = living();
        if (alive.size() < 2) {
            return;
        }
        List<Role> roles = new ArrayList<>();
        for (PlayerState ps : alive) {
            roles.add(ps.role);
        }
        List<Role> before = new ArrayList<>(roles);
        for (int i = 0; i < 8; i++) {
            Collections.shuffle(roles, random);
            if (!roles.equals(before)) {
                break;
            }
        }
        GameSettings s = settings();
        for (int i = 0; i < alive.size(); i++) {
            PlayerState ps = alive.get(i);
            ps.role = roles.get(i);
            ps.killCooldown = s.get("kill_cooldown") * 20;
            ps.seerLeft = s.get("seer_uses");
            ps.knightLeft = s.get("knight_uses");
            ps.itamaeLeft = s.get("itamae_uses");
            ps.guessLeft = s.get("guesser_shots");
            ps.buttonLeft = s.get("buttoner_uses");
            ps.sheriffLeft = s.get("sheriff_shots");
            ps.onigiri = false;
            ps.protectedByKnight = false;
            if (ps.bot) {
                brains.put(ps.id, new BotBrain(this, ps));
                continue;
            }
            ServerPlayer p = player(ps.id);
            if (p == null) {
                continue;
            }
            if (ps.vent != null && !ps.role.canVent()) {
                exitVent(p, ps, true);
            }
            active.remove(ps.id);
            activeProgress.remove(ps.id);
            p.getInventory().clearContent();
            giveRoleItem(p, ps);
        }
        for (PlayerState ps : alive) {
            ServerPlayer p = ps.bot ? null : player(ps.id);
            if (p != null) {
                send(p, new Packets.Reveal(ps.role.id, mates(ps), -1));
            }
        }
        sendRoles();
        syncAll();
        checkWin();
    }

    // ================================================================= 出入り

    public void onLeave(ServerPlayer p) {
        PlayerState ps = players.get(p.getUUID());
        if (ps == null || !running()) {
            return;
        }
        ps.left = true;
        ps.alive = false;
        active.remove(ps.id);
        if (phase == Phase.MEETING && !resultShown && votes.size() >= living().size()) {
            tally();
        }
        checkWin();
    }

    public void onJoin(ServerPlayer p) {
        sendBounds(p);
        sendRooms(p);
        PlayerState ps = players.get(p.getUUID());
        if (ps != null && running()) {
            ps.left = false;
            ps.alive = false;
            p.setGameMode(GameType.SPECTATOR);
        } else {
            send(p, Packets.State.lobby());
        }
    }

    public String status() {
        return "状態: " + phase + " / 参加 " + players.size() + "人（ボット " + bots.size() + "）/ 経過 " + ticks / 20 + "秒";
    }
}
