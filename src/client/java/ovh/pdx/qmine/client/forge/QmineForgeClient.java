package ovh.pdx.qmine.client.forge;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import ovh.pdx.qmine.client.QMineClientController;
import ovh.pdx.qmine.client.gui.QMineConfigScreen;
import ovh.pdx.qmine.client.gui.QMineYaclScreen;
import ovh.pdx.qmine.client.render.QMineHudOverlay;
import ovh.pdx.qmine.client.render.QMineOutlineRenderer;
import ovh.pdx.qmine.config.ConfigManager;
import ovh.pdx.qmine.tag.QMineTags;

@Mod.EventBusSubscriber(modid = QMineTags.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class QmineForgeClient {
    private QmineForgeClient() {
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        ConfigManager.loadClientConfig();
        QMineClientController.getInstance().initNetworking();

        MinecraftForge.EVENT_BUS.addListener(QmineForgeClient::onClientTick);
        MinecraftForge.EVENT_BUS.addListener(QmineForgeClient::onRenderLevelStage);

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
    }

    @SubscribeEvent
    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        QMineClientController controller = QMineClientController.getInstance();
        event.register(controller.keyActivate);
        event.register(controller.keyNext);
        event.register(controller.keyPrev);
    }

    @SubscribeEvent
    public static void onRegisterGuiOverlays(RegisterGuiOverlaysEvent event) {
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
