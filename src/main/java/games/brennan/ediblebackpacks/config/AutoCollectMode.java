package games.brennan.ediblebackpacks.config;

/**
 * The player's own choice of when ground pickups go into the backpack — a CLIENT setting
 * ({@link EBClientConfig#AUTO_COLLECT}) the client sends to the server, where
 * {@code storage/BackpackPickup} reads it off the player's attachment. The SERVER
 * {@link EBConfig#autoCollect()} switch still overrides it for the whole world.
 */
public enum AutoCollectMode {
    /** Pickups are vanilla; the backpack is filled by hand only. */
    OFF,
    /** Collect only while the panels are open — closed stays a lock (the default). */
    WHILE_OPEN,
    /** Collect even while the panels are closed, into slots the player can't see. */
    ALWAYS;

    public boolean collects(boolean panelsOpen) {
        return switch (this) {
            case OFF -> false;
            case WHILE_OPEN -> panelsOpen;
            case ALWAYS -> true;
        };
    }

    /** Lenient parse for saves and packets: unknown or missing names keep the default. */
    public static AutoCollectMode byName(String name) {
        for (AutoCollectMode mode : values()) {
            if (mode.name().equals(name)) return mode;
        }
        return WHILE_OPEN;
    }
}
