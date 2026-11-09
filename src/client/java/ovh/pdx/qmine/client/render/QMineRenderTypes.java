package ovh.pdx.qmine.client.render;

import com.mojang.renderpearl.api.pipeline.BlendFunction;
import com.mojang.renderpearl.api.pipeline.ColorTargetState;
import com.mojang.renderpearl.api.pipeline.CompareOp;
import com.mojang.renderpearl.api.pipeline.DepthStencilState;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import net.minecraft.client.renderer.rendertype.LayeringTransform;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import ovh.pdx.qmine.client.mixin.RenderPipelinesAccessor;
import ovh.pdx.qmine.client.mixin.RenderTypeAccessor;

public final class QMineRenderTypes {
    public static final RenderPipeline LINES_XRAY_PIPELINE = RenderPipeline.builder(RenderPipelinesAccessor.getLinesSnippet())
            .withLocation("qmine/pipeline/lines_xray")
            .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
            .withDepthStencilState(new DepthStencilState(CompareOp.ALWAYS_PASS, false))
            .build();

    public static final RenderType LINES_XRAY_TRANSLUCENT = RenderTypeAccessor.invokeCreate(
            "qmine_lines_xray",
            RenderSetup.builder(LINES_XRAY_PIPELINE)
                    .setLayeringTransform(LayeringTransform.VIEW_OFFSET_Z_LAYERING)
                    .createRenderSetup()
    );

    private QMineRenderTypes() {
    }
}
