package ovh.pdx.qmine.shape;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import ovh.pdx.qmine.tag.QMineTags;

import java.util.*;

public class ShapelessShape implements Shape {
    public static final Identifier ID = Identifier.fromNamespaceAndPath(QMineTags.MOD_ID, "shapeless");

    private static final List<BlockPos> NEIGHBORS_3D = new ArrayList<>(26);
    private static final List<BlockPos> NEIGHBORS_CROPS = new ArrayList<>(26);

    static {
        for (int x = -1; x <= 1; x++) {
            for (int y = -1; y <= 1; y++) {
                for (int z = -1; z <= 1; z++) {
                    if (x != 0 || y != 0 || z != 0) {
                        NEIGHBORS_3D.add(new BlockPos(x, y, z));
                    }
                }
            }
        }

        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                if (x != 0 || z != 0) {
                    NEIGHBORS_CROPS.add(new BlockPos(x, 0, z));
                }
            }
        }
        NEIGHBORS_CROPS.add(new BlockPos(0, 1, 0));
        NEIGHBORS_CROPS.add(new BlockPos(0, -1, 0));
        NEIGHBORS_CROPS.sort(Comparator.comparingDouble(p -> p.distSqr(BlockPos.ZERO)));
    }

    @Override
    public Identifier getId() {
        return ID;
    }

    @Override
    public boolean isIndeterminate() {
        return true;
    }

    @Override
    public boolean isDefault() {
        return true;
    }

    @Override
    public List<BlockPos> getBlocks(ShapeContext context) {
        Set<BlockPos> discovered = new LinkedHashSet<>();
        boolean isCropLike = BlockMatchers.isCrop(context.originState());
        List<BlockPos> neighborOffsets = isCropLike ? NEIGHBORS_CROPS : NEIGHBORS_3D;

        Set<BlockPos> visited = new HashSet<>();
        Queue<BlockPos> queue = new ArrayDeque<>();

        BlockPos origin = context.originPos();
        queue.add(origin);
        visited.add(origin);

        while (!queue.isEmpty()) {
            BlockPos current = queue.poll();

            if (context.check(current)) {
                discovered.add(current);
                if (discovered.size() >= context.maxBlocks()) {
                    break;
                }

                for (BlockPos offset : neighborOffsets) {
                    BlockPos next = current.offset(offset);
                    if (visited.add(next)) {
                        queue.add(next);
                    }
                }
            }
        }

        List<BlockPos> result = new ArrayList<>(discovered);
        result.sort(Comparator.comparingDouble(p -> p.distSqr(origin)));
        if (result.size() > context.maxBlocks()) {
            return result.subList(0, context.maxBlocks());
        }
        return result;
    }
}
