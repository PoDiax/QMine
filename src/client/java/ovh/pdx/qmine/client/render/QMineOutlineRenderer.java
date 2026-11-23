package ovh.pdx.qmine.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.joml.Matrix4f;
import org.jspecify.annotations.Nullable;
import ovh.pdx.qmine.client.QMineClientController;
import ovh.pdx.qmine.config.ConfigManager;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public final class QMineOutlineRenderer {
    private static final QMineOutlineRenderer INSTANCE = new QMineOutlineRenderer();

    private final AtomicInteger updateCounter = new AtomicInteger();
    private final ExecutorService backgroundWorker = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "QMine-OutlineWorker");
        t.setDaemon(true);
        return t;
    });

    @Nullable
    private volatile BlockPos cachedOriginPos;
    private volatile List<CachedEdge> cachedEdges = List.of();

    private QMineOutlineRenderer() {
    }

    public static QMineOutlineRenderer getInstance() {
        return INSTANCE;
    }

    public void clear() {
        updateCounter.incrementAndGet();
        cachedOriginPos = null;
        cachedEdges = List.of();
    }

    public void updateBlocks(List<BlockPos> blocks) {
        if (blocks.isEmpty()) {
            clear();
            return;
        }

        final int version = updateCounter.incrementAndGet();
        final List<BlockPos> blockList = List.copyOf(blocks);

        backgroundWorker.execute(() -> {
            if (version != updateCounter.get()) {
                return;
            }

            int maxOutline = ConfigManager.getClientConfig().renderOutlineMaxBlocks;
            List<BlockPos> renderList = blockList.size() > maxOutline ? blockList.subList(0, maxOutline) : blockList;

            BlockPos origin = renderList.get(0);
            List<CachedEdge> edges = new ArrayList<>();

            if (ConfigManager.getClientConfig().showBlockGrid) {
                for (BlockPos pos : renderList) {
                    double relX = pos.getX() - origin.getX();
                    double relY = pos.getY() - origin.getY();
                    double relZ = pos.getZ() - origin.getZ();
                    VoxelShape singleBlock = Shapes.box(relX + 0.002, relY + 0.002, relZ + 0.002,
                            relX + 0.998, relY + 0.998, relZ + 0.998);
                    singleBlock.forAllEdges((x1, y1, z1, x2, y2, z2) ->
                            edges.add(CachedEdge.fromDoubles(x1, y1, z1, x2, y2, z2))
                    );
                }
            } else {
                List<VoxelShape> shapes = new ArrayList<>();
                for (AABB aabb : ShapeMerger.merge(renderList, origin)) {
                    shapes.add(Shapes.create(aabb.inflate(0.005D)));
                }

                VoxelShape combined = combineShapes(shapes, 0, shapes.size());
                combined.forAllEdges((x1, y1, z1, x2, y2, z2) ->
                        edges.add(CachedEdge.fromDoubles(x1, y1, z1, x2, y2, z2))
                );
            }

            if (version == updateCounter.get()) {
                this.cachedOriginPos = origin;
                this.cachedEdges = List.copyOf(edges);
            }
        });
    }

    private static VoxelShape combineShapes(List<VoxelShape> shapes, int start, int end) {
        int count = end - start;
        if (count <= 0) {
            return Shapes.empty();
        }
        if (count == 1) {
            return shapes.get(start);
        }
        if (count == 2) {
            return Shapes.or(shapes.get(start), shapes.get(start + 1));
        }
        int mid = start + count / 2;
        return Shapes.or(combineShapes(shapes, start, mid), combineShapes(shapes, mid, end));
    }

    public void renderInGame(LevelRenderContext context) {
        QMineClientController controller = QMineClientController.getInstance();
        final BlockPos originPos = this.cachedOriginPos;
        final List<CachedEdge> edges = this.cachedEdges;

        if (!controller.isKeyPressed() || originPos == null || edges.isEmpty() || !controller.canMine()) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        Camera camera = mc.getEntityRenderDispatcher().camera;
        if (camera == null) {
            return;
        }

        Vec3 camPos = camera.position();
        PoseStack poseStack = context.poseStack();
        SubmitNodeCollector collector = context.submitNodeCollector();

        poseStack.pushPose();
        poseStack.translate(
                originPos.getX() - camPos.x,
                originPos.getY() - camPos.y,
                originPos.getZ() - camPos.z
        );

        collector.submitCustomGeometry(poseStack, RenderTypes.LINES, (pose, solidBuffer) -> {
            Matrix4f matrix = pose.pose();
            for (CachedEdge edge : edges) {
                solidBuffer.addVertex(matrix, edge.x1(), edge.y1(), edge.z1())
                        .setColor(255, 255, 255, 255)
                        .setNormal(edge.normalX(), edge.normalY(), edge.normalZ())
                        .setLineWidth(2.0f);
                solidBuffer.addVertex(matrix, edge.x2(), edge.y2(), edge.z2())
                        .setColor(255, 255, 255, 255)
                        .setNormal(edge.normalX(), edge.normalY(), edge.normalZ())
                        .setLineWidth(2.0f);
            }
        });

        var clientCfg = ConfigManager.getClientConfig();
        if (clientCfg.renderThroughWalls && clientCfg.previewLineAlpha > 0) {
            int alpha = clientCfg.previewLineAlpha;
            collector.submitCustomGeometry(poseStack, QMineRenderTypes.LINES_XRAY_TRANSLUCENT, (pose, transBuffer) -> {
                Matrix4f matrix = pose.pose();
                for (CachedEdge edge : edges) {
                    transBuffer.addVertex(matrix, edge.x1(), edge.y1(), edge.z1())
                            .setColor(0, 229, 255, alpha)
                            .setNormal(edge.normalX(), edge.normalY(), edge.normalZ())
                            .setLineWidth(1.0f);
                    transBuffer.addVertex(matrix, edge.x2(), edge.y2(), edge.z2())
                            .setColor(0, 229, 255, alpha)
                            .setNormal(edge.normalX(), edge.normalY(), edge.normalZ())
                            .setLineWidth(1.0f);
                }
            });
        }

        poseStack.popPose();
    }
}
