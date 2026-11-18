package ovh.pdx.qmine.api.event;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;

public final class QMineCropEvents {
    private QMineCropEvents() {
    }

    /**
     * Allows custom crop harvesting logic for modded crops.
     * Return true if the crop was handled and harvested.
     */
    public static final Event<CropHarvestHandler> HARVEST = EventFactory.createArrayBacked(CropHarvestHandler.class,
            listeners -> (level, player, pos, state) -> {
                for (CropHarvestHandler listener : listeners) {
                    if (listener.harvestCrop(level, player, pos, state)) {
                        return true;
                    }
                }
                return false;
            });

    @FunctionalInterface
    public interface CropHarvestHandler {
        boolean harvestCrop(ServerLevel level, ServerPlayer player, BlockPos pos, BlockState state);
    }
}
