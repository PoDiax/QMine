package ovh.pdx.qmine.tree;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import ovh.pdx.qmine.config.ConfigManager;
import ovh.pdx.qmine.config.QMineConfig;
import ovh.pdx.qmine.handler.BlockBreakHandler;
import ovh.pdx.qmine.handler.PlacedLogTracker;
import ovh.pdx.qmine.handler.TreeHarvestHandler;

import java.util.*;

public final class CascadeTreeTracker {
    private static final CascadeTreeTracker INSTANCE = new CascadeTreeTracker();

    public static final class ActiveCascade {
        final ServerLevel level;
        final ServerPlayer player;
        final List<BlockPos> remainingLogs;
        final Set<BlockPos> leaves;
        final BlockPos lowestLog;
        final BlockState originLog;
        int delayTicks = 0;

        public ActiveCascade(ServerLevel level, ServerPlayer player, List<BlockPos> remainingLogs, Set<BlockPos> leaves, BlockPos lowestLog, BlockState originLog) {
            this.level = level;
            this.player = player;
            this.remainingLogs = new ArrayList<>(remainingLogs);
            this.leaves = leaves;
            this.lowestLog = lowestLog;
            this.originLog = originLog;
        }
    }

    private final List<ActiveCascade> activeCascades = Collections.synchronizedList(new ArrayList<>());

    private CascadeTreeTracker() {
    }

    public static CascadeTreeTracker getInstance() {
        return INSTANCE;
    }

    public void addCascade(ActiveCascade cascade) {
        activeCascades.add(cascade);
    }

    public void onServerTick(MinecraftServer server) {
        if (activeCascades.isEmpty()) {
            return;
        }

        QMineConfig cfg = ConfigManager.getServerConfig();
        List<ActiveCascade> remaining = new ArrayList<>();

        synchronized (activeCascades) {
            for (ActiveCascade cascade : activeCascades) {
                ServerPlayer player = cascade.player;
                ServerLevel level = cascade.level;
                List<BlockPos> logs = cascade.remainingLogs;

                if (player.isRemoved() || !player.isAlive()) {
                    continue;
                }

                // Paced cascade: break 1 log every 2 ticks (100ms) for visible rhythmic domino effect
                cascade.delayTicks++;
                if (cascade.delayTicks < 2) {
                    remaining.add(cascade);
                    continue;
                }
                cascade.delayTicks = 0;

                while (!logs.isEmpty()) {
                    BlockPos logPos = logs.removeFirst();
                    BlockState state = level.getBlockState(logPos);

                    if (!state.is(BlockTags.LOGS)) {
                        continue;
                    }

                    PlacedLogTracker.getInstance().removePlaced(level, logPos);

                    ItemStack currentTool = player.getMainHandItem();
                    if (!player.isCreative() && BlockBreakHandler.isToolTooDamaged(currentTool)) {
                        logs.clear();
                        break;
                    }

                    // Domino step sound & particles
                    if (cfg.treeFellingEffects) {
                        float pitch = 0.75f + Math.min(0.6f, (float) (logPos.getY() - cascade.lowestLog.getY()) * 0.06f);
                        level.playSound(null, logPos, SoundEvents.WOOD_BREAK, SoundSource.BLOCKS, 0.9f, pitch);
                        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state),
                                logPos.getX() + 0.5, logPos.getY() + 0.5, logPos.getZ() + 0.5,
                                6, 0.25, 0.25, 0.25, 0.05);
                    }

                    try {
                        TreeHarvestHandler.setHarvesting(true);
                        player.gameMode.destroyBlock(logPos);
                    } finally {
                        TreeHarvestHandler.setHarvesting(false);
                    }

                    if (!player.isCreative() && cfg.consumeFood) {
                        player.causeFoodExhaustion((float) (cfg.exhaustionPerBlock * 0.005D));
                        if (BlockBreakHandler.isTooExhausted(player)) {
                            logs.clear();
                            break;
                        }
                    }

                    break; // Break 1 log per step for smooth domino pacing
                }

                if (logs.isEmpty()) {
                    // Cascade complete: trigger leaf decay and replant
                    if (cfg.treeHarvesterFastLeafDecay) {
                        FastLeafDecayHandler.getInstance().scheduleDecay(level, cascade.leaves);
                    }
                    if (cfg.treeHarvesterAutoReplant) {
                        TreeHarvestHandler.getInstance().replantSaplingAtBase(level, cascade.lowestLog, cascade.originLog);
                    }
                } else {
                    remaining.add(cascade);
                }
            }

            activeCascades.clear();
            activeCascades.addAll(remaining);
        }
    }

    public void clear(ServerLevel level) {
        activeCascades.removeIf(c -> c.level.dimension().equals(level.dimension()));
    }

    public void clearAll() {
        activeCascades.clear();
    }
}
