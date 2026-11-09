package ovh.pdx.qmine.config;

public class QMineClientConfig {
    // Rendering & Outlines
    public int renderOutlineMaxBlocks = 256;
    public boolean renderThroughWalls = false;
    public boolean showBlockGrid = false;
    public int previewLineAlpha = 65;

    // Controls & Shape Switching
    public enum ActivationMode {
        HOLD,
        TOGGLE
    }
    public ActivationMode activationMode = ActivationMode.HOLD;
    public boolean requireActiveKeyForCycling = true;
    public boolean lockScrollToSize = false;
    public boolean requireSneakForMenu = false;
    public int shapeMenuContextLines = 2;

    // HUD Overlay Styling & Position
    public enum HudTheme {
        CYAN("Cyan Neon", 0x00E5FF, 0x00B4D8),
        EMERALD("Emerald", 0x34D399, 0x10B981),
        AMBER("Amber Gold", 0xFBBF24, 0xD97706),
        AMETHYST("Amethyst", 0xC084FC, 0x9333EA),
        ROSE("Rose Diamond", 0xFB7185, 0xE11D48);

        public final String displayName;
        public final int primaryColor;
        public final int secondaryColor;

        HudTheme(String displayName, int primaryColor, int secondaryColor) {
            this.displayName = displayName;
            this.primaryColor = primaryColor;
            this.secondaryColor = secondaryColor;
        }
    }
    public HudTheme hudTheme = HudTheme.CYAN;
    public boolean showLavaHazardWarning = true;
    public boolean showCostPreview = true;
    public OverlayAnchor overlayAnchor = OverlayAnchor.TOP_LEFT;
    public int overlayInsetX = 8;
    public int overlayInsetY = 8;
    public double overlayScale = 1.0;

    public enum OverlayAnchor {
        TOP_LEFT,
        TOP_RIGHT,
        BOTTOM_LEFT,
        BOTTOM_RIGHT,
        TOP_CENTER
    }
}
