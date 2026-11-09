package ovh.pdx.qmine.tree;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;
import java.util.Set;

public record NaturalTree(
        BlockPos originPos,
        BlockState originState,
        List<BlockPos> logs,
        Set<BlockPos> leaves,
        BlockPos lowestLog,
        BlockPos topMostLog
) {
    public int logCount() {
        return logs.size();
    }

    public int leafCount() {
        return leaves.size();
    }
}
