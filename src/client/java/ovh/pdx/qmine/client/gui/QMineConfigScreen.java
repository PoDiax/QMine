package ovh.pdx.qmine.client.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;
import ovh.pdx.qmine.client.render.QMineOutlineRenderer;
import ovh.pdx.qmine.config.ConfigManager;
import ovh.pdx.qmine.config.QMineClientConfig;

public class QMineConfigScreen extends Screen {
    @Nullable
    private final Screen parent;

    public QMineConfigScreen(@Nullable Screen parent) {
        super(Component.literal("QMine Settings"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        QMineClientConfig clientCfg = ConfigManager.getClientConfig();

        int buttonWidth = 155;
        int buttonHeight = 20;
        int spacing = 24;
        int leftColX = this.width / 2 - buttonWidth - 5;
        int rightColX = this.width / 2 + 5;
        int startY = this.height / 6;

        var wallBtn = CycleButton.onOffBuilder(clientCfg.renderThroughWalls)
                .create(leftColX, startY, buttonWidth, buttonHeight, Component.literal("Through Walls"),
                        (button, value) -> clientCfg.renderThroughWalls = value);
        wallBtn.setTooltip(Tooltip.create(Component.literal(
                "If enabled, mining outlines render through solid blocks (X-ray). If disabled, blocks hide obscured outlines."
        )));
        addRenderableWidget(wallBtn);

        var gridBtn = CycleButton.onOffBuilder(clientCfg.showBlockGrid)
                .create(rightColX, startY, buttonWidth, buttonHeight, Component.literal("Block Grid"),
                        (button, value) -> {
                            clientCfg.showBlockGrid = value;
                            QMineOutlineRenderer.getInstance().clear();
                        });
        gridBtn.setTooltip(Tooltip.create(Component.literal(
                "If enabled, outlines each individual block unit so you can see depth/length segments along the shape."
        )));
        addRenderableWidget(gridBtn);

        var lockScrollBtn = CycleButton.onOffBuilder(clientCfg.lockScrollToSize)
                .create(leftColX, startY + spacing, buttonWidth, buttonHeight, Component.literal("Scroll Changes Size"),
                        (button, value) -> clientCfg.lockScrollToSize = value);
        lockScrollBtn.setTooltip(Tooltip.create(Component.literal(
                "If enabled, scrolling always resizes shape without needing Shift. Arrow keys switch modes."
        )));
        addRenderableWidget(lockScrollBtn);

        var keyCycleBtn = CycleButton.onOffBuilder(clientCfg.requireActiveKeyForCycling)
                .create(leftColX, startY + spacing * 2, buttonWidth, buttonHeight, Component.literal("Hold Key to Cycle"),
                        (button, value) -> clientCfg.requireActiveKeyForCycling = value);
        keyCycleBtn.setTooltip(Tooltip.create(Component.literal(
                "Require holding down the activation key (` by default) to switch mining modes."
        )));
        addRenderableWidget(keyCycleBtn);

        var anchorBtn = CycleButton.<QMineClientConfig.OverlayAnchor>builder(
                        anchor -> Component.literal(anchor.name()),
                        clientCfg.overlayAnchor
                )
                .withValues(QMineClientConfig.OverlayAnchor.values())
                .create(rightColX, startY + spacing * 2, buttonWidth, buttonHeight, Component.literal("HUD Anchor"),
                        (button, value) -> clientCfg.overlayAnchor = value);
        anchorBtn.setTooltip(Tooltip.create(Component.literal("Screen corner/position for the QMine HUD overlay.")));
        addRenderableWidget(anchorBtn);

        var treeBtn = CycleButton.onOffBuilder(ConfigManager.getServerConfig().treeHarvesterEnabled)
                .create(leftColX, startY + spacing * 3, buttonWidth, buttonHeight, Component.literal("Tree Harvester"),
                        (button, value) -> ConfigManager.getServerConfig().treeHarvesterEnabled = value);
        treeBtn.setTooltip(Tooltip.create(Component.literal(
                "When enabled, cutting a tree fells the whole tree and rapidly decays leaves without needing to hold the mining key."
        )));
        addRenderableWidget(treeBtn);

        var animBtn = CycleButton.<ovh.pdx.qmine.tree.TreeFellingAnimation>builder(
                        anim -> Component.literal(anim.displayName),
                        ConfigManager.getServerConfig().treeFellingAnimation
                )
                .withValues(ovh.pdx.qmine.tree.TreeFellingAnimation.values())
                .create(rightColX, startY + spacing * 3, buttonWidth, buttonHeight, Component.literal("Animation"),
                        (button, value) -> ConfigManager.getServerConfig().treeFellingAnimation = value);
        animBtn.setTooltip(Tooltip.create(Component.literal("Tree felling animation style: Instant, Falling Blocks, Shift Down, or Cascade.")));
        addRenderableWidget(animBtn);

        var leafPartBtn = CycleButton.onOffBuilder(ConfigManager.getServerConfig().leafDecayParticles)
                .create(leftColX, startY + spacing * 4, buttonWidth, buttonHeight, Component.literal("Leaf Particles"),
                        (button, value) -> ConfigManager.getServerConfig().leafDecayParticles = value);
        leafPartBtn.setTooltip(Tooltip.create(Component.literal(
                "Emit break particles when leaves rapidly decay. Turn off to reduce clutter."
        )));
        addRenderableWidget(leafPartBtn);

        var leafSoundBtn = CycleButton.onOffBuilder(ConfigManager.getServerConfig().leafDecaySounds)
                .create(rightColX, startY + spacing * 4, buttonWidth, buttonHeight, Component.literal("Leaf Sounds"),
                        (button, value) -> ConfigManager.getServerConfig().leafDecaySounds = value);
        leafSoundBtn.setTooltip(Tooltip.create(Component.literal(
                "Play leaf crunching sounds when leaves decay. Turn off for silent leaf decay."
        )));
        addRenderableWidget(leafSoundBtn);

        var keyBindsBtn = Button.builder(Component.literal("Key Binds..."), button -> {
            if (this.minecraft != null) {
                this.minecraft.gui.setScreen(new net.minecraft.client.gui.screens.options.controls.KeyBindsScreen(this, this.minecraft.options));
            }
        }).bounds(leftColX, startY + spacing * 5, buttonWidth * 2 + 10, buttonHeight).build();
        keyBindsBtn.setTooltip(Tooltip.create(Component.literal(
                "Rebind the QMine activation key and shape switching keys in Minecraft's Controls menu."
        )));
        addRenderableWidget(keyBindsBtn);

        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> onClose())
                .bounds(this.width / 2 - 100, startY + spacing * 6 + 4, 200, 20)
                .build()
        );
    }

    @Override
    public void onClose() {
        ConfigManager.saveClientConfig();
        ConfigManager.saveServerConfig();
        if (this.minecraft != null) {
            this.minecraft.gui.setScreen(this.parent);
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks) {
        extractMenuBackground(graphics);
        super.extractRenderState(graphics, mouseX, mouseY, partialTicks);
        graphics.centeredText(this.font, this.title, this.width / 2, this.height / 6 - 25, 0x00E5FF);
    }
}
