package ovh.pdx.qmine.core;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import ovh.pdx.qmine.config.ConfigManager;
import ovh.pdx.qmine.net.QMineNetworking;
import ovh.pdx.qmine.net.SyncCooldownPayload;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class CooldownTracker {
    private static final CooldownTracker CLIENT_TRACKER = new CooldownTracker();
    private static final CooldownTracker SERVER_TRACKER = new CooldownTracker();

    private final Map<UUID, Long> lastMineTime = new ConcurrentHashMap<>();
    private static long clientCooldownTicks = 0L;

    private CooldownTracker() {
    }

    private static CooldownTracker getTracker(Player player) {
        return player.level().isClientSide() ? CLIENT_TRACKER : SERVER_TRACKER;
    }

    public static long getLastMineTime(Player player) {
        return getTracker(player).lastMineTime.getOrDefault(player.getUUID(), 0L);
    }

    public static void setLastMineTime(Player player, long when) {
        getTracker(player).lastMineTime.put(player.getUUID(), when);
        if (player instanceof ServerPlayer sp) {
            QMineNetworking.sendToPlayer(sp, new SyncCooldownPayload(when, SyncCooldownPayload.CooldownType.LAST_USED));
        }
    }

    public static float getCooldownRemaining(Player player) {
        long cooldown = getCooldownTicks(player);
        if (cooldown <= 0L) {
            return 1.0f;
        }
        long tickDelta = (System.currentTimeMillis() - getLastMineTime(player)) / 50L;
        return Mth.clamp((float) tickDelta / (float) cooldown, 0.0f, 1.0f);
    }

    public static boolean isOnCooldown(Player player) {
        long cooldown = getCooldownTicks(player);
        if (cooldown <= 0L) {
            return false;
        }
        long cooldownMs = cooldown * 50L;
        return System.currentTimeMillis() - getLastMineTime(player) < cooldownMs;
    }

    public static long getCooldownTicks(Player player) {
        if (player instanceof ServerPlayer) {
            return ConfigManager.getServerConfig().cooldownTicks;
        }
        return clientCooldownTicks;
    }

    public static void setClientCooldownTicks(long ticks) {
        clientCooldownTicks = ticks;
    }
}
