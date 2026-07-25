package com.midgetcontrol.player;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.level.storage.LevelResource;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

public final class PlayerDataStore {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final int SCHEMA_VERSION = 1;

    private final Path dataFile;
    private final Logger logger;
    private final Map<UUID, StoredPlayer> players = new LinkedHashMap<>();
    private final Map<UUID, Long> activeSessionStartsNanos = new HashMap<>();

    private PlayerDataStore(Path dataFile, Logger logger) {
        this.dataFile = dataFile;
        this.logger = logger;
    }

    public static PlayerDataStore open(MinecraftServer server, Logger logger) {
        Path dataFile = server.getWorldPath(LevelResource.ROOT)
                .resolve("data")
                .resolve("midgetcontrol-players.json");
        return open(dataFile, logger);
    }

    static PlayerDataStore open(Path dataFile, Logger logger) {
        PlayerDataStore store = new PlayerDataStore(dataFile, logger);
        store.load();
        return store;
    }

    public void onJoin(ServerPlayer player) {
        long nowEpochMillis = System.currentTimeMillis();
        StoredPlayer stored = getOrCreate(player, nowEpochMillis);
        stored.lastKnownName = player.getName().getString();
        stored.lastSeenEpochMillis = nowEpochMillis;
        updateStats(stored, player);
        activeSessionStartsNanos.put(player.getUUID(), System.nanoTime());
        save();
    }

    public void onDisconnect(ServerPlayer player) {
        checkpoint(player, System.currentTimeMillis(), System.nanoTime(), false);
        save();
    }

    public void checkpointAndSave(Collection<ServerPlayer> onlinePlayers) {
        long nowEpochMillis = System.currentTimeMillis();
        long nowNanos = System.nanoTime();
        for (ServerPlayer player : onlinePlayers) {
            checkpoint(player, nowEpochMillis, nowNanos, true);
        }
        save();
    }

    public void close(Collection<ServerPlayer> onlinePlayers) {
        long nowEpochMillis = System.currentTimeMillis();
        long nowNanos = System.nanoTime();
        for (ServerPlayer player : onlinePlayers) {
            checkpoint(player, nowEpochMillis, nowNanos, false);
        }
        activeSessionStartsNanos.clear();
        save();
    }

    public Optional<PlayerInfo> find(String playerName, MinecraftServer server) {
        ServerPlayer online = server.getPlayerList().getPlayerByName(playerName);
        long nowEpochMillis = System.currentTimeMillis();
        if (online != null) {
            StoredPlayer stored = getOrCreate(online, nowEpochMillis);
            stored.lastKnownName = online.getName().getString();
            stored.lastSeenEpochMillis = nowEpochMillis;
            updateStats(stored, online);
            long nowNanos = System.nanoTime();
            long startedAtNanos = activeSessionStartsNanos.computeIfAbsent(online.getUUID(), ignored -> nowNanos);
            long sessionTime = elapsedMillis(startedAtNanos, nowNanos);
            return Optional.of(stored.toInfo(stored.onlineMillis + sessionTime));
        }

        String requested = playerName.toLowerCase(Locale.ROOT);
        return players.values().stream()
                .filter(player -> player.lastKnownName != null
                        && player.lastKnownName.toLowerCase(Locale.ROOT).equals(requested))
                .max(java.util.Comparator.comparingLong(player -> player.lastSeenEpochMillis))
                .map(player -> player.toInfo(player.onlineMillis));
    }

    public Stream<String> knownNames() {
        return players.values().stream()
                .map(player -> player.lastKnownName)
                .filter(name -> name != null && !name.isBlank())
                .distinct();
    }

    public boolean isBlipVisible(UUID playerId) {
        StoredPlayer stored = players.get(playerId);
        return stored != null && stored.blipVisible;
    }

    public boolean setBlipVisible(ServerPlayer player, boolean visible) {
        long nowEpochMillis = System.currentTimeMillis();
        StoredPlayer stored = getOrCreate(player, nowEpochMillis);
        stored.lastKnownName = player.getName().getString();
        stored.lastSeenEpochMillis = nowEpochMillis;
        updateStats(stored, player);
        return setBlipVisible(player.getUUID(), visible);
    }

    boolean setBlipVisible(UUID playerId, boolean visible) {
        StoredPlayer stored = players.get(playerId);
        if (stored == null) {
            return false;
        }
        stored.blipVisible = visible;
        return save();
    }

    private StoredPlayer getOrCreate(ServerPlayer player, long now) {
        return players.computeIfAbsent(player.getUUID(), ignored -> {
            StoredPlayer created = new StoredPlayer();
            created.lastKnownName = player.getName().getString();
            created.firstJoinedEpochMillis = now;
            created.lastSeenEpochMillis = now;
            return created;
        });
    }

