package ovh.pdx.qmine.mixin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ovh.pdx.qmine.handler.PlacedLogTracker;

@Mixin(BlockItem.class)
public abstract class BlockItemMixin {
    @Inject(method = "placeBlock", at = @At("RETURN"))
    private void qmine$onPlaceBlock(BlockPlaceContext context, BlockState state, CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValue() && state.is(BlockTags.LOGS)) {
            if (context.getLevel() instanceof ServerLevel serverLevel) {
                PlacedLogTracker.getInstance().recordPlaced(serverLevel, context.getClickedPos());
            }
        }
    }
}
