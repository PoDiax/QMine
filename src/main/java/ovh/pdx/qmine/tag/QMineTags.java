package ovh.pdx.qmine.tag;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public final class QMineTags {
    public static final String MOD_ID = "qmine";

    private QMineTags() {
    }

    public static final class Blocks {
        public static final TagKey<Block> BLOCK_WHITELIST = TagKey.create(
                Registries.BLOCK,
                Identifier.fromNamespaceAndPath(MOD_ID, "block_whitelist")
        );
        public static final TagKey<Block> EXCLUDED_BLOCKS = TagKey.create(
                Registries.BLOCK,
                Identifier.fromNamespaceAndPath(MOD_ID, "excluded_blocks")
        );
        public static final TagKey<Block> FARMLAND_TILLABLE = TagKey.create(
                Registries.BLOCK,
                Identifier.fromNamespaceAndPath(MOD_ID, "farmland_tillable")
        );
        public static final TagKey<Block> SHOVEL_FLATTENABLE = TagKey.create(
                Registries.BLOCK,
                Identifier.fromNamespaceAndPath(MOD_ID, "shovel_flattenable")
        );
        public static final TagKey<Block> SINGLE_CROP_HARVESTING_BLACKLIST = TagKey.create(
                Registries.BLOCK,
                Identifier.fromNamespaceAndPath(MOD_ID, "single_crop_harvesting_blacklist")
        );

        private Blocks() {
        }
    }

    public static final class Items {
        public static final TagKey<Item> INCLUDED_TOOLS = TagKey.create(
                Registries.ITEM,
                Identifier.fromNamespaceAndPath(MOD_ID, "included_tools")
        );
        public static final TagKey<Item> EXCLUDED_TOOLS = TagKey.create(
                Registries.ITEM,
                Identifier.fromNamespaceAndPath(MOD_ID, "excluded_tools")
        );
        public static final TagKey<Item> STRICT_EXCLUDED_TOOLS = TagKey.create(
                Registries.ITEM,
                Identifier.fromNamespaceAndPath(MOD_ID, "excluded_tools/strict")
        );

        private Items() {
        }
    }

    // ==================== HELPER METHODS ====================

    public static boolean isExcludedBlock(BlockState state) {
        return state.is(Blocks.EXCLUDED_BLOCKS);
    }

    public static boolean isWhitelistedBlock(BlockState state) {
        var whitelist = BuiltInRegistries.BLOCK.getTagOrEmpty(Blocks.BLOCK_WHITELIST);
        if (!whitelist.iterator().hasNext()) {
            return true; // No whitelist active
        }
        return state.is(Blocks.BLOCK_WHITELIST);
    }

    public static boolean isSingleCropBlacklisted(BlockState state) {
        return state.is(Blocks.SINGLE_CROP_HARVESTING_BLACKLIST);
    }

    public static boolean isFarmlandTillable(BlockState state) {
        if (state.is(Blocks.FARMLAND_TILLABLE)) {
            return true;
        }
        return state.is(net.minecraft.world.level.block.Blocks.GRASS_BLOCK)
                || state.is(net.minecraft.world.level.block.Blocks.DIRT)
                || state.is(net.minecraft.world.level.block.Blocks.DIRT_PATH)
                || state.is(net.minecraft.world.level.block.Blocks.COARSE_DIRT)
                || state.is(net.minecraft.world.level.block.Blocks.ROOTED_DIRT);
    }

    public static boolean isShovelFlattenable(BlockState state) {
        if (state.is(Blocks.SHOVEL_FLATTENABLE)) {
            return true;
        }
        return state.is(net.minecraft.world.level.block.Blocks.GRASS_BLOCK)
                || state.is(net.minecraft.world.level.block.Blocks.DIRT)
                || state.is(net.minecraft.world.level.block.Blocks.COARSE_DIRT)
                || state.is(net.minecraft.world.level.block.Blocks.ROOTED_DIRT)
                || state.is(net.minecraft.world.level.block.Blocks.MYCELIUM)
                || state.is(net.minecraft.world.level.block.Blocks.PODZOL);
    }

    public static boolean isIncludedTool(ItemStack stack) {
        return stack.is(Items.INCLUDED_TOOLS);
    }

    public static boolean isExcludedTool(ItemStack stack) {
        return stack.is(Items.EXCLUDED_TOOLS);
    }

    public static boolean isStrictExcludedTool(ItemStack stack) {
        return stack.is(Items.STRICT_EXCLUDED_TOOLS);
    }
}
