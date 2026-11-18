package ovh.pdx.qmine.core;

import com.google.common.primitives.Ints;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import ovh.pdx.qmine.mixin.ServerPlayerAccessor;

public final class XPUtils {
    private XPUtils() {
    }

    public static long getPlayerXP(Player player) {
        return getExperienceForLevel(player.experienceLevel) + (long) (player.experienceProgress * player.getXpNeededForNextLevel());
    }

    public static void addPlayerXP(Player player, int amount) {
        long currentXp = getPlayerXP(player);
        long newXp = Math.max(0, currentXp + amount);

        player.totalExperience = Ints.saturatedCast(newXp);
        player.experienceLevel = getLevelForExperience(newXp);
        long expForLevel = getExperienceForLevel(player.experienceLevel);
        player.experienceProgress = (float) (newXp - expForLevel) / (float) player.getXpNeededForNextLevel();

        if (player instanceof ServerPlayer sp && sp instanceof ServerPlayerAccessor accessor) {
            accessor.qmine$setLastSentExp(-1);
        }
    }

    public static long getExperienceForLevel(int level) {
        if (level <= 0) return 0L;
        if (level <= 15) return sum(level, 7, 2);
        if (level <= 30) return 315L + sum(level - 15, 37, 5);
        return 1395L + sum(level - 30, 112, 9);
    }

    private static long sum(int n, int a0, int d) {
        return (long) n * (2L * a0 + (long) (n - 1) * d) / 2L;
    }

    public static int xpBarCap(int level) {
        if (level >= 30) return 112 + (level - 30) * 9;
        if (level >= 15) return 37 + (level - 15) * 5;
        return 7 + level * 2;
    }

    public static int getLevelForExperience(long targetXp) {
        int level = 0;
        while (true) {
            int xpToNext = xpBarCap(level);
            if (targetXp < xpToNext) {
                return level;
            }
            level++;
            targetXp -= xpToNext;
        }
    }
}
