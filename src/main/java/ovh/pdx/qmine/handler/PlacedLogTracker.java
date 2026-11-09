package ovh.pdx.qmine.handler;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.LevelResource;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class PlacedLogTracker {
    private static final PlacedLogTracker INSTANCE = new PlacedLogTracker();

    private final Map<ResourceKey<Level>, Set<Long>> placedLogs = new ConcurrentHashMap<>();

    private PlacedLogTracker() {
    }

    public static PlacedLogTracker getInstance() {
        return INSTANCE;
    }

    public void recordPlaced(ServerLevel level, BlockPos pos) {
        placedLogs.computeIfAbsent(level.dimension(), k -> ConcurrentHashMap.newKeySet())
                .add(pos.asLong());
    }

    public void removePlaced(ServerLevel level, BlockPos pos) {
        Set<Long> set = placedLogs.get(level.dimension());
        if (set != null) {
            set.remove(pos.asLong());
        }
    }

    public boolean isPlayerPlaced(ServerLevel level, BlockPos pos) {
        Set<Long> set = placedLogs.get(level.dimension());
        return set != null && set.contains(pos.asLong());
    }

    public void save(ServerLevel level) {
        Set<Long> set = placedLogs.get(level.dimension());
        if (set == null) {
            return;
        }

        try {
            Path dir = level.getServer().getWorldPath(LevelResource.ROOT).resolve("data");
            Files.createDirectories(dir);
            Path file = dir.resolve("qmine_placed_logs_" + level.dimension().identifier().getPath() + ".dat");

            try (DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(Files.newOutputStream(file)))) {
                dos.writeInt(set.size());
                for (Long packed : set) {
                    dos.writeLong(packed);
                }
            }
        } catch (Exception e) {
            // Silently ignore
        }
    }

    public void load(ServerLevel level) {
        try {
            Path file = level.getServer().getWorldPath(LevelResource.ROOT)
                    .resolve("data")
                    .resolve("qmine_placed_logs_" + level.dimension().identifier().getPath() + ".dat");

            if (!Files.exists(file)) {
                return;
            }

            Set<Long> set = placedLogs.computeIfAbsent(level.dimension(), k -> ConcurrentHashMap.newKeySet());
            try (DataInputStream dis = new DataInputStream(new BufferedInputStream(Files.newInputStream(file)))) {
                int count = dis.readInt();
                for (int i = 0; i < count; i++) {
                    set.add(dis.readLong());
                }
            }
        } catch (Exception e) {
            // Silently ignore
        }
    }

    public void clear() {
        placedLogs.clear();
    }
}
