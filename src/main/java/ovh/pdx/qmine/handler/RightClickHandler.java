package ovh.pdx.qmine.handler;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import ovh.pdx.qmine.shape.ShapeContext;

import java.util.Collection;

@FunctionalInterface
public interface RightClickHandler {
    int handle(ShapeContext context, InteractionHand hand, Collection<BlockPos> positions);
}
