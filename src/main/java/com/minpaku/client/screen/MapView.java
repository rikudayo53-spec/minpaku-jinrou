package com.minpaku.client.screen;

import com.minpaku.client.ClientGame;
import com.minpaku.client.Ui;
import com.minpaku.network.Packets;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

/**
 * 会議中に開ける見取り図（アモアスの会議中マップのように）。
 * 部屋（建物の部屋・庭）、階の切りかえ、自分のタスク、会議の前にいた場所を描く。
 */
public final class MapView {
    private int floor;
    private int x, y, w, h;
    /** true = プレイ中（今いる場所を出す）、false = 会議中（会議の前にいた場所） */
    private final boolean live;
    private boolean floorPicked;

    public MapView() {
        this(false);
    }

    public MapView(boolean live) {
        this(live, false);
    }

    /** overlay = 歩きながら見る小さい地図（画面の右上。暗くしない・閉じるボタンなし・階は自分のいる階） */
    private final boolean overlay;

    public MapView(boolean live, boolean overlay) {
        this.live = live;
        this.overlay = overlay;
    }

    /** 今まさに直さないといけない設備か */
    private static boolean urgent(ClientGame.MapFix f) {
        Packets.State s = ClientGame.state;
        return switch (f.type()) {
            case BREAKER -> s.blackout();
            case GAS_VALVE -> s.gasTicks() >= 0 && !s.valveFixed();
            case BOILER -> s.gasTicks() >= 0 && !s.boilerFixed();
            default -> false;
        };
    }

    private static String fixName(com.minpaku.game.MarkerType t) {
        return switch (t) {
            case BREAKER -> "配電盤";
            case GAS_VALVE -> "元栓";
            case BOILER -> "ボイラー";
            default -> "呼び鈴";
        };
    }

    @org.jetbrains.annotations.Nullable
    private Vec3 me() {
        if (live) {
            var mc = net.minecraft.client.Minecraft.getInstance();
            return mc.player != null ? mc.player.position() : null;
        }
        return ClientGame.lastPlayPos;
    }

    /** 階の一覧（部屋の床の高さ） */
    private static List<Integer> floors() {
        TreeSet<Integer> ys = new TreeSet<>();
        for (ClientGame.MapRoom r : ClientGame.rooms) {
            ys.add(r.y1());
        }
        // 近い高さ（±2）はまとめる
        List<Integer> out = new ArrayList<>();
        for (int v : ys) {
            if (out.isEmpty() || v - out.get(out.size() - 1) > 2) {
                out.add(v);
            }
        }
        return out;
    }

    private static int floorOf(int y1, List<Integer> fl) {
        int best = 0;
        for (int i = 0; i < fl.size(); i++) {
            if (Math.abs(fl.get(i) - y1) <= 2) {
                best = i;
            }
        }
        return best;
    }

