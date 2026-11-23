package ovh.pdx.qmine.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraftforge.fml.loading.FMLPaths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;

public final class ConfigManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("QMine/Config");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static final Path CONFIG_DIR = FMLPaths.CONFIGDIR.get().resolve("qmine");
    private static final Path SERVER_CONFIG_PATH = CONFIG_DIR.resolve("server.json");
    private static final Path CLIENT_CONFIG_PATH = CONFIG_DIR.resolve("client.json");

    private static QMineConfig serverConfig = new QMineConfig();
    private static QMineClientConfig clientConfig = new QMineClientConfig();

    private ConfigManager() {
    }

    public static QMineConfig getServerConfig() {
        return serverConfig;
    }

    public static QMineClientConfig getClientConfig() {
        return clientConfig;
    }

    public static void loadServerConfig() {
        serverConfig = load(SERVER_CONFIG_PATH, QMineConfig.class, new QMineConfig());
        if (serverConfig.mergeTagsShaped == null || serverConfig.mergeTagsShaped.isEmpty() || serverConfig.mergeTagsShaped.contains("*")) {
            serverConfig.mergeTagsShaped = new ArrayList<>(java.util.List.of(
                    "minecraft:base_stone_overworld",
                    "minecraft:base_stone_nether",
                    "minecraft:*_ores",
                    "c:ores/*",
                    "c:ores"
            ));
            saveServerConfig();
        }
    }

    public static void saveServerConfig() {
        save(SERVER_CONFIG_PATH, serverConfig);
    }

    public static void loadClientConfig() {
        clientConfig = load(CLIENT_CONFIG_PATH, QMineClientConfig.class, new QMineClientConfig());
    }

    public static void saveClientConfig() {
        save(CLIENT_CONFIG_PATH, clientConfig);
    }

    private static <T> T load(Path path, Class<T> clazz, T defaultValue) {
        if (!Files.exists(path)) {
            save(path, defaultValue);
            return defaultValue;
        }

        try (Reader reader = Files.newBufferedReader(path)) {
            T config = GSON.fromJson(reader, clazz);
            if (config == null) {
                LOGGER.warn("Config file {} was empty, using defaults", path);
                return defaultValue;
            }
            save(path, config);
            return config;
        } catch (IOException e) {
            LOGGER.error("Failed to read config from {}, using defaults", path, e);
            return defaultValue;
        }
    }

    private static <T> void save(Path path, T config) {
        try {
            Files.createDirectories(path.getParent());
            try (Writer writer = Files.newBufferedWriter(path)) {
                GSON.toJson(config, writer);
            }
        } catch (IOException e) {
            LOGGER.error("Failed to save config to {}", path, e);
        }
    }
}
