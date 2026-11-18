package ovh.pdx.qmine.handler;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

public final class AutoToolSwapper {
    private AutoToolSwapper() {
    }

    public static int findBestToolSlot(ServerPlayer player, BlockState state) {
        ItemStack current = player.getMainHandItem();
        boolean requiresCorrectTool = state.requiresCorrectToolForDrops();
        boolean currentCorrect = player.hasCorrectToolForDrops(state);
        float currentSpeed = current.isEmpty() ? 1.0F : current.getDestroySpeed(state);

        // If current tool is already effective and has correct tool for drops, no need to swap
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
                } else if (candidateCorrect == bestCorrect && candidateSpeed > bestSpeed) {
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

        return bestSlot;
    }
}
