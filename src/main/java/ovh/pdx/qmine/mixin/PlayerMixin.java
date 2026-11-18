package ovh.pdx.qmine.mixin;

import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ovh.pdx.qmine.Qmine;

@Mixin(Player.class)
public abstract class PlayerMixin {
    @Inject(method = "tick", at = @At("HEAD"))
    private void onPlayerTick(CallbackInfo ci) {
        if ((Object) this instanceof Player player && Qmine.getInstance() != null) {
            Qmine.getInstance().playerTick(player);
        }
    }
}
