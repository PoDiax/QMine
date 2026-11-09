package ovh.pdx.qmine.shape;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import ovh.pdx.qmine.tag.QMineTags;

import java.util.ArrayList;
import java.util.List;

public class FloorShape implements Shape {
    public static final Identifier ID = Identifier.fromNamespaceAndPath(QMineTags.MOD_ID, "floor");

    @Override
    public Identifier getId() {
        return ID;
    }

    @Override
    public boolean canResize() {
        return true;
    }

    @Override
    public int minRadius() {
        return 0;
    }

    @Override
    public int maxRadius() {
        return 4;
    }

    @Override
    public int defaultRadius() {
        return 1;
    }

    @Override
    public MutableComponent getDisplayName() {
        return Component.translatable("qmine.shape.floor");
    }

    @Override
    public MutableComponent getDisplayName(int radius) {
        int dim = radius * 2 + 1;
        return Component.translatable("qmine.shape.floor").append(" (" + dim + "x" + dim + ")");
    }

    @Override
    public List<BlockPos> getBlocks(ShapeContext context) {
        int radius = Math.max(minRadius(), Math.min(maxRadius(), context.radius()));
        int estimated = (radius * 2 + 1) * (radius * 2 + 1);
        List<BlockPos> list = new ArrayList<>(Math.min(estimated, context.maxBlocks()));
        BlockPos origin = context.originPos();
        list.add(origin);

        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                if (x == 0 && z == 0) continue;
                BlockPos pos = origin.offset(x, 0, z);
                if (context.check(pos)) {
                    list.add(pos);
                    if (list.size() >= context.maxBlocks()) {
                        return list;
                    }
                }
            }
        }
        return list;
    }
}
