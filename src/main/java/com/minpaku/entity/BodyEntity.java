package com.minpaku.entity;

import com.minpaku.game.GameManager;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.Optional;
import java.util.UUID;

/**
 * キルされた人の死体。右クリックで通報（会議が始まる）。
 */
public class BodyEntity extends Entity {
    private static final EntityDataAccessor<Optional<UUID>> OWNER =
            SynchedEntityData.defineId(BodyEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<String> OWNER_NAME =
            SynchedEntityData.defineId(BodyEntity.class, EntityDataSerializers.STRING);

    public BodyEntity(EntityType<? extends BodyEntity> type, Level level) {
        super(type, level);
    }

    public void setOwner(UUID uuid, String name) {
        entityData.set(OWNER, Optional.of(uuid));
        entityData.set(OWNER_NAME, name);
    }

    public Optional<UUID> owner() {
        return entityData.get(OWNER);
    }

    public String ownerName() {
        return entityData.get(OWNER_NAME);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(OWNER, Optional.empty());
        builder.define(OWNER_NAME, "");
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return false;
    }

    /** 犯人の役職（探偵が調べる。サーバーだけ） */
    public String killerRole = "";

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        if (player instanceof ServerPlayer sp) {
            if (sp.getItemInHand(hand).is(com.minpaku.registry.ModItems.LENS)) {
                GameManager.get(sp.server).useDetective(sp, this);
            } else {
                GameManager.get(sp.server).reportBody(sp, this);
            }
        }
        return InteractionResult.sidedSuccess(level().isClientSide());
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide() && tickCount % 20 == 0 && owner().isEmpty()) {
            discard();
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        // 死体の持ち主はセーブしない → 読み込み後の tick で消える
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
    }
}
