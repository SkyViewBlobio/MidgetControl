package com.midgetcontrol.tps;

import com.midgetcontrol.config.MidgetControlConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.game.ClientboundTabListPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.Locale;

public final class TabTpsDisplay {
    private int ticksSinceUpdate;

    public void tick(MinecraftServer server, MidgetControlConfig config) {
        if (!config.tabTpsEnabled()) {
            return;
        }
        ticksSinceUpdate++;
        if (ticksSinceUpdate >= config.tabTpsUpdateIntervalTicks()) {
            ticksSinceUpdate = 0;
            broadcastNow(server, config);
        }
    }

    public void reset() {
        ticksSinceUpdate = 0;
    }

    public void broadcastNow(MinecraftServer server, MidgetControlConfig config) {
        if (!config.tabTpsEnabled()) {
            return;
        }
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            player.connection.send(createPacket(server, player, config));
        }
    }

    public void sendNow(MinecraftServer server, ServerPlayer player, MidgetControlConfig config) {
        if (config.tabTpsEnabled()) {
            player.connection.send(createPacket(server, player, config));
        }
    }

    public void clear(MinecraftServer server) {
        ClientboundTabListPacket packet = new ClientboundTabListPacket(Component.empty(), Component.empty());
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            player.connection.send(packet);
        }
    }

    private static ClientboundTabListPacket createPacket(
            MinecraftServer server,
            ServerPlayer player,
            MidgetControlConfig config
    ) {
        double targetTps = server.tickRateManager().tickrate();
        long averageTickTimeNanos = server.getAverageTickTimeNanos();
        double mspt = averageTickTimeNanos / 1_000_000.0;
        double tps = Math.min(targetTps, 1_000_000_000.0 / Math.max(1L, averageTickTimeNanos));
        double health = targetTps <= 0.0 ? 1.0 : tps / targetTps;
        ChatFormatting tpsColor = health >= 0.9
                ? ChatFormatting.GREEN
                : health >= 0.75 ? ChatFormatting.YELLOW : ChatFormatting.RED;

        MutableComponent header = Component.empty();
        if (!config.tabTpsTitle().isBlank()) {
            header.append(Component.literal(config.tabTpsTitle()).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
            header.append("\n");
        }
        header.append(Component.literal("TPS: ").withStyle(ChatFormatting.GRAY));
        header.append(Component.literal(String.format(Locale.ROOT, "%.1f", tps)).withStyle(tpsColor));
        if (config.tabTpsShowMspt()) {
            header.append(Component.literal(String.format(Locale.ROOT, "  |  MSPT: %.1f", mspt))
                    .withStyle(ChatFormatting.DARK_GRAY));
        }

        int playerCount = server.getPlayerList().getPlayerCount();
        if (!server.getPlayerList().getPlayers().contains(player)) {
            playerCount++;
        }
        Component footer = createFooter(playerCount, player.connection.latency());
        return new ClientboundTabListPacket(header, footer);
    }

    static Component createFooter(int playerCount, int latency) {
        return Component.empty()
                .append(Component.literal("Players: ").withStyle(ChatFormatting.GRAY))
                .append(Component.literal(Integer.toString(playerCount)).withStyle(ChatFormatting.WHITE))
                .append(Component.literal(" | ").withStyle(ChatFormatting.DARK_GRAY))
                .append(Component.literal("Ping: ").withStyle(ChatFormatting.GRAY))
                .append(Component.literal(Math.max(0, latency) + "ms").withStyle(ChatFormatting.WHITE));
    }
}
