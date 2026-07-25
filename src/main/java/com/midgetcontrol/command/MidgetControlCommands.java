package com.midgetcontrol.command;

import com.midgetcontrol.MidgetControl;
import com.midgetcontrol.config.MidgetControlConfig;
import com.midgetcontrol.player.PlayerDataStore;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.dialog.CommonDialogData;
import net.minecraft.server.dialog.DialogAction;
import net.minecraft.server.dialog.NoticeDialog;
import net.minecraft.server.dialog.body.PlainMessage;
import net.minecraft.server.level.ServerPlayer;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public final class MidgetControlCommands {
    private MidgetControlCommands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, MidgetControl mod) {
        boolean shortInfoCommandRegistered = MidgetControl.config().playerInfoShortCommand();
        dispatcher.register(Commands.literal("midgethelp")
                .requires(CommandSourceStack::isPlayer)
                .executes(context -> showHelp(context, shortInfoCommandRegistered)));
        dispatcher.register(Commands.literal("blipon")
                .requires(CommandSourceStack::isPlayer)
                .executes(context -> setBlipVisibility(context, mod, true)));
        dispatcher.register(Commands.literal("blipoff")
                .requires(CommandSourceStack::isPlayer)
                .executes(context -> setBlipVisibility(context, mod, false)));

        if (shortInfoCommandRegistered) {
            dispatcher.register(Commands.literal("info")
                    .requires(source -> canUseInfo(source, MidgetControl.config()))
                    .then(infoArgument(mod)));
        }

        dispatcher.register(Commands.literal("midgetcontrol")
                .then(Commands.literal("info")
                        .requires(source -> canUseInfo(source, MidgetControl.config()))
                        .then(infoArgument(mod)))
                .then(Commands.literal("reload")
                        .requires(source -> hasPermission(source, MidgetControl.config().reloadPermissionLevel()))
                        .executes(context -> reload(context, mod))));
    }

    private static ArgumentBuilder<CommandSourceStack, ?> infoArgument(MidgetControl mod) {
        return Commands.argument("player", StringArgumentType.word())
                .suggests((context, builder) -> {
                    PlayerDataStore store = mod.playerDataStore();
                    return store == null
                            ? builder.buildFuture()
                            : SharedSuggestionProvider.suggest(store.knownNames(), builder);
                })
                .executes(context -> showInfo(context, mod));
    }

    private static int showInfo(CommandContext<CommandSourceStack> context, MidgetControl mod) {
        PlayerDataStore store = mod.playerDataStore();
        if (store == null) {
            context.getSource().sendFailure(Component.literal("Player data is not available yet."));
            return 0;
        }

        String requestedName = StringArgumentType.getString(context, "player");
        Optional<PlayerDataStore.PlayerInfo> result = store.find(requestedName, context.getSource().getServer());
        if (result.isEmpty()) {
            context.getSource().sendFailure(Component.literal("No player named '" + requestedName + "' has joined this server."));
            return 0;
        }

        MidgetControlConfig config = MidgetControl.config();
        PlayerDataStore.PlayerInfo info = result.get();
        String joinedAt = config.dateFormatter().format(
                Instant.ofEpochMilli(info.firstJoinedEpochMillis()).atZone(config.timeZone()));

        MutableComponent message = Component.empty()
                .append(Component.literal(info.name()).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD))
                .append(Component.literal("\nJoined: ").withStyle(ChatFormatting.GRAY))
                .append(Component.literal(joinedAt).withStyle(ChatFormatting.WHITE))
                .append(Component.literal("\nTime online: ").withStyle(ChatFormatting.GRAY))
                .append(Component.literal(formatDuration(info.onlineMillis())).withStyle(ChatFormatting.WHITE))
                .append(Component.literal("\nPlayer kills: ").withStyle(ChatFormatting.GRAY))
                .append(Component.literal(Integer.toString(info.playerKills())).withStyle(ChatFormatting.WHITE))
                .append(Component.literal("\nDeaths: ").withStyle(ChatFormatting.GRAY))
                .append(Component.literal(Integer.toString(info.deaths())).withStyle(ChatFormatting.WHITE));

        context.getSource().sendSystemMessage(message);
        return 1;
    }

    private static int reload(CommandContext<CommandSourceStack> context, MidgetControl mod) {
        if (mod.reloadConfig(context.getSource().getServer())) {
            context.getSource().sendSystemMessage(Component.literal("MidgetControl config reloaded.")
                    .withStyle(ChatFormatting.GREEN));
            return 1;
        }
        context.getSource().sendFailure(Component.literal("MidgetControl config reload failed. Check the server log."));
        return 0;
    }

    private static int setBlipVisibility(
            CommandContext<CommandSourceStack> context,
            MidgetControl mod,
            boolean visible
    ) throws CommandSyntaxException {
        if (mod.playerDataStore() == null) {
            context.getSource().sendFailure(Component.literal("Player data is not available yet."));
            return 0;
        }

        ServerPlayer player = context.getSource().getPlayerOrException();
        boolean updated = mod.setBlipVisible(player, visible);
        if (!updated) {
            context.getSource().sendFailure(Component.literal(
                    "The blip changed for this session, but MidgetControl could not fully apply or save it. Check the server log."
            ));
            return 0;
        }

        String message = visible
                ? "Your locator blip is now visible to other players."
                : "Your locator blip is now hidden from other players.";
        context.getSource().sendSystemMessage(Component.literal(message).withStyle(ChatFormatting.GREEN));
        return 1;
    }

    private static int showHelp(
            CommandContext<CommandSourceStack> context,
            boolean shortInfoCommandRegistered
    ) throws CommandSyntaxException {
        CommonDialogData common = new CommonDialogData(
                Component.literal("MidgetCraft Commands").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD),
                Optional.empty(),
                true,
                false,
                DialogAction.CLOSE,
                List.of(new PlainMessage(
                        buildHelpBody(MidgetControl.config(), shortInfoCommandRegistered),
                        360
                )),
                List.of()
        );
        context.getSource().getPlayerOrException()
                .openDialog(Holder.direct(new NoticeDialog(common, NoticeDialog.DEFAULT_ACTION)));
        return 1;
    }

    static MutableComponent buildHelpBody(MidgetControlConfig config, boolean shortInfoCommandRegistered) {
        MutableComponent body = Component.empty();
        appendHelpLine(body, "/midgethelp", "shows you all the available commands.");
        appendHelpLine(body, "/blipon", "lets other players see your blip on the locator bar.");
        appendHelpLine(body, "/blipoff", "hides your locator-bar blip from other players.");
        if (config.playerInfoEnabled()) {
            if (shortInfoCommandRegistered) {
                appendHelpLine(body, "/info <player>", "shows join date, online time, player kills, and deaths.");
            }
            appendHelpLine(
                    body,
                    "/midgetcontrol info <player>",
                    "shows join date, online time, player kills, and deaths."
            );
        }
        appendHelpLine(body, "/midgetcontrol reload", "reloads the config (server owner only).");
        return body;
    }

    private static void appendHelpLine(MutableComponent body, String command, String description) {
        if (!body.getString().isEmpty()) {
            body.append("\n\n");
        }
        body.append(Component.literal(command).withStyle(ChatFormatting.GOLD))
                .append(Component.literal(" " + description).withStyle(ChatFormatting.GRAY));
    }

    static String formatDuration(long millis) {
        long totalMinutes = Math.max(0L, millis) / 60_000L;
        long days = totalMinutes / (24L * 60L);
        long hours = totalMinutes / 60L % 24L;
        long minutes = totalMinutes % 60L;
        return unit(days, "day") + ", " + unit(hours, "hour") + ", " + unit(minutes, "minute");
    }

    private static String unit(long value, String name) {
        return value + " " + name + (value == 1 ? "" : "s");
    }

    private static boolean canUseInfo(CommandSourceStack source, MidgetControlConfig config) {
        return config.playerInfoEnabled() && hasPermission(source, config.playerInfoPermissionLevel());
    }

    private static boolean hasPermission(CommandSourceStack source, int level) {
        return switch (level) {
            case 0 -> true;
            case 1 -> Commands.hasPermission(Commands.LEVEL_MODERATORS).test(source);
            case 2 -> Commands.hasPermission(Commands.LEVEL_GAMEMASTERS).test(source);
            case 3 -> Commands.hasPermission(Commands.LEVEL_ADMINS).test(source);
            default -> Commands.hasPermission(Commands.LEVEL_OWNERS).test(source);
        };
    }
}
