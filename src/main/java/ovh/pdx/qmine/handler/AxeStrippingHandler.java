package ovh.pdx.qmine.handler;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import ovh.pdx.qmine.config.ConfigManager;
import ovh.pdx.qmine.shape.ShapeContext;

import java.util.Collection;

public final class AxeStrippingHandler implements RightClickHandler {
    public static final AxeStrippingHandler INSTANCE = new AxeStrippingHandler();

    private AxeStrippingHandler() {
    }

    @Override
    public int handle(ShapeContext context, InteractionHand hand, Collection<BlockPos> positions) {
        ServerPlayer player = context.player();
        ItemStack stack = player.getItemInHand(hand);

        if (!ConfigManager.getServerConfig().rightClickAxe || !stack.is(ItemTags.AXES)) {
            return 0;
        }

        int affected = 0;
        for (BlockPos pos : positions) {
            if (isToolTooDamaged(stack)) {
                break;
            }

            BlockHitResult hitResult = new BlockHitResult(Vec3.atBottomCenterOf(pos.above()), Direction.UP, pos, false);
            if (stack.useOn(new UseOnContext(player, hand, hitResult)).consumesAction()) {
                affected++;
            }

            if (stack.isEmpty()) {
                break;
            }
        }

        return affected;
    }

    private static boolean isToolTooDamaged(ItemStack stack) {
        int threshold = ConfigManager.getServerConfig().preventToolBreakDurability;
        return threshold > 0 && stack.isDamageableItem() && (stack.getMaxDamage() - stack.getDamageValue()) <= threshold;
    }
}
