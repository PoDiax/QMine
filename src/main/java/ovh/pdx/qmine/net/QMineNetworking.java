package ovh.pdx.qmine.net;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import ovh.pdx.qmine.Qmine;

public final class QMineNetworking {
    private QMineNetworking() {
    }

    public static void initCommon() {
        // C2S payloads
        PayloadTypeRegistry.serverboundPlay().register(KeyPressedPayload.TYPE, KeyPressedPayload.STREAM_CODEC);
        PayloadTypeRegistry.serverboundPlay().register(ModeChangedPayload.TYPE, ModeChangedPayload.STREAM_CODEC);
        PayloadTypeRegistry.serverboundPlay().register(ShapeResizePayload.TYPE, ShapeResizePayload.STREAM_CODEC);

        // S2C payloads (register large for SendShapePayload to support up to 64k positions)
        PayloadTypeRegistry.clientboundPlay().registerLarge(SendShapePayload.TYPE, SendShapePayload.STREAM_CODEC, 1048576);
        PayloadTypeRegistry.clientboundPlay().register(SyncCooldownPayload.TYPE, SyncCooldownPayload.STREAM_CODEC);

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

    public static void sendToPlayer(ServerPlayer player, CustomPacketPayload payload) {
        if (hasClientMod(player)) {
            ServerPlayNetworking.send(player, payload);
        }
    }
}