    private void checkpoint(ServerPlayer player, long nowEpochMillis, long nowNanos, boolean continueSession) {
        StoredPlayer stored = getOrCreate(player, nowEpochMillis);
        Long startedAtNanos = continueSession
                ? activeSessionStartsNanos.put(player.getUUID(), nowNanos)
                : activeSessionStartsNanos.remove(player.getUUID());
        if (startedAtNanos != null) {
            stored.onlineMillis += elapsedMillis(startedAtNanos, nowNanos);
        }
        stored.lastKnownName = player.getName().getString();
        stored.lastSeenEpochMillis = nowEpochMillis;
        updateStats(stored, player);
    }

    private static long elapsedMillis(long startedAtNanos, long nowNanos) {
        return TimeUnit.NANOSECONDS.toMillis(Math.max(0L, nowNanos - startedAtNanos));
    }

    private static void updateStats(StoredPlayer stored, ServerPlayer player) {
        stored.playerKills = player.getStats().getValue(Stats.CUSTOM, Stats.PLAYER_KILLS);
        stored.deaths = player.getStats().getValue(Stats.CUSTOM, Stats.DEATHS);
    }

    private void load() {
        if (Files.notExists(dataFile)) {
            return;
        }

        try (Reader reader = Files.newBufferedReader(dataFile, StandardCharsets.UTF_8)) {
            DataFile loaded = GSON.fromJson(reader, DataFile.class);
            if (loaded == null || loaded.schemaVersion != SCHEMA_VERSION || loaded.players == null) {
                throw new JsonParseException("Unsupported or missing player-data schema");
            }
            for (Map.Entry<String, StoredPlayer> entry : loaded.players.entrySet()) {
                if (entry.getValue() == null) {
                    logger.warn("Ignoring null player record '{}' in {}", entry.getKey(), dataFile);
                    continue;
                }
                try {
                    players.put(UUID.fromString(entry.getKey()), entry.getValue());
                } catch (IllegalArgumentException exception) {
                    logger.warn("Ignoring invalid player UUID '{}' in {}", entry.getKey(), dataFile);
                }
            }
        } catch (IOException | JsonParseException exception) {
            preserveBrokenFile(exception);
        }
    }

    public boolean save() {
        try {
            Files.createDirectories(dataFile.getParent());
            Path temporary = dataFile.resolveSibling(dataFile.getFileName() + ".tmp");
            DataFile data = new DataFile();
            for (Map.Entry<UUID, StoredPlayer> entry : players.entrySet()) {
                data.players.put(entry.getKey().toString(), entry.getValue());
            }
            try (Writer writer = Files.newBufferedWriter(temporary, StandardCharsets.UTF_8)) {
                GSON.toJson(data, writer);
            }
            try {
                Files.move(temporary, dataFile, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException exception) {
                Files.move(temporary, dataFile, StandardCopyOption.REPLACE_EXISTING);
            }
            return true;
        } catch (IOException exception) {
            logger.error("Could not save MidgetControl player data to {}", dataFile, exception);
            return false;
        }
    }

    private void preserveBrokenFile(Exception cause) {
        String timestamp = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")
                .withZone(ZoneOffset.UTC)
                .format(Instant.now());
        Path backup = dataFile.resolveSibling(dataFile.getFileName() + ".broken-" + timestamp);
        try {
            Files.move(dataFile, backup, StandardCopyOption.REPLACE_EXISTING);
            logger.error("Could not read {}; moved it to {}", dataFile, backup, cause);
        } catch (IOException moveFailure) {
            logger.error("Could not read {} or preserve the broken file", dataFile, cause);
            logger.error("Player-data backup also failed", moveFailure);
        }
    }

    public record PlayerInfo(
            String name,
            long firstJoinedEpochMillis,
            long onlineMillis,
            int playerKills,
            int deaths
    ) {
    }

    private static final class DataFile {
        private int schemaVersion = SCHEMA_VERSION;
        private Map<String, StoredPlayer> players = new LinkedHashMap<>();
    }

    private static final class StoredPlayer {
        private String lastKnownName;
        private long firstJoinedEpochMillis;
        private long lastSeenEpochMillis;
        private long onlineMillis;
        private int playerKills;
        private int deaths;
        private boolean blipVisible;

        private PlayerInfo toInfo(long currentOnlineMillis) {
            return new PlayerInfo(
                    lastKnownName,
                    firstJoinedEpochMillis,
                    currentOnlineMillis,
                    playerKills,
                    deaths
            );
        }
    }
}
