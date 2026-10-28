package com.minpaku.game;

import com.minpaku.block.MarkerBlock;
import com.minpaku.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.SlabType;

import java.util.ArrayList;
import java.util.List;

/**
 * 基本マップ「和モダンの古民家民泊」を建てる（大きい版：45 × 41 マス、2階建て）。
 *
 * 家の座標: x = 0〜44、z = 0〜40。正面玄関は z = 40 側。
 *  1階  北: 台所 / 食堂 / 大広間（囲炉裏・呼び鈴）/ 土間（配電盤）
 *       中: 風呂場 / 階段 / 中庭（池・畑・物干し）/ 納戸
 *       南: トイレ / 洗面所 / 玄関 / 帳場 / 縁側の間 / 物置
 *  2階  客室 × 8 / 事務室 / 布団部屋 / ベランダ
 *  1階・2階とも、北と南に長い廊下がある。
 */
public final class MapBuilder {
    private static final int W = 44;      // x の最大
    private static final int D = 40;      // z の最大
    private static final int F1 = 0;
    private static final int F2 = 5;
    private static final int ROOF = 10;

    // 中庭（2階には床がない）
    private static final int CX1 = 15, CZ1 = 16, CX2 = 33, CZ2 = 25;

    private record RoomDef(String name, int floor, int x1, int z1, int x2, int z2, Block floorBlock) {
        boolean has(int x, int z) {
            return x >= x1 && x <= x2 && z >= z1 && z <= z2;
        }
    }

    private static final List<RoomDef> ROOMS = new ArrayList<>();

    private static void room(String name, int floor, int x1, int z1, int x2, int z2, Block floorBlock) {
        ROOMS.add(new RoomDef(name, floor, x1, z1, x2, z2, floorBlock));
    }

    static {
        // 1階
        room("台所", F1, 1, 1, 10, 10, Blocks.SMOOTH_STONE);
        room("食堂", F1, 12, 1, 21, 10, Blocks.OAK_PLANKS);
        room("大広間", F1, 23, 1, 34, 10, Blocks.BAMBOO_PLANKS);
        room("土間", F1, 36, 1, 43, 10, Blocks.PACKED_MUD);
        room("廊下", F1, 1, 12, 43, 14, Blocks.OAK_PLANKS);
        room("風呂場", F1, 1, 16, 9, 25, Blocks.CALCITE);
        room("階段", F1, 11, 16, 13, 25, Blocks.OAK_PLANKS);
        room("中庭", F1, CX1, CZ1, CX2, CZ2, Blocks.GRASS_BLOCK);
        room("納戸", F1, 35, 16, 43, 25, Blocks.SPRUCE_PLANKS);
        room("廊下", F1, 1, 27, 43, 29, Blocks.OAK_PLANKS);
        room("トイレ", F1, 1, 31, 5, 39, Blocks.CALCITE);
        room("洗面所", F1, 7, 31, 11, 39, Blocks.CALCITE);
        room("玄関", F1, 13, 31, 19, 39, Blocks.OAK_PLANKS);
        room("帳場", F1, 21, 31, 28, 39, Blocks.OAK_PLANKS);
        room("縁側の間", F1, 30, 31, 36, 39, Blocks.BAMBOO_PLANKS);
        room("物置", F1, 38, 31, 43, 39, Blocks.SPRUCE_PLANKS);
        // 2階
        room("客室・松", F2, 1, 1, 10, 10, Blocks.BAMBOO_PLANKS);
        room("客室・竹", F2, 12, 1, 21, 10, Blocks.BAMBOO_PLANKS);
        room("客室・梅", F2, 23, 1, 33, 10, Blocks.BAMBOO_PLANKS);
        room("客室・桜", F2, 35, 1, 43, 10, Blocks.BAMBOO_PLANKS);
        room("2階 廊下", F2, 1, 12, 43, 14, Blocks.SPRUCE_PLANKS);
        room("客室・菊", F2, 1, 16, 9, 25, Blocks.BAMBOO_PLANKS);
        room("階段", F2, 11, 16, 13, 25, Blocks.SPRUCE_PLANKS);
        room("ベランダ", F2, 35, 16, 43, 25, Blocks.SPRUCE_PLANKS);
        room("2階 廊下", F2, 1, 27, 43, 29, Blocks.SPRUCE_PLANKS);
        room("客室・藤", F2, 1, 31, 11, 39, Blocks.BAMBOO_PLANKS);
        room("事務室", F2, 13, 31, 20, 39, Blocks.SPRUCE_PLANKS);
        room("客室・萩", F2, 22, 31, 30, 39, Blocks.BAMBOO_PLANKS);
        room("布団部屋", F2, 32, 31, 43, 39, Blocks.SPRUCE_PLANKS);
    }

