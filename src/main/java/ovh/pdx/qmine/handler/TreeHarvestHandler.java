package ovh.pdx.qmine.handler;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;
import ovh.pdx.qmine.Qmine;
import ovh.pdx.qmine.config.ConfigManager;
import ovh.pdx.qmine.config.QMineConfig;
import ovh.pdx.qmine.tree.*;

import java.util.*;

public final class TreeHarvestHandler {
    private static final TreeHarvestHandler INSTANCE = new TreeHarvestHandler();

    public enum HarvestResult {
        IGNORED,
        CANCEL_EVENT,
        ALLOW_EVENT
    }

    private static final ThreadLocal<Boolean> IS_HARVESTING = ThreadLocal.withInitial(() -> false);

    public static boolean isHarvesting() {
        return IS_HARVESTING.get();
    }

    public static void setHarvesting(boolean harvesting) {
        IS_HARVESTING.set(harvesting);
    }

    private TreeHarvestHandler() {
    }

    public static TreeHarvestHandler getInstance() {
        return INSTANCE;
    }

    @Nullable
    public BlockState getSaplingForLog(BlockState logState) {
        String name = logState.getBlock().getDescriptionId(); // e.g. block.minecraft.oak_log
        if (name.contains("spruce")) return Blocks.SPRUCE_SAPLING.defaultBlockState();
        if (name.contains("birch")) return Blocks.BIRCH_SAPLING.defaultBlockState();
        if (name.contains("jungle")) return Blocks.JUNGLE_SAPLING.defaultBlockState();
        if (name.contains("acacia")) return Blocks.ACACIA_SAPLING.defaultBlockState();
        if (name.contains("dark_oak")) return Blocks.DARK_OAK_SAPLING.defaultBlockState();
        if (name.contains("mangrove")) return Blocks.MANGROVE_PROPAGULE.defaultBlockState();
        if (name.contains("cherry")) return Blocks.CHERRY_SAPLING.defaultBlockState();
        if (name.contains("pale_oak")) return Blocks.PALE_OAK_SAPLING.defaultBlockState();
        if (name.contains("crimson")) return Blocks.CRIMSON_FUNGUS.defaultBlockState();
        if (name.contains("warped")) return Blocks.WARPED_FUNGUS.defaultBlockState();
        if (name.contains("oak")) return Blocks.OAK_SAPLING.defaultBlockState();
        return null;
    }

    public HarvestResult harvestTree(@Nullable ServerLevel level, BlockPos originPos, BlockState originLog, ServerPlayer player, QMineConfig config) {
        if (level == null || !originLog.is(BlockTags.LOGS) || isHarvesting()) {
            return HarvestResult.IGNORED;
        }

        // 0. Check if this is a fallen tree on the ground (from Falling Blocks mode)
        if (FallenTreeTracker.getInstance().isFallenLog(level, originPos)) {
            return harvestFallenTree(level, originPos, originLog, player, config);
        }

        // 1. Require axe if configured
        if (config.treeHarvesterRequireAxe) {
            ItemStack mainHand = player.getMainHandItem();
            if (mainHand.isEmpty() || (!mainHand.is(ItemTags.AXES) && !mainHand.isDamageableItem())) {
                return HarvestResult.IGNORED;
            }
        }

        // 2. Tool break protection check before starting
        if (BlockBreakHandler.isToolTooDamaged(player.getMainHandItem())) {
            return HarvestResult.IGNORED;
        }

        // 3. Scan and strictly verify natural tree (FallingTree-inspired architecture)
        // Automatically rejects player-placed logs, building structures, pillars, and canopy-less poles
        Optional<NaturalTree> optTree = NaturalTreeScanner.scan(level, originPos, config.treeHarvesterMaxLogs);
        if (optTree.isEmpty()) {
            return HarvestResult.IGNORED;
        }

        NaturalTree tree = optTree.get();
        TreeFellingAnimation anim = config.treeFellingAnimation != null ? config.treeFellingAnimation : TreeFellingAnimation.INSTANT;

        return switch (anim) {
            case SHIFT_DOWN -> executeShiftDown(level, originPos, originLog, tree, player, config);
            case FALL_PHYSICS -> executeFallPhysics(level, originPos, originLog, tree, player, config);
            case CASCADE -> executeCascade(level, originPos, originLog, tree, player, config);
            case INSTANT -> executeInstant(level, originPos, originLog, tree, player, config);
        };
    }

