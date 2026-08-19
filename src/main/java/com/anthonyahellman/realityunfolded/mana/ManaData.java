package com.anthonyahellman.realityunfolded.mana;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;

public final class ManaData {
    public static final double DEFAULT_MAX_MANA = 100.0D;
    public static final double DEFAULT_REGEN_PER_SECOND = 1.0D;

    private static final String ROOT_KEY = "reality_unfolded_mana";
    private static final String CURRENT_KEY = "current";
    private static final String MAX_KEY = "maximum";
    private static final String REGEN_KEY = "regen_per_second";
    private static final String INITIALIZED_KEY = "initialized";

    private final CompoundTag tag;

    private ManaData(CompoundTag tag) {
        this.tag = tag;
        initialize();
    }

    public static ManaData get(Player player) {
        CompoundTag persisted = player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
        CompoundTag mana = persisted.getCompound(ROOT_KEY);
        persisted.put(ROOT_KEY, mana);
        player.getPersistentData().put(Player.PERSISTED_NBT_TAG, persisted);
        return new ManaData(mana);
    }

    private void initialize() {
        if (!tag.getBoolean(INITIALIZED_KEY)) {
            tag.putDouble(MAX_KEY, DEFAULT_MAX_MANA);
            tag.putDouble(CURRENT_KEY, DEFAULT_MAX_MANA);
            tag.putDouble(REGEN_KEY, DEFAULT_REGEN_PER_SECOND);
            tag.putBoolean(INITIALIZED_KEY, true);
        }
        sanitize();
    }

    private void sanitize() {
        double maximum = Math.max(0.0D, finiteOrDefault(tag.getDouble(MAX_KEY), DEFAULT_MAX_MANA));
        double regen = Math.max(0.0D, finiteOrDefault(tag.getDouble(REGEN_KEY), DEFAULT_REGEN_PER_SECOND));
        double current = finiteOrDefault(tag.getDouble(CURRENT_KEY), maximum);
        tag.putDouble(MAX_KEY, maximum);
        tag.putDouble(REGEN_KEY, regen);
        tag.putDouble(CURRENT_KEY, clamp(current, 0.0D, maximum));
    }

    public double current() {
        return tag.getDouble(CURRENT_KEY);
    }

    public double maximum() {
        return tag.getDouble(MAX_KEY);
    }

    public double regenPerSecond() {
        return tag.getDouble(REGEN_KEY);
    }

    public boolean setCurrent(double value) {
        double next = clamp(finiteOrDefault(value, current()), 0.0D, maximum());
        if (Math.abs(next - current()) < 0.000001D) return false;
        tag.putDouble(CURRENT_KEY, next);
        return true;
    }

    public boolean add(double amount) {
        return setCurrent(current() + finiteOrDefault(amount, 0.0D));
    }

    public boolean consume(double amount) {
        double safeAmount = Math.max(0.0D, finiteOrDefault(amount, 0.0D));
        if (current() + 0.000001D < safeAmount) return false;
        setCurrent(current() - safeAmount);
        return true;
    }

    public void setMaximum(double value) {
        double maximum = Math.max(0.0D, finiteOrDefault(value, maximum()));
        tag.putDouble(MAX_KEY, maximum);
        setCurrent(Math.min(current(), maximum));
    }

    public void setRegenPerSecond(double value) {
        tag.putDouble(REGEN_KEY, Math.max(0.0D, finiteOrDefault(value, regenPerSecond())));
    }

    private static double finiteOrDefault(double value, double fallback) {
        return Double.isFinite(value) ? value : fallback;
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
