package ovh.pdx.qmine.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

public final class ConfigManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("QMine/Config");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static final Path CONFIG_DIR = FabricLoader.getInstance().getConfigDir().resolve("qmine");
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
        try {
            if (!Files.exists(CONFIG_DIR)) {
                Files.createDirectories(CONFIG_DIR);
            }

            if (Files.exists(SERVER_CONFIG_PATH)) {
                try (Reader reader = Files.newBufferedReader(SERVER_CONFIG_PATH)) {
                    QMineConfig loaded = GSON.fromJson(reader, QMineConfig.class);
                    if (loaded != null) {
                        serverConfig = loaded;
                    }
                }
            } else {
                saveServerConfig();
            }
        } catch (Exception e) {
            LOGGER.error("Failed to load QMine server config, using defaults", e);
        }
    }

    public static void saveServerConfig() {
        try {
            if (!Files.exists(CONFIG_DIR)) {
                Files.createDirectories(CONFIG_DIR);
            }
            try (Writer writer = Files.newBufferedWriter(SERVER_CONFIG_PATH)) {
                GSON.toJson(serverConfig, writer);
            }
        } catch (IOException e) {
            LOGGER.error("Failed to save QMine server config", e);
        }
    }

    public static void loadClientConfig() {
        try {
            if (!Files.exists(CONFIG_DIR)) {
                Files.createDirectories(CONFIG_DIR);
            }

            if (Files.exists(CLIENT_CONFIG_PATH)) {
                try (Reader reader = Files.newBufferedReader(CLIENT_CONFIG_PATH)) {
                    QMineClientConfig loaded = GSON.fromJson(reader, QMineClientConfig.class);
                    if (loaded != null) {
                        clientConfig = loaded;
                    }
                }
            } else {
                saveClientConfig();
            }
        } catch (Exception e) {
            LOGGER.error("Failed to load QMine client config, using defaults", e);
        }
    }

    public static void saveClientConfig() {
        try {
            if (!Files.exists(CONFIG_DIR)) {
                Files.createDirectories(CONFIG_DIR);
            }
            try (Writer writer = Files.newBufferedWriter(CLIENT_CONFIG_PATH)) {
                GSON.toJson(clientConfig, writer);
            }
        } catch (IOException e) {
            LOGGER.error("Failed to save QMine client config", e);
        }
    }
}
