package ovh.pdx.qmine.client.forge;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ovh.pdx.qmine.client.QMineClientController;
import ovh.pdx.qmine.client.gui.QMineConfigScreen;
import ovh.pdx.qmine.client.gui.QMineYaclScreen;
import ovh.pdx.qmine.client.render.QMineHudOverlay;
import ovh.pdx.qmine.client.render.QMineOutlineRenderer;
import ovh.pdx.qmine.config.ConfigManager;

public final class QmineForgeClient {
    private static final Logger LOGGER = LoggerFactory.getLogger("QMine/Client");

    private QmineForgeClient() {
    }

    public static void init(IEventBus modEventBus) {
        LOGGER.info("Registering QMine Forge client listeners...");
        modEventBus.addListener(QmineForgeClient::onClientSetup);
        modEventBus.addListener(QmineForgeClient::onRegisterKeyMappings);
        modEventBus.addListener(QmineForgeClient::onRegisterGuiOverlays);

        MinecraftForge.EVENT_BUS.addListener(QmineForgeClient::onClientTick);
        MinecraftForge.EVENT_BUS.addListener(QmineForgeClient::onRenderLevelStage);
    }

    private static void onClientSetup(FMLClientSetupEvent event) {
        LOGGER.info("QMine Forge client setup starting...");
        ConfigManager.loadClientConfig();
        QMineClientController.getInstance().initNetworking();

        ModLoadingContext.get().registerExtensionPoint(
                ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new ConfigScreenHandler.ConfigScreenFactory((mc, parent) -> {
                    try {
                        return QMineYaclScreen.create(parent);
                    } catch (Throwable t) {
                        return new QMineConfigScreen(parent);
                    }
                })
        );
        LOGGER.info("QMine Forge client setup complete.");
    }

    private static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        LOGGER.info("Registering QMine key mappings on Forge...");
        QMineClientController controller = QMineClientController.getInstance();
        event.register(controller.keyActivate);
        event.register(controller.keyNext);
        event.register(controller.keyPrev);
    }

    private static void onRegisterGuiOverlays(RegisterGuiOverlaysEvent event) {
        LOGGER.info("Registering QMine HUD overlay on Forge...");
        event.registerAbove(
                VanillaGuiOverlay.SUBTITLES.id(),
                "hud_overlay",
                (gui, graphics, partialTick, width, height) -> {
                    QMineHudOverlay.getInstance().render(graphics, partialTick);
                }
        );
    }

    private static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            Minecraft mc = Minecraft.getInstance();
            QMineClientController.getInstance().clientTick(mc);
        }
    }

    private static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
            Minecraft mc = Minecraft.getInstance();
            QMineClientController controller = QMineClientController.getInstance();
            if (!controller.isKeyPressed() || !controller.canMine()) {
                return;
            }
            Camera camera = event.getCamera();
            PoseStack poseStack = event.getPoseStack();
            MultiBufferSource.BufferSource bufferSource = mc.renderBuffers().bufferSource();
            QMineOutlineRenderer.getInstance().renderOutlines(poseStack, camera, bufferSource);
        }
    }
}
