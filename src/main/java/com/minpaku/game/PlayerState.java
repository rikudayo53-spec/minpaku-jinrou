package com.minpaku.game;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** ゲームに参加している1人ぶんの状態（サーバー側） */
public class PlayerState {
    public static class TaskEntry {
        public final BlockPos pos;
        public final MarkerType type;
        public boolean done;
        public int progress;

        public TaskEntry(BlockPos pos, MarkerType type) {
            this.pos = pos;
            this.type = type;
        }
    }

    public final UUID id;
    public final String name;
    public Role role = Role.VILLAGER;
    public boolean alive = true;
    public boolean left;
    /** ボットかどうか */
    public boolean bot;
    /** キルした回数（結果画面用） */
    public int kills;
    public final List<TaskEntry> tasks = new ArrayList<>();
    public int killCooldown;
    public int seerLeft;
    public int knightLeft;
    public int emergencyLeft;
    public boolean protectedByKnight;
    @Nullable
    public BlockPos vent;
    public long lastClickTick;
    public GameType previousMode = GameType.SURVIVAL;
    @Nullable
    public Vec3 seat;

    public PlayerState(UUID id, String name) {
        this.id = id;
        this.name = name;
    }

    public boolean tasksCount() {
        return role.hasRealTasks();
    }
}
