package ovh.pdx.qmine.api;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import ovh.pdx.qmine.api.impl.QMineApiImpl;
import ovh.pdx.qmine.core.PlayerMiningData;
import ovh.pdx.qmine.handler.RightClickDispatcher;
import ovh.pdx.qmine.shape.Shape;
import ovh.pdx.qmine.shape.ShapeRegistry;

public interface QMineApi {
    static QMineApi getInstance() {
        return QMineApiImpl.INSTANCE;
    }

    ShapeRegistry getShapeRegistry();

    default void registerShape(Shape shape) {
        getShapeRegistry().register(shape);
    }

    default Shape getShape(String name) {
        return getShapeRegistry().getShape(name);
    }

    RightClickDispatcher getRightClickDispatcher();

    PlayerMiningData getMiningData(Player player);

    boolean isVeinMining(Player player);

    Shape getCurrentShape(Player player);

    void setPlayerShape(Player player, String shapeId);

    boolean isExcludedBlock(BlockState state);

    boolean isWhitelistedBlock(BlockState state);

    boolean isValidTool(Player player, BlockPos pos, BlockState state);
}
