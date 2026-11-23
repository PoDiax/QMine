package ovh.pdx.qmine.shape;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import ovh.pdx.qmine.config.ConfigManager;

import java.util.*;
import java.util.regex.Pattern;

public final class BlockMatchers {
    private BlockMatchers() {
    }

    public static final ShapeContext.Matcher MATCH_BY_BLOCK = BlockMatcher.wrap(
            (origin, target) -> origin.getBlock() == target.getBlock()
    );

    public static final ShapeContext.Matcher MATCH_BY_TAGS_SHAPELESS = BlockMatcher.wrap(
            (origin, target) -> matchTags(origin, target, ConfigManager.getServerConfig().mergeTagsShapeless)
    );

    public static final ShapeContext.Matcher MATCH_BY_TAGS_SHAPED = BlockMatcher.wrap(
            (origin, target) -> matchTags(origin, target, ConfigManager.getServerConfig().mergeTagsShaped)
    );

    public static final ShapeContext.Matcher MATCH_BY_CROP_TYPE = BlockMatcher.wrap(
            BlockMatchers::areCropsEquivalent
    );

    public static final ShapeContext.Matcher MATCH_TUNNEL_STONE = BlockMatcher.wrap(
            (origin, target) -> {
                if (origin.getBlock() == target.getBlock()) {
                    return true;
                }
                if (matchTags(origin, target, ConfigManager.getServerConfig().mergeTagsShaped)) {
                    return true;
                }
                return ovh.pdx.qmine.handler.BlockBreakHandler.isOre(target);
            }
    );

    public static ShapeContext.Matcher determineBestMatcher(Level level, BlockPos pos, BlockState originState, Shape shape) {
        if (isCrop(originState)) {
            return MATCH_BY_CROP_TYPE;
        }

        if (shape.isIndeterminate()) {
            if (matchesTagPatterns(originState, ConfigManager.getServerConfig().mergeTagsShapeless)) {
                return MATCH_BY_TAGS_SHAPELESS;
            } else {
                return MATCH_BY_BLOCK;
            }
        } else if (shape.isTunnel() && isBaseStone(originState)) {
            return MATCH_TUNNEL_STONE;
        } else {
            if (matchesTagPatterns(originState, ConfigManager.getServerConfig().mergeTagsShaped)) {
                return MATCH_BY_TAGS_SHAPED;
            } else {
                return MATCH_BY_BLOCK;
            }
        }
    }

    public static boolean isBaseStone(BlockState state) {
        return state.is(net.minecraft.tags.BlockTags.BASE_STONE_OVERWORLD)
                || state.is(net.minecraft.tags.BlockTags.BASE_STONE_NETHER);
    }

    public static boolean isCrop(BlockState state) {
        Block block = state.getBlock();
        return block instanceof VegetationBlock || block instanceof CocoaBlock;
    }

    public static boolean areCropsEquivalent(BlockState a, BlockState b) {
        if (!isCrop(a) || !isCrop(b)) {
            return false;
        }
        return getCropCategory(a.getBlock()) == getCropCategory(b.getBlock());
    }

    private static int getCropCategory(Block block) {
        if (block instanceof CropBlock) return 1;
        if (block instanceof SaplingBlock) return 2;
        if (block instanceof CocoaBlock) return 3;
        if (block instanceof SweetBerryBushBlock) return 4;
        return 0;
    }

    public static boolean matchesTagPatterns(BlockState state, List<String> tagPatterns) {
        if (state.isAir() || state.getBlock() instanceof LiquidBlock || state.getBlock() instanceof EntityBlock) {
            return false;
        }

        Set<TagKey<Block>> resolvedTags = new HashSet<>();
        for (String patternStr : tagPatterns) {
            if (patternStr == null || patternStr.isBlank() || patternStr.equals("*")) {
                continue;
            }
            Identifier id = Identifier.tryParse(patternStr);
            if (id != null) {
                resolvedTags.add(TagKey.create(Registries.BLOCK, id));
            } else {
                Pattern regex = globToRegex(patternStr);
                BuiltInRegistries.BLOCK.getTags().forEach(holder -> {
                    if (regex.matcher(holder.key().location().toString()).matches()) {
                        resolvedTags.add(holder.key());
                    }
                });
            }
        }

        for (TagKey<Block> tag : resolvedTags) {
            if (state.is(tag)) {
                return true;
            }
        }

        return false;
    }

    public static boolean matchTags(BlockState origin, BlockState target, List<String> tagPatterns) {
        if (origin.getBlock() == target.getBlock()) {
            return true;
        }

        if (target.isAir() || target.getBlock() instanceof LiquidBlock || origin.getBlock() instanceof EntityBlock || target.getBlock() instanceof EntityBlock) {
            return false;
        }

        Set<TagKey<Block>> resolvedTags = new HashSet<>();
        for (String patternStr : tagPatterns) {
            if (patternStr == null || patternStr.isBlank() || patternStr.equals("*")) {
                continue;
            }
            Identifier id = Identifier.tryParse(patternStr);
            if (id != null) {
                resolvedTags.add(TagKey.create(Registries.BLOCK, id));
            } else {
                Pattern regex = globToRegex(patternStr);
                BuiltInRegistries.BLOCK.getTags().forEach(holder -> {
                    if (regex.matcher(holder.key().location().toString()).matches()) {
                        resolvedTags.add(holder.key());
                    }
                });
            }
        }

        for (TagKey<Block> tag : resolvedTags) {
            if (origin.is(tag) && target.is(tag)) {
                return true;
            }
        }

        return false;
    }

    private static Pattern globToRegex(String glob) {
        StringBuilder sb = new StringBuilder("^");
        for (char c : glob.toCharArray()) {
            switch (c) {
                case '*' -> sb.append(".*");
                case '?' -> sb.append(".");
                case '.', '(', ')', '+', '|', '^', '$', '@', '%', '{', '}', '[', ']' -> sb.append('\\').append(c);
                default -> sb.append(c);
            }
        }
        sb.append("$");
        return Pattern.compile(sb.toString());
    }
}
