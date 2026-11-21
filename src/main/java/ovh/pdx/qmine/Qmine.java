package ovh.pdx.qmine;


import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;

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


@net.neoforged.fml.common.Mod("qmine")
public class Qmine {
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

    
    public Qmine(net.neoforged.bus.api.IEventBus modEventBus) {
        instance = this;
        LOGGER.info("Initializing QMine (NeoForge)...");

        ConfigManager.loadServerConfig();
        modEventBus.addListener(QMineNetworking::registerPayloads);

        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(this::onServerStopping);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(this::onServerTick);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(this::onLevelLoad);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(this::onLevelUnload);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(this::onPlayerLoggedIn);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(this::onBlockBreak);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(this::onRightClickBlock);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(this::onRegisterCommands);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(this::onEntityJoinLevel);
    }

    private void onEntityJoinLevel(net.neoforged.neoforge.event.entity.EntityJoinLevelEvent event) {
        if (getDropsAccumulator().handleEntity(event.getEntity(), event.getLevel())) {
            event.setCanceled(true);
        }
    }

    private void onServerStopping(net.neoforged.neoforge.event.server.ServerStoppingEvent event) {
        cleanupServer();
    }

    private void onServerTick(net.neoforged.neoforge.event.tick.ServerTickEvent.Post event) {
        net.minecraft.server.MinecraftServer server = event.getServer();
        ovh.pdx.qmine.tree.FastLeafDecayHandler.getInstance().onServerTick(server);
        ovh.pdx.qmine.tree.FallingTrunkTracker.getInstance().onServerTick(server);
        ovh.pdx.qmine.tree.CascadeTreeTracker.getInstance().onServerTick(server);
        ovh.pdx.qmine.tree.ShiftDownTreeTracker.getInstance().onServerTick(server);
    }

    private void onLevelLoad(net.neoforged.neoforge.event.level.LevelEvent.Load event) {
        if (event.getLevel() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            ovh.pdx.qmine.handler.PlacedLogTracker.getInstance().load(serverLevel);
        }
    }

    private void onLevelUnload(net.neoforged.neoforge.event.level.LevelEvent.Unload event) {
        if (event.getLevel() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            ovh.pdx.qmine.handler.PlacedLogTracker.getInstance().save(serverLevel);
            ovh.pdx.qmine.tree.FastLeafDecayHandler.getInstance().clear(serverLevel);
        }
    }

    private void onPlayerLoggedIn(net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) {
            onPlayerJoin(sp);
        }
    }

    private void onBlockBreak(net.neoforged.neoforge.event.level.BlockEvent.BreakEvent event) {
        if (event.getPlayer() instanceof ServerPlayer sp && event.getLevel() instanceof net.minecraft.server.level.ServerLevel level) {
            boolean handled = BlockBreakHandler.getInstance().handleBlockBreak(level, event.getPos(), event.getState(), sp);
            if (handled) {
                event.setCanceled(true);
            }
        }
    }

    private void onRightClickBlock(net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickBlock event) {
        if (event.getEntity() instanceof ServerPlayer sp) {
            var result = handleRightClick(sp, event.getLevel(), event.getHand(), event.getHitVec());
            if (result.consumesAction()) {
                event.setCancellationResult(result);
                event.setCanceled(true);
            }
        }
    }

    private void onRegisterCommands(net.neoforged.neoforge.event.RegisterCommandsEvent event) {
        QMineCommands.register(event.getDispatcher(), event.getBuildContext(), event.getCommandSelection());
    }

    private void cleanupServer() {
        playerDataMap.clear();
        ovh.pdx.qmine.handler.PlacedLogTracker.getInstance().clear();
        ovh.pdx.qmine.tree.FastLeafDecayHandler.getInstance().clearAll();
        ovh.pdx.qmine.tree.FallingTrunkTracker.getInstance().clearAll();
        ovh.pdx.qmine.tree.CascadeTreeTracker.getInstance().clearAll();
        ovh.pdx.qmine.tree.ShiftDownTreeTracker.getInstance().clearAll();
        ovh.pdx.qmine.tree.FallenTreeTracker.getInstance().clearAll();
    }

    private void onPlayerJoin(ServerPlayer player) {
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
                    
                    serverPlayer.swing(hand, true);
                    return InteractionResult.SUCCESS;
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
            
            serverPlayer.swing(hand, true);
            if (!player.isCreative()) {
                CooldownTracker.setLastMineTime(player, System.currentTimeMillis());
                if (ConfigManager.getServerConfig().consumeExperience) {
                    data.addPendingXPCost(Math.max(0, affected - 1));
                }
            }
            
            return InteractionResult.SUCCESS;
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
