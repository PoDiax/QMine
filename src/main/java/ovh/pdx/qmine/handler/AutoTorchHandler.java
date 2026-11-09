package ovh.pdx.qmine.handler;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.WallTorchBlock;
import net.minecraft.world.level.block.state.BlockState;
import ovh.pdx.qmine.config.QMineConfig;
import ovh.pdx.qmine.shape.Shape;

import java.util.*;

public final class AutoTorchHandler {
    private static final AutoTorchHandler INSTANCE = new AutoTorchHandler();

    private AutoTorchHandler() {
    }

    public static AutoTorchHandler getInstance() {
        return INSTANCE;
    }

    public void handleAutoTorch(ServerLevel level, List<BlockPos> minedPositions, ServerPlayer player, Shape shape, Direction clickedFace, QMineConfig config) {
        if (!config.autoPlaceTorches || minedPositions.isEmpty()) {
            return;
        }

        boolean isTunnelOrRoom = shape.isTunnel() || shape.getId().getPath().equals("room");

        Direction forward = clickedFace.getOpposite();
        Direction right = forward.getAxis().isVertical()
                ? player.getDirection().getClockWise()
                : forward.getClockWise();

        Direction side = (config.autoTorchSide == QMineConfig.TorchPlacementSide.LEFT)
                ? right.getOpposite()
                : right;

        BlockPos originPos = minedPositions.get(0);
        int spacing = Math.max(1, config.autoTorchSpacing);
        List<BlockPos> placedInSession = new ArrayList<>();

        if (isTunnelOrRoom) {
            // Group mined positions by forward distance (depth) along the tunnel/room
            Map<Integer, List<BlockPos>> depthLayers = new TreeMap<>();
            for (BlockPos pos : minedPositions) {
                int depth = (pos.getX() - originPos.getX()) * forward.getStepX()
                        + (pos.getY() - originPos.getY()) * forward.getStepY()
                        + (pos.getZ() - originPos.getZ()) * forward.getStepZ();

                depthLayers.computeIfAbsent(depth, d -> new ArrayList<>()).add(pos);
            }

            for (Map.Entry<Integer, List<BlockPos>> entry : depthLayers.entrySet()) {
                int depth = entry.getKey();
                if (depth <= 0) {
                    continue;
                }

                // If light check is disabled, fall back to pure mechanical spacing
                if (!config.autoTorchCheckLight && (depth % spacing != 0)) {
                    continue;
                }

                List<BlockPos> layer = entry.getValue();
                boolean placed = placeInDepthLayer(level, layer, originPos, side, player, config, placedInSession);
                if (!placed && !hasTorches(player)) {
                    break; // Out of torches in inventory
                }
            }
        } else {
            // General mining (Floor excavation, Shapeless ore veins, Cuboid, etc.)
            List<BlockPos> floorSpots = findFloorCandidates(level, minedPositions);
            for (BlockPos p : floorSpots) {
                if (config.autoTorchCheckLight) {
                    if (isAreaLit(level, p, config.autoTorchRadius, config.autoTorchLightLevel, placedInSession)) {
                        continue;
                    }
                }

                Item torchItem = consumeTorch(player);
                if (torchItem == null) {
                    break;
                }
                boolean isSoul = (torchItem == Items.SOUL_TORCH);
                if (tryPlaceFloorTorch(level, p, isSoul)) {
                    placedInSession.add(p);
                } else {
                    if (!player.isCreative()) {
                        player.getInventory().add(new ItemStack(torchItem));
                    }
                }
            }
        }
    }

