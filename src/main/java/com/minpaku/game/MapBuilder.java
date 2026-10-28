package com.minpaku.game;

import com.minpaku.block.MarkerBlock;
import com.minpaku.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.SlabType;

import java.util.ArrayList;
import java.util.List;

/**
 * 基本マップ「和モダンの古民家民泊」を建てる。
 *
 * 間取り（家の中の座標。x = 0〜30、z = 0〜24、正面玄関は z = 24 側）
 *   1階: 台所 / 居間（囲炉裏・呼び鈴）/ 土間（配電盤）/ 廊下 / 風呂場 / 階段 / 玄関 / 中庭
 *   2階: 客室×4 / 廊下 / 事務室
 * プレイヤーの向いている方向に、家の正面を向けて建てる。
 */
public final class MapBuilder {
    // 家の中の高さ
    private static final int F1 = 0;   // 1階の床
    private static final int F2 = 5;   // 2階の床（= 1階の天井）
    private static final int ROOF = 10;

    private final ServerLevel level;
    private final BlockPos origin;
    private final Rotation rotation;
    private final List<BlockPos> fixups = new ArrayList<>();

    private MapBuilder(ServerLevel level, BlockPos origin, Rotation rotation) {
        this.level = level;
        this.origin = origin;
        this.rotation = rotation;
    }

    /** プレイヤーの目の前に建てる。玄関がプレイヤーの方を向く。 */
    public static BlockPos build(ServerPlayer player) {
        Direction facing = player.getDirection();
        // 家の「奥」（z = 0 側）が向こう側になるように回す
        Rotation rot = switch (facing) {
            case NORTH -> Rotation.NONE;
            case EAST -> Rotation.CLOCKWISE_90;
            case SOUTH -> Rotation.CLOCKWISE_180;
            default -> Rotation.COUNTERCLOCKWISE_90;
        };
        BlockPos front = player.blockPosition().relative(facing, 3);
        // 玄関（家の座標で x = 15, z = 24）がプレイヤーの3マス先に来るように原点を決める
        BlockPos offset = new BlockPos(-15, -1, -27).rotate(rot);
        BlockPos origin = front.offset(offset);
        MapBuilder b = new MapBuilder(player.serverLevel(), origin, rot);
        b.run();
        return b.world(15, F1 + 1, 21);
    }

    // ------------------------------------------------------------ 座標の変換と置く処理

    private BlockPos world(int x, int y, int z) {
        return origin.offset(new BlockPos(x, y, z).rotate(rotation));
    }

