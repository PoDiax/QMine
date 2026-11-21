package ovh.pdx.qmine.client;

import com.mojang.blaze3d.platform.InputConstants;




import net.minecraft.resources.ResourceLocation;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.jspecify.annotations.Nullable;
import ovh.pdx.qmine.client.render.QMineOutlineRenderer;
import ovh.pdx.qmine.config.ConfigManager;
import ovh.pdx.qmine.config.QMineClientConfig;
import ovh.pdx.qmine.core.CanMineResult;
import ovh.pdx.qmine.core.CooldownTracker;
import ovh.pdx.qmine.handler.BlockBreakHandler;
import ovh.pdx.qmine.net.*;
import ovh.pdx.qmine.shape.ShapeRegistry;
import ovh.pdx.qmine.tag.QMineTags;

import java.util.Collections;
import java.util.List;

public final class QMineClientController {
    
    
    
    
    public static final String KEY_CATEGORY = "key.categories.qmine";

    private static final QMineClientController INSTANCE = new QMineClientController();

    public final KeyMapping keyActivate = new KeyMapping(
            "key.qmine.activate",
            InputConstants.KEY_GRAVE,
            KEY_CATEGORY
    );

    public final KeyMapping keyNext = new KeyMapping(
            "key.qmine.next_shape",
            InputConstants.KEY_UP,
            KEY_CATEGORY
    );

    public final KeyMapping keyPrev = new KeyMapping(
            "key.qmine.prev_shape",
            InputConstants.KEY_DOWN,
            KEY_CATEGORY
    );

    private boolean keyPressed = false;
    private boolean canMine = false;
    private CanMineResult canMineStatus = CanMineResult.NO_BLOCK_TARGETED;
    private int shapeIndex = 0;
    private int shapeRadius = 1;
    private int blockCount = 0;
    private List<BlockPos> shapeBlocks = Collections.emptyList();
    private long lastKeyToggleTime = 0L;

    private QMineClientController() {
    }

    public static QMineClientController getInstance() {
        return INSTANCE;
    }

    public boolean isKeyPressed() {
        return keyPressed;
    }

    public Component getActivateKeyName() {
        return keyActivate.getTranslatedKeyMessage();
    }

    public boolean canMine() {
        return canMine;
    }

    public CanMineResult getCanMineStatus() {
        return canMineStatus;
    }

    public int getShapeIndex() {
        return shapeIndex;
    }

    public int getShapeRadius() {
        return shapeRadius;
    }

    public int getBlockCount() {
        return blockCount;
    }

    public List<BlockPos> getShapeBlocks() {
        return shapeBlocks;
    }

    public void initNetworking() {
        
    }

    public void sendKeyPressed(boolean pressed) {
        
        
        net.neoforged.neoforge.network.PacketDistributor.sendToServer(new KeyPressedPayload(pressed));
        
        
    }

    public void sendModeChanged(boolean next) {
        
        
        net.neoforged.neoforge.network.PacketDistributor.sendToServer(new ModeChangedPayload(next));
        
        
    }

    public void sendShapeResize(boolean increase) {
        
        
        net.neoforged.neoforge.network.PacketDistributor.sendToServer(new ShapeResizePayload(increase));
        
        
    }

    public void setShape(int shapeIndex, int shapeRadius, @Nullable List<BlockPos> blocks) {
        this.shapeIndex = shapeIndex;
        this.shapeRadius = shapeRadius;
        if (blocks != null) {
            this.shapeBlocks = blocks;
            this.blockCount = blocks.size();
            QMineOutlineRenderer.getInstance().updateBlocks(blocks);
        }
    }

    private static boolean isKeyDown(int key) {
        
        
        return InputConstants.isKeyDown(Minecraft.getInstance().getWindow().getWindow(), key);
    }

    public static boolean isShiftDown() {
        Minecraft mc = Minecraft.getInstance();
        return (mc.player != null && mc.player.isShiftKeyDown())
                || isKeyDown(InputConstants.KEY_LSHIFT)
                || isKeyDown(InputConstants.KEY_RSHIFT);
    }

    public static boolean isAltDown() {
        return isKeyDown(InputConstants.KEY_LALT) || isKeyDown(InputConstants.KEY_RALT);
    }

    public static boolean isSizeModifierDown() {
        return isShiftDown() || isAltDown();
    }

    public boolean isMenuSneaking() {
        return isShiftDown();
    }

    public boolean onMouseScrolled(double amountX, double amountY) {
        if (keyPressed && (amountY != 0 || amountX != 0)) {
            boolean increase = amountY > 0D || amountX > 0D;

            // Shift (or Alt) + ~ + Scroll: locked strictly to size change, NEVER menu
            if (isSizeModifierDown() || ConfigManager.getClientConfig().lockScrollToSize) {
                sendShapeResize(increase);
                return true;
            }

            // Normal un-shifted scroll: cycles shape menu
            sendModeChanged(!increase);
            return true;
        }
        return false;
    }

    public boolean onKeyPress() {
        long now = System.currentTimeMillis();
        if (now - lastKeyToggleTime >= 125L) {
            boolean sizeMod = isSizeModifierDown() || ConfigManager.getClientConfig().lockScrollToSize;
            if (keyNext.isDown()) {
                if (sizeMod) {
                    resizeShape(true);
                } else {
                    cycleShape(true);
                }
                return true;
            } else if (keyPrev.isDown()) {
                if (sizeMod) {
                    resizeShape(false);
                } else {
                    cycleShape(false);
                }
                return true;
            }
        }
        return false;
    }

    private void cycleShape(boolean next) {
        boolean requireKey = ConfigManager.getClientConfig().requireActiveKeyForCycling;
        if (!requireKey || keyPressed) {
            sendModeChanged(next);
            lastKeyToggleTime = System.currentTimeMillis();
        }
    }

    private void resizeShape(boolean increase) {
        boolean requireKey = ConfigManager.getClientConfig().requireActiveKeyForCycling;
        if (!requireKey || keyPressed) {
            sendShapeResize(increase);
            lastKeyToggleTime = System.currentTimeMillis();
        }
    }

    public void clientTick(Minecraft mc) {
        if (mc.player == null) {
            return;
        }

        if (ConfigManager.getClientConfig().activationMode == QMineClientConfig.ActivationMode.TOGGLE) {
            while (keyActivate.consumeClick()) {
                keyPressed = !keyPressed;
                sendKeyPressed(keyPressed);
                if (!keyPressed) {
                    QMineOutlineRenderer.getInstance().clear();
                    shapeBlocks = Collections.emptyList();
                    blockCount = 0;
                }
            }
        } else {
            boolean previouslyPressed = keyPressed;
            keyPressed = keyActivate.isDown();

            if (keyPressed != previouslyPressed) {
                sendKeyPressed(keyPressed);
                if (!keyPressed) {
                    QMineOutlineRenderer.getInstance().clear();
                    shapeBlocks = Collections.emptyList();
                    blockCount = 0;
                }
            }
        }

        if (mc.hitResult instanceof BlockHitResult bhr && bhr.getType() == HitResult.Type.BLOCK) {
            BlockPos pos = bhr.getBlockPos();
            BlockState state = mc.player.level().getBlockState(pos);
            canMineStatus = BlockBreakHandler.getInstance().canMine(mc.player, pos, state);
        } else {
            canMineStatus = CanMineResult.NO_BLOCK_TARGETED;
        }

        canMine = keyPressed && canMineStatus.isAllowed();
    }
}
