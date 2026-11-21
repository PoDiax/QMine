package ovh.pdx.qmine.client.render;

import com.mojang.blaze3d.vertex.PoseStack;


import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import com.mojang.blaze3d.vertex.VertexConsumer;


import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
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

public final class QMineOutlineRenderer {
    private static final QMineOutlineRenderer INSTANCE = new QMineOutlineRenderer();

    @Nullable
    private volatile BlockPos cachedOriginPos;
    private volatile List<CachedEdge> cachedEdges = List.of();

    private QMineOutlineRenderer() {
    }

    public static QMineOutlineRenderer getInstance() {
        return INSTANCE;
    }

    public void clear() {
        cachedOriginPos = null;
        cachedEdges = List.of();
    }

    public void updateBlocks(List<BlockPos> blocks) {
        if (blocks.isEmpty()) {
            clear();
            return;
        }

        int maxOutline = ConfigManager.getClientConfig().renderOutlineMaxBlocks;
        List<BlockPos> renderList = blocks.size() > maxOutline ? blocks.subList(0, maxOutline) : blocks;

        BlockPos origin = renderList.get(0);
        List<CachedEdge> edges = new ArrayList<>();

        if (ConfigManager.getClientConfig().showBlockGrid) {
            // Individual block boxes to clearly show how many blocks deep/wide the shape is
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
            // Merged shape silhouette
            Collection<VoxelShape> shapes = new ArrayList<>();
            for (AABB aabb : ShapeMerger.merge(renderList, origin)) {
                shapes.add(Shapes.create(aabb.inflate(0.005D)));
            }

            VoxelShape combined = Shapes.empty();
            for (VoxelShape shape : shapes) {
                combined = Shapes.joinUnoptimized(combined, shape, BooleanOp.OR);
            }

            combined.optimize().forAllEdges((x1, y1, z1, x2, y2, z2) ->
                    edges.add(CachedEdge.fromDoubles(x1, y1, z1, x2, y2, z2))
            );
        }

        this.cachedOriginPos = origin;
        this.cachedEdges = List.copyOf(edges);
    }

    
    
    public void renderOutlines(PoseStack poseStack, Camera camera, MultiBufferSource consumers) {
        final BlockPos originPos = this.cachedOriginPos;
        final List<CachedEdge> edges = this.cachedEdges;
        if (originPos == null || edges.isEmpty()) {
            return;
        }

        Vec3 camPos = camera.getPosition();

        poseStack.pushPose();
        poseStack.translate(
                originPos.getX() - camPos.x,
                originPos.getY() - camPos.y,
                originPos.getZ() - camPos.z
        );

        Matrix4f matrix = poseStack.last().pose();
        org.joml.Matrix3f normalMatrix = poseStack.last().normal();

        // 1. Crisp white outer lines (foreground / depth-tested)
        VertexConsumer solidBuffer = consumers.getBuffer(RenderType.lines());
        for (CachedEdge edge : edges) {
            drawEdge(solidBuffer, matrix, normalMatrix, edge, 255, 255, 255, 255);
        }

        // 2. Translucent wireframe through blocks
        var clientCfg = ConfigManager.getClientConfig();
        if (clientCfg.renderThroughWalls && clientCfg.previewLineAlpha > 0) {
            int alpha = clientCfg.previewLineAlpha;
            VertexConsumer transBuffer = consumers.getBuffer(QMineRenderTypes.LINES_XRAY_TRANSLUCENT);
            for (CachedEdge edge : edges) {
                drawEdge(transBuffer, matrix, normalMatrix, edge, 0, 229, 255, alpha);
            }
        }

        poseStack.popPose();
    }

    public void renderInGame(WorldRenderContext context) {
        QMineClientController controller = QMineClientController.getInstance();
        if (!controller.isKeyPressed() || this.cachedOriginPos == null || this.cachedEdges.isEmpty() || !controller.canMine()) {
            return;
        }

        Camera camera = context.camera();
        PoseStack poseStack = context.matrixStack();
        MultiBufferSource consumers = context.consumers();
        if (camera == null || consumers == null || poseStack == null) {
            return;
        }

        renderOutlines(poseStack, camera, consumers);
    }
    

    private static void drawEdge(VertexConsumer buffer, Matrix4f matrix, org.joml.Matrix3f normalMatrix,
                                 CachedEdge edge, int r, int g, int b, int a) {
        buffer.addVertex(matrix, edge.x1(), edge.y1(), edge.z1())
                .setColor(r, g, b, a)
                .setNormal(edge.normalX(), edge.normalY(), edge.normalZ());
        buffer.addVertex(matrix, edge.x2(), edge.y2(), edge.z2())
                .setColor(r, g, b, a)
                .setNormal(edge.normalX(), edge.normalY(), edge.normalZ());
        
    }
}
