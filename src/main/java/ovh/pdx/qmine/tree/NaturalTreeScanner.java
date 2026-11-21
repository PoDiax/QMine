package ovh.pdx.qmine.tree;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import ovh.pdx.qmine.handler.PlacedLogTracker;

import java.util.*;

/**
 * Scans connected logs and verifies natural tree structure before felling.
 */
public final class NaturalTreeScanner {
    private static final int HORIZONTAL_SEARCH_RADIUS = 16;
    private static final int LEAF_SEARCH_DISTANCE = 4;
    private static final int MIN_CANOPY_LEAVES = 3;
    private static final int MIN_CROWN_LEAVES_AROUND_TOP = 2;

    private NaturalTreeScanner() {
    }

    public static boolean isNaturalLeaf(BlockState state) {
        if (!state.is(BlockTags.LEAVES)) {
            return false;
        }
        if (state.hasProperty(LeavesBlock.PERSISTENT)) {
            return !state.getValue(LeavesBlock.PERSISTENT);
        }
        return true;
    }

    public static boolean isPlayerBuildingBlock(BlockState state) {
        return state.is(BlockTags.PLANKS)
                || state.is(BlockTags.WOODEN_SLABS)
                || state.is(BlockTags.SLABS)
                || state.is(BlockTags.WOODEN_STAIRS)
                || state.is(BlockTags.STAIRS)
                || state.is(BlockTags.WOODEN_FENCES)
                || state.is(BlockTags.FENCES)
                || state.is(BlockTags.FENCE_GATES)
                || state.is(BlockTags.WOODEN_DOORS)
                || state.is(BlockTags.DOORS)
                || state.is(BlockTags.WOODEN_TRAPDOORS)
                || state.is(BlockTags.TRAPDOORS)
                || state.is(BlockTags.WALLS)
                || state.is(BlockTags.BEDS)
                || state.is(BlockTags.WOOL_CARPETS)
                || state.is(Blocks.GLASS)
                || state.is(Blocks.GLASS_PANE)
                || state.is(Blocks.TINTED_GLASS)
                || state.is(Blocks.COBBLESTONE)
                || state.is(Blocks.STONE_BRICKS)
                || state.is(Blocks.MOSSY_STONE_BRICKS)
                || state.is(Blocks.CRACKED_STONE_BRICKS)
                || state.is(Blocks.CHISELED_STONE_BRICKS)
                || state.is(Blocks.BRICKS)
                || state.is(Blocks.SMOOTH_STONE)
                || state.is(Blocks.DEEPSLATE_BRICKS)
                || state.is(Blocks.DEEPSLATE_TILES)
                || state.is(Blocks.POLISHED_DEEPSLATE)
                || state.is(Blocks.COBBLED_DEEPSLATE)
                || state.is(Blocks.SANDSTONE)
                || state.is(Blocks.RED_SANDSTONE)
                || state.is(Blocks.TORCH)
                || state.is(Blocks.WALL_TORCH)
                || state.is(Blocks.LANTERN)
                || state.is(Blocks.SOUL_LANTERN)
                || state.is(Blocks.CAMPFIRE)
                || state.is(Blocks.SOUL_CAMPFIRE)
                || state.is(Blocks.CRAFTING_TABLE)
                || state.is(Blocks.FURNACE)
                || state.is(Blocks.BLAST_FURNACE)
                || state.is(Blocks.SMOKER)
                || state.is(Blocks.CHEST)
                || state.is(Blocks.TRAPPED_CHEST)
                || state.is(Blocks.BARREL)
                || state.is(Blocks.BOOKSHELF)
                || state.is(Blocks.CHISELED_BOOKSHELF)
                || state.is(Blocks.ANVIL)
                || state.is(Blocks.CHIPPED_ANVIL)
                || state.is(Blocks.DAMAGED_ANVIL)
                || state.is(Blocks.ENCHANTING_TABLE)
                || state.is(Blocks.BREWING_STAND)
                || state.is(Blocks.CAULDRON)
                || state.is(Blocks.HOPPER)
                || state.is(Blocks.DISPENSER)
                || state.is(Blocks.DROPPER);
    }

    public static boolean isValidTreeSoil(BlockState state) {
        return state.is(BlockTags.DIRT)
                || state.is(BlockTags.NYLIUM)
                || state.is(Blocks.MUD)
                || state.is(Blocks.MUDDY_MANGROVE_ROOTS)
                || state.is(Blocks.MOSS_BLOCK)
                || state.is(Blocks.FARMLAND)
                || state.is(Blocks.CLAY)
                || state.is(Blocks.SAND)
                || state.is(Blocks.RED_SAND);
    }

