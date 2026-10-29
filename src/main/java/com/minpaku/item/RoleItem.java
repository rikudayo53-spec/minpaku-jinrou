package com.minpaku.item;

import com.minpaku.game.GameManager;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * 役職の道具。
 *   CRYSTAL  … 占いの水晶（占い師）: 近くの人に使うと人狼か分かる
 *   CHARM    … 守りのお守り（騎士）: 人に使うと次の会議まで守る
 *   TALISMAN … 妨害のお札（人狼）: 右クリックで妨害メニュー
 *   KEY      … 管理人の鍵: 右クリックで設定画面
 *   LENS     … 虫めがね（探偵）: 死体に使うと犯人の役職が分かる
 *   KEYRING  … 鍵束（もう使わない）
 *   GUN      … 短筒（シェリフ）: 右クリックで見ている相手を撃つ
 *   HANDBELL … 手持ちの呼び鈴（ナイスボタナー）: 右クリックでどこからでも緊急会議
 *   ONIGIRI  … おにぎり（板前）: 人に使うと1回だけキルを耐える
 */
public class RoleItem extends Item {
    public enum Kind { CRYSTAL, CHARM, TALISMAN, KEY, LENS, KEYRING, ONIGIRI, GUN, HANDBELL }

    private final Kind kind;

    public RoleItem(Properties properties, Kind kind) {
        super(properties);
        this.kind = kind;
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (kind != Kind.CRYSTAL && kind != Kind.CHARM && kind != Kind.ONIGIRI) {
            return InteractionResult.PASS;
        }
        if (player instanceof ServerPlayer sp) {
            GameManager gm = GameManager.get(sp.server);
            if (kind == Kind.CRYSTAL) {
                gm.useSeer(sp, target);
            } else if (kind == Kind.CHARM) {
                gm.useKnight(sp, target);
            } else {
                gm.useItamae(sp, target);
            }
        }
        return InteractionResult.sidedSuccess(player.level().isClientSide());
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (kind == Kind.KEY) {
            if (level.isClientSide()) {
                ClientBridge.openSettings.run();
            } else if (player instanceof ServerPlayer sp) {
                GameManager.get(sp.server).sendSettings(sp);
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
        }
        if (kind == Kind.GUN) {
            if (player instanceof ServerPlayer sp) {
                GameManager.get(sp.server).useSheriff(sp);
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
        }
        if (kind == Kind.HANDBELL) {
            if (player instanceof ServerPlayer sp) {
                GameManager.get(sp.server).useButtoner(sp);
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
        }
        if (kind == Kind.TALISMAN) {
            if (level.isClientSide()) {
                ClientBridge.openSabotage.run();
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
        }
        return InteractionResultHolder.pass(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        String text = switch (kind) {
            case CRYSTAL -> "近くの人に右クリック: 人狼かどうか占う";
            case CHARM -> "人に右クリック: 次の会議までキルから守る";
            case TALISMAN -> "右クリック: 停電・ガス漏れを起こす";
            case KEY -> "右クリック: ゲームの設定・開始";
            case LENS -> "死体に右クリック: 犯人の役職が分かる";
            case KEYRING -> "（使われていません）";
            case GUN -> "右クリック: 見ている相手を撃つ（村人を撃つと自分が倒れる）";
            case HANDBELL -> "右クリック: どこからでも緊急会議を開く";
            case ONIGIRI -> "人に右クリック: 1回だけキルを耐える";
        };
        tooltip.add(Component.literal(text).withStyle(ChatFormatting.GRAY));
    }
}
