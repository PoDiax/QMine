package ovh.pdx.qmine.shape;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import ovh.pdx.qmine.tag.QMineTags;

import java.util.ArrayList;
import java.util.List;

public class StripMinerShape implements Shape {
    public static final Identifier ID = Identifier.fromNamespaceAndPath(QMineTags.MOD_ID, "strip_miner");

    @Override
    public Identifier getId() {
        return ID;
    }

    @Override
    public MutableComponent getDisplayName() {
        return Component.translatable("qmine.shape.strip_miner");
    }

    @Override
    public MutableComponent getDisplayName(int radius) {
        int pokeDepth = radius + 2;
        return getDisplayName().append(" (" + pokeDepth + "b pokes)");
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
    public List<BlockPos> getBlocks(ShapeContext context) {
        int radius = Math.max(minRadius(), Math.min(maxRadius(), context.radius()));
        int pokeDepth = radius + 2;

        Direction forward;
        if (context.face().getAxis().isVertical()) {
            forward = context.player() != null ? context.player().getDirection() : Direction.NORTH;
        } else {
            forward = context.face().getOpposite();
        }

        Direction right = forward.getClockWise();
        Direction left = forward.getCounterClockWise();

        List<BlockPos> list = new ArrayList<>(context.maxBlocks());

        for (int step = 0; step < context.maxBlocks(); step++) {
            boolean addedAnyInStep = false;
            BlockPos stepBase = context.originPos().relative(forward, step);

            // 1x2 Main Hallway (feet & eye)
            for (int h = 0; h < 2; h++) {
                BlockPos hallPos = stepBase.above(h);
                if (context.check(hallPos)) {
                    list.add(hallPos);
                    addedAnyInStep = true;
                    if (list.size() >= context.maxBlocks()) {
                        return list;
                    }
                }
            }

            // Side poke-holes every 3 blocks at eye level (h = 1)
            if (step % 3 == 0) {
                BlockPos eyeLevel = stepBase.above();
                for (int p = 1; p <= pokeDepth; p++) {
                    BlockPos leftHole = eyeLevel.relative(left, p);
                    if (context.check(leftHole)) {
                        list.add(leftHole);
                        addedAnyInStep = true;
                        if (list.size() >= context.maxBlocks()) {
                            return list;
                        }
                    }

                    BlockPos rightHole = eyeLevel.relative(right, p);
                    if (context.check(rightHole)) {
                        list.add(rightHole);
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
