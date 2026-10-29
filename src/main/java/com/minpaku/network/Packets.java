package com.minpaku.network;

import com.minpaku.MinpakuJinrou;
import com.minpaku.game.GameSettings;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * 通信で使うデータの定義。S2C = サーバー→クライアント、C2S = クライアント→サーバー。
 */
public final class Packets {
    public static final UUID NONE = new UUID(0L, 0L);

    private Packets() {
    }

    private static <T extends CustomPacketPayload> CustomPacketPayload.Type<T> key(String name) {
        return new CustomPacketPayload.Type<>(MinpakuJinrou.id(name));
    }

    private static <T> void writeList(FriendlyByteBuf buf, List<T> list, BiConsumer<FriendlyByteBuf, T> w) {
        buf.writeVarInt(list.size());
        for (T t : list) {
            w.accept(buf, t);
        }
    }

    private static <T> List<T> readList(FriendlyByteBuf buf, Function<FriendlyByteBuf, T> r) {
        int n = buf.readVarInt();
        List<T> out = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            out.add(r.apply(buf));
        }
        return out;
    }

    // ------------------------------------------------------------ 共通の小さなデータ

    public record Mate(UUID id, String name) {
        static void write(FriendlyByteBuf b, Mate m) {
            b.writeUUID(m.id);
            b.writeUtf(m.name);
        }

        static Mate read(FriendlyByteBuf b) {
            return new Mate(b.readUUID(), b.readUtf());
        }
    }

    public record TaskInfo(BlockPos pos, int type, boolean done, String room) {
        static void write(FriendlyByteBuf b, TaskInfo t) {
            b.writeBlockPos(t.pos);
            b.writeVarInt(t.type);
            b.writeBoolean(t.done);
            b.writeUtf(t.room);
        }

        static TaskInfo read(FriendlyByteBuf b) {
            return new TaskInfo(b.readBlockPos(), b.readVarInt(), b.readBoolean(), b.readUtf());
        }
    }

    public record Participant(UUID id, String name, boolean alive, String role) {
        static void write(FriendlyByteBuf b, Participant p) {
            b.writeUUID(p.id);
            b.writeUtf(p.name);
            b.writeBoolean(p.alive);
            b.writeUtf(p.role);
        }

        static Participant read(FriendlyByteBuf b) {
            return new Participant(b.readUUID(), b.readUtf(), b.readBoolean(), b.readUtf());
        }
    }

    public record Vote(UUID voter, UUID target) {
        static void write(FriendlyByteBuf b, Vote v) {
            b.writeUUID(v.voter);
            b.writeUUID(v.target);
        }

        static Vote read(FriendlyByteBuf b) {
            return new Vote(b.readUUID(), b.readUUID());
        }
    }

    // ------------------------------------------------------------ S2C

    /** 自分のゲーム状態（毎秒数回送る） */
    public record State(int phase, String role, boolean alive, List<Mate> mates, List<TaskInfo> tasks,
                        boolean fakeTasks, float taskProgress, int killCooldown, int seerLeft, int knightLeft,
                        int emergencyLeft, boolean blackout, int gasTicks, int gasMax, boolean valveFixed,
                        boolean boilerFixed, int sabotageCooldown, boolean inVent, int activeType,
                        float activeProgress, String room, boolean killBehind, int killRange) implements CustomPacketPayload {
        public static final Type<State> TYPE = key("state");
        public static final StreamCodec<FriendlyByteBuf, State> CODEC = CustomPacketPayload.codec(State::write, State::read);

        public static State lobby() {
            return new State(0, "", true, List.of(), List.of(), false, 0, 0, 0, 0, 0,
                    false, -1, 1, false, false, 0, false, -1, 0, "", true, 2);
        }

        private void write(FriendlyByteBuf b) {
            b.writeVarInt(phase);
            b.writeUtf(role);
            b.writeBoolean(alive);
            writeList(b, mates, Mate::write);
            writeList(b, tasks, TaskInfo::write);
            b.writeBoolean(fakeTasks);
            b.writeFloat(taskProgress);
            b.writeVarInt(killCooldown);
            b.writeVarInt(seerLeft);
            b.writeVarInt(knightLeft);
            b.writeVarInt(emergencyLeft);
            b.writeBoolean(blackout);
            b.writeInt(gasTicks);
            b.writeVarInt(gasMax);
            b.writeBoolean(valveFixed);
            b.writeBoolean(boilerFixed);
            b.writeVarInt(sabotageCooldown);
            b.writeBoolean(inVent);
            b.writeInt(activeType);
            b.writeFloat(activeProgress);
            b.writeUtf(room);
            b.writeBoolean(killBehind);
            b.writeVarInt(killRange);
        }

        private static State read(FriendlyByteBuf b) {
            return new State(b.readVarInt(), b.readUtf(), b.readBoolean(), readList(b, Mate::read),
                    readList(b, TaskInfo::read), b.readBoolean(), b.readFloat(), b.readVarInt(), b.readVarInt(),
                    b.readVarInt(), b.readVarInt(), b.readBoolean(), b.readInt(), b.readVarInt(), b.readBoolean(),
                    b.readBoolean(), b.readVarInt(), b.readBoolean(), b.readInt(), b.readFloat(), b.readUtf(), b.readBoolean(), b.readVarInt());
        }

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** ゲーム開始時の役職発表 */
    public record Reveal(String role, List<Mate> mates, int players) implements CustomPacketPayload {
        public static final Type<Reveal> TYPE = key("reveal");
        public static final StreamCodec<FriendlyByteBuf, Reveal> CODEC = CustomPacketPayload.codec(Reveal::write, Reveal::read);

        private void write(FriendlyByteBuf b) {
            b.writeUtf(role);
            writeList(b, mates, Mate::write);
            b.writeVarInt(players);
        }

        private static Reveal read(FriendlyByteBuf b) {
            return new Reveal(b.readUtf(), readList(b, Mate::read), b.readVarInt());
        }

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** 設定画面の中身 */
    public record Settings(GameSettings settings, boolean canEdit, boolean running, Map<Integer, Integer> counts)
            implements CustomPacketPayload {
        public static final Type<Settings> TYPE = key("settings");
        public static final StreamCodec<FriendlyByteBuf, Settings> CODEC = CustomPacketPayload.codec(Settings::write, Settings::read);

        private void write(FriendlyByteBuf b) {
            settings.write(b);
            b.writeBoolean(canEdit);
            b.writeBoolean(running);
            b.writeVarInt(counts.size());
            counts.forEach((k, v) -> {
                b.writeVarInt(k);
                b.writeVarInt(v);
            });
        }

        private static Settings read(FriendlyByteBuf b) {
            GameSettings s = GameSettings.read(b);
            boolean edit = b.readBoolean();
            boolean running = b.readBoolean();
            int n = b.readVarInt();
            Map<Integer, Integer> counts = new HashMap<>();
            for (int i = 0; i < n; i++) {
                counts.put(b.readVarInt(), b.readVarInt());
            }
            return new Settings(s, edit, running, counts);
        }

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** 会議の開始。reason: 0 = 緊急ボタン / 1 = 死体発見 */
    public record Meeting(int reason, String caller, String bodyName, List<Participant> participants,
                          int discussionTicks, int votingTicks, boolean canVote) implements CustomPacketPayload {
        public static final Type<Meeting> TYPE = key("meeting");
        public static final StreamCodec<FriendlyByteBuf, Meeting> CODEC = CustomPacketPayload.codec(Meeting::write, Meeting::read);

        private void write(FriendlyByteBuf b) {
            b.writeVarInt(reason);
            b.writeUtf(caller);
            b.writeUtf(bodyName);
            writeList(b, participants, Participant::write);
            b.writeVarInt(discussionTicks);
            b.writeVarInt(votingTicks);
            b.writeBoolean(canVote);
        }

        private static Meeting read(FriendlyByteBuf b) {
            return new Meeting(b.readVarInt(), b.readUtf(), b.readUtf(), readList(b, Participant::read),
                    b.readVarInt(), b.readVarInt(), b.readBoolean());
        }

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** 投票が済んだ人の一覧 */
    public record Voted(List<UUID> voters) implements CustomPacketPayload {
        public static final Type<Voted> TYPE = key("voted");
        public static final StreamCodec<FriendlyByteBuf, Voted> CODEC = CustomPacketPayload.codec(Voted::write, Voted::read);

        private void write(FriendlyByteBuf b) {
            writeList(b, voters, (bb, u) -> bb.writeUUID(u));
        }

        private static Voted read(FriendlyByteBuf b) {
            return new Voted(readList(b, bb -> bb.readUUID()));
        }

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** 投票結果と追放 */
    public record MeetingResult(List<Vote> votes, boolean anonymous, UUID ejected, String ejectedName,
                                String ejectText) implements CustomPacketPayload {
        public static final Type<MeetingResult> TYPE = key("meeting_result");
        public static final StreamCodec<FriendlyByteBuf, MeetingResult> CODEC =
                CustomPacketPayload.codec(MeetingResult::write, MeetingResult::read);

        private void write(FriendlyByteBuf b) {
            writeList(b, votes, Vote::write);
            b.writeBoolean(anonymous);
            b.writeUUID(ejected);
            b.writeUtf(ejectedName);
            b.writeUtf(ejectText);
        }

        private static MeetingResult read(FriendlyByteBuf b) {
            return new MeetingResult(readList(b, Vote::read), b.readBoolean(), b.readUUID(), b.readUtf(), b.readUtf());
        }

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /**
     * キルの演出。kind: 0 = 襲われた / 1 = 襲った / 2 = 守られて失敗。otherId は相手（顔を出すため）。
     * finisher = フィニッシャーの種類。variant: 襲われた側 0 = ふつう / 1 = おにぎりで耐えた / 2 = 猫又の反撃、
     * 守られて失敗のとき 0 = 騎士 / 1 = おにぎり / 2 = 妖狐（効かない）
     */
    public record KillFx(int kind, String other, UUID otherId, String finisher, int variant) implements CustomPacketPayload {
        public KillFx(int kind, String other, UUID otherId) {
            this(kind, other, otherId, "wolf", 0);
        }

        public static final Type<KillFx> TYPE = key("kill_fx");
        public static final StreamCodec<FriendlyByteBuf, KillFx> CODEC = CustomPacketPayload.codec(KillFx::write, KillFx::read);

        private void write(FriendlyByteBuf b) {
            b.writeVarInt(kind);
            b.writeUtf(other);
            b.writeUUID(otherId);
            b.writeUtf(finisher);
            b.writeVarInt(variant);
        }

        private static KillFx read(FriendlyByteBuf b) {
            return new KillFx(b.readVarInt(), b.readUtf(), b.readUUID(), b.readUtf(), b.readVarInt());
        }

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** ミニゲーム画面を開く（type = MarkerType の番号） */
    public record OpenTask(BlockPos pos, int kind) implements CustomPacketPayload {
        public static final Type<OpenTask> TYPE = key("open_task");
        public static final StreamCodec<FriendlyByteBuf, OpenTask> CODEC = CustomPacketPayload.codec(OpenTask::write, OpenTask::read);

        private void write(FriendlyByteBuf b) {
            b.writeBlockPos(pos);
            b.writeVarInt(kind);
        }

        private static OpenTask read(FriendlyByteBuf b) {
            return new OpenTask(b.readBlockPos(), b.readVarInt());
        }

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** 大きな文字のお知らせ（妨害発生など）。sound: 0 なし / 1 警報 / 2 ベル / 3 成功 */
    public record Announce(String title, String subtitle, int color, int sound) implements CustomPacketPayload {
        public static final Type<Announce> TYPE = key("announce");
        public static final StreamCodec<FriendlyByteBuf, Announce> CODEC = CustomPacketPayload.codec(Announce::write, Announce::read);

        private void write(FriendlyByteBuf b) {
            b.writeUtf(title);
            b.writeUtf(subtitle);
            b.writeInt(color);
            b.writeVarInt(sound);
        }

        private static Announce read(FriendlyByteBuf b) {
            return new Announce(b.readUtf(), b.readUtf(), b.readInt(), b.readVarInt());
        }

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** ゲーム終了（winner = Role.Team の番号） */
    public record Result(int winner, String reason, List<Participant> players, List<Integer> kills, int taskPercent,
                         int meetings) implements CustomPacketPayload {
        public static final Type<Result> TYPE = key("result");
        public static final StreamCodec<FriendlyByteBuf, Result> CODEC = CustomPacketPayload.codec(Result::write, Result::read);

        private void write(FriendlyByteBuf b) {
            b.writeVarInt(winner);
            b.writeUtf(reason);
            writeList(b, players, Participant::write);
            writeList(b, kills, (bb, k) -> bb.writeVarInt(k));
            b.writeVarInt(taskPercent);
            b.writeVarInt(meetings);
        }

        private static Result read(FriendlyByteBuf b) {
            return new Result(b.readVarInt(), b.readUtf(), readList(b, Participant::read), readList(b, bb -> bb.readVarInt()),
                    b.readVarInt(), b.readVarInt());
        }

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** 会議中の発言（ボット・人間とも）。画面の右側に並べる */
    public record Talk(UUID id, String name, String text) implements CustomPacketPayload {
        public static final Type<Talk> TYPE = key("talk");
        public static final StreamCodec<FriendlyByteBuf, Talk> CODEC = CustomPacketPayload.codec(Talk::write, Talk::read);

        private void write(FriendlyByteBuf b) {
            b.writeUUID(id);
            b.writeUtf(name);
            b.writeUtf(text);
        }

        private static Talk read(FriendlyByteBuf b) {
            return new Talk(b.readUUID(), b.readUtf(), b.readUtf());
        }

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    // ------------------------------------------------------------ C2S

    /** 設定を1つ変える */
    public record SetSetting(String id, int value) implements CustomPacketPayload {
        public static final Type<SetSetting> TYPE = key("set_setting");
        public static final StreamCodec<FriendlyByteBuf, SetSetting> CODEC = CustomPacketPayload.codec(SetSetting::write, SetSetting::read);

        private void write(FriendlyByteBuf b) {
            b.writeUtf(id);
            b.writeVarInt(value);
        }

        private static SetSetting read(FriendlyByteBuf b) {
            return new SetSetting(b.readUtf(), b.readVarInt());
        }

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** ゲームの操作。action: 0 = 開始 / 1 = 強制終了 */
    public record Control(int action) implements CustomPacketPayload {
        public static final Type<Control> TYPE = key("control");
        public static final StreamCodec<FriendlyByteBuf, Control> CODEC = CustomPacketPayload.codec(Control::write, Control::read);

        private void write(FriendlyByteBuf b) {
            b.writeVarInt(action);
        }

        private static Control read(FriendlyByteBuf b) {
            return new Control(b.readVarInt());
        }

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** 投票（target = NONE ならスキップ） */
    /** アニメーションの確認（/minpaku preview）。kind: 1 = 追放 / 2 = 追放なし */
    public record Preview(int kind, UUID ejected, String name, String text, List<UUID> carriers) implements CustomPacketPayload {
        public static final Type<Preview> TYPE = key("preview");
        public static final StreamCodec<FriendlyByteBuf, Preview> CODEC = CustomPacketPayload.codec(Preview::write, Preview::read);

        private void write(FriendlyByteBuf b) {
            b.writeVarInt(kind);
            b.writeUUID(ejected);
            b.writeUtf(name);
            b.writeUtf(text);
            b.writeVarInt(carriers.size());
            for (UUID u : carriers) {
                b.writeUUID(u);
            }
        }

        private static Preview read(FriendlyByteBuf b) {
            int kind = b.readVarInt();
            UUID e = b.readUUID();
            String name = b.readUtf();
            String text = b.readUtf();
            int n = b.readVarInt();
            List<UUID> list = new java.util.ArrayList<>();
            for (int i = 0; i < n; i++) {
                list.add(b.readUUID());
            }
            return new Preview(kind, e, name, text, list);
        }

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** アニメーションの確認（/minpaku preview）。kind と opt は Previews を参照 */
    public record PreviewAnim(String kind, String opt, String otherName, UUID otherId, List<UUID> carriers)
            implements CustomPacketPayload {
        public static final Type<PreviewAnim> TYPE = key("preview_anim");
        public static final StreamCodec<FriendlyByteBuf, PreviewAnim> CODEC = CustomPacketPayload.codec(PreviewAnim::write, PreviewAnim::read);

        private void write(FriendlyByteBuf b) {
            b.writeUtf(kind);
            b.writeUtf(opt);
            b.writeUtf(otherName);
            b.writeUUID(otherId);
            b.writeVarInt(carriers.size());
            for (UUID u : carriers) {
                b.writeUUID(u);
            }
        }

        private static PreviewAnim read(FriendlyByteBuf b) {
            String k = b.readUtf();
            String o = b.readUtf();
            String n = b.readUtf();
            UUID id = b.readUUID();
            int c = b.readVarInt();
            List<UUID> list = new java.util.ArrayList<>();
            for (int i = 0; i < c; i++) {
                list.add(b.readUUID());
            }
            return new PreviewAnim(k, o, n, id, list);
        }

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** 番頭に送る：部屋ごとの人数 */
    public record RoomCounts(List<String> rooms, List<Integer> counts) implements CustomPacketPayload {
        public static final Type<RoomCounts> TYPE = key("room_counts");
        public static final StreamCodec<FriendlyByteBuf, RoomCounts> CODEC = CustomPacketPayload.codec(RoomCounts::write, RoomCounts::read);

        private void write(FriendlyByteBuf b) {
            b.writeVarInt(rooms.size());
            for (int i = 0; i < rooms.size(); i++) {
                b.writeUtf(rooms.get(i));
                b.writeVarInt(counts.get(i));
            }
        }

        private static RoomCounts read(FriendlyByteBuf b) {
            int n = b.readVarInt();
            List<String> r = new java.util.ArrayList<>();
            List<Integer> c = new java.util.ArrayList<>();
            for (int i = 0; i < n; i++) {
                r.add(b.readUtf());
                c.add(b.readVarInt());
            }
            return new RoomCounts(r, c);
        }

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** 自分の共通フィニッシャーを選ぶ */
    public record SetFinisher(String finisher) implements CustomPacketPayload {
        public static final Type<SetFinisher> TYPE = key("set_finisher");
        public static final StreamCodec<FriendlyByteBuf, SetFinisher> CODEC = CustomPacketPayload.codec(SetFinisher::write, SetFinisher::read);

        private void write(FriendlyByteBuf b) {
            b.writeUtf(finisher);
        }

        private static SetFinisher read(FriendlyByteBuf b) {
            return new SetFinisher(b.readUtf());
        }

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** 人狼のキル（能力）。target は参加者の UUID */
    public record KillRequest(UUID target) implements CustomPacketPayload {
        public static final Type<KillRequest> TYPE = key("kill_request");
        public static final StreamCodec<FriendlyByteBuf, KillRequest> CODEC = CustomPacketPayload.codec(KillRequest::write, KillRequest::read);

        private void write(FriendlyByteBuf b) {
            b.writeUUID(target);
        }

        private static KillRequest read(FriendlyByteBuf b) {
            return new KillRequest(b.readUUID());
        }

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record CastVote(UUID target) implements CustomPacketPayload {
        public static final Type<CastVote> TYPE = key("cast_vote");
        public static final StreamCodec<FriendlyByteBuf, CastVote> CODEC = CustomPacketPayload.codec(CastVote::write, CastVote::read);

        private void write(FriendlyByteBuf b) {
            b.writeUUID(target);
        }

        private static CastVote read(FriendlyByteBuf b) {
            return new CastVote(b.readUUID());
        }

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** ミニゲームを終えた */
    public record TaskDone(BlockPos pos) implements CustomPacketPayload {
        public static final Type<TaskDone> TYPE = key("task_done");
        public static final StreamCodec<FriendlyByteBuf, TaskDone> CODEC = CustomPacketPayload.codec(TaskDone::write, TaskDone::read);

        private void write(FriendlyByteBuf b) {
            b.writeBlockPos(pos);
        }

        private static TaskDone read(FriendlyByteBuf b) {
            return new TaskDone(b.readBlockPos());
        }

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** 妨害を起こす。kind: 0 = 停電 / 1 = ガス漏れ */
    public record Sabotage(int kind) implements CustomPacketPayload {
        public static final Type<Sabotage> TYPE = key("sabotage");
        public static final StreamCodec<FriendlyByteBuf, Sabotage> CODEC = CustomPacketPayload.codec(Sabotage::write, Sabotage::read);

        private void write(FriendlyByteBuf b) {
            b.writeVarInt(kind);
        }

        private static Sabotage read(FriendlyByteBuf b) {
            return new Sabotage(b.readVarInt());
        }

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** ベントの操作。dir: -1 / 1 = 隣のベントへ、0 = 出る */
    public record VentMove(int dir) implements CustomPacketPayload {
        public static final Type<VentMove> TYPE = key("vent_move");
        public static final StreamCodec<FriendlyByteBuf, VentMove> CODEC = CustomPacketPayload.codec(VentMove::write, VentMove::read);

        private void write(FriendlyByteBuf b) {
            b.writeVarInt(dir);
        }

        private static VentMove read(FriendlyByteBuf b) {
            return new VentMove(b.readVarInt());
        }

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** 幽霊がブロックを右クリックした（幽霊はふつうに触れないので代わりに送る） */
    public record GhostUse(BlockPos pos) implements CustomPacketPayload {
        public static final Type<GhostUse> TYPE = key("ghost_use");
        public static final StreamCodec<FriendlyByteBuf, GhostUse> CODEC = CustomPacketPayload.codec(GhostUse::write, GhostUse::read);

        private void write(FriendlyByteBuf b) {
            b.writeBlockPos(pos);
        }

        private static GhostUse read(FriendlyByteBuf b) {
            return new GhostUse(b.readBlockPos());
        }

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }
}
