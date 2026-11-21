package ovh.pdx.qmine.api.event;

import ovh.pdx.qmine.api.event.compat.Event;
import ovh.pdx.qmine.api.event.compat.EventFactory;
import ovh.pdx.qmine.handler.RightClickDispatcher;

public final class QMineRightClickEvents {
    private QMineRightClickEvents() {
    }

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
