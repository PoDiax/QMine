package ovh.pdx.qmine.mixin;

import net.minecraft.world.food.FoodData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(FoodData.class)
public interface FoodDataAccess {
    @Accessor("exhaustionLevel")
    float qmine$getExhaustionLevel();
}
