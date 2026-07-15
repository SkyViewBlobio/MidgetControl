package com.midgetcontrol.config;

import org.slf4j.Logger;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.DateTimeException;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Properties;

public final class MidgetControlConfig {
    private static final String DEFAULT_FILE = """
            # MidgetControl 1.0.0
            # Natural caps are percentages of Minecraft's normal biome-spawn caps.
            # 100 = vanilla cap, 50 = half the vanilla cap, 0 = no natural spawns in that category.
            # These settings do not block spawners, trial spawners, breeding, spawn eggs, commands,
            # summoned mobs, raids, patrols, structures, golems, withers, dragons, or persistent mobs.
            natural-spawning.enabled=true
            natural-spawning.monster-cap-percent=50
            natural-spawning.creature-cap-percent=50
            natural-spawning.ambient-cap-percent=50
            natural-spawning.axolotl-cap-percent=50
            natural-spawning.underground-water-creature-cap-percent=50
            natural-spawning.water-creature-cap-percent=50
            natural-spawning.water-ambient-cap-percent=50

            # /info <player> is visible to everyone at permission level 0.
            # Permission levels: 0=everyone, 1=moderator, 2=gamemaster, 3=admin, 4=owner.
            player-info.enabled=true
            player-info.permission-level=0
            player-info.register-short-command=true
            player-info.date-format=yyyy-MM-dd HH:mm z
            player-info.time-zone=SYSTEM

            # The TPS line is sent as the vanilla tab-list header. No client mod is needed.
            tab-tps.enabled=true
            tab-tps.update-interval-ticks=40
            tab-tps.title=MidgetControl
            tab-tps.show-mspt=true

            # Player join/playtime data is stored per world in world/data/midgetcontrol-players.json.
            player-data.autosave-interval-seconds=60
            admin.reload-permission-level=4
            """;

    private final boolean naturalSpawningEnabled;
    private final int monsterCapPercent;
    private final int creatureCapPercent;
    private final int ambientCapPercent;
    private final int axolotlCapPercent;
    private final int undergroundWaterCreatureCapPercent;
    private final int waterCreatureCapPercent;
    private final int waterAmbientCapPercent;
    private final boolean playerInfoEnabled;
    private final int playerInfoPermissionLevel;
    private final boolean playerInfoShortCommand;
    private final DateTimeFormatter dateFormatter;
    private final ZoneId timeZone;
    private final boolean tabTpsEnabled;
    private final int tabTpsUpdateIntervalTicks;
    private final String tabTpsTitle;
    private final boolean tabTpsShowMspt;
    private final int autosaveIntervalSeconds;
    private final int reloadPermissionLevel;

    private MidgetControlConfig(Properties properties, Logger logger) {
        naturalSpawningEnabled = booleanValue(properties, "natural-spawning.enabled", true, logger);
        monsterCapPercent = intValue(properties, "natural-spawning.monster-cap-percent", 50, 0, 100, logger);
        creatureCapPercent = intValue(properties, "natural-spawning.creature-cap-percent", 50, 0, 100, logger);
        ambientCapPercent = intValue(properties, "natural-spawning.ambient-cap-percent", 50, 0, 100, logger);
        axolotlCapPercent = intValue(properties, "natural-spawning.axolotl-cap-percent", 50, 0, 100, logger);
        undergroundWaterCreatureCapPercent = intValue(properties, "natural-spawning.underground-water-creature-cap-percent", 50, 0, 100, logger);
        waterCreatureCapPercent = intValue(properties, "natural-spawning.water-creature-cap-percent", 50, 0, 100, logger);
        waterAmbientCapPercent = intValue(properties, "natural-spawning.water-ambient-cap-percent", 50, 0, 100, logger);

        playerInfoEnabled = booleanValue(properties, "player-info.enabled", true, logger);
        playerInfoPermissionLevel = intValue(properties, "player-info.permission-level", 0, 0, 4, logger);
        playerInfoShortCommand = booleanValue(properties, "player-info.register-short-command", true, logger);
        dateFormatter = dateFormatter(properties.getProperty("player-info.date-format", "yyyy-MM-dd HH:mm z"), logger);
        timeZone = timeZone(properties.getProperty("player-info.time-zone", "SYSTEM"), logger);

        tabTpsEnabled = booleanValue(properties, "tab-tps.enabled", true, logger);
        tabTpsUpdateIntervalTicks = intValue(properties, "tab-tps.update-interval-ticks", 40, 10, 1200, logger);
        tabTpsTitle = properties.getProperty("tab-tps.title", "MidgetControl").trim();
        tabTpsShowMspt = booleanValue(properties, "tab-tps.show-mspt", true, logger);
        autosaveIntervalSeconds = intValue(properties, "player-data.autosave-interval-seconds", 60, 10, 3600, logger);
        reloadPermissionLevel = intValue(properties, "admin.reload-permission-level", 4, 0, 4, logger);
    }

