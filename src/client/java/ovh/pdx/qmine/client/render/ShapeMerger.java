package ovh.pdx.qmine.client.render;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

public final class ShapeMerger {
    private static final Long2ObjectMap<Direction> BY_NORMAL = Arrays.stream(Direction.values())
            .collect(Collectors.toMap(
                    dir -> BlockPos.asLong(dir.getStepX(), dir.getStepY(), dir.getStepZ()),
                    dir -> dir,
                    (dir, dir2) -> {
                        throw new IllegalArgumentException("Duplicate keys");
                    },
                    Long2ObjectOpenHashMap::new
            ));

    private double regionBuildingAxis = Double.NEGATIVE_INFINITY;
    private double faceBuildingAxis = Double.NEGATIVE_INFINITY;

    @Nullable
    private Vec3 lastCenterPoint = null;
    @Nullable
    private AABB currentBox;

    private final Map<Vec3, AABB> boxAssignments = Maps.newHashMap();
    private final Multimap<AABB, Vec3> mergerAssignments = HashMultimap.create();

    public double getRegionBuildingAxisValue() {
        return regionBuildingAxis;
    }

    public void setRegionBuildingAxisValue(double regionBuildingAxis) {
        this.regionBuildingAxis = regionBuildingAxis;
    }

    public double getFaceBuildingAxisValue() {
        return faceBuildingAxis;
    }

    public void setFaceBuildingAxisValue(double faceBuildingAxis) {
        this.faceBuildingAxis = faceBuildingAxis;
    }

    @Nullable
    public AABB getCurrentBox() {
        return currentBox;
    }

    public void setCurrentBox(@Nullable AABB currentBox, @Nullable Vec3 centerPoint) {
        this.currentBox = currentBox;
        if (currentBox != null && centerPoint != null) {
            boxAssignments.put(centerPoint, currentBox);
            mergerAssignments.put(currentBox, centerPoint);
        }
    }

    public Optional<AABB> getBoxFor(Vec3 target) {
        return Optional.ofNullable(boxAssignments.get(target));
    }

    public Optional<Vec3> getLastCenter() {
        return Optional.ofNullable(lastCenterPoint);
    }

    public void onNextEntry(Vec3 lastCenterPoint) {
        this.lastCenterPoint = lastCenterPoint;
    }

    public void expandCurrentBoxTowards(AABB target, Vec3 center) {
        AABB current = this.getCurrentBox();
        if (current == null) {
            throw new IllegalStateException("Can not expand current box, if current is not set.");
        }

        AABB expanded = current.minmax(target);
        Collection<Vec3> currentlyAssigned = mergerAssignments.removeAll(current);

        currentlyAssigned.forEach(v -> boxAssignments.put(v, expanded));
        mergerAssignments.putAll(expanded, currentlyAssigned);

        boxAssignments.put(center, expanded);
        mergerAssignments.put(expanded, center);

        this.currentBox = expanded;
    }

    public Collection<AABB> getBoxes() {
        return mergerAssignments.keySet();
    }

    public void expandBoxAt(Vec3 neighborCenter, AABB entryData, Vec3 centerPoint) {
        AABB current = boxAssignments.get(neighborCenter);
        if (current == null) {
            throw new IllegalStateException("Can not expand box at: " + neighborCenter + ", if current is not set.");
        }

        AABB expanded = current.minmax(entryData);
        Collection<Vec3> currentlyAssigned = mergerAssignments.removeAll(current);

        currentlyAssigned.forEach(v -> boxAssignments.put(v, expanded));
        mergerAssignments.putAll(expanded, currentlyAssigned);

        boxAssignments.put(centerPoint, expanded);
        mergerAssignments.put(expanded, centerPoint);
    }

    public boolean mergeNeighbors(Vec3 centerPoint, AABB aabb) {
        for (Direction offsetDirection : Direction.values()) {
            Vec3 neighborCenter = centerPoint.add(Vec3.atLowerCornerOf(offsetDirection.getUnitVec3i()));
            Optional<AABB> potentialNeighbor = getBoxFor(neighborCenter);

            if (potentialNeighbor.isPresent()) {
                AABB neighborBox = potentialNeighbor.get();
                if (areBoxesNeighbors(aabb, neighborBox, offsetDirection)) {
                    expandBoxAt(neighborCenter, aabb, centerPoint);
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean areBoxesNeighbors(AABB l, AABB r, Direction direction) {
        double endOfL = getDirectionalValue(l, direction);
        double startOfR = getDirectionalValue(r, direction.getOpposite());

        if (endOfL != startOfR) {
            return false;
        }

        for (Direction d : Direction.values()) {
            if (d.getAxis() != direction.getAxis() && getDirectionalValue(l, d) != getDirectionalValue(r, d)) {
                return false;
            }
        }
        return true;
    }

    public static double getDirectionalValue(AABB bb, Direction direction) {
        return direction.getAxisDirection() == Direction.AxisDirection.POSITIVE
                ? bb.max(direction.getAxis())
                : bb.min(direction.getAxis());
    }

    public static Collection<AABB> merge(Collection<BlockPos> positions, BlockPos origin) {
        ShapeMerger merger = new ShapeMerger();
        positions.stream()
                .map(pos -> pos.subtract(origin))
                .sorted()
                .map(AABB::new)
                .forEachOrdered(aabb -> {
                    if (merger.getRegionBuildingAxisValue() != aabb.minX) {
                        merger.setCurrentBox(null, null);
                    }
                    merger.setRegionBuildingAxisValue(aabb.minX);

                    if (merger.getFaceBuildingAxisValue() != aabb.minY) {
                        merger.setCurrentBox(null, null);
                    }
                    merger.setFaceBuildingAxisValue(aabb.minY);

                    Optional<Vec3> previousCenter = merger.getLastCenter();
                    Vec3 centerPoint = aabb.getCenter();
                    merger.onNextEntry(centerPoint);

                    Optional<Direction> moveNext = previousCenter.map(v -> {
                        Vec3 w = centerPoint.subtract(v);
                        BlockPos onPos = BlockPos.containing(w);
                        return dirFromNormal(onPos.getX(), onPos.getY(), onPos.getZ());
                    });

                    if (merger.getCurrentBox() != null) {
                        if (moveNext.map(dir -> areBoxesNeighbors(merger.getCurrentBox(), aabb, dir)).orElse(false)) {
                            merger.expandCurrentBoxTowards(aabb, centerPoint);
                            if (merger.mergeNeighbors(centerPoint, merger.getCurrentBox())) {
                                return;
                            }
                            return;
                        }
                    }

                    if (merger.mergeNeighbors(centerPoint, aabb)) {
                        return;
                    }

                    merger.setCurrentBox(aabb, centerPoint);
                });
        return merger.getBoxes();
    }

    private static Direction dirFromNormal(int x, int y, int z) {
        return BY_NORMAL.get(BlockPos.asLong(x, y, z));
    }
}
