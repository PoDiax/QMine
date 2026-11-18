# QMine (FTB Ultimine Fabric Recreation) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Recreate the core functionality of FTB Ultimine as "QMine", a lightweight, pure Fabric mod for Minecraft 26.3 with no external library dependencies (no FTB Library), designed for easy maintenance across future Minecraft updates.

**Architecture:** A native Fabric mod utilizing Minecraft 26.3 Mojang mappings, Fabric Networking API v1 (`PayloadTypeRegistry`), Fabric Lifecycle & Interaction Events, Fabric Key Mapping API, and standard JSON configuration (Gson). The server tracks player mining state, calculates shapes (shapeless BFS vein mining, tunnels, stairs), batches block breaking with hunger/durability/XP costs, and intercepts item/XP drops with an accumulator to prevent lag. The client handles keybindings, scroll wheel shape cycling, HUD overlay rendering, and in-world VoxelShape merged wireframe outline rendering.

**Tech Stack:**
- Minecraft 26.3 (Mojang Mappings)
- Fabric Loader 0.20.1 & Fabric API 0.165.2+26.3
- Java 25
- MixinExtras & SpongePowered Mixin 0.8.7
- Google Gson (Minecraft bundled) for config

---

## Global Constraints

- Mod ID: `qmine`
- Java Package: `ovh.pdx.qmine`
- Minecraft version: `26.3`
- Gradle: Java 25 compatibility (`targetJavaVersion = 25`)
- Zero external library dependencies: completely independent of `ftb-library` or `architectury`. All networking, configs, and events use vanilla MC and Fabric API.
- Keep codebase clean, modern, and easy to maintain for future Minecraft releases.

---

## File Structure

```
src/
├── main/
│   ├── java/ovh/pdx/qmine/
│   │   ├── Qmine.java                                 # Main ModInitializer
│   │   ├── config/
│   │   │   ├── QMineConfig.java                      # Server/Gameplay configuration data
│   │   │   ├── QMineClientConfig.java                # Client visual/interaction configuration data
│   │   │   └── ConfigManager.java                    # Gson-based config loader & serializer
│   │   ├── net/
│   │   │   ├── QMineNetworking.java                  # Payload registration & networking entry point
│   │   │   ├── KeyPressedPayload.java                # C2S: key press / release
│   │   │   ├── ModeChangedPayload.java               # C2S: cycle shape next/prev
│   │   │   ├── SendShapePayload.java                 # S2C: sync shape index & block list
│   │   │   └── SyncCooldownPayload.java              # S2C: sync cooldown timestamp
│   │   ├── shape/
│   │   │   ├── Shape.java                            # Shape interface
│   │   │   ├── ShapeContext.java                     # Context passed to shapes (pos, face, matcher, max)
│   │   │   ├── ShapeRegistry.java                    # Shape registry (server & client)
│   │   │   ├── ShapelessShape.java                   # 3x3x3 BFS vein miner
│   │   │   ├── SmallTunnelShape.java                 # 1x1 straight tunnel
│   │   │   ├── LargeTunnelShape.java                 # 3x3 tunnel
│   │   │   ├── SmallSquareShape.java                 # 3x3 face/wall
│   │   │   ├── MiningTunnelShape.java                # Downward staircase tunnel
│   │   │   ├── EscapeTunnelShape.java                # Upward staircase tunnel
│   │   │   ├── BlockMatcher.java                     # Matcher filter wrapper with whitelist/tag check
│   │   │   └── BlockMatchers.java                    # Strategy selector (ore, crop, block)
│   │   ├── core/
│   │   │   ├── PlayerMiningData.java                 # Per-player server mining state & raytracing
│   │   │   ├── CooldownTracker.java                  # Server & client cooldown tracking
│   │   │   ├── CanMineResult.java                    # Enum for can-mine status & feedback reasons
│   │   │   ├── DropsAccumulator.java                 # Batches dropped ItemEntity & ExperienceOrb
│   │   │   ├── ItemCollector.java                    # Deduplicated item stack counter
│   │   │   └── XPUtils.java                          # Vanilla player experience calculation helper
│   │   ├── handler/
│   │   │   ├── BlockBreakHandler.java                # Multi-block breaking logic & validations
│   │   │   ├── RightClickDispatcher.java             # Right-click multi-action dispatcher
│   │   │   ├── RightClickHandler.java                # Right-click action interface
│   │   │   ├── AxeStrippingHandler.java              # Axe log strip & copper scrape/wax
│   │   │   ├── ShovelFlatteningHandler.java          # Shovel dirt path flattening
│   │   │   ├── FarmlandConversionHandler.java        # Hoe farmland tilling
│   │   │   └── CropHarvestingHandler.java            # Crop harvesting & auto-replant
│   │   ├── command/
│   │   │   └── QMineCommands.java                    # /qmine reload and status commands
│   │   ├── tag/
│   │   │   └── QMineTags.java                        # Mod block and item tags
│   │   └── mixin/
│   │       ├── CropBlockAccessor.java                # Accessor for crop age property
│   │       ├── FoodDataAccess.java                   # Accessor for hunger exhaustion level
│   │       ├── ServerPlayerAccessor.java             # Accessor for lastSentExp sync
│   │       ├── PlayerMixin.java                      # Player tick hook for raytrace updates
│   │       └── PersistentEntitySectionManagerMixin.java # Entity add hook for drop accumulator
│   └── resources/
│       ├── fabric.mod.json
│       ├── qmine.mixins.json
│       ├── assets/qmine/lang/en_us.json
│       └── data/qmine/tags/
│           ├── block/
│           │   ├── block_whitelist.json
│           │   ├── excluded_blocks.json
│           │   └── single_crop_harvesting_blacklist.json
│           └── item/
│               ├── included_tools.json
│               ├── excluded_tools.json
│               └── excluded_tools/strict.json
└── client/
    ├── java/ovh/pdx/qmine/client/
    │   ├── QmineClient.java                          # ClientModInitializer
    │   ├── QMineClientController.java                # Client state, tick, key listeners, cache
    │   ├── render/
    │   │   ├── QMineOutlineRenderer.java             # In-world block bounding box renderer
    │   │   ├── QMineHudOverlay.java                  # Screen HUD overlay with shape menu & status
    │   │   ├── ShapeMerger.java                      # AABB compressor/merger for fast outlines
    │   │   └── CachedEdge.java                       # Wireframe line segment cache
    │   └── mixin/
    │       ├── KeyboardHandlerMixin.java             # Intercepts keyboard shape cycling
    │       └── MouseHandlerMixin.java                # Intercepts mouse scroll shape cycling
    └── resources/
        └── qmine.client.mixins.json
```

