package ovh.pdx.qmine.core;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.jspecify.annotations.Nullable;
import ovh.pdx.qmine.config.ConfigManager;
import ovh.pdx.qmine.config.QMineConfig;
import ovh.pdx.qmine.config.QMineConfig.VanillaActivation;
import ovh.pdx.qmine.handler.BlockBreakHandler;
import ovh.pdx.qmine.net.QMineNetworking;
import ovh.pdx.qmine.net.SendShapePayload;
import ovh.pdx.qmine.shape.BlockMatchers;
import ovh.pdx.qmine.shape.Shape;
import ovh.pdx.qmine.shape.ShapeContext;
import ovh.pdx.qmine.shape.ShapeRegistry;

import java.util.*;
import java.util.function.IntSupplier;

public class PlayerMiningData {
    private final UUID playerId;
    private boolean pressed = false;
    private int shapeIndex = 0;
    private final Map<Identifier, Integer> shapeRadii = new HashMap<>();
    private double pendingXPCost = 0.0;

    @Nullable
    private VanillaActivation playerActivationOverride = null;

    @Nullable
    private BlockPos cachedPos;
    @Nullable
    private Direction cachedDirection;
    @Nullable
    private List<BlockPos> cachedBlocks;

    public PlayerMiningData(UUID playerId) {
        this.playerId = playerId;
    }

    public void clearCache() {
        cachedPos = null;
        cachedDirection = null;
        cachedBlocks = null;
    }

    public UUID getPlayerId() {
        return playerId;
    }

    public boolean isPressed() {
        return pressed;
    }

    public void setPressed(boolean pressed) {
        this.pressed = pressed;
    }

    @Nullable
    public VanillaActivation getActivationOverride() {
        return playerActivationOverride;
    }

    public void setActivationOverride(@Nullable VanillaActivation override) {
        this.playerActivationOverride = override;
    }

    public VanillaActivation getEffectiveActivation() {
        if (playerActivationOverride != null) {
            return playerActivationOverride;
        }
        return ConfigManager.getServerConfig().vanillaActivation;
    }

    public static boolean isPlayerSneaking(ServerPlayer player) {
        if (player.isShiftKeyDown() || player.isCrouching()) {
            return true;
        }
        if (player.getPose() == Pose.CROUCHING) {
            return true;
        }
        Input input = player.getLastClientInput();
        return input != null && input.shift();
    }

    public boolean isMiningActive(ServerPlayer player) {
        if (this.pressed) {
            return true;
        }

        var config = ConfigManager.getServerConfig();
        if (config.vanillaClientSupport) {
            var mode = getEffectiveActivation();
            if (mode == VanillaActivation.ALWAYS) {
                return true;
            }
            if (mode == VanillaActivation.SNEAK && isPlayerSneaking(player)) {
                return true;
            }
        }

        return false;
    }

    public boolean setShape(String shapeName) {
        ShapeRegistry registry = ShapeRegistry.getInstance();
        List<Shape> shapes = registry.getShapes();
        for (int i = 0; i < shapes.size(); i++) {
            Shape s = shapes.get(i);
            if (s.getId().getPath().equalsIgnoreCase(shapeName) || s.getId().toString().equalsIgnoreCase(shapeName)) {
                this.shapeIndex = i;
                clearCache();
                return true;
            }
        }
        return false;
    }

    public boolean setRadius(int radius) {
        Shape shape = getCurrentShape();
        if (!shape.canResize()) {
            return false;
        }
        int clamped = Math.max(shape.minRadius(), Math.min(shape.maxRadius(), radius));
        setShapeRadius(shape, clamped);
        clearCache();
        return true;
    }

    public boolean hasCachedPositions() {
        return cachedBlocks != null && !cachedBlocks.isEmpty();
    }

    @Nullable
    public List<BlockPos> getCachedPositions() {
        return cachedBlocks;
    }

    public Shape getCurrentShape() {
        return ShapeRegistry.getInstance().getShape(shapeIndex);
    }

    public int getCurrentShapeIndex() {
        return shapeIndex;
    }

    public int getShapeRadius(Shape shape) {
        return shapeRadii.computeIfAbsent(shape.getId(), id -> shape.defaultRadius());
    }

    public int getCurrentShapeRadius() {
        return getShapeRadius(getCurrentShape());
    }

    public void setShapeRadius(Shape shape, int radius) {
        int clamped = Math.max(shape.minRadius(), Math.min(shape.maxRadius(), radius));
        shapeRadii.put(shape.getId(), clamped);
    }