    public HarvestResult harvestFallenTree(ServerLevel level, BlockPos originPos, BlockState originLog, ServerPlayer player, QMineConfig config) {
        Set<BlockPos> fallenLogs = FallenTreeTracker.getInstance().scanFallenTree(level, originPos, config.treeHarvesterMaxLogs);
        if (fallenLogs.isEmpty()) {
            return HarvestResult.IGNORED;
        }

        Qmine qmine = Qmine.getInstance();
        boolean hadMainHandItem = !player.getMainHandItem().isEmpty();
        int minedCount = 0;

        try {
            setHarvesting(true);
            qmine.getDropsAccumulator().start();

            for (BlockPos pos : fallenLogs) {
                BlockState targetState = level.getBlockState(pos);
                if (!targetState.is(BlockTags.LOGS)) {
                    continue;
                }

                FallenTreeTracker.getInstance().unregister(level, pos);
                PlacedLogTracker.getInstance().removePlaced(level, pos);

                if (!player.gameMode.destroyBlock(pos)) {
                    continue;
                }

                minedCount++;

                if (!player.isCreative()) {
                    if (config.consumeFood) {
                        player.causeFoodExhaustion((float) (config.exhaustionPerBlock * 0.005D));
                        if (BlockBreakHandler.isTooExhausted(player)) {
                            break;
                        }
                    }

                    ItemStack currentTool = player.getMainHandItem();
                    if (hadMainHandItem && (currentTool.isEmpty() || BlockBreakHandler.isToolTooDamaged(currentTool))) {
                        break;
                    }
                }
            }
        } finally {
            qmine.getDropsAccumulator().finish(level, originPos, player, config.dropCollection);
            setHarvesting(false);
        }

        if (minedCount > 0) {
            if (config.treeFellingEffects) {
                level.playSound(null, originPos, SoundEvents.WOOD_BREAK, SoundSource.BLOCKS, 1.0f, 0.8f);
                level.playSound(null, originPos, SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.3f, 1.2f);
                level.sendParticles(ParticleTypes.HAPPY_VILLAGER, originPos.getX() + 0.5, originPos.getY() + 0.5, originPos.getZ() + 0.5, 6, 0.4, 0.4, 0.4, 0.05);
            }
            return HarvestResult.CANCEL_EVENT;
        }

        return HarvestResult.IGNORED;
    }

    public boolean harvestWholeTree(@Nullable ServerLevel level, BlockPos originPos, BlockState originLog, ServerPlayer player, QMineConfig config) {
        HarvestResult result = harvestTree(level, originPos, originLog, player, config);
        return result != HarvestResult.IGNORED;
    }

    private HarvestResult executeInstant(ServerLevel level, BlockPos originPos, BlockState originLog, NaturalTree tree, ServerPlayer player, QMineConfig config) {
        Qmine qmine = Qmine.getInstance();
        boolean hadMainHandItem = !player.getMainHandItem().isEmpty();
        int minedCount = 0;

        try {
            setHarvesting(true);
            qmine.getDropsAccumulator().start();

            // HARVEST LOGS ONLY - Leaves are never touched or destroyed by the harvester!
            for (BlockPos logPos : tree.logs()) {
                BlockState targetState = level.getBlockState(logPos);
                if (!targetState.is(BlockTags.LOGS)) {
                    continue;
                }

                PlacedLogTracker.getInstance().removePlaced(level, logPos);

                if (!player.gameMode.destroyBlock(logPos)) {
                    continue;
                }

                minedCount++;

                if (!player.isCreative()) {
                    if (config.consumeFood) {
                        player.causeFoodExhaustion((float) (config.exhaustionPerBlock * 0.005D));
                        if (BlockBreakHandler.isTooExhausted(player)) {
                            break;
                        }
                    }

                    ItemStack currentTool = player.getMainHandItem();
                    if (hadMainHandItem && (currentTool.isEmpty() || BlockBreakHandler.isToolTooDamaged(currentTool))) {
                        break;
                    }
                }
            }
        } finally {
            qmine.getDropsAccumulator().finish(level, originPos, player, config.dropCollection);
            setHarvesting(false);
        }

        if (minedCount > 0) {
            if (config.treeFellingEffects) {
                level.playSound(null, originPos, SoundEvents.WOOD_BREAK, SoundSource.BLOCKS, 1.0f, 0.8f);
                level.playSound(null, originPos, SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.3f, 1.2f);
                level.sendParticles(ParticleTypes.HAPPY_VILLAGER, originPos.getX() + 0.5, originPos.getY() + 0.5, originPos.getZ() + 0.5, 6, 0.4, 0.4, 0.4, 0.05);
            }

            // Replant sapling at base soil if configured (NO LEAVES TOUCHED)
            if (config.treeHarvesterAutoReplant) {
                replantSaplingAtBase(level, tree.lowestLog(), originLog);
            }

            // Trigger fast natural leaf decay on the tree canopy
            if (config.treeHarvesterFastLeafDecay) {
                FastLeafDecayHandler.getInstance().scheduleDecay(level, tree.leaves());
            }

            return HarvestResult.CANCEL_EVENT;
        }

        return HarvestResult.IGNORED;
    }

