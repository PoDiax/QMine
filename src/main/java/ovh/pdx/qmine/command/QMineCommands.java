package ovh.pdx.qmine.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
import ovh.pdx.qmine.Qmine;
import ovh.pdx.qmine.config.ConfigManager;
import ovh.pdx.qmine.config.QMineConfig;
import ovh.pdx.qmine.core.PlayerMiningData;
import ovh.pdx.qmine.net.QMineNetworking;
import ovh.pdx.qmine.net.SyncCooldownPayload;
import ovh.pdx.qmine.shape.Shape;
import ovh.pdx.qmine.shape.ShapeRegistry;

import java.util.List;

public final class QMineCommands {
    private QMineCommands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext registryAccess, Commands.CommandSelection environment) {
        dispatcher.register(Commands.literal("qmine")
                // /qmine (status / help)
                .executes(context -> {
                    CommandSourceStack source = context.getSource();
                    var cfg = ConfigManager.getServerConfig();
                    if (source.isPlayer()) {
                        ServerPlayer player = source.getPlayerOrException();
                        PlayerMiningData data = Qmine.getInstance().getPlayerData(player);
                        Shape shape = data.getCurrentShape();
                        int radius = data.getCurrentShapeRadius();
                        boolean hasClient = QMineNetworking.hasClientMod(player);
                        var mode = data.getEffectiveActivation();

                        source.sendSuccess(() -> Component.literal(
                                "§b--- QMine Status ---§r\n" +
                                "§7Current Shape: §e" + shape.getId().getPath() + (shape.canResize() ? " §7(Radius: §e" + radius + "§7)" : "") + "\n" +
                                "§7Activation: §e" + (hasClient ? "Keybind (~)" : mode.name() + " §7(Vanilla Client Mode)") + "\n" +
                                "§7Client Mod: " + (hasClient ? "§aDetected" : "§cNot Detected (Server-only mode: ores only)") + "\n" +
                                "§7Commands: §e/qmine shape [name]§7, §e/qmine next§7, §e/qmine prev§7, §e/qmine radius <1-9>§7, §e/qmine mode <sneak|always|disabled>"
                        ), false);
                    } else {
                        source.sendSuccess(() -> Component.literal("§b[QMine]§r Server v1.0 | Max Blocks: §e" + cfg.maxBlocks + "§r | Cooldown: §e" + cfg.cooldownTicks + " ticks"), false);
                    }
                    return 1;
                })

                // /qmine info
                .then(Commands.literal("info")
                        .executes(context -> {
                            var cfg = ConfigManager.getServerConfig();
                            context.getSource().sendSuccess(() -> Component.literal(
                                    "§b[QMine]§r v1.0 | Max Blocks: §e" + cfg.maxBlocks + "§r | Cooldown: §e" + cfg.cooldownTicks + " ticks | Vanilla Support: §e" + cfg.vanillaClientSupport
                            ), false);
                            return 1;
                        })
                )

                // /qmine shape
                .then(Commands.literal("shape")
                        .executes(context -> {
                            ServerPlayer player = context.getSource().getPlayerOrException();
                            PlayerMiningData data = Qmine.getInstance().getPlayerData(player);
                            Shape cur = data.getCurrentShape();
                            List<String> names = ShapeRegistry.getInstance().getShapes().stream()
                                    .map(s -> s.getId().getPath())
                                    .toList();
                            context.getSource().sendSuccess(() -> Component.literal(
                                    "§b[QMine]§r Current shape: §e" + cur.getId().getPath() + "§r\n" +
                                    "§7Available shapes: §e" + String.join(", ", names) + "§r\n" +
                                    "§7Use §e/qmine shape <name>§7 or §e/qmine next§7/§eprev"
                            ), false);
                            return 1;
                        })
                        .then(Commands.argument("name", StringArgumentType.word())
                                .suggests((c, builder) -> {
                                    List<String> names = ShapeRegistry.getInstance().getShapes().stream()
                                            .map(s -> s.getId().getPath())
                                            .toList();
                                    return SharedSuggestionProvider.suggest(names, builder);
                                })
                                .executes(context -> {
                                    ServerPlayer player = context.getSource().getPlayerOrException();
                                    String name = StringArgumentType.getString(context, "name");
                                    PlayerMiningData data = Qmine.getInstance().getPlayerData(player);
                                    if (data.setShape(name)) {
                                        Shape s = data.getCurrentShape();
                                        int r = data.getCurrentShapeRadius();
                                        player.sendSystemMessage(Component.literal("§b[QMine]§r Shape set to: §e" + s.getId().getPath() + (s.canResize() ? " §7(Radius: " + r + ")" : "")), true);
                                        context.getSource().sendSuccess(() -> Component.literal("§b[QMine]§r Selected shape: §e" + s.getId().getPath()), false);
                                        return 1;
                                    } else {
                                        List<String> names = ShapeRegistry.getInstance().getShapes().stream()
                                                .map(s -> s.getId().getPath())
                                                .toList();
                                        context.getSource().sendFailure(Component.literal("§cUnknown shape: " + name + ". Available: " + String.join(", ", names)));
                                        return 0;
                                    }
                                })
                        )
                )

                // /qmine next (cycle to next shape)
                .then(Commands.literal("next")
                        .executes(context -> {
                            ServerPlayer player = context.getSource().getPlayerOrException();
                            Qmine.getInstance().cycleMode(player, true);
                            PlayerMiningData data = Qmine.getInstance().getPlayerData(player);
                            Shape s = data.getCurrentShape();
                            int r = data.getCurrentShapeRadius();
                            player.sendSystemMessage(Component.literal("§b[QMine]§r Shape: §e" + s.getId().getPath() + (s.canResize() ? " §7(Radius: " + r + ")" : "")), true);
                            context.getSource().sendSuccess(() -> Component.literal("§b[QMine]§r Shape changed to: §e" + s.getId().getPath()), false);
                            return 1;
                        })
                )

                // /qmine prev (cycle to previous shape)
                .then(Commands.literal("prev")
                        .executes(context -> {
                            ServerPlayer player = context.getSource().getPlayerOrException();
                            Qmine.getInstance().cycleMode(player, false);
                            PlayerMiningData data = Qmine.getInstance().getPlayerData(player);
                            Shape s = data.getCurrentShape();
                            int r = data.getCurrentShapeRadius();
                            player.sendSystemMessage(Component.literal("§b[QMine]§r Shape: §e" + s.getId().getPath() + (s.canResize() ? " §7(Radius: " + r + ")" : "")), true);
                            context.getSource().sendSuccess(() -> Component.literal("§b[QMine]§r Shape changed to: §e" + s.getId().getPath()), false);
                            return 1;
                        })
                )

                // /qmine radius <num>
                .then(Commands.literal("radius")
                        .then(Commands.argument("value", IntegerArgumentType.integer(1, 9))
                                .executes(context -> {
                                    ServerPlayer player = context.getSource().getPlayerOrException();
                                    int radius = IntegerArgumentType.getInteger(context, "value");
                                    PlayerMiningData data = Qmine.getInstance().getPlayerData(player);
                                    Shape shape = data.getCurrentShape();
                                    if (!shape.canResize()) {
                                        context.getSource().sendFailure(Component.literal("§cShape " + shape.getId().getPath() + " cannot be resized."));
                                        return 0;
                                    }
                                    data.setRadius(radius);
                                    int actual = data.getCurrentShapeRadius();
                                    player.sendSystemMessage(Component.literal("§b[QMine]§r " + shape.getId().getPath() + " radius: §e" + actual), true);
                                    context.getSource().sendSuccess(() -> Component.literal("§b[QMine]§r Radius set to: §e" + actual), false);
                                    return 1;
                                })
                        )
                )

                // /qmine mode <sneak|always|disabled|default>
                .then(Commands.literal("mode")
                        .then(Commands.argument("type", StringArgumentType.word())
                                .suggests((c, builder) -> SharedSuggestionProvider.suggest(List.of("sneak", "always", "disabled", "default"), builder))
                                .executes(context -> {
                                    ServerPlayer player = context.getSource().getPlayerOrException();
                                    String type = StringArgumentType.getString(context, "type").toLowerCase();
                                    PlayerMiningData data = Qmine.getInstance().getPlayerData(player);
                                    switch (type) {
                                        case "sneak" -> data.setActivationOverride(QMineConfig.VanillaActivation.SNEAK);
                                        case "always" -> data.setActivationOverride(QMineConfig.VanillaActivation.ALWAYS);
                                        case "disabled" -> data.setActivationOverride(QMineConfig.VanillaActivation.DISABLED);
                                        case "default" -> data.setActivationOverride(null);
                                        default -> {
                                            context.getSource().sendFailure(Component.literal("§cUnknown mode: " + type + ". Choose sneak, always, disabled, or default."));
                                            return 0;
                                        }
                                    }
                                    var effective = data.getEffectiveActivation();
                                    player.sendSystemMessage(Component.literal("§b[QMine]§r Activation mode: §e" + effective.name()), true);
                                    context.getSource().sendSuccess(() -> Component.literal("§b[QMine]§r Personal activation mode set to: §e" + effective.name()), false);
                                    return 1;
                                })
                        )
                )

                // /qmine toggle (quick toggle between enabled and disabled)
                .then(Commands.literal("toggle")
                        .executes(context -> {
                            ServerPlayer player = context.getSource().getPlayerOrException();
                            PlayerMiningData data = Qmine.getInstance().getPlayerData(player);
                            var cur = data.getEffectiveActivation();
                            if (cur == QMineConfig.VanillaActivation.DISABLED) {
                                data.setActivationOverride(QMineConfig.VanillaActivation.SNEAK);
                            } else {
                                data.setActivationOverride(QMineConfig.VanillaActivation.DISABLED);
                            }
                            var updated = data.getEffectiveActivation();
                            player.sendSystemMessage(Component.literal("§b[QMine]§r Vein mining: §e" + (updated != QMineConfig.VanillaActivation.DISABLED ? "ENABLED (" + updated.name() + ")" : "DISABLED")), true);
                            context.getSource().sendSuccess(() -> Component.literal("§b[QMine]§r Vein mining is now: §e" + updated.name()), false);
                            return 1;
                        })
                )

                // /qmine reload (admin only)
                .then(Commands.literal("reload")
                        .requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER))
                        .executes(context -> {
                            ConfigManager.loadServerConfig();
                            long cooldownTicks = ConfigManager.getServerConfig().cooldownTicks;

                            context.getSource().getServer().getPlayerList().getPlayers().forEach(sp ->
                                    QMineNetworking.sendToPlayer(sp, new SyncCooldownPayload(cooldownTicks, SyncCooldownPayload.CooldownType.COOLDOWN))
                            );

                            context.getSource().sendSuccess(() -> Component.literal("§b[QMine]§r Server configuration reloaded!"), true);
                            return 1;
                        })
                )
        );
    }
}