    private final ServerLevel level;
    private final BlockPos origin;
    private final Rotation rotation;
    private final List<BlockPos> fixups = new ArrayList<>();

    private MapBuilder(ServerLevel level, BlockPos origin, Rotation rotation) {
        this.level = level;
        this.origin = origin;
        this.rotation = rotation;
    }

    /** プレイヤーの目の前に建てる。玄関がプレイヤーの方を向く。スタート地点を返す */
    public static BlockPos build(ServerPlayer player) {
        Direction facing = player.getDirection();
        Rotation rot = switch (facing) {
            case NORTH -> Rotation.NONE;
            case EAST -> Rotation.CLOCKWISE_90;
            case SOUTH -> Rotation.CLOCKWISE_180;
            default -> Rotation.COUNTERCLOCKWISE_90;
        };
        BlockPos front = player.blockPosition().relative(facing, 3);
        BlockPos offset = new BlockPos(-16, -1, -(D + 3)).rotate(rot);
        MapBuilder b = new MapBuilder(player.serverLevel(), front.offset(offset), rot);
        b.run();
        return b.world(16, F1 + 1, 35);
    }

    // ------------------------------------------------------------ 座標と置く処理

    private BlockPos world(int x, int y, int z) {
        return origin.offset(new BlockPos(x, y, z).rotate(rotation));
    }

    private void set(int x, int y, int z, BlockState state) {
        BlockPos pos = world(x, y, z);
        level.setBlock(pos, state.rotate(rotation), Block.UPDATE_CLIENTS);
        Block b = state.getBlock();
        if (state.is(BlockTags.STAIRS) || b == Blocks.WHITE_STAINED_GLASS_PANE || b == Blocks.GLASS_PANE
                || state.is(BlockTags.FENCES)) {
            fixups.add(pos);
        }
    }

    private void set(int x, int y, int z, Block block) {
        set(x, y, z, block.defaultBlockState());
    }

    private void fill(int x1, int y1, int z1, int x2, int y2, int z2, BlockState state) {
        for (int x = Math.min(x1, x2); x <= Math.max(x1, x2); x++) {
            for (int y = Math.min(y1, y2); y <= Math.max(y1, y2); y++) {
                for (int z = Math.min(z1, z2); z <= Math.max(z1, z2); z++) {
                    set(x, y, z, state);
                }
            }
        }
    }

    private void fill(int x1, int y1, int z1, int x2, int y2, int z2, Block block) {
        fill(x1, y1, z1, x2, y2, z2, block.defaultBlockState());
    }

    private void marker(MarkerType type, int x, int y, int z, Direction facing) {
        set(x, y, z, ModBlocks.BLOCKS.get(type).defaultBlockState().setValue(MarkerBlock.FACING, facing));
    }

    private boolean isAir(int x, int y, int z) {
        return level.getBlockState(world(x, y, z)).isAir();
    }

    private static boolean courtyard(int x, int z) {
        return x >= CX1 && x <= CX2 && z >= CZ1 && z <= CZ2;
    }

    /** その階で、そこが部屋の中か */
    private static boolean inRoom(int floor, int x, int z) {
        for (RoomDef r : ROOMS) {
            if (r.floor == floor && r.has(x, z)) {
                return true;
            }
        }
        return false;
    }

    /** その階で、そこが壁になるか（家の中で部屋でない所）。2階の中庭の上は空 */
    private static boolean isWall(int floor, int x, int z) {
        if (x < 0 || x > W || z < 0 || z > D) {
            return false;
        }
        if (floor == F2 && courtyard(x, z)) {
            return false;
        }
        return !inRoom(floor, x, z);
    }

    // ------------------------------------------------------------ 建てる

