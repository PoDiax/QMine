package ovh.pdx.qmine.enchantment;

import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import ovh.pdx.qmine.tag.QMineTags;

import java.util.Locale;

public final class ExcavationHelper {
    public static final Identifier EXCAVATING_ID = Identifier.fromNamespaceAndPath(QMineTags.MOD_ID, "excavating");
    public static final Identifier EXCAVATION_ID = Identifier.fromNamespaceAndPath(QMineTags.MOD_ID, "excavation");
    public static final ResourceKey<Enchantment> EXCAVATING_KEY = ResourceKey.create(Registries.ENCHANTMENT, EXCAVATING_ID);
    public static final ResourceKey<Enchantment> EXCAVATION_KEY = ResourceKey.create(Registries.ENCHANTMENT, EXCAVATION_ID);

    private ExcavationHelper() {
    }

    public static boolean isExcavating(Holder<Enchantment> holder) {
        if (holder == null) {
            return false;
        }

        if (holder.is(EXCAVATING_ID) || holder.is(EXCAVATION_ID) || holder.is(EXCAVATING_KEY) || holder.is(EXCAVATION_KEY)) {
            return true;
        }

        if (holder.unwrapKey().isPresent()) {
            Identifier id = holder.unwrapKey().get().identifier();
            if (id.getNamespace().equals(QMineTags.MOD_ID) && (id.getPath().equals("excavating") || id.getPath().equals("excavation") || id.getPath().contains("excavat"))) {
                return true;
            }
        }

        String regName = holder.getRegisteredName();
        if (regName != null && !regName.isEmpty() && !regName.equals("[unregistered]")) {
            if (regName.equals("qmine:excavating") || regName.equals("qmine:excavation") || regName.contains("excavat")) {
                return true;
            }
        }

        try {
            if (holder.isBound()) {
                Enchantment ench = holder.value();
                if (ench != null) {
                    Component desc = ench.description();
                    if (desc != null) {
                        if (desc.getContents() instanceof TranslatableContents trans) {
                            String key = trans.getKey();
                            if (key != null && key.toLowerCase(Locale.ROOT).contains("excavat")) {
                                return true;
                            }
                        }
                        String str = desc.getString();
                        if (str != null && str.toLowerCase(Locale.ROOT).contains("excavat")) {
                            return true;
                        }
                    }
                }
            }
        } catch (Throwable ignored) {
        }

        return false;
    }

    public static int getExcavatingLevel(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return 0;
        }

        int maxLevel = 0;

        // 1. Regular enchantments
        ItemEnchantments enchantments = stack.get(DataComponents.ENCHANTMENTS);
        if (enchantments != null && !enchantments.isEmpty()) {
            for (var entry : enchantments.entrySet()) {
                if (isExcavating(entry.getKey())) {
                    maxLevel = Math.max(maxLevel, entry.getIntValue());
                }
            }
        }

        // 2. Stored enchantments (enchanted books)
        ItemEnchantments stored = stack.get(DataComponents.STORED_ENCHANTMENTS);
        if (stored != null && !stored.isEmpty()) {
            for (var entry : stored.entrySet()) {
                if (isExcavating(entry.getKey())) {
                    maxLevel = Math.max(maxLevel, entry.getIntValue());
                }
            }
        }

        // 3. Fallback to stack.getEnchantments()
        if (maxLevel <= 0) {
            ItemEnchantments allEnch = stack.getEnchantments();
            if (allEnch != null && !allEnch.isEmpty()) {
                for (var entry : allEnch.entrySet()) {
                    if (isExcavating(entry.getKey())) {
                        maxLevel = Math.max(maxLevel, entry.getIntValue());
                    }
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
