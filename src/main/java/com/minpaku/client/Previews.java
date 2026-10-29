package com.minpaku.client;

import com.minpaku.client.scene.EjectScene;
import com.minpaku.client.scene.FinisherScenes;
import com.minpaku.client.screen.MeetingScreen;
import com.minpaku.client.screen.PreviewScreen;
import com.minpaku.client.screen.ResultScreen;
import com.minpaku.client.screen.RevealScreen;
import com.minpaku.game.Role;
import com.minpaku.network.Packets;
import net.minecraft.client.Minecraft;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.UUID;

/**
 * /minpaku preview で、アニメーションを自分の画面だけで再生する。
 * all のときは全部を順番に流す。
 */
public final class Previews {
    private Previews() {
    }

    private record Item(String kind, String opt, int ticks) {
    }

    private static final Deque<Item> QUEUE = new ArrayDeque<>();
    private static int wait;
    private static String otherName = "";
    private static UUID otherId = new UUID(0, 0);
    private static List<UUID> carriers = List.of();
    private static boolean closeAfter;

    /** サーバーから届いた再生の指示 */
    public static void play(Packets.PreviewAnim p) {
        otherName = p.otherName();
        otherId = p.otherId();
        carriers = p.carriers();
        QUEUE.clear();
        wait = 0;
        if (p.kind().equals("all")) {
            queueAll();
        } else {
            QUEUE.add(new Item(p.kind(), p.opt(), 0));
        }
    }

    /** 全部（順番に） */
    private static void queueAll() {
        for (Role r : Role.all()) {
            QUEUE.add(new Item("reveal", r.id, 150));
        }
        for (String id : FinisherScenes.NAMES.keySet()) {
            QUEUE.add(new Item("finisher", id, FinisherScenes.total(id, 0) + ClientGame.VICTIM_TAIL + 10));
        }
        QUEUE.add(new Item("finisher", "wolf:guard", FinisherScenes.total("wolf", 1) + 50));
        QUEUE.add(new Item("finisher", "wolf:counter", FinisherScenes.total("wolf", 2) + ClientGame.VICTIM_TAIL + 10));
        for (String o : new String[]{"wolf", "villager", "hidden", "teru"}) {
            QUEUE.add(new Item("eject", o, EjectScene.LEN + 6));
        }
        QUEUE.add(new Item("skip", "", EjectScene.LEN + 6));
        QUEUE.add(new Item("meeting", "report", 90));
        QUEUE.add(new Item("meeting", "bell", 90));
        for (String o : new String[]{"village", "wolf", "fox", "teru"}) {
            QUEUE.add(new Item("result", o, 140));
        }
        QUEUE.add(new Item("kill", "", 70));
        for (String o : new String[]{"knight", "onigiri", "fox"}) {
            QUEUE.add(new Item("guard", o, 70));
        }
        QUEUE.add(new Item("sabotage", "blackout", 80));
        QUEUE.add(new Item("sabotage", "gas", 80));
    }

    public static void tick() {
        if (wait > 0) {
            wait--;
            if (wait == 0 && closeAfter) {
                Minecraft.getInstance().setScreen(null);
                closeAfter = false;
            }
            return;
        }
        Item it = QUEUE.poll();
        if (it == null) {
            return;
        }
        start(it.kind, it.opt);
        wait = it.ticks;
    }

    private static List<Packets.Participant> people(Minecraft mc) {
        List<Packets.Participant> list = new ArrayList<>();
        String[] names = {"たろう", "はなこ", "けんた", "さくら", "ゆうき", "みお", "そうた"};
        String[] roles = {"villager", "wolf", "seer", "knight", "medium", "villager", "madman"};
        if (mc.player != null) {
            list.add(new Packets.Participant(mc.player.getUUID(), mc.player.getGameProfile().getName(), true, "villager"));
        }
        for (int i = 0; i < names.length; i++) {
            list.add(new Packets.Participant(UUID.nameUUIDFromBytes(("preview-" + i).getBytes()), names[i], i % 3 != 1, roles[i]));
        }
        return list;
    }