    private void set(int x, int y, int z, BlockState state) {
        BlockPos pos = world(x, y, z);
        level.setBlock(pos, state.rotate(rotation), Block.UPDATE_CLIENTS);
        Block b = state.getBlock();
        if (state.is(net.minecraft.tags.BlockTags.STAIRS) || b == Blocks.WHITE_STAINED_GLASS_PANE
                || b == Blocks.GLASS_PANE || state.is(net.minecraft.tags.BlockTags.FENCES)) {
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

    /** 専用ブロックを置く。facing は家の座標での正面の向き（NORTH = 家の奥向き） */
    private void marker(MarkerType type, int x, int y, int z, Direction facing) {
        set(x, y, z, ModBlocks.BLOCKS.get(type).defaultBlockState().setValue(MarkerBlock.FACING, facing));
    }

    private boolean isAir(int x, int y, int z) {
        return level.getBlockState(world(x, y, z)).isAir();
    }

    // ------------------------------------------------------------ 建てる

    private void run() {
        MapData.get(level).clearAll();
        clearSite();
        floors();
        outerWalls();
        innerWalls();
        stairs();
        roof();
        windows();
        doorways();
        furnish();
        lights();
        fixShapes();
        setupWorld();
    }

    private void clearSite() {
        BlockState air = Blocks.AIR.defaultBlockState();
        fill(-4, 0, -4, 34, ROOF + 4, 28, air);
        fill(-4, -1, -4, 34, -1, 28, Blocks.GRASS_BLOCK);
        fill(-4, -2, -4, 34, -3, 28, Blocks.DIRT);
        // 家のまわりの砂利道と生け垣
        fill(-2, -1, -2, 32, -1, 26, Blocks.GRAVEL);
        fill(14, -1, 25, 17, -1, 28, Blocks.STONE_BRICKS);
    }

    private void floors() {
        // 土台
        fill(0, F1, 0, 30, F1, 24, Blocks.STONE_BRICKS);
        fill(1, F1, 1, 9, F1, 9, Blocks.SMOOTH_STONE);          // 台所
        fill(11, F1, 1, 21, F1, 9, Blocks.BAMBOO_PLANKS);       // 居間（畳っぽく）
        fill(23, F1, 1, 29, F1, 9, Blocks.PACKED_MUD);          // 土間
        fill(1, F1, 11, 29, F1, 13, Blocks.OAK_PLANKS);         // 廊下
        fill(1, F1, 15, 7, F1, 23, Blocks.CALCITE);             // 風呂場
        fill(9, F1, 15, 11, F1, 23, Blocks.OAK_PLANKS);         // 階段
        fill(13, F1, 15, 17, F1, 19, Blocks.OAK_PLANKS);        // 玄関（上がりかまち）
        fill(13, F1, 20, 17, F1, 23, Blocks.POLISHED_ANDESITE); // 玄関（たたき）
        fill(19, F1, 15, 29, F1, 23, Blocks.GRASS_BLOCK);       // 中庭
        fill(19, F1, 15, 29, F1, 15, Blocks.OAK_PLANKS);        // 縁側

        // 2階の床（中庭と階段の吹き抜けは空ける）
        fill(1, F2, 1, 29, F2, 23, Blocks.SPRUCE_PLANKS);
        fill(1, F2, 1, 29, F2, 9, Blocks.BAMBOO_PLANKS);        // 客室1〜3
        fill(1, F2, 15, 7, F2, 23, Blocks.BAMBOO_PLANKS);       // 客室4
        fill(19, F2, 15, 29, F2, 23, Blocks.AIR);               // 中庭の上
        fill(9, F2, 18, 11, F2, 23, Blocks.AIR);                // 階段の吹き抜け
    }

    private void wallColumn(int x, int z, int base, boolean pillar) {
        if (pillar) {
            fill(x, base + 1, z, x, base + 4, z, Blocks.DARK_OAK_LOG);
        } else {
            set(x, base + 1, z, Blocks.SPRUCE_PLANKS);
            fill(x, base + 2, z, x, base + 4, z, Blocks.SMOOTH_QUARTZ);
        }
    }

    private void outerWalls() {
        for (int x = 0; x <= 30; x++) {
            for (int z = 0; z <= 24; z++) {
                boolean edge = x == 0 || x == 30 || z == 0 || z == 24;
                if (!edge) {
                    continue;
                }
                boolean pillar = (x % 5 == 0 && (z == 0 || z == 24)) || (z % 4 == 0 && (x == 0 || x == 30));
                wallColumn(x, z, F1, pillar);
                set(x, F2, z, Blocks.DARK_OAK_PLANKS);            // 1階と2階の間の梁
                boolean courtyardSide = x >= 18 && z >= 14;
                if (!courtyardSide) {
                    wallColumn(x, z, F2, pillar);
                }
            }
        }
        // 2階のうち、中庭に面した壁
        for (int z = 14; z <= 24; z++) {
            wallColumn(18, z, F2, z % 4 == 0 || z == 14 || z == 24);
        }
        for (int x = 18; x <= 30; x++) {
            wallColumn(x, 14, F2, x % 5 == 0 || x == 18 || x == 30);
        }
    }

    private void innerWall(int x1, int z1, int x2, int z2, int base) {
        for (int x = x1; x <= x2; x++) {
            for (int z = z1; z <= z2; z++) {
                boolean end = (x == x1 && z == z1) || (x == x2 && z == z2);
                if (end) {
                    fill(x, base + 1, z, x, base + 4, z, Blocks.STRIPPED_SPRUCE_LOG);
                } else {
                    fill(x, base + 1, z, x, base + 3, z, Blocks.SMOOTH_QUARTZ);
                    set(x, base + 4, z, Blocks.STRIPPED_SPRUCE_WOOD);   // 鴨居
                }
            }
        }
    }

    private void innerWalls() {
        // 1階
        innerWall(1, 10, 29, 10, F1);    // 北の部屋 | 廊下
        innerWall(1, 14, 29, 14, F1);    // 廊下 | 南の部屋
        innerWall(10, 1, 10, 9, F1);     // 台所 | 居間
        innerWall(22, 1, 22, 9, F1);     // 居間 | 土間
        innerWall(8, 15, 8, 23, F1);     // 風呂 | 階段
        innerWall(12, 15, 12, 23, F1);   // 階段 | 玄関
        innerWall(18, 15, 18, 23, F1);   // 玄関 | 中庭
        // 2階
        innerWall(1, 10, 29, 10, F2);
        innerWall(1, 14, 17, 14, F2);
        innerWall(10, 1, 10, 9, F2);     // 客室1 | 客室2
        innerWall(20, 1, 20, 9, F2);     // 客室2 | 客室3
        innerWall(8, 15, 8, 23, F2);     // 客室4 | 階段
        innerWall(12, 15, 12, 23, F2);   // 階段 | 事務室
    }

    private void doorway(int x1, int z1, int x2, int z2, int base) {
        fill(x1, base + 1, z1, x2, base + 3, z2, Blocks.AIR);
    }

    private void doorways() {
        // 1階
        doorway(4, 10, 5, 10, F1);       // 台所
        doorway(15, 10, 17, 10, F1);     // 居間
        doorway(25, 10, 26, 10, F1);     // 土間
        doorway(3, 14, 4, 14, F1);       // 風呂場
        doorway(9, 14, 11, 14, F1);      // 階段
        doorway(14, 14, 16, 14, F1);     // 玄関
        doorway(23, 14, 24, 14, F1);     // 中庭
        // 2階
        doorway(4, 10, 5, 10, F2);       // 客室1
        doorway(14, 10, 15, 10, F2);     // 客室2
        doorway(24, 10, 25, 10, F2);     // 客室3
        doorway(3, 14, 4, 14, F2);       // 客室4
        doorway(9, 14, 11, 14, F2);      // 階段
        doorway(14, 14, 15, 14, F2);     // 事務室
        // 正面玄関の引き戸（飾り。開けても外には石畳しかない）
        door(15, 24);
        door(16, 24);
    }

    private void door(int x, int z) {
        BlockState lower = Blocks.SPRUCE_DOOR.defaultBlockState()
                .setValue(DoorBlock.FACING, Direction.NORTH)
                .setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER);
        set(x, F1 + 1, z, lower);
        set(x, F1 + 2, z, lower.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER));
        set(x, F1 + 3, z, Blocks.DARK_OAK_PLANKS);
    }

