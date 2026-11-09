package ovh.pdx.qmine.tree;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import ovh.pdx.qmine.config.ConfigManager;
import ovh.pdx.qmine.config.QMineConfig;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * High-performance, natural fast leaf decay handler inspired by FallingTree and FastLeafDecay.
 * Accelerates natural vanilla leaf decay on disconnected tree canopies without breaking player-placed leaves
 * or touching leaves connected to neighboring trees.
 */
public final class FastLeafDecayHandler {
    private static final FastLeafDecayHandler INSTANCE = new FastLeafDecayHandler();

    public record PendingDecay(BlockPos pos, int delayTicks) {
        public PendingDecay tick() {
            return new PendingDecay(pos, delayTicks - 1);
        }
    }

    private final Map<ResourceKey<Level>, List<PendingDecay>> pendingDecays = new ConcurrentHashMap<>();
    private final Map<ResourceKey<Level>, Set<BlockPos>> queuedPositions = new ConcurrentHashMap<>();
    private final Random random = new Random();

    private FastLeafDecayHandler() {
    }

    public static FastLeafDecayHandler getInstance() {
        return INSTANCE;
    }

    public void scheduleDecay(ServerLevel level, Set<BlockPos> canopyLeaves) {
        if (canopyLeaves == null || canopyLeaves.isEmpty()) {
            return;
        }

        ResourceKey<Level> dim = level.dimension();
        List<PendingDecay> list = pendingDecays.computeIfAbsent(dim, k -> Collections.synchronizedList(new ArrayList<>()));
        Set<BlockPos> queued = queuedPositions.computeIfAbsent(dim, k -> ConcurrentHashMap.newKeySet());

        for (BlockPos leafPos : canopyLeaves) {
            if (queued.add(leafPos)) {
                // Staggered cascade: 1 to 12 ticks delay
                int delay = 1 + random.nextInt(12);
                list.add(new PendingDecay(leafPos, delay));
            }
        }
    }

    public void onServerTick(MinecraftServer server) {
        if (pendingDecays.isEmpty()) {
            return;
        }

        for (ServerLevel level : server.getAllLevels()) {
            ResourceKey<Level> dim = level.dimension();
            List<PendingDecay> list = pendingDecays.get(dim);
            if (list == null || list.isEmpty()) {
                continue;
            }

            Set<BlockPos> queued = queuedPositions.get(dim);
            List<PendingDecay> remaining = new ArrayList<>();
            List<PendingDecay> readyToProcess = new ArrayList<>();

            synchronized (list) {
                for (PendingDecay decay : list) {
                    PendingDecay ticked = decay.tick();
                    if (ticked.delayTicks() <= 0) {
                        readyToProcess.add(ticked);
                    } else {
                        remaining.add(ticked);
                    }
                }
                list.clear();
                list.addAll(remaining);
            }

            for (PendingDecay entry : readyToProcess) {
                BlockPos pos = entry.pos();
                if (queued != null) {
                    queued.remove(pos);
                }

                if (!level.isLoaded(pos)) {
                    continue;
                }

                BlockState state = level.getBlockState(pos);
                if (!state.is(BlockTags.LEAVES)) {
                    continue;
                }

                // Never decay player-placed leaves
                if (state.hasProperty(LeavesBlock.PERSISTENT) && state.getValue(LeavesBlock.PERSISTENT)) {
                    continue;
                }

                boolean shouldDecay = false;

                // 1. If distance is already 7 (vanilla disconnected)
                if (state.hasProperty(LeavesBlock.DISTANCE) && state.getValue(LeavesBlock.DISTANCE) == 7) {
                    shouldDecay = true;
                } else {
                    // Check if there are any logs within connecting distance (<= 4 blocks)
                    boolean hasNearbyLog = false;
                    for (int dx = -4; dx <= 4 && !hasNearbyLog; dx++) {
                        for (int dy = -4; dy <= 4 && !hasNearbyLog; dy++) {
                            for (int dz = -4; dz <= 4 && !hasNearbyLog; dz++) {
                                if (Math.abs(dx) + Math.abs(dy) + Math.abs(dz) > 6) continue;
                                BlockPos check = pos.offset(dx, dy, dz);
                                if (level.getBlockState(check).is(BlockTags.LOGS)) {
                                    hasNearbyLog = true;
                                }
                            }
                        }
                    }

                    if (!hasNearbyLog) {
                        shouldDecay = true;
                    }
                }

                if (shouldDecay) {
                    // Authentic vanilla decay: drop resources with vanilla loot tables and remove block
                    Block.dropResources(state, level, pos);
                    level.removeBlock(pos, false);

                    // Configurable decay particles and sound
                    QMineConfig cfg = ConfigManager.getServerConfig();
                    if (cfg.leafDecayParticles && cfg.leafDecaySounds) {
                        level.levelEvent(2001, pos, Block.getId(state));
                    } else if (cfg.leafDecaySounds) {
                        level.playSound(null, pos, state.getSoundType().getBreakSound(), SoundSource.BLOCKS, (state.getSoundType().getVolume() + 1.0F) / 2.0F, state.getSoundType().getPitch() * 0.8F);
                    } else if (cfg.leafDecayParticles) {
                        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state), pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 10, 0.25, 0.25, 0.25, 0.15);
                    }

                    // Propagate decay to neighboring leaves
                    if (queued != null) {
                        for (Direction dir : Direction.values()) {
                            BlockPos neighbor = pos.relative(dir);
                            BlockState neighborState = level.getBlockState(neighbor);
                            if (neighborState.is(BlockTags.LEAVES) && !neighborState.getValue(LeavesBlock.PERSISTENT)) {
                                if (queued.add(neighbor)) {
                                    list.add(new PendingDecay(neighbor, 1 + random.nextInt(3)));
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    public void clear(ServerLevel level) {
        pendingDecays.remove(level.dimension());
        queuedPositions.remove(level.dimension());
    }

    public void clearAll() {
        pendingDecays.clear();
        queuedPositions.clear();
    }
}
