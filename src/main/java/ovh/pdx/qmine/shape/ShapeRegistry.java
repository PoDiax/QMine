package ovh.pdx.qmine.shape;

import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ShapeRegistry {
    private static final ShapeRegistry INSTANCE = new ShapeRegistry();

    private final List<Shape> shapes = new ArrayList<>();

    private ShapeRegistry() {
        register(new ShapelessShape());
        register(new TunnelShape());
        register(new WallShape());
        register(new FloorShape());
        register(new MinerPassageShape());
        register(new MiningTunnelShape());
        register(new EscapeTunnelShape());
        register(new StaircaseTunnelShape());
        register(new StripMinerShape());
        register(new RoomShape());

        // Fire Modder API event so other mods can register custom shapes
        ovh.pdx.qmine.api.event.QMineShapeEvents.REGISTER.invoker().registerShapes(this);
    }

    public static ShapeRegistry getInstance() {
        return INSTANCE;
    }

    public synchronized void register(Shape shape) {
        shapes.add(shape);
    }

    public int shapeCount() {
        return shapes.size();
    }

    public List<Shape> getShapes() {
        return Collections.unmodifiableList(shapes);
    }

    public Shape getShape(int index) {
        if (shapes.isEmpty()) {
            throw new IllegalStateException("No shapes registered in ShapeRegistry");
        }
        int size = shapes.size();
        int wrapped = (index % size + size) % size;
        return shapes.get(wrapped);
    }

    public int cycle(int currentIndex, boolean next) {
        int count = shapeCount();
        if (count == 0) return 0;
        return next ? (currentIndex + 1) % count : (currentIndex - 1 + count) % count;
    }

    public int indexOf(Identifier id) {
        for (int i = 0; i < shapes.size(); i++) {
            if (shapes.get(i).getId().equals(id)) {
                return i;
            }
        }
        return -1;
    }

    public Shape getShape(Identifier id) {
        int index = indexOf(id);
        return index >= 0 ? shapes.get(index) : null;
    }

    public Shape getShape(String name) {
        for (Shape s : shapes) {
            if (s.getId().getPath().equalsIgnoreCase(name) || s.getId().toString().equalsIgnoreCase(name)) {
                return s;
            }
        }
        return null;
    }
}
