package com.minpaku.game;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.LongArrayTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * マップに置かれた専用ブロックの位置と、ゲームの設定をワールドに保存する。
 */
public class MapData extends SavedData {
    private static final String NAME = "minpaku_map";

    private final Map<MarkerType, Set<Long>> markers = new EnumMap<>(MarkerType.class);

    /** 部屋（名前と範囲）。基本マップを建てたときに登録される */
    public record Room(String name, int x1, int y1, int z1, int x2, int y2, int z2) {
        public boolean contains(BlockPos p) {
            return p.getX() >= x1 && p.getX() <= x2 && p.getY() >= y1 && p.getY() <= y2
                    && p.getZ() >= z1 && p.getZ() <= z2;
        }
    }

    private final List<Room> rooms = new ArrayList<>();

    public void setRooms(List<Room> list) {
        rooms.clear();
        rooms.addAll(list);
        setDirty();
    }

    /** その場所の部屋の名前（なければ空文字） */
    public String roomAt(BlockPos pos) {
        for (Room r : rooms) {
            if (r.contains(pos)) {
                return r.name();
            }
        }
        return "";
    }
    private final GameSettings settings = new GameSettings();

    public static MapData get(ServerLevel level) {
        ServerLevel overworld = level.getServer().overworld();
        return overworld.getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(MapData::new, MapData::load, null), NAME);
    }

    public GameSettings settings() {
        return settings;
    }

    public void add(MarkerType type, BlockPos pos) {
        if (markers.computeIfAbsent(type, t -> new LinkedHashSet<>()).add(pos.asLong())) {
            setDirty();
        }
    }

    public void remove(MarkerType type, BlockPos pos) {
        Set<Long> set = markers.get(type);
        if (set != null && set.remove(pos.asLong())) {
            setDirty();
        }
    }

    public List<BlockPos> get(MarkerType type) {
        List<BlockPos> out = new ArrayList<>();
        Set<Long> set = markers.get(type);
        if (set != null) {
            for (long l : set) {
                out.add(BlockPos.of(l));
            }
        }
        return out;
    }

    public int count(MarkerType type) {
        Set<Long> set = markers.get(type);
        return set == null ? 0 : set.size();
    }

    public void clearAll() {
        markers.clear();
        setDirty();
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        CompoundTag m = new CompoundTag();
        markers.forEach((type, set) -> {
            long[] arr = new long[set.size()];
            int i = 0;
            for (long l : set) {
                arr[i++] = l;
            }
            m.put(type.name(), new LongArrayTag(arr));
        });
        tag.put("markers", m);
        tag.put("settings", settings.save());
        net.minecraft.nbt.ListTag list = new net.minecraft.nbt.ListTag();
        for (Room r : rooms) {
            CompoundTag t = new CompoundTag();
            t.putString("name", r.name());
            t.putIntArray("box", new int[]{r.x1(), r.y1(), r.z1(), r.x2(), r.y2(), r.z2()});
            list.add(t);
        }
        tag.put("rooms", list);
        return tag;
    }

    public static MapData load(CompoundTag tag, HolderLookup.Provider registries) {
        MapData data = new MapData();
        CompoundTag m = tag.getCompound("markers");
        for (MarkerType type : MarkerType.values()) {
            if (m.contains(type.name())) {
                Set<Long> set = new LinkedHashSet<>();
                for (long l : m.getLongArray(type.name())) {
                    set.add(l);
                }
                data.markers.put(type, set);
            }
        }
        data.settings.load(tag.getCompound("settings"));
        net.minecraft.nbt.ListTag list = tag.getList("rooms", net.minecraft.nbt.Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag t = list.getCompound(i);
            int[] b = t.getIntArray("box");
            if (b.length == 6) {
                data.rooms.add(new Room(t.getString("name"), b[0], b[1], b[2], b[3], b[4], b[5]));
            }
        }
        return data;
    }
}
