package com.anthonyahellman.realityunfolded.client;

public final class ClientMana {
    private static double current;
    private static double maximum = 100.0D;
    private static double regenPerSecond = 1.0D;

    private ClientMana() {}

    public static void update(double currentMana, double maximumMana, double regen) {
        current = currentMana;
        maximum = maximumMana;
        regenPerSecond = regen;
    }

    public static double current() { return current; }
    public static double maximum() { return maximum; }
    public static double regenPerSecond() { return regenPerSecond; }
}