    private void run() {
        MapData.get(level).clearAll();
        clearSite();
        floors();
        walls(F1);
        walls(F2);
        stairs();
        doorways();
        windows();
        roof();
        furnish();
        lights();
        fixShapes();
        registerRooms();
        setupWorld();
    }

    private void clearSite() {
        fill(-4, 0, -4, W + 4, ROOF + 4, D + 6, Blocks.AIR.defaultBlockState());
        fill(-4, -1, -4, W + 4, -1, D + 6, Blocks.GRASS_BLOCK);
        fill(-4, -3, -4, W + 4, -2, D + 6, Blocks.DIRT);
        fill(-2, -1, -2, W + 2, -1, D + 2, Blocks.GRAVEL);
        fill(15, -1, D + 1, 17, -1, D + 6, Blocks.STONE_BRICKS);
    }

    private void floors() {
        fill(0, F1, 0, W, F1, D, Blocks.STONE_BRICKS);
        fill(1, F2, 1, W - 1, F2, D - 1, Blocks.SPRUCE_PLANKS);
        for (RoomDef r : ROOMS) {
            fill(r.x1, r.floor, r.z1, r.x2, r.floor, r.z2, r.floorBlock);
        }
        // 玄関のたたき
        fill(13, F1, 36, 19, F1, 39, Blocks.POLISHED_ANDESITE);
        // 中庭の上（2階の床なし）と階段の吹き抜け
        fill(CX1, F2, CZ1, CX2, F2, CZ2, Blocks.AIR);
        fill(11, F2, 19, 13, F2, 25, Blocks.AIR);
        // 中庭の縁側
        fill(CX1, F1, CZ1, CX2, F1, CZ1, Blocks.OAK_PLANKS);
    }

    private void walls(int base) {
        for (int x = 0; x <= W; x++) {
            for (int z = 0; z <= D; z++) {
                if (!isWall(base, x, z)) {
                    continue;
                }
                boolean outer = x == 0 || x == W || z == 0 || z == D;
                boolean wx = isWall(base, x - 1, z) || isWall(base, x + 1, z);
                boolean wz = isWall(base, x, z - 1) || isWall(base, x, z + 1);
                boolean pillar = (wx && wz) || (outer && ((z == 0 || z == D) ? x % 4 == 0 : z % 4 == 0));
                if (pillar) {
                    fill(x, base + 1, z, x, base + 4, z, outer ? Blocks.DARK_OAK_LOG : Blocks.STRIPPED_SPRUCE_LOG);
                } else {
                    set(x, base + 1, z, Blocks.SPRUCE_PLANKS);
                    fill(x, base + 2, z, x, base + 3, z, Blocks.SMOOTH_QUARTZ);
                    set(x, base + 4, z, outer ? Blocks.DARK_OAK_PLANKS : Blocks.STRIPPED_SPRUCE_WOOD);
                }
                if (base == F1) {
                    set(x, F2, z, Blocks.DARK_OAK_PLANKS);   // 梁
                }
            }
        }
    }

    private void doorway(int floor, int x1, int z1, int x2, int z2) {
        fill(x1, floor + 1, z1, x2, floor + 3, z2, Blocks.AIR.defaultBlockState());
    }

