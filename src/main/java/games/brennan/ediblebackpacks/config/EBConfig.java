package games.brennan.ediblebackpacks.config;

import games.brennan.ediblebackpacks.menu.BackpackLayout;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * SERVER config ({@code serverconfig/ediblebackpacks-server.toml}, per-world,
 * synced to clients). Kept deliberately tiny.
 */
public final class EBConfig {

    /** What happens to backpack slots + contents when the player dies. */
    public enum ResetMode {
        /** Defer to the host mod's opinion ({@code EdibleBackpacksApi}); persist when standalone. */
        DEFAULT,
        /** Always reset: slots drop to 0, contents drop at the death site. */
        ON,
        /** Never reset — backpack persists through death (the standalone behaviour). */
        OFF
    }

    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.EnumValue<ResetMode> RESET_ON_DEATH;
    public static final ModConfigSpec.IntValue MAX_SLOTS;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        RESET_ON_DEATH = builder
            .comment(
                "What happens to the backpack when a player dies.",
                "DEFAULT = follow the host mod's policy if one is installed (standalone = persist).",
                "ON = slots reset to 0 and contents drop at the death site.",
                "OFF = backpack persists through death.")
            .defineEnum("resetOnDeath", ResetMode.DEFAULT);

        MAX_SLOTS = builder
            .comment("Maximum backpack slots a player can unlock by eating backpacks (render cap "
                + BackpackLayout.MAX_SLOTS + ").")
            .defineInRange("maxSlots", BackpackLayout.MAX_SLOTS, 0, BackpackLayout.MAX_SLOTS);

        SPEC = builder.build();
    }

    private EBConfig() {}

    /** Config-loaded guard: fall back to defaults before the spec loads. */
    public static int maxSlots() {
        return SPEC.isLoaded() ? MAX_SLOTS.get() : BackpackLayout.MAX_SLOTS;
    }

    public static ResetMode resetMode() {
        return SPEC.isLoaded() ? RESET_ON_DEATH.get() : ResetMode.DEFAULT;
    }
}
