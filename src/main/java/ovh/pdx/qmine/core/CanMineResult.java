package ovh.pdx.qmine.core;

public enum CanMineResult {
    ALLOWED("qmine.status.allowed", true),
    NO_BLOCK_TARGETED("qmine.status.no_block_targeted", false),
    NOT_A_VALID_BLOCK("qmine.status.not_a_valid_block", false),
    TOO_HUNGRY("qmine.status.too_hungry", false),
    NOT_ENOUGH_XP("qmine.status.not_enough_xp", false),
    NO_VALID_TOOL("qmine.status.no_valid_tool", false),
    TOOL_NOT_ALLOWED("qmine.status.tool_not_allowed", false),
    ON_COOLDOWN("qmine.status.on_cooldown", false),
    NO_EXCAVATION_ENCHANTMENT("qmine.status.no_excavation_enchantment", false),
    OTHER_RESTRICTION("qmine.status.other_restriction", false);

    private final String translationKey;
    private final boolean allowed;

    CanMineResult(String translationKey, boolean allowed) {
        this.translationKey = translationKey;
        this.allowed = allowed;
    }

    public String getTranslationKey() {
        return translationKey;
    }

    public boolean isAllowed() {
        return allowed;
    }
}
