package com.midgetcontrol.command;

import com.midgetcontrol.config.MidgetControlConfig;
import org.junit.jupiter.api.Test;
import org.slf4j.helpers.NOPLogger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MidgetControlCommandsTest {
    @Test
    void formatsOnlineTimeAsDaysHoursAndMinutes() {
        long duration = ((2L * 24L + 3L) * 60L + 4L) * 60_000L;
        assertEquals("2 days, 3 hours, 4 minutes", MidgetControlCommands.formatDuration(duration));
        assertEquals("0 days, 1 hour, 1 minute", MidgetControlCommands.formatDuration(61L * 60_000L));
    }

    @Test
    void helpDialogListsEveryCommand() {
        String help = MidgetControlCommands.buildHelpBody(
                MidgetControlConfig.defaults(NOPLogger.NOP_LOGGER),
                true
        ).getString();

        assertTrue(help.contains("/midgethelp"));
        assertTrue(help.contains("/blipon"));
        assertTrue(help.contains("/blipoff"));
        assertTrue(help.contains("/info <player>"));
        assertTrue(help.contains("/midgetcontrol info <player>"));
        assertTrue(help.contains("/midgetcontrol reload"));
    }
}

