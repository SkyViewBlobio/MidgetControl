package com.midgetcontrol.spawn;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SpawnCapCalculatorTest {
    @Test
    void scalesVanillaCapByConfiguredPercentage() {
        assertEquals(35, SpawnCapCalculator.scaledCap(70, 289, 50, 17));
        assertEquals(70, SpawnCapCalculator.scaledCap(70, 289, 100, 17));
        assertEquals(0, SpawnCapCalculator.scaledCap(70, 289, 0, 17));
    }

    @Test
    void followsVanillaFlooringForPartialSpawnAreas() {
        assertEquals(17, SpawnCapCalculator.scaledCap(70, 144, 50, 17));
        assertEquals(0, SpawnCapCalculator.scaledCap(10, 20, 50, 17));
    }
}

