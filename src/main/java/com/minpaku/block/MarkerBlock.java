package com.minpaku.block;

import com.minpaku.game.GameManager;
import com.minpaku.game.MapData;
import com.minpaku.game.MarkerType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.List;

/**
 * マップに置く専用ブロック（呼び鈴・ベント・タスクなど）。
 * 置くとマップに登録され、壊すと登録が消える。
 */
public class MarkerBlock extends Block {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    private final MarkerType type;
    private final VoxelShape shape;

    public MarkerBlock(Properties properties, MarkerType type) {
        super(properties);
        this.type = type;
        this.shape = shapeFor(type);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    public MarkerType type() {
        return type;
    }

    private static VoxelShape shapeFor(MarkerType type) {
        return switch (type) {
            case SPAWN, DUST -> Block.box(0, 0, 0, 16, 1.5, 16);
            case VENT -> Block.box(0, 0, 0, 16, 1, 16);
            case BELL -> Block.box(3, 0, 3, 13, 9, 13);
            case FIELD -> Block.box(0, 0, 0, 16, 7, 16);
            case FUTON -> Block.box(0, 0, 0, 16, 7, 16);
            case LEDGER -> Block.box(1, 0, 1, 15, 12, 15);
            case CHOPPING -> Block.box(2, 0, 2, 14, 10, 14);
            case GAS_VALVE -> Block.box(4, 0, 4, 12, 12, 12);
            default -> Block.box(1, 0, 1, 15, 16, 15);
        };
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite());
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return shape;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        // 床に置くもの（マット・ベント・ほこり）は踏んでも引っかからない
        if (type == MarkerType.SPAWN || type == MarkerType.VENT || type == MarkerType.DUST) {
            return Block.box(0, 0, 0, 16, 0.5, 16);
        }
        return shape;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                               BlockHitResult hit) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (player instanceof ServerPlayer sp) {
            return GameManager.get(sp.server).onUseMarker(sp, pos, type);
        }
        return InteractionResult.PASS;
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean moved) {
        super.onPlace(state, level, pos, oldState, moved);
        if (level instanceof ServerLevel sl && !oldState.is(this)) {
            MapData.get(sl).add(type, pos);
        }
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
        if (level instanceof ServerLevel sl && !newState.is(this)) {
            MapData.get(sl).remove(type, pos);
        }
        super.onRemove(state, level, pos, newState, moved);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal(type.taskName != null ? "タスク: " + type.taskName : type.label)
                .withStyle(net.minecraft.ChatFormatting.GRAY));
    }
}