---

## Tasks

### Task 1: Project Metadata, Configuration & Tags

**Files:**
- Create: `src/main/resources/assets/qmine/lang/en_us.json`
- Create: `src/main/resources/data/qmine/tags/block/block_whitelist.json`
- Create: `src/main/resources/data/qmine/tags/block/excluded_blocks.json`
- Create: `src/main/resources/data/qmine/tags/block/single_crop_harvesting_blacklist.json`
- Create: `src/main/resources/data/qmine/tags/item/included_tools.json`
- Create: `src/main/resources/data/qmine/tags/item/excluded_tools.json`
- Create: `src/main/resources/data/qmine/tags/item/excluded_tools/strict.json`
- Create: `src/main/java/ovh/pdx/qmine/tag/QMineTags.java`
- Create: `src/main/java/ovh/pdx/qmine/config/QMineConfig.java`
- Create: `src/main/java/ovh/pdx/qmine/config/QMineClientConfig.java`
- Create: `src/main/java/ovh/pdx/qmine/config/ConfigManager.java`
- Modify: `src/main/resources/fabric.mod.json`

**Interfaces:**
- Produces: `QMineTags.Blocks`, `QMineTags.Items`
- Produces: `ConfigManager.getServerConfig()`, `ConfigManager.getClientConfig()`, `ConfigManager.load()`, `ConfigManager.save()`

- [ ] **Step 1: Create tags JSON files in data/qmine/tags**
- [ ] **Step 2: Create QMineTags.java with TagKey definitions**
- [ ] **Step 3: Create QMineConfig.java and QMineClientConfig.java with complete options**
- [ ] **Step 4: Create ConfigManager.java using Gson for serialization/deserialization to `config/qmine.json`**
- [ ] **Step 5: Verify build with `./gradlew compileJava`**

---

### Task 2: Core Data Types & Networking Payloads

**Files:**
- Create: `src/main/java/ovh/pdx/qmine/core/CanMineResult.java`
- Create: `src/main/java/ovh/pdx/qmine/core/CooldownTracker.java`
- Create: `src/main/java/ovh/pdx/qmine/core/XPUtils.java`
- Create: `src/main/java/ovh/pdx/qmine/net/KeyPressedPayload.java`
- Create: `src/main/java/ovh/pdx/qmine/net/ModeChangedPayload.java`
- Create: `src/main/java/ovh/pdx/qmine/net/SendShapePayload.java`
- Create: `src/main/java/ovh/pdx/qmine/net/SyncCooldownPayload.java`
- Create: `src/main/java/ovh/pdx/qmine/net/QMineNetworking.java`

