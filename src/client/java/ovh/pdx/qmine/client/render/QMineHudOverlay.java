package ovh.pdx.qmine.client.render;

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import ovh.pdx.qmine.client.QMineClientController;
import ovh.pdx.qmine.config.ConfigManager;
import ovh.pdx.qmine.config.QMineClientConfig;
import ovh.pdx.qmine.core.CanMineResult;
import ovh.pdx.qmine.core.CooldownTracker;
import ovh.pdx.qmine.shape.Shape;
import ovh.pdx.qmine.shape.ShapeRegistry;
import ovh.pdx.qmine.tag.QMineTags;

import java.util.ArrayList;
import java.util.List;

public final class QMineHudOverlay implements HudElement {
    public static final Identifier ID = Identifier.fromNamespaceAndPath(QMineTags.MOD_ID, "hud_overlay");
    private static final QMineHudOverlay INSTANCE = new QMineHudOverlay();

    private float alphaFade = 0.0f;

    private QMineHudOverlay() {
    }

    public static QMineHudOverlay getInstance() {
        return INSTANCE;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.gui.hud.isHidden()) {
            return;
        }

        QMineClientController controller = QMineClientController.getInstance();
        boolean active = controller.isKeyPressed();

        float partialTick = deltaTracker.getGameTimeDeltaPartialTick(false);
        if (active) {
            alphaFade = Math.min(1.0f, alphaFade + 0.15f * partialTick);
        } else {
            alphaFade = Math.max(0.0f, alphaFade - 0.15f * partialTick);
        }

        if (alphaFade <= 0.01f) {
            return;
        }

        Font font = mc.font;
        QMineClientConfig clientCfg = ConfigManager.getClientConfig();
        float scale = (float) clientCfg.overlayScale;

        // Collect information to display
        CanMineResult status = controller.getCanMineStatus();
        boolean canMine = controller.canMine();
        int blockCount = controller.getBlockCount();

        // Hazard detection (Lava)
        boolean lavaHazard = false;
        if (clientCfg.showLavaHazardWarning && canMine && blockCount > 0 && mc.level != null) {
            for (BlockPos p : controller.getShapeBlocks()) {
                for (net.minecraft.core.Direction dir : net.minecraft.core.Direction.values()) {
                    if (mc.level.getBlockState(p.relative(dir)).is(net.minecraft.world.level.block.Blocks.LAVA)) {
                        lavaHazard = true;
                        break;
                    }
                }
                if (lavaHazard) break;
            }
        }

        // Status text (clean text, no icons)
        String badgeText;
        int badgeColor;
        if (lavaHazard) {
            badgeText = "LAVA DETECTED!";
            badgeColor = 0xEF4444; // Bright Red
        } else if (CooldownTracker.isOnCooldown(mc.player)) {
            badgeText = "COOLDOWN";
            badgeColor = 0xFBBF24; // Amber
        } else if (canMine && blockCount > 0) {
            badgeText = "ACTIVE";
            badgeColor = clientCfg.hudTheme.primaryColor;
        } else {
            badgeText = Component.translatable(status.getTranslationKey()).getString();
            badgeColor = 0xF87171; // Rose
        }

        ShapeRegistry registry = ShapeRegistry.getInstance();
        int shapeIdx = controller.getShapeIndex();
        Shape currentShape = registry.getShape(shapeIdx);

        List<Component> lines = new ArrayList<>();
        lines.add(Component.literal("QMine - ").append(Component.literal(badgeText)));

        if (canMine && blockCount > 0) {
            String countStr = blockCount + " Blocks";
            if (blockCount > clientCfg.renderOutlineMaxBlocks) {
                countStr += " (" + clientCfg.renderOutlineMaxBlocks + " outlined)";
            }
            var srv = ConfigManager.getServerConfig();

            if (clientCfg.showCostPreview) {
                net.minecraft.world.item.ItemStack mainHand = mc.player.getMainHandItem();
                if (mainHand.isDamageableItem()) {
                    int remainingDurability = mainHand.getMaxDamage() - mainHand.getDamageValue();
                    countStr += " | ⛏ ~" + blockCount + " Dura";
                    if (remainingDurability - blockCount <= srv.preventToolBreakDurability) {
                        countStr += " (⚠ " + remainingDurability + " left)";
                    }
                }
            }

            if (srv.consumeFood && srv.exhaustionPerBlock > 0) {
                double hunger = (blockCount * srv.exhaustionPerBlock * 0.005D) / 4.0;
                countStr += String.format(java.util.Locale.ROOT, " | ~%.1f Sat", hunger);
            }
            if (srv.consumeExperience && srv.experiencePerBlock > 0) {
                int xp = (int) Math.ceil(blockCount * srv.experiencePerBlock);
                countStr += " | " + xp + " XP";
            }
            lines.add(Component.literal("  " + countStr));

            if (srv.requireEnchantment && !mc.player.isCreative()) {
                int ench = ovh.pdx.qmine.enchantment.ExcavationHelper.getExcavatingLevel(mc.player.getMainHandItem());
                if (ench <= 0) {
                    ench = ovh.pdx.qmine.enchantment.ExcavationHelper.getExcavatingLevel(mc.player.getOffhandItem());
                }
                if (ench <= 0) {
                    lines.add(Component.literal("  §c⚠ Requires Excavating enchantment!§r"));
                }
            }
        }

