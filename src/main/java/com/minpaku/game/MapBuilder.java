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
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.SlabType;

import java.util.ArrayList;
import java.util.List;

/**
 * マップの設計図（MapPlan）から建物を建てる。
 *  - 部屋のすき間が壁になる（庭だけに面した壁は土塀）
 *  - となり合う部屋には自動で出入り口があく
 *  - 2階は plan.upper の範囲。庭（open）の上には2階も屋根もない
 */
public final class MapBuilder {
    public static final int F1 = 0;
    public static final int F2 = 5;

    private final ServerLevel level;
    private final BlockPos origin;
    private final Rotation rotation;
    private final MapPlan plan;
    private final int roof;
    private final List<BlockPos> fixups = new ArrayList<>();

    private MapBuilder(ServerLevel level, BlockPos origin, Rotation rotation, MapPlan plan) {
        this.level = level;
        this.origin = origin;
        this.rotation = rotation;
        this.plan = plan;
        this.roof = plan.twoFloors() ? 10 : 5;
    }

    /** プレイヤーの目の前に建てる。玄関がプレイヤーの方を向く。スタート地点を返す */
    public static BlockPos build(ServerPlayer player, MapPlan plan) {
        Direction facing = player.getDirection();
        Rotation rot = switch (facing) {
            case NORTH -> Rotation.NONE;
            case EAST -> Rotation.CLOCKWISE_90;
            case SOUTH -> Rotation.CLOCKWISE_180;
            default -> Rotation.COUNTERCLOCKWISE_90;
        };
        BlockPos front = player.blockPosition().relative(facing, 3);
        BlockPos offset = new BlockPos(-(plan.entranceX + 1), -1, -(plan.d + 3)).rotate(rot);
        MapBuilder b = new MapBuilder(player.serverLevel(), front.offset(offset), rot, plan);
        b.run();
        return b.world(plan.spawnX, F1 + 1, plan.spawnZ);
    }

    // ------------------------------------------------------------ 設計図から使う道具

    /** 階（0 = 1階、1 = 2階）の床の上の高さ */
    public int y(int floor) {
        return (floor == 0 ? F1 : F2) + 1;
    }

    public BlockPos world(int x, int y, int z) {
        return origin.offset(new BlockPos(x, y, z).rotate(rotation));
    }

    public void set(int x, int y, int z, BlockState state) {
        BlockPos pos = world(x, y, z);
        level.setBlock(pos, state.rotate(rotation), Block.UPDATE_CLIENTS);
        Block b = state.getBlock();
        if (state.is(BlockTags.STAIRS) || b == Blocks.WHITE_STAINED_GLASS_PANE || b == Blocks.GLASS_PANE
                || state.is(BlockTags.FENCES) || state.is(BlockTags.WALLS)) {
            fixups.add(pos);
        }
    }

    public void set(int x, int y, int z, Block block) {
        set(x, y, z, block.defaultBlockState());
    }

    public void fill(int x1, int y1, int z1, int x2, int y2, int z2, BlockState state) {
        for (int x = Math.min(x1, x2); x <= Math.max(x1, x2); x++) {
            for (int y = Math.min(y1, y2); y <= Math.max(y1, y2); y++) {
                for (int z = Math.min(z1, z2); z <= Math.max(z1, z2); z++) {
                    set(x, y, z, state);
                }
            }
        }
    }

    public void fill(int x1, int y1, int z1, int x2, int y2, int z2, Block block) {
        fill(x1, y1, z1, x2, y2, z2, block.defaultBlockState());
    }

    /** 家具（専用ブロック）を置く。y は高さ（ふつうは y(階)） */
    public void marker(MarkerType type, int x, int y, int z, Direction facing) {
        set(x, y, z, ModBlocks.BLOCKS.get(type).defaultBlockState().setValue(MarkerBlock.FACING, facing));
    }

    public boolean isWater(int x, int y, int z) {
        return level.getBlockState(world(x, y, z)).is(Blocks.WATER);
    }

    public boolean isAir(int x, int y, int z) {
        return level.getBlockState(world(x, y, z)).isAir();
    }

