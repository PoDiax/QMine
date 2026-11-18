package ovh.pdx.qmine.handler;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import ovh.pdx.qmine.Qmine;
import ovh.pdx.qmine.config.ConfigManager;
import ovh.pdx.qmine.core.CanMineResult;
import ovh.pdx.qmine.core.CooldownTracker;
import ovh.pdx.qmine.core.PlayerMiningData;
import ovh.pdx.qmine.core.XPUtils;
import ovh.pdx.qmine.mixin.FoodDataAccess;
import ovh.pdx.qmine.net.QMineNetworking;
import ovh.pdx.qmine.net.SendShapePayload;
import ovh.pdx.qmine.shape.BlockMatchers;
import ovh.pdx.qmine.shape.Shape;
import ovh.pdx.qmine.tag.QMineTags;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class BlockBreakHandler {
    private static final BlockBreakHandler INSTANCE = new BlockBreakHandler();

    private BlockBreakHandler() {
    }

    public static BlockBreakHandler getInstance() {
        return INSTANCE;
    }

    public static boolean isValidTool(Player player, BlockPos pos, BlockState state) {
        ItemStack mainHand = player.getMainHandItem();

        boolean hasRequiredTool = !ConfigManager.getServerConfig().requireTool
                || (!mainHand.isEmpty() && (mainHand.has(DataComponents.TOOL) || mainHand.getMaxDamage() > 0 || QMineTags.isIncludedTool(mainHand)));
        if (!hasRequiredTool) {
            return false;
        }

        return !ConfigManager.getServerConfig().requireValidToolForBlock
                || !state.requiresCorrectToolForDrops()
                || player.hasCorrectToolForDrops(state);
    }

    public static boolean isTooExhausted(Player player) {
        if (player.getFoodData() instanceof FoodDataAccess accessor) {
            return player.getFoodData().getFoodLevel() <= 0 && accessor.qmine$getExhaustionLevel() >= 4.0F;
        }
        return player.getFoodData().getFoodLevel() <= 0;
    }

    public static boolean isToolTooDamaged(ItemStack stack) {
        int threshold = ConfigManager.getServerConfig().preventToolBreakDurability;
        return threshold > 0 && stack.isDamageableItem() && (stack.getMaxDamage() - stack.getDamageValue()) <= threshold;
    }

    public CanMineResult canMine(Player player, BlockPos pos, BlockState state) {
        var cfg = ConfigManager.getServerConfig();

        if (CooldownTracker.isOnCooldown(player)) {
            return CanMineResult.ON_COOLDOWN;
        }

        if (cfg.consumeFood && !player.isCreative() && player.getFoodData().getFoodLevel() <= 0) {
            return CanMineResult.TOO_HUNGRY;
        }

        if (cfg.consumeExperience && !player.isCreative() && XPUtils.getPlayerXP(player) < cfg.experiencePerBlock) {
            return CanMineResult.NOT_ENOUGH_XP;
        }

        ItemStack mainHand = player.getMainHandItem();
        ItemStack offHand = player.getOffhandItem();

        if (cfg.requireEnchantment && !player.isCreative()) {
            int level = ovh.pdx.qmine.enchantment.ExcavationHelper.getExcavatingLevel(mainHand);
            if (level <= 0) {
                level = ovh.pdx.qmine.enchantment.ExcavationHelper.getExcavatingLevel(offHand);
            }
            if (level <= 0) {
                return CanMineResult.NO_EXCAVATION_ENCHANTMENT;
            }
        }

        if (QMineTags.isStrictExcludedTool(mainHand) || QMineTags.isStrictExcludedTool(offHand) || QMineTags.isExcludedTool(mainHand)) {
            return CanMineResult.TOOL_NOT_ALLOWED;
        }

        if (!isValidTool(player, pos, state)) {
            return CanMineResult.NO_VALID_TOOL;
        }

        return CanMineResult.ALLOWED;
    }

    public static boolean isOre(BlockState state) {
        if (state.is(BlockTags.ORES)) {
            return true;
        }
        if (state.is(TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath("c", "ores")))) {
            return true;
        }
        if (BuiltInRegistries.BLOCK.wrapAsHolder(state.getBlock()).tags().anyMatch(tag -> {
            String path = tag.location().getPath();
            return path.equals("ores") || path.endsWith("_ores") || path.startsWith("ores/");
        })) {
            return true;
        }
        Identifier id = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        String path = id.getPath();
        return path.endsWith("_ore")
                || path.contains("_ore_")
                || path.equals("ancient_debris")
                || path.equals("gilded_blackstone")
                || (path.startsWith("raw_") && path.endsWith("_block"));
    }

    public static boolean isLavaAdjacent(LevelAccessor level, BlockPos pos) {
        for (Direction dir : Direction.values()) {
            BlockPos neighbor = pos.relative(dir);
            BlockState neighborState = level.getBlockState(neighbor);
            if (neighborState.is(net.minecraft.world.level.block.Blocks.LAVA) || neighborState.getFluidState().is(net.minecraft.tags.FluidTags.LAVA)) {
                return true;
            }
        }
        return false;
    }

    private static final ThreadLocal<Boolean> IS_PROCESSING = ThreadLocal.withInitial(() -> false);

    public boolean handleBlockBreak(LevelAccessor level, BlockPos originPos, BlockState state, ServerPlayer player) {
        if (IS_PROCESSING.get() || TreeHarvestHandler.isHarvesting()) {
            return false;
        }

        Qmine qmine = Qmine.getInstance();
        if (qmine.getDropsAccumulator().isActive()) {
            return false;
        }

        try {
            IS_PROCESSING.set(true);

            PlayerMiningData data = qmine.getPlayerData(player);
            var config = ConfigManager.getServerConfig();
            boolean active = data.isMiningActive(player);

            // 1. Standalone Tree Harvester
            if (config.treeHarvesterEnabled && !active) {
                boolean sneaking = PlayerMiningData.isPlayerSneaking(player);
                if (!(config.treeHarvesterSneakToDisable && sneaking)) {
                    if (state.is(BlockTags.LOGS) && level instanceof ServerLevel sLevel) {
                        TreeHarvestHandler.HarvestResult result = TreeHarvestHandler.getInstance().harvestTree(sLevel, originPos, state, player, config);
                        if (result == TreeHarvestHandler.HarvestResult.CANCEL_EVENT) {
                            return true;
                        } else if (result == TreeHarvestHandler.HarvestResult.ALLOW_EVENT) {
                            return false;
                        }
                    }
                }
            }

            // Safe sneak log breaking: sneaking on a log breaks only that single log
            if (active && state.is(BlockTags.LOGS) && config.treeHarvesterEnabled && config.treeHarvesterSneakToDisable && !data.isPressed()) {
                return false;
            }

            // 2. Vein Miner / Shape Miner
            if (!active || !canMine(player, originPos, state).isAllowed()) {
                return false;
            }

            // Server-side only mode (without client mod, or when activating via sneak without keybind):
            boolean isServerOnlyMode = !QMineNetworking.hasClientMod(player) || !data.isPressed();
            if (config.vanillaOresOnly && isServerOnlyMode) {
                if (!isOre(state)) {
                    return false;
                }
            }

            HitResult hit = PlayerMiningData.rayTrace(player);
            Direction hitDir;
            if (hit instanceof BlockHitResult bhr && bhr.getBlockPos().equals(originPos)) {
                hitDir = bhr.getDirection();
            } else {
                double dx = player.getX() - (originPos.getX() + 0.5);
                double dy = player.getEyeY() - (originPos.getY() + 0.5);
                double dz = player.getZ() - (originPos.getZ() + 0.5);
                hitDir = Direction.getApproximateNearest(dx, dy, dz);
            }

            data.clearCache();
            // In server-side only mode, always use ShapelessShape so ore veins are cleanly harvested
            int originalShapeIndex = data.getCurrentShapeIndex();
            if (isServerOnlyMode && originalShapeIndex != 0) {
                data.setShape("shapeless");
            }

            int effectiveMaxBlocks = config.maxBlocks;
            if (config.requireEnchantment && !player.isCreative()) {
                int enchLevel = ovh.pdx.qmine.enchantment.ExcavationHelper.getExcavatingLevel(player.getMainHandItem());
                if (enchLevel <= 0) {
                    enchLevel = ovh.pdx.qmine.enchantment.ExcavationHelper.getExcavatingLevel(player.getOffhandItem());
                }
                effectiveMaxBlocks = ovh.pdx.qmine.enchantment.ExcavationHelper.getMaxBlocksForLevel(enchLevel, config.maxBlocks);
            }

            data.updateBlocks(player, originPos, hitDir, false, effectiveMaxBlocks);
            if (isServerOnlyMode && originalShapeIndex != 0) {
                data.setShape(ovh.pdx.qmine.shape.ShapeRegistry.getInstance().getShape(originalShapeIndex).getId().getPath());
            }

            List<BlockPos> positions = data.getCachedPositions();
            if (positions == null || positions.size() <= 1) {
                return false;
            }

        if (config.consumeExperience && config.experiencePerBlock > 0.0 && !player.isCreative()) {
            if (XPUtils.getPlayerXP(player) < positions.size() * config.experiencePerBlock) {
                return false;
            }
        }

        int minedCount = 0;
        float baseDestroySpeed = state.getDestroySpeed(level, originPos);
        boolean hadMainHandItem = !player.getMainHandItem().isEmpty();

        Map<BlockPos, Block> blockSnapshot = new HashMap<>();
        for (BlockPos p : positions) {
            blockSnapshot.put(p, level.getBlockState(p).getBlock());
        }

        boolean preventedLava = false;
        int originalSlot = player.getInventory().getSelectedSlot();
        int activeSlot = originalSlot;

        try {
            qmine.getDropsAccumulator().start();

            for (BlockPos pos : positions) {
                BlockState targetState = level.getBlockState(pos);
                if (targetState.isAir() || targetState.getBlock() != blockSnapshot.get(pos)) {
                    continue;
                }

                // Claim Protection check
                if (level instanceof ServerLevel sLevel && !ovh.pdx.qmine.compat.ClaimProtection.canBreak(sLevel, pos, player)) {
                    continue;
                }

                // Anti-Lava Handling & Auto-Replace
                if (isLavaAdjacent(level, pos)) {
                    if (config.autoReplaceLava) {
                        int replaced = LavaReplacer.replaceAdjacentLava(level, pos, player, qmine.getDropsAccumulator());
                        if (replaced > 0) {
                            preventedLava = true;
                        }
                    }

                    // If still bordering lava (out of filler blocks or autoReplace disabled), keep protective wall
                    if (isLavaAdjacent(level, pos) && config.antiLavaShield) {
                        preventedLava = true;
                        continue;
                    }
                }

                // Auto Tool-Swap
                if (config.autoToolSwap && !player.isCreative()) {
                    int bestSlot = AutoToolSwapper.findBestToolSlot(player, targetState);
                    if (bestSlot != -1 && bestSlot != activeSlot) {
                        player.getInventory().setSelectedSlot(bestSlot);
                        activeSlot = bestSlot;
                        player.connection.send(new net.minecraft.network.protocol.game.ClientboundSetHeldSlotPacket(bestSlot));
                    }
                }

                Shape currentShape = data.getCurrentShape();
                List<String> mergeTags = (currentShape != null && !currentShape.isIndeterminate()) ? config.mergeTagsShaped : config.mergeTagsShapeless;
                float destroySpeed = targetState.getDestroySpeed(level, pos);
                boolean hardnessValid = destroySpeed >= 0 && (destroySpeed <= Math.max(1.0f, baseDestroySpeed * 3.0f) || BlockMatchers.matchTags(state, targetState, mergeTags));
                if (!player.isCreative() && (!hardnessValid || !isValidTool(player, pos, targetState))) {
                    continue;
                }

                // Modder API: Before block break event (allows claim/protection mods to veto this block)
                if (level instanceof ServerLevel sLevel) {
                    net.minecraft.world.InteractionResult restriction = ovh.pdx.qmine.api.event.QMineMiningEvents.BEFORE_BLOCK_BREAK.invoker().beforeBlockBreak(sLevel, player, pos, targetState);
                    if (restriction == net.minecraft.world.InteractionResult.FAIL) {
                        continue;
                    }
                }

                if (!player.gameMode.destroyBlock(pos)) {
                    if (config.cancelOnBlockBreakFail) {
                        break;
                    }
                    continue;
                }

                minedCount++;

                // Cascading sound pitch modulation
                if (config.cascadingSoundPitch && level instanceof ServerLevel sLevel) {
                    float progress = (float) minedCount / (float) Math.max(1, Math.min(positions.size(), 32));
                    float pitch = 0.85f + (progress * 0.70f);
                    sLevel.playSound(null, pos, targetState.getSoundType().getBreakSound(), net.minecraft.sounds.SoundSource.BLOCKS, 0.5f, pitch);
                }

                if (!player.isCreative()) {
                    if (config.consumeFood) {
                        player.causeFoodExhaustion((float) (config.exhaustionPerBlock * 0.005D));
                        if (isTooExhausted(player)) {
                            break;
                        }
                    }
                }

                ItemStack currentTool = player.getMainHandItem();
                if (hadMainHandItem && (currentTool.isEmpty() || isToolTooDamaged(currentTool))) {
                    break;
                }
            }
        } finally {
            if (activeSlot != originalSlot) {
                player.getInventory().setSelectedSlot(originalSlot);
                player.connection.send(new net.minecraft.network.protocol.game.ClientboundSetHeldSlotPacket(originalSlot));
            }
            if (player.level() instanceof ServerLevel serverLevel) {
                qmine.getDropsAccumulator().finish(serverLevel, originPos, player, config.dropCollection);
            }
        }

        if (preventedLava && player.level() instanceof ServerLevel sLevel) {
            sLevel.playSound(null, originPos, net.minecraft.sounds.SoundEvents.LAVA_EXTINGUISH, net.minecraft.sounds.SoundSource.BLOCKS, 0.4f, 1.2f);
            sLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.SMOKE, originPos.getX() + 0.5, originPos.getY() + 0.5, originPos.getZ() + 0.5, 5, 0.3, 0.3, 0.3, 0.02);
        }

        if (!player.isCreative()) {
            CooldownTracker.setLastMineTime(player, System.currentTimeMillis());
            if (config.consumeExperience) {
                data.addPendingXPCost(Math.max(0, minedCount - 1));
            }
        }

        if (minedCount > 1 && player.level() instanceof ServerLevel sLevel) {
            sLevel.playSound(null, originPos, net.minecraft.sounds.SoundEvents.AMETHYST_BLOCK_CHIME, net.minecraft.sounds.SoundSource.PLAYERS, 0.4f, 1.4f);
            sLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.HAPPY_VILLAGER, originPos.getX() + 0.5, originPos.getY() + 0.5, originPos.getZ() + 0.5, 5, 0.3, 0.3, 0.3, 0.05);

            // Tree auto-replant at base
            if (config.autoReplantTrees && state.is(BlockTags.LOGS)) {
                TreeHarvestHandler.getInstance().replantSaplingAtBase(sLevel, originPos, state);
            }

            // Automatic torch placement (tunnels, rooms, floors, veins)
            if (config.autoPlaceTorches) {
                AutoTorchHandler.getInstance().handleAutoTorch(
                        sLevel, positions, player, data.getCurrentShape(), hitDir, config
                );
            }

            // Modder API: After vein mine event
            ovh.pdx.qmine.api.event.QMineMiningEvents.AFTER_VEIN_MINE.invoker().afterVeinMine(
                    sLevel, player, originPos, data.getCurrentShape(), positions.subList(0, Math.min(minedCount, positions.size()))
            );
        }

        data.clearCache();
        QMineNetworking.sendToPlayer(player, SendShapePayload.ofShapeAndBlocks(data.getCurrentShapeIndex(), data.getCurrentShapeRadius(), List.of()));

        return true;
        } finally {
            IS_PROCESSING.set(false);
        }
    }
}
