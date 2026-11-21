package ovh.pdx.qmine.compat;

import net.minecraftforge.fml.ModList;

import java.util.*;

public final class TreeModDetector {
    private static final Map<String, String> KNOWN_TREE_MODS = Map.of(
            "treechop", "HT's TreeChop",
            "fallingtree", "FallingTree",
            "lumberjack", "Lumberjack",
            "timber", "Timber",
            "choptree", "ChopTree",
            "fastleafdecay", "Fast Leaf Decay",
            "diggusmaximus", "Diggus Maximus",
            "veinmining", "Vein Mining",
            "ftbultimine", "FTB Ultimine"
    );

    private TreeModDetector() {
    }

    public static List<String> detectLoadedTreeMods() {
        List<String> found = new ArrayList<>();
        ModList modList = ModList.get();
        if (modList == null) {
            return found;
        }
        for (Map.Entry<String, String> entry : KNOWN_TREE_MODS.entrySet()) {
            if (modList.isLoaded(entry.getKey())) {
                var container = modList.getModContainerById(entry.getKey());
                String name = container.map(c -> c.getModInfo().getDisplayName()).orElse(entry.getValue());
                found.add(name + " (" + entry.getKey() + ")");
            }
        }
        return found;
    }

    public static boolean hasConflictingTreeMods() {
        return !detectLoadedTreeMods().isEmpty();
    }

    public static String getConflictSummary() {
        List<String> mods = detectLoadedTreeMods();
        return String.join(", ", mods);
    }
}