    /** 1つ再生する */
    public static void start(String kind, String opt) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }
        String me = mc.player.getGameProfile().getName();
        UUID myId = mc.player.getUUID();
        String killer = otherName.isEmpty() ? me : otherName;
        UUID killerId = otherName.isEmpty() ? myId : otherId;
        if (mc.screen != null && !(mc.screen instanceof PreviewScreen)) {
            mc.setScreen(null);
        }
        switch (kind) {
            case "finisher" -> {
                String[] parts = opt.split(":");
                String id = parts[0].isEmpty() ? "wolf" : parts[0];
                int variant = parts.length > 1 ? switch (parts[1]) {
                    case "guard" -> 1;
                    case "counter" -> 2;
                    default -> 0;
                } : 0;
                ClientGame.startVictimFx(id.equals("neko") ? "たま" : killer, killerId, id, variant);
            }
            case "eject" -> {
                String text = switch (opt) {
                    case "villager" -> me + " は人狼ではなかった";
                    case "hidden" -> me + " は追放された";
                    case "teru" -> me + " はてるてるだった！";
                    default -> me + " は人狼だった";
                };
                List<UUID> c = carriers.isEmpty() ? fakeCarriers() : carriers;
                mc.setScreen(new PreviewScreen(new EjectScene.Cast(myId, me, text, c)));
            }
            case "skip" -> mc.setScreen(new PreviewScreen(new EjectScene.Cast(Packets.NONE, "", "誰も追放されなかった", List.of())));
            case "reveal" -> {
                Role r = Role.byId(opt.isEmpty() ? "wolf" : opt);
                List<Packets.Mate> mates = new ArrayList<>();
                if (r.knowsWolves() || r == Role.KYOYU) {
                    mates.add(new Packets.Mate(UUID.nameUUIDFromBytes("preview-1".getBytes()), "はなこ"));
                }
                mc.setScreen(new RevealScreen(new Packets.Reveal(r.id, mates, 8)));
            }
            case "meeting" -> {
                boolean report = !opt.equals("bell");
                mc.setScreen(new MeetingScreen(new Packets.Meeting(report ? 1 : 0, "はなこ", report ? "けんた" : "", people(mc), 400, 300,
                        false)));
                closeAfter = true;
            }
            case "result" -> {
                Role.Team team = switch (opt) {
                    case "wolf" -> Role.Team.WOLF;
                    case "fox" -> Role.Team.FOX;
                    case "teru" -> Role.Team.TERU;
                    default -> Role.Team.VILLAGE;
                };
                String reason = switch (team) {
                    case WOLF -> "人狼が民泊を乗っ取った";
                    case FOX -> "人狼をすべて追放した…が、妖狐が生き残っていた";
                    case TERU -> "てるてるが追放されて一人勝ち！";
                    default -> "すべてのタスクを終えた";
                };
                List<Packets.Participant> ps = people(mc);
                if (team == Role.Team.FOX || team == Role.Team.TERU) {
                    Packets.Participant p0 = ps.get(1);
                    ps.set(1, new Packets.Participant(p0.id(), p0.name(), true, team == Role.Team.FOX ? "fox" : "teru"));
                }
                List<Integer> kills = new ArrayList<>();
                for (int i = 0; i < ps.size(); i++) {
                    kills.add(ps.get(i).role().equals("wolf") ? 2 : 0);
                }
                mc.setScreen(new ResultScreen(new Packets.Result(team.ordinal(), reason, ps, kills, 74, 3)));
            }
            case "kill" -> {
                ClientGame.killFxKind = 1;
                ClientGame.killFxOther = "はなこ";
                ClientGame.killFxOtherId = UUID.nameUUIDFromBytes("preview-1".getBytes());
                ClientGame.killFxLen = ClientGame.KILL_FX_LEN;
                ClientGame.killFxTicks = ClientGame.killFxLen;
                ClientGame.play(net.minecraft.sounds.SoundEvents.PLAYER_ATTACK_STRONG, 0.6f);
            }
            case "guard" -> {
                ClientGame.killFxKind = 2;
                ClientGame.killFxVariant = switch (opt) {
                    case "onigiri" -> 1;
                    case "fox" -> 2;
                    default -> 0;
                };
                ClientGame.killFxOther = "はなこ";
                ClientGame.killFxLen = ClientGame.KILL_FX_LEN;
                ClientGame.killFxTicks = ClientGame.killFxLen;
                ClientGame.play(net.minecraft.sounds.SoundEvents.SHIELD_BLOCK, 1f);
            }
            case "sabotage" -> {
                if (opt.equals("gas")) {
                    ClientGame.announce("ガス漏れ！", "元栓とボイラーを止めろ", 0xFF6A4A);
                } else {
                    ClientGame.announce("停電！", "配電盤を直そう", 0xFFD54F);
                }
            }
            default -> {
            }
        }
    }

    private static List<UUID> fakeCarriers() {
        List<UUID> c = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            c.add(UUID.nameUUIDFromBytes(("minpaku-carrier-" + i).getBytes()));
        }
        return c;
    }
}
