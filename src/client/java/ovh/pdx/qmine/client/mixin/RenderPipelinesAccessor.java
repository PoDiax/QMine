package ovh.pdx.qmine.client.mixin;

import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import net.minecraft.client.renderer.RenderPipelines;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(RenderPipelines.class)
public interface RenderPipelinesAccessor {
    @Accessor("LINES_SNIPPET")
    static RenderPipeline.Snippet getLinesSnippet() {
        throw new AssertionError();
    }
}
