package games.brennan.ediblebackpacks;

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

    private EdibleBackpacksApi() {}

    /** Host mod's answer for what config {@code DEFAULT} means. */
    public static void setHostDefaultResetOnDeath(boolean reset) {
        hostDefaultResetOnDeath = reset;
    }

    /** {@code null} = no host opinion (standalone install) → persist. */
    public static Boolean hostDefaultResetOnDeath() {
        return hostDefaultResetOnDeath;
    }
}