    public void render(GuiGraphics g, int sw, int sh, int mouseX, int mouseY) {
        if (overlay) {
            w = Math.min(sw * 62 / 100, 520);
            h = Math.min(sh * 72 / 100, w * 3 / 4 + 40);
            x = sw - w - 6;
            y = 6;
            floorPicked = false;   // いつも自分のいる階を出す
        } else {
            w = Math.min(sw - 16, 900);
            h = Math.min(sh - 16, 520);
            x = (sw - w) / 2;
            y = (sh - h) / 2;
            g.fill(0, 0, sw, sh, 0xA0000000);
        }
        Ui.sprite(g, "panel", x, y, w, h);
        Ui.serif(g, "見取り図", x + 12, y + 8, Ui.INK, overlay ? 1.1f : 1.4f, -1);
        if (overlay) {
            String key = com.minpaku.client.GameHud.MAP_KEY != null
                    ? com.minpaku.client.GameHud.MAP_KEY.getTranslatedKeyMessage().getString() : "M";
            Ui.sans(g, "［" + key + "］で閉じる", x + w - 10, y + 10, Ui.INK_SOFT, 0.7f, 1);
        } else {
            boolean hc = inside(mouseX, mouseY, x + w - 58, y + 8, 48, 16);
            Ui.button(g, x + w - 58, y + 8, 48, 16, "閉じる", false, hc, true, 0.85f);
        }
        List<Integer> fl = floors();
        if (ClientGame.rooms.isEmpty()) {
            Ui.sans(g, "見取り図がありません（基本マップを建てると出ます）", x + w / 2f, y + h / 2f, Ui.INK_SOFT, 1f, 0);
            return;
        }
        if (!floorPicked) {
            // 最初は、直す場所があればその階、なければ自分のいる階
            floorPicked = true;
            Vec3 m0 = me();
            if (m0 != null) {
                floor = taskFloor((int) Math.floor(m0.y), fl);
            }
            for (ClientGame.MapFix f : ClientGame.fixes) {
                if (urgent(f)) {
                    floor = taskFloor(f.pos().getY(), fl);
                    break;
                }
            }
        }
        floor = Math.min(floor, fl.size() - 1);
        float pulse = (float) (Math.sin(System.currentTimeMillis() / 160.0) * 0.5 + 0.5);
        // 階のボタン（小さい地図では「n階」と表示だけ）
        if (overlay && fl.size() > 1) {
            Ui.sans(g, (floor + 1) + "階", x + 84, y + 10, Ui.INK, 0.8f, -1);
        }
        for (int i = 0; i < fl.size() && !overlay; i++) {
            boolean hv = inside(mouseX, mouseY, x + 110 + i * 52, y + 8, 48, 16);
            Ui.button(g, x + 110 + i * 52, y + 8, 48, 16, (i + 1) + "階", i == floor, hv, true, 0.85f);
            // ほかの階に直す場所があれば「！」
            for (ClientGame.MapFix f : ClientGame.fixes) {
                if (urgent(f) && taskFloor(f.pos().getY(), fl) == i && i != floor) {
                    Ui.disc(g, x + 110 + i * 52 + 46, y + 9, 5f, Ui.alpha(0xFFE53935, 0.6f + 0.4f * pulse));
                    Ui.sans(g, "!", x + 110 + i * 52 + 46, y + 5, 0xFFFFFFFF, 0.75f, 0);
                    break;
                }
            }
        }
        // 範囲（全部の部屋の外枠）
        int minX = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;
        for (ClientGame.MapRoom r : ClientGame.rooms) {
            minX = Math.min(minX, r.x1());
            minZ = Math.min(minZ, r.z1());
            maxX = Math.max(maxX, r.x2() + 1);
            maxZ = Math.max(maxZ, r.z2() + 1);
        }
        int ax = x + 12;
        int ay = y + 32;
        int aw = w - 24;
        int ah = overlay ? h - 40 : h - 56;
        float scale = Math.min(aw / (float) Math.max(1, maxX - minX), ah / (float) Math.max(1, maxZ - minZ));
        float ox = ax + (aw - (maxX - minX) * scale) / 2f;
        float oz = ay + (ah - (maxZ - minZ) * scale) / 2f;
        g.fill(ax, ay, ax + aw, ay + ah, 0xFF3B3226);
        g.enableScissor(ax, ay, ax + aw, ay + ah);
        // 部屋：種類ごとに色を分け、太い壁で囲む。名前はいつも出す
        // 庭（屋根のない所）を先に描き、建物をその上に描く
        List<ClientGame.MapRoom> order = new ArrayList<>();
        for (ClientGame.MapRoom r : ClientGame.rooms) {
            if (r.y2() - r.y1() > 4) {
                order.add(r);
            }
        }
        for (ClientGame.MapRoom r : ClientGame.rooms) {
            if (r.y2() - r.y1() <= 4) {
                order.add(r);
            }
        }
        for (int pass = 0; pass < 2; pass++) {
            for (ClientGame.MapRoom r : order) {
                boolean open = r.y2() - r.y1() > 4;
                int f = floorOf(r.y1(), fl);
                if (f != floor && !(open && floor > 0)) {
                    continue;
                }
                int rx1 = (int) (ox + (r.x1() - minX) * scale);
                int rz1 = (int) (oz + (r.z1() - minZ) * scale);
                int rx2 = (int) (ox + (r.x2() + 1 - minX) * scale);
                int rz2 = (int) (oz + (r.z2() + 1 - minZ) * scale);
                boolean dim = f != floor;
                if (pass == 0) {
                    int fillC = dim ? 0xFF6D7A60 : roomColor(r.name(), open, r.x2() - r.x1(), r.z2() - r.z1());
                    g.fill(rx1, rz1, rx2, rz2, 0xFF2A2118);
                    g.fill(rx1 + 2, rz1 + 2, rx2 - 2, rz2 - 2, fillC);
                } else if (!dim) {
                    float cx = (rx1 + rx2) / 2f;
                    float cz = (rz1 + rz2) / 2f - 4;
                    int bw = rx2 - rx1 - 4;
                    if (bw >= 18 && rz2 - rz1 >= 10) {
                        float sc = Math.min(1.1f, Math.max(0.7f, Math.min(bw, rz2 - rz1) / 46f));
                        Ui.fitSans(g, r.name(), cx, cz, Math.max(10, bw), open ? 0xFF1F4A16 : 0xFF2B1D12, sc, 0);
                    }
                }
            }
        }
        // 自分のタスク
        for (Packets.TaskInfo t : ClientGame.state.tasks()) {
            int f = taskFloor(t.pos().getY(), fl);
            if (f != floor) {
                continue;
            }
            float px = ox + (t.pos().getX() + 0.5f - minX) * scale;
            float pz = oz + (t.pos().getZ() + 0.5f - minZ) * scale;
            if (!t.done()) {
                Ui.disc(g, px, pz, 7f + pulse * 2f, Ui.alpha(0xFFFFC21F, 0.35f));
            }
            Ui.disc(g, px, pz, 5f, 0xFF2B1D12);
            Ui.disc(g, px, pz, 4f, t.done() ? 0xFF3FA34D : 0xFFFFC21F);
            if (!t.done() && !overlay) {
                Ui.sansShadow(g, ClientGame.taskName(t.type()), px, pz + 6, 0xFFFFE9A8, 0.7f, 0);
            }
        }
        // 設備（呼び鈴・妨害を直す場所）
        boolean anyUrgent = false;
        for (ClientGame.MapFix f : ClientGame.fixes) {
            if (taskFloor(f.pos().getY(), fl) != floor) {
                anyUrgent |= urgent(f);
                continue;
            }
            float fx = ox + (f.pos().getX() + 0.5f - minX) * scale;
            float fz = oz + (f.pos().getZ() + 0.5f - minZ) * scale;
            if (urgent(f)) {
                anyUrgent = true;
                float r = 7f + pulse * 5f;
                Ui.disc(g, fx, fz, r, Ui.alpha(0xFFE53935, 0.35f * (1f - pulse) + 0.15f));
                Ui.disc(g, fx, fz, 5f, 0xFFE53935);
                Ui.sans(g, "!", fx, fz - 4, 0xFFFFFFFF, 0.8f, 0);
                Ui.sansShadow(g, fixName(f.type()) + "で直す", fx, fz - 17, 0xFFFFE08A, 0.8f, 0);
            } else if (f.type() == com.minpaku.game.MarkerType.BELL) {
                Ui.disc(g, fx, fz, 3.5f, 0xFFD4A017);
                Ui.disc(g, fx, fz, 1.5f, 0xFF5A3A00);
            } else {
                g.fill((int) fx - 2, (int) fz - 2, (int) fx + 2, (int) fz + 2, 0xFF6E6E6E);
            }
        }
        // 自分のいる場所（会議中は会議の前にいた場所）
        Vec3 me = me();
        if (me != null && taskFloor((int) Math.floor(me.y), fl) == floor) {
            float px = ox + (float) (me.x - minX) * scale;
            float pz = oz + (float) (me.z - minZ) * scale;
            if (live && net.minecraft.client.Minecraft.getInstance().player != null) {
                // 向いている方向の矢印（白いふちの赤い矢じり）
                float yaw = net.minecraft.client.Minecraft.getInstance().player.getYRot();
                arrow(g, px, pz, yaw, 9f, 0xFFFFFFFF);
                arrow(g, px, pz, yaw, 7f, 0xFFC8372D);
            } else {
                Ui.disc(g, px, pz, 7f, 0xFFFFFFFF);
                Ui.disc(g, px, pz, 5.5f, 0xFFC8372D);
                Ui.disc(g, px, pz, 2.5f, 0xFFFFFFFF);
            }
            Ui.sansShadow(g, "あなた", px, pz - 16, 0xFFFF8A80, 0.8f, 0);
        }
        g.disableScissor();
        // 凡例（小さい地図では省く）
        int ly = y + h - 18;
        if (overlay) {
            ly = -1000;
        }
        Ui.disc(g, x + 20, ly + 4, 3.5f, 0xFFFFC21F);
        Ui.sans(g, "自分のタスク", x + 27, ly, Ui.INK_SOFT, 0.75f, -1);
        Ui.disc(g, x + 100, ly + 4, 3.5f, 0xFF3FA34D);
        Ui.sans(g, "終わったタスク", x + 107, ly, Ui.INK_SOFT, 0.75f, -1);
        Ui.disc(g, x + 190, ly + 4, 4f, 0xFFC8372D);
        Ui.sans(g, live ? "いまの場所" : "会議の前にいた場所", x + 197, ly, Ui.INK_SOFT, 0.75f, -1);
        Ui.disc(g, x + 290, ly + 4, 3.5f, 0xFFD4A017);
        Ui.sans(g, "呼び鈴", x + 297, ly, Ui.INK_SOFT, 0.75f, -1);
        Ui.disc(g, x + 340, ly + 4, 4f, 0xFFE53935);
        Ui.sans(g, "妨害を直す場所", x + 347, ly, Ui.INK_SOFT, 0.75f, -1);
        if (anyUrgent) {
            Packets.State s = ClientGame.state;
            String what = s.blackout() ? "停電中：配電盤へ！" : "ガス漏れ：元栓とボイラーへ！";
            Ui.sans(g, what, x + w - 12, overlay ? y + h - 16 : y + h - 30, Ui.alpha(0xFFE53935, 0.7f + 0.3f * pulse), 0.85f, 1);
        }
    }