    /** 池：x1〜x2, z1〜z2 を水にして、まわりを苔石で囲む（地面の高さ） */
    public void pond(int x1, int z1, int x2, int z2) {
        fill(x1, F1, z1, x2, F1, z2, Blocks.WATER);
        fill(x1, F1 - 1, z1, x2, F1 - 1, z2, Blocks.CLAY);
        for (int x = x1 - 1; x <= x2 + 1; x++) {
            set(x, F1, z1 - 1, Blocks.MOSSY_COBBLESTONE);
            set(x, F1, z2 + 1, Blocks.MOSSY_COBBLESTONE);
        }
        for (int z = z1; z <= z2; z++) {
            set(x1 - 1, F1, z, Blocks.MOSSY_COBBLESTONE);
            set(x2 + 1, F1, z, Blocks.MOSSY_COBBLESTONE);
        }
        set((x1 + x2) / 2, F1 + 1, (z1 + z2) / 2, Blocks.LILY_PAD);
    }

    // ------------------------------------------------------------ 部屋と壁の判定

    private boolean inside(int x, int z) {
        return x >= 0 && x <= plan.w && z >= 0 && z <= plan.d;
    }

    private boolean inUpper(int x, int z) {
        int[] u = plan.upper;
        return u != null && x >= u[0] && x <= u[2] && z >= u[1] && z <= u[3];
    }

    private MapPlan.Room roomAt(int floor, int x, int z) {
        for (MapPlan.Room r : plan.rooms) {
            if (r.floor() == floor && r.has(x, z)) {
                return r;
            }
        }
        return null;
    }

    private boolean openAt(int x, int z) {
        MapPlan.Room r = roomAt(0, x, z);
        return r != null && r.open();
    }

    /** その階で壁になるか */
    private boolean isWall(int floor, int x, int z) {
        if (!inside(x, z)) {
            return false;
        }
        if (floor == 1 && (!inUpper(x, z) || openAt(x, z))) {
            return false;
        }
        return roomAt(floor, x, z) == null;
    }

    /** 1階の壁のうち、庭か外にしか面していない（＝屋根のいらない土塀） */
    private boolean gardenWall(int x, int z) {
        if (!isWall(0, x, z)) {
            return false;
        }
        boolean touchesGarden = false;
        int[][] dirs = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        for (int[] dd : dirs) {
            int nx = x + dd[0];
            int nz = z + dd[1];
            MapPlan.Room r = roomAt(0, nx, nz);
            if (r != null && !r.open()) {
                return false;
            }
            if (r != null) {
                touchesGarden = true;
            }
        }
        // 角（斜め）に部屋があるときも家の壁にする
        for (int[] dd : new int[][]{{1, 1}, {1, -1}, {-1, 1}, {-1, -1}}) {
            MapPlan.Room r = roomAt(0, x + dd[0], z + dd[1]);
            if (r != null && !r.open()) {
                return false;
            }
        }
        return touchesGarden || !inside(x + 1, z) || !inside(x - 1, z) || !inside(x, z + 1) || !inside(x, z - 1);
    }

    /** 屋根がいる所（1階の屋根の高さ） */
    private boolean roofed(int x, int z) {
        if (!inside(x, z) || openAt(x, z)) {
            return false;
        }
        return !gardenWall(x, z);
    }

    // ------------------------------------------------------------ 建てる

    private void run() {
        MapData data = MapData.get(level);
        data.clearAll();
        clearPrevious(data);
        clearSite();
        if (plan.custom != null) {
            // 自由な形のマップ：設計図の関数が全部建てる
            plan.custom.accept(this);
            plan.furnish.accept(this);
            fixShapes();
            registerRooms();
            setupWorld();
            clearDrops(level);
            return;
        }
        floors();
        walls(0);
        if (plan.twoFloors()) {
            walls(1);
        }
        stairs();
        doorways();
        entrance();
        windows();
        roofs();
        plan.furnish.accept(this);
        lights();
        fixShapes();
        registerRooms();
        setupWorld();
        clearDrops(level);
    }

