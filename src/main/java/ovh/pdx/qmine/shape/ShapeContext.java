package ovh.pdx.qmine.shape;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public record ShapeContext(
        ServerPlayer player,
        BlockPos originPos,
        Direction face,
        BlockState originState,
        Matcher matcher,
        int maxBlocks,
        int radius
) {
    public BlockState getBlockState(BlockPos pos) {
        return player.level().hasChunkAt(pos) ? player.level().getBlockState(pos) : Blocks.AIR.defaultBlockState();
    }

    public boolean check(BlockPos pos) {
        if (!player.level().hasChunkAt(pos)) {
            return false;
        }
        BlockState state = getBlockState(pos);
        return matcher.check(originState, state);
    }

    @FunctionalInterface
    public interface Matcher {
        boolean check(BlockState origin, BlockState target);
    }
}