    private void doorways() {
        // 1階：北の部屋
        doorway(F1, 5, 11, 6, 11);
        doorway(F1, 16, 11, 17, 11);
        doorway(F1, 11, 5, 11, 6);         // 台所 ↔ 食堂
        doorway(F1, 27, 11, 30, 11);       // 大広間（広め）
        doorway(F1, 39, 11, 40, 11);
        doorway(F1, 35, 6, 35, 7);         // 大広間 ↔ 土間
        // 1階：真ん中
        doorway(F1, 4, 15, 5, 15);
        doorway(F1, 11, 15, 13, 15);
        doorway(F1, 11, 26, 13, 26);
        doorway(F1, 19, 15, 20, 15);
        doorway(F1, 28, 15, 29, 15);
        doorway(F1, 24, 26, 25, 26);
        doorway(F1, 38, 15, 39, 15);
        doorway(F1, 38, 26, 39, 26);
        // 1階：南の部屋
        doorway(F1, 3, 30, 3, 30);
        doorway(F1, 8, 30, 9, 30);
        doorway(F1, 15, 30, 17, 30);
        doorway(F1, 24, 30, 25, 30);
        doorway(F1, 32, 30, 33, 30);
        doorway(F1, 40, 30, 41, 30);
        // 2階
        doorway(F2, 5, 11, 6, 11);
        doorway(F2, 16, 11, 17, 11);
        doorway(F2, 27, 11, 28, 11);
        doorway(F2, 39, 11, 40, 11);
        doorway(F2, 4, 15, 5, 15);
        doorway(F2, 4, 26, 5, 26);
        doorway(F2, 11, 15, 13, 15);
        doorway(F2, 38, 15, 39, 15);
        doorway(F2, 38, 26, 39, 26);
        doorway(F2, 5, 30, 6, 30);
        doorway(F2, 16, 30, 17, 30);
        doorway(F2, 25, 30, 26, 30);
        doorway(F2, 37, 30, 38, 30);
        // 正面玄関の引き戸
        for (int x = 15; x <= 17; x++) {
            if (x == 17) {
                fill(x, F1 + 1, D, x, F1 + 3, D, Blocks.DARK_OAK_PLANKS);
                continue;
            }
            BlockState lower = Blocks.SPRUCE_DOOR.defaultBlockState()
                    .setValue(DoorBlock.FACING, Direction.NORTH).setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER);
            set(x, F1 + 1, D, lower);
            set(x, F1 + 2, D, lower.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER));
            set(x, F1 + 3, D, Blocks.DARK_OAK_PLANKS);
        }
    }

    private void stairs() {
        // 階段：南から入って北へ上る（z = 22 → 19）。上りきると z = 16〜18 の踊り場（2階）
        BlockState step = Blocks.SPRUCE_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.NORTH);
        for (int i = 0; i < 4; i++) {
            int z = 22 - i;
            if (i > 0) {
                fill(11, F1 + 1, z, 13, F1 + i, z, Blocks.SPRUCE_PLANKS);
            }
            fill(11, F1 + 1 + i, z, 13, F1 + 1 + i, z, step);
        }
    }

    private void windows() {
        BlockState shoji = Blocks.WHITE_STAINED_GLASS_PANE.defaultBlockState();
        BlockState glass = Blocks.GLASS_PANE.defaultBlockState();
        for (int base : new int[]{F1, F2}) {
            for (int x = 1; x < W; x++) {
                if (x % 4 == 2) {
                    for (int z : new int[]{0, D}) {
                        if (isWall(base, x, z) && !(base == F1 && z == D && x >= 14 && x <= 18)) {
                            set(x, base + 2, z, shoji);
                            set(x, base + 3, z, shoji);
                        }
                    }
                }
            }
            for (int z = 1; z < D; z++) {
                if (z % 4 == 2) {
                    for (int x : new int[]{0, W}) {
                        if (isWall(base, x, z)) {
                            set(x, base + 2, z, shoji);
                            set(x, base + 3, z, shoji);
                        }
                    }
                }
            }
        }
        // 中庭のまわりはガラス戸（1階）とガラス窓（2階）
        for (int x = CX1; x <= CX2; x++) {
            for (int z : new int[]{CZ1 - 1, CZ2 + 1}) {
                if (isAir(x, F1 + 1, z)) {
                    continue;     // 出入り口
                }
                if (x % 3 != 0) {
                    fill(x, F1 + 1, z, x, F1 + 3, z, glass);
                }
                if (x % 2 == 0) {
                    set(x, F2 + 2, z, glass);
                    set(x, F2 + 3, z, glass);
                }
            }
        }
        for (int z = CZ1; z <= CZ2; z++) {
            for (int x : new int[]{CX1 - 1, CX2 + 1}) {
                if (z % 3 != 0) {
                    set(x, F1 + 2, z, glass);
                    set(x, F1 + 3, z, glass);
                }
                if (z % 2 == 0) {
                    set(x, F2 + 2, z, glass);
                    set(x, F2 + 3, z, glass);
                }
            }
        }
    }

    private void roof() {
        BlockState tiles = Blocks.DEEPSLATE_TILES.defaultBlockState();
        BlockState slab = Blocks.DEEPSLATE_TILE_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.BOTTOM);
        for (int x = -1; x <= W + 1; x++) {
            for (int z = -1; z <= D + 1; z++) {
                if (courtyard(x, z)) {
                    continue;
                }
                boolean eave = x == -1 || x == W + 1 || z == -1 || z == D + 1;
                set(x, ROOF, z, eave ? slab : tiles);
            }
        }
        // 中庭のふちのひさし
        for (int x = CX1; x <= CX2; x++) {
            set(x, ROOF, CZ1, slab);
            set(x, ROOF, CZ2, slab);
        }
        // 棟
        BlockState n = Blocks.DEEPSLATE_TILE_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.NORTH);
        BlockState s = Blocks.DEEPSLATE_TILE_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.SOUTH);
        for (int x = 0; x <= W; x++) {
            set(x, ROOF + 1, 6, n);
            set(x, ROOF + 1, 7, s);
            set(x, ROOF + 1, 33, n);
            set(x, ROOF + 1, 34, s);
        }
    }

    private void furnish() {
        int a = F1 + 1;
        int b = F2 + 1;
        BlockState zabuton = Blocks.RED_CARPET.defaultBlockState();

        // ---- 台所
        marker(MarkerType.KAMADO, 2, a, 1, Direction.SOUTH);
        marker(MarkerType.KAMADO, 4, a, 1, Direction.SOUTH);
        marker(MarkerType.SINK, 7, a, 1, Direction.SOUTH);
        marker(MarkerType.GAS_VALVE, 9, a, 3, Direction.WEST);
        fill(1, a, 6, 1, a + 1, 7, Blocks.BARREL);
        set(4, a, 6, Blocks.SPRUCE_FENCE);
        set(4, a + 1, 6, Blocks.SPRUCE_PRESSURE_PLATE);
        marker(MarkerType.VENT, 8, a, 9, Direction.NORTH);

        // ---- 食堂（長い座卓と座布団）
        for (int x = 14; x <= 19; x++) {
            set(x, a, 5, Blocks.SPRUCE_FENCE);
            set(x, a + 1, 5, Blocks.SPRUCE_PRESSURE_PLATE);
            set(x, a, 4, zabuton);
            set(x, a, 6, zabuton);
        }
        marker(MarkerType.VASE, 20, a, 1, Direction.SOUTH);
        marker(MarkerType.SHOJI, 13, a, 1, Direction.SOUTH);
        marker(MarkerType.DUST, 13, a, 8, Direction.NORTH);

        // ---- 大広間（囲炉裏と呼び鈴。会議はここ）
        marker(MarkerType.BELL, 28, a, 5, Direction.SOUTH);
        for (int i = 0; i < 16; i++) {
            double ang = i / 16.0 * Math.PI * 2;
            int cx = 28 + (int) Math.round(Math.cos(ang) * 2.6);
            int cz = 5 + (int) Math.round(Math.sin(ang) * 2.6);
            if (isAir(cx, a, cz)) {
                set(cx, a, cz, zabuton);
            }
        }
        marker(MarkerType.VASE, 28, a, 1, Direction.SOUTH);
        set(26, a, 1, Blocks.POTTED_AZALEA);
        set(30, a, 1, Blocks.BOOKSHELF);
        set(24, a, 1, Blocks.CHISELED_BOOKSHELF);
        marker(MarkerType.SHOJI, 24, a, 10, Direction.NORTH);
        marker(MarkerType.SHOJI, 33, a, 10, Direction.NORTH);

        // ---- 土間
        marker(MarkerType.CHOPPING, 39, a, 8, Direction.NORTH);
        marker(MarkerType.BREAKER, 42, a + 1, 1, Direction.SOUTH);
        fill(36, a, 1, 37, a + 1, 2, Blocks.STRIPPED_OAK_LOG);
        set(43, a, 5, Blocks.WATER_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, 3));
        set(43, a, 8, Blocks.COMPOSTER);
        marker(MarkerType.VENT, 37, a, 6, Direction.NORTH);

        // ---- 1階 廊下
        marker(MarkerType.DUST, 8, a, 13, Direction.NORTH);
        marker(MarkerType.DUST, 31, a, 13, Direction.NORTH);
        marker(MarkerType.DUST, 20, a, 28, Direction.NORTH);
        set(1, a, 13, Blocks.POTTED_FERN);
        set(43, a, 13, Blocks.POTTED_FERN);
        set(1, a, 28, Blocks.POTTED_BAMBOO);
        set(43, a, 28, Blocks.POTTED_BAMBOO);

        // ---- 風呂場
        fill(2, a, 20, 6, a, 24, Blocks.STRIPPED_SPRUCE_LOG);
        fill(3, a, 21, 5, a, 23, Blocks.WATER);
        marker(MarkerType.BATH, 8, a, 22, Direction.WEST);
        marker(MarkerType.BOILER, 1, a, 17, Direction.EAST);
        marker(MarkerType.VENT, 6, a, 17, Direction.NORTH);

        // ---- 中庭（池・畑・物干し）
        fill(21, F1, 19, 26, F1, 22, Blocks.WATER);
        fill(21, F1 - 1, 19, 26, F1 - 1, 22, Blocks.CLAY);
        for (int x = 20; x <= 27; x++) {
            set(x, F1, 18, Blocks.MOSSY_COBBLESTONE);
            set(x, F1, 23, Blocks.MOSSY_COBBLESTONE);
        }
        for (int z = 19; z <= 22; z++) {
            set(20, F1, z, Blocks.MOSSY_COBBLESTONE);
            set(27, F1, z, Blocks.MOSSY_COBBLESTONE);
        }
        set(23, F1 + 1, 20, Blocks.LILY_PAD);
        marker(MarkerType.FIELD, 16, a, 18, Direction.NORTH);
        marker(MarkerType.FIELD, 17, a, 18, Direction.NORTH);
        marker(MarkerType.FIELD, 16, a, 19, Direction.NORTH);
        marker(MarkerType.LAUNDRY, 31, a, 24, Direction.NORTH);
        set(32, a, 18, Blocks.FLOWERING_AZALEA);
        set(29, a, 21, Blocks.STONE_BRICK_WALL);
        set(29, a + 1, 21, Blocks.LANTERN);
        set(17, a, 24, Blocks.POPPY);
        set(18, a, 24, Blocks.DANDELION);
        set(33, a, 24, Blocks.CORNFLOWER);

        // ---- 納戸
        fill(43, a, 17, 43, a + 2, 19, Blocks.BARREL);
        set(42, a, 17, Blocks.CHEST);
        marker(MarkerType.DUST, 40, a, 21, Direction.NORTH);
        marker(MarkerType.VENT, 36, a, 24, Direction.NORTH);

        // ---- トイレ・洗面所
        set(3, a, 38, Blocks.CAULDRON);
        set(1, a, 32, Blocks.POTTED_FERN);
        marker(MarkerType.SINK, 9, a, 39, Direction.NORTH);
        marker(MarkerType.VASE, 7, a, 31, Direction.SOUTH);

        // ---- 玄関（スタート地点）
        marker(MarkerType.SPAWN, 16, a, 35, Direction.NORTH);
        marker(MarkerType.SHOE_RACK, 13, a, 38, Direction.EAST);
        set(19, a, 38, Blocks.POTTED_BAMBOO);
        set(19, a, 32, Blocks.FLOWER_POT);

        // ---- 帳場
        marker(MarkerType.LEDGER, 24, a, 34, Direction.SOUTH);
        fill(21, a, 39, 22, a + 1, 39, Blocks.BOOKSHELF);
        set(28, a, 31, Blocks.LECTERN);
        marker(MarkerType.VENT, 27, a, 38, Direction.NORTH);

        // ---- 縁側の間
        marker(MarkerType.SHOJI, 33, a, 39, Direction.NORTH);
        marker(MarkerType.DUST, 35, a, 35, Direction.NORTH);
        set(31, a, 35, Blocks.SPRUCE_TRAPDOOR);
        set(30, a, 31, Blocks.POTTED_CHERRY_SAPLING);

        // ---- 物置
        fill(43, a, 36, 43, a + 2, 39, Blocks.BARREL);
        marker(MarkerType.DUST, 40, a, 35, Direction.NORTH);
        marker(MarkerType.VENT, 41, a, 38, Direction.NORTH);

        // ================= 2階
        marker(MarkerType.FUTON, 3, b, 3, Direction.SOUTH);           // 松
        marker(MarkerType.DUST, 8, b, 8, Direction.NORTH);
        marker(MarkerType.VENT, 9, b, 2, Direction.NORTH);

        marker(MarkerType.FUTON, 14, b, 3, Direction.SOUTH);          // 竹
        marker(MarkerType.SHOJI, 18, b, 1, Direction.SOUTH);

        marker(MarkerType.FUTON, 25, b, 3, Direction.SOUTH);          // 梅
        marker(MarkerType.VASE, 32, b, 1, Direction.SOUTH);

        marker(MarkerType.FUTON, 37, b, 3, Direction.SOUTH);          // 桜
        marker(MarkerType.DUST, 42, b, 8, Direction.NORTH);

        marker(MarkerType.DUST, 25, b, 13, Direction.NORTH);          // 2階 廊下

        marker(MarkerType.FUTON, 3, b, 23, Direction.NORTH);          // 菊
        marker(MarkerType.SHOJI, 1, b, 19, Direction.EAST);
        marker(MarkerType.VENT, 8, b, 24, Direction.NORTH);

        marker(MarkerType.LAUNDRY, 39, b, 20, Direction.NORTH);       // ベランダ
        set(43, b, 17, Blocks.POTTED_AZALEA);

        marker(MarkerType.FUTON, 3, b, 37, Direction.NORTH);          // 藤
        marker(MarkerType.DUST, 9, b, 33, Direction.NORTH);

        marker(MarkerType.WIRING, 16, b, 39, Direction.NORTH);        // 事務室
        marker(MarkerType.LEDGER, 14, b, 33, Direction.SOUTH);
        fill(13, b, 39, 13, b + 1, 39, Blocks.BOOKSHELF);
        set(20, b, 39, Blocks.BARREL);
        marker(MarkerType.VENT, 19, b, 36, Direction.NORTH);

        marker(MarkerType.FUTON, 24, b, 37, Direction.NORTH);         // 萩
        marker(MarkerType.SHOJI, 29, b, 39, Direction.NORTH);

        marker(MarkerType.FUTON, 34, b, 37, Direction.NORTH);         // 布団部屋
        fill(41, b, 36, 43, b + 1, 39, Blocks.WHITE_WOOL);
        fill(41, b, 32, 43, b, 34, Blocks.BLUE_WOOL);
        marker(MarkerType.VENT, 38, b, 33, Direction.NORTH);
    }

    private void lights() {
        BlockState lantern = Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true);
        for (RoomDef r : ROOMS) {
            if (r.name.equals("中庭")) {
                continue;
            }
            int ceil = r.floor == F1 ? F2 : ROOF;
            for (int x = r.x1 + 1; x <= r.x2 - 1; x += 4) {
                for (int z = r.z1 + 1; z <= r.z2 - 1; z += 4) {
                    if (isAir(x, ceil - 1, z) && !isAir(x, ceil, z)) {
                        set(x, ceil - 1, z, lantern);
                    }
                }
            }
        }
    }

    private void fixShapes() {
        for (BlockPos pos : fixups) {
            BlockState s = level.getBlockState(pos);
            BlockState updated = Block.updateFromNeighbourShapes(s, level, pos);
            if (updated != s) {
                level.setBlock(pos, updated, Block.UPDATE_CLIENTS);
            }
        }
    }

    /** 部屋の名前と範囲を登録（画面の「現在地」やタスクの場所の表示に使う） */
    private void registerRooms() {
        List<MapData.Room> list = new ArrayList<>();
        for (RoomDef r : ROOMS) {
            int top = r.name.equals("中庭") ? ROOF : r.floor + 4;
            BlockPos p1 = world(r.x1, r.floor, r.z1);
            BlockPos p2 = world(r.x2, top, r.z2);
            list.add(new MapData.Room(r.name,
                    Math.min(p1.getX(), p2.getX()), Math.min(p1.getY(), p2.getY()), Math.min(p1.getZ(), p2.getZ()),
                    Math.max(p1.getX(), p2.getX()), Math.max(p1.getY(), p2.getY()), Math.max(p1.getZ(), p2.getZ())));
        }
        MapData.get(level).setRooms(list);
    }

    private void setupWorld() {
        level.setDayTime(6000);
        level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, level.getServer());
        level.getGameRules().getRule(GameRules.RULE_WEATHER_CYCLE).set(false, level.getServer());
        level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false, level.getServer());
        level.setWeatherParameters(6000, 0, false, false);
    }
}
