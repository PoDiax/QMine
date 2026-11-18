package ovh.pdx.qmine.api.event;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import ovh.pdx.qmine.shape.ShapeRegistry;

public final class QMineShapeEvents {
    private QMineShapeEvents() {
    }

    /**
     * Fired when QMine initializes shapes. Mods can register custom mining shapes here.
     */
    public static final Event<Register> REGISTER = EventFactory.createArrayBacked(Register.class,
            listeners -> registry -> {
                for (Register listener : listeners) {
                    listener.registerShapes(registry);
                }
            });

    @FunctionalInterface
    public interface Register {
        void registerShapes(ShapeRegistry registry);
    }
}
