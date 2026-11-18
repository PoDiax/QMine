package ovh.pdx.qmine.client.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ovh.pdx.qmine.client.QMineClientController;

@Mixin(MouseHandler.class)
public abstract class MouseHandlerMixin {
    @Inject(method = "onScroll", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;isSpectator()Z", ordinal = 0), cancellable = true)
    public void onMouseScrolled(long handle, double xOffset, double yOffset, CallbackInfo ci,
                                @Local(name = "scaledXOffset") double amountX,
                                @Local(name = "scaledYOffset") double amountY) {
        if (!ci.isCancelled()) {
            if (QMineClientController.getInstance().onMouseScrolled(amountX, amountY)) {
                ci.cancel();
            }
        }
    }
}