**Interfaces:**
- Consumes: `QMineConfig`
- Produces: `CanMineResult`, `CooldownTracker`, `XPUtils`
- Produces: `QMineNetworking.initCommon()`, `QMineNetworking.initClient()`

- [ ] **Step 1: Create CanMineResult enum with translation keys**
- [ ] **Step 2: Create XPUtils for calculating and modifying Minecraft player total experience**
- [ ] **Step 3: Create CooldownTracker for managing ultimine cooldown ticks**
- [ ] **Step 4: Create network payload records (`KeyPressedPayload`, `ModeChangedPayload`, `SendShapePayload`, `SyncCooldownPayload`) implementing `CustomPacketPayload` with `StreamCodec`**
- [ ] **Step 5: Create QMineNetworking registering payloads via `PayloadTypeRegistry`**
- [ ] **Step 6: Verify build with `./gradlew compileJava`**

---

### Task 3: Mining Shapes & Block Matching Strategy

**Files:**
- Create: `src/main/java/ovh/pdx/qmine/shape/Shape.java`
- Create: `src/main/java/ovh/pdx/qmine/shape/ShapeContext.java`
- Create: `src/main/java/ovh/pdx/qmine/shape/BlockMatcher.java`
- Create: `src/main/java/ovh/pdx/qmine/shape/BlockMatchers.java`
- Create: `src/main/java/ovh/pdx/qmine/shape/ShapelessShape.java`
- Create: `src/main/java/ovh/pdx/qmine/shape/SmallTunnelShape.java`
- Create: `src/main/java/ovh/pdx/qmine/shape/LargeTunnelShape.java`
- Create: `src/main/java/ovh/pdx/qmine/shape/SmallSquareShape.java`
- Create: `src/main/java/ovh/pdx/qmine/shape/DiagonalTunnelShape.java`
- Create: `src/main/java/ovh/pdx/qmine/shape/MiningTunnelShape.java`
- Create: `src/main/java/ovh/pdx/qmine/shape/EscapeTunnelShape.java`
- Create: `src/main/java/ovh/pdx/qmine/shape/ShapeRegistry.java`

**Interfaces:**
- Produces: `ShapeRegistry.getShape(int index)`, `ShapeRegistry.getShapes()`, `ShapeRegistry.init()`
- Produces: `BlockMatchers.determineBestMatcher(level, pos, state, shape)`

- [ ] **Step 1: Implement `Shape` interface and `ShapeContext` record**
- [ ] **Step 2: Implement `BlockMatcher` and `BlockMatchers` supporting ore tags (`#c:ores`, `#minecraft:base_stone_overworld`) and crop matching**
- [ ] **Step 3: Implement `ShapelessShape` with 26-neighbor BFS flood-fill and distance sorting**
- [ ] **Step 4: Implement tunnel shapes (`SmallTunnelShape`, `LargeTunnelShape`, `SmallSquareShape`, `MiningTunnelShape`, `EscapeTunnelShape`)**
- [ ] **Step 5: Implement `ShapeRegistry` registering all 6 built-in shapes**
- [ ] **Step 6: Verify build with `./gradlew compileJava`**

---

### Task 4: Server Mining Engine & Right-Click Dispatcher

**Files:**
- Create: `src/main/java/ovh/pdx/qmine/core/ItemCollector.java`
- Create: `src/main/java/ovh/pdx/qmine/core/DropsAccumulator.java`
- Create: `src/main/java/ovh/pdx/qmine/core/PlayerMiningData.java`
- Create: `src/main/java/ovh/pdx/qmine/mixin/FoodDataAccess.java`
- Create: `src/main/java/ovh/pdx/qmine/mixin/CropBlockAccessor.java`
- Create: `src/main/java/ovh/pdx/qmine/mixin/ServerPlayerAccessor.java`
- Create: `src/main/java/ovh/pdx/qmine/mixin/PlayerMixin.java`
- Create: `src/main/java/ovh/pdx/qmine/mixin/PersistentEntitySectionManagerMixin.java`
- Create: `src/main/java/ovh/pdx/qmine/handler/RightClickHandler.java`
- Create: `src/main/java/ovh/pdx/qmine/handler/RightClickDispatcher.java`
- Create: `src/main/java/ovh/pdx/qmine/handler/AxeStrippingHandler.java`
- Create: `src/main/java/ovh/pdx/qmine/handler/ShovelFlatteningHandler.java`
- Create: `src/main/java/ovh/pdx/qmine/handler/FarmlandConversionHandler.java`
- Create: `src/main/java/ovh/pdx/qmine/handler/CropHarvestingHandler.java`
- Create: `src/main/java/ovh/pdx/qmine/handler/BlockBreakHandler.java`
- Modify: `src/main/resources/qmine.mixins.json`

