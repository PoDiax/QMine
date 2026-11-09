package ovh.pdx.qmine.tree;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import ovh.pdx.qmine.config.ConfigManager;
import ovh.pdx.qmine.config.QMineConfig;
import ovh.pdx.qmine.handler.BlockBreakHandler;
import ovh.pdx.qmine.handler.PlacedLogTracker;
import ovh.pdx.qmine.handler.TreeHarvestHandler;

import java.util.*;

public final class ShiftDownTreeTracker {
    private static final ShiftDownTreeTracker INSTANCE = new ShiftDownTreeTracker();

    public static final class ActiveShiftTree {
        final ServerLevel level;
        final ServerPlayer player;
        List<BlockPos> remainingLogs;
        final Set<BlockPos> leaves;
        final BlockPos lowestLog;
        final BlockState originLog;
        int delayTicks = 0;

        public ActiveShiftTree(ServerLevel level, ServerPlayer player, List<BlockPos> remainingLogs, Set<BlockPos> leaves, BlockPos lowestLog, BlockState originLog) {
            this.level = level;
            this.player = player;
            this.remainingLogs = new ArrayList<>(remainingLogs);
            this.leaves = leaves;
            this.lowestLog = lowestLog;
            this.originLog = originLog;
        }
    }

    private final List<ActiveShiftTree> activeTrees = Collections.synchronizedList(new ArrayList<>());

    private ShiftDownTreeTracker() {
    }

    public static ShiftDownTreeTracker getInstance() {
        return INSTANCE;
    }

    public void addTree(ActiveShiftTree tree) {
        activeTrees.add(tree);
    }

    public void onServerTick(MinecraftServer server) {
        if (activeTrees.isEmpty()) {
            return;
        }

        QMineConfig cfg = ConfigManager.getServerConfig();
        List<ActiveShiftTree> remaining = new ArrayList<>();

        synchronized (activeTrees) {
            for (ActiveShiftTree tree : activeTrees) {
                ServerPlayer player = tree.player;
                ServerLevel level = tree.level;

                if (player.isRemoved() || !player.isAlive()) {
                    continue;
                }

                tree.delayTicks++;
                if (tree.delayTicks < 2) {
                    remaining.add(tree);
                    continue;
                }
                tree.delayTicks = 0;

                if (tree.remainingLogs.isEmpty()) {
                    completeTree(tree, cfg);
                    continue;
                }

                // 1. Shift remaining logs down by 1 block
                tree.remainingLogs.sort(Comparator.comparingInt(BlockPos::getY));
                List<BlockPos> shiftedLogs = new ArrayList<>();

                for (BlockPos pos : tree.remainingLogs) {
                    BlockPos targetPos = pos.below();
                    BlockState state = level.getBlockState(pos);
                    if (state.is(BlockTags.LOGS)) {
                        level.setBlock(targetPos, state, Block.UPDATE_ALL);
                        level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                        PlacedLogTracker.getInstance().removePlaced(level, pos);
                        PlacedLogTracker.getInstance().removePlaced(level, targetPos);
                        shiftedLogs.add(targetPos);
                    }
                }
                tree.remainingLogs = shiftedLogs;

                if (tree.remainingLogs.isEmpty()) {
                    completeTree(tree, cfg);
                    continue;
                }

                // Wood step down effects
                if (cfg.treeFellingEffects) {
                    level.playSound(null, tree.lowestLog, SoundEvents.WOOD_PLACE, SoundSource.BLOCKS, 0.9f, 0.8f);
                    level.playSound(null, tree.lowestLog, SoundEvents.SCAFFOLDING_FALL, SoundSource.BLOCKS, 0.5f, 1.2f);
                    level.sendParticles(ParticleTypes.HAPPY_VILLAGER, tree.lowestLog.getX() + 0.5, tree.lowestLog.getY() + 0.5, tree.lowestLog.getZ() + 0.5, 3, 0.2, 0.2, 0.2, 0.05);
                }

                // 2. Break the lowest slice now sitting at the base
                int currentMinY = tree.remainingLogs.stream().mapToInt(BlockPos::getY).min().orElse(Integer.MAX_VALUE);
                Iterator<BlockPos> it = tree.remainingLogs.iterator();

                while (it.hasNext()) {
                    BlockPos pos = it.next();
                    if (pos.getY() == currentMinY) {
                        it.remove();
                        PlacedLogTracker.getInstance().removePlaced(level, pos);

                        ItemStack currentTool = player.getMainHandItem();
                        if (!player.isCreative() && BlockBreakHandler.isToolTooDamaged(currentTool)) {
                            tree.remainingLogs.clear();
                            break;
                        }

                        try {
                            TreeHarvestHandler.setHarvesting(true);
                            player.gameMode.destroyBlock(pos);
                        } finally {
                            TreeHarvestHandler.setHarvesting(false);
                        }

                        if (!player.isCreative() && cfg.consumeFood) {
                            player.causeFoodExhaustion((float) (cfg.exhaustionPerBlock * 0.005D));
                            if (BlockBreakHandler.isTooExhausted(player)) {
                                tree.remainingLogs.clear();
                                break;
                            }
                        }
                    }
                }

                if (tree.remainingLogs.isEmpty()) {
                    completeTree(tree, cfg);
                } else {
                    remaining.add(tree);
                }
            }

            activeTrees.clear();
            activeTrees.addAll(remaining);
        }
    }

    private void completeTree(ActiveShiftTree tree, QMineConfig cfg) {
        if (cfg.treeHarvesterFastLeafDecay) {
            FastLeafDecayHandler.getInstance().scheduleDecay(tree.level, tree.leaves);
        }
        if (cfg.treeHarvesterAutoReplant) {
            TreeHarvestHandler.getInstance().replantSaplingAtBase(tree.level, tree.lowestLog, tree.originLog);
        }
    }

    public void clear(ServerLevel level) {
        activeTrees.removeIf(t -> t.level.dimension().equals(level.dimension()));
    }

    public void clearAll() {
        activeTrees.clear();
    }
}
