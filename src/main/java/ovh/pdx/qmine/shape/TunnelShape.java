package ovh.pdx.qmine.shape;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import ovh.pdx.qmine.tag.QMineTags;

import java.util.ArrayList;
import java.util.List;

public class TunnelShape implements Shape {
    public static final Identifier ID = Identifier.fromNamespaceAndPath(QMineTags.MOD_ID, "tunnel");

    @Override
    public Identifier getId() {
        return ID;
    }

    @Override
    public boolean canResize() {
        return true;
    }

    @Override
    public boolean isTunnel() {
        return true;
    }

    @Override
    public int minRadius() {
        return 0;
    }

    @Override
    public int maxRadius() {
        return 3;
    }

    @Override
    public int defaultRadius() {
        return 1;
    }

    @Override
    public MutableComponent getDisplayName() {
        return Component.translatable("qmine.shape.tunnel");
    }

    @Override
    public MutableComponent getDisplayName(int radius) {
        int dim = radius * 2 + 1;
        return Component.translatable("qmine.shape.tunnel").append(" (" + dim + "x" + dim + ")");
    }

    @Override
    public List<BlockPos> getBlocks(ShapeContext context) {
        int radius = Math.max(minRadius(), Math.min(maxRadius(), context.radius()));

        if (radius == 0) {
            List<BlockPos> list = new ArrayList<>(context.maxBlocks());
            for (int i = 0; i < context.maxBlocks(); i++) {
                BlockPos pos = context.originPos().relative(context.face(), -i);
                if (!context.check(pos)) {
                    break;
                }
                list.add(pos);
            }
            return list;
        }

        List<BlockPos> list = new ArrayList<>();
        BlockPos basePos = context.originPos();
        list.add(basePos);

        int depth = 0;
        while (list.size() < context.maxBlocks()) {
            int previousSize = list.size();
            LAYER:
            for (int a = -radius; a <= radius; a++) {
                for (int b = -radius; b <= radius; b++) {
                    if (depth > 0 || a != 0 || b != 0) {
                        BlockPos pos = switch (context.face().getAxis()) {
                            case X -> basePos.offset(0, a, b);
                            case Y -> basePos.offset(a, 0, b);
                            case Z -> basePos.offset(a, b, 0);
                        };

                        if (context.check(pos)) {
                            list.add(pos);
                            if (list.size() >= context.maxBlocks()) {
                                break LAYER;
                            }
                        }
                    }
                }
            }

            if (list.size() == previousSize) {
                break;
            }
            basePos = basePos.relative(context.face().getOpposite());
            depth++;
        }

        return list;
    }
}
