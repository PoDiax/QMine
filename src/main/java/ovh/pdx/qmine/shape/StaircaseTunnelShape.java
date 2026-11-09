package ovh.pdx.qmine.shape;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import ovh.pdx.qmine.tag.QMineTags;

import java.util.ArrayList;
import java.util.List;

public class StaircaseTunnelShape implements Shape {
    public static final Identifier ID = Identifier.fromNamespaceAndPath(QMineTags.MOD_ID, "staircase_tunnel");

    @Override
    public Identifier getId() {
        return ID;
    }

    @Override
    public MutableComponent getDisplayName() {
        return Component.translatable("qmine.shape.staircase_tunnel");
    }

    @Override
    public MutableComponent getDisplayName(int radius) {
        int width = radius + 1;
        return getDisplayName().append(" (" + width + "w x 3h)");
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
        return 2;
    }

    @Override
    public int defaultRadius() {
        return 0;
    }

    @Override
    public List<BlockPos> getBlocks(ShapeContext context) {
        int radius = Math.max(minRadius(), Math.min(maxRadius(), context.radius()));

        Direction horizontalDir;
        if (context.face().getAxis().isVertical()) {
            horizontalDir = context.player() != null ? context.player().getDirection() : Direction.NORTH;
        } else {
            horizontalDir = context.face().getOpposite();
        }

        Direction right = horizontalDir.getClockWise();

        // Looking up ascends (+1 Y), looking down or neutral descends (-1 Y)
        int yDir = -1;
        if (context.player() != null && context.player().getXRot() < -15.0F) {
            yDir = 1;
        } else if (context.face() == Direction.DOWN) {
            yDir = 1;
        }

        List<BlockPos> list = new ArrayList<>(context.maxBlocks());

        for (int step = 0; step < context.maxBlocks(); step++) {
            boolean addedAnyInStep = false;
            BlockPos stepBase = context.originPos()
                    .offset(horizontalDir.getStepX() * step, yDir * step, horizontalDir.getStepZ() * step);

            for (int w = 0; w <= radius; w++) {
                int lateralOffset = (radius == 2) ? (w - 1) : w;
                BlockPos colPos = stepBase.relative(right, lateralOffset);

                // 3 blocks high clearance for smooth sprinting up/down stairs
                for (int h = 0; h < 3; h++) {
                    BlockPos pos = colPos.above(h);
                    if (context.check(pos)) {
                        list.add(pos);
                        addedAnyInStep = true;
                        if (list.size() >= context.maxBlocks()) {
                            return list;
                        }
                    }
                }
            }

            if (!addedAnyInStep && step > 0) {
                break;
            }
        }

        return list;
    }
}