    private HarvestResult executeShiftDown(ServerLevel level, BlockPos originPos, BlockState originLog, NaturalTree tree, ServerPlayer player, QMineConfig config) {
        PlacedLogTracker.getInstance().removePlaced(level, originPos);

        int lowestY = tree.lowestLog().getY();
        List<BlockPos> upperLogs = new ArrayList<>();

        for (BlockPos pos : tree.logs()) {
            if (pos.equals(originPos)) {
                continue;
            }
            if (pos.getY() == lowestY) {
                // Instantly break any other base logs at the lowest level so the trunk can shift down
                PlacedLogTracker.getInstance().removePlaced(level, pos);
                try {
                    setHarvesting(true);
                    player.gameMode.destroyBlock(pos);
                } finally {
                    setHarvesting(false);
                }
            } else {
                upperLogs.add(pos);
            }
        }

        if (upperLogs.isEmpty()) {
            if (config.treeFellingEffects) {
                level.playSound(null, originPos, SoundEvents.WOOD_BREAK, SoundSource.BLOCKS, 1.0f, 0.8f);
                level.sendParticles(ParticleTypes.HAPPY_VILLAGER, originPos.getX() + 0.5, originPos.getY() + 0.5, originPos.getZ() + 0.5, 6, 0.4, 0.4, 0.4, 0.05);
            }
            if (config.treeHarvesterAutoReplant) {
                replantSaplingAtBase(level, tree.lowestLog(), originLog);
            }
            if (config.treeHarvesterFastLeafDecay) {
                FastLeafDecayHandler.getInstance().scheduleDecay(level, tree.leaves());
            }
            return HarvestResult.ALLOW_EVENT;
        }

        upperLogs.sort(Comparator.comparingInt(BlockPos::getY));

        ShiftDownTreeTracker.getInstance().addTree(new ShiftDownTreeTracker.ActiveShiftTree(
                level, player, upperLogs, tree.leaves(), tree.lowestLog(), originLog
        ));

        if (config.treeFellingEffects) {
            level.playSound(null, originPos, SoundEvents.WOOD_BREAK, SoundSource.BLOCKS, 1.0f, 0.8f);
        }

        return HarvestResult.ALLOW_EVENT;
    }