    public static Optional<NaturalTree> scan(ServerLevel level, BlockPos originPos, int maxLogs) {
        BlockState originState = level.getBlockState(originPos);
        if (!originState.is(BlockTags.LOGS)) {
            return Optional.empty();
        }

        if (PlacedLogTracker.getInstance().isPlayerPlaced(level, originPos)) {
            return Optional.empty();
        }

        Block originBlock = originState.getBlock();
        PlacedLogTracker tracker = PlacedLogTracker.getInstance();

        Set<BlockPos> discoveredLogs = new HashSet<>();
        Set<BlockPos> discoveredLeaves = new HashSet<>();
        Queue<BlockPos> queue = new ArrayDeque<>();

        queue.add(originPos);
        discoveredLogs.add(originPos);

        while (!queue.isEmpty()) {
            BlockPos current = queue.poll();

            for (Direction dir : Direction.values()) {
                BlockState adjacent = level.getBlockState(current.relative(dir));
                if (isPlayerBuildingBlock(adjacent)) {
                    return Optional.empty();
                }
            }

            for (int dx = -LEAF_SEARCH_DISTANCE; dx <= LEAF_SEARCH_DISTANCE; dx++) {
                for (int dy = -1; dy <= LEAF_SEARCH_DISTANCE; dy++) {
                    for (int dz = -LEAF_SEARCH_DISTANCE; dz <= LEAF_SEARCH_DISTANCE; dz++) {
                        if (Math.abs(dx) + Math.abs(dy) + Math.abs(dz) > 5) continue;
                        BlockPos leafPos = current.offset(dx, dy, dz);
                        if (discoveredLeaves.contains(leafPos)) continue;

                        BlockState leafState = level.getBlockState(leafPos);
                        if (isNaturalLeaf(leafState)) {
                            discoveredLeaves.add(leafPos);
                        }
                    }
                }
            }

            for (int x = -1; x <= 1; x++) {
                for (int y = -1; y <= 1; y++) {
                    for (int z = -1; z <= 1; z++) {
                        if (x == 0 && y == 0 && z == 0) continue;

                        BlockPos neighbor = current.offset(x, y, z);

                        if (Math.abs(neighbor.getX() - originPos.getX()) > HORIZONTAL_SEARCH_RADIUS
                                || Math.abs(neighbor.getZ() - originPos.getZ()) > HORIZONTAL_SEARCH_RADIUS) {
                            continue;
                        }

                        if (discoveredLogs.contains(neighbor)) {
                            continue;
                        }

                        BlockState neighborState = level.getBlockState(neighbor);
                        if (neighborState.is(BlockTags.LOGS) && (neighborState.getBlock() == originBlock || neighborState.is(BlockTags.LOGS))) {
                            if (tracker.isPlayerPlaced(level, neighbor)) {
                                return Optional.empty();
                            }

                            discoveredLogs.add(neighbor);
                            queue.add(neighbor);

                            if (discoveredLogs.size() > maxLogs) {
                                return Optional.empty();
                            }
                        }
                    }
                }
            }
        }

        if (discoveredLeaves.size() < MIN_CANOPY_LEAVES) {
            return Optional.empty();
        }

        BlockPos topMostLog = originPos;
        BlockPos lowestLog = originPos;
        for (BlockPos log : discoveredLogs) {
            if (log.getY() > topMostLog.getY()) {
                topMostLog = log;
            }
            if (log.getY() < lowestLog.getY()) {
                lowestLog = log;
            }
        }

        // Natural trees have leaves around their crown; pillars/poles do not
        int crownLeaves = 0;
        for (int dx = -2; dx <= 2; dx++) {
            for (int dy = -1; dy <= 2; dy++) {
                for (int dz = -2; dz <= 2; dz++) {
                    BlockPos p = topMostLog.offset(dx, dy, dz);
                    if (isNaturalLeaf(level.getBlockState(p))) {
                        crownLeaves++;
                    }
                }
            }
        }
        if (crownLeaves < MIN_CROWN_LEAVES_AROUND_TOP) {
            return Optional.empty();
        }

        // At least one base log must rest on tree soil
        boolean hasSoil = false;
        for (BlockPos log : discoveredLogs) {
            BlockState groundState = level.getBlockState(log.below());
            if (isValidTreeSoil(groundState)) {
                hasSoil = true;
                break;
            }
        }
        if (!hasSoil) {
            return Optional.empty();
        }

        // Sort descending Y for top-down harvesting
        List<BlockPos> sortedLogs = new ArrayList<>(discoveredLogs);
        sortedLogs.sort((a, b) -> Integer.compare(b.getY(), a.getY()));

        return Optional.of(new NaturalTree(originPos, originState, sortedLogs, discoveredLeaves, lowestLog, topMostLog));
    }
}
