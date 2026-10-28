package com.minpaku.client;

import com.minpaku.entity.BotEntity;
import com.minpaku.game.Role;
import com.minpaku.network.Packets;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

/** 人狼のキル能力で狙う相手を選ぶ（照準の先の人、いなければ届く範囲でいちばん近い人） */
public final class KillTarget {
    private KillTarget() {
    }

    public static boolean canUse() {
        Packets.State s = ClientGame.state;
        Role role = ClientGame.role();
        Minecraft mc = Minecraft.getInstance();
        return role != null && role.canKill() && s.alive() && !s.inVent() && s.phase() == 1 && mc.player != null;
    }

    private static boolean valid(Minecraft mc, Entity e) {
        if (!(e instanceof Player || e instanceof BotEntity) || e == mc.player || !e.isAlive() || e.isSpectator()) {
            return false;
        }
        if (ClientGame.isMate(e.getUUID())) {
            return false;
        }
        return mc.player.distanceTo(e) <= ClientGame.state.killRange() + 0.6 && mc.player.hasLineOfSight(e);
    }

    @Nullable
    public static LivingEntity find(Minecraft mc) {
        if (!canUse() || mc.level == null) {
            return null;
        }
        if (mc.crosshairPickEntity instanceof LivingEntity le && valid(mc, le)) {
            return le;
        }
        LivingEntity best = null;
        double bestD = Double.MAX_VALUE;
        for (Entity e : mc.level.entitiesForRendering()) {
            if (e instanceof LivingEntity le && valid(mc, le)) {
                double d = mc.player.distanceToSqr(le);
                if (d < bestD) {
                    bestD = d;
                    best = le;
                }
            }
        }
        return best;
    }
}
