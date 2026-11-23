package ovh.pdx.qmine.config;

import ovh.pdx.qmine.tree.TreeFellingAnimation;

import java.util.ArrayList;
import java.util.List;

public class QMineConfig {
    public boolean consumeFood = true;
    public double exhaustionPerBlock = 20.0;
    public boolean consumeExperience = false;
    public double experiencePerBlock = 1.0;

    public enum DropCollectionMode {
        ORIGIN,
        MAGNET,
        INVENTORY
    }
    public DropCollectionMode dropCollection = DropCollectionMode.MAGNET;

    public int maxBlocks = 64;
    public boolean requireTool = false;
    public boolean requireValidToolForBlock = true;
    public long cooldownTicks = 0L;
    public int preventToolBreakDurability = 1;
    public boolean cancelOnBlockBreakFail = false;

    public boolean antiLavaShield = true;
    public boolean autoReplaceLava = true;
    public boolean cascadingSoundPitch = true;
    public boolean autoToolSwap = true;
    public boolean requireEnchantment = false;

    public boolean treeHarvesterEnabled = true;
    public TreeFellingAnimation treeFellingAnimation = TreeFellingAnimation.INSTANT;
    public boolean treeHarvesterRequireAxe = true;
    public boolean treeHarvesterSneakToDisable = true;
    public int treeHarvesterMaxLogs = 128;
    public boolean treeHarvesterFastLeafDecay = true;
    public boolean leafDecayParticles = true;
    public boolean leafDecaySounds = true;
    public boolean treeFellingEffects = true;
    public boolean treeHarvesterAutoReplant = true;
    public boolean warnConflictingTreeMods = true;

    public boolean autoReplantTrees = true;
    public boolean instantLeafDecay = true;
    public boolean autoPlaceTorches = false;
    public boolean autoTorchCheckLight = true;
    public int autoTorchLightLevel = 7;
    public int autoTorchRadius = 8;
    public int autoTorchSpacing = 8;
    public enum TorchPlacementSide {
        FLOOR,
        RIGHT,
        LEFT,
        AUTO
    }
    public TorchPlacementSide autoTorchSide = TorchPlacementSide.FLOOR;

    public boolean rightClickAxe = true;
    public boolean rightClickShovel = true;
    public boolean rightClickHoe = true;
    public boolean rightClickHarvesting = true;
    public boolean singleCropHarvesting = true;

    public boolean vanillaClientSupport = true;
    public enum VanillaActivation {
        SNEAK,
        ALWAYS,
        DISABLED
    }
    public VanillaActivation vanillaActivation = VanillaActivation.SNEAK;
    public boolean vanillaOresOnly = true;
    public boolean vanillaWelcomeMessage = true;

    public List<String> mergeTagsShapeless = new ArrayList<>(List.of(
            "minecraft:base_stone_overworld",
            "minecraft:base_stone_nether",
            "minecraft:*_ores",
            "c:ores/*",
            "c:ores"
    ));

    public List<String> mergeTagsShaped = new ArrayList<>(List.of(
            "*"
    ));
}
