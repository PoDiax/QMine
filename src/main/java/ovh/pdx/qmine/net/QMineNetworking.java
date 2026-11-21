package ovh.pdx.qmine.net;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import ovh.pdx.qmine.Qmine;
import ovh.pdx.qmine.tag.QMineTags;

import java.util.Optional;
import java.util.function.Supplier;

public final class QMineNetworking {
    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(QMineTags.MOD_ID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    private QMineNetworking() {
    }

    public static void register() {
        int id = 0;

        CHANNEL.registerMessage(
                id++,
                KeyPressedPayload.class,
                KeyPressedPayload::write,
                KeyPressedPayload::read,
                QMineNetworking::handleKeyPressed,
                Optional.of(NetworkDirection.PLAY_TO_SERVER)
        );

        CHANNEL.registerMessage(
                id++,
                ModeChangedPayload.class,
                ModeChangedPayload::write,
                ModeChangedPayload::read,
                QMineNetworking::handleModeChanged,
                Optional.of(NetworkDirection.PLAY_TO_SERVER)
        );

        CHANNEL.registerMessage(
                id++,
                ShapeResizePayload.class,
                ShapeResizePayload::write,
                ShapeResizePayload::read,
                QMineNetworking::handleShapeResize,
                Optional.of(NetworkDirection.PLAY_TO_SERVER)
        );

        CHANNEL.registerMessage(
                id++,
                SendShapePayload.class,
                SendShapePayload::write,
                SendShapePayload::read,
                QMineNetworking::handleSendShape,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT)
        );

        CHANNEL.registerMessage(
                id++,
                SyncCooldownPayload.class,
                SyncCooldownPayload::write,
                SyncCooldownPayload::read,
                QMineNetworking::handleSyncCooldown,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT)
        );
    }

    private static void handleKeyPressed(KeyPressedPayload payload, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player != null && Qmine.getInstance() != null) {
                Qmine.getInstance().setKeyPressed(player, payload.pressed());
            }
        });
        ctx.setPacketHandled(true);
    }

    private static void handleModeChanged(ModeChangedPayload payload, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player != null && Qmine.getInstance() != null) {
                Qmine.getInstance().cycleMode(player, payload.next());
            }
        });
        ctx.setPacketHandled(true);
    }

    private static void handleShapeResize(ShapeResizePayload payload, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player != null && Qmine.getInstance() != null) {
                Qmine.getInstance().resizeMode(player, payload.increase());
            }
        });
        ctx.setPacketHandled(true);
    }

    private static void handleSendShape(SendShapePayload payload, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> ClientPayloadReceiver.handleSendShape(payload));
        ctx.setPacketHandled(true);
    }

    private static void handleSyncCooldown(SyncCooldownPayload payload, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> ClientPayloadReceiver.handleSyncCooldown(payload));
        ctx.setPacketHandled(true);
    }

    public static void sendToServer(Object packet) {
        CHANNEL.sendToServer(packet);
    }

    public static void sendToPlayer(ServerPlayer player, Object packet) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }

    public static boolean hasClientMod(ServerPlayer player) {
        return true;
    }
}
