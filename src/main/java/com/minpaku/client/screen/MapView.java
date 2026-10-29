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
        w = Math.min(sw - 30, 520);
        h = Math.min(sh - 30, 330);
        x = (sw - w) / 2;
        y = (sh - h) / 2;
        g.fill(0, 0, sw, sh, 0xA0000000);
        Ui.sprite(g, "panel", x, y, w, h);
        Ui.serif(g, "見取り図", x + 12, y + 8, Ui.INK, 1.4f, -1);
        boolean hc = inside(mouseX, mouseY, x + w - 58, y + 8, 48, 16);
        Ui.button(g, x + w - 58, y + 8, 48, 16, "閉じる", false, hc, true, 0.85f);
        List<Integer> fl = floors();
        if (ClientGame.rooms.isEmpty()) {
            Ui.sans(g, "見取り図がありません（基本マップを建てると出ます）", x + w / 2f, y + h / 2f, Ui.INK_SOFT, 1f, 0);
            return;
        }
        floor = Math.min(floor, fl.size() - 1);
        // 階のボタン
        for (int i = 0; i < fl.size(); i++) {
            boolean hv = inside(mouseX, mouseY, x + 110 + i * 52, y + 8, 48, 16);
            Ui.button(g, x + 110 + i * 52, y + 8, 48, 16, (i + 1) + "階", i == floor, hv, true, 0.85f);
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
        int ah = h - 56;
        float scale = Math.min(aw / (float) Math.max(1, maxX - minX), ah / (float) Math.max(1, maxZ - minZ));
        float ox = ax + (aw - (maxX - minX) * scale) / 2f;
        float oz = ay + (ah - (maxZ - minZ) * scale) / 2f;
        g.fill(ax, ay, ax + aw, ay + ah, 0xFFE9DFC6);
        // 部屋
        for (ClientGame.MapRoom r : ClientGame.rooms) {
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
            int fillC = open ? 0xFF9CC48A : 0xFFF4E7C4;
            if (dim) {
                fillC = 0xFFCFD8C4;
            }
            g.fill(rx1, rz1, rx2, rz2, 0xFF5A4632);
            g.fill(rx1 + 1, rz1 + 1, rx2 - 1, rz2 - 1, fillC);
            if (!dim) {
                float cx = (rx1 + rx2) / 2f;
                float cz = (rz1 + rz2) / 2f - 3;
                Ui.fitSans(g, r.name(), cx, cz, Math.max(10, rx2 - rx1 - 4), open ? 0xFF2E5A22 : Ui.INK, 0.7f, 0);
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
            Ui.disc(g, px, pz, 3.5f, t.done() ? 0xFF3FA34D : 0xFFFFC21F);
            Ui.disc(g, px, pz, 1.6f, 0xFF2B1D12);
        }
        // 会議の前にいた場所
        Vec3 me = ClientGame.lastPlayPos;
        if (me != null && taskFloor((int) Math.floor(me.y), fl) == floor) {
            float px = ox + (float) (me.x - minX) * scale;
            float pz = oz + (float) (me.z - minZ) * scale;
            Ui.disc(g, px, pz, 5f, 0xFFC8372D);
            Ui.disc(g, px, pz, 2.5f, 0xFFFFFFFF);
            Ui.sans(g, "あなた", px, pz - 14, 0xFFC8372D, 0.75f, 0);
        }
        // 凡例
        int ly = y + h - 18;
        Ui.disc(g, x + 20, ly + 4, 3.5f, 0xFFFFC21F);
        Ui.sans(g, "自分のタスク", x + 27, ly, Ui.INK_SOFT, 0.75f, -1);
        Ui.disc(g, x + 100, ly + 4, 3.5f, 0xFF3FA34D);
        Ui.sans(g, "終わったタスク", x + 107, ly, Ui.INK_SOFT, 0.75f, -1);
        Ui.disc(g, x + 190, ly + 4, 4f, 0xFFC8372D);
        Ui.sans(g, "会議の前にいた場所", x + 197, ly, Ui.INK_SOFT, 0.75f, -1);
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
