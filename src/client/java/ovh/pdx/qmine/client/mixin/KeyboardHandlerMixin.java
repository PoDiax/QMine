package ovh.pdx.qmine.client.mixin;

import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.Minecraft;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ovh.pdx.qmine.client.QMineClientController;

@Mixin(KeyboardHandler.class)
public abstract class KeyboardHandlerMixin {
    
    
    @Inject(method = "keyPress", at = @At("RETURN"), cancellable = true)
    public void onKeyPress(long handle, int key, int scancode, int action, int modifiers, CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        if (handle == mc.getWindow().getWindow()) {
            if (QMineClientController.getInstance().onKeyPress()) {
                ci.cancel();
            }
        }
    }
}
