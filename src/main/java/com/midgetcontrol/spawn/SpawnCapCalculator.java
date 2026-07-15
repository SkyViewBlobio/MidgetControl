package com.midgetcontrol.spawn;

public final class SpawnCapCalculator {
    private SpawnCapCalculator() {
    }

    public static int scaledCap(int vanillaCategoryCap, int spawnableChunkCount, int capPercent, int spawnDiameter) {
        long denominator = (long) spawnDiameter * spawnDiameter * 100L;
        long scaled = (long) vanillaCategoryCap * spawnableChunkCount * capPercent;
        return (int) (scaled / denominator);
    }
}

