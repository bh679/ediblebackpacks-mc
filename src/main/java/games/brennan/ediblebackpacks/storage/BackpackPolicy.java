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
}
