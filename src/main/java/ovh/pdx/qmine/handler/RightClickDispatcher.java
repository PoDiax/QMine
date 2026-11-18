package ovh.pdx.qmine.handler;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import ovh.pdx.qmine.api.event.QMineRightClickEvents;
import ovh.pdx.qmine.shape.ShapeContext;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public final class RightClickDispatcher {
    private static final RightClickDispatcher INSTANCE = new RightClickDispatcher();

    private final List<RightClickHandler> handlers = new ArrayList<>();

    private RightClickDispatcher() {
        handlers.add(AxeStrippingHandler.INSTANCE);
        handlers.add(ShovelFlatteningHandler.INSTANCE);
        handlers.add(FarmlandConversionHandler.INSTANCE);
        handlers.add(CropHarvestingHandler.INSTANCE);

        // Fire Modder API event so other mods can register custom right-click handlers
        QMineRightClickEvents.REGISTER.invoker().registerHandlers(this);
    }

    public static RightClickDispatcher getInstance() {
        return INSTANCE;
    }

    public synchronized void registerHandler(RightClickHandler handler) {
        handlers.add(handler);
    }

    public List<RightClickHandler> getHandlers() {
        return List.copyOf(handlers);
    }

    public int dispatch(ShapeContext context, InteractionHand hand, Collection<BlockPos> positions) {
        for (RightClickHandler handler : handlers) {
            int count = handler.handle(context, hand, positions);
            if (count > 0) {
                return count;
            }
        }
        return 0;
    }
}
