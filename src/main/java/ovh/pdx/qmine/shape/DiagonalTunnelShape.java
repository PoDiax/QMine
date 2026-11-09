package ovh.pdx.qmine.shape;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.ArrayList;
import java.util.List;

public abstract class DiagonalTunnelShape implements Shape {
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
        return 2;
    }

    @Override
    public int defaultRadius() {
        return 0;
    }

    @Override
    public MutableComponent getDisplayName(int radius) {
        int width = radius + 1;
        return getDisplayName().append(" (" + width + "w)");
    }

    @Override
    public List<BlockPos> getBlocks(ShapeContext context) {
        int radius = Math.max(minRadius(), Math.min(maxRadius(), context.radius()));
        Direction face = context.face().getAxis().isVertical()
                ? (context.player() != null ? context.player().getDirection().getOpposite() : Direction.NORTH)
                : context.face();

        Direction right = face.getClockWise();
        List<BlockPos> list = new ArrayList<>(context.maxBlocks());

        for (int i = 0; i < context.maxBlocks(); i++) {
            boolean addedAnyInStep = false;
            for (int w = 0; w <= radius; w++) {
                int lateralOffset = (radius == 2) ? (w - 1) : w;
                BlockPos pos = context.originPos()
                        .offset(-face.getStepX() * i, yDirection() * i, -face.getStepZ() * i)
                        .relative(right, lateralOffset);

                if (context.check(pos)) {
                    list.add(pos);
                    addedAnyInStep = true;
                    if (list.size() >= context.maxBlocks()) {
                        return list;
                    }
                }
            }

            if (!addedAnyInStep && i > 0) {
                break;
            }
        }

        return list;
    }

    protected abstract int yDirection();
}
