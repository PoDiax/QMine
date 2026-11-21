package ovh.pdx.qmine.net;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

import net.minecraft.server.level.ServerPlayer;
import ovh.pdx.qmine.Qmine;

public final class QMineNetworking {
    private QMineNetworking() {
    }

    public static void initCommon() {
        
        PayloadTypeRegistry.playC2S().register(KeyPressedPayload.TYPE, KeyPressedPayload.STREAM_CODEC);
        PayloadTypeRegistry.playC2S().register(ModeChangedPayload.TYPE, ModeChangedPayload.STREAM_CODEC);
        PayloadTypeRegistry.playC2S().register(ShapeResizePayload.TYPE, ShapeResizePayload.STREAM_CODEC);

        PayloadTypeRegistry.playS2C().register(SendShapePayload.TYPE, SendShapePayload.STREAM_CODEC);
        PayloadTypeRegistry.playS2C().register(SyncCooldownPayload.TYPE, SyncCooldownPayload.STREAM_CODEC);

        // Server receivers
        ServerPlayNetworking.registerGlobalReceiver(KeyPressedPayload.TYPE, (payload, context) -> {
            ServerPlayer player = context.player();
            context.server().execute(() -> Qmine.getInstance().setKeyPressed(player, payload.pressed()));
        });

        ServerPlayNetworking.registerGlobalReceiver(ModeChangedPayload.TYPE, (payload, context) -> {
            ServerPlayer player = context.player();
            context.server().execute(() -> Qmine.getInstance().cycleMode(player, payload.next()));
        });

        ServerPlayNetworking.registerGlobalReceiver(ShapeResizePayload.TYPE, (payload, context) -> {
            ServerPlayer player = context.player();
            context.server().execute(() -> Qmine.getInstance().resizeMode(player, payload.increase()));
        });
        
        
    }

    public static boolean hasClientMod(ServerPlayer player) {
        return ServerPlayNetworking.canSend(player, SendShapePayload.TYPE);
        
    }

    public static void sendToPlayer(ServerPlayer player, SendShapePayload payload) {
        if (!hasClientMod(player)) {
            return;
        }
        ServerPlayNetworking.send(player, payload);
        
    }

    public static void sendToPlayer(ServerPlayer player, SyncCooldownPayload payload) {
        if (!hasClientMod(player)) {
            return;
        }
        ServerPlayNetworking.send(player, payload);
        
    }
    
}
