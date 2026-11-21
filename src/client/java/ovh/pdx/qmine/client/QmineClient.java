package ovh.pdx.qmine.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;


import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;


import ovh.pdx.qmine.client.render.QMineHudOverlay;
import ovh.pdx.qmine.client.render.QMineOutlineRenderer;
import ovh.pdx.qmine.config.ConfigManager;

public class QmineClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ConfigManager.loadClientConfig();

        QMineClientController controller = QMineClientController.getInstance();
        controller.initNetworking();

        ClientTickEvents.START_CLIENT_TICK.register(controller::clientTick);

        
        
        HudRenderCallback.EVENT.register((guiGraphics, deltaTracker) -> {
            QMineHudOverlay.getInstance().render(guiGraphics, deltaTracker.getGameTimeDeltaPartialTick(false));
        });

        WorldRenderEvents.AFTER_TRANSLUCENT.register(context -> {
            QMineOutlineRenderer.getInstance().renderInGame(context);
        });
        
        
    }
}