    private void stairs() {
        // 階段：1階の廊下から入って奥（z = 22）まで進み、折り返して手前へ上る。
        // z = 21 → 18 で1段ずつ上がり、z = 15〜17 の踊り場（2階の床）に着く。
        BlockState step = Blocks.SPRUCE_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.NORTH);
        for (int i = 0; i < 4; i++) {
            int z = 21 - i;
            if (i > 0) {
                fill(9, F1 + 1, z, 11, F1 + i, z, Blocks.SPRUCE_PLANKS);
            }
            fill(9, F1 + 1 + i, z, 11, F1 + 1 + i, z, step);
        }
    }

    private void roof() {
        BlockState tiles = Blocks.DEEPSLATE_TILES.defaultBlockState();
        for (int x = -1; x <= 31; x++) {
            for (int z = -1; z <= 25; z++) {
                boolean courtyard = x >= 19 && z >= 15;
                if (courtyard) {
                    continue;
                }
                boolean eave = x == -1 || x == 31 || z == -1 || z == 25;
                if (eave) {
                    set(x, ROOF, z, Blocks.DEEPSLATE_TILE_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.BOTTOM));
                } else {
                    set(x, ROOF, z, tiles);
                }
            }
        }
        // 棟（屋根のてっぺん）
        for (int x = 0; x <= 30; x++) {
            set(x, ROOF + 1, 6, Blocks.DEEPSLATE_TILE_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.NORTH));
            set(x, ROOF + 1, 7, Blocks.DEEPSLATE_TILE_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.SOUTH));
        }
        // 中庭のまわりの1階のひさし
        for (int x = 19; x <= 29; x++) {
            set(x, F2, 24, Blocks.DEEPSLATE_TILE_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.BOTTOM));
        }
    }

    private void windows() {
        BlockState pane = Blocks.WHITE_STAINED_GLASS_PANE.defaultBlockState();
        BlockState glass = Blocks.GLASS_PANE.defaultBlockState();
        // 外壁の窓（柱と柱の間）
        for (int base : new int[]{F1, F2}) {
            for (int x = 1; x <= 29; x++) {
                if (x % 5 == 2 || x % 5 == 3) {
                    set(x, base + 2, 0, pane);
                    set(x, base + 3, 0, pane);
                    if (!(x >= 14 && x <= 17 && base == F1)) {
                        if (base == F1 || x < 18) {
                            set(x, base + 2, 24, pane);
                            set(x, base + 3, 24, pane);
                        }
                    }
                }
            }
            for (int z = 1; z <= 23; z++) {
                if (z % 4 == 2) {
                    set(0, base + 2, z, pane);
                    set(0, base + 3, z, pane);
                    if (base == F1 || z < 14) {
                        set(30, base + 2, z, pane);
                        set(30, base + 3, z, pane);
                    }
                }
            }
        }
        // 中庭が見える大きなガラス戸（廊下側と玄関側）
        for (int x = 19; x <= 29; x++) {
            if (x != 23 && x != 24) {
                set(x, F1 + 1, 14, glass);
                set(x, F1 + 2, 14, glass);
                set(x, F1 + 3, 14, glass);
            }
        }
        for (int z = 16; z <= 22; z++) {
            set(18, F1 + 2, z, glass);
            set(18, F1 + 3, z, glass);
        }
        // 2階から中庭を見下ろす窓
        for (int x = 20; x <= 28; x += 2) {
            set(x, F2 + 2, 14, glass);
            set(x, F2 + 3, 14, glass);
        }
        for (int z = 16; z <= 22; z += 2) {
            set(18, F2 + 2, z, glass);
            set(18, F2 + 3, z, glass);
        }
    }

    private void furnish() {
        int y1 = F1 + 1;
        int y2 = F2 + 1;
        BlockState redCarpet = Blocks.RED_CARPET.defaultBlockState();

        // ---- 玄関（スタート地点）
        marker(MarkerType.SPAWN, 15, y1, 21, Direction.NORTH);
        marker(MarkerType.SHOE_RACK, 13, y1, 22, Direction.EAST);
        marker(MarkerType.LEDGER, 17, y1, 17, Direction.WEST);
        set(17, y1, 19, Blocks.FLOWER_POT);
        set(13, y1, 16, Blocks.POTTED_BAMBOO);

        // ---- 居間（囲炉裏と呼び鈴）
        marker(MarkerType.BELL, 16, y1, 5, Direction.SOUTH);
        for (int i = 0; i < 12; i++) {
            double a = i / 12.0 * Math.PI * 2;
            int cx = 16 + (int) Math.round(Math.cos(a) * 2.5);
            int cz = 5 + (int) Math.round(Math.sin(a) * 2.5);
            if (isAir(cx, y1, cz)) {
                set(cx, y1, cz, redCarpet);       // 座布団
            }
        }
        marker(MarkerType.VASE, 16, y1, 1, Direction.SOUTH);         // 床の間
        set(14, y1, 1, Blocks.POTTED_AZALEA);
        set(18, y1, 1, Blocks.BOOKSHELF);
        set(12, y1, 8, Blocks.CHISELED_BOOKSHELF);
        marker(MarkerType.SHOJI, 20, y1, 9, Direction.NORTH);

        // ---- 台所
        marker(MarkerType.KAMADO, 2, y1, 1, Direction.SOUTH);
        marker(MarkerType.KAMADO, 4, y1, 1, Direction.SOUTH);
        marker(MarkerType.SINK, 7, y1, 1, Direction.SOUTH);
        marker(MarkerType.GAS_VALVE, 9, y1, 2, Direction.WEST);
        set(1, y1, 5, Blocks.BARREL);
        set(1, y1 + 1, 5, Blocks.BARREL);
        set(1, y1, 6, Blocks.BARREL);
        set(4, y1, 6, Blocks.SPRUCE_FENCE);
        set(4, y1 + 1, 6, Blocks.SPRUCE_PRESSURE_PLATE);
        marker(MarkerType.VENT, 8, y1, 8, Direction.NORTH);

        // ---- 土間
        marker(MarkerType.CHOPPING, 26, y1, 7, Direction.NORTH);
        marker(MarkerType.BREAKER, 28, y1 + 1, 1, Direction.SOUTH);
        fill(23, y1, 1, 24, y1 + 1, 2, Blocks.STRIPPED_OAK_LOG);    // 薪の山
        set(29, y1, 5, Blocks.WATER_CAULDRON.defaultBlockState()
                .setValue(net.minecraft.world.level.block.LayeredCauldronBlock.LEVEL, 3));
        set(29, y1, 8, Blocks.COMPOSTER);
        marker(MarkerType.VENT, 24, y1, 6, Direction.NORTH);

        // ---- 廊下
        marker(MarkerType.DUST, 6, y1, 12, Direction.NORTH);
        marker(MarkerType.DUST, 21, y1, 12, Direction.NORTH);
        set(1, y1, 12, Blocks.POTTED_FERN);
        set(29, y1, 12, Blocks.POTTED_FERN);

        // ---- 風呂場
        fill(2, y1, 18, 6, y1, 22, Blocks.STRIPPED_SPRUCE_LOG);      // 湯船のふち
        fill(3, y1, 19, 5, y1, 21, Blocks.WATER);
        marker(MarkerType.BATH, 7, y1, 20, Direction.WEST);
        marker(MarkerType.BOILER, 1, y1, 16, Direction.EAST);
        set(6, y1, 16, Blocks.SPRUCE_TRAPDOOR);
        marker(MarkerType.VENT, 4, y1, 16, Direction.NORTH);

        // ---- 中庭
        marker(MarkerType.FIELD, 21, y1, 19, Direction.NORTH);
        marker(MarkerType.FIELD, 22, y1, 19, Direction.NORTH);
        marker(MarkerType.FIELD, 21, y1, 20, Direction.NORTH);
        marker(MarkerType.LAUNDRY, 27, y1, 21, Direction.NORTH);
        set(28, y1, 17, Blocks.FLOWERING_AZALEA);
        set(25, y1, 22, Blocks.LANTERN);
        set(25, y1 - 1, 22, Blocks.STONE_BRICKS);
        set(24, y1, 18, Blocks.MOSSY_COBBLESTONE);
        set(20, y1, 23, Blocks.POPPY);
        set(29, y1, 23, Blocks.DANDELION);

        // ---- 2階 客室1〜3
        marker(MarkerType.FUTON, 3, y2, 3, Direction.SOUTH);
        marker(MarkerType.DUST, 7, y2, 7, Direction.NORTH);
        marker(MarkerType.SHOJI, 6, y2, 9, Direction.NORTH);
        marker(MarkerType.VENT, 8, y2, 2, Direction.NORTH);

        marker(MarkerType.FUTON, 13, y2, 3, Direction.SOUTH);
        marker(MarkerType.VASE, 18, y2, 1, Direction.SOUTH);
        marker(MarkerType.DUST, 17, y2, 7, Direction.NORTH);

        marker(MarkerType.FUTON, 23, y2, 3, Direction.SOUTH);
        marker(MarkerType.SHOJI, 27, y2, 9, Direction.NORTH);
        set(28, y2, 2, Blocks.POTTED_CHERRY_SAPLING);

        // ---- 2階 廊下
        marker(MarkerType.DUST, 16, y2, 12, Direction.NORTH);
        set(29, y2, 12, Blocks.POTTED_BAMBOO);

        // ---- 2階 客室4
        marker(MarkerType.FUTON, 3, y2, 21, Direction.NORTH);
        marker(MarkerType.SHOJI, 1, y2, 18, Direction.EAST);
        marker(MarkerType.VENT, 6, y2, 16, Direction.NORTH);

        // ---- 2階 事務室
        marker(MarkerType.WIRING, 15, y2, 23, Direction.NORTH);
        marker(MarkerType.LEDGER, 14, y2, 17, Direction.SOUTH);
        set(13, y2, 23, Blocks.BOOKSHELF);
        set(13, y2 + 1, 23, Blocks.BOOKSHELF);
        set(17, y2, 23, Blocks.BARREL);
        marker(MarkerType.VENT, 17, y2, 19, Direction.NORTH);
    }

    private void lights() {
        BlockState lantern = Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true);
        for (int x = 2; x <= 28; x += 4) {
            for (int z = 2; z <= 22; z += 4) {
                boolean courtyard = x >= 19 && z >= 15;
                if (!courtyard && isAir(x, F2 - 1, z) && !isAir(x, F2, z)) {
                    set(x, F2 - 1, z, lantern);
                }
                if (!courtyard && isAir(x, ROOF - 1, z) && !isAir(x, ROOF, z)) {
                    set(x, ROOF - 1, z, lantern);
                }
            }
        }
        // 廊下は明るめに
        for (int x = 3; x <= 27; x += 4) {
            if (isAir(x, F2 - 1, 12)) {
                set(x, F2 - 1, 12, lantern);
            }
            if (isAir(x, ROOF - 1, 12)) {
                set(x, ROOF - 1, 12, lantern);
            }
        }
    }

    /** ガラス板・柵・階段のつながりを直して、光の計算をやり直す */
    private void fixShapes() {
        for (BlockPos pos : fixups) {
            BlockState s = level.getBlockState(pos);
            BlockState updated = Block.updateFromNeighbourShapes(s, level, pos);
            if (updated != s) {
                level.setBlock(pos, updated, Block.UPDATE_CLIENTS);
            }
        }
    }

    private void setupWorld() {
        level.setDayTime(6000);
        level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, level.getServer());
        level.getGameRules().getRule(GameRules.RULE_WEATHER_CYCLE).set(false, level.getServer());
        level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false, level.getServer());
        level.setWeatherParameters(6000, 0, false, false);
    }
}
