package ovh.pdx.qmine.api.event.compat;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public final class Event<T> {
    private final Class<T> type;
    private final Function<T[], T> invokerFactory;
    private final List<T> handlers = new ArrayList<>();
    private T invoker;

    public Event(Class<T> type, Function<T[], T> invokerFactory) {
        this.type = type;
        this.invokerFactory = invokerFactory;
        update();
    }

    public synchronized void register(T handler) {
        handlers.add(handler);
        update();
    }

    public T invoker() {
        return invoker;
    }

    @SuppressWarnings("unchecked")
    private void update() {
        T[] array = (T[]) Array.newInstance(type, handlers.size());
        invoker = invokerFactory.apply(handlers.toArray(array));
    }
}