    /** 矢印（矢じりの形）。yaw はマイクラの向き（0 = 南）。地図は北が上 */
    private static void arrow(GuiGraphics g, float cx, float cy, float yaw, float size, int color) {
        g.pose().pushPose();
        g.pose().translate(cx, cy, 0);
        // 地図の上（-Z）が北。yaw 0（南）は下向き
        g.pose().mulPose(com.mojang.math.Axis.ZP.rotationDegrees(yaw + 180f));
        int s = Math.round(size);
        // 先（上）から下へ幅を広げ、うしろを少しへこませる
        for (int y = -s; y <= s; y++) {
            float t = (y + s) / (2f * s);
            int half = Math.round(t * size * 0.8f);
            if (y > s * 0.4f) {
                int notch = Math.round((y - s * 0.4f) / (s * 0.6f) * size * 0.5f);
                g.fill(-half, y, -notch, y + 1, color);
                g.fill(notch, y, half + 1, y + 1, color);
            } else {
                g.fill(-half, y, half + 1, y + 1, color);
            }
        }
        g.pose().popPose();
    }

    /** 部屋の種類ごとの色（見分けやすく） */
    private static int roomColor(String name, boolean open, int dx, int dz) {
        if (open) {
            return name.contains("池") || name.contains("湯") || name.contains("風呂") ? 0xFF9FD3E6 : 0xFFA9D48E;
        }
        if (Math.min(dx, dz) <= 3 || name.contains("廊下")) {
            return 0xFFD9C9A8;
        }
        if (name.contains("厨房") || name.contains("板場") || name.contains("台所") || name.contains("食")) {
            return 0xFFF5CFA0;
        }
        if (name.contains("浴") || name.contains("湯") || name.contains("風呂")) {
            return 0xFFB9E1F2;
        }
        if (name.contains("広間") || name.contains("会議") || name.contains("宴会")) {
            return 0xFFF4C3B8;
        }
        if (name.contains("機械") || name.contains("蔵") || name.contains("納屋") || name.contains("倉")) {
            return 0xFFCFCFCA;
        }
        if (name.contains("客室") || name.contains("布団") || name.contains("離れ")) {
            return 0xFFF7EBC6;
        }
        return 0xFFEADFC8;
    }

    /** ブロックの高さ（タスクの場所など）から階 */
    private static int taskFloor(int by, List<Integer> fl) {
        int best = 0;
        for (int i = 0; i < fl.size(); i++) {
            if (by >= fl.get(i) - 1) {
                best = i;
            }
        }
        return best;
    }

    /** クリック。閉じるなら true */
    public boolean click(double mx, double my) {
        if (inside(mx, my, x + w - 58, y + 8, 48, 16)) {
            return true;
        }
        List<Integer> fl = floors();
        for (int i = 0; i < fl.size(); i++) {
            if (inside(mx, my, x + 110 + i * 52, y + 8, 48, 16)) {
                floor = i;
                return false;
            }
        }
        return !inside(mx, my, x, y, w, h);
    }

    private static boolean inside(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }
}
