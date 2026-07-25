package com.midgetcontrol.tps;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TabTpsDisplayTest {
    @Test
    void formatsPlayerCountAndViewerPingInTheFooter() {
        assertEquals("Players: 3 | Ping: 42ms", TabTpsDisplay.createFooter(3, 42).getString());
        assertEquals("Players: 3 | Ping: 175ms", TabTpsDisplay.createFooter(3, 175).getString());
    }
}
