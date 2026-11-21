package ovh.pdx.qmine.net;


import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.minecraft.server.level.ServerPlayer;
import ovh.pdx.qmine.Qmine;

public final class QMineNetworking {
    private QMineNetworking() {
    }

    
    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(ovh.pdx.qmine.tag.QMineTags.MOD_ID).versioned("1.0.1");

        registrar.playToServer(KeyPressedPayload.TYPE, KeyPressedPayload.STREAM_CODEC, (payload, context) -> {
            context.enqueueWork(() -> {
                if (context.player() instanceof ServerPlayer player) {
                    Qmine.getInstance().setKeyPressed(player, payload.pressed());
                }
            });
        });

        registrar.playToServer(ModeChangedPayload.TYPE, ModeChangedPayload.STREAM_CODEC, (payload, context) -> {
            context.enqueueWork(() -> {
                if (context.player() instanceof ServerPlayer player) {
                    Qmine.getInstance().cycleMode(player, payload.next());
                }
            });
        });

        registrar.playToServer(ShapeResizePayload.TYPE, ShapeResizePayload.STREAM_CODEC, (payload, context) -> {
            context.enqueueWork(() -> {
                if (context.player() instanceof ServerPlayer player) {
                    Qmine.getInstance().resizeMode(player, payload.increase());
                }
            });
        });

        registrar.playToClient(SendShapePayload.TYPE, SendShapePayload.STREAM_CODEC, (payload, context) -> {
            context.enqueueWork(() -> {
                // Client handler
                ClientPayloadReceiver.handleSendShape(payload);
            });
        });

        registrar.playToClient(SyncCooldownPayload.TYPE, SyncCooldownPayload.STREAM_CODEC, (payload, context) -> {
            context.enqueueWork(() -> {
                // Client handler
                ClientPayloadReceiver.handleSyncCooldown(payload);
            });
        });
    }

    public static boolean hasClientMod(ServerPlayer player) {
        return player.connection != null;
    }

    public static void sendToPlayer(ServerPlayer player, SendShapePayload payload) {
        if (player != null && player.connection != null) {
            PacketDistributor.sendToPlayer(player, payload);
        }
    }

    public static void sendToPlayer(ServerPlayer player, SyncCooldownPayload payload) {
        if (player != null && player.connection != null) {
            PacketDistributor.sendToPlayer(player, payload);
        }
    }
}