    /** 建てるときにこわれたブロックから落ちたアイテムを消す（少しあとにもう一度呼ぶ） */
    public static void clearDrops(ServerLevel level) {
        int[] b = MapData.get(level).lastBuilt;
        if (b.length != 6) {
            return;
        }
        net.minecraft.world.phys.AABB box = new net.minecraft.world.phys.AABB(b[0] - 8, b[1] - 8, b[2] - 8, b[3] + 9, b[4] + 16, b[5] + 9);
        for (net.minecraft.world.entity.item.ItemEntity e : level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, box)) {
            e.discard();
        }
    }

    /** 前に建てたマップを片づける（小さいマップに建て直したときに古い建物が残らないように） */
    private void clearPrevious(MapData data) {
        int[] b = data.lastBuilt;
        if (b.length == 6) {
            BlockState air = Blocks.AIR.defaultBlockState();
            BlockState grass = Blocks.GRASS_BLOCK.defaultBlockState();
            for (int x = b[0]; x <= b[3]; x++) {
                for (int z = b[2]; z <= b[5]; z++) {
                    for (int y = b[1] + 1; y <= b[4]; y++) {
                        BlockPos pos = new BlockPos(x, y, z);
                        if (!level.getBlockState(pos).isAir()) {
                            level.setBlock(pos, air, Block.UPDATE_CLIENTS);
                        }
                    }
                    level.setBlock(new BlockPos(x, b[1], z), grass, Block.UPDATE_CLIENTS);
                }
            }
        }
        // 今回の範囲を覚えておく
        BlockPos p1 = world(-4 - RING, -1, -4 - RING);
        BlockPos p2 = world(plan.w + 4 + RING, Math.max(roof + 4, BAMBOO_TOP + 2), plan.d + 6 + RING);
        data.setLastBuilt(new int[]{Math.min(p1.getX(), p2.getX()), Math.min(p1.getY(), p2.getY()), Math.min(p1.getZ(), p2.getZ()),
                Math.max(p1.getX(), p2.getX()), Math.max(p1.getY(), p2.getY()), Math.max(p1.getZ(), p2.getZ())});
    }

    private void clearSite() {
        int w = plan.w;
        int d = plan.d;
        fill(-4 - RING, 0, -4 - RING, w + 4 + RING, Math.max(roof + 4, BAMBOO_TOP + 2), d + 6 + RING, Blocks.AIR.defaultBlockState());
        fill(-4 - RING, -1, -4 - RING, w + 4 + RING, -1, d + 6 + RING, Blocks.GRASS_BLOCK);
        fill(-4 - RING, -3, -4 - RING, w + 4 + RING, -2, d + 6 + RING, Blocks.DIRT);
        bambooRing();
        fill(-2, -1, -2, w + 2, -1, d + 2, Blocks.GRAVEL);
        fill(plan.entranceX - 1, -1, d + 1, plan.entranceX + 2, -1, d + 6, Blocks.STONE_BRICKS);
    }

    /** 敷地のまわりの竹林の厚さ（ブロック） */
    static final int RING = 7;
    static final int BAMBOO_TOP = 17;

    /** 敷地のまわりをぐるっと竹林で囲む（外が見えないように）。霧はクライアント側でかける */
    private void bambooRing() {
        int x1 = -4 - RING, z1 = -4 - RING, x2 = plan.w + 4 + RING, z2 = plan.d + 6 + RING;
        java.util.Random rnd = new java.util.Random(plan.w * 7919L + plan.d * 31L + 5);
        BlockState base = Blocks.BAMBOO.defaultBlockState()
                .setValue(net.minecraft.world.level.block.BambooStalkBlock.STAGE, 1);
        for (int x = x1; x <= x2; x++) {
            for (int z = z1; z <= z2; z++) {
                boolean inside = x > -4 && x < plan.w + 4 && z > -4 && z < plan.d + 6;
                if (inside) {
                    continue;
                }
                // 外側ほど濃く、内側の1列目は少しまばらに
                int depth = Math.min(Math.min(x - x1, x2 - x), Math.min(z - z1, z2 - z));
                float chance = depth >= RING - 1 ? 0.45f : 0.82f;
                if (rnd.nextFloat() > chance) {
                    set(x, -1, z, rnd.nextInt(3) == 0 ? Blocks.PODZOL : Blocks.GRASS_BLOCK);
                    continue;
                }
                set(x, -1, z, Blocks.PODZOL);
                int h = 11 + rnd.nextInt(BAMBOO_TOP - 10);
                boolean thick = rnd.nextInt(3) > 0;
                for (int y = 0; y < h; y++) {
                    var leaves = y >= h - 2 ? net.minecraft.world.level.block.state.properties.BambooLeaves.LARGE
                            : y >= h - 4 ? net.minecraft.world.level.block.state.properties.BambooLeaves.SMALL
                            : net.minecraft.world.level.block.state.properties.BambooLeaves.NONE;
                    set(x, y, z, base.setValue(net.minecraft.world.level.block.BambooStalkBlock.AGE, thick ? 1 : 0)
                            .setValue(net.minecraft.world.level.block.BambooStalkBlock.LEAVES, leaves));
                }
            }
        }
    }

    private void floors() {
        for (int x = 0; x <= plan.w; x++) {
            for (int z = 0; z <= plan.d; z++) {
                set(x, F1, z, Blocks.STONE_BRICKS);
                if (inUpper(x, z) && !openAt(x, z)) {
                    set(x, F2, z, Blocks.SPRUCE_PLANKS);
                }
            }
        }
        for (MapPlan.Room r : plan.rooms) {
            int fy = r.floor() == 0 ? F1 : F2;
            fill(r.x1(), fy, r.z1(), r.x2(), fy, r.z2(), r.floorBlock());
        }
        // 階段の吹き抜け
        for (MapPlan.Stair s : plan.stairs) {
            fill(s.x1(), F2, s.zStart() - 3, s.x2(), F2, s.zStart() + 1, Blocks.AIR);
        }
    }

    private void walls(int floor) {
        int base = floor == 0 ? F1 : F2;
        for (int x = 0; x <= plan.w; x++) {
            for (int z = 0; z <= plan.d; z++) {
                if (!isWall(floor, x, z)) {
                    continue;
                }
                if (floor == 0 && gardenWall(x, z)) {
                    // 土塀
                    fill(x, base + 1, z, x, base + 2, z, Blocks.MUD_BRICKS);
                    set(x, base + 3, z, Blocks.DEEPSLATE_TILE_SLAB);
                    continue;
                }
                boolean outer = x == 0 || x == plan.w || z == 0 || z == plan.d;
                boolean wx = isWall(floor, x - 1, z) || isWall(floor, x + 1, z);
                boolean wz = isWall(floor, x, z - 1) || isWall(floor, x, z + 1);
                boolean pillar = (wx && wz) || (outer && ((z == 0 || z == plan.d) ? x % 4 == 0 : z % 4 == 0));
                if (pillar) {
                    fill(x, base + 1, z, x, base + 4, z, outer ? Blocks.DARK_OAK_LOG : Blocks.STRIPPED_SPRUCE_LOG);
                } else {
                    set(x, base + 1, z, Blocks.SPRUCE_PLANKS);
                    fill(x, base + 2, z, x, base + 3, z, Blocks.SMOOTH_QUARTZ);
                    set(x, base + 4, z, outer ? Blocks.DARK_OAK_PLANKS : Blocks.STRIPPED_SPRUCE_WOOD);
                }
                if (floor == 0) {
                    set(x, F2, z, Blocks.DARK_OAK_PLANKS);   // 梁
                }
            }
        }
    }

    private void doorway(int floor, int x1, int z1, int x2, int z2) {
        int base = floor == 0 ? F1 : F2;
        fill(x1, base + 1, z1, x2, base + 3, z2, Blocks.AIR.defaultBlockState());
    }

    /** となり合う部屋のあいだ（1マスの壁）に出入り口をあける */
    private void doorways() {
        for (MapPlan.Door dr : plan.doors) {
            doorway(dr.floor(), dr.x1(), dr.z1(), dr.x2(), dr.z2());
        }
        if (!plan.autoDoors) {
            return;
        }
        List<MapPlan.Room> rs = plan.rooms;
        for (int i = 0; i < rs.size(); i++) {
            for (int j = i + 1; j < rs.size(); j++) {
                MapPlan.Room a = rs.get(i);
                MapPlan.Room b = rs.get(j);
                if (a.floor() != b.floor() || plan.noDoor.contains(a.name() + "|" + b.name())) {
                    continue;
                }
                boolean wide = a.open() || b.open() || a.name().contains("廊下") || b.name().contains("廊下")
                        || a.name().contains("回廊") || b.name().contains("回廊");
                int width = wide ? 3 : 2;
                // 東西に並ぶ（あいだが x = 壁）
                int wallX = a.x2() + 2 == b.x1() ? a.x2() + 1 : b.x2() + 2 == a.x1() ? b.x2() + 1 : Integer.MIN_VALUE;
                if (wallX != Integer.MIN_VALUE) {
                    int lo = Math.max(a.z1(), b.z1());
                    int hi = Math.min(a.z2(), b.z2());
                    cut(a.floor(), true, wallX, lo, hi, width);
                }
                int wallZ = a.z2() + 2 == b.z1() ? a.z2() + 1 : b.z2() + 2 == a.z1() ? b.z2() + 1 : Integer.MIN_VALUE;
                if (wallZ != Integer.MIN_VALUE) {
                    int lo = Math.max(a.x1(), b.x1());
                    int hi = Math.min(a.x2(), b.x2());
                    cut(a.floor(), false, wallZ, lo, hi, width);
                }
            }
        }
    }

    /** 壁の線（alongZ なら x = line の壁を z 方向に）の lo〜hi の範囲に出入り口を1〜2か所 */
    private void cut(int floor, boolean alongZ, int line, int lo, int hi, int width) {
        int len = hi - lo + 1;
        if (len < 2) {
            return;
        }
        width = Math.min(width, len);
        List<Integer> centers = new ArrayList<>();
        if (len >= 16) {
            centers.add(lo + len / 4);
            centers.add(lo + len * 3 / 4);
        } else {
            centers.add(lo + len / 2);
        }
        for (int c : centers) {
            int s = Math.max(lo, Math.min(hi - width + 1, c - width / 2));
            if (alongZ) {
                doorway(floor, line, s, line, s + width - 1);
            } else {
                doorway(floor, s, line, s + width - 1, line);
            }
        }
    }

    private void entrance() {
        int z = plan.d;
        for (int x = plan.entranceX; x <= plan.entranceX + 1; x++) {
            BlockState lower = Blocks.SPRUCE_DOOR.defaultBlockState()
                    .setValue(DoorBlock.FACING, Direction.NORTH).setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER)
                    .setValue(DoorBlock.HINGE, x == plan.entranceX ? net.minecraft.world.level.block.state.properties.DoorHingeSide.LEFT
                            : net.minecraft.world.level.block.state.properties.DoorHingeSide.RIGHT);
            set(x, F1 + 1, z, lower);
            set(x, F1 + 2, z, lower.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER));
            set(x, F1 + 3, z, Blocks.DARK_OAK_PLANKS);
        }
        // 玄関のひさしとちょうちん
        BlockState lantern = Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true);
        fill(plan.entranceX - 1, F1 + 4, z + 1, plan.entranceX + 2, F1 + 4, z + 1,
                Blocks.DEEPSLATE_TILE_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.TOP));
        set(plan.entranceX - 1, F1 + 3, z + 1, lantern);
        set(plan.entranceX + 2, F1 + 3, z + 1, lantern);
    }

    private void stairs() {
        BlockState step = Blocks.SPRUCE_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.NORTH);
        for (MapPlan.Stair s : plan.stairs) {
            for (int i = 0; i < 4; i++) {
                int z = s.zStart() - i;
                if (i > 0) {
                    fill(s.x1(), F1 + 1, z, s.x2(), F1 + i, z, Blocks.SPRUCE_PLANKS);
                }
                fill(s.x1(), F1 + 1 + i, z, s.x2(), F1 + 1 + i, z, step);
            }
        }
    }

    private void windows() {
        BlockState shoji = Blocks.WHITE_STAINED_GLASS_PANE.defaultBlockState();
        BlockState glass = Blocks.GLASS_PANE.defaultBlockState();
        for (int floor = 0; floor <= (plan.twoFloors() ? 1 : 0); floor++) {
            int base = floor == 0 ? F1 : F2;
            for (int x = 0; x <= plan.w; x++) {
                for (int z = 0; z <= plan.d; z++) {
                    if (!isWall(floor, x, z) || (floor == 0 && gardenWall(x, z))) {
                        continue;
                    }
                    if (level.getBlockState(world(x, base + 2, z)).is(Blocks.DARK_OAK_LOG)
                            || level.getBlockState(world(x, base + 2, z)).is(Blocks.STRIPPED_SPRUCE_LOG)) {
                        continue;
                    }
                    boolean outer = x == 0 || x == plan.w || z == 0 || z == plan.d;
                    boolean nearEntrance = z == plan.d && x >= plan.entranceX - 1 && x <= plan.entranceX + 2;
                    boolean facesGarden = openAt(x + 1, z) || openAt(x - 1, z) || openAt(x, z + 1) || openAt(x, z - 1);
                    boolean along = (z == 0 || z == plan.d) ? x % 4 == 2 : z % 4 == 2;
                    if (outer && along && !nearEntrance && !isAir(x, base + 2, z)) {
                        set(x, base + 2, z, shoji);
                        set(x, base + 3, z, shoji);
                    } else if (facesGarden && (x + z) % 3 != 0 && !isAir(x, base + 2, z)) {
                        set(x, base + 2, z, glass);
                        set(x, base + 3, z, glass);
                    }
                }
            }
        }
    }

    private void roofs() {
        BlockState tiles = Blocks.DEEPSLATE_TILES.defaultBlockState();
        BlockState slab = Blocks.DEEPSLATE_TILE_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.BOTTOM);
        for (int x = -1; x <= plan.w + 1; x++) {
            for (int z = -1; z <= plan.d + 1; z++) {
                boolean edge = !inside(x, z);
                int cx = Math.max(0, Math.min(plan.w, x));
                int cz = Math.max(0, Math.min(plan.d, z));
                if (!roofed(cx, cz)) {
                    continue;
                }
                boolean up = inUpper(cx, cz);
                int y = up ? roof : F2;
                if (!up) {
                    // 1階だけの所：天井（2階の床の高さ）に瓦
                    set(x, y, z, edge ? slab : tiles);
                } else {
                    set(x, y, z, edge ? slab : tiles);
                }
                // 庭のふちはひさし
                if (!edge && (openAt(x + 1, z) || openAt(x - 1, z) || openAt(x, z + 1) || openAt(x, z - 1))) {
                    set(x, y, z, slab);
                }
            }
        }
        // 2階の屋根の棟
        if (plan.twoFloors()) {
            int[] u = plan.upper;
            int mid = (u[1] + u[3]) / 2;
            BlockState n = Blocks.DEEPSLATE_TILE_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.NORTH);
            BlockState s = Blocks.DEEPSLATE_TILE_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.SOUTH);
            for (int x = u[0]; x <= u[2]; x++) {
                if (roofed(x, mid) && roofed(x, mid + 1)) {
                    set(x, roof + 1, mid, n);
                    set(x, roof + 1, mid + 1, s);
                }
            }
        }
    }

    private void lights() {
        BlockState lantern = Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true);
        for (MapPlan.Room r : plan.rooms) {
            if (r.open()) {
                continue;
            }
            int ceil = r.floor() == 0 ? F2 : roof;
            if (r.floor() == 0 && !inUpper(r.x1(), r.z1()) && !plan.twoFloors()) {
                ceil = F2;
            }
            for (int x = r.x1() + 1; x <= r.x2() - 1; x += 4) {
                for (int z = r.z1() + 1; z <= r.z2() - 1; z += 4) {
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
        for (MapPlan.Room r : plan.rooms) {
            int fy = r.floor() == 0 ? F1 : F2;
            int top = r.open() ? roof : fy + 4;
            BlockPos p1 = world(r.x1(), fy, r.z1());
            BlockPos p2 = world(r.x2(), top, r.z2());
            list.add(new MapData.Room(r.name(),
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
