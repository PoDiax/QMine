package ovh.pdx.qmine.core;

import it.unimi.dsi.fastutil.Hash;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenCustomHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import ovh.pdx.qmine.config.QMineConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public final class ItemCollector implements Consumer<ItemStack> {
    private static final Hash.Strategy<ItemStack> STACK_STRATEGY = new Hash.Strategy<>() {
        @Override
        public int hashCode(ItemStack stack) {
            return ItemStack.hashItemAndComponents(stack);
        }

        @Override
        public boolean equals(ItemStack a, ItemStack b) {
            return a == b || (a != null && b != null && a.isEmpty() == b.isEmpty() && ItemStack.isSameItemSameComponents(a, b));
        }
    };

    private final Object2IntMap<ItemStack> items = new Object2IntOpenCustomHashMap<>(STACK_STRATEGY);

    public void clear() {
        items.clear();
    }

    public synchronized ItemStack takeFirstMatching(java.util.function.Predicate<ItemStack> predicate) {
        for (var entry : items.object2IntEntrySet()) {
            ItemStack key = entry.getKey();
            if (predicate.test(key) && entry.getIntValue() > 0) {
                int count = entry.getIntValue();
                if (count <= 1) {
                    items.removeInt(key);
                } else {
                    entry.setValue(count - 1);
                }
                return key.copyWithCount(1);
            }
        }
        return ItemStack.EMPTY;
    }

    @Override
    public void accept(ItemStack stack) {
        if (!stack.isEmpty()) {
            items.mergeInt(stack.copyWithCount(1), stack.getCount(), Integer::sum);
        }
    }

    public void drop(Level level, BlockPos pos) {
        if (items.isEmpty()) {
            return;
        }

        List<ItemStack> stacks = unpackStacks();
        for (ItemStack stack : stacks) {
            Block.popResource(level, pos, stack);
        }
    }

    public void deliver(Level level, BlockPos originPos, ServerPlayer player, QMineConfig.DropCollectionMode mode) {
        if (items.isEmpty()) {
            return;
        }

        List<ItemStack> stacks = unpackStacks();

        switch (mode) {
            case INVENTORY -> {
                for (ItemStack stack : stacks) {
                    if (!player.getInventory().add(stack)) {
                        Block.popResource(level, player.blockPosition(), stack);
                    }
                }
            }
            case MAGNET -> {
                for (ItemStack stack : stacks) {
                    Block.popResource(level, player.blockPosition(), stack);
                }
            }
            case ORIGIN -> {
                for (ItemStack stack : stacks) {
                    Block.popResource(level, originPos, stack);
                }
            }
        }
    }

    private List<ItemStack> unpackStacks() {
        List<ItemStack> stacks = new ArrayList<>();
        items.forEach((stack, remaining) -> {
            while (remaining > 0) {
                int count = Math.min(stack.getMaxStackSize(), remaining);
                stacks.add(stack.copyWithCount(count));
                remaining -= count;
            }
        });
        return stacks;
    }
}
