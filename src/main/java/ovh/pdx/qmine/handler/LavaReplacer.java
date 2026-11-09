package ovh.pdx.qmine.handler;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.state.BlockState;
import ovh.pdx.qmine.core.DropsAccumulator;

public final class LavaReplacer {
    private LavaReplacer() {
    }

    public static boolean isLava(LevelAccessor level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return state.is(Blocks.LAVA) || state.getFluidState().is(FluidTags.LAVA);
    }

    public static boolean isFillerBlock(ItemStack stack) {
        if (stack.isEmpty() || !(stack.getItem() instanceof BlockItem blockItem)) {
            return false;
        }

        Block block = blockItem.getBlock();
        BlockState state = block.defaultBlockState();

        if (!state.isSolidRender()) {
            return false;
        }

        if (block instanceof FallingBlock) {
            return false;
        }

        Identifier id = BuiltInRegistries.BLOCK.getKey(block);
        String path = id.getPath();

        if (path.contains("ore") || path.startsWith("raw_")) {
            return false;
        }

        return path.contains("cobblestone")
                || path.contains("deepslate")
                || path.contains("stone")
                || path.contains("diorite")
                || path.contains("granite")
                || path.contains("andesite")
                || path.contains("tuff")
                || path.contains("basalt")
                || path.contains("blackstone")
                || path.contains("netherrack")
                || path.contains("dirt")
                || path.contains("mud")
                || path.contains("sandstone")
                || path.contains("terracotta")
                || path.contains("end_stone");
    }

    public static ItemStack takeFillerBlock(ServerPlayer player, DropsAccumulator accumulator) {
        // 1. Try from freshly mined drops first (e.g. cobblestone we just broke!)
        if (accumulator != null && accumulator.isActive()) {
            ItemStack fromDrops = accumulator.getItemCollector().takeFirstMatching(LavaReplacer::isFillerBlock);
            if (!fromDrops.isEmpty()) {
                return fromDrops;
            }
        }

        // 2. Creative mode gets free cobblestone
        if (player.isCreative()) {
            return new ItemStack(Blocks.COBBLESTONE);
        }

        // 3. Search player inventory
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (!stack.isEmpty() && isFillerBlock(stack)) {
                ItemStack result = stack.copyWithCount(1);
                stack.shrink(1);
                if (stack.isEmpty()) {
                    player.getInventory().setItem(i, ItemStack.EMPTY);
                }
                return result;
            }
        }

        return ItemStack.EMPTY;
    }

    public static int replaceAdjacentLava(LevelAccessor level, BlockPos pos, ServerPlayer player, DropsAccumulator accumulator) {
        int replaced = 0;

        for (Direction dir : Direction.values()) {
            BlockPos neighbor = pos.relative(dir);
            if (isLava(level, neighbor)) {
                ItemStack filler = takeFillerBlock(player, accumulator);
                if (!filler.isEmpty() && filler.getItem() instanceof BlockItem bi) {
                    level.setBlock(neighbor, bi.getBlock().defaultBlockState(), Block.UPDATE_ALL);
                    replaced++;

                    if (level instanceof ServerLevel sLevel) {
                        sLevel.playSound(null, neighbor, SoundEvents.LAVA_EXTINGUISH, SoundSource.BLOCKS, 0.5f, 1.2f);
                        sLevel.sendParticles(ParticleTypes.SMOKE, neighbor.getX() + 0.5, neighbor.getY() + 0.5, neighbor.getZ() + 0.5, 6, 0.25, 0.25, 0.25, 0.02);
                    }
                }
            }
        }

        return replaced;
    }
}
