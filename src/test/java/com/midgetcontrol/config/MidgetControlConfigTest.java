package com.midgetcontrol.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.helpers.NOPLogger;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MidgetControlConfigTest {
    @TempDir
    Path tempDirectory;

    @Test
    void generatesDefaultsWhenConfigDoesNotExist() throws Exception {
        Path configPath = tempDirectory.resolve("config").resolve("midgetcontrol.properties");
        MidgetControlConfig config = MidgetControlConfig.load(configPath, NOPLogger.NOP_LOGGER);

        assertTrue(Files.exists(configPath));
        assertTrue(config.naturalSpawningEnabled());
        assertTrue(config.playerInfoShortCommand());
        assertEquals("MidgetCraft", config.tabTpsTitle());
    }

    @Test
    void trimsBooleanValues() throws Exception {
        Path configPath = tempDirectory.resolve("midgetcontrol.properties");
        Files.writeString(configPath, "tab-tps.enabled=false  \n");

        MidgetControlConfig config = MidgetControlConfig.load(configPath, NOPLogger.NOP_LOGGER);

        assertFalse(config.tabTpsEnabled());
    }

    @Test
    void migratesOnlyTheLegacyTabTitle() throws Exception {
        Path legacyPath = tempDirectory.resolve("legacy.properties");
        Files.writeString(legacyPath, "tab-tps.title=MidgetControl\n");
        assertEquals("MidgetCraft", MidgetControlConfig.load(legacyPath, NOPLogger.NOP_LOGGER).tabTpsTitle());

        Path customPath = tempDirectory.resolve("custom.properties");
        Files.writeString(customPath, "tab-tps.title=Custom Server\n");
        assertEquals("Custom Server", MidgetControlConfig.load(customPath, NOPLogger.NOP_LOGGER).tabTpsTitle());
    }
}

