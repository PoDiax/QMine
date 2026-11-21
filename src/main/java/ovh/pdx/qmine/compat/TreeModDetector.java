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
        
        for (Map.Entry<String, String> entry : KNOWN_TREE_MODS.entrySet()) {
            if (net.neoforged.fml.loading.FMLLoader.getLoadingModList().getModFileById(entry.getKey()) != null) {
                found.add(entry.getValue() + " (" + entry.getKey() + ")");
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
