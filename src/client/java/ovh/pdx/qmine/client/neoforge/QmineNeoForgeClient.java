package ovh.pdx.qmine.client.neoforge;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.SubmitCustomGeometryEvent;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import ovh.pdx.qmine.client.QMineClientController;
import ovh.pdx.qmine.client.gui.QMineConfigScreen;
import ovh.pdx.qmine.client.gui.QMineYaclScreen;
import ovh.pdx.qmine.client.render.QMineHudOverlay;
import ovh.pdx.qmine.client.render.QMineOutlineRenderer;
import ovh.pdx.qmine.config.ConfigManager;
import ovh.pdx.qmine.core.CooldownTracker;
import ovh.pdx.qmine.net.ClientPayloadReceiver;
import ovh.pdx.qmine.net.SyncCooldownPayload;
import ovh.pdx.qmine.tag.QMineTags;

@EventBusSubscriber(modid = QMineTags.MOD_ID, value = Dist.CLIENT)
public final class QmineNeoForgeClient {
    private QmineNeoForgeClient() {
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        ConfigManager.loadClientConfig();
        QMineClientController.getInstance();

        ClientPayloadReceiver.registerHandlers(
                payload -> QMineClientController.getInstance().setShape(payload.shapeIndex(), payload.shapeRadius(), payload.positions().orElse(null)),
                payload -> {
                    if (payload.cooldownType() == SyncCooldownPayload.CooldownType.COOLDOWN) {
                        CooldownTracker.setClientCooldownTicks(payload.timestamp());
                    } else if (payload.cooldownType() == SyncCooldownPayload.CooldownType.LAST_USED) {
                        Minecraft mc = Minecraft.getInstance();
                        if (mc.player != null) {
                            CooldownTracker.setLastMineTime(mc.player, payload.timestamp());
                        }
                    }
                }
        );

        ModLoadingContext.get().registerExtensionPoint(
                IConfigScreenFactory.class,
                () -> (mc, parent) -> {
                    try {
                        return QMineYaclScreen.create(parent);
                    } catch (Throwable t) {
                        return new QMineConfigScreen(parent);
                    }
                }
        );
    }

    @SubscribeEvent
    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        QMineClientController controller = QMineClientController.getInstance();
        event.register(controller.keyActivate);
        event.register(controller.keyNext);
        event.register(controller.keyPrev);
    }

    @SubscribeEvent
    public static void onRegisterGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAbove(
                VanillaGuiLayers.SUBTITLE_OVERLAY,
                Identifier.fromNamespaceAndPath(QMineTags.MOD_ID, "hud_overlay"),
                (graphics, deltaTracker) -> {
                    QMineHudOverlay.getInstance().render(graphics, deltaTracker);
                }
        );
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Pre event) {
        Minecraft mc = Minecraft.getInstance();
        QMineClientController.getInstance().clientTick(mc);
    }

    @SubscribeEvent
    public static void onSubmitCustomGeometry(SubmitCustomGeometryEvent event) {
        QMineOutlineRenderer.getInstance().renderNeoForge(event);
    }
}