    public boolean resizeCurrentShape(boolean increase) {
        Shape shape = getCurrentShape();
        if (!shape.canResize()) {
            return false;
        }
        int cur = getCurrentShapeRadius();
        int next = increase ? cur + 1 : cur - 1;
        int clamped = Math.max(shape.minRadius(), Math.min(shape.maxRadius(), next));
        if (clamped != cur) {
            shapeRadii.put(shape.getId(), clamped);
            return true;
        }
        return false;
    }

    public void cycleShape(boolean next) {
        shapeIndex = ShapeRegistry.getInstance().cycle(shapeIndex, next);
    }

    public static HitResult rayTrace(ServerPlayer player) {
        double distance = player.getAttributeValue(Attributes.BLOCK_INTERACTION_RANGE);
        return player.pick(player.isCreative() ? distance + 0.5D : distance, 1.0F, false);
    }

    public void addPendingXPCost(int blockCount) {
        var config = ConfigManager.getServerConfig();
        if (config.consumeExperience && config.experiencePerBlock > 0.0) {
            pendingXPCost += blockCount * config.experiencePerBlock;
        }
    }

    public void takePendingXP(ServerPlayer player) {
        if (pendingXPCost >= 1.0) {
            int toTake = (int) pendingXPCost;
            XPUtils.addPlayerXP(player, -toTake);
            pendingXPCost -= toTake;
        }
    }

    public void checkBlocks(ServerPlayer player, boolean sendUpdate, IntSupplier maxBlocks) {
        if (!isMiningActive(player) || !QMineNetworking.hasClientMod(player)) {
            return;
        }

        HitResult result = rayTrace(player);
        if (!(result instanceof BlockHitResult hitResult) || result.getType() != HitResult.Type.BLOCK) {
            if (cachedBlocks != null && !cachedBlocks.isEmpty()) {
                clearCache();
                if (sendUpdate) {
                    QMineNetworking.sendToPlayer(player, SendShapePayload.ofShapeAndBlocks(shapeIndex, getCurrentShapeRadius(), Collections.emptyList()));
                }
            }
            return;
        }

        boolean isServerOnlyMode = !QMineNetworking.hasClientMod(player) || !this.pressed;
        var config = ConfigManager.getServerConfig();
        if (config.vanillaOresOnly && isServerOnlyMode) {
            BlockState targetedState = player.level().getBlockState(hitResult.getBlockPos());
            if (!BlockBreakHandler.isOre(targetedState)) {
                if (cachedBlocks != null && !cachedBlocks.isEmpty()) {
                    clearCache();
                    if (sendUpdate) {
                        QMineNetworking.sendToPlayer(player, SendShapePayload.ofShapeAndBlocks(shapeIndex, getCurrentShapeRadius(), Collections.emptyList()));
                    }
                }
                return;
            }
        }

        if (cachedDirection != hitResult.getDirection() || cachedPos == null || !cachedPos.equals(hitResult.getBlockPos())) {
            int originalShapeIndex = shapeIndex;
            if (isServerOnlyMode && originalShapeIndex != 0) {
                setShape("shapeless");
            }
            updateBlocks(player, hitResult.getBlockPos(), hitResult.getDirection(), sendUpdate, maxBlocks.getAsInt());
            if (isServerOnlyMode && originalShapeIndex != 0) {
                setShape(ShapeRegistry.getInstance().getShape(originalShapeIndex).getId().getPath());
            }
        }
    }

    @Nullable
    public ShapeContext updateBlocks(ServerPlayer player, BlockPos pos, Direction dir, boolean sendUpdate, int maxBlocks) {
        cachedPos = pos.immutable();
        cachedDirection = dir;

        Shape shape = getCurrentShape();
        int radius = getCurrentShapeRadius();
        ShapeContext context = null;

        if (maxBlocks <= 0) {
            cachedBlocks = Collections.emptyList();
        } else {
            BlockState originState = player.level().getBlockState(cachedPos);
            ShapeContext.Matcher matcher = BlockMatchers.determineBestMatcher(player.level(), cachedPos, originState, shape);
            context = new ShapeContext(player, cachedPos, cachedDirection, originState, matcher, maxBlocks, radius);
            cachedBlocks = shape.getBlocks(context);

            var config = ConfigManager.getServerConfig();
            if (config.consumeExperience && config.experiencePerBlock > 0.0 && !player.isCreative()) {
                int maxAffordable = (int) (XPUtils.getPlayerXP(player) / config.experiencePerBlock);
                if (maxAffordable < cachedBlocks.size()) {
                    cachedBlocks = cachedBlocks.subList(0, Math.max(1, maxAffordable));
                }
            }
        }

        if (sendUpdate) {
            QMineNetworking.sendToPlayer(player, SendShapePayload.ofShapeAndBlocks(shapeIndex, radius, cachedBlocks));
        }

        return context;
    }
}
