package com.midgetcontrol.player;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.helpers.NOPLogger;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerDataStoreTest {
    private static final UUID PLAYER_ID = UUID.fromString("11111111-2222-3333-4444-555555555555");

    @TempDir
    Path tempDirectory;

    @Test
    void legacyPlayersDefaultHiddenAndBlipChoicePersists() throws Exception {
        Path dataFile = tempDirectory.resolve("midgetcontrol-players.json");
        Files.writeString(dataFile, """
                {
                  "schemaVersion": 1,
                  "players": {
                    "11111111-2222-3333-4444-555555555555": {
                      "lastKnownName": "TestPlayer",
                      "firstJoinedEpochMillis": 1,
                      "lastSeenEpochMillis": 2,
                      "onlineMillis": 3,
                      "playerKills": 4,
                      "deaths": 5
                    }
                  }
                }
                """);

        PlayerDataStore store = PlayerDataStore.open(dataFile, NOPLogger.NOP_LOGGER);
        assertFalse(store.isBlipVisible(PLAYER_ID));

        assertTrue(store.setBlipVisible(PLAYER_ID, true));
        store = PlayerDataStore.open(dataFile, NOPLogger.NOP_LOGGER);
        assertTrue(store.isBlipVisible(PLAYER_ID));

        assertTrue(store.setBlipVisible(PLAYER_ID, false));
        store = PlayerDataStore.open(dataFile, NOPLogger.NOP_LOGGER);
        assertFalse(store.isBlipVisible(PLAYER_ID));
    }
}