        boolean showCarousel = true;
        if (showCarousel) {
            lines.add(Component.empty());
            int context = clientCfg.shapeMenuContextLines;
            for (int i = -context; i < 0; i++) {
                lines.add(Component.literal("    " + registry.getShape(shapeIdx + i).getDisplayName().getString()));
            }
            lines.add(Component.literal("  > " + currentShape.getDisplayName(controller.getShapeRadius()).getString() + " <"));
            for (int i = 1; i <= context; i++) {
                lines.add(Component.literal("    " + registry.getShape(shapeIdx + i).getDisplayName().getString()));
            }
            lines.add(Component.empty());
            if (clientCfg.lockScrollToSize) {
                lines.add(Component.literal("  [Up/Down] shape | [Scroll] size"));
            } else {
                lines.add(Component.literal("  [Scroll]/[Up/Down] shape | [Shift+Scroll] size"));
            }
        }

        // Layout measurements
        int maxTextWidth = 120;
        for (Component c : lines) {
            int w = font.width(c);
            if (w > maxTextWidth) {
                maxTextWidth = w;
            }
        }

        int padding = 6;
        int cardWidth = maxTextWidth + padding * 2;
        int cardHeight = lines.size() * (font.lineHeight + 2) + padding * 2;

        // Position by anchor
        int screenW = graphics.guiWidth();
        int screenH = graphics.guiHeight();
        int insetX = clientCfg.overlayInsetX;
        int insetY = clientCfg.overlayInsetY;

        float posX = switch (clientCfg.overlayAnchor) {
            case TOP_LEFT -> insetX;
            case TOP_RIGHT -> screenW - (cardWidth * scale) - insetX;
            case BOTTOM_LEFT -> insetX;
            case BOTTOM_RIGHT -> screenW - (cardWidth * scale) - insetX;
            case TOP_CENTER -> (screenW - (cardWidth * scale)) / 2.0f;
        };

        float posY = switch (clientCfg.overlayAnchor) {
            case TOP_LEFT, TOP_RIGHT, TOP_CENTER -> insetY;
            case BOTTOM_LEFT, BOTTOM_RIGHT -> screenH - (cardHeight * scale) - insetY;
        };

        graphics.pose().pushMatrix();
        graphics.pose().translate(posX, posY);
        graphics.pose().scale(scale, scale);

        int bgAlpha = (int) (0xDD * alphaFade);
        int borderAlpha = (int) (0x66 * alphaFade);
        int borderRgb = lavaHazard ? 0xFF0000 : (canMine ? clientCfg.hudTheme.primaryColor : badgeColor);

        // Dark card background & subtle glowing border
        graphics.fill(0, 0, cardWidth, cardHeight, (bgAlpha << 24) | 0x0F172A);
        graphics.outline(0, 0, cardWidth, cardHeight, (borderAlpha << 24) | borderRgb);

        // Cooldown bar
        float cdFrac = CooldownTracker.getCooldownRemaining(mc.player);
        if (cdFrac < 1.0f) {
            int barWidth = (int) ((cardWidth - 4) * cdFrac);
            int barAlpha = (int) (0xCC * alphaFade);
            graphics.fill(2, cardHeight - 4, 2 + barWidth, cardHeight - 2, (barAlpha << 24) | 0x38BDF8);
        }

        // Draw text lines
        int textY = padding;
        int textAlpha = (int) (0xFF * alphaFade);

        for (int i = 0; i < lines.size(); i++) {
            Component line = lines.get(i);
            int color;
            if (i == 0) {
                color = (textAlpha << 24) | (lavaHazard ? 0xF87171 : 0xF8FAFC);
            } else if (line.getString().contains("> ")) {
                color = (textAlpha << 24) | clientCfg.hudTheme.primaryColor; // Themed active mode
            } else if (line.getString().contains("Blocks")) {
                color = (textAlpha << 24) | clientCfg.hudTheme.secondaryColor; // Themed block count
            } else {
                color = (textAlpha << 24) | 0x94A3B8; // Slate grey
            }

            graphics.text(font, line, padding, textY, color, true);
            textY += font.lineHeight + 2;
        }

        graphics.pose().popMatrix();
    }
}
