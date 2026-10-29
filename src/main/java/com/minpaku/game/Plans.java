package com.minpaku.game;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/**
 * 基本マップ6種類（小さい順）。管理人の帳簿の「マップ」タブで選んで建てる。
 *
 * ★ マップを増やすときは、ここに MapPlan を1つ足して ALL に入れる。
 */
public final class Plans {
    private Plans() {
    }

    public static final MapPlan MACHIYA = machiya();
    public static final MapPlan HIRAYA = hiraya();
    public static final MapPlan KOMINKA = kominka();
    public static final MapPlan YASHIKI = yashiki();
    public static final MapPlan RYOKAN = ryokan();
    public static final MapPlan SHUKUBO = shukubo();

    public static final List<MapPlan> ALL = List.of(MACHIYA, HIRAYA, KOMINKA, YASHIKI, RYOKAN, SHUKUBO);

    /** 設定の値（1〜6）からマップ */
    public static MapPlan byIndex(int i) {
        return ALL.get(Math.max(0, Math.min(ALL.size() - 1, i - 1)));
    }

    public static String[] labels() {
        String[] s = new String[ALL.size()];
        for (int i = 0; i < s.length; i++) {
            s[i] = ALL.get(i).name + "（" + ALL.get(i).players + "）";
        }
        return s;
    }

