package ovh.pdx.qmine.shape;

import net.minecraft.world.level.block.state.BlockState;
import ovh.pdx.qmine.tag.QMineTags;

public record BlockMatcher(ShapeContext.Matcher wrapped) implements ShapeContext.Matcher {
    public static BlockMatcher wrap(ShapeContext.Matcher matcher) {
        return new BlockMatcher(matcher);
    }

    @Override
    public boolean check(BlockState origin, BlockState target) {
        if (target.isAir()) {
            return false;
        }

        if (QMineTags.isExcludedBlock(target)) {
            return false;
        }

        if (!QMineTags.isWhitelistedBlock(target)) {
            return false;
        }

        return wrapped.check(origin, target);
    }
}
