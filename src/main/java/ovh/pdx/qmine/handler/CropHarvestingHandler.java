package ovh.pdx.qmine.handler;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CocoaBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.SweetBerryBushBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import ovh.pdx.qmine.config.ConfigManager;
import ovh.pdx.qmine.core.ItemCollector;
import ovh.pdx.qmine.mixin.CropBlockAccessor;
import ovh.pdx.qmine.shape.ShapeContext;
import ovh.pdx.qmine.tag.QMineTags;

import java.util.Collection;
import java.util.List;

public final class CropHarvestingHandler implements RightClickHandler {
    public static final CropHarvestingHandler INSTANCE = new CropHarvestingHandler();

    private CropHarvestingHandler() {
    }

    public static boolean isMatureCrop(BlockState state) {
        if (QMineTags.isSingleCropBlacklisted(state)) {
            return false;
        }

        if (state.getBlock() instanceof CropBlock cropBlock) {
            return cropBlock.isMaxAge(state);
        } else if (state.getBlock() instanceof SweetBerryBushBlock) {
            return state.getValue(SweetBerryBushBlock.AGE) >= SweetBerryBushBlock.MAX_AGE;
        } else if (state.getBlock() instanceof CocoaBlock) {
            return state.getValue(CocoaBlock.AGE) >= CocoaBlock.MAX_AGE;
        }
        return false;
    }

    public static boolean harvestSingleCrop(Player player, BlockPos pos, BlockState state) {
        if (!isMatureCrop(state) || !(player.level() instanceof ServerLevel serverLevel)) {
            return false;
        }

        ItemCollector collector = new ItemCollector();
        doHarvest(serverLevel, player, pos, state, collector);
        collector.drop(serverLevel, pos);
        return true;
    }

    @Override
    public int handle(ShapeContext context, InteractionHand hand, Collection<BlockPos> positions) {
        ServerPlayer player = context.player();
        if (!ConfigManager.getServerConfig().rightClickHarvesting) {
            return 0;
        }

        ServerLevel level = player.level();
        ItemCollector collector = new ItemCollector();
        int harvested = 0;

        for (BlockPos pos : positions) {
            BlockState state = level.getBlockState(pos);
            if (isMatureCrop(state)) {
                doHarvest(level, player, pos, state, collector);
                harvested++;
            } else if (ovh.pdx.qmine.api.event.QMineCropEvents.HARVEST.invoker().harvestCrop(level, player, pos, state)) {
                harvested++;
            }
        }

        if (harvested > 0) {
            collector.drop(level, context.originPos());
        }

        return harvested;
    }

    private static void doHarvest(ServerLevel level, Player player, BlockPos pos, BlockState state, ItemCollector collector) {
        BlockEntity blockEntity = state.hasBlockEntity() ? level.getBlockEntity(pos) : null;
        List<ItemStack> drops = Block.getDrops(state, level, pos, blockEntity, player, ItemStack.EMPTY);

        boolean replanted = false;
        for (ItemStack drop : drops) {
            if (!replanted && Block.byItem(drop.getItem()) == state.getBlock() && state.getBlock() != Blocks.SWEET_BERRY_BUSH) {
                drop.shrink(1);
                replanted = true;
            }
            if (!drop.isEmpty()) {
                collector.accept(drop);
            }
        }

        resetCropAge(level, pos, state);
    }

    private static void resetCropAge(Level level, BlockPos pos, BlockState state) {
        if (state.getBlock() instanceof CropBlock cropBlock && cropBlock instanceof CropBlockAccessor accessor) {
            level.setBlock(pos, state.setValue(accessor.qmine$getAgeProperty(), 0), Block.UPDATE_ALL);
        } else if (state.getBlock() instanceof SweetBerryBushBlock) {
            level.setBlock(pos, state.setValue(SweetBerryBushBlock.AGE, 1), Block.UPDATE_ALL);
        } else if (state.getBlock() instanceof CocoaBlock) {
            level.setBlock(pos, state.setValue(CocoaBlock.AGE, 0), Block.UPDATE_ALL);
        }
    }
}