**Interfaces:**
- Produces: `BlockBreakHandler.handleBlockBreak(level, pos, state, player)`
- Produces: `RightClickDispatcher.dispatch(player, hand, hitResult)`
- Produces: `DropsAccumulator.start()`, `DropsAccumulator.finish(level, pos)`

- [ ] **Step 1: Implement `ItemCollector` (fastutil item grouping) and `DropsAccumulator`**
- [ ] **Step 2: Implement mixin accessors (`FoodDataAccess`, `CropBlockAccessor`, `ServerPlayerAccessor`) and entity/tick mixins (`PlayerMixin`, `PersistentEntitySectionManagerMixin`)**
- [ ] **Step 3: Implement `PlayerMiningData` tracking active shape, raytracing, and pending XP**
- [ ] **Step 4: Implement right click handlers (`AxeStrippingHandler`, `ShovelFlatteningHandler`, `FarmlandConversionHandler`, `CropHarvestingHandler`) and `RightClickDispatcher`**
- [ ] **Step 5: Implement `BlockBreakHandler` with tool checks, hunger, durability limits, hardness validation, and batch execution**
- [ ] **Step 6: Verify build with `./gradlew compileJava`**

---

### Task 5: Client HUD Overlay & In-World Outline Renderer

**Files:**
- Create: `src/client/java/ovh/pdx/qmine/client/render/CachedEdge.java`
- Create: `src/client/java/ovh/pdx/qmine/client/render/ShapeMerger.java`
- Create: `src/client/java/ovh/pdx/qmine/client/render/QMineOutlineRenderer.java`
- Create: `src/client/java/ovh/pdx/qmine/client/render/QMineHudOverlay.java`
- Create: `src/client/java/ovh/pdx/qmine/client/QMineClientController.java`
- Create: `src/client/java/ovh/pdx/qmine/client/mixin/KeyboardHandlerMixin.java`
- Create: `src/client/java/ovh/pdx/qmine/client/mixin/MouseHandlerMixin.java`
- Modify: `src/client/resources/qmine.client.mixins.json`

**Interfaces:**
- Produces: `QMineClientController.getInstance()`
- Produces: Key mappings registered with `KeyMappingHelper`
- Produces: HUD element attached with `HudElementRegistry`
- Produces: World outline attached with `LevelRenderEvents.AFTER_TRANSLUCENT_FEATURES`

- [ ] **Step 1: Implement `ShapeMerger` and `CachedEdge` for AABB merging**
- [ ] **Step 2: Implement `QMineOutlineRenderer` rendering solid outer lines and translucent occluded lines**
- [ ] **Step 3: Implement `QMineHudOverlay` displaying status, cooldown bar, block count, and scrollable shape carousel**
- [ ] **Step 4: Implement `KeyboardHandlerMixin` and `MouseHandlerMixin` for smooth shape switching**
- [ ] **Step 5: Implement `QMineClientController` wiring keys, networking receivers, and client tick updates**
- [ ] **Step 6: Verify build with `./gradlew compileClientJava`**

---

### Task 6: Commands, Registration & Verification

**Files:**
- Create: `src/main/java/ovh/pdx/qmine/command/QMineCommands.java`
- Modify: `src/main/java/ovh/pdx/qmine/Qmine.java`
- Modify: `src/client/java/ovh/pdx/qmine/client/QmineClient.java`
- Modify: `src/main/resources/assets/qmine/lang/en_us.json`

**Interfaces:**
- Entry points: `Qmine.onInitialize()`, `QmineClient.onInitializeClient()`

- [ ] **Step 1: Implement `/qmine reload` and `/qmine info` command**
- [ ] **Step 2: Wire all callbacks in `Qmine.onInitialize()` (networking, events, commands, server lifecycle)**
- [ ] **Step 3: Wire all callbacks in `QmineClient.onInitializeClient()` (keybindings, hud, renderer, networking)**
- [ ] **Step 4: Populate complete english localization in `en_us.json`**
- [ ] **Step 5: Run full project build `./gradlew build`**
- [ ] **Step 6: Run git status and commit complete implementation**

---

## Execution Handoff

Plan complete and saved to `docs/superpowers/plans/2026-11-06-qmine-fabric-recreation.md`.