    public static MidgetControlConfig defaults(Logger logger) {
        return new MidgetControlConfig(defaultProperties(), logger);
    }

    public static MidgetControlConfig load(Path path, Logger logger) throws IOException {
        Files.createDirectories(path.getParent());
        if (Files.notExists(path)) {
            Files.writeString(path, DEFAULT_FILE, StandardCharsets.UTF_8);
        }

        Properties properties = new Properties();
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            properties.load(reader);
        }
        return new MidgetControlConfig(properties, logger);
    }

    private static Properties defaultProperties() {
        Properties properties = new Properties();
        try {
            properties.load(new java.io.StringReader(DEFAULT_FILE));
        } catch (IOException impossible) {
            throw new IllegalStateException(impossible);
        }
        return properties;
    }

    private static boolean booleanValue(Properties properties, String key, boolean fallback, Logger logger) {
        String value = properties.getProperty(key);
        if (value == null) {
            return fallback;
        }
        value = value.trim();
        if (value.equalsIgnoreCase("true")) {
            return true;
        }
        if (value.equalsIgnoreCase("false")) {
            return false;
        }
        logger.warn("Invalid {} value '{}'; using {}", key, value, fallback);
        return fallback;
    }

    private static int intValue(Properties properties, String key, int fallback, int min, int max, Logger logger) {
        String value = properties.getProperty(key);
        if (value == null) {
            return fallback;
        }
        try {
            int parsed = Integer.parseInt(value.trim());
            if (parsed >= min && parsed <= max) {
                return parsed;
            }
        } catch (NumberFormatException ignored) {
        }
        logger.warn("Invalid {} value '{}'; expected {}-{}, using {}", key, value, min, max, fallback);
        return fallback;
    }

    private static DateTimeFormatter dateFormatter(String pattern, Logger logger) {
        try {
            return DateTimeFormatter.ofPattern(pattern);
        } catch (IllegalArgumentException exception) {
            logger.warn("Invalid player-info.date-format '{}'; using the default", pattern);
            return DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm z");
        }
    }

    private static ZoneId timeZone(String configured, Logger logger) {
        if (configured.equalsIgnoreCase("SYSTEM")) {
            return ZoneId.systemDefault();
        }
        try {
            return ZoneId.of(configured);
        } catch (DateTimeException exception) {
            logger.warn("Invalid player-info.time-zone '{}'; using the server time zone", configured);
            return ZoneId.systemDefault();
        }
    }

    public boolean naturalSpawningEnabled() {
        return naturalSpawningEnabled;
    }

    public int monsterCapPercent() {
        return monsterCapPercent;
    }

    public int creatureCapPercent() {
        return creatureCapPercent;
    }

    public int ambientCapPercent() {
        return ambientCapPercent;
    }

    public int axolotlCapPercent() {
        return axolotlCapPercent;
    }

    public int undergroundWaterCreatureCapPercent() {
        return undergroundWaterCreatureCapPercent;
    }

    public int waterCreatureCapPercent() {
        return waterCreatureCapPercent;
    }

    public int waterAmbientCapPercent() {
        return waterAmbientCapPercent;
    }

    public boolean playerInfoEnabled() {
        return playerInfoEnabled;
    }

    public int playerInfoPermissionLevel() {
        return playerInfoPermissionLevel;
    }

    public boolean playerInfoShortCommand() {
        return playerInfoShortCommand;
    }

    public DateTimeFormatter dateFormatter() {
        return dateFormatter;
    }

    public ZoneId timeZone() {
        return timeZone;
    }

    public boolean tabTpsEnabled() {
        return tabTpsEnabled;
    }

    public int tabTpsUpdateIntervalTicks() {
        return tabTpsUpdateIntervalTicks;
    }

    public String tabTpsTitle() {
        return tabTpsTitle;
    }

    public boolean tabTpsShowMspt() {
        return tabTpsShowMspt;
    }

    public int autosaveIntervalSeconds() {
        return autosaveIntervalSeconds;
    }

    public int reloadPermissionLevel() {
        return reloadPermissionLevel;
    }
}
