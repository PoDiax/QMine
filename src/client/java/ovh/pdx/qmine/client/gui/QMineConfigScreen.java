package ovh.pdx.qmine.client.gui;



import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.options.controls.KeyBindsScreen;


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

        // 1. Through-Wall Outline (X-Ray Preview)
        var wallBtn = CycleButton.onOffBuilder(clientCfg.renderThroughWalls)
                .create(leftColX, startY, buttonWidth, buttonHeight, Component.literal("Through Walls"),
                        (button, value) -> clientCfg.renderThroughWalls = value);
        wallBtn.setTooltip(Tooltip.create(Component.literal(
                "If enabled, mining outlines render through solid blocks (X-ray). If disabled, blocks hide obscured outlines."
        )));
        addRenderableWidget(wallBtn);

        // 2. Show Block Grid / Slices
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

        // 3. Lock Scroll to Size Change
        var lockScrollBtn = CycleButton.onOffBuilder(clientCfg.lockScrollToSize)
                .create(leftColX, startY + spacing, buttonWidth, buttonHeight, Component.literal("Scroll Changes Size"),
                        (button, value) -> clientCfg.lockScrollToSize = value);
        lockScrollBtn.setTooltip(Tooltip.create(Component.literal(
                "If enabled, scrolling always resizes shape without needing Shift. Arrow keys switch modes."
        )));
        addRenderableWidget(lockScrollBtn);

        // 4. Require Active Key for Cycling
        var keyCycleBtn = CycleButton.onOffBuilder(clientCfg.requireActiveKeyForCycling)
                .create(leftColX, startY + spacing * 2, buttonWidth, buttonHeight, Component.literal("Hold Key to Cycle"),
                        (button, value) -> clientCfg.requireActiveKeyForCycling = value);
        keyCycleBtn.setTooltip(Tooltip.create(Component.literal(
                "Require holding down the activation key (` by default) to switch mining modes."
        )));
        addRenderableWidget(keyCycleBtn);

        // 6. Overlay Anchor Position
        
        
        var anchorBtn = CycleButton.<QMineClientConfig.OverlayAnchor>builder(anchor -> Component.literal(anchor.name()))
                .withValues(QMineClientConfig.OverlayAnchor.values())
                .withInitialValue(clientCfg.overlayAnchor)
                .create(rightColX, startY + spacing * 2, buttonWidth, buttonHeight, Component.literal("HUD Anchor"),
                        (button, value) -> clientCfg.overlayAnchor = value);
        anchorBtn.setTooltip(Tooltip.create(Component.literal("Screen corner/position for the QMine HUD overlay.")));
        addRenderableWidget(anchorBtn);

        // 7. Tree Harvester
        var treeBtn = CycleButton.onOffBuilder(ConfigManager.getServerConfig().treeHarvesterEnabled)
                .create(leftColX, startY + spacing * 3, buttonWidth, buttonHeight, Component.literal("Tree Harvester"),
                        (button, value) -> ConfigManager.getServerConfig().treeHarvesterEnabled = value);
        treeBtn.setTooltip(Tooltip.create(Component.literal(
                "When enabled, cutting a tree fells the whole tree and rapidly decays leaves without needing to hold the mining key."
        )));
        addRenderableWidget(treeBtn);

        // 8. Felling Animation Style
        
        
        var animBtn = CycleButton.<ovh.pdx.qmine.tree.TreeFellingAnimation>builder(anim -> Component.literal(anim.displayName))
                .withValues(ovh.pdx.qmine.tree.TreeFellingAnimation.values())
                .withInitialValue(ConfigManager.getServerConfig().treeFellingAnimation)
                .create(rightColX, startY + spacing * 3, buttonWidth, buttonHeight, Component.literal("Animation"),
                        (button, value) -> ConfigManager.getServerConfig().treeFellingAnimation = value);
        animBtn.setTooltip(Tooltip.create(Component.literal(
                "Visual animation style when a tree fells (None, Falling physics blocks, or Smoke puff particles)."
        )));
        addRenderableWidget(animBtn);

        // 9. Instant Leaf Decay
        var decayBtn = CycleButton.onOffBuilder(ConfigManager.getServerConfig().instantLeafDecay)
                .create(leftColX, startY + spacing * 4, buttonWidth, buttonHeight, Component.literal("Instant Leaf Decay"),
                        (button, value) -> ConfigManager.getServerConfig().instantLeafDecay = value);
        decayBtn.setTooltip(Tooltip.create(Component.literal(
                "When enabled, connected tree leaves decay instantaneously instead of decaying one-by-one."
        )));
        addRenderableWidget(decayBtn);

        // 10. Play Leaf Break Sounds
        var leafSoundBtn = CycleButton.onOffBuilder(ConfigManager.getServerConfig().leafDecaySounds)
                .create(rightColX, startY + spacing * 4, buttonWidth, buttonHeight, Component.literal("Leaf Decay Sounds"),
                        (button, value) -> ConfigManager.getServerConfig().leafDecaySounds = value);
        leafSoundBtn.setTooltip(Tooltip.create(Component.literal(
                "Play soft rustling sounds as tree leaves decay."
        )));
        addRenderableWidget(leafSoundBtn);

        // 11. Key Binds Configuration
        var keyBindsBtn = Button.builder(Component.literal("Key Binds..."), button -> {
            if (this.minecraft != null) {
                
                
                this.minecraft.setScreen(new KeyBindsScreen(this, this.minecraft.options));
            }
        }).bounds(leftColX, startY + spacing * 5, buttonWidth * 2 + 10, buttonHeight).build();
        keyBindsBtn.setTooltip(Tooltip.create(Component.literal(
                "Rebind the QMine activation key and shape switching keys in Minecraft's Controls menu."
        )));
        addRenderableWidget(keyBindsBtn);

        // Done button
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
            
            
            this.minecraft.setScreen(this.parent);
        }
    }

    
    
    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(graphics, mouseX, mouseY, partialTicks);
        
        super.render(graphics, mouseX, mouseY, partialTicks);
        graphics.drawCenteredString(this.font, this.title, this.width / 2, this.height / 6 - 25, 0x00E5FF);
    }
}