    private boolean placeInDepthLayer(ServerLevel level, List<BlockPos> layer, BlockPos originPos, Direction side,
                                      ServerPlayer player, QMineConfig config, List<BlockPos> placedInSession) {
        // 1. Find floor candidates in this layer
        List<BlockPos> floorCandidates = findFloorCandidates(level, layer);
        BlockPos bestFloor = null;
        if (!floorCandidates.isEmpty()) {
            int maxLateral = Integer.MIN_VALUE;
            for (BlockPos p : floorCandidates) {
                int lateral = (p.getX() - originPos.getX()) * side.getStepX()
                        + (p.getZ() - originPos.getZ()) * side.getStepZ();
                if (lateral > maxLateral) {
                    maxLateral = lateral;
                    bestFloor = p;
                }
            }
        }

        // 2. Find wall candidates in this layer
        List<BlockPos> wallCandidates = findWallCandidates(level, layer, side);
        BlockPos bestWall = null;
        if (!wallCandidates.isEmpty()) {
            bestWall = wallCandidates.get(0);
        }

        // 3. Determine target pos for light checking based on chosen placement mode
        BlockPos targetPos;
        if (config.autoTorchSide == QMineConfig.TorchPlacementSide.FLOOR) {
            targetPos = (bestFloor != null) ? bestFloor : bestWall;
        } else {
            targetPos = (bestWall != null) ? bestWall : bestFloor;
        }

        if (targetPos == null) {
            return false;
        }

        // Check if area is already sufficiently lit
        if (config.autoTorchCheckLight) {
            if (isAreaLit(level, targetPos, config.autoTorchRadius, config.autoTorchLightLevel, placedInSession)) {
                return true; // Already lit!
            }
        }

        Item torchItem = consumeTorch(player);
        if (torchItem == null) {
            return false;
        }

        boolean isSoul = (torchItem == Items.SOUL_TORCH);
        boolean placed = false;
        BlockPos placedPos = null;

        if (config.autoTorchSide == QMineConfig.TorchPlacementSide.FLOOR) {
            // Floor placement first, fallback to wall if floor has gap
            if (bestFloor != null && tryPlaceFloorTorch(level, bestFloor, isSoul)) {
                placed = true;
                placedPos = bestFloor;
            } else if (bestWall != null && tryPlaceWallTorch(level, bestWall, side, isSoul)) {
                placed = true;
                placedPos = bestWall;
            }
        } else if (config.autoTorchSide == QMineConfig.TorchPlacementSide.AUTO) {
            // Smart: prefer wall to keep walkway open, fallback to floor
            if (bestWall != null && tryPlaceWallTorch(level, bestWall, side, isSoul)) {
                placed = true;
                placedPos = bestWall;
            } else if (bestFloor != null && tryPlaceFloorTorch(level, bestFloor, isSoul)) {
                placed = true;
                placedPos = bestFloor;
            }
        } else {
            // RIGHT or LEFT wall first, fallback to floor
            if (bestWall != null && tryPlaceWallTorch(level, bestWall, side, isSoul)) {
                placed = true;
                placedPos = bestWall;
            } else if (bestFloor != null && tryPlaceFloorTorch(level, bestFloor, isSoul)) {
                placed = true;
                placedPos = bestFloor;
            }
        }

        if (placed) {
            placedInSession.add(placedPos);
            return true;
        } else {
            if (!player.isCreative()) {
                player.getInventory().add(new ItemStack(torchItem));
            }
            return false;
        }
    }

    private List<BlockPos> findFloorCandidates(ServerLevel level, List<BlockPos> positions) {
        List<BlockPos> candidates = new ArrayList<>();
        for (BlockPos p : positions) {
            if (level.getBlockState(p).isAir() && level.getFluidState(p).isEmpty()) {
                BlockPos below = p.below();
                if (level.getBlockState(below).isFaceSturdy(level, below, Direction.UP)) {
                    candidates.add(p);
                }
            }
        }
        return candidates;
    }

    private List<BlockPos> findWallCandidates(ServerLevel level, List<BlockPos> positions, Direction side) {
        List<BlockPos> candidates = new ArrayList<>();
        for (BlockPos p : positions) {
            if (level.getBlockState(p).isAir() && level.getFluidState(p).isEmpty()) {
                BlockPos wall = p.relative(side);
                if (level.getBlockState(wall).isFaceSturdy(level, wall, side.getOpposite())) {
                    candidates.add(p);
                }
            }
        }
        // Prefer middle/eye level height
        candidates.sort((a, b) -> Integer.compare(b.getY(), a.getY()));
        return candidates;
    }

