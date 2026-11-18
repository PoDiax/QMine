package ovh.pdx.qmine.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
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

        HudElementRegistry.attachElementAfter(
                VanillaHudElements.SUBTITLES,
                QMineHudOverlay.ID,
                QMineHudOverlay.getInstance()
        );

        LevelRenderEvents.AFTER_TRANSLUCENT_FEATURES.register(context -> {
            QMineOutlineRenderer.getInstance().renderInGame(context);
        });
    }
}
