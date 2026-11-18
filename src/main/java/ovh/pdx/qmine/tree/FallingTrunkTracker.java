package ovh.pdx.qmine.tree;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import ovh.pdx.qmine.config.ConfigManager;
import ovh.pdx.qmine.config.QMineConfig;
import ovh.pdx.qmine.handler.TreeHarvestHandler;

import java.util.*;

public final class FallingTrunkTracker {
    private static final FallingTrunkTracker INSTANCE = new FallingTrunkTracker();

    public record TrackedFallingBlock(
            ServerLevel level,
            FallingBlockEntity entity,
            ServerPlayer player,
            ItemStack tool,
            BlockState originalState,
            int maxAgeTicks
    ) {}

    public record TreePhysicsGroup(
            ServerLevel level,
            List<TrackedFallingBlock> blocks,
            Set<BlockPos> leaves,
            BlockPos lowestLog,
            BlockState originLog
    ) {}

    private final List<TreePhysicsGroup> activeGroups = Collections.synchronizedList(new ArrayList<>());

    private FallingTrunkTracker() {
    }

    public static FallingTrunkTracker getInstance() {
        return INSTANCE;
    }

    public void addGroup(TreePhysicsGroup group) {
        activeGroups.add(group);
    }

    public void onServerTick(MinecraftServer server) {
        if (activeGroups.isEmpty()) {
            return;
        }

        QMineConfig cfg = ConfigManager.getServerConfig();
        List<TreePhysicsGroup> remainingGroups = new ArrayList<>();

        synchronized (activeGroups) {
            for (TreePhysicsGroup group : activeGroups) {
                ServerLevel level = group.level();
                List<TrackedFallingBlock> remainingBlocks = new ArrayList<>();

                for (TrackedFallingBlock tracked : group.blocks()) {
                    FallingBlockEntity entity = tracked.entity();

                    // Check if block has landed or expired
                    boolean landed = entity.isRemoved()
                            || entity.onGround()
                            || entity.verticalCollision
                            || entity.time >= tracked.maxAgeTicks();

                    if (landed) {
                        BlockPos impactPos = entity.blockPosition();
                        BlockPos targetPos = level.getBlockState(impactPos).canBeReplaced() ? impactPos : impactPos.above();

                        // Place as solid fallen log on the ground
                        if (level.getBlockState(targetPos).canBeReplaced()) {
                            level.setBlock(targetPos, tracked.originalState(), Block.UPDATE_ALL);
                            FallenTreeTracker.getInstance().registerFallenLog(level, targetPos);
                        } else {
                            Block.dropResources(tracked.originalState(), level, targetPos, null, tracked.player(), tracked.tool());
                        }

                        entity.discard();

                        // Impact effects
                        if (cfg.treeFellingEffects) {
                            level.playSound(null, targetPos, SoundEvents.WOOD_PLACE, SoundSource.BLOCKS, 0.9f, 0.8f);
                        }
                        if (cfg.leafDecayParticles) {
                            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, tracked.originalState()),
                                    targetPos.getX() + 0.5, targetPos.getY() + 0.5, targetPos.getZ() + 0.5,
                                    6, 0.25, 0.25, 0.25, 0.1);
                        }
                    } else {
                        remainingBlocks.add(tracked);
                    }
                }

                if (remainingBlocks.isEmpty()) {
                    // All falling logs have landed: trigger leaf decay and replant
                    if (cfg.treeHarvesterFastLeafDecay) {
                        FastLeafDecayHandler.getInstance().scheduleDecay(level, group.leaves());
                    }
                    if (cfg.treeHarvesterAutoReplant) {
                        TreeHarvestHandler.getInstance().replantSaplingAtBase(level, group.lowestLog(), group.originLog());
                    }
                } else {
                    remainingGroups.add(new TreePhysicsGroup(level, remainingBlocks, group.leaves(), group.lowestLog(), group.originLog()));
                }
            }

            activeGroups.clear();
            activeGroups.addAll(remainingGroups);
        }
    }

    public void clearAll() {
        activeGroups.clear();
    }
}
