package ovh.pdx.qmine.net;

import java.util.function.Consumer;

public final class ClientPayloadReceiver {
    private static Consumer<SendShapePayload> sendShapeHandler;
    private static Consumer<SyncCooldownPayload> syncCooldownHandler;

    private ClientPayloadReceiver() {
    }

    public static void registerHandlers(Consumer<SendShapePayload> shapeHandler, Consumer<SyncCooldownPayload> cooldownHandler) {
        sendShapeHandler = shapeHandler;
        syncCooldownHandler = cooldownHandler;
    }

    public static void handleSendShape(SendShapePayload payload) {
        if (sendShapeHandler != null) {
            sendShapeHandler.accept(payload);
        }
    }

    public static void handleSyncCooldown(SyncCooldownPayload payload) {
        if (syncCooldownHandler != null) {
            syncCooldownHandler.accept(payload);
        }
    }
}
