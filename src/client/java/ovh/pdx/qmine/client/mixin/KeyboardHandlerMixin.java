package ovh.pdx.qmine.client.mixin;

import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.KeyEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ovh.pdx.qmine.client.QMineClientController;

@Mixin(KeyboardHandler.class)
public abstract class KeyboardHandlerMixin {
    @Inject(method = "keyPress", at = @At("RETURN"), cancellable = true)
    public void onKeyPress(long handle, int action, KeyEvent keyEvent, CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        if (handle == mc.getWindow().handle()) {
            if (QMineClientController.getInstance().onKeyPress()) {
                ci.cancel();
            }
        }
    }
}
