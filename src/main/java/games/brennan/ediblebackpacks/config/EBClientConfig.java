package games.brennan.ediblebackpacks.config;

import games.brennan.ediblebackpacks.client.ButtonPlacement;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * CLIENT config ({@code config/ediblebackpacks-client.toml}): the open/close button is a
 * convenience, so it can be turned off entirely and moved wherever the player wants. The
 * hotkey ({@code client/BackpackKeyBindings}) works either way, which is what makes hiding
 * the button safe.
 */
public final class EBClientConfig {

    /** Custom coordinates are relative to the GUI's top-left; keep them near the screen. */
    private static final int COORD_MIN = -512;
    private static final int COORD_MAX = 512;

    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue BUTTON_ENABLED;
    public static final ModConfigSpec.EnumValue<ButtonAnchor> BUTTON_ANCHOR;
    public static final ModConfigSpec.IntValue BUTTON_X;
    public static final ModConfigSpec.IntValue BUTTON_Y;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        BUTTON_ENABLED = builder
            .comment("Show the open/close button on the inventory and container screens.",
                "The keybind (Options -> Controls -> Edible Backpacks) still works when this is off.")
            .define("buttonEnabled", true);

        BUTTON_ANCHOR = builder
            .comment("Where the button sits.",
                "ABOVE_PANEL = just above the left backpack panel, the same spot on every screen.",
                "CUSTOM = at buttonX/buttonY below.",
                "The old preset spots are still available as CUSTOM coordinates:",
                "above the shield slot = " + ButtonPlacement.OFFHAND_X + ", " + ButtonPlacement.OFFHAND_Y,
                "right of the recipe-book button = "
                    + ButtonPlacement.RECIPE_BOOK_X + ", " + ButtonPlacement.RECIPE_BOOK_Y,
                "(both are inventory-screen landmarks: in a chest they land on container slots.)")
            .defineEnum("buttonAnchor", ButtonAnchor.ABOVE_PANEL);

        BUTTON_X = builder
            // The CUSTOM defaults are the ABOVE_PANEL anchor's, so switching to CUSTOM starts
            // the button where it already was. These are compile-time constants, so naming
            // ButtonPlacement here classloads nothing.
            .comment("CUSTOM only: button x, relative to the GUI's top-left corner "
                + "(the GUI is 176 wide; negative values sit left of it).")
            .defineInRange("buttonX", ButtonPlacement.ABOVE_PANEL_X, COORD_MIN, COORD_MAX);

        BUTTON_Y = builder
            .comment("CUSTOM only: button y, relative to the GUI's top-left corner "
                + "(the inventory GUI is 166 tall; negative values sit above it).")
            .defineInRange("buttonY", ButtonPlacement.ABOVE_PANEL_Y, COORD_MIN, COORD_MAX);

        SPEC = builder.build();
    }

    private EBClientConfig() {}

    /** Config-loaded guards: the screen can be built before the spec loads. */
    public static boolean buttonEnabled() {
        return !SPEC.isLoaded() || BUTTON_ENABLED.get();
    }

    public static ButtonAnchor buttonAnchor() {
        return SPEC.isLoaded() ? BUTTON_ANCHOR.get() : ButtonAnchor.ABOVE_PANEL;
    }

    public static int buttonX() {
        return SPEC.isLoaded() ? BUTTON_X.get() : ButtonPlacement.ABOVE_PANEL_X;
    }

    public static int buttonY() {
        return SPEC.isLoaded() ? BUTTON_Y.get() : ButtonPlacement.ABOVE_PANEL_Y;
    }
}
