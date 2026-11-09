package ovh.pdx.qmine.shape;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import ovh.pdx.qmine.tag.QMineTags;

import java.util.ArrayList;
import java.util.List;

public class RoomShape implements Shape {
    public static final Identifier ID = Identifier.fromNamespaceAndPath(QMineTags.MOD_ID, "room");

    @Override
    public Identifier getId() {
        return ID;
    }

    @Override
    public MutableComponent getDisplayName() {
        return Component.translatable("qmine.shape.room");
    }

    @Override
    public MutableComponent getDisplayName(int radius) {
        int size = radius * 2 + 1;
        int depth = radius + 2;
        return getDisplayName().append(" (" + size + "x" + size + "x" + depth + "d)");
    }

    @Override
    public boolean canResize() {
        return true;
    }

    @Override
    public int minRadius() {
        return 1;
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
    public List<BlockPos> getBlocks(ShapeContext context) {
        int radius = Math.max(minRadius(), Math.min(maxRadius(), context.radius()));
        int depth = radius + 2;

        Direction face = context.face();
        Direction inward = face.getOpposite();
        List<BlockPos> list = new ArrayList<>(context.maxBlocks());

        if (face.getAxis().isVertical()) {
            for (int d = 0; d < depth; d++) {
                BlockPos layerCenter = context.originPos().relative(inward, d);
                for (int x = -radius; x <= radius; x++) {
                    for (int z = -radius; z <= radius; z++) {
                        BlockPos pos = layerCenter.offset(x, 0, z);
                        if (context.check(pos)) {
                            list.add(pos);
                            if (list.size() >= context.maxBlocks()) {
                                return list;
                            }
                        }
                    }
                }
            }
        } else {
            Direction right = face.getClockWise();
            for (int d = 0; d < depth; d++) {
                BlockPos layerCenter = context.originPos().relative(inward, d);
                for (int lateral = -radius; lateral <= radius; lateral++) {
                    for (int y = -1; y < (radius * 2); y++) {
                        BlockPos pos = layerCenter.relative(right, lateral).above(y);
                        if (context.check(pos)) {
                            list.add(pos);
                            if (list.size() >= context.maxBlocks()) {
                                return list;
                            }
                        }
                    }
                }
            }
        }

        return list;
    }
}
