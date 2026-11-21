package ovh.pdx.qmine.compat;

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
        net.neoforged.fml.ModList modList = net.neoforged.fml.ModList.get();
        if (modList != null) {
            for (Map.Entry<String, String> entry : KNOWN_TREE_MODS.entrySet()) {
                if (modList.isLoaded(entry.getKey())) {
                    found.add(entry.getValue() + " (" + entry.getKey() + ")");
                }
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