    private boolean tryPlaceFloorTorch(ServerLevel level, BlockPos pos, boolean isSoul) {
        if (!level.getBlockState(pos).isAir() || !level.getFluidState(pos).isEmpty()) {
            return false;
        }
        BlockPos below = pos.below();
        if (!level.getBlockState(below).isFaceSturdy(level, below, Direction.UP)) {
            return false;
        }
        BlockState torchState = (isSoul ? Blocks.SOUL_TORCH : Blocks.TORCH).defaultBlockState();
        if (!torchState.canSurvive(level, pos)) {
            return false;
        }
        level.setBlockAndUpdate(pos, torchState);
        level.playSound(null, pos, SoundEvents.WOOD_PLACE, SoundSource.BLOCKS, 1.0f, 1.0f);
        return true;
    }

    private boolean tryPlaceWallTorch(ServerLevel level, BlockPos pos, Direction side, boolean isSoul) {
        if (!level.getBlockState(pos).isAir() || !level.getFluidState(pos).isEmpty()) {
            return false;
        }
        BlockPos wall = pos.relative(side);
        if (!level.getBlockState(wall).isFaceSturdy(level, wall, side.getOpposite())) {
            return false;
        }
        BlockState torchState = (isSoul ? Blocks.SOUL_WALL_TORCH : Blocks.WALL_TORCH)
                .defaultBlockState()
                .setValue(WallTorchBlock.FACING, side.getOpposite());
        if (!torchState.canSurvive(level, pos)) {
            return false;
        }
        level.setBlockAndUpdate(pos, torchState);
        level.playSound(null, pos, SoundEvents.WOOD_PLACE, SoundSource.BLOCKS, 1.0f, 1.0f);
        return true;
    }

    public static boolean isAreaLit(ServerLevel level, BlockPos pos, int radius, int lightThreshold, List<BlockPos> placedInSession) {
        // 1. Check recently placed torches in this mining session within radius
        for (BlockPos placed : placedInSession) {
            if (placed.closerThan(pos, radius)) {
                return true;
            }
        }

        // 2. Check local brightness at target position
        int rawLight = level.getMaxLocalRawBrightness(pos);
        int blockLight = level.getBrightness(LightLayer.BLOCK, pos);
        if (rawLight > lightThreshold || blockLight > lightThreshold) {
            return true;
        }

        // 3. Scan a radius of blocks around pos for existing torches or light sources
        int r = Math.min(radius, 12);
        BlockPos.MutableBlockPos mpos = new BlockPos.MutableBlockPos();
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                if (dx * dx + dz * dz > r * r) {
                    continue;
                }
                for (int dy = -2; dy <= 3; dy++) {
                    mpos.set(pos.getX() + dx, pos.getY() + dy, pos.getZ() + dz);
                    BlockState state = level.getBlockState(mpos);
                    if (state.is(Blocks.TORCH) || state.is(Blocks.WALL_TORCH)
                            || state.is(Blocks.SOUL_TORCH) || state.is(Blocks.SOUL_WALL_TORCH)
                            || state.is(Blocks.LANTERN) || state.is(Blocks.SOUL_LANTERN)
                            || state.is(Blocks.LAVA) || state.getLightEmission() > lightThreshold) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    private boolean hasTorches(ServerPlayer player) {
        if (player.isCreative()) {
            return true;
        }
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.is(Items.TORCH) || stack.is(Items.SOUL_TORCH)) {
                return true;
            }
        }
        return false;
    }

    private Item consumeTorch(ServerPlayer player) {
        if (player.isCreative()) {
            return Items.TORCH;
        }
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.is(Items.TORCH) || stack.is(Items.SOUL_TORCH)) {
                Item item = stack.getItem();
                stack.shrink(1);
                return item;
            }
        }
        return null;
    }
}
