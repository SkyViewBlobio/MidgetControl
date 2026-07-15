package com.midgetcontrol;

import com.midgetcontrol.command.MidgetControlCommands;
import com.midgetcontrol.config.MidgetControlConfig;
import com.midgetcontrol.player.PlayerDataStore;
import com.midgetcontrol.tps.TabTpsDisplay;
import com.mojang.logging.LogUtils;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Path;

public final class MidgetControl implements ModInitializer {
    public static final String MOD_ID = "midgetcontrol";
    public static final Logger LOGGER = LogUtils.getLogger();

    private static volatile MidgetControlConfig config = MidgetControlConfig.defaults(LOGGER);

    private final TabTpsDisplay tabTpsDisplay = new TabTpsDisplay();
    private Path configPath;
    private PlayerDataStore playerDataStore;
    private long nextAutosaveAt;

    @Override
    public void onInitialize() {
        configPath = FabricLoader.getInstance().getConfigDir().resolve("midgetcontrol.properties");
        loadInitialConfig();

        CommandRegistrationCallback.EVENT.register((dispatcher, buildContext, selection) ->
                MidgetControlCommands.register(dispatcher, this));

        ServerLifecycleEvents.SERVER_STARTED.register(this::onServerStarted);
        ServerLifecycleEvents.BEFORE_SAVE.register((server, flush, force) -> savePlayerData(server));
        ServerLifecycleEvents.SERVER_STOPPING.register(this::onServerStopping);
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> playerDataStore = null);

        ServerPlayConnectionEvents.JOIN.register((listener, sender, server) -> {
            if (playerDataStore != null) {
                playerDataStore.onJoin(listener.getPlayer());
            }
            tabTpsDisplay.sendNow(server, listener.getPlayer(), config);
        });
        ServerPlayConnectionEvents.DISCONNECT.register((listener, server) -> {
            if (playerDataStore != null) {
                playerDataStore.onDisconnect(listener.getPlayer());
            }
        });

        ServerTickEvents.END_SERVER_TICK.register(this::onServerTick);
        LOGGER.info("MidgetControl initialized for Minecraft 26.2");
    }

    private void loadInitialConfig() {
        try {
            config = MidgetControlConfig.load(configPath, LOGGER);
        } catch (IOException exception) {
            LOGGER.error("Could not load {}; using built-in defaults", configPath, exception);
            config = MidgetControlConfig.defaults(LOGGER);
        }
    }

    private void onServerStarted(MinecraftServer server) {
        playerDataStore = PlayerDataStore.open(server, LOGGER);
        nextAutosaveAt = System.currentTimeMillis() + config.autosaveIntervalSeconds() * 1000L;
        tabTpsDisplay.reset();
    }

    private void onServerTick(MinecraftServer server) {
        tabTpsDisplay.tick(server, config);

        long now = System.currentTimeMillis();
        if (playerDataStore != null && now >= nextAutosaveAt) {
            playerDataStore.checkpointAndSave(server.getPlayerList().getPlayers());
            nextAutosaveAt = now + config.autosaveIntervalSeconds() * 1000L;
        }
    }

    private void savePlayerData(MinecraftServer server) {
        if (playerDataStore != null) {
            playerDataStore.checkpointAndSave(server.getPlayerList().getPlayers());
        }
    }

    private void onServerStopping(MinecraftServer server) {
        if (playerDataStore != null) {
            playerDataStore.close(server.getPlayerList().getPlayers());
        }
    }

    public boolean reloadConfig(MinecraftServer server) {
        try {
            MidgetControlConfig previous = config;
            MidgetControlConfig reloaded = MidgetControlConfig.load(configPath, LOGGER);
            config = reloaded;
            nextAutosaveAt = System.currentTimeMillis() + reloaded.autosaveIntervalSeconds() * 1000L;
            tabTpsDisplay.reset();
            if (reloaded.tabTpsEnabled()) {
                tabTpsDisplay.broadcastNow(server, reloaded);
            } else if (previous.tabTpsEnabled()) {
                tabTpsDisplay.clear(server);
            }
            for (var player : server.getPlayerList().getPlayers()) {
                server.getCommands().sendCommands(player);
            }
            LOGGER.info("Reloaded {}", configPath);
            return true;
        } catch (IOException exception) {
            LOGGER.error("Could not reload {}", configPath, exception);
            return false;
        }
    }

    public PlayerDataStore playerDataStore() {
        return playerDataStore;
    }

    public static MidgetControlConfig config() {
        return config;
    }
}
