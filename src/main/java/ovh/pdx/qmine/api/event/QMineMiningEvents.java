package ovh.pdx.qmine.api.event;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.block.state.BlockState;
import ovh.pdx.qmine.shape.Shape;

import java.util.List;

public final class QMineMiningEvents {
    private QMineMiningEvents() {
    }

    /**
     * Fired before an individual block is broken during a vein-mine operation.
     * Return InteractionResult.FAIL to cancel breaking this specific block (e.g. claim protection).
     * Return InteractionResult.PASS to allow default checks.
     */
    public static final Event<BeforeBlockBreak> BEFORE_BLOCK_BREAK = EventFactory.createArrayBacked(BeforeBlockBreak.class,
            listeners -> (level, player, pos, state) -> {
                for (BeforeBlockBreak listener : listeners) {
                    InteractionResult result = listener.beforeBlockBreak(level, player, pos, state);
                    if (result != InteractionResult.PASS) {
                        return result;
                    }
                }
                return InteractionResult.PASS;
            });

    /**
     * Fired after a vein-mine operation completes.
     */
    public static final Event<AfterVeinMine> AFTER_VEIN_MINE = EventFactory.createArrayBacked(AfterVeinMine.class,
            listeners -> (level, player, originPos, shape, minedBlocks) -> {
                for (AfterVeinMine listener : listeners) {
                    listener.afterVeinMine(level, player, originPos, shape, minedBlocks);
                }
            });

    @FunctionalInterface
    public interface BeforeBlockBreak {
        InteractionResult beforeBlockBreak(ServerLevel level, ServerPlayer player, BlockPos pos, BlockState state);
    }

    @FunctionalInterface
    public interface AfterVeinMine {
        void afterVeinMine(ServerLevel level, ServerPlayer player, BlockPos originPos, Shape shape, List<BlockPos> minedBlocks);
    }
}
