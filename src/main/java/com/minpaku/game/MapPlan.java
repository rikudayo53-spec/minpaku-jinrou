package com.minpaku.game;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

/**
 * 1つのマップの設計図。部屋（四角）を並べると、部屋と部屋のすき間が壁になり、
 * となり合う部屋には自動で出入り口があく。家具は furnish で置く。
 *
 * 座標: x = 0〜w、z = 0〜d（外壁を含む）。正面玄関は z = d 側。
 * floor 0 = 1階、1 = 2階。open の部屋は屋根も2階もない（庭・中庭）。
 */
public final class MapPlan {
    public record Room(String name, int floor, int x1, int z1, int x2, int z2, Block floorBlock, boolean open) {
        boolean has(int x, int z) {
            return x >= x1 && x <= x2 && z >= z1 && z <= z2;
        }
    }

    /** 階段：x1〜x2 の幅で、zStart から北（-Z）へ4段上る（1階 → 2階） */
    public record Stair(int x1, int x2, int zStart) {
    }

    public record Door(int floor, int x1, int z1, int x2, int z2) {
    }

    public final String id;
    public final String name;
    public final String players;
    public final int w;
    public final int d;
    /** 2階のある範囲（なければ null） */
    public int[] upper;
    public final List<Room> rooms = new ArrayList<>();
    public final List<Door> doors = new ArrayList<>();
    public final List<Stair> stairs = new ArrayList<>();
    public final Set<String> noDoor = new HashSet<>();
    public boolean autoDoors = true;
    public int entranceX = 1;
    public int spawnX;
    public int spawnZ;
    public Consumer<MapBuilder> furnish = b -> {
    };

    public MapPlan(String id, String name, String players, int w, int d) {
        this.id = id;
        this.name = name;
        this.players = players;
        this.w = w;
        this.d = d;
    }

    public MapPlan room(String name, int floor, int x1, int z1, int x2, int z2, Block floorBlock) {
        rooms.add(new Room(name, floor, x1, z1, x2, z2, floorBlock, false));
        return this;
    }

    /** 屋根のない部屋（庭・中庭） */
    public MapPlan garden(String name, int x1, int z1, int x2, int z2, Block floorBlock) {
        rooms.add(new Room(name, 0, x1, z1, x2, z2, floorBlock, true));
        return this;
    }

    public MapPlan door(int floor, int x1, int z1, int x2, int z2) {
        doors.add(new Door(floor, x1, z1, x2, z2));
        return this;
    }

    /** この2部屋のあいだには自動で出入り口をあけない */
    public MapPlan noDoor(String a, String b) {
        noDoor.add(a + "|" + b);
        noDoor.add(b + "|" + a);
        return this;
    }

    public boolean twoFloors() {
        return upper != null;
    }
}
