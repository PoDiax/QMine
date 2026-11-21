package ovh.pdx.qmine.client.mixin;


import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ovh.pdx.qmine.client.QMineClientController;

@Mixin(MouseHandler.class)
public abstract class MouseHandlerMixin {
    
    
    @Inject(method = "onScroll", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;isSpectator()Z", ordinal = 0), cancellable = true)
    public void onMouseScrolled(long handle, double xOffset, double yOffset, CallbackInfo ci) {
        if (!ci.isCancelled()) {
            if (QMineClientController.getInstance().onMouseScrolled(xOffset, yOffset)) {
                ci.cancel();
            }
        }
    }
}
