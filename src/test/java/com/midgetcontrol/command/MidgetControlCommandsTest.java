package com.midgetcontrol.command;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MidgetControlCommandsTest {
    @Test
    void formatsOnlineTimeAsDaysHoursAndMinutes() {
        long duration = ((2L * 24L + 3L) * 60L + 4L) * 60_000L;
        assertEquals("2 days, 3 hours, 4 minutes", MidgetControlCommands.formatDuration(duration));
        assertEquals("0 days, 1 hour, 1 minute", MidgetControlCommands.formatDuration(61L * 60_000L));
    }
}

