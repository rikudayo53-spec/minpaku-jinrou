package com.minpaku.entity;

import com.minpaku.game.GameManager;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;

/**
 * ソロ用のボット。プレイヤーと同じ見た目で、ゲームに参加する。
 * 動きは GameManager / BotBrain が決める（自分では何も考えない）。
 */
public class BotEntity extends PathfinderMob {

    public BotEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        setPersistenceRequired();
        setCustomNameVisible(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0)
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.FOLLOW_RANGE, 80.0);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    public void tick() {
        super.tick();
        // ゲームに参加していないボット（セーブから戻ったものなど）は消す
        if (!level().isClientSide() && tickCount > 40 && tickCount % 20 == 0) {
            GameManager gm = GameManager.current();
            if (gm == null || !gm.isBot(getUUID())) {
                discard();
            }
        }
    }
}
