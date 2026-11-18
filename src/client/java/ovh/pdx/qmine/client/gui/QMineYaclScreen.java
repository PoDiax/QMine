package ovh.pdx.qmine.client.gui;

import dev.isxander.yacl3.api.*;
import dev.isxander.yacl3.api.controller.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.controls.KeyBindsScreen;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;
import ovh.pdx.qmine.client.QMineClientController;
import ovh.pdx.qmine.client.render.QMineOutlineRenderer;
import ovh.pdx.qmine.compat.TreeModDetector;
import ovh.pdx.qmine.config.ConfigManager;
import ovh.pdx.qmine.config.QMineClientConfig;
import ovh.pdx.qmine.config.QMineConfig;

public final class QMineYaclScreen {
    private QMineYaclScreen() {
    }

    public static Screen create(@Nullable Screen parent) {
        QMineClientConfig clientCfg = ConfigManager.getClientConfig();
        QMineConfig serverCfg = ConfigManager.getServerConfig();

        return YetAnotherConfigLib.createBuilder()
                .title(Component.literal("QMine Configuration"))
                .save(() -> {
                    ConfigManager.saveClientConfig();
                    ConfigManager.saveServerConfig();
                    QMineOutlineRenderer.getInstance().clear();
                })

                // ==================== 1. VISUALS & HUD ====================
                .category(ConfigCategory.createBuilder()
                        .name(Component.literal("Visuals & HUD"))
                        .tooltip(Component.literal("Customize world outlines, X-ray preview, and HUD overlay position"))
                        .group(OptionGroup.createBuilder()
                                .name(Component.literal("Outline Preview"))
                                .option(Option.<Boolean>createBuilder()
                                        .name(Component.literal("Through-Wall Outlines (X-Ray)"))
                                        .description(OptionDescription.of(Component.literal(
                                                "Renders glowing wireframes through solid blocks so you can see how deep a tunnel or vein extends."
                                        )))
                                        .binding(false, () -> clientCfg.renderThroughWalls, val -> clientCfg.renderThroughWalls = val)
                                        .controller(opt -> BooleanControllerBuilder.create(opt).coloured(true))
                                        .build()
                                )
                                .option(Option.<Boolean>createBuilder()
                                        .name(Component.literal("Show Block Grid Slices"))
                                        .description(OptionDescription.of(Component.literal(
                                                "Outlines each individual block unit so you can see exact depth steps and counts along the shape."
                                        )))
                                        .binding(false, () -> clientCfg.showBlockGrid, val -> {
                                            clientCfg.showBlockGrid = val;
                                            QMineOutlineRenderer.getInstance().clear();
                                        })
                                        .controller(opt -> BooleanControllerBuilder.create(opt).coloured(true))
                                        .build()
                                )
                                .option(Option.<Integer>createBuilder()
                                        .name(Component.literal("Through-Wall Opacity"))
                                        .description(OptionDescription.of(Component.literal(
                                                "Alpha transparency of through-wall wireframe lines (0 = hidden, 255 = fully opaque)."
                                        )))
                                        .binding(65, () -> clientCfg.previewLineAlpha, val -> clientCfg.previewLineAlpha = val)
                                        .controller(opt -> IntegerSliderControllerBuilder.create(opt).range(0, 255).step(5))
                                        .build()
                                )
                                .option(Option.<Integer>createBuilder()
                                        .name(Component.literal("Max Outline Blocks"))
                                        .description(OptionDescription.of(Component.literal(
                                                "Maximum number of blocks rendered in world wireframes at one time."
                                        )))
                                        .binding(256, () -> clientCfg.renderOutlineMaxBlocks, val -> clientCfg.renderOutlineMaxBlocks = val)
                                        .controller(opt -> IntegerSliderControllerBuilder.create(opt).range(16, 512).step(16))
                                        .build()
                                )
                                .build()
                        )
                        .group(OptionGroup.createBuilder()
                                .name(Component.literal("HUD Card & Theme"))
                                .option(Option.<QMineClientConfig.HudTheme>createBuilder()
                                        .name(Component.literal("HUD Color Theme"))
                                        .description(OptionDescription.of(Component.literal(
                                                "Color theme for HUD card, badges, active mode indicators, and outlines."
                                        )))
                                        .binding(QMineClientConfig.HudTheme.CYAN, () -> clientCfg.hudTheme, val -> clientCfg.hudTheme = val)
                                        .controller(opt -> EnumControllerBuilder.create(opt).enumClass(QMineClientConfig.HudTheme.class))
                                        .build()
                                )
                                .option(Option.<Boolean>createBuilder()
                                        .name(Component.literal("Lava Hazard Warning"))
                                        .description(OptionDescription.of(Component.literal(
                                                "Flashes a bright red warning on the HUD if any mined block touches hidden lava."
                                        )))
                                        .binding(true, () -> clientCfg.showLavaHazardWarning, val -> clientCfg.showLavaHazardWarning = val)
                                        .controller(opt -> BooleanControllerBuilder.create(opt).coloured(true))
                                        .build()
                                )
                                .option(Option.<Boolean>createBuilder()
                                        .name(Component.literal("Show Cost Preview"))
                                        .description(OptionDescription.of(Component.literal(
                                                "Displays estimated durability cost, food/saturation usage, and low-durability warnings on the HUD."
                                        )))
                                        .binding(true, () -> clientCfg.showCostPreview, val -> clientCfg.showCostPreview = val)
                                        .controller(opt -> BooleanControllerBuilder.create(opt).coloured(true))
                                        .build()
                                )
                                .option(Option.<QMineClientConfig.OverlayAnchor>createBuilder()
                                        .name(Component.literal("HUD Anchor Position"))
                                        .description(OptionDescription.of(Component.literal(
                                                "Screen corner/anchor for the QMine HUD status card."
                                        )))
                                        .binding(QMineClientConfig.OverlayAnchor.TOP_LEFT, () -> clientCfg.overlayAnchor, val -> clientCfg.overlayAnchor = val)
                                        .controller(opt -> EnumControllerBuilder.create(opt).enumClass(QMineClientConfig.OverlayAnchor.class))
                                        .build()
                                )
                                .option(Option.<Double>createBuilder()
                                        .name(Component.literal("HUD Scale"))
                                        .description(OptionDescription.of(Component.literal("Size scale multiplier for the HUD card.")))
                                        .binding(1.0, () -> clientCfg.overlayScale, val -> clientCfg.overlayScale = val)
                                        .controller(opt -> DoubleSliderControllerBuilder.create(opt).range(0.5, 2.0).step(0.1))
                                        .build()
                                )
                                .option(Option.<Integer>createBuilder()
                                        .name(Component.literal("Horizontal Inset"))
                                        .description(OptionDescription.of(Component.literal("Distance in pixels from the screen edge.")))
                                        .binding(8, () -> clientCfg.overlayInsetX, val -> clientCfg.overlayInsetX = val)
                                        .controller(opt -> IntegerSliderControllerBuilder.create(opt).range(0, 128).step(2))
                                        .build()
                                )
                                .option(Option.<Integer>createBuilder()
                                        .name(Component.literal("Vertical Inset"))
                                        .description(OptionDescription.of(Component.literal("Distance in pixels from the top/bottom edge.")))
                                        .binding(8, () -> clientCfg.overlayInsetY, val -> clientCfg.overlayInsetY = val)
                                        .controller(opt -> IntegerSliderControllerBuilder.create(opt).range(0, 128).step(2))
                                        .build()
                                )
                                .build()
                        )
                        .build()
                )

                // ==================== 2. ENERGY & COSTS ====================
                .category(ConfigCategory.createBuilder()
                        .name(Component.literal("Energy & Costs"))
                        .tooltip(Component.literal("Configure saturation exhaustion and XP costs (switch on/off or disable all costs)"))
                        .group(OptionGroup.createBuilder()
                                .name(Component.literal("Saturation / Food Exhaustion"))
                                .option(Option.<Boolean>createBuilder()
                                        .name(Component.literal("Consume Saturation"))
                                        .description(OptionDescription.of(Component.literal(
                                                "If enabled, vein mining causes saturation and hunger exhaustion. Turn off to mine without losing any saturation or hunger!"
                                        )))
                                        .binding(true, () -> serverCfg.consumeFood, val -> serverCfg.consumeFood = val)
                                        .controller(opt -> BooleanControllerBuilder.create(opt).coloured(true))
                                        .build()
                                )
                                .option(Option.<Double>createBuilder()
                                        .name(Component.literal("Exhaustion Per Block"))
                                        .description(OptionDescription.of(Component.literal(
                                                "Exhaustion points added per mined block (vanilla block break is 0.005, default is 20.0)."
                                        )))
                                        .binding(20.0, () -> serverCfg.exhaustionPerBlock, val -> serverCfg.exhaustionPerBlock = val)
                                        .controller(opt -> DoubleSliderControllerBuilder.create(opt).range(0.0, 50.0).step(1.0))
                                        .build()
                                )
                                .build()
                        )
                        .group(OptionGroup.createBuilder()
                                .name(Component.literal("Experience (XP) Cost"))
                                .option(Option.<Boolean>createBuilder()
                                        .name(Component.literal("Consume Experience"))
                                        .description(OptionDescription.of(Component.literal(
                                                "If enabled, vein mining costs XP points. Turn off to mine without consuming any XP!"
                                        )))
                                        .binding(false, () -> serverCfg.consumeExperience, val -> serverCfg.consumeExperience = val)
                                        .controller(opt -> BooleanControllerBuilder.create(opt).coloured(true))
                                        .build()
                                )
                                .option(Option.<Double>createBuilder()
                                        .name(Component.literal("XP Cost Per Block"))
                                        .description(OptionDescription.of(Component.literal(
                                                "Experience points required and consumed per block mined."
                                        )))
                                        .binding(1.0, () -> serverCfg.experiencePerBlock, val -> serverCfg.experiencePerBlock = val)
                                        .controller(opt -> DoubleSliderControllerBuilder.create(opt).range(0.0, 10.0).step(0.5))
                                        .build()
                                )
                                .build()
                        )
                        .build()
                )

                // ==================== 3. UPGRADES & PROTECTION ====================
                .category(ConfigCategory.createBuilder()
                        .name(Component.literal("Upgrades & Protection"))
                        .tooltip(Component.literal("Drop collection modes, tool break protection buffer, and mining limits"))
                        .group(OptionGroup.createBuilder()
                                .name(Component.literal("Drop Automation"))
                                .option(Option.<QMineConfig.DropCollectionMode>createBuilder()
                                        .name(Component.literal("Drop Collection Mode"))
                                        .description(OptionDescription.of(Component.literal(
                                                "INVENTORY: items go directly into your inventory! MAGNET: drops spawn at your feet for instant pickup. ORIGIN: classic block drops."
                                        )))
                                        .binding(QMineConfig.DropCollectionMode.MAGNET, () -> serverCfg.dropCollection, val -> serverCfg.dropCollection = val)
                                        .controller(opt -> EnumControllerBuilder.create(opt).enumClass(QMineConfig.DropCollectionMode.class))
                                        .build()
                                )
                                .build()
                        )
                        .group(OptionGroup.createBuilder()
                                .name(Component.literal("Tree Harvester (Autonomous Tree Felling)"))
                                .option(Option.<Boolean>createBuilder()
                                        .name(Component.literal("Enable Tree Harvester"))
                                        .description(OptionDescription.of(Component.literal(
                                                TreeModDetector.hasConflictingTreeMods()
                                                        ? "[Warning] Conflicting mod(s) found: [" + TreeModDetector.getConflictSummary() + "]. Fells only natural tree logs without touching leaves or player buildings. Disable this or remove the other mod if conflicts occur."
                                                        : "Fells only the logs of natural trees when cut with an axe, leaving all leaves untouched. Automatically protects player-placed logs and buildings!"
                                        )))
                                        .binding(true, () -> serverCfg.treeHarvesterEnabled, val -> serverCfg.treeHarvesterEnabled = val)
                                        .controller(opt -> BooleanControllerBuilder.create(opt).coloured(true))
                                        .build()
                                )
                                .option(Option.<ovh.pdx.qmine.tree.TreeFellingAnimation>createBuilder()
                                        .name(Component.literal("Felling Animation"))
                                        .description(OptionDescription.of(Component.literal(
                                                "INSTANT: all logs break simultaneously. FALLING BLOCKS: logs physically plunge down with gravity and shatter into items on impact. SHIFT DOWN: chopping the bottom log slides the entire tree down by 1 block for continuous chopping. CASCADE: logs break sequentially from top to bottom."
                                        )))
                                        .binding(ovh.pdx.qmine.tree.TreeFellingAnimation.INSTANT,
                                                () -> serverCfg.treeFellingAnimation,
                                                val -> serverCfg.treeFellingAnimation = val)
                                        .controller(opt -> EnumControllerBuilder.create(opt).enumClass(ovh.pdx.qmine.tree.TreeFellingAnimation.class))
                                        .build()
                                )
                                .option(Option.<Boolean>createBuilder()
                                        .name(Component.literal("Require Axe"))
                                        .description(OptionDescription.of(Component.literal(
                                                "Only triggers tree felling when breaking logs with an axe (prevents accidental chopping)."
                                        )))
                                        .binding(true, () -> serverCfg.treeHarvesterRequireAxe, val -> serverCfg.treeHarvesterRequireAxe = val)
                                        .controller(opt -> BooleanControllerBuilder.create(opt).coloured(true))
                                        .build()
                                )
                                .option(Option.<Boolean>createBuilder()
                                        .name(Component.literal("Sneak to Disable"))
                                        .description(OptionDescription.of(Component.literal(
                                                "Holding Sneak (Shift) breaks only a single log, allowing precise manual building."
                                        )))
                                        .binding(true, () -> serverCfg.treeHarvesterSneakToDisable, val -> serverCfg.treeHarvesterSneakToDisable = val)
                                        .controller(opt -> BooleanControllerBuilder.create(opt).coloured(true))
                                        .build()
                                )
                                .option(Option.<Boolean>createBuilder()
                                        .name(Component.literal("Auto-Replant Sapling"))
                                        .description(OptionDescription.of(Component.literal(
                                                "Automatically plants a new sapling at the base of the trunk on dirt/grass."
                                        )))
                                        .binding(true, () -> serverCfg.treeHarvesterAutoReplant, val -> serverCfg.treeHarvesterAutoReplant = val)
                                        .controller(opt -> BooleanControllerBuilder.create(opt).coloured(true))
                                        .build()
                                )
                                .option(Option.<Boolean>createBuilder()
                                        .name(Component.literal("Fast Leaf Decay"))
                                        .description(OptionDescription.of(Component.literal(
                                                "Naturally accelerates leaf decay on disconnected tree canopies after a tree is felled."
                                        )))
                                        .binding(true, () -> serverCfg.treeHarvesterFastLeafDecay, val -> serverCfg.treeHarvesterFastLeafDecay = val)
                                        .controller(opt -> BooleanControllerBuilder.create(opt).coloured(true))
                                        .build()
                                )
                                .option(Option.<Boolean>createBuilder()
                                        .name(Component.literal("Leaf Decay Particles"))
                                        .description(OptionDescription.of(Component.literal(
                                                "Emits leaf break particles when leaves rapidly decay. Turn off to reduce particle clutter."
                                        )))
                                        .binding(true, () -> serverCfg.leafDecayParticles, val -> serverCfg.leafDecayParticles = val)
                                        .controller(opt -> BooleanControllerBuilder.create(opt).coloured(true))
                                        .build()
                                )
                                .option(Option.<Boolean>createBuilder()
                                        .name(Component.literal("Leaf Decay Sounds"))
                                        .description(OptionDescription.of(Component.literal(
                                                "Plays leaf crunching sounds when leaves decay. Turn off for silent leaf decay."
                                        )))
                                        .binding(true, () -> serverCfg.leafDecaySounds, val -> serverCfg.leafDecaySounds = val)
                                        .controller(opt -> BooleanControllerBuilder.create(opt).coloured(true))
                                        .build()
                                )
                                .option(Option.<Boolean>createBuilder()
                                        .name(Component.literal("Tree Felling Sound & Particles"))
                                        .description(OptionDescription.of(Component.literal(
                                                "Plays wood break sound, chime, and green villager particles upon successfully felling a whole tree."
                                        )))
                                        .binding(true, () -> serverCfg.treeFellingEffects, val -> serverCfg.treeFellingEffects = val)
                                        .controller(opt -> BooleanControllerBuilder.create(opt).coloured(true))
                                        .build()
                                )
                                .option(Option.<Integer>createBuilder()
                                        .name(Component.literal("Max Tree Logs"))
                                        .description(OptionDescription.of(Component.literal("Maximum logs felled from a single tree.")))
                                        .binding(128, () -> serverCfg.treeHarvesterMaxLogs, val -> serverCfg.treeHarvesterMaxLogs = val)
                                        .controller(opt -> IntegerSliderControllerBuilder.create(opt).range(16, 256).step(16))
                                        .build()
                                )
                                .option(Option.<Boolean>createBuilder()
                                        .name(Component.literal("Warn on Conflicting Mods"))
                                        .description(OptionDescription.of(Component.literal(
                                                "Notifies you on world join if another tree felling mod (TreeChop, FallingTree, etc.) is detected."
                                        )))
                                        .binding(true, () -> serverCfg.warnConflictingTreeMods, val -> serverCfg.warnConflictingTreeMods = val)
                                        .controller(opt -> BooleanControllerBuilder.create(opt).coloured(true))
                                        .build()
                                )
                                .build()
                        )
                        .group(OptionGroup.createBuilder()
                                .name(Component.literal("Mining & Tunnel Automation"))
                                .option(Option.<Boolean>createBuilder()
                                        .name(Component.literal("Auto-Replant when Vein Mining"))
                                        .description(OptionDescription.of(Component.literal(
                                                "Automatically replants matching saplings when harvesting trees using ~ vein mining."
                                        )))
                                        .binding(true, () -> serverCfg.autoReplantTrees, val -> serverCfg.autoReplantTrees = val)
                                        .controller(opt -> BooleanControllerBuilder.create(opt).coloured(true))
                                        .build()
                                )
                                .option(Option.<Boolean>createBuilder()
                                        .name(Component.literal("Auto-Torch Placement"))
                                        .description(OptionDescription.of(Component.literal(
                                                "Automatically places torches along tunnels and excavated rooms. Supports wall mounting or floor fallback, respecting light levels."
                                        )))
                                        .binding(false, () -> serverCfg.autoPlaceTorches, val -> serverCfg.autoPlaceTorches = val)
                                        .controller(opt -> BooleanControllerBuilder.create(opt).coloured(true))
                                        .build()
                                )
                                .option(Option.<Boolean>createBuilder()
                                        .name(Component.literal("Place by Light Level"))
                                        .description(OptionDescription.of(Component.literal(
                                                "When enabled, torches are only placed if the area is dark (at or below max darkness light level) and no existing light sources exist within the detection radius."
                                        )))
                                        .binding(true, () -> serverCfg.autoTorchCheckLight, val -> serverCfg.autoTorchCheckLight = val)
                                        .controller(opt -> BooleanControllerBuilder.create(opt).coloured(true))
                                        .build()
                                )
                                .option(Option.<Integer>createBuilder()
                                        .name(Component.literal("Max Darkness Light Level"))
                                        .description(OptionDescription.of(Component.literal(
                                                "Torches are placed if the light level is at or below this value (default: 7)."
                                        )))
                                        .binding(7, () -> serverCfg.autoTorchLightLevel, val -> serverCfg.autoTorchLightLevel = val)
                                        .controller(opt -> IntegerSliderControllerBuilder.create(opt).range(0, 14).step(1))
                                        .build()
                                )
                                .option(Option.<Integer>createBuilder()
                                        .name(Component.literal("Light Detection Radius"))
                                        .description(OptionDescription.of(Component.literal(
                                                "Radius in blocks around each target position to scan for existing light sources, torches, or lanterns (default: 8)."
                                        )))
                                        .binding(8, () -> serverCfg.autoTorchRadius, val -> serverCfg.autoTorchRadius = val)
                                        .controller(opt -> IntegerSliderControllerBuilder.create(opt).range(3, 16).step(1))
                                        .build()
                                )
                                .option(Option.<Integer>createBuilder()
                                        .name(Component.literal("Auto-Torch Spacing"))
                                        .description(OptionDescription.of(Component.literal(
                                                "Fallback distance in blocks between torches along the tunnel when light level check is disabled (default: 8)."
                                        )))
                                        .binding(8, () -> serverCfg.autoTorchSpacing, val -> serverCfg.autoTorchSpacing = val)
                                        .controller(opt -> IntegerSliderControllerBuilder.create(opt).range(4, 16).step(2))
                                        .build()
                                )
                                .option(Option.<QMineConfig.TorchPlacementSide>createBuilder()
                                        .name(Component.literal("Torch Placement Location"))
                                        .description(OptionDescription.of(Component.literal(
                                                "FLOOR: Places torches directly on the floor. RIGHT: Mounts on right wall (floor fallback). LEFT: Mounts on left wall (floor fallback). AUTO: Smart placement on wall or floor."
                                        )))
                                        .binding(QMineConfig.TorchPlacementSide.FLOOR, () -> serverCfg.autoTorchSide, val -> serverCfg.autoTorchSide = val)
                                        .controller(opt -> EnumControllerBuilder.create(opt).enumClass(QMineConfig.TorchPlacementSide.class))
                                        .build()
                                )
                                .build()
                        )
                        .group(OptionGroup.createBuilder()
                                .name(Component.literal("Tool Protection & Limits"))
                                .option(Option.<Integer>createBuilder()
                                        .name(Component.literal("Tool Durability Buffer"))
                                        .description(OptionDescription.of(Component.literal(
                                                "Stops vein mining if tool durability reaches this amount, completely preventing tools from breaking! (0 = disabled, 1 = stop at 1 durability)."
                                        )))
                                        .binding(1, () -> serverCfg.preventToolBreakDurability, val -> serverCfg.preventToolBreakDurability = val)
                                        .controller(opt -> IntegerSliderControllerBuilder.create(opt).range(0, 20).step(1))
                                        .build()
                                )
                                .option(Option.<Integer>createBuilder()
                                        .name(Component.literal("Max Blocks Per Mine"))
                                        .description(OptionDescription.of(Component.literal("Maximum blocks vein mined in a single action.")))
                                        .binding(64, () -> serverCfg.maxBlocks, val -> serverCfg.maxBlocks = val)
                                        .controller(opt -> IntegerSliderControllerBuilder.create(opt).range(8, 256).step(8))
                                        .build()
                                )
                                .option(Option.<Long>createBuilder()
                                        .name(Component.literal("Cooldown Ticks"))
                                        .description(OptionDescription.of(Component.literal("Cooldown delay between vein mining uses (20 ticks = 1 second).")))
                                        .binding(0L, () -> serverCfg.cooldownTicks, val -> serverCfg.cooldownTicks = val)
                                        .controller(opt -> LongSliderControllerBuilder.create(opt).range(0L, 100L).step(5L))
                                        .build()
                                )
                                .option(Option.<Boolean>createBuilder()
                                        .name(Component.literal("Require Tool"))
                                        .description(OptionDescription.of(Component.literal("Whether vein mining requires any tool to be held in hand.")))
                                        .binding(false, () -> serverCfg.requireTool, val -> serverCfg.requireTool = val)
                                        .controller(opt -> BooleanControllerBuilder.create(opt).coloured(true))
                                        .build()
                                )
                                .option(Option.<Boolean>createBuilder()
                                        .name(Component.literal("Require Valid Tool for Block"))
                                        .description(OptionDescription.of(Component.literal("Whether the tool held must be effective for block drops (e.g. pickaxe for stone).")))
                                        .binding(false, () -> serverCfg.requireValidToolForBlock, val -> serverCfg.requireValidToolForBlock = val)
                                        .controller(opt -> BooleanControllerBuilder.create(opt).coloured(true))
                                        .build()
                                )
                                .option(Option.<Boolean>createBuilder()
                                        .name(Component.literal("Anti-Lava Shield"))
                                        .description(OptionDescription.of(Component.literal(
                                                "Protects you from opening lava pockets! Skips breaking any block directly adjacent to lava, walling off hazards."
                                        )))
                                        .binding(true, () -> serverCfg.antiLavaShield, val -> serverCfg.antiLavaShield = val)
                                        .controller(opt -> BooleanControllerBuilder.create(opt).coloured(true))
                                        .build()
                                )
                                .option(Option.<Boolean>createBuilder()
                                        .name(Component.literal("Auto-Replace Lava With Blocks"))
                                        .description(OptionDescription.of(Component.literal(
                                                "Automatically replaces adjacent lava blocks with cobblestone, stone, or debris from freshly mined drops or inventory, safely sealing hazards."
                                        )))
                                        .binding(true, () -> serverCfg.autoReplaceLava, val -> serverCfg.autoReplaceLava = val)
                                        .controller(opt -> BooleanControllerBuilder.create(opt).coloured(true))
                                        .build()
                                )
                                .option(Option.<Boolean>createBuilder()
                                        .name(Component.literal("Auto Tool-Swap"))
                                        .description(OptionDescription.of(Component.literal(
                                                "Automatically selects the best tool in your hotbar for each block (e.g. shovel for dirt, pickaxe for stone) and restores your hand afterwards."
                                        )))
                                        .binding(true, () -> serverCfg.autoToolSwap, val -> serverCfg.autoToolSwap = val)
                                        .controller(opt -> BooleanControllerBuilder.create(opt).coloured(true))
                                        .build()
                                )
                                .option(Option.<Boolean>createBuilder()
                                        .name(Component.literal("Cascading Audio Pitch"))
                                        .description(OptionDescription.of(Component.literal(
                                                "Modulates block break sound pitch in rising harmonic chords, eliminating deafening sound spam on large veins."
                                        )))
                                        .binding(true, () -> serverCfg.cascadingSoundPitch, val -> serverCfg.cascadingSoundPitch = val)
                                        .controller(opt -> BooleanControllerBuilder.create(opt).coloured(true))
                                        .build()
                                )
                                .option(Option.<Boolean>createBuilder()
                                        .name(Component.literal("Require Excavating Enchantment"))
                                        .description(OptionDescription.of(Component.literal(
                                                "Survival progression mode: requires tools to have the 'Excavating' enchantment to vein mine."
                                        )))
                                        .binding(false, () -> serverCfg.requireEnchantment, val -> serverCfg.requireEnchantment = val)
                                        .controller(opt -> BooleanControllerBuilder.create(opt).coloured(true))
                                        .build()
                                )
                                .build()
                        )
                        .build()
                )

                // ==================== 4. CONTROLS & INTERACTIONS ====================
                .category(ConfigCategory.createBuilder()
                        .name(Component.literal("Controls & Right-Click"))
                        .tooltip(Component.literal("Shape menu controls and right-click harvesting interactions"))
                        .group(OptionGroup.createBuilder()
                                .name(Component.literal("Activation & Cycling"))
                                .option(ButtonOption.createBuilder()
                                        .name(Component.literal("Configure Key Binds"))
                                        .text(Component.literal("Open Controls..."))
                                        .description(OptionDescription.of(Component.literal(
                                                "Opens Minecraft's Key Binds screen so you can rebind the QMine activation key (currently set to '" +
                                                QMineClientController.getInstance().getActivateKeyName().getString() + "') to any key or mouse button."
                                        )))
                                        .action(yaclScreen -> {
                                            Minecraft mc = Minecraft.getInstance();
                                            mc.gui.setScreen(new KeyBindsScreen(yaclScreen, mc.options));
                                        })
                                        .build()
                                )
                                .option(Option.<QMineClientConfig.ActivationMode>createBuilder()
                                        .name(Component.literal("Activation Mode"))
                                        .description(OptionDescription.of(Component.literal(
                                                "HOLD: hold the key to keep QMine active. TOGGLE: tap the key to turn ON/OFF (great for accessibility and long sessions!)."
                                        )))
                                        .binding(QMineClientConfig.ActivationMode.HOLD, () -> clientCfg.activationMode, val -> clientCfg.activationMode = val)
                                        .controller(opt -> EnumControllerBuilder.create(opt).enumClass(QMineClientConfig.ActivationMode.class))
                                        .build()
                                )
                                .option(Option.<Boolean>createBuilder()
                                        .name(Component.literal("Hold Key to Cycle"))
                                        .description(OptionDescription.of(Component.literal("Require holding activation key to switch shapes.")))
                                        .binding(true, () -> clientCfg.requireActiveKeyForCycling, val -> clientCfg.requireActiveKeyForCycling = val)
                                        .controller(opt -> BooleanControllerBuilder.create(opt).coloured(true))
                                        .build()
                                )
                                .option(Option.<Boolean>createBuilder()
                                        .name(Component.literal("Lock Scroll to Size Change"))
                                        .description(OptionDescription.of(Component.literal(
                                                "If enabled, scrolling while holding the activation key always changes the shape size without needing Shift. (Shape switching is done via arrow keys [Up/Down])."
                                        )))
                                        .binding(false, () -> clientCfg.lockScrollToSize, val -> clientCfg.lockScrollToSize = val)
                                        .controller(opt -> BooleanControllerBuilder.create(opt).coloured(true))
                                        .build()
                                )
                                .option(Option.<Boolean>createBuilder()
                                        .name(Component.literal("Sneak for Shape Menu"))
                                        .description(OptionDescription.of(Component.literal("Require holding Shift to expand shape selection carousel.")))
                                        .binding(false, () -> clientCfg.requireSneakForMenu, val -> clientCfg.requireSneakForMenu = val)
                                        .controller(opt -> BooleanControllerBuilder.create(opt).coloured(true))
                                        .build()
                                )
                                .build()
                        )
                        .group(OptionGroup.createBuilder()
                                .name(Component.literal("Vanilla / Server-Only Clients"))
                                .option(Option.<Boolean>createBuilder()
                                        .name(Component.literal("Vanilla Client Support"))
                                        .description(OptionDescription.of(Component.literal("Allow players without the QMine mod installed on their client to use vein mining.")))
                                        .binding(true, () -> serverCfg.vanillaClientSupport, val -> serverCfg.vanillaClientSupport = val)
                                        .controller(opt -> BooleanControllerBuilder.create(opt).coloured(true))
                                        .build()
                                )
                                .option(Option.<QMineConfig.VanillaActivation>createBuilder()
                                        .name(Component.literal("Vanilla Activation Mode"))
                                        .description(OptionDescription.of(Component.literal("Activation trigger for players without the client mod (SNEAK, ALWAYS, or DISABLED).")))
                                        .binding(QMineConfig.VanillaActivation.SNEAK, () -> serverCfg.vanillaActivation, val -> serverCfg.vanillaActivation = val)
                                        .controller(opt -> EnumControllerBuilder.create(opt).enumClass(QMineConfig.VanillaActivation.class))
                                        .build()
                                )
                                .option(Option.<Boolean>createBuilder()
                                        .name(Component.literal("Vanilla Ores Only"))
                                        .description(OptionDescription.of(Component.literal("Restrict server-side / vanilla vein mining strictly to ores (coal, iron, diamond, etc.) to protect bases and terrain.")))
                                        .binding(true, () -> serverCfg.vanillaOresOnly, val -> serverCfg.vanillaOresOnly = val)
                                        .controller(opt -> BooleanControllerBuilder.create(opt).coloured(true))
                                        .build()
                                )
                                .option(Option.<Boolean>createBuilder()
                                        .name(Component.literal("Join Welcome Tip"))
                                        .description(OptionDescription.of(Component.literal("Send a helpful one-time tip message in chat to players without the client mod when they join.")))
                                        .binding(true, () -> serverCfg.vanillaWelcomeMessage, val -> serverCfg.vanillaWelcomeMessage = val)
                                        .controller(opt -> BooleanControllerBuilder.create(opt).coloured(true))
                                        .build()
                                )
                                .build()
                        )
                        .group(OptionGroup.createBuilder()
                                .name(Component.literal("Right-Click Harvesting"))
                                .option(Option.<Boolean>createBuilder()
                                        .name(Component.literal("Axe Log Stripping"))
                                        .description(OptionDescription.of(Component.literal("Strip whole trees with right-click using an axe.")))
                                        .binding(true, () -> serverCfg.rightClickAxe, val -> serverCfg.rightClickAxe = val)
                                        .controller(opt -> BooleanControllerBuilder.create(opt).coloured(true))
                                        .build()
                                )
                                .option(Option.<Boolean>createBuilder()
                                        .name(Component.literal("Shovel Path Flattening"))
                                        .description(OptionDescription.of(Component.literal("Flatten wide grass paths with right-click shovel.")))
                                        .binding(true, () -> serverCfg.rightClickShovel, val -> serverCfg.rightClickShovel = val)
                                        .controller(opt -> BooleanControllerBuilder.create(opt).coloured(true))
                                        .build()
                                )
                                .option(Option.<Boolean>createBuilder()
                                        .name(Component.literal("Hoe Farmland Tilling"))
                                        .description(OptionDescription.of(Component.literal("Till wide farmland plots with right-click hoe.")))
                                        .binding(true, () -> serverCfg.rightClickHoe, val -> serverCfg.rightClickHoe = val)
                                        .controller(opt -> BooleanControllerBuilder.create(opt).coloured(true))
                                        .build()
                                )
                                .option(Option.<Boolean>createBuilder()
                                        .name(Component.literal("Crop Auto-Harvest & Replant"))
                                        .description(OptionDescription.of(Component.literal("Harvest and automatically replant mature crops in area.")))
                                        .binding(true, () -> serverCfg.rightClickHarvesting, val -> serverCfg.rightClickHarvesting = val)
                                        .controller(opt -> BooleanControllerBuilder.create(opt).coloured(true))
                                        .build()
                                )
                                .build()
                        )
                        .build()
                )
                .build()
                .generateScreen(parent);
    }
}
