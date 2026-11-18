package ovh.pdx.qmine.tree;

public enum TreeFellingAnimation {
    INSTANT("Instant", "All logs in the tree are harvested simultaneously."),
    FALL_PHYSICS("Falling Blocks", "Logs drop from the sky with gravity physics and burst into items on impact."),
    SHIFT_DOWN("Shift Down", "Chopping the bottom log slides the entire tree down by 1 block for continuous chopping."),
    CASCADE("Cascade", "Logs break sequentially from top to bottom in a rapid falling chain reaction.");

    public final String displayName;
    public final String description;

    TreeFellingAnimation(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
