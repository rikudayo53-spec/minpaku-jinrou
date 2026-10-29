package com.minpaku.block;

import com.minpaku.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * 専用ブロック（家具）。見た目を描くほか、タスク中の動き（どこまで進んだか・最後に手を動かした時刻）を
 * まわりのプレイヤーに送る。
 */
public class MarkerBlockEntity extends BlockEntity {
    /** 作業の進み具合（0〜1） */
    public float work;
    /** 最後に作業した（手を動かした）時刻 */
    public long lastPulse = -10000;
    /** 作業を始めた時刻 */
    public long started = -10000;

    public MarkerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MARKER, pos, state);
    }

    /** サーバー：作業が少し進んだ（step = 進んだ割合）。まわりに知らせる */
    public void pulse(float step) {
        if (level == null || level.isClientSide()) {
            return;
        }
        long now = level.getGameTime();
        if (now - lastPulse > 60) {
            // しばらく誰も触っていなければ最初から
            work = 0;
            started = now;
        }
        work = Math.min(1f, work + Math.max(0f, step));
        lastPulse = now;
        setChanged();
        level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putFloat("work", work);
        tag.putLong("pulse", lastPulse);
        tag.putLong("started", started);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        work = tag.getFloat("work");
        lastPulse = tag.contains("pulse") ? tag.getLong("pulse") : -10000;
        started = tag.contains("started") ? tag.getLong("started") : -10000;
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveCustomOnly(registries);
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
