package ovh.pdx.qmine.shape;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;

import java.util.List;

public interface Shape {
    Identifier getId();

    List<BlockPos> getBlocks(ShapeContext context);

    default boolean isIndeterminate() {
        return false;
    }

    default boolean isDefault() {
        return false;
    }

    default MutableComponent getDisplayName() {
        return Component.translatable("qmine.shape." + getId().getPath());
    }

    default MutableComponent getDisplayName(int radius) {
        return getDisplayName();
    }

    default boolean canResize() {
        return false;
    }

    default int minRadius() {
        return 1;
    }

    default int maxRadius() {
        return 1;
    }

    default int defaultRadius() {
        return 1;
    }

    default boolean isTunnel() {
        return false;
    }
}
