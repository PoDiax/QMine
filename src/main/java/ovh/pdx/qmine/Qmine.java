package ovh.pdx.qmine;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.component.SwingAnimation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ovh.pdx.qmine.command.QMineCommands;
import ovh.pdx.qmine.config.ConfigManager;
import ovh.pdx.qmine.core.CooldownTracker;
import ovh.pdx.qmine.core.DropsAccumulator;
import ovh.pdx.qmine.core.PlayerMiningData;
import ovh.pdx.qmine.handler.BlockBreakHandler;
import ovh.pdx.qmine.handler.CropHarvestingHandler;
import ovh.pdx.qmine.handler.RightClickDispatcher;
import ovh.pdx.qmine.net.QMineNetworking;
import ovh.pdx.qmine.net.SendShapePayload;
import ovh.pdx.qmine.net.SyncCooldownPayload;
import ovh.pdx.qmine.shape.ShapeContext;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class Qmine implements ModInitializer {
    public static final String MOD_ID = "qmine";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static Qmine instance;

    private final Map<UUID, PlayerMiningData> playerDataMap = new ConcurrentHashMap<>();
    private final DropsAccumulator dropsAccumulator = new DropsAccumulator();

    public static Qmine getInstance() {
        return instance;
    }

    public DropsAccumulator getDropsAccumulator() {
        return dropsAccumulator;
    }

    public PlayerMiningData getPlayerData(Player player) {
        return playerDataMap.computeIfAbsent(player.getUUID(), PlayerMiningData::new);
    }

    @Override
    public void onInitialize() {
        instance = this;
        LOGGER.info("Initializing QMine...");

        ConfigManager.loadServerConfig();
        QMineNetworking.initCommon();

        // Sync cooldown on player join
        ServerPlayerEvents.JOIN.register(player -> {
            var cfg = ConfigManager.getServerConfig();
            long cooldownTicks = cfg.cooldownTicks;
            QMineNetworking.sendToPlayer(player, new SyncCooldownPayload(cooldownTicks, SyncCooldownPayload.CooldownType.COOLDOWN));

            if (cfg.treeHarvesterEnabled && cfg.warnConflictingTreeMods && ovh.pdx.qmine.compat.TreeModDetector.hasConflictingTreeMods()) {
                player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                        "§b[QMine]§r External tree mod detected: §e" + ovh.pdx.qmine.compat.TreeModDetector.getConflictSummary() + "§r. You can toggle QMine's Tree Harvester under Mod Menu / Settings."
                ));
            }

            if (cfg.vanillaClientSupport && cfg.vanillaWelcomeMessage && !QMineNetworking.hasClientMod(player)) {
                player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                        "§b[QMine]§r Server ore vein mining enabled! Sneak while mining ores to vein mine."
                ));
            }
        });

        // Block breaking hook
        PlayerBlockBreakEvents.BEFORE.register((level, player, blockPos, blockState, blockEntity) -> {
            if (!(player instanceof ServerPlayer sp)) {
                return true;
            }
            return !BlockBreakHandler.getInstance().handleBlockBreak(level, blockPos, blockState, sp);
        });

        // Right-click interaction hook
        UseBlockCallback.EVENT.register(this::handleRightClick);

        // Command registration
        CommandRegistrationCallback.EVENT.register(QMineCommands::register);

        // Placed log tracking & leaf decay lifecycle
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerLevelEvents.LOAD.register((server, level) -> ovh.pdx.qmine.handler.PlacedLogTracker.getInstance().load(level));
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerLevelEvents.UNLOAD.register((server, level) -> {
            ovh.pdx.qmine.handler.PlacedLogTracker.getInstance().save(level);
            ovh.pdx.qmine.tree.FastLeafDecayHandler.getInstance().clear(level);
        });

        // Tree animation & leaf decay server ticking
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents.END_SERVER_TICK.register(server -> {
            ovh.pdx.qmine.tree.FastLeafDecayHandler.getInstance().onServerTick(server);
            ovh.pdx.qmine.tree.FallingTrunkTracker.getInstance().onServerTick(server);
            ovh.pdx.qmine.tree.CascadeTreeTracker.getInstance().onServerTick(server);
            ovh.pdx.qmine.tree.ShiftDownTreeTracker.getInstance().onServerTick(server);
        });

        // Lifecycle cleanup
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            playerDataMap.clear();
            ovh.pdx.qmine.handler.PlacedLogTracker.getInstance().clear();
            ovh.pdx.qmine.tree.FastLeafDecayHandler.getInstance().clearAll();
            ovh.pdx.qmine.tree.FallingTrunkTracker.getInstance().clearAll();
            ovh.pdx.qmine.tree.CascadeTreeTracker.getInstance().clearAll();
            ovh.pdx.qmine.tree.ShiftDownTreeTracker.getInstance().clearAll();
            ovh.pdx.qmine.tree.FallenTreeTracker.getInstance().clearAll();
        });
    }

    public InteractionResult handleRightClick(Player player, net.minecraft.world.level.Level level, InteractionHand hand, BlockHitResult hitResult) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.PASS;
        }

        BlockPos clickPos = hitResult.getBlockPos();
        BlockState state = level.getBlockState(clickPos);
        PlayerMiningData data = getPlayerData(player);

        // Single crop harvesting without key pressed
        if (!data.isMiningActive(serverPlayer)) {
            if (ConfigManager.getServerConfig().singleCropHarvesting && CropHarvestingHandler.isMatureCrop(state)) {
                if (CropHarvestingHandler.harvestSingleCrop(player, clickPos, state)) {
                    serverPlayer.swing(hand, SwingAnimation.DEFAULT, false);
                    return InteractionResult.SUCCESS_SERVER;
                }
            }
            return InteractionResult.PASS;
        }

        if (player.getFoodData().getFoodLevel() <= 0 && !player.isCreative()) {
            return InteractionResult.PASS;
        }

        data.clearCache();
        ShapeContext context = data.updateBlocks(serverPlayer, clickPos, hitResult.getDirection(), false, ConfigManager.getServerConfig().maxBlocks);
        if (context == null || !data.hasCachedPositions()) {
            return InteractionResult.PASS;
        }

        int affected = RightClickDispatcher.getInstance().dispatch(context, hand, data.getCachedPositions());
        if (affected > 0) {
            serverPlayer.swing(hand, SwingAnimation.DEFAULT, false);
            if (!player.isCreative()) {
                CooldownTracker.setLastMineTime(player, System.currentTimeMillis());
                if (ConfigManager.getServerConfig().consumeExperience) {
                    data.addPendingXPCost(Math.max(0, affected - 1));
                }
            }
            return InteractionResult.SUCCESS_SERVER;
        }

        return InteractionResult.PASS;
    }

    public void setKeyPressed(ServerPlayer player, boolean pressed) {
        PlayerMiningData data = getPlayerData(player);
        data.setPressed(pressed);
        data.clearCache();

        if (!data.isPressed()) {
            QMineNetworking.sendToPlayer(player, SendShapePayload.ofShapeOnly(data.getCurrentShapeIndex(), data.getCurrentShapeRadius()));
        }
    }

    public void cycleMode(ServerPlayer player, boolean next) {
        PlayerMiningData data = getPlayerData(player);
        data.cycleShape(next);
        data.clearCache();
        HitResult ray = PlayerMiningData.rayTrace(player);
        if (ray instanceof BlockHitResult bhr && ray.getType() == HitResult.Type.BLOCK) {
            data.updateBlocks(player, bhr.getBlockPos(), bhr.getDirection(), true, ConfigManager.getServerConfig().maxBlocks);
        } else {
            QMineNetworking.sendToPlayer(player, SendShapePayload.ofShapeOnly(data.getCurrentShapeIndex(), data.getCurrentShapeRadius()));
        }
    }

    public void resizeMode(ServerPlayer player, boolean increase) {
        PlayerMiningData data = getPlayerData(player);
        if (data.resizeCurrentShape(increase)) {
            data.clearCache();
            HitResult ray = PlayerMiningData.rayTrace(player);
            if (ray instanceof BlockHitResult bhr && ray.getType() == HitResult.Type.BLOCK) {
                data.updateBlocks(player, bhr.getBlockPos(), bhr.getDirection(), true, ConfigManager.getServerConfig().maxBlocks);
            } else {
                QMineNetworking.sendToPlayer(player, SendShapePayload.ofShapeOnly(data.getCurrentShapeIndex(), data.getCurrentShapeRadius()));
            }
        }
    }

    public void playerTick(Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            PlayerMiningData data = getPlayerData(player);
            data.checkBlocks(serverPlayer, true, () -> ConfigManager.getServerConfig().maxBlocks);
            data.takePendingXP(serverPlayer);
        }
    }
}
