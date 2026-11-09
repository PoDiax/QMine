package ovh.pdx.qmine.api.event;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import ovh.pdx.qmine.handler.RightClickDispatcher;

public final class QMineRightClickEvents {
    private QMineRightClickEvents() {
    }

    /**
     * Fired when QMine initializes right-click handlers. Mods can register custom right-click actions here.
     */
    public static final Event<Register> REGISTER = EventFactory.createArrayBacked(Register.class,
            listeners -> dispatcher -> {
                for (Register listener : listeners) {
                    listener.registerHandlers(dispatcher);
                }
            });

    @FunctionalInterface
    public interface Register {
        void registerHandlers(RightClickDispatcher dispatcher);
    }
}
