package ovh.pdx.qmine.handler;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import ovh.pdx.qmine.config.ConfigManager;

public final class AutoToolSwapper {
    private AutoToolSwapper() {
    }

    public static int findBestToolSlot(ServerPlayer player, BlockState state) {
        ItemStack current = player.getMainHandItem();
        boolean requiresCorrectTool = state.requiresCorrectToolForDrops();
        boolean currentCorrect = player.hasCorrectToolForDrops(state);
        float currentSpeed = current.isEmpty() ? 1.0F : current.getDestroySpeed(state);

        if ((!requiresCorrectTool || currentCorrect) && currentSpeed > 1.0F) {
            return -1;
        }

        int bestSlot = -1;
        float bestSpeed = currentSpeed;
        boolean bestCorrect = currentCorrect;

        for (int slot = 0; slot < 9; slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (stack.isEmpty() || BlockBreakHandler.isToolTooDamaged(stack)) {
                continue;
            }

            boolean candidateCorrect = stack.isCorrectToolForDrops(state);
            float candidateSpeed = stack.getDestroySpeed(state);

            if (requiresCorrectTool) {
                if (candidateCorrect && !bestCorrect) {
                    bestCorrect = true;
                    bestSpeed = candidateSpeed;
                    bestSlot = slot;
                } else if (candidateCorrect && candidateSpeed > bestSpeed) {
                    bestSpeed = candidateSpeed;
                    bestSlot = slot;
                }
            } else {
                if (candidateSpeed > bestSpeed) {
                    bestSpeed = candidateSpeed;
                    bestSlot = slot;
                }
            }
        }

        if (requiresCorrectTool && !bestCorrect) {
            return -1;
        }

        return bestSlot;
    }

    public static boolean hasHarvestTool(Player player, BlockState state) {
        if (player.isCreative()) {
            return true;
        }
        if (!state.requiresCorrectToolForDrops()) {
            return true;
        }
        if (player.hasCorrectToolForDrops(state)) {
            ItemStack held = player.getMainHandItem();
            if (held.isEmpty() || !BlockBreakHandler.isToolTooDamaged(held)) {
                return true;
            }
        }
        var config = ConfigManager.getServerConfig();
        if (config.autoToolSwap) {
            for (int slot = 0; slot < 9; slot++) {
                ItemStack stack = player.getInventory().getItem(slot);
                if (!stack.isEmpty() && !BlockBreakHandler.isToolTooDamaged(stack) && stack.isCorrectToolForDrops(state)) {
                    return true;
                }
            }
        }
        return false;
    }
}
