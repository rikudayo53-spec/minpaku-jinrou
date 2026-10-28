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
        return data;
    }
}