    // ================================================================ 極小：町家
    /** 細長い京町家。通り庭（土間）にそって部屋が並ぶ。16 × 36、平屋 */
    private static MapPlan machiya() {
        MapPlan p = new MapPlan("machiya", "町家", "2〜4人", 16, 36)
                .room("風呂場", 0, 1, 1, 5, 6, Blocks.CALCITE)
                .room("洗面所", 0, 7, 1, 10, 6, Blocks.CALCITE)
                .room("奥の間", 0, 1, 8, 10, 13, Blocks.BAMBOO_PLANKS)
                .garden("坪庭", 1, 15, 10, 19, Blocks.MOSS_BLOCK)
                .room("中の間", 0, 1, 21, 10, 27, Blocks.BAMBOO_PLANKS)
                .room("店の間", 0, 1, 29, 10, 35, Blocks.OAK_PLANKS)
                .room("台所", 0, 12, 1, 15, 10, Blocks.SMOOTH_STONE)
                .room("通り庭", 0, 12, 12, 15, 35, Blocks.PACKED_MUD);
        p.entranceX = 13;
        p.spawnX = 13;
        p.spawnZ = 33;
        p.furnish = m -> {
            int a = 1;
            m.marker(MarkerType.SPAWN, 13, a, 33, Direction.NORTH);
            m.marker(MarkerType.SHOE_RACK, 15, a, 32, Direction.WEST);
            // 台所
            m.marker(MarkerType.KAMADO, 13, a, 1, Direction.SOUTH);
            m.marker(MarkerType.SINK, 15, a, 5, Direction.WEST);
            m.marker(MarkerType.GAS_VALVE, 12, a, 8, Direction.EAST);
            m.set(15, a, 9, Blocks.BARREL);
            // 風呂・洗面
            m.marker(MarkerType.BATH, 3, a, 2, Direction.SOUTH);
            m.marker(MarkerType.BOILER, 1, a, 6, Direction.EAST);
            m.marker(MarkerType.VASE, 9, a, 1, Direction.SOUTH);
            m.marker(MarkerType.VENT, 9, a, 5, Direction.NORTH);
            // 奥の間
            m.marker(MarkerType.FUTON, 3, a, 9, Direction.SOUTH);
            m.marker(MarkerType.SHOJI, 10, a, 9, Direction.WEST);
            m.marker(MarkerType.DUST, 7, a, 12, Direction.NORTH);
            m.marker(MarkerType.VENT, 1, a, 12, Direction.NORTH);
            // 坪庭
            m.marker(MarkerType.FIELD, 2, a, 16, Direction.NORTH);
            m.marker(MarkerType.LAUNDRY, 7, a, 18, Direction.NORTH);
            m.set(10, a, 15, Blocks.STONE_BRICK_WALL);
            m.set(10, a + 1, 15, Blocks.LANTERN);
            m.set(5, a, 15, Blocks.FERN);
            // 中の間（呼び鈴）
            m.marker(MarkerType.BELL, 5, a, 24, Direction.SOUTH);
            m.marker(MarkerType.DUST, 9, a, 26, Direction.NORTH);
            m.marker(MarkerType.VENT, 2, a, 26, Direction.NORTH);
            // 店の間
            m.marker(MarkerType.LEDGER, 5, a, 30, Direction.SOUTH);
            m.marker(MarkerType.WIRING, 1, a, 33, Direction.EAST);
            m.fill(9, a, 35, 10, a + 1, 35, Blocks.BOOKSHELF);
            // 通り庭
            m.marker(MarkerType.CHOPPING, 14, a, 14, Direction.WEST);
            m.marker(MarkerType.BREAKER, 15, a + 1, 21, Direction.WEST);
            m.marker(MarkerType.VENT, 13, a, 18, Direction.NORTH);
            m.set(15, a, 25, Blocks.WATER_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, 3));
            m.set(15, a, 27, Blocks.POTTED_BAMBOO);
        };
        return p;
    }

    // ================================================================ 小：平屋の古民家
    /** 囲炉裏のある平屋。32 × 26 */
    private static MapPlan hiraya() {
        MapPlan p = new MapPlan("hiraya", "平屋", "4〜6人", 32, 26)
                .room("台所", 0, 1, 1, 8, 10, Blocks.SMOOTH_STONE)
                .room("風呂場", 0, 10, 1, 15, 5, Blocks.CALCITE)
                .room("洗面所", 0, 10, 7, 15, 10, Blocks.CALCITE)
                .room("納戸", 0, 17, 1, 22, 10, Blocks.SPRUCE_PLANKS)
                .room("仏間", 0, 24, 1, 31, 5, Blocks.BAMBOO_PLANKS)
                .garden("中庭", 24, 7, 31, 10, Blocks.GRASS_BLOCK)
                .room("廊下", 0, 1, 12, 31, 13, Blocks.OAK_PLANKS)
                .room("土間", 0, 1, 15, 10, 25, Blocks.PACKED_MUD)
                .room("囲炉裏の間", 0, 12, 15, 21, 25, Blocks.BAMBOO_PLANKS)
                .room("座敷", 0, 23, 15, 31, 25, Blocks.BAMBOO_PLANKS);
        p.entranceX = 4;
        p.spawnX = 5;
        p.spawnZ = 22;
        p.furnish = m -> {
            int a = 1;
            BlockState zabuton = Blocks.RED_CARPET.defaultBlockState();
            m.marker(MarkerType.SPAWN, 5, a, 22, Direction.NORTH);
            m.marker(MarkerType.SHOE_RACK, 1, a, 23, Direction.EAST);
            m.marker(MarkerType.CHOPPING, 8, a, 17, Direction.NORTH);
            m.marker(MarkerType.BREAKER, 10, a + 1, 20, Direction.WEST);
            m.fill(1, a, 15, 2, a + 1, 16, Blocks.STRIPPED_OAK_LOG);
            m.marker(MarkerType.KAMADO, 2, a, 1, Direction.SOUTH);
            m.marker(MarkerType.SINK, 6, a, 1, Direction.SOUTH);
            m.marker(MarkerType.GAS_VALVE, 8, a, 8, Direction.WEST);
            m.marker(MarkerType.VENT, 3, a, 9, Direction.NORTH);
            m.marker(MarkerType.BATH, 12, a, 2, Direction.SOUTH);
            m.marker(MarkerType.BOILER, 15, a, 4, Direction.WEST);
            m.marker(MarkerType.VASE, 13, a, 10, Direction.NORTH);
            m.marker(MarkerType.DUST, 19, a, 5, Direction.NORTH);
            m.marker(MarkerType.WIRING, 22, a, 8, Direction.WEST);
            m.marker(MarkerType.VENT, 20, a, 2, Direction.NORTH);
            m.marker(MarkerType.VASE, 28, a, 1, Direction.SOUTH);
            m.marker(MarkerType.FIELD, 25, a, 8, Direction.NORTH);
            m.marker(MarkerType.LAUNDRY, 29, a, 9, Direction.NORTH);
            m.marker(MarkerType.DUST, 20, a, 12, Direction.NORTH);
            // 囲炉裏の間（呼び鈴）
            m.marker(MarkerType.BELL, 16, a, 20, Direction.SOUTH);
            for (int i = 0; i < 12; i++) {
                double ang = i / 12.0 * Math.PI * 2;
                int cx = 16 + (int) Math.round(Math.cos(ang) * 3);
                int cz = 20 + (int) Math.round(Math.sin(ang) * 3);
                if (m.isAir(cx, a, cz)) {
                    m.set(cx, a, cz, zabuton);
                }
            }
            m.marker(MarkerType.VENT, 20, a, 24, Direction.NORTH);
            // 座敷
            m.marker(MarkerType.FUTON, 25, a, 23, Direction.NORTH);
            m.marker(MarkerType.SHOJI, 31, a, 18, Direction.WEST);
            m.marker(MarkerType.LEDGER, 28, a, 16, Direction.SOUTH);
            m.marker(MarkerType.VENT, 30, a, 24, Direction.NORTH);
        };
        return p;
    }

    // ================================================================ 中：2階建ての古民家（これまでの基本マップ）
    private static MapPlan kominka() {
        MapPlan p = new MapPlan("kominka", "古民家", "6〜10人", 44, 40);
        int f1 = 0;
        int f2 = 1;
        p.room("台所", f1, 1, 1, 10, 10, Blocks.SMOOTH_STONE)
                .room("食堂", f1, 12, 1, 21, 10, Blocks.OAK_PLANKS)
                .room("大広間", f1, 23, 1, 34, 10, Blocks.BAMBOO_PLANKS)
                .room("土間", f1, 36, 1, 43, 10, Blocks.PACKED_MUD)
                .room("廊下", f1, 1, 12, 43, 14, Blocks.OAK_PLANKS)
                .room("風呂場", f1, 1, 16, 9, 25, Blocks.CALCITE)
                .room("階段", f1, 11, 16, 13, 25, Blocks.OAK_PLANKS)
                .garden("中庭", 15, 16, 33, 25, Blocks.GRASS_BLOCK)
                .room("納戸", f1, 35, 16, 43, 25, Blocks.SPRUCE_PLANKS)
                .room("廊下", f1, 1, 27, 43, 29, Blocks.OAK_PLANKS)
                .room("トイレ", f1, 1, 31, 5, 39, Blocks.CALCITE)
                .room("洗面所", f1, 7, 31, 11, 39, Blocks.CALCITE)
                .room("玄関", f1, 13, 31, 19, 39, Blocks.OAK_PLANKS)
                .room("帳場", f1, 21, 31, 28, 39, Blocks.OAK_PLANKS)
                .room("縁側の間", f1, 30, 31, 36, 39, Blocks.BAMBOO_PLANKS)
                .room("物置", f1, 38, 31, 43, 39, Blocks.SPRUCE_PLANKS)
                .room("客室・松", f2, 1, 1, 10, 10, Blocks.BAMBOO_PLANKS)
                .room("客室・竹", f2, 12, 1, 21, 10, Blocks.BAMBOO_PLANKS)
                .room("客室・梅", f2, 23, 1, 33, 10, Blocks.BAMBOO_PLANKS)
                .room("客室・桜", f2, 35, 1, 43, 10, Blocks.BAMBOO_PLANKS)
                .room("2階 廊下", f2, 1, 12, 43, 14, Blocks.SPRUCE_PLANKS)
                .room("客室・菊", f2, 1, 16, 9, 25, Blocks.BAMBOO_PLANKS)
                .room("階段", f2, 11, 16, 13, 25, Blocks.SPRUCE_PLANKS)
                .room("ベランダ", f2, 35, 16, 43, 25, Blocks.SPRUCE_PLANKS)
                .room("2階 廊下", f2, 1, 27, 43, 29, Blocks.SPRUCE_PLANKS)
                .room("客室・藤", f2, 1, 31, 11, 39, Blocks.BAMBOO_PLANKS)
                .room("事務室", f2, 13, 31, 20, 39, Blocks.SPRUCE_PLANKS)
                .room("客室・萩", f2, 22, 31, 30, 39, Blocks.BAMBOO_PLANKS)
                .room("布団部屋", f2, 32, 31, 43, 39, Blocks.SPRUCE_PLANKS)
                .door(0, 5, 11, 6, 11)
                .door(0, 16, 11, 17, 11)
                .door(0, 11, 5, 11, 6)
                .door(0, 27, 11, 30, 11)
                .door(0, 39, 11, 40, 11)
                .door(0, 35, 6, 35, 7)
                .door(0, 4, 15, 5, 15)
                .door(0, 11, 15, 13, 15)
                .door(0, 11, 26, 13, 26)
                .door(0, 19, 15, 20, 15)
                .door(0, 28, 15, 29, 15)
                .door(0, 24, 26, 25, 26)
                .door(0, 38, 15, 39, 15)
                .door(0, 38, 26, 39, 26)
                .door(0, 3, 30, 3, 30)
                .door(0, 8, 30, 9, 30)
                .door(0, 15, 30, 17, 30)
                .door(0, 24, 30, 25, 30)
                .door(0, 32, 30, 33, 30)
                .door(0, 40, 30, 41, 30)
                .door(1, 5, 11, 6, 11)
                .door(1, 16, 11, 17, 11)
                .door(1, 27, 11, 28, 11)
                .door(1, 39, 11, 40, 11)
                .door(1, 4, 15, 5, 15)
                .door(1, 4, 26, 5, 26)
                .door(1, 11, 15, 13, 15)
                .door(1, 38, 15, 39, 15)
                .door(1, 38, 26, 39, 26)
                .door(1, 5, 30, 6, 30)
                .door(1, 16, 30, 17, 30)
                .door(1, 25, 30, 26, 30)
                .door(1, 37, 30, 38, 30);
        p.upper = new int[]{0, 0, 44, 40};
        p.autoDoors = false;
        p.stairs.add(new MapPlan.Stair(11, 13, 22));
        p.entranceX = 15;
        p.spawnX = 16;
        p.spawnZ = 35;
        p.furnish = m -> {
            // 玄関のたたきと中庭の縁側
            m.fill(13, MapBuilder.F1, 36, 19, MapBuilder.F1, 39, Blocks.POLISHED_ANDESITE);
            m.fill(15, MapBuilder.F1, 16, 33, MapBuilder.F1, 16, Blocks.OAK_PLANKS);

        int a = 1;
        int b = 6;
        BlockState zabuton = Blocks.RED_CARPET.defaultBlockState();

        // ---- 台所
        m.marker(MarkerType.KAMADO, 2, a, 1, Direction.SOUTH);
        m.marker(MarkerType.KAMADO, 4, a, 1, Direction.SOUTH);
        m.marker(MarkerType.SINK, 7, a, 1, Direction.SOUTH);
        m.marker(MarkerType.GAS_VALVE, 9, a, 3, Direction.WEST);
        m.fill(1, a, 6, 1, a + 1, 7, Blocks.BARREL);
        m.set(4, a, 6, Blocks.SPRUCE_FENCE);
        m.set(4, a + 1, 6, Blocks.SPRUCE_PRESSURE_PLATE);
        m.marker(MarkerType.VENT, 8, a, 9, Direction.NORTH);

        // ---- 食堂（長い座卓と座布団）
        for (int x = 14; x <= 19; x++) {
            m.set(x, a, 5, Blocks.SPRUCE_FENCE);
            m.set(x, a + 1, 5, Blocks.SPRUCE_PRESSURE_PLATE);
            m.set(x, a, 4, zabuton);
            m.set(x, a, 6, zabuton);
        }
        m.marker(MarkerType.VASE, 20, a, 1, Direction.SOUTH);
        m.marker(MarkerType.SHOJI, 13, a, 1, Direction.SOUTH);
        m.marker(MarkerType.DUST, 13, a, 8, Direction.NORTH);

        // ---- 大広間（囲炉裏と呼び鈴。会議はここ）
        m.marker(MarkerType.BELL, 28, a, 5, Direction.SOUTH);
        for (int i = 0; i < 16; i++) {
            double ang = i / 16.0 * Math.PI * 2;
            int cx = 28 + (int) Math.round(Math.cos(ang) * 2.6);
            int cz = 5 + (int) Math.round(Math.sin(ang) * 2.6);
            if (m.isAir(cx, a, cz)) {
                m.set(cx, a, cz, zabuton);
            }
        }
        m.marker(MarkerType.VASE, 28, a, 1, Direction.SOUTH);
        m.set(26, a, 1, Blocks.POTTED_AZALEA);
        m.set(30, a, 1, Blocks.BOOKSHELF);
        m.set(24, a, 1, Blocks.CHISELED_BOOKSHELF);
        m.marker(MarkerType.SHOJI, 24, a, 10, Direction.NORTH);
        m.marker(MarkerType.SHOJI, 33, a, 10, Direction.NORTH);

        // ---- 土間
        m.marker(MarkerType.CHOPPING, 39, a, 8, Direction.NORTH);
        m.marker(MarkerType.BREAKER, 42, a + 1, 1, Direction.SOUTH);
        m.fill(36, a, 1, 37, a + 1, 2, Blocks.STRIPPED_OAK_LOG);
        m.set(43, a, 5, Blocks.WATER_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, 3));
        m.set(43, a, 8, Blocks.COMPOSTER);
        m.marker(MarkerType.VENT, 37, a, 6, Direction.NORTH);

        // ---- 1階 廊下
        m.marker(MarkerType.DUST, 8, a, 13, Direction.NORTH);
        m.marker(MarkerType.DUST, 31, a, 13, Direction.NORTH);
        m.marker(MarkerType.DUST, 20, a, 28, Direction.NORTH);
        m.set(1, a, 13, Blocks.POTTED_FERN);
        m.set(43, a, 13, Blocks.POTTED_FERN);
        m.set(1, a, 28, Blocks.POTTED_BAMBOO);
        m.set(43, a, 28, Blocks.POTTED_BAMBOO);

        // ---- 風呂場
        m.fill(2, a, 20, 6, a, 24, Blocks.STRIPPED_SPRUCE_LOG);
        m.fill(3, a, 21, 5, a, 23, Blocks.WATER);
        m.marker(MarkerType.BATH, 8, a, 22, Direction.WEST);
        m.marker(MarkerType.BOILER, 1, a, 17, Direction.EAST);
        m.marker(MarkerType.VENT, 6, a, 17, Direction.NORTH);

        // ---- 中庭（池・畑・物干し）
        m.fill(21, MapBuilder.F1, 19, 26, MapBuilder.F1, 22, Blocks.WATER);
        m.fill(21, MapBuilder.F1 - 1, 19, 26, MapBuilder.F1 - 1, 22, Blocks.CLAY);
        for (int x = 20; x <= 27; x++) {
            m.set(x, MapBuilder.F1, 18, Blocks.MOSSY_COBBLESTONE);
            m.set(x, MapBuilder.F1, 23, Blocks.MOSSY_COBBLESTONE);
        }
        for (int z = 19; z <= 22; z++) {
            m.set(20, MapBuilder.F1, z, Blocks.MOSSY_COBBLESTONE);
            m.set(27, MapBuilder.F1, z, Blocks.MOSSY_COBBLESTONE);
        }
        m.set(23, MapBuilder.F1 + 1, 20, Blocks.LILY_PAD);
        m.marker(MarkerType.FIELD, 16, a, 17, Direction.NORTH);
        m.marker(MarkerType.FIELD, 16, a, 21, Direction.NORTH);
        m.marker(MarkerType.FIELD, 19, a, 25, Direction.NORTH);
        m.marker(MarkerType.LAUNDRY, 31, a, 24, Direction.NORTH);
        m.set(32, a, 18, Blocks.FLOWERING_AZALEA);
        m.set(29, a, 21, Blocks.STONE_BRICK_WALL);
        m.set(29, a + 1, 21, Blocks.LANTERN);
        m.set(17, a, 24, Blocks.POPPY);
        m.set(18, a, 24, Blocks.DANDELION);
        m.set(33, a, 24, Blocks.CORNFLOWER);

        // ---- 納戸
        m.fill(43, a, 17, 43, a + 2, 19, Blocks.BARREL);
        m.set(42, a, 17, Blocks.CHEST);
        m.marker(MarkerType.DUST, 40, a, 21, Direction.NORTH);
        m.marker(MarkerType.VENT, 36, a, 24, Direction.NORTH);

        // ---- トイレ・洗面所
        m.set(3, a, 38, Blocks.CAULDRON);
        m.set(1, a, 32, Blocks.POTTED_FERN);
        m.marker(MarkerType.SINK, 9, a, 39, Direction.NORTH);
        m.marker(MarkerType.VASE, 7, a, 31, Direction.SOUTH);

        // ---- 玄関（スタート地点）
        m.marker(MarkerType.SPAWN, 16, a, 35, Direction.NORTH);
        m.marker(MarkerType.SHOE_RACK, 13, a, 38, Direction.EAST);
        m.set(19, a, 38, Blocks.POTTED_BAMBOO);
        m.set(19, a, 32, Blocks.FLOWER_POT);

        // ---- 帳場
        m.marker(MarkerType.LEDGER, 24, a, 34, Direction.SOUTH);
        m.fill(21, a, 39, 22, a + 1, 39, Blocks.BOOKSHELF);
        m.set(28, a, 31, Blocks.LECTERN);
        m.marker(MarkerType.VENT, 27, a, 38, Direction.NORTH);

        // ---- 縁側の間
        m.marker(MarkerType.SHOJI, 33, a, 39, Direction.NORTH);
        m.marker(MarkerType.DUST, 35, a, 35, Direction.NORTH);
        m.set(31, a, 35, Blocks.SPRUCE_TRAPDOOR);
        m.set(30, a, 31, Blocks.POTTED_CHERRY_SAPLING);

        // ---- 物置
        m.fill(43, a, 36, 43, a + 2, 39, Blocks.BARREL);
        m.marker(MarkerType.DUST, 40, a, 35, Direction.NORTH);
        m.marker(MarkerType.VENT, 41, a, 38, Direction.NORTH);

        // ================= 2階
        m.marker(MarkerType.FUTON, 3, b, 3, Direction.SOUTH);           // 松
        m.marker(MarkerType.DUST, 8, b, 8, Direction.NORTH);
        m.marker(MarkerType.VENT, 9, b, 2, Direction.NORTH);

        m.marker(MarkerType.FUTON, 14, b, 3, Direction.SOUTH);          // 竹
        m.marker(MarkerType.SHOJI, 18, b, 1, Direction.SOUTH);

        m.marker(MarkerType.FUTON, 25, b, 3, Direction.SOUTH);          // 梅
        m.marker(MarkerType.VASE, 32, b, 1, Direction.SOUTH);

        m.marker(MarkerType.FUTON, 37, b, 3, Direction.SOUTH);          // 桜
        m.marker(MarkerType.DUST, 42, b, 8, Direction.NORTH);

        m.marker(MarkerType.DUST, 25, b, 13, Direction.NORTH);          // 2階 廊下

        m.marker(MarkerType.FUTON, 3, b, 23, Direction.NORTH);          // 菊
        m.marker(MarkerType.SHOJI, 1, b, 19, Direction.EAST);
        m.marker(MarkerType.VENT, 8, b, 24, Direction.NORTH);

        m.marker(MarkerType.LAUNDRY, 39, b, 20, Direction.NORTH);       // ベランダ
        m.set(43, b, 17, Blocks.POTTED_AZALEA);

        m.marker(MarkerType.FUTON, 3, b, 37, Direction.NORTH);          // 藤
        m.marker(MarkerType.DUST, 9, b, 33, Direction.NORTH);

        m.marker(MarkerType.WIRING, 16, b, 39, Direction.NORTH);        // 事務室
        m.marker(MarkerType.LEDGER, 14, b, 33, Direction.SOUTH);
        m.fill(13, b, 39, 13, b + 1, 39, Blocks.BOOKSHELF);
        m.set(20, b, 39, Blocks.BARREL);
        m.marker(MarkerType.VENT, 19, b, 36, Direction.NORTH);

        m.marker(MarkerType.FUTON, 24, b, 37, Direction.NORTH);         // 萩
        m.marker(MarkerType.SHOJI, 29, b, 39, Direction.NORTH);

        m.marker(MarkerType.FUTON, 34, b, 37, Direction.NORTH);         // 布団部屋
        m.fill(41, b, 36, 43, b + 1, 39, Blocks.WHITE_WOOL);
        m.fill(41, b, 32, 43, b, 34, Blocks.BLUE_WOOL);
        m.marker(MarkerType.VENT, 38, b, 33, Direction.NORTH);
    
        };
        return p;
    }

    // ================================================================ 大：屋敷（離れ・蔵・庭つき）
    /** 庭と離れのある武家屋敷風。60 × 48、平屋 */
    private static MapPlan yashiki() {
        MapPlan p = new MapPlan("yashiki", "屋敷", "10〜12人", 60, 48)
                .room("蔵", 0, 1, 1, 9, 10, Blocks.SPRUCE_PLANKS)
                .garden("裏庭", 11, 1, 24, 10, Blocks.GRASS_BLOCK)
                .room("台所", 0, 26, 1, 35, 10, Blocks.SMOOTH_STONE)
                .room("女中部屋", 0, 37, 1, 44, 10, Blocks.BAMBOO_PLANKS)
                .room("風呂場", 0, 46, 1, 52, 10, Blocks.CALCITE)
                .room("洗面所", 0, 54, 1, 59, 10, Blocks.CALCITE)
                .room("北の廊下", 0, 1, 12, 59, 13, Blocks.OAK_PLANKS)
                .room("茶の間", 0, 1, 15, 10, 28, Blocks.BAMBOO_PLANKS)
                .garden("中庭", 12, 15, 27, 28, Blocks.GRASS_BLOCK)
                .room("仏間", 0, 29, 15, 36, 28, Blocks.BAMBOO_PLANKS)
                .room("奥座敷", 0, 38, 15, 47, 28, Blocks.BAMBOO_PLANKS)
                .room("書斎", 0, 49, 15, 59, 28, Blocks.SPRUCE_PLANKS)
                .room("南の廊下", 0, 1, 30, 59, 31, Blocks.OAK_PLANKS)
                .room("土間", 0, 1, 33, 10, 47, Blocks.PACKED_MUD)
                .room("玄関", 0, 12, 33, 18, 47, Blocks.OAK_PLANKS)
                .room("表座敷", 0, 20, 33, 31, 47, Blocks.BAMBOO_PLANKS)
                .room("次の間", 0, 33, 33, 40, 47, Blocks.BAMBOO_PLANKS)
                .room("渡り廊下", 0, 42, 33, 44, 47, Blocks.OAK_PLANKS)
                .room("離れ", 0, 46, 33, 59, 40, Blocks.BAMBOO_PLANKS)
                .garden("前庭", 46, 42, 59, 47, Blocks.MOSS_BLOCK)
                .noDoor("蔵", "裏庭");
        p.entranceX = 14;
        p.spawnX = 15;
        p.spawnZ = 44;
        p.furnish = m -> {
            int a = 1;
            BlockState zabuton = Blocks.RED_CARPET.defaultBlockState();
            // 玄関・土間
            m.marker(MarkerType.SPAWN, 15, a, 44, Direction.NORTH);
            m.marker(MarkerType.SHOE_RACK, 12, a, 45, Direction.EAST);
            m.fill(12, MapBuilder.F1, 46, 18, MapBuilder.F1, 47, Blocks.POLISHED_ANDESITE);
            m.marker(MarkerType.CHOPPING, 4, a, 43, Direction.NORTH);
            m.marker(MarkerType.BREAKER, 1, a + 1, 36, Direction.EAST);
            m.fill(8, a, 46, 10, a + 1, 47, Blocks.BARREL);
            m.marker(MarkerType.VENT, 7, a, 37, Direction.NORTH);
            // 蔵
            m.marker(MarkerType.DUST, 4, a, 4, Direction.NORTH);
            m.marker(MarkerType.WIRING, 1, a, 8, Direction.EAST);
            m.fill(7, a, 1, 9, a + 2, 1, Blocks.CHEST);
            m.marker(MarkerType.VENT, 8, a, 8, Direction.NORTH);
            // 裏庭
            m.marker(MarkerType.LAUNDRY, 15, a, 4, Direction.NORTH);
            m.marker(MarkerType.FIELD, 20, a, 3, Direction.NORTH);
            m.marker(MarkerType.FIELD, 21, a, 8, Direction.NORTH);
            m.set(12, a, 9, Blocks.COMPOSTER);
            // 台所
            m.marker(MarkerType.KAMADO, 27, a, 1, Direction.SOUTH);
            m.marker(MarkerType.KAMADO, 30, a, 1, Direction.SOUTH);
            m.marker(MarkerType.SINK, 34, a, 1, Direction.SOUTH);
            m.marker(MarkerType.GAS_VALVE, 35, a, 8, Direction.WEST);
            m.marker(MarkerType.VENT, 27, a, 9, Direction.NORTH);
            // 女中部屋・風呂・洗面
            m.marker(MarkerType.FUTON, 40, a, 2, Direction.SOUTH);
            m.marker(MarkerType.DUST, 43, a, 9, Direction.NORTH);
            m.marker(MarkerType.BATH, 48, a, 2, Direction.SOUTH);
            m.marker(MarkerType.BOILER, 52, a, 8, Direction.WEST);
            m.marker(MarkerType.VASE, 56, a, 1, Direction.SOUTH);
            m.marker(MarkerType.SINK, 58, a, 6, Direction.WEST);
            // 北の廊下
            m.marker(MarkerType.DUST, 30, a, 12, Direction.NORTH);
            // 茶の間
            m.marker(MarkerType.VASE, 5, a, 15, Direction.SOUTH);
            m.marker(MarkerType.DUST, 8, a, 27, Direction.NORTH);
            m.marker(MarkerType.VENT, 2, a, 20, Direction.NORTH);
            // 中庭（池）
            m.pond(17, 19, 23, 24);
            m.marker(MarkerType.FIELD, 13, a, 16, Direction.NORTH);
            m.set(25, a, 17, Blocks.STONE_BRICK_WALL);
            m.set(25, a + 1, 17, Blocks.LANTERN);
            m.set(14, a, 26, Blocks.FLOWERING_AZALEA);
            m.set(26, a, 26, Blocks.AZALEA);
            // 仏間・奥座敷・書斎
            m.marker(MarkerType.VASE, 32, a, 15, Direction.SOUTH);
            m.marker(MarkerType.SHOJI, 36, a, 22, Direction.WEST);
            m.marker(MarkerType.FUTON, 41, a, 26, Direction.NORTH);
            m.marker(MarkerType.SHOJI, 47, a, 18, Direction.WEST);
            m.marker(MarkerType.LEDGER, 53, a, 16, Direction.SOUTH);
            m.marker(MarkerType.WIRING, 59, a, 22, Direction.WEST);
            m.fill(49, a, 28, 51, a + 1, 28, Blocks.BOOKSHELF);
            m.marker(MarkerType.VENT, 57, a, 26, Direction.NORTH);
            // 表座敷（呼び鈴。会議はここ）
            m.marker(MarkerType.BELL, 25, a, 39, Direction.SOUTH);
            for (int i = 0; i < 16; i++) {
                double ang = i / 16.0 * Math.PI * 2;
                int cx = 25 + (int) Math.round(Math.cos(ang) * 3.4);
                int cz = 39 + (int) Math.round(Math.sin(ang) * 3.4);
                if (m.isAir(cx, a, cz)) {
                    m.set(cx, a, cz, zabuton);
                }
            }
            m.marker(MarkerType.SHOJI, 21, a, 47, Direction.NORTH);
            // 次の間・渡り廊下・離れ・前庭
            m.marker(MarkerType.DUST, 37, a, 36, Direction.NORTH);
            m.marker(MarkerType.SHOJI, 36, a, 47, Direction.NORTH);
            m.marker(MarkerType.VENT, 39, a, 45, Direction.NORTH);
            m.marker(MarkerType.DUST, 43, a, 40, Direction.NORTH);
            m.marker(MarkerType.VASE, 52, a, 33, Direction.SOUTH);
            m.marker(MarkerType.LEDGER, 56, a, 36, Direction.SOUTH);
            m.marker(MarkerType.VENT, 48, a, 39, Direction.NORTH);
            m.marker(MarkerType.LAUNDRY, 55, a, 45, Direction.NORTH);
            m.set(48, a, 46, Blocks.CHERRY_LEAVES);
            m.set(50, a, 43, Blocks.STONE_BRICK_WALL);
            m.set(50, a + 1, 43, Blocks.LANTERN);
        };
        return p;
    }

    // ================================================================ 特大：温泉旅館
    /** 大浴場・露天風呂・宴会場のある温泉旅館。64 × 52、北側が2階建て */
    private static MapPlan ryokan() {
        MapPlan p = new MapPlan("ryokan", "温泉旅館", "12人〜", 64, 52)
                // 1階 北
                .room("厨房", 0, 1, 1, 12, 12, Blocks.SMOOTH_STONE)
                .room("食事処", 0, 14, 1, 29, 12, Blocks.OAK_PLANKS)
                .room("宴会場", 0, 31, 1, 46, 12, Blocks.BAMBOO_PLANKS)
                .room("布団部屋", 0, 48, 1, 53, 12, Blocks.SPRUCE_PLANKS)
                .room("ボイラー室", 0, 55, 1, 63, 12, Blocks.SMOOTH_STONE)
                .room("1階 廊下", 0, 1, 14, 63, 15, Blocks.OAK_PLANKS)
                // 1階 中
                .room("男湯", 0, 1, 17, 14, 32, Blocks.CALCITE)
                .room("女湯", 0, 16, 17, 29, 32, Blocks.CALCITE)
                .garden("中庭", 31, 17, 46, 32, Blocks.GRASS_BLOCK)
                .room("階段", 0, 48, 17, 50, 32, Blocks.OAK_PLANKS)
                .room("遊戯室", 0, 52, 17, 63, 32, Blocks.SPRUCE_PLANKS)
                .room("南の廊下", 0, 1, 34, 63, 35, Blocks.OAK_PLANKS)
                // 1階 南
                .garden("露天風呂", 1, 37, 14, 51, Blocks.POLISHED_ANDESITE)
                .room("脱衣所", 0, 16, 37, 22, 51, Blocks.BAMBOO_PLANKS)
                .room("売店", 0, 24, 37, 31, 51, Blocks.OAK_PLANKS)
                .room("ロビー", 0, 33, 37, 46, 51, Blocks.OAK_PLANKS)
                .room("帳場", 0, 48, 37, 55, 51, Blocks.OAK_PLANKS)
                .room("事務室", 0, 57, 37, 63, 51, Blocks.SPRUCE_PLANKS)
                // 2階
                .room("客室・松", 1, 1, 1, 9, 12, Blocks.BAMBOO_PLANKS)
                .room("客室・竹", 1, 11, 1, 19, 12, Blocks.BAMBOO_PLANKS)
                .room("客室・梅", 1, 21, 1, 29, 12, Blocks.BAMBOO_PLANKS)
                .room("客室・桜", 1, 31, 1, 39, 12, Blocks.BAMBOO_PLANKS)
                .room("客室・菊", 1, 41, 1, 49, 12, Blocks.BAMBOO_PLANKS)
                .room("客室・藤", 1, 51, 1, 63, 12, Blocks.BAMBOO_PLANKS)
                .room("2階 廊下", 1, 1, 14, 63, 15, Blocks.SPRUCE_PLANKS)
                .room("客室・萩", 1, 1, 17, 14, 32, Blocks.BAMBOO_PLANKS)
                .room("客室・椿", 1, 16, 17, 29, 32, Blocks.BAMBOO_PLANKS)
                .room("階段", 1, 48, 17, 50, 32, Blocks.SPRUCE_PLANKS)
                .room("リネン室", 1, 52, 17, 63, 32, Blocks.SPRUCE_PLANKS)
                .noDoor("男湯", "女湯")
                .noDoor("男湯", "露天風呂");
        p.upper = new int[]{0, 0, 64, 33};
        p.stairs.add(new MapPlan.Stair(48, 50, 26));
        p.entranceX = 39;
        p.spawnX = 40;
        p.spawnZ = 47;
        p.furnish = m -> {
            int a = 1;
            int b = 6;
            BlockState zabuton = Blocks.RED_CARPET.defaultBlockState();
            // 厨房・食事処
            m.marker(MarkerType.KAMADO, 2, a, 1, Direction.SOUTH);
            m.marker(MarkerType.KAMADO, 5, a, 1, Direction.SOUTH);
            m.marker(MarkerType.SINK, 9, a, 1, Direction.SOUTH);
            m.marker(MarkerType.GAS_VALVE, 12, a, 6, Direction.WEST);
            m.marker(MarkerType.VENT, 10, a, 10, Direction.NORTH);
            m.marker(MarkerType.DUST, 20, a, 11, Direction.NORTH);
            m.marker(MarkerType.VASE, 28, a, 1, Direction.SOUTH);
            for (int x = 16; x <= 26; x += 5) {
                m.fill(x, a, 5, x + 2, a, 6, Blocks.SPRUCE_FENCE);
                m.fill(x, a + 1, 5, x + 2, a + 1, 6, Blocks.SPRUCE_PRESSURE_PLATE);
            }
            // 宴会場（呼び鈴。会議はここ）
            m.marker(MarkerType.BELL, 38, a, 6, Direction.SOUTH);
            for (int i = 0; i < 18; i++) {
                double ang = i / 18.0 * Math.PI * 2;
                int cx = 38 + (int) Math.round(Math.cos(ang) * 3.6);
                int cz = 6 + (int) Math.round(Math.sin(ang) * 3.6);
                if (m.isAir(cx, a, cz)) {
                    m.set(cx, a, cz, zabuton);
                }
            }
            m.marker(MarkerType.SHOJI, 46, a, 10, Direction.WEST);
            m.marker(MarkerType.VENT, 45, a, 2, Direction.NORTH);
            // 布団部屋・ボイラー室
            m.marker(MarkerType.FUTON, 50, a, 2, Direction.SOUTH);
            m.fill(52, a, 9, 53, a + 1, 12, Blocks.WHITE_WOOL);
            m.marker(MarkerType.BOILER, 59, a, 2, Direction.SOUTH);
            m.marker(MarkerType.BREAKER, 63, a + 1, 8, Direction.WEST);
            m.marker(MarkerType.WIRING, 56, a, 11, Direction.NORTH);
            // 大浴場
            m.fill(2, MapBuilder.F1, 24, 12, MapBuilder.F1, 31, Blocks.WATER);
            m.fill(2, MapBuilder.F1 - 1, 24, 12, MapBuilder.F1 - 1, 31, Blocks.SMOOTH_STONE);
            m.marker(MarkerType.BATH, 6, a, 18, Direction.SOUTH);
            m.marker(MarkerType.VENT, 13, a, 20, Direction.NORTH);
            m.fill(17, MapBuilder.F1, 24, 28, MapBuilder.F1, 31, Blocks.WATER);
            m.fill(17, MapBuilder.F1 - 1, 24, 28, MapBuilder.F1 - 1, 31, Blocks.SMOOTH_STONE);
            m.marker(MarkerType.BATH, 21, a, 18, Direction.SOUTH);
            m.marker(MarkerType.VASE, 28, a, 18, Direction.WEST);
            // 中庭
            m.pond(37, 22, 41, 27);
            m.marker(MarkerType.FIELD, 33, a, 19, Direction.NORTH);
            m.marker(MarkerType.LAUNDRY, 43, a, 30, Direction.NORTH);
            m.set(45, a, 18, Blocks.FLOWERING_AZALEA);
            // 遊戯室
            m.marker(MarkerType.LEDGER, 57, a, 19, Direction.SOUTH);
            m.marker(MarkerType.DUST, 60, a, 30, Direction.NORTH);
            m.marker(MarkerType.VENT, 62, a, 23, Direction.NORTH);
            m.fill(55, a, 25, 58, a, 27, Blocks.GREEN_CARPET);
            // 露天風呂
            m.fill(3, MapBuilder.F1, 40, 12, MapBuilder.F1, 49, Blocks.WATER);
            m.fill(3, MapBuilder.F1 - 1, 40, 12, MapBuilder.F1 - 1, 49, Blocks.STONE);
            for (int x = 2; x <= 13; x++) {
                m.set(x, a, 39, Blocks.MOSSY_COBBLESTONE_WALL);
                m.set(x, a, 50, Blocks.MOSSY_COBBLESTONE_WALL);
            }
            m.marker(MarkerType.CHOPPING, 13, a, 37, Direction.WEST);
            m.set(1, a, 51, Blocks.POTTED_BAMBOO);
            // 脱衣所・売店
            m.marker(MarkerType.LAUNDRY, 19, a, 39, Direction.SOUTH);
            m.marker(MarkerType.DUST, 20, a, 49, Direction.NORTH);
            m.marker(MarkerType.LEDGER, 27, a, 38, Direction.SOUTH);
            m.fill(24, a, 45, 24, a + 1, 50, Blocks.BARREL);
            m.marker(MarkerType.VENT, 30, a, 50, Direction.NORTH);
            // ロビー（スタート）
            m.marker(MarkerType.SPAWN, 40, a, 47, Direction.NORTH);
            m.marker(MarkerType.SHOE_RACK, 33, a, 49, Direction.EAST);
            m.marker(MarkerType.VASE, 46, a, 40, Direction.WEST);
            m.fill(37, MapBuilder.F1, 49, 42, MapBuilder.F1, 51, Blocks.POLISHED_ANDESITE);
            // 帳場・事務室
            m.marker(MarkerType.LEDGER, 51, a, 38, Direction.SOUTH);
            m.marker(MarkerType.WIRING, 55, a, 47, Direction.WEST);
            m.marker(MarkerType.DUST, 60, a, 44, Direction.NORTH);
            m.marker(MarkerType.VENT, 62, a, 50, Direction.NORTH);
            // 2階
            int[] fx = {3, 13, 23, 33, 43, 55};
            for (int x : fx) {
                m.marker(MarkerType.FUTON, x, b, 2, Direction.SOUTH);
            }
            m.marker(MarkerType.SHOJI, 9, b, 8, Direction.WEST);
            m.marker(MarkerType.VASE, 29, b, 8, Direction.WEST);
            m.marker(MarkerType.DUST, 46, b, 11, Direction.NORTH);
            m.marker(MarkerType.VENT, 8, b, 10, Direction.NORTH);
            m.marker(MarkerType.VENT, 38, b, 10, Direction.NORTH);
            m.marker(MarkerType.DUST, 30, b, 14, Direction.NORTH);
            m.marker(MarkerType.FUTON, 5, b, 30, Direction.NORTH);
            m.marker(MarkerType.SHOJI, 1, b, 24, Direction.EAST);
            m.marker(MarkerType.VENT, 13, b, 30, Direction.NORTH);
            m.marker(MarkerType.FUTON, 20, b, 30, Direction.NORTH);
            m.marker(MarkerType.VASE, 28, b, 17, Direction.SOUTH);
            m.marker(MarkerType.DUST, 56, b, 25, Direction.NORTH);
            m.marker(MarkerType.LAUNDRY, 60, b, 30, Direction.NORTH);
            m.marker(MarkerType.VENT, 62, b, 19, Direction.NORTH);
        };
        return p;
    }

    // ================================================================ 超大：宿坊（お寺）
    /** 本堂・鐘楼・枯山水・座禅堂のあるお寺の宿坊。80 × 64、平屋 */
    private static MapPlan shukubo() {
        MapPlan p = new MapPlan("shukubo", "宿坊", "12人〜・超大", 80, 64)
                .room("庫裏", 0, 1, 1, 14, 14, Blocks.SMOOTH_STONE)
                .room("斎堂", 0, 16, 1, 25, 14, Blocks.OAK_PLANKS)
                .room("本堂", 0, 27, 1, 52, 14, Blocks.BAMBOO_PLANKS)
                .garden("鐘楼", 54, 1, 63, 14, Blocks.GRAVEL)
                .room("宝物庫", 0, 65, 1, 79, 14, Blocks.SPRUCE_PLANKS)
                .room("北の回廊", 0, 1, 16, 79, 17, Blocks.DARK_OAK_PLANKS)
                .room("写経室", 0, 1, 19, 12, 40, Blocks.BAMBOO_PLANKS)
                .room("座禅堂", 0, 14, 19, 25, 40, Blocks.BAMBOO_PLANKS)
                .garden("枯山水", 27, 19, 52, 40, Blocks.SAND)
                .room("書院", 0, 54, 19, 63, 40, Blocks.BAMBOO_PLANKS)
                .room("住職の間", 0, 65, 19, 79, 40, Blocks.BAMBOO_PLANKS)
                .room("南の回廊", 0, 1, 42, 79, 43, Blocks.DARK_OAK_PLANKS)
                .room("風呂場", 0, 1, 45, 10, 63, Blocks.CALCITE)
                .room("洗面所", 0, 12, 45, 18, 63, Blocks.CALCITE)
                .room("宿坊・一", 0, 20, 45, 28, 63, Blocks.BAMBOO_PLANKS)
                .room("宿坊・二", 0, 30, 45, 38, 63, Blocks.BAMBOO_PLANKS)
                .room("玄関", 0, 40, 45, 49, 63, Blocks.OAK_PLANKS)
                .room("宿坊・三", 0, 51, 45, 59, 63, Blocks.BAMBOO_PLANKS)
                .room("宿坊・四", 0, 61, 45, 69, 63, Blocks.BAMBOO_PLANKS)
                .garden("池の庭", 71, 45, 79, 63, Blocks.GRASS_BLOCK);
        p.entranceX = 44;
        p.spawnX = 45;
        p.spawnZ = 60;
        p.furnish = m -> {
            int a = 1;
            BlockState zabuton = Blocks.PURPLE_CARPET.defaultBlockState();
            // 庫裏（台所）
            m.marker(MarkerType.KAMADO, 2, a, 1, Direction.SOUTH);
            m.marker(MarkerType.KAMADO, 6, a, 1, Direction.SOUTH);
            m.marker(MarkerType.SINK, 11, a, 1, Direction.SOUTH);
            m.marker(MarkerType.GAS_VALVE, 14, a, 9, Direction.WEST);
            m.marker(MarkerType.CHOPPING, 3, a, 12, Direction.NORTH);
            m.marker(MarkerType.VENT, 12, a, 12, Direction.NORTH);
            // 斎堂（食堂）
            m.marker(MarkerType.DUST, 20, a, 13, Direction.NORTH);
            m.marker(MarkerType.VASE, 24, a, 1, Direction.SOUTH);
            for (int z = 4; z <= 10; z += 3) {
                m.fill(18, a, z, 23, a, z, Blocks.SPRUCE_FENCE);
                m.fill(18, a + 1, z, 23, a + 1, z, Blocks.SPRUCE_PRESSURE_PLATE);
            }
            // 本堂
            m.fill(36, a, 1, 43, a, 2, Blocks.GOLD_BLOCK);
            m.fill(38, a + 1, 1, 41, a + 2, 1, Blocks.GILDED_BLACKSTONE);
            m.marker(MarkerType.VASE, 35, a, 3, Direction.SOUTH);
            m.marker(MarkerType.VASE, 44, a, 3, Direction.SOUTH);
            m.marker(MarkerType.LEDGER, 31, a, 3, Direction.SOUTH);
            m.marker(MarkerType.SHOJI, 27, a, 9, Direction.EAST);
            m.marker(MarkerType.SHOJI, 52, a, 9, Direction.WEST);
            m.marker(MarkerType.DUST, 46, a, 12, Direction.NORTH);
            m.marker(MarkerType.VENT, 50, a, 12, Direction.NORTH);
            for (int x = 30; x <= 49; x += 2) {
                m.set(x, a, 10, zabuton);
            }
            // 鐘楼（呼び鈴＝梵鐘。会議はここ）
            m.marker(MarkerType.BELL, 58, a, 7, Direction.SOUTH);
            for (int i = 0; i < 16; i++) {
                double ang = i / 16.0 * Math.PI * 2;
                int cx = 58 + (int) Math.round(Math.cos(ang) * 3.4);
                int cz = 7 + (int) Math.round(Math.sin(ang) * 3.4);
                if (m.isAir(cx, a, cz)) {
                    m.set(cx, a, cz, zabuton);
                }
            }
            // 宝物庫
            m.marker(MarkerType.BREAKER, 66, a + 1, 1, Direction.SOUTH);
            m.marker(MarkerType.WIRING, 79, a, 8, Direction.WEST);
            m.marker(MarkerType.DUST, 72, a, 10, Direction.NORTH);
            m.fill(75, a, 1, 79, a + 1, 1, Blocks.CHEST);
            m.marker(MarkerType.VENT, 78, a, 3, Direction.NORTH);
            // 写経室・座禅堂
            m.marker(MarkerType.LEDGER, 4, a, 20, Direction.SOUTH);
            m.marker(MarkerType.LEDGER, 9, a, 20, Direction.SOUTH);
            m.marker(MarkerType.LEDGER, 4, a, 28, Direction.SOUTH);
            m.marker(MarkerType.VENT, 10, a, 38, Direction.NORTH);
            for (int x = 16; x <= 23; x += 3) {
                for (int z = 22; z <= 37; z += 5) {
                    m.set(x, a, z, zabuton);
                }
            }
            m.marker(MarkerType.SHOJI, 25, a, 24, Direction.WEST);
            m.marker(MarkerType.DUST, 19, a, 39, Direction.NORTH);
            // 枯山水（砂紋と石）
            for (int x = 27; x <= 52; x++) {
                for (int z = 19; z <= 40; z++) {
                    if ((x + z) % 4 == 0) {
                        m.set(x, MapBuilder.F1, z, Blocks.SMOOTH_SANDSTONE);
                    }
                }
            }
            m.fill(33, a, 25, 34, a + 1, 26, Blocks.MOSSY_COBBLESTONE);
            m.set(45, a, 33, Blocks.MOSSY_COBBLESTONE);
            m.fill(40, a, 28, 41, a, 29, Blocks.STONE);
            m.set(30, a, 37, Blocks.AZALEA);
            m.marker(MarkerType.DUST, 31, a, 21, Direction.NORTH);
            m.marker(MarkerType.DUST, 48, a, 37, Direction.NORTH);
            // 書院・住職の間
            m.marker(MarkerType.LEDGER, 58, a, 20, Direction.SOUTH);
            m.marker(MarkerType.VASE, 63, a, 30, Direction.WEST);
            m.marker(MarkerType.VENT, 62, a, 38, Direction.NORTH);
            m.marker(MarkerType.FUTON, 70, a, 20, Direction.SOUTH);
            m.marker(MarkerType.SHOJI, 79, a, 30, Direction.WEST);
            m.fill(65, a, 40, 67, a + 1, 40, Blocks.BOOKSHELF);
            // 風呂・洗面
            m.marker(MarkerType.BATH, 4, a, 46, Direction.SOUTH);
            m.marker(MarkerType.BOILER, 1, a, 56, Direction.EAST);
            m.fill(3, MapBuilder.F1, 52, 8, MapBuilder.F1, 60, Blocks.WATER);
            m.marker(MarkerType.VENT, 8, a, 62, Direction.NORTH);
            m.marker(MarkerType.SINK, 15, a, 45, Direction.SOUTH);
            m.marker(MarkerType.VASE, 17, a, 60, Direction.WEST);
            // 宿坊（客室）
            m.marker(MarkerType.FUTON, 22, a, 46, Direction.SOUTH);
            m.marker(MarkerType.DUST, 27, a, 60, Direction.NORTH);
            m.marker(MarkerType.FUTON, 32, a, 46, Direction.SOUTH);
            m.marker(MarkerType.SHOJI, 38, a, 55, Direction.WEST);
            m.marker(MarkerType.VENT, 36, a, 62, Direction.NORTH);
            m.marker(MarkerType.FUTON, 53, a, 46, Direction.SOUTH);
            m.marker(MarkerType.DUST, 58, a, 60, Direction.NORTH);
            m.marker(MarkerType.FUTON, 63, a, 46, Direction.SOUTH);
            m.marker(MarkerType.SHOJI, 69, a, 55, Direction.WEST);
            m.marker(MarkerType.VENT, 68, a, 62, Direction.NORTH);
            // 玄関（スタート）
            m.marker(MarkerType.SPAWN, 45, a, 60, Direction.NORTH);
            m.marker(MarkerType.SHOE_RACK, 40, a, 61, Direction.EAST);
            m.fill(40, MapBuilder.F1, 62, 49, MapBuilder.F1, 63, Blocks.POLISHED_ANDESITE);
            // 池の庭
            m.pond(73, 50, 77, 57);
            m.marker(MarkerType.FIELD, 73, a, 46, Direction.NORTH);
            m.marker(MarkerType.LAUNDRY, 76, a, 61, Direction.NORTH);
            m.set(72, a, 60, Blocks.STONE_BRICK_WALL);
            m.set(72, a + 1, 60, Blocks.LANTERN);
        };
        return p;
    }
}
