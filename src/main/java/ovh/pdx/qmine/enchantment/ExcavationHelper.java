package ovh.pdx.qmine.enchantment;


import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import ovh.pdx.qmine.compat.IdCompat;
import ovh.pdx.qmine.tag.QMineTags;

import java.util.Locale;

public final class ExcavationHelper {
    
    
    public static final ResourceLocation EXCAVATING_ID = IdCompat.of("excavating");
    public static final ResourceLocation EXCAVATION_ID = IdCompat.of("excavation");

    private ExcavationHelper() {
    }

    
    public static boolean isExcavating120(Enchantment ench) {
        if (ench == null) return false;
        ResourceLocation id = net.minecraft.core.registries.BuiltInRegistries.ENCHANTMENT.getKey(ench);
        if (id != null && id.getNamespace().equals(QMineTags.MOD_ID) && id.getPath().contains("excavat")) {
            return true;
        }
        String desc = ench.getDescriptionId();
        return desc != null && desc.toLowerCase(Locale.ROOT).contains("excavat");
    }

    public static int getExcavatingLevel(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return 0;
        }

        int maxLevel = 0;

        
        var enchantments = net.minecraft.world.item.enchantment.EnchantmentHelper.getEnchantments(stack);
        if (enchantments != null && !enchantments.isEmpty()) {
            for (var entry : enchantments.entrySet()) {
                if (isExcavating120(entry.getKey())) {
                    maxLevel = Math.max(maxLevel, entry.getValue());
                }
            }
        }

        return maxLevel;
    }

    public static int getMaxBlocksForLevel(int level, int defaultMax) {
        return switch (level) {
            case 1 -> Math.min(16, defaultMax);
            case 2 -> Math.min(32, defaultMax);
            case 3 -> Math.min(64, defaultMax);
            case 4 -> Math.min(128, defaultMax);
            default -> defaultMax;
        };
    }
}