    private HarvestResult executeFallPhysics(ServerLevel level, BlockPos originPos, BlockState originLog, NaturalTree tree, ServerPlayer player, QMineConfig config) {
        PlacedLogTracker.getInstance().removePlaced(level, originPos);

        ItemStack tool = player.getMainHandItem().copy();
        List<FallingTrunkTracker.TrackedFallingBlock> trackedBlocks = new ArrayList<>();
        net.minecraft.core.Direction playerFacing = player.getDirection();

        for (BlockPos logPos : tree.logs()) {
            if (logPos.equals(originPos)) {
                continue;
            }

            BlockState state = level.getBlockState(logPos);
            if (!state.is(BlockTags.LOGS)) {
                continue;
            }

            // Durability and food exhaustion check
            if (!player.isCreative()) {
                if (BlockBreakHandler.isToolTooDamaged(player.getMainHandItem())) {
                    break;
                }
                if (config.consumeFood) {
                    player.causeFoodExhaustion((float) (config.exhaustionPerBlock * 0.005D));
                    if (BlockBreakHandler.isTooExhausted(player)) {
                        break;
                    }
                }
            }

            PlacedLogTracker.getInstance().removePlaced(level, logPos);
            level.setBlock(logPos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);

            FallingBlockEntity entity = FallingBlockEntity.fall(level, logPos, state);
            entity.dropItem = false;

            // Outward tumble momentum away from tree trunk and in player's forward direction
            double relX = logPos.getX() - originPos.getX();
            double relZ = logPos.getZ() - originPos.getZ();
            double dx = relX * 0.10 + playerFacing.getStepX() * 0.08 + (level.getRandom().nextDouble() - 0.5) * 0.06;
            double dz = relZ * 0.10 + playerFacing.getStepZ() * 0.08 + (level.getRandom().nextDouble() - 0.5) * 0.06;
            double dy = 0.08 + level.getRandom().nextDouble() * 0.05;

            entity.setDeltaMovement(dx, dy, dz);

            trackedBlocks.add(new FallingTrunkTracker.TrackedFallingBlock(
                    level, entity, player, tool, state, 120
            ));
        }

        if (!trackedBlocks.isEmpty()) {
            FallingTrunkTracker.getInstance().addGroup(new FallingTrunkTracker.TreePhysicsGroup(
                    level, trackedBlocks, tree.leaves(), tree.lowestLog(), originLog
            ));
            if (config.treeFellingEffects) {
                level.playSound(null, originPos, SoundEvents.WOOD_BREAK, SoundSource.BLOCKS, 1.0f, 0.7f);
            }
        } else {
            // Single log tree
            if (config.treeHarvesterFastLeafDecay) {
                FastLeafDecayHandler.getInstance().scheduleDecay(level, tree.leaves());
            }
            if (config.treeHarvesterAutoReplant) {
                replantSaplingAtBase(level, tree.lowestLog(), originLog);
            }
        }

        // Return ALLOW_EVENT so Minecraft destroys originPos naturally on this tick without re-entering
        return HarvestResult.ALLOW_EVENT;
    }

    private HarvestResult executeCascade(ServerLevel level, BlockPos originPos, BlockState originLog, NaturalTree tree, ServerPlayer player, QMineConfig config) {
        PlacedLogTracker.getInstance().removePlaced(level, originPos);

        List<BlockPos> remainingLogs = new ArrayList<>();
        for (BlockPos log : tree.logs()) {
            if (!log.equals(originPos)) {
                remainingLogs.add(log);
            }
        }

        if (remainingLogs.isEmpty()) {
            if (config.treeHarvesterFastLeafDecay) {
                FastLeafDecayHandler.getInstance().scheduleDecay(level, tree.leaves());
            }
            if (config.treeHarvesterAutoReplant) {
                replantSaplingAtBase(level, tree.lowestLog(), originLog);
            }
            return HarvestResult.ALLOW_EVENT;
        }

        // Sort outward from the cut origin point for a natural ripple domino cascade
        remainingLogs.sort(Comparator.comparingDouble(pos -> pos.distSqr(originPos)));

        CascadeTreeTracker.getInstance().addCascade(new CascadeTreeTracker.ActiveCascade(
                level, player, remainingLogs, tree.leaves(), tree.lowestLog(), originLog
        ));

        if (config.treeFellingEffects) {
            level.playSound(null, originPos, SoundEvents.WOOD_BREAK, SoundSource.BLOCKS, 1.0f, 0.8f);
        }

        // Return ALLOW_EVENT so Minecraft destroys originPos naturally on this tick without re-entering
        return HarvestResult.ALLOW_EVENT;
    }

    public void replantSaplingAtBase(ServerLevel level, BlockPos basePos, BlockState originLog) {
        BlockPos groundPos = basePos.below();
        BlockState groundState = level.getBlockState(groundPos);
        if (NaturalTreeScanner.isValidTreeSoil(groundState) && level.getBlockState(basePos).isAir()) {
            BlockState sapling = getSaplingForLog(originLog);
            if (sapling != null && sapling.canSurvive(level, basePos)) {
                level.setBlockAndUpdate(basePos, sapling);
                if (ConfigManager.getServerConfig().treeFellingEffects) {
                    level.playSound(null, basePos, SoundEvents.GRASS_PLACE, SoundSource.BLOCKS, 1.0f, 1.0f);
                }
            }
        }
    }
}
