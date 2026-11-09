package ovh.pdx.qmine.api.impl;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import ovh.pdx.qmine.Qmine;
import ovh.pdx.qmine.api.QMineApi;
import ovh.pdx.qmine.core.PlayerMiningData;
import ovh.pdx.qmine.handler.BlockBreakHandler;
import ovh.pdx.qmine.handler.RightClickDispatcher;
import ovh.pdx.qmine.shape.Shape;
import ovh.pdx.qmine.shape.ShapeRegistry;
import ovh.pdx.qmine.tag.QMineTags;

public final class QMineApiImpl implements QMineApi {
    public static final QMineApiImpl INSTANCE = new QMineApiImpl();

    private QMineApiImpl() {
    }

    @Override
    public ShapeRegistry getShapeRegistry() {
        return ShapeRegistry.getInstance();
    }

    @Override
    public RightClickDispatcher getRightClickDispatcher() {
        return RightClickDispatcher.getInstance();
    }

    @Override
    public PlayerMiningData getMiningData(Player player) {
        return Qmine.getInstance().getPlayerData(player);
    }

    @Override
    public boolean isVeinMining(Player player) {
        if (!(player instanceof net.minecraft.server.level.ServerPlayer sp)) {
            return false;
        }
        PlayerMiningData data = getMiningData(player);
        return data != null && data.isMiningActive(sp);
    }

    @Override
    public Shape getCurrentShape(Player player) {
        PlayerMiningData data = getMiningData(player);
        return data != null ? data.getCurrentShape() : ShapeRegistry.getInstance().getShape(0);
    }

    @Override
    public void setPlayerShape(Player player, String shapeId) {
        PlayerMiningData data = getMiningData(player);
        if (data != null) {
            data.setShape(shapeId);
        }
    }

    @Override
    public boolean isExcludedBlock(BlockState state) {
        return QMineTags.isExcludedBlock(state);
    }

    @Override
    public boolean isWhitelistedBlock(BlockState state) {
        return QMineTags.isWhitelistedBlock(state);
    }

    @Override
    public boolean isValidTool(Player player, BlockPos pos, BlockState state) {
        return BlockBreakHandler.isValidTool(player, pos, state);
    }
}
