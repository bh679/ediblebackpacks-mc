package games.brennan.ediblebackpacks.storage;

import games.brennan.ediblebackpacks.config.EBConfig.ResetMode;

/**
 * Pure death-policy resolution (unit-tested; no Minecraft imports beyond the
 * config enum).
 */
public final class BackpackPolicy {

    private BackpackPolicy() {}

    /**
     * @param mode        the server-config value
     * @param hostDefault the host mod's opinion for {@code DEFAULT} ({@code null} = none)
     * @return whether the backpack resets on death
     */
    public static boolean shouldResetOnDeath(ResetMode mode, Boolean hostDefault) {
        return switch (mode) {
            case ON -> true;
            case OFF -> false;
            case DEFAULT -> hostDefault != null && hostDefault;
        };
    }

    /** Clamp an unlocked-slot count into {@code [0, max]}. */
    public static int clampUnlocked(int unlocked, int max) {
        return Math.max(0, Math.min(unlocked, max));
    }

    /**
     * How many slots eating a {@code granted}-slot backpack actually yields:
     * the whole grant, or whatever room is left under {@code max}.
     *
     * <p>Partial rather than all-or-nothing on purpose. Backpacks are
     * non-stackable, which makes a "craft one upgraded backpack back into
     * nine singles" recipe impossible (a crafting result is one stack, and a
     * count of 9 fails validation against a max stack size of 1). Without
     * that escape hatch, refusing a partial grant would strand an upgraded
     * backpack as a dead item for anyone near the cap.</p>
     *
     * @return slots actually granted; {@code 0} when already at the cap
     */
    public static int effectiveGrant(int unlocked, int granted, int max) {
        return Math.max(0, Math.min(granted, max - unlocked));
    }
}
