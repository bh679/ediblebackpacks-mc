package games.brennan.ediblebackpacks;

import net.minecraft.world.entity.player.Player;

import java.util.function.Predicate;

/**
 * Tiny host-mod API. A bundling/host mod (e.g. Dungeon Train) may declare what
 * the {@code resetOnDeath=DEFAULT} config value resolves to, without changing
 * the standalone mod's own default (which is "persist through death").
 *
 * <p>Call once during common setup. Server operators always win: an explicit
 * {@code ON}/{@code OFF} in the server config overrides the host default.</p>
 */
public final class EdibleBackpacksApi {

    private static volatile Boolean hostDefaultResetOnDeath = null;
    private static volatile Predicate<Player> accessLock = player -> false;

    private EdibleBackpacksApi() {}

    /** Host mod's answer for what config {@code DEFAULT} means. */
    public static void setHostDefaultResetOnDeath(boolean reset) {
        hostDefaultResetOnDeath = reset;
    }

    /** {@code null} = no host opinion (standalone install) → persist. */
    public static Boolean hostDefaultResetOnDeath() {
        return hostDefaultResetOnDeath;
    }

    /**
     * Host mod's lock on a player's backpack (e.g. Dungeon Train while the player is Zombified). While it
     * answers true the backpack slots stay visible but take nothing in and give nothing out, and pickups
     * skip the backpack. Must give the same answer on the client and the server. Call once during setup.
     */
    public static void setAccessLock(Predicate<Player> lock) {
        accessLock = lock == null ? player -> false : lock;
    }

    /** Whether the host has {@code player}'s backpack locked right now. False on a standalone install. */
    public static boolean isAccessLocked(Player player) {
        return player != null && accessLock.test(player);
    }
}
