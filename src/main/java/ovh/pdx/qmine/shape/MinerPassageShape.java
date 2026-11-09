package ovh.pdx.qmine.shape;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import ovh.pdx.qmine.tag.QMineTags;

import java.util.ArrayList;
import java.util.List;

public class MinerPassageShape implements Shape {
    public static final Identifier ID = Identifier.fromNamespaceAndPath(QMineTags.MOD_ID, "miner_passage");

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
        return 2;
    }

    @Override
    public int defaultRadius() {
        return 0;
    }

    @Override
    public MutableComponent getDisplayName() {
        return Component.translatable("qmine.shape.miner_passage");
    }

    @Override
    public MutableComponent getDisplayName(int radius) {
        int width = radius + 1;
        return Component.translatable("qmine.shape.miner_passage").append(" (" + width + "x2)");
    }

    @Override
    public List<BlockPos> getBlocks(ShapeContext context) {
        int radius = Math.max(minRadius(), Math.min(maxRadius(), context.radius()));
        List<BlockPos> list = new ArrayList<>();
        Direction face = context.face();
        BlockPos origin = context.originPos();

        boolean clickIsUpper = context.player() != null && context.player().getY() < origin.getY() - 0.5;
        Direction forward = face.getOpposite();
        Direction right = forward.getAxis().isVertical()
                ? (context.player() != null ? context.player().getDirection().getClockWise() : Direction.EAST)
                : forward.getClockWise();

        int maxLayers = Math.max(1, context.maxBlocks() / ((radius + 1) * 2));
        for (int depth = 0; depth < maxLayers; depth++) {
            BlockPos layerBase = origin.relative(forward, depth);
            boolean addedAnyInLayer = false;

            for (int w = 0; w <= radius; w++) {
                int lateralOffset = (radius == 2) ? (w - 1) : w;
                BlockPos colBase = layerBase.relative(right, lateralOffset);
                BlockPos b1 = clickIsUpper ? colBase.below() : colBase;
                BlockPos b2 = clickIsUpper ? colBase : colBase.above();

                if (context.check(b1)) {
                    list.add(b1);
                    addedAnyInLayer = true;
                    if (list.size() >= context.maxBlocks()) return list;
                }
                if (context.check(b2)) {
                    list.add(b2);
                    addedAnyInLayer = true;
                    if (list.size() >= context.maxBlocks()) return list;
                }
            }

            if (!addedAnyInLayer) {
                break;
            }
        }

        return list;
    }
}
