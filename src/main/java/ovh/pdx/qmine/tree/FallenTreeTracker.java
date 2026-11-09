package ovh.pdx.qmine.tree;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import ovh.pdx.qmine.handler.PlacedLogTracker;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks logs that have fallen from the sky during Falling Blocks mode.
 * Allows players to harvest fallen tree trunks on the ground without needing leaves.
 */
public final class FallenTreeTracker {
    private static final FallenTreeTracker INSTANCE = new FallenTreeTracker();

    private final Map<ResourceKey<Level>, Set<Long>> fallenLogs = new ConcurrentHashMap<>();

    private FallenTreeTracker() {
    }

    public static FallenTreeTracker getInstance() {
        return INSTANCE;
    }

    public void registerFallenLog(ServerLevel level, BlockPos pos) {
        fallenLogs.computeIfAbsent(level.dimension(), k -> ConcurrentHashMap.newKeySet()).add(pos.asLong());
        PlacedLogTracker.getInstance().removePlaced(level, pos);
    }

    public void unregister(ServerLevel level, BlockPos pos) {
        Set<Long> set = fallenLogs.get(level.dimension());
        if (set != null) {
            set.remove(pos.asLong());
        }
    }

    public boolean isFallenLog(ServerLevel level, BlockPos pos) {
        Set<Long> set = fallenLogs.get(level.dimension());
        return set != null && set.contains(pos.asLong());
    }

    /**
     * Gathers all connected fallen logs in the cluster (up to maxLogs).
     */
    public Set<BlockPos> scanFallenTree(ServerLevel level, BlockPos originPos, int maxLogs) {
        Set<BlockPos> discovered = new LinkedHashSet<>();
        Queue<BlockPos> queue = new ArrayDeque<>();

        discovered.add(originPos);
        queue.add(originPos);

        while (!queue.isEmpty() && discovered.size() < maxLogs) {
            BlockPos current = queue.poll();
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        if (dx == 0 && dy == 0 && dz == 0) continue;
                        BlockPos neighbor = current.offset(dx, dy, dz);
                        if (!discovered.contains(neighbor)) {
                            BlockState state = level.getBlockState(neighbor);
                            if (state.is(BlockTags.LOGS) && (isFallenLog(level, neighbor) || !PlacedLogTracker.getInstance().isPlayerPlaced(level, neighbor))) {
                                discovered.add(neighbor);
                                queue.add(neighbor);
                            }
                        }
                    }
                }
            }
        }

        return discovered;
    }

    public void clear(ServerLevel level) {
        fallenLogs.remove(level.dimension());
    }

    public void clearAll() {
        fallenLogs.clear();
    }
}
