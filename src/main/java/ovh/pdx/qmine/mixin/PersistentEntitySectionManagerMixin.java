package ovh.pdx.qmine.mixin;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.entity.EntityAccess;
import net.minecraft.world.level.entity.PersistentEntitySectionManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ovh.pdx.qmine.Qmine;

@Mixin(PersistentEntitySectionManager.class)
public abstract class PersistentEntitySectionManagerMixin<T extends EntityAccess> {
    @Inject(method = "addNewEntityWithoutEvent", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void qmine$addNewEntityWithoutEvent(T entityAccess, CallbackInfoReturnable<Boolean> cir) {
        if (entityAccess instanceof Entity entity && Qmine.getInstance() != null) {
            if (Qmine.getInstance().getDropsAccumulator().handleEntity(entity, entity.level())) {
                cir.setReturnValue(false);
            }
        }
    }
}
