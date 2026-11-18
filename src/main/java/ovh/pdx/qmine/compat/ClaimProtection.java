package ovh.pdx.qmine.compat;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.lang.reflect.Method;

public final class ClaimProtection {
    private static Boolean commonProtectionPresent = null;
    private static Method canBreakBlockMethod = null;

    private ClaimProtection() {
    }

    public static boolean canBreak(ServerLevel level, BlockPos pos, ServerPlayer player) {
        if (player.blockActionRestricted(level, pos, player.gameMode.getGameModeForPlayer())) {
            return false;
        }

        if (commonProtectionPresent == null) {
            try {
                Class<?> clazz = Class.forName("eu.pb4.common.protection.api.CommonProtection");
                canBreakBlockMethod = clazz.getMethod("canBreakBlock", net.minecraft.world.level.Level.class, BlockPos.class, com.mojang.authlib.GameProfile.class, Player.class);
                commonProtectionPresent = true;
            } catch (Throwable t) {
                commonProtectionPresent = false;
            }
        }

        if (Boolean.TRUE.equals(commonProtectionPresent) && canBreakBlockMethod != null) {
            try {
                return (boolean) canBreakBlockMethod.invoke(null, level, pos, player.getGameProfile(), player);
            } catch (Throwable ignored) {
            }
        }

        return true;
    }
}
