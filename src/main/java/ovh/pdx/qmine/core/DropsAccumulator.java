package ovh.pdx.qmine.core;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import ovh.pdx.qmine.config.QMineConfig;

public final class DropsAccumulator {
    private boolean active = false;
    private int accumulatedXp = 0;
    private final ItemCollector itemCollector = new ItemCollector();

    public void start() {
        active = true;
        accumulatedXp = 0;
        itemCollector.clear();
    }

    public void finish(ServerLevel level, BlockPos pos) {
        finish(level, pos, null, QMineConfig.DropCollectionMode.ORIGIN);
    }

    public void finish(ServerLevel level, BlockPos pos, ServerPlayer player, QMineConfig.DropCollectionMode mode) {
        active = false;
        if (player != null) {
            itemCollector.deliver(level, pos, player, mode);
        } else {
            itemCollector.drop(level, pos);
        }
        itemCollector.clear();

        if (accumulatedXp > 0) {
            if (player != null && mode == QMineConfig.DropCollectionMode.INVENTORY) {
                player.giveExperiencePoints(accumulatedXp);
            } else if (player != null && mode == QMineConfig.DropCollectionMode.MAGNET) {
                level.addFreshEntity(new ExperienceOrb(
                        level,
                        player.getX(),
                        player.getY(),
                        player.getZ(),
                        accumulatedXp
                ));
            } else {
                level.addFreshEntity(new ExperienceOrb(
                        level,
                        pos.getX() + 0.5D,
                        pos.getY() + 0.5D,
                        pos.getZ() + 0.5D,
                        accumulatedXp
                ));
            }
            accumulatedXp = 0;
        }
    }

    public boolean handleEntity(Entity entity, Level level) {
        if (!active || !entity.isAlive() || !(level instanceof ServerLevel serverLevel)) {
            return false;
        }

        if (entity instanceof ItemEntity itemEntity) {
            ItemStack stack = itemEntity.getItem();
            if (!stack.isEmpty()) {
                itemCollector.accept(stack);
                itemEntity.setItem(ItemStack.EMPTY);
            }
            return true;
        } else if (entity instanceof ExperienceOrb orb) {
            accumulatedXp += orb.getValue();
            entity.kill(serverLevel);
            return true;
        }

        return false;
    }

    public boolean isActive() {
        return active;
    }

    public ItemCollector getItemCollector() {
        return itemCollector;
    }
}
