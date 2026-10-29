package com.minpaku.mixin.client;

import com.minpaku.client.MinpakuClient;
import com.minpaku.registry.ModItems;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.world.InteractionHand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** シェリフの銃を持っているときは、両手で前に構える（クロスボウを構えるときと同じ姿勢） */
@Mixin(PlayerRenderer.class)
public abstract class GunPoseMixin {
    @Inject(method = "getArmPose", at = @At("HEAD"), cancellable = true)
    private static void minpaku$gunPose(AbstractClientPlayer player, InteractionHand hand,
                                        CallbackInfoReturnable<HumanoidModel.ArmPose> cir) {
        if (player.getItemInHand(hand).is(ModItems.GUN) && MinpakuClient.roleItemVisible(ModItems.GUN)) {
            cir.setReturnValue(HumanoidModel.ArmPose.CROSSBOW_HOLD);
        }
    }
}
