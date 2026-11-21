package ovh.pdx.qmine.net;


import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

import net.minecraft.server.level.ServerPlayer;
import ovh.pdx.qmine.Qmine;

public final class QMineNetworking {
    private QMineNetworking() {
    }

    public static void initCommon() {
        
        
        ServerPlayNetworking.registerGlobalReceiver(KeyPressedPayload.ID, (server, player, handler, buf, responseSender) -> {
            KeyPressedPayload payload = KeyPressedPayload.read(buf);
            server.execute(() -> Qmine.getInstance().setKeyPressed(player, payload.pressed()));
        });

        ServerPlayNetworking.registerGlobalReceiver(ModeChangedPayload.ID, (server, player, handler, buf, responseSender) -> {
            ModeChangedPayload payload = ModeChangedPayload.read(buf);
            server.execute(() -> Qmine.getInstance().cycleMode(player, payload.next()));
        });

        ServerPlayNetworking.registerGlobalReceiver(ShapeResizePayload.ID, (server, player, handler, buf, responseSender) -> {
            ShapeResizePayload payload = ShapeResizePayload.read(buf);
            server.execute(() -> Qmine.getInstance().resizeMode(player, payload.increase()));
        });
    }

    public static boolean hasClientMod(ServerPlayer player) {
        
        return ServerPlayNetworking.canSend(player, SendShapePayload.ID);
    }

    public static void sendToPlayer(ServerPlayer player, SendShapePayload payload) {
        if (!hasClientMod(player)) {
            return;
        }
        
        net.minecraft.network.FriendlyByteBuf buf = net.fabricmc.fabric.api.networking.v1.PacketByteBufs.create();
        payload.write(buf);
        ServerPlayNetworking.send(player, SendShapePayload.ID, buf);
    }

    public static void sendToPlayer(ServerPlayer player, SyncCooldownPayload payload) {
        if (!hasClientMod(player)) {
            return;
        }
        
        net.minecraft.network.FriendlyByteBuf buf = net.fabricmc.fabric.api.networking.v1.PacketByteBufs.create();
        payload.write(buf);
        ServerPlayNetworking.send(player, SyncCooldownPayload.ID, buf);
    }
    
}
